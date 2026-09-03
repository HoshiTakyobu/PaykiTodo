package com.example.todoalarm.sync

import android.content.Intent
import android.os.Build
import com.example.todoalarm.BuildConfig
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.alarm.ReminderDispatchTracker
import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.AppSettingsStore
import com.example.todoalarm.data.BackupArchiveCodec
import com.example.todoalarm.data.BackupSnapshot
import com.example.todoalarm.widget.TodoWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


data class AccountAuthResult(
    val error: String? = null,
    val recoveryCode: String? = null
) {
    val succeeded: Boolean get() = error == null
}

internal data class InitialSyncState(
    val revision: Long,
    val dirty: Boolean,
    val conflictRevision: Long
)

class RemoteSyncCoordinator(
    private val app: TodoApplication,
    private val settingsStore: AppSettingsStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val applyingRemote = AtomicBoolean(false)
    private var pendingJob: Job? = null
    private var periodicJob: Job? = null

    fun start() {
        val settings = settingsStore.currentSettings()
        if (!settings.remoteSyncEnabled || !settingsStore.hasValidRemoteSession()) {
            if (settings.remoteSyncUsername.isNotBlank()) settingsStore.clearExpiredRemoteSessionIfNeeded()
            return
        }
        if (periodicJob?.isActive == true) return
        periodicJob = scope.launch {
            while (isActive && settingsStore.currentSettings().remoteSyncEnabled) {
                runCatching { syncNow() }
                    .onFailure { settingsStore.markRemoteSyncError(it.message ?: "云同步失败") }
                delay(TimeUnit.MINUTES.toMillis(1))
            }
            periodicJob = null
        }
    }

    fun stop() {
        pendingJob?.cancel()
        periodicJob?.cancel()
        pendingJob = null
        periodicJob = null
    }

    fun onLocalDataChanged() {
        if (applyingRemote.get()) return
        val settings = settingsStore.currentSettings()
        if (!settings.remoteSyncEnabled || !settingsStore.hasValidRemoteSession()) {
            pendingJob?.cancel()
            pendingJob = null
            if (settings.remoteSyncUsername.isNotBlank()) settingsStore.clearExpiredRemoteSessionIfNeeded()
            return
        }
        if (settings.remoteSyncUsername.isNotBlank()) {
            settingsStore.markRemoteSyncDirty()
        }
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(1_000L)
            runCatching { syncNow() }
                .onFailure { settingsStore.markRemoteSyncError(it.message ?: "云同步失败") }
        }
    }

    suspend fun authenticate(
        serverUrl: String,
        username: String,
        password: String,
        displayName: String,
        register: Boolean
    ): String? = syncMutex.withLock {
        authenticateAccount(username, password, displayName, register).error
    }

    suspend fun login(username: String, password: String): AccountAuthResult {
        return syncMutex.withLock { authenticateAccount(username, password, "", false) }
    }

    suspend fun register(username: String, displayName: String, password: String): AccountAuthResult {
        return syncMutex.withLock { authenticateAccount(username, password, displayName, true) }
    }

    suspend fun recoverPassword(username: String, recoveryCode: String, newPassword: String): String? = syncMutex.withLock {
        runCatching {
            postJson(
                cloudServerUrl(),
                "/v2/auth/recover",
                JSONObject()
                    .put("username", username.trim().lowercase())
                    .put("recoveryCode", recoveryCode.trim())
                    .put("newPassword", newPassword)
            )
            null
        }.getOrElse { error -> error.message ?: "找回密码失败" }
    }

    private suspend fun authenticateAccount(
        username: String,
        password: String,
        displayName: String,
        register: Boolean
    ): AccountAuthResult {
        return runCatching {
            val normalizedServer = cloudServerUrl()
            val normalizedUsername = username.trim().lowercase()
            require(normalizedUsername.matches(Regex("[A-Za-z0-9_.-]{3,64}"))) { "账号格式无效" }
            require(password.length >= if (register) 10 else 1) { if (register) "注册密码至少 10 位" else "请输入密码" }
            val deviceId = settingsStore.remoteSyncDeviceId()
            val response = postJson(
                normalizedServer,
                if (register) "/v2/auth/register" else "/v2/auth/login",
                JSONObject().apply {
                    put("username", normalizedUsername)
                    put("password", password)
                    put("displayName", displayName.trim().ifBlank { normalizedUsername })
                    put("deviceId", deviceId)
                    put("deviceName", Build.MODEL.ifBlank { "Android 设备" })
                    put("platform", "android")
                }
            )
            val canonicalUsername = response.getJSONObject("user").getString("username")
            val remoteRevision = response.optLong("snapshotRevision", 0L)
            val localSnapshot = app.repository.exportSnapshot(settingsStore.currentSettings())
            val hasLocalData = localSnapshot.hasMeaningfulData()
            val initialSyncState = initialSyncState(hasLocalData, remoteRevision)
            settingsStore.saveRemoteAccountSession(
                serverUrl = normalizedServer,
                username = canonicalUsername,
                deviceId = response.optString("deviceId", deviceId),
                accessToken = response.getString("accessToken"),
                refreshToken = response.getString("refreshToken"),
                accessExpiresAt = response.getLong("accessExpiresAt"),
                refreshExpiresAt = response.getLong("refreshExpiresAt"),
                encryptionSecret = deriveAccountSecret(canonicalUsername, password),
                revision = initialSyncState.revision,
                dirty = initialSyncState.dirty,
                conflictRevision = initialSyncState.conflictRevision
            )
            TodoWidgetProvider.notifyWidgetDataChanged(app)
            start()
            when {
                remoteRevision > 0L && hasLocalData -> {
                    settingsStore.markRemoteSyncConflict(remoteRevision)
                    AccountAuthResult(recoveryCode = response.optString("recoveryCode").takeIf { it.isNotBlank() })
                }
                else -> when (val result = syncAccountNow()) {
                    is RemoteSyncResult.Failed -> AccountAuthResult(result.message)
                    else -> AccountAuthResult(recoveryCode = response.optString("recoveryCode").takeIf { it.isNotBlank() })
                }
            }
        }.getOrElse { error ->
            AccountAuthResult(error.message ?: if (register) "注册失败" else "登录失败")
        }
    }

    fun logout() {
        val settings = settingsStore.currentSettings()
        scope.launch {
            runCatching {
                if (settings.remoteSyncAccessToken.isNotBlank()) {
                    postJson(cloudServerUrl(), "/v2/auth/logout", JSONObject(), settings.remoteSyncAccessToken)
                }
            }
        }
        stop()
        settingsStore.clearRemoteAccount()
        TodoWidgetProvider.notifyWidgetDataChanged(app)
        app.desktopSyncCoordinator.stop()
        app.stopService(Intent(app, DesktopSyncService::class.java))
    }

    suspend fun syncNow(): RemoteSyncResult = syncMutex.withLock {
        val settings = settingsStore.currentSettings()
        if (!settings.remoteSyncEnabled) return@withLock RemoteSyncResult.Disabled
        if (!settingsStore.hasValidRemoteSession()) {
            if (settings.remoteSyncUsername.isNotBlank()) settingsStore.clearExpiredRemoteSessionIfNeeded()
            return@withLock RemoteSyncResult.NotConfigured
        }
        if (settings.remoteSyncUsername.isBlank()) return@withLock RemoteSyncResult.NotConfigured
        syncAccountNow()
    }

    private suspend fun syncAccountNow(): RemoteSyncResult {
        var settings = settingsStore.currentSettings()
        if (!settings.remoteSyncEnabled || settings.remoteSyncUsername.isBlank() || !settingsStore.hasValidRemoteSession()) return RemoteSyncResult.NotConfigured
        val serverUrl = cloudServerUrl()
        val accessToken = ensureAccessToken(serverUrl, settings)
        settings = settingsStore.currentSettings()
        val status = getJson(serverUrl, "/v2/sync/status", accessToken)
        val remoteRevision = status.optLong("revision", 0L)
        val localRevision = settings.remoteSyncRevision

        if (remoteRevision > localRevision) {
            if (settings.remoteSyncDirty) {
                settingsStore.markRemoteSyncConflict(remoteRevision)
                return RemoteSyncResult.Conflict(remoteRevision)
            }
            val snapshot = downloadAccountSnapshot(serverUrl, accessToken, settings.remoteSyncSecret)
            applyRemoteSnapshot(snapshot)
            settingsStore.markRemoteSyncSuccess(remoteRevision)
            return RemoteSyncResult.Downloaded(remoteRevision)
        }
        if (remoteRevision < localRevision) {
            settingsStore.markRemoteSyncConflict(remoteRevision)
            return RemoteSyncResult.Failed("云端版本低于本机记录，已停止同步以避免覆盖")
        }
        if (!settings.remoteSyncDirty) {
            settingsStore.markRemoteSyncSuccess(localRevision)
            return RemoteSyncResult.UpToDate(localRevision)
        }

        val snapshot = app.repository.exportSnapshot(settings)
        val encrypted = RemoteSyncCrypto.encrypt(BackupArchiveCodec.encode(snapshot), settings.remoteSyncSecret)
        val upload = uploadAccountSnapshot(serverUrl, accessToken, localRevision, encrypted)
        if (upload.conflictRevision != null) {
            settingsStore.markRemoteSyncConflict(upload.conflictRevision)
            return RemoteSyncResult.Conflict(upload.conflictRevision)
        }
        settingsStore.markRemoteSyncSuccess(upload.revision)
        return RemoteSyncResult.Uploaded(snapshot.exportedAtMillis, upload.revision)
    }

    suspend fun downloadLatest(): BackupSnapshot? = syncMutex.withLock {
        val settings = settingsStore.currentSettings()
        if (!settings.remoteSyncEnabled) return@withLock null
        if (settings.remoteSyncUsername.isNotBlank() && settingsStore.hasValidRemoteSession()) {
            val serverUrl = cloudServerUrl()
            val token = ensureAccessToken(serverUrl, settings)
            return@withLock runCatching { downloadAccountSnapshot(serverUrl, token, settings.remoteSyncSecret) }
                .getOrElse { error ->
                    if (error is RemoteNotFoundException) null else throw error
                }
        }
        return@withLock null
    }

    private suspend fun applyRemoteSnapshot(snapshot: BackupSnapshot) {
        val current = settingsStore.currentSettings()
        applyingRemote.set(true)
        try {
            app.repository.getAllTodos().forEach { existing ->
                app.alarmScheduler.cancel(existing.id)
                ReminderDispatchTracker.clear(app, existing.id)
            }
            app.repository.importSnapshot(snapshot)
            settingsStore.replaceAll(snapshot.settings.withRemoteAccountFrom(current))
            app.repository.ensureRecurringInstancesAhead()
            app.repository.futureReminderItems(System.currentTimeMillis()).forEach(app.alarmScheduler::schedule)
            TodoWidgetProvider.notifyWidgetDataChanged(app)
        } finally {
            applyingRemote.set(false)
        }
    }

    private fun AppSettings.withRemoteAccountFrom(current: AppSettings): AppSettings = copy(
        backupDirectoryUri = current.backupDirectoryUri,
        autoBackupEnabled = current.autoBackupEnabled,
        remoteSyncEnabled = current.remoteSyncEnabled,
        remoteSyncServerUrl = current.remoteSyncServerUrl,
        remoteSyncDeviceId = current.remoteSyncDeviceId,
        remoteSyncSecret = current.remoteSyncSecret,
        remoteSyncUsername = current.remoteSyncUsername,
        remoteSyncAccessToken = current.remoteSyncAccessToken,
        remoteSyncRefreshToken = current.remoteSyncRefreshToken,
        remoteSyncAccessExpiresAt = current.remoteSyncAccessExpiresAt,
        remoteSyncRefreshExpiresAt = current.remoteSyncRefreshExpiresAt,
        remoteSyncRevision = current.remoteSyncRevision,
        remoteSyncDirty = current.remoteSyncDirty,
        remoteSyncConflictRevision = current.remoteSyncConflictRevision,
        remoteSyncLastSuccessAt = current.remoteSyncLastSuccessAt,
        remoteSyncLastError = current.remoteSyncLastError,
        desktopSyncEnabled = current.desktopSyncEnabled,
        desktopSyncToken = current.desktopSyncToken,
        planningAiEnabled = current.planningAiEnabled,
        planningAiProviderName = current.planningAiProviderName,
        planningAiBaseUrl = current.planningAiBaseUrl,
        planningAiApiKey = current.planningAiApiKey,
        planningAiModel = current.planningAiModel,
        planningAiProviders = current.planningAiProviders
    )

    private fun BackupSnapshot.hasMeaningfulData(): Boolean {
        return tasks.isNotEmpty() || templates.isNotEmpty() || scheduleTemplates.isNotEmpty() ||
            planningNotes.isNotEmpty() || planningNodes.isNotEmpty() || aiReports.isNotEmpty() ||
            activityRecords.isNotEmpty()
    }

    private fun ensureAccessToken(serverUrl: String, settings: AppSettings): String {
        val now = System.currentTimeMillis() / 1000L
        if (settings.remoteSyncRefreshExpiresAt <= now) {
            settingsStore.clearRemoteAccount()
            throw IOException("登录已过期，请重新登录")
        }
        if (settings.remoteSyncAccessToken.isNotBlank() && settings.remoteSyncAccessExpiresAt > now + 30L) {
            return settings.remoteSyncAccessToken
        }
        require(settings.remoteSyncRefreshToken.isNotBlank()) { "登录已过期，请重新登录" }
        val response = postJson(
            serverUrl,
            "/v2/auth/refresh",
            JSONObject().put("refreshToken", settings.remoteSyncRefreshToken)
        )
        settingsStore.updateRemoteAccountTokens(
            accessToken = response.getString("accessToken"),
            refreshToken = response.getString("refreshToken"),
            accessExpiresAt = response.getLong("accessExpiresAt"),
            refreshExpiresAt = response.getLong("refreshExpiresAt")
        )
        return response.getString("accessToken")
    }

    private fun downloadAccountSnapshot(serverUrl: String, accessToken: String, secret: String): BackupSnapshot {
        val connection = openApiConnection(serverUrl, "/v2/sync/snapshot", "GET", accessToken)
        return try {
            when (val status = connection.responseCode) {
                HttpURLConnection.HTTP_NOT_FOUND -> throw RemoteNotFoundException()
                in 200..299 -> {
                    val encrypted = connection.inputStream.use { it.readBytesLimited(MAX_PAYLOAD_BYTES) }
                    BackupArchiveCodec.decode(RemoteSyncCrypto.decrypt(encrypted, secret))
                }
                else -> throw apiException(connection, status)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun uploadAccountSnapshot(
        serverUrl: String,
        accessToken: String,
        revision: Long,
        payload: ByteArray
    ): AccountUploadResult {
        val connection = openApiConnection(serverUrl, "/v2/sync/snapshot", "PUT", accessToken)
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.setRequestProperty("If-Match", "\"$revision\"")
            connection.setRequestProperty("X-Payki-Operation-Id", "android:${UUID.randomUUID()}")
            connection.setFixedLengthStreamingMode(payload.size)
            connection.outputStream.use { it.write(payload) }
            val status = connection.responseCode
            val json = connection.readJsonResponse(status)
            if (status == HttpURLConnection.HTTP_CONFLICT) {
                return AccountUploadResult(revision, json.optLong("currentRevision", revision))
            }
            if (status !in 200..299) throw apiException(connection, status, json)
            return AccountUploadResult(json.getLong("revision"), null)
        } finally {
            connection.disconnect()
        }
    }

    private fun getJson(serverUrl: String, path: String, accessToken: String): JSONObject {
        val connection = openApiConnection(serverUrl, path, "GET", accessToken)
        return try {
            val status = connection.responseCode
            val json = connection.readJsonResponse(status)
            if (status !in 200..299) throw apiException(connection, status, json)
            json
        } finally {
            connection.disconnect()
        }
    }

    private fun postJson(serverUrl: String, path: String, body: JSONObject, accessToken: String = ""): JSONObject {
        val payload = body.toString().toByteArray(StandardCharsets.UTF_8)
        val connection = openApiConnection(serverUrl, path, "POST", accessToken)
        return try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setFixedLengthStreamingMode(payload.size)
            connection.outputStream.use { it.write(payload) }
            val status = connection.responseCode
            val json = connection.readJsonResponse(status)
            if (status !in 200..299) throw apiException(connection, status, json)
            json
        } finally {
            connection.disconnect()
        }
    }

    private fun openApiConnection(serverUrl: String, path: String, method: String, accessToken: String): HttpURLConnection {
        val normalized = normalizeServerUrl(serverUrl)
        return (URL(normalized + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 20_000
            useCaches = false
            setRequestProperty("Accept", "application/json, application/octet-stream")
            if (accessToken.isNotBlank()) setRequestProperty("Authorization", "Bearer $accessToken")
        }
    }

    private fun cloudServerUrl(): String {
        return normalizeServerUrl(BuildConfig.PAYKITODO_CLOUD_BASE_URL)
    }

    private fun normalizeServerUrl(value: String): String {
        val normalized = value.trim().trimEnd('/')
        val url = URL(normalized)
        val localDevelopment = url.host == "127.0.0.1" || url.host == "localhost" || url.host == "10.0.2.2"
        require(url.protocol.equals("https", ignoreCase = true) || localDevelopment) { "云同步服务器必须使用 HTTPS" }
        return normalized
    }

    private fun HttpURLConnection.readJsonResponse(status: Int): JSONObject {
        val stream = if (status in 200..299) inputStream else errorStream
        val raw = stream?.use { it.readBytesLimited(MAX_JSON_BYTES) } ?: ByteArray(0)
        return if (raw.isEmpty()) JSONObject() else JSONObject(raw.toString(StandardCharsets.UTF_8))
    }

    private fun apiException(connection: HttpURLConnection, status: Int, parsed: JSONObject? = null): IOException {
        val json = parsed ?: runCatching { connection.readJsonResponse(status) }.getOrDefault(JSONObject())
        return IOException(json.optString("message").ifBlank { "云服务请求失败（HTTP $status）" })
    }

    private suspend fun syncLegacyNow(settings: AppSettings): RemoteSyncResult {
        val serverUrl = normalizeServerUrl(settings.remoteSyncServerUrl)
        val credentials = settingsStore.remoteSyncCredentials()
        val snapshot = app.repository.exportSnapshot(settings)
        val encrypted = RemoteSyncCrypto.encrypt(BackupArchiveCodec.encode(snapshot), credentials.second)
        uploadLegacy(serverUrl, credentials.first, credentials.second, encrypted)
        return RemoteSyncResult.Uploaded(snapshot.exportedAtMillis, 0L)
    }

    private fun uploadLegacy(serverUrl: String, deviceId: String, secret: String, payload: ByteArray) {
        val connection = openLegacyConnection(serverUrl, deviceId, secret, "PUT")
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.setFixedLengthStreamingMode(payload.size)
            connection.outputStream.use { it.write(payload) }
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("远程同步上传失败（HTTP $status）")
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadLegacy(serverUrl: String, deviceId: String, secret: String): ByteArray? {
        val connection = openLegacyConnection(serverUrl, deviceId, secret, "GET")
        return try {
            when (val status = connection.responseCode) {
                HttpURLConnection.HTTP_NOT_FOUND -> null
                in 200..299 -> connection.inputStream.use { it.readBytesLimited(MAX_PAYLOAD_BYTES) }
                else -> throw IOException("远程同步下载失败（HTTP $status）")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun openLegacyConnection(serverUrl: String, deviceId: String, secret: String, method: String): HttpURLConnection {
        val endpoint = "$serverUrl/v1/sync/${java.net.URLEncoder.encode(deviceId, "UTF-8")}"
        return (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 15_000
            useCaches = false
            setRequestProperty("Authorization", "Bearer $secret")
            setRequestProperty("X-Payki-Device", deviceId)
        }
    }

    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
            require(output.size() <= limit) { "远程同步数据过大" }
        }
        return output.toByteArray()
    }

    sealed interface RemoteSyncResult {
        data object Disabled : RemoteSyncResult
        data object NotConfigured : RemoteSyncResult
        data class Uploaded(val exportedAtMillis: Long, val revision: Long) : RemoteSyncResult
        data class Downloaded(val revision: Long) : RemoteSyncResult
        data class UpToDate(val revision: Long) : RemoteSyncResult
        data class Conflict(val remoteRevision: Long) : RemoteSyncResult
        data class Failed(val message: String) : RemoteSyncResult
    }

    private data class AccountUploadResult(
        val revision: Long,
        val conflictRevision: Long?
    )

    private class RemoteNotFoundException : IOException("云端暂无数据")

    companion object {
        private const val MAX_PAYLOAD_BYTES = 32 * 1024 * 1024
        private const val MAX_JSON_BYTES = 128 * 1024

        internal fun initialSyncState(hasLocalData: Boolean, remoteRevision: Long): InitialSyncState {
            val normalizedRemoteRevision = remoteRevision.coerceAtLeast(0L)
            return if (hasLocalData) {
                InitialSyncState(
                    revision = 0L,
                    dirty = true,
                    conflictRevision = normalizedRemoteRevision.takeIf { it > 0L } ?: 0L
                )
            } else {
                // An empty device must start at revision zero so an existing remote
                // snapshot is downloaded during the first authenticated sync.
                InitialSyncState(revision = 0L, dirty = false, conflictRevision = 0L)
            }
        }

        internal fun deriveAccountSecret(username: String, password: String): String {
            val material = "PaykiTodo Account Sync v2\u0000${username.trim().lowercase()}\u0000$password"
                .toByteArray(StandardCharsets.UTF_8)
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(material)
            )
        }
    }
}

internal object RemoteSyncCrypto {
    private val magic = "PKSYNC1".toByteArray(StandardCharsets.US_ASCII)
    private val salt = "PaykiTodo Remote Sync v1".toByteArray(StandardCharsets.UTF_8)

    fun encrypt(payload: ByteArray, secret: String): ByteArray {
        try {
            val iv = ByteArray(12).also { java.security.SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(secret), GCMParameterSpec(128, iv))
            val ciphertext = cipher.doFinal(payload)
            return ByteBuffer.allocate(magic.size + 1 + iv.size + ciphertext.size)
                .put(magic)
                .put(1)
                .put(iv)
                .put(ciphertext)
                .array()
        } catch (error: GeneralSecurityException) {
            throw IllegalStateException("远程同步加密失败", error)
        }
    }

    fun decrypt(envelope: ByteArray, secret: String): ByteArray {
        try {
            require(envelope.size > magic.size + 1 + 12 + 16) { "远程同步数据不完整" }
            require(envelope.copyOfRange(0, magic.size).contentEquals(magic)) { "远程同步数据格式无效" }
            require(envelope[magic.size].toInt() == 1) { "远程同步数据版本过新" }
            val ivStart = magic.size + 1
            val iv = envelope.copyOfRange(ivStart, ivStart + 12)
            val ciphertext = envelope.copyOfRange(ivStart + 12, envelope.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(secret), GCMParameterSpec(128, iv))
            return cipher.doFinal(ciphertext)
        } catch (error: GeneralSecurityException) {
            throw IllegalArgumentException("账号密码不匹配或云端数据已损坏", error)
        }
    }

    private fun deriveKey(secret: String): SecretKeySpec {
        val spec = PBEKeySpec(secret.toCharArray(), salt, 120_000, 256)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(key, "AES")
    }
}
