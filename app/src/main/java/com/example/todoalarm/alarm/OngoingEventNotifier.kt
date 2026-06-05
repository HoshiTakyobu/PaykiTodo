package com.example.todoalarm.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.todoalarm.R
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.MainActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object OngoingEventNotifier {
    private const val CHANNEL_ID = "ongoing_event_v2"
    private const val LEGACY_CHANNEL_ID = "ongoing_event"
    private const val NOTIFICATION_BASE_ID = 93_000
    const val ACTION_START = "com.paykitodo.app.ONGOING_EVENT_START"
    const val ACTION_END = "com.paykitodo.app.ONGOING_EVENT_END"
    const val EXTRA_EVENT_ID = "extra_event_id"
    private const val START_OFFSET = 30_000
    private const val END_OFFSET = 40_000
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "进行中日程",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "日程进行期间的常驻提示，会在日程结束后自动消失"
                    setShowBadge(false)
                }
            )
        }
        manager.getNotificationChannel(LEGACY_CHANNEL_ID)?.let {
            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        }
    }

    fun schedule(context: Context, event: TodoItem) {
        val startMillis = event.startAtMillis
        if (!event.isEvent || startMillis == null || event.isHistory || !isEnabled(context)) {
            cancelAll(context, event.id)
            return
        }
        val endMillis = (event.endAtMillis ?: startMillis).coerceAtLeast(startMillis)
        val now = System.currentTimeMillis()
        cancelScheduled(context, event.id)
        when {
            now >= endMillis -> cancel(context, event.id)
            now >= startMillis -> {
                post(context, event)
                scheduleAlarm(context, event.id, endMillis, ACTION_END, END_OFFSET)
            }
            else -> {
                scheduleAlarm(context, event.id, startMillis, ACTION_START, START_OFFSET)
                scheduleAlarm(context, event.id, endMillis, ACTION_END, END_OFFSET)
            }
        }
    }

    fun handleStart(context: Context, event: TodoItem) {
        val startMillis = event.startAtMillis
        if (!event.isEvent || startMillis == null || event.isHistory || !isEnabled(context)) {
            cancelAll(context, event.id)
            return
        }
        val endMillis = (event.endAtMillis ?: startMillis).coerceAtLeast(startMillis)
        val now = System.currentTimeMillis()
        when {
            now >= endMillis -> cancelAll(context, event.id)
            now >= startMillis -> {
                post(context, event)
                scheduleAlarm(context, event.id, endMillis, ACTION_END, END_OFFSET)
            }
            else -> schedule(context, event)
        }
    }

    fun handleEnd(context: Context, event: TodoItem) {
        if (shouldRefreshOngoingEventAfterEndBroadcast(event, System.currentTimeMillis())) {
            schedule(context, event)
        } else {
            cancelAll(context, event.id)
        }
    }

    fun post(context: Context, event: TodoItem) {
        val startMillis = event.startAtMillis ?: return
        if (!event.isEvent) return
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_EVENT_ID, event.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId(event.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val contentText = "进行中 · ${formatTimeRange(event)}"
        val customContent = ongoingEventViews(context, event, contentText)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_payki_todo)
            .setContentTitle("进行中：${event.title}")
            .setContentText(contentText)
            .setSubText(event.location.takeIf { it.isNotBlank() })
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setWhen(startMillis)
            .setUsesChronometer(!event.allDay)
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(customContent)
            .setCustomBigContentView(customContent)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(event.id), notification)
    }

    fun cancel(context: Context, eventId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(eventId))
    }

    fun cancelAll(context: Context, eventId: Long) {
        cancelScheduled(context, eventId)
        cancel(context, eventId)
    }

    private fun cancelScheduled(context: Context, eventId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent(context, eventId, ACTION_START, START_OFFSET))
        alarmManager.cancel(pendingIntent(context, eventId, ACTION_END, END_OFFSET))
    }

    private fun scheduleAlarm(
        context: Context,
        eventId: Long,
        triggerAtMillis: Long,
        action: String,
        offset: Int
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = pendingIntent(context, eventId, action, offset)
        val canUseExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        if (!canUseExact) {
            scheduleInexact(alarmManager, triggerAtMillis, pendingIntent)
            return
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }.onFailure { throwable ->
            if (throwable is SecurityException) {
                scheduleInexact(alarmManager, triggerAtMillis, pendingIntent)
            } else {
                throw throwable
            }
        }
    }

    private fun scheduleInexact(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun pendingIntent(context: Context, eventId: Long, action: String, offset: Int): PendingIntent {
        val intent = Intent(context, OngoingEventReceiver::class.java).apply {
            this.action = action
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            putExtra(EXTRA_EVENT_ID, eventId)
        }
        return PendingIntent.getBroadcast(
            context,
            AlarmScheduler.requestCodeFor(eventId, 0L) + offset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun isEnabled(context: Context): Boolean {
        val app = context.applicationContext as? TodoApplication ?: return true
        return app.settingsStore.currentSettings().ongoingEventNotificationEnabled
    }

    private fun ongoingEventViews(context: Context, event: TodoItem, contentText: String): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_ongoing_event).apply {
            setTextViewText(R.id.ongoingEventEyebrow, "进行中日程")
            setTextViewText(R.id.ongoingEventTitle, event.title.ifBlank { "未命名日程" })
            setTextViewText(R.id.ongoingEventTime, contentText)
            val location = event.location.trim()
            setTextViewText(R.id.ongoingEventLocation, location)
            setViewVisibility(R.id.ongoingEventLocation, if (location.isBlank()) View.GONE else View.VISIBLE)
        }
    }

    private fun formatTimeRange(event: TodoItem): String {
        val startMillis = event.startAtMillis ?: return "未设置时间"
        if (event.allDay) return "全天"
        val endMillis = event.endAtMillis ?: startMillis
        val start = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val end = Instant.ofEpochMilli(endMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        return if (start.toLocalDate() == end.toLocalDate()) {
            "${start.format(timeFormatter)}-${end.format(timeFormatter)}"
        } else {
            "${start.format(dateTimeFormatter)}-${end.format(dateTimeFormatter)}"
        }
    }

    fun notificationId(eventId: Long): Int {
        return NOTIFICATION_BASE_ID + AlarmScheduler.requestCodeFor(eventId, 0L)
    }
}

internal fun shouldRefreshOngoingEventAfterEndBroadcast(event: TodoItem, nowMillis: Long): Boolean {
    val startMillis = event.startAtMillis ?: return false
    if (!event.isEvent || event.isHistory) return false
    val endMillis = (event.endAtMillis ?: startMillis).coerceAtLeast(startMillis)
    return nowMillis < endMillis
}
