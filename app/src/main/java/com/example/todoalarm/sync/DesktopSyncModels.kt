package com.example.todoalarm.sync

import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.DailyBoardSnapshot
import com.example.todoalarm.data.PlanningAnnouncement
import com.example.todoalarm.data.QuickChecklistGroup
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.WeekStartMode
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

data class DesktopSyncStatus(
    val enabled: Boolean,
    val running: Boolean,
    val port: Int,
    val token: String,
    val ipAddresses: List<String>,
    val connected: Boolean = false,
    val lastAuthorizedAtMillis: Long = 0L,
    val secondsUntilAutoStop: Long? = null,
    val localNetworkAvailable: Boolean = false,
    val networkHint: String = ""
)

data class DesktopSyncSnapshot(
    val generatedAtMillis: Long,
    val groups: List<TaskGroup>,
    val quickChecklistGroups: List<QuickChecklistGroup>,
    val todos: List<TodoItem>,
    val events: List<TodoItem>,
    val announcements: List<PlanningAnnouncement> = emptyList(),
    val todayBoard: DesktopDailyBoardSnapshot,
    val weekStartMode: WeekStartMode = WeekStartMode.MONDAY,
    val partial: Boolean = false
)

data class DesktopDailyBoardSnapshot(
    val date: String,
    val nowMillis: Long,
    val countdownItems: List<TodoItem>,
    val todoItems: List<TodoItem>,
    val allTodayEvents: List<TodoItem>,
    val visibleTodayEvents: List<TodoItem>,
    val tomorrowEvents: List<TodoItem>
)

fun DesktopSyncStatus.toJson(): JSONObject {
    return JSONObject().apply {
        put("enabled", enabled)
        put("running", running)
        put("port", port)
        put("token", token)
        put("ipAddresses", JSONArray(ipAddresses))
        put("connected", connected)
        put("lastAuthorizedAtMillis", lastAuthorizedAtMillis)
        if (secondsUntilAutoStop == null) {
            put("secondsUntilAutoStop", JSONObject.NULL)
        } else {
            put("secondsUntilAutoStop", secondsUntilAutoStop)
        }
        put("localNetworkAvailable", localNetworkAvailable)
        put("networkHint", networkHint)
    }
}

fun DesktopSyncSnapshot.toJson(
    groupsById: Map<Long, TaskGroup>,
    todoGroupIdsByTodoId: Map<Long, List<Long>> = emptyMap()
): JSONObject {
    return JSONObject().apply {
        put("generatedAtMillis", generatedAtMillis)
        put("partial", partial)
        put("groups", JSONArray(groups.map { it.toDesktopJson() }))
        put("quickChecklistGroups", JSONArray(quickChecklistGroups.map { it.toDesktopJson() }))
        put("todos", JSONArray(todos.map { it.toDesktopJson(groupsById[it.groupId], todoGroupIdsByTodoId[it.id].orEmpty()) }))
        put("events", JSONArray(events.map { it.toDesktopJson(groupsById[it.groupId]) }))
        put("announcements", JSONArray(announcements.map { it.toDesktopJson() }))
        put("weekStartMode", weekStartMode.name)
        put("todayBoard", todayBoard.toDesktopJson(groupsById, todoGroupIdsByTodoId))
    }
}

fun QuickChecklistGroup.toDesktopJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("title", title)
        put("collapsed", collapsed)
        put("sourceFingerprint", sourceFingerprint)
        put("createdAtMillis", createdAtMillis)
        put("updatedAtMillis", updatedAtMillis)
    }
}

fun DailyBoardSnapshot.toDesktopSyncBoard(nowMillis: Long): DesktopDailyBoardSnapshot {
    return DesktopDailyBoardSnapshot(
        date = date.toString(),
        nowMillis = nowMillis,
        countdownItems = countdownItems,
        todoItems = todoItems,
        allTodayEvents = allTodayEvents,
        visibleTodayEvents = visibleTodayEvents,
        tomorrowEvents = tomorrowEvents
    )
}

private fun DesktopDailyBoardSnapshot.toDesktopJson(
    groupsById: Map<Long, TaskGroup>,
    todoGroupIdsByTodoId: Map<Long, List<Long>> = emptyMap()
): JSONObject {
    return JSONObject().apply {
        put("date", date)
        put("nowMillis", nowMillis)
        put("countdownItems", JSONArray(countdownItems.map { it.toDesktopJson(groupsById[it.groupId], todoGroupIdsByTodoId[it.id].orEmpty()) }))
        put("todoItems", JSONArray(todoItems.map { it.toDesktopJson(groupsById[it.groupId], todoGroupIdsByTodoId[it.id].orEmpty()) }))
        put("allTodayEvents", JSONArray(allTodayEvents.map { it.toDesktopJson(groupsById[it.groupId]) }))
        put("visibleTodayEvents", JSONArray(visibleTodayEvents.map { it.toDesktopJson(groupsById[it.groupId]) }))
        put("tomorrowEvents", JSONArray(tomorrowEvents.map { it.toDesktopJson(groupsById[it.groupId]) }))
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

fun TodoItem.toDesktopJson(group: TaskGroup?, groupIds: List<Long> = emptyList()): JSONObject {
    val zone = ZoneId.systemDefault()
    val normalizedGroupIds = (groupIds.ifEmpty { listOf(groupId) })
        .filter { it > 0 }
        .distinct()
    return JSONObject().apply {
        put("id", id)
        put("itemType", itemType)
        put("title", title)
        put("notes", notes)
        put("location", location)
        put("groupId", groupId)
        put("groupIds", JSONArray(normalizedGroupIds))
        put("groupName", group?.name)
        put("groupColorHex", group?.colorHex)
        put("completed", completed)
        put("canceled", canceled)
        put("missed", missed)
        put("allDay", allDay)
        put("eventTimeZoneId", eventTimeZoneId)
        put("allDayReminderRulesJson", allDayReminderRulesJson)
        put("eventBusyStatus", eventBusyStatus)
        put("eventVisibility", eventVisibility)
        put("recurrenceInterval", recurrenceInterval)
        put("recurrenceInvalidDatePolicy", recurrenceInvalidDatePolicy)

        put("countdownEnabled", countdownEnabled)
        put("hiddenFromBoard", hiddenFromBoard)
        put("checkInEnabled", checkInEnabled)
        put("checkInReminderOffsetsMinutes", JSONArray(configuredCheckInReminderOffsetsMinutes))
        put("totalCheckInMinutes", totalCheckInMinutes)
        put("isRecurring", isRecurring)
        put("recurringSeriesId", recurringSeriesId)
        put("multiSlotBundleId", multiSlotBundleId)
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
        put("alarmMode", alarmMode)
        put("reminderDeliveryMode", reminderDeliveryMode)
        put("accentColorHex", accentColorHex)
        put("recurrenceType", recurrenceType)
        put("recurrenceWeekdays", recurrenceWeekdays)
        put("recurrenceMonthlyOrdinal", recurrenceMonthlyOrdinal)
        put("recurrenceMonthlyWeekday", recurrenceMonthlyWeekday)
        put("recurrenceMonthlyDay", recurrenceMonthlyDay)
        put("recurrenceEndDate", recurrenceEndDate?.toString())
        put("quickChecklistGroupId", quickChecklistGroupId)
        put("parentTodoId", parentTodoId)
        put("quickChecklistDepth", quickChecklistDepth)
        put("quickChecklistSortOrder", quickChecklistSortOrder)
        put("quickChecklistCollapsed", quickChecklistCollapsed)
    }
}

fun AppSettings.toDesktopSyncStatus(
    running: Boolean,
    port: Int,
    ipAddresses: List<String>,
    connected: Boolean = false,
    lastAuthorizedAtMillis: Long = 0L,
    secondsUntilAutoStop: Long? = null,
    localNetworkAvailable: Boolean = ipAddresses.isNotEmpty(),
    networkHint: String = ""
): DesktopSyncStatus {
    return DesktopSyncStatus(
        enabled = desktopSyncEnabled,
        running = running,
        port = port,
        token = desktopSyncToken,
        ipAddresses = ipAddresses,
        connected = connected,
        lastAuthorizedAtMillis = lastAuthorizedAtMillis,
        secondsUntilAutoStop = secondsUntilAutoStop,
        localNetworkAvailable = localNetworkAvailable,
        networkHint = networkHint
    )
}
