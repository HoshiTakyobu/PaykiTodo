package com.example.todoalarm.ui

import com.example.todoalarm.data.parseReminderTextInput
import com.example.todoalarm.data.parseSnoozeTextInput
import com.example.todoalarm.data.reminderTextLeadTimeLabel
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
