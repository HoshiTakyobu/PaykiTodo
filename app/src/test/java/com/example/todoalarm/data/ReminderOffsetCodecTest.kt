package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderOffsetCodecTest {
    @Test
    fun eventReminderTimesMergeOrdinaryAndCheckInOffsetsWithoutDuplicates() {
        val startAt = LocalDateTime.of(2026, 8, 28, 10, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val item = eventItem(
            startAtMillis = startAt,
            reminderOffsetsCsv = "15,5",
            checkInEnabled = true,
            checkInReminderOffsetsCsv = "30,15"
        )

        assertEquals(
            listOf(startAt - 30 * 60_000L, startAt - 15 * 60_000L, startAt - 5 * 60_000L),
            item.reminderTriggerTimesMillis()
        )
    }

    @Test
    fun disabledCheckInDoesNotScheduleCheckInOffsets() {
        val startAt = 2_000_000_000_000L
        val item = eventItem(
            startAtMillis = startAt,
            reminderOffsetsCsv = "15",
            checkInEnabled = false,
            checkInReminderOffsetsCsv = "30"
        )

        assertEquals(listOf(startAt - 15 * 60_000L), item.reminderTriggerTimesMillis())
    }

    @Test
    fun onlyTodosCanBeMarkedCompleted() {
        val todo = eventItem(startAtMillis = 1L).copy(itemType = PlannerItemType.TODO.name)
        val event = eventItem(startAtMillis = 1L)

        assertTrue(todo.canBeMarkedCompleted)
        assertFalse(event.canBeMarkedCompleted)
    }

    private fun eventItem(
        startAtMillis: Long,
        reminderOffsetsCsv: String = "",
        checkInEnabled: Boolean = false,
        checkInReminderOffsetsCsv: String = ""
    ): TodoItem {
        return TodoItem(
            itemType = PlannerItemType.EVENT.name,
            title = "课程",
            dueAtMillis = startAtMillis,
            startAtMillis = startAtMillis,
            endAtMillis = startAtMillis + 60 * 60_000L,
            checkInEnabled = checkInEnabled,
            checkInReminderOffsetsCsv = checkInReminderOffsetsCsv,
            reminderAtMillis = null,
            reminderOffsetsCsv = reminderOffsetsCsv,
            reminderEnabled = reminderOffsetsCsv.isNotBlank() || checkInReminderOffsetsCsv.isNotBlank(),
            ringEnabled = true,
            vibrateEnabled = true
        )
    }
}
