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
                com.example.todoalarm.sync.AccountExpiryNotifier.schedule(context)
                if (!app.settingsStore.hasLocalAccountAccess()) return@launch
                app.repository.markMissedTasks(System.currentTimeMillis())
                app.repository.ensureRecurringInstancesAhead()
                app.repository.getAllTodos().forEach(app.alarmScheduler::recover)
                app.remoteSyncCoordinator.start()
                DailyReportScheduler.scheduleNext(context.applicationContext)
                DailyBriefScheduler.scheduleNext(context.applicationContext)
                if (app.settingsStore.currentSettings().desktopSyncEnabled) {
                    DesktopSyncService.start(context.applicationContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
