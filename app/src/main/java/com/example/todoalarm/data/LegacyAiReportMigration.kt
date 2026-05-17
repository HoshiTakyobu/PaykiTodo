package com.example.todoalarm.data

import com.example.todoalarm.TodoApplication
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object LegacyAiReportMigration {
    private const val DAILY_TITLE = "AI 日报"
    private const val WEEKLY_TITLE = "AI 周报"
    private const val LEGACY_PROVIDER = "历史归档"
    private val HeaderRegex = Regex("""(?m)^##\s+(.+?)\s*$""")
    private val DateTimeFormatters = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd EEE HH:mm", Locale.CHINA),
        DateTimeFormatter.ofPattern("yyyy-MM-dd EEE H:mm", Locale.CHINA),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA),
        DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm", Locale.CHINA)
    )
    private val DateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)

    suspend fun migrateIfNeeded(app: TodoApplication) {
        val settings = app.settingsStore.currentSettings()
        if (settings.legacyAiReportMigrated) return
        migrate(
            repository = app.repository,
            settingsStore = app.settingsStore
        )
        app.settingsStore.markLegacyAiReportMigrated()
    }

    private suspend fun migrate(
        repository: TodoRepository,
        settingsStore: AppSettingsStore
    ) {
        val legacyNotes = repository.getAllPlanningNotes()
            .filter { it.title == DAILY_TITLE || it.title == WEEKLY_TITLE }
        if (legacyNotes.isEmpty()) return

        val reports = legacyNotes.flatMap { note ->
            val type = if (note.title == WEEKLY_TITLE) AiReportType.WEEKLY else AiReportType.DAILY
            parseLegacyNote(note, type)
        }
        repository.saveAiReports(reports)

        val deletedIds = legacyNotes.map { it.id }.toSet()
        legacyNotes.forEach { repository.deletePlanningNote(it.id) }
        if (settingsStore.currentSettings().lastOpenedPlanningNoteId in deletedIds) {
            settingsStore.updateLastOpenedPlanningNoteId(null)
        }
    }

    private fun parseLegacyNote(note: PlanningNote, type: AiReportType): List<AiReport> {
        val matches = HeaderRegex.findAll(note.contentMarkdown).toList()
        if (matches.isEmpty()) {
            val content = note.contentMarkdown.trim()
            if (content.isBlank()) return emptyList()
            return listOf(reportFromContent(type, note.updatedAtMillis, content))
        }
        return matches.mapIndexedNotNull { index, match ->
            val nextStart = matches.getOrNull(index + 1)?.range?.first ?: note.contentMarkdown.length
            val bodyStart = match.range.last + 1
            val body = trimLegacyBody(note.contentMarkdown.substring(bodyStart, nextStart))
            if (body.isBlank()) return@mapIndexedNotNull null
            val generatedAt = parseHeaderMillis(match.groupValues.getOrNull(1).orEmpty())
                ?: note.updatedAtMillis
            reportFromContent(type, generatedAt, body)
        }
    }

    private fun reportFromContent(
        type: AiReportType,
        generatedAtMillis: Long,
        content: String
    ): AiReport {
        val (periodStart, periodEnd) = when (type) {
            AiReportType.DAILY -> dailyRange(generatedAtMillis)
            AiReportType.WEEKLY -> weeklyRange(generatedAtMillis)
        }
        return AiReport(
            type = type,
            generatedAtMillis = generatedAtMillis,
            periodStartMillis = periodStart,
            periodEndMillis = periodEnd,
            content = content,
            providerName = LEGACY_PROVIDER,
            isLocalFallback = false
        )
    }

    private fun parseHeaderMillis(text: String): Long? {
        val normalized = text.trim()
        for (formatter in DateTimeFormatters) {
            val parsed = runCatching { LocalDateTime.parse(normalized, formatter) }.getOrNull()
            if (parsed != null) return parsed.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        val date = runCatching { LocalDate.parse(normalized.take(10), DateFormatter) }.getOrNull()
        return date?.atTime(LocalTime.MIN)?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    private fun trimLegacyBody(raw: String): String {
        return raw.trim()
            .replace(Regex("""(?m)\n?\s*---\s*$"""), "")
            .trim()
    }

    private fun dailyRange(millis: Long): Pair<Long, Long> {
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        return start to end
    }

    private fun weeklyRange(millis: Long): Pair<Long, Long> {
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        val startDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = startDate.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        return start to end
    }
}
