package com.example.todoalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.todoalarm.CrashLogger
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
        if (todoItem.isHistory) {
            cancel(todoItem.id)
            return null
        }

        cancelReminderAlarms(todoItem.id)
        if (todoItem.isEvent) {
            OngoingEventNotifier.schedule(context, todoItem)
        }
        if (!todoItem.reminderEnabled) {
            return null
        }

        val now = System.currentTimeMillis()
        val triggerTimes = todoItem.reminderTriggerTimesMillis().filter { it >= now }
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
        val canUseAlarmClock = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        val canUseExactBackup = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        if (!canUseAlarmClock && !canUseExactBackup) {
            return "系统未授予精确闹钟权限，提醒尚未启用。"
        }

        val scheduledPrimaryTimes = mutableListOf<Long>()
        var firstFailure: Throwable? = null

        triggerTimes.forEach { scheduledAt ->
            val exactIntent = buildBroadcastIntent(todoItem.id, scheduledAt, ACTION_EXACT, EXACT_OFFSET)
            val backupIntent = buildBroadcastIntent(todoItem.id, scheduledAt, ACTION_BACKUP, BACKUP_OFFSET)
            val primaryScheduled = when {
                canUseAlarmClock -> safeSchedule(
                    todoItem = todoItem,
                    scheduledAt = scheduledAt,
                    mode = "alarmClock",
                    onFailure = { firstFailure = firstFailure ?: it }
                ) {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(scheduledAt, buildShowIntent(todoItem.id)),
                        exactIntent
                    )
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> safeSchedule(
                    todoItem = todoItem,
                    scheduledAt = scheduledAt,
                    mode = "exactAllowWhileIdle",
                    onFailure = { firstFailure = firstFailure ?: it }
                ) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        scheduledAt,
                        exactIntent
                    )
                }

                else -> safeSchedule(
                    todoItem = todoItem,
                    scheduledAt = scheduledAt,
                    mode = "exact",
                    onFailure = { firstFailure = firstFailure ?: it }
                ) {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        scheduledAt,
                        exactIntent
                    )
                }
            }

            val accepted = if (primaryScheduled) {
                true
            } else {
                safeSchedule(
                    todoItem = todoItem,
                    scheduledAt = scheduledAt,
                    mode = "inexactFallback",
                    onFailure = { firstFailure = firstFailure ?: it }
                ) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, scheduledAt, exactIntent)
                }
            }

            if (accepted) {
                scheduledPrimaryTimes += scheduledAt
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canUseExactBackup) {
                    safeSchedule(
                        todoItem = todoItem,
                        scheduledAt = scheduledAt + BACKUP_DELAY_MILLIS,
                        mode = "backupExact",
                        onFailure = { firstFailure = firstFailure ?: it }
                    ) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            scheduledAt + BACKUP_DELAY_MILLIS,
                            backupIntent
                        )
                    }
                }
            }
        }

        return if (scheduledPrimaryTimes.isNotEmpty()) {
            persistScheduledTimes(todoItem.id, scheduledPrimaryTimes)
            ReminderChainLogger.log(
                context = context,
                todoId = todoItem.id,
                source = "AlarmScheduler",
                stage = ReminderChainStage.SCHEDULED,
                status = ReminderChainStatus.OK,
                reminderAtMillis = triggerAtMillis,
                message = if (scheduledPrimaryTimes.size == triggerTimes.size) {
                    if (canUseAlarmClock) "alarmClock" else "exact"
                } else {
                    "partial:${scheduledPrimaryTimes.size}/${triggerTimes.size}"
                }
            )
            null
        } else {
            cancel(todoItem.id)
            val throwable = firstFailure
            ReminderChainLogger.log(
                context = context,
                todoId = todoItem.id,
                source = "AlarmScheduler",
                stage = ReminderChainStage.SCHEDULE_FAILED,
                status = ReminderChainStatus.ERROR,
                reminderAtMillis = triggerAtMillis,
                message = throwable?.javaClass?.simpleName ?: "NoScheduleAccepted"
            )
            "系统拒绝设置提醒：${throwable?.javaClass?.simpleName ?: "NoScheduleAccepted"}"
        }
    }

    fun cancel(todoId: Long) {
        OngoingEventNotifier.cancelAll(context, todoId)
        cancelReminderAlarms(todoId)
    }

    private fun cancelReminderAlarms(todoId: Long) {
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

    private fun safeSchedule(
        todoItem: TodoItem,
        scheduledAt: Long,
        mode: String,
        onFailure: (Throwable) -> Unit,
        block: () -> Unit
    ): Boolean {
        return try {
            block()
            true
        } catch (security: SecurityException) {
            recordScheduleFailure(todoItem, scheduledAt, mode, security)
            onFailure(security)
            false
        } catch (exception: Exception) {
            recordScheduleFailure(todoItem, scheduledAt, mode, exception)
            onFailure(exception)
            false
        }
    }

    private fun recordScheduleFailure(
        todoItem: TodoItem,
        scheduledAt: Long,
        mode: String,
        throwable: Throwable
    ) {
        CrashLogger.recordNonFatal(throwable)
        ReminderChainLogger.log(
            context = context,
            todoId = todoItem.id,
            source = "AlarmScheduler",
            stage = ReminderChainStage.SCHEDULE_FAILED,
            status = ReminderChainStatus.ERROR,
            reminderAtMillis = scheduledAt,
            message = "$mode:${throwable.javaClass.simpleName}"
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
        private const val SCHEDULE_PREFS = "paykitodo_scheduled_reminders"

        fun requestCodeFor(todoId: Long, reminderAtMillis: Long): Int {
            val folded = todoId xor reminderAtMillis xor (reminderAtMillis ushr 32)
            return (folded xor (folded ushr 32)).toInt()
        }

        private fun scheduledSetKey(todoId: Long): String = "scheduled_$todoId"
    }
}
