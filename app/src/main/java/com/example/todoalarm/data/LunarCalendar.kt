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
        var cursor = LocalDate.of(year, 1, 1).minusDays(45)
        val last = LocalDate.of(year, 12, 31).plusDays(45)
        while (!cursor.isAfter(last)) {
            val label = labelFor(cursor)
            if (label.month == month && label.day == day && label.isLeapMonth == isLeapMonth) {
                return cursor
            }
            cursor = cursor.plusDays(1)
        }
        return null
    }

    fun sameLunarDateInYear(sourceDate: LocalDate, targetYear: Int): LocalDate? {
        val source = labelFor(sourceDate)
        return findDate(targetYear, source.month, source.day, source.isLeapMonth)
            ?: if (source.isLeapMonth) findDate(targetYear, source.month, source.day, false) else null
    }
}
