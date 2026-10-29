package com.example.todoalarm.data

import org.json.JSONArray
import org.json.JSONObject

/** Three-way reconciliation uses an acknowledged copy, never export time as edit authority. */
internal object PersonalSnapshotMerge {
    private val lifecycle = setOf("completed", "canceled", "completedAtMillis", "canceledAtMillis")
    private val ignored = setOf("exportedAtMillis", "reminderChainLogs")

    fun same(left: BackupSnapshot, right: BackupSnapshot): Boolean =
        stable(left.personalSyncSnapshot().toJson()) == stable(right.personalSyncSnapshot().toJson())

    fun fingerprint(snapshot: BackupSnapshot): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(stable(snapshot.personalSyncSnapshot().toJson()).toByteArray()).joinToString("") { "%02x".format(it) }

    fun merge(base: BackupSnapshot, local: BackupSnapshot, remote: BackupSnapshot): BackupSnapshot? = runCatching {
        val b = base.personalSyncSnapshot().toJson()
        val l = local.personalSyncSnapshot().toJson()
        val r = remote.personalSyncSnapshot().toJson()
        val result = JSONObject(l.toString())
        for (key in keys(b, l, r) - ignored) {
            val bv = b.opt(key); val lv = l.opt(key); val rv = r.opt(key)
            result.put(key, when {
                key == "settings" -> mergeFields(bv as JSONObject, lv as JSONObject, rv as JSONObject)
                bv is JSONArray && lv is JSONArray && rv is JSONArray -> mergeRows(key, bv, lv, rv)
                else -> choose(bv, lv, rv)
            })
        }
        val merged = backupSnapshotFromJson(result).copy(reminderChainLogs = local.reminderChainLogs)
        validateReferences(merged)
        merged
    }.getOrNull()

    private fun mergeRows(table: String, base: JSONArray, local: JSONArray, remote: JSONArray): JSONArray {
        fun indexed(rows: JSONArray): Map<String, JSONObject> {
            val entries = (0 until rows.length()).map { rows.getJSONObject(it) }
            val index = entries.associateBy { row ->
                if (table == "todoGroupTags") "${row.getLong("todoId")}:${row.getLong("groupId")}"
                else canonical(row.get("id"))
            }
            require(index.size == entries.size) { "Duplicate row identity" }
            return index
        }
        val b = indexed(base); val l = indexed(local); val r = indexed(remote)
        val result = JSONArray()
        for (id in (b.keys + l.keys + r.keys).sorted()) {
            val bv = b[id]; val lv = l[id]; val rv = r[id]
            val row = if (table == "tasks" && bv != null && lv != null && rv != null &&
                !equal(lv, bv) && !equal(rv, bv) && !equal(lv, rv)) mergeTask(bv, lv, rv)
            else choose(bv, lv, rv)
            if (row != null) result.put(row)
        }
        return result
    }

    private fun mergeTask(base: JSONObject, local: JSONObject, remote: JSONObject): JSONObject {
        fun section(row: JSONObject, fields: Set<String>) = JSONObject().apply {
            fields.sorted().forEach { key -> if (row.has(key)) put(key, row.get(key)) }
        }
        val fields = keys(base, local, remote)
        val content = fields - lifecycle - setOf("reminderEnabled", "missed", "missedAtMillis")
        // A completion and a snooze affect independent sections. Changes to the
        // same schedule/editor section remain a conflict, including delete/edit.
        val result = JSONObject((choose(section(base, content), section(local, content), section(remote, content)) as JSONObject).toString())
        val sameTerminal = (local.optBoolean("completed") || local.optBoolean("canceled")) &&
            local.optBoolean("completed") == remote.optBoolean("completed") && local.optBoolean("canceled") == remote.optBoolean("canceled")
        val state = if (sameTerminal) section(local, lifecycle).apply {
            for (key in listOf("completedAtMillis", "canceledAtMillis")) {
                val times = listOf(local.optLong(key), remote.optLong(key)).filter { it > 0L }
                if (times.isNotEmpty()) put(key, times.min())
            }
        } else choose(section(base, lifecycle), section(local, lifecycle), section(remote, lifecycle)) as JSONObject
        lifecycle.forEach { if (state.has(it)) result.put(it, state.get(it)) }
        val finished = result.optBoolean("completed") || result.optBoolean("canceled")
        result.put("reminderEnabled", if (finished) false else choose(base.opt("reminderEnabled"), local.opt("reminderEnabled"), remote.opt("reminderEnabled")))
        if (finished) { result.put("missed", false); result.remove("missedAtMillis") }
        else {
            result.put("missed", choose(base.opt("missed"), local.opt("missed"), remote.opt("missed")))
            if (result.optBoolean("missed")) result.put("missedAtMillis", maxOf(local.optLong("missedAtMillis"), remote.optLong("missedAtMillis")))
        }
        return result
    }

    private fun mergeFields(base: JSONObject, local: JSONObject, remote: JSONObject) = JSONObject().apply {
        for (key in keys(base, local, remote)) put(key, choose(base.opt(key), local.opt(key), remote.opt(key)))
    }

    private fun choose(base: Any?, local: Any?, remote: Any?): Any? = when {
        equal(local, remote) -> local
        equal(local, base) -> remote
        equal(remote, base) -> local
        else -> error("Both copies changed the same value")
    }

    private fun validateReferences(snapshot: BackupSnapshot) {
        val tasks = snapshot.tasks.map { it.id }.toSet()
        val groups = snapshot.groups.map { it.id }.toSet()
        val calendars = snapshot.calendars.map { it.id }.toSet()
        val checklists = snapshot.quickChecklistGroups.map { it.id }.toSet()
        val notes = snapshot.planningNotes.map { it.id }.toSet()
        val nodes = snapshot.planningNodes.map { it.id }.toSet()
        require(snapshot.tasks.all { (it.groupId == 0L || it.groupId in groups) && it.calendarId in calendars &&
            (it.parentTodoId == null || it.parentTodoId in tasks) && (it.quickChecklistGroupId == null || it.quickChecklistGroupId in checklists) })
        require(snapshot.templates.all { (it.groupId == 0L || it.groupId in groups) && it.calendarId in calendars })
        require(snapshot.todoGroupTags.all { it.todoId in tasks && it.groupId in groups })
        require(snapshot.eventCheckIns.all { it.eventId in tasks })
        require(snapshot.planningNodes.all { it.noteId in notes && (it.parentNodeId == null || it.parentNodeId in nodes) &&
            (it.linkedTodoId == null || it.linkedTodoId in tasks) && (it.linkedEndTodoId == null || it.linkedEndTodoId in tasks) })
    }

    private fun keys(vararg objects: JSONObject) = objects.flatMap { it.keys().asSequence().toList() }.toSet()
    private fun equal(a: Any?, b: Any?) = canonical(a) == canonical(b)
    private fun stable(json: JSONObject): String = keys(json).filterNot { it in ignored }.sorted().joinToString(",", "{", "}") { key ->
        val value = json.opt(key)
        JSONObject.quote(key) + ":" + if (value is JSONArray) (0 until value.length()).map { canonical(value.opt(it)) }.sorted().joinToString(",", "[", "]") else canonical(value)
    }
    private fun canonical(value: Any?): String = when (value) {
        null, JSONObject.NULL -> "null"
        is JSONObject -> keys(value).sorted().joinToString(",", "{", "}") { JSONObject.quote(it) + ":" + canonical(value.opt(it)) }
        is JSONArray -> (0 until value.length()).joinToString(",", "[", "]") { canonical(value.opt(it)) }
        is String -> JSONObject.quote(value)
        else -> value.toString()
    }
}
