package com.example.todoalarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.todoalarm.data.ActivityRecord
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.effectiveDurationMillis
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DetailFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)

/** 开始记录对话框：标题可空、可选分组。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StartActivityTimerDialog(
    groups: List<TaskGroup>,
    onDismiss: () -> Unit,
    onStart: (title: String, groupId: Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("开始记录") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("在做什么？（可留空）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (groups.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("分组（可选）")
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        groups.forEach { group ->
                            FilterChip(
                                selected = selectedGroupId == group.id,
                                onClick = {
                                    selectedGroupId = if (selectedGroupId == group.id) null else group.id
                                },
                                label = { Text(group.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onStart(title.trim(), selectedGroupId) }) {
                Text("开始")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 随手补记对话框：标题 + 时长（分钟）+ 可选分组。开始时间默认现在往前推时长。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ManualActivityRecordDialog(
    groups: List<TaskGroup>,
    onDismiss: () -> Unit,
    onSave: (title: String, startAtMillis: Long, durationMinutes: Int, groupId: Long?, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }

    val durationMinutes = durationText.toIntOrNull() ?: 0
    val canSave = title.isNotBlank() && durationMinutes > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("补记一条") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("做了什么") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("用时（分钟）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (groups.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("分组（可选）")
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        groups.forEach { group ->
                            FilterChip(
                                selected = selectedGroupId == group.id,
                                onClick = {
                                    selectedGroupId = if (selectedGroupId == group.id) null else group.id
                                },
                                label = { Text(group.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val now = System.currentTimeMillis()
                    val start = now - durationMinutes * 60_000L
                    onSave(title.trim(), start, durationMinutes, selectedGroupId, notes.trim())
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 记录详情对话框：查看 + 删除。 */
@Composable
internal fun ActivityRecordDetailDialog(
    record: ActivityRecord,
    group: TaskGroup?,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val start = Instant.ofEpochMilli(record.startAtMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val end = record.endAtMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
    val minutes = (record.effectiveDurationMillis() / 60_000L).toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(record.title.trim().ifBlank { "未命名记录" }) },
        text = {
            Column {
                Text("开始：${start.format(DetailFormatter)}")
                if (end != null) {
                    Text("结束：${end.format(DetailFormatter)}")
                }
                Text("用时：${minutes} 分钟")
                if (group != null) {
                    Text("分组：${group.name}")
                }
                if (record.notes.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("备注：${record.notes}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete) { Text("删除") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
