package com.example.todoalarm.ui

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.todoalarm.data.LunarCalendar
import com.example.todoalarm.data.RecurrenceConfig
import com.example.todoalarm.data.RecurrencePreviewResult
import com.example.todoalarm.data.RecurrenceType
import com.example.todoalarm.data.ReminderDeliveryMode
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
    initialGroupIds: List<Long> = emptyList(),
    defaultRingEnabled: Boolean,
    defaultVibrateEnabled: Boolean,
    onDismiss: () -> Unit,
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
    var selectedGroupIds by remember(initialTodo?.id, initialGroupIds, groups) {
        val validGroupIds = groups.map { it.id }.toSet()
        val fallback = initialGroupIds.ifEmpty {
            listOf(initialTodo?.groupId?.takeIf { it > 0 } ?: defaultGroupId)
        }
        mutableStateOf(
            fallback
                .filter { it > 0 && it in validGroupIds }
                .distinct()
                .ifEmpty { listOf(defaultGroupId).filter { it > 0 } }
        )
    }
    val primaryGroupId = selectedGroupIds.firstOrNull() ?: defaultGroupId
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
    var reminderInput by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.configuredReminderOffsetsMinutes?.takeIf { it.isNotEmpty() }?.joinToString(",") ?: "5")
    }
    var ringEnabled by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.ringEnabled ?: defaultRingEnabled)
    }
    var vibrateEnabled by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.vibrateEnabled ?: defaultVibrateEnabled)
    }
    var alarmMode by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.alarmMode == true)
    }
    var reminderDeliveryMode by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.reminderDeliveryModeEnum ?: ReminderDeliveryMode.FULLSCREEN)
    }
    var countdownEnabled by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.countdownEnabled == true)
    }
    var hiddenFromBoard by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.hiddenFromBoard == true)
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
    var weeklyDaysManuallyEdited by remember(initialTodo?.id) {
        mutableStateOf(
            initialTodoWeeklyDaysManuallyEdited(
                isRecurring = initialTodo?.isRecurring == true,
                recurrenceType = initialTodo?.recurrenceTypeEnum ?: RecurrenceType.NONE,
                storedWeeklyDays = initialTodo?.let { storageStringToWeekdays(it.recurrenceWeekdays) }.orEmpty(),
                dueDayOfWeek = dueAt.dayOfWeek
            )
        )
    }
    var recurrenceEndDate by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.recurrenceEndDate ?: dueAt.toLocalDate().plusDays(90))
    }
    var recurrencePreview by remember { mutableStateOf<RecurrencePreviewResult?>(null) }
    var showReminderDeliveryPicker by remember { mutableStateOf(false) }
    var showRecurrenceTypePicker by remember { mutableStateOf(false) }
    var helpTopic by remember { mutableStateOf<InputSyntaxHelpTopic?>(null) }
    var activeDateTimeTarget by remember { mutableStateOf<TodoDateTimeTarget?>(null) }
    var showDueLunarPicker by remember { mutableStateOf(false) }
    val reminderValidation = remember(reminderInput, dueAt, reminderEnabled, hasDueDate, isHistory, initialTodo?.id) {
        if (!reminderEnabled || !hasDueDate || isHistory) {
            ReminderInputValidation(isValid = true)
        } else {
            parseReminderInput(
                raw = reminderInput,
                anchor = dueAt,
                requireFuture = initialTodo == null
            )
        }
    }
    val confirmEnabled = title.isNotBlank() && (!reminderEnabled || !hasDueDate || isHistory || reminderValidation.isValid)
    LaunchedEffect(dueAt, recurrenceType, recurringEnabled, weeklyDaysManuallyEdited) {
        if (shouldAutoSyncTodoWeeklyDays(recurringEnabled, recurrenceType, weeklyDaysManuallyEdited)) {
            weeklyDays = setOf(dueAt.dayOfWeek)
        }
    }
    val shouldAutoExpandMore = remember(initialTodo?.id, defaultRingEnabled, defaultVibrateEnabled) {
        initialTodo != null && (
            initialTodo.notes.isNotBlank() ||
                initialTodo.configuredReminderOffsetsMinutes.joinToString(",").ifBlank { "5" } != "5" ||
                initialTodo.reminderDeliveryModeEnum != ReminderDeliveryMode.FULLSCREEN ||
                initialTodo.recurrenceTypeEnum != RecurrenceType.NONE ||
                initialTodo.hiddenFromBoard ||
                initialTodo.alarmMode ||
                initialTodo.ringEnabled != defaultRingEnabled ||
                initialTodo.vibrateEnabled != defaultVibrateEnabled
            )
    }
    var moreOptionsExpanded by remember(initialTodo?.id, shouldAutoExpandMore) { mutableStateOf(shouldAutoExpandMore) }
    val hasUnsavedChanges = remember(
        initialTodo?.id,
        title,
        notes,
        selectedGroupIds,
        hasDueDate,
        dueAt,
        reminderEnabled,
        reminderInput,
        ringEnabled,
        vibrateEnabled,
        alarmMode,
        reminderDeliveryMode,
        countdownEnabled,
        hiddenFromBoard,
        recurringEnabled,
        recurrenceType,
        weeklyDays,
        recurrenceEndDate
    ) {
        if (initialTodo == null) {
            title.isNotBlank() ||
                notes.isNotBlank() ||
                selectedGroupIds.isNotEmpty() ||
                hasDueDate ||
                reminderInput != "5" ||
                recurringEnabled ||
                alarmMode ||
                countdownEnabled ||
                hiddenFromBoard
        } else {
            title != initialTodo.title ||
                notes != initialTodo.notes ||
                hasDueDate != initialTodo.hasDueDate ||
                dueAt != (initialTodo.dueDateTimeOrNull() ?: dueAt) ||
                reminderEnabled != initialTodo.reminderEnabled ||
                reminderInput != initialTodo.configuredReminderOffsetsMinutes.joinToString(",").ifBlank { "5" } ||
                ringEnabled != initialTodo.ringEnabled ||
                vibrateEnabled != initialTodo.vibrateEnabled ||
                alarmMode != initialTodo.alarmMode ||
                reminderDeliveryMode != initialTodo.reminderDeliveryModeEnum ||
                countdownEnabled != initialTodo.countdownEnabled ||
                hiddenFromBoard != initialTodo.hiddenFromBoard ||
                recurringEnabled != initialTodo.isRecurring ||
                recurrenceType != initialTodo.recurrenceTypeEnum ||
                weeklyDays != storageStringToWeekdays(initialTodo.recurrenceWeekdays) ||
                recurrenceEndDate != (initialTodo.recurrenceEndDate ?: recurrenceEndDate)
        }
    }

    EditorBottomSheet(
        title = if (initialTodo == null) "新增任务" else "编辑任务",
        confirmLabel = if (initialTodo == null) "创建" else "保存",
        confirmEnabled = confirmEnabled,
        onDismiss = onDismiss,
        hasUnsavedChanges = hasUnsavedChanges,
        onConfirm = {
            val parsedReminderTimes = if (isHistory || !hasDueDate || !reminderEnabled || !reminderValidation.isValid) {
                emptyList()
            } else {
                reminderValidation.triggerTimes
            }
            val parsedReminderOffsets = if (isHistory || !hasDueDate || !reminderEnabled || !reminderValidation.isValid) {
                emptyList()
            } else {
                reminderValidation.offsetsMinutes
            }
            onConfirm(
                TodoDraft(
                    title = title,
                    notes = notes,
                    dueAt = dueAt.takeIf { hasDueDate },
                    reminderAt = parsedReminderTimes.minOrNull(),
                    groupId = primaryGroupId,
                    ringEnabled = ringEnabled,
                    vibrateEnabled = vibrateEnabled,
                    alarmMode = alarmMode && hasDueDate && reminderEnabled && !isHistory,
                    reminderDeliveryMode = reminderDeliveryMode,
                    countdownEnabled = countdownEnabled && hasDueDate,
                    hiddenFromBoard = hiddenFromBoard && hasDueDate,
                    recurrence = RecurrenceConfig(
                        enabled = !isHistory && hasDueDate && recurringEnabled,
                        type = recurrenceType,
                        weeklyDays = weeklyDays,
                        endDate = recurrenceEndDate
                    ),
                    reminderOffsetsMinutes = parsedReminderOffsets,
                    groupIds = selectedGroupIds
                )
            )
        }
    ) {
                TodoEditorBlock(title = "标题") {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("添加标题") },
                        modifier = Modifier.fillMaxWidth()
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
                        }
                        Switch(
                            checked = hasDueDate,
                            enabled = canDisableDueDate,
                            onCheckedChange = { checked ->
                                hasDueDate = checked
                                if (!checked) {
                                    reminderEnabled = false
                                    recurringEnabled = false
                                    countdownEnabled = false
                                    hiddenFromBoard = false
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
                                activeDateTimeTarget = TodoDateTimeTarget.DueAt
                            }
                        )
                        OutlinedButton(
                            onClick = { showDueLunarPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("农历 DDL") }
                    }
                }

                TodoEditorBlock(title = "分组") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "可多选；第一个选中的分组作为主分组和颜色来源。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            groups.sortedWith(compareBy<TaskGroup> { it.sortOrder }.thenBy { it.id }).forEach { group ->
                                FilterChip(
                                    selected = group.id in selectedGroupIds,
                                    onClick = {
                                        selectedGroupIds = toggleTodoEditorGroup(
                                            selected = selectedGroupIds,
                                            groupId = group.id,
                                            fallbackGroupId = defaultGroupId
                                        )
                                    },
                                    label = { Text(group.name) }
                                )
                            }
                        }
                    }
                }

                if (hasDueDate) {
                    TodoEditorBlock(title = "倒数日") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("在每日看板显示倒数日", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text("适合考试、报名截止、论文提交等关键目标。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = countdownEnabled,
                                onCheckedChange = { countdownEnabled = it },
                                thumbContent = null
                            )
                        }
                    }
                }

                MoreOptionsToggle(
                    expanded = moreOptionsExpanded,
                    onToggle = { moreOptionsExpanded = !moreOptionsExpanded }
                )

                AnimatedVisibility(
                    visible = moreOptionsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

                        if (isHistory) {
                            Text(
                                text = "历史任务允许修改标题、备注、分类和 DDL，但不会重新启用提醒。",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!isHistory && hasDueDate) {
                            TodoEditorBlock(title = "看板显示") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("仅提醒，不在看板/日历显示", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("适合吃药、记账这类只需要到点提醒、不想占用每日看板空间的循环任务。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Switch(
                                        checked = hiddenFromBoard,
                                        onCheckedChange = { hiddenFromBoard = it },
                                        thumbContent = null
                                    )
                                }
                            }

                            TodoEditorBlock(title = "提醒") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("提醒", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Switch(
                                        checked = reminderEnabled,
                                        onCheckedChange = {
                                            reminderEnabled = it
                                            if (it && initialTodo == null) {
                                                reminderAt = dueAt
                                                if (reminderInput.isBlank()) reminderInput = "5"
                                            }
                                        },
                                        thumbContent = null
                                    )
                                }

                                if (reminderEnabled) {
                                    TodoSelectionRow(
                                        title = "提醒方式",
                                        value = reminderDeliveryMode.label,
                                        onClick = { showReminderDeliveryPicker = true }
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("闹钟模式（持续响铃直到操作）", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                            Text(
                                                "适合吃药、重要 DDL 等绝对不能漏的提醒；5 分钟后会降级为通知并间歇重试。",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Switch(
                                            checked = alarmMode,
                                            onCheckedChange = { alarmMode = it },
                                            thumbContent = null
                                        )
                                    }
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
                                            Text("提醒时间", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                            Text(
                                                text = if (reminderValidation.isValid) reminderValidation.message else "输入存在非法项",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (reminderValidation.isValid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    if (!reminderValidation.isValid) {
                                        Text(
                                            text = "非法输入：${reminderValidation.message}",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    OutlinedTextField(
                                        value = reminderInput,
                                        onValueChange = { reminderInput = it },
                                        label = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("提醒时间")
                                                InputSyntaxHelpIconButton(
                                                    topic = InputSyntaxHelpTopic.Reminder,
                                                    onClick = { helpTopic = InputSyntaxHelpTopic.Reminder }
                                                )
                                            }
                                        },
                                        placeholder = { Text("例：5（提前5分钟）或 5,15,16:30") },
                                        isError = !reminderValidation.isValid,
                                        supportingText = {
                                            Text("数字=提前分钟；HH:mm=当天时刻；MM-DD HH:mm=当年；YYYY-MM-DD HH:mm=完整时刻。用英文逗号分隔。")
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = false,
                                        minLines = 1,
                                        maxLines = 3
                                    )
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
                                    TodoSelectionRow(
                                        title = "重复规则",
                                        value = recurrenceType.label,
                                        onClick = { showRecurrenceTypePicker = true }
                                    )

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
                                                        weeklyDaysManuallyEdited = true
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
                                                    reminderAt = if (reminderEnabled && reminderValidation.isValid) reminderValidation.triggerTimes.minOrNull() else null,
                                                    groupId = primaryGroupId,
                                                    ringEnabled = ringEnabled,
                                                    vibrateEnabled = vibrateEnabled,
                                                    alarmMode = alarmMode && reminderEnabled && hasDueDate && !isHistory,
                                                    reminderDeliveryMode = reminderDeliveryMode,
                                                    hiddenFromBoard = hiddenFromBoard && hasDueDate,
                                                    recurrence = RecurrenceConfig(
                                                        enabled = true,
                                                        type = recurrenceType,
                                                        weeklyDays = weeklyDays,
                                                        endDate = recurrenceEndDate
                                                    ),
                                                    reminderOffsetsMinutes = if (reminderEnabled && reminderValidation.isValid) reminderValidation.offsetsMinutes else emptyList(),
                                                    groupIds = selectedGroupIds
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
                }
    }

    recurrencePreview?.let { preview ->
        RecurrencePreviewDialog(
            title = "循环任务预览",
            preview = preview,
            onDismiss = { recurrencePreview = null }
        )
    }

    if (showReminderDeliveryPicker) {
        TodoSingleChoiceListDialog(
            title = "选择提醒方式",
            doneLabel = "完成",
            options = ReminderDeliveryMode.entries.map { mode ->
                TodoChoiceItem(
                    key = mode.name,
                    title = mode.label,
                    subtitle = when (mode) {
                        ReminderDeliveryMode.FULLSCREEN -> "到点直接弹出全屏提醒界面"
                        ReminderDeliveryMode.NOTIFICATION -> "只走通知栏提醒，不主动弹出全屏界面"
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

    if (showRecurrenceTypePicker) {
        TodoSingleChoiceListDialog(
            title = "选择重复规则",
            doneLabel = "完成",
            options = RecurrenceType.entries
                .filter { it != RecurrenceType.NONE }
                .map { type ->
                    TodoChoiceItem(
                        key = type.name,
                        title = type.label,
                        subtitle = recurrenceTypeHint(type, dueAt)
                    )
                },
            selectedKey = recurrenceType.name,
            onSelect = { key ->
                recurrenceType = RecurrenceType.entries.first { it.name == key }
            },
            onDismiss = { showRecurrenceTypePicker = false },
            onDone = { showRecurrenceTypePicker = false }
        )
    }

    helpTopic?.let { topic ->
        InputSyntaxHelpDialog(topic = topic, onDismiss = { helpTopic = null })
    }

    if (showDueLunarPicker) {
        LunarDatePickerDialog(
            title = "选择农历 DDL",
            initialDate = dueAt.toLocalDate(),
            onDismiss = { showDueLunarPicker = false },
            onConfirm = { pickedDate ->
                val pickedDue = LocalDateTime.of(pickedDate, dueAt.toLocalTime())
                dueAt = pickedDue
                if (reminderEnabled && initialTodo == null) {
                    reminderAt = pickedDue
                }
                showDueLunarPicker = false
            }
        )
    }
    activeDateTimeTarget?.let { target ->
        WheelDateTimePickerDialog(
            title = when (target) {
                TodoDateTimeTarget.DueAt -> "选择 DDL"
                TodoDateTimeTarget.ReminderAt -> "选择提醒时间"
            },
            initialDateTime = when (target) {
                TodoDateTimeTarget.DueAt -> dueAt
                TodoDateTimeTarget.ReminderAt -> reminderAt
            },
            onDismiss = { activeDateTimeTarget = null },
            onConfirm = { picked ->
                when (target) {
                    TodoDateTimeTarget.DueAt -> {
                        dueAt = picked
                        if (reminderEnabled && initialTodo == null) {
                            reminderAt = picked
                        }
                    }

                    TodoDateTimeTarget.ReminderAt -> {
                        reminderAt = picked
                    }
                }
                activeDateTimeTarget = null
            }
        )
    }
}

@Composable
private fun MoreOptionsToggle(
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "更多选项",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "收起更多选项" else "展开更多选项",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private enum class TodoDateTimeTarget {
    DueAt,
    ReminderAt
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
                style = MaterialTheme.typography.titleLarge,
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

private data class TodoChoiceItem(
    val key: String,
    val title: String,
    val subtitle: String? = null
)

@Composable
private fun TodoSingleChoiceListDialog(
    title: String,
    doneLabel: String,
    options: List<TodoChoiceItem>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatLocalDateTime(dateTime).takeLast(5),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = todoEditorDateLine(dateTime.toLocalDate()),
                style = MaterialTheme.typography.labelMedium,
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
        RecurrenceType.YEARLY_LUNAR_DATE -> "将按每年农历 ${LunarCalendar.labelFor(dueAt.toLocalDate()).displayText} 生成。"
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
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.CHINA)) + "（农历${LunarCalendar.labelFor(date).displayText}） · " + date.dayOfWeek.shortLabel()
}

private fun recurrenceTypeHint(
    recurrenceType: RecurrenceType,
    dueAt: LocalDateTime
): String? {
    return when (recurrenceType) {
        RecurrenceType.NONE -> null
        RecurrenceType.DAILY -> "从 ${dueAt.toLocalDate()} 起，每天 ${formatLocalDateTime(dueAt).takeLast(5)}"
        RecurrenceType.WEEKLY -> "按选中的周几，在 ${formatLocalDateTime(dueAt).takeLast(5)} 生成"
        RecurrenceType.MONTHLY_NTH_WEEKDAY -> "每月第 ${(dueAt.dayOfMonth - 1) / 7 + 1} 个${dueAt.dayOfWeek.shortLabel()}"
        RecurrenceType.MONTHLY_DAY -> "每月 ${dueAt.dayOfMonth} 日"
        RecurrenceType.YEARLY_DATE -> "每年 ${dueAt.monthValue} 月 ${dueAt.dayOfMonth} 日"
        RecurrenceType.YEARLY_LUNAR_DATE -> "每年农历 ${LunarCalendar.labelFor(dueAt.toLocalDate()).displayText}"
    }
}

private fun toggleTodoEditorGroup(
    selected: List<Long>,
    groupId: Long,
    fallbackGroupId: Long
): List<Long> {
    val next = if (groupId in selected) {
        selected - groupId
    } else {
        selected + groupId
    }
    return next
        .filter { it > 0 }
        .distinct()
        .ifEmpty { listOf(fallbackGroupId).filter { it > 0 } }
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

internal fun shouldAutoSyncTodoWeeklyDays(
    recurringEnabled: Boolean,
    recurrenceType: RecurrenceType,
    weeklyDaysManuallyEdited: Boolean
): Boolean {
    return recurringEnabled && recurrenceType == RecurrenceType.WEEKLY && !weeklyDaysManuallyEdited
}

internal fun initialTodoWeeklyDaysManuallyEdited(
    isRecurring: Boolean,
    recurrenceType: RecurrenceType,
    storedWeeklyDays: Set<DayOfWeek>,
    dueDayOfWeek: DayOfWeek
): Boolean {
    if (!isRecurring || recurrenceType != RecurrenceType.WEEKLY || storedWeeklyDays.isEmpty()) {
        return false
    }
    return storedWeeklyDays != setOf(dueDayOfWeek)
}
