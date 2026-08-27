package com.example.todoalarm.alarm

import com.example.todoalarm.data.TodoItem

internal fun shouldUseFullscreenReminder(
    item: TodoItem
): Boolean {
    return item.reminderEnabled && item.reminderDeliveryModeEnum.usesFullscreen
}
