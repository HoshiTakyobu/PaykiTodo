package com.example.todoalarm.ui

import android.content.ClipboardManager
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.CalendarEventDraft
import com.example.todoalarm.data.RecurrenceConfig
import com.example.todoalarm.data.RecurrenceType
import com.example.todoalarm.data.ReminderDeliveryMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

internal data class CalendarBatchImportDefaults(
    val defaultReminderMinutesBefore: Int,
    val defaultRingEnabled: Boolean,
    val defaultVibrateEnabled: Boolean,
    val defaultReminderDeliveryMode: ReminderDeliveryMode,
    val defaultAccentColorHex: String = "#4E87E1"
)

internal data class CalendarBatchImportPreviewItem(
    val index: Int,
    val source: String,
    val draft: CalendarEventDraft
)

internal data class CalendarBatchImportParseResult(
    val previews: List<CalendarBatchImportPreviewItem>,
    val errors: List<String>
) {
    val canImport: Boolean
        get() = previews.isNotEmpty() && errors.isEmpty()
}

internal enum class CalendarImportFormat(val label: String) {
    AUTO("自动识别"),
    CUSTOM("自定义语法"),
    CSV("CSV"),
    TSV("TSV"),
    ICS("ICS")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CalendarBatchImportDialog(
    defaults: CalendarBatchImportDefaults,
    onDismiss: () -> Unit,
    onImport: (List<CalendarEventDraft>) -> Unit
) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf(CalendarImportFormat.AUTO) }
    var parseResult by remember { mutableStateOf<CalendarBatchImportParseResult?>(null) }
    var showHelp by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("批量导入日程", fontWeight = FontWeight.Bold)
                    Text(
                        text = "支持按你约定的文本语法，一次性生成多条日程。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showHelp = true }) {
                    Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = "查看语法帮助")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "快速导入",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "首条必须带日期；同一天后续条目可省略日期；条目之间用分号或换行分隔。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "例：2026-04-27: 10:20-11:55, 辅导员助理值班, @MB-B1-412; 12:30-14:00, 午休",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        parseResult = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp),
                    label = { Text("输入语法文本") },
                    placeholder = {
                        Text(
                            text = "2026-03-02: 19:30-21:55, 【课】二胡演奏基础, @品学楼C506, Weekly, 2026-05-12",
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    minLines = 8
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalendarImportFormat.entries.forEach { format ->
                        AssistChip(
                            onClick = {
                                selectedFormat = format
                                parseResult = null
                            },
                            label = { Text(format.label) }
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            input = CalendarBatchImportSampleText
                            parseResult = null
                        },
                        label = { Text("载入示例") }
                    )
                    AssistChip(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            val pasted = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            if (pasted.isNotBlank()) {
                                input = pasted
                                parseResult = null
                            }
                        },
                        label = { Text("粘贴剪贴板") }
                    )
                    AssistChip(
                        onClick = { showHelp = true },
                        label = { Text("查看 Wiki") }
                    )
                }

                parseResult?.let { result ->
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (result.errors.isEmpty()) {
                            Color(0x1A2E7D32)
                        } else {
                            Color(0x1AC62828)
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (result.canImport) {
                                    "解析成功，共 ${result.previews.size} 条，可直接导入。"
                                } else {
                                    "解析完成：${result.previews.size} 条有效，${result.errors.size} 条问题。"
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (result.errors.isEmpty()) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            if (result.errors.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    result.errors.forEach { error ->
                                        Text(
                                            text = error,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                parseResult?.previews?.takeIf { it.isNotEmpty() }?.let { previews ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "解析预览",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        previews.forEach { preview ->
                            BatchImportPreviewCard(preview)
                        }
                    }
                }
            }
        },
        confirmButton = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        parseResult = CalendarBatchImportHub.parse(input, defaults, selectedFormat)
                    }
                ) {
                    Text("解析预览")
                }
                Button(
                    onClick = {
                        val latestResult = parseResult ?: CalendarBatchImportHub.parse(input, defaults, selectedFormat).also {
                            parseResult = it
                        }
                        if (latestResult.canImport) {
                            onImport(latestResult.previews.map { it.draft })
                        }
                    }
                ) {
                    Text("一键导入")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )

    if (showHelp) {
        CalendarBatchImportHelpDialog(onDismiss = { showHelp = false })
    }
}

@Composable
private fun BatchImportPreviewCard(preview: CalendarBatchImportPreviewItem) {
    val draft = preview.draft
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "第 ${preview.index} 条",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = batchImportTimeLabel(draft),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = draft.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            draft.location.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ImportTag(
                    label = if (draft.recurrence.isRecurring) {
                        recurrencePreviewLabel(draft.recurrence, draft.startAt.toLocalDate())
                    } else {
                        "单次"
                    }
                )
                ImportTag(
                    label = draft.reminderMinutesBefore?.let {
                        "提醒 ${reminderLeadTimeText(it)}"
                    } ?: "不提醒"
                )
                if (draft.reminderMinutesBefore != null) {
                    ImportTag(label = draft.reminderDeliveryMode.label)
                }
            }
            if (draft.notes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = draft.notes,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Text(
                text = preview.source,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ImportTag(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun CalendarBatchImportHelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量导入语法 Wiki", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HelpSectionCard(
                    title = "1. 最小可用格式",
                    body = listOf(
                        "每条日程至少要有：日期、时间段、标题。",
                        "首条必须带日期；后续同一天的日程可以省略日期。",
                        "条目之间可以用英文分号 ; 或直接换行。"
                    ),
                    code = """
                        2026-04-27: 10:20-11:55, 辅导员助理值班, @MB-B1-412;
                        12:30-14:00, 午休
                    """.trimIndent()
                )
                HelpSectionCard(
                    title = "2. 字段顺序",
                    body = listOf(
                        "格式固定为：[日期:] 时间段, 标题, @地点, 循环规则, 循环截止日期, 扩展字段。",
                        "地点、循环、提醒等字段都可以省略。",
                        "如果标题或地点里需要出现逗号、分号，请用双引号包起来。"
                    ),
                    code = """
                        2026-03-02: 19:30-21:55, "【课】二胡演奏基础", @"品学楼,C506", Weekly, 2026-05-12
                    """.trimIndent()
                )
                HelpSectionCard(
                    title = "3. 时间写法",
                    body = listOf(
                        "普通日程：HH:MM-HH:MM",
                        "跨天日程：HH:MM-次日HH:MM",
                        "全天日程：全天"
                    ),
                    code = """
                        2026-04-28: 全天, 【第9周】
                        2026-04-29: 22:30-次日01:00, 熬夜赶论文
                    """.trimIndent()
                )
                HelpSectionCard(
                    title = "4. 循环规则",
                    body = listOf(
                        "Daily：每天",
                        "Weekly：每周按起始日期对应的星期重复",
                        "Weekly[Mon,Wed,Fri]：每周按多个星期重复",
                        "MonthlyDay：每月同一天",
                        "MonthlyNthWeekday：每月第 N 个星期 X",
                        "Yearly：每年同月同日"
                    ),
                    code = """
                        2026-03-02: 19:30-21:55, 【课】二胡演奏基础, @品学楼C506, Weekly, 2026-05-12
                        2026-05-01: 08:00-09:35, 【课】高代, @A305, Weekly[Mon,Wed,Fri], 2026-06-30
                    """.trimIndent()
                )
                HelpSectionCard(
                    title = "5. 提醒扩展字段",
                    body = listOf(
                        "使用 Remind=... 可以显式覆盖默认提醒。",
                        "如果不写 Remind，则默认提前 5 分钟提醒，并使用铃声+震动。",
                        "支持 Remind=15m / 1h / 2d / off。",
                        "Mode=Notification 或 Mode=Fullscreen。",
                        "Ring=On/Off，Vibrate=On/Off。"
                    ),
                    code = """
                        2026-05-02: 18:30-20:00, 复习线代, Note="刷题 2 套", Remind=30m, Mode=Notification, Ring=On, Vibrate=Off
                    """.trimIndent()
                )
                HelpSectionCard(
                    title = "6. 默认规则",
                    body = listOf(
                        "不写循环规则：按单次日程处理。",
                        "写了循环规则但不写循环截止日期：默认按起始日期后 90 天截止。",
                        "批量导入默认是：提前 5 分钟、通知栏提醒、铃声开启、震动开启。",
                        "Weekly 不写方括号时，会按这条日程的起始星期循环。"
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    )
}

@Composable
private fun HelpSectionCard(
    title: String,
    body: List<String>,
    code: String? = null
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                body.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            code?.let {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

internal object CalendarBatchImportParser {
    fun parse(
        input: String,
        defaults: CalendarBatchImportDefaults
    ): CalendarBatchImportParseResult {
        val previews = mutableListOf<CalendarBatchImportPreviewItem>()
        val errors = mutableListOf<String>()
        var inheritedDate: LocalDate? = null
        val entries = splitTopLevel(input, setOf(';', '\n', '\r'))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (entries.isEmpty()) {
            return CalendarBatchImportParseResult(
                previews = emptyList(),
                errors = listOf("请输入至少一条日程，再进行解析。")
            )
        }

        entries.forEachIndexed { index, rawEntry ->
            runCatching {
                val parsed = parseEntry(rawEntry, inheritedDate, defaults)
                inheritedDate = parsed.date
                previews += CalendarBatchImportPreviewItem(
                    index = index + 1,
                    source = rawEntry,
                    draft = parsed.draft
                )
            }.onFailure { error ->
                errors += "第 ${index + 1} 条：${error.message ?: "无法解析"}"
            }
        }

        return CalendarBatchImportParseResult(previews = previews, errors = errors)
    }

    private fun parseEntry(
        entry: String,
        inheritedDate: LocalDate?,
        defaults: CalendarBatchImportDefaults
    ): ParsedEntry {
        val dateMatch = EntryDatePattern.matchEntire(entry)
        val date = when {
            dateMatch != null -> parseDate(dateMatch.groupValues[1], "日期格式错误，应为 YYYY-MM-DD")
            inheritedDate != null -> inheritedDate
            else -> error("首条日程必须显式写日期，格式如 2026-04-27: 10:20-11:55, 标题")
        }
        val body = dateMatch?.groupValues?.get(2) ?: entry
        val fields = splitTopLevel(body, setOf(','))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        require(fields.size >= 2) { "至少需要“时间段, 标题”两个字段" }

        val timeRange = parseTimeRange(date, decodeToken(fields[0]))
        val title = decodeToken(fields[1])
        require(title.isNotBlank()) { "标题不能为空" }

        var location = ""
        var notes = ""
        var recurrenceType = RecurrenceType.NONE
        var recurrenceSpecified = false
        var weeklyDays = emptySet<DayOfWeek>()
        var recurrenceEndDate: LocalDate? = null
        var reminderMinutesBefore: Int? = defaults.defaultReminderMinutesBefore
        var mode = defaults.defaultReminderDeliveryMode
        var ringEnabled = defaults.defaultRingEnabled
        var vibrateEnabled = defaults.defaultVibrateEnabled
        var accentColorHex = defaults.defaultAccentColorHex
        var groupName = ""

        fields.drop(2).forEach { field ->
            val normalized = field.trim()
            when {
                location.isBlank() && !looksLikeExtensionField(normalized) && !looksLikeDateLiteral(normalized) && !looksLikeRecurrenceRule(normalized) -> {
                    require(location.isBlank()) { "地点字段重复" }
                    location = decodeToken(normalized)
                }
                looksLikeExtensionField(normalized) -> {
                    val (key, value) = splitExtensionField(normalized)
                    when (key.lowercase(Locale.ROOT)) {
                        "note" -> notes = decodeToken(value)
                        "remind" -> {
                            reminderMinutesBefore = parseReminderValue(value)
                        }
                        "mode" -> {
                            mode = parseReminderMode(value)
                        }
                        "ring" -> {
                            ringEnabled = parseBooleanSwitch(value, "Ring")
                        }
                        "vibrate" -> {
                            vibrateEnabled = parseBooleanSwitch(value, "Vibrate")
                        }
                        "color" -> accentColorHex = parseColorValue(value)
                        "group" -> groupName = decodeToken(value)
                        else -> error("暂不支持字段 $key")
                    }
                }
                looksLikeDateLiteral(normalized) -> {
                    require(recurrenceSpecified) { "循环截止日期前必须先写循环规则" }
                    require(recurrenceEndDate == null) { "循环截止日期重复" }
                    recurrenceEndDate = parseDate(normalized, "循环截止日期格式错误，应为 YYYY-MM-DD")
                }
                else -> {
                    val recurrenceRule = parseRecurrenceRule(normalized, date)
                    require(!recurrenceSpecified) { "循环规则重复" }
                    recurrenceSpecified = true
                    recurrenceType = recurrenceRule.type
                    weeklyDays = recurrenceRule.weeklyDays
                }
            }
        }

        val recurrence = if (recurrenceSpecified) {
            RecurrenceConfig(
                enabled = true,
                type = recurrenceType,
                weeklyDays = weeklyDays,
                endDate = recurrenceEndDate ?: date.plusDays(90)
            )
        } else {
            RecurrenceConfig()
        }

        if (!timeRange.allDay && !timeRange.endAt.isAfter(timeRange.startAt)) {
            error("结束时间必须晚于开始时间；若跨天请显式写成 HH:MM-次日HH:MM")
        }

        return ParsedEntry(
            date = date,
            draft = CalendarEventDraft(
                title = title,
                notes = notes,
                location = location,
                startAt = timeRange.startAt,
                endAt = timeRange.endAt,
                allDay = timeRange.allDay,
                accentColorHex = accentColorHex,
                reminderMinutesBefore = reminderMinutesBefore,
                ringEnabled = ringEnabled,
                vibrateEnabled = vibrateEnabled,
                reminderDeliveryMode = mode,
                recurrence = recurrence,
                groupName = groupName
            )
        )
    }

    private fun parseTimeRange(date: LocalDate, value: String): ParsedTimeRange {
        val normalized = value.trim()
        if (normalized == "全天") {
            val startAt = LocalDateTime.of(date, LocalTime.MIN)
            return ParsedTimeRange(startAt = startAt, endAt = startAt, allDay = true)
        }

        val match = TimeRangePattern.matchEntire(normalized)
            ?: error("时间段格式错误，应为 HH:MM-HH:MM、HH:MM-次日HH:MM 或 全天")
        val start = parseClockTime(match.groupValues[1])
        val nextDay = match.groupValues[2].isNotBlank()
        val end = parseClockTime(match.groupValues[3])
        val startAt = LocalDateTime.of(date, start)
        val endAt = LocalDateTime.of(if (nextDay) date.plusDays(1) else date, end)
        return ParsedTimeRange(startAt = startAt, endAt = endAt, allDay = false)
    }

    private fun parseRecurrenceRule(
        value: String,
        date: LocalDate
    ): ParsedRecurrenceRule {
        val normalized = value.trim()
        return when {
            normalized.equals("Daily", ignoreCase = true) || normalized == "每天" ->
                ParsedRecurrenceRule(RecurrenceType.DAILY, emptySet())
            normalized.equals("Weekly", ignoreCase = true) || normalized == "每周" ->
                ParsedRecurrenceRule(RecurrenceType.WEEKLY, setOf(date.dayOfWeek))
            normalized.startsWith("Weekly[", ignoreCase = true) && normalized.endsWith("]") -> {
                val content = normalized.substringAfter('[').substringBeforeLast(']')
                val weekdays = splitTopLevel(content, setOf(','))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .map(::parseWeekday)
                    .toSet()
                require(weekdays.isNotEmpty()) { "Weekly[...] 至少需要一个星期" }
                ParsedRecurrenceRule(RecurrenceType.WEEKLY, weekdays)
            }
            normalized.equals("MonthlyDay", ignoreCase = true) || normalized == "每月D日" ->
                ParsedRecurrenceRule(RecurrenceType.MONTHLY_DAY, emptySet())
            normalized.equals("MonthlyNthWeekday", ignoreCase = true) || normalized == "每月第N个星期X" ->
                ParsedRecurrenceRule(RecurrenceType.MONTHLY_NTH_WEEKDAY, emptySet())
            normalized.equals("Yearly", ignoreCase = true) || normalized == "每年" ->
                ParsedRecurrenceRule(RecurrenceType.YEARLY_DATE, emptySet())
            else -> error("无法识别循环规则：$value")
        }
    }

    private fun looksLikeRecurrenceRule(value: String): Boolean {
        val normalized = value.trim()
        return normalized.equals("Daily", ignoreCase = true) ||
            normalized == "每天" ||
            normalized.equals("Weekly", ignoreCase = true) ||
            normalized == "每周" ||
            (normalized.startsWith("Weekly[", ignoreCase = true) && normalized.endsWith("]")) ||
            normalized.equals("MonthlyDay", ignoreCase = true) ||
            normalized == "每月D日" ||
            normalized.equals("MonthlyNthWeekday", ignoreCase = true) ||
            normalized == "每月第N个星期X" ||
            normalized.equals("Yearly", ignoreCase = true) ||
            normalized == "每年"
    }

    private fun parseWeekday(value: String): DayOfWeek {
        return when (value.trim().lowercase(Locale.ROOT)) {
            "mon", "monday", "周一", "星期一" -> DayOfWeek.MONDAY
            "tue", "tues", "tuesday", "周二", "星期二" -> DayOfWeek.TUESDAY
            "wed", "wednesday", "周三", "星期三" -> DayOfWeek.WEDNESDAY
            "thu", "thur", "thurs", "thursday", "周四", "星期四" -> DayOfWeek.THURSDAY
            "fri", "friday", "周五", "星期五" -> DayOfWeek.FRIDAY
            "sat", "saturday", "周六", "星期六" -> DayOfWeek.SATURDAY
            "sun", "sunday", "周日", "星期日", "星期天" -> DayOfWeek.SUNDAY
            else -> error("无法识别星期值：$value")
        }
    }

    private fun parseReminderValue(value: String): Int? {
        val normalized = value.trim()
        if (normalized.equals("off", ignoreCase = true)) return null
        val match = ReminderPattern.matchEntire(normalized)
            ?: error("Remind 格式错误，示例：Remind=15m、Remind=1h、Remind=2d、Remind=off")
        val amount = match.groupValues[1].toIntOrNull() ?: error("Remind 数值错误：$value")
        return when (match.groupValues[2].lowercase(Locale.ROOT)) {
            "m", "min", "mins", "minute", "minutes", "分钟" -> amount
            "h", "hour", "hours", "小时" -> amount * 60
            "d", "day", "days", "天" -> amount * 24 * 60
            else -> error("不支持的提醒单位：${match.groupValues[2]}")
        }
    }

    private fun parseReminderMode(value: String): ReminderDeliveryMode {
        return when (value.trim().lowercase(Locale.ROOT)) {
            "notification", "通知栏", "通知栏提醒" -> ReminderDeliveryMode.NOTIFICATION
            "fullscreen", "全屏", "全屏界面提醒" -> ReminderDeliveryMode.FULLSCREEN
            else -> error("Mode 仅支持 Notification 或 Fullscreen")
        }
    }

    private fun parseBooleanSwitch(value: String, label: String): Boolean {
        return when (value.trim().lowercase(Locale.ROOT)) {
            "on", "true", "yes", "开" -> true
            "off", "false", "no", "关" -> false
            else -> error("$label 仅支持 On 或 Off")
        }
    }

    private fun parseColorValue(value: String): String {
        val normalized = value.trim()
        require(ColorPattern.matches(normalized)) { "Color 必须是 #RRGGBB 格式" }
        return normalized.uppercase(Locale.ROOT)
    }

    private fun parseDate(value: String, errorMessage: String): LocalDate {
        return try {
            LocalDate.parse(value.trim())
        } catch (_: DateTimeParseException) {
            error(errorMessage)
        }
    }

    private fun parseClockTime(value: String): LocalTime {
        val parts = value.trim().split(':')
        require(parts.size == 2) { "时间格式错误：$value" }
        val hour = parts[0].toIntOrNull() ?: error("时间格式错误：$value")
        val minute = parts[1].toIntOrNull() ?: error("时间格式错误：$value")
        require(hour in 0..23 && minute in 0..59) { "时间超出范围：$value" }
        return LocalTime.of(hour, minute)
    }

    private fun decodeToken(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.length >= 2 && trimmed.startsWith('"') && trimmed.endsWith('"')) {
            trimmed.substring(1, trimmed.length - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim()
        } else {
            trimmed
        }
    }

    private fun splitExtensionField(value: String): Pair<String, String> {
        val separatorIndex = value.indexOf('=')
        require(separatorIndex in 1 until value.lastIndex) { "扩展字段格式错误：$value" }
        return value.substring(0, separatorIndex).trim() to value.substring(separatorIndex + 1).trim()
    }

    private fun looksLikeExtensionField(value: String): Boolean {
        val separatorIndex = value.indexOf('=')
        return separatorIndex in 1 until value.lastIndex
    }

    private fun looksLikeDateLiteral(value: String): Boolean {
        return DateLiteralPattern.matches(value.trim())
    }

    private data class ParsedEntry(
        val date: LocalDate,
        val draft: CalendarEventDraft
    )

    private data class ParsedTimeRange(
        val startAt: LocalDateTime,
        val endAt: LocalDateTime,
        val allDay: Boolean
    )

    private data class ParsedRecurrenceRule(
        val type: RecurrenceType,
        val weeklyDays: Set<DayOfWeek>
    )

    private val EntryDatePattern = Regex("""^\s*(\d{4}-\d{2}-\d{2})\s*:\s*(.+)$""")
    private val DateLiteralPattern = Regex("""^\d{4}-\d{2}-\d{2}$""")
    private val TimeRangePattern = Regex("""^\s*([0-2]?\d:\d{2})\s*-\s*(次日)?\s*([0-2]?\d:\d{2})\s*$""")
    private val ReminderPattern = Regex("""^\s*(\d+)\s*(m|min|mins|minute|minutes|分钟|h|hour|hours|小时|d|day|days|天)\s*$""")
    private val ColorPattern = Regex("""^#[0-9A-Fa-f]{6}$""")
}

internal object CalendarBatchImportHub {
    fun parse(
        input: String,
        defaults: CalendarBatchImportDefaults,
        preferredFormat: CalendarImportFormat = CalendarImportFormat.AUTO
    ): CalendarBatchImportParseResult {
        val format = if (preferredFormat == CalendarImportFormat.AUTO) {
            detectFormat(input)
        } else {
            preferredFormat
        }
        return when (format) {
            CalendarImportFormat.AUTO, CalendarImportFormat.CUSTOM -> CalendarBatchImportParser.parse(input, defaults)
            CalendarImportFormat.CSV -> parseDelimited(input, defaults, ',')
            CalendarImportFormat.TSV -> parseDelimited(input, defaults, '\t')
            CalendarImportFormat.ICS -> parseIcs(input, defaults)
        }
    }

    private fun detectFormat(input: String): CalendarImportFormat {
        val trimmed = input.trim()
        return when {
            trimmed.contains("BEGIN:VCALENDAR", ignoreCase = true) || trimmed.contains("BEGIN:VEVENT", ignoreCase = true) -> CalendarImportFormat.ICS
            trimmed.lineSequence().firstOrNull()?.contains('\t') == true -> CalendarImportFormat.TSV
            trimmed.lineSequence().firstOrNull()?.contains(',') == true && trimmed.lineSequence().firstOrNull()?.contains("title", ignoreCase = true) == true -> CalendarImportFormat.CSV
            else -> CalendarImportFormat.CUSTOM
        }
    }

    private fun parseDelimited(
        input: String,
        defaults: CalendarBatchImportDefaults,
        delimiter: Char
    ): CalendarBatchImportParseResult {
        val lines = input.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) {
            return CalendarBatchImportParseResult(emptyList(), listOf("请输入要导入的内容。"))
        }
        val header = splitCsvLine(lines.first(), delimiter).map { it.trim().lowercase(Locale.ROOT) }
        val previews = mutableListOf<CalendarBatchImportPreviewItem>()
        val errors = mutableListOf<String>()

        lines.drop(1).forEachIndexed { index, line ->
            runCatching {
                val cells = splitCsvLine(line, delimiter)
                val row = header.mapIndexedNotNull { colIndex, name ->
                    cells.getOrNull(colIndex)?.let { name to it.trim() }
                }.toMap()
                buildDraftFromRow(row, defaults)
            }.onSuccess { draft ->
                previews += CalendarBatchImportPreviewItem(index + 1, line, draft)
            }.onFailure {
                errors += "第 ${index + 1} 条：${it.message ?: "无法解析"}"
            }
        }
        return CalendarBatchImportParseResult(previews, errors)
    }

    private fun buildDraftFromRow(
        row: Map<String, String>,
        defaults: CalendarBatchImportDefaults
    ): CalendarEventDraft {
        val date = LocalDate.parse(requireField(row, "date"))
        val title = requireField(row, "title")
        val allDay = row["allday"]?.equals("true", ignoreCase = true) == true || row["allday"] == "1"
        val startAt = if (allDay) {
            LocalDateTime.of(date, LocalTime.MIN)
        } else {
            LocalDateTime.of(date, parseClock(row["start"] ?: error("缺少 start")))
        }
        val endAt = if (allDay) {
            LocalDateTime.of(LocalDate.parse(row["enddate"] ?: date.toString()), LocalTime.MIN)
        } else {
            val endTime = parseClock(row["end"] ?: error("缺少 end"))
            val rawEnd = LocalDateTime.of(date, endTime)
            if (rawEnd.isAfter(startAt)) rawEnd else rawEnd.plusDays(1)
        }
        val recurrence = parseRecurrenceFromRow(row, date)
        return CalendarEventDraft(
            title = title,
            notes = row["notes"].orEmpty(),
            location = row["location"].orEmpty(),
            startAt = startAt,
            endAt = endAt,
            allDay = allDay,
            accentColorHex = row["color"]?.takeIf { it.matches(Regex("^#[0-9A-Fa-f]{6}$")) } ?: defaults.defaultAccentColorHex,
            reminderMinutesBefore = row["remind"]?.let { parseReminderCell(it) } ?: defaults.defaultReminderMinutesBefore,
            ringEnabled = row["ring"]?.let { parseOnOff(it) } ?: defaults.defaultRingEnabled,
            vibrateEnabled = row["vibrate"]?.let { parseOnOff(it) } ?: defaults.defaultVibrateEnabled,
            reminderDeliveryMode = row["mode"]?.let { parseModeCell(it) } ?: defaults.defaultReminderDeliveryMode,
            recurrence = recurrence,
            groupName = row["group"].orEmpty()
        )
    }

    private fun parseRecurrenceFromRow(row: Map<String, String>, date: LocalDate): RecurrenceConfig {
        val recurrenceRaw = row["recurrence"]?.trim().orEmpty()
        if (recurrenceRaw.isBlank()) return RecurrenceConfig()
        val endDate = row["recurrenceend"]?.takeIf { it.isNotBlank() }?.let(LocalDate::parse) ?: date.plusDays(90)
        return when (recurrenceRaw.lowercase(Locale.ROOT)) {
            "daily" -> RecurrenceConfig(true, RecurrenceType.DAILY, endDate = endDate)
            "weekly" -> RecurrenceConfig(true, RecurrenceType.WEEKLY, weeklyDays = setOf(date.dayOfWeek), endDate = endDate)
            "monthlyday" -> RecurrenceConfig(true, RecurrenceType.MONTHLY_DAY, endDate = endDate)
            "monthlynthweekday" -> RecurrenceConfig(true, RecurrenceType.MONTHLY_NTH_WEEKDAY, endDate = endDate)
            "yearly" -> RecurrenceConfig(true, RecurrenceType.YEARLY_DATE, endDate = endDate)
            else -> RecurrenceConfig()
        }
    }

    private fun parseIcs(
        input: String,
        defaults: CalendarBatchImportDefaults
    ): CalendarBatchImportParseResult {
        val previews = mutableListOf<CalendarBatchImportPreviewItem>()
        val errors = mutableListOf<String>()
        val events = input.split("BEGIN:VEVENT").drop(1).map { it.substringBefore("END:VEVENT") }
        if (events.isEmpty()) {
            return CalendarBatchImportParseResult(emptyList(), listOf("没有识别到 VEVENT 内容。"))
        }
        events.forEachIndexed { index, block ->
            runCatching {
                val fields = block.lineSequence()
                    .map { it.trim() }
                    .filter { ':' in it }
                    .associate { line ->
                        val key = line.substringBefore(':')
                        val value = line.substringAfter(':')
                        key to value
                    }
                buildDraftFromIcs(fields, defaults)
            }.onSuccess { draft ->
                previews += CalendarBatchImportPreviewItem(index + 1, block.trim(), draft)
            }.onFailure {
                errors += "第 ${index + 1} 条：${it.message ?: "ICS 解析失败"}"
            }
        }
        return CalendarBatchImportParseResult(previews, errors)
    }

    private fun buildDraftFromIcs(
        fields: Map<String, String>,
        defaults: CalendarBatchImportDefaults
    ): CalendarEventDraft {
        val title = requireField(fields, "SUMMARY")
        val startRaw = fields.entries.firstOrNull { it.key.startsWith("DTSTART", ignoreCase = true) }?.value
            ?: error("缺少 DTSTART")
        val endRaw = fields.entries.firstOrNull { it.key.startsWith("DTEND", ignoreCase = true) }?.value
            ?: error("缺少 DTEND")
        val allDay = startRaw.length == 8
        val startAt = parseIcsDateTime(startRaw, allDay)
        val endAt = parseIcsDateTime(endRaw, allDay)
        val recurrence = fields["RRULE"]?.let { parseRRule(it, startAt.toLocalDate()) } ?: RecurrenceConfig()
        return CalendarEventDraft(
            title = title,
            notes = fields["DESCRIPTION"].orEmpty(),
            location = fields["LOCATION"].orEmpty(),
            startAt = startAt,
            endAt = endAt,
            allDay = allDay,
            accentColorHex = defaults.defaultAccentColorHex,
            reminderMinutesBefore = defaults.defaultReminderMinutesBefore,
            ringEnabled = defaults.defaultRingEnabled,
            vibrateEnabled = defaults.defaultVibrateEnabled,
            reminderDeliveryMode = defaults.defaultReminderDeliveryMode,
            recurrence = recurrence,
            groupName = fields["CATEGORIES"].orEmpty()
        )
    }

    private fun parseRRule(value: String, anchorDate: LocalDate): RecurrenceConfig {
        val parts = value.split(';').associate {
            val key = it.substringBefore('=')
            val raw = it.substringAfter('=', "")
            key.uppercase(Locale.ROOT) to raw
        }
        val endDate = parts["UNTIL"]?.takeIf { it.isNotBlank() }?.let {
            if (it.length >= 8) LocalDate.parse(it.take(8), DateTimeFormatter.BASIC_ISO_DATE) else anchorDate.plusDays(90)
        } ?: anchorDate.plusDays(90)
        return when (parts["FREQ"]?.uppercase(Locale.ROOT)) {
            "DAILY" -> RecurrenceConfig(true, RecurrenceType.DAILY, endDate = endDate)
            "WEEKLY" -> {
                val days = parts["BYDAY"]?.split(',')?.mapNotNull { byDayTokenToWeekday(it) }?.toSet().orEmpty()
                RecurrenceConfig(true, RecurrenceType.WEEKLY, weeklyDays = if (days.isEmpty()) setOf(anchorDate.dayOfWeek) else days, endDate = endDate)
            }
            "MONTHLY" -> {
                if (parts.containsKey("BYMONTHDAY")) {
                    RecurrenceConfig(true, RecurrenceType.MONTHLY_DAY, endDate = endDate)
                } else {
                    RecurrenceConfig(true, RecurrenceType.MONTHLY_NTH_WEEKDAY, endDate = endDate)
                }
            }
            "YEARLY" -> RecurrenceConfig(true, RecurrenceType.YEARLY_DATE, endDate = endDate)
            else -> RecurrenceConfig()
        }
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        return splitTopLevel(line, setOf(delimiter))
    }

    private fun requireField(row: Map<String, String>, key: String): String {
        return row.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value?.takeIf { it.isNotBlank() }
            ?: error("缺少字段 $key")
    }

    private fun parseClock(value: String): LocalTime {
        val parts = value.trim().split(':')
        require(parts.size == 2) { "时间格式错误：$value" }
        return LocalTime.of(parts[0].toInt(), parts[1].toInt())
    }

    private fun parseReminderCell(value: String): Int? {
        val normalized = value.trim()
        if (normalized.equals("off", ignoreCase = true)) return null
        val match = Regex("^\\s*(\\d+)\\s*(m|min|mins|minute|minutes|分钟|h|hour|hours|小时|d|day|days|天)\\s*$")
            .matchEntire(normalized)
            ?: error("Remind 格式错误：$value")
        val amount = match.groupValues[1].toIntOrNull() ?: error("Remind 数值错误：$value")
        return when (match.groupValues[2].lowercase(Locale.ROOT)) {
            "m", "min", "mins", "minute", "minutes", "分钟" -> amount
            "h", "hour", "hours", "小时" -> amount * 60
            "d", "day", "days", "天" -> amount * 24 * 60
            else -> error("不支持的提醒单位：${match.groupValues[2]}")
        }
    }

    private fun parseModeCell(value: String): ReminderDeliveryMode {
        return when (value.trim().lowercase(Locale.ROOT)) {
            "notification" -> ReminderDeliveryMode.NOTIFICATION
            "fullscreen" -> ReminderDeliveryMode.FULLSCREEN
            else -> ReminderDeliveryMode.NOTIFICATION
        }
    }

    private fun parseOnOff(value: String): Boolean {
        return when (value.trim().lowercase(Locale.ROOT)) {
            "on", "true", "1", "yes" -> true
            else -> false
        }
    }

    private fun parseIcsDateTime(value: String, allDay: Boolean): LocalDateTime {
        return if (allDay) {
            LocalDate.parse(value.take(8), DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay()
        } else {
            val normalized = value.removeSuffix("Z")
            LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
        }
    }

    private fun byDayTokenToWeekday(token: String): DayOfWeek? {
        return when (token.uppercase(Locale.ROOT)) {
            "MO" -> DayOfWeek.MONDAY
            "TU" -> DayOfWeek.TUESDAY
            "WE" -> DayOfWeek.WEDNESDAY
            "TH" -> DayOfWeek.THURSDAY
            "FR" -> DayOfWeek.FRIDAY
            "SA" -> DayOfWeek.SATURDAY
            "SU" -> DayOfWeek.SUNDAY
            else -> null
        }
    }
}

private fun splitTopLevel(
    text: String,
    separators: Set<Char>
): List<String> {
    if (text.isBlank()) return emptyList()
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var previous: Char? = null

    text.forEach { ch ->
        if (ch == '"' && previous != '\\') {
            inQuotes = !inQuotes
        }
        if (separators.contains(ch) && !inQuotes) {
            val token = current.toString().trim()
            if (token.isNotEmpty()) result += token
            current.clear()
        } else {
            current.append(ch)
        }
        previous = ch
    }

    val tail = current.toString().trim()
    if (tail.isNotEmpty()) result += tail
    return result
}

private fun batchImportTimeLabel(draft: CalendarEventDraft): String {
    if (draft.allDay) {
        return if (draft.startAt.toLocalDate() == draft.endAt.toLocalDate()) {
            "${draft.startAt.toLocalDate()} · 全天"
        } else {
            "${draft.startAt.toLocalDate()} - ${draft.endAt.toLocalDate()} · 全天"
        }
    }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    return if (draft.startAt.toLocalDate() == draft.endAt.toLocalDate()) {
        "${draft.startAt.format(formatter)} - ${draft.endAt.format(timeFormatter)}"
    } else {
        "${draft.startAt.format(formatter)} - ${draft.endAt.format(formatter)}"
    }
}

private fun recurrencePreviewLabel(
    recurrence: RecurrenceConfig,
    startDate: LocalDate
): String {
    return when (recurrence.type) {
        RecurrenceType.NONE -> "单次"
        RecurrenceType.DAILY -> "每天"
        RecurrenceType.WEEKLY -> {
            val detail = recurrence.weeklyDays
                .ifEmpty { setOf(startDate.dayOfWeek) }
                .sortedBy { it.value }
                .joinToString("/") { weekdayShortLabel(it) }
            "每周 $detail"
        }
        RecurrenceType.MONTHLY_DAY -> "每月 ${startDate.dayOfMonth} 日"
        RecurrenceType.MONTHLY_NTH_WEEKDAY -> "每月第 ${((startDate.dayOfMonth - 1) / 7) + 1} 个${weekdayShortLabel(startDate.dayOfWeek)}"
        RecurrenceType.YEARLY_DATE -> "每年 ${startDate.monthValue} 月 ${startDate.dayOfMonth} 日"
    }
}

private fun reminderLeadTimeText(minutes: Int): String {
    return when {
        minutes % (24 * 60) == 0 -> "${minutes / (24 * 60)} 天前"
        minutes % 60 == 0 -> "${minutes / 60} 小时前"
        else -> "$minutes 分钟前"
    }
}

private fun weekdayShortLabel(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "周一"
    DayOfWeek.TUESDAY -> "周二"
    DayOfWeek.WEDNESDAY -> "周三"
    DayOfWeek.THURSDAY -> "周四"
    DayOfWeek.FRIDAY -> "周五"
    DayOfWeek.SATURDAY -> "周六"
    DayOfWeek.SUNDAY -> "周日"
}

private val CalendarBatchImportSampleText = """
    2026-03-02: 19:30-21:55, 【课】二胡演奏基础, @品学楼C506, Weekly, 2026-05-12, Remind=30m, Mode=Notification;
    09:00-10:05, 【课】辅导员助理值班, @MB-B1-412, Weekly, 2026-06-15;
    12:30-14:00, 午休
""".trimIndent()
