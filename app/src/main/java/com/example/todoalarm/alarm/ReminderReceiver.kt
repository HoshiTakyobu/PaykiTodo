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
        if (reminderAtMillis > 0L) {
            if (ReminderDispatchTracker.wasDispatched(context, todoId, reminderAtMillis)) return
            ReminderDispatchTracker.markDispatched(context, todoId, reminderAtMillis)
        }
        ReminderForegroundService.start(context, todoId)
    }
}
