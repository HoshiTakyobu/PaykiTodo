package com.example.todoalarm.data

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

internal fun normalizeReminderOffsets(offsets: List<Int>, fallback: Int? = null): List<Int> {
    val seed = if (offsets.isNotEmpty()) offsets else fallback?.let { listOf(it) }.orEmpty()
    return seed
        .asSequence()
        .map { it.coerceAtLeast(0) }
        .distinct()
        .sortedDescending()
        .toList()
}

internal fun encodeReminderOffsets(offsets: List<Int>, fallback: Int? = null): String {
    return normalizeReminderOffsets(offsets, fallback).joinToString(",")
}

internal fun decodeReminderOffsets(csv: String?, fallback: Int? = null): List<Int> {
    val parsed = csv
        .orEmpty()
        .split(',')
        .mapNotNull { token -> token.trim().toIntOrNull() }
    return normalizeReminderOffsets(parsed, fallback)
}

internal fun CalendarEventDraft.reminderTriggerTimesMillis(): List<Long> {
    val anchorMillis = calendarEpochMillis(reminderAnchorAt, eventTimeZoneId)
    val primary = if (allDay && allDayReminderRulesJson.isNotBlank())
        allDayReminderTimes(startAt.toLocalDate(), eventTimeZoneId, allDayReminderRulesJson)
    else normalizedReminderOffsetsMinutes.map { anchorMillis - it * 60_000L }
    return (primary + normalizedCheckInReminderOffsetsMinutes.map { anchorMillis - it * 60_000L } + absoluteReminderTimesMillis)
        .distinct()
        .sorted()
}

internal fun TodoItem.reminderTriggerTimesMillis(): List<Long> {
    if (!reminderEnabled) return emptyList()
    val absolute = reminderAbsoluteTimesCsv.split(',').mapNotNull { it.trim().toLongOrNull() }
    if (!isEvent) {
        val offsets = configuredReminderOffsetsMinutes
        if (hasDueDate && offsets.isNotEmpty()) {
            return (offsets
                .map { dueAtMillis - it * 60_000L }
                + absolute)
                .distinct()
                .sorted()
        }
        return (listOfNotNull(reminderAtMillis) + absolute).distinct().sorted()
    }
    val anchorMillis = eventReminderAnchorMillis()
    if (allDay && allDayReminderRulesJson.isNotBlank()) {
        val date = Instant.ofEpochMilli(startAtMillis ?: dueAtMillis).atZone(calendarZone(eventTimeZoneId)).toLocalDate()
        return (absolute + allDayReminderTimes(date, eventTimeZoneId, allDayReminderRulesJson) +
            configuredCheckInReminderOffsetsMinutes.takeIf { checkInEnabled }.orEmpty().map { anchorMillis - it * 60_000L }).distinct().sorted()
    }
    val offsets = normalizeReminderOffsets(
        configuredReminderOffsetsMinutes +
            configuredCheckInReminderOffsetsMinutes.takeIf { checkInEnabled }.orEmpty()
    )
    return (offsets
        .map { anchorMillis - it * 60_000L }
        + absolute)
        .distinct()
        .sorted()
}

internal fun TodoItem.eventReminderAnchorMillis(): Long {
    val baseMillis = startAtMillis ?: dueAtMillis
    if (!allDay) return baseMillis
    val date = Instant.ofEpochMilli(baseMillis).atZone(calendarZone(eventTimeZoneId)).toLocalDate()
    return calendarEpochMillis(java.time.LocalDateTime.of(date, LocalTime.of(9, 0)), eventTimeZoneId)
}
