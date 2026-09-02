package com.example.todoalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Re-checks the item before routing a retained notification. A completed or
 * canceled item should not reopen a stale preview from the notification shade.
 */
class ReminderNotificationClickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra(AlarmScheduler.EXTRA_TODO_ID, -1L)
        if (todoId <= 0L) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as TodoApplication
                val item = app.repository.getTodo(todoId)
                if (!app.settingsStore.hasValidRemoteSession() || !shouldOpenReminderNotification(item)) {
                    context.getSystemService(android.app.NotificationManager::class.java)
                        ?.cancel(ReminderNotifier.notificationId(todoId))
                    return@launch
                }
                val activeItem = item ?: return@launch
                val target = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    if (activeItem.isEvent) {
                        putExtra(MainActivity.EXTRA_OPEN_EVENT_ID, activeItem.id)
                    } else {
                        putExtra(MainActivity.EXTRA_OPEN_TODO_ID, activeItem.id)
                    }
                }
                context.startActivity(target)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

internal fun shouldOpenReminderNotification(item: com.example.todoalarm.data.TodoItem?): Boolean {
    return item != null && !item.isHistory
}
