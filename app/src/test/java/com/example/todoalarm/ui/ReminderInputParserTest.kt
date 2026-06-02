package com.example.todoalarm.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ReminderInputParserTest {
    private val now = LocalDateTime.of(2026, 5, 17, 9, 0)
    private val currentDueAt = LocalDateTime.of(2026, 5, 20, 15, 0)

    @Test
    fun snoozeMinutesRejectsZero() {
        val parsed = parseSnoozeInput(
            raw = "0",
            now = now
        )

        assertFalse(parsed.isValid)
    }

    @Test
    fun snoozeClockRejectsPastTarget() {
        val parsed = parseSnoozeInput(
            raw = "08:30",
            now = now
        )

        assertFalse(parsed.isValid)
    }

    @Test
    fun snoozeClockComputesMinutesUntilTarget() {
        val parsed = parseSnoozeInput(
            raw = "09:45",
            now = now
        )

        assertTrue(parsed.isValid)
        assertEquals(45, parsed.minutes)
    }

    @Test
    fun ddlPostponeMinutesAddsToCurrentDueAt() {
        val parsed = parseDdlPostponeInput(
            raw = "往后推45分钟",
            currentDueAt = currentDueAt,
            now = now
        )

        assertTrue(parsed.isValid)
        assertEquals(LocalDateTime.of(2026, 5, 20, 15, 45), parsed.targetDueAt)
    }

    @Test
    fun ddlPostponeClockUsesCurrentDueDate() {
        val parsed = parseDdlPostponeInput(
            raw = "16:30",
            currentDueAt = currentDueAt,
            now = now
        )

        assertTrue(parsed.isValid)
        assertEquals(LocalDateTime.of(2026, 5, 20, 16, 30), parsed.targetDueAt)
    }

    @Test
    fun ddlPostponeRejectsTargetNotLaterThanCurrentDueAt() {
        val parsed = parseDdlPostponeInput(
            raw = "14:30",
            currentDueAt = currentDueAt,
            now = now
        )

        assertFalse(parsed.isValid)
    }

    @Test
    fun ddlPostponeRejectsNonPositiveMinutes() {
        val parsed = parseDdlPostponeInput(
            raw = "推迟0分钟",
            currentDueAt = currentDueAt,
            now = now
        )

        assertFalse(parsed.isValid)
    }

    @Test
    fun ddlPostponeParsesFullDateTimeTarget() {
        val parsed = parseDdlPostponeInput(
            raw = "2026-05-22 16:30",
            currentDueAt = currentDueAt,
            now = now
        )

        assertTrue(parsed.isValid)
        assertEquals(LocalDateTime.of(2026, 5, 22, 16, 30), parsed.targetDueAt)
    }
}
