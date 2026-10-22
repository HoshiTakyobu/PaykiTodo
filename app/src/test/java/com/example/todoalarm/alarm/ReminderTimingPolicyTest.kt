package com.example.todoalarm.alarm

import org.junit.Assert.*
import org.junit.Test

class ReminderTimingPolicyTest {
    private val now = 2_000_000L

    @Test fun historicalEntriesNeverScheduleEvenWithinRecoveryWindow() {
        assertFalse(ReminderTimingPolicy.shouldSchedule(now - 1, now, false, false))
        assertFalse(ReminderTimingPolicy.shouldSchedule(now, now, false, false))
        assertTrue(ReminderTimingPolicy.shouldSchedule(now + 1, now, false, false))
    }

    @Test fun onlyPreviouslyScheduledUndeliveredRecentAlarmsRecover() {
        assertTrue(ReminderTimingPolicy.shouldSchedule(now - 600_000, now, true, false))
        assertFalse(ReminderTimingPolicy.shouldSchedule(now - 600_001, now, true, false))
        assertFalse(ReminderTimingPolicy.shouldSchedule(now - 1000, now, true, true))
        assertFalse(ReminderTimingPolicy.shouldSchedule(now + 1000, now, true, true))
    }
}
