package com.example.todoalarm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.sync.CalendarInvitation
import com.example.todoalarm.sync.SharedCalendarSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private fun sharedRoleLabel(role: String): String = when (role.lowercase()) { "owner" -> "所有者"; "editor" -> "可编辑"; "revoked" -> "访问已撤销"; else -> "只读" }

@Composable
internal fun SharedCalendarManager(onDismiss: () -> Unit) {
    val app = LocalContext.current.applicationContext as TodoApplication
    val coordinator = app.calendarCollaboration
    val scope = rememberCoroutineScope()
    val status by coordinator.status.collectAsState()
    var calendars by remember { mutableStateOf<List<SharedCalendarSummary>>(emptyList()) }
    var invitations by remember { mutableStateOf<List<CalendarInvitation>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SharedCalendarSummary?>(null) }
    var inviting by remember { mutableStateOf<SharedCalendarSummary?>(null) }
    var revoking by remember { mutableStateOf<Pair<SharedCalendarSummary, String>?>(null) }
    var deleting by remember { mutableStateOf<SharedCalendarSummary?>(null) }
    var remoteChoice by remember { mutableStateOf<SharedCalendarSummary?>(null) }
    suspend fun reload() { calendars = coordinator.refresh(); invitations = coordinator.invitations() }
    fun action(operation: suspend () -> String?) {
        if (busy) return
        busy = true; error = null
        scope.launch {
            try { error = operation(); if (error == null) reload() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: "共享日历操作失败" }
            finally { busy = false }
        }
    }
    LaunchedEffect(Unit) { action { null } }
    SettingsSectionDialog("共享日历", onDismiss) {
        Text("共享日程由你选择的服务器保存，成员按照只读或编辑权限访问。个人日历继续使用账号的加密同步。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(enabled = !busy, onClick = { creating = true }) { Text("创建共享日历") }
            TextButton(enabled = !busy, onClick = { action { null } }) { Text("刷新") }
        }
        if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        (error ?: status)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (invitations.isNotEmpty()) {
            Text("待处理邀请", style = MaterialTheme.typography.titleMedium)
            invitations.forEach { invitation ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(invitation.calendarName, style = MaterialTheme.typography.titleSmall)
                    Text("${invitation.inviterUsername} 邀请你加入 · ${sharedRoleLabel(invitation.role)}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(enabled = !busy, onClick = { action { coordinator.respond(invitation.id, true) } }) { Text("接受") }
                        TextButton(enabled = !busy, onClick = { action { coordinator.respond(invitation.id, false) } }) { Text("拒绝") }
                    }
                }
                HorizontalDivider()
            }
        }
        calendars.forEach { calendar ->
            Text(calendar.name, style = MaterialTheme.typography.titleMedium)
            Text("${sharedRoleLabel(calendar.role)} · 所有者 ${calendar.ownerUsername} · 云端版本 ${calendar.revision}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (calendar.description.isNotBlank()) Text(calendar.description, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !busy, onClick = { action { coordinator.syncCalendar(calendar.localCalendarId) } }) { Text("同步") }
                if (calendar.canManage) {
                    TextButton(enabled = !busy, onClick = { editing = calendar }) { Text("编辑") }
                    TextButton(enabled = !busy, onClick = { inviting = calendar }) { Text("邀请成员") }
                }
            }
            calendar.members.forEach { member ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${member.displayName.ifBlank { member.username }} · ${sharedRoleLabel(member.role)}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    if (calendar.canManage && member.role.lowercase() != "owner") TextButton(enabled = !busy, onClick = { revoking = calendar to member.username }) { Text("移除") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if ((error ?: status).orEmpty().contains("冲突") && calendar.canEdit) TextButton(enabled = !busy, onClick = { remoteChoice = calendar }) { Text("使用云端版本") }
                if (calendar.canManage) TextButton(enabled = !busy, onClick = { deleting = calendar }) { Text("删除共享日历", color = MaterialTheme.colorScheme.error) }
            }
            HorizontalDivider()
        }
        if (!busy && error == null && calendars.isEmpty()) Text("暂无共享日历。可以创建一个，或接受其他账号的邀请。", style = MaterialTheme.typography.bodyMedium)
    }
    if (creating || editing != null) {
        val source = editing
        var name by remember(source?.id) { mutableStateOf(source?.name.orEmpty()) }
        var color by remember(source?.id) { mutableStateOf(source?.colorHex ?: "#3370FF") }
        var description by remember(source?.id) { mutableStateOf(source?.description.orEmpty()) }
        AlertDialog(onDismissRequest = { if (!busy) { creating = false; editing = null } }, title = { Text(if (source == null) "创建共享日历" else "编辑共享日历") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("日历名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth())
                PreferenceChoice("颜色", color, listOf("#3370FF" to "蓝色", "#16A369" to "绿色", "#DB5298" to "粉色", "#BD6B17" to "橙色", "#7955CC" to "紫色", "#D94646" to "红色")) { color = it }
                if (source == null) Text("创建后，放入这个日历的日程将保存在当前服务器，并向你授权的成员开放。", style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(enabled = !busy && name.isNotBlank(), onClick = { action {
            val result = if (source == null) { coordinator.createSharedCalendar(name.trim(), color, description.trim()); null } else coordinator.updateSharedCalendar(source.localCalendarId, name.trim(), color, description.trim())
            if (result == null) { creating = false; editing = null }; result
        } }) { Text("完成") } }, dismissButton = { TextButton(enabled = !busy, onClick = { creating = false; editing = null }) { Text("取消") } })
    }
    inviting?.let { calendar ->
        var username by remember(calendar.id) { mutableStateOf("") }
        var role by remember(calendar.id) { mutableStateOf("viewer") }
        AlertDialog(onDismissRequest = { if (!busy) inviting = null }, title = { Text("邀请成员") }, text = {
            Column {
                OutlinedTextField(username, { username = it }, label = { Text("对方的账号") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                PreferenceChoice("权限", role, listOf("viewer" to "只读", "editor" to "可编辑")) { role = it }
                Text("对方接受邀请后才能访问“${calendar.name}”。", style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(enabled = !busy && username.isNotBlank(), onClick = { action { coordinator.invite(calendar.localCalendarId, username.trim(), role).also { if (it == null) inviting = null } } }) { Text("发送邀请") } }, dismissButton = { TextButton(enabled = !busy, onClick = { inviting = null }) { Text("取消") } })
    }
    revoking?.let { (calendar, username) -> AlertDialog(onDismissRequest = { revoking = null }, title = { Text("移除成员") }, text = { Text("移除 $username 对“${calendar.name}”的访问权限？") },
        confirmButton = { TextButton(enabled = !busy, onClick = { action { coordinator.revoke(calendar.localCalendarId, username).also { if (it == null) revoking = null } } }) { Text("移除") } }, dismissButton = { TextButton(onClick = { revoking = null }) { Text("取消") } }) }
    deleting?.let { calendar -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除共享日历") }, text = { Text("删除“${calendar.name}”后，成员将无法继续访问服务器上的日程。本机缓存会保留为不可访问状态。") },
        confirmButton = { TextButton(enabled = !busy, onClick = { action { coordinator.deleteSharedCalendar(calendar.localCalendarId).also { if (it == null) deleting = null } } }) { Text("删除", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }) }
    remoteChoice?.let { calendar -> AlertDialog(onDismissRequest = { remoteChoice = null }, title = { Text("使用云端版本") }, text = { Text("本机内容会先保存为应用内的安全副本，再载入“${calendar.name}”的云端版本。未同步的编辑不会直接覆盖云端。") },
        confirmButton = { TextButton(enabled = !busy, onClick = { action { coordinator.useRemoteVersion(calendar.localCalendarId).also { if (it == null) remoteChoice = null } } }) { Text("确认") } }, dismissButton = { TextButton(onClick = { remoteChoice = null }) { Text("取消") } }) }
}

@Composable
internal fun CalendarRsvpRow(item: TodoItem) {
    if (!item.calendarId.startsWith("shared:") || item.sharedEventId.isBlank()) return
    val app = LocalContext.current.applicationContext as TodoApplication
    val scope = rememberCoroutineScope()
    var response by remember(item.id, item.eventParticipantsJson) { mutableStateOf(runCatching {
        val participants = org.json.JSONArray(item.eventParticipantsJson)
        (0 until participants.length()).asSequence().map { participants.getJSONObject(it) }
            .firstOrNull { it.optString("username") == app.settingsStore.currentSettings().remoteSyncUsername }?.optString("response") ?: "NEEDS_ACTION"
    }.getOrDefault("NEEDS_ACTION")) }
    var error by remember(item.id) { mutableStateOf<String?>(null) }
    var busy by remember(item.id) { mutableStateOf(false) }
    Column {
        PreferenceChoice(if (busy) "正在提交回应" else "我的回应", response, listOf("NEEDS_ACTION" to "尚未回应", "ACCEPTED" to "接受", "TENTATIVE" to "待定", "DECLINED" to "拒绝")) { selected ->
            if (!busy && selected != "NEEDS_ACTION") {
                busy = true
                scope.launch {
                    try { error = app.calendarCollaboration.rsvp(item.calendarId, item.sharedEventId, selected); if (error == null) response = selected }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (failure: Exception) { error = failure.message ?: "回应失败" }
                    finally { busy = false }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
