package com.example.todoalarm.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(
    private val context: Context
) {
    fun exportToUri(targetUri: Uri, snapshot: BackupSnapshot) {
        context.contentResolver.openOutputStream(targetUri)?.bufferedWriter()?.use { writer ->
            writer.write(snapshot.toJson().toString(2))
        } ?: error("无法写入所选文件")
    }

    fun importFromUri(sourceUri: Uri): BackupSnapshot {
        val text = context.contentResolver.openInputStream(sourceUri)?.bufferedReader()?.use { it.readText() }
            ?: error("无法读取所选文件")
        return backupSnapshotFromJson(JSONObject(text))
    }

    fun autoBackupToDirectory(directoryUri: String, snapshot: BackupSnapshot): String {
        val tree = DocumentFile.fromTreeUri(context, Uri.parse(directoryUri))
            ?: error("备份目录不可访问")
        val fileName = "PaykiTodo-backup-${timestamp()}.json"
        val file = tree.createFile("application/json", fileName)
            ?: error("无法在备份目录中创建文件")
        context.contentResolver.openOutputStream(file.uri)?.bufferedWriter()?.use { writer ->
            writer.write(snapshot.toJson().toString(2))
        } ?: error("无法写入自动备份文件")
        return fileName
    }

    fun autoBackupToInternalStorage(snapshot: BackupSnapshot): String {
        val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
        val file = File(backupDir, "PaykiTodo-backup-${timestamp()}.json")
        file.writeText(snapshot.toJson().toString(2))
        return file.absolutePath
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA).format(Date())
    }
}

internal fun BackupSnapshot.toJson(): JSONObject {
    return JSONObject().apply {
        put("exportedAtMillis", exportedAtMillis)
        put("snapshotVersion", pendingQuoteVersion)
        put("settings", settings.toJson())
        put("groups", JSONArray(groups.map { it.toJson() }))
        put("templates", JSONArray(templates.map { it.toJson() }))
        put("tasks", JSONArray(tasks.map { it.toJson() }))
        put("reminderChainLogs", JSONArray(reminderChainLogs.map { it.toJson() }))
        put("scheduleTemplates", JSONArray(scheduleTemplates.map { it.toJson() }))
        put("planningNotes", JSONArray(planningNotes.map { it.toJson() }))
        put("planningLineMappings", JSONArray(planningLineMappings.map { it.toJson() }))
        put("planningNodes", JSONArray(planningNodes.map { it.toJson() }))
        put("aiReports", JSONArray(aiReports.map { it.toJson() }))
        put("todoGroupTags", JSONArray(todoGroupTags.map { it.toJson() }))
        put("eventCheckIns", JSONArray(eventCheckIns.map { it.toJson() }))
        put("recurringInstanceSkips", JSONArray(recurringInstanceSkips.map { it.toJson() }))
    }
}

private fun AppSettings.toJson(): JSONObject {
    return JSONObject().apply {
        put("themeMode", themeMode.name)
        put("weekStartMode", weekStartMode.name)
        put("defaultSnoozeMinutes", defaultSnoozeMinutes)
        put("defaultRingEnabled", defaultRingEnabled)
        put("defaultVibrateEnabled", defaultVibrateEnabled)
        put("defaultVoiceEnabled", defaultVoiceEnabled)
        put("defaultCalendarReminderMode", defaultCalendarReminderMode.name)
        put("autoCheckOutEventOnEnd", autoCheckOutEventOnEnd)
        put("showEventCheckInStatsOnComplete", showEventCheckInStatsOnComplete)
        put("eventCheckInIdleAutoCheckOutHours", eventCheckInIdleAutoCheckOutHours)
        put("reminderToneUri", reminderToneUri)
        put("reminderToneName", reminderToneName)
        put("reminderAudioChannel", reminderAudioChannel.name)
        put("reminderInternalVolumePercent", reminderInternalVolumePercent)
        put("reminderBoostSystemVolume", reminderBoostSystemVolume)
        put("reminderBoostVolumePercent", reminderBoostVolumePercent)
        put("workQuietModeEnabled", workQuietModeEnabled)
        put("quoteIndex", quoteIndex)
        put("backupDirectoryUri", backupDirectoryUri)
        put("autoBackupEnabled", autoBackupEnabled)
        put("desktopSyncWifiKeepAlive", desktopSyncWifiKeepAlive)
        put("lastOpenedPlanningNoteId", lastOpenedPlanningNoteId)
        put("planningOutlineHintVisible", planningOutlineHintVisible)
        put("planningEventEndTodoEnabled", planningEventEndTodoEnabled)
        put("dailyBriefEnabled", dailyBriefEnabled)
        put("dailyBriefHour", dailyBriefHour)
        put("dailyBriefMinute", dailyBriefMinute)
        put("ongoingEventNotificationEnabled", ongoingEventNotificationEnabled)
        put("boardCountdownCollapsed", boardCountdownCollapsed)
        put("boardTodayTodosCollapsed", boardTodayTodosCollapsed)
        put("boardTodayEventsCollapsed", boardTodayEventsCollapsed)
        put("boardTomorrowEventsCollapsed", boardTomorrowEventsCollapsed)
        put("boardAnnouncementCollapsed", boardAnnouncementCollapsed)
        put("planningAiEnabled", planningAiEnabled)
        put("planningAiProviderName", planningAiProviderName)
        put("planningAiBaseUrl", planningAiBaseUrl)
        put("planningAiModel", planningAiModel)
        put("planningAiProviders", JSONArray(planningAiProvidersToJson(planningAiProviders, includeApiKey = false)))
        put("hasSeenOnboarding", hasSeenOnboarding)
        put("dailyReportEnabled", dailyReportEnabled)
        put("dailyReportHour", dailyReportHour)
        put("dailyReportMinute", dailyReportMinute)
        put("weeklyReportEnabled", weeklyReportEnabled)
        put("weeklyReportHour", weeklyReportHour)
        put("weeklyReportMinute", weeklyReportMinute)
        put("aiReportRetention", aiReportRetention.name)
        put("legacyAiReportMigrated", legacyAiReportMigrated)
    }
}

private fun TaskGroup.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("name", name)
        put("colorHex", colorHex)
        put("sortOrder", sortOrder)
        put("isDefault", isDefault)
        put("createdAtMillis", createdAtMillis)
    }
}

private fun RecurringTaskTemplate.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("seriesId", seriesId)
        put("itemType", itemType)
        put("title", title)
        put("notes", notes)
        put("location", location)
        put("accentColorHex", accentColorHex)
        put("countdownEnabled", countdownEnabled)
        put("hiddenFromBoard", hiddenFromBoard)
        put("allDay", allDay)
        put("groupId", groupId)
        put("dueHour", dueHour)
        put("dueMinute", dueMinute)
        put("eventDurationMinutes", eventDurationMinutes)
        put("reminderOffsetMinutes", reminderOffsetMinutes)
        put("reminderOffsetsCsv", reminderOffsetsCsv)
        put("ringEnabled", ringEnabled)
        put("vibrateEnabled", vibrateEnabled)
        put("alarmMode", alarmMode)
        put("reminderDeliveryMode", reminderDeliveryMode)
        put("recurrenceType", recurrenceType)
        put("recurrenceWeekdays", recurrenceWeekdays)
        put("recurrenceMonthlyOrdinal", recurrenceMonthlyOrdinal)
        put("recurrenceMonthlyWeekday", recurrenceMonthlyWeekday)
        put("recurrenceMonthlyDay", recurrenceMonthlyDay)
        put("recurrenceYearlyMonth", recurrenceYearlyMonth)
        put("recurrenceYearlyDay", recurrenceYearlyDay)
        put("startEpochDay", startEpochDay)
        put("endEpochDay", endEpochDay)
        put("createdAtMillis", createdAtMillis)
    }
}

private fun TodoItem.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("itemType", itemType)
        put("title", title)
        put("notes", notes)
        put("dueAtMillis", dueAtMillis)
        put("startAtMillis", startAtMillis)
        put("endAtMillis", endAtMillis)
        put("allDay", allDay)
        put("location", location)
        put("accentColorHex", accentColorHex)
        put("countdownEnabled", countdownEnabled)
        put("hiddenFromBoard", hiddenFromBoard)
        put("checkInEnabled", checkInEnabled)
        put("totalCheckInMinutes", totalCheckInMinutes)
        put("reminderAtMillis", reminderAtMillis)
        put("reminderOffsetsCsv", reminderOffsetsCsv)
        put("reminderEnabled", reminderEnabled)
        put("ringEnabled", ringEnabled)
        put("vibrateEnabled", vibrateEnabled)
        put("alarmMode", alarmMode)
        put("voiceEnabled", voiceEnabled)
        put("reminderDeliveryMode", reminderDeliveryMode)
        put("groupId", groupId)
        put("categoryKey", categoryKey)
        put("completed", completed)
        put("completedAtMillis", completedAtMillis)
        put("canceled", canceled)
        put("canceledAtMillis", canceledAtMillis)
        put("missed", missed)
        put("missedAtMillis", missedAtMillis)
        put("recurringSeriesId", recurringSeriesId)
        put("recurrenceType", recurrenceType)
        put("recurrenceWeekdays", recurrenceWeekdays)
        put("recurrenceMonthlyOrdinal", recurrenceMonthlyOrdinal)
        put("recurrenceMonthlyWeekday", recurrenceMonthlyWeekday)
        put("recurrenceMonthlyDay", recurrenceMonthlyDay)
        put("recurrenceEndEpochDay", recurrenceEndEpochDay)
        put("recurrenceAnchorDueAtMillis", recurrenceAnchorDueAtMillis)
        put("reminderOffsetMinutes", reminderOffsetMinutes)
        put("createdAtMillis", createdAtMillis)
    }
}

private fun ReminderChainLog.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("todoId", todoId)
        put("chainKey", chainKey)
        put("source", source)
        put("stage", stage)
        put("status", status)
        put("message", message)
        put("reminderAtMillis", reminderAtMillis)
        put("createdAtMillis", createdAtMillis)
    }
}

private fun ScheduleTemplate.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("name", name)
        put("templateType", templateType)
        put("payloadJson", payloadJson)
        put("accentColorHex", accentColorHex)
        put("createdAtMillis", createdAtMillis)
        put("updatedAtMillis", updatedAtMillis)
    }
}

private fun PlanningNote.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("title", title)
        put("contentMarkdown", contentMarkdown)
        put("createdAtMillis", createdAtMillis)
        put("updatedAtMillis", updatedAtMillis)
        put("archived", archived)
        put("documentDateEpochDay", documentDateEpochDay)
        put("hasAnnouncementHint", hasAnnouncementHint)
    }
}

private fun PlanningLineMapping.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("noteId", noteId)
        put("contentFingerprint", contentFingerprint)
        put("originalLineText", originalLineText)
        put("currentLineText", currentLineText)
        put("todoId", todoId)
        put("eventId", eventId)
        put("batchId", batchId)
        put("operationType", operationType)
        put("createdAtMillis", createdAtMillis)
        put("lastRefreshedAtMillis", lastRefreshedAtMillis)
        put("status", status.name)
        put("postponeOffsetMinutes", postponeOffsetMinutes)
        put("lastKnownLineNumber", lastKnownLineNumber)
    }
}

private fun PlanningNode.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("noteId", noteId)
        put("parentNodeId", parentNodeId)
        put("sortOrder", sortOrder)
        put("text", text)
        put("createdAtMillis", createdAtMillis)
        put("updatedAtMillis", updatedAtMillis)
        put("startAtMillis", startAtMillis)
        put("endAtMillis", endAtMillis)
        put("dueAtMillis", dueAtMillis)
        put("location", location)
        put("linkedTodoId", linkedTodoId)
        put("linkedEndTodoId", linkedEndTodoId)
        put("isDraft", isDraft)
        put("isNote", isNote)
        put("syncEnabled", syncEnabled)
        put("collapsed", collapsed)
        put("completed", completed)
        put("completedAtMillis", completedAtMillis)
    }
}

private fun AiReport.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("type", type.name)
        put("generatedAtMillis", generatedAtMillis)
        put("periodStartMillis", periodStartMillis)
        put("periodEndMillis", periodEndMillis)
        put("content", content)
        put("providerName", providerName)
        put("isLocalFallback", isLocalFallback)
    }
}

private fun TodoGroupTag.toJson(): JSONObject {
    return JSONObject().apply {
        put("todoId", todoId)
        put("groupId", groupId)
    }
}

private fun EventCheckIn.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("eventId", eventId)
        put("checkInAtMillis", checkInAtMillis)
        put("checkOutAtMillis", checkOutAtMillis)
        put("durationMinutes", durationMinutes)
    }
}

private fun RecurringInstanceSkip.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("seriesId", seriesId)
        put("instanceEpochDay", instanceEpochDay)
        put("createdAtMillis", createdAtMillis)
    }
}

internal fun backupSnapshotFromJson(json: JSONObject): BackupSnapshot {
    return BackupSnapshot(
        exportedAtMillis = json.optLong("exportedAtMillis", System.currentTimeMillis()),
        pendingQuoteVersion = json.optInt("snapshotVersion", 1),
        groups = json.optJSONArray("groups").toGroups(),
        templates = json.optJSONArray("templates").toTemplates(),
        tasks = json.optJSONArray("tasks").toTasks(),
        reminderChainLogs = json.optJSONArray("reminderChainLogs").toReminderChainLogs(),
        scheduleTemplates = json.optJSONArray("scheduleTemplates").toScheduleTemplates(),
        planningNotes = json.optJSONArray("planningNotes").toPlanningNotes(),
        planningLineMappings = json.optJSONArray("planningLineMappings").toPlanningLineMappings(),
        planningNodes = json.optJSONArray("planningNodes").toPlanningNodes(),
        aiReports = json.optJSONArray("aiReports").toAiReports(),
        todoGroupTags = json.optJSONArray("todoGroupTags").toTodoGroupTags(),
        eventCheckIns = json.optJSONArray("eventCheckIns").toEventCheckIns(),
        recurringInstanceSkips = json.optJSONArray("recurringInstanceSkips").toRecurringInstanceSkips(),
        settings = json.optJSONObject("settings").toSettings()
    )
}

private fun JSONArray?.toGroups(): List<TaskGroup> {
    if (this == null) return emptyList()
    return buildList(length()) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                TaskGroup(
                    id = item.optLong("id", 0L),
                    name = item.optString("name"),
                    colorHex = item.optString("colorHex"),
                    sortOrder = item.optInt("sortOrder", index),
                    isDefault = item.optBoolean("isDefault", false),
                    createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis())
                )
            )
        }
    }
}

private fun JSONArray?.toTemplates(): List<RecurringTaskTemplate> {
    if (this == null) return emptyList()
    return buildList(length()) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                RecurringTaskTemplate(
                    id = item.optLong("id", 0L),
                    seriesId = item.optString("seriesId"),
                    itemType = item.optString("itemType", PlannerItemType.TODO.name),
                    title = item.optString("title"),
                    notes = item.optString("notes"),
                    location = item.optString("location", ""),
                    accentColorHex = item.optStringOrNull("accentColorHex"),
                    countdownEnabled = item.optBoolean("countdownEnabled", false),
                    hiddenFromBoard = item.optBoolean("hiddenFromBoard", false),
                    allDay = item.optBoolean("allDay", false),
                    groupId = item.optLong("groupId", 0L),
                    dueHour = item.optInt("dueHour"),
                    dueMinute = item.optInt("dueMinute"),
                    eventDurationMinutes = item.optIntOrNull("eventDurationMinutes"),
                    reminderOffsetMinutes = item.optIntOrNull("reminderOffsetMinutes"),
                    reminderOffsetsCsv = item.optString("reminderOffsetsCsv", item.optIntOrNull("reminderOffsetMinutes")?.toString().orEmpty()),
                    ringEnabled = item.optBoolean("ringEnabled", true),
                    vibrateEnabled = item.optBoolean("vibrateEnabled", true),
                    alarmMode = item.optBoolean("alarmMode", false),
                    reminderDeliveryMode = item.optString("reminderDeliveryMode", ReminderDeliveryMode.FULLSCREEN.name),
                    recurrenceType = item.optString("recurrenceType"),
                    recurrenceWeekdays = item.optString("recurrenceWeekdays"),
                    recurrenceMonthlyOrdinal = item.optIntOrNull("recurrenceMonthlyOrdinal"),
                    recurrenceMonthlyWeekday = item.optIntOrNull("recurrenceMonthlyWeekday"),
                    recurrenceMonthlyDay = item.optIntOrNull("recurrenceMonthlyDay"),
                    recurrenceYearlyMonth = item.optIntOrNull("recurrenceYearlyMonth"),
                    recurrenceYearlyDay = item.optIntOrNull("recurrenceYearlyDay"),
                    startEpochDay = item.optLong("startEpochDay"),
                    endEpochDay = item.optLong("endEpochDay"),
                    createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis())
                )
            )
        }
    }
}

private fun JSONArray?.toTasks(): List<TodoItem> {
    if (this == null) return emptyList()
    return buildList(length()) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                TodoItem(
                    id = item.optLong("id", 0L),
                    itemType = item.optString("itemType", PlannerItemType.TODO.name),
                    title = item.optString("title"),
                    notes = item.optString("notes"),
                    dueAtMillis = item.optLong("dueAtMillis"),
                    startAtMillis = item.optLongOrNull("startAtMillis"),
                    endAtMillis = item.optLongOrNull("endAtMillis"),
                    allDay = item.optBoolean("allDay", false),
                    location = item.optString("location", ""),
                    accentColorHex = item.optStringOrNull("accentColorHex"),
                    countdownEnabled = item.optBoolean("countdownEnabled", false),
                    hiddenFromBoard = item.optBoolean("hiddenFromBoard", false),
                    checkInEnabled = item.optBoolean("checkInEnabled", false),
                    totalCheckInMinutes = item.optInt("totalCheckInMinutes", 0),
                    reminderAtMillis = item.optLongOrNull("reminderAtMillis"),
                    reminderOffsetsCsv = item.optString("reminderOffsetsCsv", item.optIntOrNull("reminderOffsetMinutes")?.toString().orEmpty()),
                    reminderEnabled = item.optBoolean("reminderEnabled", false),
                    ringEnabled = item.optBoolean("ringEnabled", true),
                    vibrateEnabled = item.optBoolean("vibrateEnabled", true),
                    alarmMode = item.optBoolean("alarmMode", false),
                    voiceEnabled = item.optBoolean("voiceEnabled", false),
                    reminderDeliveryMode = item.optString("reminderDeliveryMode", ReminderDeliveryMode.FULLSCREEN.name),
                    groupId = item.optLong("groupId", 0L),
                    categoryKey = item.optString("categoryKey", TodoCategory.ROUTINE.key),
                    completed = item.optBoolean("completed", false),
                    completedAtMillis = item.optLongOrNull("completedAtMillis"),
                    canceled = item.optBoolean("canceled", false),
                    canceledAtMillis = item.optLongOrNull("canceledAtMillis"),
                    missed = item.optBoolean("missed", false),
                    missedAtMillis = item.optLongOrNull("missedAtMillis"),
                    recurringSeriesId = item.optStringOrNull("recurringSeriesId"),
                    recurrenceType = item.optString("recurrenceType", RecurrenceType.NONE.name),
                    recurrenceWeekdays = item.optString("recurrenceWeekdays"),
                    recurrenceMonthlyOrdinal = item.optIntOrNull("recurrenceMonthlyOrdinal"),
                    recurrenceMonthlyWeekday = item.optIntOrNull("recurrenceMonthlyWeekday"),
                    recurrenceMonthlyDay = item.optIntOrNull("recurrenceMonthlyDay"),
                    recurrenceEndEpochDay = item.optLongOrNull("recurrenceEndEpochDay"),
                    recurrenceAnchorDueAtMillis = item.optLongOrNull("recurrenceAnchorDueAtMillis"),
                    reminderOffsetMinutes = item.optIntOrNull("reminderOffsetMinutes"),
                    createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis())
                )
            )
        }
    }
}

private fun JSONArray?.toReminderChainLogs(): List<ReminderChainLog> {
    if (this == null) return emptyList()
    return buildList(length()) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                ReminderChainLog(
                    id = item.optLong("id", 0L),
                    todoId = item.optLong("todoId", 0L),
                    chainKey = item.optString("chainKey"),
                    source = item.optString("source"),
                    stage = item.optString("stage"),
                    status = item.optString("status"),
                    message = item.optStringOrNull("message"),
                    reminderAtMillis = item.optLongOrNull("reminderAtMillis"),
                    createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis())
                )
            )
        }
    }
}

private fun JSONArray?.toScheduleTemplates(): List<ScheduleTemplate> {
    if (this == null) return emptyList()
    return buildList(length()) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                ScheduleTemplate(
                    id = item.optLong("id", 0L),
                    name = item.optString("name"),
                    templateType = item.optString("templateType"),
                    payloadJson = item.optString("payloadJson"),
                    accentColorHex = item.optStringOrNull("accentColorHex"),
                    createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis()),
                    updatedAtMillis = item.optLong("updatedAtMillis", System.currentTimeMillis())
                )
            )
        }
    }
}

private fun JSONArray?.toPlanningNotes(): List<PlanningNote> {
    if (this == null) return emptyList()
    return buildList(length()) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                PlanningNote(
                    id = item.optLong("id", 0L),
                    title = item.optString("title", "我的规划"),
                    contentMarkdown = item.optString("contentMarkdown"),
                    createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis()),
                    updatedAtMillis = item.optLong("updatedAtMillis", System.currentTimeMillis()),
                    archived = item.optBoolean("archived", false),
                    documentDateEpochDay = item.optLongOrNull("documentDateEpochDay"),
                    hasAnnouncementHint = PlanningAnnouncementParser.mightContainAnnouncement(item.optString("contentMarkdown"))
                )
            )
        }
    }
}

private fun JSONArray?.toPlanningLineMappings(): List<PlanningLineMapping> {
    if (this == null) return emptyList()
    return buildList(length()) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                PlanningLineMapping(
                    id = item.optLong("id", 0L),
                    noteId = item.optLong("noteId", 0L),
                    contentFingerprint = item.optString("contentFingerprint"),
                    originalLineText = item.optString("originalLineText"),
                    currentLineText = item.optString("currentLineText"),
                    todoId = item.optLongOrNull("todoId"),
                    eventId = item.optLongOrNull("eventId"),
                    batchId = item.optString("batchId"),
                    operationType = item.optString("operationType", "IMPORT"),
                    createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis()),
                    lastRefreshedAtMillis = item.optLong("lastRefreshedAtMillis", System.currentTimeMillis()),
                    status = MappingStatus.entries.firstOrNull { it.name == item.optString("status") } ?: MappingStatus.ACTIVE,
                    postponeOffsetMinutes = item.optInt("postponeOffsetMinutes", 0),
                    lastKnownLineNumber = item.optInt("lastKnownLineNumber", 0)
                )
            )
        }
    }
}

private fun JSONArray?.toPlanningNodes(): List<PlanningNode> {
    if (this == null) return emptyList()
    return buildList(length()) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val noteId = item.optLong("noteId", 0L)
            val text = item.optString("text").trim()
            if (noteId <= 0 || text.isBlank()) continue
            add(
                PlanningNode(
                    id = item.optLong("id", 0L),
                    noteId = noteId,
                    parentNodeId = item.optLongOrNull("parentNodeId"),
                    sortOrder = item.optInt("sortOrder", index),
                    text = text,
                    createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis()),
                    updatedAtMillis = item.optLong("updatedAtMillis", System.currentTimeMillis()),
                    startAtMillis = item.optLongOrNull("startAtMillis"),
                    endAtMillis = item.optLongOrNull("endAtMillis"),
                    dueAtMillis = item.optLongOrNull("dueAtMillis"),
                    location = item.optStringOrNull("location"),
                    linkedTodoId = item.optLongOrNull("linkedTodoId"),
                    linkedEndTodoId = item.optLongOrNull("linkedEndTodoId"),
                    isDraft = item.optBoolean("isDraft", false),
                    isNote = item.optBoolean("isNote", false),
                    syncEnabled = item.optBoolean("syncEnabled", true),
                    collapsed = item.optBoolean("collapsed", false),
                    completed = item.optBoolean("completed", false),
                    completedAtMillis = item.optLongOrNull("completedAtMillis")
                )
            )
        }
    }
}

private fun JSONArray?.toAiReports(): List<AiReport> {
    if (this == null) return emptyList()
    return buildList(length()) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                AiReport(
                    id = item.optLong("id", 0L),
                    type = AiReportType.entries.firstOrNull { it.name == item.optString("type") } ?: AiReportType.DAILY,
                    generatedAtMillis = item.optLong("generatedAtMillis", System.currentTimeMillis()),
                    periodStartMillis = item.optLong("periodStartMillis", item.optLong("generatedAtMillis", System.currentTimeMillis())),
                    periodEndMillis = item.optLong("periodEndMillis", item.optLong("generatedAtMillis", System.currentTimeMillis())),
                    content = item.optString("content"),
                    providerName = item.optString("providerName", ""),
                    isLocalFallback = item.optBoolean("isLocalFallback", false)
                )
            )
        }
    }
}

private fun JSONArray?.toTodoGroupTags(): List<TodoGroupTag> {
    if (this == null) return emptyList()
    return buildList(length()) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val todoId = item.optLong("todoId", 0L)
            val groupId = item.optLong("groupId", 0L)
            if (todoId > 0 && groupId > 0) {
                add(TodoGroupTag(todoId = todoId, groupId = groupId))
            }
        }
    }
}

private fun JSONArray?.toEventCheckIns(): List<EventCheckIn> {
    if (this == null) return emptyList()
    return buildList(length()) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val eventId = item.optLong("eventId", 0L)
            val checkInAtMillis = item.optLong("checkInAtMillis", 0L)
            if (eventId <= 0 || checkInAtMillis <= 0) continue
            add(
                EventCheckIn(
                    id = item.optLong("id", 0L),
                    eventId = eventId,
                    checkInAtMillis = checkInAtMillis,
                    checkOutAtMillis = item.optLongOrNull("checkOutAtMillis"),
                    durationMinutes = item.optInt("durationMinutes", 0).coerceAtLeast(0)
                )
            )
        }
    }
}

private fun JSONArray?.toRecurringInstanceSkips(): List<RecurringInstanceSkip> {
    if (this == null) return emptyList()
    return buildList(length()) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val seriesId = item.optString("seriesId").trim()
            val instanceEpochDay = item.optLong("instanceEpochDay", Long.MIN_VALUE)
            if (seriesId.isBlank() || instanceEpochDay == Long.MIN_VALUE) continue
            add(
                RecurringInstanceSkip(
                    id = item.optLong("id", 0L),
                    seriesId = seriesId,
                    instanceEpochDay = instanceEpochDay,
                    createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis())
                )
            )
        }
    }
}

private fun JSONObject?.toSettings(): AppSettings {
    if (this == null) return AppSettings()
    return AppSettings(
        themeMode = ThemeMode.entries.firstOrNull { it.name == optString("themeMode") } ?: ThemeMode.SYSTEM,
        weekStartMode = WeekStartMode.entries.firstOrNull { it.name == optString("weekStartMode") } ?: WeekStartMode.MONDAY,
        defaultSnoozeMinutes = optInt("defaultSnoozeMinutes", 10),
        defaultRingEnabled = optBoolean("defaultRingEnabled", true),
        defaultVibrateEnabled = optBoolean("defaultVibrateEnabled", true),
        defaultVoiceEnabled = optBoolean("defaultVoiceEnabled", false),
        defaultCalendarReminderMode = ReminderDeliveryMode.fromStorage(
            optString("defaultCalendarReminderMode", ReminderDeliveryMode.NOTIFICATION.name)
        ),
        autoCheckOutEventOnEnd = optBoolean("autoCheckOutEventOnEnd", true),
        showEventCheckInStatsOnComplete = optBoolean("showEventCheckInStatsOnComplete", true),
        eventCheckInIdleAutoCheckOutHours = optInt("eventCheckInIdleAutoCheckOutHours", 2).coerceIn(0, 8),
        reminderToneUri = optStringOrNull("reminderToneUri"),
        reminderToneName = optStringOrNull("reminderToneName"),
        reminderAudioChannel = ReminderAudioChannel.fromStorage(optString("reminderAudioChannel", ReminderAudioChannel.ALARM.name)),
        reminderInternalVolumePercent = optInt("reminderInternalVolumePercent", 80),
        reminderBoostSystemVolume = optBoolean("reminderBoostSystemVolume", false),
        reminderBoostVolumePercent = optInt("reminderBoostVolumePercent", 50),
        workQuietModeEnabled = optBoolean("workQuietModeEnabled", false),
        quoteIndex = optInt("quoteIndex", 0),
        backupDirectoryUri = optStringOrNull("backupDirectoryUri"),
        autoBackupEnabled = optBoolean("autoBackupEnabled", false),
        desktopSyncEnabled = false,
        desktopSyncToken = "",
        desktopSyncWifiKeepAlive = optBoolean("desktopSyncWifiKeepAlive", true),
        lastOpenedPlanningNoteId = optLongOrNull("lastOpenedPlanningNoteId")?.takeIf { it > 0 },
        planningOutlineHintVisible = optBoolean("planningOutlineHintVisible", true),
        planningEventEndTodoEnabled = optBoolean("planningEventEndTodoEnabled", false),
        dailyBriefEnabled = optBoolean("dailyBriefEnabled", true),
        dailyBriefHour = optInt("dailyBriefHour", 8).coerceIn(0, 23),
        dailyBriefMinute = optInt("dailyBriefMinute", 0).coerceIn(0, 59),
        ongoingEventNotificationEnabled = optBoolean("ongoingEventNotificationEnabled", true),
        boardCountdownCollapsed = optBoolean("boardCountdownCollapsed", false),
        boardTodayTodosCollapsed = optBoolean("boardTodayTodosCollapsed", false),
        boardTodayEventsCollapsed = optBoolean("boardTodayEventsCollapsed", false),
        boardTomorrowEventsCollapsed = optBoolean("boardTomorrowEventsCollapsed", false),
        boardAnnouncementCollapsed = optBoolean("boardAnnouncementCollapsed", false),
        planningAiEnabled = optBoolean("planningAiEnabled", false),
        planningAiProviderName = optString("planningAiProviderName", ""),
        planningAiBaseUrl = optString("planningAiBaseUrl", ""),
        planningAiApiKey = "",
        planningAiModel = optString("planningAiModel", ""),
        planningAiProviders = optJSONArray("planningAiProviders")
            ?.let { planningAiProvidersFromJson(it.toString()) }
            ?: emptyList(),
        hasSeenOnboarding = optBoolean("hasSeenOnboarding", false),
        dailyReportEnabled = optBoolean("dailyReportEnabled", false),
        dailyReportHour = optInt("dailyReportHour", 22),
        dailyReportMinute = optInt("dailyReportMinute", 0),
        weeklyReportEnabled = optBoolean("weeklyReportEnabled", false),
        weeklyReportHour = optInt("weeklyReportHour", 22),
        weeklyReportMinute = optInt("weeklyReportMinute", 0),
        aiReportRetention = AiReportRetention.fromStorage(optString("aiReportRetention", AiReportRetention.DAYS_90.name)),
        legacyAiReportMigrated = optBoolean("legacyAiReportMigrated", false)
    )
}

private fun JSONObject.optLongOrNull(key: String): Long? {
    return if (isNull(key)) null else optLong(key)
}

private fun JSONObject.optIntOrNull(key: String): Int? {
    return if (isNull(key)) null else optInt(key)
}

private fun JSONObject.optStringOrNull(key: String): String? {
    return if (isNull(key)) null else optString(key)
}
