package com.example.todoalarm.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest

/** Elapsed hours from generation, independent of calendar dates and weekly retention. */
object AiReportPolicy {
    const val DAILY_LIFETIME_MILLIS = 168L * 60 * 60 * 1000

    fun dailyCutoff(now: Long = System.currentTimeMillis()): Long = now - DAILY_LIFETIME_MILLIS

    fun isExpired(report: AiReport, now: Long = System.currentTimeMillis()): Boolean =
        report.type == AiReportType.DAILY && report.generatedAtMillis <= dailyCutoff(now)

    fun isExpired(report: AiReport, retention: AiReportRetention, now: Long): Boolean =
        if (report.type == AiReportType.DAILY) isExpired(report, now)
        else retention.days?.let { report.generatedAtMillis <= now - it * 86_400_000L } ?: false

    fun remainingMillis(report: AiReport, now: Long = System.currentTimeMillis()): Long =
        (report.generatedAtMillis - dailyCutoff(now)).coerceAtLeast(0)
}

/** Re-evaluate even if Room has not changed and the report page stays open. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal fun Flow<List<AiReport>>.withDailyExpiry(): Flow<List<AiReport>> = transformLatest { reports ->
    while (true) {
        val now = System.currentTimeMillis()
        val visible = reports.filterNot { AiReportPolicy.isExpired(it, now) }
        emit(visible)
        val next = visible.filter { it.type == AiReportType.DAILY }
            .minOfOrNull { AiReportPolicy.remainingMillis(it, now) } ?: break
        // Bound the wait to notice wall-clock corrections as well as elapsed time.
        delay(next.coerceIn(1, 60_000))
    }
}.distinctUntilChanged()

internal fun BackupSnapshot.withoutExpiredDailyReports(now: Long = System.currentTimeMillis()): BackupSnapshot =
    copy(aiReports = aiReports.filterNot { AiReportPolicy.isExpired(it, now) })
