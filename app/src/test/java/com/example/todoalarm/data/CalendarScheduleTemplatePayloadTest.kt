package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class CalendarScheduleTemplatePayloadTest {
    @Test
    fun roundTripKeepsCheckInConfigurationAndReminderOffsets() {
        val weekStart = LocalDate.of(2026, 8, 24)
        val startAt = LocalDateTime.of(2026, 8, 28, 10, 20).toMillis()
        val endAt = LocalDateTime.of(2026, 8, 28, 11, 55).toMillis()
        val event = TodoItem(
            itemType = PlannerItemType.EVENT.name,
            title = "课程",
            calendarId = "courses",
            notes = "带教材",
            location = "@主楼",
            dueAtMillis = startAt,
            startAtMillis = startAt,
            endAtMillis = endAt,
            accentColorHex = "#33D6C0",
            checkInEnabled = true,
            checkInReminderOffsetsCsv = "30,5",
            reminderAtMillis = startAt - 15 * 60_000L,
            reminderOffsetsCsv = "15",
            reminderEnabled = true,
            ringEnabled = true,
            vibrateEnabled = false,
            reminderDeliveryMode = ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION.name
        )

        val restored = parseScheduleTemplatePayload(
            buildScheduleTemplatePayload(weekStart, listOf(event), emptyMap()).toJsonString()
        ).toDraftsForWeek(weekStart).single()

        assertEquals("courses", restored.calendarId)
        assertTrue(restored.checkInEnabled)
        assertEquals(listOf(30, 5), restored.normalizedCheckInReminderOffsetsMinutes)
        assertEquals(listOf(15), restored.normalizedReminderOffsetsMinutes)
        assertEquals("#33D6C0", restored.accentColorHex)
        assertEquals(ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION, restored.reminderDeliveryMode)
        assertFalse(restored.vibrateEnabled)
    }

    @Test
    fun legacyTemplateDefaultsToNoCheckInReminder() {
        val payload = parseScheduleTemplatePayload(
            """{"version":1,"weekStart":"2026-08-24","entries":[{"title":"旧日程","notes":"","location":"","groupName":"","startDayOffset":0,"startMinuteOfDay":600,"endDayOffset":0,"endMinuteOfDay":660,"allDay":false,"accentColorHex":"#336DF4","reminderMinutesBefore":15,"reminderOffsetsMinutes":[15],"ringEnabled":true,"vibrateEnabled":true,"reminderDeliveryMode":"FULLSCREEN"}]}"""
        )

        val draft = payload.toDraftsForWeek(LocalDate.of(2026, 8, 24)).single()
        assertFalse(draft.checkInEnabled)
        assertTrue(draft.normalizedCheckInReminderOffsetsMinutes.isEmpty())
    }

    private fun LocalDateTime.toMillis(): Long {
        return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
