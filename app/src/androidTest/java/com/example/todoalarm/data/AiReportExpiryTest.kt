package com.example.todoalarm.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AiReportExpiryTest {
    @Test fun applicationMonitorPhysicallyDeletesAnExpiredReportWhileOpen() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as com.example.todoalarm.TodoApplication
        val id = app.repository.saveAiReport(AiReport(type = AiReportType.DAILY,
            generatedAtMillis = System.currentTimeMillis() - 604_800_000 + 2_000,
            periodStartMillis = 0, periodEndMillis = 1, content = "expiry-runtime-fixture"))
        assertTrue(id > 0)
        try {
            assertNotNull(app.database.todoDao().getAiReportById(id, Long.MIN_VALUE))
            kotlinx.coroutines.withTimeout(10_000) {
                while (app.database.todoDao().getAiReportById(id, Long.MIN_VALUE) != null) kotlinx.coroutines.delay(50)
            }
        } finally { app.repository.deleteAiReport(id) }
    }

    @Test fun expiredReportsAreHiddenBeforeCleanupAndRestoresKeepOnlyValidReports() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            val dao = database.todoDao(); val repo = TodoRepository(database)
            val now = System.currentTimeMillis()
            fun report(id: Long, type: AiReportType, at: Long) = AiReport(id, type, at, 1, 2, "命中报告", "来源")
            val expired = report(1, AiReportType.DAILY, now - 604_800_001)
            val weekly = report(2, AiReportType.WEEKLY, 1)
            val valid = report(3, AiReportType.DAILY, now)
            dao.insertAiReports(listOf(expired, weekly, valid))
            assertNull(repo.getAiReportById(1))
            assertEquals(listOf(3L, 2L), repo.getRecentAiReports(2).map { it.id })
            assertEquals(listOf(3L), repo.getAiReportsByType(AiReportType.DAILY).map { it.id })
            assertEquals(listOf(3L, 2L), repo.globalSearch("命中").aiReports.map { it.id })
            assertEquals(listOf(3L, 2L), repo.observeAiReports(2).first().map { it.id })
            assertEquals(listOf(3L), repo.observeAiReportsFiltered(AiReportType.DAILY, "来源", 0, Long.MAX_VALUE, 10).first().map { it.id })
            assertEquals(3L, repo.getAiReportForPeriod(AiReportType.DAILY, 1, 2)!!.id)
            val snapshot = repo.exportSnapshot(AppSettings(aiReportRetention = AiReportRetention.FOREVER))
            assertEquals(2, snapshot.aiReports.size)
            assertEquals(1, repo.purgeAiReportsOlderThan(AiReportRetention.FOREVER))
            repo.importSnapshot(snapshot.copy(aiReports = listOf(expired, weekly, valid)))
            assertEquals(2, repo.getAllAiReports().size)
            assertEquals(0, dao.getExpiredDailyReports(AiReportPolicy.dailyCutoff()).size)
            assertTrue(repo.saveAiReport(valid.copy(id = 0)) > 3)
        } finally { database.close() }
    }

    @Test fun daoHonorsInclusiveBoundaryAndWeeklyRetentionSeparately() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            val dao = database.todoDao(); val now = System.currentTimeMillis(); val cutoff = now - 604_800_000
            dao.insertAiReports(listOf(AiReport(1, AiReportType.DAILY, cutoff, 0, 0, "边界"),
                AiReport(2, AiReportType.DAILY, cutoff + 1, 0, 0, "未到期"), AiReport(3, AiReportType.WEEKLY, 1, 0, 0, "周报")))
            assertEquals(listOf(2L, 3L), dao.getAllAiReports(cutoff).map { it.id })
            assertEquals(1, dao.purgeExpiredDailyReports(cutoff))
            assertEquals(1, dao.purgeAiReportsBefore(now - 30 * 86_400_000L))
            assertEquals(listOf(2L), dao.getAllAiReports(cutoff).map { it.id })
        } finally { database.close() }
    }
}
