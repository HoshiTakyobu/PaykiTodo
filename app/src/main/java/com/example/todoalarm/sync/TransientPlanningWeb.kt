package com.example.todoalarm.sync

import com.example.todoalarm.data.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

/** The web editor sends expressions back through the same parser as the phone. */
internal object TransientPlanningWeb {
    fun toJson(result: TransientPlanningResult): JSONObject = JSONObject()
        .put("message", result.message)
        .put("diagnostics", JSONArray(result.diagnostics.map { error -> JSONObject()
            .put("lineNumber", error.lineNumber).put("column", error.column).put("message", error.message) }))
        .put("candidates", JSONArray(result.candidates.map { candidate -> JSONObject()
            .put("id", candidate.id).put("parentId", candidate.parentId ?: JSONObject.NULL)
            .put("lineNumber", candidate.lineNumber).put("title", candidate.title)
            .put("location", candidate.location).put("notes", candidate.notes)
            .put("time", candidate.timeExpression).put("reminders", candidate.reminderExpression)
            .put("range", candidate.rangeExpression).put("groupId", candidate.groupId ?: JSONObject.NULL)
            .put("calendarId", candidate.calendarId).put("type", candidate.type.name)
            .put("absoluteAssignments", JSONArray(if (candidate.reminderMode != PlanningReminderMode.DISABLED && candidate.slots.any { it.recurrence.isRecurring })
                assignPlanningAbsoluteReminders(candidate.slots, candidate.absoluteReminderTimes).map { owner -> JSONObject()
                    .put("start", owner.occurrenceStartAt.toString()).put("slotIndex", owner.slotIndex)
                    .put("reminders", JSONArray(owner.absoluteReminderTimes.map(LocalDateTime::toString))) } else emptyList<JSONObject>()))
            .put("slots", JSONArray(candidate.slots.map { slot -> JSONObject()
                .put("start", slot.startAt.toString()).put("end", slot.endAt.toString())
                .put("recurring", slot.recurrence.isRecurring) })) }))

    fun readEdits(edits: JSONArray, original: List<TransientPlanningCandidate>, now: LocalDateTime): List<TransientPlanningCandidate> {
        val byId = original.associateBy { it.id }
        require(edits.length() in 1..original.size) { "请选择至少一条事项" }
        return (0 until edits.length()).map { index ->
            val edit = edits.getJSONObject(index)
            val source = byId[edit.getString("id")] ?: error("候选已经变化，请重新识别")
            val fields = listOf("time", "reminders", "range").map { edit.optString(it).trim() }.filter(String::isNotEmpty)
            val parsed = TransientPlanningParser.parse("预览事项" + (if (fields.isEmpty()) "" else "," + fields.joinToString(",")) + ";", now)
            require(parsed.diagnostics.isEmpty() && parsed.candidates.size == 1) {
                "第${source.lineNumber}行：${parsed.diagnostics.firstOrNull()?.message ?: "表达式包含额外事项"}"
            }
            parsed.candidates.single().copy(id = source.id, parentId = edit.optString("parentId").takeUnless { it.isBlank() || it == "null" },
                lineNumber = source.lineNumber, column = source.column, sourceLine = source.sourceLine,
                title = edit.getString("title").trim(), notes = edit.optString("notes"), location = edit.optString("location"),
                groupId = edit.optLong("groupId", 0).takeIf { it > 0 }, calendarId = edit.optString("calendarId", DEFAULT_CALENDAR_ID))
                .also { candidate -> candidate.validate()?.let { error("第${source.lineNumber}行：$it") } }
        }
    }
}
