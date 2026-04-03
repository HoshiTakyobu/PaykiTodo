package com.example.todoalarm.ui

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun reminderAtMillisToDateTime(reminderAtMillis: Long): LocalDateTime {
    return Instant.ofEpochMilli(reminderAtMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
}

fun formatLocalDateTime(dateTime: LocalDateTime): String {
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA))
}
