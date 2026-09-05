package com.example.todoalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todoalarm.data.ReminderChainStage
import com.example.todoalarm.data.ReminderChainStatus

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra(AlarmScheduler.EXTRA_TODO_ID, -1L)
        if (todoId <= 0L) return
        val reminderAtMillis = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_AT, -1L)
        if (reminderAtMillis > 0L && !AlarmScheduler(context).isReminderScheduled(todoId, reminderAtMillis)) {
            ReminderChainLogger.log(
                context = context,
                todoId = todoId,
                source = "ReminderReceiver",
                stage = if (intent.action == "com.paykitodo.app.EXACT_BACKUP") {
                    ReminderChainStage.RECEIVER_BACKUP
                } else {
                    ReminderChainStage.RECEIVER_EXACT
                },
                status = ReminderChainStatus.INFO,
                reminderAtMillis = reminderAtMillis,
                message = "stale_alarm_ignored"
            )
            return
        }
        ReminderChainLogger.log(
            context = context,
            todoId = todoId,
            source = "ReminderReceiver",
            stage = if (intent.action == "com.paykitodo.app.EXACT_BACKUP") {
                ReminderChainStage.RECEIVER_BACKUP
            } else {
                ReminderChainStage.RECEIVER_EXACT
            },
            status = ReminderChainStatus.OK,
            reminderAtMillis = reminderAtMillis.takeIf { it > 0L },
            message = intent.action
        )
        ReminderForegroundService.start(
            context = context,
            todoId = todoId,
            triggeredAtMillis = reminderAtMillis.takeIf { it > 0L }
        )
    }
}
