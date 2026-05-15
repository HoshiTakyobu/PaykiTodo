package com.example.todoalarm.data

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

enum class PlanningParsedType {
    TODO,
    EVENT,
    SKIPPED,
    ERROR
}

data class PlanningParseResult(
    val candidates: List<PlanningParsedCandidate>,
    val message: String = ""
) {
    val importableCount: Int
        get() = candidates.count { it.importable }
}

data class PlanningParsedCandidate(
    val id: String,
    val lineNumber: Int,
    val sourceLine: String,
    val type: PlanningParsedType,
    val title: String = "",
    val notes: String = "",
    val groupName: String = "",
    val dueAt: LocalDateTime? = null,
    val startAt: LocalDateTime? = null,
    val endAt: LocalDateTime? = null,
    val reminderOffsetsMinutes: List<Int> = listOf(DEFAULT_PLANNING_REMINDER_MINUTES),
    val createLinkedTodo: Boolean = false,
    val defaultToday: Boolean = false,
    val imported: Boolean = false,
    val completed: Boolean = false,
    val importBlocked: Boolean = false,
    val parentTitle: String? = null,
    val message: String = ""
) {
    val importable: Boolean
        get() = (type == PlanningParsedType.TODO || type == PlanningParsedType.EVENT) && !importBlocked
}

object PlanningMarkdownParser {
    fun markImportedLines(markdown: String, importedLineNumbers: Set<Int>): String {
        if (markdown.isEmpty() || importedLineNumbers.isEmpty()) return markdown
        val hasTrailingNewline = markdown.endsWith("\n") || markdown.endsWith("\r")
        val normalized = markdown.replace("\r\n", "\n").replace('\r', '\n')
        val updated = normalized.lines().mapIndexed { index, line ->
            val lineNumber = index + 1
            if (lineNumber !in importedLineNumbers || line.isBlank() || hasSimpleTag(line, "imported")) {
                line
            } else {
                "$line #imported"
            }
        }.joinToString("\n")
        return if (hasTrailingNewline && !updated.endsWith("\n")) "$updated\n" else updated
    }

    fun parse(
        markdown: String,
        now: LocalDateTime = LocalDateTime.now()
    ): PlanningParseResult {
        val candidates = mutableListOf<PlanningParsedCandidate>()
        var dateContext: LocalDate? = null
        var topLevelTaskTitle: String? = null

        markdown.lineSequence().forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            val line = rawLine.trimEnd()
            val trimmed = line.trim()
            if (trimmed.isBlank()) return@forEachIndexed

            val headingText = headingTextOrNull(trimmed)
            if (headingText != null) {
                dateContext = parseHeadingDateContext(headingText, now.toLocalDate())?.date
                topLevelTaskTitle = null
                return@forEachIndexed
            }

            val checkbox = parseCheckbox(line)
            val content = checkbox?.content?.trim().orEmpty().ifBlank { trimmed }
            val indentLevel = checkbox?.indentLevel ?: 0
            val imported = hasSimpleTag(content, "imported")
            if (imported) {
                candidates += skipped(lineNumber, line, "已带 #imported，默认跳过。", imported = true)
                return@forEachIndexed
            }
            if (checkbox?.completed == true) {
                candidates += skipped(lineNumber, line, "已完成任务，默认跳过。", completed = true)
                return@forEachIndexed
            }

            val parent = if (indentLevel > 0) topLevelTaskTitle else null
            val parsed = parseCandidateLine(
                lineNumber = lineNumber,
                sourceLine = line,
                content = content,
                dateContext = dateContext,
                parentTitle = parent,
                checkboxPresent = checkbox != null,
                now = now
            )
            if (parsed != null) {
                candidates += parsed
                if (indentLevel == 0 && (parsed.type == PlanningParsedType.TODO || parsed.type == PlanningParsedType.EVENT) && parsed.title.isNotBlank()) {
                    topLevelTaskTitle = parsed.title
                }
            }
        }

        return PlanningParseResult(candidates)
    }

    private fun parseCandidateLine(
        lineNumber: Int,
        sourceLine: String,
        content: String,
        dateContext: LocalDate?,
        parentTitle: String?,
        checkboxPresent: Boolean,
        now: LocalDateTime
    ): PlanningParsedCandidate? {
        val bareDdl = bareDdlValue(content)
        val explicitDdl = tagValue(content, "ddl") ?: bareDdl
        val explicitSchedule = tagValue(content, "schedule")
        val scheduleSource = explicitSchedule ?: content
        val naturalEvent = if (explicitSchedule != null || explicitDdl == null) {
            parseNaturalEvent(scheduleSource, dateContext, now)
        } else {
            null
        }
        if (explicitSchedule != null || naturalEvent != null) {
            val event = naturalEvent ?: return error(lineNumber, sourceLine, "无法识别 #schedule 后的日程时间。")
            val title = if (explicitSchedule == null) {
                event.title
            } else {
                cleanTitle(content).trim().ifBlank { event.title }
            }
            if (title.isBlank()) return error(lineNumber, sourceLine, "日程标题不能为空。")
            if (!event.endAt.isAfter(event.startAt)) return error(lineNumber, sourceLine, "日程结束时间必须晚于开始时间。")
            val reminderResult = parseReminderTag(content, event.startAt, now)
            if (reminderResult != null && !reminderResult.isValid) {
                return error(lineNumber, sourceLine, "提醒格式无效：${reminderResult.message}")
            }
            val reminder = reminderResult?.offsetsMinutes ?: listOf(DEFAULT_PLANNING_REMINDER_MINUTES)
            val group = tagValue(content, "group").orEmpty()
            val warnings = mutableListOf<String>()
            if (event.defaultToday) warnings += "未写日期，默认今天。"
            val importBlocked = reminder.any { offset -> !event.startAt.minusMinutes(offset.toLong()).isAfter(now) }
            if (importBlocked) warnings += "提醒时间已经过去，请调整日期或提醒时间后再导入。"
            return PlanningParsedCandidate(
                id = "line-$lineNumber",
                lineNumber = lineNumber,
                sourceLine = sourceLine,
                type = PlanningParsedType.EVENT,
                title = title,
                notes = parentTitle?.let { "所属大任务：$it" }.orEmpty(),
                groupName = group,
                startAt = event.startAt,
                endAt = event.endAt,
                reminderOffsetsMinutes = reminder,
                createLinkedTodo = true,
                defaultToday = event.defaultToday,
                importBlocked = importBlocked,
                parentTitle = parentTitle,
                message = warnings.joinToString("；")
            )
        }

        if (!checkboxPresent && !hasSimpleTag(content, "task") && explicitDdl == null) {
            return parseNaturalTodoHints(
                lineNumber = lineNumber,
                sourceLine = sourceLine,
                content = content,
                dateContext = dateContext,
                parentTitle = parentTitle,
                now = now
            )
        }

        val title = cleanTitle(content).trim()
        if (title.isBlank()) return error(lineNumber, sourceLine, "待办标题不能为空。")
        val naturalDdl = if (explicitDdl == null) parseNaturalDdlHint(content, dateContext, now) else null
        val ddl = explicitDdl?.let { ddlText ->
            parseDateTimeExpression(ddlText, defaultDate = dateContext, nowDate = now.toLocalDate(), defaultTime = LocalTime.of(23, 59))
                ?: return error(lineNumber, sourceLine, "无法识别 DDL：$ddlText")
        } ?: naturalDdl
        val reminderResult = if (ddl != null) parseReminderTag(content, ddl, now) else null
        if (reminderResult != null && !reminderResult.isValid) {
            return error(lineNumber, sourceLine, "提醒格式无效：${reminderResult.message}")
        }
        val reminder = if (ddl != null) reminderResult?.offsetsMinutes ?: listOf(DEFAULT_PLANNING_REMINDER_MINUTES) else emptyList()
        val warnings = mutableListOf<String>()
        if (ddl != null && !ddl.isAfter(now)) warnings += "DDL 已早于当前时间，导入前请调整。"
        val importBlocked = ddl != null && (!ddl.isAfter(now) || reminder.any { offset -> !ddl.minusMinutes(offset.toLong()).isAfter(now) })
        if (importBlocked && warnings.isEmpty()) warnings += "提醒时间已经过去，请调整 DDL 或提醒时间后再导入。"
        naturalTextMessage(content, naturalDdl != null)?.let { warnings += it }
        val group = tagValue(content, "group").orEmpty()
        val notes = parentTitle?.let { "所属大任务：$it" }.orEmpty()
        return PlanningParsedCandidate(
            id = "line-$lineNumber",
            lineNumber = lineNumber,
            sourceLine = sourceLine,
            type = PlanningParsedType.TODO,
            title = title,
            notes = notes,
            groupName = group,
            dueAt = ddl,
            reminderOffsetsMinutes = reminder,
            parentTitle = parentTitle,
            importBlocked = importBlocked,
            message = warnings.distinct().joinToString("；")
        )
    }

    private fun parseNaturalTodoHints(
        lineNumber: Int,
        sourceLine: String,
        content: String,
        dateContext: LocalDate?,
        parentTitle: String?,
        now: LocalDateTime
    ): PlanningParsedCandidate? {
        val ddl = parseNaturalDdlHint(content, dateContext, now) ?: return null
        val title = cleanNaturalTodoTitle(content).trim()
        if (title.isBlank()) return error(lineNumber, sourceLine, "待办标题不能为空。")
        val reminderResult = parseReminderTag(content, ddl, now)
        if (reminderResult != null && !reminderResult.isValid) {
            return error(lineNumber, sourceLine, "提醒格式无效：${reminderResult.message}")
        }
        val reminder = reminderResult?.offsetsMinutes ?: listOf(DEFAULT_PLANNING_REMINDER_MINUTES)
        val warnings = mutableListOf("根据自然文本推断，建议确认")
        naturalTextMessage(content, true)?.let { warnings += it }
        if (!ddl.isAfter(now)) warnings += "DDL 已早于当前时间，导入前请调整。"
        val importBlocked = !ddl.isAfter(now) || reminder.any { offset -> !ddl.minusMinutes(offset.toLong()).isAfter(now) }
        if (importBlocked && warnings.none { it.contains("提醒时间已经过去") }) {
            warnings += "提醒时间已经过去，请调整 DDL 或提醒时间后再导入。"
        }
        return PlanningParsedCandidate(
            id = "line-$lineNumber",
            lineNumber = lineNumber,
            sourceLine = sourceLine,
            type = PlanningParsedType.TODO,
            title = title,
            notes = parentTitle?.let { "所属大任务：$it" }.orEmpty(),
            groupName = tagValue(content, "group").orEmpty(),
            dueAt = ddl,
            reminderOffsetsMinutes = reminder,
            parentTitle = parentTitle,
            importBlocked = importBlocked,
            message = warnings.distinct().joinToString("；")
        )
    }

    private fun parseNaturalDdlHint(content: String, dateContext: LocalDate?, now: LocalDateTime): LocalDateTime? {
        val text = normalizeSyntaxText(content)
        DdlKeywordRegex.find(text)?.let { match ->
            val raw = text.substring(match.range.last + 1).trim().trimStart(':', '：', ' ', ',')
            parseDateTimeExpression(raw, defaultDate = dateContext, nowDate = now.toLocalDate(), defaultTime = LocalTime.of(23, 59))?.let {
                return it
            }
        }
        BeforeTimeRegex.find(text)?.let { match ->
            val prefix = text.substring(0, match.range.first)
            val date = parseDateFromNaturalPrefix(prefix.trim(), now.toLocalDate()) ?: dateContext ?: now.toLocalDate()
            val period = ChineseDayPeriodRegex.find(prefix)?.value.orEmpty()
            val hour = match.groupValues[1].toIntOrNull() ?: return@let
            val minute = match.groupValues[2].takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
            val normalizedHour = normalizeNaturalHour(hour, period)
            if (normalizedHour in 0..23 && minute in 0..59) {
                return LocalDateTime.of(date, LocalTime.of(normalizedHour, minute))
            }
        }
        if (dateContext != null) {
            FuzzyPeriodDeadlineRegex.find(text)?.let { match ->
                return LocalDateTime.of(dateContext, fuzzyPeriodDeadlineTime(match.value) ?: return@let)
            }
        }
        return null
    }

    private fun parseDateFromNaturalPrefix(prefix: String, today: LocalDate): LocalDate? {
        val match = LeadingDateRegex.find(prefix)?.takeIf { it.range.first == 0 } ?: return null
        return parseDateExpression(match.value.trim(), today)?.date
    }

    private fun normalizeNaturalHour(hour: Int, period: String): Int {
        if (period.isBlank()) return if (hour in 1..7) hour + 12 else hour
        return applyChineseDayPeriod(hour, period) ?: hour
    }

    private fun fuzzyPeriodDeadlineTime(period: String): LocalTime? {
        return when (period) {
            "早上" -> LocalTime.of(9, 0)
            "上午", "中午" -> LocalTime.NOON
            "下午" -> LocalTime.of(17, 0)
            "晚上" -> LocalTime.of(22, 0)
            else -> null
        }
    }

    private fun naturalTextMessage(content: String, inferred: Boolean): String? {
        val parts = mutableListOf<String>()
        if (inferred) parts += "根据自然文本推断，建议确认"
        if (RecurrenceHintRegex.containsMatchIn(content)) parts += "检测到循环关键词，如需循环请导入后在待办编辑器中设置"
        return parts.distinct().joinToString("；").takeIf { it.isNotBlank() }
    }

    private fun cleanNaturalTodoTitle(raw: String): String {
        return cleanTitle(raw)
            .replace(DdlKeywordRegex, " ")
            .replace(BeforeTimeRegex, " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun parseReminderTag(content: String, anchor: LocalDateTime, now: LocalDateTime): ReminderTextParseResult? {
        val raw = tagValue(content, "remind") ?: return null
        return parseReminderTextInput(raw = raw, anchor = anchor, now = now, requireFuture = false)
    }

    private fun parseNaturalEvent(text: String, dateContext: LocalDate?, now: LocalDateTime): ParsedNaturalEvent? {
        val trimmed = normalizeSyntaxText(text).trim()
        val leadingDate = parseLeadingDate(trimmed, now.toLocalDate())
        val date = leadingDate?.date ?: dateContext ?: now.toLocalDate()
        val defaultToday = leadingDate == null && dateContext == null
        val rest = leadingDate?.rest?.trimStart() ?: trimmed
        val match = TimeRangeRegex.find(rest) ?: return null
        val startTime = parseTimeToken(match.groupValues[1]) ?: return null
        val endNextDay = match.groupValues[2].isNotBlank()
        val endTime = parseTimeToken(match.groupValues[3]) ?: return null
        val startAt = LocalDateTime.of(date, startTime)
        var endAt = LocalDateTime.of(date, endTime)
        if (endNextDay || !endAt.isAfter(startAt)) {
            endAt = endAt.plusDays(1)
        }
        val title = listOf(
            rest.substring(0, match.range.first).trim(),
            rest.substring(match.range.last + 1).trim()
        ).filter { it.isNotBlank() }.joinToString(" ")
        return ParsedNaturalEvent(
            startAt = startAt,
            endAt = endAt,
            title = cleanTitle(title),
            matchedText = rest.substring(match.range),
            defaultToday = defaultToday
        )
    }

    private fun parseLeadingDate(text: String, today: LocalDate): ParsedLeadingDate? {
        LeadingDateRegex.find(text)?.takeIf { it.range.first == 0 }?.let { match ->
            val rest = text.substring(match.range.last + 1)
            if (!isLeadingDateBoundary(rest)) return null
            val parsed = parseDateExpression(match.value.trim(), today) ?: return null
            return ParsedLeadingDate(parsed.date, rest)
        }
        return null
    }

    fun parseDateTimeExpression(
        raw: String,
        defaultDate: LocalDate?,
        nowDate: LocalDate,
        defaultTime: LocalTime?
    ): LocalDateTime? {
        val text = normalizeSyntaxText(raw).trim().replace('T', ' ')
        if (text.isBlank()) return null
        val leadingDate = parseLeadingDate(text, nowDate)
        val date = leadingDate?.date ?: defaultDate ?: nowDate
        val rest = if (leadingDate == null) text else leadingDate.rest.trimStart().trimStart(',').trimStart()
        val time = if (rest.isBlank()) {
            defaultTime
        } else {
            TimeOnlyRegex.find(rest)?.takeIf { it.range.first == 0 }?.let { parseTimeToken(it.value) }
        } ?: defaultTime
        return time?.let { LocalDateTime.of(date, it) }
    }

    private fun parseDateExpression(raw: String, today: LocalDate): ParsedDate? {
        val text = normalizeSyntaxText(raw).trim().removePrefix("#").trim()
        when (text) {
            "今天", "今日" -> return ParsedDate(today)
            "明天", "明日" -> return ParsedDate(today.plusDays(1))
            "后天" -> return ParsedDate(today.plusDays(2))
        }
        weekdayFromText(text)?.let { weekday ->
            val delta = (weekday.value - today.dayOfWeek.value + 7) % 7
            return ParsedDate(today.plusDays(delta.toLong()))
        }
        FullDateRegex.matchEntire(text)?.let { match ->
            return safeDate(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())?.let(::ParsedDate)
        }
        FullChineseDateRegex.matchEntire(text)?.let { match ->
            return safeDate(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())?.let(::ParsedDate)
        }
        MonthDayRegex.matchEntire(text)?.let { match ->
            return safeDate(today.year, match.groupValues[1].toInt(), match.groupValues[2].toInt())?.let(::ParsedDate)
        }
        ChineseMonthDayRegex.matchEntire(text)?.let { match ->
            return safeDate(today.year, match.groupValues[1].toInt(), match.groupValues[2].toInt())?.let(::ParsedDate)
        }
        return null
    }

    private fun parseHeadingDateContext(raw: String, today: LocalDate): ParsedDate? {
        val text = normalizeSyntaxText(raw).trim()
        when (text) {
            "今日", "今天", "今日计划", "今天计划" -> return ParsedDate(today)
            "明天", "明日", "明天计划", "明日计划" -> return ParsedDate(today.plusDays(1))
            "后天", "后天计划" -> return ParsedDate(today.plusDays(2))
        }
        parseDateExpression(text, today)?.let { return it }
        val match = LeadingDateRegex.find(text)?.takeIf { it.range.first == 0 } ?: return null
        val date = parseDateExpression(match.value.trim(), today) ?: return null
        val rest = text.substring(match.range.last + 1)
        return if (isHeadingDateContextRest(rest)) date else null
    }

    private fun parseTimeToken(raw: String): LocalTime? {
        val match = TimeOnlyRegex.matchEntire(normalizeSyntaxText(raw).trim()) ?: return null
        val chinesePeriod = match.groupValues.getOrNull(1).orEmpty()
        var hour = match.groupValues[2].toInt()
        val minute = match.groupValues[3].toInt()
        val period = match.groupValues.getOrNull(4).orEmpty().lowercase(Locale.ROOT)
        if (minute !in 0..59) return null
        if (period.isNotBlank()) {
            if (hour !in 1..12) return null
            hour = when {
                period == "pm" && hour < 12 -> hour + 12
                period == "am" && hour == 12 -> 0
                else -> hour
            }
        } else if (chinesePeriod.isNotBlank()) {
            hour = applyChineseDayPeriod(hour, chinesePeriod) ?: return null
        }
        if (hour !in 0..23) return null
        return LocalTime.of(hour, minute)
    }

    private fun parseCheckbox(line: String): ParsedCheckbox? {
        val match = CheckboxRegex.matchEntire(line) ?: return null
        val indent = match.groupValues[1].replace("\t", "  ").length / 2
        val completed = match.groupValues[2].equals("x", ignoreCase = true)
        return ParsedCheckbox(indent, completed, match.groupValues[3])
    }

    private fun headingTextOrNull(trimmed: String): String? {
        val match = HeadingRegex.matchEntire(trimmed) ?: return null
        return match.groupValues[1].trim()
    }

    private fun cleanTitle(raw: String): String {
        var text = raw
        listOf("schedule", "ddl", "remind", "group").forEach { tag ->
            text = text.replace(Regex("(?:^|\\s)#$tag\\s*[^#]+"), " ")
        }
        text = text.replace(BareDdlRegex, " ")
        text = text.replace(BeforeTimeRegex, " ")
        listOf("task", "imported").forEach { tag ->
            text = text.replace(Regex("(?:^|\\s)#$tag(?=\\s|$)"), " ")
        }
        return text.trim().replace(Regex("\\s+"), " ")
    }

    private fun tagValue(content: String, tag: String): String? {
        val match = Regex("(?:^|\\s)#$tag\\s*([^#]+)").find(content) ?: return null
        return match.groupValues[1].trim().takeIf { it.isNotBlank() }
    }

    private fun bareDdlValue(content: String): String? {
        if (content.contains("#ddl")) return null
        val match = BareDdlRegex.find(content) ?: return null
        return match.groupValues[1].trim().takeIf { it.isNotBlank() }
    }

    private fun hasSimpleTag(content: String, tag: String): Boolean {
        return Regex("(?:^|\\s)#$tag(?=\\s|$)").containsMatchIn(content)
    }

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate? {
        return try {
            LocalDate.of(year, month, day)
        } catch (_: DateTimeException) {
            null
        }
    }

    private fun normalizeSyntaxText(raw: String): String {
        return raw
            .replace('：', ':')
            .replace('，', ',')
            .replace('．', '.')
            .replace('。', '.')
            .replace('／', '/')
            .replace('－', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace('～', '~')
            .replace('〜', '~')
    }

    private fun isLeadingDateBoundary(rest: String): Boolean {
        val first = rest.firstOrNull() ?: return true
        return first.isWhitespace() || first == ',' || first.isDigit()
    }

    private fun isHeadingDateContextRest(rest: String): Boolean {
        val trimmed = rest.trim()
        if (trimmed.isBlank()) return true
        val first = rest.firstOrNull()
        if (first != null && (first.isWhitespace() || first == ',')) return true
        return trimmed == "计划" ||
            trimmed == "安排" ||
            trimmed == "日程" ||
            trimmed == "任务" ||
            trimmed == "待办" ||
            trimmed == "清单" ||
            trimmed.endsWith("计划")
    }

    private fun applyChineseDayPeriod(hour: Int, period: String): Int? {
        if (hour !in 0..23) return null
        return when (period) {
            "凌晨" -> if (hour == 12) 0 else hour
            "早上", "上午" -> if (hour == 12) 0 else hour
            "中午" -> if (hour in 1..10) hour + 12 else hour
            "下午", "晚上" -> if (hour < 12) hour + 12 else hour
            else -> null
        }
    }

    private fun weekdayFromText(text: String): DayOfWeek? {
        return when (text) {
            "周一", "星期一", "礼拜一" -> DayOfWeek.MONDAY
            "周二", "星期二", "礼拜二" -> DayOfWeek.TUESDAY
            "周三", "星期三", "礼拜三" -> DayOfWeek.WEDNESDAY
            "周四", "星期四", "礼拜四" -> DayOfWeek.THURSDAY
            "周五", "星期五", "礼拜五" -> DayOfWeek.FRIDAY
            "周六", "星期六", "礼拜六" -> DayOfWeek.SATURDAY
            "周日", "周天", "星期日", "星期天", "礼拜日", "礼拜天" -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    private fun skipped(lineNumber: Int, sourceLine: String, message: String, imported: Boolean = false, completed: Boolean = false): PlanningParsedCandidate {
        return PlanningParsedCandidate(
            id = "line-$lineNumber",
            lineNumber = lineNumber,
            sourceLine = sourceLine,
            type = PlanningParsedType.SKIPPED,
            imported = imported,
            completed = completed,
            message = message
        )
    }

    private fun error(lineNumber: Int, sourceLine: String, message: String): PlanningParsedCandidate {
        return PlanningParsedCandidate(
            id = "line-$lineNumber",
            lineNumber = lineNumber,
            sourceLine = sourceLine,
            type = PlanningParsedType.ERROR,
            message = message
        )
    }

    private data class ParsedCheckbox(val indentLevel: Int, val completed: Boolean, val content: String)
    private data class ParsedDate(val date: LocalDate)
    private data class ParsedLeadingDate(val date: LocalDate, val rest: String)
    private data class ParsedNaturalEvent(
        val startAt: LocalDateTime,
        val endAt: LocalDateTime,
        val title: String,
        val matchedText: String,
        val defaultToday: Boolean
    )

    private val HeadingRegex = Regex("^#{1,6}\\s+(.+)$")
    private val CheckboxRegex = Regex("^(\\s*)-\\s*\\[([ xX])\\]\\s*(.*)$")
    private val FullDateRegex = Regex("^(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})$")
    private val FullChineseDateRegex = Regex("^(\\d{4})年(\\d{1,2})月(\\d{1,2})日?$")
    private val MonthDayRegex = Regex("^(\\d{1,2})[-./](\\d{1,2})$")
    private val ChineseMonthDayRegex = Regex("^(\\d{1,2})月(\\d{1,2})日?$")
    private val TimeOnlyRegex = Regex("^(凌晨|早上|上午|中午|下午|晚上)?\\s*(\\d{1,2})[:：](\\d{2})\\s*([aApP][mM])?$")
    private val TimeTokenPattern = "(?:凌晨|早上|上午|中午|下午|晚上)?\\s*\\d{1,2}[:：]\\d{2}\\s*(?:[aApP][mM])?"
    private val TimeRangeRegex = Regex("($TimeTokenPattern)\\s*(?:-|~|至|到)\\s*(次日)?($TimeTokenPattern)")
    private val LeadingDateRegex = Regex("^(今天|今日|明天|明日|后天|周[一二三四五六日天]|星期[一二三四五六日天]|礼拜[一二三四五六日天]|\\d{4}[-./]\\d{1,2}[-./]\\d{1,2}|\\d{4}年\\d{1,2}月\\d{1,2}日?|\\d{1,2}[-./]\\d{1,2}|\\d{1,2}月\\d{1,2}日?)")
    private val BareDdlRegex = Regex("(?:^|\\s)ddl\\s+([^#]+)", RegexOption.IGNORE_CASE)
    private val DdlKeywordRegex = Regex("(截止|deadline|ddl)", RegexOption.IGNORE_CASE)
    private val BeforeTimeRegex = Regex("(\\d{1,2})(?::(\\d{2})|点)(?:前|之前)")
    private val ChineseDayPeriodRegex = Regex("(早上|上午|中午|下午|晚上)")
    private val FuzzyPeriodDeadlineRegex = Regex("(早上|上午|中午|下午|晚上)")
    private val RecurrenceHintRegex = Regex("(每天|每日|每周|每星期|每月|每年|工作日|周末)")
}

const val DEFAULT_PLANNING_REMINDER_MINUTES = 5
