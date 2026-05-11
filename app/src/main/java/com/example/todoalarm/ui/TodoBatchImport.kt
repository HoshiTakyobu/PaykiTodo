package com.example.todoalarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.RecurrenceConfig
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.TodoDraft
import java.time.DateTimeException
import java.time.LocalDateTime

internal data class TodoBatchImportDefaults(
    val defaultGroupId: Long,
    val defaultRingEnabled: Boolean,
    val defaultVibrateEnabled: Boolean
)

private data class TodoBatchPreviewItem(
    val lineNumber: Int,
    val draft: TodoDraft,
    val reminderSummary: String
)

private data class TodoBatchParseResult(
    val previews: List<TodoBatchPreviewItem>,
    val errors: List<String>
)

@Composable
internal fun TodoBatchImportDialog(
    groups: List<TaskGroup>,
    defaults: TodoBatchImportDefaults,
    onDismiss: () -> Unit,
    onImport: (List<TodoDraft>) -> Unit
) {
    var input by remember { mutableStateOf(TodoBatchImportSampleText) }
    var parseResult by remember { mutableStateOf(TodoBatchImportParser.parse(input, groups, defaults)) }
    var showHelp by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量添加待办", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "每行一条待办，字段用 | 分隔；提醒时间内部继续用英文逗号分隔。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        parseResult = TodoBatchImportParser.parse(it, groups, defaults)
                    },
                    label = { Text("待办文本") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 8,
                    maxLines = 14
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showHelp = true }) { Text("语法说明") }
                    OutlinedButton(onClick = {
                        input = TodoBatchImportSampleText
                        parseResult = TodoBatchImportParser.parse(input, groups, defaults)
                    }) { Text("填入示例") }
                }
                if (parseResult.errors.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.82f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("需要修正", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            parseResult.errors.take(5).forEach { error ->
                                Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                if (parseResult.previews.isNotEmpty()) {
                    Text("预览 ${parseResult.previews.size} 条", fontWeight = FontWeight.SemiBold)
                    parseResult.previews.take(8).forEach { preview ->
                        TodoBatchPreviewCard(preview, groups)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = parseResult.errors.isEmpty() && parseResult.previews.isNotEmpty(),
                onClick = { onImport(parseResult.previews.map { it.draft }) }
            ) { Text("导入") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("待办批量导入语法", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("格式：DDL | 标题 | 备注可选 | Remind=提醒 | Group=分组 | Ring=true | Vibrate=true")
                    Text("DDL 支持：2026-05-12 18:00、05-12 18:00、无DDL。")
                    Text("Remind 与单条编辑一致：5,15,16:30,05-10 15:00,2026-05-10 14:30。")
                    Text("如果提醒时刻晚于 DDL，或提醒已经过去，该行会被判定为非法。")
                    Text("示例：2026-05-12 18:00 | 写报告 | 交初稿 | Remind=5,15 | Group=学习")
                }
            },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("知道了") } }
        )
    }
}

@Composable
private fun TodoBatchPreviewCard(preview: TodoBatchPreviewItem, groups: List<TaskGroup>) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("#${preview.lineNumber} ${preview.draft.title}", fontWeight = FontWeight.Bold)
            Text(
                text = preview.draft.dueAt?.let { "DDL：${formatLocalDateTime(it)}" } ?: "无 DDL",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (preview.draft.notes.isNotBlank()) {
                Text(preview.draft.notes, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "分组：${groups.firstOrNull { it.id == preview.draft.groupId }?.name ?: "未分组"}；提醒：${preview.reminderSummary.ifBlank { "不提醒" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private object TodoBatchImportParser {
    fun parse(
        input: String,
        groups: List<TaskGroup>,
        defaults: TodoBatchImportDefaults
    ): TodoBatchParseResult {
        val previews = mutableListOf<TodoBatchPreviewItem>()
        val errors = mutableListOf<String>()
        val groupByName = groups.associateBy { it.name.trim() }

        input.lineSequence().forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#")) return@forEachIndexed

            val parts = line.split('|').map { it.trim() }
            if (parts.size < 2) {
                errors += "第 $lineNumber 行：至少需要 DDL 和标题，用 | 分隔。"
                return@forEachIndexed
            }

            val dueAt = parseTodoDueAt(parts[0])
            if (dueAt == null && !parts[0].isNoDueToken()) {
                errors += "第 $lineNumber 行：DDL 格式无效。"
                return@forEachIndexed
            }
            if (dueAt != null && !dueAt.isAfter(LocalDateTime.now().withSecond(0).withNano(0))) {
                errors += "第 $lineNumber 行：DDL 必须晚于当前时间。"
                return@forEachIndexed
            }

            val title = parts[1].trim()
            if (title.isBlank()) {
                errors += "第 $lineNumber 行：标题不能为空。"
                return@forEachIndexed
            }

            var notes = ""
            var groupId = defaults.defaultGroupId
            var ringEnabled = defaults.defaultRingEnabled
            var vibrateEnabled = defaults.defaultVibrateEnabled
            var reminderOffsets = emptyList<Int>()
            var reminderAt: LocalDateTime? = null
            var reminderSummary = ""

            parts.drop(2).forEach { token ->
                val keyValue = token.split('=', limit = 2)
                if (keyValue.size == 2) {
                    val key = keyValue[0].trim().lowercase()
                    val value = keyValue[1].trim()
                    when (key) {
                        "remind", "reminder", "提醒" -> {
                            if (dueAt == null) {
                                errors += "第 $lineNumber 行：无 DDL 的待办不能设置提醒。"
                            } else {
                                val parsedReminder = parseReminderInput(value, dueAt)
                                if (!parsedReminder.isValid) {
                                    errors += "第 $lineNumber 行：${parsedReminder.message}"
                                } else {
                                    reminderOffsets = parsedReminder.offsetsMinutes
                                    reminderAt = parsedReminder.triggerTimes.minOrNull()
                                    reminderSummary = parsedReminder.message
                                }
                            }
                        }

                        "group", "分组" -> {
                            val group = groupByName[value]
                            if (group == null) {
                                errors += "第 $lineNumber 行：找不到分组“$value”。"
                            } else {
                                groupId = group.id
                            }
                        }

                        "ring", "响铃" -> ringEnabled = value.toBooleanLike(defaults.defaultRingEnabled)
                        "vibrate", "震动" -> vibrateEnabled = value.toBooleanLike(defaults.defaultVibrateEnabled)
                        "note", "notes", "备注" -> notes = value
                        else -> errors += "第 $lineNumber 行：无法识别字段“${keyValue[0]}”。"
                    }
                } else if (notes.isBlank()) {
                    notes = token
                } else {
                    notes += "\n$token"
                }
            }

            previews += TodoBatchPreviewItem(
                lineNumber = lineNumber,
                draft = TodoDraft(
                    title = title,
                    notes = notes,
                    dueAt = dueAt,
                    reminderAt = reminderAt,
                    groupId = groupId,
                    ringEnabled = ringEnabled,
                    vibrateEnabled = vibrateEnabled,
                    recurrence = RecurrenceConfig(),
                    reminderOffsetsMinutes = reminderOffsets
                ),
                reminderSummary = reminderSummary
            )
        }

        if (previews.isEmpty() && errors.isEmpty()) {
            errors += "请输入要导入的待办。"
        }
        return TodoBatchParseResult(previews, errors)
    }
}

private fun parseTodoDueAt(raw: String): LocalDateTime? {
    val token = raw.trim()
    if (token.isNoDueToken()) return null
    val now = LocalDateTime.now()
    DateTimeTokenRegex.matchEntire(token)?.let { match ->
        return safeDateTime(
            year = match.groupValues[1].toInt(),
            month = match.groupValues[2].toInt(),
            day = match.groupValues[3].toInt(),
            hour = match.groupValues[4].toInt(),
            minute = match.groupValues[5].toInt()
        )
    }
    MonthDayTimeTokenRegex.matchEntire(token)?.let { match ->
        return safeDateTime(
            year = now.year,
            month = match.groupValues[1].toInt(),
            day = match.groupValues[2].toInt(),
            hour = match.groupValues[3].toInt(),
            minute = match.groupValues[4].toInt()
        )
    }
    return null
}

private fun String.isNoDueToken(): Boolean {
    val normalized = trim().lowercase()
    return normalized == "无ddl" || normalized == "无 ddl" || normalized == "无截止" || normalized == "no due" || normalized == "-"
}

private fun String.toBooleanLike(defaultValue: Boolean): Boolean {
    return when (trim().lowercase()) {
        "true", "1", "yes", "y", "是", "开", "开启" -> true
        "false", "0", "no", "n", "否", "关", "关闭" -> false
        else -> defaultValue
    }
}

private val MonthDayTimeTokenRegex = Regex("""^(\d{1,2})-(\d{1,2})\s+(\d{1,2}):(\d{2})$""")
private val DateTimeTokenRegex = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})\s+(\d{1,2}):(\d{2})$""")

private fun safeDateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): LocalDateTime? {
    if (hour !in 0..23 || minute !in 0..59) return null
    return try {
        LocalDateTime.of(year, month, day, hour, minute)
    } catch (_: DateTimeException) {
        null
    }
}

private val TodoBatchImportSampleText = """
2026-05-12 18:00 | 写报告 | 交初稿 | Remind=5,15,16:30 | Group=学习
05-13 09:30 | 给老师发消息 | 确认材料 | Remind=10 | Ring=true | Vibrate=true
无DDL | 整理 Obsidian 待办 | 不设置截止时间
""".trimIndent()
