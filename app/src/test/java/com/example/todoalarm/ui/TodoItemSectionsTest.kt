package com.example.todoalarm.ui

import com.example.todoalarm.data.NO_DUE_DATE_MILLIS
import com.example.todoalarm.data.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class TodoItemSectionsTest {
    @Test
    fun noDdlTodosStayInTodaySectionOnEveryDate() {
        val noDdl = todo(id = 1, title = "无 DDL 想办的事", dueAt = null)
        val todayWithDdl = todo(id = 2, title = "今天有 DDL", dueAt = LocalDateTime.of(2026, 5, 18, 18, 0))
        val tomorrow = todo(id = 3, title = "明天待办", dueAt = LocalDateTime.of(2026, 5, 19, 9, 0))

        val firstDay = classifyActiveTodoItems(
            activeTaskItems = listOf(noDdl, todayWithDdl, tomorrow),
            today = LocalDate.of(2026, 5, 18)
        )
        val nextDay = classifyActiveTodoItems(
            activeTaskItems = listOf(noDdl),
            today = LocalDate.of(2026, 5, 19)
        )

        assertEquals(listOf("今天有 DDL", "无 DDL 想办的事"), firstDay.todayItems.map { it.title })
        assertEquals(listOf("明天待办"), firstDay.upcomingItems.map { it.title })
        assertEquals(listOf("无 DDL 想办的事"), nextDay.todayItems.map { it.title })
    }

    @Test
    fun noDdlTodosAreNeverClassifiedAsUpcoming() {
        val noDdl = todo(id = 1, title = "无 DDL", dueAt = null)

        val sections = classifyActiveTodoItems(
            activeTaskItems = listOf(noDdl),
            today = LocalDate.of(2026, 5, 18)
        )

        assertTrue(sections.upcomingItems.isEmpty())
        assertEquals(listOf("无 DDL"), sections.todayItems.map { it.title })
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
