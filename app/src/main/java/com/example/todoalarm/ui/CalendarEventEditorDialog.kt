package com.example.todoalarm.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.CalendarEventDraft
import com.example.todoalarm.data.RecurrenceConfig
import com.example.todoalarm.data.ReminderDeliveryMode
import com.example.todoalarm.data.RecurrencePreviewResult
import com.example.todoalarm.data.RecurrenceType
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.normalizeReminderOffsets
import com.example.todoalarm.data.previewCalendarRecurrence
import com.example.todoalarm.data.storageStringToWeekdays
import com.example.todoalarm.data.toEpochMillis
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class ReminderLeadTimeOption(
    val minutes: Int,
    val label: String
)

private val ReminderLeadTimeOptions = listOf(
    ReminderLeadTimeOption(5, "提前 5 分钟"),
    ReminderLeadTimeOption(10, "提前 10 分钟"),
    ReminderLeadTimeOption(15, "提前 15 分钟"),
    ReminderLeadTimeOption(30, "提前 30 分钟"),
    ReminderLeadTimeOption(60, "提前 1 小时"),
    ReminderLeadTimeOption(120, "提前 2 小时"),
    ReminderLeadTimeOption(24 * 60, "提前 1 天"),
    ReminderLeadTimeOption(48 * 60, "提前 2 天")
)
private val CalendarColorOptions = listOf(
    "#4E87E1",
    "#4CB782",
    "#FF6B4A",
    "#BF7B4D",
    "#8B5CF6",
    "#0F766E",
    "#D97706",
    "#E11D48"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CalendarEventEditorDialog(
    initialEvent: TodoItem?,
    initialDraft: CalendarEventDraft? = null,
    defaultRingEnabled: Boolean,
    defaultVibrateEnabled: Boolean,
    defaultReminderDeliveryMode: ReminderDeliveryMode,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: (CalendarEventDraft) -> Unit
) {
    val context = LocalContext.current
    val now = remember { LocalDateTime.now().withSecond(0).withNano(0) }
    val seedDraft = remember(initialEvent?.id, initialDraft) { if (initialEvent == null) initialDraft else null }
    var title by remember(initialEvent?.id) { mutableStateOf(initialEvent?.title.orEmpty()) }
    var location by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.location ?: seedDraft?.location.orEmpty()) }
    var notes by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.notes ?: seedDraft?.notes.orEmpty()) }
    var allDay by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.allDay ?: (seedDraft?.allDay == true)) }
    var startAt by remember(initialEvent?.id) {
        mutableStateOf(initialEvent?.startAtMillis?.let(::reminderAtMillisToDateTime) ?: seedDraft?.startAt ?: now.plusHours(2))
    }
    var endAt by remember(initialEvent?.id) {
        mutableStateOf(initialEvent?.endAtMillis?.let(::reminderAtMillisToDateTime) ?: seedDraft?.endAt ?: now.plusHours(3))
    }
    var accentColorHex by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(initialEvent?.accentColorHex ?: seedDraft?.accentColorHex ?: CalendarColorOptions.first())
    }
    var reminderEnabled by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(initialEvent?.reminderEnabled == true || seedDraft?.normalizedReminderOffsetsMinutes?.isNotEmpty() == true)
    }
    var reminderOffsetsMinutes by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(
            initialEvent?.configuredReminderOffsetsMinutes?.takeIf { it.isNotEmpty() }
                ?: seedDraft?.normalizedReminderOffsetsMinutes?.takeIf { it.isNotEmpty() }
                ?: listOf(15)
        )
    }
    var reminderAt by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(
            initialEvent?.reminderAtMillis?.let(::reminderAtMillisToDateTime)
                ?: seedDraft?.let { draft ->
                    draft.normalizedReminderOffsetsMinutes.firstOrNull()?.let { minutes ->
                        val anchor = if (draft.allDay) {
                            LocalDateTime.of(draft.startAt.toLocalDate(), LocalTime.of(9, 0))
                        } else {
                            draft.startAt
                        }
                        anchor.minusMinutes(minutes.toLong())
                    }
                }
                ?: startAt.minusMinutes((reminderOffsetsMinutes.firstOrNull() ?: 15).toLong())
        )
    }
    var ringEnabled by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.ringEnabled ?: seedDraft?.ringEnabled ?: defaultRingEnabled) }
    var vibrateEnabled by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.vibrateEnabled ?: seedDraft?.vibrateEnabled ?: defaultVibrateEnabled) }
    var reminderDeliveryMode by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(initialEvent?.reminderDeliveryModeEnum ?: seedDraft?.reminderDeliveryMode ?: defaultReminderDeliveryMode)
    }
    var recurringEnabled by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.isRecurring == true || seedDraft?.recurrence?.enabled == true) }
    var recurrenceType by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(initialEvent?.recurrenceTypeEnum ?: seedDraft?.recurrence?.type ?: RecurrenceType.DAILY)
    }
    var weeklyDays by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(
            if (initialEvent?.isRecurring == true) {
                storageStringToWeekdays(initialEvent.recurrenceWeekdays).ifEmpty { setOf(startAt.dayOfWeek) }
            } else if (seedDraft?.recurrence?.enabled == true) {
                seedDraft.recurrence.weeklyDays.ifEmpty { setOf(startAt.dayOfWeek) }
            } else {
                setOf(startAt.dayOfWeek)
            }
        )
    }
    var recurrenceEndDate by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(initialEvent?.recurrenceEndDate ?: seedDraft?.recurrence?.endDate ?: startAt.toLocalDate().plusDays(90))
    }
    var recurrencePreview by remember { mutableStateOf<RecurrencePreviewResult?>(null) }
    var showReminderPicker by remember { mutableStateOf(false) }
    var showReminderDeliveryPicker by remember { mutableStateOf(false) }
    var showRecurrencePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (initialEvent == null) "新增日程" else "编辑日程", fontWeight = FontWeight.Bold)
                Text(
                    text = "主题、时间、重复、地点、描述、提醒与颜色都在这里完成设置。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EditorBlock(title = "主题") {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("添加主题") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                EditorBlock(title = "起止时间") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("全天日程", fontWeight = FontWeight.SemiBold)
                            Text("开启后会显示在全天栏", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = allDay, onCheckedChange = { allDay = it })
                    }

                    if (allDay) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EditorDateSelectorCard(
                                modifier = Modifier.weight(1f),
                                label = "开始日期",
                                date = startAt.toLocalDate(),
                                onClick = {
                                    showDatePicker(context, startAt.toLocalDate()) { picked ->
                                        startAt = LocalDateTime.of(picked, LocalTime.MIN)
                                        if (endAt.toLocalDate().isBefore(picked)) {
                                            endAt = LocalDateTime.of(picked, LocalTime.MIN)
                                        }
                                    }
                                }
                            )
                            EditorMiddlePill(
                                modifier = Modifier.width(72.dp),
                                label = formatAllDaySpan(startAt.toLocalDate(), endAt.toLocalDate())
                            )
                            EditorDateSelectorCard(
                                modifier = Modifier.weight(1f),
                                label = "结束日期",
                                date = endAt.toLocalDate(),
                                onClick = {
                                    showDatePicker(context, endAt.toLocalDate()) { picked ->
                                        endAt = LocalDateTime.of(picked, LocalTime.MIN)
                                    }
                                }
                            )
                        }
                        Text(
                            text = "全天日程会固定显示在日期栏下方。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EditorDateTimeSelectorCard(
                                modifier = Modifier.weight(1f),
                                label = "开始",
                                dateTime = startAt,
                                onClick = {
                                    showDateTimePicker(context, startAt) { picked ->
                                        startAt = picked
                                        if (!picked.isBefore(endAt)) {
                                            endAt = picked.plusMinutes(30)
                                        }
                                    }
                                }
                            )
                            EditorMiddlePill(
                                modifier = Modifier.width(64.dp),
                                label = formatTimedSpan(startAt, endAt)
                            )
                            EditorDateTimeSelectorCard(
                                modifier = Modifier.weight(1f),
                                label = "结束",
                                dateTime = endAt,
                                onClick = {
                                    showDateTimePicker(context, endAt) { picked ->
                                        endAt = picked
                                        if (!picked.isAfter(startAt)) {
                                            startAt = picked.minusMinutes(30)
                                        }
                                    }
                                }
                            )
                        }
                        Text(
                            text = "点按时间卡片即可修改起止时刻。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                EditorBlock(title = "地点") {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("添加地点") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                EditorBlock(title = "描述") {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("添加描述") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }

                EditorBlock(title = "提醒") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("提醒", fontWeight = FontWeight.SemiBold)
                            Text(if (allDay) "全天日程按当天 09:00 作为基准" else "按开始时间提前提醒", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            thumbContent = null
                        )
                    }

                    if (reminderEnabled) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = if (reminderOffsetsMinutes.isEmpty()) "尚未选择提醒时点" else reminderOffsetsMinutes
                                        .sortedDescending()
                                        .joinToString("、") { reminderLeadTimeLabel(it) },
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = buildReminderMetaLabel(
                                        reminderDeliveryMode = reminderDeliveryMode,
                                        ringEnabled = ringEnabled,
                                        vibrateEnabled = vibrateEnabled
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        EditorSelectionRow(
                            title = "提醒时间",
                            value = reminderSelectionSummary(reminderOffsetsMinutes),
                            onClick = { showReminderPicker = true }
                        )
                        EditorSelectionRow(
                            title = "提醒方式",
                            value = reminderDeliveryMode.label,
                            onClick = { showReminderDeliveryPicker = true }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = ringEnabled,
                                onClick = { ringEnabled = !ringEnabled },
                                label = { Text("响铃") }
                            )
                            FilterChip(
                                selected = vibrateEnabled,
                                onClick = { vibrateEnabled = !vibrateEnabled },
                                label = { Text("震动") }
                            )
                        }
                    }
                }

                EditorBlock(title = "重复") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("重复", fontWeight = FontWeight.SemiBold)
                            Text(if (recurringEnabled) recurrenceType.label else "不重复", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = recurringEnabled,
                            onCheckedChange = { recurringEnabled = it },
                            thumbContent = null
                        )
                    }

                    if (recurringEnabled) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = recurrenceSummaryLabel(recurrenceType, startAt, weeklyDays),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "循环到 $recurrenceEndDate 为止",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        EditorSelectionRow(
                            title = "重复规则",
                            value = recurrenceSummaryLabel(recurrenceType, startAt, weeklyDays),
                            onClick = { showRecurrencePicker = true }
                        )
                        if (recurrenceType == RecurrenceType.MONTHLY_DAY && startAt.dayOfMonth > 28) {
                            Text(
                                text = "（若某个月不存在 ${startAt.dayOfMonth} 日，则自动落到该月最后一天）",
                                color = Color(0xFFC62828),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        OutlinedButton(
                            onClick = { showDatePicker(context, recurrenceEndDate) { recurrenceEndDate = it } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("循环截止日期：$recurrenceEndDate")
                        }
                        OutlinedButton(
                            onClick = {
                                val normalizedOffsets = if (reminderEnabled) normalizeReminderOffsets(reminderOffsetsMinutes) else emptyList()
                                recurrencePreview = previewCalendarRecurrence(
                                    CalendarEventDraft(
                                        title = title,
                                        notes = notes,
                                        location = location,
                                        startAt = startAt,
                                        endAt = endAt,
                                        allDay = allDay,
                                        accentColorHex = accentColorHex,
                                        reminderMinutesBefore = normalizedOffsets.minOrNull(),
                                        reminderOffsetsMinutes = normalizedOffsets,
                                        ringEnabled = ringEnabled,
                                        vibrateEnabled = vibrateEnabled,
                                        reminderDeliveryMode = reminderDeliveryMode,
                                        recurrence = RecurrenceConfig(
                                            enabled = true,
                                            type = recurrenceType,
                                            weeklyDays = weeklyDays,
                                            endDate = recurrenceEndDate
                                        )
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("预览循环生成")
                        }
                    }
                }

                EditorBlock(title = "日程颜色") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CalendarColorOptions.forEach { candidate ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colorFromHex(candidate))
                                    .border(
                                        width = if (accentColorHex == candidate) 3.dp else 1.dp,
                                        color = if (accentColorHex == candidate) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .clickable { accentColorHex = candidate }
                                    .padding(3.dp)
                            ) {
                                Spacer(Modifier.size(if (accentColorHex == candidate) 30.dp else 26.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normalizedOffsets = if (reminderEnabled) normalizeReminderOffsets(reminderOffsetsMinutes) else emptyList()
                    onConfirm(
                        CalendarEventDraft(
                            title = title,
                            notes = notes,
                            location = location,
                            startAt = startAt,
                            endAt = endAt,
                            allDay = allDay,
                            accentColorHex = accentColorHex,
                            reminderMinutesBefore = normalizedOffsets.minOrNull(),
                            reminderOffsetsMinutes = normalizedOffsets,
                            ringEnabled = ringEnabled,
                            vibrateEnabled = vibrateEnabled,
                            reminderDeliveryMode = reminderDeliveryMode,
                            recurrence = RecurrenceConfig(
                                enabled = recurringEnabled,
                                type = recurrenceType,
                                weeklyDays = weeklyDays,
                                endDate = recurrenceEndDate
                            )
                        )
                    )
                }
            ) {
                Text(if (initialEvent == null) "创建日程" else "保存修改")
            }
        },
        dismissButton = {
            Row {
                if (initialEvent != null) {
                    TextButton(onClick = onDelete) {
                        Text("删除")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )

    recurrencePreview?.let { preview ->
        RecurrencePreviewDialog(
            title = "循环日程预览",
            preview = preview,
            onDismiss = { recurrencePreview = null }
        )
    }

    if (showReminderPicker) {
        ReminderSelectionDialog(
            selectedOffsets = reminderOffsetsMinutes,
            anchorDateTime = if (allDay) LocalDateTime.of(startAt.toLocalDate(), LocalTime.of(9, 0)) else startAt,
            customReminderAt = reminderAt,
            onDismiss = { showReminderPicker = false },
            onPickedCustomTime = { picked ->
                reminderAt = picked
                val anchor = if (allDay) LocalDateTime.of(startAt.toLocalDate(), LocalTime.of(9, 0)) else startAt
                val offset = ((anchor.toEpochMillis() - picked.toEpochMillis()) / 60_000L).toInt().coerceAtLeast(0)
                reminderOffsetsMinutes = normalizeReminderOffsets(reminderOffsetsMinutes + offset)
            },
            onConfirm = {
                reminderOffsetsMinutes = normalizeReminderOffsets(it)
                showReminderPicker = false
            }
        )
    }

    if (showReminderDeliveryPicker) {
        SingleChoiceListDialog(
            title = "选择提醒方式",
            doneLabel = "完成",
            options = ReminderDeliveryMode.entries.map { mode ->
                ChoiceItem(
                    key = mode.name,
                    title = mode.label,
                    subtitle = when (mode) {
                        ReminderDeliveryMode.FULLSCREEN -> "直接弹出提醒界面"
                        ReminderDeliveryMode.NOTIFICATION -> "走通知栏提醒"
                    }
                )
            },
            selectedKey = reminderDeliveryMode.name,
            onSelect = { key ->
                reminderDeliveryMode = ReminderDeliveryMode.entries.first { it.name == key }
            },
            onDismiss = { showReminderDeliveryPicker = false },
            onDone = { showReminderDeliveryPicker = false }
        )
    }

    if (showRecurrencePicker) {
        RecurrenceSelectionDialog(
            recurrenceType = recurrenceType,
            weeklyDays = weeklyDays,
            startAt = startAt,
            onDismiss = { showRecurrencePicker = false },
            onSelectType = { recurrenceType = it },
            onToggleWeekday = { day ->
                weeklyDays = if (day in weeklyDays) weeklyDays - day else weeklyDays + day
            },
            onDone = { showRecurrencePicker = false }
        )
    }
}

@Composable
private fun EditorBlock(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
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
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

private data class ChoiceItem(
    val key: String,
    val title: String,
    val subtitle: String? = null
)

@Composable
private fun EditorSelectionRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SingleChoiceListDialog(
    title: String,
    doneLabel: String,
    options: List<ChoiceItem>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option.key) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(option.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            option.subtitle?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (selectedKey == option.key) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDone) { Text(doneLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun ReminderSelectionDialog(
    selectedOffsets: List<Int>,
    anchorDateTime: LocalDateTime,
    customReminderAt: LocalDateTime,
    onDismiss: () -> Unit,
    onPickedCustomTime: (LocalDateTime) -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    val context = LocalContext.current
    var workingSelection by remember(selectedOffsets) { mutableStateOf(selectedOffsets) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择提醒时间", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("日程开始时", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ReminderLeadTimeOptions.forEach { option ->
                    val selected = option.minutes in workingSelection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                workingSelection = normalizeReminderOffsets(
                                    if (selected) workingSelection - option.minutes else workingSelection + option.minutes
                                )
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(option.label, modifier = Modifier.weight(1f), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDateTimePicker(context, customReminderAt) { picked ->
                                onPickedCustomTime(picked)
                                val offset = ((anchorDateTime.toEpochMillis() - picked.toEpochMillis()) / 60_000L).toInt().coerceAtLeast(0)
                                workingSelection = normalizeReminderOffsets(workingSelection + offset)
                            }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("自定义", color = MaterialTheme.colorScheme.onSurface)
                        Text(formatLocalDateTime(customReminderAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(workingSelection) }) { Text("完成") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecurrenceSelectionDialog(
    recurrenceType: RecurrenceType,
    weeklyDays: Set<DayOfWeek>,
    startAt: LocalDateTime,
    onDismiss: () -> Unit,
    onSelectType: (RecurrenceType) -> Unit,
    onToggleWeekday: (DayOfWeek) -> Unit,
    onDone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择重复", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                recurrenceChoiceItems(startAt).forEach { item ->
                    val selected = recurrenceType == item.type
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectType(item.type) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(item.label, modifier = Modifier.weight(1f), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (recurrenceType == RecurrenceType.WEEKLY) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                    ) {
                        FlowRow(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DayOfWeek.entries.forEach { day ->
                                FilterChip(
                                    selected = day in weeklyDays,
                                    onClick = { onToggleWeekday(day) },
                                    label = { Text(day.shortLabel()) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDone) { Text("完成") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun EditorDateTimeSelectorCard(
    modifier: Modifier = Modifier,
    label: String,
    dateTime: LocalDateTime,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatClockTime(dateTime),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatEditorDateLine(dateTime.toLocalDate()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EditorDateSelectorCard(
    modifier: Modifier = Modifier,
    label: String,
    date: LocalDate,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = date.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = date.dayOfWeek.shortLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditorMiddlePill(
    modifier: Modifier = Modifier,
    label: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "至",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun reminderLeadTimeLabel(minutes: Int): String {
    return ReminderLeadTimeOptions.firstOrNull { it.minutes == minutes }?.label ?: when {
        minutes % (24 * 60) == 0 -> "提前 ${minutes / (24 * 60)} 天"
        minutes % 60 == 0 -> "提前 ${minutes / 60} 小时"
        else -> "提前 $minutes 分钟"
    }
}

private fun buildReminderMetaLabel(
    reminderDeliveryMode: ReminderDeliveryMode,
    ringEnabled: Boolean,
    vibrateEnabled: Boolean
): String {
    val toggles = buildList {
        if (ringEnabled) add("响铃")
        if (vibrateEnabled) add("震动")
        if (isEmpty()) add("静默")
    }
    return reminderDeliveryMode.label + " · " + toggles.joinToString(" + ")
}

private fun reminderSelectionSummary(reminderOffsetsMinutes: List<Int>): String {
    if (reminderOffsetsMinutes.isEmpty()) return "未选择"
    return reminderOffsetsMinutes
        .sortedDescending()
        .joinToString("、") { reminderLeadTimeLabel(it) }
}

private data class RecurrenceChoiceItem(
    val type: RecurrenceType,
    val label: String
)

private fun recurrenceChoiceItems(startAt: LocalDateTime): List<RecurrenceChoiceItem> {
    val nth = ((startAt.dayOfMonth - 1) / 7) + 1
    return listOf(
        RecurrenceChoiceItem(RecurrenceType.DAILY, "每天"),
        RecurrenceChoiceItem(RecurrenceType.WEEKLY, "每周${startAt.dayOfWeek.shortLabel().removePrefix("周")}"),
        RecurrenceChoiceItem(RecurrenceType.MONTHLY_NTH_WEEKDAY, "每月第 ${nth} 个${startAt.dayOfWeek.shortLabel()}"),
        RecurrenceChoiceItem(RecurrenceType.MONTHLY_DAY, "每月${startAt.dayOfMonth}日"),
        RecurrenceChoiceItem(RecurrenceType.YEARLY_DATE, "每年${startAt.monthValue}月${startAt.dayOfMonth}日")
    )
}

private fun recurrenceSummaryLabel(
    recurrenceType: RecurrenceType,
    startAt: LocalDateTime,
    weeklyDays: Set<DayOfWeek>
): String {
    return when (recurrenceType) {
        RecurrenceType.DAILY -> "每天重复"
        RecurrenceType.WEEKLY -> {
            val labels = weeklyDays.sortedBy { it.value }.joinToString("、") { it.shortLabel() }
            "每周 · $labels"
        }
        RecurrenceType.MONTHLY_NTH_WEEKDAY -> {
            val weekIndex = ((startAt.dayOfMonth - 1) / 7) + 1
            "每月第 ${weekIndex} 个${startAt.dayOfWeek.shortLabel()}"
        }
        RecurrenceType.MONTHLY_DAY -> "每月 ${startAt.dayOfMonth} 日"
        RecurrenceType.YEARLY_DATE -> "每年 ${startAt.monthValue} 月 ${startAt.dayOfMonth} 日"
        RecurrenceType.NONE -> "不重复"
    }
}

private fun formatTimedSpan(startAt: LocalDateTime, endAt: LocalDateTime): String {
    val minutes = Duration.between(startAt, endAt).toMinutes().coerceAtLeast(0)
    return when {
        minutes >= 24 * 60 -> "${minutes / (24 * 60)}天"
        minutes >= 60 && minutes % 60 == 0L -> "${minutes / 60}小时"
        minutes >= 60 -> "${minutes / 60}小时${minutes % 60}分"
        else -> "${minutes}分"
    }
}

private fun formatAllDaySpan(startDate: LocalDate, endDate: LocalDate): String {
    val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).coerceAtLeast(0) + 1
    return if (days <= 1) "当天" else "${days}天"
}

private fun formatEditorDateLine(date: LocalDate): String {
    val nowYear = LocalDate.now().year
    val pattern = if (date.year == nowYear) "M月d日" else "yyyy年M月d日"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.CHINA)) + " · " + date.dayOfWeek.shortLabel()
}

private fun formatClockTime(dateTime: LocalDateTime): String {
    return dateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA))
}

private fun showDateTimePicker(
    context: Context,
    initialDateTime: LocalDateTime,
    onPicked: (LocalDateTime) -> Unit
) {
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    onPicked(LocalDateTime.of(year, month + 1, day, hour, minute))
                },
                initialDateTime.hour,
                initialDateTime.minute,
                true
            ).show()
        },
        initialDateTime.year,
        initialDateTime.monthValue - 1,
        initialDateTime.dayOfMonth
    ).show()
}

private fun showDatePicker(
    context: Context,
    initialDate: LocalDate,
    onPicked: (LocalDate) -> Unit
) {
    DatePickerDialog(
        context,
        { _, year, month, day ->
            onPicked(LocalDate.of(year, month + 1, day))
        },
        initialDate.year,
        initialDate.monthValue - 1,
        initialDate.dayOfMonth
    ).show()
}

private fun DayOfWeek.shortLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "周一"
    DayOfWeek.TUESDAY -> "周二"
    DayOfWeek.WEDNESDAY -> "周三"
    DayOfWeek.THURSDAY -> "周四"
    DayOfWeek.FRIDAY -> "周五"
    DayOfWeek.SATURDAY -> "周六"
    DayOfWeek.SUNDAY -> "周日"
}
