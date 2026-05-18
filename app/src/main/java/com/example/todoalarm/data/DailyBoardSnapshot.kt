package com.example.todoalarm.data

import java.time.Instant
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DailyBoardSnapshot(
    val date: LocalDate,
    val now: LocalDateTime,
    val announcements: List<PlanningAnnouncement>,
    val countdownItems: List<TodoItem>,
    val todoItems: List<TodoItem>,
    val allTodayEvents: List<TodoItem>,
    val visibleTodayEvents: List<TodoItem>,
    val tomorrowEvents: List<TodoItem>
)

data class CountdownRemainingDisplay(
    val primary: String,
    val secondary: String
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
        val nowMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val countdownItems = items
            .filter { it.isActive && it.countdownEnabled }
            .filter { item -> countdownTargetMillis(item)?.let { it >= nowMillis } == true }
            .distinctBy { it.id }
            .sortedWith(
                compareBy<TodoItem> { countdownTargetMillis(it) ?: Long.MAX_VALUE }
                    .thenBy { it.createdAtMillis }
            )
        val todayTodos = activeTaskItems.filter { !it.missed && (!it.hasDueDate || it.dueDate() == today) }
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
            countdownItems = countdownItems,
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

    fun countdownTargetMillis(item: TodoItem): Long? {
        return when {
            item.isEvent -> item.startAtMillis ?: item.dueAtMillis
            item.isTodo && item.hasDueDate -> item.dueAtMillis
            else -> null
        }
    }

    fun countdownTargetDate(item: TodoItem): LocalDate? {
        return countdownTargetMillis(item)
            ?.let(::millisToLocalDateTime)
            ?.toLocalDate()
    }

    fun countdownDays(item: TodoItem, today: LocalDate): Long? {
        val targetDate = countdownTargetDate(item) ?: return null
        return java.time.temporal.ChronoUnit.DAYS.between(today, targetDate)
    }

    fun countdownRemainingDisplay(item: TodoItem, now: LocalDateTime): CountdownRemainingDisplay? {
        val target = countdownTargetMillis(item)?.let(::millisToLocalDateTime) ?: return null
        return countdownRemainingDisplay(target = target, now = now)
    }

    fun countdownRemainingDisplay(target: LocalDateTime, now: LocalDateTime): CountdownRemainingDisplay {
        val remaining = Duration.between(now, target).takeIf { !it.isNegative } ?: Duration.ZERO
        val totalMinutes = remaining.toMinutes()
        return when {
            totalMinutes >= MinutesPerDay -> {
                val days = totalMinutes / MinutesPerDay
                val remainder = totalMinutes % MinutesPerDay
                val hours = remainder / 60
                val minutes = remainder % 60
                CountdownRemainingDisplay(
                    primary = "${days}d",
                    secondary = "${hours}h ${minutes}m"
                )
            }
            totalMinutes >= 60 -> {
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                CountdownRemainingDisplay(
                    primary = "${hours}h",
                    secondary = "${minutes}m"
                )
            }
            else -> CountdownRemainingDisplay(
                primary = "${totalMinutes}m",
                secondary = ""
            )
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
    private const val MinutesPerDay = 24L * 60L
}
