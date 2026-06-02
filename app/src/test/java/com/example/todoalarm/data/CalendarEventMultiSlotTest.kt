package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CalendarEventMultiSlotTest {
    @Test
    fun buildsOneWeeklyRecurringDraftPerTimeSlot() {
        val baseDraft = baseDraft()
        val recurrenceEnd = LocalDate.of(2026, 9, 1)

        val drafts = buildWeeklyMultiSlotEventDrafts(
            baseDraft = baseDraft,
            baseDate = LocalDate.of(2026, 5, 18),
            recurrenceEndDate = recurrenceEnd,
            slots = listOf(
                CalendarEventTimeSlot(DayOfWeek.TUESDAY, LocalTime.of(10, 20), LocalTime.of(11, 55)),
                CalendarEventTimeSlot(DayOfWeek.THURSDAY, LocalTime.of(8, 30), LocalTime.of(10, 5))
            )
        )

        assertEquals(2, drafts.size)
        assertEquals(LocalDateTime.of(2026, 5, 19, 10, 20), drafts[0].startAt)
        assertEquals(LocalDateTime.of(2026, 5, 19, 11, 55), drafts[0].endAt)
        assertEquals(setOf(DayOfWeek.TUESDAY), drafts[0].recurrence.weeklyDays)
        assertEquals(LocalDateTime.of(2026, 5, 21, 8, 30), drafts[1].startAt)
        assertEquals(LocalDateTime.of(2026, 5, 21, 10, 5), drafts[1].endAt)
        assertEquals(setOf(DayOfWeek.THURSDAY), drafts[1].recurrence.weeklyDays)
        drafts.forEach { draft ->
            assertTrue(draft.recurrence.enabled)
            assertEquals(RecurrenceType.WEEKLY, draft.recurrence.type)
            assertEquals(recurrenceEnd, draft.recurrence.endDate)
            assertFalse(draft.allDay)
            assertEquals(baseDraft.title, draft.title)
            assertEquals(baseDraft.location, draft.location)
        }
    }

    @Test
    fun computesReminderOffsetsForEachSlotAnchor() {
        val drafts = buildWeeklyMultiSlotEventDrafts(
            baseDraft = baseDraft(),
            baseDate = LocalDate.of(2026, 5, 18),
            recurrenceEndDate = LocalDate.of(2026, 9, 1),
            slots = listOf(
                CalendarEventTimeSlot(DayOfWeek.TUESDAY, LocalTime.of(10, 20), LocalTime.of(11, 55)),
                CalendarEventTimeSlot(DayOfWeek.THURSDAY, LocalTime.of(8, 30), LocalTime.of(10, 5))
            )
        ) { slot, _ ->
            if (slot.weekday == DayOfWeek.TUESDAY) listOf(15) else listOf(30, 5)
        }

        assertEquals(listOf(15), drafts[0].normalizedReminderOffsetsMinutes)
        assertEquals(listOf(30, 5), drafts[1].normalizedReminderOffsetsMinutes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyTimeSlots() {
        buildWeeklyMultiSlotEventDrafts(
            baseDraft = baseDraft(),
            baseDate = LocalDate.of(2026, 5, 18),
            recurrenceEndDate = LocalDate.of(2026, 9, 1),
            slots = emptyList()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTimeSlotEndingBeforeStart() {
        buildWeeklyMultiSlotEventDrafts(
            baseDraft = baseDraft(),
            baseDate = LocalDate.of(2026, 5, 18),
            recurrenceEndDate = LocalDate.of(2026, 9, 1),
            slots = listOf(CalendarEventTimeSlot(DayOfWeek.TUESDAY, LocalTime.of(11, 55), LocalTime.of(10, 20)))
        )
    }

    private fun baseDraft(): CalendarEventDraft {
        return CalendarEventDraft(
            title = "习思想",
            notes = "课程备注",
            location = "@主楼B1-412",
            startAt = LocalDateTime.of(2026, 5, 18, 9, 0),
            endAt = LocalDateTime.of(2026, 5, 18, 10, 0),
            allDay = false,
            accentColorHex = "#4E87E1",
            reminderMinutesBefore = 15,
            reminderOffsetsMinutes = listOf(15),
            ringEnabled = true,
            vibrateEnabled = true,
            recurrence = RecurrenceConfig()
        )
    }
}
