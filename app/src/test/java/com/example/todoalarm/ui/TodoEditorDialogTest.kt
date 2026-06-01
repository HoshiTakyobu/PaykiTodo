package com.example.todoalarm.ui

import com.example.todoalarm.data.RecurrenceType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class TodoEditorDialogTest {
    @Test
    fun weeklyRecurrenceAutoSyncsWhenUserHasNotEditedWeekdays() {
        assertTrue(
            shouldAutoSyncTodoWeeklyDays(
                recurringEnabled = true,
                recurrenceType = RecurrenceType.WEEKLY,
                weeklyDaysManuallyEdited = false
            )
        )
    }

    @Test
    fun weeklyRecurrenceDoesNotAutoSyncAfterManualWeekdayEdit() {
        assertFalse(
            shouldAutoSyncTodoWeeklyDays(
                recurringEnabled = true,
                recurrenceType = RecurrenceType.WEEKLY,
                weeklyDaysManuallyEdited = true
            )
        )
    }

    @Test
    fun nonWeeklyRecurrenceDoesNotAutoSyncWeekdays() {
        assertFalse(
            shouldAutoSyncTodoWeeklyDays(
                recurringEnabled = true,
                recurrenceType = RecurrenceType.DAILY,
                weeklyDaysManuallyEdited = false
            )
        )
    }

    @Test
    fun disabledRecurrenceDoesNotAutoSyncWeekdays() {
        assertFalse(
            shouldAutoSyncTodoWeeklyDays(
                recurringEnabled = false,
                recurrenceType = RecurrenceType.WEEKLY,
                weeklyDaysManuallyEdited = false
            )
        )
    }

    @Test
    fun existingWeeklyTodoWithOnlyDueWeekdayKeepsAutoSyncEnabled() {
        assertFalse(
            initialTodoWeeklyDaysManuallyEdited(
                isRecurring = true,
                recurrenceType = RecurrenceType.WEEKLY,
                storedWeeklyDays = setOf(DayOfWeek.MONDAY),
                dueDayOfWeek = DayOfWeek.MONDAY
            )
        )
    }

    @Test
    fun existingWeeklyTodoWithDifferentStoredWeekdayIsManualRule() {
        assertTrue(
            initialTodoWeeklyDaysManuallyEdited(
                isRecurring = true,
                recurrenceType = RecurrenceType.WEEKLY,
                storedWeeklyDays = setOf(DayOfWeek.WEDNESDAY),
                dueDayOfWeek = DayOfWeek.MONDAY
            )
        )
    }

    @Test
    fun existingWeeklyTodoWithMultipleStoredWeekdaysIsManualRule() {
        assertTrue(
            initialTodoWeeklyDaysManuallyEdited(
                isRecurring = true,
                recurrenceType = RecurrenceType.WEEKLY,
                storedWeeklyDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                dueDayOfWeek = DayOfWeek.MONDAY
            )
        )
    }
}
