package com.example.todoalarm.alarm

import com.example.todoalarm.data.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CalendarNotificationDateTest {
    @Test fun allDayNotificationKeepsTheEventDateAcrossTheDateLine() {
        val zone = ZoneId.of("Pacific/Kiritimati")
        val start = LocalDate.of(2030, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.of(2030, 1, 3).atStartOfDay(zone).toInstant().toEpochMilli()
        val item = TodoItem(id = 1, itemType = "EVENT", title = "全天", dueAtMillis = start,
            startAtMillis = start, endAtMillis = end, allDay = true, eventTimeZoneId = zone.id,
            reminderAtMillis = null, reminderEnabled = false, ringEnabled = false, vibrateEnabled = false)
        assertEquals("2030-01-01 - 2030-01-02", calendarEventNotificationDateLabel(item))
    }

    @Test fun oneAllDayDateHasNoTimeAndNoExclusiveEndDateInItsLabel() {
        val zone = ZoneId.of("America/New_York")
        val start = LocalDate.of(2030, 3, 10).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.of(2030, 3, 11).atStartOfDay(zone).toInstant().toEpochMilli()
        val item = TodoItem(id = 2, itemType = "EVENT", title = "全天", dueAtMillis = start,
            startAtMillis = start, endAtMillis = end, allDay = true, eventTimeZoneId = zone.id,
            reminderAtMillis = null, reminderEnabled = false, ringEnabled = false, vibrateEnabled = false)
        assertEquals("2030-03-10", calendarEventNotificationDateLabel(item))
    }
}
