package com.example.todoalarm.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FeedbackDataTest {
    private val today = LocalDate.of(2026, 10, 23)
    private fun time(day: LocalDate) = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun item(daysAgo: Long) = TodoItem(id = 81, title = "纪念日", dueAtMillis = time(today.minusDays(daysAgo)),
        reminderAtMillis = null, reminderEnabled = false, ringEnabled = false, vibrateEnabled = false, countdownEnabled = true)

    @Test fun historicalCountdownBoundaryAndCompletionChoiceAreIndependentOfTaskStatus() {
        val now = time(today) + 12 * 3600000
        assertTrue(CountdownVisibility.visible(item(7), now))
        assertFalse(CountdownVisibility.visible(item(8), now))
        val completed = item(90).copy(completed = true, completedAtMillis = now)
        assertTrue(CountdownVisibility.needsDecision(completed, now))
        assertFalse(CountdownVisibility.visible(completed, now))
        val kept = completed.copy(countdownPastMode = "KEEP")
        assertTrue(CountdownVisibility.visible(kept, now))
        assertTrue(kept.completed)
        assertFalse(CountdownVisibility.needsDecision(kept, now))
        assertFalse(CountdownVisibility.visible(kept.copy(canceled = true), now))
        assertFalse(CountdownVisibility.visible(kept.copy(countdownPastMode = "HIDE"), now))
        assertEquals("已过", DailyBoardSnapshotBuilder.countdownRemainingDisplay(kept, today.atTime(12, 0))!!.secondary)
    }

    @Test fun endedEventPromptsAtEndRatherThanAtStart() {
        val now = time(today) + 12 * 3600000
        val event = item(0).copy(itemType = "EVENT", startAtMillis = now - 60000, endAtMillis = now + 60000)
        assertFalse(CountdownVisibility.needsDecision(event, now))
        assertTrue(CountdownVisibility.needsDecision(event, now + 60000))
    }

    @Test fun calendarAndMemorialChoiceSurviveBackupAndChangeDataFingerprint() {
        val source = BackupSnapshot(exportedAtMillis = 1, groups = DefaultTaskGroups.seed, templates = emptyList(),
            tasks = listOf(item(10).copy(calendarId = "courses", countdownPastMode = "KEEP", completed = true)),
            calendars = listOf(PlannerCalendar(), PlannerCalendar("courses", "课程表", "#7955CC", 1)),
            settings = AppSettings(countdownPastDays = 14, hiddenCalendarIds = setOf("courses"), mainFont = "SARASA", desktopFontFamily = "微软雅黑", desktopFontSize = 20))
        val restored = BackupArchiveCodec.decode(BackupArchiveCodec.encode(source))
        assertEquals(source.tasks, restored.tasks)
        assertEquals(source.calendars, restored.calendars)
        assertEquals(source.settings.countdownPastDays, restored.settings.countdownPastDays)
        assertEquals(source.settings.hiddenCalendarIds, restored.settings.hiddenCalendarIds)
        assertEquals(source.settings.desktopFontFamily, restored.settings.desktopFontFamily)
        assertEquals(20, restored.settings.desktopFontSize)
        assertNotEquals(BackupArchiveCodec.dataFingerprint(source), BackupArchiveCodec.dataFingerprint(source.copy(calendars = listOf(PlannerCalendar()))))
        val legacy = source.toJson().apply { remove("calendars"); getJSONArray("tasks").getJSONObject(0).remove("calendarId"); getJSONArray("tasks").getJSONObject(0).remove("countdownPastMode") }
        val old = backupSnapshotFromJson(legacy)
        assertEquals("default", old.tasks.single().calendarId)
        assertEquals("AUTO", old.tasks.single().countdownPastMode)
        assertEquals(listOf(PlannerCalendar()), old.calendars)
    }
}
