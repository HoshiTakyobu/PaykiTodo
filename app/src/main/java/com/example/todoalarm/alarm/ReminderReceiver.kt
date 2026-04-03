package com.example.todoalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra(AlarmScheduler.EXTRA_TODO_ID, -1L)
        if (todoId <= 0L) return
        if (recentlyHandled(context, todoId)) return
        markHandled(context, todoId)
        ReminderForegroundService.start(context, todoId)
    }

    private fun recentlyHandled(context: Context, todoId: Long): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastTime = prefs.getLong("todo_$todoId", 0L)
        return System.currentTimeMillis() - lastTime < 15_000L
    }

    private fun markHandled(context: Context, todoId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong("todo_$todoId", System.currentTimeMillis()).apply()
    }

    companion object {
        private const val PREFS_NAME = "paykitodo_alarm_receiver"
    }
}
