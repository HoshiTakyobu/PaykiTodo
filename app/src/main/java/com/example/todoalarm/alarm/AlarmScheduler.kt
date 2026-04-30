package com.example.todoalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.ReminderChainStage
import com.example.todoalarm.data.ReminderChainStatus
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

    fun schedule(todoItem: TodoItem): String? {
        val triggerAtMillis = todoItem.reminderAtMillis ?: return null
        ReminderChainLogger.log(
            context = context,
            todoId = todoItem.id,
            source = "AlarmScheduler",
            stage = ReminderChainStage.SCHEDULE_REQUESTED,
            status = ReminderChainStatus.INFO,
            reminderAtMillis = triggerAtMillis,
            message = "itemType=${todoItem.itemType}"
        )
        if (todoItem.isHistory || !todoItem.reminderEnabled) {
            cancel(todoItem.id)
            return null
        }

        cancel(todoItem.id)
        val exactIntent = buildBroadcastIntent(todoItem.id, triggerAtMillis, ACTION_EXACT, EXACT_OFFSET)
        val backupIntent = buildBroadcastIntent(todoItem.id, triggerAtMillis, ACTION_BACKUP, BACKUP_OFFSET)
        val canUseAlarmClock = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        val canUseExactBackup = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        if (!canUseAlarmClock && !canUseExactBackup) {
            return "系统未授予精确闹钟权限，提醒尚未启用。"
        }

        return runCatching {
            if (canUseAlarmClock) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, buildShowIntent(todoItem.id)),
                    exactIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canUseExactBackup) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis + BACKUP_DELAY_MILLIS,
                    backupIntent
                )
            }
            ReminderChainLogger.log(
                context = context,
                todoId = todoItem.id,
                source = "AlarmScheduler",
                stage = ReminderChainStage.SCHEDULED,
                status = ReminderChainStatus.OK,
                reminderAtMillis = triggerAtMillis,
                message = if (canUseAlarmClock) "alarmClock" else "exact"
            )
        }.exceptionOrNull()?.let { throwable ->
            cancel(todoItem.id)
            ReminderChainLogger.log(
                context = context,
                todoId = todoItem.id,
                source = "AlarmScheduler",
                stage = ReminderChainStage.SCHEDULE_FAILED,
                status = ReminderChainStatus.ERROR,
                reminderAtMillis = triggerAtMillis,
                message = throwable.javaClass.simpleName
            )
            "系统拒绝设置提醒：${throwable.javaClass.simpleName}"
        }
    }

    fun cancel(todoId: Long) {
        alarmManager.cancel(buildBroadcastIntent(todoId, null, ACTION_EXACT, EXACT_OFFSET))
        alarmManager.cancel(buildBroadcastIntent(todoId, null, ACTION_BACKUP, BACKUP_OFFSET))
    }

    private fun buildBroadcastIntent(
        todoId: Long,
        reminderAtMillis: Long?,
        action: String,
        offset: Int
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            putExtra(EXTRA_TODO_ID, todoId)
            reminderAtMillis?.let { putExtra(EXTRA_REMINDER_AT, it) }
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(todoId) + offset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildShowIntent(todoId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_TODO_ID, todoId)
        }
        return PendingIntent.getActivity(
            context,
            requestCodeFor(todoId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_TODO_ID = "extra_todo_id"
        const val EXTRA_REMINDER_AT = "extra_reminder_at"
        private const val ACTION_EXACT = "com.paykitodo.app.EXACT"
        private const val ACTION_BACKUP = "com.paykitodo.app.EXACT_BACKUP"
        private const val EXACT_OFFSET = 10_000
        private const val BACKUP_OFFSET = 20_000
        private const val BACKUP_DELAY_MILLIS = 2_000L

        fun requestCodeFor(todoId: Long): Int {
            return (todoId xor (todoId ushr 32)).toInt()
        }
    }
}
