package com.example.todoalarm.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class PlanningAbsoluteAssignment(
    val slotIndex: Int,
    val occurrenceStartAt: LocalDateTime,
    val absoluteReminderTimes: List<LocalDateTime>
)

/** Absolute instants belong to one occurrence across all slots, independently of the generation window. */
fun assignPlanningAbsoluteReminders(
    slots: List<TransientPlanningSlot>,
    absolutes: List<LocalDateTime>
): List<PlanningAbsoluteAssignment> {
    val owners = linkedMapOf<Pair<Int, LocalDateTime>, MutableList<LocalDateTime>>()
    absolutes.distinct().sorted().forEach { reminder ->
        val next = slots.mapIndexedNotNull { index, slot ->
            nextPlanningOccurrence(slot, reminder)?.let { index to it }
        }.minWithOrNull(compareBy<Pair<Int, LocalDateTime>> { it.second }.thenBy { it.first })
        require(next != null) { "绝对提醒${reminder.toString().replace('T', ' ')}之后没有范围内的课程，请修改提醒或循环范围" }
        owners.getOrPut(next) { mutableListOf() }.add(reminder)
    }
    return owners.map { (key, times) -> PlanningAbsoluteAssignment(key.first, key.second, times.toList()) }
}

private fun nextPlanningOccurrence(slot: TransientPlanningSlot, reminder: LocalDateTime): LocalDateTime? {
    if (!slot.recurrence.isRecurring) return slot.startAt.takeIf { it >= reminder }
    val config = slot.recurrence
    require(config.interval in 1..10000) { "循环间隔必须为1至10000" }
    require(config.type in setOf(RecurrenceType.DAILY, RecurrenceType.WEEKLY, RecurrenceType.MONTHLY_DAY, RecurrenceType.YEARLY_DATE)) {
        "规划绝对提醒需要每天、每周、每月或每年的循环表达式"
    }
    val anchor = slot.startAt.toLocalDate()
    var lower = maxOf(anchor, reminder.toLocalDate())
    if (lower.atTime(slot.startAt.toLocalTime()) < reminder) lower = lower.plusDays(1)
    if (lower.year > 9999 || config.endDate?.let { lower > it } == true) return null
    val interval = config.interval.toLong()
    fun rounds(distance: Long) = (distance.coerceAtLeast(0) + interval - 1) / interval
    fun inPeriod(begin: LocalDate, end: LocalDate): LocalDateTime? {
        if (begin.year > 9999 || end.year > 9999 || config.endDate?.let { begin > it } == true) return null
        return calendarRecurrenceDates(anchor, config, maxOf(lower, begin), end).firstOrNull()
            ?.atTime(slot.startAt.toLocalTime())
    }
    when (config.type) {
        RecurrenceType.DAILY -> {
            val date = anchor.plusDays(rounds(ChronoUnit.DAYS.between(anchor, lower)) * interval)
            return inPeriod(date, date)
        }
        RecurrenceType.WEEKLY -> {
            val anchorWeek = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val lowerWeek = lower.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            var week = anchorWeek.plusWeeks(rounds(ChronoUnit.WEEKS.between(anchorWeek, lowerWeek)) * interval)
            repeat(2) {
                inPeriod(week, week.plusDays(6))?.let { return it }
                week = week.plusWeeks(interval)
            }
        }
        RecurrenceType.MONTHLY_DAY -> {
            val anchorMonth = YearMonth.from(anchor)
            var month = anchorMonth.plusMonths(rounds(ChronoUnit.MONTHS.between(anchorMonth, YearMonth.from(lower))) * interval)
            // Gregorian month shapes repeat every 4800 months; include a partial first period.
            repeat(4801) {
                if (month.year > 9999 || config.endDate?.let { month.atDay(1) > it } == true) return null
                inPeriod(month.atDay(1), month.atEndOfMonth())?.let { return it }
                month = month.plusMonths(interval)
            }
        }
        RecurrenceType.YEARLY_DATE -> {
            var year = anchor.year.toLong() + rounds(lower.year.toLong() - anchor.year) * interval
            repeat(401) {
                if (year > 9999 || config.endDate?.let { LocalDate.of(year.toInt(), 1, 1) > it } == true) return null
                inPeriod(LocalDate.of(year.toInt(), 1, 1), LocalDate.of(year.toInt(), 12, 31))?.let { return it }
                year += interval
            }
        }
        else -> Unit
    }
    return null
}
