package com.example.todoalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.todoalarm.TodoApplication
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

object DailyReportScheduler {
    private const val REQUEST_CODE_DAILY = 90_001
    private const val REQUEST_CODE_WEEKLY = 90_002

    fun scheduleNext(context: Context) {
        val app = context.applicationContext as TodoApplication
        val settings = app.settingsStore.currentSettings()
        if (settings.dailyReportEnabled) {
            schedule(
                context = context,
                requestCode = REQUEST_CODE_DAILY,
                action = DailyReportReceiver.ACTION_GENERATE_DAILY,
                triggerAt = nextDaily(settings.dailyReportHour, settings.dailyReportMinute)
            )
        } else {
            cancel(context, REQUEST_CODE_DAILY, DailyReportReceiver.ACTION_GENERATE_DAILY)
        }
        if (settings.weeklyReportEnabled) {
            schedule(
                context = context,
                requestCode = REQUEST_CODE_WEEKLY,
                action = DailyReportReceiver.ACTION_GENERATE_WEEKLY,
                triggerAt = nextWeekly(settings.weeklyReportHour, settings.weeklyReportMinute)
            )
        } else {
            cancel(context, REQUEST_CODE_WEEKLY, DailyReportReceiver.ACTION_GENERATE_WEEKLY)
        }
    }

    fun cancelAll(context: Context) {
        cancel(context, REQUEST_CODE_DAILY, DailyReportReceiver.ACTION_GENERATE_DAILY)
        cancel(context, REQUEST_CODE_WEEKLY, DailyReportReceiver.ACTION_GENERATE_WEEKLY)
    }

    private fun schedule(context: Context, requestCode: Int, action: String, triggerAt: ZonedDateTime) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = pendingIntent(context, requestCode, action)
        val triggerMillis = triggerAt.toInstant().toEpochMilli()
        val canUseExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        if (!canUseExact) {
            scheduleInexact(alarmManager, triggerMillis, pendingIntent)
            return
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        }.onFailure { throwable ->
            if (throwable is SecurityException) {
                scheduleInexact(alarmManager, triggerMillis, pendingIntent)
            } else {
                throw throwable
            }
        }
    }

    private fun scheduleInexact(
        alarmManager: AlarmManager,
        triggerMillis: Long,
        pendingIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    private fun cancel(context: Context, requestCode: Int, action: String) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, requestCode, action))
    }

    private fun pendingIntent(context: Context, requestCode: Int, action: String): PendingIntent {
        val intent = Intent(context, DailyReportReceiver::class.java).apply {
            this.action = action
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextDaily(hour: Int, minute: Int): ZonedDateTime {
        val now = ZonedDateTime.now()
        var target = now.withHour(hour.coerceIn(0, 23)).withMinute(minute.coerceIn(0, 59)).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target
    }

    private fun nextWeekly(hour: Int, minute: Int): ZonedDateTime {
        val now = ZonedDateTime.now()
        var target = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            .withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(now)) target = target.plusWeeks(1)
        return target
    }
}
