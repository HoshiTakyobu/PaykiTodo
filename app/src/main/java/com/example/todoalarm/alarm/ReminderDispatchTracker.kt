package com.example.todoalarm.alarm

import android.content.Context

object ReminderDispatchTracker {
    private const val PREFS_NAME = "paykitodo_reminder_dispatch"

    fun wasDispatched(context: Context, todoId: Long, reminderAtMillis: Long): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(key(todoId), 0L) >= reminderAtMillis
    }

    fun markDispatched(context: Context, todoId: Long, dispatchedAtMillis: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(key(todoId), dispatchedAtMillis).apply()
    }

    fun clear(context: Context, todoId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(key(todoId)).apply()
    }

    private fun key(todoId: Long): String = "todo_$todoId"
}
