package com.example.todoalarm.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.todoalarm.R
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DesktopSyncService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var autoStopJob: Job? = null
    private var startedAtMillis: Long = 0L
    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as TodoApplication
        if (!app.settingsStore.currentSettings().desktopSyncEnabled) {
            stopSelf()
            return
        }
        startedAtMillis = System.currentTimeMillis()
        app.desktopSyncCoordinator.resetClientTracking()
        ensureChannel()
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(
                    MainActivity.EXTRA_OPEN_SETTINGS_SECTION,
                    MainActivity.SETTINGS_SECTION_DESKTOP_SYNC
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )
        val keepAlive = app.settingsStore.currentSettings().desktopSyncWifiKeepAlive
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_payki_todo)
            .setContentTitle("PaykiTodo 电脑同步已运行")
            .setContentText(if (keepAlive) "桌面端连接中 · 已保持网络唤醒" else "桌面端连接中")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        app.desktopSyncCoordinator.ensureRunning()
        updateKeepAliveLocks(keepAlive)
        scheduleAutoStopIfNoClient()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as TodoApplication
        if (!app.settingsStore.currentSettings().desktopSyncEnabled) {
            app.desktopSyncCoordinator.stop()
            stopSelf(startId)
            return START_NOT_STICKY
        }
        app.desktopSyncCoordinator.ensureRunning()
        if (startedAtMillis == 0L) {
            startedAtMillis = System.currentTimeMillis()
        }
        updateKeepAliveLocks(app.settingsStore.currentSettings().desktopSyncWifiKeepAlive)
        scheduleAutoStopIfNoClient()
        return START_STICKY
    }

    override fun onDestroy() {
        autoStopJob?.cancel()
        releaseLocks()
        serviceScope.cancel()
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

    private fun scheduleAutoStopIfNoClient() {
        autoStopJob?.cancel()
        val start = startedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis().also { startedAtMillis = it }
        val remainingMillis = (start + NO_CLIENT_AUTO_STOP_MILLIS - System.currentTimeMillis()).coerceAtLeast(0L)
        autoStopJob = serviceScope.launch {
            delay(remainingMillis)
            val app = application as TodoApplication
            if (!app.settingsStore.currentSettings().desktopSyncEnabled) return@launch
            val lastClientAt = app.desktopSyncCoordinator.lastAuthorizedClientAtMillis()
            if (lastClientAt >= start) return@launch
            app.settingsStore.updateDesktopSyncEnabled(false)
            app.desktopSyncCoordinator.stop()
            stopSelf()
        }
    }

    private fun updateKeepAliveLocks(enabled: Boolean) {
        if (!enabled) {
            releaseLocks()
            return
        }
        if (wifiLock?.isHeld != true) {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager
            wifiLock = wifiManager
                ?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "PaykiTodo:DesktopSync")
                ?.apply { acquire() }
        }
        if (wakeLock?.isHeld != true) {
            val powerManager = applicationContext.getSystemService(POWER_SERVICE) as? PowerManager
            wakeLock = powerManager
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PaykiTodo:DesktopSync")
                ?.apply { acquire() }
        }
    }

    private fun releaseLocks() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
        if (wifiLock?.isHeld == true) {
            wifiLock?.release()
        }
        wifiLock = null
    }

    companion object {
        private const val CHANNEL_ID = "paykitodo_desktop_sync"
        private const val NOTIFICATION_ID = 42071
        private const val NO_CLIENT_AUTO_STOP_MILLIS = 5 * 60 * 1000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, DesktopSyncService::class.java))
        }
    }

    private fun immutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }
}
