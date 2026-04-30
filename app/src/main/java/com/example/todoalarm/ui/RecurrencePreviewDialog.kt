package com.example.todoalarm.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.RecurrencePreviewResult

@Composable
internal fun RecurrencePreviewDialog(
    title: String,
    preview: RecurrencePreviewResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("共将生成 ${preview.totalCount} 条实例，以下预览前 ${preview.occurrences.size} 条。")
                preview.occurrences.forEach { occurrence ->
                    Text(
                        text = buildString {
                            append(formatLocalDateTime(occurrence.startAt))
                            occurrence.endAt?.let {
                                append(" - ")
                                append(formatLocalDateTime(it))
                            }
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        dismissButton = {}
    )
}
