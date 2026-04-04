package com.example.todoalarm.alarm

import android.content.Context

object ActiveReminderStore {
    private const val PREFS_NAME = "paykitodo_active_reminder"
    private const val KEY_TODO_ID = "active_todo_id"
    private const val KEY_UPDATED_AT = "updated_at"

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
            .apply()
    }

    fun clearIfMatches(context: Context, todoId: Long) {
        if (getActiveTodoId(context) == todoId) {
            clear(context)
        }
    }
}
