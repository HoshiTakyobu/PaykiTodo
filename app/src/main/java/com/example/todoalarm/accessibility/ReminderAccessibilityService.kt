package com.example.todoalarm.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.example.todoalarm.alarm.ActiveReminderStore
import com.example.todoalarm.alarm.ReminderChainLogger
import com.example.todoalarm.data.ReminderChainStage
import com.example.todoalarm.data.ReminderChainStatus
import com.example.todoalarm.ui.ReminderActivity

class ReminderAccessibilityService : AccessibilityService() {
    private lateinit var overlay: ReminderAccessibilityOverlay
    private var triggerReceiver: BroadcastReceiver? = null
    private var screenStateReceiver: BroadcastReceiver? = null
    private var lastObservedPackageName: String? = null
    private var lastObservedClassName: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = ReminderAccessibilityOverlay(this)
        activeService = this
        registerTriggerReceiver()
        registerScreenStateReceiver()
        maybeLaunchReminder()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
        ) {
            return
        }
        lastObservedPackageName = event.packageName?.toString()
        lastObservedClassName = event.className?.toString()
        maybeLaunchReminder(
            sourcePackageName = lastObservedPackageName,
            className = lastObservedClassName
        )
    }

    private fun maybeLaunchReminder(
        todoIdOverride: Long? = null,
        sourcePackageName: String? = null,
        className: String? = null,
        forceOverlay: Boolean = false
    ) {
        val todoId = todoIdOverride ?: ActiveReminderStore.getActiveTodoId(this)
        if (todoId <= 0L) {
            overlay.hide()
            return
        }

        if (forceOverlay) {
            ActiveReminderStore.clearActivityHandoff(this, todoId)
            ReminderChainLogger.log(
                context = this,
                todoId = todoId,
                source = "ReminderAccessibilityService",
                stage = ReminderChainStage.ACCESSIBILITY_OVERLAY,
                status = ReminderChainStatus.INFO,
                message = "forced"
            )
            overlay.showFor(todoId, ignoreActivityHandoff = true)
            return
        }

        if (sourcePackageName == packageName && className == ReminderActivity::class.java.name) {
            ActiveReminderStore.clearActivityHandoff(this, todoId)
            overlay.hide(todoId)
            return
        }

        if (overlay.isShowing(todoId)) return
        if (ActiveReminderStore.isActivityHandoffPending(this, todoId)) return

        launchReminderActivity(todoId)
    }

    override fun onInterrupt() {
        overlay.hide()
    }

    override fun onDestroy() {
        unregisterScreenStateReceiver()
        unregisterTriggerReceiver()
        overlay.destroy()
        if (activeService === this) {
            activeService = null
        }
        super.onDestroy()
    }

    private fun registerTriggerReceiver() {
        unregisterTriggerReceiver()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                val todoId = intent?.getLongExtra(EXTRA_TODO_ID, -1L) ?: -1L
                if (todoId > 0L) {
                    val forceOverlay = intent?.getBooleanExtra(EXTRA_FORCE_OVERLAY, false) == true
                    maybeLaunchReminder(
                        todoIdOverride = todoId,
                        sourcePackageName = lastObservedPackageName,
                        className = lastObservedClassName,
                        forceOverlay = forceOverlay
                    )
                }
            }
        }
        val filter = IntentFilter(ACTION_TRIGGER_REMINDER_OVERLAY)
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        triggerReceiver = receiver
    }

    private fun unregisterTriggerReceiver() {
        val receiver = triggerReceiver ?: return
        runCatching { unregisterReceiver(receiver) }
        triggerReceiver = null
    }

    private fun registerScreenStateReceiver() {
        unregisterScreenStateReceiver()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                val todoId = ActiveReminderStore.getActiveTodoId(this@ReminderAccessibilityService)
                if (todoId <= 0L) return
                maybeLaunchReminder(
                    todoIdOverride = todoId,
                    sourcePackageName = lastObservedPackageName,
                    className = lastObservedClassName,
                    forceOverlay = isDeviceLocked()
                )
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        screenStateReceiver = receiver
    }

    private fun unregisterScreenStateReceiver() {
        val receiver = screenStateReceiver ?: return
        runCatching { unregisterReceiver(receiver) }
        screenStateReceiver = null
    }

    private fun launchReminderActivity(todoId: Long) {
        ActiveReminderStore.markActivityHandoff(this, todoId)
        runCatching {
            startActivity(ReminderActivity.createIntent(this, todoId))
            ReminderChainLogger.log(
                context = this,
                todoId = todoId,
                source = "ReminderAccessibilityService",
                stage = ReminderChainStage.FULLSCREEN_ACTIVITY_LAUNCH,
                status = ReminderChainStatus.OK,
                message = "accessibility_service"
            )
        }.onFailure {
            ActiveReminderStore.clearActivityHandoff(this, todoId)
            ReminderChainLogger.log(
                context = this,
                todoId = todoId,
                source = "ReminderAccessibilityService",
                stage = ReminderChainStage.FULLSCREEN_ACTIVITY_FAILED,
                status = ReminderChainStatus.WARN,
                message = it.javaClass.simpleName
            )
            overlay.showFor(todoId)
        }
    }

    private fun isDeviceLocked(): Boolean {
        val keyguardManager = getSystemService(KeyguardManager::class.java) ?: return false
        return keyguardManager.isDeviceLocked || keyguardManager.isKeyguardLocked
    }

    companion object {
        const val ACTION_TRIGGER_REMINDER_OVERLAY = "com.paykitodo.app.TRIGGER_REMINDER_OVERLAY"
        const val EXTRA_TODO_ID = "extra_todo_id"
        const val EXTRA_FORCE_OVERLAY = "extra_force_overlay"

        @Volatile
        private var activeService: ReminderAccessibilityService? = null

        fun showOverlayNow(todoId: Long, forceOverlay: Boolean = false): Boolean {
            val service = activeService ?: return false
            service.maybeLaunchReminder(
                todoIdOverride = todoId,
                sourcePackageName = service.lastObservedPackageName,
                className = service.lastObservedClassName,
                forceOverlay = forceOverlay
            )
            return true
        }
    }
}
