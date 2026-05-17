package com.example.todoalarm.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.ManageSearch
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.PlanningAiCaller
import com.example.todoalarm.data.PlanningAiModelFetchResult
import com.example.todoalarm.data.PlanningAiProvider
import com.example.todoalarm.data.PlanningAiTestResult
import com.example.todoalarm.data.ReminderAudioChannel
import com.example.todoalarm.data.ReminderChainLog
import com.example.todoalarm.data.ReminderDeliveryMode
import com.example.todoalarm.data.WeekStartMode
import com.example.todoalarm.sync.DesktopSyncStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class SettingsSection {
    PERMISSIONS,
    SOUND_STRATEGY,
    TONE,
    CALENDAR,
    FOCUS,
    AI_CONFIG,
    ABOUT,
    DIAGNOSTICS,
    BACKUP,
    CRASH,
    DESKTOP_SYNC
}

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
    onFocusPreferencesChange: (Int, Int, Boolean, Boolean) -> Unit,
    onPlanningAiProvidersChange: (Boolean, List<PlanningAiProvider>) -> Unit,
    onReportPreferencesChange: (Boolean, Int, Int, Boolean, Int, Int) -> Unit,
    onGenerateDailyReportNow: suspend () -> String?,
    onResetOnboarding: () -> Unit,
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
    var desktopSyncAddressesExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialSectionSerial) {
        selectedSection = when (initialSectionKey) {
            MainActivity.SETTINGS_SECTION_DESKTOP_SYNC -> SettingsSection.DESKTOP_SYNC
            else -> selectedSection
        }
    }

    LaunchedEffect(selectedSection, settings.desktopSyncEnabled, desktopSyncStatus.running) {
        if (selectedSection != SettingsSection.DESKTOP_SYNC || !settings.desktopSyncEnabled || !desktopSyncStatus.running) {
            desktopSyncAddressesExpanded = false
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
                    onClick = { selectedSection = SettingsSection.TONE }
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
                    icon = Icons.Rounded.Timer,
                    title = "专注模式",
                    summary = "默认 ${settings.focusDefaultMinutes} 分钟 · 延续 ${settings.focusExtensionMinutes} 分钟",
                    onClick = { selectedSection = SettingsSection.FOCUS }
                )
                SettingsMenuDivider()
                SettingsMenuItem(
                    icon = Icons.Rounded.Psychology,
                    title = "AI 调用配置",
                    summary = if (settings.planningAiEnabled && settings.planningAiProviders.isNotEmpty()) {
                        "${settings.planningAiProviders.count { it.enabled }} 个已启用 · 共 ${settings.planningAiProviders.size} 个源"
                    } else {
                        "为规划台 AI 识别配置多个 Base URL、API Key 和模型名"
                    },
                    onClick = { selectedSection = SettingsSection.AI_CONFIG }
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
                    icon = Icons.Rounded.Computer,
                    title = "电脑同步",
                    summary = when {
                        desktopSyncStatus.running -> "正在运行"
                        settings.desktopSyncEnabled -> "未运行"
                        else -> "未开启"
                    },
                    onClick = { selectedSection = SettingsSection.DESKTOP_SYNC }
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

        SettingsSection.TONE -> SettingsSectionDialog("提示音", { selectedSection = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ToneChoiceRow(
                    title = "内置提醒音",
                    selected = settings.reminderToneUri == null,
                    onClick = onUseBuiltInReminderTone
                )
                ToneChoiceRow(
                    title = "系统通知提示音",
                    summary = settings.reminderToneName ?: "从系统通知提示音列表选择",
                    selected = settings.reminderToneUri != null,
                    onClick = onPickSystemReminderTone
                )
            }
        }

        SettingsSection.CALENDAR -> SettingsSectionDialog("日历与提醒", { selectedSection = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("默认延后时长", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                OutlinedButton(onClick = { showSnoozeDialog = true }) { Text("当前：${normalizeSnooze(defaultSnooze)} 分钟") }

                Spacer(modifier = Modifier.height(4.dp))
                CompactDropdownSetting(
                    title = "日历周起始日",
                    value = settings.weekStartMode.label,
                    options = WeekStartMode.entries.map { it.label },
                    onSelect = { label -> WeekStartMode.entries.firstOrNull { it.label == label }?.let(onWeekStartModeChange) }
                )

                Spacer(modifier = Modifier.height(4.dp))
                CompactDropdownSetting(
                    title = "日历提醒默认方式",
                    value = settings.defaultCalendarReminderMode.label,
                    options = ReminderDeliveryMode.entries.map { it.label },
                    onSelect = { label -> ReminderDeliveryMode.entries.firstOrNull { it.label == label }?.let(onDefaultCalendarReminderModeChange) }
                )
            }
        }

        SettingsSection.AI_CONFIG -> SettingsSectionDialog("AI 调用配置", { selectedSection = null }) {
            PlanningAiConfigPanel(
                settings = settings,
                onSave = onPlanningAiProvidersChange,
                onReportPreferencesChange = onReportPreferencesChange,
                onGenerateDailyReportNow = onGenerateDailyReportNow
            )
        }

        SettingsSection.FOCUS -> SettingsSectionDialog("专注模式", { selectedSection = null }) {
            FocusPreferencesPanel(
                settings = settings,
                onChange = onFocusPreferencesChange
            )
        }

        SettingsSection.ABOUT -> SettingsSectionDialog("关于 PaykiTodo", { selectedSection = null }) {
            AboutPanel(
                onOpenWiki = onOpenWiki,
                onResetOnboarding = onResetOnboarding
            )
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
            val context = LocalContext.current
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("启用电脑同步", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(if (desktopSyncStatus.running) "当前服务正在运行" else "当前服务未运行", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.desktopSyncEnabled, onCheckedChange = onDesktopSyncEnabledChange)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("访问密钥：${settings.desktopSyncToken}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        val clip = context.getSystemService(android.content.ClipboardManager::class.java)
                        clip?.setPrimaryClip(android.content.ClipData.newPlainText("token", settings.desktopSyncToken))
                        Toast.makeText(context, "密钥已复制", Toast.LENGTH_SHORT).show()
                    }) { Text("复制") }
                }
                OutlinedButton(onClick = onRotateDesktopSyncToken) { Text("重新生成访问密钥") }
                if (settings.desktopSyncEnabled) {
                    Text("连接地址", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    if (!desktopSyncStatus.running) {
                        Text("电脑同步服务未运行。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (!desktopSyncAddressesExpanded) {
                        OutlinedButton(onClick = { desktopSyncAddressesExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("显示连接地址")
                        }
                    } else if (desktopSyncStatus.ipAddresses.isEmpty()) {
                        Text("未检测到可用的局域网 IPv4 地址。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { desktopSyncAddressesExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("隐藏连接地址")
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            desktopSyncStatus.ipAddresses.forEach { ip ->
                                val url = "http://$ip:${desktopSyncStatus.port}"
                                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = url,
                                            modifier = Modifier.weight(1f),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        OutlinedButton(onClick = {
                                            val clip = context.getSystemService(android.content.ClipboardManager::class.java)
                                            clip?.setPrimaryClip(android.content.ClipData.newPlainText("url", url))
                                            Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
                                        }) { Text("复制") }
                                    }
                                }
                            }
                            OutlinedButton(onClick = { desktopSyncAddressesExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                                Text("隐藏连接地址")
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
        CompactDropdownSetting(
            title = "播放通道",
            value = settings.reminderAudioChannel.label,
            options = ReminderAudioChannel.entries.map { it.label },
            onSelect = { label -> ReminderAudioChannel.entries.firstOrNull { it.label == label }?.let { commit(channel = it) } }
        )

        PercentSettingRow(
            title = "PaykiTodo 内部音量",
            summary = "",
            value = settings.reminderInternalVolumePercent,
            onChange = { commit(internalVolume = it) }
        )

        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("工作模式", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
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
                    }
                    Switch(checked = settings.reminderBoostSystemVolume, onCheckedChange = { commit(boostSystemVolume = it) })
                }
                PercentSettingRow(
                    title = "临时提升目标",
                    summary = "",
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
    var textValue by remember(value) { mutableStateOf(value.coerceIn(0, 100).toString()) }
    fun commitPercent(raw: Int) {
        val normalized = raw.coerceIn(0, 100)
        textValue = normalized.toString()
        onChange(normalized)
    }

    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.26f else 0.12f)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, fontWeight = FontWeight.SemiBold, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (summary.isNotBlank()) {
                        Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Slider(
                    value = value.coerceIn(0, 100).toFloat(),
                    onValueChange = { commitPercent(it.roundToInt()) },
                    valueRange = 0f..100f,
                    steps = 99,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { raw ->
                        val filtered = raw.filter(Char::isDigit).take(3)
                        textValue = filtered
                        filtered.toIntOrNull()?.let { commitPercent(it) }
                    },
                    enabled = enabled,
                    singleLine = true,
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.42f)
                )
            }
        }
    }
}

@Composable
private fun FocusPreferencesPanel(
    settings: AppSettings,
    onChange: (Int, Int, Boolean, Boolean) -> Unit
) {
    fun commit(
        defaultMinutes: Int = settings.focusDefaultMinutes,
        extensionMinutes: Int = settings.focusExtensionMinutes,
        keepScreenOn: Boolean = settings.focusKeepScreenOn,
        blockNotifications: Boolean = settings.focusBlockNotifications
    ) {
        onChange(
            defaultMinutes.coerceIn(5, 90),
            extensionMinutes.coerceIn(1, 30),
            keepScreenOn,
            blockNotifications
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MinuteSliderSettingRow(
            title = "默认专注时长",
            summary = "从待办或自由专注入口启动时默认使用这个时长。",
            value = settings.focusDefaultMinutes,
            min = 5,
            max = 90,
            step = 5,
            onChange = { commit(defaultMinutes = it) }
        )
        MinuteSliderSettingRow(
            title = "单次延续时长",
            summary = "倒计时结束后点击延续，会追加这段时间。",
            value = settings.focusExtensionMinutes,
            min = 1,
            max = 30,
            step = 1,
            onChange = { commit(extensionMinutes = it) }
        )
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("专注期间保持屏幕常亮", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Switch(checked = settings.focusKeepScreenOn, onCheckedChange = { commit(keepScreenOn = it) })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("专注期间压制新提醒", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "专注期间不再弹出新提醒（专注内的事项除外）。当前版本只记录偏好，不真实拦截系统通知。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(checked = settings.focusBlockNotifications, onCheckedChange = { commit(blockNotifications = it) })
                }
            }
        }
    }
}

@Composable
private fun MinuteSliderSettingRow(
    title: String,
    summary: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    onChange: (Int) -> Unit
) {
    fun normalize(raw: Float): Int {
        val rounded = ((raw.roundToInt() - min + step / 2) / step) * step + min
        return rounded.coerceIn(min, max)
    }

    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Text("${value.coerceIn(min, max)} 分钟", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = value.coerceIn(min, max).toFloat(),
                onValueChange = { onChange(normalize(it)) },
                valueRange = min.toFloat()..max.toFloat(),
                steps = ((max - min) / step - 1).coerceAtLeast(0)
            )
        }
    }
}

@Composable
private fun PlanningAiConfigPanel(
    settings: AppSettings,
    onSave: (Boolean, List<PlanningAiProvider>) -> Unit,
    onReportPreferencesChange: (Boolean, Int, Int, Boolean, Int, Int) -> Unit,
    onGenerateDailyReportNow: suspend () -> String?
) {
    var enabled by remember(settings.planningAiEnabled) { mutableStateOf(settings.planningAiEnabled) }
    var providers by remember(settings.planningAiProviders) {
        mutableStateOf(
            settings.planningAiProviders.ifEmpty {
                listOf(
                    PlanningAiProvider(
                        name = settings.planningAiProviderName.ifBlank { "DeepSeek / Qwen" },
                        baseUrl = settings.planningAiBaseUrl,
                        apiKey = settings.planningAiApiKey,
                        model = settings.planningAiModel,
                        enabled = settings.planningAiEnabled
                    )
                ).filter { it.baseUrl.isNotBlank() || it.apiKey.isNotBlank() || it.model.isNotBlank() }
            }
        )
    }
    var editingProvider by remember { mutableStateOf<PlanningAiProvider?>(null) }
    var deleteTarget by remember { mutableStateOf<PlanningAiProvider?>(null) }
    var savedHint by remember { mutableStateOf(false) }
    val requiredMissing = enabled && providers.filter { it.enabled }.any { it.baseUrl.isBlank() || it.apiKey.isBlank() || it.model.isBlank() }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
        ) {
            Text(
                text = "启用后，规划台“识别”会优先调用这里的 AI 源，把随手写的内容转成预览候选；AI 失败或未配置完整时会自动回到本地规则。AI 只生成候选，必须预览确认后才能导入。",
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("启用 AI 识别配置", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text("识别时会按下方顺序从上到下轮询可用源。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = enabled, onCheckedChange = { enabled = it; savedHint = false })
        }

        if (providers.isEmpty()) {
            Text("当前还没有 AI 源。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            providers.forEachIndexed { index, provider ->
                PlanningAiProviderRow(
                    index = index,
                    total = providers.size,
                    provider = provider,
                    onToggle = { checked ->
                        providers = providers.map { if (it.id == provider.id) it.copy(enabled = checked) else it }
                        savedHint = false
                    },
                    onMoveUp = {
                        providers = providers.moveProvider(index, index - 1)
                        savedHint = false
                    },
                    onMoveDown = {
                        providers = providers.moveProvider(index, index + 1)
                        savedHint = false
                    },
                    onEdit = {
                        editingProvider = provider
                    },
                    onDelete = {
                        deleteTarget = provider
                    }
                )
            }
        }

        if (requiredMissing) {
            Text(
                text = "启用的 AI 源需要填写 Base URL、API Key 和模型名。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Text(
                text = "真实 API Key 只保存在本机设置中；备份 JSON 不会导出 API Key。AI 输出会进入现有预览页，不会直接写入待办或日程。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(
            onClick = {
                editingProvider = PlanningAiProvider(
                    name = "",
                    baseUrl = "",
                    apiKey = "",
                    model = "",
                    enabled = true
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("添加 AI 源")
        }

        OutlinedButton(
            onClick = {
                onSave(enabled, providers)
                savedHint = true
            },
            enabled = !requiredMissing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存 AI 调用配置")
        }
        if (savedHint) {
            Text("已保存。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))

        AiReportPreferencesPanel(
            settings = settings,
            onChange = onReportPreferencesChange,
            onGenerateDailyReportNow = onGenerateDailyReportNow
        )
    }

    editingProvider?.let { provider ->
        PlanningAiProviderDialog(
            provider = provider,
            onDismiss = { editingProvider = null },
            onConfirm = { updated ->
                providers = if (providers.any { it.id == updated.id }) {
                    providers.map { if (it.id == updated.id) updated else it }
                } else {
                    providers + updated
                }
                savedHint = false
                editingProvider = null
            }
        )
    }

    deleteTarget?.let { provider ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除 AI 源") },
            text = { Text("确定删除「${provider.name.ifBlank { "未命名服务" }}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    providers = providers.filterNot { it.id == provider.id }
                    savedHint = false
                    deleteTarget = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AiReportPreferencesPanel(
    settings: AppSettings,
    onChange: (Boolean, Int, Int, Boolean, Int, Int) -> Unit,
    onGenerateDailyReportNow: suspend () -> String?
) {
    var dailyEnabled by remember(settings.dailyReportEnabled) { mutableStateOf(settings.dailyReportEnabled) }
    var dailyTimeText by remember(settings.dailyReportHour, settings.dailyReportMinute) {
        mutableStateOf(formatReportTime(settings.dailyReportHour, settings.dailyReportMinute))
    }
    var weeklyEnabled by remember(settings.weeklyReportEnabled) { mutableStateOf(settings.weeklyReportEnabled) }
    var weeklyTimeText by remember(settings.weeklyReportHour, settings.weeklyReportMinute) {
        mutableStateOf(formatReportTime(settings.weeklyReportHour, settings.weeklyReportMinute))
    }
    var savedHint by remember { mutableStateOf(false) }
    var runningNow by remember { mutableStateOf(false) }
    var runMessage by remember { mutableStateOf<String?>(null) }
    var showHelpSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dailyTime = parseReportTime(dailyTimeText)
    val weeklyTime = parseReportTime(weeklyTimeText)
    val invalid = (dailyEnabled && dailyTime == null) || (weeklyEnabled && weeklyTime == null)

    fun save() {
        val daily = dailyTime ?: (settings.dailyReportHour to settings.dailyReportMinute)
        val weekly = weeklyTime ?: (settings.weeklyReportHour to settings.weeklyReportMinute)
        onChange(dailyEnabled, daily.first, daily.second, weeklyEnabled, weekly.first, weekly.second)
        savedHint = true
        runMessage = null
    }

    PrefCard("AI 日报 / 周报") {
        Text(
            text = "日报会在每天设定时间生成并写入规划台「AI 日报」；周报会在每周日设定时间写入「AI 周报」。AI 配置不可用时会用本地模板兜底。若未开启「精确闹钟」权限，系统可能延后生成。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ReportScheduleRow(
            title = "启用日报",
            summary = "每天生成，默认 22:00",
            enabled = dailyEnabled,
            timeText = dailyTimeText,
            timeValid = dailyTime != null,
            onEnabledChange = {
                dailyEnabled = it
                savedHint = false
            },
            onTimeChange = {
                dailyTimeText = it
                savedHint = false
            }
        )
        ReportScheduleRow(
            title = "启用周报",
            summary = "每周日生成，默认 22:00",
            enabled = weeklyEnabled,
            timeText = weeklyTimeText,
            timeValid = weeklyTime != null,
            onEnabledChange = {
                weeklyEnabled = it
                savedHint = false
            },
            onTimeChange = {
                weeklyTimeText = it
                savedHint = false
            }
        )
        if (invalid) {
            Text("时间格式需要是 HH:mm，例如 22:00。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        OutlinedButton(
            onClick = { save() },
            enabled = !invalid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存日报 / 周报设置")
        }
        OutlinedButton(
            onClick = { showHelpSheet = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("了解 AI 日报")
        }
        OutlinedButton(
            onClick = {
                scope.launch {
                    runningNow = true
                    runMessage = null
                    try {
                        val message = onGenerateDailyReportNow()
                        val finalMessage = message ?: "AI 日报已生成，并写入规划台「AI 日报」。"
                        runMessage = finalMessage
                        Toast.makeText(context, finalMessage, Toast.LENGTH_SHORT).show()
                    } finally {
                        runningNow = false
                    }
                }
            },
            enabled = !runningNow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (runningNow) "生成中..." else "立即生成一次日报")
        }
        if (savedHint) {
            Text("日报 / 周报设置已保存，系统定时已重新调度。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        runMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }

    if (showHelpSheet) {
        AiReportHelpSheet(onDismiss = { showHelpSheet = false })
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AiReportHelpSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "AI 日报怎么用",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            AiReportHelpCard("什么是 AI 日报") {
                HelpBullet("AI 日报会汇总今天完成的待办、错过的待办、今日和明日日程、明日 DDL、今日专注分钟数。")
                HelpBullet("它会调用你已配置的 AI 源生成 1-3 段中文总结，并给出「明天先做什么」的建议。")
                HelpBullet("周报在每周日生成，覆盖本周累计数据。")
            }
            AiReportHelpCard("使用前提") {
                HelpBullet("至少配置一个可用的 AI 源，例如 DeepSeek、Qwen 或 OpenAI 兼容服务，并通过「测试连接」。")
                HelpBullet("如果所有 AI 源都不可用，会自动用本地模板生成简短报告，不会生成空内容。")
                HelpBullet("Android 12+ 建议授予「闹钟和提醒 / 精确闹钟」权限，否则系统可能延后几小时甚至跳过生成。")
            }
            AiReportHelpCard("启用步骤") {
                HelpBullet("1. 在本卡片打开「日报」开关，设置生成时间，默认 22:00。")
                HelpBullet("2. 可选：打开「周报」开关。")
                HelpBullet("3. 首次使用建议点击「立即生成一次日报」验证链路。")
                HelpBullet("4. 报告生成后会有通知栏推送，点击通知可直达对应文档。")
            }
            AiReportHelpCard("报告在哪看") {
                HelpBullet("通知栏点击「AI 日报已生成」通知，会自动打开规划台「AI 日报」文档。")
                HelpBullet("也可以从抽屉进入规划台，再在文档列表中选择「AI 日报」或「AI 周报」。")
                HelpBullet("报告文档顶部会有紫色提示条，说明这是自动生成的复盘记录。")
            }
            AiReportHelpCard("常见问题") {
                HelpBullet("没收到报告：检查 AI 源测试连接、通知权限和 Android 12+ 的闹钟权限；手机深度休眠时可能延后，可点「立即生成一次日报」手动触发。")
                HelpBullet("报告很简短：通常是 AI 不可用时走了本地模板，只列出完成数量和明天 DDL，检查 AI 配置和网络。")
                HelpBullet("AI 写得不准：报告基于生成时的数据库快照，22:00 之后完成的任务不会进入当天日报；AI 也可能偶尔幻觉，严重错误需要继续改 prompt。")
            }
        }
    }
}

@Composable
private fun AiReportHelpCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun HelpBullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReportScheduleRow(
    title: String,
    summary: String,
    enabled: Boolean,
    timeText: String,
    timeValid: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(
                value = timeText,
                onValueChange = { if (it.length <= 5) onTimeChange(it) },
                modifier = Modifier.size(width = 92.dp, height = 58.dp),
                singleLine = true,
                isError = !timeValid,
                enabled = enabled,
                label = { Text("时间") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                placeholder = { Text("22:00") }
            )
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}

private fun formatReportTime(hour: Int, minute: Int): String {
    return "%02d:%02d".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
}

private fun parseReportTime(value: String): Pair<Int, Int>? {
    val match = Regex("""^\s*(\d{1,2})[:：](\d{1,2})\s*$""").matchEntire(value) ?: return null
    val hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour to minute
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun PlanningAiProviderRow(
    index: Int,
    total: Int,
    provider: PlanningAiProvider,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEdit,
                onLongClick = onDelete
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${index + 1}. ${provider.name.ifBlank { "未命名服务" }}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = provider.baseUrl.ifBlank { "未填写 Base URL" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = provider.model.ifBlank { "未填写模型" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Switch(checked = provider.enabled, onCheckedChange = onToggle)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onMoveUp, enabled = index > 0) {
                    Text("上移")
                }
                TextButton(onClick = onMoveDown, enabled = index < total - 1) {
                    Text("下移")
                }
            }
            TextButton(onClick = onDelete) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun List<PlanningAiProvider>.moveProvider(from: Int, to: Int): List<PlanningAiProvider> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply {
        val item = removeAt(from)
        add(to, item)
    }
}

@Composable
private fun PlanningAiProviderDialog(
    provider: PlanningAiProvider,
    onDismiss: () -> Unit,
    onConfirm: (PlanningAiProvider) -> Unit
) {
    var name by remember(provider.id) { mutableStateOf(provider.name) }
    var baseUrl by remember(provider.id) { mutableStateOf(provider.baseUrl) }
    var apiKey by remember(provider.id) { mutableStateOf(provider.apiKey) }
    var model by remember(provider.id) { mutableStateOf(provider.model) }
    var enabled by remember(provider.id) { mutableStateOf(provider.enabled) }
    var showApiKey by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<PlanningAiTestResult?>(null) }
    var fetchingModels by remember { mutableStateOf(false) }
    var fetchedModels by remember(provider.id) { mutableStateOf<List<String>>(emptyList()) }
    var modelFetchResult by remember { mutableStateOf<PlanningAiModelFetchResult?>(null) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val missingRequired = baseUrl.isBlank() || apiKey.isBlank() || model.isBlank()
    val modelFetchReady = baseUrl.isNotBlank() && apiKey.isNotBlank()

    LaunchedEffect(testResult) {
        if (testResult != null) {
            delay(5_000)
            testResult = null
        }
    }

    fun clearTestResult() {
        testResult = null
    }

    fun clearFetchedModels() {
        fetchedModels = emptyList()
        modelFetchResult = null
        modelMenuExpanded = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (provider.name.isBlank() && provider.baseUrl.isBlank()) "添加 AI 源" else "编辑 AI 源") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("启用此源", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = enabled, onCheckedChange = { enabled = it; clearTestResult() })
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; clearTestResult() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("显示名") },
                    singleLine = true,
                    placeholder = { Text("例如 DeepSeek、Qwen、硅基流动") }
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        clearTestResult()
                        clearFetchedModels()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Base URL") },
                    singleLine = true,
                    isError = baseUrl.isBlank(),
                    placeholder = { Text("例如 https://api.example.com/v1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        clearTestResult()
                        clearFetchedModels()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    singleLine = true,
                    isError = apiKey.isBlank(),
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showApiKey = !showApiKey }) {
                            Text(if (showApiKey) "隐藏" else "显示")
                        }
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                fetchingModels = true
                                fetchedModels = emptyList()
                                modelFetchResult = null
                                clearTestResult()
                                try {
                                    val result = PlanningAiCaller.fetchModels(
                                        provider.copy(
                                            name = name.ifBlank { "未命名服务" },
                                            baseUrl = baseUrl,
                                            apiKey = apiKey,
                                            model = model,
                                            enabled = true
                                        )
                                    )
                                    modelFetchResult = result
                                    if (result is PlanningAiModelFetchResult.Success) {
                                        fetchedModels = result.models
                                        if (model.isBlank() || model !in result.models) {
                                            model = result.models.first()
                                        }
                                    }
                                } finally {
                                    fetchingModels = false
                                }
                            }
                        },
                        enabled = !fetchingModels && modelFetchReady,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (fetchingModels) "获取中..." else "获取模型")
                    }
                    Text(
                        text = if (fetchedModels.isEmpty()) "未获取" else "${fetchedModels.size} 个模型",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (fetchedModels.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                }
                if (fetchedModels.isNotEmpty()) {
                    PlanningAiModelDropdown(
                        selectedModel = model,
                        models = fetchedModels,
                        expanded = modelMenuExpanded,
                        onExpandedChange = { modelMenuExpanded = it },
                        onSelect = { selected ->
                            model = selected
                            modelMenuExpanded = false
                            clearTestResult()
                        }
                    )
                }
                OutlinedTextField(
                    value = model,
                    onValueChange = {
                        model = it
                        clearTestResult()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (fetchedModels.isEmpty()) "模型名" else "模型名（可手动覆盖）") },
                    singleLine = true,
                    isError = model.isBlank(),
                    placeholder = { Text("例如 deepseek-v4-flash / qwen3.6") }
                )
                modelFetchResult?.let { result ->
                    val (message, color) = when (result) {
                        is PlanningAiModelFetchResult.Success -> {
                            "✓ 已从 ${result.endpoint} 获取 ${result.models.size} 个模型" to Color(0xFF1B7F3A)
                        }
                        is PlanningAiModelFetchResult.Failure -> {
                            "✗ 模型获取失败：${result.message}；仍可手动填写模型名。" to MaterialTheme.colorScheme.error
                        }
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            testing = true
                            testResult = null
                            try {
                                testResult = PlanningAiCaller.testProvider(
                                    provider.copy(
                                        name = name.ifBlank { "未命名服务" },
                                        baseUrl = baseUrl,
                                        apiKey = apiKey,
                                        model = model,
                                        enabled = true
                                    )
                                )
                            } finally {
                                testing = false
                            }
                        }
                    },
                    enabled = !testing && !missingRequired,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (testing) "测试中..." else "测试连接")
                }
                testResult?.let { result ->
                    val (message, color) = when (result) {
                        is PlanningAiTestResult.Success -> {
                            "✓ 连接成功，模型：${result.model}，响应：${result.contentLength} 字" to Color(0xFF1B7F3A)
                        }
                        is PlanningAiTestResult.Failure -> {
                            "✗ 失败：${result.message}" to MaterialTheme.colorScheme.error
                        }
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        provider.copy(
                            name = name.ifBlank { "未命名服务" },
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            model = model,
                            enabled = enabled
                        ).normalized()
                    )
                },
                enabled = !missingRequired
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun PlanningAiModelDropdown(
    selectedModel: String,
    models: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit
) {
    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(true) },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("从已获取模型中选择", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = selectedModel.ifBlank { "请选择模型" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            models.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingIcon = {
                        if (option == selectedModel) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}

@Composable
private fun CompactDropdownSetting(
    title: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    summary: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    summary?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        trailingIcon = {
                            if (option == value) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
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
    onOpenWiki: () -> Unit = {},
    onResetOnboarding: () -> Unit = {}
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
            OutlinedButton(onClick = onOpenWiki) {
                Text("打开使用说明")
            }
            SettingsMenuDivider()
            SettingsActionRow(
                title = "重新显示新手引导",
                summary = "下次回到每日看板时重新展示欢迎引导卡",
                actionText = "重新显示",
                onClick = onResetOnboarding
            )
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
private fun ToneChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    summary: String? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (selected) 0.34f else 0.18f),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.46f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                summary?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
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
private fun SettingsActionRow(
    title: String,
    summary: String,
    actionText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = onClick) {
            Text(actionText)
        }
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
