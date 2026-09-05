package com.example.todoalarm.alarm

import android.app.Notification
import android.app.NotificationManager
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
import com.example.todoalarm.CrashLogger
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.accessibility.ReminderAccessibilityService
import com.example.todoalarm.data.ReminderChainStage
import com.example.todoalarm.data.ReminderChainStatus
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.ResolvedTaskGroup
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
    private var foregroundTodoId: Long = -1L
    private var keepDetachedNotification = false

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
        if (intent?.action == ACTION_DISMISS_WITHOUT_NOTIFICATION) {
            keepDetachedNotification = false
            stopAlertingWithoutNotification(todoId, startId)
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_DISMISS_KEEP_NOTIFICATION) {
            stopAlertingKeepingNotification(
                todoId = todoId,
                startId = startId,
                message = intent.getStringExtra(EXTRA_RETAINED_MESSAGE).orEmpty()
            )
            return START_NOT_STICKY
        }
        keepDetachedNotification = false
        if (todoId <= 0L) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val triggeredAtMillis = intent
            ?.getLongExtra(AlarmScheduler.EXTRA_REMINDER_AT, -1L)
            ?.takeIf { it > 0L }
            ?: ActiveReminderStore.getTriggeredAtMillis(this, todoId)
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
            if (!app.settingsStore.hasValidRemoteSession()) {
                ReminderChainLogger.log(
                    context = this@ReminderForegroundService,
                    todoId = todoId,
                    source = "ReminderForegroundService",
                    stage = ReminderChainStage.SERVICE_INVALID_ITEM,
                    status = ReminderChainStatus.WARN,
                    message = "authentication_required"
                )
                dismiss(this@ReminderForegroundService, todoId)
                stopSelf(startId)
                return@launch
            }
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
                dismiss(this@ReminderForegroundService, todoId)
                stopSelf(startId)
                return@launch
            }

            if (triggeredAtMillis != null && !app.alarmScheduler.isReminderScheduled(todoId, triggeredAtMillis)) {
                ReminderChainLogger.log(
                    context = this@ReminderForegroundService,
                    todoId = todoId,
                    source = "ReminderForegroundService",
                    stage = ReminderChainStage.SERVICE_INVALID_ITEM,
                    status = ReminderChainStatus.INFO,
                    reminderAtMillis = triggeredAtMillis,
                    message = "stale_alarm_ignored"
                )
                discardReminderStart(todoId, startId)
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

            val useFullscreenReminder = shouldUseFullscreenReminder(todoItem)
            if (useFullscreenReminder) {
                ActiveReminderStore.markActive(this@ReminderForegroundService, todoId, triggeredAtMillis)
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
                requestFullscreen = useFullscreenReminder,
                showReminderContent = true,
                triggeredAtMillis = triggeredAtMillis
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
                attemptFullscreenReminder(todoId, triggeredAtMillis = triggeredAtMillis)
                FULLSCREEN_RETRY_DELAYS_MS.forEach { delayMs ->
                    scheduleReminderRetry(todoId, triggeredAtMillis, delayMs)
                }
            }
            delay(REMINDER_SESSION_DURATION_MS)
            if (todoItem.alarmMode && ActiveReminderStore.getActiveTodoId(this@ReminderForegroundService) == todoId) {
                runAlarmModeTimeoutRetries(app, todoItem.id)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
            keepDetachedNotification = true
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        alertController.shutdown()
        if (!keepDetachedNotification) {
            removeForegroundNotification()
        }
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
        reminderNotifier = null
        unregisterUserPresentReceiver()
        foregroundTodoId.takeIf { it > 0L }?.let { todoId ->
            ActiveReminderStore.clearServiceIfMatches(this, todoId)
        }
        activeFullscreenTodoId = -1L
        foregroundTodoId = -1L
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun startInForeground(todoId: Long, notification: Notification) {
        foregroundTodoId = todoId
        ActiveReminderStore.markServiceActive(this, todoId)
        val id = ReminderNotifier.notificationId(todoId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(id, notification)
        }
    }

    private fun stopAlertingKeepingNotification(todoId: Long, startId: Int, message: String) {
        alertController.stop()
        ReminderAccessibilityService.hideOverlayNow(todoId)
        ActiveReminderStore.clearIfMatches(this, todoId)
        ActiveReminderStore.clearActivityHandoff(this, todoId)
        ActiveReminderStore.clearReminderSurfaceMarker(this, todoId)
        unregisterUserPresentReceiver()
        if (foregroundTodoId == todoId) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
        }
        keepDetachedNotification = true
        scope.launch {
            val app = application as TodoApplication
            val item = app.repository.getTodo(todoId)
            if (item != null && !item.isHistory) {
                val taskGroup = resolveTaskGroup(item, app.repository.getGroup(item.groupId))
                (reminderNotifier ?: ReminderNotifier(this@ReminderForegroundService)).showRetained(
                    todoItem = item,
                    message = message,
                    taskGroup = taskGroup
                )
            }
            stopSelf(startId)
        }
    }

    private fun stopAlertingWithoutNotification(todoId: Long, startId: Int) {
        alertController.stop()
        ReminderAccessibilityService.hideOverlayNow(todoId)
        ActiveReminderStore.clearIfMatches(this, todoId)
        ActiveReminderStore.clearActivityHandoff(this, todoId)
        ActiveReminderStore.clearReminderSurfaceMarker(this, todoId)
        unregisterUserPresentReceiver()
        if (foregroundTodoId == todoId) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        }
        getSystemService(NotificationManager::class.java)?.cancel(ReminderNotifier.notificationId(todoId))
        keepDetachedNotification = false
        stopSelf(startId)
    }

    private fun discardReminderStart(todoId: Long, startId: Int) {
        alertController.stop()
        ReminderAccessibilityService.hideOverlayNow(todoId)
        ActiveReminderStore.clearIfMatches(this, todoId)
        ActiveReminderStore.clearActivityHandoff(this, todoId)
        ActiveReminderStore.clearReminderSurfaceMarker(this, todoId)
        getSystemService(NotificationManager::class.java)?.cancel(ReminderNotifier.notificationId(todoId))
        stopSelf(startId)
    }

    private fun removeForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        foregroundTodoId.takeIf { it > 0L }?.let { todoId ->
            getSystemService(NotificationManager::class.java)?.cancel(ReminderNotifier.notificationId(todoId))
        }
    }

    private suspend fun runAlarmModeTimeoutRetries(app: TodoApplication, todoId: Long) {
        alertController.stop()
        app.repository.getTodo(todoId)?.let { latest ->
            val taskGroup = resolveTaskGroup(latest, app.repository.getGroup(latest.groupId))
            (reminderNotifier ?: ReminderNotifier(this)).showUnhandledAlarmMode(latest, taskGroup)
        }
        ReminderChainLogger.log(
            context = this,
            todoId = todoId,
            source = "ReminderForegroundService",
            stage = ReminderChainStage.SERVICE_START,
            status = ReminderChainStatus.INFO,
            message = "alarm_mode_timeout_downgrade"
        )
        repeat(ALARM_MODE_RETRY_COUNT) { index ->
            delay(ALARM_MODE_RETRY_INTERVAL_MS)
            val latest = app.repository.getTodo(todoId)
            if (
                latest == null ||
                latest.isHistory ||
                !latest.reminderEnabled ||
                ActiveReminderStore.getActiveTodoId(this) != todoId
            ) {
                return
            }
            ReminderChainLogger.log(
                context = this,
                todoId = todoId,
                source = "ReminderForegroundService",
                stage = ReminderChainStage.SERVICE_START,
                status = ReminderChainStatus.INFO,
                message = "alarm_mode_retry=${index + 1}"
            )
            alertController.start(latest.copy(alarmMode = false))
            delay(ALARM_MODE_RETRY_RING_MS)
            if (ActiveReminderStore.getActiveTodoId(this) == todoId) {
                alertController.stop()
            }
        }
    }

    private fun wakeDevice() {
        val powerManager = getSystemService(PowerManager::class.java) ?: return
        val lock = wakeLock ?: createReminderWakeLock(powerManager).also { wakeLock = it }
        if (!lock.isHeld) {
            lock.acquire(10_000L)
        }
    }

    @Suppress("DEPRECATION")
    private fun createReminderWakeLock(powerManager: PowerManager): PowerManager.WakeLock {
        return powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "PaykiTodo:ReminderWakeLock"
        )
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

    private fun attemptFullscreenReminder(
        todoId: Long,
        triggeredAtMillis: Long? = ActiveReminderStore.getTriggeredAtMillis(this, todoId),
    ) {
        ActiveReminderStore.refreshActive(this, todoId)
        if (ActiveReminderStore.wasReminderSurfaceRecentlyShown(this, todoId, FULLSCREEN_RESHOW_COOLDOWN_MS)) {
            logRecentSurfaceSkip(todoId)
            return
        }
        val locked = isDeviceLocked()
        val fullScreenIntentAllowed = canUseFullScreenIntent()
        ReminderChainLogger.log(
            context = this,
            todoId = todoId,
            source = "ReminderForegroundService",
            stage = ReminderChainStage.FULLSCREEN_ATTEMPT,
            status = if (fullScreenIntentAllowed) ReminderChainStatus.INFO else ReminderChainStatus.WARN,
            message = "locked=$locked; fullScreenIntentAllowed=$fullScreenIntentAllowed"
        )
        // When Android accepts the notification full-screen intent, it is the only
        // launch path. Starting the Activity here as well can show duplicate reminders.
        if (fullScreenIntentAllowed) return
        if (locked) {
            ActiveReminderStore.clearActivityHandoff(this, todoId)
            val overlayShown = ReminderAccessibilityService.showOverlayNow(todoId, forceOverlay = true)
            if (!overlayShown) {
                triggerAccessibilityOverlay(todoId, forceOverlay = true)
            }
            if (!overlayShown) {
                launchReminderActivity(todoId, triggeredAtMillis)
            }
            return
        }
        launchReminderActivity(todoId, triggeredAtMillis)
    }

    private fun scheduleReminderRetry(todoId: Long, triggeredAtMillis: Long?, delayMs: Long) {
        scope.launch {
            delay(delayMs)
            if (ActiveReminderStore.getActiveTodoId(this@ReminderForegroundService) != todoId) return@launch
            if (canUseFullScreenIntent()) return@launch
            if (ActiveReminderStore.wasReminderSurfaceRecentlyShown(this@ReminderForegroundService, todoId, FULLSCREEN_RESHOW_COOLDOWN_MS)) {
                logRecentSurfaceSkip(todoId)
                return@launch
            }
            ActiveReminderStore.refreshActive(this@ReminderForegroundService, todoId)
            if (isDeviceLocked()) {
                ActiveReminderStore.clearActivityHandoff(this@ReminderForegroundService, todoId)
                val overlayShown = ReminderAccessibilityService.showOverlayNow(todoId, forceOverlay = true)
                if (!overlayShown) {
                    triggerAccessibilityOverlay(todoId, forceOverlay = true)
                }
                if (!overlayShown) {
                    launchReminderActivity(todoId, triggeredAtMillis)
                }
            } else {
                if (!ActiveReminderStore.isActivityHandoffPending(this@ReminderForegroundService, todoId)) return@launch
                ActiveReminderStore.clearActivityHandoff(this@ReminderForegroundService, todoId)
                val overlayShown = ReminderAccessibilityService.showOverlayNow(todoId, forceOverlay = true)
                if (!overlayShown) {
                    triggerAccessibilityOverlay(todoId, forceOverlay = true)
                }
                if (!overlayShown) {
                    launchReminderActivity(todoId, triggeredAtMillis)
                }
            }
        }
    }

    private fun launchReminderActivity(todoId: Long, triggeredAtMillis: Long?) {
        if (ActiveReminderStore.wasReminderSurfaceRecentlyShown(this, todoId, FULLSCREEN_RESHOW_COOLDOWN_MS)) {
            logRecentSurfaceSkip(todoId)
            return
        }
        ActiveReminderStore.markActivityHandoff(this, todoId)
        runCatching {
            startActivity(com.example.todoalarm.ui.ReminderActivity.createIntent(this, todoId, triggeredAtMillis))
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
            ReminderAccessibilityService.showOverlayNow(todoId, forceOverlay = true)
            triggerAccessibilityOverlay(todoId, forceOverlay = true)
        }
    }

    private fun logRecentSurfaceSkip(todoId: Long) {
        ReminderChainLogger.log(
            context = this,
            todoId = todoId,
            source = "ReminderForegroundService",
            stage = ReminderChainStage.FULLSCREEN_ATTEMPT,
            status = ReminderChainStatus.INFO,
            message = "skip_recent_surface"
        )
    }

    private fun canUseFullScreenIntent(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val notificationManager = getSystemService(NotificationManager::class.java) ?: return true
        return notificationManager.canUseFullScreenIntent()
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
        private const val ACTION_DISMISS_KEEP_NOTIFICATION = "com.example.todoalarm.action.DISMISS_KEEP_NOTIFICATION"
        private const val ACTION_DISMISS_WITHOUT_NOTIFICATION = "com.example.todoalarm.action.DISMISS_WITHOUT_NOTIFICATION"
        private const val EXTRA_RETAINED_MESSAGE = "com.example.todoalarm.extra.RETAINED_MESSAGE"
        private const val REMINDER_SESSION_DURATION_MS = 5 * 60 * 1000L
        private const val ALARM_MODE_RETRY_INTERVAL_MS = 2 * 60 * 1000L
        private const val ALARM_MODE_RETRY_RING_MS = 30 * 1000L
        private const val ALARM_MODE_RETRY_COUNT = 3
        private const val FULLSCREEN_RESHOW_COOLDOWN_MS = 60 * 1000L
        private val FULLSCREEN_RETRY_DELAYS_MS = listOf(1_500L, 4_000L, 10_000L)

        fun start(context: android.content.Context, todoId: Long, triggeredAtMillis: Long? = null) {
            if (triggeredAtMillis != null && !ReminderDispatchTracker.claim(context, todoId, triggeredAtMillis)) {
                ReminderChainLogger.log(
                    context = context,
                    todoId = todoId,
                    source = "ReminderForegroundService",
                    stage = ReminderChainStage.SERVICE_START,
                    status = ReminderChainStatus.INFO,
                    reminderAtMillis = triggeredAtMillis,
                    message = "duplicate_start_ignored"
                )
                return
            }
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, ReminderForegroundService::class.java).apply {
                        putExtra(AlarmScheduler.EXTRA_TODO_ID, todoId)
                        triggeredAtMillis?.let { putExtra(AlarmScheduler.EXTRA_REMINDER_AT, it) }
                    }
                )
            } catch (error: RuntimeException) {
                // Android 12+ can reject a background foreground-service start. A reminder
                // must still degrade to a normal notification instead of crashing the app.
                ReminderChainLogger.log(
                    context = context,
                    todoId = todoId,
                    source = "ReminderForegroundService",
                    stage = ReminderChainStage.SERVICE_START,
                    status = ReminderChainStatus.WARN,
                    reminderAtMillis = triggeredAtMillis,
                    message = "start_rejected=${error.javaClass.simpleName};fallback=notification"
                )
                showNotificationFallback(context, todoId, triggeredAtMillis)
            }
        }

        private fun showNotificationFallback(
            context: Context,
            todoId: Long,
            triggeredAtMillis: Long?
        ) {
            val app = context.applicationContext as? TodoApplication ?: return
            app.applicationScope.launch {
                runCatching {
                    val item = app.repository.getTodo(todoId)
                    if (item == null || item.isHistory || !item.reminderEnabled) return@runCatching
                    if (triggeredAtMillis != null && !app.alarmScheduler.isReminderScheduled(todoId, triggeredAtMillis)) {
                        return@runCatching
                    }
                    val requestFullscreen = shouldUseFullscreenReminder(item)
                    if (requestFullscreen) {
                        ActiveReminderStore.markActive(app, todoId, triggeredAtMillis)
                    } else {
                        ActiveReminderStore.clearIfMatches(app, todoId)
                    }
                    val taskGroup = resolveTaskGroup(item, app.repository.getGroup(item.groupId))
                    ReminderNotifier(app).showTriggered(
                        todoItem = item,
                        taskGroup = taskGroup,
                        requestFullscreen = requestFullscreen,
                        triggeredAtMillis = triggeredAtMillis
                    )
                    ReminderChainLogger.log(
                        context = app,
                        todoId = todoId,
                        source = "ReminderForegroundService",
                        stage = ReminderChainStage.NOTIFICATION_POSTED,
                        status = ReminderChainStatus.WARN,
                        reminderAtMillis = triggeredAtMillis,
                        message = if (requestFullscreen) {
                            "foreground_service_rejected;fullscreen_notification_fallback"
                        } else {
                            "foreground_service_rejected;notification_fallback"
                        }
                    )
                }.onFailure { error ->
                    CrashLogger.recordNonFatal(error)
                    ReminderChainLogger.log(
                        context = app,
                        todoId = todoId,
                        source = "ReminderForegroundService",
                        stage = ReminderChainStage.NOTIFICATION_POSTED,
                        status = ReminderChainStatus.ERROR,
                        reminderAtMillis = triggeredAtMillis,
                        message = "notification_fallback_failed=${error.javaClass.simpleName}"
                    )
                }
            }
        }

        fun dismiss(context: Context, todoId: Long) {
            ReminderAccessibilityService.hideOverlayNow(todoId)
            context.getSystemService(NotificationManager::class.java)
                ?.cancel(ReminderNotifier.notificationId(todoId))
            ActiveReminderStore.clearIfMatches(context, todoId)
            ActiveReminderStore.clearActivityHandoff(context, todoId)
            ActiveReminderStore.clearReminderSurfaceMarker(context, todoId)
            context.getSystemService(NotificationManager::class.java)
                ?.cancel(ReminderNotifier.notificationId(todoId))
            if (ActiveReminderStore.getServiceTodoId(context) == todoId) {
                runCatching {
                    context.startService(
                        Intent(context, ReminderForegroundService::class.java).apply {
                            action = ACTION_DISMISS_WITHOUT_NOTIFICATION
                            putExtra(AlarmScheduler.EXTRA_TODO_ID, todoId)
                        }
                    )
                }
                context.stopService(Intent(context, ReminderForegroundService::class.java))
            }
            context.getSystemService(NotificationManager::class.java)
                ?.cancel(ReminderNotifier.notificationId(todoId))
        }

        fun dismissForReschedule(context: Context, todoId: Long) {
            ReminderAccessibilityService.hideOverlayNow(todoId)
            ReminderDispatchTracker.clear(context, todoId)
            ActiveReminderStore.clearIfMatches(context, todoId)
            ActiveReminderStore.clearActivityHandoff(context, todoId)
            ActiveReminderStore.clearReminderSurfaceMarker(context, todoId)
            context.getSystemService(NotificationManager::class.java)
                ?.cancel(ReminderNotifier.notificationId(todoId))
            context.stopService(Intent(context, ReminderForegroundService::class.java))
        }

        fun dismissKeepingNotification(
            context: Context,
            todoItem: TodoItem,
            message: String,
            taskGroup: ResolvedTaskGroup
        ) {
            if (!todoItem.reminderDeliveryModeEnum.usesNotification) {
                dismiss(context, todoItem.id)
                return
            }
            val todoId = todoItem.id
            ReminderAccessibilityService.hideOverlayNow(todoId)
            ActiveReminderStore.clearIfMatches(context, todoId)
            ActiveReminderStore.clearActivityHandoff(context, todoId)
            ActiveReminderStore.clearReminderSurfaceMarker(context, todoId)
            if (ActiveReminderStore.getServiceTodoId(context) == todoId) {
                context.startService(
                    Intent(context, ReminderForegroundService::class.java).apply {
                        action = ACTION_DISMISS_KEEP_NOTIFICATION
                        putExtra(AlarmScheduler.EXTRA_TODO_ID, todoId)
                        putExtra(EXTRA_RETAINED_MESSAGE, message)
                    }
                )
            } else {
                ReminderNotifier(context).showRetained(todoItem, message, taskGroup)
            }
        }
    }
}
