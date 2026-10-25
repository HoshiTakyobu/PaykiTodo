package com.example.todoalarm.ui

import com.example.todoalarm.data.GlobalSearchResult
import com.example.todoalarm.data.TodoItem
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

class CalendarSearchPresentationTest {
    private fun event(id: Long = 1, calendar: String = "default") = TodoItem(
        id = id, itemType = "EVENT", calendarId = calendar, title = "日程 $id", dueAtMillis = 0,
        reminderEnabled = false, reminderAtMillis = 1_000, reminderOffsetsCsv = "15,0",
        ringEnabled = false, vibrateEnabled = false
    )

    @Test fun revokedSearchResultsDisappearWithoutDiscardingRecoverableRecords() {
        val personal = event(1)
        val shared = event(2, "shared:allowed")
        val otherSource = event(3, "shared:other")
        val raw = GlobalSearchResult.empty("日程").copy(events = listOf(personal, shared, otherSource))
        assertEquals(listOf(personal, shared), raw.withAccessibleCalendars(setOf(shared.calendarId)).events)
        assertEquals(listOf(personal), raw.withAccessibleCalendars(emptySet()).events)
        assertEquals(listOf(personal, shared, otherSource), raw.events)
        assertEquals(listOf(personal, shared), raw.withAccessibleCalendars(setOf(shared.calendarId)).events)
    }

    @Test fun disabledReminderHidesRetainedTimedAllDayAndAbsoluteRules() {
        assertNull(eventReminderSummary(event()))
        assertNull(eventReminderSummary(event().copy(allDay = true,
            allDayReminderRulesJson = "[{\"daysBefore\":0,\"minuteOfDay\":480}]")))
    }

    @Test fun activeAllDayReminderDoesNotPresentRememberedTimedOffsetsAsActive() {
        assertEquals("全天提醒：当天 08:00", eventReminderSummary(event().copy(
            allDay = true, reminderEnabled = true,
            allDayReminderRulesJson = "[{\"daysBefore\":0,\"minuteOfDay\":480}]")))
    }

    @Test fun zeroOffsetIsTheStartTimeAndNormalReminderStillShowsItsOffset() {
        assertEquals("普通提醒：开始时", eventReminderSummary(event().copy(reminderEnabled = true, reminderOffsetsCsv = "0")))
        assertEquals("普通提醒：提前15分钟", eventReminderSummary(event().copy(reminderEnabled = true, reminderOffsetsCsv = "15")))
    }

    @Test fun allDaySearchUsesTheEventDatesAcrossTheDateLineAndExcludesTheEndDay() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val zone = ZoneId.of("Pacific/Kiritimati")
            val date = LocalDate.of(2030, 1, 1)
            val item = event().copy(allDay = true, eventTimeZoneId = zone.id,
                startAtMillis = date.atStartOfDay(zone).toInstant().toEpochMilli(),
                endAtMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
            assertEquals("2030-01-01 全天", calendarEventSearchTimeLabel(item))
            assertEquals("2030-01-01 - 2030-01-02 全天", calendarEventSearchTimeLabel(item.copy(
                endAtMillis = date.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli())))
        } finally { TimeZone.setDefault(previous) }
    }
}
