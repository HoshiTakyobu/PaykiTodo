package com.example.todoalarm.sync

import android.content.Context
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.alarm.ActiveReminderStore
import com.example.todoalarm.alarm.ReminderDispatchTracker
import com.example.todoalarm.alarm.ReminderForegroundService
import com.example.todoalarm.data.AppSettingsStore
import com.example.todoalarm.data.CalendarEventDraft
import com.example.todoalarm.data.DEFAULT_PLANNING_REMINDER_MINUTES
import com.example.todoalarm.data.DailyBoardSnapshotBuilder
import com.example.todoalarm.data.EventCheckIn
import com.example.todoalarm.data.EventCheckInCompletionSummary
import com.example.todoalarm.data.PlanningImportCandidate
import com.example.todoalarm.data.PlanningLineMapping
import com.example.todoalarm.data.PlanningLineMatcher
import com.example.todoalarm.data.MappingStatus
import com.example.todoalarm.data.PlanningNode
import com.example.todoalarm.data.PlanningNodeChangeResult
import com.example.todoalarm.data.PlanningNodeDraft
import com.example.todoalarm.data.PlanningNodeEdit
import com.example.todoalarm.data.PlanningNote
import com.example.todoalarm.data.PlanningOperationResult
import com.example.todoalarm.data.PlanningParsedCandidate
import com.example.todoalarm.data.PlanningParsedType
import com.example.todoalarm.data.PlanningPostponeScope
import com.example.todoalarm.data.PlanningRefreshScope
import com.example.todoalarm.data.PlanningRecognitionService
import com.example.todoalarm.data.PlanningMarkdownParser
import com.example.todoalarm.data.PlanningAnnouncementParser
import com.example.todoalarm.data.toPlanningImportCandidate
import com.example.todoalarm.data.RecurrenceConfig
import com.example.todoalarm.data.RecurrenceScope
import com.example.todoalarm.data.RecurrenceType
import com.example.todoalarm.data.TodoDraft
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.parseReminderTextInput
import com.example.todoalarm.data.reminderTriggerTimesMillis
import com.example.todoalarm.data.storageStringToWeekdays
import com.example.todoalarm.sync.toDesktopSyncBoard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.Locale
import java.util.UUID

private const val DESKTOP_TODO_PAGE_LIMIT_DEFAULT = 80
private const val DESKTOP_TODO_PAGE_LIMIT_MAX = 200
private const val DESKTOP_TODO_QUERY_MAX_LENGTH = 80

class DesktopSyncCoordinator(
    private val context: Context,
    private val app: TodoApplication,
    private val settingsStore: AppSettingsStore
) {
    private var server: DesktopSyncServer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val port = 18765
    @Volatile
    private var lastAuthorizedClientAtMillis: Long = 0L
    @Volatile
    private var trackingStartedAtMillis: Long = 0L

    fun ensureRunning() {
        if (!settingsStore.currentSettings().desktopSyncEnabled) {
            stop()
            return
        }
        if (server != null) return
        if (trackingStartedAtMillis <= 0L) {
            trackingStartedAtMillis = System.currentTimeMillis()
        }
        server = DesktopSyncServer(port = port) { method, path, body, headers ->
            handleRequest(method, path, body, headers)
        }.also { it.start() }
    }

    fun stop() {
        server?.stop()
        server = null
        trackingStartedAtMillis = 0L
    }

    fun resetClientTracking() {
        lastAuthorizedClientAtMillis = 0L
        trackingStartedAtMillis = System.currentTimeMillis()
    }

    fun lastAuthorizedClientAtMillis(): Long = lastAuthorizedClientAtMillis

    fun shutdown() {
        stop()
        scope.cancel()
    }

    fun status(): DesktopSyncStatus {
        val settings = settingsStore.currentSettings()
        if (settings.desktopSyncEnabled && server == null) {
            DesktopSyncService.start(context.applicationContext)
        }
        val enabledAndRunning = settings.desktopSyncEnabled && server != null
        val now = System.currentTimeMillis()
        val lastAuthorizedAt = lastAuthorizedClientAtMillis
        val connected = enabledAndRunning &&
            lastAuthorizedAt > 0L &&
            now - lastAuthorizedAt < DesktopSyncService.NO_CLIENT_AUTO_STOP_MILLIS
        val secondsUntilAutoStop = if (enabledAndRunning) {
            val referenceMillis = lastAuthorizedAt.takeIf { it > 0L } ?: trackingStartedAtMillis
            ((referenceMillis + DesktopSyncService.NO_CLIENT_AUTO_STOP_MILLIS - now).coerceAtLeast(0L) + 999L) / 1000L
        } else {
            null
        }
        return settings.toDesktopSyncStatus(
            running = enabledAndRunning,
            port = port,
            ipAddresses = if (enabledAndRunning) currentIpv4Addresses() else emptyList(),
            connected = connected,
            lastAuthorizedAtMillis = lastAuthorizedAt,
            secondsUntilAutoStop = secondsUntilAutoStop
        )
    }

    private suspend fun handleRequest(
        method: String,
        path: String,
        body: String,
        headers: Map<String, String>
    ): DesktopSyncServer.Response {
        val routePath = path.substringBefore('?')
        if (routePath == "/" || routePath == "/index.html") {
            return DesktopSyncServer.Response.html(DesktopSyncWebAssets.indexHtml(context))
        }
        if (routePath == "/app.js") {
            return DesktopSyncServer.Response.js(DesktopSyncWebAssets.appJs(context))
        }
        if (routePath == "/app.css") {
            return DesktopSyncServer.Response.css(DesktopSyncWebAssets.appCss(context))
        }

        if (!authorize(headers)) {
            return DesktopSyncServer.Response.json(JSONObject().put("error", "未授权，请填写手机端显示的访问密钥。"), 401)
        }
        lastAuthorizedClientAtMillis = System.currentTimeMillis()

        return runCatching {
            when {
                method == "GET" && path == "/api/status" -> DesktopSyncServer.Response.json(status().toJson())
                method == "GET" && routePath == "/api/snapshot" -> {
                    val snapshot = buildSnapshot(boardOnly = queryParam(path, "scope") == "board")
                    DesktopSyncServer.Response.json(
                        snapshot.toJson(
                            groupsById = snapshot.groups.associateBy { it.id },
                            todoGroupIdsByTodoId = todoGroupIdsByTodoId()
                        )
                    )
                }
                method == "GET" && routePath == "/api/todos" -> DesktopSyncServer.Response.json(desktopTodos(path))
                method == "GET" && routePath == "/api/events" -> DesktopSyncServer.Response.json(desktopEvents(path))
                method == "POST" && path == "/api/todos" -> DesktopSyncServer.Response.json(createTodo(JSONObject(body)))
                method == "POST" && path == "/api/events" -> DesktopSyncServer.Response.json(createEvent(JSONObject(body)))
                method == "PUT" && routePath.matches(Regex("/api/todos/\\d+")) -> DesktopSyncServer.Response.json(updateTodo(routePath, JSONObject(body)))
                method == "PUT" && routePath.matches(Regex("/api/events/\\d+")) -> DesktopSyncServer.Response.json(updateEvent(routePath, JSONObject(body)))
                method == "GET" && routePath.matches(Regex("/api/events/\\d+/check-ins")) -> DesktopSyncServer.Response.json(eventCheckIns(routePath))
                method == "POST" && routePath.matches(Regex("/api/events/\\d+/check-in")) -> DesktopSyncServer.Response.json(checkInEvent(routePath))
                method == "POST" && routePath.matches(Regex("/api/events/\\d+/check-out")) -> DesktopSyncServer.Response.json(checkOutEvent(routePath))
                method == "GET" && path == "/api/planning/notes" -> DesktopSyncServer.Response.json(planningNotes())
                method == "POST" && path == "/api/planning/notes" -> DesktopSyncServer.Response.json(createPlanningNote(JSONObject(body)))
                method == "PUT" && routePath.matches(Regex("/api/planning/notes/\\d+")) -> DesktopSyncServer.Response.json(updatePlanningNote(routePath, JSONObject(body)))
                method == "DELETE" && routePath.matches(Regex("/api/planning/notes/\\d+")) -> DesktopSyncServer.Response.json(deletePlanningNote(routePath))
                method == "GET" && routePath == "/api/planning/nodes" -> DesktopSyncServer.Response.json(planningNodes(path))
                method == "POST" && path == "/api/planning/nodes/create" -> DesktopSyncServer.Response.json(createPlanningNode(JSONObject(body)))
                method == "POST" && path == "/api/planning/nodes/update" -> DesktopSyncServer.Response.json(updatePlanningNode(JSONObject(body)))
                method == "POST" && path == "/api/planning/nodes/delete" -> DesktopSyncServer.Response.json(deletePlanningNode(JSONObject(body)))
                method == "POST" && path == "/api/planning/nodes/reorder" -> DesktopSyncServer.Response.json(reorderPlanningNodes(JSONObject(body)))
                method == "POST" && routePath.matches(Regex("/api/planning/nodes/\\d+/publish")) -> DesktopSyncServer.Response.json(publishPlanningNode(routePath))
                method == "POST" && path == "/api/planning/nodes/publish-all" -> DesktopSyncServer.Response.json(publishAllPlanningDrafts(JSONObject(body)))
                method == "POST" && path == "/api/planning/parse" -> DesktopSyncServer.Response.json(parsePlanning(JSONObject(body)))
                method == "POST" && path == "/api/planning/import" -> DesktopSyncServer.Response.json(importPlanning(JSONObject(body)))
                method == "GET" && routePath == "/api/planning/mappings" -> DesktopSyncServer.Response.json(planningMappings(path))
                method == "POST" && path == "/api/planning/refresh" -> DesktopSyncServer.Response.json(refreshPlanning(JSONObject(body)))
                method == "POST" && path == "/api/planning/postpone" -> DesktopSyncServer.Response.json(postponePlanning(JSONObject(body)))
                method == "POST" && path == "/api/planning/undo-last" -> DesktopSyncServer.Response.json(undoPlanning(JSONObject(body)))
                method == "POST" && path == "/api/planning/conflict/document" -> DesktopSyncServer.Response.json(resolvePlanningConflictDocument(JSONObject(body)))
                method == "POST" && path == "/api/planning/conflict/item" -> DesktopSyncServer.Response.json(resolvePlanningConflictItem(JSONObject(body)))
                method == "POST" && routePath.matches(Regex("/api/items/\\d+/complete")) -> DesktopSyncServer.Response.json(markCompleted(routePath))
                method == "POST" && routePath.matches(Regex("/api/items/\\d+/cancel")) -> DesktopSyncServer.Response.json(cancelItem(path))
                method == "DELETE" && routePath.matches(Regex("/api/items/\\d+")) -> DesktopSyncServer.Response.json(deleteItem(path))
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

    private suspend fun buildSnapshot(boardOnly: Boolean = false): DesktopSyncSnapshot {
        val groups = app.repository.getAllGroups().ifEmpty { app.repository.ensureDefaultGroups() }
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val items = if (boardOnly) app.repository.getActiveItemsForBoardRange(today) else app.repository.getAllTodos()
        val announcementNotes = app.repository.getPlanningNotesWithAnnouncementHints()
        val announcements = PlanningAnnouncementParser.activeAnnouncements(announcementNotes, today)
        val nowMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val board = DailyBoardSnapshotBuilder.build(
            items = items,
            planningNotes = announcementNotes,
            now = now
        )
        val snapshotTodos = if (boardOnly) {
            board.todoItems
        } else {
            items.filter { it.isTodo }.sortedBy { it.dueAtMillis }
        }
        val snapshotEvents = if (boardOnly) {
            (board.allTodayEvents + board.tomorrowEvents).distinctBy { it.id }
        } else {
            items.filter { it.isEvent }.sortedBy { it.startAtMillis ?: it.dueAtMillis }
        }
        return DesktopSyncSnapshot(
            generatedAtMillis = System.currentTimeMillis(),
            groups = groups,
            todos = snapshotTodos,
            events = snapshotEvents,
            announcements = announcements,
            todayBoard = board.toDesktopSyncBoard(nowMillis = nowMillis),
            partial = boardOnly
        )
    }

    private suspend fun desktopTodos(path: String): JSONObject {
        val offset = queryParam(path, "offset")?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val limit = queryParam(path, "limit")?.toIntOrNull()?.coerceIn(1, DESKTOP_TODO_PAGE_LIMIT_MAX) ?: DESKTOP_TODO_PAGE_LIMIT_DEFAULT
        val query = queryParam(path, "q").orEmpty().trim().take(DESKTOP_TODO_QUERY_MAX_LENGTH)
        val groups = app.repository.getAllGroups().ifEmpty { app.repository.ensureDefaultGroups() }
        val groupsById = groups.associateBy { it.id }
        val todos = app.repository.getDesktopTodoItemsPaged(query, limit, offset)
        val groupIdsByTodoId = todoGroupIdsByTodoId()
        val total = app.repository.countDesktopTodoItems(query)
        return JSONObject()
            .put("generatedAtMillis", System.currentTimeMillis())
            .put("offset", offset)
            .put("limit", limit)
            .put("total", total)
            .put("hasMore", offset + todos.size < total)
            .put("query", query)
            .put("groups", JSONArray(groups.map { it.toDesktopJson() }))
            .put("todos", JSONArray(todos.map { it.toDesktopJson(groupsById[it.groupId], groupIdsByTodoId[it.id].orEmpty()) }))
    }

    private suspend fun desktopEvents(path: String): JSONObject {
        val groups = app.repository.getAllGroups().ifEmpty { app.repository.ensureDefaultGroups() }
        val groupsById = groups.associateBy { it.id }
        val today = LocalDate.now()
        val startDate = queryParam(path, "start")?.let(LocalDate::parse) ?: today
        val endDate = queryParam(path, "end")?.let(LocalDate::parse) ?: startDate.plusDays(10)
        val zone = ZoneId.systemDefault()
        val rangeStartMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val rangeEndMillis = endDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val events = app.repository.getActiveCalendarEventsInRangeOnce(rangeStartMillis, rangeEndMillis)
        return JSONObject()
            .put("generatedAtMillis", System.currentTimeMillis())
            .put("rangeStart", startDate.toString())
            .put("rangeEnd", endDate.toString())
            .put("groups", JSONArray(groups.map { it.toDesktopJson() }))
            .put("events", JSONArray(events.map { it.toDesktopJson(groupsById[it.groupId]) }))
    }

    private suspend fun createTodo(json: JSONObject): JSONObject {
        val groupIds = resolveGroupIds(json)
        val groupId = groupIds.firstOrNull() ?: resolveGroupId(json)
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
            alarmMode = json.optBoolean("alarmMode", false),
            reminderDeliveryMode = com.example.todoalarm.data.ReminderDeliveryMode.fromStorage(json.optString("reminderDeliveryMode")),
            countdownEnabled = json.optBoolean("countdownEnabled", false),
            hiddenFromBoard = json.optBoolean("hiddenFromBoard", false),
            recurrence = recurrence,
            reminderOffsetsMinutes = reminderOffsets,
            groupIds = groupIds
        )
        require(draft.title.isNotBlank()) { "标题不能为空" }
        validateTodoDraft(draft = draft, original = null, scope = RecurrenceScope.CURRENT)?.let { error(it) }
        val created = app.repository.createFromDraft(draft)
        created.forEach { scheduleReminderOrDisable(it) }
        autoBackupIfNeeded()
        return JSONObject().put("created", created.size)
    }

    private suspend fun updateTodo(path: String, json: JSONObject): JSONObject {
        val id = path.substringAfter("/api/todos/").toLong()
        val original = app.repository.getTodo(id) ?: return JSONObject().put("ok", false)
        require(original.isTodo) { "仅支持更新待办" }

        val groupIds = resolveGroupIds(json)
        val groupId = groupIds.firstOrNull() ?: resolveGroupId(json)
        val dueAt = json.optStringOrNull("dueAt")?.let(LocalDateTime::parse)
        val reminderAt = json.optStringOrNull("reminderAt")?.let(LocalDateTime::parse)
        val reminderOffsets = json.optJSONArray("reminderOffsetsMinutes")?.toIntList().orEmpty()
        val recurrence = parseRecurrence(json.optJSONObject("recurrence"), dueAt?.toLocalDate())
        val targetScope = resolveDesktopRecurrenceScope(
            original = original,
            recurrence = recurrence,
            requested = parseRecurrenceScope(json.optStringOrNull("scope"))
        )
        val draft = sanitizeTodoDraft(
            title = json.optString("title").trim(),
            notes = json.optString("notes").trim(),
            dueAt = dueAt,
            reminderAt = reminderAt,
            groupId = groupId,
            ringEnabled = json.optBoolean("ringEnabled", original.ringEnabled),
            vibrateEnabled = json.optBoolean("vibrateEnabled", original.vibrateEnabled),
            alarmMode = json.optBoolean("alarmMode", original.alarmMode),
            reminderDeliveryMode = com.example.todoalarm.data.ReminderDeliveryMode.fromStorage(
                json.optString("reminderDeliveryMode", original.reminderDeliveryMode)
            ),
            countdownEnabled = json.optBoolean("countdownEnabled", original.countdownEnabled),
            hiddenFromBoard = json.optBoolean("hiddenFromBoard", original.hiddenFromBoard),
            recurrence = recurrence,
            reminderOffsetsMinutes = reminderOffsets,
            groupIds = groupIds
        )
        require(draft.title.isNotBlank()) { "标题不能为空" }
        validateTodoDraft(draft = draft, original = original, scope = targetScope)?.let { error(it) }
        val affected = app.repository.getActiveItemsForScope(original, targetScope)
        clearReminderArtifacts(affected.ifEmpty { listOf(original) })
        val updated = app.repository.updateFromDraft(original, draft, targetScope)
        updated.forEach { scheduleReminderOrDisable(it) }
        autoBackupIfNeeded()
        return JSONObject().put("ok", updated.isNotEmpty()).put("scope", targetScope.name)
    }

    private suspend fun createEvent(json: JSONObject): JSONObject {
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
            countdownEnabled = json.optBoolean("countdownEnabled", false),
            checkInEnabled = json.optBoolean("checkInEnabled", false),
            recurrence = recurrence,
            groupId = groupId
        )
        require(draft.title.isNotBlank()) { "日程标题不能为空" }
        validateEventDraft(draft = draft, original = null, scope = RecurrenceScope.CURRENT)?.let { error(it) }
        val created = app.repository.createCalendarEventFromDraft(draft)
        created.forEach { scheduleReminderOrDisable(it) }
        autoBackupIfNeeded()
        return JSONObject().put("created", created.size)
    }

    private suspend fun updateEvent(path: String, json: JSONObject): JSONObject {
        val id = path.substringAfter("/api/events/").toLong()
        val original = app.repository.getTodo(id) ?: return JSONObject().put("ok", false)
        require(original.isEvent) { "仅支持更新日程" }

        val groupId = resolveGroupId(json)
        val startAt = LocalDateTime.parse(json.getString("startAt"))
        val endAt = LocalDateTime.parse(json.getString("endAt"))
        val reminderOffsets = json.optJSONArray("reminderOffsetsMinutes")?.toIntList().orEmpty()
        val recurrence = parseRecurrence(json.optJSONObject("recurrence"), startAt.toLocalDate())
        val targetScope = resolveDesktopRecurrenceScope(
            original = original,
            recurrence = recurrence,
            requested = parseRecurrenceScope(json.optStringOrNull("scope"))
        )
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
            countdownEnabled = json.optBoolean("countdownEnabled", original.countdownEnabled),
            checkInEnabled = json.optBoolean("checkInEnabled", original.checkInEnabled),
            recurrence = recurrence,
            groupId = groupId
        )
        require(draft.title.isNotBlank()) { "日程标题不能为空" }
        validateEventDraft(draft = draft, original = original, scope = targetScope)?.let { error(it) }
        val affected = app.repository.getActiveItemsForScope(original, targetScope)
        clearReminderArtifacts(affected.ifEmpty { listOf(original) })
        val updated = app.repository.updateCalendarEventFromDraft(original, draft, targetScope)
        updated.forEach { scheduleReminderOrDisable(it) }
        autoBackupIfNeeded()
        return JSONObject().put("ok", updated.isNotEmpty()).put("scope", targetScope.name)
    }

    private suspend fun eventCheckIns(path: String): JSONObject {
        val id = path.substringAfter("/api/events/").substringBefore('/').toLong()
        val event = app.repository.getTodo(id) ?: return JSONObject().put("error", "日程不存在").put("checkIns", JSONArray())
        require(event.isEvent) { "仅支持日程打卡" }
        val checkIns = app.repository.getCheckInsForEvent(id)
        val active = checkIns.firstOrNull { it.checkOutAtMillis == null }
        return JSONObject()
            .put("eventId", id)
            .put("checkInEnabled", event.checkInEnabled)
            .put("totalCheckInMinutes", event.totalCheckInMinutes)
            .put("activeCheckInId", active?.id)
            .put("checkIns", JSONArray(checkIns.map { it.toDesktopJson() }))
    }

    private suspend fun checkInEvent(path: String): JSONObject {
        val id = path.substringAfter("/api/events/").substringBefore('/').toLong()
        val checkIn = app.repository.checkInEvent(id)
        autoBackupIfNeeded()
        return JSONObject()
            .put("ok", checkIn != null)
            .put("checkIn", checkIn?.toDesktopJson())
    }

    private suspend fun checkOutEvent(path: String): JSONObject {
        val id = path.substringAfter("/api/events/").substringBefore('/').toLong()
        val checkOut = app.repository.checkOutEvent(id)
        val updated = app.repository.getTodo(id)
        autoBackupIfNeeded()
        return JSONObject()
            .put("ok", checkOut != null)
            .put("checkIn", checkOut?.toDesktopJson())
            .put("totalCheckInMinutes", updated?.totalCheckInMinutes ?: 0)
    }

    private suspend fun markCompleted(path: String): JSONObject {
        val id = path.substringAfter("/api/items/").substringBefore('/').toLong()
        val settings = settingsStore.currentSettings()
        val result = app.repository.setCompletedWithResult(
            id = id,
            completed = true,
            autoCheckOutEventOnEnd = settings.autoCheckOutEventOnEnd
        )
        val updated = result?.item
        result?.affectedItems?.let(::clearReminderArtifacts)
        autoBackupIfNeeded()
        return JSONObject()
            .put("ok", updated != null)
            .put(
                "eventCheckInSummary",
                result?.eventCheckInSummary
                    ?.takeIf { settings.showEventCheckInStatsOnComplete }
                    ?.toDesktopJson()
            )
    }

    private suspend fun cancelItem(path: String): JSONObject {
        val routePath = path.substringBefore('?')
        val id = routePath.substringAfter("/api/items/").substringBefore('/').toLong()
        val item = app.repository.getTodo(id) ?: return JSONObject().put("ok", false)
        val targetScope = parseRecurrenceScope(queryParam(path, "scope")) ?: RecurrenceScope.CURRENT
        val canceled = if (item.isEvent) {
            app.repository.deleteCalendarEvent(item, targetScope)
        } else {
            app.repository.cancelTodo(item, targetScope)
        }
        clearReminderArtifacts(canceled.ifEmpty { listOf(item) })
        autoBackupIfNeeded()
        return JSONObject().put("ok", true).put("scope", targetScope.name)
    }

    private suspend fun deleteItem(path: String): JSONObject {
        val routePath = path.substringBefore('?')
        val id = routePath.substringAfter("/api/items/").toLong()
        val item = app.repository.getTodo(id) ?: return JSONObject().put("ok", false)
        val targetScope = parseRecurrenceScope(queryParam(path, "scope")) ?: RecurrenceScope.CURRENT
        val deletedItems = if (item.isEvent) {
            app.repository.deleteCalendarEvent(item, targetScope)
        } else {
            app.repository.deleteTodo(item, targetScope)
        }
        clearReminderArtifacts(deletedItems.ifEmpty { listOf(item) })
        autoBackupIfNeeded()
        return JSONObject().put("ok", true).put("scope", targetScope.name)
    }

    private suspend fun planningNotes(): JSONObject {
        val notes = app.repository.getAllPlanningNotes().filter { !it.archived }
        val activeId = settingsStore.currentSettings().lastOpenedPlanningNoteId ?: notes.firstOrNull()?.id
        return JSONObject()
            .put("activeNoteId", activeId)
            .put("notes", JSONArray(notes.map { it.toPlanningJson() }))
    }

    private suspend fun createPlanningNote(json: JSONObject): JSONObject {
        val documentDateEpochDay = json.optLong("documentDateEpochDay", Long.MIN_VALUE)
            .takeIf { it != Long.MIN_VALUE }
            ?.let(LocalDate::ofEpochDay)
        val note = if (documentDateEpochDay != null) {
            app.repository.createPlanningNote(json.optString("title", "新的规划"), documentDateEpochDay)
        } else {
            app.repository.createPlanningNote(json.optString("title", "新的规划"))
        }
        settingsStore.updateLastOpenedPlanningNoteId(note.id)
        autoBackupIfNeeded()
        return JSONObject().put("note", note.toPlanningJson())
    }

    private suspend fun updatePlanningNote(path: String, json: JSONObject): JSONObject {
        val id = path.substringAfter("/api/planning/notes/").toLong()
        val title = json.optStringOrNull("title")
        val content = json.optStringOrNull("contentMarkdown")
        val updatedTitle = if (title != null) app.repository.renamePlanningNote(id, title) else app.repository.getPlanningNote(id)
        val updated = if (content != null) app.repository.updatePlanningNoteContent(id, content) else updatedTitle
        require(updated != null) { "规划文档不存在" }
        settingsStore.updateLastOpenedPlanningNoteId(id)
        autoBackupIfNeeded()
        return JSONObject().put("note", updated.toPlanningJson())
    }

    private suspend fun deletePlanningNote(path: String): JSONObject {
        val id = path.substringAfter("/api/planning/notes/").toLong()
        val deletedItems = app.repository.deletePlanningNote(id)
        clearReminderArtifacts(deletedItems)
        val fallback = app.repository.ensureDefaultPlanningNote()
        settingsStore.updateLastOpenedPlanningNoteId(fallback.id)
        autoBackupIfNeeded()
        return JSONObject().put("ok", true)
    }

    private suspend fun planningNodes(path: String): JSONObject {
        val noteId = queryParam(path, "noteId")?.toLongOrNull() ?: error("缺少 noteId")
        val note = app.repository.getPlanningNote(noteId) ?: error("规划文档不存在")
        val nodes = app.repository.getPlanningNodesForNote(note.id)
        settingsStore.updateLastOpenedPlanningNoteId(note.id)
        return JSONObject()
            .put("noteId", note.id)
            .put("nodes", JSONArray(nodes.map { it.toPlanningNodeJson() }))
            .put("markdown", app.repository.exportPlanningNodesToMarkdown(note.id))
    }

    private suspend fun createPlanningNode(json: JSONObject): JSONObject {
        val noteId = json.optLong("noteId", 0L).takeIf { it > 0L } ?: error("缺少 noteId")
        val draft = PlanningNodeDraft(
            noteId = noteId,
            parentNodeId = json.optNullableLong("parentNodeId"),
            text = json.optString("text").trim(),
            sortOrder = json.optIntOrNull("sortOrder"),
            dueAt = parsePlanningNodeDateTime(json.optStringOrNull("dueAt"), defaultTime = LocalTime.of(23, 59)),
            startAt = parsePlanningNodeDateTime(json.optStringOrNull("startAt")),
            endAt = parsePlanningNodeDateTime(json.optStringOrNull("endAt")),
            location = json.optStringOrNull("location")?.trim(),
            isDraft = json.optBoolean("isDraft", true),
            isNote = json.optBoolean("isNote", false),
            syncEnabled = json.optBoolean("syncEnabled", true),
            collapsed = json.optBoolean("collapsed", false),
            completed = json.optBoolean("completed", false)
        )
        require(draft.text.isNotBlank()) { "事项不能为空" }
        val result = app.repository.createPlanningNode(
            draft,
            createEventEndTodo = settingsStore.currentSettings().planningEventEndTodoEnabled
        ) ?: error("规划节点创建失败")
        handlePlanningNodeChange(result)
        settingsStore.updateLastOpenedPlanningNoteId(result.node.noteId)
        if (!result.node.isDraft) autoBackupIfNeeded()
        return planningNodeMutationResult(result.node.noteId, result)
    }

    private suspend fun updatePlanningNode(json: JSONObject): JSONObject {
        val nodeId = json.optLong("id", 0L).takeIf { it > 0L } ?: error("缺少节点 id")
        val existing = app.repository.getPlanningNode(nodeId) ?: error("规划节点不存在")
        val beforeLinked = listOfNotNull(
            existing.linkedTodoId?.let { app.repository.getTodo(it) },
            existing.linkedEndTodoId?.let { app.repository.getTodo(it) }
        )
        val edit = PlanningNodeEdit(
            text = json.optStringOrNull("text")?.trim() ?: existing.text,
            parentNodeId = if (json.has("parentNodeId")) json.optNullableLong("parentNodeId") else existing.parentNodeId,
            sortOrder = json.optIntOrNull("sortOrder") ?: existing.sortOrder,
            dueAt = if (json.has("dueAt")) parsePlanningNodeDateTime(json.optStringOrNull("dueAt"), defaultTime = LocalTime.of(23, 59)) else existing.dueAtMillis?.toPlanningNodeDateTime(),
            startAt = if (json.has("startAt")) parsePlanningNodeDateTime(json.optStringOrNull("startAt")) else existing.startAtMillis?.toPlanningNodeDateTime(),
            endAt = if (json.has("endAt")) parsePlanningNodeDateTime(json.optStringOrNull("endAt")) else existing.endAtMillis?.toPlanningNodeDateTime(),
            location = if (json.has("location")) json.optStringOrNull("location")?.trim() else existing.location,
            isNote = json.optBoolean("isNote", existing.isNote),
            syncEnabled = json.optBoolean("syncEnabled", existing.syncEnabled),
            collapsed = json.optBoolean("collapsed", existing.collapsed),
            completed = json.optBoolean("completed", existing.completed)
        )
        require(edit.text.isNotBlank()) { "事项不能为空" }
        val result = app.repository.updatePlanningNode(
            existing.id,
            edit,
            createEventEndTodo = settingsStore.currentSettings().planningEventEndTodoEnabled
        ) ?: error("规划节点更新失败")
        clearReminderArtifacts(beforeLinked)
        handlePlanningNodeChange(result)
        settingsStore.updateLastOpenedPlanningNoteId(result.node.noteId)
        if (!result.node.isDraft) autoBackupIfNeeded()
        return planningNodeMutationResult(result.node.noteId, result)
    }

    private suspend fun publishPlanningNode(routePath: String): JSONObject {
        val nodeId = Regex("/api/planning/nodes/(\\d+)/publish").matchEntire(routePath)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
            ?: error("缺少节点 id")
        val result = app.repository.publishPlanningNode(
            nodeId,
            createEventEndTodo = settingsStore.currentSettings().planningEventEndTodoEnabled
        ) ?: error("草稿发布失败，请检查时间格式")
        clearReminderArtifacts(result.deletedLinkedItems)
        handlePlanningNodeChange(result)
        settingsStore.updateLastOpenedPlanningNoteId(result.node.noteId)
        autoBackupIfNeeded()
        return planningNodeMutationResult(result.node.noteId, result)
    }

    private suspend fun publishAllPlanningDrafts(json: JSONObject): JSONObject {
        val noteId = json.optLong("noteId", 0L).takeIf { it > 0L } ?: error("缺少 noteId")
        val result = app.repository.publishAllPlanningDrafts(
            noteId,
            createEventEndTodo = settingsStore.currentSettings().planningEventEndTodoEnabled
        )
        result.published.forEach { change ->
            clearReminderArtifacts(change.deletedLinkedItems)
            handlePlanningNodeChange(change)
        }
        if (result.published.isNotEmpty()) {
            settingsStore.updateLastOpenedPlanningNoteId(noteId)
            autoBackupIfNeeded()
        }
        return JSONObject()
            .put("ok", true)
            .put("publishedCount", result.published.size)
            .put("failedCount", result.failedCount)
            .put("noteId", noteId)
            .put("nodes", JSONArray(app.repository.getPlanningNodesForNote(noteId).map { it.toPlanningNodeJson() }))
            .put("markdown", app.repository.exportPlanningNodesToMarkdown(noteId))
    }

    private suspend fun deletePlanningNode(json: JSONObject): JSONObject {
        val nodeId = json.optLong("id", 0L).takeIf { it > 0L } ?: error("缺少节点 id")
        val existing = app.repository.getPlanningNode(nodeId) ?: return JSONObject().put("ok", false)
        val result = app.repository.deletePlanningNodeTree(
            existing.id,
            createEventEndTodo = settingsStore.currentSettings().planningEventEndTodoEnabled
        )
        clearReminderArtifacts(result.deletedLinkedItems)
        result.affectedLinkedItems.forEach { scheduleReminderOrDisable(it) }
        settingsStore.updateLastOpenedPlanningNoteId(existing.noteId)
        if (!existing.isDraft || result.deletedLinkedItems.isNotEmpty() || result.affectedLinkedItems.isNotEmpty()) {
            autoBackupIfNeeded()
        }
        return JSONObject()
            .put("ok", true)
            .put("deletedLinkedItems", result.deletedLinkedItems.size)
            .put("noteId", existing.noteId)
            .put("nodes", JSONArray(app.repository.getPlanningNodesForNote(existing.noteId).map { it.toPlanningNodeJson() }))
            .put("markdown", app.repository.exportPlanningNodesToMarkdown(existing.noteId))
    }

    private suspend fun reorderPlanningNodes(json: JSONObject): JSONObject {
        val noteId = json.optLong("noteId", 0L).takeIf { it > 0L } ?: error("缺少 noteId")
        val orderedIds = json.optJSONArray("orderedNodeIds")?.toLongList().orEmpty()
        require(orderedIds.isNotEmpty()) { "缺少排序节点" }
        app.repository.reorderPlanningNodes(
            noteId = noteId,
            parentNodeId = json.optNullableLong("parentNodeId"),
            orderedNodeIds = orderedIds
        )
        settingsStore.updateLastOpenedPlanningNoteId(noteId)
        autoBackupIfNeeded()
        return JSONObject()
            .put("ok", true)
            .put("noteId", noteId)
            .put("nodes", JSONArray(app.repository.getPlanningNodesForNote(noteId).map { it.toPlanningNodeJson() }))
            .put("markdown", app.repository.exportPlanningNodesToMarkdown(noteId))
    }

    private suspend fun parsePlanning(json: JSONObject): JSONObject {
        val markdown = json.optString("markdown")
        val noteId = json.optLong("noteId", 0L).takeIf { it > 0L }
        val defaultDate = noteId
            ?.let { app.repository.getPlanningNote(it)?.documentDateEpochDay }
            ?.let(LocalDate::ofEpochDay)
        val result = PlanningRecognitionService.recognize(
            markdown = markdown,
            settings = settingsStore.currentSettings(),
            defaultDate = defaultDate
        )
        return result.toPlanningParseJson()
    }

    private suspend fun importPlanning(json: JSONObject): JSONObject {
        val markdown = json.optString("markdown")
        val selectedIds = json.optJSONArray("selectedIds")?.toStringSet().orEmpty()
        val noteId = json.optLong("noteId", 0L).takeIf { it > 0L }
        val defaultDate = noteId
            ?.let { app.repository.getPlanningNote(it)?.documentDateEpochDay }
            ?.let(LocalDate::ofEpochDay)
        val result = PlanningMarkdownParser.parse(markdown, documentDate = defaultDate)
        val editedCandidates = json.optJSONArray("candidates")?.toPlanningImportCandidates(result.candidates).orEmpty()
        val sourceCandidates = if (editedCandidates.isNotEmpty()) editedCandidates else result.candidates.map { it.toPlanningImportCandidate() }
        val groups = app.repository.getAllGroups().ifEmpty { app.repository.ensureDefaultGroups() }
        val selected = sourceCandidates.filter { it.id in selectedIds && it.importable }
        require(selected.isNotEmpty()) { "没有可导入的规划条目" }
        selected.forEachIndexed { index, candidate ->
            candidate.validate()?.let { error("第 ${index + 1} 条：$it") }
        }
        val batchId = UUID.randomUUID().toString()
        val importAtMillis = System.currentTimeMillis()
        val markdownLines = planningDocumentLines(markdown)
        val mappings = mutableListOf<PlanningLineMapping>()
        selected.forEach { candidate ->
            val sourceLine = markdownLines.getOrNull(candidate.lineNumber - 1) ?: candidate.sourceLine
            when (candidate.type) {
                PlanningParsedType.TODO -> {
                    val created = app.repository.createFromDraft(candidate.toPlanningTodoDraft(groups))
                    created.forEach { scheduleReminderOrDisable(it) }
                    mappings += created.mapNotNull { item ->
                        candidate.toPlanningLineMapping(noteId, item, sourceLine, batchId, importAtMillis)
                    }
                }
                PlanningParsedType.EVENT -> {
                    val created = app.repository.createCalendarEventFromDraft(candidate.toPlanningEventDraft(groups))
                    created.forEach { scheduleReminderOrDisable(it) }
                    mappings += created.mapNotNull { item ->
                        candidate.toPlanningLineMapping(noteId, item, sourceLine, batchId, importAtMillis)
                    }
                    if (candidate.createLinkedTodo) {
                        val linked = app.repository.createFromDraft(candidate.toPlanningLinkedTodoDraft(groups))
                        linked.forEach { scheduleReminderOrDisable(it) }
                        mappings += linked.mapNotNull { item ->
                            candidate.toPlanningLineMapping(noteId, item, sourceLine, batchId, importAtMillis)
                        }
                    }
                }
                else -> Unit
            }
        }
        val updatedMarkdown = PlanningMarkdownParser.markImportedLines(markdown, selected.map { it.lineNumber }.toSet())
        if (noteId != null) {
            app.repository.insertPlanningMappings(mappings)
            app.repository.updatePlanningNoteContent(noteId, updatedMarkdown)
            settingsStore.updateLastOpenedPlanningNoteId(noteId)
        }
        autoBackupIfNeeded()
        return JSONObject()
            .put("imported", selected.size)
            .put("updatedMarkdown", updatedMarkdown)
    }

    private suspend fun planningMappings(path: String): JSONObject {
        val noteId = queryParam(path, "noteId")?.toLongOrNull() ?: error("缺少 noteId")
        val note = app.repository.getPlanningNote(noteId) ?: error("规划文档不存在")
        val synced = app.repository.syncPlanningMappingStatuses(noteId, note.contentMarkdown)
        return JSONObject()
            .put("changed", synced.changedCount)
            .put("mappings", JSONArray(synced.mappings.map { it.toPlanningMappingJson() }))
    }

    private suspend fun refreshPlanning(json: JSONObject): JSONObject {
        val noteId = json.optLong("noteId", 0L).takeIf { it > 0L } ?: error("缺少 noteId")
        val markdown = json.optString("markdown")
        val scope = PlanningRefreshScope.fromStorage(json.optString("scope", PlanningRefreshScope.CURRENT_SECTION.name))
        val cursorLineNumber = json.optInt("cursorLineNumber", 0).takeIf { it > 0 }
        val result = app.repository.refreshPlanningImportedItems(
            noteId = noteId,
            markdown = markdown,
            wholeDocument = scope == PlanningRefreshScope.WHOLE_DOCUMENT,
            cursorLineNumber = cursorLineNumber
        )
        reschedulePlanningOperationItems(result)
        autoBackupIfNeeded()
        return result.toPlanningOperationJson()
    }

    private suspend fun postponePlanning(json: JSONObject): JSONObject {
        val noteId = json.optLong("noteId", 0L).takeIf { it > 0L } ?: error("缺少 noteId")
        val markdown = json.optString("markdown")
        val mappingId = json.optLong("mappingId", 0L).takeIf { it > 0L }
        val offsetMinutes = json.optInt("offsetMinutes", 0)
        val scope = PlanningPostponeScope.fromStorage(json.optString("scope", PlanningPostponeScope.FROM_ITEM_TO_SECTION_END.name))
        val result = app.repository.postponePlanningImportedItems(
            noteId = noteId,
            markdown = markdown,
            startMappingId = mappingId,
            offsetMinutes = offsetMinutes,
            scope = scope
        )
        reschedulePlanningOperationItems(result)
        autoBackupIfNeeded()
        return result.toPlanningOperationJson()
    }

    private suspend fun undoPlanning(json: JSONObject): JSONObject {
        val noteId = json.optLong("noteId", 0L).takeIf { it > 0L } ?: error("缺少 noteId")
        val markdown = json.optString("markdown")
        val result = app.repository.undoLastPlanningOperation(noteId, markdown)
        clearReminderArtifacts(result.affectedBeforeItems)
        result.affectedAfterItems.forEach { scheduleReminderOrDisable(it) }
        autoBackupIfNeeded()
        return result.toPlanningOperationJson()
    }

    private suspend fun resolvePlanningConflictDocument(json: JSONObject): JSONObject {
        val noteId = json.optLong("noteId", 0L).takeIf { it > 0L } ?: error("缺少 noteId")
        val markdown = json.optString("markdown")
        val mappingId = json.optLong("mappingId", 0L).takeIf { it > 0L } ?: error("缺少 mappingId")
        val result = app.repository.resolvePlanningConflictWithDocument(noteId, markdown, mappingId)
        reschedulePlanningOperationItems(result)
        autoBackupIfNeeded()
        return result.toPlanningOperationJson()
    }

    private suspend fun resolvePlanningConflictItem(json: JSONObject): JSONObject {
        val noteId = json.optLong("noteId", 0L).takeIf { it > 0L } ?: error("缺少 noteId")
        val markdown = json.optString("markdown")
        val mappingId = json.optLong("mappingId", 0L).takeIf { it > 0L } ?: error("缺少 mappingId")
        val result = app.repository.resolvePlanningConflictWithItem(noteId, markdown, mappingId)
        autoBackupIfNeeded()
        return result.toPlanningOperationJson()
    }

    private suspend fun resolveGroupId(json: JSONObject): Long {
        val groups = app.repository.getAllGroups().ifEmpty { app.repository.ensureDefaultGroups() }
        val groupId = json.optLong("groupId", 0L)
        if (groupId > 0 && groups.any { it.id == groupId }) return groupId
        val requestedName = json.optString("groupName").trim()
        if (requestedName.isNotBlank()) {
            val existing = groups.firstOrNull { it.name == requestedName }
            if (existing != null) return existing.id
            return app.repository.createGroup(requestedName, json.optString("groupColorHex", "#4E87E1")).id
        }
        return groups.firstOrNull { it.name == "例行" }?.id ?: groups.firstOrNull()?.id ?: 0L
    }

    private suspend fun resolveGroupIds(json: JSONObject): List<Long> {
        val groups = app.repository.getAllGroups().ifEmpty { app.repository.ensureDefaultGroups() }
        val validIds = groups.map { it.id }.toSet()
        val explicitIds = json.optJSONArray("groupIds")
            ?.toLongList()
            .orEmpty()
            .filter { it > 0 && it in validIds }
            .distinct()
        if (explicitIds.isNotEmpty()) return explicitIds
        return listOf(resolveGroupId(json)).filter { it > 0 }
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

    private fun parseRecurrenceScope(raw: String?): RecurrenceScope? {
        return when (raw?.trim()?.uppercase(Locale.ROOT)) {
            "CURRENT" -> RecurrenceScope.CURRENT
            "CURRENT_AND_FUTURE", "FUTURE" -> RecurrenceScope.CURRENT_AND_FUTURE
            "ALL" -> RecurrenceScope.ALL
            else -> null
        }
    }

    private fun resolveDesktopRecurrenceScope(
        original: TodoItem,
        recurrence: RecurrenceConfig,
        requested: RecurrenceScope?
    ): RecurrenceScope {
        if (!original.isRecurring) return RecurrenceScope.CURRENT
        if (!recurrence.isRecurring && requested == RecurrenceScope.CURRENT) {
            return RecurrenceScope.CURRENT_AND_FUTURE
        }
        return requested ?: if (recurrence.isRecurring) {
            RecurrenceScope.CURRENT
        } else {
            RecurrenceScope.CURRENT_AND_FUTURE
        }
    }

    private fun sanitizeTodoDraft(
        title: String,
        notes: String,
        dueAt: LocalDateTime?,
        reminderAt: LocalDateTime?,
        groupId: Long,
        ringEnabled: Boolean,
        vibrateEnabled: Boolean,
        alarmMode: Boolean,
        reminderDeliveryMode: com.example.todoalarm.data.ReminderDeliveryMode,
        countdownEnabled: Boolean,
        hiddenFromBoard: Boolean,
        recurrence: RecurrenceConfig,
        reminderOffsetsMinutes: List<Int>,
        groupIds: List<Long> = emptyList()
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
            alarmMode = alarmMode && dueAt != null && reminderOffsetsMinutes.isNotEmpty(),
            reminderDeliveryMode = reminderDeliveryMode,
            countdownEnabled = countdownEnabled,
            hiddenFromBoard = hiddenFromBoard && dueAt != null,
            recurrence = recurrence,
            reminderOffsetsMinutes = reminderOffsetsMinutes,
            groupIds = groupIds
        )
    }

    private fun validateTodoDraft(
        draft: TodoDraft,
        original: TodoItem?,
        scope: RecurrenceScope
    ): String? {
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
        if (original?.isRecurring == true && scope == RecurrenceScope.CURRENT &&
            recurrenceSignatureChanged(original, draft.recurrence)
        ) {
            return "仅修改当前事件时不能变更循环规则；请改用“当前事件和此后所有事件”或“所有事件”"
        }
        return null
    }

    private fun validateEventDraft(
        draft: CalendarEventDraft,
        original: TodoItem?,
        scope: RecurrenceScope
    ): String? {
        if (draft.title.isBlank()) return "日程标题不能为空"
        val startMillis = draft.startAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = if (draft.allDay) {
            draft.endAt.toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else {
            draft.endAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        if (endMillis <= startMillis) return "结束时间必须晚于开始时间"

        val recurrence = draft.recurrence
        if (recurrence.enabled) {
            if (recurrence.type == RecurrenceType.NONE) return "请选择循环规则"
            val endDate = recurrence.endDate ?: return "请设置循环截止日期"
            val canTerminateSeriesEarly = original?.isRecurring == true && scope != RecurrenceScope.CURRENT
            if (!canTerminateSeriesEarly && endDate.isBefore(draft.startAt.toLocalDate())) {
                return "循环截止日期不能早于首次日程日期"
            }
            if (recurrence.type == RecurrenceType.WEEKLY && recurrence.weeklyDays.isEmpty()) {
                return "每周循环至少选择一天"
            }
        }
        if (original?.isRecurring == true && scope == RecurrenceScope.CURRENT &&
            recurrenceSignatureChanged(original, recurrence)
        ) {
            return "仅修改当前事件时不能变更循环规则；请改用“当前事件和此后所有事件”或“所有事件”"
        }
        return null
    }

    private fun recurrenceSignatureChanged(original: TodoItem, recurrence: RecurrenceConfig): Boolean {
        return recurrence.enabled != original.isRecurring ||
            recurrence.type != original.recurrenceTypeEnum ||
            recurrence.weeklyDays != storageStringToWeekdays(original.recurrenceWeekdays) ||
            recurrence.endDate != original.recurrenceEndDate
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
        countdownEnabled: Boolean,
        checkInEnabled: Boolean,
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
            countdownEnabled = countdownEnabled,
            checkInEnabled = checkInEnabled,
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

    private suspend fun scheduleReminderOrDisable(item: TodoItem) {
        if (item.isTodo && !item.hasDueDate) {
            if (item.reminderEnabled || item.reminderAtMillis != null) {
                ReminderDispatchTracker.clear(context, item.id)
                app.repository.updateTodo(item.copy(reminderEnabled = false, reminderAtMillis = null))
            }
            return
        }
        if (!item.reminderEnabled) return
        ReminderDispatchTracker.clear(context, item.id)
        val message = app.alarmScheduler.schedule(item)
        if (message != null) {
            app.repository.updateTodo(item.copy(reminderEnabled = false))
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

    private suspend fun reschedulePlanningOperationItems(result: PlanningOperationResult) {
        clearReminderArtifacts(result.affectedBeforeItems)
        result.affectedAfterItems.forEach { scheduleReminderOrDisable(it) }
    }

    private suspend fun handlePlanningNodeChange(result: PlanningNodeChangeResult) {
        clearReminderArtifacts(result.deletedLinkedItems)
        val linkedItems = result.affectedLinkedItems.ifEmpty { result.linkedItem?.let { listOf(it) }.orEmpty() }
        linkedItems.forEach { linked ->
            if (linked.completed || linked.canceled) {
                clearReminderArtifacts(listOf(linked))
            } else {
                scheduleReminderOrDisable(linked)
            }
        }
    }

    private suspend fun planningNodeMutationResult(noteId: Long, result: PlanningNodeChangeResult): JSONObject {
        return JSONObject()
            .put("ok", true)
            .put("node", result.node.toPlanningNodeJson())
            .put("linkedItemId", result.linkedItem?.id)
            .put("linkedItemType", result.linkedItem?.let { if (it.isEvent) "EVENT" else "TODO" })
            .put("nodes", JSONArray(app.repository.getPlanningNodesForNote(noteId).map { it.toPlanningNodeJson() }))
            .put("markdown", app.repository.exportPlanningNodesToMarkdown(noteId))
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

    private suspend fun todoGroupIdsByTodoId(): Map<Long, List<Long>> {
        return app.repository.getAllTodoGroupTags()
            .groupBy { it.todoId }
            .mapValues { entry -> entry.value.map { it.groupId }.filter { it > 0 }.distinct().sorted() }
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
                .sortedWith(compareBy<String> { ipv4Priority(it) }.thenBy { it })
        }.getOrDefault(emptyList())
    }

    private fun ipv4Priority(address: String): Int {
        return when {
            address.startsWith("192.168.") -> 0
            address.startsWith("10.") -> 1
            address.matches(Regex("^172\\.(1[6-9]|2\\d|3[0-1])\\..*")) -> 2
            else -> 3
        }
    }
}

private fun JSONObject.optStringOrNull(key: String): String? {
    if (isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
}

private fun JSONObject.optNullableLong(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    return optLong(key, 0L).takeIf { it > 0L }
}

private fun JSONObject.optIntOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}

private fun PlanningNode.toPlanningNodeJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("noteId", noteId)
        .put("parentNodeId", parentNodeId)
        .put("sortOrder", sortOrder)
        .put("text", text)
        .put("createdAtMillis", createdAtMillis)
        .put("updatedAtMillis", updatedAtMillis)
        .put("startAtMillis", startAtMillis)
        .put("endAtMillis", endAtMillis)
        .put("dueAtMillis", dueAtMillis)
        .put("startAt", startAtMillis?.toPlanningNodeDateTime()?.toString())
        .put("endAt", endAtMillis?.toPlanningNodeDateTime()?.toString())
        .put("dueAt", dueAtMillis?.toPlanningNodeDateTime()?.toString())
        .put("location", location)
        .put("linkedTodoId", linkedTodoId)
        .put("linkedEndTodoId", linkedEndTodoId)
        .put("isDraft", isDraft)
        .put("isNote", isNote)
        .put("syncEnabled", syncEnabled)
        .put("collapsed", collapsed)
        .put("completed", completed)
        .put("completedAtMillis", completedAtMillis)
}

private fun parsePlanningNodeDateTime(raw: String?, defaultTime: LocalTime? = null): LocalDateTime? {
    return parsePlanningImportDateTime(raw, defaultTime = defaultTime)
}

private fun Long.toPlanningNodeDateTime(): LocalDateTime {
    return java.time.Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
}

private fun JSONArray.toIntList(): List<Int> {
    return buildList(length()) {
        for (index in 0 until length()) {
            add(optInt(index))
        }
    }
}

private fun JSONArray.toLongList(): List<Long> {
    return buildList(length()) {
        for (index in 0 until length()) {
            add(optLong(index))
        }
    }
}

private fun JSONArray.toStringSet(): Set<String> {
    return buildSet(length()) {
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}

private fun queryParam(path: String, key: String): String? {
    val query = path.substringAfter('?', missingDelimiterValue = "")
    if (query.isBlank()) return null
    return query.split('&').firstNotNullOfOrNull { part ->
        val name = part.substringBefore('=')
        val value = part.substringAfter('=', missingDelimiterValue = "")
        if (name == key) java.net.URLDecoder.decode(value, Charsets.UTF_8.name()) else null
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

private fun RecurrenceConfig.toPlanningJson(): JSONObject {
    return JSONObject()
        .put("enabled", enabled)
        .put("type", type.name)
        .put("weeklyDays", JSONArray(weeklyDays.map { it.value }.sorted()))
        .put("endDate", endDate?.toString())
}

private fun PlanningNote.toPlanningJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("title", title)
        .put("contentMarkdown", contentMarkdown)
        .put("createdAtMillis", createdAtMillis)
        .put("updatedAtMillis", updatedAtMillis)
        .put("archived", archived)
        .put("documentDateEpochDay", documentDateEpochDay)
        .put("hasAnnouncementHint", hasAnnouncementHint)
}

private fun com.example.todoalarm.data.PlanningParseResult.toPlanningParseJson(): JSONObject {
    return JSONObject()
        .put("message", message)
        .put("importableCount", importableCount)
        .put("candidates", JSONArray(candidates.map { it.toPlanningCandidateJson() }))
}

private fun PlanningParsedCandidate.toPlanningCandidateJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("lineNumber", lineNumber)
        .put("sourceLine", sourceLine)
        .put("type", type.name)
        .put("title", title)
        .put("notes", notes)
        .put("location", location)
        .put("groupName", groupName)
        .put("dueAt", dueAt?.toString())
        .put("startAt", startAt?.toString())
        .put("endAt", endAt?.toString())
        .put("allDay", allDay)
        .put("countdownEnabled", countdownEnabled)
        .put("checkInEnabled", checkInEnabled)
        .put("reminderOffsetsMinutes", JSONArray(reminderOffsetsMinutes))
        .put("reminderInputText", reminderOffsetsMinutes.joinToString(","))
        .put("recurrence", recurrence.toPlanningJson())
        .put("createLinkedTodo", createLinkedTodo)
        .put("defaultToday", defaultToday)
        .put("imported", imported)
        .put("completed", completed)
        .put("importBlocked", importBlocked)
        .put("parentTitle", parentTitle)
        .put("message", message)
        .put("importable", importable)
}

private fun PlanningLineMapping.toPlanningMappingJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("noteId", noteId)
        .put("contentFingerprint", contentFingerprint)
        .put("originalLineText", originalLineText)
        .put("currentLineText", currentLineText)
        .put("todoId", todoId)
        .put("eventId", eventId)
        .put("batchId", batchId)
        .put("operationType", operationType)
        .put("createdAtMillis", createdAtMillis)
        .put("lastRefreshedAtMillis", lastRefreshedAtMillis)
        .put("status", status.name)
        .put("postponeOffsetMinutes", postponeOffsetMinutes)
        .put("lastKnownLineNumber", lastKnownLineNumber)
}

private fun PlanningOperationResult.toPlanningOperationJson(): JSONObject {
    return JSONObject()
        .put("message", message)
        .put("updatedMarkdown", updatedMarkdown)
        .put("refreshedCount", refreshedCount)
        .put("skippedCount", skippedCount)
        .put("orphanedCount", orphanedCount)
        .put("conflictCount", conflictCount)
        .put("batchId", batchId)
}

private fun JSONArray.toPlanningImportCandidates(fallback: List<PlanningParsedCandidate>): List<PlanningImportCandidate> {
    val fallbackById = fallback.associateBy { it.id }
    return (0 until length()).mapNotNull { index ->
        val json = optJSONObject(index) ?: return@mapNotNull null
        val base = fallbackById[json.optString("id")]?.toPlanningImportCandidate()
            ?: json.toPlanningImportCandidateOrNull(index)
            ?: return@mapNotNull null
        val reminderRaw = json.optStringOrNull("reminderInputText")
        val edited = base.copy(
            title = json.optString("title", base.title),
            notes = json.optString("notes", base.notes),
            location = json.optString("location", base.location),
            groupName = json.optString("groupName", base.groupName),
            dueAt = parsePlanningImportDateTime(json.optStringOrNull("dueAt"), defaultTime = LocalTime.of(23, 59)),
            startAt = parsePlanningImportDateTime(json.optStringOrNull("startAt"), defaultTime = null),
            endAt = parsePlanningImportDateTime(json.optStringOrNull("endAt"), defaultTime = null),
            allDay = json.optBoolean("allDay", base.allDay),
            countdownEnabled = json.optBoolean("countdownEnabled", base.countdownEnabled),
            checkInEnabled = json.optBoolean("checkInEnabled", base.checkInEnabled),
            reminderOffsetsMinutes = json.optJSONArray("reminderOffsetsMinutes")?.toIntList().orEmpty(),
            reminderInputText = reminderRaw.orEmpty(),
            recurrence = parsePlanningRecurrence(json.optJSONObject("recurrence"), when (base.type) {
                PlanningParsedType.TODO -> parsePlanningImportDateTime(json.optStringOrNull("dueAt"), defaultTime = LocalTime.of(23, 59)) ?: base.dueAt
                PlanningParsedType.EVENT -> parsePlanningImportDateTime(json.optStringOrNull("startAt"), defaultTime = null) ?: base.startAt
                else -> null
            }, base.recurrence),
            createLinkedTodo = json.optBoolean("createLinkedTodo", base.createLinkedTodo)
        )
        if (reminderRaw.isNullOrBlank()) {
            edited
        } else {
            val anchor = when (edited.type) {
                PlanningParsedType.TODO -> edited.dueAt
                PlanningParsedType.EVENT -> edited.startAt
                else -> null
            }
            if (anchor == null) {
                edited.copy(reminderInputError = "请先设置 DDL / 日程开始时间。")
            } else {
                val parsed = parseReminderTextInput(raw = reminderRaw, anchor = anchor, requireFuture = false)
                if (parsed.isValid) {
                    edited.copy(reminderOffsetsMinutes = parsed.offsetsMinutes, reminderInputError = "")
                } else {
                    edited.copy(reminderInputError = parsed.message)
                }
            }
        }
    }
}

private fun JSONObject.toPlanningImportCandidateOrNull(index: Int): PlanningImportCandidate? {
    val type = runCatching { PlanningParsedType.valueOf(optString("type")) }.getOrNull()
        ?: when (optString("type").lowercase(Locale.ROOT)) {
            "todo", "task", "待办", "任务" -> PlanningParsedType.TODO
            "event", "schedule", "calendar", "日程", "事件" -> PlanningParsedType.EVENT
            "skipped", "skip", "跳过" -> PlanningParsedType.SKIPPED
            "error", "错误" -> PlanningParsedType.ERROR
            else -> null
        }
        ?: return null
    val lineNumber = optInt("lineNumber", index + 1).coerceAtLeast(1)
    val id = optString("id").ifBlank { "desktop-ai-$lineNumber-$index" }
    return PlanningImportCandidate(
        id = id,
        lineNumber = lineNumber,
        sourceLine = optString("sourceLine").ifBlank { optString("title") },
        type = type,
        title = optString("title"),
        notes = optString("notes"),
        location = optString("location"),
        groupName = optString("groupName"),
        dueAt = parsePlanningImportDateTime(optStringOrNull("dueAt"), defaultTime = LocalTime.of(23, 59)),
        startAt = parsePlanningImportDateTime(optStringOrNull("startAt"), defaultTime = null),
        endAt = parsePlanningImportDateTime(optStringOrNull("endAt"), defaultTime = null),
        allDay = optBoolean("allDay", false),
        countdownEnabled = optBoolean("countdownEnabled", false),
        checkInEnabled = optBoolean("checkInEnabled", false),
        reminderOffsetsMinutes = optJSONArray("reminderOffsetsMinutes")?.toIntList().orEmpty(),
        reminderInputText = optString("reminderInputText"),
        recurrence = parsePlanningRecurrence(
            optJSONObject("recurrence"),
            when (type) {
                PlanningParsedType.TODO -> parsePlanningImportDateTime(optStringOrNull("dueAt"), defaultTime = LocalTime.of(23, 59))
                PlanningParsedType.EVENT -> parsePlanningImportDateTime(optStringOrNull("startAt"), defaultTime = null)
                else -> null
            },
            RecurrenceConfig()
        ),
        createLinkedTodo = optBoolean("createLinkedTodo", false),
        defaultToday = optBoolean("defaultToday", false),
        imported = optBoolean("imported", false),
        completed = optBoolean("completed", false),
        importBlocked = optBoolean("importBlocked", false),
        parentTitle = optStringOrNull("parentTitle"),
        message = optString("message")
    )
}

private fun PlanningImportCandidate.toPlanningLineMapping(
    noteId: Long?,
    item: TodoItem,
    sourceLine: String,
    batchId: String,
    timestamp: Long
): PlanningLineMapping? {
    val resolvedNoteId = noteId ?: return null
    return PlanningLineMapping(
        noteId = resolvedNoteId,
        contentFingerprint = PlanningLineMatcher.fingerprint(sourceLine),
        originalLineText = sourceLine,
        currentLineText = sourceLine,
        todoId = item.id.takeIf { item.isTodo },
        eventId = item.id.takeIf { item.isEvent },
        batchId = batchId,
        operationType = "IMPORT",
        createdAtMillis = timestamp,
        lastRefreshedAtMillis = timestamp,
        lastKnownLineNumber = lineNumber
    )
}

private fun planningDocumentLines(markdown: String): List<String> {
    return markdown.replace("\r\n", "\n").replace('\r', '\n').lines().let { lines ->
        if (lines.size > 1 && lines.last().isEmpty() && markdown.endsWith("\n")) lines.dropLast(1) else lines
    }
}

private fun parsePlanningImportDateTime(raw: String?, defaultTime: LocalTime?): LocalDateTime? {
    val text = raw.orEmpty().trim().replace('：', ':').replace('T', ' ')
    if (text.isBlank()) return null
    runCatching { LocalDateTime.parse(text.replace(' ', 'T')) }.getOrNull()?.let { return it }
    PlanningMarkdownParser.parseDateTimeExpression(
        raw = text,
        defaultDate = null,
        nowDate = LocalDate.now(),
        defaultTime = defaultTime
    )?.let { return it }
    listOf("yyyy-MM-dd HH:mm", "yyyy-M-d H:mm", "yyyy.MM.dd HH:mm", "yyyy.M.d H:mm").forEach { pattern ->
        runCatching { LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern, Locale.CHINA)) }.getOrNull()?.let { return it }
    }
    val monthDay = Regex("^(\\d{1,2})[-.](\\d{1,2})[\\s,，]+(\\d{1,2}):(\\d{2})$").matchEntire(text) ?: return null
    val month = monthDay.groupValues[1].toIntOrNull() ?: return null
    val day = monthDay.groupValues[2].toIntOrNull() ?: return null
    val hour = monthDay.groupValues[3].toIntOrNull() ?: return null
    val minute = monthDay.groupValues[4].toIntOrNull() ?: return null
    return runCatching { LocalDateTime.of(LocalDate.now().year, month, day, hour, minute) }.getOrNull()
}

private fun parsePlanningRecurrence(
    json: JSONObject?,
    anchor: LocalDateTime?,
    fallback: RecurrenceConfig
): RecurrenceConfig {
    if (json == null) return fallback
    if (!json.optBoolean("enabled", false)) return RecurrenceConfig()
    val type = RecurrenceType.fromStorage(json.optString("type"))
    if (type == RecurrenceType.NONE) return RecurrenceConfig()
    val weeklyDays = json.optJSONArray("weeklyDays")?.toWeekdays().orEmpty()
    val endDate = json.optStringOrNull("endDate")?.let { raw ->
        runCatching { LocalDate.parse(raw) }.getOrNull()
    }
    return RecurrenceConfig(
        enabled = true,
        type = type,
        weeklyDays = if (weeklyDays.isEmpty() && type == RecurrenceType.WEEKLY) {
            anchor?.dayOfWeek?.let { setOf(it) }.orEmpty()
        } else {
            weeklyDays
        },
        endDate = endDate
    )
}

private fun PlanningImportCandidate.toPlanningTodoDraft(groups: List<com.example.todoalarm.data.TaskGroup>): TodoDraft {
    return TodoDraft(
        title = title,
        notes = notes,
        dueAt = dueAt,
        reminderAt = null,
        groupId = resolvePlanningGroupId(groupName, groups),
        ringEnabled = true,
        vibrateEnabled = true,
        countdownEnabled = countdownEnabled && dueAt != null,
        recurrence = recurrence,
        reminderOffsetsMinutes = if (dueAt == null) emptyList() else normalizedReminderOffsets().ifEmpty { listOf(DEFAULT_PLANNING_REMINDER_MINUTES) }
    )
}

private fun PlanningImportCandidate.toPlanningLinkedTodoDraft(groups: List<com.example.todoalarm.data.TaskGroup>): TodoDraft {
    val ddl = requireNotNull(endAt) { "日程结束时间不存在" }
    return TodoDraft(
        title = title,
        notes = notes,
        dueAt = ddl,
        reminderAt = null,
        groupId = resolvePlanningGroupId(groupName, groups),
        ringEnabled = true,
        vibrateEnabled = true,
        recurrence = RecurrenceConfig(),
        reminderOffsetsMinutes = normalizedReminderOffsets().ifEmpty { listOf(DEFAULT_PLANNING_REMINDER_MINUTES) }
    )
}

private fun PlanningImportCandidate.toPlanningEventDraft(groups: List<com.example.todoalarm.data.TaskGroup>): CalendarEventDraft {
    val offsets = normalizedReminderOffsets().ifEmpty { listOf(DEFAULT_PLANNING_REMINDER_MINUTES) }
    return CalendarEventDraft(
        title = title,
        notes = notes,
        location = location,
        startAt = requireNotNull(startAt) { "日程开始时间不存在" },
        endAt = requireNotNull(endAt) { "日程结束时间不存在" },
        allDay = allDay,
        accentColorHex = groups.firstOrNull { it.name == groupName }?.colorHex ?: "#4E87E1",
        reminderMinutesBefore = offsets.firstOrNull() ?: DEFAULT_PLANNING_REMINDER_MINUTES,
        reminderOffsetsMinutes = offsets,
        ringEnabled = true,
        vibrateEnabled = true,
        reminderDeliveryMode = com.example.todoalarm.data.ReminderDeliveryMode.FULLSCREEN,
        countdownEnabled = countdownEnabled,
        checkInEnabled = checkInEnabled,
        recurrence = recurrence,
        groupId = resolvePlanningGroupId(groupName, groups)
    )
}

private fun resolvePlanningGroupId(groupName: String, groups: List<com.example.todoalarm.data.TaskGroup>): Long {
    if (groupName.isNotBlank()) {
        groups.firstOrNull { it.name.equals(groupName.trim(), ignoreCase = true) }?.let { return it.id }
    }
    return groups.firstOrNull { it.name == "例行" }?.id ?: groups.firstOrNull()?.id ?: 0L
}

private fun EventCheckIn.toDesktopJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("eventId", eventId)
        .put("checkInAtMillis", checkInAtMillis)
        .put("checkOutAtMillis", checkOutAtMillis)
        .put("durationMinutes", durationMinutes)
}

private fun EventCheckInCompletionSummary.toDesktopJson(): JSONObject {
    return JSONObject()
        .put("eventId", eventId)
        .put("title", title)
        .put("plannedMinutes", plannedMinutes)
        .put("investedMinutes", investedMinutes)
        .put("checkInCount", checkInCount)
        .put("investmentRatePercent", investmentRatePercent)
        .put("autoCheckedOut", autoCheckedOut)
}
