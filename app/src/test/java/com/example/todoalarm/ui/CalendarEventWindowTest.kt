package com.example.todoalarm.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CalendarEventWindowTest {
    @Test
    fun keepsCurrentWindowWhenRequestIsAlreadyCovered() {
        val current = CalendarEventWindow(
            startInclusive = LocalDate.of(2026, 5, 18),
            endExclusive = LocalDate.of(2026, 5, 28)
        )

        val next = nextCalendarEventWindow(
            current = current,
            requestStartInclusive = LocalDate.of(2026, 5, 20),
            requestEndExclusive = LocalDate.of(2026, 5, 23),
            paddingDays = 2
        )

        assertNull(next)
    }

    @Test
    fun createsPaddedWindowWhenRequestLeavesCurrentWindow() {
        val current = CalendarEventWindow(
            startInclusive = LocalDate.of(2026, 5, 18),
            endExclusive = LocalDate.of(2026, 5, 28)
        )

        val next = nextCalendarEventWindow(
            current = current,
            requestStartInclusive = LocalDate.of(2026, 5, 26),
            requestEndExclusive = LocalDate.of(2026, 5, 31),
            paddingDays = 2
        )

        assertEquals(
            CalendarEventWindow(
                startInclusive = LocalDate.of(2026, 5, 24),
                endExclusive = LocalDate.of(2026, 6, 2)
            ),
            next
        )
    }
}
