package com.example.todoalarm.ui

import com.example.todoalarm.data.normalizeReminderOffsets
import java.time.DateTimeException
import java.time.Duration
import java.time.LocalDateTime

internal data class ReminderInputValidation(
    val isValid: Boolean,
    val offsetsMinutes: List<Int> = emptyList(),
    val triggerTimes: List<LocalDateTime> = emptyList(),
    val message: String = ""
)

internal data class SnoozeInputValidation(
    val isValid: Boolean,
    val minutes: Int = 0,
    val message: String = ""
)

private val TimeTokenRegex = Regex("""^(\d{1,2}):(\d{2})$""")
private val MonthDayTimeTokenRegex = Regex("""^(\d{1,2})-(\d{1,2})\s+(\d{1,2}):(\d{2})$""")
private val DateTimeTokenRegex = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})\s+(\d{1,2}):(\d{2})$""")

internal fun parseReminderInput(
    raw: String,
    anchor: LocalDateTime,
    now: LocalDateTime = LocalDateTime.now().withSecond(0).withNano(0),
    requireFuture: Boolean = true
): ReminderInputValidation {
    val tokens = raw.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    if (tokens.isEmpty()) {
        return ReminderInputValidation(isValid = false, message = "请输入提醒时间。")
    }

    val offsets = mutableListOf<Int>()
    val triggerTimes = mutableListOf<LocalDateTime>()

    for (token in tokens) {
        val triggerAt = when {
            token.all { it.isDigit() } -> {
                val minutes = token.toIntOrNull()
                    ?: return ReminderInputValidation(false, message = "“$token” 不是有效的提前分钟数。")
                if (minutes < 0) {
                    return ReminderInputValidation(false, message = "提醒提前分钟数不能为负数。")
                }
                anchor.minusMinutes(minutes.toLong())
            }

            TimeTokenRegex.matches(token) -> {
                val match = requireNotNull(TimeTokenRegex.matchEntire(token))
                val hour = match.groupValues[1].toInt()
                val minute = match.groupValues[2].toInt()
                safeDateTime(anchor.year, anchor.monthValue, anchor.dayOfMonth, hour, minute)
                    ?: return ReminderInputValidation(false, message = "“$token” 不是有效的当天时刻。")
            }

            MonthDayTimeTokenRegex.matches(token) -> {
                val match = requireNotNull(MonthDayTimeTokenRegex.matchEntire(token))
                safeDateTime(
                    year = now.year,
                    month = match.groupValues[1].toInt(),
                    day = match.groupValues[2].toInt(),
                    hour = match.groupValues[3].toInt(),
                    minute = match.groupValues[4].toInt()
                ) ?: return ReminderInputValidation(false, message = "“$token” 不是有效的当年日期时间。")
            }

            DateTimeTokenRegex.matches(token) -> {
                val match = requireNotNull(DateTimeTokenRegex.matchEntire(token))
                safeDateTime(
                    year = match.groupValues[1].toInt(),
                    month = match.groupValues[2].toInt(),
                    day = match.groupValues[3].toInt(),
                    hour = match.groupValues[4].toInt(),
                    minute = match.groupValues[5].toInt()
                ) ?: return ReminderInputValidation(false, message = "“$token” 不是有效的日期时间。")
            }

            else -> return ReminderInputValidation(
                isValid = false,
                message = "无法识别“$token”。示例：5,15,16:30,05-10 15:00,2026-05-10 14:30"
            )
        }

        if (triggerAt.isAfter(anchor)) {
            return ReminderInputValidation(false, message = "“$token” 晚于任务 DDL / 日程开始时间。")
        }
        if (requireFuture && !triggerAt.isAfter(now)) {
            return ReminderInputValidation(false, message = "“$token” 对应的提醒时间已经过去。")
        }

        val offset = Duration.between(triggerAt, anchor).toMinutes().toInt()
        offsets += offset.coerceAtLeast(0)
        triggerTimes += triggerAt.withSecond(0).withNano(0)
    }

    val normalizedOffsets = normalizeReminderOffsets(offsets)
    val normalizedTriggers = normalizedOffsets
        .map { anchor.minusMinutes(it.toLong()).withSecond(0).withNano(0) }
        .sorted()
    return ReminderInputValidation(
        isValid = true,
        offsetsMinutes = normalizedOffsets,
        triggerTimes = normalizedTriggers,
        message = normalizedOffsets.joinToString("、") { reminderInputLeadTimeLabel(it) }
    )
}

internal fun reminderInputLeadTimeLabel(minutes: Int): String {
    return when {
        minutes == 0 -> "准时提醒"
        minutes < 60 -> "提前 ${minutes} 分钟"
        minutes % (24 * 60) == 0 -> "提前 ${minutes / (24 * 60)} 天"
        minutes % 60 == 0 -> "提前 ${minutes / 60} 小时"
        else -> "提前 ${minutes} 分钟"
    }
}

internal fun parseSnoozeInput(
    raw: String,
    now: LocalDateTime = LocalDateTime.now().withSecond(0).withNano(0),
    maxMinutes: Int = 180
): SnoozeInputValidation {
    val token = raw.trim()
    if (token.isBlank()) {
        return SnoozeInputValidation(false, message = "请输入延后分钟或具体时刻。")
    }

    val target = when {
        token.all { it.isDigit() } -> {
            val minutes = token.toIntOrNull()
                ?: return SnoozeInputValidation(false, message = "延后分钟数无效。")
            if (minutes !in 1..maxMinutes) {
                return SnoozeInputValidation(false, message = "请输入 1 到 $maxMinutes 分钟，或一个未来时刻。")
            }
            return SnoozeInputValidation(true, minutes = minutes, message = "延后 $minutes 分钟")
        }

        TimeTokenRegex.matches(token) -> {
            val match = requireNotNull(TimeTokenRegex.matchEntire(token))
            safeDateTime(now.year, now.monthValue, now.dayOfMonth, match.groupValues[1].toInt(), match.groupValues[2].toInt())
                ?: return SnoozeInputValidation(false, message = "时刻格式无效。")
        }

        MonthDayTimeTokenRegex.matches(token) -> {
            val match = requireNotNull(MonthDayTimeTokenRegex.matchEntire(token))
            safeDateTime(
                year = now.year,
                month = match.groupValues[1].toInt(),
                day = match.groupValues[2].toInt(),
                hour = match.groupValues[3].toInt(),
                minute = match.groupValues[4].toInt()
            ) ?: return SnoozeInputValidation(false, message = "日期时间格式无效。")
        }

        DateTimeTokenRegex.matches(token) -> {
            val match = requireNotNull(DateTimeTokenRegex.matchEntire(token))
            safeDateTime(
                year = match.groupValues[1].toInt(),
                month = match.groupValues[2].toInt(),
                day = match.groupValues[3].toInt(),
                hour = match.groupValues[4].toInt(),
                minute = match.groupValues[5].toInt()
            ) ?: return SnoozeInputValidation(false, message = "日期时间格式无效。")
        }

        else -> return SnoozeInputValidation(false, message = "示例：5、16:30、05-10 15:00、2026-05-10 14:30。")
    }

    if (!target.isAfter(now)) {
        return SnoozeInputValidation(false, message = "延后目标时间必须晚于当前时间。")
    }
    val minutes = Duration.between(now, target).toMinutes().toInt().coerceAtLeast(1)
    if (minutes > maxMinutes) {
        return SnoozeInputValidation(false, message = "延后时长不能超过 $maxMinutes 分钟。")
    }
    return SnoozeInputValidation(true, minutes = minutes, message = "延后 $minutes 分钟")
}

private fun safeDateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): LocalDateTime? {
    if (hour !in 0..23 || minute !in 0..59) return null
    return try {
        LocalDateTime.of(year, month, day, hour, minute)
    } catch (_: DateTimeException) {
        null
    }
}
