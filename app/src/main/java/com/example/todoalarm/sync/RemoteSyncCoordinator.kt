package com.example.todoalarm.sync

import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.BackupArchiveCodec
import com.example.todoalarm.data.BackupSnapshot
import com.example.todoalarm.data.AppSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.spec.KeySpec
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class RemoteSyncCoordinator(
    private val app: TodoApplication,
    private val settingsStore: AppSettingsStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private var pendingJob: Job? = null
    private var periodicJob: Job? = null

    fun start() {
        if (!settingsStore.currentSettings().remoteSyncEnabled) return
        if (periodicJob?.isActive == true) return
        periodicJob = scope.launch {
            while (isActive && settingsStore.currentSettings().remoteSyncEnabled) {
                runCatching { syncNow() }
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
        if (!settingsStore.currentSettings().remoteSyncEnabled) {
            pendingJob?.cancel()
            pendingJob = null
            return
        }
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(1_000L)
            runCatching { syncNow() }
        }
    }

    suspend fun syncNow(): RemoteSyncResult = syncMutex.withLock {
        val settings = settingsStore.currentSettings()
        if (!settings.remoteSyncEnabled) return@withLock RemoteSyncResult.Disabled
        val serverUrl = settings.remoteSyncServerUrl.trim().trimEnd('/')
        if (serverUrl.isBlank()) return@withLock RemoteSyncResult.NotConfigured
        val credentials = settingsStore.remoteSyncCredentials()
        val snapshot = app.repository.exportSnapshot(settings)
        val encrypted = RemoteSyncCrypto.encrypt(BackupArchiveCodec.encode(snapshot), credentials.second)
        upload(serverUrl, credentials.first, credentials.second, encrypted)
        RemoteSyncResult.Uploaded(snapshot.exportedAtMillis)
    }

    suspend fun downloadLatest(): BackupSnapshot? = syncMutex.withLock {
        val settings = settingsStore.currentSettings()
        if (!settings.remoteSyncEnabled) return@withLock null
        val serverUrl = settings.remoteSyncServerUrl.trim().trimEnd('/')
        if (serverUrl.isBlank()) return@withLock null
        val credentials = settingsStore.remoteSyncCredentials()
        val encrypted = download(serverUrl, credentials.first, credentials.second) ?: return@withLock null
        BackupArchiveCodec.decode(RemoteSyncCrypto.decrypt(encrypted, credentials.second))
    }

    private fun upload(serverUrl: String, deviceId: String, secret: String, payload: ByteArray) {
        val connection = openConnection(serverUrl, deviceId, secret, "PUT")
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

    private fun download(serverUrl: String, deviceId: String, secret: String): ByteArray? {
        val connection = openConnection(serverUrl, deviceId, secret, "GET")
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

    private fun openConnection(serverUrl: String, deviceId: String, secret: String, method: String): HttpURLConnection {
        require(serverUrl.startsWith("https://", ignoreCase = true)) { "远程同步必须使用 HTTPS 地址" }
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
        data class Uploaded(val exportedAtMillis: Long) : RemoteSyncResult
    }

    companion object {
        private const val MAX_PAYLOAD_BYTES = 32 * 1024 * 1024
    }
}

private object RemoteSyncCrypto {
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
            throw IllegalArgumentException("远程同步密钥不匹配或数据已损坏", error)
        }
    }

    private fun deriveKey(secret: String): SecretKeySpec {
        val spec: KeySpec = PBEKeySpec(secret.toCharArray(), salt, 120_000, 256)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(key, "AES")
    }
}
