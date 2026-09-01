package com.example.todoalarm.data

import java.time.LocalDate
import java.time.LocalDateTime

data class CalendarEventDraft(
    val title: String,
    val notes: String,
    val location: String,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val allDay: Boolean,
    val accentColorHex: String,
    val reminderMinutesBefore: Int? = null,
    val reminderOffsetsMinutes: List<Int> = reminderMinutesBefore?.let { listOf(it) } ?: emptyList(),
    val ringEnabled: Boolean,
    val vibrateEnabled: Boolean,
    val reminderDeliveryMode: ReminderDeliveryMode = ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION,
    val countdownEnabled: Boolean = false,
    val checkInEnabled: Boolean = false,
    val checkInReminderOffsetsMinutes: List<Int> = emptyList(),
    val recurrence: RecurrenceConfig = RecurrenceConfig(),
    val multiSlotBundleId: String? = null,
    val groupId: Long = 0,
    val groupName: String = ""
) {
    val normalizedReminderOffsetsMinutes: List<Int>
        get() = normalizeReminderOffsets(reminderOffsetsMinutes, reminderMinutesBefore)

    val normalizedCheckInReminderOffsetsMinutes: List<Int>
        get() = if (checkInEnabled) normalizeReminderOffsets(checkInReminderOffsetsMinutes) else emptyList()

    val normalizedAllReminderOffsetsMinutes: List<Int>
        get() = normalizeReminderOffsets(normalizedReminderOffsetsMinutes + normalizedCheckInReminderOffsetsMinutes)

    val reminderAnchorAt: LocalDateTime
        get() = if (allDay) {
            LocalDateTime.of(startAt.toLocalDate(), java.time.LocalTime.of(9, 0))
        } else {
            startAt
        }

    val normalizedStartDate: LocalDate
        get() = startAt.toLocalDate()
}
