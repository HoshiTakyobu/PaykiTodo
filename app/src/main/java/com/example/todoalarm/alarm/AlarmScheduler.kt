package com.example.todoalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.todoalarm.data.TodoItem

class AlarmScheduler(
    private val context: Context
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedule(todoItem: TodoItem) {
        val triggerAtMillis = todoItem.reminderAtMillis ?: return
        if (todoItem.completed || !todoItem.reminderEnabled) {
            cancel(todoItem.id)
            return
        }

        val pendingIntent = buildPendingIntent(todoItem.id)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancel(todoId: Long) {
        alarmManager.cancel(buildPendingIntent(todoId))
    }

    private fun buildPendingIntent(todoId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TODO_ID, todoId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(todoId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_TODO_ID = "extra_todo_id"

        fun requestCodeFor(todoId: Long): Int {
            return (todoId xor (todoId ushr 32)).toInt()
        }
    }
}

