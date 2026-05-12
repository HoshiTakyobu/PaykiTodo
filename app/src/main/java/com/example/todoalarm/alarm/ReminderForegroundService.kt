package com.example.todoalarm.alarm

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.app.KeyguardManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.accessibility.ReminderAccessibilityService
import com.example.todoalarm.data.ReminderChainStage
import com.example.todoalarm.data.ReminderChainStatus
import com.example.todoalarm.data.ReminderDeliveryMode
import com.example.todoalarm.ui.resolveTaskGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReminderForegroundService : Service() {
    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.Main.immediate + serviceJob)
    private lateinit var alertController: ReminderAlertController
    private var reminderNotifier: ReminderNotifier? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var userPresentReceiver: BroadcastReceiver? = null
    private var activeFullscreenTodoId: Long = -1L

    override fun onCreate() {
        super.onCreate()
        alertController = ReminderAlertController(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val explicitTodoId = intent?.getLongExtra(AlarmScheduler.EXTRA_TODO_ID, -1L) ?: -1L
        val todoId = if (explicitTodoId > 0L) {
            explicitTodoId
        } else {
            ActiveReminderStore.getActiveTodoId(this)
        }
        if (todoId <= 0L) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        ReminderChainLogger.log(
            context = this,
            todoId = todoId,
            source = "ReminderForegroundService",
            stage = ReminderChainStage.SERVICE_START,
            status = ReminderChainStatus.INFO,
            message = "explicit=$explicitTodoId"
        )

        scope.launch {
            val app = application as TodoApplication
            val todoItem = app.repository.getTodo(todoId)
            if (todoItem == null || todoItem.isHistory || !todoItem.reminderEnabled) {
                ReminderChainLogger.log(
                    context = this@ReminderForegroundService,
                    todoId = todoId,
                    source = "ReminderForegroundService",
                    stage = ReminderChainStage.SERVICE_INVALID_ITEM,
                    status = ReminderChainStatus.WARN,
                    message = if (todoItem == null) "null" else "history_or_disabled"
                )
                ActiveReminderStore.clearIfMatches(this@ReminderForegroundService, todoId)
                stopSelf(startId)
                return@launch
            }

            ReminderChainLogger.log(
                context = this@ReminderForegroundService,
                todoId = todoId,
                source = "ReminderForegroundService",
                stage = ReminderChainStage.SERVICE_ITEM_LOADED,
                status = ReminderChainStatus.OK,
                reminderAtMillis = todoItem.reminderAtMillis,
                message = "mode=${todoItem.reminderDeliveryMode}"
            )

            val settings = app.settingsStore.currentSettings()
            val useFullscreenReminder = settings.workQuietModeEnabled ||
                !todoItem.isEvent ||
                todoItem.reminderDeliveryModeEnum == ReminderDeliveryMode.FULLSCREEN
            if (useFullscreenReminder) {
                ActiveReminderStore.markActive(this@ReminderForegroundService, todoId)
                activeFullscreenTodoId = todoId
                ensureUserPresentReceiver()
            } else {
                ActiveReminderStore.clearIfMatches(this@ReminderForegroundService, todoId)
                ActiveReminderStore.clearActivityHandoff(this@ReminderForegroundService, todoId)
                activeFullscreenTodoId = -1L
                unregisterUserPresentReceiver()
            }
            val notifier = ReminderNotifier(this@ReminderForegroundService)
            reminderNotifier = notifier
            val taskGroup = resolveTaskGroup(todoItem, app.repository.getGroup(todoItem.groupId))
            val notification = notifier.build(
                todoItem = todoItem,
                taskGroup = taskGroup,
                requestFullscreen = useFullscreenReminder
            )
            startInForeground(todoId, notification)
            ReminderChainLogger.log(
                context = this@ReminderForegroundService,
                todoId = todoId,
                source = "ReminderForegroundService",
                stage = ReminderChainStage.NOTIFICATION_POSTED,
                status = ReminderChainStatus.OK,
                reminderAtMillis = todoItem.reminderAtMillis,
                message = if (useFullscreenReminder) "fullscreen_requested" else "notification_only"
            )
            alertController.start(todoItem)
            wakeDevice()
            if (useFullscreenReminder) {
                attemptFullscreenReminder(todoId)
                FULLSCREEN_RETRY_DELAYS_MS.forEach { delayMs ->
                    scheduleReminderRetry(todoId, delayMs)
                }
            }
            delay(REMINDER_SESSION_DURATION_MS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
            stopSelf(startId)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        alertController.shutdown()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
        reminderNotifier = null
        unregisterUserPresentReceiver()
        activeFullscreenTodoId = -1L
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun startInForeground(todoId: Long, notification: Notification) {
        val id = ReminderNotifier.notificationId(todoId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(id, notification)
        }
    }

    private fun wakeDevice() {
        val powerManager = getSystemService(PowerManager::class.java) ?: return
        val lock = wakeLock ?: powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "PaykiTodo:ReminderWakeLock"
        ).also { wakeLock = it }
        if (!lock.isHeld) {
            @Suppress("DEPRECATION")
            lock.acquire(10_000L)
        }
    }

    private fun triggerAccessibilityOverlay(todoId: Long, forceOverlay: Boolean = false) {
        ReminderChainLogger.log(
            context = this,
            todoId = todoId,
            source = "ReminderForegroundService",
            stage = ReminderChainStage.ACCESSIBILITY_TRIGGER,
            status = ReminderChainStatus.INFO,
            message = "forceOverlay=$forceOverlay"
        )
        sendBroadcast(
            Intent(ReminderAccessibilityService.ACTION_TRIGGER_REMINDER_OVERLAY).apply {
                setPackage(packageName)
                putExtra(ReminderAccessibilityService.EXTRA_TODO_ID, todoId)
                putExtra(ReminderAccessibilityService.EXTRA_FORCE_OVERLAY, forceOverlay)
            }
        )
    }

    private fun attemptFullscreenReminder(todoId: Long) {
        ActiveReminderStore.refreshActive(this, todoId)
        ReminderChainLogger.log(
            context = this,
            todoId = todoId,
            source = "ReminderForegroundService",
            stage = ReminderChainStage.FULLSCREEN_ATTEMPT,
            status = ReminderChainStatus.INFO,
            message = if (isDeviceLocked()) "locked" else "unlocked"
        )
        if (isDeviceLocked()) {
            ActiveReminderStore.clearActivityHandoff(this, todoId)
            val overlayShown = ReminderAccessibilityService.showOverlayNow(todoId, forceOverlay = true)
            triggerAccessibilityOverlay(todoId, forceOverlay = true)
            if (!overlayShown) {
                launchReminderActivity(todoId)
            }
            return
        }
        launchReminderActivity(todoId)
    }

    private fun scheduleReminderRetry(todoId: Long, delayMs: Long) {
        scope.launch {
            delay(delayMs)
            if (ActiveReminderStore.getActiveTodoId(this@ReminderForegroundService) != todoId) return@launch
            ActiveReminderStore.refreshActive(this@ReminderForegroundService, todoId)
            if (isDeviceLocked()) {
                ActiveReminderStore.clearActivityHandoff(this@ReminderForegroundService, todoId)
                val overlayShown = ReminderAccessibilityService.showOverlayNow(todoId, forceOverlay = true)
                triggerAccessibilityOverlay(todoId, forceOverlay = true)
                if (!overlayShown) {
                    launchReminderActivity(todoId)
                }
            } else {
                if (!ActiveReminderStore.isActivityHandoffPending(this@ReminderForegroundService, todoId)) return@launch
                ActiveReminderStore.clearActivityHandoff(this@ReminderForegroundService, todoId)
                launchReminderActivity(todoId)
            }
        }
    }

    private fun launchReminderActivity(todoId: Long) {
        ActiveReminderStore.markActivityHandoff(this, todoId)
        runCatching {
            startActivity(com.example.todoalarm.ui.ReminderActivity.createIntent(this, todoId))
            ReminderChainLogger.log(
                context = this,
                todoId = todoId,
                source = "ReminderForegroundService",
                stage = ReminderChainStage.FULLSCREEN_ACTIVITY_LAUNCH,
                status = ReminderChainStatus.OK
            )
        }.onFailure {
            ActiveReminderStore.clearActivityHandoff(this, todoId)
            ReminderChainLogger.log(
                context = this,
                todoId = todoId,
                source = "ReminderForegroundService",
                stage = ReminderChainStage.FULLSCREEN_ACTIVITY_FAILED,
                status = ReminderChainStatus.WARN,
                message = it.javaClass.simpleName
            )
            val forceOverlay = isDeviceLocked()
            ReminderAccessibilityService.showOverlayNow(todoId, forceOverlay = forceOverlay)
            triggerAccessibilityOverlay(todoId, forceOverlay = forceOverlay)
        }
    }

    private fun ensureUserPresentReceiver() {
        if (userPresentReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val todoId = activeFullscreenTodoId
                if (todoId <= 0L) return
                if (ActiveReminderStore.getActiveTodoId(this@ReminderForegroundService) != todoId) return
                when (intent?.action) {
                    Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON -> {
                        ActiveReminderStore.refreshActive(this@ReminderForegroundService, todoId)
                        attemptFullscreenReminder(todoId)
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        userPresentReceiver = receiver
    }

    private fun unregisterUserPresentReceiver() {
        val receiver = userPresentReceiver ?: return
        runCatching { unregisterReceiver(receiver) }
        userPresentReceiver = null
    }

    private fun isDeviceLocked(): Boolean {
        val keyguardManager = getSystemService(KeyguardManager::class.java) ?: return false
        return keyguardManager.isDeviceLocked || keyguardManager.isKeyguardLocked
    }

    companion object {
        private const val REMINDER_SESSION_DURATION_MS = 5 * 60 * 1000L
        private val FULLSCREEN_RETRY_DELAYS_MS = listOf(1_200L, 3_200L, 10_000L, 20_000L, 45_000L, 90_000L, 180_000L)

        fun start(context: android.content.Context, todoId: Long) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ReminderForegroundService::class.java).putExtra(AlarmScheduler.EXTRA_TODO_ID, todoId)
            )
        }
    }
}
