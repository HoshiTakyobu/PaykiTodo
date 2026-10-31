package com.example.todoalarm.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class UpdateDownloadRecoveryTest {
    @Test fun systemDownloadIdSurvivesNewCoordinatorAndCancelRemovesOnlyThatDownload() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
        val original = prefs.all.toMap()
        val manager = context.getSystemService(DownloadManager::class.java)
        val file = "PaykiTodo-1.17.12-${System.currentTimeMillis()}.apk"
        val id = manager.enqueue(DownloadManager.Request(Uri.parse("https://github.com/${GithubRelease.REPO}/releases/download/v1.17.12/PaykiTodo-1.17.12-release.apk"))
            .setAllowedNetworkTypes(0).setDestinationInExternalFilesDir(context, "updates", file))
        try {
            val target = AppRelease("1.17.12", "https://github.com/${GithubRelease.REPO}/releases/tag/v1.17.12", "fixture", "https://github.com/${GithubRelease.REPO}/releases/download/v1.17.12/PaykiTodo-1.17.12-release.apk", 1, null)
            prefs.edit().putLong("download", id).putString("file", file).putString("target", target.json()).commit()
            val recreated = AppUpdater(context)
            assertEquals(id, recreated.state.value.downloadId)
            recreated.refresh()
            assertEquals(id, recreated.state.value.downloadId); assertFalse(recreated.state.value.ready)
            recreated.cancel(); assertEquals(-1L, recreated.state.value.downloadId)
            manager.query(DownloadManager.Query().setFilterById(id)).use { assertFalse(it.moveToFirst()) }
        } finally {
            manager.remove(id)
            val restore = prefs.edit().clear()
            original.forEach { (key, value) -> when (value) { is String -> restore.putString(key, value); is Long -> restore.putLong(key, value); is Int -> restore.putInt(key, value); is Boolean -> restore.putBoolean(key, value) } }
            restore.commit()
        }
    }
}
