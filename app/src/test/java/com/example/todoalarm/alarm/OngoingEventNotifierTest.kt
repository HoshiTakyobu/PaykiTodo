package com.example.todoalarm.alarm

import com.example.todoalarm.data.PlannerItemType
import com.example.todoalarm.data.TodoItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OngoingEventNotifierTest {
    @Test
    fun endBroadcastRefreshesWhenEventWasExtendedIntoFuture() {
        val now = 1_000_000L
        val event = eventItem(
            startAtMillis = now - 30 * 60_000L,
            endAtMillis = now + 30 * 60_000L
        )

        assertTrue(shouldRefreshOngoingEventAfterEndBroadcast(event, now))
    }

    @Test
    fun endBroadcastClearsWhenEventAlreadyEnded() {
        val now = 1_000_000L
        val event = eventItem(
            startAtMillis = now - 60 * 60_000L,
            endAtMillis = now - 10 * 60_000L
        )

        assertFalse(shouldRefreshOngoingEventAfterEndBroadcast(event, now))
    }

    @Test
    fun endBroadcastClearsHistoryEvent() {
        val now = 1_000_000L
        val event = eventItem(
            startAtMillis = now - 30 * 60_000L,
            endAtMillis = now + 30 * 60_000L,
            completed = true
        )

        assertFalse(shouldRefreshOngoingEventAfterEndBroadcast(event, now))
    }

    @Test
    fun endBroadcastClearsCanceledEvent() {
        val now = 1_000_000L
        val event = eventItem(
            startAtMillis = now - 30 * 60_000L,
            endAtMillis = now + 30 * 60_000L,
            canceled = true
        )

        assertFalse(shouldRefreshOngoingEventAfterEndBroadcast(event, now))
    }

    @Test
    fun endBroadcastClearsEventWithoutStartTime() {
        val now = 1_000_000L
        val event = eventItem(
            startAtMillis = null,
            endAtMillis = now + 30 * 60_000L
        )

        assertFalse(shouldRefreshOngoingEventAfterEndBroadcast(event, now))
    }

    @Test
    fun endBroadcastClearsNonEventItem() {
        val now = 1_000_000L
        val todo = eventItem(
            itemType = PlannerItemType.TODO.name,
            startAtMillis = now - 30 * 60_000L,
            endAtMillis = now + 30 * 60_000L
        )

        assertFalse(shouldRefreshOngoingEventAfterEndBroadcast(todo, now))
    }

    private fun eventItem(
        itemType: String = PlannerItemType.EVENT.name,
        startAtMillis: Long?,
        endAtMillis: Long?,
        completed: Boolean = false,
        canceled: Boolean = false
    ): TodoItem {
        return TodoItem(
            id = 1L,
            itemType = itemType,
            title = "测试日程",
            dueAtMillis = endAtMillis ?: startAtMillis ?: 0L,
            startAtMillis = startAtMillis,
            endAtMillis = endAtMillis,
            reminderAtMillis = null,
            reminderEnabled = false,
            ringEnabled = false,
            vibrateEnabled = false,
            completed = completed,
            canceled = canceled
        )
    }
}
