package com.example.todoalarm.sync

import android.content.Intent
import android.os.Build
import android.net.ConnectivityManager
import android.net.Network
import androidx.room.withTransaction
import java.io.File
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
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

data class AccountDevice(
    val id: String,
    val name: String,
    val platform: String,
    val lastSeenAt: Long,
    val current: Boolean,
    val active: Boolean
)

internal data class InitialSyncState(
    val revision: Long,
    val dirty: Boolean,
    val conflictRevision: Long
)

class RemoteSyncCoordinator(
    private val app: TodoApplication,
    private val settingsStore: AppSettingsStore,
    private val serverBaseUrl: String = BuildConfig.PAYKITODO_CLOUD_BASE_URL
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private var pendingJob: Job? = null
    private var periodicJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    @Volatile private var accountGeneration = 0L
    private data class Download(val snapshot: BackupSnapshot, val revision: Long)
    data class CloudReview(val snapshot: BackupSnapshot, val revision: Long, val localFingerprint: String, val accountGeneration: Long)

    @Synchronized fun start() {
        val settings = settingsStore.currentSettings()
        if (!settings.remoteSyncEnabled || !settingsStore.hasLocalAccountAccess()) return
        RemoteSyncJobService.schedule(app)
        if (networkCallback == null) {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { scope.launch { runCatching { syncNow() } } }
            }
            app.getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(callback)
            networkCallback = callback
        }
        if (periodicJob?.isActive == true) return
        periodicJob = scope.launch {
            while (isActive && settingsStore.currentSettings().remoteSyncEnabled) {
                runCatching { syncNow() }
                    .onFailure { settingsStore.markRemoteSyncError(it.message ?: "云同步失败") }
                delay(TimeUnit.SECONDS.toMillis(15))
            }
            periodicJob = null
        }
    }

    @Synchronized fun stop() {
        RemoteSyncJobService.cancel(app)
        networkCallback?.let { callback ->
            runCatching { app.getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(callback) }
        }
        networkCallback = null
        pendingJob?.cancel()
        periodicJob?.cancel()
        pendingJob = null
        periodicJob = null
    }

    fun onLocalDataChanged() {
        val settings = settingsStore.currentSettings()
        if (!settingsStore.hasLocalAccountAccess()) return
        settingsStore.markRemoteSyncDirty()
        if (settings.remoteSyncEnabled && settings.remoteSyncUsername.isNotBlank()) RemoteSyncJobService.schedule(app, changed = true)
        if (!settings.remoteSyncEnabled || !settingsStore.hasValidRemoteSession()) {
            pendingJob?.cancel()
            pendingJob = null
            if (settings.remoteSyncUsername.isNotBlank()) settingsStore.clearExpiredRemoteSessionIfNeeded()
            return
        }
        requestLocalSync()
    }

    private fun requestLocalSync(): Unit = synchronized(this) {
        if (pendingJob != null) return@synchronized
        pendingJob = scope.launch(start = CoroutineStart.LAZY) {
            var completedGeneration = -1L
            var successful = false
            try {
                delay(1_000L)
                do {
                    completedGeneration = settingsStore.remoteChangeGeneration()
                    val result = syncNow()
                    successful = result is RemoteSyncResult.Uploaded || result is RemoteSyncResult.UpToDate || result is RemoteSyncResult.Downloaded
                } while (isActive && successful && settingsStore.currentSettings().remoteSyncDirty)
            } finally {
                synchronized(this@RemoteSyncCoordinator) {
                    if (pendingJob === coroutineContext[Job]) {
                        pendingJob = null
                        if (isActive && successful && completedGeneration != settingsStore.remoteChangeGeneration() &&
                            settingsStore.currentSettings().remoteSyncEnabled && settingsStore.hasValidRemoteSession()) requestLocalSync()
                    }
                }
            }
        }
        pendingJob?.start()
        Unit
    }

    suspend fun authenticate(
        serverUrl: String,
        username: String,
        password: String,
        displayName: String,
        register: Boolean,
        turnstileToken: String? = null
    ): String? = syncMutex.withLock {
        authenticateAccount(username, password, displayName, register, turnstileToken).error
    }

    suspend fun login(username: String, password: String, turnstileToken: String? = null): AccountAuthResult {
        return withContext(Dispatchers.IO) { syncMutex.withLock { authenticateAccount(username, password, "", false, turnstileToken) } }
    }

    suspend fun register(username: String, displayName: String, password: String, turnstileToken: String? = null): AccountAuthResult {
        return withContext(Dispatchers.IO) { syncMutex.withLock { authenticateAccount(username, password, displayName, true, turnstileToken) } }
    }

    suspend fun recoverPassword(
        username: String,
        recoveryCode: String,
        newPassword: String,
        turnstileToken: String? = null
    ): String? = syncMutex.withLock {
        runCatching {
            postJson(
                cloudServerUrl(),
                "/v2/auth/recover",
                JSONObject()
                    .put("username", username.trim().lowercase())
                    .put("recoveryCode", recoveryCode.trim())
                    .put("newPassword", newPassword)
                    .apply {
                        turnstileToken?.trim()?.takeIf { it.isNotBlank() }?.let { put("turnstileToken", it) }
                    }
            )
            null
        }.getOrElse { error -> error.message ?: "找回密码失败" }
    }

    private suspend fun authenticateAccount(
        username: String,
        password: String,
        displayName: String,
        register: Boolean,
        turnstileToken: String?
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
                    turnstileToken?.trim()?.takeIf { it.isNotBlank() }?.let { put("turnstileToken", it) }
                }
            )
            val canonicalUsername = response.getJSONObject("user").getString("username")
            val remoteRevision = response.optLong("snapshotRevision", 0L)
            val localSnapshot = app.repository.exportSnapshot(settingsStore.currentSettings())
            val hasLocalData = localSnapshot.hasMeaningfulData()
            val previous = settingsStore.currentSettings()
            val sameAccount = previous.remoteSyncUsername == canonicalUsername && previous.remoteSyncSecret == deriveAccountSecret(canonicalUsername, password)
            val initialSyncState = if (sameAccount) InitialSyncState(previous.remoteSyncRevision, previous.remoteSyncDirty, previous.remoteSyncConflictRevision)
                else initialSyncState(hasLocalData, remoteRevision)
            if (!sameAccount && hasLocalData) saveSafetyCopy(localSnapshot, "before-account-change")
            accountGeneration++
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
            response.getJSONObject("user").let { user ->
                settingsStore.updateRemoteAccountProfile(user.optLong("id"), user.optString("displayName", canonicalUsername))
            }
            TodoWidgetProvider.notifyWidgetDataChanged(app)
            start()
            when {
                !sameAccount && remoteRevision > 0L && hasLocalData -> {
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

    suspend fun refreshAccountProfile(): Unit = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val token = ensureAccessToken(cloudServerUrl(), settingsStore.currentSettings())
            val user = getJson(cloudServerUrl(), "/v2/account", token).getJSONObject("user")
            settingsStore.updateRemoteAccountProfile(user.getLong("id"), user.getString("displayName"))
        }
    }

    suspend fun updateDisplayName(displayName: String): Unit = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val token = ensureAccessToken(cloudServerUrl(), settingsStore.currentSettings())
            val user = postJson(cloudServerUrl(), "/v2/account", JSONObject().put("displayName", displayName.trim()), token)
                .getJSONObject("user")
            settingsStore.updateRemoteAccountProfile(user.getLong("id"), user.getString("displayName"))
        }
    }

    suspend fun accountDevices(): List<AccountDevice> = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val token = ensureAccessToken(cloudServerUrl(), settingsStore.currentSettings())
            val devices = getJson(cloudServerUrl(), "/v2/devices", token).getJSONArray("devices")
            List(devices.length()) { index ->
                val device = devices.getJSONObject(index)
                AccountDevice(
                    id = device.getString("id"), name = device.getString("name"), platform = device.getString("platform"),
                    lastSeenAt = device.getLong("lastSeenAt"), current = device.getBoolean("current"),
                    active = device.optBoolean("active", !device.optBoolean("revoked"))
                )
            }
        }
    }

    suspend fun revokeAccountDevice(deviceId: String): Unit = withContext(Dispatchers.IO) {
        require(deviceId.matches(Regex("[A-Za-z0-9_-]{8,128}"))) { "设备 ID 无效" }
        syncMutex.withLock {
            val token = ensureAccessToken(cloudServerUrl(), settingsStore.currentSettings())
            val connection = openApiConnection(cloudServerUrl(), "/v2/devices/$deviceId", "DELETE", token)
            try {
                val status = connection.responseCode
                if (status !in 200..299) throw apiException(connection, status)
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun changeAccountPassword(currentPassword: String, newPassword: String): Unit = withContext(Dispatchers.IO) {
        require(newPassword.length in 10..256) { "新密码需为 10-256 位" }
        require(currentPassword != newPassword) { "新密码不能与原密码相同" }
        syncMutex.withLock {
            val settings = settingsStore.currentSettings()
            val server = cloudServerUrl()
            val token = ensureAccessToken(server, settings)
            val oldSecret = deriveAccountSecret(settings.remoteSyncUsername, currentPassword)
            require(oldSecret == settings.remoteSyncSecret) { "原密码不正确" }
            val newSecret = deriveAccountSecret(settings.remoteSyncUsername, newPassword)
            val connection = openApiConnection(server, "/v2/sync/snapshot", "GET", token)
            var revision = 0L
            val encrypted = try {
                when (val status = connection.responseCode) {
                    HttpURLConnection.HTTP_NOT_FOUND -> null
                    in 200..299 -> {
                        revision = connection.getHeaderField("X-Payki-Revision")?.toLongOrNull()
                            ?: throw IOException("云端未返回数据版本，请重试")
                        val snapshot = connection.inputStream.use { it.readBytesLimited(MAX_PAYLOAD_BYTES) }
                        RemoteSyncCrypto.encrypt(RemoteSyncCrypto.decrypt(snapshot, oldSecret), newSecret)
                    }
                    else -> throw apiException(connection, status)
                }
            } finally {
                connection.disconnect()
            }
            postJson(
                server, "/v2/account/password",
                JSONObject().put("currentPassword", currentPassword).put("newPassword", newPassword)
                    .put("snapshotRevision", revision)
                    .put("encryptedSnapshot", encrypted?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) } ?: JSONObject.NULL),
                token
            )
            logout()
        }
    }

    fun logout() {
        accountGeneration++
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

    suspend fun syncNow(): RemoteSyncResult = withContext(Dispatchers.IO) { syncMutex.withLock {
        val settings = settingsStore.currentSettings()
        if (!settings.remoteSyncEnabled) return@withLock RemoteSyncResult.Disabled
        if (!settingsStore.hasValidRemoteSession()) {
            if (settings.remoteSyncUsername.isNotBlank()) settingsStore.clearExpiredRemoteSessionIfNeeded()
            return@withLock RemoteSyncResult.NotConfigured
        }
        if (settings.remoteSyncUsername.isBlank()) return@withLock RemoteSyncResult.NotConfigured
        try { syncAccountNow() } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            settingsStore.markRemoteSyncError(error.message ?: "云同步失败")
            RemoteSyncResult.Failed(error.message ?: "云同步失败")
        }
    } }

    private suspend fun syncAccountNow(): RemoteSyncResult {
        var settings = settingsStore.currentSettings()
        if (!settings.remoteSyncEnabled || settings.remoteSyncUsername.isBlank() || !settingsStore.hasValidRemoteSession()) return RemoteSyncResult.NotConfigured
        val serverUrl = cloudServerUrl()
        val account = accountGeneration
        val accessToken = ensureAccessToken(serverUrl, settings)
        if (account != accountGeneration || !settingsStore.hasLocalAccountAccess()) return RemoteSyncResult.NotConfigured
        settings = settingsStore.currentSettings()
        val generation = settingsStore.remoteChangeGeneration()
        val localBefore = app.database.withTransaction { app.repository.exportSnapshot(settings) }
        val fingerprint = BackupArchiveCodec.dataFingerprint(localBefore)
        val previousFingerprint = settingsStore.remoteDataFingerprint()
        if (!settings.remoteSyncDirty && previousFingerprint.isNotBlank() && previousFingerprint != fingerprint) {
            settingsStore.markRemoteSyncDirty()
            settings = settingsStore.currentSettings()
        }
        val status = getJson(serverUrl, "/v2/sync/status", accessToken)
        if (account != accountGeneration || !settingsStore.hasLocalAccountAccess()) return RemoteSyncResult.NotConfigured
        val remoteRevision = status.optLong("revision", 0L)
        val localRevision = settings.remoteSyncRevision

        if (settings.remoteSyncDirty && settings.remoteSyncConflictRevision > 0L) {
            settingsStore.markRemoteSyncConflict(maxOf(remoteRevision, settings.remoteSyncConflictRevision))
            return RemoteSyncResult.Conflict(remoteRevision)
        }

        // Older builds may have a clean flag without an acknowledged content hash.
        // Inspect instead of silently replacing a potentially newer phone copy.
        if (!settings.remoteSyncDirty && previousFingerprint.isBlank() && remoteRevision > 0L && localBefore.hasMeaningfulData()) {
            val remote = downloadAccountSnapshot(serverUrl, accessToken, settings.remoteSyncSecret)
            if (fingerprint != BackupArchiveCodec.dataFingerprint(remote.snapshot)) {
                settingsStore.markRemoteSyncDirty()
                settingsStore.markRemoteSyncConflict(remote.revision)
                return RemoteSyncResult.Conflict(remote.revision)
            }
        }

        if (remoteRevision > localRevision) {
            if (settings.remoteSyncDirty) {
                settingsStore.markRemoteSyncConflict(remoteRevision)
                return RemoteSyncResult.Conflict(remoteRevision)
            }
            val downloaded = downloadAccountSnapshot(serverUrl, accessToken, settings.remoteSyncSecret)
            val snapshot = downloaded.snapshot
            app.database.withTransaction {
                if (account != accountGeneration || !settingsStore.hasLocalAccountAccess()) return@withTransaction
                val current = app.repository.exportSnapshot(settingsStore.currentSettings())
                if (generation != settingsStore.remoteChangeGeneration() || BackupArchiveCodec.contentFingerprint(current) != BackupArchiveCodec.contentFingerprint(localBefore)) {
                    settingsStore.markRemoteSyncDirty()
                    settingsStore.markRemoteSyncConflict(downloaded.revision)
                    return@withTransaction
                }
                saveSafetyCopy(current, "before-cloud")
                applyRemoteSnapshot(snapshot)
                val applied = app.repository.exportSnapshot(settingsStore.currentSettings())
                settingsStore.markRemoteSyncSuccess(downloaded.revision, generation, BackupArchiveCodec.dataFingerprint(applied))
            }
            if (account != accountGeneration || !settingsStore.hasLocalAccountAccess()) return RemoteSyncResult.NotConfigured
            if (settingsStore.currentSettings().remoteSyncDirty) return RemoteSyncResult.Conflict(remoteRevision)
            return RemoteSyncResult.Downloaded(downloaded.revision)
        }
        if (remoteRevision < localRevision) {
            settingsStore.markRemoteSyncConflict(remoteRevision)
            return RemoteSyncResult.Failed("云端版本低于本机记录，已停止同步以避免覆盖")
        }
        if (!settings.remoteSyncDirty) {
            settingsStore.markRemoteSyncSuccess(localRevision, generation, fingerprint)
            return RemoteSyncResult.UpToDate(localRevision)
        }

        val snapshot = localBefore
        val encrypted = RemoteSyncCrypto.encrypt(BackupArchiveCodec.encode(snapshot), settings.remoteSyncSecret)
        val upload = uploadAccountSnapshot(serverUrl, accessToken, localRevision, encrypted)
        if (upload.conflictRevision != null) {
            settingsStore.markRemoteSyncConflict(upload.conflictRevision)
            return RemoteSyncResult.Conflict(upload.conflictRevision)
        }
        if (account == accountGeneration && settingsStore.hasLocalAccountAccess()) {
            settingsStore.markRemoteSyncSuccess(upload.revision, generation, fingerprint)
        }
        return RemoteSyncResult.Uploaded(snapshot.exportedAtMillis, upload.revision)
    }

    suspend fun downloadLatest(): BackupSnapshot? = syncMutex.withLock {
        val settings = settingsStore.currentSettings()
        if (!settings.remoteSyncEnabled) return@withLock null
        if (settings.remoteSyncUsername.isNotBlank() && settingsStore.hasValidRemoteSession()) {
            val serverUrl = cloudServerUrl()
            val token = ensureAccessToken(serverUrl, settings)
            return@withLock runCatching { downloadAccountSnapshot(serverUrl, token, settings.remoteSyncSecret).snapshot }
                .getOrElse { error ->
                    if (error is RemoteNotFoundException) null else throw error
                }
        }
        return@withLock null
    }

    suspend fun inspectCloud(): CloudReview = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val settings = settingsStore.currentSettings()
            val account = accountGeneration
            check(settingsStore.hasValidRemoteSession()) { "请先重新登录账号" }
            val token = ensureAccessToken(cloudServerUrl(), settings)
            val remote = downloadAccountSnapshot(cloudServerUrl(), token, settings.remoteSyncSecret)
            val local = app.database.withTransaction { app.repository.exportSnapshot(settingsStore.currentSettings()) }
            check(account == accountGeneration && settingsStore.hasLocalAccountAccess()) { "账号已改变，请重新检查" }
            CloudReview(remote.snapshot, remote.revision, BackupArchiveCodec.contentFingerprint(local), account)
        }
    }

    suspend fun resolveCloud(review: CloudReview, keepLocal: Boolean): String = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val settings = settingsStore.currentSettings()
            check(settingsStore.hasValidRemoteSession()) { "请先重新登录账号" }
            val account = accountGeneration
            check(review.accountGeneration == account) { "账号已改变，请重新检查" }
            val token = ensureAccessToken(cloudServerUrl(), settings)
            val revision = getJson(cloudServerUrl(), "/v2/sync/status", token).getLong("revision")
            require(revision == review.revision) { "云端又有更新，请重新检查后确认" }
            check(account == accountGeneration && settingsStore.hasLocalAccountAccess()) { "账号已改变，请重新检查" }
            saveSafetyCopy(review.snapshot, "cloud-v${review.revision}")
            val generation = settingsStore.remoteChangeGeneration()
            val local = app.database.withTransaction {
                val current = app.repository.exportSnapshot(settingsStore.currentSettings())
                require(BackupArchiveCodec.contentFingerprint(current) == review.localFingerprint) { "本机已有新修改，请重新检查后确认" }
                saveSafetyCopy(current, "local-before-conflict")
                if (!keepLocal) {
                    check(account == accountGeneration && settingsStore.hasLocalAccountAccess()) { "账号已退出" }
                    applyRemoteSnapshot(review.snapshot)
                    val applied = app.repository.exportSnapshot(settingsStore.currentSettings())
                    settingsStore.markRemoteSyncSuccess(review.revision, generation, BackupArchiveCodec.dataFingerprint(applied))
                }
                current
            }
            if (keepLocal) {
                val encrypted = RemoteSyncCrypto.encrypt(BackupArchiveCodec.encode(local), settings.remoteSyncSecret)
                val result = uploadAccountSnapshot(cloudServerUrl(), token, review.revision, encrypted)
                if (result.conflictRevision != null) {
                    settingsStore.markRemoteSyncConflict(result.conflictRevision)
                    error("云端又有更新，上传已停止；请重新检查")
                }
                if (account == accountGeneration && settingsStore.hasLocalAccountAccess()) {
                    settingsStore.markRemoteSyncSuccess(result.revision, generation, BackupArchiveCodec.dataFingerprint(local))
                    if (settingsStore.currentSettings().remoteSyncDirty) requestLocalSync()
                }
            }
            "${if (keepLocal) "本机副本已上传" else "已恢复云端副本"}；双方恢复副本已保存在本机"
        }
    }

    private fun saveSafetyCopy(snapshot: BackupSnapshot, reason: String): String {
        val directory = safetyDirectory().apply { check(isDirectory || mkdirs()) { "无法创建恢复副本目录" } }
        val file = File(directory, "${System.currentTimeMillis()}-$reason-${UUID.randomUUID()}.paykibak")
        file.outputStream().use { BackupArchiveCodec.write(snapshot, it) }
        return file.absolutePath
    }

    private fun safetyDirectory(): File {
        val identity = settingsStore.currentSettings().remoteSyncUsername
        val key = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(app.filesDir, "sync-safety/$key")
    }

    suspend fun restoreNewestSafetyCopy(): Unit = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            check(settingsStore.hasLocalAccountAccess()) { "请先登录账号" }
            val file = safetyDirectory().listFiles()?.filter { it.name.contains("local-before") || it.name.contains("before-cloud") || it.name.contains("before-safety-restore") }
                ?.maxByOrNull { it.lastModified() } ?: error("尚无操作前恢复副本")
            val snapshot = file.inputStream().use(BackupArchiveCodec::read)
            app.database.withTransaction {
                saveSafetyCopy(app.repository.exportSnapshot(settingsStore.currentSettings()), "before-safety-restore")
                applyRemoteSnapshot(snapshot)
                settingsStore.markRemoteSyncDirty()
            }
            onLocalDataChanged()
        }
    }

    private suspend fun applyRemoteSnapshot(snapshot: BackupSnapshot) {
        val current = settingsStore.currentSettings()
        app.repository.getAllTodos().forEach { existing ->
            app.alarmScheduler.cancel(existing.id)
            ReminderDispatchTracker.clear(app, existing.id)
        }
        app.repository.importSnapshot(snapshot, notify = false)
        settingsStore.replaceAll(snapshot.settings.withRemoteAccountFrom(current))
        app.repository.ensureRecurringInstancesAhead()
        app.repository.futureReminderItems(System.currentTimeMillis()).forEach(app.alarmScheduler::schedule)
        TodoWidgetProvider.notifyWidgetDataChanged(app)
    }

    private fun AppSettings.withRemoteAccountFrom(current: AppSettings): AppSettings = copy(
        backupDirectoryUri = current.backupDirectoryUri,
        autoBackupEnabled = current.autoBackupEnabled,
        remoteSyncEnabled = current.remoteSyncEnabled,
        remoteSyncServerUrl = current.remoteSyncServerUrl,
        remoteSyncDeviceId = current.remoteSyncDeviceId,
        remoteSyncSecret = current.remoteSyncSecret,
        remoteSyncUsername = current.remoteSyncUsername,
        remoteSyncUserId = current.remoteSyncUserId,
        remoteSyncDisplayName = current.remoteSyncDisplayName,
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
        val account = accountGeneration
        val now = System.currentTimeMillis() / 1000L
        if (settings.remoteSyncRefreshExpiresAt <= now) {
            settingsStore.clearExpiredRemoteSessionIfNeeded()
            throw IOException("登录已过期，请重新登录")
        }
        if (settings.remoteSyncAccessToken.isNotBlank() && settings.remoteSyncAccessExpiresAt > now + 30L) {
            return settings.remoteSyncAccessToken
        }
        require(settings.remoteSyncRefreshToken.isNotBlank()) { "登录已过期，请重新登录" }
        val response = try { postJson(
            serverUrl,
            "/v2/auth/refresh",
            JSONObject().put("refreshToken", settings.remoteSyncRefreshToken)
        ) } catch (error: UnauthorizedException) {
            if (account == accountGeneration && settingsStore.currentSettings().remoteSyncRefreshToken == settings.remoteSyncRefreshToken)
                settingsStore.clearExpiredRemoteSessionIfNeeded(Long.MAX_VALUE)
            throw error
        }
        check(account == accountGeneration && settingsStore.hasLocalAccountAccess()) { "账号已退出" }
        settingsStore.updateRemoteAccountTokens(
            accessToken = response.getString("accessToken"),
            refreshToken = response.getString("refreshToken"),
            accessExpiresAt = response.getLong("accessExpiresAt"),
            refreshExpiresAt = response.getLong("refreshExpiresAt")
        )
        return response.getString("accessToken")
    }

    private fun downloadAccountSnapshot(serverUrl: String, accessToken: String, secret: String): Download {
        val connection = openApiConnection(serverUrl, "/v2/sync/snapshot", "GET", accessToken)
        return try {
            when (val status = connection.responseCode) {
                HttpURLConnection.HTTP_NOT_FOUND -> throw RemoteNotFoundException()
                in 200..299 -> {
                    val encrypted = connection.inputStream.use { it.readBytesLimited(MAX_PAYLOAD_BYTES) }
                    val revision = connection.getHeaderField("X-Payki-Revision")?.toLongOrNull()
                        ?: throw IOException("云端未返回数据版本，请重试")
                    Download(BackupArchiveCodec.decode(RemoteSyncCrypto.decrypt(encrypted, secret)), revision)
                }
                else -> { invalidateRejectedToken(status, accessToken); throw apiException(connection, status) }
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
            invalidateRejectedToken(status, accessToken)
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
            invalidateRejectedToken(status, accessToken)
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
            invalidateRejectedToken(status, accessToken)
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

    private fun invalidateRejectedToken(status: Int, token: String) {
        if (status == HttpURLConnection.HTTP_UNAUTHORIZED && token.isNotBlank() &&
            token == settingsStore.currentSettings().remoteSyncAccessToken)
            settingsStore.clearExpiredRemoteSessionIfNeeded(Long.MAX_VALUE)
    }

    private fun cloudServerUrl(): String {
        return normalizeServerUrl(serverBaseUrl)
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
        if (status == HttpURLConnection.HTTP_UNAUTHORIZED) return UnauthorizedException(json.optString("message").ifBlank { "登录验证失败，请重新登录" })
        return IOException(json.optString("message").ifBlank { "云服务请求失败（HTTP $status）" })
    }

    private class UnauthorizedException(message: String) : IOException(message)

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
