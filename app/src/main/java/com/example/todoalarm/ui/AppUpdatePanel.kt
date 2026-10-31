package com.example.todoalarm.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.todoalarm.BuildConfig
import com.example.todoalarm.update.AppUpdater
import com.example.todoalarm.update.GithubRelease
import com.example.todoalarm.update.UpdateStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppUpdatePanel() {
    val context = LocalContext.current
    val updater = remember(context) { AppUpdater.get(context) }
    val state by updater.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.downloadId) { do { updater.refresh(); if (state.downloadId < 0 || state.ready) break; delay(1500) } while (true) }
    AppUpdateContent(state, BuildConfig.DEBUG, onCheck = { scope.launch { updater.check(true) } },
        onPage = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.release?.page ?: GithubRelease.PAGE))) },
        onInstall = { scope.launch { updater.install() } }, onCancel = { scope.launch { updater.cancel() } }, onDownload = { scope.launch { updater.download() } })
}

@Composable
internal fun AppUpdateContent(state: UpdateStatus, debug: Boolean, onCheck: () -> Unit, onPage: () -> Unit, onInstall: () -> Unit, onCancel: () -> Unit, onDownload: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(state.message, style = MaterialTheme.typography.bodyMedium)
        state.release?.let { Text(it.notes.take(3000), style = MaterialTheme.typography.bodySmall) }
        if (debug) Text("当前为 Debug 签名；公开 Release 不能直接覆盖。迁移前请按说明导出数据。", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(enabled = !state.busy, onClick = onCheck) { Text("检查更新") }
        state.release?.let { release ->
            OutlinedButton(onClick = onPage) { Text("查看发布页面") }
            if (state.downloadId < 0) OutlinedButton(enabled = !debug && release.apk != null, onClick = onDownload) { Text("下载安装包") }
        }
        if (state.ready) Button(onClick = onInstall) { Text("安装更新") }
        else if (state.downloadId >= 0) OutlinedButton(onClick = onCancel) { Text("取消下载（${state.progress}%）") }
        if (state.release == null) TextButton(onClick = onPage) { Text("打开 GitHub 发布页面") }
    }
}
