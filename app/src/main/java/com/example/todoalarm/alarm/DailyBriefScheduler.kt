package com.example.todoalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.todoalarm.TodoApplication
import java.time.ZonedDateTime

object DailyBriefScheduler {
    private const val REQUEST_CODE = 92_001

    fun scheduleNext(context: Context) {
        val app = context.applicationContext as TodoApplication
        val settings = app.settingsStore.currentSettings()
        if (!settings.dailyBriefEnabled) {
            cancel(context)
            return
        }
        schedule(
            context = context,
            triggerAt = nextDaily(settings.dailyBriefHour, settings.dailyBriefMinute)
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    private fun schedule(context: Context, triggerAt: ZonedDateTime) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = pendingIntent(context)
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

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyBriefReceiver::class.java).apply {
            action = DailyBriefReceiver.ACTION_DAILY_BRIEF
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextDaily(hour: Int, minute: Int): ZonedDateTime {
        val now = ZonedDateTime.now()
        var target = now.withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target
    }
}
