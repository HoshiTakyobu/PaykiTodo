package com.example.todoalarm.sync

import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.TodoItem

/** Use at UI/notification boundaries; raw repository reads remain available for cache recovery. */
internal suspend fun TodoApplication.canReadCalendarItem(item: TodoItem): Boolean {
    if (!settingsStore.hasLocalAccountAccess()) return false
    if (!item.calendarId.startsWith("shared:")) return true
    val calendar = database.todoDao().getCalendars().firstOrNull { it.id == item.calendarId } ?: return false
    return calendarCollaboration.canReadCachedCalendar(calendar)
}
