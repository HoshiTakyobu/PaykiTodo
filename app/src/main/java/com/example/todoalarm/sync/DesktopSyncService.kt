package com.example.todoalarm.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.todoalarm.R
import com.example.todoalarm.TodoApplication

class DesktopSyncService : Service() {
    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_payki_todo)
            .setContentTitle("PaykiTodo 电脑同步已运行")
            .setContentText("同局域网电脑可通过浏览器连接此手机，直接编辑待办与日程。")
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        (application as TodoApplication).desktopSyncCoordinator.ensureRunning()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        (application as TodoApplication).desktopSyncCoordinator.ensureRunning()
        return START_STICKY
    }

    override fun onDestroy() {
        (application as TodoApplication).desktopSyncCoordinator.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "PaykiTodo 电脑同步",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于保持手机端的局域网同步控制台在线"
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    companion object {
        private const val CHANNEL_ID = "paykitodo_desktop_sync"
        private const val NOTIFICATION_ID = 42071

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, DesktopSyncService::class.java))
        }
    }
}
