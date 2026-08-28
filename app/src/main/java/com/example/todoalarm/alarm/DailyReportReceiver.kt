package com.example.todoalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todoalarm.CrashLogger
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.AiReportType
import com.example.todoalarm.data.DailyReportGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyReportReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val app = context.applicationContext as TodoApplication
            val type = when (intent.action) {
                ACTION_GENERATE_WEEKLY -> AiReportType.WEEKLY
                else -> AiReportType.DAILY
            }
            val runtimeState = AiReportRuntimeStateStore(context)
            runtimeState.recordAttempt(type)
            try {
                val result = when (type) {
                    AiReportType.DAILY -> DailyReportGenerator.generateDailyDetailedIfMissing(app)
                    AiReportType.WEEKLY -> DailyReportGenerator.generateWeeklyDetailedIfMissing(app)
                }
                runtimeState.recordSuccess(type, result)
            } catch (throwable: Throwable) {
                runtimeState.recordFailure(type, throwable)
                CrashLogger.recordNonFatal(throwable)
            } finally {
                runCatching { DailyReportScheduler.scheduleNext(context.applicationContext) }
                    .onFailure(CrashLogger::recordNonFatal)
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_GENERATE_DAILY = "com.example.todoalarm.GENERATE_DAILY_REPORT"
        const val ACTION_GENERATE_WEEKLY = "com.example.todoalarm.GENERATE_WEEKLY_REPORT"
    }
}
