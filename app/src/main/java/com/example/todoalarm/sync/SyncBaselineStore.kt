package com.example.todoalarm.sync

import android.util.AtomicFile
import android.util.Base64
import com.example.todoalarm.data.BackupArchiveCodec
import com.example.todoalarm.data.BackupSnapshot
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Account/server-bound acknowledged content. A failed/mismatched read never grants overwrite permission. */
internal class SyncBaselineStore(private val directory: File) {
    private var cachedKey = ""
    private var cachedRevision = -1L
    private var cached: BackupSnapshot? = null
    private fun key(server: String, username: String) = MessageDigest.getInstance("SHA-256")
        .digest("${server.trimEnd('/')}\n$username".toByteArray()).joinToString("") { "%02x".format(it) }

    fun read(server: String, username: String, secret: String, revision: Long): BackupSnapshot? {
        val identity = key(server, username)
        if (cachedKey == identity && cachedRevision == revision) return cached
        return runCatching {
            val json = JSONObject(AtomicFile(File(directory, "$identity.bin")).readFully().toString(Charsets.UTF_8))
            require(json.getLong("revision") == revision)
            BackupArchiveCodec.decode(RemoteSyncCrypto.decrypt(Base64.decode(json.getString("payload"), Base64.NO_WRAP), secret))
        }.getOrNull()?.also { cachedKey = identity; cachedRevision = revision; cached = it }
    }

    fun write(server: String, username: String, secret: String, revision: Long, snapshot: BackupSnapshot) {
        val identity = key(server, username)
        check(directory.isDirectory || directory.mkdirs())
        val json = JSONObject().put("revision", revision).put("payload", Base64.encodeToString(
            RemoteSyncCrypto.encrypt(BackupArchiveCodec.encode(snapshot), secret), Base64.NO_WRAP))
        val file = AtomicFile(File(directory, "$identity.bin"))
        val stream = file.startWrite()
        try { stream.write(json.toString().toByteArray()); file.finishWrite(stream) }
        catch (error: Exception) { file.failWrite(stream); throw error }
        cachedKey = identity; cachedRevision = revision; cached = snapshot
    }
}
