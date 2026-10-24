package com.example.todoalarm.data

import android.icu.util.Calendar
import android.icu.util.ChineseCalendar
import android.icu.util.TimeZone
import java.time.LocalDate
import java.util.Date

data class LunarDateLabel(
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean,
    val displayText: String
)

object LunarCalendar {
    private data class LunarDay(val month: Int, val day: Int, val leap: Boolean)
    private data class YearKey(val year: Int, val zone: String)
    private data class YearIndex(val extendedYear: Int, val dates: Map<LunarDay, LocalDate>)
    private val yearIndexes = object : LinkedHashMap<YearKey, YearIndex>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<YearKey, YearIndex>?): Boolean = size > 16
    }
    private val monthNames = listOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
    private val dayNames = listOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    fun labelFor(date: LocalDate): LunarDateLabel {
        val calendar = ChineseCalendar().apply {
            timeZone = TimeZone.getDefault()
            time = Date.from(date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
        }
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val isLeap = calendar.get(ChineseCalendar.IS_LEAP_MONTH) == 1
        val monthText = monthNames.getOrElse(month - 1) { month.toString() } + "月"
        val dayText = dayNames.getOrElse(day - 1) { day.toString() }
        val display = if (day == 1) {
            (if (isLeap) "闰" else "") + monthText
        } else {
            dayText
        }
        return LunarDateLabel(
            month = month,
            day = day,
            isLeapMonth = isLeap,
            displayText = display
        )
    }

    fun findDate(year: Int, month: Int, day: Int, isLeapMonth: Boolean = false): LocalDate? {
        if (month !in 1..12 || day !in 1..30) return null
        return yearIndex(year).dates[LunarDay(month, day, isLeapMonth)]
    }

    fun sameLunarDateInYear(sourceDate: LocalDate, targetYear: Int): LocalDate? {
        val sourceCalendar = ChineseCalendar().apply {
            timeZone = TimeZone.getDefault()
            time = Date.from(sourceDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
        }
        val sourceLunarYearOffset = sourceCalendar.get(Calendar.EXTENDED_YEAR) - yearIndex(sourceDate.year).extendedYear
        val month = sourceCalendar.get(Calendar.MONTH) + 1
        val day = sourceCalendar.get(Calendar.DAY_OF_MONTH)
        val leap = sourceCalendar.get(ChineseCalendar.IS_LEAP_MONTH) == 1
        val index = yearIndex(targetYear + sourceLunarYearOffset)
        return index.dates[LunarDay(month, day, leap)]
            ?: if (leap) index.dates[LunarDay(month, day, false)] else null
    }

    /** Index a complete lunar year, including its January/February dates in the next civil year. */
    @Synchronized
    private fun yearIndex(year: Int): YearIndex {
        val zone = java.time.ZoneId.systemDefault()
        val key = YearKey(year, zone.id)
        yearIndexes[key]?.let { return it }
        val calendar = ChineseCalendar().apply {
            timeZone = TimeZone.getTimeZone(zone.id)
            time = Date.from(LocalDate.of(year, 7, 1).atStartOfDay(zone).toInstant())
        }
        val extendedYear = calendar.get(Calendar.EXTENDED_YEAR)
        val dates = mutableMapOf<LunarDay, LocalDate>()
        var cursor = LocalDate.of(year, 1, 1)
        val end = LocalDate.of(year + 1, 3, 1)
        while (cursor < end) {
            calendar.time = Date.from(cursor.atStartOfDay(zone).toInstant())
            if (calendar.get(Calendar.EXTENDED_YEAR) == extendedYear) {
                dates[LunarDay(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(ChineseCalendar.IS_LEAP_MONTH) == 1)] = cursor
            }
            cursor = cursor.plusDays(1)
        }
        return YearIndex(extendedYear, dates).also { yearIndexes[key] = it }
    }
}
