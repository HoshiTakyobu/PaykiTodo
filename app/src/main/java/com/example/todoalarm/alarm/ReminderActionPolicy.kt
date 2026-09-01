package com.example.todoalarm.alarm

import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.reminderTriggerTimesMillis

internal fun shouldOfferReminderSnooze(
    item: TodoItem,
    triggeredAtMillis: Long?
): Boolean {
    if (item.isEvent) return false
    val triggerTimes = item.reminderTriggerTimesMillis()
    if (triggerTimes.size <= 1) return true
    // If the alarm did not carry its exact trigger time, do not expose a
    // final-reminder-only action on an intermediate retry.
    val currentTrigger = triggeredAtMillis ?: return false
    val finalTrigger = triggerTimes.maxOrNull() ?: return true
    return currentTrigger >= finalTrigger
}
