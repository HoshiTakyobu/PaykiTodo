package com.example.todoalarm.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.example.todoalarm.alarm.ActiveReminderStore
import com.example.todoalarm.ui.ReminderActivity

class ReminderAccessibilityService : AccessibilityService() {
    private lateinit var overlay: ReminderAccessibilityOverlay
    private var triggerReceiver: BroadcastReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = ReminderAccessibilityOverlay(this)
        registerTriggerReceiver()
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
        maybeLaunchReminder(
            sourcePackageName = event.packageName?.toString(),
            className = event.className?.toString(),
            eventType = event.eventType
        )
    }

    private fun maybeLaunchReminder(
        sourcePackageName: String? = null,
        className: String? = null,
        eventType: Int = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    ) {
        val todoId = ActiveReminderStore.getActiveTodoId(this)
        if (todoId <= 0L) {
            overlay.hide()
            return
        }

        if (sourcePackageName == packageName) {
            if (className == ReminderActivity::class.java.name) {
                ActiveReminderStore.clearActivityHandoff(this, todoId)
                overlay.hide(todoId)
            }
            return
        }

        if (sourcePackageName == packageName &&
            eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
        ) {
            return
        }

        overlay.showFor(todoId)
    }

    override fun onInterrupt() {
        overlay.hide()
    }

    override fun onDestroy() {
        unregisterTriggerReceiver()
        overlay.destroy()
        super.onDestroy()
    }

    private fun registerTriggerReceiver() {
        unregisterTriggerReceiver()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                val todoId = intent?.getLongExtra(EXTRA_TODO_ID, -1L) ?: -1L
                if (todoId > 0L) {
                    overlay.showFor(todoId)
                }
            }
        }
        val filter = IntentFilter(ACTION_TRIGGER_REMINDER_OVERLAY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }
        triggerReceiver = receiver
    }

    private fun unregisterTriggerReceiver() {
        val receiver = triggerReceiver ?: return
        runCatching { unregisterReceiver(receiver) }
        triggerReceiver = null
    }

    companion object {
        const val ACTION_TRIGGER_REMINDER_OVERLAY = "com.paykitodo.app.TRIGGER_REMINDER_OVERLAY"
        const val EXTRA_TODO_ID = "extra_todo_id"
    }
}
