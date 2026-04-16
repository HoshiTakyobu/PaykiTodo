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

private fun BackupSnapshot.toJson(): JSONObject {
    return JSONObject().apply {
        put("exportedAtMillis", exportedAtMillis)
        put("settings", settings.toJson())
        put("groups", JSONArray(groups.map { it.toJson() }))
        put("templates", JSONArray(templates.map { it.toJson() }))
        put("tasks", JSONArray(tasks.map { it.toJson() }))
    }
}

private fun AppSettings.toJson(): JSONObject {
    return JSONObject().apply {
        put("themeMode", themeMode.name)
        put("defaultSnoozeMinutes", defaultSnoozeMinutes)
        put("defaultRingEnabled", defaultRingEnabled)
        put("defaultVibrateEnabled", defaultVibrateEnabled)
        put("defaultVoiceEnabled", defaultVoiceEnabled)
        put("quoteIndex", quoteIndex)
        put("backupDirectoryUri", backupDirectoryUri)
        put("autoBackupEnabled", autoBackupEnabled)
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
        put("allDay", allDay)
        put("groupId", groupId)
        put("dueHour", dueHour)
        put("dueMinute", dueMinute)
        put("eventDurationMinutes", eventDurationMinutes)
        put("reminderOffsetMinutes", reminderOffsetMinutes)
        put("ringEnabled", ringEnabled)
        put("vibrateEnabled", vibrateEnabled)
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
        put("reminderAtMillis", reminderAtMillis)
        put("reminderEnabled", reminderEnabled)
        put("ringEnabled", ringEnabled)
        put("vibrateEnabled", vibrateEnabled)
        put("voiceEnabled", voiceEnabled)
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

private fun backupSnapshotFromJson(json: JSONObject): BackupSnapshot {
    return BackupSnapshot(
        exportedAtMillis = json.optLong("exportedAtMillis", System.currentTimeMillis()),
        groups = json.optJSONArray("groups").toGroups(),
        templates = json.optJSONArray("templates").toTemplates(),
        tasks = json.optJSONArray("tasks").toTasks(),
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
                    allDay = item.optBoolean("allDay", false),
                    groupId = item.optLong("groupId", 0L),
                    dueHour = item.optInt("dueHour"),
                    dueMinute = item.optInt("dueMinute"),
                    eventDurationMinutes = item.optIntOrNull("eventDurationMinutes"),
                    reminderOffsetMinutes = item.optIntOrNull("reminderOffsetMinutes"),
                    ringEnabled = item.optBoolean("ringEnabled", true),
                    vibrateEnabled = item.optBoolean("vibrateEnabled", true),
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
                    reminderAtMillis = item.optLongOrNull("reminderAtMillis"),
                    reminderEnabled = item.optBoolean("reminderEnabled", false),
                    ringEnabled = item.optBoolean("ringEnabled", true),
                    vibrateEnabled = item.optBoolean("vibrateEnabled", true),
                    voiceEnabled = item.optBoolean("voiceEnabled", false),
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

private fun JSONObject?.toSettings(): AppSettings {
    if (this == null) return AppSettings()
    return AppSettings(
        themeMode = ThemeMode.entries.firstOrNull { it.name == optString("themeMode") } ?: ThemeMode.SYSTEM,
        defaultSnoozeMinutes = optInt("defaultSnoozeMinutes", 10),
        defaultRingEnabled = optBoolean("defaultRingEnabled", true),
        defaultVibrateEnabled = optBoolean("defaultVibrateEnabled", true),
        defaultVoiceEnabled = optBoolean("defaultVoiceEnabled", false),
        quoteIndex = optInt("quoteIndex", 0),
        backupDirectoryUri = optStringOrNull("backupDirectoryUri"),
        autoBackupEnabled = optBoolean("autoBackupEnabled", false)
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
