package com.example.todoalarm.alarm

import com.example.todoalarm.data.ReminderDeliveryMode
import com.example.todoalarm.data.TodoItem

internal fun shouldUseFullscreenReminder(
    item: TodoItem,
    workQuietModeEnabled: Boolean
): Boolean {
    return workQuietModeEnabled ||
        item.alarmMode ||
        item.reminderDeliveryModeEnum == ReminderDeliveryMode.FULLSCREEN
}
