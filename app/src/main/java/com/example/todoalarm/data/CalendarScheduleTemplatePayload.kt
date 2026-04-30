package com.example.todoalarm.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class ScheduleTemplateEntry(
    val title: String,
    val notes: String,
    val location: String,
    val groupName: String,
    val startDayOffset: Int,
    val startMinuteOfDay: Int,
    val endDayOffset: Int,
    val endMinuteOfDay: Int,
    val allDay: Boolean,
    val accentColorHex: String,
    val reminderMinutesBefore: Int?,
    val ringEnabled: Boolean,
    val vibrateEnabled: Boolean,
    val reminderDeliveryMode: ReminderDeliveryMode
)

data class ScheduleTemplatePayload(
    val version: Int = 1,
    val weekStart: LocalDate,
    val entries: List<ScheduleTemplateEntry>
)

fun buildScheduleTemplatePayload(
    weekStart: LocalDate,
    items: List<TodoItem>,
    groupsById: Map<Long, TaskGroup>
): ScheduleTemplatePayload {
    val weekEnd = weekStart.plusDays(6)
    val entries = items.mapNotNull { item ->
        val startAt = item.startAtMillis ?: item.dueAtMillis
        val endAt = item.endAtMillis ?: startAt
        val startDateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(startAt),
            ZoneId.systemDefault()
        )
        val endDateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(endAt),
            ZoneId.systemDefault()
        )
        val startDate = startDateTime.toLocalDate()
        if (startDate.isBefore(weekStart) || startDate.isAfter(weekEnd)) {
            null
        } else {
            val groupName = groupsById[item.groupId]?.name.orEmpty()
            ScheduleTemplateEntry(
                title = item.title,
                notes = item.notes,
                location = item.location,
                groupName = groupName,
                startDayOffset = weekStart.until(startDate).days,
                startMinuteOfDay = startDateTime.toLocalTime().toSecondOfDay() / 60,
                endDayOffset = weekStart.until(endDateTime.toLocalDate()).days,
                endMinuteOfDay = endDateTime.toLocalTime().toSecondOfDay() / 60,
                allDay = item.allDay,
                accentColorHex = item.accentColorHex ?: "#4E87E1",
                reminderMinutesBefore = item.reminderOffsetMinutes,
                ringEnabled = item.ringEnabled,
                vibrateEnabled = item.vibrateEnabled,
                reminderDeliveryMode = item.reminderDeliveryModeEnum
            )
        }
    }.sortedWith(compareBy<ScheduleTemplateEntry> { it.startDayOffset }.thenBy { it.startMinuteOfDay })
    return ScheduleTemplatePayload(weekStart = weekStart, entries = entries)
}

fun ScheduleTemplatePayload.toJsonString(): String {
    return JSONObject().apply {
        put("version", version)
        put("weekStart", weekStart.toString())
        put(
            "entries",
            JSONArray(entries.map { entry ->
                JSONObject().apply {
                    put("title", entry.title)
                    put("notes", entry.notes)
                    put("location", entry.location)
                    put("groupName", entry.groupName)
                    put("startDayOffset", entry.startDayOffset)
                    put("startMinuteOfDay", entry.startMinuteOfDay)
                    put("endDayOffset", entry.endDayOffset)
                    put("endMinuteOfDay", entry.endMinuteOfDay)
                    put("allDay", entry.allDay)
                    put("accentColorHex", entry.accentColorHex)
                    put("reminderMinutesBefore", entry.reminderMinutesBefore)
                    put("ringEnabled", entry.ringEnabled)
                    put("vibrateEnabled", entry.vibrateEnabled)
                    put("reminderDeliveryMode", entry.reminderDeliveryMode.name)
                }
            })
        )
    }.toString()
}

fun parseScheduleTemplatePayload(payloadJson: String): ScheduleTemplatePayload {
    val json = JSONObject(payloadJson)
    val weekStart = LocalDate.parse(json.getString("weekStart"))
    val entriesJson = json.optJSONArray("entries") ?: JSONArray()
    val entries = buildList {
        for (index in 0 until entriesJson.length()) {
            val item = entriesJson.optJSONObject(index) ?: continue
            add(
                ScheduleTemplateEntry(
                    title = item.optString("title"),
                    notes = item.optString("notes"),
                    location = item.optString("location"),
                    groupName = item.optString("groupName"),
                    startDayOffset = item.optInt("startDayOffset"),
                    startMinuteOfDay = item.optInt("startMinuteOfDay"),
                    endDayOffset = item.optInt("endDayOffset"),
                    endMinuteOfDay = item.optInt("endMinuteOfDay"),
                    allDay = item.optBoolean("allDay"),
                    accentColorHex = item.optString("accentColorHex", "#4E87E1"),
                    reminderMinutesBefore = item.optIntOrNull("reminderMinutesBefore"),
                    ringEnabled = item.optBoolean("ringEnabled", true),
                    vibrateEnabled = item.optBoolean("vibrateEnabled", true),
                    reminderDeliveryMode = ReminderDeliveryMode.fromStorage(item.optString("reminderDeliveryMode"))
                )
            )
        }
    }
    return ScheduleTemplatePayload(
        version = json.optInt("version", 1),
        weekStart = weekStart,
        entries = entries
    )
}

fun ScheduleTemplatePayload.toDraftsForWeek(targetWeekStart: LocalDate): List<CalendarEventDraft> {
    return entries.map { entry ->
        val startDate = targetWeekStart.plusDays(entry.startDayOffset.toLong())
        val endDate = targetWeekStart.plusDays(entry.endDayOffset.toLong())
        val startAt = LocalDateTime.of(startDate, minuteOfDayToTime(entry.startMinuteOfDay))
        val endAt = LocalDateTime.of(endDate, minuteOfDayToTime(entry.endMinuteOfDay))
        CalendarEventDraft(
            title = entry.title,
            notes = entry.notes,
            location = entry.location,
            startAt = if (entry.allDay) LocalDateTime.of(startDate, LocalTime.MIN) else startAt,
            endAt = if (entry.allDay) LocalDateTime.of(endDate, LocalTime.MIN) else endAt,
            allDay = entry.allDay,
            accentColorHex = entry.accentColorHex,
            reminderMinutesBefore = entry.reminderMinutesBefore,
            ringEnabled = entry.ringEnabled,
            vibrateEnabled = entry.vibrateEnabled,
            reminderDeliveryMode = entry.reminderDeliveryMode,
            recurrence = RecurrenceConfig(),
            groupName = entry.groupName
        )
    }
}

fun ScheduleTemplatePayload.toWeeklyRecurringDrafts(
    targetWeekStart: LocalDate,
    recurrenceEndDate: LocalDate
): List<CalendarEventDraft> {
    return entries.map { entry ->
        val startDate = targetWeekStart.plusDays(entry.startDayOffset.toLong())
        val endDate = targetWeekStart.plusDays(entry.endDayOffset.toLong())
        val startAt = LocalDateTime.of(startDate, minuteOfDayToTime(entry.startMinuteOfDay))
        val endAt = LocalDateTime.of(endDate, minuteOfDayToTime(entry.endMinuteOfDay))
        CalendarEventDraft(
            title = entry.title,
            notes = entry.notes,
            location = entry.location,
            startAt = if (entry.allDay) LocalDateTime.of(startDate, LocalTime.MIN) else startAt,
            endAt = if (entry.allDay) LocalDateTime.of(endDate, LocalTime.MIN) else endAt,
            allDay = entry.allDay,
            accentColorHex = entry.accentColorHex,
            reminderMinutesBefore = entry.reminderMinutesBefore,
            ringEnabled = entry.ringEnabled,
            vibrateEnabled = entry.vibrateEnabled,
            reminderDeliveryMode = entry.reminderDeliveryMode,
            recurrence = RecurrenceConfig(
                enabled = true,
                type = RecurrenceType.WEEKLY,
                weeklyDays = setOf(startDate.dayOfWeek),
                endDate = recurrenceEndDate
            ),
            groupName = entry.groupName
        )
    }
}

private fun minuteOfDayToTime(minutes: Int): LocalTime {
    val normalized = minutes.coerceIn(0, 23 * 60 + 59)
    return LocalTime.of(normalized / 60, normalized % 60)
}

private fun JSONObject.optIntOrNull(key: String): Int? {
    return if (has(key) && !isNull(key)) optInt(key) else null
}
