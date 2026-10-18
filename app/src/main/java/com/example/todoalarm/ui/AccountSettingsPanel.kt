package com.example.todoalarm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.sync.AccountDevice
import com.example.todoalarm.sync.RemoteSyncCoordinator
import com.example.todoalarm.sync.remoteSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun AccountSettingsPanel(settings: AppSettings, onLogout: () -> Unit) {
    val app = LocalContext.current.applicationContext as TodoApplication
    val coordinator = app.remoteSyncCoordinator
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editName by remember { mutableStateOf(false) }
    var editPassword by remember { mutableStateOf(false) }
    var showDevices by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<AccountDevice>>(emptyList()) }
    var revokeTarget by remember { mutableStateOf<AccountDevice?>(null) }
    var loginVisible by remember { mutableStateOf(false) }
    var review by remember { mutableStateOf<RemoteSyncCoordinator.CloudReview?>(null) }
    var keepLocal by remember { mutableStateOf(false) }
    var confirmSafetyRestore by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf<String?>(null) }
    fun runAction(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        success = null
        scope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "操作失败，请重试"
            } finally {
                busy = false
            }
        }
    }
    LaunchedEffect(Unit) { if (app.settingsStore.hasValidRemoteSession()) runAction { coordinator.refreshAccountProfile() } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AccountInfoRow("账号", settings.remoteSyncUsername)
        AccountInfoRow("用户 ID", settings.remoteSyncUserId.takeIf { it > 0 }?.toString() ?: "待联网读取")
        HorizontalDivider()
        AccountInfoRow("昵称", settings.remoteSyncDisplayName.ifBlank { settings.remoteSyncUsername }) {
            error = null
            editName = true
        }
        AccountInfoRow("修改密码", "") { error = null; editPassword = true }
        AccountInfoRow("安全设置", "登录设备") {
            showDevices = true
            runAction { devices = coordinator.accountDevices() }
        }
        HorizontalDivider()
        Text(
            when {
                !app.settingsStore.hasValidRemoteSession() -> "登录已过期。本机数据和离线提醒继续可用，重新登录后补传修改。"
                settings.remoteSyncConflictRevision > 0 -> "云端和本机都有更新，请检查后选择保留哪一份。"
                settings.remoteSyncDirty -> "本机变更待同步"
                settings.remoteSyncLastSuccessAt > 0 -> "最近同步：${accountTime(settings.remoteSyncLastSuccessAt / 1000)}"
                else -> "尚无成功同步记录"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        settings.remoteSyncLastError.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        success?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        if (!app.settingsStore.hasValidRemoteSession()) {
            OutlinedButton(onClick = { loginVisible = true }, modifier = Modifier.fillMaxWidth()) { Text("重新登录") }
        } else {
            OutlinedButton(enabled = !busy, onClick = { runAction { coordinator.syncNow() } }, modifier = Modifier.fillMaxWidth()) { Text("立即同步") }
            Text("云同步在联网且数据更改后自动进行，不需要开启连接电脑。", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = !busy, onClick = { keepLocal = false; runAction { review = coordinator.inspectCloud() } }, modifier = Modifier.fillMaxWidth()) { Text("检查云端并恢复") }
            OutlinedButton(enabled = !busy, onClick = { keepLocal = true; runAction { review = coordinator.inspectCloud() } }, modifier = Modifier.fillMaxWidth()) { Text("保留本机上传") }
        }
        OutlinedButton(enabled = !busy, onClick = { confirmSafetyRestore = true }, modifier = Modifier.fillMaxWidth()) { Text("恢复最近的操作前副本") }
        Text("替换前会保留双方副本；若检查后又有修改，需重新检查。", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { confirmLogout = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("退出账号") }
    }

    if (loginVisible) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { loginVisible = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            AccountAuthScreen(
                onLogin = { username, password, token -> coordinator.login(username, password, token).also { if (it.succeeded) loginVisible = false } },
                onRegister = { username, name, password, token -> coordinator.register(username, name, password, token) },
                onRecover = coordinator::recoverPassword, onRecoveryCode = {},
                turnstileEnabled = !com.example.todoalarm.BuildConfig.PAYKITODO_CLOUD_BASE_URL.endsWith(".invalid"),
                turnstileChallengeUrl = com.example.todoalarm.BuildConfig.PAYKITODO_CLOUD_BASE_URL.trimEnd('/') + "/v2/auth/challenge"
            )
        }
    }
    review?.let { checked ->
        val summary = checked.snapshot.remoteSummary()
        AlertDialog(onDismissRequest = { if (!busy) review = null },
            title = { Text(if (keepLocal) "确认保留本机上传" else "确认恢复云端副本") },
            text = { Text("云端版本 ${checked.revision}：${summary.todoCount} 项待办、${summary.eventCount} 项日程、${summary.planningNoteCount} 份规划文档。\n${if (keepLocal) "本机数据将作为新云端版本，其他设备下次同步会更新。" else "云端数据将替换本机，未上传修改保留在恢复副本中。"}\n确认前会保存双方副本。") },
            confirmButton = { TextButton(enabled = !busy, onClick = { runAction { success = coordinator.resolveCloud(checked, keepLocal); review = null } }) { Text("确认") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { review = null }) { Text("取消") } })
    }

    if (confirmSafetyRestore) AlertDialog(
        onDismissRequest = { if (!busy) confirmSafetyRestore = false },
        title = { Text("恢复操作前副本") },
        text = { Text("将使用当前账号最近一次操作前的本机副本替换数据，并标记为待同步。当前数据也会先保存为恢复副本。") },
        confirmButton = { TextButton(enabled = !busy, onClick = { runAction {
            coordinator.restoreNewestSafetyCopy(); confirmSafetyRestore = false; success = "操作前副本已恢复，当前数据也已保留副本"
        } }) { Text("确认恢复") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { confirmSafetyRestore = false }) { Text("取消") } })

    if (editName) {
        var name by remember { mutableStateOf(settings.remoteSyncDisplayName.ifBlank { settings.remoteSyncUsername }) }
        AlertDialog(
            onDismissRequest = { if (!busy) editName = false },
            title = { Text("修改昵称") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("昵称") }, singleLine = true, enabled = !busy)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = !busy && name.trim().length in 1..80, onClick = {
                    runAction { coordinator.updateDisplayName(name); editName = false }
                }) { Text(if (busy) "提交中" else "完成") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { editName = false }) { Text("取消") } }
        )
    }
    if (editPassword) {
        var current by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var repeated by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!busy) editPassword = false },
            title = { Text("修改密码") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountPasswordField("原密码", current, !busy) { current = it }
                    AccountPasswordField("新密码（10-256 位）", password, !busy) { password = it }
                    AccountPasswordField("确认新密码", repeated, !busy) { repeated = it }
                    Text("修改后所有设备需要重新登录，本机数据保留。", style = MaterialTheme.typography.bodySmall)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && current.isNotEmpty() && password.length in 10..256 && repeated == password,
                    onClick = { runAction { coordinator.changeAccountPassword(current, password); editPassword = false; onLogout() } }
                ) { Text(if (busy) "提交中" else "修改密码") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { editPassword = false }) { Text("取消") } }
        )
    }
    if (showDevices) {
        SettingsSectionDialog("安全设置", { showDevices = false }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("登录设备", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                IconButton(enabled = !busy, onClick = { runAction { devices = coordinator.accountDevices() } }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "刷新登录设备")
                }
            }
            if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            devices.sortedByDescending { it.active }.forEach { device ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(if (device.platform == "android") Icons.Rounded.Smartphone else Icons.Rounded.Computer, contentDescription = null)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(if (device.current) "本机" else device.name, fontWeight = FontWeight.Medium)
                        Text(if (device.active) "正在登录" else "已退出", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("最近活动：${accountTime(device.lastSeenAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!device.current && device.active) {
                        TextButton(enabled = !busy, onClick = { revokeTarget = device }) { Text("退出") }
                    }
                }
                HorizontalDivider()
            }
            if (!busy && error == null && devices.isEmpty()) Text("暂无登录设备")
        }
    }
    revokeTarget?.let { device ->
        AlertDialog(
            onDismissRequest = { if (!busy) revokeTarget = null },
            title = { Text("退出设备") },
            text = { Text("确定要退出 ${device.name} 吗？该设备的本地数据将保留。") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                revokeTarget = null
                runAction { coordinator.revokeAccountDevice(device.id); devices = coordinator.accountDevices() }
            }) { Text("退出设备") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { revokeTarget = null }) { Text("取消") } }
        )
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false }, title = { Text("退出账号") },
            text = { Text("本机数据将保留，重新登录后可继续使用。") },
            confirmButton = { TextButton(onClick = onLogout) { Text("退出") } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun AccountInfoRow(title: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onClick != null) Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun AccountPasswordField(label: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, enabled = enabled,
        singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
}

private fun accountTime(seconds: Long): String = Instant.ofEpochSecond(seconds)
    .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
