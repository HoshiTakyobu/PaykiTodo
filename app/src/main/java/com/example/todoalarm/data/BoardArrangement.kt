package com.example.todoalarm.data

/** Read-only ordering: event start and each task's own DDL stay unchanged. */
object BoardArrangement {
    fun section(item: TodoItem): Int = when {
        item.isEvent && item.allDay -> 0
        item.isTodo && !item.hasDueDate -> 2
        else -> 1
    }
    fun ordered(todos: List<TodoItem>, events: List<TodoItem>): List<TodoItem> =
        (todos + events).distinctBy { it.id }.sortedWith(compareBy<TodoItem> { section(it) }
            .thenBy { if (it.isEvent) it.startAtMillis ?: Long.MAX_VALUE else it.dueAtMillis }
            .thenBy { if (it.isEvent) 0 else 1 }.thenBy { it.id })
}
