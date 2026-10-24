package com.example.todoalarm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.PlannerCalendar
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

internal data class CalendarParticipant(val username: String, val response: String)
internal data class CalendarParticipantMember(val username: String, val displayName: String)

internal fun calendarParticipantMembers(calendar: PlannerCalendar): List<CalendarParticipantMember> = runCatching {
    val members = JSONArray(calendar.membersJson)
    (0 until members.length()).mapNotNull { index ->
        val member = members.optJSONObject(index) ?: return@mapNotNull null
        val username = member.optString("username").trim().lowercase(Locale.ROOT)
        if (username.isBlank() || member.optString("role").lowercase(Locale.ROOT) !in setOf("owner", "editor", "viewer")) null
        else CalendarParticipantMember(username, member.optString("displayName").trim().ifBlank { username })
    }.distinctBy { it.username }
}.getOrDefault(emptyList())

internal fun calendarParticipants(json: String): List<CalendarParticipant> = runCatching {
    val participants = JSONArray(json)
    (0 until participants.length()).mapNotNull { index ->
        val participant = participants.optJSONObject(index) ?: return@mapNotNull null
        val username = participant.optString("username").trim().lowercase(Locale.ROOT)
        if (username.isBlank()) null else CalendarParticipant(username,
            participant.optString("response").takeIf { it in setOf("ACCEPTED", "DECLINED", "TENTATIVE", "NEEDS_ACTION") } ?: "NEEDS_ACTION")
    }.distinctBy { it.username }
}.getOrDefault(emptyList())

internal fun calendarParticipantSelection(calendar: PlannerCalendar, usernames: Set<String>, previousJson: String): String {
    if (!calendar.id.startsWith("shared:") || calendar.shareMode != "SHARED") return "[]"
    val previous = calendarParticipants(previousJson).associateBy { it.username }
    val result = JSONArray()
    calendarParticipantMembers(calendar).filter { it.username in usernames }.forEach { member ->
        result.put(JSONObject().put("username", member.username).put("response", previous[member.username]?.response ?: "NEEDS_ACTION"))
    }
    return result.toString()
}

internal fun calendarParticipantResponseLabel(response: String): String = when (response) {
    "ACCEPTED" -> "已接受"
    "DECLINED" -> "已拒绝"
    "TENTATIVE" -> "待定"
    else -> "尚未回应"
}

internal fun calendarParticipantResponseFor(json: String, username: String): String? =
    calendarParticipants(json).firstOrNull { it.username == username.trim().lowercase(Locale.ROOT) }?.response

@Composable
internal fun CalendarParticipantResponseChoice(response: String, busy: Boolean = false, onSelect: (String) -> Unit) {
    PreferenceChoice(if (busy) "正在提交回应" else "我的回应", response,
        listOf("NEEDS_ACTION" to "尚未回应", "ACCEPTED" to "接受", "TENTATIVE" to "待定", "DECLINED" to "拒绝")) { selected ->
        if (!busy && selected != "NEEDS_ACTION") onSelect(selected)
    }
}

@Composable
internal fun CalendarParticipantsEditor(calendar: PlannerCalendar, value: String, onChange: (String) -> Unit) {
    if (!calendar.id.startsWith("shared:") || calendar.shareMode != "SHARED") return
    val members = remember(calendar.membersJson) { calendarParticipantMembers(calendar) }
    val allowed = members.map { it.username }.toSet()
    val selected = calendarParticipants(value).filter { it.username in allowed }
    var choosing by remember(calendar.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val organizer = members.firstOrNull { it.username == calendar.ownerAccount.lowercase(Locale.ROOT) }?.displayName ?: calendar.ownerAccount
        Text("组织者 · ${organizer.ifBlank { "暂无信息" }}", style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth().clickable { choosing = true }, verticalAlignment = Alignment.CenterVertically) {
            Text("参与者", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Text(if (selected.isEmpty()) "选择成员" else "${selected.size} 人", color = MaterialTheme.colorScheme.primary)
            Text("  ›", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected.isNotEmpty()) Text(selected.joinToString("、") { participant ->
            members.firstOrNull { it.username == participant.username }?.displayName ?: participant.username
        }, style = MaterialTheme.typography.bodySmall)
        Text("选择当前日历的成员。需要邀请新成员时，请前往日历共享管理。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (choosing) {
        var draftSelection by remember(calendar.id, calendar.membersJson) { mutableStateOf(selected.map { it.username }.toSet()) }
        AlertDialog(onDismissRequest = { choosing = false }, title = { Text("选择参与者") }, text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (members.isEmpty()) Text("当前没有可选择的成员，请刷新日历成员信息。")
                members.forEach { member ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = member.username in draftSelection, onCheckedChange = { checked ->
                            draftSelection = if (checked) draftSelection + member.username else draftSelection - member.username
                        }, modifier = Modifier.semantics { contentDescription = "选择参与者：${member.username}" })
                        Column(Modifier.weight(1f)) {
                            Text(member.displayName, style = MaterialTheme.typography.bodyMedium)
                            Text(member.username, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        selected.firstOrNull { it.username == member.username }?.let {
                            Text(calendarParticipantResponseLabel(it.response), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = {
            onChange(calendarParticipantSelection(calendar, draftSelection, value)); choosing = false
        }, modifier = Modifier.testTag("calendar-participants-confirm")) { Text("完成") } },
            dismissButton = { TextButton(onClick = { choosing = false }, modifier = Modifier.testTag("calendar-participants-cancel")) { Text("取消") } })
    }
}

@Composable
internal fun CalendarParticipantsDetails(calendar: PlannerCalendar, participantsJson: String, restricted: Boolean = false) {
    if (!calendar.id.startsWith("shared:") || calendar.shareMode != "SHARED") return
    val members = remember(calendar.membersJson) { calendarParticipantMembers(calendar).associateBy { it.username } }
    val participants = remember(participantsJson) { calendarParticipants(participantsJson) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val organizer = members[calendar.ownerAccount.lowercase(Locale.ROOT)]?.displayName ?: calendar.ownerAccount
        Text("组织者 · ${organizer.ifBlank { "暂无信息" }}", style = MaterialTheme.typography.titleSmall)
        Text("参与者（${participants.size}）", style = MaterialTheme.typography.titleSmall)
        if (restricted) Text("私密日程 · 仅显示你有权限查看的参与者与回应", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (participants.isEmpty()) Text("暂无可显示的参与者", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        participants.forEach { participant ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(members[participant.username]?.displayName ?: participant.username, style = MaterialTheme.typography.bodyMedium)
                    if (members[participant.username]?.displayName != participant.username) Text(participant.username, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(calendarParticipantResponseLabel(participant.response), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
