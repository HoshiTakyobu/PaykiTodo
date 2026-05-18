package com.example.todoalarm.data

import com.example.todoalarm.TodoApplication
import com.example.todoalarm.alarm.DailyReportNotifier
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object DailyReportGenerator {
    private val DateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)
    private val DateTimeLabelFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)
    private val ClockFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)

    suspend fun generateDaily(app: TodoApplication): AiReport {
        val today = LocalDate.now()
        val context = collectDailyContext(app, today)
        val aiReport = callAiIfPossible(app, buildDailyPrompt(context), reportType = "日报")
        val content = aiReport?.content?.trim()?.takeIf { it.isNotBlank() } ?: buildLocalDaily(context)
        val report = saveReport(
            app = app,
            type = AiReportType.DAILY,
            content = content,
            providerName = aiReport?.providerName ?: LOCAL_PROVIDER_NAME,
            isLocalFallback = aiReport == null,
            periodStartMillis = dayStartMillis(today),
            periodEndMillis = dayEndMillis(today)
        )
        DailyReportNotifier.postReportNotification(
            context = app.applicationContext,
            reportId = report.id,
            reportTitle = "AI 日报已生成",
            preview = content,
            weekly = false
        )
        return report
    }

    suspend fun generateWeekly(app: TodoApplication): AiReport {
        val today = LocalDate.now()
        val context = collectWeeklyContext(app, today)
        val aiReport = callAiIfPossible(app, buildWeeklyPrompt(context), reportType = "周报")
        val content = aiReport?.content?.trim()?.takeIf { it.isNotBlank() } ?: buildLocalWeekly(context)
        val report = saveReport(
            app = app,
            type = AiReportType.WEEKLY,
            content = content,
            providerName = aiReport?.providerName ?: LOCAL_PROVIDER_NAME,
            isLocalFallback = aiReport == null,
            periodStartMillis = dayStartMillis(context.weekStart),
            periodEndMillis = dayEndMillis(context.weekEnd)
        )
        DailyReportNotifier.postReportNotification(
            context = app.applicationContext,
            reportId = report.id,
            reportTitle = "AI 周报已生成",
            preview = content,
            weekly = true
        )
        return report
    }

    private suspend fun collectDailyContext(app: TodoApplication, date: LocalDate): DailyContext {
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEndExclusive = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val tomorrowEndExclusive = date.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        return DailyContext(
            date = date,
            todayEventCheckInMinutes = app.repository.getTodayEventCheckInMinutes(date),
            todayCompleted = app.repository.getCompletedTodosInRange(dayStart, dayEndExclusive)
                .sortedBy { it.completedAtMillis ?: it.dueAtMillis },
            todayMissed = app.repository.getMissedTodosDueInRange(dayStart, dayEndExclusive)
                .sortedBy { it.dueAtMillis },
            todayEvents = app.repository.getActiveEventsOverlappingRange(dayStart, dayEndExclusive)
                .filter { DailyBoardSnapshotBuilder.eventOverlapsDay(it, date) }
                .sortedBy { it.startAtMillis ?: it.dueAtMillis },
            tomorrowEvents = app.repository.getActiveEventsOverlappingRange(dayEndExclusive, tomorrowEndExclusive)
                .filter { DailyBoardSnapshotBuilder.eventOverlapsDay(it, date.plusDays(1)) }
                .sortedBy { it.startAtMillis ?: it.dueAtMillis },
            tomorrowDdls = app.repository.getActiveTodosDueInRange(dayEndExclusive, tomorrowEndExclusive)
                .sortedBy { it.dueAtMillis }
        )
    }

    private suspend fun collectWeeklyContext(app: TodoApplication, today: LocalDate): WeeklyContext {
        val zone = ZoneId.systemDefault()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val startMillis = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val endExclusiveMillis = weekEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val nextWeekEndExclusiveMillis = weekEnd.plusDays(8).atStartOfDay(zone).toInstant().toEpochMilli()
        return WeeklyContext(
            weekStart = weekStart,
            weekEnd = weekEnd,
            completedTodos = app.repository.getCompletedTodosInRange(startMillis, endExclusiveMillis)
                .sortedBy { it.completedAtMillis ?: it.dueAtMillis },
            missedTodos = app.repository.getMissedTodosDueInRange(startMillis, endExclusiveMillis)
                .sortedBy { it.dueAtMillis },
            events = app.repository.getActiveEventsOverlappingRange(startMillis, endExclusiveMillis)
                .filter { item -> (0..6).any { DailyBoardSnapshotBuilder.eventOverlapsDay(item, weekStart.plusDays(it.toLong())) } }
                .sortedBy { it.startAtMillis ?: it.dueAtMillis },
            upcomingDdls = app.repository.getActiveTodosDueInRange(endExclusiveMillis, nextWeekEndExclusiveMillis)
                .sortedBy { it.dueAtMillis }
        )
    }

    private suspend fun callAiIfPossible(app: TodoApplication, prompt: String, reportType: String): ReportText? {
        val settings = app.settingsStore.currentSettings()
        if (!settings.planningAiEnabled) return null
        return runCatching {
            val response = PlanningAiCaller.callWithFallback(
                providers = settings.planningAiProviders,
                request = PlanningAiRequest(
                    systemPrompt = "你是 PaykiTodo 的温和复盘助手。请输出简短中文$reportType，不要寒暄，不要编造数据。",
                    prompt = prompt
                )
            )
            ReportText(
                content = response.content,
                providerName = response.provider.name.ifBlank { response.provider.model }.ifBlank { "AI 源" }
            )
        }.getOrNull()
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

    private fun buildDailyPrompt(context: DailyContext): String {
        return """
            请根据以下数据生成一份简短的中文日报，控制在 200 字以内。

            日期：${context.date.format(DateFormatter)}
            今天完成的待办（${context.todayCompleted.size} 条）：
            ${context.todayCompleted.toBulletList { it.title }}

            今天错过的待办（${context.todayMissed.size} 条）：
            ${context.todayMissed.toBulletList { it.title }}

            今天的日程（${context.todayEvents.size} 条）：
            ${context.todayEvents.toBulletList { "${it.title}（${eventTimeLabel(it)}）" }}

            今日日程投入：${context.todayEventCheckInMinutes} 分钟

            明天的日程（${context.tomorrowEvents.size} 条）：
            ${context.tomorrowEvents.toBulletList { "${it.title}（${eventTimeLabel(it)}）" }}

            明天 DDL（${context.tomorrowDdls.size} 条）：
            ${context.tomorrowDdls.toBulletList { "${it.title}（${formatMillis(it.dueAtMillis)}）" }}

            格式要求：
            1. 第一段 1-2 句，总结今天并肯定已完成部分。
            2. 第二段 1-2 句，提示明天最紧要的 DDL 和日程。
            3. 第三段可选，给一句温和建议，避免说教。
            不要使用 Markdown 标题，直接写自然段落。
        """.trimIndent()
    }

    private fun buildWeeklyPrompt(context: WeeklyContext): String {
        return """
            请根据以下数据生成一份简短中文周报，控制在 260 字以内。

            周期：${context.weekStart.format(DateFormatter)} 至 ${context.weekEnd.format(DateFormatter)}
            本周完成待办（${context.completedTodos.size} 条）：
            ${context.completedTodos.toBulletList { it.title }}

            本周错过待办（${context.missedTodos.size} 条）：
            ${context.missedTodos.toBulletList { it.title }}

            本周日程（${context.events.size} 条）：
            ${context.events.take(12).toBulletList { "${it.title}（${eventTimeLabel(it)}）" }}

            下周 DDL（${context.upcomingDdls.size} 条）：
            ${context.upcomingDdls.take(12).toBulletList { "${it.title}（${formatMillis(it.dueAtMillis)}）" }}

            请输出自然段落：先总结本周，再指出下周优先事项，最后给一句温和建议。不要使用 Markdown 标题。
        """.trimIndent()
    }

    private fun buildLocalDaily(context: DailyContext): String {
        return buildString {
            append("今天完成 ${context.todayCompleted.size} 条待办")
            append("。")
            append("今日日程投入 ${context.todayEventCheckInMinutes} 分钟。")
            if (context.todayMissed.isNotEmpty()) {
                append("有 ${context.todayMissed.size} 条待办错过，需要尽快重新安排。")
            }
            append("\n\n")
            if (context.tomorrowDdls.isNotEmpty()) {
                append("明天有 ${context.tomorrowDdls.size} 条 DDL，建议优先处理：\n")
                context.tomorrowDdls.take(8).forEach { append("- ${it.title}（${formatMillis(it.dueAtMillis)}）\n") }
            } else {
                append("明天暂时没有 DDL，可以优先推进最重要的一件事。\n")
            }
            if (context.tomorrowEvents.isNotEmpty()) {
                append("\n明天日程：\n")
                context.tomorrowEvents.take(6).forEach { append("- ${it.title}（${eventTimeLabel(it)}）\n") }
            }
        }.trim()
    }

    private fun buildLocalWeekly(context: WeeklyContext): String {
        return buildString {
            append("本周完成 ${context.completedTodos.size} 条待办")
            append("。")
            if (context.missedTodos.isNotEmpty()) append("本周有 ${context.missedTodos.size} 条错过待办，建议在下周开头重新排期。")
            append("\n\n")
            if (context.upcomingDdls.isNotEmpty()) {
                append("下周需要优先关注这些 DDL：\n")
                context.upcomingDdls.take(8).forEach { append("- ${it.title}（${formatMillis(it.dueAtMillis)}）\n") }
            } else {
                append("下周暂时没有明确 DDL，可以先安排阶段性目标。\n")
            }
        }.trim()
    }

    private fun <T> List<T>.toBulletList(label: (T) -> String): String {
        return if (isEmpty()) "（无）" else joinToString("\n") { "- ${label(it)}" }
    }

    private fun eventTimeLabel(item: TodoItem): String {
        if (item.allDay) {
            val start = item.startAtMillis?.let(::toLocalDateTime)?.toLocalDate() ?: return "全天"
            val end = item.endAtMillis?.let(::toLocalDateTime)?.toLocalDate()?.minusDays(1) ?: start
            return if (start == end) "${start.format(DateFormatter)} 全天" else "${start.format(DateFormatter)}-${end.format(DateFormatter)} 全天"
        }
        val start = item.startAtMillis?.let(::toLocalDateTime) ?: return "未设置时间"
        val end = item.endAtMillis?.let(::toLocalDateTime) ?: start
        return if (start.toLocalDate() == end.toLocalDate()) {
            "${start.format(DateTimeLabelFormatter)}-${end.format(ClockFormatter)}"
        } else {
            "${start.format(DateTimeLabelFormatter)}-${end.format(DateTimeLabelFormatter)}"
        }
    }

    private fun formatMillis(millis: Long): String = toLocalDateTime(millis).format(DateTimeLabelFormatter)

    private fun toLocalDateTime(millis: Long): LocalDateTime {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
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

    private data class DailyContext(
        val date: LocalDate,
        val todayEventCheckInMinutes: Int,
        val todayCompleted: List<TodoItem>,
        val todayMissed: List<TodoItem>,
        val todayEvents: List<TodoItem>,
        val tomorrowEvents: List<TodoItem>,
        val tomorrowDdls: List<TodoItem>
    )

    private data class WeeklyContext(
        val weekStart: LocalDate,
        val weekEnd: LocalDate,
        val completedTodos: List<TodoItem>,
        val missedTodos: List<TodoItem>,
        val events: List<TodoItem>,
        val upcomingDdls: List<TodoItem>
    )

    private const val LOCAL_PROVIDER_NAME = "本地模板"
}
