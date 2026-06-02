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
    private var connectionWatchdogJob: Job? = null
    private var startedAtMillis: Long = 0L
    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastNotificationConnected: Boolean? = null
    private var lastNotificationKeepAlive: Boolean? = null

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
        startForeground(NOTIFICATION_ID, buildNotification(keepAlive = keepAlive, connected = false, contentIntent = contentIntent))
        app.desktopSyncCoordinator.ensureRunning()
        updateKeepAliveLocks(keepAlive)
        startConnectionWatchdog()
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
        startConnectionWatchdog()
        return START_STICKY
    }

    override fun onDestroy() {
        connectionWatchdogJob?.cancel()
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

    private fun startConnectionWatchdog() {
        connectionWatchdogJob?.cancel()
        connectionWatchdogJob = serviceScope.launch {
            while (true) {
                val delayMillis = checkDesktopConnectionAndUpdateService()
                if (delayMillis == null) return@launch
                delay(delayMillis)
            }
        }
    }

    private fun checkDesktopConnectionAndUpdateService(): Long? {
        val app = application as TodoApplication
        val settings = app.settingsStore.currentSettings()
        if (!settings.desktopSyncEnabled) {
            app.desktopSyncCoordinator.stop()
            stopForegroundAndSelf()
            return null
        }

        app.desktopSyncCoordinator.ensureRunning()
        val now = System.currentTimeMillis()
        if (startedAtMillis <= 0L) startedAtMillis = now
        val lastAuthorizedAt = app.desktopSyncCoordinator.lastAuthorizedClientAtMillis()
        val referenceMillis = lastAuthorizedAt.takeIf { it > 0L } ?: startedAtMillis
        val connected = lastAuthorizedAt > 0L && now - lastAuthorizedAt < NO_CLIENT_AUTO_STOP_MILLIS
        updateForegroundNotification(keepAlive = settings.desktopSyncWifiKeepAlive, connected = connected)

        val remainingMillis = referenceMillis + NO_CLIENT_AUTO_STOP_MILLIS - now
        if (remainingMillis <= 0L) {
            app.settingsStore.updateDesktopSyncEnabled(false)
            app.desktopSyncCoordinator.stop()
            stopForegroundAndSelf()
            return null
        }
        return remainingMillis.coerceAtMost(CONNECTION_WATCHDOG_INTERVAL_MILLIS).coerceAtLeast(1_000L)
    }

    private fun updateForegroundNotification(keepAlive: Boolean, connected: Boolean) {
        if (lastNotificationConnected == connected && lastNotificationKeepAlive == keepAlive) return
        lastNotificationConnected = connected
        lastNotificationKeepAlive = keepAlive
        startForeground(NOTIFICATION_ID, buildNotification(keepAlive = keepAlive, connected = connected, contentIntent = desktopSyncSettingsIntent()))
    }

    private fun buildNotification(
        keepAlive: Boolean,
        connected: Boolean,
        contentIntent: PendingIntent
    ): Notification {
        val title = if (connected) {
            "PaykiTodo 电脑同步已连接"
        } else {
            "PaykiTodo 电脑同步等待连接"
        }
        val text = when {
            connected && keepAlive -> "已检测到桌面端心跳 · 保持网络唤醒；断开 5 分钟后自动关闭"
            connected -> "已检测到桌面端心跳；断开 5 分钟后自动关闭"
            keepAlive -> "等待电脑输入访问密钥 · 5 分钟无连接将自动关闭 · 已保持网络唤醒"
            else -> "等待电脑输入访问密钥 · 5 分钟无连接将自动关闭"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_payki_todo)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun desktopSyncSettingsIntent(): PendingIntent {
        return PendingIntent.getActivity(
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
    }

    private fun stopForegroundAndSelf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    private fun updateKeepAliveLocks(enabled: Boolean) {
        if (!enabled) {
            releaseLocks()
            return
        }
        if (wifiLock?.isHeld != true) {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager
            wifiLock = wifiManager
                ?.createWifiLock(desktopSyncWifiLockMode(), "PaykiTodo:DesktopSync")
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
        const val NO_CLIENT_AUTO_STOP_MILLIS = 5 * 60 * 1000L
        private const val CONNECTION_WATCHDOG_INTERVAL_MILLIS = 15 * 1000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, DesktopSyncService::class.java))
        }
    }

    private fun immutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }

    private fun desktopSyncWifiLockMode(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        } else {
            @Suppress("DEPRECATION")
            WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }
    }
}
