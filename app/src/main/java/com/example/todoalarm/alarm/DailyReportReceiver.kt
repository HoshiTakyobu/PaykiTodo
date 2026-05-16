package com.example.todoalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.DailyReportGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyReportReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as TodoApplication
                when (intent.action) {
                    ACTION_GENERATE_DAILY -> DailyReportGenerator.generateDaily(app)
                    ACTION_GENERATE_WEEKLY -> DailyReportGenerator.generateWeekly(app)
                }
                DailyReportScheduler.scheduleNext(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_GENERATE_DAILY = "com.example.todoalarm.GENERATE_DAILY_REPORT"
        const val ACTION_GENERATE_WEEKLY = "com.example.todoalarm.GENERATE_WEEKLY_REPORT"
    }
}
