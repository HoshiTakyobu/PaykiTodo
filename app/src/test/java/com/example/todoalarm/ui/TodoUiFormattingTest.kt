package com.example.todoalarm.ui

import com.example.todoalarm.data.PlannerItemType
import com.example.todoalarm.data.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TodoUiFormattingTest {
    @Test
    fun formatEventDateTimeRangeShowsSameDayStartAndEnd() {
        val start = LocalDateTime.of(2026, 7, 25, 16, 0)
        val end = LocalDateTime.of(2026, 7, 25, 17, 30)

        assertEquals(
            "7月25日 16:00 - 17:30",
            formatEventDateTimeRange(event(start, end))
        )
    }

    @Test
    fun formatEventDateTimeRangeUsesInclusiveDatesForAllDayEvent() {
        val start = LocalDateTime.of(2026, 7, 25, 0, 0)
        val endExclusive = LocalDateTime.of(2026, 7, 28, 0, 0)

        assertEquals(
            "7月25日 - 7月27日 全天",
            formatEventDateTimeRange(event(start, endExclusive, allDay = true))
        )
    }

    private fun event(
        start: LocalDateTime,
        end: LocalDateTime,
        allDay: Boolean = false
    ): TodoItem {
        val zone = ZoneId.systemDefault()
        val startMillis = start.atZone(zone).toInstant().toEpochMilli()
        return TodoItem(
            itemType = PlannerItemType.EVENT.name,
            title = "test event",
            dueAtMillis = startMillis,
            startAtMillis = startMillis,
            endAtMillis = end.atZone(zone).toInstant().toEpochMilli(),
            allDay = allDay,
            reminderAtMillis = null,
            reminderEnabled = false,
            ringEnabled = false,
            vibrateEnabled = false
        )
    }
}
