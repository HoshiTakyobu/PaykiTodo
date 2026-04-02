package com.example.todoalarm.data

import kotlinx.coroutines.flow.Flow

class TodoRepository(
    private val todoDao: TodoDao
) {
    fun observeActiveTodos(): Flow<List<TodoItem>> = todoDao.observeActiveTodos()

    suspend fun addTodo(item: TodoItem): TodoItem {
        val id = todoDao.insert(item)
        return item.copy(id = id)
    }

    suspend fun getTodo(id: Long): TodoItem? = todoDao.getById(id)

    suspend fun setCompleted(id: Long, completed: Boolean): TodoItem? {
        val item = todoDao.getById(id) ?: return null
        val updated = item.copy(
            completed = completed,
            reminderEnabled = if (completed) false else item.reminderEnabled
        )
        todoDao.update(updated)
        return updated
    }

    suspend fun snoozeTodo(id: Long, nextReminderMillis: Long): TodoItem? {
        val item = todoDao.getById(id) ?: return null
        val updated = item.copy(
            reminderAtMillis = nextReminderMillis,
            reminderEnabled = true
        )
        todoDao.update(updated)
        return updated
    }

    suspend fun futureReminderItems(now: Long): List<TodoItem> {
        return todoDao.getFutureReminderItems(now)
    }
}

