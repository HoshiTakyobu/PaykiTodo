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
    val anchorMillis = reminderAnchorAt.toEpochMillis()
    return normalizedReminderOffsetsMinutes
        .map { anchorMillis - it * 60_000L }
        .distinct()
        .sorted()
}

internal fun TodoItem.reminderTriggerTimesMillis(): List<Long> {
    if (!reminderEnabled) return emptyList()
    if (!isEvent) {
        val offsets = configuredReminderOffsetsMinutes
        if (hasDueDate && offsets.isNotEmpty()) {
            return offsets
                .map { dueAtMillis - it * 60_000L }
                .distinct()
                .sorted()
        }
        return listOfNotNull(reminderAtMillis)
    }
    val anchorMillis = eventReminderAnchorMillis()
    return configuredReminderOffsetsMinutes
        .map { anchorMillis - it * 60_000L }
        .distinct()
        .sorted()
}

internal fun TodoItem.eventReminderAnchorMillis(): Long {
    val baseMillis = startAtMillis ?: dueAtMillis
    if (!allDay) return baseMillis
    val date = Instant.ofEpochMilli(baseMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return java.time.LocalDateTime.of(date, LocalTime.of(9, 0)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
