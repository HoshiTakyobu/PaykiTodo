package com.example.todoalarm.sync

import android.content.Context
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.alarm.ActiveReminderStore
import com.example.todoalarm.alarm.ReminderDispatchTracker
import com.example.todoalarm.alarm.ReminderForegroundService
import com.example.todoalarm.data.AppSettingsStore
import com.example.todoalarm.data.CalendarEventDraft
import com.example.todoalarm.data.RecurrenceConfig
import com.example.todoalarm.data.RecurrenceScope
import com.example.todoalarm.data.RecurrenceType
import com.example.todoalarm.data.TodoDraft
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.reminderTriggerTimesMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Collections

class DesktopSyncCoordinator(
    private val context: Context,
    private val app: TodoApplication,
    private val settingsStore: AppSettingsStore
) {
    private var server: DesktopSyncServer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val port = 18765

    fun ensureRunning() {
        if (!settingsStore.currentSettings().desktopSyncEnabled) {
            stop()
            return
        }
        if (server != null) return
        server = DesktopSyncServer(port = port) { method, path, body, headers ->
            handleRequest(method, path, body, headers)
        }.also { it.start() }
    }

    fun stop() {
        server?.stop()
        server = null
    }

    fun shutdown() {
        stop()
        scope.cancel()
    }

    fun status(): DesktopSyncStatus {
        return settingsStore.currentSettings().toDesktopSyncStatus(
            running = server != null,
            port = port,
            ipAddresses = if (server != null) currentIpv4Addresses() else emptyList()
        )
    }

    private fun handleRequest(
        method: String,
        path: String,
        body: String,
        headers: Map<String, String>
    ): DesktopSyncServer.Response {
        if (path == "/" || path == "/index.html") {
            return DesktopSyncServer.Response.html(DesktopSyncWebAssets.indexHtml(context))
        }
        if (path == "/app.js") {
            return DesktopSyncServer.Response.js(DesktopSyncWebAssets.appJs(context))
        }
        if (path == "/app.css") {
            return DesktopSyncServer.Response.css(DesktopSyncWebAssets.appCss(context))
        }

        if (!authorize(headers)) {
            return DesktopSyncServer.Response.json(JSONObject().put("error", "未授权，请填写手机端显示的访问密钥。"), 401)
        }

        return runCatching {
            when {
                method == "GET" && path == "/api/status" -> DesktopSyncServer.Response.json(status().toJson())
                method == "GET" && path == "/api/snapshot" -> DesktopSyncServer.Response.json(buildSnapshot().toJson(buildGroupsMap()))
                method == "POST" && path == "/api/todos" -> DesktopSyncServer.Response.json(createTodo(JSONObject(body)))
                method == "PUT" && path.matches(Regex("/api/todos/\\d+")) -> DesktopSyncServer.Response.json(updateTodo(path, JSONObject(body)))
                method == "POST" && path == "/api/events" -> DesktopSyncServer.Response.json(createEvent(JSONObject(body)))
                method == "PUT" && path.matches(Regex("/api/events/\\d+")) -> DesktopSyncServer.Response.json(updateEvent(path, JSONObject(body)))
                method == "POST" && path.matches(Regex("/api/items/\\d+/complete")) -> DesktopSyncServer.Response.json(markCompleted(path))
                method == "POST" && path.matches(Regex("/api/items/\\d+/cancel")) -> DesktopSyncServer.Response.json(cancelItem(path))
                method == "DELETE" && path.matches(Regex("/api/items/\\d+")) -> DesktopSyncServer.Response.json(deleteItem(path))
                else -> DesktopSyncServer.Response.json(JSONObject().put("error", "未找到接口"), 404)
            }
        }.getOrElse { throwable ->
            DesktopSyncServer.Response.json(
                JSONObject()
                    .put("error", throwable.message ?: throwable.javaClass.simpleName)
                    .put("type", throwable.javaClass.simpleName),
                500
            )
        }
    }

    private fun authorize(headers: Map<String, String>): Boolean {
        val expected = settingsStore.currentSettings().desktopSyncToken
        val provided = headers.entries.firstOrNull { it.key.equals("X-Payki-Token", ignoreCase = true) }?.value
            ?: headers.entries.firstOrNull { it.key.equals("Authorization", ignoreCase = true) }?.value?.removePrefix("Bearer ")
        return expected.isNotBlank() && provided == expected
    }

    private fun buildSnapshot(): DesktopSyncSnapshot {
        val groups = runBlocking { app.repository.getAllGroups().ifEmpty { app.repository.ensureDefaultGroups() } }
        val items = runBlocking { app.repository.getAllTodos() }
        return DesktopSyncSnapshot(
            generatedAtMillis = System.currentTimeMillis(),
            groups = groups,
            todos = items.filter { it.isTodo }.sortedBy { it.dueAtMillis },
            events = items.filter { it.isEvent }.sortedBy { it.startAtMillis ?: it.dueAtMillis }
        )
    }

    private fun buildGroupsMap(): Map<Long, com.example.todoalarm.data.TaskGroup> {
        return runBlocking { app.repository.getAllGroups().ifEmpty { app.repository.ensureDefaultGroups() } }.associateBy { it.id }
    }

    private fun createTodo(json: JSONObject): JSONObject {
        val groupId = resolveGroupId(json)
        val dueAt = json.optStringOrNull("dueAt")?.let(LocalDateTime::parse)
        val reminderAt = json.optStringOrNull("reminderAt")?.let(LocalDateTime::parse)
        val reminderOffsets = json.optJSONArray("reminderOffsetsMinutes")?.toIntList().orEmpty()
        val recurrence = parseRecurrence(json.optJSONObject("recurrence"), dueAt?.toLocalDate())
        val draft = sanitizeTodoDraft(
            title = json.optString("title").trim(),
            notes = json.optString("notes").trim(),
            dueAt = dueAt,
            reminderAt = reminderAt,
            groupId = groupId,
            ringEnabled = json.optBoolean("ringEnabled", true),
            vibrateEnabled = json.optBoolean("vibrateEnabled", true),
            recurrence = recurrence,
            reminderOffsetsMinutes = reminderOffsets
        )
        require(draft.title.isNotBlank()) { "标题不能为空" }
        val created = runBlocking { app.repository.createFromDraft(draft) }
        created.forEach(::scheduleReminderOrDisable)
        autoBackupIfNeeded()
        return JSONObject().put("created", created.size)
    }

    private fun updateTodo(path: String, json: JSONObject): JSONObject {
        val id = path.substringAfter("/api/todos/").toLong()
        val original = runBlocking { app.repository.getTodo(id) } ?: return JSONObject().put("ok", false)
        require(original.isTodo) { "仅支持更新待办" }

        val groupId = resolveGroupId(json)
        val dueAt = json.optStringOrNull("dueAt")?.let(LocalDateTime::parse)
        val reminderAt = json.optStringOrNull("reminderAt")?.let(LocalDateTime::parse)
        val reminderOffsets = json.optJSONArray("reminderOffsetsMinutes")?.toIntList().orEmpty()
        val recurrence = parseRecurrence(json.optJSONObject("recurrence"), dueAt?.toLocalDate())
        val draft = sanitizeTodoDraft(
            title = json.optString("title").trim(),
            notes = json.optString("notes").trim(),
            dueAt = dueAt,
            reminderAt = reminderAt,
            groupId = groupId,
            ringEnabled = json.optBoolean("ringEnabled", original.ringEnabled),
            vibrateEnabled = json.optBoolean("vibrateEnabled", original.vibrateEnabled),
            recurrence = recurrence,
            reminderOffsetsMinutes = reminderOffsets
        )
        require(draft.title.isNotBlank()) { "标题不能为空" }
        validateTodoDraft(draft = draft, original = original)?.let { error(it) }
        val affected = runBlocking { app.repository.getActiveItemsForScope(original, RecurrenceScope.CURRENT) }
        clearReminderArtifacts(affected.ifEmpty { listOf(original) })
        val updated = runBlocking { app.repository.updateFromDraft(original, draft, RecurrenceScope.CURRENT) }
        updated.forEach(::scheduleReminderOrDisable)
        autoBackupIfNeeded()
        return JSONObject().put("ok", updated.isNotEmpty())
    }

    private fun createEvent(json: JSONObject): JSONObject {
        val groupId = resolveGroupId(json)
        val startAt = LocalDateTime.parse(json.getString("startAt"))
        val endAt = LocalDateTime.parse(json.getString("endAt"))
        val reminderOffsets = json.optJSONArray("reminderOffsetsMinutes")?.toIntList().orEmpty()
        val recurrence = parseRecurrence(json.optJSONObject("recurrence"), startAt.toLocalDate())
        val draft = sanitizeEventDraft(
            title = json.optString("title").trim(),
            notes = json.optString("notes").trim(),
            location = json.optString("location").trim(),
            startAt = startAt,
            endAt = endAt,
            allDay = json.optBoolean("allDay", false),
            accentColorHex = json.optString("accentColorHex", "#4E87E1"),
            reminderOffsetsMinutes = reminderOffsets,
            ringEnabled = json.optBoolean("ringEnabled", true),
            vibrateEnabled = json.optBoolean("vibrateEnabled", true),
            reminderDeliveryMode = com.example.todoalarm.data.ReminderDeliveryMode.fromStorage(json.optString("reminderDeliveryMode")),
            recurrence = recurrence,
            groupId = groupId
        )
        require(draft.title.isNotBlank()) { "日程标题不能为空" }
        val created = runBlocking { app.repository.createCalendarEventFromDraft(draft) }
        created.forEach(::scheduleReminderOrDisable)
        autoBackupIfNeeded()
        return JSONObject().put("created", created.size)
    }

    private fun updateEvent(path: String, json: JSONObject): JSONObject {
        val id = path.substringAfter("/api/events/").toLong()
        val original = runBlocking { app.repository.getTodo(id) } ?: return JSONObject().put("ok", false)
        require(original.isEvent) { "仅支持更新日程" }

        val groupId = resolveGroupId(json)
        val startAt = LocalDateTime.parse(json.getString("startAt"))
        val endAt = LocalDateTime.parse(json.getString("endAt"))
        val reminderOffsets = json.optJSONArray("reminderOffsetsMinutes")?.toIntList().orEmpty()
        val recurrence = parseRecurrence(json.optJSONObject("recurrence"), startAt.toLocalDate())
        val draft = sanitizeEventDraft(
            title = json.optString("title").trim(),
            notes = json.optString("notes").trim(),
            location = json.optString("location").trim(),
            startAt = startAt,
            endAt = endAt,
            allDay = json.optBoolean("allDay", false),
            accentColorHex = json.optString("accentColorHex", original.accentColorHex?.ifBlank { "#4E87E1" } ?: "#4E87E1"),
            reminderOffsetsMinutes = reminderOffsets,
            ringEnabled = json.optBoolean("ringEnabled", true),
            vibrateEnabled = json.optBoolean("vibrateEnabled", true),
            reminderDeliveryMode = com.example.todoalarm.data.ReminderDeliveryMode.fromStorage(json.optString("reminderDeliveryMode")),
            recurrence = recurrence,
            groupId = groupId
        )
        require(draft.title.isNotBlank()) { "日程标题不能为空" }
        val updated = runBlocking { app.repository.updateCalendarEventFromDraft(original, draft, RecurrenceScope.CURRENT) }
        updated.forEach(::scheduleReminderOrDisable)
        autoBackupIfNeeded()
        return JSONObject().put("ok", updated.isNotEmpty())
    }
    private fun markCompleted(path: String): JSONObject {
        val id = path.substringAfter("/api/items/").substringBefore('/').toLong()
        val updated = runBlocking { app.repository.setCompleted(id, true) }
        updated?.let { clearReminderArtifacts(listOf(it)) }
        autoBackupIfNeeded()
        return JSONObject().put("ok", updated != null)
    }

    private fun cancelItem(path: String): JSONObject {
        val id = path.substringAfter("/api/items/").substringBefore('/').toLong()
        val item = runBlocking { app.repository.getTodo(id) } ?: return JSONObject().put("ok", false)
        val canceled = if (item.isEvent) {
            runBlocking { app.repository.deleteCalendarEvent(item, RecurrenceScope.CURRENT) }
        } else {
            runBlocking { app.repository.cancelTodo(item, RecurrenceScope.CURRENT) }
        }
        clearReminderArtifacts(canceled.ifEmpty { listOf(item) })
        autoBackupIfNeeded()
        return JSONObject().put("ok", true)
    }

    private fun deleteItem(path: String): JSONObject {
        val id = path.substringAfter("/api/items/").toLong()
        val item = runBlocking { app.repository.getTodo(id) } ?: return JSONObject().put("ok", false)
        if (item.isEvent) {
            runBlocking { app.repository.deleteCalendarEvent(item, RecurrenceScope.CURRENT) }
        } else {
            runBlocking { app.repository.deleteTodo(id) }
        }
        clearReminderArtifacts(listOf(item))
        autoBackupIfNeeded()
        return JSONObject().put("ok", true)
    }

    private fun resolveGroupId(json: JSONObject): Long {
        val groups = runBlocking { app.repository.getAllGroups().ifEmpty { app.repository.ensureDefaultGroups() } }
        val groupId = json.optLong("groupId", 0L)
        if (groupId > 0 && groups.any { it.id == groupId }) return groupId
        val requestedName = json.optString("groupName").trim()
        if (requestedName.isNotBlank()) {
            val existing = groups.firstOrNull { it.name == requestedName }
            if (existing != null) return existing.id
            return runBlocking { app.repository.createGroup(requestedName, json.optString("groupColorHex", "#4E87E1")).id }
        }
        return groups.firstOrNull { it.name == "例行" }?.id ?: groups.firstOrNull()?.id ?: 0L
    }

    private fun parseRecurrence(json: JSONObject?, anchorDate: LocalDate?): RecurrenceConfig {
        if (json == null || !json.optBoolean("enabled", false)) return RecurrenceConfig()
        val type = RecurrenceType.fromStorage(json.optString("type"))
        val weekdays = json.optJSONArray("weeklyDays")?.toWeekdays().orEmpty()
        val endDate = json.optStringOrNull("endDate")?.let(LocalDate::parse)
        return RecurrenceConfig(
            enabled = type != RecurrenceType.NONE,
            type = type,
            weeklyDays = if (weekdays.isEmpty() && anchorDate != null && type == RecurrenceType.WEEKLY) {
                setOf(anchorDate.dayOfWeek)
            } else {
                weekdays
            },
            endDate = endDate
        )
    }

    private fun sanitizeTodoDraft(
        title: String,
        notes: String,
        dueAt: LocalDateTime?,
        reminderAt: LocalDateTime?,
        groupId: Long,
        ringEnabled: Boolean,
        vibrateEnabled: Boolean,
        recurrence: RecurrenceConfig,
        reminderOffsetsMinutes: List<Int>
    ): TodoDraft {
        val now = LocalDateTime.now()
        val reminder = reminderAt?.takeIf { candidate ->
            dueAt != null && dueAt.isAfter(now) && candidate.isAfter(now) && !candidate.isAfter(dueAt)
        }
        return TodoDraft(
            title = title,
            notes = notes,
            dueAt = dueAt,
            reminderAt = reminder,
            groupId = groupId,
            ringEnabled = ringEnabled,
            vibrateEnabled = vibrateEnabled,
            recurrence = recurrence,
            reminderOffsetsMinutes = reminderOffsetsMinutes
        )
    }

    private fun validateTodoDraft(draft: TodoDraft, original: TodoItem?): String? {
        if (draft.title.isBlank()) return "标题不能为空"
        val now = System.currentTimeMillis()
        val isHistory = original?.isHistory == true
        val dueAtMillis = draft.dueAt?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        if (!isHistory && original == null && dueAtMillis != null && dueAtMillis <= now) {
            return "DDL 必须晚于当前时间"
        }
        if (draft.dueAt == null && draft.normalizedReminderOffsetsMinutes.isNotEmpty()) {
            return "未设置 DDL 的任务不能启用提醒"
        }
        val triggerTimes = if (draft.dueAt == null) {
            emptyList()
        } else {
            val probe = TodoItem(
                id = original?.id ?: 0,
                title = draft.title,
                notes = draft.notes,
                dueAtMillis = dueAtMillis ?: 0L,
                reminderAtMillis = draft.reminderAt?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                reminderOffsetsCsv = draft.normalizedReminderOffsetsMinutes.joinToString(","),
                reminderEnabled = draft.normalizedReminderOffsetsMinutes.isNotEmpty(),
                ringEnabled = draft.ringEnabled,
                vibrateEnabled = draft.vibrateEnabled,
                groupId = draft.groupId
            )
            probe.reminderTriggerTimesMillis()
        }
        if (!isHistory && original == null && triggerTimes.any { it <= now }) {
            return "提醒时间必须晚于当前时间"
        }
        if (triggerTimes.any { it > (dueAtMillis ?: Long.MAX_VALUE) }) {
            return "提醒时间不能晚于 DDL"
        }
        if (draft.recurrence.enabled) {
            val dueAt = draft.dueAt ?: return "循环任务必须设置 DDL"
            if (draft.recurrence.type == RecurrenceType.NONE) return "请选择循环规则"
            val endDate = draft.recurrence.endDate ?: return "请设置循环截止日期"
            if (endDate.isBefore(dueAt.toLocalDate())) return "循环截止日期不能早于首次任务日期"
            if (draft.recurrence.type == RecurrenceType.WEEKLY && draft.recurrence.weeklyDays.isEmpty()) return "每周循环至少选择一天"
        }
        if (original?.isRecurring == true && draft.dueAt == null) {
            return "循环任务必须保留 DDL"
        }
        return null
    }

    private fun sanitizeEventDraft(
        title: String,
        notes: String,
        location: String,
        startAt: LocalDateTime,
        endAt: LocalDateTime,
        allDay: Boolean,
        accentColorHex: String,
        reminderOffsetsMinutes: List<Int>,
        ringEnabled: Boolean,
        vibrateEnabled: Boolean,
        reminderDeliveryMode: com.example.todoalarm.data.ReminderDeliveryMode,
        recurrence: RecurrenceConfig,
        groupId: Long
    ): CalendarEventDraft {
        val baseDraft = CalendarEventDraft(
            title = title,
            notes = notes,
            location = location,
            startAt = startAt,
            endAt = endAt,
            allDay = allDay,
            accentColorHex = accentColorHex,
            reminderMinutesBefore = reminderOffsetsMinutes.firstOrNull(),
            reminderOffsetsMinutes = reminderOffsetsMinutes,
            ringEnabled = ringEnabled,
            vibrateEnabled = vibrateEnabled,
            reminderDeliveryMode = reminderDeliveryMode,
            recurrence = recurrence,
            groupId = groupId
        )
        val now = System.currentTimeMillis()
        val validOffsets = baseDraft.normalizedReminderOffsetsMinutes.filter { minutes ->
            baseDraft.reminderAnchorAt.minusMinutes(minutes.toLong()).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() > now
        }
        return baseDraft.copy(
            reminderMinutesBefore = validOffsets.firstOrNull(),
            reminderOffsetsMinutes = validOffsets
        )
    }

    private fun scheduleReminderOrDisable(item: TodoItem) {
        if (item.isTodo && !item.hasDueDate) {
            if (item.reminderEnabled || item.reminderAtMillis != null) {
                ReminderDispatchTracker.clear(context, item.id)
                runBlocking { app.repository.updateTodo(item.copy(reminderEnabled = false, reminderAtMillis = null)) }
            }
            return
        }
        if (!item.reminderEnabled) return
        ReminderDispatchTracker.clear(context, item.id)
        val message = app.alarmScheduler.schedule(item)
        if (message != null) {
            runBlocking { app.repository.updateTodo(item.copy(reminderEnabled = false)) }
        }
    }

    private fun clearReminderArtifacts(items: List<TodoItem>) {
        items.forEach { item ->
            app.alarmScheduler.cancel(item.id)
            app.reminderNotifier.cancel(item.id)
            ReminderDispatchTracker.clear(context, item.id)
            ActiveReminderStore.clearIfMatches(context, item.id)
        }
        context.stopService(android.content.Intent(context, ReminderForegroundService::class.java))
    }

    private fun autoBackupIfNeeded() {
        val settings = settingsStore.currentSettings()
        if (!settings.autoBackupEnabled) return
        val directoryUri = settings.backupDirectoryUri ?: return
        scope.launch {
            runCatching {
                val snapshot = app.repository.exportSnapshot(settings)
                app.backupManager.autoBackupToDirectory(directoryUri, snapshot)
            }
        }
    }

    private fun currentIpv4Addresses(): List<String> {
        return runCatching {
            Collections.list(NetworkInterface.getNetworkInterfaces())
                .filter { it.isUp && !it.isLoopback }
                .flatMap { network -> Collections.list(network.inetAddresses) }
                .filterIsInstance<Inet4Address>()
                .filterNot { it.isLoopbackAddress }
                .map { it.hostAddress ?: "" }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }.getOrDefault(emptyList())
    }
}

private fun JSONObject.optStringOrNull(key: String): String? {
    if (isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
}

private fun JSONArray.toIntList(): List<Int> {
    return buildList(length()) {
        for (index in 0 until length()) {
            add(optInt(index))
        }
    }
}

private fun JSONArray.toWeekdays(): Set<DayOfWeek> {
    return buildSet(length()) {
        for (index in 0 until length()) {
            val value = optInt(index)
            runCatching { DayOfWeek.of(value) }.getOrNull()?.let(::add)
        }
    }
}
