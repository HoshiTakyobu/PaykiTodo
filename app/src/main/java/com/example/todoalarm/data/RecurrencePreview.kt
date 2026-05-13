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
        recurringEnabled = draft.recurrence.isRecurring
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
        recurringEnabled = draft.recurrence.isRecurring
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
    recurringEnabled: Boolean
): List<LocalDate> {
    if (!recurringEnabled || type == RecurrenceType.NONE || end == null) {
        return listOf(start)
    }
    return when (type) {
        RecurrenceType.NONE -> listOf(start)
        RecurrenceType.DAILY -> generateDailyDates(start, end)
        RecurrenceType.WEEKLY -> generateWeeklyDates(start, end, weeklyDays)
        RecurrenceType.MONTHLY_NTH_WEEKDAY -> generateNthWeekdayDates(start, end)
        RecurrenceType.MONTHLY_DAY -> generateMonthlyDayDates(start, end)
        RecurrenceType.YEARLY_DATE -> generateYearlyDates(start, end)
        RecurrenceType.YEARLY_LUNAR_DATE -> generateYearlyLunarDates(start, end)
    }
}

private fun generateDailyDates(start: LocalDate, end: LocalDate): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var cursor = start
    while (!cursor.isAfter(end)) {
        dates += cursor
        cursor = cursor.plusDays(1)
    }
    return dates
}

private fun generateWeeklyDates(
    start: LocalDate,
    end: LocalDate,
    weekdays: Set<DayOfWeek>
): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var cursor = start
    while (!cursor.isAfter(end)) {
        if (cursor.dayOfWeek in weekdays) {
            dates += cursor
        }
        cursor = cursor.plusDays(1)
    }
    return dates
}

private fun generateNthWeekdayDates(start: LocalDate, end: LocalDate): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    val ordinal = start.nthWeekOrdinal()
    val weekday = start.dayOfWeek
    var cursor = YearMonth.from(start)
    val endMonth = YearMonth.from(end)
    while (!cursor.isAfter(endMonth)) {
        val occurrence = resolveNthWeekdayDate(cursor, ordinal, weekday)
        if (occurrence != null && !occurrence.isBefore(start) && !occurrence.isAfter(end)) {
            dates += occurrence
        }
        cursor = cursor.plusMonths(1)
    }
    return dates
}

private fun generateMonthlyDayDates(start: LocalDate, end: LocalDate): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    val targetDay = start.dayOfMonth
    var cursor = YearMonth.from(start)
    val endMonth = YearMonth.from(end)
    while (!cursor.isAfter(endMonth)) {
        val occurrence = resolveMonthlyDate(cursor, targetDay)
        if (!occurrence.isBefore(start) && !occurrence.isAfter(end)) {
            dates += occurrence
        }
        cursor = cursor.plusMonths(1)
    }
    return dates
}

private fun generateYearlyDates(start: LocalDate, end: LocalDate): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var year = start.year
    while (year <= end.year) {
        val candidate = resolveMonthlyDate(YearMonth.of(year, start.monthValue), start.dayOfMonth)
        if (!candidate.isBefore(start) && !candidate.isAfter(end)) {
            dates += candidate
        }
        year += 1
    }
    return dates
}

private fun generateYearlyLunarDates(start: LocalDate, end: LocalDate): List<LocalDate> {
    val dates = linkedSetOf<LocalDate>()
    var year = start.year
    while (year <= end.year + 1) {
        val candidate = LunarCalendar.sameLunarDateInYear(start, year)
        if (candidate != null && !candidate.isBefore(start) && !candidate.isAfter(end)) {
            dates += candidate
        }
        year += 1
    }
    return dates.sorted()
}
