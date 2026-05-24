package com.example.todoalarm.ui

import com.example.todoalarm.data.NO_DUE_DATE_MILLIS
import com.example.todoalarm.data.RecurrenceType
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

    @Test
    fun dueDateBoundaryUsesTodayLocalMillisRange() {
        val todayStart = todo(id = 1, title = "今天零点", dueAt = LocalDateTime.of(2026, 5, 18, 0, 0))
        val todayEnd = todo(id = 2, title = "今天最后一分钟", dueAt = LocalDateTime.of(2026, 5, 18, 23, 59))
        val tomorrowStart = todo(id = 3, title = "明天零点", dueAt = LocalDateTime.of(2026, 5, 19, 0, 0))

        val sections = classifyActiveTodoItems(
            activeTaskItems = listOf(tomorrowStart, todayEnd, todayStart),
            today = LocalDate.of(2026, 5, 18)
        )

        assertEquals(listOf("今天零点", "今天最后一分钟"), sections.todayItems.map { it.title })
        assertEquals(listOf("明天零点"), sections.upcomingItems.map { it.title })
    }

    @Test
    fun recurringUpcomingItemsFoldIntoOneDisplayGroupWithSortedInstances() {
        val oneOff = todo(id = 1, title = "单次任务", dueAt = LocalDateTime.of(2026, 5, 19, 9, 0))
        val laterRecurring = recurringTodo(id = 2, title = "循环任务-后", dueAt = LocalDateTime.of(2026, 5, 21, 8, 0))
        val earlierRecurring = recurringTodo(id = 3, title = "循环任务-前", dueAt = LocalDateTime.of(2026, 5, 20, 8, 0))

        val groups = buildUpcomingTodoDisplayGroups(listOf(oneOff, laterRecurring, earlierRecurring))

        assertEquals(listOf(null, "daily-audit"), groups.map { it.seriesId })
        assertEquals(listOf("单次任务"), groups[0].items.map { it.title })
        assertTrue(groups[1].isCollapsibleRecurringSeries)
        assertEquals(listOf("循环任务-前", "循环任务-后"), groups[1].items.map { it.title })
    }

    private fun recurringTodo(id: Long, title: String, dueAt: LocalDateTime): TodoItem {
        return todo(id = id, title = title, dueAt = dueAt).copy(
            recurringSeriesId = "daily-audit",
            recurrenceType = RecurrenceType.DAILY.name,
            recurrenceEndEpochDay = dueAt.toLocalDate().plusDays(30).toEpochDay(),
            recurrenceAnchorDueAtMillis = dueAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
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
