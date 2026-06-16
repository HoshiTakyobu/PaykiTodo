package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PlanningImportCandidateTest {
    private val now = LocalDateTime.of(2026, 6, 16, 17, 30)

    @Test
    fun pastEventWithoutReminderCanBeImported() {
        val candidate = PlanningImportCandidate(
            id = "event-1",
            lineNumber = 1,
            sourceLine = "13:00-14:00 合唱活动 @成电会堂",
            type = PlanningParsedType.EVENT,
            title = "合唱活动",
            location = "@成电会堂",
            startAt = LocalDateTime.of(2026, 6, 16, 13, 0),
            endAt = LocalDateTime.of(2026, 6, 16, 14, 0),
            reminderOffsetsMinutes = emptyList(),
            reminderEnabled = false,
            reminderInputText = ""
        )

        assertNull(candidate.validate(now))
        assertEquals(emptyList<Int>(), candidate.normalizedReminderOffsets())
    }

    @Test
    fun pastEventWithPastReminderStillRequiresAdjustment() {
        val candidate = PlanningImportCandidate(
            id = "event-2",
            lineNumber = 1,
            sourceLine = "13:00-14:00 合唱活动 @成电会堂",
            type = PlanningParsedType.EVENT,
            title = "合唱活动",
            startAt = LocalDateTime.of(2026, 6, 16, 13, 0),
            endAt = LocalDateTime.of(2026, 6, 16, 14, 0),
            reminderOffsetsMinutes = listOf(5),
            reminderEnabled = true,
            reminderInputText = "5"
        )

        assertTrue(candidate.validate(now).orEmpty().contains("提醒时间必须晚于当前时间"))
    }
}
