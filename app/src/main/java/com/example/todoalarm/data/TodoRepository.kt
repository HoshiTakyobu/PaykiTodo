package com.example.todoalarm.data

import kotlinx.coroutines.flow.Flow

class TodoRepository(
    private val todoDao: TodoDao
) {
    fun observeTodos(): Flow<List<TodoItem>> = todoDao.observeTodos()

    suspend fun addTodo(item: TodoItem): TodoItem {
        val id = todoDao.insert(item)
        return item.copy(id = id)
    }

    suspend fun getTodo(id: Long): TodoItem? = todoDao.getById(id)

    suspend fun updateTodo(item: TodoItem): TodoItem {
        todoDao.update(item)
        return item
    }

    suspend fun deleteTodo(id: Long) {
        todoDao.deleteById(id)
    }

    suspend fun setCompleted(id: Long, completed: Boolean): TodoItem? {
        val item = todoDao.getById(id) ?: return null
        val now = System.currentTimeMillis()
        val updated = item.copy(
            completed = completed,
            completedAtMillis = if (completed) now else null,
            reminderEnabled = if (completed) {
                false
            } else {
                item.reminderAtMillis?.let { it > now } == true
            }
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
