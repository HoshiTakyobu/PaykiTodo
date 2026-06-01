package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSnoozePolicyTest {
    @Test
    fun snoozingOverdueTodoPushesDueAfterNextReminder() {
        val now = 1_000_000L
        val overdueDueAt = now - 10_000L
        val nextReminder = now + 5 * 60_000L

        val resolved = resolveSnoozedTodoDueAtMillis(
            currentDueAtMillis = overdueDueAt,
            nowMillis = now,
            nextReminderMillis = nextReminder
        )

        assertEquals(nextReminder + SNOOZE_DDL_GAP_MILLIS, resolved)
    }

    @Test
    fun snoozingTodoWithDueBeforeNextReminderPushesDueAfterReminder() {
        val now = 1_000_000L
        val dueBeforeReminder = now + 2 * 60_000L
        val nextReminder = now + 5 * 60_000L

        val resolved = resolveSnoozedTodoDueAtMillis(
            currentDueAtMillis = dueBeforeReminder,
            nowMillis = now,
            nextReminderMillis = nextReminder
        )

        assertEquals(nextReminder + SNOOZE_DDL_GAP_MILLIS, resolved)
    }

    @Test
    fun snoozingTodoWithLaterDueKeepsExistingDue() {
        val now = 1_000_000L
        val laterDueAt = now + 30 * 60_000L
        val nextReminder = now + 5 * 60_000L

        val resolved = resolveSnoozedTodoDueAtMillis(
            currentDueAtMillis = laterDueAt,
            nowMillis = now,
            nextReminderMillis = nextReminder
        )

        assertEquals(laterDueAt, resolved)
    }

    @Test
    fun snoozingNoDueTodoDoesNotInventDueDate() {
        val now = 1_000_000L
        val nextReminder = now + 5 * 60_000L

        val resolved = resolveSnoozedTodoDueAtMillis(
            currentDueAtMillis = NO_DUE_DATE_MILLIS,
            nowMillis = now,
            nextReminderMillis = nextReminder
        )

        assertEquals(NO_DUE_DATE_MILLIS, resolved)
    }
}
