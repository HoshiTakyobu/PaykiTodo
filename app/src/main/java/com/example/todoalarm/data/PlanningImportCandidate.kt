package com.example.todoalarm.data

import java.time.LocalDateTime

data class PlanningImportCandidate(
    val id: String,
    val lineNumber: Int,
    val sourceLine: String,
    val type: PlanningParsedType,
    val title: String = "",
    val notes: String = "",
    val location: String = "",
    val groupName: String = "",
    val dueAt: LocalDateTime? = null,
    val startAt: LocalDateTime? = null,
    val endAt: LocalDateTime? = null,
    val allDay: Boolean = false,
    val countdownEnabled: Boolean = false,
    val checkInEnabled: Boolean = false,
    val reminderOffsetsMinutes: List<Int> = listOf(DEFAULT_PLANNING_REMINDER_MINUTES),
    val reminderEnabled: Boolean = reminderOffsetsMinutes.isNotEmpty(),
    val reminderInputText: String = "",
    val reminderInputError: String = "",
    val reminderDeliveryMode: ReminderDeliveryMode = ReminderDeliveryMode.FULLSCREEN,
    val recurrence: RecurrenceConfig = RecurrenceConfig(),
    val createLinkedTodo: Boolean = false,
    val defaultToday: Boolean = false,
    val imported: Boolean = false,
    val completed: Boolean = false,
    val importBlocked: Boolean = false,
    val parentTitle: String? = null,
    val message: String = ""
) {
    val importable: Boolean
        get() = (type == PlanningParsedType.TODO || type == PlanningParsedType.EVENT) && !imported && !completed

    fun validate(now: LocalDateTime = LocalDateTime.now()): String? {
        if (!importable) return message.takeIf { it.isNotBlank() } ?: "该条目不可导入"
        if (title.isBlank()) return "标题不能为空"
        if (reminderInputError.isNotBlank()) return reminderInputError
        val offsets = normalizedReminderOffsets()
        return when (type) {
            PlanningParsedType.TODO -> {
                if (dueAt != null) {
                    if (!dueAt.isAfter(now)) return "DDL 必须晚于当前时间"
                    if (offsets.any { !dueAt.minusMinutes(it.toLong()).isAfter(now) }) return "提醒时间必须晚于当前时间"
                } else if (reminderEnabled) {
                    return "未设置 DDL 的待办不能设置提醒"
                }
                recurrenceError(anchor = dueAt, label = "循环待办")?.let { return it }
                if (countdownEnabled && dueAt == null) return "无 DDL 待办不能启用倒数日"
                null
            }
            PlanningParsedType.EVENT -> {
                val start = startAt ?: return "日程开始时间不能为空"
                val end = endAt ?: return "日程结束时间不能为空"
                if (!end.isAfter(start)) return "日程结束时间必须晚于开始时间"
                if (offsets.any { !start.minusMinutes(it.toLong()).isAfter(now) }) return "提醒时间必须晚于当前时间"
                recurrenceError(anchor = start, label = "循环日程")?.let { return it }
                null
            }
            else -> "该条目不可导入"
        }
    }

    fun normalizedReminderOffsets(): List<Int> {
        if (!reminderEnabled) return emptyList()
        if (type == PlanningParsedType.TODO && dueAt == null) return emptyList()
        return reminderOffsetsMinutes
            .map { it.coerceAtLeast(0) }
            .distinct()
            .sortedDescending()
    }

    private fun recurrenceError(anchor: LocalDateTime?, label: String): String? {
        if (!recurrence.enabled) return null
        val start = anchor ?: return "$label 必须先设置时间"
        if (recurrence.type == RecurrenceType.NONE) return "请选择循环规则"
        val endDate = recurrence.endDate ?: return "请设置循环截止日期"
        if (endDate.isBefore(start.toLocalDate())) return "循环截止日期不能早于首次日期"
        if (recurrence.type == RecurrenceType.WEEKLY && recurrence.weeklyDays.isEmpty()) return "每周循环至少选择一天"
        return null
    }
}

fun PlanningParsedCandidate.toPlanningImportCandidate(): PlanningImportCandidate {
    return PlanningImportCandidate(
        id = id,
        lineNumber = lineNumber,
        sourceLine = sourceLine,
        type = type,
        title = title,
        notes = notes,
        location = location,
        groupName = groupName,
        dueAt = dueAt,
        startAt = startAt,
        endAt = endAt,
        allDay = allDay,
        countdownEnabled = countdownEnabled,
        checkInEnabled = checkInEnabled,
        reminderEnabled = reminderOffsetsMinutes.isNotEmpty(),
        reminderOffsetsMinutes = reminderOffsetsMinutes,
        reminderInputText = reminderOffsetsMinutes.joinToString(","),
        reminderDeliveryMode = ReminderDeliveryMode.FULLSCREEN,
        recurrence = recurrence,
        createLinkedTodo = createLinkedTodo,
        defaultToday = defaultToday,
        imported = imported,
        completed = completed,
        importBlocked = importBlocked,
        parentTitle = parentTitle,
        message = message
    )
}

fun PlanningImportCandidate.toParsedCandidate(): PlanningParsedCandidate {
    return PlanningParsedCandidate(
        id = id,
        lineNumber = lineNumber,
        sourceLine = sourceLine,
        type = type,
        title = title,
        notes = notes,
        location = location,
        groupName = groupName,
        dueAt = dueAt,
        startAt = startAt,
        endAt = endAt,
        allDay = allDay,
        countdownEnabled = countdownEnabled,
        checkInEnabled = checkInEnabled,
        reminderOffsetsMinutes = normalizedReminderOffsets(),
        recurrence = recurrence,
        createLinkedTodo = createLinkedTodo,
        defaultToday = defaultToday,
        imported = imported,
        completed = completed,
        importBlocked = validate() != null,
        parentTitle = parentTitle,
        message = validate().orEmpty()
    )
}
