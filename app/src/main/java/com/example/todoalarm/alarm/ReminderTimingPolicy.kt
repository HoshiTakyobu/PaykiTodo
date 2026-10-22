package com.example.todoalarm.alarm

internal object ReminderTimingPolicy {
    const val RECOVERY_WINDOW_MILLIS = 10 * 60_000L

    fun shouldSchedule(trigger: Long, now: Long, previouslyScheduled: Boolean, dispatched: Boolean): Boolean =
        !dispatched && (trigger > now || (previouslyScheduled && trigger in (now - RECOVERY_WINDOW_MILLIS)..now))
}
