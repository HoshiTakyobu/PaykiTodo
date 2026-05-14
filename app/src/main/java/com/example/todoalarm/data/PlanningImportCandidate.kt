package com.example.todoalarm.data

import java.time.LocalDateTime

data class PlanningImportCandidate(
    val id: String,
    val lineNumber: Int,
    val sourceLine: String,
    val type: PlanningParsedType,
    val title: String = "",
    val notes: String = "",
    val groupName: String = "",
    val dueAt: LocalDateTime? = null,
    val startAt: LocalDateTime? = null,
    val endAt: LocalDateTime? = null,
    val reminderOffsetsMinutes: List<Int> = listOf(DEFAULT_PLANNING_REMINDER_MINUTES),
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
        val offsets = normalizedReminderOffsets()
        return when (type) {
            PlanningParsedType.TODO -> {
                if (dueAt != null) {
                    if (!dueAt.isAfter(now)) return "DDL 必须晚于当前时间"
                    if (offsets.any { !dueAt.minusMinutes(it.toLong()).isAfter(now) }) return "提醒时间必须晚于当前时间"
                } else if (offsets.isNotEmpty()) {
                    return "未设置 DDL 的待办不能设置提醒"
                }
                null
            }
            PlanningParsedType.EVENT -> {
                val start = startAt ?: return "日程开始时间不能为空"
                val end = endAt ?: return "日程结束时间不能为空"
                if (!end.isAfter(start)) return "日程结束时间必须晚于开始时间"
                if (offsets.any { !start.minusMinutes(it.toLong()).isAfter(now) }) return "提醒时间必须晚于当前时间"
                null
            }
            else -> "该条目不可导入"
        }
    }

    fun normalizedReminderOffsets(): List<Int> {
        if (type == PlanningParsedType.TODO && dueAt == null) return emptyList()
        return reminderOffsetsMinutes
            .map { it.coerceAtLeast(0) }
            .distinct()
            .sortedDescending()
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
        groupName = groupName,
        dueAt = dueAt,
        startAt = startAt,
        endAt = endAt,
        reminderOffsetsMinutes = reminderOffsetsMinutes,
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
        groupName = groupName,
        dueAt = dueAt,
        startAt = startAt,
        endAt = endAt,
        reminderOffsetsMinutes = normalizedReminderOffsets(),
        createLinkedTodo = createLinkedTodo,
        defaultToday = defaultToday,
        imported = imported,
        completed = completed,
        importBlocked = validate() != null,
        parentTitle = parentTitle,
        message = validate().orEmpty()
    )
}
