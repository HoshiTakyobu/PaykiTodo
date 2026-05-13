package com.example.todoalarm.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class RecurrenceType(val label: String) {
    NONE("不重复"),
    DAILY("每天"),
    WEEKLY("每周的周几"),
    MONTHLY_NTH_WEEKDAY("每月的第几个星期几"),
    MONTHLY_DAY("每月的 D 日"),
    YEARLY_DATE("每年的 M 月 D 日"),
    YEARLY_LUNAR_DATE("每年同农历月日");

    companion object {
        fun fromStorage(value: String?): RecurrenceType {
            return entries.firstOrNull { it.name == value } ?: NONE
        }
    }
}

enum class RecurrenceScope(val label: String) {
    CURRENT("当前事件"),
    CURRENT_AND_FUTURE("当前事件和此后所有事件"),
    ALL("所有事件")
}

data class RecurrenceConfig(
    val enabled: Boolean = false,
    val type: RecurrenceType = RecurrenceType.NONE,
    val weeklyDays: Set<DayOfWeek> = emptySet(),
    val endDate: LocalDate? = null
) {
    val isRecurring: Boolean
        get() = enabled && type != RecurrenceType.NONE && endDate != null
}

data class TodoDraft(
    val title: String,
    val notes: String,
    val dueAt: LocalDateTime?,
    val reminderAt: LocalDateTime?,
    val groupId: Long,
    val ringEnabled: Boolean,
    val vibrateEnabled: Boolean,
    val recurrence: RecurrenceConfig = RecurrenceConfig(),
    val reminderOffsetsMinutes: List<Int> = emptyList()
) {
    val normalizedReminderOffsetsMinutes: List<Int>
        get() {
            val fallback = if (dueAt != null && reminderAt != null) {
                ((dueAt.toEpochMillis() - reminderAt.toEpochMillis()) / 60_000L).toInt()
            } else {
                null
            }
            return normalizeReminderOffsets(reminderOffsetsMinutes, fallback)
        }
}

fun Set<DayOfWeek>.toStorageString(): String {
    return map { it.value }
        .sorted()
        .joinToString(",")
}

fun storageStringToWeekdays(value: String): Set<DayOfWeek> {
    if (value.isBlank()) return emptySet()
    return value.split(',')
        .mapNotNull { token -> token.toIntOrNull()?.let { runCatching { DayOfWeek.of(it) }.getOrNull() } }
        .toSet()
}

fun LocalDateTime.toEpochMillis(): Long {
    return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun LocalDate.nthWeekOrdinal(): Int {
    return ((dayOfMonth - 1) / 7) + 1
}

fun LocalDate.resolveMonthlyDay(dayOfMonth: Int): LocalDate {
    val yearMonth = YearMonth.of(year, month)
    return atDaySafe(yearMonth, dayOfMonth)
}

fun resolveMonthlyDate(yearMonth: YearMonth, dayOfMonth: Int): LocalDate {
    return atDaySafe(yearMonth, dayOfMonth)
}

private fun atDaySafe(yearMonth: YearMonth, dayOfMonth: Int): LocalDate {
    return yearMonth.atDay(dayOfMonth.coerceAtMost(yearMonth.lengthOfMonth()))
}

fun resolveNthWeekdayDate(
    yearMonth: YearMonth,
    ordinal: Int,
    dayOfWeek: DayOfWeek
): LocalDate? {
    val first = yearMonth.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth(ordinal, dayOfWeek))
    return if (first.month == yearMonth.month) first else null
}
