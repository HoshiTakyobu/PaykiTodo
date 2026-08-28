package com.example.todoalarm.ui

import com.example.todoalarm.data.PlannerItemType
import com.example.todoalarm.data.RecurrenceType
import com.example.todoalarm.data.ReminderDeliveryMode
import com.example.todoalarm.data.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

class CalendarCopyDraftTest {
    @Test
    fun copyDraftKeepsEditableFieldsButDropsMultiSlotIdentity() {
        val startAt = LocalDateTime.of(2026, 8, 28, 10, 20).toMillis()
        val event = TodoItem(
            id = 99,
            itemType = PlannerItemType.EVENT.name,
            title = "课程",
            notes = "备注",
            location = "@主楼",
            dueAtMillis = startAt,
            startAtMillis = startAt,
            endAtMillis = startAt + 95 * 60_000L,
            accentColorHex = "#4752E6",
            countdownEnabled = true,
            checkInEnabled = true,
            checkInReminderOffsetsCsv = "30",
            reminderAtMillis = startAt - 15 * 60_000L,
            reminderOffsetsCsv = "15",
            reminderEnabled = true,
            ringEnabled = true,
            vibrateEnabled = false,
            reminderDeliveryMode = ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION.name,
            groupId = 7,
            recurringSeriesId = "series-1",
            multiSlotBundleId = "bundle-1",
            recurrenceType = RecurrenceType.WEEKLY.name,
            recurrenceWeekdays = DayOfWeek.FRIDAY.value.toString()
        )

        val draft = event.toCalendarCopyDraft()

        assertEquals(event.title, draft.title)
        assertEquals(event.location, draft.location)
        assertEquals(listOf(15), draft.normalizedReminderOffsetsMinutes)
        assertEquals(listOf(30), draft.normalizedCheckInReminderOffsetsMinutes)
        assertTrue(draft.recurrence.enabled)
        assertEquals(setOf(DayOfWeek.FRIDAY), draft.recurrence.weeklyDays)
        assertNull(draft.multiSlotBundleId)
    }

    private fun LocalDateTime.toMillis(): Long {
        return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
