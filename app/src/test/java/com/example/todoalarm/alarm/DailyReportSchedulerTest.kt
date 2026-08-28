package com.example.todoalarm.alarm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyReportSchedulerTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun dailyRecoveryUsesPreviousDayBeforeScheduledTime() {
        val now = ZonedDateTime.of(2026, 8, 28, 21, 59, 0, 0, zone)

        assertEquals(
            LocalDate.of(2026, 8, 27),
            DailyReportScheduler.mostRecentDailyDate(now, hour = 22, minute = 0)
        )
    }

    @Test
    fun dailyRecoveryUsesTodayAtScheduledTime() {
        val now = ZonedDateTime.of(2026, 8, 28, 22, 0, 0, 0, zone)

        assertEquals(
            LocalDate.of(2026, 8, 28),
            DailyReportScheduler.mostRecentDailyDate(now, hour = 22, minute = 0)
        )
    }

    @Test
    fun weeklyRecoveryUsesPreviousSundayBeforeSundaySchedule() {
        val now = ZonedDateTime.of(2026, 8, 30, 21, 59, 0, 0, zone)

        assertEquals(
            LocalDate.of(2026, 8, 23),
            DailyReportScheduler.mostRecentWeeklyEndDate(now, hour = 22, minute = 0)
        )
    }

    @Test
    fun weeklyRecoveryUsesCurrentSundayAtScheduledTime() {
        val now = ZonedDateTime.of(2026, 8, 30, 22, 0, 0, 0, zone)

        assertEquals(
            LocalDate.of(2026, 8, 30),
            DailyReportScheduler.mostRecentWeeklyEndDate(now, hour = 22, minute = 0)
        )
    }

    @Test
    fun recoveryRunsWhenLegacyStateHasNoRecordedTrigger() {
        assertEquals(true, DailyReportScheduler.shouldRecoverMostRecent(nextTriggerAtMillis = 0L, nowMillis = 100L))
    }

    @Test
    fun recoveryRunsWhenRecordedTriggerIsOverdue() {
        assertEquals(true, DailyReportScheduler.shouldRecoverMostRecent(nextTriggerAtMillis = 99L, nowMillis = 100L))
    }

    @Test
    fun recoverySkipsAFirstFutureTrigger() {
        assertEquals(false, DailyReportScheduler.shouldRecoverMostRecent(nextTriggerAtMillis = 101L, nowMillis = 100L))
    }
}
