package com.example.todoalarm.alarm

import android.content.Context

object ReminderDispatchTracker {
    private const val PREFS_NAME = "paykitodo_reminder_dispatch"

    fun wasDispatched(context: Context, todoId: Long, reminderAtMillis: Long): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(key(todoId, reminderAtMillis), false)
    }

    fun markDispatched(context: Context, todoId: Long, dispatchedAtMillis: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(key(todoId, dispatchedAtMillis), true).apply()
    }

    fun clear(context: Context, todoId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val keys = prefs.all.keys.filter { it.startsWith("todo_${todoId}@") || it == legacyKey(todoId) }
        prefs.edit().apply {
            keys.forEach(::remove)
        }.apply()
    }

    private fun key(todoId: Long, reminderAtMillis: Long): String = "todo_${todoId}@${reminderAtMillis}"
    private fun legacyKey(todoId: Long): String = "todo_$todoId"
}
