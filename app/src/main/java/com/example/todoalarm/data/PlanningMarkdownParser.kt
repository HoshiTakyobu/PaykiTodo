package com.example.todoalarm.data

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.Duration
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
    val candidates: List<PlanningParsedCandidate>
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
                parseDateExpression(headingText, now.toLocalDate())?.let { dateContext = it.date }
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
                if (indentLevel == 0 && parsed.type == PlanningParsedType.TODO && parsed.title.isNotBlank()) {
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
        val explicitSchedule = tagValue(content, "schedule")
        val scheduleSource = explicitSchedule ?: content
        val naturalEvent = parseNaturalEvent(scheduleSource, dateContext, now)
        if (explicitSchedule != null || naturalEvent != null) {
            val event = naturalEvent ?: return error(lineNumber, sourceLine, "无法识别 #schedule 后的日程时间。")
            val title = cleanTitle(content)
                .removePrefix(event.matchedText)
                .trim()
                .ifBlank { event.title }
            if (title.isBlank()) return error(lineNumber, sourceLine, "日程标题不能为空。")
            if (!event.endAt.isAfter(event.startAt)) return error(lineNumber, sourceLine, "日程结束时间必须晚于开始时间。")
            val reminder = parseReminderTag(content, event.startAt, now) ?: listOf(DEFAULT_PLANNING_REMINDER_MINUTES)
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

        if (!checkboxPresent && !hasSimpleTag(content, "task") && !content.contains("#ddl")) {
            return null
        }

        val title = cleanTitle(content).trim()
        if (title.isBlank()) return error(lineNumber, sourceLine, "待办标题不能为空。")
        val ddl = tagValue(content, "ddl")?.let { ddlText ->
            parseDateTimeExpression(ddlText, defaultDate = dateContext, nowDate = now.toLocalDate(), defaultTime = LocalTime.of(23, 59))
                ?: return error(lineNumber, sourceLine, "无法识别 DDL：$ddlText")
        }
        val reminder = if (ddl != null) parseReminderTag(content, ddl, now) ?: listOf(DEFAULT_PLANNING_REMINDER_MINUTES) else emptyList()
        val warnings = mutableListOf<String>()
        if (ddl != null && !ddl.isAfter(now)) warnings += "DDL 已早于当前时间，导入前请调整。"
        val importBlocked = ddl != null && (!ddl.isAfter(now) || reminder.any { offset -> !ddl.minusMinutes(offset.toLong()).isAfter(now) })
        if (importBlocked && warnings.isEmpty()) warnings += "提醒时间已经过去，请调整 DDL 或提醒时间后再导入。"
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
            message = warnings.joinToString("；")
        )
    }

    private fun parseReminderTag(content: String, anchor: LocalDateTime, now: LocalDateTime): List<Int>? {
        val raw = tagValue(content, "remind") ?: return null
        val offsets = mutableListOf<Int>()
        val tokens = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        tokens.forEach { token ->
            val normalized = token.replace('：', ':')
            val offset = normalized.toIntOrNull()
            if (offset != null) {
                offsets += offset.coerceAtLeast(0)
            } else {
                val triggerAt = parseDateTimeExpression(normalized, defaultDate = anchor.toLocalDate(), nowDate = now.toLocalDate(), defaultTime = null)
                    ?: return null
                if (triggerAt.isAfter(anchor)) return null
                offsets += Duration.between(triggerAt, anchor).toMinutes().toInt().coerceAtLeast(0)
            }
        }
        return normalizeReminderOffsets(offsets)
    }

    private fun parseNaturalEvent(text: String, dateContext: LocalDate?, now: LocalDateTime): ParsedNaturalEvent? {
        val trimmed = text.trim()
        val leadingDate = parseLeadingDate(trimmed, now.toLocalDate())
        val date = leadingDate?.date ?: dateContext ?: now.toLocalDate()
        val defaultToday = leadingDate == null && dateContext == null
        val rest = leadingDate?.rest?.trimStart() ?: trimmed
        val match = TimeRangeRegex.find(rest) ?: return null
        if (match.range.first != 0) return null
        val startTime = parseTimeToken(match.groupValues[1]) ?: return null
        val endNextDay = match.groupValues[2].isNotBlank()
        val endTime = parseTimeToken(match.groupValues[3]) ?: return null
        val startAt = LocalDateTime.of(date, startTime)
        var endAt = LocalDateTime.of(date, endTime)
        if (endNextDay || !endAt.isAfter(startAt)) {
            endAt = endAt.plusDays(1)
        }
        val title = rest.substring(match.range.last + 1).trim()
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
            val parsed = parseDateExpression(match.value.trim(), today) ?: return null
            return ParsedLeadingDate(parsed.date, text.substring(match.range.last + 1))
        }
        return null
    }

    fun parseDateTimeExpression(
        raw: String,
        defaultDate: LocalDate?,
        nowDate: LocalDate,
        defaultTime: LocalTime?
    ): LocalDateTime? {
        val text = raw.trim().replace('：', ':').replace("，", ",")
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
        val text = raw.trim().removePrefix("#").trim()
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

    private fun parseTimeToken(raw: String): LocalTime? {
        val match = TimeOnlyRegex.matchEntire(raw.trim().replace('：', ':')) ?: return null
        var hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        val period = match.groupValues.getOrNull(3).orEmpty().lowercase(Locale.ROOT)
        if (minute !in 0..59) return null
        if (period.isNotBlank()) {
            if (hour !in 1..12) return null
            hour = when {
                period == "pm" && hour < 12 -> hour + 12
                period == "am" && hour == 12 -> 0
                else -> hour
            }
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
            text = text.replace(Regex("(?:^|\\s)#$tag\\s+[^#]+"), " ")
        }
        listOf("task", "project", "today", "tomorrow", "important", "imported").forEach { tag ->
            text = text.replace(Regex("(?:^|\\s)#$tag(?=\\s|$)"), " ")
        }
        return text.trim().replace(Regex("\\s+"), " ")
    }

    private fun tagValue(content: String, tag: String): String? {
        val match = Regex("(?:^|\\s)#$tag\\s+([^#]+)").find(content) ?: return null
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
    private val FullDateRegex = Regex("^(\\d{4})[-.](\\d{1,2})[-.](\\d{1,2})$")
    private val FullChineseDateRegex = Regex("^(\\d{4})年(\\d{1,2})月(\\d{1,2})日?$")
    private val MonthDayRegex = Regex("^(\\d{1,2})[.-](\\d{1,2})$")
    private val ChineseMonthDayRegex = Regex("^(\\d{1,2})月(\\d{1,2})日?$")
    private val TimeOnlyRegex = Regex("^(\\d{1,2})[:：](\\d{2})\\s*([aApP][mM])?$")
    private val TimeRangeRegex = Regex("^(\\d{1,2}[:：]\\d{2}\\s*(?:[aApP][mM])?)\\s*(?:-|~|至|到)\\s*(次日)?(\\d{1,2}[:：]\\d{2}\\s*(?:[aApP][mM])?)")
    private val LeadingDateRegex = Regex("^(今天|今日|明天|明日|后天|周[一二三四五六日天]|星期[一二三四五六日天]|礼拜[一二三四五六日天]|\\d{4}[-.]\\d{1,2}[-.]\\d{1,2}|\\d{4}年\\d{1,2}月\\d{1,2}日?|\\d{1,2}[.-]\\d{1,2}|\\d{1,2}月\\d{1,2}日?)")
}

const val DEFAULT_PLANNING_REMINDER_MINUTES = 5
