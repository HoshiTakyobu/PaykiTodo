package com.example.todoalarm.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.todoalarm.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateStatus(val release: AppRelease? = null, val message: String = "可检查 GitHub 稳定版本。", val busy: Boolean = false,
    val downloadId: Long = -1, val progress: Int = 0, val ready: Boolean = false)

class AppUpdater internal constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
    private val downloads = context.getSystemService(DownloadManager::class.java)
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(UpdateStatus(release = GithubRelease.restore(prefs.getString("latest", "") ?: "")?.takeIf { GithubRelease.compare(it.version, BuildConfig.VERSION_NAME) > 0 }, downloadId = prefs.getLong("download", -1)))
    val state = mutable.asStateFlow()
    suspend fun check(manual: Boolean) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            if (!manual && now - prefs.getLong("checked", 0) < 12 * 3600_000L) return@withLock
            prefs.edit().putLong("checked", now).apply(); mutable.value = mutable.value.copy(busy = true, message = "正在检查更新…")
            try {
                val connection = URL(GithubRelease.API).openConnection() as HttpURLConnection
                val text = try {
                    connection.connectTimeout = 10000; connection.readTimeout = 15000
                    connection.setRequestProperty("User-Agent", "PaykiTodo-Android-Updater")
                    connection.setRequestProperty("Accept", "application/vnd.github+json")
                    if (connection.responseCode == 403 || connection.responseCode == 429) error("GitHub 暂时限制请求，请稍后重试")
                    check(connection.responseCode == 200) { "GitHub 返回 ${connection.responseCode}" }
                    connection.inputStream.use { stream ->
                        val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
                        while (true) { val n = stream.read(buffer); if (n < 0) break; require(output.size() + n <= 4_000_000) { "更新响应过大" }; output.write(buffer, 0, n) }
                        output.toString("UTF-8")
                    }
                } finally { connection.disconnect() }
                val release = GithubRelease.parse(text, BuildConfig.VERSION_NAME)
                prefs.edit().putString("latest", release?.json() ?: "").apply()
                mutable.value = mutable.value.copy(release = release, message = release?.let { "发现 ${it.version}" } ?: "当前没有更新的稳定版本。", busy = false)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { mutable.value = mutable.value.copy(busy = false); throw cancelled }
            catch (error: Exception) { mutable.value = mutable.value.copy(busy = false, message = "检查失败：${if (error is java.io.IOException) "网络暂不可用，请重试" else error.message}") }
        }
    }
    suspend fun download() = withContext(Dispatchers.IO) { mutex.withLock {
        val release = mutable.value.release ?: return@withLock
        if (BuildConfig.DEBUG) { mutable.value = mutable.value.copy(message = "当前是 Debug 签名，不能覆盖安装公开 Release。请查看发布页面和迁移说明。"); return@withLock }
        val url = release.apk ?: run { mutable.value = mutable.value.copy(message = "此发布没有手机安装包，请查看发布页面。"); return@withLock }
        if (mutable.value.downloadId >= 0) return@withLock
        try {
            val name = "PaykiTodo-${release.version}-${System.currentTimeMillis()}.apk"
            val id = downloads.enqueue(DownloadManager.Request(Uri.parse(url)).setTitle("PaykiTodo ${release.version}").setDescription("下载应用更新")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setDestinationInExternalFilesDir(context, "updates", name))
            prefs.edit().putLong("download", id).putString("file", name).putString("target", release.json()).commit()
            mutable.value = mutable.value.copy(downloadId = id, progress = 0, ready = false, message = "正在下载…")
        } catch (error: Exception) { mutable.value = mutable.value.copy(message = "下载无法开始，请重试或查看发布页面。") }
    } }
    suspend fun refresh() = withContext(Dispatchers.IO) { mutex.withLock {
        val id = prefs.getLong("download", -1); if (id < 0) return@withLock
        downloads.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (!cursor.moveToFirst()) { resetDownload("下载记录不存在，可重新下载。"); return@withLock }
            when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    try { validateDownloaded(); mutable.value = mutable.value.copy(downloadId = id, progress = 100, ready = true, message = "安装包已校验，可交给系统安装。") }
                    catch (error: Exception) { resetDownload("安装包校验失败：${error.message}。请重新下载。") }
                }
                DownloadManager.STATUS_FAILED -> resetDownload("下载失败（${cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))}），可重试。")
                else -> {
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)); val done = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    mutable.value = mutable.value.copy(downloadId = id, progress = if (total > 0) (done * 100 / total).toInt().coerceIn(0, 100) else 0, ready = false, message = "下载中，${if (total > 0) done * 100 / total else 0}%")
                }
            }
        }
    } }
    suspend fun cancel() = withContext(Dispatchers.IO) { mutex.withLock { val id = prefs.getLong("download", -1); if (id >= 0) downloads.remove(id); resetDownload("下载已取消，可重新下载。") } }
    private fun resetDownload(message: String) { prefs.edit().remove("download").remove("file").remove("target").apply(); mutable.value = mutable.value.copy(downloadId = -1, ready = false, message = message) }
    private fun apkFile(): File {
        val name = prefs.getString("file", "") ?: ""
        require(name.matches(Regex("PaykiTodo-\\d+\\.\\d+\\.\\d+-\\d+\\.apk"))) { "下载路径无效" }
        return File(context.getExternalFilesDir("updates"), name)
    }
    @Suppress("DEPRECATION")
    private fun validateDownloaded(): File {
        val target = GithubRelease.restore(prefs.getString("target", "") ?: "") ?: error("缺少安装包版本")
        val file = apkFile(); require(file.isFile && file.length() == target.size) { "安装包大小不匹配" }
        if (target.digest != null) {
            val hash = MessageDigest.getInstance("SHA-256"); file.inputStream().use { stream -> val buffer = ByteArray(65536); while (true) { val n = stream.read(buffer); if (n < 0) break; hash.update(buffer, 0, n) } }
            require(hash.digest().joinToString("") { "%02x".format(it) }.equals(target.digest, true)) { "安装包哈希不匹配" }
        }
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags) ?: error("无效的 APK")
        ApkUpdateIdentity.validate(installed, archive, context.packageName, target.version)
        return file
    }
    suspend fun install() {
        try {
            val file = withContext(Dispatchers.IO) { mutex.withLock { validateDownloaded() } }
            if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                mutable.value = mutable.value.copy(message = "允许本应用安装后，返回并再次点击安装。"); return
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (error: Exception) { mutable.value = mutable.value.copy(message = "无法安装：${error.message}") }
    }
    companion object {
        @Volatile private var instance: AppUpdater? = null
        fun get(context: Context): AppUpdater = instance ?: synchronized(this) { instance ?: AppUpdater(context.applicationContext).also { instance = it } }
    }
}
