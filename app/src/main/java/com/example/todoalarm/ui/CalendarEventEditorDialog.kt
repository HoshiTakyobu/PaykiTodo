package com.example.todoalarm.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.todoalarm.data.previewCalendarRecurrence
import com.example.todoalarm.data.storageStringToWeekdays
import com.example.todoalarm.data.toEpochMillis
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private data class ReminderLeadTimeOption(
    val minutes: Int,
    val label: String
)

private enum class ReminderTimeInputMode {
    RELATIVE,
    ABSOLUTE
}

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
        mutableStateOf(initialEvent?.reminderAtMillis != null || seedDraft?.reminderMinutesBefore != null)
    }
    var reminderMinutesBefore by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(existingReminderOffsetMinutes(initialEvent) ?: seedDraft?.reminderMinutesBefore ?: 15)
    }
    var reminderTimeInputMode by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(ReminderTimeInputMode.RELATIVE)
    }
    var reminderAt by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(
            initialEvent?.reminderAtMillis?.let(::reminderAtMillisToDateTime)
                ?: seedDraft?.let { draft ->
                    draft.reminderMinutesBefore?.let { minutes ->
                        val anchor = if (draft.allDay) {
                            LocalDateTime.of(draft.startAt.toLocalDate(), LocalTime.of(9, 0))
                        } else {
                            draft.startAt
                        }
                        anchor.minusMinutes(minutes.toLong())
                    }
                }
                ?: startAt.minusMinutes(reminderMinutesBefore.toLong())
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEvent == null) "新增日程" else "编辑日程") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("日程标题") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("地点") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("全天日程", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "开启后会显示在全天栏。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = allDay, onCheckedChange = { allDay = it })
                }

                LaunchedEffect(startAt, allDay, reminderEnabled, reminderMinutesBefore, reminderTimeInputMode) {
                    if (!reminderEnabled) return@LaunchedEffect
                    if (reminderTimeInputMode != ReminderTimeInputMode.RELATIVE) return@LaunchedEffect
                    val anchor = if (allDay) {
                        LocalDateTime.of(startAt.toLocalDate(), LocalTime.of(9, 0))
                    } else {
                        startAt
                    }
                    reminderAt = anchor.minusMinutes(reminderMinutesBefore.toLong())
                }

                if (allDay) {
                    OutlinedButton(
                        onClick = {
                            showDatePicker(context, startAt.toLocalDate()) { picked ->
                                startAt = LocalDateTime.of(picked, LocalTime.MIN)
                                if (endAt.toLocalDate().isBefore(picked)) {
                                    endAt = LocalDateTime.of(picked, LocalTime.MIN)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("开始日期：${startAt.toLocalDate()}")
                    }
                    OutlinedButton(
                        onClick = {
                            showDatePicker(context, endAt.toLocalDate()) { picked ->
                                endAt = LocalDateTime.of(picked, LocalTime.MIN)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("结束日期：${endAt.toLocalDate()}")
                    }
                } else {
                    OutlinedButton(
                        onClick = { showDateTimePicker(context, startAt) { startAt = it } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("开始时间：${formatLocalDateTime(startAt)}")
                    }
                    OutlinedButton(
                        onClick = { showDateTimePicker(context, endAt) { endAt = it } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("结束时间：${formatLocalDateTime(endAt)}")
                    }
                }

                Text("颜色", fontWeight = FontWeight.SemiBold)
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
                                    color = if (accentColorHex == candidate) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = CircleShape
                                )
                                .clickable { accentColorHex = candidate }
                                .padding(3.dp)
                        ) {
                            Spacer(Modifier.size(if (accentColorHex == candidate) 30.dp else 26.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("提醒", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (allDay) "全天日程按当天 09:00 作为提醒基准。" else "按开始时间提前提醒。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }

                if (reminderEnabled) {
                    var leadTimeExpanded by remember { mutableStateOf(false) }
                    var deliveryModeExpanded by remember { mutableStateOf(false) }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "提醒时间",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { leadTimeExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(reminderLeadTimeLabel(reminderMinutesBefore))
                            }
                            DropdownMenu(
                                expanded = leadTimeExpanded,
                                onDismissRequest = { leadTimeExpanded = false }
                            ) {
                                ReminderLeadTimeOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        onClick = {
                                            reminderTimeInputMode = ReminderTimeInputMode.RELATIVE
                                            reminderMinutesBefore = option.minutes
                                            leadTimeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                showDateTimePicker(context, reminderAt) { picked ->
                                    reminderTimeInputMode = ReminderTimeInputMode.ABSOLUTE
                                    reminderAt = picked
                                    val anchor = if (allDay) {
                                        LocalDateTime.of(startAt.toLocalDate(), LocalTime.of(9, 0))
                                    } else {
                                        startAt
                                    }
                                    reminderMinutesBefore = ((anchor.toEpochMillis() - picked.toEpochMillis()) / 60_000L)
                                        .toInt()
                                        .coerceAtLeast(0)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("直接设置提醒时刻：${formatLocalDateTime(reminderAt)}")
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "提醒方式",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { deliveryModeExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(reminderDeliveryMode.label)
                            }
                            DropdownMenu(
                                expanded = deliveryModeExpanded,
                                onDismissRequest = { deliveryModeExpanded = false }
                            ) {
                                ReminderDeliveryMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.label) },
                                        onClick = {
                                            reminderDeliveryMode = mode
                                            deliveryModeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("循环日程", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "支持每天、每周、每月和每年循环。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = recurringEnabled, onCheckedChange = { recurringEnabled = it })
                }

                if (recurringEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecurrenceType.entries.filter { it != RecurrenceType.NONE }.forEach { type ->
                            FilterChip(
                                selected = recurrenceType == type,
                                onClick = { recurrenceType = type },
                                label = { Text(type.label) }
                            )
                        }
                    }
                    if (recurrenceType == RecurrenceType.WEEKLY) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DayOfWeek.entries.forEach { day ->
                                FilterChip(
                                    selected = day in weeklyDays,
                                    onClick = {
                                        weeklyDays = if (day in weeklyDays) {
                                            weeklyDays - day
                                        } else {
                                            weeklyDays + day
                                        }
                                    },
                                    label = { Text(day.shortLabel()) }
                                )
                            }
                        }
                    }
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
                            recurrencePreview = previewCalendarRecurrence(
                                CalendarEventDraft(
                                    title = title,
                                    notes = notes,
                                    location = location,
                                    startAt = startAt,
                                    endAt = endAt,
                                    allDay = allDay,
                                    accentColorHex = accentColorHex,
                                    reminderMinutesBefore = if (reminderEnabled) reminderMinutesBefore else null,
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
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReminderMinutesBefore = if (reminderEnabled) {
                        val anchor = if (allDay) {
                            LocalDateTime.of(startAt.toLocalDate(), LocalTime.of(9, 0))
                        } else {
                            startAt
                        }
                        ((anchor.toEpochMillis() - reminderAt.toEpochMillis()) / 60_000L)
                            .toInt()
                            .coerceAtLeast(0)
                    } else {
                        null
                    }
                    onConfirm(
                        CalendarEventDraft(
                            title = title,
                            notes = notes,
                            location = location,
                            startAt = startAt,
                            endAt = endAt,
                            allDay = allDay,
                            accentColorHex = accentColorHex,
                            reminderMinutesBefore = finalReminderMinutesBefore,
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
}

private fun existingReminderOffsetMinutes(item: TodoItem?): Int? {
    item ?: return null
    val reminderAt = item.reminderAtMillis ?: return null
    val anchor = if (item.allDay) {
        LocalDateTime.of(
            reminderAtMillisToDateTime(item.startAtMillis ?: item.dueAtMillis).toLocalDate(),
            LocalTime.of(9, 0)
        )
    } else {
        reminderAtMillisToDateTime(item.startAtMillis ?: item.dueAtMillis)
    }
    return ((anchor.toEpochMillis() - reminderAt) / 60_000L).toInt().coerceAtLeast(0)
}

private fun reminderLeadTimeLabel(minutes: Int): String {
    return ReminderLeadTimeOptions.firstOrNull { it.minutes == minutes }?.label ?: when {
        minutes % (24 * 60) == 0 -> "提前 ${minutes / (24 * 60)} 天"
        minutes % 60 == 0 -> "提前 ${minutes / 60} 小时"
        else -> "提前 $minutes 分钟"
    }
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
