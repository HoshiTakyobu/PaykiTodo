package com.example.todoalarm.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.RecurrenceConfig
import com.example.todoalarm.data.RecurrencePreviewResult
import com.example.todoalarm.data.RecurrenceType
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.TodoDraft
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.previewTodoRecurrence
import com.example.todoalarm.data.storageStringToWeekdays
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodoEditorDialog(
    initialTodo: TodoItem?,
    groups: List<TaskGroup>,
    defaultRingEnabled: Boolean,
    defaultVibrateEnabled: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: (TodoDraft) -> Unit
) {
    val context = LocalContext.current
    val now = remember { LocalDateTime.now().withSecond(0).withNano(0) }
    val isHistory = initialTodo?.isHistory == true
    val isRecurringSeries = initialTodo?.isRecurring == true
    val canDisableDueDate = !isRecurringSeries

    var title by remember(initialTodo?.id) { mutableStateOf(initialTodo?.title.orEmpty()) }
    var notes by remember(initialTodo?.id) { mutableStateOf(initialTodo?.notes.orEmpty()) }
    val defaultGroupId = remember(groups) {
        groups.firstOrNull { it.name == "例行" }?.id ?: groups.firstOrNull()?.id ?: 0L
    }
    var groupId by remember(initialTodo?.id, groups) {
        mutableStateOf(initialTodo?.groupId?.takeIf { it > 0 } ?: defaultGroupId)
    }
    var hasDueDate by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.hasDueDate ?: true)
    }
    var dueAt by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.dueDateTimeOrNull() ?: now.plusHours(2))
    }
    var reminderEnabled by remember(initialTodo?.id) {
        mutableStateOf(
            if (isHistory || initialTodo?.hasDueDate == false) {
                false
            } else {
                initialTodo?.reminderEnabled ?: true
            }
        )
    }
    var reminderAt by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.reminderAtMillis?.let(::reminderAtMillisToDateTime) ?: dueAt)
    }
    var ringEnabled by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.ringEnabled ?: defaultRingEnabled)
    }
    var vibrateEnabled by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.vibrateEnabled ?: defaultVibrateEnabled)
    }
    var recurringEnabled by remember(initialTodo?.id) { mutableStateOf(initialTodo?.isRecurring == true) }
    var recurrenceType by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.recurrenceTypeEnum ?: RecurrenceType.DAILY)
    }
    var weeklyDays by remember(initialTodo?.id) {
        mutableStateOf(
            if (initialTodo?.isRecurring == true) {
                storageStringToWeekdays(initialTodo.recurrenceWeekdays).ifEmpty { setOf(dueAt.dayOfWeek) }
            } else {
                setOf(dueAt.dayOfWeek)
            }
        )
    }
    var recurrenceEndDate by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.recurrenceEndDate ?: dueAt.toLocalDate().plusDays(90))
    }
    var recurrencePreview by remember { mutableStateOf<RecurrencePreviewResult?>(null) }
    var showGroupPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (initialTodo == null) "新增任务" else "编辑任务",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "标题、DDL、提醒、循环、分组与备注都集中在这里。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TodoEditorBlock(title = "标题") {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("添加标题") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TodoEditorBlock(title = "分组") {
                    TodoSelectionRow(
                        title = "所属分组",
                        value = groups.firstOrNull { it.id == groupId }?.name ?: "未分组",
                        onClick = { showGroupPicker = true }
                    )
                }

                TodoEditorBlock(title = "截止时间") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("设置 DDL", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = if (hasDueDate) {
                                    "当前任务会按截止时间排序。"
                                } else {
                                    "未设置 DDL 的任务会排在计划中末尾。"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = hasDueDate,
                            enabled = canDisableDueDate,
                            onCheckedChange = { checked ->
                                hasDueDate = checked
                                if (!checked) {
                                    reminderEnabled = false
                                    recurringEnabled = false
                                }
                            },
                            thumbContent = null
                        )
                    }

                    if (hasDueDate) {
                        TodoDateTimeCard(
                            label = "DDL",
                            dateTime = dueAt,
                            onClick = {
                                showDateTimePicker(context, dueAt) { picked ->
                                    dueAt = picked
                                    if (reminderEnabled && initialTodo == null) {
                                        reminderAt = picked
                                    }
                                }
                            }
                        )
                    }
                }

                TodoEditorBlock(title = "备注") {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("添加备注") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                }

                if (!canDisableDueDate) {
                    Text(
                        text = "循环任务必须保留 DDL，不能移除截止时间。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (hasDueDate) {
                } else {
                    Text(
                        text = "当前任务不设置截止日期，也不会启用提醒或循环。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isHistory) {
                    Text(
                        text = "历史任务允许修改标题、备注、分类和 DDL，但不会重新启用提醒。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isHistory && hasDueDate) {
                    TodoEditorBlock(title = "提醒") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("提醒", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text("建议在 DDL 之前提醒。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = {
                                    reminderEnabled = it
                                    if (it && initialTodo == null) {
                                        reminderAt = dueAt
                                    }
                                },
                                thumbContent = null
                            )
                        }

                        if (reminderEnabled) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.24f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text("提醒时刻", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(formatLocalDateTime(reminderAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            TodoSelectionRow(
                                title = "提醒时间",
                                value = formatLocalDateTime(reminderAt),
                                onClick = { showDateTimePicker(context, reminderAt) { reminderAt = it } }
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

                    TodoEditorBlock(title = "循环") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("循环", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (recurringEnabled) recurrenceType.label else "不重复", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = recurringEnabled,
                                onCheckedChange = { recurringEnabled = it },
                                thumbContent = null
                            )
                        }

                        if (recurringEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RecurrenceType.entries
                                    .filter { it != RecurrenceType.NONE }
                                    .forEach { type ->
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
                                                weeklyDays = if (day in weeklyDays) weeklyDays - day else weeklyDays + day
                                            },
                                            label = { Text(day.shortLabel()) }
                                        )
                                    }
                                }
                            }

                            recurrenceHint(
                                recurrenceType = recurrenceType,
                                dueAt = dueAt,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (recurrenceType == RecurrenceType.MONTHLY_DAY && dueAt.dayOfMonth > 28) {
                                Text(
                                    text = "（若某个月不存在 ${dueAt.dayOfMonth} 日，则自动落到该月最后一天）",
                                    color = Color(0xFFC62828),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            TodoSelectionRow(
                                title = "循环截止日期",
                                value = recurrenceEndDate.toString(),
                                onClick = { showDatePicker(context, recurrenceEndDate) { recurrenceEndDate = it } }
                            )
                            OutlinedButton(
                                onClick = {
                                    recurrencePreview = previewTodoRecurrence(
                                        TodoDraft(
                                            title = title,
                                            notes = notes,
                                            dueAt = dueAt,
                                            reminderAt = if (reminderEnabled) reminderAt else null,
                                            groupId = groupId,
                                            ringEnabled = ringEnabled,
                                            vibrateEnabled = vibrateEnabled,
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
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        TodoDraft(
                            title = title,
                            notes = notes,
                            dueAt = dueAt.takeIf { hasDueDate },
                            reminderAt = if (isHistory || !hasDueDate || !reminderEnabled) null else reminderAt,
                            groupId = groupId,
                            ringEnabled = ringEnabled,
                            vibrateEnabled = vibrateEnabled,
                            recurrence = RecurrenceConfig(
                                enabled = !isHistory && hasDueDate && recurringEnabled,
                                type = recurrenceType,
                                weeklyDays = weeklyDays,
                                endDate = recurrenceEndDate
                            )
                        )
                    )
                }
            ) {
                Text(if (initialTodo == null) "创建任务" else "保存修改")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initialTodo != null) {
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
            title = "循环任务预览",
            preview = preview,
            onDismiss = { recurrencePreview = null }
        )
    }

    if (showGroupPicker) {
        AlertDialog(
            onDismissRequest = { showGroupPicker = false },
            title = { Text("选择分组", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    groups.forEach { item ->
                        TodoSelectionRow(
                            title = item.name,
                            value = if (groupId == item.id) "当前分组" else "",
                            onClick = {
                                groupId = item.id
                                showGroupPicker = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGroupPicker = false }) { Text("关闭") }
            },
            dismissButton = {}
        )
    }
}

@Composable
private fun TodoEditorBlock(
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

@Composable
private fun TodoSelectionRow(
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
                if (value.isNotBlank()) {
                    Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
private fun TodoDateTimeCard(
    label: String,
    dateTime: LocalDateTime,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                text = formatLocalDateTime(dateTime).takeLast(5),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = todoEditorDateLine(dateTime.toLocalDate()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun recurrenceHint(
    recurrenceType: RecurrenceType,
    dueAt: LocalDateTime,
    modifier: Modifier = Modifier
) {
    val hint = when (recurrenceType) {
        RecurrenceType.NONE -> null
        RecurrenceType.DAILY -> "将从 ${dueAt.toLocalDate()} 起按每天 ${formatLocalDateTime(dueAt).takeLast(5)} 生成。"
        RecurrenceType.WEEKLY -> "将按选中的周几、在 ${formatLocalDateTime(dueAt).takeLast(5)} 生成。"
        RecurrenceType.MONTHLY_NTH_WEEKDAY -> "将按每月第 ${(dueAt.dayOfMonth - 1) / 7 + 1} 个${dueAt.dayOfWeek.shortLabel()}生成。"
        RecurrenceType.MONTHLY_DAY -> "将按每月 ${dueAt.dayOfMonth} 日生成。"
        RecurrenceType.YEARLY_DATE -> "将按每年 ${dueAt.monthValue} 月 ${dueAt.dayOfMonth} 日生成。"
    } ?: return

    Text(
        text = hint,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall
    )
}

private fun todoEditorDateLine(date: LocalDate): String {
    val nowYear = LocalDate.now().year
    val pattern = if (date.year == nowYear) "M月d日" else "yyyy年M月d日"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.CHINA)) + " · " + date.dayOfWeek.shortLabel()
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
