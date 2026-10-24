package com.example.todoalarm.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

data class RecurrencePreviewOccurrence(
    val startAt: LocalDateTime,
    val endAt: LocalDateTime? = null
)

data class RecurrencePreviewResult(
    val totalCount: Int,
    val occurrences: List<RecurrencePreviewOccurrence>
)

fun previewTodoRecurrence(
    draft: TodoDraft,
    limit: Int = 20
): RecurrencePreviewResult {
    val dueAt = draft.dueAt ?: return RecurrencePreviewResult(0, emptyList())
    val dates = generatePreviewDates(
        start = dueAt.toLocalDate(),
        end = draft.recurrence.endDate,
        type = draft.recurrence.type,
        weeklyDays = draft.recurrence.weeklyDays.ifEmpty { setOf(dueAt.dayOfWeek) },
        recurringEnabled = draft.recurrence.isRecurring,
        config = draft.recurrence
    )
    val occurrences = dates.take(limit).map { date ->
        RecurrencePreviewOccurrence(startAt = LocalDateTime.of(date, dueAt.toLocalTime()))
    }
    return RecurrencePreviewResult(totalCount = dates.size, occurrences = occurrences)
}

fun previewCalendarRecurrence(
    draft: CalendarEventDraft,
    limit: Int = 20
): RecurrencePreviewResult {
    val duration = java.time.Duration.between(draft.startAt, draft.endAt).toMinutes().coerceAtLeast(0)
    val dates = generatePreviewDates(
        start = draft.startAt.toLocalDate(),
        end = draft.recurrence.endDate,
        type = draft.recurrence.type,
        weeklyDays = draft.recurrence.weeklyDays.ifEmpty { setOf(draft.startAt.dayOfWeek) },
        recurringEnabled = draft.recurrence.isRecurring,
        config = draft.recurrence
    )
    val occurrences = dates.take(limit).map { date ->
        val startAt = LocalDateTime.of(date, draft.startAt.toLocalTime())
        RecurrencePreviewOccurrence(startAt = startAt, endAt = startAt.plusMinutes(duration))
    }
    return RecurrencePreviewResult(totalCount = dates.size, occurrences = occurrences)
}

private fun generatePreviewDates(
    start: LocalDate,
    end: LocalDate?,
    type: RecurrenceType,
    weeklyDays: Set<DayOfWeek>,
    recurringEnabled: Boolean,
    config: RecurrenceConfig
): List<LocalDate> = calendarRecurrenceDates(start, config, rangeEnd = end ?: start.plusDays(90))
