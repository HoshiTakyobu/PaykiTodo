package com.example.todoalarm.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.example.todoalarm.alarm.ActiveReminderStore
import com.example.todoalarm.alarm.AlarmScheduler
import com.example.todoalarm.ui.ReminderActivity

class ReminderAccessibilityService : AccessibilityService() {
    private var lastLaunchedTodoId: Long = -1L
    private var lastLaunchAtElapsed: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        maybeLaunchReminder()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }
        maybeLaunchReminder(event.packageName?.toString(), event.className?.toString())
    }

    private fun maybeLaunchReminder(
        sourcePackageName: String? = null,
        className: String? = null
    ) {
        val todoId = ActiveReminderStore.getActiveTodoId(this)
        if (todoId <= 0L) return

        if (sourcePackageName == packageName && className == ReminderActivity::class.java.name) {
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (todoId == lastLaunchedTodoId && now - lastLaunchAtElapsed < 2_500L) {
            return
        }

        lastLaunchedTodoId = todoId
        lastLaunchAtElapsed = now
        startActivity(
            Intent(this, ReminderActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(AlarmScheduler.EXTRA_TODO_ID, todoId)
            }
        )
    }

    override fun onInterrupt() = Unit
}
