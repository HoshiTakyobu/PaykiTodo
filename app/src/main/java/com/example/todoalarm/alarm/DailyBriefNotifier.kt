package com.example.todoalarm.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.todoalarm.R
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.PlannerItemType
import com.example.todoalarm.ui.MainActivity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DailyBriefNotifier {
    private const val CHANNEL_ID = "daily_brief"
    private const val NOTIFICATION_ID = 92_000

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "每日摘要",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "每天固定时间汇总今日待办、日程和临近倒数日"
                setShowBadge(false)
            }
        )
    }

    suspend fun postDailyBrief(context: Context) {
        val app = context.applicationContext as TodoApplication
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val items = app.repository.getActiveItemsForBoardRange(today)
        val todoCount = items.count { item ->
            item.itemTypeEnum == PlannerItemType.TODO &&
                !item.hiddenFromBoard &&
                (item.dueAtMillis == com.example.todoalarm.data.NO_DUE_DATE_MILLIS ||
                    item.dueAtMillis in start until end ||
                    item.missed)
        }
        val eventCount = items.count { item ->
            item.itemTypeEnum == PlannerItemType.EVENT &&
                item.startAtMillis != null &&
                item.startAtMillis < end &&
                (item.endAtMillis ?: item.startAtMillis) >= start
        }
        val countdown = items
            .filter { it.countdownEnabled }
            .mapNotNull { item ->
                val target = item.startAtMillis ?: item.dueAtMillis.takeIf { item.hasDueDate }
                target?.let { item to it }
            }
            .filter { (_, targetMillis) -> targetMillis >= start }
            .minByOrNull { (_, targetMillis) -> targetMillis }
            ?.let { (item, targetMillis) ->
                val targetDate = Instant.ofEpochMilli(targetMillis).atZone(zone).toLocalDate()
                val days = ChronoUnit.DAYS.between(today, targetDate)
                if (days in 0..7) " · 距${item.title.take(8)} $days 天" else ""
            }
            .orEmpty()
        post(
            context = context,
            title = "今日 · ${today.format(DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINA))}",
            content = "${todoCount} 件待办 · ${eventCount} 个日程$countdown"
        )
    }

    private fun post(context: Context, title: String, content: String) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_BOARD, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_payki_todo)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
