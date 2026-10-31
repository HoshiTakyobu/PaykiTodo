package com.example.todoalarm.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.todoalarm.data.PlanningNote

/** Existing material remains readable without restoring the document-creation workflow. */
@Composable internal fun LegacyPlanningArchiveDialog(
    notes: List<PlanningNote>, exportOutline: suspend (Long) -> String, onDismiss: () -> Unit
) {
    var chosen by remember { mutableStateOf<PlanningNote?>(null) }
    var outline by remember { mutableStateOf("") }
    LaunchedEffect(chosen?.id) {
        outline = chosen?.let { runCatching { exportOutline(it.id) }.getOrDefault("") }.orEmpty()
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(chosen?.title ?: "旧规划原文") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            val note = chosen
            if (note == null) {
                if (notes.isEmpty()) Text("没有旧规划文档")
                notes.forEach { old -> TextButton(onClick = { chosen = old }) { Text(old.title) } }
            } else {
                Text("原文保留在数据与迁移包中。新规划从每日看板输入。")
                Text(note.contentMarkdown)
                if (outline.isNotBlank() && outline != note.contentMarkdown) Text(outline)
            }
        } }, confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        dismissButton = { if (chosen != null) TextButton(onClick = { chosen = null }) { Text("返回列表") } })
}
