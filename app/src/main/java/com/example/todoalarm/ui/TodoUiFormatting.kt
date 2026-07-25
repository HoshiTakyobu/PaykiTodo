package com.example.todoalarm.ui

import com.example.todoalarm.data.TodoItem
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

fun formatEventDateTimeRange(item: TodoItem): String {
    val start = item.startAtMillis?.let(::reminderAtMillisToDateTime) ?: return "未设置时间"
    val end = item.endAtMillis?.let(::reminderAtMillisToDateTime) ?: start
    if (item.allDay) {
        val endDate = end.toLocalDate().minusDays(1).takeIf { it >= start.toLocalDate() } ?: start.toLocalDate()
        return if (start.toLocalDate() == endDate) {
            "${start.monthValue}月${start.dayOfMonth}日 全天"
        } else {
            "${start.monthValue}月${start.dayOfMonth}日 - ${endDate.monthValue}月${endDate.dayOfMonth}日 全天"
        }
    }
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    return if (start.toLocalDate() == end.toLocalDate()) {
        "${start.monthValue}月${start.dayOfMonth}日 ${start.format(timeFormatter)} - ${end.format(timeFormatter)}"
    } else {
        "${formatLocalDateTime(start)} - ${formatLocalDateTime(end)}"
    }
}
