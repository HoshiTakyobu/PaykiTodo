package com.example.todoalarm.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.ManageSearch
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.ReminderAudioChannel
import com.example.todoalarm.data.ReminderChainLog
import com.example.todoalarm.data.ReminderDeliveryMode
import com.example.todoalarm.data.WeekStartMode
import com.example.todoalarm.sync.DesktopSyncStatus
import kotlinx.coroutines.launch

private enum class SettingsSection {
    PERMISSIONS,
    SOUND_STRATEGY,
    CALENDAR,
    TONE,
    HELP,
    ABOUT,
    DIAGNOSTICS,
    BACKUP,
    CRASH,
    DESKTOP_SYNC
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPanel(
    settings: AppSettings,
    permissions: PermissionSnapshot,
    defaultSnooze: Int,
    crashLog: String?,
    initialSectionKey: String? = null,
    initialSectionSerial: Int = 0,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestFullScreenPermission: () -> Unit,
    onRequestNotificationPolicyAccess: () -> Unit,
    onRequestIgnoreBatteryOptimization: () -> Unit,
    onRequestAccessibilityService: () -> Unit,
    onWeekStartModeChange: (WeekStartMode) -> Unit,
    onDefaultSnoozeChange: (Int) -> Unit,
    onDefaultCalendarReminderModeChange: (ReminderDeliveryMode) -> Unit,
    onReminderAudioStrategyChange: (ReminderAudioChannel, Int, Boolean, Int, Boolean) -> Unit,
    onDesktopSyncEnabledChange: (Boolean) -> Unit,
    onRotateDesktopSyncToken: () -> Unit,
    onUseBuiltInReminderTone: () -> Unit,
    onPickSystemReminderTone: () -> Unit,
    onOpenWiki: () -> Unit,
    desktopSyncStatus: DesktopSyncStatus,
    reminderChainLogs: List<ReminderChainLog>,
    onRunReminderChainTest: suspend (Int) -> String?,
    onClearReminderDiagnostics: suspend () -> Unit,
    onPickBackupDirectory: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onAutoBackupChange: (Boolean) -> Unit,
    onCopyCrashLog: () -> Unit,
    onClearCrashLog: () -> Unit
) {
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showCrashDialog by remember(crashLog) { mutableStateOf(false) }
    var showReminderTestDialog by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf<SettingsSection?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialSectionSerial) {
        selectedSection = when (initialSectionKey) {
            MainActivity.SETTINGS_SECTION_DESKTOP_SYNC -> SettingsSection.DESKTOP_SYNC
            else -> selectedSection
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SettingsCategoryHeader(
                title = "常用设置",
                summary = null
            )
        }
        item {
            SettingsMenuCard {
                SettingsMenuItem(
                    icon = Icons.Rounded.Security,
                    title = "提醒权限",
                    summary = "通知、精确闹钟、全屏提醒、免打扰、电池优化、辅助功能",
                    onClick = { selectedSection = SettingsSection.PERMISSIONS }
                )
                SettingsMenuDivider()
                SettingsMenuItem(
                    icon = Icons.Rounded.VolumeUp,
                    title = "提醒声音策略",
                    summary = "${settings.reminderAudioChannel.label} · App 内部音量 ${settings.reminderInternalVolumePercent}%",
                    onClick = { selectedSection = SettingsSection.SOUND_STRATEGY }
                )
                SettingsMenuDivider()
                SettingsMenuItem(
                    icon = Icons.Rounded.LibraryMusic,
                    title = "提示音",
                    summary = settings.reminderToneName ?: "当前使用内置提醒音",
                    onClick = onPickSystemReminderTone
                )
                SettingsMenuDivider()
                SettingsMenuItem(
                    icon = Icons.Rounded.CalendarMonth,
                    title = "日历与提醒",
                    summary = "默认延后时长、周起始日、日历默认提醒方式",
                    onClick = { selectedSection = SettingsSection.CALENDAR }
                )
                SettingsMenuDivider()
                SettingsMenuItem(
                    icon = Icons.Rounded.ManageSearch,
                    title = "使用说明",
                    summary = null,
                    onClick = onOpenWiki
                )
                SettingsMenuDivider()
                SettingsMenuItem(
                    icon = Icons.Rounded.Folder,
                    title = "关于",
                    summary = null,
                    onClick = { selectedSection = SettingsSection.ABOUT }
                )
            }
        }

        item {
            SettingsCategoryHeader(
                title = "高级设置",
                summary = null
            )
        }
        item {
            SettingsMenuCard {
                SettingsMenuItem(
                    icon = Icons.Rounded.BugReport,
                    title = "提醒链路诊断",
                    summary = if (reminderChainLogs.isEmpty()) "当前没有诊断记录" else "最近 ${reminderChainLogs.size} 条诊断记录",
                    onClick = { selectedSection = SettingsSection.DIAGNOSTICS }
                )
                SettingsMenuDivider()
                SettingsMenuItem(
                    icon = Icons.Rounded.Storage,
                    title = "数据与备份",
                    summary = null,
                    onClick = { selectedSection = SettingsSection.BACKUP }
                )
                SettingsMenuDivider()
                SettingsMenuItem(
                    icon = Icons.Rounded.Computer,
                    title = "电脑同步",
                    summary = null,
                    onClick = { selectedSection = SettingsSection.DESKTOP_SYNC }
                )
                SettingsMenuDivider()
                SettingsMenuItem(
                    icon = Icons.AutoMirrored.Rounded.Article,
                    title = "崩溃日志",
                    summary = null,
                    onClick = { selectedSection = SettingsSection.CRASH }
                )
            }
        }
    }

    when (selectedSection) {
        SettingsSection.PERMISSIONS -> SettingsSectionDialog("提醒权限", { selectedSection = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionRow(Icons.Rounded.Notifications, "通知权限", permissions.notificationGranted, onRequestNotificationPermission)
                PermissionRow(Icons.Rounded.Alarm, "精确闹钟", permissions.exactAlarmGranted, onRequestExactAlarmPermission)
                PermissionRow(Icons.Rounded.TaskAlt, "全屏提醒", permissions.fullScreenGranted, onRequestFullScreenPermission)
                PermissionRow(Icons.Rounded.Security, "免打扰穿透", permissions.dndAccessGranted, onRequestNotificationPolicyAccess)
                PermissionRow(Icons.Rounded.Alarm, "忽略电池优化", permissions.batteryOptimizationIgnored, onRequestIgnoreBatteryOptimization)
                PermissionRow(Icons.Rounded.Security, "辅助功能提醒", permissions.accessibilityServiceEnabled, onRequestAccessibilityService)
            }
        }

        SettingsSection.SOUND_STRATEGY -> SettingsSectionDialog("提醒声音策略", { selectedSection = null }) {
            ReminderAudioStrategyPanel(
                settings = settings,
                onChange = onReminderAudioStrategyChange
            )
        }

        SettingsSection.CALENDAR -> SettingsSectionDialog("日历与提醒", { selectedSection = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("默认延后时长", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                OutlinedButton(onClick = { showSnoozeDialog = true }) { Text("当前：${normalizeSnooze(defaultSnooze)} 分钟") }

                Spacer(modifier = Modifier.height(4.dp))
                Text("日历周起始日", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CalendarReminderModeButton(WeekStartMode.MONDAY.label, settings.weekStartMode == WeekStartMode.MONDAY) { onWeekStartModeChange(WeekStartMode.MONDAY) }
                    CalendarReminderModeButton(WeekStartMode.SUNDAY.label, settings.weekStartMode == WeekStartMode.SUNDAY) { onWeekStartModeChange(WeekStartMode.SUNDAY) }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("日历提醒默认方式", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CalendarReminderModeButton(ReminderDeliveryMode.NOTIFICATION.label, settings.defaultCalendarReminderMode == ReminderDeliveryMode.NOTIFICATION) {
                        onDefaultCalendarReminderModeChange(ReminderDeliveryMode.NOTIFICATION)
                    }
                    CalendarReminderModeButton(ReminderDeliveryMode.FULLSCREEN.label, settings.defaultCalendarReminderMode == ReminderDeliveryMode.FULLSCREEN) {
                        onDefaultCalendarReminderModeChange(ReminderDeliveryMode.FULLSCREEN)
                    }
                }
            }
        }

        SettingsSection.TONE -> SettingsSectionDialog("提示音", { selectedSection = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PermissionRow(Icons.Rounded.LibraryMusic, settings.reminderToneName ?: "当前：内置提醒音", true, onPickSystemReminderTone)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onUseBuiltInReminderTone) { Text("使用内置提醒音") }
                    OutlinedButton(onClick = onPickSystemReminderTone) { Text("选择系统提示音") }
                }
            }
        }

        SettingsSection.HELP -> SettingsSectionDialog("使用说明", { selectedSection = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onOpenWiki) { Text("打开使用说明") }
            }
        }

        SettingsSection.ABOUT -> SettingsSectionDialog("关于 PaykiTodo", { selectedSection = null }) {
            AboutPanel(onOpenWiki = onOpenWiki)
        }

        SettingsSection.DIAGNOSTICS -> SettingsSectionDialog("提醒链路诊断", { selectedSection = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { showReminderTestDialog = true }) {
                        Icon(Icons.Rounded.BugReport, contentDescription = null)
                        Text("提醒链路测试")
                    }
                    OutlinedButton(onClick = { scope.launch { onClearReminderDiagnostics() } }) {
                        Text("清空诊断")
                    }
                }
                if (reminderChainLogs.isEmpty()) {
                    Text("当前还没有诊断记录。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        reminderChainLogs.take(12).forEach { log ->
                            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${log.stage} · ${log.status}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("todoId=${log.todoId} · ${log.source}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    log.message?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SettingsSection.BACKUP -> SettingsSectionDialog("数据与备份", { selectedSection = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自动备份", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Switch(checked = settings.autoBackupEnabled, onCheckedChange = onAutoBackupChange)
                }
                Text(settings.backupDirectoryUri ?: "当前未选择备份目录", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onPickBackupDirectory, modifier = Modifier.fillMaxWidth()) { Text("选择备份目录") }
                    OutlinedButton(onClick = onExportBackup, modifier = Modifier.fillMaxWidth()) { Text("导出 JSON") }
                    OutlinedButton(onClick = onImportBackup, modifier = Modifier.fillMaxWidth()) { Text("导入 JSON") }
                }
            }
        }

        SettingsSection.DESKTOP_SYNC -> SettingsSectionDialog("电脑同步", { selectedSection = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("启用电脑同步", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(if (desktopSyncStatus.running) "当前服务正在运行" else "当前服务未运行", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.desktopSyncEnabled, onCheckedChange = onDesktopSyncEnabledChange)
                }
                Text("访问密钥：${settings.desktopSyncToken}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                OutlinedButton(onClick = onRotateDesktopSyncToken) { Text("重新生成访问密钥") }
                Text("可访问地址", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                if (desktopSyncStatus.ipAddresses.isEmpty()) {
                    Text("当前未检测到可用的局域网 IPv4 地址。请确认手机已连 Wi‑Fi。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        desktopSyncStatus.ipAddresses.forEach { ip ->
                            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) {
                                Text(
                                    text = "http://$ip:${desktopSyncStatus.port}",
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        SettingsSection.CRASH -> SettingsSectionDialog("崩溃日志", { selectedSection = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { showCrashDialog = true }, enabled = !crashLog.isNullOrBlank()) {
                        Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null)
                        Text("查看日志")
                    }
                    OutlinedButton(onClick = onClearCrashLog, enabled = !crashLog.isNullOrBlank()) {
                        Text("清空日志")
                    }
                }
            }
        }

        null -> Unit
    }

    if (showSnoozeDialog) {
        SnoozePickerDialog(
            defaultSnooze = defaultSnooze,
            onDismiss = { showSnoozeDialog = false },
            onConfirm = {
                onDefaultSnoozeChange(it)
                showSnoozeDialog = false
            }
        )
    }

    if (showCrashDialog && !crashLog.isNullOrBlank()) {
        CrashLogDialog(
            crashLog = crashLog,
            onDismiss = { showCrashDialog = false },
            onCopy = onCopyCrashLog
        )
    }

    if (showReminderTestDialog) {
        ReminderChainTestDialog(
            onDismiss = { showReminderTestDialog = false },
            onRun = { seconds ->
                scope.launch {
                    onRunReminderChainTest(seconds)
                    showReminderTestDialog = false
                }
            }
        )
    }
}

@Composable
private fun ReminderChainTestDialog(
    onDismiss: () -> Unit,
    onRun: (Int) -> Unit
) {
    var secondsText by remember { mutableStateOf("15") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("提醒链路测试") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "创建一条短延迟测试任务，用于验证通知、前台服务、全屏提醒和无障碍回退链路。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = secondsText,
                    onValueChange = { secondsText = it.filter(Char::isDigit) },
                    label = { Text("多少秒后提醒") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onRun(secondsText.toIntOrNull()?.coerceIn(5, 120) ?: 15) }) {
                Text("开始测试")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderAudioStrategyPanel(
    settings: AppSettings,
    onChange: (ReminderAudioChannel, Int, Boolean, Int, Boolean) -> Unit
) {
    fun commit(
        channel: ReminderAudioChannel = settings.reminderAudioChannel,
        internalVolume: Int = settings.reminderInternalVolumePercent,
        boostSystemVolume: Boolean = settings.reminderBoostSystemVolume,
        boostVolume: Int = settings.reminderBoostVolumePercent,
        quietMode: Boolean = settings.workQuietModeEnabled
    ) {
        onChange(channel, internalVolume.coerceIn(0, 100), boostSystemVolume, boostVolume.coerceIn(0, 100), quietMode)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Android 不允许普通应用新增系统级 PaykiTodo 独立音量。这里提供的是 PaykiTodo 内部音量和播放通道策略。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )

        Text("播放通道", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ReminderAudioChannel.entries.forEach { channel ->
                CalendarReminderModeButton(channel.label, settings.reminderAudioChannel == channel) {
                    commit(channel = channel)
                }
            }
        }

        PercentSettingRow(
            title = "PaykiTodo 内部音量",
            summary = "只影响 PaykiTodo 自己播放提醒音的音量，不会改变系统音量滑块。",
            value = settings.reminderInternalVolumePercent,
            onChange = { commit(internalVolume = it) }
        )

        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("课堂 / 上班模式", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "开启后提醒默认不外放声音，改用更强震动，并让日程提醒也走全屏 / 无障碍兜底链路。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(checked = settings.workQuietModeEnabled, onCheckedChange = { commit(quietMode = it) })
                }
            }
        }

        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f)) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("提醒时临时提升系统通道音量", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "高级选项。会临时修改所选系统通道的全局音量，播放结束或提醒结束后尽量恢复原值。默认关闭。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(checked = settings.reminderBoostSystemVolume, onCheckedChange = { commit(boostSystemVolume = it) })
                }
                PercentSettingRow(
                    title = "临时提升目标",
                    summary = "例如通知通道静音时，可在提醒期间尝试提升到 50%，随后恢复。",
                    value = settings.reminderBoostVolumePercent,
                    enabled = settings.reminderBoostSystemVolume,
                    onChange = { commit(boostVolume = it) }
                )
            }
        }
    }
}

@Composable
private fun PercentSettingRow(
    title: String,
    summary: String,
    value: Int,
    enabled: Boolean = true,
    onChange: (Int) -> Unit
) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.26f else 0.12f)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, fontWeight = FontWeight.SemiBold, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Text("${value.coerceIn(0, 100)}%", fontWeight = FontWeight.Bold, color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(enabled = enabled, onClick = { onChange((value - 10).coerceIn(0, 100)) }) { Text("-10") }
                OutlinedButton(enabled = enabled, onClick = { onChange((value + 10).coerceIn(0, 100)) }) { Text("+10") }
            }
        }
    }
}

@Composable
private fun CalendarReminderModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    ) {
        Text(label)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SnoozePickerDialog(
    defaultSnooze: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val actualValues = remember { (5..60 step 5).toList() }
    val displayValues = remember { listOf<Int?>(null, null) + actualValues + listOf(null, null) }
    val initialActualIndex = actualValues.indexOf(normalizeSnooze(defaultSnooze)).coerceAtLeast(0)
    val rowHeight = 52.dp
    val rowHeightPx = with(LocalDensity.current) { rowHeight.roundToPx() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialActualIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()

    val selectedDisplayIndex by remember {
        derivedStateOf {
            val step = if (listState.firstVisibleItemScrollOffset >= rowHeightPx / 2) 1 else 0
            (listState.firstVisibleItemIndex + 2 + step).coerceIn(2, displayValues.lastIndex - 2)
        }
    }

    val selectedMinutes = displayValues[selectedDisplayIndex] ?: actualValues[initialActualIndex]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择默认延后时长") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "上下滑动选择 5 到 60 分钟。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight * 5)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        state = listState,
                        flingBehavior = flingBehavior,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(displayValues) { index, minutes ->
                            val selected = index == selectedDisplayIndex
                            val label = minutes?.let { "$it 分钟" }.orEmpty()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeight)
                                    .then(
                                        if (minutes != null) {
                                            Modifier.clickable {
                                                scope.launch {
                                                    listState.animateScrollToItem(index - 2)
                                                }
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(horizontal = 12.dp)
                                    .background(
                                        color = if (selected && minutes != null) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        } else {
                                            Color.Transparent
                                        },
                                        shape = RoundedCornerShape(18.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (minutes != null) {
                                    Text(
                                        text = label,
                                        textAlign = TextAlign.Center,
                                        style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(rowHeight)
                            .padding(horizontal = 12.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(18.dp)
                            )
                    )
                }
                Text(
                    text = "当前选择：$selectedMinutes 分钟",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedMinutes) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    LaunchedEffect(initialActualIndex) {
        listState.scrollToItem(initialActualIndex)
    }
}

@Composable
fun AboutPanel(
    onOpenWiki: () -> Unit = {}
) {
    val context = LocalContext.current
    val packageInfo = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val versionName = packageInfo.versionName ?: "未知"
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo).toString()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PrefCard("关于 PaykiTodo") {
            Text(
                text = "PaykiTodo 是一款本地单机的待办与提醒应用，当前重点在于强提醒、任务节奏管理和持续迭代。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        PrefCard("版本信息") {
            AboutRow("应用名称", "PaykiTodo")
            AboutRow("版本号", versionName)
            AboutRow("内部版本", versionCode)
            AboutRow("包名", context.packageName)
        }

        PrefCard("项目信息") {
            AboutRow("作者", "Hoshi Takyobu")
            AboutRow("定位", "本地单机 / 持续迭代中")
            AboutRow("数据存储", "仅保存在当前设备")
            AboutRow("版权", "© Copyright Hoshi Takyobu, 2026-2026")
        }

        PrefCard("使用说明") {
            Text(
                text = "如果对提醒链路测试、批量导入、周模板、日历视图或备份功能不熟，可以直接打开内置使用说明。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            OutlinedButton(onClick = onOpenWiki) {
                Text("打开使用说明")
            }
        }
    }
}

@Composable
private fun CrashLogDialog(
    crashLog: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("最近一次崩溃日志") },
        text = {
            Text(
                text = crashLog,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            TextButton(onClick = onCopy) {
                Text("复制")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

private fun normalizeSnooze(minutes: Int): Int {
    val clamped = minutes.coerceIn(5, 60)
    return if (clamped % 5 == 0) clamped else clamped - (clamped % 5)
}

@Composable
private fun SettingsMenuCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsCategoryHeader(
    title: String,
    summary: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
        )
        summary?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.96f)
            )
        }
    }
}

@Composable
private fun SettingsMenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
    )
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    summary: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            summary?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSectionDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun PrefCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    label: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (granted) "当前状态：已开启" else "当前状态：未开启",
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            )
        }
        OutlinedButton(onClick = onClick) {
            Text("去设置")
        }
    }
}
