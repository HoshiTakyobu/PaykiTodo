package com.example.todoalarm.sync

import com.example.todoalarm.data.AppSettings
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
    val events: List<TodoItem>
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
        put("groups", JSONArray(groups.map { it.toDesktopJson() }))
        put("todos", JSONArray(todos.map { it.toDesktopJson(groupsById[it.groupId]) }))
        put("events", JSONArray(events.map { it.toDesktopJson(groupsById[it.groupId]) }))
    }
}

private fun TaskGroup.toDesktopJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("name", name)
        put("colorHex", colorHex)
        put("isDefault", isDefault)
    }
}

private fun TodoItem.toDesktopJson(group: TaskGroup?): JSONObject {
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
        put("isRecurring", isRecurring)
        put("hasDueDate", hasDueDate)
        put("dueAtMillis", dueAtMillis)
        put("startAtMillis", startAtMillis)
        put("endAtMillis", endAtMillis)
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
