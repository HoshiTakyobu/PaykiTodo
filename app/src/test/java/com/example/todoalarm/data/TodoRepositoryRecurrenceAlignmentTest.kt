package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TodoRepositoryRecurrenceAlignmentTest {
    @Test
    fun allScopeEditKeepsUserSelectedNewStartDateForRecurringTodo() {
        val aligned = alignRecurringDateForAllScope(
            editedDate = LocalDate.of(2026, 5, 25),
            originalDate = LocalDate.of(2026, 5, 26),
            seriesStartDate = LocalDate.of(2026, 5, 24)
        )

        assertEquals(LocalDate.of(2026, 5, 25), aligned)
    }

    @Test
    fun allScopeEditStillRebasesTimeOnlyChangeToSeriesStartDate() {
        val aligned = alignRecurringDateForAllScope(
            editedDate = LocalDate.of(2026, 5, 26),
            originalDate = LocalDate.of(2026, 5, 26),
            seriesStartDate = LocalDate.of(2026, 5, 24)
        )

        assertEquals(LocalDate.of(2026, 5, 24), aligned)
    }
}
