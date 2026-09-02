package com.example.todoalarm.data

import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal object BackupArchiveCodec {
    const val MIME_TYPE = "application/vnd.paykitodo.backup+zip"
    const val FILE_EXTENSION = "paykibak"

    private const val ARCHIVE_FORMAT = "PaykiTodoBackupArchive"
    private const val ARCHIVE_VERSION = 1
    private const val MANIFEST_ENTRY = "manifest.json"
    private const val SNAPSHOT_ENTRY = "backup.json"
    private const val MAX_ARCHIVE_ENTRY_COUNT = 8
    private const val MAX_ENTRY_BYTES = 32 * 1024 * 1024

    fun write(snapshot: BackupSnapshot, output: OutputStream) {
        val snapshotBytes = snapshot.toJson().toString(2).toByteArray(StandardCharsets.UTF_8)
        val manifestBytes = JSONObject().apply {
            put("format", ARCHIVE_FORMAT)
            put("archiveVersion", ARCHIVE_VERSION)
            put("snapshotVersion", snapshot.snapshotVersion)
            put("exportedAtMillis", snapshot.exportedAtMillis)
            put("payload", SNAPSHOT_ENTRY)
            put("payloadSha256", snapshotBytes.sha256())
            put("containsSecrets", false)
        }.toString(2).toByteArray(StandardCharsets.UTF_8)

        ZipOutputStream(output.buffered()).use { zip ->
            zip.writeEntry(MANIFEST_ENTRY, manifestBytes, snapshot.exportedAtMillis)
            zip.writeEntry(SNAPSHOT_ENTRY, snapshotBytes, snapshot.exportedAtMillis)
        }
    }

    fun read(input: InputStream): BackupSnapshot {
        val buffered = BufferedInputStream(input)
        buffered.mark(4)
        val prefix = ByteArray(4)
        val prefixLength = buffered.read(prefix)
        buffered.reset()
        return if (prefixLength >= 2 && prefix[0] == 'P'.code.toByte() && prefix[1] == 'K'.code.toByte()) {
            readArchive(buffered)
        } else {
            val jsonBytes = buffered.readLimited(MAX_ENTRY_BYTES)
            backupSnapshotFromJson(JSONObject(jsonBytes.toString(StandardCharsets.UTF_8).trimStart('\uFEFF')))
        }
    }

    internal fun encode(snapshot: BackupSnapshot): ByteArray {
        return ByteArrayOutputStream().use { output ->
            write(snapshot, output)
            output.toByteArray()
        }
    }

    internal fun decode(bytes: ByteArray): BackupSnapshot {
        return read(ByteArrayInputStream(bytes))
    }

    internal fun contentFingerprint(snapshot: BackupSnapshot): String {
        val stableSnapshot = snapshot.copy(exportedAtMillis = 0L)
        return stableSnapshot.toJson().toString().toByteArray(StandardCharsets.UTF_8).sha256()
    }

    private fun readArchive(input: InputStream): BackupSnapshot {
        var manifestBytes: ByteArray? = null
        var snapshotBytes: ByteArray? = null
        var entryCount = 0
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                entryCount += 1
                require(entryCount <= MAX_ARCHIVE_ENTRY_COUNT) { "备份包包含过多文件" }
                when (entry.name) {
                    MANIFEST_ENTRY -> {
                        require(manifestBytes == null) { "备份包清单重复" }
                        manifestBytes = zip.readLimited(MAX_ENTRY_BYTES)
                    }
                    SNAPSHOT_ENTRY -> {
                        require(snapshotBytes == null) { "备份包数据重复" }
                        snapshotBytes = zip.readLimited(MAX_ENTRY_BYTES)
                    }
                }
                zip.closeEntry()
            }
        }

        val manifest = manifestBytes?.let { JSONObject(it.toString(StandardCharsets.UTF_8)) }
            ?: error("备份包缺少 manifest.json")
        require(manifest.optString("format") == ARCHIVE_FORMAT) { "这不是 PaykiTodo 迁移包" }
        val archiveVersion = manifest.optInt("archiveVersion", 0)
        require(archiveVersion in 1..ARCHIVE_VERSION) { "备份包版本过新，请先更新 PaykiTodo" }
        require(manifest.optString("payload") == SNAPSHOT_ENTRY) { "备份包数据入口无效" }

        val payload = snapshotBytes ?: error("备份包缺少 backup.json")
        val expectedHash = manifest.optString("payloadSha256").lowercase(Locale.ROOT)
        require(expectedHash.isNotBlank() && expectedHash == payload.sha256()) { "备份包校验失败，文件可能不完整" }
        return backupSnapshotFromJson(JSONObject(payload.toString(StandardCharsets.UTF_8)))
    }

    private fun ZipOutputStream.writeEntry(name: String, bytes: ByteArray, timestamp: Long) {
        putNextEntry(ZipEntry(name).apply { time = timestamp })
        write(bytes)
        closeEntry()
    }

    private fun InputStream.readLimited(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
            require(output.size() <= limit) { "备份文件过大" }
        }
        return output.toByteArray()
    }

    private fun ByteArray.sha256(): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
