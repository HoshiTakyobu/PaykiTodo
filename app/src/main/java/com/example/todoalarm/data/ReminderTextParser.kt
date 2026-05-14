package com.example.todoalarm.data

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

internal data class ReminderTextParseResult(
    val isValid: Boolean,
    val offsetsMinutes: List<Int> = emptyList(),
    val triggerTimes: List<LocalDateTime> = emptyList(),
    val message: String = ""
)

internal data class SnoozeTextParseResult(
    val isValid: Boolean,
    val minutes: Int = 0,
    val message: String = ""
)

internal fun parseReminderTextInput(
    raw: String,
    anchor: LocalDateTime,
    now: LocalDateTime = LocalDateTime.now().withSecond(0).withNano(0),
    requireFuture: Boolean = true
): ReminderTextParseResult {
    val tokens = splitReminderTokens(raw)

    if (tokens.isEmpty()) {
        return ReminderTextParseResult(isValid = false, message = "请输入提醒时间。")
    }

    val offsets = mutableListOf<Int>()
    val triggerTimes = mutableListOf<LocalDateTime>()
    for (token in tokens) {
        val triggerAt = parseReminderTriggerToken(token, anchor, now.toLocalDate())
            ?: return ReminderTextParseResult(
                isValid = false,
                message = "无法识别“$token”。示例：5,15,16:30,05-10 15:00,5.10 15:00,5月10日 15:00,2026-05-10 14:30"
            )

        if (triggerAt.isAfter(anchor)) {
            return ReminderTextParseResult(false, message = "“$token” 晚于任务 DDL / 日程开始时间。")
        }
        if (requireFuture && !triggerAt.isAfter(now)) {
            return ReminderTextParseResult(false, message = "“$token” 对应的提醒时间已经过去。")
        }

        offsets += Duration.between(triggerAt, anchor).toMinutes().toInt().coerceAtLeast(0)
        triggerTimes += triggerAt.withSecond(0).withNano(0)
    }

    val normalizedOffsets = normalizeReminderOffsets(offsets)
    val normalizedTriggers = normalizedOffsets
        .map { anchor.minusMinutes(it.toLong()).withSecond(0).withNano(0) }
        .sorted()
    return ReminderTextParseResult(
        isValid = true,
        offsetsMinutes = normalizedOffsets,
        triggerTimes = normalizedTriggers,
        message = normalizedOffsets.joinToString("、") { reminderTextLeadTimeLabel(it) }
    )
}

private fun splitReminderTokens(raw: String): List<String> {
    val parts = raw.split(',', '，')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (parts.isEmpty()) return emptyList()

    val tokens = mutableListOf<String>()
    var index = 0
    while (index < parts.size) {
        val current = parts[index]
        val next = parts.getOrNull(index + 1)
        if (next != null && isDateOnlyReminderToken(current) && parseTimeToken(next) != null) {
            tokens += "$current $next"
            index += 2
        } else {
            tokens += current
            index += 1
        }
    }
    return tokens
}

internal fun reminderTextLeadTimeLabel(minutes: Int): String {
    return when {
        minutes == 0 -> "准时提醒"
        minutes < 60 -> "提前 ${minutes} 分钟"
        minutes % (24 * 60) == 0 -> "提前 ${minutes / (24 * 60)} 天"
        minutes % 60 == 0 -> "提前 ${minutes / 60} 小时"
        else -> "提前 ${minutes} 分钟"
    }
}

internal fun parseSnoozeTextInput(
    raw: String,
    now: LocalDateTime = LocalDateTime.now().withSecond(0).withNano(0)
): SnoozeTextParseResult {
    val token = normalizeSyntaxText(raw).trim()
    if (token.isBlank()) {
        return SnoozeTextParseResult(false, message = "请输入延后分钟或具体时刻。")
    }

    val target = if (token.all { it.isDigit() }) {
        val minutes = token.toIntOrNull() ?: return SnoozeTextParseResult(false, message = "延后分钟数无效。")
        if (minutes < 1) return SnoozeTextParseResult(false, message = "延后分钟数必须大于 0。")
        return SnoozeTextParseResult(true, minutes = minutes, message = "延后 $minutes 分钟")
    } else {
        parseReminderTriggerToken(token, anchor = now, today = now.toLocalDate())
            ?: return SnoozeTextParseResult(false, message = "示例：5、16:30、05-10 15:00、5月10日 15:00、2026-05-10 14:30。")
    }

    if (!target.isAfter(now)) {
        return SnoozeTextParseResult(false, message = "延后目标时间必须晚于当前时间。")
    }
    val minutes = Duration.between(now, target).toMinutes().toInt().coerceAtLeast(1)
    return SnoozeTextParseResult(true, minutes = minutes, message = "延后 $minutes 分钟")
}

private fun parseReminderTriggerToken(token: String, anchor: LocalDateTime, today: LocalDate): LocalDateTime? {
    val text = normalizeSyntaxText(token).trim().replace('T', ' ')
    if (text.isBlank()) return null
    if (text.all { it.isDigit() }) {
        val minutes = text.toIntOrNull() ?: return null
        if (minutes < 0) return null
        return anchor.minusMinutes(minutes.toLong())
    }

    parseTimeToken(text)?.let { return LocalDateTime.of(anchor.toLocalDate(), it) }

    val leadingDate = parseLeadingDate(text, today) ?: return null
    val rest = leadingDate.rest.trimStart().trimStart(',').trimStart()
    val time = parseTimeToken(rest) ?: return null
    return LocalDateTime.of(leadingDate.date, time)
}

private fun parseLeadingDate(text: String, today: LocalDate): ParsedLeadingDate? {
    LeadingDateRegex.find(text)?.takeIf { it.range.first == 0 }?.let { match ->
        val rest = text.substring(match.range.last + 1)
        if (!isLeadingDateBoundary(rest)) return null
        val date = parseDateExpression(match.value.trim(), today) ?: return null
        return ParsedLeadingDate(date, rest)
    }
    return null
}

private fun parseDateExpression(raw: String, today: LocalDate): LocalDate? {
    val text = normalizeSyntaxText(raw).trim().removePrefix("#").trim()
    when (text) {
        "今天", "今日" -> return today
        "明天", "明日" -> return today.plusDays(1)
        "后天" -> return today.plusDays(2)
    }
    weekdayFromText(text)?.let { weekday ->
        val delta = (weekday.value - today.dayOfWeek.value + 7) % 7
        return today.plusDays(delta.toLong())
    }
    FullDateRegex.matchEntire(text)?.let { match ->
        return safeDate(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())
    }
    FullChineseDateRegex.matchEntire(text)?.let { match ->
        return safeDate(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())
    }
    MonthDayRegex.matchEntire(text)?.let { match ->
        return safeDate(today.year, match.groupValues[1].toInt(), match.groupValues[2].toInt())
    }
    ChineseMonthDayRegex.matchEntire(text)?.let { match ->
        return safeDate(today.year, match.groupValues[1].toInt(), match.groupValues[2].toInt())
    }
    return null
}

private fun isDateOnlyReminderToken(text: String): Boolean {
    return ReminderDateOnlyRegex.matches(normalizeSyntaxText(text).trim())
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

private data class ParsedLeadingDate(val date: LocalDate, val rest: String)

private val FullDateRegex = Regex("^(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})$")
private val FullChineseDateRegex = Regex("^(\\d{4})年(\\d{1,2})月(\\d{1,2})日?$")
private val MonthDayRegex = Regex("^(\\d{1,2})[-./](\\d{1,2})$")
private val ChineseMonthDayRegex = Regex("^(\\d{1,2})月(\\d{1,2})日?$")
private val TimeOnlyRegex = Regex("^(凌晨|早上|上午|中午|下午|晚上)?\\s*(\\d{1,2})[:：](\\d{2})\\s*([aApP][mM])?$")
private val LeadingDateRegex = Regex("^(今天|今日|明天|明日|后天|周[一二三四五六日天]|星期[一二三四五六日天]|礼拜[一二三四五六日天]|\\d{4}[-./]\\d{1,2}[-./]\\d{1,2}|\\d{4}年\\d{1,2}月\\d{1,2}日?|\\d{1,2}[-./]\\d{1,2}|\\d{1,2}月\\d{1,2}日?)")
private val ReminderDateOnlyRegex = Regex("^(今天|今日|明天|明日|后天|周[一二三四五六日天]|星期[一二三四五六日天]|礼拜[一二三四五六日天]|\\d{4}[-./]\\d{1,2}[-./]\\d{1,2}|\\d{4}年\\d{1,2}月\\d{1,2}日?|\\d{1,2}[-./]\\d{1,2}|\\d{1,2}月\\d{1,2}日?)$")
