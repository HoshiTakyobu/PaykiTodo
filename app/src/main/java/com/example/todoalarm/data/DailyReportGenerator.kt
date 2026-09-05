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
        val context = collectDailyContext(app, date)
        val aiOutcome = callAiIfPossible(app, buildDailyPrompt(context), reportType = "日报")
        val content = aiOutcome.report?.content?.trim()?.takeIf { it.isNotBlank() } ?: buildLocalDaily(context)
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
        val context = collectWeeklyContext(app, periodEndDate)
        val aiOutcome = callAiIfPossible(app, buildWeeklyPrompt(context), reportType = "周报")
        val content = aiOutcome.report?.content?.trim()?.takeIf { it.isNotBlank() } ?: buildLocalWeekly(context)
        val report = saveReport(
            app = app,
            type = AiReportType.WEEKLY,
            content = content,
            providerName = aiOutcome.report?.providerName ?: LOCAL_PROVIDER_NAME,
            isLocalFallback = aiOutcome.report == null,
            periodStartMillis = dayStartMillis(context.weekStart),
            periodEndMillis = dayEndMillis(context.weekEnd)
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

    private suspend fun callAiIfPossible(app: TodoApplication, prompt: String, reportType: String): AiCallOutcome {
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
                    systemPrompt = buildReportSystemPrompt(reportType),
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

    private fun buildReportSystemPrompt(reportType: String): String {
        return """
            你是 PaykiTodo 的复盘助手，负责把用户的待办与日程数据写成一份全面、具体、可读的中文$reportType。

            写作要求：
            1. 用纯文本输出，禁止使用 Markdown 标题（不要出现 # ## 这类符号）、代码块或表格；可以用「今日回顾」「日程与投入」「明日重点」这样的中文小节标签独立成行来分段。
            2. 内容要全面，覆盖数据区提供的每一个维度：完成情况、错过/逾期情况、日程安排与时间分布、签到投入、临近的 DDL 和后续日程。不要只挑一两点说。
            3. 要具体，点名关键事项的标题，不要只给笼统的数量统计；在合适处可以引用具体时间或时间段。
            4. 客观真实，只能依据数据区内容，不要编造任何用户没有提供的事项、数字或时间；数据区没有「签到投入」字段时，不要推断、也不要写「0 分钟」。
            5. 有分析也有建议：先客观陈述与简要分析（例如完成节奏、负荷是否集中、有无连续错过），再给具体可执行的下一步建议，避免空泛说教和寒暄。
            6. 语气温和、鼓励但不浮夸；如果某个维度确实没有数据，简短带过即可，不要强行扩写。
        """.trimIndent()
    }

    private fun buildDailyPrompt(context: DailyContext): String {
        val completionRate = run {
            val planned = context.todayCompleted.size + context.todayMissed.size
            if (planned > 0) "${context.todayCompleted.size}/$planned（约 ${context.todayCompleted.size * 100 / planned}%）" else "今天没有以今天为 DDL 的待办"
        }
        return buildString {
            appendLine("请根据以下真实数据，生成一份内容充实、覆盖全面的中文日报，约 350-500 字。")
            appendLine("只使用下面提供的数据，不要编造没有出现的任务、日程或数字。")
            appendLine()
            appendLine("【日期】${context.date.format(DateFormatter)}（${weekdayLabel(context.date)}）")
            appendLine("【今日待办完成情况】完成率 $completionRate")
            appendLine()
            appendLine("今天完成的待办（${context.todayCompleted.size} 条）：")
            appendLine(context.todayCompleted.toBulletList { todoLine(it) })
            appendLine()
            appendLine("今天错过 / 逾期的待办（${context.todayMissed.size} 条）：")
            appendLine(context.todayMissed.toBulletList { todoLine(it) })
            appendLine()
            appendLine("今天的日程（${context.todayEvents.size} 条）：")
            appendLine(context.todayEvents.toBulletList { "${it.title}（${eventTimeLabel(it)}${eventLocationSuffix(it)}）" })
            if (context.todayEventCheckInMinutes > 0) {
                appendLine()
                appendLine("今日日程签到投入：${context.todayEventCheckInMinutes} 分钟")
            }
            appendLine()
            appendLine("明天的日程（${context.tomorrowEvents.size} 条）：")
            appendLine(context.tomorrowEvents.toBulletList { "${it.title}（${eventTimeLabel(it)}${eventLocationSuffix(it)}）" })
            appendLine()
            appendLine("明天 DDL（${context.tomorrowDdls.size} 条）：")
            appendLine(context.tomorrowDdls.toBulletList { "${it.title}（${formatMillis(it.dueAtMillis)}）" })
            appendLine()
            appendLine("写作要求：")
            appendLine("1. 用中文纯文本分段，每段前可加一个简短中文小标题加冒号，例如“今日回顾：”“值得肯定：”“需要补救：”“明日重点：”“一句建议：”。不要使用 Markdown 的 # 号、星号或表格。")
            appendLine("2. 今日回顾：结合完成率和具体完成的待办，说清楚今天实际推进了什么，点名 1-3 件具体任务，不要只说“完成了若干待办”。")
            appendLine("3. 需要补救：如果有错过 / 逾期待办，逐条点名并指出建议尽快重排；没有则说明今天没有遗留。")
            appendLine("4. 时间投入：只有上方提供“今日日程签到投入”时才分析签到投入时长；没有该字段不要推断，也不要写“0 分钟”。")
            appendLine("5. 明日重点：结合明天的 DDL 和日程，排出最紧要的 1-3 件事，并提示时间冲突或紧凑时段。")
            appendLine("6. 一句建议：给一句具体、贴合今天数据的温和建议，避免空泛说教。")
            append("7. 内容要具体、有信息量，但语气克制平实，不要为了凑字数重复同一句话。")
        }.trim()
    }

    private fun buildWeeklyPrompt(context: WeeklyContext): String {
        val completionRate = run {
            val planned = context.completedTodos.size + context.missedTodos.size
            if (planned > 0) "${context.completedTodos.size}/$planned（约 ${context.completedTodos.size * 100 / planned}%）" else "本周没有以本周为 DDL 的待办"
        }
        return buildString {
            appendLine("请根据以下真实数据，生成一份内容充实、覆盖全面的中文周报，约 450-650 字。")
            appendLine("只使用下面提供的数据，不要编造没有出现的任务、日程或数字。")
            appendLine()
            appendLine("【周期】${context.weekStart.format(DateFormatter)} 至 ${context.weekEnd.format(DateFormatter)}")
            appendLine("【本周待办完成情况】完成率 $completionRate")
            appendLine()
            appendLine("本周完成待办（${context.completedTodos.size} 条）：")
            appendLine(context.completedTodos.toBulletList { todoLine(it) })
            appendLine()
            appendLine("本周错过 / 逾期待办（${context.missedTodos.size} 条）：")
            appendLine(context.missedTodos.toBulletList { todoLine(it) })
            appendLine()
            appendLine("本周日程（${context.events.size} 条）：")
            appendLine(context.events.take(20).toBulletList { "${it.title}（${eventTimeLabel(it)}${eventLocationSuffix(it)}）" })
            appendLine()
            appendLine("下周 DDL（${context.upcomingDdls.size} 条）：")
            appendLine(context.upcomingDdls.take(20).toBulletList { "${it.title}（${formatMillis(it.dueAtMillis)}）" })
            appendLine()
            appendLine("写作要求：")
            appendLine("1. 用中文纯文本分段，每段前可加一个简短中文小标题加冒号，例如“本周概览：”“完成亮点：”“遗留与反思：”“节奏观察：”“下周重点：”“一句建议：”。不要使用 Markdown 的 # 号、星号或表格。")
            appendLine("2. 本周概览结合完成率说明整体推进程度；完成亮点点名 2-4 件本周具体完成的代表性任务。")
            appendLine("3. 遗留与反思逐条或归类说明错过 / 逾期的待办，并指出可能的改进方向。")
            appendLine("4. 节奏观察结合本周日程分布，说明忙碌时段或类型集中情况；数据不足时简要说明即可，不要编造。")
            appendLine("5. 下周重点结合下周 DDL 与已知日程，排出优先级最高的几件事并提示时间风险。")
            append("6. 一句建议要具体贴合数据，语气温和克制，不要空泛说教或重复凑字数。")
        }.trim()
    }

    private fun buildLocalDaily(context: DailyContext): String {
        return buildString {
            append("今天完成 ${context.todayCompleted.size} 条待办")
            append("。")
            if (context.todayEventCheckInMinutes > 0) {
                append("有签到记录的日程投入 ${context.todayEventCheckInMinutes} 分钟。")
            }
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

    private fun todoLine(item: TodoItem): String {
        return buildString {
            append(item.title)
            if (item.hasDueDate) {
                append("（DDL ").append(formatMillis(item.dueAtMillis)).append("）")
            }
            item.notes.trim().takeIf { it.isNotBlank() }?.let {
                append("，备注：").append(it.replace(Regex("\\s+"), " ").take(40))
            }
        }
    }

    private fun eventLocationSuffix(item: TodoItem): String {
        val location = item.location.trim()
        return if (location.isBlank()) "" else "，地点 $location"
    }

    private fun weekdayLabel(date: LocalDate): String {
        return when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "周一"
            DayOfWeek.TUESDAY -> "周二"
            DayOfWeek.WEDNESDAY -> "周三"
            DayOfWeek.THURSDAY -> "周四"
            DayOfWeek.FRIDAY -> "周五"
            DayOfWeek.SATURDAY -> "周六"
            DayOfWeek.SUNDAY -> "周日"
            else -> "未知"
        }
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

    private data class AiCallOutcome(
        val report: ReportText?,
        val attempted: Boolean,
        val failureMessage: String? = null
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

data class AiReportGenerationResult(
    val report: AiReport,
    val aiAttempted: Boolean,
    val aiFailureMessage: String? = null,
    val reusedExistingReport: Boolean = false
)
