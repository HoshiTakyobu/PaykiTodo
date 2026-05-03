package com.example.todoalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.todoalarm.data.reminderTriggerTimesMillis
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
        val triggerTimes = todoItem.reminderTriggerTimesMillis()
        val triggerAtMillis = triggerTimes.minOrNull() ?: return null
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
            triggerTimes.forEach { scheduledAt ->
                val exactIntent = buildBroadcastIntent(todoItem.id, scheduledAt, ACTION_EXACT, EXACT_OFFSET)
                val backupIntent = buildBroadcastIntent(todoItem.id, scheduledAt, ACTION_BACKUP, BACKUP_OFFSET)
                if (canUseAlarmClock) {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(scheduledAt, buildShowIntent(todoItem.id)),
                        exactIntent
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        scheduledAt,
                        exactIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        scheduledAt,
                        exactIntent
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canUseExactBackup) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        scheduledAt + BACKUP_DELAY_MILLIS,
                        backupIntent
                    )
                }
            }
            persistScheduledTimes(todoItem.id, triggerTimes)
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
        alarmManager.cancel(buildBroadcastIntent(todoId, 0L, ACTION_EXACT, EXACT_OFFSET))
        alarmManager.cancel(buildBroadcastIntent(todoId, 0L, ACTION_BACKUP, BACKUP_OFFSET))
        val prefs = context.getSharedPreferences(SCHEDULE_PREFS, Context.MODE_PRIVATE)
        val scheduledTimes = prefs.getStringSet(scheduledSetKey(todoId), emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }
        scheduledTimes.forEach { scheduledAt ->
            alarmManager.cancel(buildBroadcastIntent(todoId, scheduledAt, ACTION_EXACT, EXACT_OFFSET))
            alarmManager.cancel(buildBroadcastIntent(todoId, scheduledAt, ACTION_BACKUP, BACKUP_OFFSET))
        }
        prefs.edit().remove(scheduledSetKey(todoId)).apply()
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
            requestCodeFor(todoId, reminderAtMillis ?: 0L) + offset,
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
            requestCodeFor(todoId, 0L),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun persistScheduledTimes(todoId: Long, triggerTimes: List<Long>) {
        context.getSharedPreferences(SCHEDULE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(scheduledSetKey(todoId), triggerTimes.map { it.toString() }.toSet())
            .apply()
    }

    companion object {
        const val EXTRA_TODO_ID = "extra_todo_id"
        const val EXTRA_REMINDER_AT = "extra_reminder_at"
        private const val ACTION_EXACT = "com.paykitodo.app.EXACT"
        private const val ACTION_BACKUP = "com.paykitodo.app.EXACT_BACKUP"
        private const val EXACT_OFFSET = 10_000
        private const val BACKUP_OFFSET = 20_000
        private const val BACKUP_DELAY_MILLIS = 2_000L
        private const val SCHEDULE_PREFS = "paykitodo_scheduled_reminders"

        fun requestCodeFor(todoId: Long, reminderAtMillis: Long): Int {
            val folded = todoId xor reminderAtMillis xor (reminderAtMillis ushr 32)
            return (folded xor (folded ushr 32)).toInt()
        }

        private fun scheduledSetKey(todoId: Long): String = "scheduled_$todoId"
    }
}
