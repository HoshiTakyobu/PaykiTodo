package com.example.todoalarm.sync

import android.content.Context
import androidx.room.withTransaction
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.alarm.ReminderForegroundService
import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.BackupArchiveCodec
import com.example.todoalarm.data.BackupSnapshot
import com.example.todoalarm.data.PlannerCalendar
import com.example.todoalarm.data.RecurringTaskTemplate
import com.example.todoalarm.data.RecurringInstanceSkip
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.backupSnapshotFromJson
import com.example.todoalarm.data.calendarZone
import com.example.todoalarm.data.decodeAllDayReminderRules
import com.example.todoalarm.data.toJson
import com.example.todoalarm.data.applyCalendarParticipantResponse
import com.example.todoalarm.widget.TodoWidgetProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID

data class SharedCalendarMember(val username: String, val displayName: String, val role: String)

data class SharedCalendarSummary(
    val localCalendarId: String,
    val id: String,
    val name: String,
    val colorHex: String,
    val description: String,
    val ownerUsername: String,
    val role: String,
    val revision: Long,
    val members: List<SharedCalendarMember>,
    val permissionVersion: Long = 0,
    val canEdit: Boolean = role == "owner" || role == "editor",
    val canManage: Boolean = role == "owner"
)

data class CalendarInvitation(
    val id: String,
    val calendarId: String,
    val calendarName: String,
    val inviterUsername: String,
    val username: String,
    val role: String
)

/** Shares calendar records separately from the encrypted personal snapshot transport. */
class CalendarCollaborationCoordinator(private val app: TodoApplication) {
    private val gate = Mutex()
    private val preferences = app.getSharedPreferences("calendar_collaboration", Context.MODE_PRIVATE)
    private val mutableStatus = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = mutableStatus
    private var periodicJob: Job? = null
    private var pendingJob: Job? = null
    private val dao get() = app.database.todoDao()

    @Synchronized fun start() {
        if (periodicJob?.isActive == true) return
        periodicJob = app.applicationScope.launch {
            while (isActive) {
                if (app.settingsStore.hasValidRemoteSession()) {
                    try { refresh() } catch (error: CancellationException) { throw error }
                    catch (error: Exception) { mutableStatus.value = error.message ?: "共享日历同步失败" }
                }
                delay(15_000)
            }
        }
    }

    @Synchronized fun onLocalDataChanged() {
        start()
        if (pendingJob?.isActive == true || !app.settingsStore.hasValidRemoteSession()) return
        pendingJob = app.applicationScope.launch {
            delay(1_000)
            try { refresh() } catch (error: CancellationException) { throw error }
            catch (error: Exception) { mutableStatus.value = error.message ?: "共享日历同步失败" }
        }
    }

    suspend fun refresh(): List<SharedCalendarSummary> = gate.withLock { refreshLocked() }

    /** Reads cached permissions locally, including when the account token has expired. */
    fun canReadCachedCalendar(calendar: PlannerCalendar): Boolean {
        if (!app.settingsStore.hasLocalAccountAccess()) return false
        if (!calendar.id.startsWith("shared:")) return true
        if (calendar.shareMode != "SHARED" || calendar.sharedRole !in setOf("OWNER", "EDITOR", "VIEWER")) return false
        val settings = app.settingsStore.currentSettings()
        val server = app.remoteSyncCoordinator.effectiveServerUrl().trimEnd('/')
        val scope = "$server|${settings.remoteSyncUserId}|${settings.remoteSyncUsername}"
        return calendar.sharedServerUrl.trimEnd('/') == server &&
            preferences.getString("source:${calendar.id}", "") == scope
    }

    /** Invalidates the cache locally before an account/server change; record contents remain. */
    suspend fun invalidateCachedPermissions() = gate.withLock {
        val settings = app.settingsStore.currentSettings()
        val calendars = dao.getCalendars().filter { it.id.startsWith("shared:") }
        if (calendars.isEmpty()) return@withLock
        val ids = calendars.map { it.id }.toSet()
        if (calendars.any { it.sharedRole != "REVOKED" }) savePermissionSafetyCopy()
        val affected = app.database.withTransaction {
            dao.insertCalendars(calendars.map { it.copy(sharedRole = "REVOKED", shareMode = "REVOKED") })
            val items = dao.getAllTodos().filter { it.calendarId in ids }.map { it.copy(reminderEnabled = false, reminderAtMillis = null) }
            dao.updateAll(items)
            // REVOKED sources are excluded by repository replenishment; original rules remain recoverable.
            items
        }
        val editor = preferences.edit()
        calendars.forEach { calendar ->
            val source = preferences.getString("source:${calendar.id}", "").orEmpty()
            if (source.isNotBlank()) {
                val key = stateKey(source, calendar.id)
                if (!preferences.getBoolean("$key:revoked", false)) editor.putBoolean("$key:hiddenBeforeRevocation", calendar.id in settings.hiddenCalendarIds)
                editor.putBoolean("$key:revoked", true)
            }
        }
        editor.apply()
        affected.forEach {
            app.alarmScheduler.cancel(it.id)
            ReminderForegroundService.dismiss(app, it.id)
        }
        app.settingsStore.updateDisplayPreferences(hiddenCalendars = settings.hiddenCalendarIds + ids)
        TodoWidgetProvider.notifyWidgetDataChanged(app)
    }

    private suspend fun refreshLocked(): List<SharedCalendarSummary> {
        val scope = accountScope()
        val response = try { request("GET", "/v3/calendars") }
        catch (error: CalendarServiceException) {
            if (error.status == 404) throw IllegalStateException("当前服务器尚未提供共享日历，请先更新服务器或选择支持此功能的服务器", error)
            throw error
        }
        requireScope(scope)
        val entries = response.optJSONArray("calendars") ?: error("共享日历响应不完整")
        val metadata = entries.objects()
        val ids = metadata.map { "shared:" + it.getString("id") }.toSet()
        val reacquiring = mutableMapOf<String, Boolean>()
        app.database.withTransaction {
            requireScope(scope)
            val current = dao.getCalendars()
            metadata.forEach { entry ->
                val id = "shared:" + entry.getString("id")
                val existing = current.firstOrNull { it.id == id }
                val previousScope = preferences.getString("source:$id", "").orEmpty()
                val roleChanged = SharedCalendarSyncPolicy.requiresPermissionReload(existing?.sharedRole, entry.optString("role", "viewer"))
                val needsImport = existing?.sharedRole == "REVOKED" || (existing != null && previousScope != scope) || roleChanged
                val calendar = calendarFrom(entry, existing)
                if (needsImport) {
                    if (roleChanged && existing?.sharedRole != "REVOKED") savePermissionSafetyCopy()
                    val previousKey = stateKey(previousScope, id)
                    reacquiring[id] = if (preferences.getBoolean("$previousKey:revoked", false))
                        preferences.getBoolean("$previousKey:hiddenBeforeRevocation", false)
                    else id in app.settingsStore.currentSettings().hiddenCalendarIds
                }
                dao.insertCalendars(listOf(if (needsImport) calendar.copy(sharedRole = "REVOKED", shareMode = "REVOKED") else calendar))
            }
        }
        preferences.edit().also { editor ->
            ids.forEach { editor.putString("source:$it", scope) }
            reacquiring.forEach { (id, hiddenBefore) ->
                val key = stateKey(scope, id)
                editor.putBoolean("$key:revoked", true).putBoolean("$key:hiddenBeforeRevocation", hiddenBefore)
            }
        }.apply()
        if (reacquiring.isNotEmpty()) {
            val settings = app.settingsStore.currentSettings()
            app.settingsStore.updateDisplayPreferences(hiddenCalendars = settings.hiddenCalendarIds + reacquiring.keys)
        }
        dao.getCalendars().filter { it.id.startsWith("shared:") &&
            preferences.getString("source:${it.id}", "") == scope && it.id !in ids && it.sharedRole != "REVOKED"
        }.forEach { markRevoked(it.id, scope) }
        var issue: String? = null
        for (entry in metadata) {
            try { syncLocked("shared:" + entry.getString("id"), scope, allowRevoked = true) }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { issue = error.message ?: "共享日历同步失败" }
        }
        mutableStatus.value = issue
        val local = dao.getCalendars().associateBy { it.id }
        return metadata.map(::summaryFrom).map { summary ->
            if (local[summary.localCalendarId]?.sharedRole == "REVOKED") summary.copy(role = "revoked", canEdit = false, canManage = false) else summary
        }
    }

    suspend fun invitations(): List<CalendarInvitation> = gate.withLock {
        val scope = accountScope()
        val response = request("GET", "/v3/calendar-invitations")
        requireScope(scope)
        response.optJSONArray("invitations").objects().map { entry -> CalendarInvitation(
            id = entry.getString("id"), calendarId = entry.optString("calendarId"),
            calendarName = entry.optString("calendarName"),
            inviterUsername = entry.optString("inviterUsername", entry.optString("ownerUsername")),
            username = entry.optString("username"), role = entry.optString("role", "viewer")
        ) }
    }

    suspend fun createSharedCalendar(name: String, colorHex: String, description: String): String = gate.withLock {
        val scope = accountScope()
        val entry = request("POST", "/v3/calendars", JSONObject().put("name", name)
            .put("colorHex", colorHex).put("description", description)).getJSONObject("calendar")
        requireScope(scope)
        val calendar = calendarFrom(entry, null)
        app.database.withTransaction {
            requireScope(scope)
            dao.insertCalendars(listOf(calendar))
        }
        preferences.edit().putString("source:${calendar.id}", scope).apply()
        syncLocked(calendar.id, scope)
        calendar.id
    }

    suspend fun invite(localCalendarId: String, username: String, role: String): String? = action {
        val scope = accountScope()
        localCalendar(localCalendarId, scope)
        request("POST", calendarPath(localCalendarId) + "/invitations", JSONObject()
            .put("username", username.trim()).put("role", role.lowercase()))
        requireScope(scope)
        refreshLocked()
    }

    suspend fun respond(invitationId: String, accept: Boolean): String? = action {
        val scope = accountScope()
        request("POST", "/v3/calendar-invitations/${segment(invitationId)}/respond", JSONObject().put("accept", accept))
        requireScope(scope)
        refreshLocked()
    }

    suspend fun revoke(localCalendarId: String, username: String): String? = action {
        val scope = accountScope()
        localCalendar(localCalendarId, scope)
        request("DELETE", calendarPath(localCalendarId) + "/members/${segment(username)}")
        requireScope(scope)
        refreshLocked()
    }

    suspend fun updateSharedCalendar(localCalendarId: String, name: String, colorHex: String, description: String): String? = action {
        val scope = accountScope()
        syncLocked(localCalendarId, scope)
        val calendar = localCalendar(localCalendarId, scope)
        require(calendar.sharedRole == "OWNER") { "只有日历所有者可以修改日历设置" }
        request("PATCH", calendarPath(localCalendarId), JSONObject().put("expectedRevision", calendar.sharedRevision)
            .put("name", name).put("colorHex", colorHex).put("description", description))
        requireScope(scope)
        refreshLocked()
    }

    suspend fun deleteSharedCalendar(localCalendarId: String): String? = action {
        val scope = accountScope()
        val calendar = localCalendar(localCalendarId, scope)
        require(calendar.sharedRole == "OWNER") { "只有日历所有者可以删除共享日历" }
        request("DELETE", calendarPath(localCalendarId))
        requireScope(scope)
        markRevoked(localCalendarId, scope)
    }

    suspend fun syncCalendar(localCalendarId: String): String? = action { syncLocked(localCalendarId, accountScope()) }

    suspend fun rsvp(localCalendarId: String, sharedEventId: String, response: String): String? = action {
        val scope = accountScope()
        val calendar = localCalendar(localCalendarId, scope)
        val before = capture(calendar, scope)
        val key = stateKey(scope, localCalendarId)
        val baseline = preferences.getString("$key:fingerprint", null)
        val result = request("POST", calendarPath(localCalendarId) + "/events/${segment(sharedEventId)}/rsvp",
            JSONObject().put("response", response.uppercase()))
        requireScope(scope)
        val username = app.settingsStore.currentSettings().remoteSyncUsername
        val updatedBaseline = app.database.withTransaction {
            val currentCalendar = localCalendar(localCalendarId, scope)
            val current = captureInTransaction(currentCalendar, scope)
            if (current.fingerprint != before.fingerprint) savePermissionSafetyCopy("response-update")
            dao.getAllTodos().filter { it.calendarId == localCalendarId && it.sharedEventId == sharedEventId && it.isEvent }
                .forEach { item -> dao.update(item.copy(eventParticipantsJson =
                    applyCalendarParticipantResponse(item.eventParticipantsJson, username, result.getString("response")))) }
            val after = captureInTransaction(currentCalendar, scope)
            SharedCalendarSyncPolicy.baselineAfterAuthoritativeResponse(baseline, before.fingerprint,
                current.fingerprint, after.fingerprint)
        }
        requireScope(scope)
        if (updatedBaseline != baseline) preferences.edit().putString("$key:fingerprint", updatedBaseline).apply()
        // Keep the old revision: unrelated edits must still conflict instead of overwriting the server.
        try { syncLocked(localCalendarId, scope) }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { throw IllegalStateException("回应已发送；共享日历尚未完成同步：${error.message ?: "请稍后重试"}", error) }
    }

    /** Called only by an explicit, confirmed UI choice; a full safety copy is retained. */
    suspend fun useRemoteVersion(localCalendarId: String): String? = action {
        val scope = accountScope()
        val calendar = localCalendar(localCalendarId, scope)
        val before = capture(calendar, scope)
        val remote = request("GET", calendarPath(localCalendarId) + "/events")
        requireScope(scope)
        saveConflict(localCalendarId, scope, remote, force = true)
        importRemote(calendar, scope, before.fingerprint, remote)
    }

    private suspend fun action(block: suspend () -> Unit): String? = gate.withLock {
        mutableStatus.value = null
        try { block(); null }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { (error.message ?: "共享日历操作失败").also { mutableStatus.value = it } }
    }

    private data class LocalState(val events: JSONArray, val fingerprint: String)

    private suspend fun syncLocked(id: String, scope: String, allowRevoked: Boolean = false) {
        val calendar = localCalendar(id, scope, allowRevoked)
        val before = capture(calendar, scope)
        val remote = try { request("GET", calendarPath(id) + "/events") }
        catch (error: CalendarServiceException) {
            if (error.status == 403 || error.status == 404) markRevoked(id, scope)
            throw error
        }
        requireScope(scope)
        val revision = remote.getLong("revision")
        val key = stateKey(scope, id)
        val baseline = preferences.getString("$key:fingerprint", null)
        val baselineRevision = preferences.getLong("$key:revision", -1)
        val decision = SharedCalendarSyncPolicy.decide(baseline, baselineRevision, before.fingerprint,
            revision, before.events.length() == 0, remote.getJSONArray("events").length() == 0,
            remote.optBoolean("canEdit", calendar.sharedRole != "VIEWER"), preferences.getBoolean("$key:revoked", false))
        when (decision) {
            SharedCalendarSyncPolicy.Decision.CONFLICT -> {
                saveConflict(id, scope, remote)
                error("共享日历“${calendar.name}”的本机和云端均有修改，已保留安全备份，请检查冲突后选择版本")
            }
            SharedCalendarSyncPolicy.Decision.IMPORT -> importRemote(calendar, scope, before.fingerprint, remote)
            SharedCalendarSyncPolicy.Decision.UPLOAD -> {
                app.database.withTransaction {
                    requireScope(scope)
                    require(captureInTransaction(calendar, scope).fingerprint == before.fingerprint) { "本机日程正在变化，请稍后重试同步" }
                }
                val result = try { request("PUT", calendarPath(id) + "/events", JSONObject()
                    .put("expectedRevision", revision).put("events", before.events)) }
                catch (error: CalendarServiceException) {
                    if (error.status == 409) {
                        val latest = request("GET", calendarPath(id) + "/events")
                        saveConflict(id, scope, latest)
                    }
                    throw error
                }
                requireScope(scope)
                app.database.withTransaction {
                    requireScope(scope)
                    val current = localCalendar(id, scope)
                    dao.insertCalendars(listOf(current.copy(sharedRevision = result.getLong("revision"))))
                }
                requireScope(scope)
                // The uploaded fingerprint, rather than current rows, leaves in-flight edits dirty.
                preferences.edit().putString("$key:fingerprint", before.fingerprint)
                    .putLong("$key:revision", result.getLong("revision"))
                    .putString("$key:wire", before.events.toString()).remove("$key:conflict").apply()
            }
            SharedCalendarSyncPolicy.Decision.CLEAN -> Unit
        }
    }

    private suspend fun capture(calendar: PlannerCalendar, scope: String): LocalState = app.database.withTransaction {
        requireScope(scope)
        captureInTransaction(calendar, scope)
    }

    private suspend fun captureInTransaction(calendar: PlannerCalendar, scope: String,
                                            wireOverride: JSONArray? = null, removedOverride: Set<String>? = null): LocalState {
        val key = stateKey(scope, calendar.id)
        val removed = removedOverride ?: preferences.getStringSet("$key:removed", emptySet()).orEmpty()
        val tasks = dao.getAllTodos().filter { it.calendarId == calendar.id && it.isEvent }.map { item ->
            if (item.sharedEventId.isNotBlank()) item else item.copy(sharedEventId = SharedCalendarWireCodec.eventId(item)).also { dao.update(it) }
        }.filterNot { it.sharedEventId in removed && it.canceled }
        val templates = dao.getAllRecurringTemplates().filter { it.calendarId == calendar.id && it.recurrenceType != "NONE" }
        val seriesIds = templates.map { it.seriesId }.toSet()
        val skips = dao.getAllRecurringSkips().filter { it.seriesId in seriesIds }
        val cachedWire = wireOverride ?: runCatching { JSONArray(preferences.getString("$key:wire", "[]")) }.getOrDefault(JSONArray())
        val events = SharedCalendarWireCodec.encode(tasks, templates, cachedWire, skips)
        return LocalState(events, SharedCalendarWireCodec.fingerprint(events))
    }

    private suspend fun importRemote(calendar: PlannerCalendar, scope: String, expectedFingerprint: String, remote: JSONObject) {
        val key = stateKey(scope, calendar.id)
        val events = remote.getJSONArray("events")
        val result = app.database.withTransaction {
            requireScope(scope)
            require(captureInTransaction(calendar, scope).fingerprint == expectedFingerprint) { "本机日程正在变化，已保留本机修改，请重试同步" }
            val current = dao.getAllTodos().filter { it.calendarId == calendar.id }
            val existing = current.associateBy { it.sharedEventId }
            val templates = dao.getAllRecurringTemplates().filter { it.calendarId == calendar.id }
            val groupId = dao.getAllGroups().firstOrNull { it.isDefault }?.id ?: 0
            val parsed = SharedCalendarWireCodec.decode(events, calendar.id, groupId)
            val globalTemplates = dao.getAllRecurringTemplates().associateBy { it.seriesId }
            require(parsed.templates.all { globalTemplates[it.seriesId]?.calendarId.let { owner -> owner == null || owner == calendar.id } }) {
                "循环日程标识与其他日历冲突，已保留本机数据，请检查日历来源"
            }
            val imported = parsed.tasks.map { value ->
                val old = existing[value.sharedEventId]
                // Check-in history and device-specific reminder channels belong to this local copy.
                value.copy(id = old?.id ?: 0, totalCheckInMinutes = old?.totalCheckInMinutes ?: 0)
            }
            val affected = mutableListOf<TodoItem>()
            imported.forEach { item ->
                val inserted = if (item.id == 0L) item.copy(id = dao.insert(item)) else item.also { dao.update(it) }
                affected += inserted
            }
            val present = imported.map { it.sharedEventId }.toSet()
            val removed = current.filter { it.sharedEventId !in present }.map { item ->
                item.copy(canceled = true, canceledAtMillis = item.canceledAtMillis ?: System.currentTimeMillis(),
                    reminderEnabled = false, reminderAtMillis = null)
            }
            dao.updateAll(removed)
            affected += removed
            val bySeries = templates.associateBy { it.seriesId }
            parsed.templates.forEach { master ->
                val old = bySeries[master.seriesId]
                if (old == null) dao.insertTemplate(master.copy(id = 0)) else dao.updateTemplate(master.copy(id = old.id))
            }
            val activeSeries = parsed.templates.map { it.seriesId }.toSet()
            templates.filter { it.seriesId !in activeSeries }.forEach { master -> dao.updateTemplate(master.copy(
                recurrenceType = "NONE", recurrenceNeverEnds = false, endEpochDay = master.startEpochDay - 1)) }
            (templates.map { it.seriesId } + activeSeries).toSet().forEach { dao.deleteRecurringSkipsBySeriesId(it) }
            dao.insertRecurringSkips(parsed.skips)
            val role = remote.optString("role", calendar.sharedRole).uppercase()
            dao.insertCalendars(listOf(calendar.copy(sharedRevision = remote.getLong("revision"), sharedRole = role, shareMode = "SHARED")))
            val removedIds = removed.map { it.sharedEventId }.toSet()
            val after = captureInTransaction(calendar, scope, events, removedIds)
            Triple(affected, after.fingerprint, removedIds)
        }
        requireScope(scope)
        val wasRevoked = preferences.getBoolean("$key:revoked", false)
        preferences.edit().putString("$key:wire", events.toString())
            .putStringSet("$key:removed", result.third).putString("$key:fingerprint", result.second)
            .putLong("$key:revision", remote.getLong("revision")).remove("$key:conflict").remove("$key:revoked").apply()
        if (wasRevoked && !preferences.getBoolean("$key:hiddenBeforeRevocation", false)) {
            val settings = app.settingsStore.currentSettings()
            app.settingsStore.updateDisplayPreferences(hiddenCalendars = settings.hiddenCalendarIds - calendar.id)
        }
        result.first.forEach { item ->
            requireScope(scope)
            val current = dao.getById(item.id) ?: return@forEach
            if (current.calendarId != calendar.id) return@forEach
            app.alarmScheduler.cancel(current.id)
            if (!current.completed && !current.canceled && current.reminderEnabled) app.alarmScheduler.schedule(current)
            else ReminderForegroundService.dismiss(app, current.id)
        }
        TodoWidgetProvider.notifyWidgetDataChanged(app)
    }

    private suspend fun markRevoked(id: String, scope: String) {
        requireScope(scope)
        if (dao.getCalendars().firstOrNull { it.id == id }?.sharedRole != "REVOKED") savePermissionSafetyCopy()
        val affected = app.database.withTransaction {
            requireScope(scope)
            val calendar = dao.getCalendars().firstOrNull { it.id == id } ?: return@withTransaction emptyList<TodoItem>()
            dao.insertCalendars(listOf(calendar.copy(sharedRole = "REVOKED", shareMode = "REVOKED")))
            val tasks = dao.getAllTodos().filter { it.calendarId == id }.map { it.copy(reminderEnabled = false, reminderAtMillis = null) }
            dao.updateAll(tasks)
            tasks
        }
        affected.forEach {
            app.alarmScheduler.cancel(it.id)
            ReminderForegroundService.dismiss(app, it.id)
        }
        val settings = app.settingsStore.currentSettings()
        val key = stateKey(scope, id)
        preferences.edit().putBoolean("$key:revoked", true)
            .putBoolean("$key:hiddenBeforeRevocation", id in settings.hiddenCalendarIds).apply()
        app.settingsStore.updateDisplayPreferences(hiddenCalendars = settings.hiddenCalendarIds + id)
        TodoWidgetProvider.notifyWidgetDataChanged(app)
    }

    private suspend fun savePermissionSafetyCopy(reason: String = "permission-change") {
        val snapshot = app.database.withTransaction { app.repository.exportSnapshot(app.settingsStore.currentSettings()) }
        val directory = File(app.filesDir, "shared-calendar-conflicts").apply { mkdirs() }
        val file = File(directory, "before-$reason-${System.currentTimeMillis()}-${UUID.randomUUID()}.paykibak")
        file.outputStream().use { BackupArchiveCodec.write(snapshot, it) }
    }

    private suspend fun saveConflict(id: String, scope: String, remote: JSONObject, force: Boolean = false) {
        requireScope(scope)
        val key = stateKey(scope, id)
        val revision = remote.optLong("revision", -1)
        if (force || preferences.getLong("$key:conflictRevision", -2) != revision) {
            val snapshot = app.database.withTransaction { requireScope(scope); app.repository.exportSnapshot(app.settingsStore.currentSettings()) }
            val directory = File(app.filesDir, "shared-calendar-conflicts").apply { mkdirs() }
            val prefix = "${key.take(20)}-${System.currentTimeMillis()}"
            File(directory, "$prefix.paykibak").outputStream().use { BackupArchiveCodec.write(snapshot, it) }
            File(directory, "$prefix.remote.json").writeText(remote.toString(), Charsets.UTF_8)
        }
        requireScope(scope)
        preferences.edit().putLong("$key:conflictRevision", revision).putString("$key:conflict", remote.toString()).apply()
    }

    private suspend fun localCalendar(id: String, scope: String, allowRevoked: Boolean = false): PlannerCalendar {
        requireScope(scope)
        require(id.startsWith("shared:")) { "请选择共享日历" }
        val calendar = dao.getCalendars().firstOrNull { it.id == id } ?: error("共享日历不存在")
        require(calendar.sharedServerUrl.trimEnd('/') == app.remoteSyncCoordinator.effectiveServerUrl().trimEnd('/') &&
            preferences.getString("source:$id", "") == scope) { "该日历属于其他账号或服务器，请刷新当前账号的日历" }
        require(allowRevoked || calendar.sharedRole != "REVOKED") { "你已没有此日历的访问权限" }
        return calendar
    }

    private fun accountScope(): String {
        require(app.settingsStore.hasLocalAccountAccess()) { "请先登录账号" }
        val settings = app.settingsStore.currentSettings()
        return "${app.remoteSyncCoordinator.effectiveServerUrl().trimEnd('/')}|${settings.remoteSyncUserId}|${settings.remoteSyncUsername}"
    }

    private fun requireScope(expected: String) { require(accountScope() == expected) { "账号或登录服务器已变更，请重试" } }
    private fun stateKey(scope: String, id: String) = SharedCalendarWireCodec.hash("$scope|$id")
    private suspend fun request(method: String, path: String, body: JSONObject? = null): JSONObject {
        try { return app.remoteSyncCoordinator.calendarRequest(method, path, body) }
        catch (error: CalendarServiceException) {
            if (error.status == 404 && (path == "/v3/calendars" || path == "/v3/calendar-invitations")) {
                throw CalendarServiceException(404, -1, "当前服务器尚未提供共享日历，请先更新服务器或选择支持此功能的服务器")
            }
            throw error
        }
    }
    private fun calendarPath(id: String) = "/v3/calendars/${segment(id.removePrefix("shared:"))}"
    private fun segment(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun calendarFrom(entry: JSONObject, old: PlannerCalendar?): PlannerCalendar = PlannerCalendar(
        id = "shared:" + entry.getString("id"), name = entry.getString("name"),
        colorHex = entry.optString("colorHex", "#3370FF"), sortOrder = old?.sortOrder ?: 100,
        description = entry.optString("description"), ownerAccount = entry.optString("ownerUsername"),
        shareMode = "SHARED", membersJson = entry.optJSONArray("members")?.toString() ?: "[]",
        sharedRole = entry.optString("role", "viewer").uppercase(), sharedRevision = old?.sharedRevision ?: 0,
        sharedServerUrl = app.remoteSyncCoordinator.effectiveServerUrl().trimEnd('/')
    )

    private fun summaryFrom(entry: JSONObject) = SharedCalendarSummary(
        localCalendarId = "shared:" + entry.getString("id"), id = entry.getString("id"), name = entry.getString("name"),
        colorHex = entry.optString("colorHex", "#3370FF"), description = entry.optString("description"),
        ownerUsername = entry.optString("ownerUsername"), role = entry.optString("role", "viewer"),
        revision = entry.optLong("revision"), permissionVersion = entry.optLong("permissionVersion"),
        members = entry.optJSONArray("members").objects().map { SharedCalendarMember(it.optString("username"),
            it.optString("displayName"), it.optString("role", "viewer")) }
    )
}

internal object SharedCalendarSyncPolicy {
    enum class Decision { CLEAN, IMPORT, UPLOAD, CONFLICT }
    fun requiresPermissionReload(previousRole: String?, remoteRole: String): Boolean =
        previousRole != null && previousRole.uppercase() != remoteRole.uppercase()
    fun baselineAfterAuthoritativeResponse(baseline: String?, before: String, current: String, after: String): String? =
        if (baseline != null && baseline == before && before == current) after else baseline
    fun decide(baseline: String?, baselineRevision: Long, local: String, remoteRevision: Long,
               localEmpty: Boolean, remoteEmpty: Boolean, canEdit: Boolean, wasRevoked: Boolean = false): Decision {
        if (wasRevoked) return Decision.IMPORT
        if (baseline == null) return when {
            localEmpty -> Decision.IMPORT
            remoteEmpty && canEdit -> Decision.UPLOAD
            else -> Decision.CONFLICT
        }
        val localChanged = local != baseline
        val remoteChanged = remoteRevision != baselineRevision
        return when {
            localChanged && (remoteChanged || !canEdit) -> Decision.CONFLICT
            remoteChanged -> Decision.IMPORT
            localChanged -> Decision.UPLOAD
            else -> Decision.CLEAN
        }
    }
}

internal object SharedCalendarWireCodec {
    fun eventId(item: TodoItem): String = if (!item.recurringSeriesId.isNullOrBlank()) {
        "${item.recurringSeriesId}:${item.recurrenceAnchorDueAtMillis ?: item.startAtMillis ?: item.dueAtMillis}"
    } else UUID.randomUUID().toString()

    fun encode(tasks: List<TodoItem>, templates: List<RecurringTaskTemplate>, cached: JSONArray = JSONArray(),
               skips: List<RecurringInstanceSkip> = emptyList()): JSONArray {
        val json = BackupSnapshot(0, emptyList(), templates, tasks, settings = AppSettings()).toJson()
        val masters = json.getJSONArray("templates").objects().associateBy { it.getString("seriesId") }
        val previous = cached.objects().associateBy { it.getString("id") }
        val emitted = mutableSetOf<String>()
        return JSONArray(json.getJSONArray("tasks").objects().sortedBy { it.getString("sharedEventId") }.map { item ->
            val id = item.getString("sharedEventId")
            val result = previous[id]?.let { JSONObject(it.toString()) } ?: JSONObject()
            item.keys().forEach { result.put(it, item.get(it)) }
            result.put("id", id)
            listOf("calendarId", "groupId", "sharedEventId", "quickChecklistGroupId", "parentTodoId",
                "quickChecklistDepth", "quickChecklistSortOrder", "quickChecklistCollapsed").forEach(result::remove)
            result.put("participants", JSONArray(item.optString("eventParticipantsJson", "[]")))
            result.remove("eventParticipantsJson")
            result.remove("template")
            val series = item.optString("recurringSeriesId").takeUnless { it.isBlank() || it == "null" }
            if (series != null && emitted.add(series)) masters[series]?.let { master ->
                val value = previous[id]?.optJSONObject("template")?.let { JSONObject(it.toString()) } ?: JSONObject()
                master.keys().forEach { value.put(it, master.get(it)) }
                listOf("id", "calendarId", "groupId").forEach(value::remove)
                value.put("instanceSkips", JSONArray(skips.filter { it.seriesId == series }
                    .map { it.instanceEpochDay }.distinct().sorted()))
                result.put("template", value)
            }
            result
        })
    }

    data class Decoded(val tasks: List<TodoItem>, val templates: List<RecurringTaskTemplate>, val skips: List<RecurringInstanceSkip>)

    fun decode(events: JSONArray, calendarId: String, groupId: Long): Decoded {
        val tasks = JSONArray()
        val templates = LinkedHashMap<String, JSONObject>()
        val seen = mutableSetOf<String>()
        events.objects().forEach { source ->
            val wireId = source.getString("id")
            require(wireId.isNotBlank() && seen.add(wireId)) { "共享日程标识重复或无效" }
            validateCalendarFields(source)
            val item = JSONObject(source.toString()).put("id", 0).put("sharedEventId", wireId)
                .put("calendarId", calendarId).put("groupId", groupId).put("itemType", "EVENT")
                .put("eventParticipantsJson", source.optJSONArray("participants")?.toString() ?: "[]")
            if (source.optBoolean("redacted", false)) {
                item.put("notes", "").put("location", "").put("reminderEnabled", false)
                    .put("reminderAtMillis", JSONObject.NULL).put("recurringSeriesId", JSONObject.NULL).put("recurrenceType", "NONE")
            } else source.optJSONObject("template")?.let { master ->
                validateCalendarFields(master)
                val value = JSONObject(master.toString()).put("id", 0).put("calendarId", calendarId).put("groupId", groupId)
                templates[value.getString("seriesId")] = value
            }
            tasks.put(item)
        }
        val snapshot = backupSnapshotFromJson(JSONObject().put("tasks", tasks).put("templates", JSONArray(templates.values.toList())))
        val skips = templates.values.flatMap { master ->
            val days = master.optJSONArray("instanceSkips") ?: JSONArray()
            (0 until days.length()).map { days.getLong(it) }.distinct().map { day ->
                java.time.LocalDate.ofEpochDay(day)
                RecurringInstanceSkip(seriesId = master.getString("seriesId"), instanceEpochDay = day)
            }
        }
        return Decoded(snapshot.tasks, snapshot.templates, skips)
    }

    private fun validateCalendarFields(value: JSONObject) {
        calendarZone(value.optString("eventTimeZoneId", ""))
        val rules = value.optString("allDayReminderRulesJson", "")
        if (rules.isNotBlank()) decodeAllDayReminderRules(rules)
    }

    fun fingerprint(events: JSONArray): String = hash(canonical(events))
    fun hash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun canonical(value: Any?): String = when (value) {
        null, JSONObject.NULL -> "null"
        is JSONObject -> value.keys().asSequence().toList().sorted().joinToString(",", "{", "}") { JSONObject.quote(it) + ":" + canonical(value.get(it)) }
        is JSONArray -> (0 until value.length()).joinToString(",", "[", "]") { canonical(value.get(it)) }
        is String -> JSONObject.quote(value)
        else -> value.toString()
    }
}

private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else
    (0 until length()).map { getJSONObject(it) }
