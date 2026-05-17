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
}
