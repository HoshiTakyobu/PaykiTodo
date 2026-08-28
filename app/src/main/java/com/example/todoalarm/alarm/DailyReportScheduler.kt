package com.example.todoalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.CrashLogger
import com.example.todoalarm.data.AiReportType
import com.example.todoalarm.data.DailyReportGenerator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

object DailyReportScheduler {
    private const val REQUEST_CODE_DAILY = 90_001
    private const val REQUEST_CODE_WEEKLY = 90_002

    fun scheduleNext(context: Context) {
        val app = context.applicationContext as TodoApplication
        val settings = app.settingsStore.currentSettings()
        val runtimeState = AiReportRuntimeStateStore(context)
        if (settings.dailyReportEnabled) {
            val triggerAt = nextDaily(settings.dailyReportHour, settings.dailyReportMinute)
            schedule(
                context = context,
                requestCode = REQUEST_CODE_DAILY,
                action = DailyReportReceiver.ACTION_GENERATE_DAILY,
                triggerAt = triggerAt,
                type = AiReportType.DAILY,
                runtimeState = runtimeState
            )
        } else {
            cancel(context, REQUEST_CODE_DAILY, DailyReportReceiver.ACTION_GENERATE_DAILY)
            runtimeState.clearScheduled(AiReportType.DAILY)
        }
        if (settings.weeklyReportEnabled) {
            val triggerAt = nextWeekly(settings.weeklyReportHour, settings.weeklyReportMinute)
            schedule(
                context = context,
                requestCode = REQUEST_CODE_WEEKLY,
                action = DailyReportReceiver.ACTION_GENERATE_WEEKLY,
                triggerAt = triggerAt,
                type = AiReportType.WEEKLY,
                runtimeState = runtimeState
            )
        } else {
            cancel(context, REQUEST_CODE_WEEKLY, DailyReportReceiver.ACTION_GENERATE_WEEKLY)
            runtimeState.clearScheduled(AiReportType.WEEKLY)
        }
    }

    fun cancelAll(context: Context) {
        cancel(context, REQUEST_CODE_DAILY, DailyReportReceiver.ACTION_GENERATE_DAILY)
        cancel(context, REQUEST_CODE_WEEKLY, DailyReportReceiver.ACTION_GENERATE_WEEKLY)
        AiReportRuntimeStateStore(context).apply {
            clearScheduled(AiReportType.DAILY)
            clearScheduled(AiReportType.WEEKLY)
        }
    }

    suspend fun recoverMostRecentMissedReports(app: TodoApplication, now: ZonedDateTime = ZonedDateTime.now()) {
        val settings = app.settingsStore.currentSettings()
        val runtimeState = AiReportRuntimeStateStore(app).current()
        val nowMillis = now.toInstant().toEpochMilli()
        if (settings.dailyReportEnabled && shouldRecoverMostRecent(runtimeState.nextDailyAtMillis, nowMillis)) {
            mostRecentDailyDate(now, settings.dailyReportHour, settings.dailyReportMinute)?.let { date ->
                recoverDailyIfMissing(app, date)
            }
        }
        if (settings.weeklyReportEnabled && shouldRecoverMostRecent(runtimeState.nextWeeklyAtMillis, nowMillis)) {
            mostRecentWeeklyEndDate(now, settings.weeklyReportHour, settings.weeklyReportMinute)?.let { weekEnd ->
                recoverWeeklyIfMissing(app, weekEnd)
            }
        }
    }

    private suspend fun recoverDailyIfMissing(app: TodoApplication, date: LocalDate) {
        val start = date.atStartOfDay(appZone()).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(appZone()).toInstant().toEpochMilli() - 1L
        if (app.repository.getAiReportForPeriod(AiReportType.DAILY, start, end) != null) return
        generateRecovered(app, AiReportType.DAILY) {
            DailyReportGenerator.generateDailyDetailed(app, date)
        }
    }

    private suspend fun recoverWeeklyIfMissing(app: TodoApplication, weekEnd: LocalDate) {
        val weekStart = weekEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val start = weekStart.atStartOfDay(appZone()).toInstant().toEpochMilli()
        val end = weekEnd.plusDays(1).atStartOfDay(appZone()).toInstant().toEpochMilli() - 1L
        if (app.repository.getAiReportForPeriod(AiReportType.WEEKLY, start, end) != null) return
        generateRecovered(app, AiReportType.WEEKLY) {
            DailyReportGenerator.generateWeeklyDetailed(app, weekEnd)
        }
    }

    private suspend fun generateRecovered(
        app: TodoApplication,
        type: AiReportType,
        generate: suspend () -> com.example.todoalarm.data.AiReportGenerationResult
    ) {
        val runtimeState = AiReportRuntimeStateStore(app)
        runtimeState.recordAttempt(type)
        runCatching { generate() }
            .onSuccess { runtimeState.recordSuccess(type, it) }
            .onFailure {
                runtimeState.recordFailure(type, it)
                CrashLogger.recordNonFatal(it)
            }
    }

    private fun schedule(
        context: Context,
        requestCode: Int,
        action: String,
        triggerAt: ZonedDateTime,
        type: AiReportType,
        runtimeState: AiReportRuntimeStateStore
    ) {
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
            runtimeState.recordScheduled(type, triggerMillis, exact = false)
            return
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        }.onSuccess {
            runtimeState.recordScheduled(type, triggerMillis, exact = true)
        }.onFailure { throwable ->
            if (throwable is SecurityException) {
                scheduleInexact(alarmManager, triggerMillis, pendingIntent)
                runtimeState.recordScheduled(type, triggerMillis, exact = false)
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

    internal fun mostRecentDailyDate(now: ZonedDateTime, hour: Int, minute: Int): LocalDate? {
        val scheduledToday = now.toLocalDate().atTime(LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
            .atZone(now.zone)
        return if (!scheduledToday.isAfter(now)) now.toLocalDate() else now.toLocalDate().minusDays(1)
    }

    internal fun mostRecentWeeklyEndDate(now: ZonedDateTime, hour: Int, minute: Int): LocalDate? {
        val thisSunday = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val scheduled = thisSunday.atTime(LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))).atZone(now.zone)
        return if (!scheduled.isAfter(now)) thisSunday else thisSunday.minusWeeks(1)
    }

    internal fun shouldRecoverMostRecent(nextTriggerAtMillis: Long, nowMillis: Long): Boolean {
        return nextTriggerAtMillis <= 0L || nextTriggerAtMillis <= nowMillis
    }

    private fun appZone() = java.time.ZoneId.systemDefault()
}
