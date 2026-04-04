package com.example.todoalarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.todoalarm.R
import com.example.todoalarm.data.TodoCategory
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.ReminderActivity
import com.example.todoalarm.ui.formatLocalDateTime
import com.example.todoalarm.ui.reminderAtMillisToDateTime

class ReminderNotifier(
    private val context: Context
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun build(todoItem: TodoItem): Notification {
        val channelId = ensureChannel(todoItem)
        val fullScreenIntent = reminderPendingIntent(todoItem.id)
        val body = reminderText(todoItem)

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle("${categoryEmoji(todoItem)} ${todoItem.title}")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .setSilent(true)
            .build()
    }

    fun show(todoItem: TodoItem) {
        notificationManager.notify(notificationId(todoItem.id), build(todoItem))
    }

    fun createReminderIntent(todoId: Long): Intent = buildReminderIntent(todoId)

    fun reminderPendingIntent(todoId: Long): PendingIntent {
        return PendingIntent.getActivity(
            context,
            AlarmScheduler.requestCodeFor(todoId),
            buildReminderIntent(todoId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancel(todoId: Long) {
        notificationManager.cancel(notificationId(todoId))
    }

    private fun buildReminderIntent(todoId: Long): Intent {
        return Intent(context, ReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmScheduler.EXTRA_TODO_ID, todoId)
        }
    }

    private fun ensureChannel(todoItem: TodoItem): String {
        val channelId = when {
            todoItem.ringEnabled -> "paykitodo_alarm_audible_v7"
            todoItem.vibrateEnabled -> "paykitodo_alarm_vibrate_v7"
            else -> "paykitodo_alarm_silent_v7"
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelId

        val bypassDnd = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            false
        }

        notificationManager.getNotificationChannel(channelId)?.let {
            notificationManager.deleteNotificationChannel(it.id)
        }

        val channel = NotificationChannel(
            channelId,
            channelName(todoItem),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_description)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            enableLights(true)
            enableVibration(false)
            setBypassDnd(bypassDnd)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
        return channelId
    }

    private fun reminderText(todoItem: TodoItem): String {
        val due = formatLocalDateTime(reminderAtMillisToDateTime(todoItem.dueAtMillis))
        return "\u23F0 DDL $due"
    }

    private fun channelName(todoItem: TodoItem): String = when {
        todoItem.ringEnabled -> context.getString(R.string.channel_name_audible)
        todoItem.vibrateEnabled -> context.getString(R.string.channel_name_vibrate)
        else -> context.getString(R.string.channel_name_silent)
    }

    private fun categoryEmoji(todoItem: TodoItem): String = when (TodoCategory.fromKey(todoItem.categoryKey)) {
        TodoCategory.IMPORTANT -> "\u2B50"
        TodoCategory.URGENT -> "\u26A0\uFE0F"
        TodoCategory.FOCUS -> "\uD83C\uDFAF"
        TodoCategory.ROUTINE -> "\uD83E\uDDFD"
    }

    companion object {
        fun notificationId(todoId: Long): Int = 10_000 + AlarmScheduler.requestCodeFor(todoId)
    }
}
