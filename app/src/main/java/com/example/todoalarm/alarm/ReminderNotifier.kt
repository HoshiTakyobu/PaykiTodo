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
import com.example.todoalarm.data.calendarZone
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
        requestFullscreen: Boolean = false,
        showReminderContent: Boolean = true,
        triggeredAtMillis: Long? = null
    ): Notification {
        val channelId = ensureChannel(todoItem)
        val fullScreenIntent = reminderPendingIntent(todoItem.id, triggeredAtMillis)
        val notificationClickIntent = detailPendingIntent(todoItem)
        val body = reminderText(todoItem, taskGroup)

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_payki_todo)
            .setContentTitle(if (showReminderContent) "${taskGroupEmoji(taskGroup)} ${todoItem.title}" else "PaykiTodo")
            .setContentText(if (showReminderContent) body else "全屏提醒正在显示")
            .setStyle(if (showReminderContent) NotificationCompat.BigTextStyle().bigText(body) else NotificationCompat.BigTextStyle().bigText("PaykiTodo 正在显示全屏提醒。"))
            .setPriority(if (showReminderContent) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_MIN)
            .setCategory(if (showReminderContent) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(if (showReminderContent) NotificationCompat.VISIBILITY_PUBLIC else NotificationCompat.VISIBILITY_SECRET)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(!showReminderContent)
            .setShowWhen(showReminderContent)
            .setLocalOnly(!showReminderContent)
            .setDefaults(0)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(notificationClickIntent)
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

    fun showTriggered(
        todoItem: TodoItem,
        taskGroup: ResolvedTaskGroup = resolveTaskGroup(todoItem, emptyList()),
        requestFullscreen: Boolean,
        triggeredAtMillis: Long? = null
    ) {
        notificationManager.notify(
            notificationId(todoItem.id),
            build(
                todoItem = todoItem,
                taskGroup = taskGroup,
                requestFullscreen = requestFullscreen,
                showReminderContent = true,
                triggeredAtMillis = triggeredAtMillis
            )
        )
    }

    fun showUnhandledAlarmMode(
        todoItem: TodoItem,
        taskGroup: ResolvedTaskGroup = resolveTaskGroup(todoItem, emptyList())
    ) {
        val channelId = ensureChannel(todoItem.copy(alarmMode = false))
        val pendingIntent = detailPendingIntent(todoItem)
        val body = "你有一条未处理的提醒。闹钟模式已暂停响铃，请完成任务或确认本次提醒。"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_payki_todo)
            .setContentTitle("未处理提醒：${taskGroupEmoji(taskGroup)} ${todoItem.title}")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setDefaults(0)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(notificationId(todoItem.id), notification)
    }

    fun showRetained(
        todoItem: TodoItem,
        message: String,
        taskGroup: ResolvedTaskGroup = resolveTaskGroup(todoItem, emptyList())
    ) {
        val body = "$message · 点击查看详情"
        val notification = NotificationCompat.Builder(context, ensureRetainedChannel())
            .setSmallIcon(R.drawable.ic_stat_payki_todo)
            .setContentTitle("${taskGroupEmoji(taskGroup)} ${todoItem.title}")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(false)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setDefaults(0)
            .setContentIntent(detailPendingIntent(todoItem))
            .build()
        notificationManager.notify(notificationId(todoItem.id), notification)
    }

    fun createReminderIntent(todoId: Long): Intent = buildReminderIntent(todoId)

    fun reminderPendingIntent(todoId: Long, triggeredAtMillis: Long? = null): PendingIntent {
        return PendingIntent.getActivity(
            context,
            AlarmScheduler.requestCodeFor(todoId, 0L),
            buildReminderIntent(todoId, triggeredAtMillis),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun detailPendingIntent(todoItem: TodoItem): PendingIntent {
        val intent = Intent(context, ReminderNotificationClickReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_TODO_ID, todoItem.id)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId(todoItem.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancel(todoId: Long) {
        notificationManager.cancel(notificationId(todoId))
    }

    private fun buildReminderIntent(todoId: Long, triggeredAtMillis: Long? = null): Intent {
        return ReminderActivity.createIntent(context, todoId, triggeredAtMillis)
    }

    private fun ensureChannel(todoItem: TodoItem): String {
        val channelId = when {
            todoItem.ringEnabled -> "paykitodo_alarm_audible_v10"
            todoItem.vibrateEnabled -> "paykitodo_alarm_vibrate_v10"
            else -> "paykitodo_alarm_silent_v10"
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

    private fun ensureRetainedChannel(): String {
        val channelId = "paykitodo_reminder_retained_v1"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelId
        notificationManager.getNotificationChannel(channelId)?.let { return channelId }
        notificationManager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "已确认的提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "保留已延后或已确认的事项提醒，点击可查看详情"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                enableVibration(false)
                setSound(null, null)
            }
        )
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
        return calendarEventNotificationDateLabel(todoItem)
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

internal fun calendarEventNotificationDateLabel(item: TodoItem): String {
    val zone = calendarZone(item.eventTimeZoneId)
    val start = Instant.ofEpochMilli(item.startAtMillis ?: item.dueAtMillis).atZone(zone).toLocalDate()
    val last = item.endAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().minusDays(1) }
        ?.coerceAtLeast(start) ?: start
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)
    return if (start == last) start.format(formatter) else "${start.format(formatter)} - ${last.format(formatter)}"
}
