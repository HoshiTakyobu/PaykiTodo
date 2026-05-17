package com.example.todoalarm.ui

import com.example.todoalarm.data.TodoItem
import java.time.LocalDate

internal data class ActiveTodoSections(
    val missedItems: List<TodoItem>,
    val todayItems: List<TodoItem>,
    val upcomingItems: List<TodoItem>
)

internal fun classifyActiveTodoItems(
    activeTaskItems: List<TodoItem>,
    today: LocalDate
): ActiveTodoSections {
    val sortedActiveTaskItems = activeTaskItems.sortedBy { it.dueAtMillis }
    return ActiveTodoSections(
        missedItems = sortedActiveTaskItems.filter { it.missed },
        todayItems = sortedActiveTaskItems.filter { !it.missed && (!it.hasDueDate || it.dueDate() == today) },
        upcomingItems = sortedActiveTaskItems.filter { it.hasDueDate && !it.missed && it.dueDate().isAfter(today) }
    )
}
