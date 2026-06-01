package com.example.todoalarm.data

internal const val SNOOZE_DDL_GAP_MILLIS: Long = 60_000L

internal fun resolveSnoozedTodoDueAtMillis(
    currentDueAtMillis: Long,
    nowMillis: Long,
    nextReminderMillis: Long,
    minimumGapMillis: Long = SNOOZE_DDL_GAP_MILLIS
): Long {
    if (!hasDueDate(currentDueAtMillis)) return currentDueAtMillis
    return if (currentDueAtMillis <= nowMillis || currentDueAtMillis <= nextReminderMillis) {
        nextReminderMillis + minimumGapMillis
    } else {
        currentDueAtMillis
    }
}
