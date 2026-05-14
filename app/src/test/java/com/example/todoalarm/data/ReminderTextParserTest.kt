package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ReminderTextParserTest {
    private val now = LocalDateTime.of(2026, 5, 14, 8, 0)
    private val anchor = LocalDateTime.of(2026, 5, 20, 18, 0)

    @Test
    fun parsesMixedReminderTextWithNaturalDates() {
        val parsed = parseReminderTextInput(
            raw = "5,15,2:30 pm,明天 16:30,周五 16:30,5.20 15:00,5月20日 15:30",
            anchor = anchor,
            now = now,
            requireFuture = false
        )

        assertTrue(parsed.isValid)
        assertEquals(listOf(7290, 210, 180, 150, 15, 5), parsed.offsetsMinutes)
    }

    @Test
    fun rejectsReminderAfterAnchor() {
        val parsed = parseReminderTextInput(
            raw = "5月20日 19:00",
            anchor = anchor,
            now = now,
            requireFuture = false
        )

        assertFalse(parsed.isValid)
    }

    @Test
    fun treatsCommaBetweenDateAndTimeAsOneReminderToken() {
        val parsed = parseReminderTextInput(
            raw = "5,5.20,15:00,5月20日，15:30",
            anchor = anchor,
            now = now,
            requireFuture = false
        )

        assertTrue(parsed.isValid)
        assertEquals(listOf(180, 150, 5), parsed.offsetsMinutes)
    }

    @Test
    fun parsesNaturalSnoozeTarget() {
        val parsed = parseSnoozeTextInput(raw = "明天 16:30", now = now)

        assertTrue(parsed.isValid)
        assertEquals(1950, parsed.minutes)
    }

    @Test
    fun parsesSlashFullwidthAndChinesePeriodReminderTimes() {
        val parsed = parseReminderTextInput(
            raw = "5/20 下午 2:30,5．20，15:30",
            anchor = anchor,
            now = now,
            requireFuture = false
        )

        assertTrue(parsed.isValid)
        assertEquals(listOf(210, 150), parsed.offsetsMinutes)
    }

    @Test
    fun parsesChinesePeriodSnoozeTarget() {
        val parsed = parseSnoozeTextInput(raw = "上午 9:30", now = now)

        assertTrue(parsed.isValid)
        assertEquals(90, parsed.minutes)
    }
}
