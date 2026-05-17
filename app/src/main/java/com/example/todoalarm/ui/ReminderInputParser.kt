package com.example.todoalarm.ui

import com.example.todoalarm.data.parseConcreteReminderDateTimeText
import com.example.todoalarm.data.parseReminderTextInput
import com.example.todoalarm.data.parseSnoozeTextInput
import com.example.todoalarm.data.reminderTextLeadTimeLabel
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.util.Locale

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

internal data class DdlPostponeInputValidation(
    val isValid: Boolean,
    val targetDueAt: LocalDateTime? = null,
    val message: String = ""
)

internal fun parseReminderInput(
    raw: String,
    anchor: LocalDateTime,
    now: LocalDateTime = LocalDateTime.now().withSecond(0).withNano(0),
    requireFuture: Boolean = true
): ReminderInputValidation {
    val parsed = parseReminderTextInput(raw = raw, anchor = anchor, now = now, requireFuture = requireFuture)
    return ReminderInputValidation(
        isValid = parsed.isValid,
        offsetsMinutes = parsed.offsetsMinutes,
        triggerTimes = parsed.triggerTimes,
        message = parsed.message
    )
}

internal fun reminderInputLeadTimeLabel(minutes: Int): String {
    return reminderTextLeadTimeLabel(minutes)
}

internal fun parseSnoozeInput(
    raw: String,
    now: LocalDateTime = LocalDateTime.now().withSecond(0).withNano(0)
): SnoozeInputValidation {
    val parsed = parseSnoozeTextInput(raw = raw, now = now)
    return SnoozeInputValidation(parsed.isValid, parsed.minutes, parsed.message)
}

internal fun parseDdlPostponeInput(
    raw: String,
    currentDueAt: LocalDateTime,
    now: LocalDateTime = LocalDateTime.now().withSecond(0).withNano(0)
): DdlPostponeInputValidation {
    val token = raw.trim()
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
    if (token.isBlank()) {
        return DdlPostponeInputValidation(false, message = "请输入要推迟的分钟数或目标 DDL。")
    }

    val target = DdlPostponeMinuteRegex.matchEntire(token)?.let { match ->
        val minutes = match.groupValues[1].toIntOrNull()
            ?: return DdlPostponeInputValidation(false, message = "推迟分钟数无效。")
        if (minutes <= 0) {
            return DdlPostponeInputValidation(false, message = "推迟分钟数必须大于 0。")
        }
        currentDueAt.plusMinutes(minutes.toLong())
    } ?: parseConcreteReminderDateTimeText(
        raw = token,
        anchor = currentDueAt,
        today = now.toLocalDate()
    ) ?: return DdlPostponeInputValidation(
        false,
        message = "示例：30分钟、往后推45分钟、16:30、2026-05-22 16:30。"
    )

    if (!target.isAfter(currentDueAt)) {
        return DdlPostponeInputValidation(false, message = "新的 DDL 必须晚于当前 DDL。")
    }
    return DdlPostponeInputValidation(
        isValid = true,
        targetDueAt = target.withSecond(0).withNano(0),
        message = "DDL 将推迟到 ${target.format(DdlPostponeFormatter)}"
    )
}

private val DdlPostponeMinuteRegex = Regex("^(?:往后推|推迟|延后)?\\s*(\\d+)\\s*(?:分钟|分|min|m)?$", RegexOption.IGNORE_CASE)
private val DdlPostponeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA)
