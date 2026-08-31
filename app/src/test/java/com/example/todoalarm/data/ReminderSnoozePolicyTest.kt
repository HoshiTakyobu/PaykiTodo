package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSnoozePolicyTest {
    @Test
    fun snoozingOverdueTodoKeepsOriginalDueTime() {
        val overdueDueAt = 990_000L

        val resolved = resolveSnoozedTodoDueAtMillis(overdueDueAt)

        assertEquals(overdueDueAt, resolved)
    }

    @Test
    fun snoozingFutureTodoKeepsOriginalDueTime() {
        val laterDueAt = 2_800_000L

        val resolved = resolveSnoozedTodoDueAtMillis(laterDueAt)

        assertEquals(laterDueAt, resolved)
    }

    @Test
    fun snoozingNoDueTodoDoesNotInventDueDate() {
        val resolved = resolveSnoozedTodoDueAtMillis(NO_DUE_DATE_MILLIS)

        assertEquals(NO_DUE_DATE_MILLIS, resolved)
    }
}
