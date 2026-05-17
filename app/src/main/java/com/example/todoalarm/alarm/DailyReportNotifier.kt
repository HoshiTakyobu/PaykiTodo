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
import com.example.todoalarm.ui.MainActivity

object DailyReportNotifier {
    private const val CHANNEL_ID = "ai_report_channel"
    private const val NOTIFICATION_ID_DAILY = 91_000
    private const val NOTIFICATION_ID_WEEKLY = 91_001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "AI 日报 / 周报",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AI 自动生成的日报、周报完成通知"
                setShowBadge(false)
            }
        )
    }

    fun postReportNotification(
        context: Context,
        reportId: Long,
        reportTitle: String,
        preview: String,
        weekly: Boolean = false
    ) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_AI_REPORT_ID, reportId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            if (weekly) NOTIFICATION_ID_WEEKLY else NOTIFICATION_ID_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = preview.replace(Regex("\\s+"), " ").trim().take(120).ifBlank { "报告已写入 AI 报告归档。" }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_payki_todo)
            .setContentTitle(reportTitle)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        NotificationManagerCompat.from(context).notify(
            if (weekly) NOTIFICATION_ID_WEEKLY else NOTIFICATION_ID_DAILY,
            notification
        )
    }
}
