package com.example.todoalarm.ui

import com.example.todoalarm.data.PlannerItemType
import com.example.todoalarm.data.RecurrenceType
import com.example.todoalarm.data.ReminderDeliveryMode
import com.example.todoalarm.data.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
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
        assertFalse(draft.recurrence.enabled)
        assertTrue(draft.recurrence.weeklyDays.isEmpty())
        assertEquals(LocalDateTime.of(2026, 8, 28, 10, 20), draft.startAt)
        assertNull(draft.multiSlotBundleId)
    }

    @Test
    fun allDayCopyPreservesZoneAndInclusiveDatesAndExplicitReminders() {
        val zone = ZoneId.of("America/New_York")
        val start = LocalDateTime.of(2026, 11, 1, 0, 0)
        val end = LocalDateTime.of(2026, 11, 3, 0, 0)
        val event = TodoItem(id = 100, itemType = PlannerItemType.EVENT.name, title = "两天日程", reminderAtMillis = null,
            reminderEnabled = true, ringEnabled = true, vibrateEnabled = true,
            dueAtMillis = start.atZone(zone).toInstant().toEpochMilli(), startAtMillis = start.atZone(zone).toInstant().toEpochMilli(),
            endAtMillis = end.atZone(zone).toInstant().toEpochMilli(), allDay = true, eventTimeZoneId = zone.id,
            allDayReminderRulesJson = "[{\"daysBefore\":1,\"minuteOfDay\":497}]", eventBusyStatus = "FREE", eventVisibility = "PRIVATE")
        val draft = event.toCalendarCopyDraft()
        assertEquals(start, draft.startAt)
        assertEquals(end.minusDays(1), draft.endAt)
        assertEquals(zone.id, draft.eventTimeZoneId)
        assertEquals(event.allDayReminderRulesJson, draft.allDayReminderRulesJson)
        assertEquals("FREE", draft.eventBusyStatus)
        assertEquals("PRIVATE", draft.eventVisibility)
        assertFalse(draft.recurrence.enabled)
    }

    private fun LocalDateTime.toMillis(): Long {
        return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
