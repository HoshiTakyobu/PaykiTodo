package com.example.todoalarm.data

import com.example.todoalarm.TodoApplication
import com.example.todoalarm.alarm.DailyReportNotifier
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object DailyReportGenerator {

    suspend fun generateDaily(app: TodoApplication): AiReport = generateDailyDetailed(app).report

    suspend fun generateDailyDetailedIfMissing(
        app: TodoApplication,
        date: LocalDate = LocalDate.now()
    ): AiReportGenerationResult {
        val existing = app.repository.getAiReportForPeriod(
            type = AiReportType.DAILY,
            periodStartMillis = dayStartMillis(date),
            periodEndMillis = dayEndMillis(date)
        )
        if (existing != null && (!existing.isLocalFallback || !hasConfiguredAiProvider(app))) {
            return AiReportGenerationResult(report = existing, aiAttempted = false, reusedExistingReport = true)
        }
        return generateDailyDetailed(app, date).also { result ->
            if (existing?.isLocalFallback == true && existing.id != result.report.id) {
                app.repository.deleteAiReport(existing.id)
            }
        }
    }

    suspend fun generateDailyDetailed(
        app: TodoApplication,
        date: LocalDate = LocalDate.now()
    ): AiReportGenerationResult {
        val context = app.repository.reportContext(date, false, app.settingsStore.currentSettings().remoteSyncServerUrl)
        val aiOutcome = callAiIfPossible(app, context.prompt())
        val content = aiOutcome.report?.content?.trim()?.takeIf { it.isNotBlank() } ?: context.localText()
        val report = saveReport(
            app = app,
            type = AiReportType.DAILY,
            content = content,
            providerName = aiOutcome.report?.providerName ?: LOCAL_PROVIDER_NAME,
            isLocalFallback = aiOutcome.report == null,
            periodStartMillis = dayStartMillis(date),
            periodEndMillis = dayEndMillis(date)
        )
        DailyReportNotifier.postReportNotification(
            context = app.applicationContext,
            reportId = report.id,
            reportTitle = if (report.isLocalFallback) "本地模板日报已生成" else "AI 日报已生成",
            preview = fallbackNotificationPreview(aiOutcome.failureMessage, content),
            weekly = false
        )
        return AiReportGenerationResult(
            report = report,
            aiAttempted = aiOutcome.attempted,
            aiFailureMessage = aiOutcome.failureMessage
        )
    }

    suspend fun generateWeekly(app: TodoApplication): AiReport = generateWeeklyDetailed(app).report

    suspend fun generateWeeklyDetailedIfMissing(
        app: TodoApplication,
        periodEndDate: LocalDate = LocalDate.now()
    ): AiReportGenerationResult {
        val weekStart = periodEndDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = periodEndDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val existing = app.repository.getAiReportForPeriod(
            type = AiReportType.WEEKLY,
            periodStartMillis = dayStartMillis(weekStart),
            periodEndMillis = dayEndMillis(weekEnd)
        )
        if (existing != null && (!existing.isLocalFallback || !hasConfiguredAiProvider(app))) {
            return AiReportGenerationResult(report = existing, aiAttempted = false, reusedExistingReport = true)
        }
        return generateWeeklyDetailed(app, periodEndDate).also { result ->
            if (existing?.isLocalFallback == true && existing.id != result.report.id) {
                app.repository.deleteAiReport(existing.id)
            }
        }
    }

    suspend fun generateWeeklyDetailed(
        app: TodoApplication,
        periodEndDate: LocalDate = LocalDate.now()
    ): AiReportGenerationResult {
        val context = app.repository.reportContext(periodEndDate, true, app.settingsStore.currentSettings().remoteSyncServerUrl)
        val aiOutcome = callAiIfPossible(app, context.prompt())
        val content = aiOutcome.report?.content?.trim()?.takeIf { it.isNotBlank() } ?: context.localText()
        val report = saveReport(
            app = app,
            type = AiReportType.WEEKLY,
            content = content,
            providerName = aiOutcome.report?.providerName ?: LOCAL_PROVIDER_NAME,
            isLocalFallback = aiOutcome.report == null,
            periodStartMillis = dayStartMillis(context.start),
            periodEndMillis = dayEndMillis(context.end)
        )
        DailyReportNotifier.postReportNotification(
            context = app.applicationContext,
            reportId = report.id,
            reportTitle = if (report.isLocalFallback) "本地模板周报已生成" else "AI 周报已生成",
            preview = fallbackNotificationPreview(aiOutcome.failureMessage, content),
            weekly = true
        )
        return AiReportGenerationResult(
            report = report,
            aiAttempted = aiOutcome.attempted,
            aiFailureMessage = aiOutcome.failureMessage
        )
    }

    private suspend fun callAiIfPossible(app: TodoApplication, prompt: String): AiCallOutcome {
        val settings = app.settingsStore.currentSettings()
        if (!settings.planningAiEnabled) {
            return AiCallOutcome(
                report = null,
                attempted = false,
                failureMessage = "AI 调用未启用"
            )
        }
        return runCatching {
            val response = PlanningAiCaller.callWithFallback(
                providers = settings.planningAiProviders,
                request = PlanningAiRequest(
                    systemPrompt = app.assets.open("ai/report-system.txt").bufferedReader().use { it.readText() },
                    prompt = prompt
                ),
                onSelectionChanged = { app.activatePlanningAiProvider(it) }
            )
            AiCallOutcome(
                report = ReportText(
                    content = response.content,
                    providerName = response.provider.name.ifBlank { response.provider.model }.ifBlank { "AI 源" }
                ),
                attempted = true
            )
        }.getOrElse { throwable ->
            AiCallOutcome(
                report = null,
                attempted = true,
                failureMessage = sanitizeAiFailure(throwable)
            )
        }
    }

    private fun hasConfiguredAiProvider(app: TodoApplication): Boolean {
        val settings = app.settingsStore.currentSettings()
        return settings.planningAiEnabled && settings.planningAiProviders.any {
            it.enabled && it.baseUrl.isNotBlank() && it.apiKey.isNotBlank() && it.model.isNotBlank()
        }
    }

    private fun fallbackNotificationPreview(failureMessage: String?, content: String): String {
        return if (failureMessage.isNullOrBlank()) {
            content
        } else {
            "AI 未生成：$failureMessage。$content"
        }
    }

    private fun sanitizeAiFailure(throwable: Throwable): String {
        val message = throwable.message.orEmpty()
            .replace(Regex("https?://\\S+", RegexOption.IGNORE_CASE), "<服务地址>")
            .replace(Regex("(?i)(api[-_ ]?key|authorization|bearer)\\s*[:=]?\\s*\\S+")) { match ->
                "${match.groupValues[1]}=<已隐藏>"
            }
            .trim()
            .take(500)
        return if (message.isBlank()) throwable::class.java.simpleName else "${throwable::class.java.simpleName}: $message"
    }

    private suspend fun saveReport(
        app: TodoApplication,
        type: AiReportType,
        content: String,
        providerName: String,
        isLocalFallback: Boolean,
        periodStartMillis: Long,
        periodEndMillis: Long
    ): AiReport {
        val report = AiReport(
            type = type,
            generatedAtMillis = System.currentTimeMillis(),
            periodStartMillis = periodStartMillis,
            periodEndMillis = periodEndMillis,
            content = content.trim(),
            providerName = providerName,
            isLocalFallback = isLocalFallback
        )
        val id = app.repository.saveAiReport(report)
        app.repository.purgeAiReportsOlderThan(app.settingsStore.currentSettings().aiReportRetention)
        return report.copy(id = id)
    }

    private fun dayStartMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun dayEndMillis(date: LocalDate): Long {
        return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
    }

    private data class ReportText(
        val content: String,
        val providerName: String
    )

    private data class AiCallOutcome(
        val report: ReportText?,
        val attempted: Boolean,
        val failureMessage: String? = null
    )

    private const val LOCAL_PROVIDER_NAME = "本地模板"
}

data class AiReportGenerationResult(
    val report: AiReport,
    val aiAttempted: Boolean,
    val aiFailureMessage: String? = null,
    val reusedExistingReport: Boolean = false
)
