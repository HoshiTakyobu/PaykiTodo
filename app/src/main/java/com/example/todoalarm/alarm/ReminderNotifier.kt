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
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.ResolvedTaskGroup
import com.example.todoalarm.ui.ReminderActivity
import com.example.todoalarm.ui.formatLocalDateTime
import com.example.todoalarm.ui.reminderAtMillisToDateTime
import com.example.todoalarm.ui.resolveTaskGroup
import com.example.todoalarm.ui.taskGroupEmoji
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReminderNotifier(
    private val context: Context
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun build(
        todoItem: TodoItem,
        taskGroup: ResolvedTaskGroup = resolveTaskGroup(todoItem, emptyList()),
        requestFullscreen: Boolean = false
    ): Notification {
        val channelId = ensureChannel(todoItem)
        val fullScreenIntent = reminderPendingIntent(todoItem.id)
        val body = reminderText(todoItem, taskGroup)

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_payki_todo)
            .setContentTitle("${taskGroupEmoji(taskGroup)} ${todoItem.title}")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setDefaults(0)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(fullScreenIntent)
            .apply {
                if (requestFullscreen) {
                    setFullScreenIntent(fullScreenIntent, true)
                }
            }
            .build()
    }

    fun show(
        todoItem: TodoItem,
        taskGroup: ResolvedTaskGroup = resolveTaskGroup(todoItem, emptyList())
    ) {
        notificationManager.notify(notificationId(todoItem.id), build(todoItem, taskGroup))
    }

    fun createReminderIntent(todoId: Long): Intent = buildReminderIntent(todoId)

    fun reminderPendingIntent(todoId: Long): PendingIntent {
        return PendingIntent.getActivity(
            context,
            AlarmScheduler.requestCodeFor(todoId, 0L),
            buildReminderIntent(todoId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancel(todoId: Long) {
        notificationManager.cancel(notificationId(todoId))
    }

    private fun buildReminderIntent(todoId: Long): Intent {
        return ReminderActivity.createIntent(context, todoId)
    }

    private fun ensureChannel(todoItem: TodoItem): String {
        val channelId = when {
            todoItem.ringEnabled -> "paykitodo_alarm_audible_v9"
            todoItem.vibrateEnabled -> "paykitodo_alarm_vibrate_v9"
            else -> "paykitodo_alarm_silent_v9"
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelId

        val bypassDnd = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            false
        }

        notificationManager.getNotificationChannel(channelId)?.let {
            return channelId
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
            vibrationPattern = longArrayOf(0L)
            setBypassDnd(bypassDnd)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
        return channelId
    }

    private fun reminderText(todoItem: TodoItem, taskGroup: ResolvedTaskGroup): String {
        if (todoItem.isEvent) {
            return if (todoItem.allDay) {
                "全天日程 · ${eventDateLabel(todoItem)}"
            } else {
                "日程 · ${eventTimeRangeLabel(todoItem)}"
            }
        }
        if (!todoItem.hasDueDate) {
            return "${taskGroup.name} · 未设置 DDL"
        }
        val due = formatLocalDateTime(reminderAtMillisToDateTime(todoItem.dueAtMillis))
        return "${taskGroup.name} · \u23F0 DDL $due"
    }

    private fun channelName(todoItem: TodoItem): String = when {
        todoItem.ringEnabled -> context.getString(R.string.channel_name_audible)
        todoItem.vibrateEnabled -> context.getString(R.string.channel_name_vibrate)
        else -> context.getString(R.string.channel_name_silent)
    }

    private fun eventDateLabel(todoItem: TodoItem): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)
        val start = todoItem.startAtMillis?.let(::toLocalDateTime)
            ?: return formatLocalDateTime(reminderAtMillisToDateTime(todoItem.dueAtMillis))
        val endExclusive = todoItem.endAtMillis?.let(::toLocalDateTime)?.toLocalDate()?.minusDays(1)
            ?: start.toLocalDate()
        return if (start.toLocalDate() == endExclusive) {
            start.toLocalDate().format(formatter)
        } else {
            "${start.toLocalDate().format(formatter)} - ${endExclusive.format(formatter)}"
        }
    }

    private fun eventTimeRangeLabel(todoItem: TodoItem): String {
        val formatter = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
        val start = todoItem.startAtMillis?.let(::toLocalDateTime)
            ?: return formatLocalDateTime(reminderAtMillisToDateTime(todoItem.dueAtMillis))
        val end = todoItem.endAtMillis?.let(::toLocalDateTime) ?: start
        return if (start.toLocalDate() == end.toLocalDate()) {
            "${start.format(formatter)} - ${end.format(timeFormatter)}"
        } else {
            "${start.format(formatter)} - ${end.format(formatter)}"
        }
    }

    private fun toLocalDateTime(epochMillis: Long) =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

    companion object {
        fun notificationId(todoId: Long): Int = 10_000 + AlarmScheduler.requestCodeFor(todoId, 0L)
    }
}
