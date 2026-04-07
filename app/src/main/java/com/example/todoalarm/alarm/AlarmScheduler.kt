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
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedule(todoItem: TodoItem): String? {
        val triggerAtMillis = todoItem.reminderAtMillis ?: return null
        if (todoItem.isHistory || !todoItem.reminderEnabled) {
            cancel(todoItem.id)
            return null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return "系统未授予精确闹钟权限，提醒尚未启用。"
        }

        val exactIntent = buildBroadcastIntent(todoItem.id, ACTION_EXACT, EXACT_OFFSET)
        return runCatching {
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
        }.exceptionOrNull()?.let { throwable ->
            cancel(todoItem.id)
            "系统拒绝设置提醒：${throwable.javaClass.simpleName}"
        }
    }

    fun cancel(todoId: Long) {
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

    companion object {
        const val EXTRA_TODO_ID = "extra_todo_id"
        private const val ACTION_EXACT = "com.paykitodo.app.EXACT"
        private const val EXACT_OFFSET = 10_000

        fun requestCodeFor(todoId: Long): Int {
            return (todoId xor (todoId ushr 32)).toInt()
        }
    }
}
