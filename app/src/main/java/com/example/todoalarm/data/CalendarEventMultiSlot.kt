package com.example.todoalarm.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

const val DEFAULT_MULTI_SLOT_EVENT_DAYS = 90L

data class CalendarEventTimeSlot(
    val weekday: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime
)

fun buildWeeklyMultiSlotEventDrafts(
    baseDraft: CalendarEventDraft,
    slots: List<CalendarEventTimeSlot>,
    baseDate: LocalDate = baseDraft.startAt.toLocalDate(),
    recurrenceEndDate: LocalDate = baseDraft.recurrence.endDate
        ?: baseDate.plusDays(DEFAULT_MULTI_SLOT_EVENT_DAYS),
    reminderOffsetsForSlot: (CalendarEventTimeSlot, LocalDateTime) -> List<Int> = { _, _ ->
        baseDraft.normalizedReminderOffsetsMinutes
    }
): List<CalendarEventDraft> {
    require(slots.isNotEmpty()) { "至少需要一个时间段" }
    val bundleId = baseDraft.multiSlotBundleId?.takeIf { it.isNotBlank() }
        ?: UUID.randomUUID().toString()
    return slots.map { slot ->
        require(slot.endTime.isAfter(slot.startTime)) {
            "${slot.weekday} ${slot.startTime}-${slot.endTime}：结束时间必须晚于开始时间"
        }
        val slotDate = nextOrSameWeekday(baseDate, slot.weekday)
        val slotStartAt = LocalDateTime.of(slotDate, slot.startTime)
        val slotEndAt = LocalDateTime.of(slotDate, slot.endTime)
        val offsets = normalizeReminderOffsets(reminderOffsetsForSlot(slot, slotStartAt))
        baseDraft.copy(
            startAt = slotStartAt,
            endAt = slotEndAt,
            allDay = false,
            reminderMinutesBefore = offsets.minOrNull(),
            reminderOffsetsMinutes = offsets,
            multiSlotBundleId = bundleId,
            recurrence = RecurrenceConfig(
                enabled = true,
                type = RecurrenceType.WEEKLY,
                weeklyDays = setOf(slot.weekday),
                endDate = recurrenceEndDate
            )
        )
    }
}

private fun nextOrSameWeekday(base: LocalDate, weekday: DayOfWeek): LocalDate {
    val delta = (weekday.value - base.dayOfWeek.value + 7) % 7
    return base.plusDays(delta.toLong())
}
