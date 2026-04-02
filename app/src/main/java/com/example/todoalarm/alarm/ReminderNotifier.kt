package com.example.todoalarm.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.todoalarm.R
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.ReminderActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReminderNotifier(
    private val context: Context
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun show(todoItem: TodoItem) {
        ensureChannel()

        val activityIntent = Intent(context, ReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmScheduler.EXTRA_TODO_ID, todoItem.id)
        }

        val fullScreenIntent = PendingIntent.getActivity(
            context,
            AlarmScheduler.requestCodeFor(todoItem.id),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val reminderText = buildReminderText(todoItem)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(todoItem.title)
            .setContentText(reminderText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminderText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .build()

        notificationManager.notify(notificationId(todoItem.id), notification)
    }

    fun cancel(todoId: Long) {
        notificationManager.cancel(notificationId(todoId))
    }

    private fun ensureChannel() {
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_description)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildReminderText(todoItem: TodoItem): String {
        val formatter = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)
        val reminderTime = Instant.ofEpochMilli(
            todoItem.reminderAtMillis ?: System.currentTimeMillis()
        ).atZone(ZoneId.systemDefault()).format(formatter)
        return context.getString(R.string.notification_body, reminderTime)
    }

    companion object {
        private const val CHANNEL_ID = "todo_alarm_channel"

        fun notificationId(todoId: Long): Int = 10_000 + AlarmScheduler.requestCodeFor(todoId)
    }
}

