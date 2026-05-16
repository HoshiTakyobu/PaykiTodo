package com.example.todoalarm.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DailyBoardSnapshot(
    val date: LocalDate,
    val now: LocalDateTime,
    val announcements: List<PlanningAnnouncement>,
    val todoItems: List<TodoItem>,
    val allTodayEvents: List<TodoItem>,
    val visibleTodayEvents: List<TodoItem>,
    val tomorrowEvents: List<TodoItem>
)

object DailyBoardSnapshotBuilder {
    fun build(
        items: List<TodoItem>,
        planningNotes: List<PlanningNote> = emptyList(),
        now: LocalDateTime = LocalDateTime.now(),
        selectedGroupId: Long? = null
    ): DailyBoardSnapshot {
        val today = now.toLocalDate()
        val filteredItems = selectedGroupId?.let { groupId ->
            items.filter { it.groupId == groupId }
        } ?: items
        val activeTaskItems = filteredItems
            .filter { it.isTodo && it.isActive }
            .sortedBy { it.dueAtMillis }
        val calendarItems = items
            .filter { it.isEvent && it.isActive }
            .sortedBy { it.startAtMillis ?: it.dueAtMillis }
        val todayTodos = activeTaskItems.filter { it.hasDueDate && !it.missed && it.dueDate() == today }
        val missedTodos = activeTaskItems.filter { it.missed }
        val boardTodos = (missedTodos + todayTodos)
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<TodoItem> { it.missed }
                    .thenBy { it.dueAtMillis }
            )
        val allTodayEvents = calendarItems.filter { eventOverlapsDay(it, today) }
            .sortedBy { it.startAtMillis ?: it.dueAtMillis }
        val visibleTodayEvents = allTodayEvents.filter { eventVisibleForToday(it, today, now) }
        val tomorrowEvents = calendarItems.filter { eventOverlapsDay(it, today.plusDays(1)) }
            .sortedBy { it.startAtMillis ?: it.dueAtMillis }
        return DailyBoardSnapshot(
            date = today,
            now = now,
            announcements = PlanningAnnouncementParser.activeAnnouncements(planningNotes, today),
            todoItems = boardTodos,
            allTodayEvents = allTodayEvents,
            visibleTodayEvents = visibleTodayEvents,
            tomorrowEvents = tomorrowEvents
        )
    }

    fun eventOverlapsDay(item: TodoItem, date: LocalDate): Boolean {
        val start = item.startAtMillis?.let(::millisToLocalDateTime)?.toLocalDate() ?: return false
        val end = if (item.allDay) {
            item.endAtMillis?.let(::millisToLocalDateTime)?.toLocalDate()?.minusDays(1) ?: start
        } else {
            item.endAtMillis?.let(::millisToLocalDateTime)?.toLocalDate() ?: start
        }
        return !date.isBefore(start) && !date.isAfter(end)
    }

    fun eventVisibleForToday(item: TodoItem, today: LocalDate, now: LocalDateTime): Boolean {
        if (!eventOverlapsDay(item, today)) return false
        if (item.allDay) return true
        val end = item.endAtMillis?.let(::millisToLocalDateTime)
            ?: item.startAtMillis?.let(::millisToLocalDateTime)
            ?: return false
        return end.isAfter(now)
    }

    fun eventInProgress(item: TodoItem, now: LocalDateTime): Boolean {
        val start = item.startAtMillis?.let(::millisToLocalDateTime) ?: return false
        if (item.allDay) return eventOverlapsDay(item, now.toLocalDate())
        val end = item.endAtMillis?.let(::millisToLocalDateTime) ?: start
        return !now.isBefore(start) && now.isBefore(end)
    }

    fun eventSecondaryText(item: TodoItem): String {
        return if (item.allDay) {
            "全天"
        } else {
            val start = item.startAtMillis?.let(::millisToLocalDateTime) ?: return "未设置时间"
            val end = item.endAtMillis?.let(::millisToLocalDateTime) ?: start
            "${clockTime(start)} - ${clockTime(end)}"
        }
    }

    fun clockTime(dateTime: LocalDateTime): String {
        return dateTime.format(TimeFormatter)
    }

    private fun millisToLocalDateTime(millis: Long): LocalDateTime {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
}
