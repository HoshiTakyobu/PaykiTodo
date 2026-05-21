package com.example.todoalarm.ui

import com.example.todoalarm.data.TodoItem
import java.time.LocalDate

internal data class ActiveTodoSections(
    val missedItems: List<TodoItem>,
    val todayItems: List<TodoItem>,
    val upcomingItems: List<TodoItem>
)

data class UpcomingTodoDisplayGroup(
    val seriesId: String?,
    val items: List<TodoItem>
) {
    val representative: TodoItem
        get() = items.first()

    val isCollapsibleRecurringSeries: Boolean
        get() = seriesId != null && items.size > 1
}

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

internal fun buildUpcomingTodoDisplayGroups(upcomingItems: List<TodoItem>): List<UpcomingTodoDisplayGroup> {
    if (upcomingItems.isEmpty()) return emptyList()
    val recurringGroups = upcomingItems
        .filter { it.isRecurring && !it.recurringSeriesId.isNullOrBlank() }
        .groupBy { it.recurringSeriesId.orEmpty() }
    val emittedSeriesIds = mutableSetOf<String>()
    return buildList {
        upcomingItems.forEach { item ->
            val seriesId = item.recurringSeriesId?.takeIf { item.isRecurring && it.isNotBlank() }
            if (seriesId == null) {
                add(UpcomingTodoDisplayGroup(seriesId = null, items = listOf(item)))
            } else if (emittedSeriesIds.add(seriesId)) {
                add(
                    UpcomingTodoDisplayGroup(
                        seriesId = seriesId,
                        items = recurringGroups.getValue(seriesId).sortedBy { it.dueAtMillis }
                    )
                )
            }
        }
    }
}
