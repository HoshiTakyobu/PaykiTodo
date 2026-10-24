package com.example.todoalarm.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Participant selection does not grant an editor authority over another person's response. */
internal fun mergeCalendarParticipantResponses(requestedJson: String, existingJson: String = "[]"): String {
    fun rows(json: String): List<Pair<String, String>> {
        val array = JSONArray(json.ifBlank { "[]" })
        require(array.length() <= 128) { "日程参与者数量过多" }
        return (0 until array.length()).map { index ->
            val value = array.get(index)
            val username = when (value) {
                is JSONObject -> value.getString("username")
                is String -> value
                else -> error("日程参与者数据无效")
            }.trim().lowercase(Locale.ROOT)
            require(username.isNotBlank() && username.length <= 64) { "日程参与者账号无效" }
            username to ((value as? JSONObject)?.optString("response", "NEEDS_ACTION") ?: "NEEDS_ACTION")
        }.distinctBy { it.first }
    }
    val previous = rows(existingJson).toMap()
    return JSONArray(rows(requestedJson).map { (username, _) ->
        JSONObject().put("username", username).put("response", previous[username] ?: "NEEDS_ACTION")
    }).toString()
}

internal fun resetCalendarParticipantResponses(json: String): String = mergeCalendarParticipantResponses(json)

/** Apply a server-confirmed response for this account without changing other participants. */
internal fun applyCalendarParticipantResponse(json: String, username: String, response: String): String {
    require(response in setOf("ACCEPTED", "DECLINED", "TENTATIVE")) { "日程回应无效" }
    val account = username.trim().lowercase(Locale.ROOT)
    require(account.isNotBlank()) { "日程回应账号无效" }
    val participants = JSONArray(json.ifBlank { "[]" })
    val matching = (0 until participants.length()).map { participants.getJSONObject(it) }
        .firstOrNull { it.getString("username").trim().lowercase(Locale.ROOT) == account }
    if (matching != null) matching.put("response", response)
    else participants.put(JSONObject().put("username", account).put("response", response))
    return participants.toString()
}
