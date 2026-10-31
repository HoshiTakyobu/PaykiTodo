package com.example.todoalarm.data

import org.json.JSONArray
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

data class AllDayReminderRule(val daysBefore: Int, val minuteOfDay: Int)

fun calendarZone(timeZoneId: String): ZoneId =
    if (timeZoneId.isBlank()) ZoneId.systemDefault() else ZoneId.of(timeZoneId)

fun calendarEpochMillis(value: LocalDateTime, timeZoneId: String): Long {
    val zone = calendarZone(timeZoneId)
    require(zone.rules.getValidOffsets(value).isNotEmpty()) { "所选时区的这个时间不存在，请调整时间" }
    return value.atZone(zone).withLaterOffsetAtOverlap().toInstant().toEpochMilli()
}

fun decodeAllDayReminderRules(json: String): List<AllDayReminderRule> {
    if (json.isBlank()) return emptyList()
    val array = JSONArray(json)
    require(array.length() <= 24) { "最多设置24个全天提醒" }
    return (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        val days = item.get("daysBefore")
        val minutes = item.get("minuteOfDay")
        require((days is Int || days is Long) && (minutes is Int || minutes is Long)) { "全天提醒必须使用整数日期和钟点" }
        require((days as Number).toLong() in 0..3650 && (minutes as Number).toLong() in 0..1439) { "全天提醒的日期或钟点无效" }
        AllDayReminderRule((days as Number).toInt(), (minutes as Number).toInt()).also {
            require(it.daysBefore in 0..3650 && it.minuteOfDay in 0..1439) { "全天提醒的日期或钟点无效" }
        }
    }.distinct().sortedWith(compareByDescending<AllDayReminderRule> { it.daysBefore }.thenBy { it.minuteOfDay })
}

fun allDayReminderTimes(date: LocalDate, timeZoneId: String, json: String): List<Long> =
    decodeAllDayReminderRules(json).map { rule ->
        calendarEpochMillis(date.minusDays(rule.daysBefore.toLong()).atTime(LocalTime.of(rule.minuteOfDay / 60, rule.minuteOfDay % 60)), timeZoneId)
    }.distinct().sorted()

/** Generate only the requested date window; the original anchor determines interval phases. */
fun calendarRecurrenceDates(
    anchor: LocalDate,
    config: RecurrenceConfig,
    rangeStart: LocalDate = anchor,
    rangeEnd: LocalDate = config.endDate ?: rangeStart.plusDays(90)
): List<LocalDate> {
    if (!config.isRecurring) return listOf(anchor).filter { it >= rangeStart && it <= rangeEnd }
    require(config.interval in 1..10000) { "循环间隔必须为1至10000" }
    val start = maxOf(anchor, rangeStart)
    val end = minOf(rangeEnd, config.endDate ?: rangeEnd)
    if (end < start) return emptyList()
    require(ChronoUnit.DAYS.between(start, end) <= 36600) { "一次最多预览100年的循环日期" }
    if (config.type == RecurrenceType.YEARLY_LUNAR_DATE) {
        val firstYear = maxOf(anchor.year, start.year - 1)
        return (firstYear..(end.year + 1)).filter { (it - anchor.year) % config.interval == 0 }
            .mapNotNull { LunarCalendar.sameLunarDateInYear(anchor, it) }
            .filter { it >= start && it <= end }.distinct().sorted()
    }
    val weekdays = config.weeklyDays.ifEmpty { setOf(anchor.dayOfWeek) }
    val anchorWeek = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val interval = config.interval.toLong()
    val ordinal = config.monthlyOrdinal ?: anchor.nthWeekOrdinal()
    val skipInvalid = config.invalidDatePolicy == "SKIP"
    val intendedDay = config.monthlyDay ?: anchor.dayOfMonth
    return generateSequence(start) { if (it < end) it.plusDays(1) else null }.filter { date ->
        when (config.type) {
            RecurrenceType.NONE -> date == anchor
            RecurrenceType.DAILY -> ChronoUnit.DAYS.between(anchor, date) % interval == 0L
            RecurrenceType.WEEKLY -> date.dayOfWeek in weekdays &&
                ChronoUnit.WEEKS.between(anchorWeek, date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) % interval == 0L
            RecurrenceType.MONTHLY_DAY -> ChronoUnit.MONTHS.between(anchor.withDayOfMonth(1), date.withDayOfMonth(1)) % interval == 0L &&
                date.dayOfMonth == (if (skipInvalid) intendedDay else minOf(intendedDay, date.lengthOfMonth()))
            RecurrenceType.MONTHLY_NTH_WEEKDAY -> ChronoUnit.MONTHS.between(anchor.withDayOfMonth(1), date.withDayOfMonth(1)) % interval == 0L &&
                date.dayOfWeek == anchor.dayOfWeek && (if (ordinal == -1) date.plusWeeks(1).month != date.month else date.nthWeekOrdinal() == ordinal)
            RecurrenceType.YEARLY_DATE -> (date.year - anchor.year) % config.interval == 0 && date.month == anchor.month &&
                date.dayOfMonth == (if (skipInvalid) intendedDay else minOf(intendedDay, date.lengthOfMonth()))
            RecurrenceType.YEARLY_LUNAR_DATE -> (date.year - anchor.year) % config.interval == 0 &&
                LunarCalendar.sameLunarDateInYear(anchor, date.year) == date
        }
    }.toList()
}

fun TodoItem.calendarStartDateTime(): LocalDateTime =
    Instant.ofEpochMilli(startAtMillis ?: dueAtMillis).atZone(calendarZone(eventTimeZoneId)).toLocalDateTime()

fun TodoItem.calendarEndDateTime(): LocalDateTime =
    Instant.ofEpochMilli(endAtMillis ?: startAtMillis ?: dueAtMillis).atZone(calendarZone(eventTimeZoneId)).toLocalDateTime()
