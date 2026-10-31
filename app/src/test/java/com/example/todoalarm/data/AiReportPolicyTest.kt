package com.example.todoalarm.data

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AiReportPolicyTest {
    private fun report(at: Long, type: AiReportType = AiReportType.DAILY) = AiReport(
        id = 1, type = type, generatedAtMillis = at, periodStartMillis = 0, periodEndMillis = 1, content = "报告")

    @Test fun expiresAtExactly168HoursEvenWithForeverAndLocalFallback() {
        val original = report(1_800_000_000_000).copy(isLocalFallback = true)
        val boundary = original.generatedAtMillis + 604_800_000
        assertFalse(AiReportPolicy.isExpired(original, boundary - 1))
        assertTrue(AiReportPolicy.isExpired(original, boundary))
        assertTrue(AiReportPolicy.isExpired(original, AiReportRetention.FOREVER, boundary))
        assertFalse(AiReportPolicy.isExpired(original.copy(type = AiReportType.WEEKLY), AiReportRetention.FOREVER, boundary))
        assertTrue(AiReportPolicy.isExpired(original.copy(type = AiReportType.WEEKLY), AiReportRetention.DAYS_30, original.generatedAtMillis + 30 * 86_400_000L))
    }

    @Test fun openFlowExpiresWithoutAnyDatabaseEmission() = runBlocking {
        val original = report(System.currentTimeMillis() - AiReportPolicy.DAILY_LIFETIME_MILLIS + 250)
        val emissions = withTimeout(3_000) { flowOf(listOf(original)).withDailyExpiry().take(2).toList() }
        assertEquals(listOf(listOf(original), emptyList<AiReport>()), emissions)
    }

    @Test fun archiveAndMergeNeverResurrectExpiredDailyButKeepWeekly() {
        val snapshot = BackupSnapshot(0, emptyList(), emptyList(), emptyList(), settings = AppSettings(),
            aiReports = listOf(report(1), report(1, AiReportType.WEEKLY).copy(id = 2)))
        val restored = BackupArchiveCodec.decode(BackupArchiveCodec.encode(snapshot))
        assertEquals(listOf(AiReportType.WEEKLY), restored.aiReports.map { it.type })
        val merged = PersonalSnapshotMerge.merge(snapshot, restored, snapshot.copy(aiReports = snapshot.aiReports.map { it.copy(content = "旧副本内容") }))!!
        assertEquals(listOf(AiReportType.WEEKLY), merged.aiReports.map { it.type })
        assertTrue(PersonalSnapshotMerge.same(snapshot, restored))
    }

    @Test fun contextBoundsEvidenceAndDoesNotCountPlansAsCompleted() {
        val zone = ZoneId.of("Asia/Shanghai"); val date = LocalDate.of(2026, 10, 31)
        val midnight = date.atStartOfDay(zone).toInstant().toEpochMilli()
        fun item(id: Long, title: String, at: Long) = TodoItem(id = id, title = title, dueAtMillis = at,
            reminderAtMillis = null, reminderEnabled = false, ringEnabled = false, vibrateEnabled = false)
        val event = item(1, "既定用药安排", midnight).copy(itemType = "EVENT", startAtMillis = midnight - 60_000,
            endAtMillis = midnight + 3_600_000, recurringSeriesId = "routine", multiSlotBundleId = "course")
        val oldCompleted = item(2, "今日完成旧DDL", midnight - 30 * 86_400_000L).copy(completed = true, completedAtMillis = midnight + 60_000)
        val farFuture = item(3, "不相关远期", midnight + 90 * 86_400_000L)
        val next = item(4, "明天准备", midnight + 86_400_000)
        val context = AiReportContext.build(listOf(event, oldCompleted, farFuture, next),
            listOf(EventCheckIn(eventId = 1, checkInAtMillis = midnight - 600_000, checkOutAtMillis = midnight + 600_000)), date, false, midnight + 3_600_000, zone)
        assertFalse(context.prompt().contains("不相关远期"))
        assertEquals(1, context.data.getJSONObject("本期").getInt("完成待办数"))
        val currentEvent = context.data.getJSONObject("本期").getJSONArray("记录").let { rows ->
            (0 until rows.length()).map { rows.getJSONObject(it) }.first { it.getString("类型") == "日程" }
        }
        assertEquals("安排（不证明完成）", currentEvent.getString("记录状态"))
        assertEquals(10, currentEvent.getInt("本段签到分钟"))
        assertEquals("routine", currentEvent.getString("循环系列"))
        assertEquals("course", currentEvent.getString("同组时段"))
        assertEquals(1, context.data.getJSONObject("接下来").getInt("总数"))
    }

    @Test fun weeklyUsesMondayAndShowsTruncationWithoutInventingTotals() {
        val zone = ZoneId.of("UTC"); val date = LocalDate.of(2026, 10, 31)
        val at = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val items = (1..70).map { TodoItem(id = it.toLong(), title = "事项$it", dueAtMillis = at,
            reminderAtMillis = null, reminderEnabled = false, ringEnabled = false, vibrateEnabled = false) }
        val context = AiReportContext.build(items, emptyList(), date, true, at, zone)
        assertEquals(LocalDate.of(2026, 10, 26), context.start)
        assertEquals(LocalDate.of(2026, 11, 1), context.end)
        assertEquals(70, context.data.getJSONObject("本期").getInt("总数"))
        assertEquals(60, context.data.getJSONObject("本期").getInt("已列出"))
        assertFalse(context.prompt().contains("完成率"))
        assertFalse(context.prompt().contains("本段签到分钟"))
    }
}
