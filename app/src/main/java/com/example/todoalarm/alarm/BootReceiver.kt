package com.example.todoalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.sync.DesktopSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as TodoApplication
                app.repository.markMissedTasks(System.currentTimeMillis())
                val items = app.repository.futureReminderItems(System.currentTimeMillis())
                items.forEach(app.alarmScheduler::schedule)
                if (app.settingsStore.currentSettings().desktopSyncEnabled) {
                    DesktopSyncService.start(context.applicationContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
