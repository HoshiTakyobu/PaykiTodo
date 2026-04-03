package com.example.todoalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.MainActivity

class AlarmScheduler(
    private val context: Context
) {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

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

        val alarmClockIntent = buildBroadcastIntent(todoItem.id, ACTION_ALARM_CLOCK, ALARM_CLOCK_OFFSET)
        val exactIntent = buildBroadcastIntent(todoItem.id, ACTION_EXACT, EXACT_OFFSET)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, buildPreviewIntent(todoItem.id)),
                alarmClockIntent
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                exactIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                exactIntent
            )
        }
    }

    fun cancel(todoId: Long) {
        alarmManager.cancel(buildBroadcastIntent(todoId, ACTION_ALARM_CLOCK, ALARM_CLOCK_OFFSET))
        alarmManager.cancel(buildBroadcastIntent(todoId, ACTION_EXACT, EXACT_OFFSET))
    }

    private fun buildBroadcastIntent(
        todoId: Long,
        action: String,
        offset: Int
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_TODO_ID, todoId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(todoId) + offset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildPreviewIntent(todoId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TODO_ID, todoId)
        }
        return PendingIntent.getActivity(
            context,
            requestCodeFor(todoId) + PREVIEW_OFFSET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_TODO_ID = "extra_todo_id"
        private const val ACTION_ALARM_CLOCK = "com.paykitodo.app.ALARM_CLOCK"
        private const val ACTION_EXACT = "com.paykitodo.app.EXACT"
        private const val ALARM_CLOCK_OFFSET = 0
        private const val EXACT_OFFSET = 10_000
        private const val PREVIEW_OFFSET = 20_000

        fun requestCodeFor(todoId: Long): Int {
            return (todoId xor (todoId ushr 32)).toInt()
        }
    }
}
