package com.example.todoalarm.alarm

import android.content.Context

object ActiveReminderStore {
    private const val PREFS_NAME = "paykitodo_active_reminder"
    private const val KEY_TODO_ID = "active_todo_id"
    private const val KEY_UPDATED_AT = "updated_at"
    private const val KEY_HANDOFF_TODO_ID = "handoff_todo_id"
    private const val KEY_HANDOFF_UNTIL = "handoff_until"

    fun markActive(context: Context, todoId: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_TODO_ID, todoId)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun getActiveTodoId(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_TODO_ID, -1L)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TODO_ID)
            .remove(KEY_UPDATED_AT)
            .remove(KEY_HANDOFF_TODO_ID)
            .remove(KEY_HANDOFF_UNTIL)
            .apply()
    }

    fun clearIfMatches(context: Context, todoId: Long) {
        if (getActiveTodoId(context) == todoId) {
            clear(context)
        }
    }

    fun markActivityHandoff(
        context: Context,
        todoId: Long,
        durationMs: Long = 5_000L
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_HANDOFF_TODO_ID, todoId)
            .putLong(KEY_HANDOFF_UNTIL, System.currentTimeMillis() + durationMs)
            .apply()
    }

    fun isActivityHandoffPending(context: Context, todoId: Long): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val handoffTodoId = prefs.getLong(KEY_HANDOFF_TODO_ID, -1L)
        val handoffUntil = prefs.getLong(KEY_HANDOFF_UNTIL, 0L)
        val pending = handoffTodoId == todoId && handoffUntil > System.currentTimeMillis()
        if (!pending && handoffTodoId == todoId) {
            clearActivityHandoff(context, todoId)
        }
        return pending
    }

    fun clearActivityHandoff(context: Context, todoId: Long? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (todoId != null && prefs.getLong(KEY_HANDOFF_TODO_ID, -1L) != todoId) return
        prefs.edit()
            .remove(KEY_HANDOFF_TODO_ID)
            .remove(KEY_HANDOFF_UNTIL)
            .apply()
    }
}
