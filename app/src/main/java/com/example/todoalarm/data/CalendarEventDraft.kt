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
    val reminderMinutesBefore: Int?,
    val ringEnabled: Boolean,
    val vibrateEnabled: Boolean,
    val reminderDeliveryMode: ReminderDeliveryMode = ReminderDeliveryMode.NOTIFICATION,
    val recurrence: RecurrenceConfig = RecurrenceConfig(),
    val groupId: Long = 0,
    val groupName: String = ""
) {
    val reminderAnchorAt: LocalDateTime
        get() = if (allDay) {
            LocalDateTime.of(startAt.toLocalDate(), java.time.LocalTime.of(9, 0))
        } else {
            startAt
        }

    val normalizedStartDate: LocalDate
        get() = startAt.toLocalDate()
}
