package com.example.todoalarm.sync

import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.DailyBoardSnapshot
import com.example.todoalarm.data.PlanningAnnouncement
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.TodoItem
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

data class DesktopSyncStatus(
    val enabled: Boolean,
    val running: Boolean,
    val port: Int,
    val token: String,
    val ipAddresses: List<String>
)

data class DesktopSyncSnapshot(
    val generatedAtMillis: Long,
    val groups: List<TaskGroup>,
    val todos: List<TodoItem>,
    val events: List<TodoItem>,
    val announcements: List<PlanningAnnouncement> = emptyList(),
    val todayBoard: DesktopDailyBoardSnapshot,
    val partial: Boolean = false
)

data class DesktopDailyBoardSnapshot(
    val date: String,
    val nowMillis: Long,
    val countdownItems: List<TodoItem>,
    val todoItems: List<TodoItem>,
    val allTodayEvents: List<TodoItem>,
    val visibleTodayEvents: List<TodoItem>,
    val tomorrowEvents: List<TodoItem>,
    val todayFocusMinutes: Int,
    val todayFocusSessionCount: Int,
    val todayCompletedFocusSessionCount: Int
)

fun DesktopSyncStatus.toJson(): JSONObject {
    return JSONObject().apply {
        put("enabled", enabled)
        put("running", running)
        put("port", port)
        put("token", token)
        put("ipAddresses", JSONArray(ipAddresses))
    }
}

fun DesktopSyncSnapshot.toJson(groupsById: Map<Long, TaskGroup>): JSONObject {
    return JSONObject().apply {
        put("generatedAtMillis", generatedAtMillis)
        put("partial", partial)
        put("groups", JSONArray(groups.map { it.toDesktopJson() }))
        put("todos", JSONArray(todos.map { it.toDesktopJson(groupsById[it.groupId]) }))
        put("events", JSONArray(events.map { it.toDesktopJson(groupsById[it.groupId]) }))
        put("announcements", JSONArray(announcements.map { it.toDesktopJson() }))
        put("todayBoard", todayBoard.toDesktopJson(groupsById))
    }
}

fun DailyBoardSnapshot.toDesktopSyncBoard(
    nowMillis: Long,
    todayFocusMinutes: Int,
    todayFocusSessionCount: Int,
    todayCompletedFocusSessionCount: Int
): DesktopDailyBoardSnapshot {
    return DesktopDailyBoardSnapshot(
        date = date.toString(),
        nowMillis = nowMillis,
        countdownItems = countdownItems,
        todoItems = todoItems,
        allTodayEvents = allTodayEvents,
        visibleTodayEvents = visibleTodayEvents,
        tomorrowEvents = tomorrowEvents,
        todayFocusMinutes = todayFocusMinutes,
        todayFocusSessionCount = todayFocusSessionCount,
        todayCompletedFocusSessionCount = todayCompletedFocusSessionCount
    )
}

private fun DesktopDailyBoardSnapshot.toDesktopJson(groupsById: Map<Long, TaskGroup>): JSONObject {
    return JSONObject().apply {
        put("date", date)
        put("nowMillis", nowMillis)
        put("countdownItems", JSONArray(countdownItems.map { it.toDesktopJson(groupsById[it.groupId]) }))
        put("todoItems", JSONArray(todoItems.map { it.toDesktopJson(groupsById[it.groupId]) }))
        put("allTodayEvents", JSONArray(allTodayEvents.map { it.toDesktopJson(groupsById[it.groupId]) }))
        put("visibleTodayEvents", JSONArray(visibleTodayEvents.map { it.toDesktopJson(groupsById[it.groupId]) }))
        put("tomorrowEvents", JSONArray(tomorrowEvents.map { it.toDesktopJson(groupsById[it.groupId]) }))
        put("todayFocusMinutes", todayFocusMinutes)
        put("todayFocusSessionCount", todayFocusSessionCount)
        put("todayCompletedFocusSessionCount", todayCompletedFocusSessionCount)
    }
}

private fun PlanningAnnouncement.toDesktopJson(): JSONObject {
    return JSONObject().apply {
        put("text", text)
        put("rangeLabel", rangeLabel())
        put("sourceNoteTitle", sourceNoteTitle)
        put("sourceNoteId", sourceNoteId)
        put("lineNumber", lineNumber)
    }
}

fun TaskGroup.toDesktopJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("name", name)
        put("colorHex", colorHex)
        put("isDefault", isDefault)
    }
}

fun TodoItem.toDesktopJson(group: TaskGroup?): JSONObject {
    val zone = ZoneId.systemDefault()
    return JSONObject().apply {
        put("id", id)
        put("itemType", itemType)
        put("title", title)
        put("notes", notes)
        put("location", location)
        put("groupId", groupId)
        put("groupName", group?.name)
        put("groupColorHex", group?.colorHex)
        put("completed", completed)
        put("canceled", canceled)
        put("missed", missed)
        put("allDay", allDay)
        put("countdownEnabled", countdownEnabled)
        put("isRecurring", isRecurring)
        put("recurringSeriesId", recurringSeriesId)
        put("hasDueDate", hasDueDate)
        put("dueAtMillis", dueAtMillis)
        put("startAtMillis", startAtMillis)
        put("endAtMillis", endAtMillis)
        put("completedAtMillis", completedAtMillis)
        put("canceledAtMillis", canceledAtMillis)
        put("missedAtMillis", missedAtMillis)
        put("dueAtLabel", dueDateTimeOrNull()?.toString())
        put("startAtLabel", startAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime().toString() })
        put("endAtLabel", endAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime().toString() })
        put("reminderEnabled", reminderEnabled)
        put("reminderAtMillis", reminderAtMillis)
        put("reminderOffsetsMinutes", JSONArray(configuredReminderOffsetsMinutes))
        put("ringEnabled", ringEnabled)
        put("vibrateEnabled", vibrateEnabled)
        put("reminderDeliveryMode", reminderDeliveryMode)
        put("accentColorHex", accentColorHex)
        put("recurrenceType", recurrenceType)
        put("recurrenceWeekdays", recurrenceWeekdays)
        put("recurrenceMonthlyOrdinal", recurrenceMonthlyOrdinal)
        put("recurrenceMonthlyWeekday", recurrenceMonthlyWeekday)
        put("recurrenceMonthlyDay", recurrenceMonthlyDay)
        put("recurrenceEndDate", recurrenceEndDate?.toString())
    }
}

fun AppSettings.toDesktopSyncStatus(running: Boolean, port: Int, ipAddresses: List<String>): DesktopSyncStatus {
    return DesktopSyncStatus(
        enabled = desktopSyncEnabled,
        running = running,
        port = port,
        token = desktopSyncToken,
        ipAddresses = ipAddresses
    )
}
