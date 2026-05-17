package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DailyBoardSnapshotBuilderTest {
    private val now = LocalDateTime.of(2026, 5, 14, 8, 0)

    @Test
    fun noDdlTodosAreShownAsTodayTodos() {
        val todayWithDdl = todo(
            id = 1,
            title = "今天有 DDL",
            dueAt = LocalDateTime.of(2026, 5, 14, 18, 0)
        )
        val noDdl = todo(
            id = 2,
            title = "随手想办的事",
            dueAt = null
        )
        val future = todo(
            id = 3,
            title = "明天再做",
            dueAt = LocalDateTime.of(2026, 5, 15, 12, 0)
        )

        val snapshot = DailyBoardSnapshotBuilder.build(
            items = listOf(todayWithDdl, noDdl, future),
            now = now
        )

        assertEquals(listOf("今天有 DDL", "随手想办的事"), snapshot.todoItems.map { it.title })
    }

    @Test
    fun noDdlTodosStayInTodayTodosOnLaterDates() {
        val noDdl = todo(
            id = 2,
            title = "随手想办的事",
            dueAt = null
        )

        val firstDay = DailyBoardSnapshotBuilder.build(
            items = listOf(noDdl),
            now = LocalDateTime.of(2026, 5, 14, 8, 0)
        )
        val nextDay = DailyBoardSnapshotBuilder.build(
            items = listOf(noDdl),
            now = LocalDateTime.of(2026, 5, 15, 8, 0)
        )

        assertEquals(listOf("随手想办的事"), firstDay.todoItems.map { it.title })
        assertEquals(listOf("随手想办的事"), nextDay.todoItems.map { it.title })
    }

    private fun todo(id: Long, title: String, dueAt: LocalDateTime?): TodoItem {
        return TodoItem(
            id = id,
            title = title,
            dueAtMillis = dueAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                ?: NO_DUE_DATE_MILLIS,
            reminderAtMillis = null,
            reminderEnabled = false,
            ringEnabled = true,
            vibrateEnabled = true
        )
    }
}
