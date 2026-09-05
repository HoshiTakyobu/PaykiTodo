package com.example.todoalarm.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.todoalarm.R
import com.example.todoalarm.data.PlanningAiProvider

object PlanningAiSwitchNotifier {
    private const val CHANNEL_ID = "ai_source_status_channel"
    private const val NOTIFICATION_ID = 91_010

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "AI 源状态",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "AI 源或模型在真实业务调用失败后发生切换时通知"
                setShowBadge(false)
            }
        )
    }

    fun post(context: Context, provider: PlanningAiProvider) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val providerName = provider.name.ifBlank { "未命名服务" }
        val modelName = provider.model.ifBlank { "未命名模型" }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_payki_todo)
            .setContentTitle("AI 源已自动切换")
            .setContentText("当前使用 $providerName / $modelName")
            .setStyle(NotificationCompat.BigTextStyle().bigText("真实 AI 调用失败后，已切换为 $providerName / $modelName。"))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
