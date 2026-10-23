package com.example.todoalarm.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.todoalarm.ui.theme.resolveMainFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun AppearanceSettings(onDismiss: () -> Unit) {
    val workspace = LocalWorkspacePreferences.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var font by remember { mutableStateOf(workspace.settings.mainFont) }
    var path by remember { mutableStateOf(workspace.settings.customFontPath) }
    var days by remember { mutableStateOf(workspace.settings.countdownPastDays.toString()) }
    var message by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            importing = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytesWithLimit(16 * 1024 * 1024) } ?: error("无法读取字体")
                    val directory = java.io.File(context.filesDir, "fonts").apply { mkdirs() }
                    val file = java.io.File(directory, "custom-${java.util.UUID.randomUUID()}.ttf")
                    file.writeBytes(bytes); android.graphics.Typeface.createFromFile(file)
                    file.absolutePath
                }
            }.onSuccess { path = it; font = "CUSTOM"; message = "字体已载入，点击完成后应用。自定义字体文件仅保存在本机。" }
                .onFailure { message = "字体导入失败：${it.message}" }
            importing = false
        }
    }
    SettingsSectionDialog("外观与显示", onDismiss, onDone = {
        workspace.display(font, path, days.toInt().coerceIn(0, 365), workspace.settings.hiddenCalendarIds); onDismiss()
    }, doneEnabled = !importing && days.toIntOrNull() in 0..365, onCancel = onDismiss) {
        Text("主字体", style = MaterialTheme.typography.titleMedium)
        PreferenceChoice("字体", font, listOf("SYSTEM" to "系统默认", "SARASA" to "更纱黑体", "SERIF" to "系统衬线", "MONOSPACE" to "系统等宽") + if (path.isNotBlank()) listOf("CUSTOM" to "自定义字体") else emptyList()) { font = it }
        TextButton(enabled = !importing, onClick = { picker.launch(arrayOf("font/*", "application/x-font-ttf", "application/octet-stream")) }) { Text(if (importing) "正在载入…" else "导入 TTF / OTF 字体") }
        val previewFont = remember(font, path) { resolveMainFont(font, path) }
        Surface(color = MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.medium) {
            Text("把今天安排得刚刚好\nPaykiTodo · 09:30 · DDL", Modifier.fillMaxWidth().padding(20.dp), fontFamily = previewFont, style = MaterialTheme.typography.titleMedium)
        }
        Text("问候语保留原来的字体。", style = MaterialTheme.typography.bodySmall)
        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text("值得期待", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(days, { days = it.filter(Char::isDigit).take(3) }, label = { Text("历史目标保留天数（0–365）") }, singleLine = true,
            isError = days.toIntOrNull() !in 0..365, modifier = Modifier.fillMaxWidth())
        Text("默认保留过去7天内的目标。完成时可以选择“保留纪念”，之后始终展示。", style = MaterialTheme.typography.bodySmall)
    }
}

private fun java.io.InputStream.readBytesWithLimit(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
    while (true) { val size = read(buffer); if (size < 0) break; require(output.size() + size <= limit) { "字体文件不能超过16 MB" }; output.write(buffer, 0, size) }
    return output.toByteArray()
}
