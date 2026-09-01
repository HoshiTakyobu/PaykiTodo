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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.todoalarm.sync.RemoteBackupInspection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.AiReportRetention
import com.example.todoalarm.data.DataHealthCleanResult
import com.example.todoalarm.data.DataHealthReport
import com.example.todoalarm.data.PlanningAiCaller
import com.example.todoalarm.data.PlanningAiModelFetchResult
import com.example.todoalarm.data.PlanningAiProvider
import com.example.todoalarm.data.PlanningAiTestResult
import com.example.todoalarm.data.ReminderAudioChannel
import com.example.todoalarm.data.ReminderChainLog
import com.example.todoalarm.data.ReminderChainStage
import com.example.todoalarm.data.ReminderChainStatus
import com.example.todoalarm.data.WeekStartMode
import com.example.todoalarm.sync.DesktopSyncStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class SettingsSection {
    PERMISSIONS,
    SOUND_STRATEGY,
    TONE,
    CALENDAR,
    DAILY_BRIEF,
    AI_CONFIG,
    ABOUT,
    DIAGNOSTICS,
    BACKUP,
    CRASH,
    DESKTOP_SYNC
}

private data class PlanningAiConfigDraft(
    val enabled: Boolean,
    val providers: List<PlanningAiProvider>,
    val dailyReportEnabled: Boolean,
    val dailyReportTimeText: String,
    val weeklyReportEnabled: Boolean,
    val weeklyReportTimeText: String,
    val retention: AiReportRetention
) {
    val requiredProviderMissing: Boolean
        get() = enabled && providers.filter { it.enabled }.any {
            it.baseUrl.isBlank() || it.apiKey.isBlank() || it.model.isBlank()
        }
    val dailyReportTime: Pair<Int, Int>?
        get() = parseReportTime(dailyReportTimeText)
    val weeklyReportTime: Pair<Int, Int>?
        get() = parseReportTime(weeklyReportTimeText)
    val reportTimeInvalid: Boolean
        get() = (dailyReportEnabled && dailyReportTime == null) ||
            (weeklyReportEnabled && weeklyReportTime == null)
    val valid: Boolean
        get() = !requiredProviderMissing && !reportTimeInvalid

    companion object {
        fun from(settings: AppSettings): PlanningAiConfigDraft {
            val providers = settings.planningAiProviders.ifEmpty {
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
            return PlanningAiConfigDraft(
                enabled = settings.planningAiEnabled,
                providers = providers,
                dailyReportEnabled = settings.dailyReportEnabled,
                dailyReportTimeText = formatReportTime(settings.dailyReportHour, settings.dailyReportMinute),
                weeklyReportEnabled = settings.weeklyReportEnabled,
                weeklyReportTimeText = formatReportTime(settings.weeklyReportHour, settings.weeklyReportMinute),
                retention = settings.aiReportRetention
            )
        }
    }
}

private data class DailyBriefDraft(
    val enabled: Boolean,
    val timeText: String
) {
    val parsedTime: Pair<Int, Int>?
        get() = parseReportTime(timeText)
    val valid: Boolean
        get() = !enabled || parsedTime != null

    companion object {
        fun from(settings: AppSettings): DailyBriefDraft {
            return DailyBriefDraft(
                enabled = settings.dailyBriefEnabled,
                timeText = formatReportTime(settings.dailyBriefHour, settings.dailyBriefMinute)
            )
        }
    }
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
    onEventCheckInPreferencesChange: (Boolean, Boolean) -> Unit,
    onEventCheckInIdleAutoCheckOutHoursChange: (Int) -> Unit,
    onOngoingEventNotificationEnabledChange: (Boolean) -> Unit,
    @Suppress("UNUSED_PARAMETER") onPlanningOutlinerPreferencesChange: (Boolean, Boolean) -> Unit,
    onReminderAudioStrategyChange: (ReminderAudioChannel, Int, Boolean, Int, Boolean) -> Unit,
    onPlanningAiProvidersChange: (Boolean, List<PlanningAiProvider>) -> Unit,
    onReportPreferencesChange: (Boolean, Int, Int, Boolean, Int, Int, AiReportRetention) -> Unit,
    onDailyBriefPreferencesChange: (Boolean, Int, Int) -> Unit,
    onGenerateDailyReportNow: suspend () -> String?,
    onResetOnboarding: () -> Unit,
    onDesktopSyncEnabledChange: (Boolean) -> Unit,
    onDesktopSyncWifiKeepAliveChange: (Boolean) -> Unit,
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
    onRemoteSyncEnabledChange: (Boolean, String) -> Unit,
    onRemoteSyncServerUrlChange: (String) -> Unit,
    onRemoteSyncNow: () -> Unit,
    onInspectRemoteBackup: suspend () -> RemoteBackupInspection?,
    onRestoreRemoteBackup: suspend (RemoteBackupInspection) -> String?,
    onInspectDataHealth: suspend () -> DataHealthReport,
    onCleanSafeDataHealthItems: suspend () -> DataHealthCleanResult,
    onCopyCrashLog: () -> Unit,
    onClearCrashLog: () -> Unit
) {
    val context = LocalContext.current
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showCrashDialog by remember(crashLog) { mutableStateOf(false) }
    var showReminderTestDialog by remember { mutableStateOf(false) }
    var reminderTestRunning by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf<SettingsSection?>(null) }
    var planningAiDraft by remember { mutableStateOf<PlanningAiConfigDraft?>(null) }
    var dailyBriefDraft by remember { mutableStateOf<DailyBriefDraft?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialSectionSerial) {
        selectedSection = when (initialSectionKey) {
            MainActivity.SETTINGS_SECTION_DESKTOP_SYNC -> SettingsSection.DESKTOP_SYNC
            else -> selectedSection
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
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
                    summary = "默认延后时长、周起始日和日程进行中通知",
                    onClick = { selectedSection = SettingsSection.CALENDAR }
                )
                SettingsMenuDivider()
                SettingsMenuItem(
                    icon = Icons.Rounded.Notifications,
                    title = "每日摘要通知",
                    summary = if (settings.dailyBriefEnabled) {
                        "每天 ${formatReportTime(settings.dailyBriefHour, settings.dailyBriefMinute)} 推送今日待办和日程"
                    } else {
                        "已关闭"
                    },
                    onClick = {
                        dailyBriefDraft = DailyBriefDraft.from(settings)
                        selectedSection = SettingsSection.DAILY_BRIEF
                    }
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
                    onClick = {
                        planningAiDraft = PlanningAiConfigDraft.from(settings)
                        selectedSection = SettingsSection.AI_CONFIG
                    }
                )
                SettingsMenuDivider()
                SettingsMenuItem(
                    icon = Icons.AutoMirrored.Rounded.ManageSearch,
                    title = "使用说明",
                    summary = null,
                    onClick = onOpenWiki
                )
                SettingsMenuDivider()
                SettingsMenuItem(
                    icon = Icons.Rounded.Computer,
                    title = "电脑同步",
                    summary = when {
                        desktopSyncStatus.connected -> "已连接"
                        desktopSyncStatus.running -> "等待密钥"
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

                SettingsSwitchRow(
                    title = "日程进行时显示通知",
                    summary = "开启后，日程在进行期间会显示更醒目的常驻通知；不要求单独开启日程提醒，日程结束后自动消失。",
                    checked = settings.ongoingEventNotificationEnabled,
                    onCheckedChange = onOngoingEventNotificationEnabledChange
                )

                Spacer(modifier = Modifier.height(4.dp))
                SettingsSwitchRow(
                    title = "日程结束时自动签退",
                    summary = "开启后，日程结束时会自动结束仍在进行的签到。",
                    checked = settings.autoCheckOutEventOnEnd,
                    onCheckedChange = {
                        onEventCheckInPreferencesChange(
                            it,
                            settings.showEventCheckInStatsOnComplete
                        )
                    }
                )
                CompactDropdownSetting(
                    title = "闲置自动签退阈值",
                    value = when (settings.eventCheckInIdleAutoCheckOutHours) {
                        0 -> "关闭"
                        1 -> "1 小时"
                        2 -> "2 小时"
                        4 -> "4 小时"
                        8 -> "8 小时"
                        else -> "2 小时"
                    },
                    options = listOf("关闭", "1 小时", "2 小时", "4 小时", "8 小时"),
                    onSelect = { label ->
                        onEventCheckInIdleAutoCheckOutHoursChange(
                            when (label) {
                                "关闭" -> 0
                                "1 小时" -> 1
                                "2 小时" -> 2
                                "4 小时" -> 4
                                "8 小时" -> 8
                                else -> 2
                            }
                        )
                    }
                )
            }
        }

        SettingsSection.DAILY_BRIEF -> {
            val draft = dailyBriefDraft ?: DailyBriefDraft.from(settings)
            SettingsSectionDialog(
                title = "每日摘要通知",
                onDismiss = {
                    dailyBriefDraft = null
                    selectedSection = null
                },
                onDone = {
                    val time = draft.parsedTime ?: (settings.dailyBriefHour to settings.dailyBriefMinute)
                    onDailyBriefPreferencesChange(draft.enabled, time.first, time.second)
                    dailyBriefDraft = null
                    selectedSection = null
                },
                doneEnabled = draft.valid
            ) {
                DailyBriefPreferencesPanel(
                    draft = draft,
                    onDraftChange = { dailyBriefDraft = it }
                )
            }
        }

        SettingsSection.AI_CONFIG -> {
            val draft = planningAiDraft ?: PlanningAiConfigDraft.from(settings)
            SettingsSectionDialog(
                title = "AI 调用配置",
                onDismiss = {
                    planningAiDraft = null
                    selectedSection = null
                },
                onDone = {
                    val dailyTime = draft.dailyReportTime ?: (settings.dailyReportHour to settings.dailyReportMinute)
                    val weeklyTime = draft.weeklyReportTime ?: (settings.weeklyReportHour to settings.weeklyReportMinute)
                    onPlanningAiProvidersChange(draft.enabled, draft.providers)
                    onReportPreferencesChange(
                        draft.dailyReportEnabled,
                        dailyTime.first,
                        dailyTime.second,
                        draft.weeklyReportEnabled,
                        weeklyTime.first,
                        weeklyTime.second,
                        draft.retention
                    )
                    planningAiDraft = null
                    selectedSection = null
                },
                doneEnabled = draft.valid
            ) {
                PlanningAiConfigPanel(
                    draft = draft,
                    onDraftChange = { planningAiDraft = it },
                    onGenerateDailyReportNow = onGenerateDailyReportNow
                )
            }
        }

        SettingsSection.ABOUT -> SettingsSectionDialog("关于 PaykiTodo", { selectedSection = null }) {
            AboutPanel(
                onOpenWiki = onOpenWiki,
                onResetOnboarding = onResetOnboarding
            )
        }

        SettingsSection.DIAGNOSTICS -> SettingsSectionDialog("提醒链路诊断", { selectedSection = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReminderReadinessCard(
                    permissions = permissions,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                    onRequestFullScreenPermission = onRequestFullScreenPermission,
                    onRequestNotificationPolicyAccess = onRequestNotificationPolicyAccess,
                    onRequestIgnoreBatteryOptimization = onRequestIgnoreBatteryOptimization,
                    onRequestAccessibilityService = onRequestAccessibilityService
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("一键强提醒测试", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "会创建一条短延迟测试待办，强制使用全屏提醒、响铃和震动。到点后应直接打开提醒界面；如果只出通知栏，下面日志能看到卡在哪一步。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { showReminderTestDialog = true },
                                enabled = !reminderTestRunning
                            ) {
                                if (reminderTestRunning) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Rounded.BugReport, contentDescription = null)
                                }
                                Text(if (reminderTestRunning) "创建中" else "开始测试")
                            }
                            OutlinedButton(
                                onClick = { scope.launch { onClearReminderDiagnostics() } },
                                enabled = !reminderTestRunning
                            ) {
                                Text("清空日志")
                            }
                        }
                    }
                }
                if (reminderChainLogs.isEmpty()) {
                    Text("当前还没有诊断记录。运行一次测试后，最近链路阶段会显示在这里。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                } else {
                    ReminderChainLogList(reminderChainLogs.take(18))
                }
            }
        }

        SettingsSection.BACKUP -> SettingsSectionDialog("数据与备份", { selectedSection = null }) {
            var dataHealthReport by remember { mutableStateOf<DataHealthReport?>(null) }
            var dataHealthScanning by remember { mutableStateOf(false) }
            var dataHealthCleaning by remember { mutableStateOf(false) }
            var dataHealthMessage by remember { mutableStateOf<String?>(null) }
            var dataHealthConfirmVisible by remember { mutableStateOf(false) }
            var remoteSyncServerUrl by remember(settings.remoteSyncServerUrl) { mutableStateOf(settings.remoteSyncServerUrl) }
            var remoteInspection by remember { mutableStateOf<RemoteBackupInspection?>(null) }
            var remoteInspecting by remember { mutableStateOf(false) }
            var remoteRestoring by remember { mutableStateOf(false) }
            var remoteMessage by remember { mutableStateOf<String?>(null) }
            var remoteRestoreConfirmVisible by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自动备份", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Switch(checked = settings.autoBackupEnabled, onCheckedChange = onAutoBackupChange)
                }
                Text(settings.backupDirectoryUri ?: "当前未选择备份目录", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(
                    "迁移包可在卸载 Debug 版后保留，用于把待办、日程、规划文档、历史记录和普通设置迁入正式版。AI API Key 与电脑访问密钥不会写入文件。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onPickBackupDirectory, modifier = Modifier.fillMaxWidth()) { Text("选择备份目录") }
                    OutlinedButton(onClick = onExportBackup, modifier = Modifier.fillMaxWidth()) { Text("导出迁移包") }
                    OutlinedButton(onClick = onImportBackup, modifier = Modifier.fillMaxWidth()) { Text("从迁移包恢复") }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("服务器加密同步", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "手机仍以本地数据库为准；服务器只保存加密迁移包，用于跨网络备份与后续桌面同步。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = settings.remoteSyncEnabled,
                        onCheckedChange = { onRemoteSyncEnabledChange(it, remoteSyncServerUrl) }
                    )
                }
                OutlinedTextField(
                    value = remoteSyncServerUrl,
                    onValueChange = {
                        remoteSyncServerUrl = it
                        onRemoteSyncServerUrlChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("服务器 HTTPS 地址") },
                    placeholder = { Text("https://panel.example.com/paykitodo-sync") },
                    supportingText = { Text("必须使用 HTTPS；例如部署在 Nginx 路径 /paykitodo-sync 下。") }
                )
                if (settings.remoteSyncEnabled && settings.remoteSyncDeviceId.isNotBlank() && settings.remoteSyncSecret.isNotBlank()) {
                    RemoteSyncCredentialRows(
                        deviceId = settings.remoteSyncDeviceId,
                        secret = settings.remoteSyncSecret,
                        context = context
                    )
                }
                OutlinedButton(
                    onClick = onRemoteSyncNow,
                    enabled = settings.remoteSyncEnabled && remoteSyncServerUrl.startsWith("https://", ignoreCase = true),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("立即上传本机加密数据") }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            remoteInspecting = true
                            remoteMessage = null
                            remoteInspection = runCatching { onInspectRemoteBackup() }
                                .onFailure { remoteMessage = "检查失败：${it.message ?: it::class.java.simpleName}" }
                                .getOrNull()
                            if (remoteInspection == null && remoteMessage == null) {
                                remoteMessage = "服务器上还没有可恢复的快照。"
                            }
                            remoteInspecting = false
                        }
                    },
                    enabled = settings.remoteSyncEnabled && remoteSyncServerUrl.startsWith("https://", ignoreCase = true) && !remoteInspecting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (remoteInspecting) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("检查远程备份")
                }
                remoteMessage?.let { message ->
                    Text(
                        message,
                        color = if (message.contains("失败") || message.contains("错误")) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                remoteInspection?.let { inspection ->
                    RemoteBackupInspectionCard(
                        inspection = inspection,
                        restoring = remoteRestoring,
                        onRestore = { remoteRestoreConfirmVisible = true }
                    )
                }
                if (remoteRestoreConfirmVisible && remoteInspection != null) {
                    AlertDialog(
                        onDismissRequest = { if (!remoteRestoring) remoteRestoreConfirmVisible = false },
                        title = { Text("确认恢复远程快照？") },
                        text = {
                            Text(
                                "这会用服务器快照替换当前本地待办、日程、规划文档和历史数据。恢复前会自动保存当前本地快照，API Key、电脑同步密钥和服务器同步密钥继续保留在本机。"
                            )
                        },
                        confirmButton = {
                            TextButton(
                                enabled = !remoteRestoring,
                                onClick = {
                                    remoteInspection?.let { selected ->
                                        scope.launch {
                                            remoteRestoring = true
                                            val error = onRestoreRemoteBackup(selected)
                                            if (error == null) {
                                                remoteInspection = null
                                                remoteMessage = "远程快照恢复完成，当前本地数据已替换。"
                                            } else {
                                                remoteMessage = error
                                            }
                                            remoteRestoring = false
                                            remoteRestoreConfirmVisible = false
                                        }
                                    }
                                }
                            ) { Text("确认恢复") }
                        },
                        dismissButton = {
                            TextButton(enabled = !remoteRestoring, onClick = { remoteRestoreConfirmVisible = false }) {
                                Text("暂不恢复")
                            }
                        }
                    )
                }
                HorizontalDivider()
                DataHealthPanel(
                    report = dataHealthReport,
                    scanning = dataHealthScanning,
                    cleaning = dataHealthCleaning,
                    message = dataHealthMessage,
                    onScan = {
                        scope.launch {
                            dataHealthScanning = true
                            dataHealthMessage = null
                            try {
                                dataHealthReport = onInspectDataHealth()
                            } catch (error: Exception) {
                                dataHealthMessage = "检查失败：${error.message ?: error::class.java.simpleName}"
                            } finally {
                                dataHealthScanning = false
                            }
                        }
                    },
                    onCleanSafeItems = {
                        if ((dataHealthReport?.safeCleanableCount ?: 0) > 0) {
                            dataHealthConfirmVisible = true
                        } else {
                            dataHealthMessage = "当前没有可一键清理的安全项。"
                        }
                    }
                )
                if (dataHealthConfirmVisible) {
                    DataHealthCleanConfirmDialog(
                        report = dataHealthReport ?: DataHealthReport(),
                        cleaning = dataHealthCleaning,
                        onDismiss = { if (!dataHealthCleaning) dataHealthConfirmVisible = false },
                        onConfirm = {
                            scope.launch {
                                dataHealthCleaning = true
                                dataHealthMessage = null
                                try {
                                    val result = onCleanSafeDataHealthItems()
                                    dataHealthMessage = result.message()
                                    dataHealthReport = onInspectDataHealth()
                                } catch (error: Exception) {
                                    dataHealthMessage = "清理失败：${error.message ?: error::class.java.simpleName}"
                                } finally {
                                    dataHealthCleaning = false
                                    dataHealthConfirmVisible = false
                                }
                            }
                        }
                    )
                }
            }
        }

        SettingsSection.DESKTOP_SYNC -> SettingsSectionDialog("电脑同步", { selectedSection = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val syncStatusTitle = desktopSyncConnectionTitle(settings, desktopSyncStatus)
                val syncStatusSummary = desktopSyncConnectionSummary(settings, desktopSyncStatus)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("启用电脑同步", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            syncStatusTitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = settings.desktopSyncEnabled, onCheckedChange = onDesktopSyncEnabledChange)
                }
                Text(
                    "开启后 5 分钟内如果没有电脑端用正确访问密钥完成连接，会自动关闭电脑同步；只打开网页但未通过密钥不算已连接。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (syncStatusSummary.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (desktopSyncStatus.connected) 0.12f else 0.08f)
                    ) {
                        Text(
                            text = syncStatusSummary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                        )
                    }
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
                SettingsSwitchRow(
                    title = "桌面同步期间保持网络唤醒",
                    summary = "保证浏览器在你熄屏时仍能连接，会增加少量电量消耗。",
                    checked = settings.desktopSyncWifiKeepAlive,
                    onCheckedChange = onDesktopSyncWifiKeepAliveChange
                )
                if (settings.desktopSyncEnabled) {
                    Text("连接地址", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    if (!desktopSyncStatus.running) {
                        Text("电脑同步服务正在启动。如果几秒后仍没有出现地址，请关闭再重新开启电脑同步。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (desktopSyncStatus.ipAddresses.isEmpty()) {
                        Text(
                            desktopSyncStatus.networkHint.ifBlank { "未检测到可用的局域网 IPv4 地址。" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                desktopSyncStatus.networkHint.ifBlank { "请让电脑和手机处于同一个 Wi-Fi / 手机热点局域网，再在电脑浏览器打开下方地址。" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            desktopSyncStatus.ipAddresses.forEachIndexed { index, ip ->
                                val url = "http://$ip:${desktopSyncStatus.port}"
                                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = if (index == 0) "推荐局域网地址" else "可选局域网地址",
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = url,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        OutlinedButton(onClick = {
                                            val clip = context.getSystemService(android.content.ClipboardManager::class.java)
                                            clip?.setPrimaryClip(android.content.ClipData.newPlainText("url", url))
                                            Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
                                        }) { Text("复制") }
                                    }
                                }
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
                    reminderTestRunning = true
                    val message = runCatching { onRunReminderChainTest(seconds) }
                        .getOrElse { it.message ?: "测试提醒创建失败" }
                    reminderTestRunning = false
                    Toast.makeText(
                        context,
                        message ?: "已创建 ${seconds} 秒后的全屏提醒测试",
                        Toast.LENGTH_SHORT
                    ).show()
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
private fun ReminderReadinessCard(
    permissions: PermissionSnapshot,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestFullScreenPermission: () -> Unit,
    onRequestNotificationPolicyAccess: () -> Unit,
    onRequestIgnoreBatteryOptimization: () -> Unit,
    onRequestAccessibilityService: () -> Unit
) {
    val criticalOk = permissions.notificationGranted &&
        permissions.exactAlarmGranted &&
        permissions.fullScreenGranted
    val containerColor = if (criticalOk) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.36f)
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (criticalOk) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.error.copy(alpha = 0.28f)
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = if (criticalOk) "强提醒硬前提已满足" else "强提醒硬前提未满足",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (criticalOk) {
                    "现在可以运行测试提醒，验证锁屏 / 后台时是否真的弹出全屏界面。"
                } else {
                    "通知、精确闹钟或全屏权限缺失时，到点很可能只剩普通通知，甚至不准时。"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            ReadinessCheckRow(
                title = "通知权限",
                summary = "没有它，提醒通知和前台服务提示可能无法显示。",
                ok = permissions.notificationGranted,
                level = "必要",
                onClick = onRequestNotificationPermission
            )
            ReadinessCheckRow(
                title = "精确闹钟",
                summary = "没有它，Android 可能把提醒延后到系统方便的时候。",
                ok = permissions.exactAlarmGranted,
                level = "必要",
                onClick = onRequestExactAlarmPermission
            )
            ReadinessCheckRow(
                title = "全屏提醒权限",
                summary = "Android 14+ 会用它决定能否直接弹出全屏提醒界面。",
                ok = permissions.fullScreenGranted,
                level = "必要",
                onClick = onRequestFullScreenPermission
            )
            ReadinessCheckRow(
                title = "忽略电池优化",
                summary = "建议开启；厂商省电策略可能杀后台服务或延后闹钟。",
                ok = permissions.batteryOptimizationIgnored,
                level = "建议",
                onClick = onRequestIgnoreBatteryOptimization
            )
            ReadinessCheckRow(
                title = "免打扰穿透",
                summary = "如果你经常开免打扰，建议授权，否则响铃可能被系统压掉。",
                ok = permissions.dndAccessGranted,
                level = "增强",
                onClick = onRequestNotificationPolicyAccess
            )
            ReadinessCheckRow(
                title = "辅助功能兜底",
                summary = "当系统限制全屏启动时，可作为强提醒兜底方案。",
                ok = permissions.accessibilityServiceEnabled,
                level = "兜底",
                onClick = onRequestAccessibilityService
            )
        }
    }
}

@Composable
private fun ReadinessCheckRow(
    title: String,
    summary: String,
    ok: Boolean,
    level: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (ok) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
            ) {
                Text(
                    text = if (ok) "已开" else "未开",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(level, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReminderChainLogList(logs: List<ReminderChainLog>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "最近链路日志",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        logs.forEach { log ->
            val isError = log.status == ReminderChainStatus.ERROR || log.status == ReminderChainStatus.WARN
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isError) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.34f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                },
                border = BorderStroke(
                    1.dp,
                    if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.24f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${readableReminderStage(log.stage)} · ${readableReminderStatus(log.status)}",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            formatReminderLogTime(log.createdAtMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("todoId=${log.todoId} · ${log.source}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    log.message?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun readableReminderStage(stage: String): String {
    return when (stage) {
        ReminderChainStage.TEST_CREATED -> "测试任务已创建"
        ReminderChainStage.SCHEDULE_REQUESTED -> "请求调度"
        ReminderChainStage.SCHEDULED -> "闹钟已调度"
        ReminderChainStage.SCHEDULE_FAILED -> "调度失败"
        ReminderChainStage.RECEIVER_EXACT -> "精确闹钟触发"
        ReminderChainStage.RECEIVER_BACKUP -> "备用触发"
        ReminderChainStage.POLL_DISPATCH -> "轮询派发"
        ReminderChainStage.SERVICE_START -> "前台服务启动"
        ReminderChainStage.SERVICE_ITEM_LOADED -> "提醒事项已读取"
        ReminderChainStage.SERVICE_INVALID_ITEM -> "提醒事项无效"
        ReminderChainStage.NOTIFICATION_POSTED -> "通知已发出"
        ReminderChainStage.FULLSCREEN_ATTEMPT -> "尝试全屏"
        ReminderChainStage.FULLSCREEN_ACTIVITY_LAUNCH -> "全屏界面已启动"
        ReminderChainStage.FULLSCREEN_ACTIVITY_FAILED -> "全屏启动失败"
        ReminderChainStage.ACCESSIBILITY_TRIGGER -> "辅助功能兜底触发"
        ReminderChainStage.ACCESSIBILITY_OVERLAY -> "辅助浮层显示"
        ReminderChainStage.REMINDER_ACTIVITY_RESUME -> "全屏界面可见"
        ReminderChainStage.USER_COMPLETE -> "用户完成"
        ReminderChainStage.USER_SNOOZE -> "用户延后"
        ReminderChainStage.USER_ACKNOWLEDGE -> "用户知道了"
        ReminderChainStage.USER_CANCEL -> "用户取消"
        else -> stage
    }
}

private fun readableReminderStatus(status: String): String {
    return when (status) {
        ReminderChainStatus.OK -> "正常"
        ReminderChainStatus.INFO -> "记录"
        ReminderChainStatus.WARN -> "警告"
        ReminderChainStatus.ERROR -> "错误"
        else -> status
    }
}

private val ReminderLogTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

private fun formatReminderLogTime(millis: Long): String {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(ReminderLogTimeFormatter)
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
private fun PlanningAiConfigPanel(
    draft: PlanningAiConfigDraft,
    onDraftChange: (PlanningAiConfigDraft) -> Unit,
    onGenerateDailyReportNow: suspend () -> String?
) {
    var editingProvider by remember { mutableStateOf<PlanningAiProvider?>(null) }
    var deleteTarget by remember { mutableStateOf<PlanningAiProvider?>(null) }
    val enabledProviders = draft.providers.filter { it.enabled }
    val completeEnabledProviders = enabledProviders.filter {
        it.baseUrl.isNotBlank() && it.apiKey.isNotBlank() && it.model.isNotBlank()
    }
    val incompleteEnabledCount = enabledProviders.size - completeEnabledProviders.size
    val visionEnabledCount = completeEnabledProviders.count { it.supportsVision }
    val aiSummary = when {
        !draft.enabled -> "AI 识别当前关闭；规划台会只使用本地规则。"
        completeEnabledProviders.isEmpty() -> "当前没有可用于规划台识别的完整 AI 源；会自动回退本地规则。"
        else -> "当前可用于规划台识别：${completeEnabledProviders.size} 个；可用于图片识别：${visionEnabledCount} 个；配置不完整：${incompleteEnabledCount} 个。"
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("AI 识别", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    aiSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "AI 只生成预览候选，不会直接写入待办或日程。点击右上角“完成”后，本页全部改动才会生效。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        PrefCard("AI 源") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("启用 AI 识别", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("按列表顺序调用，失败后才尝试下一个源。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = draft.enabled,
                    onCheckedChange = { onDraftChange(draft.copy(enabled = it)) }
                )
            }

            if (draft.providers.isEmpty()) {
                Text("当前还没有 AI 源。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                draft.providers.forEachIndexed { index, provider ->
                    PlanningAiProviderRow(
                        index = index,
                        total = draft.providers.size,
                        provider = provider,
                        onToggle = { checked ->
                            onDraftChange(
                                draft.copy(providers = draft.providers.map {
                                    if (it.id == provider.id) it.copy(enabled = checked) else it
                                })
                            )
                        },
                        onMoveUp = {
                            onDraftChange(draft.copy(providers = draft.providers.moveProvider(index, index - 1)))
                        },
                        onMoveDown = {
                            onDraftChange(draft.copy(providers = draft.providers.moveProvider(index, index + 1)))
                        },
                        onEdit = { editingProvider = provider },
                        onDelete = { deleteTarget = provider }
                    )
                }
            }

            if (draft.requiredProviderMissing) {
                Text(
                    text = "启用的 AI 源需要填写 Base URL、API Key 和模型名。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "API Key 只保存在本机设置中，备份 JSON 不会导出密钥。",
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
        }

        AiReportPreferencesPanel(
            draft = draft,
            onDraftChange = onDraftChange,
            onGenerateDailyReportNow = onGenerateDailyReportNow
        )
    }

    editingProvider?.let { provider ->
        PlanningAiProviderDialog(
            provider = provider,
            onDismiss = { editingProvider = null },
            onConfirm = { updated ->
                val updatedProviders = if (draft.providers.any { it.id == updated.id }) {
                    draft.providers.map { if (it.id == updated.id) updated else it }
                } else {
                    draft.providers + updated
                }
                onDraftChange(draft.copy(providers = updatedProviders))
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
                    onDraftChange(draft.copy(providers = draft.providers.filterNot { it.id == provider.id }))
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
private fun DailyBriefPreferencesPanel(
    draft: DailyBriefDraft,
    onDraftChange: (DailyBriefDraft) -> Unit
) {
    PrefCard("每日摘要") {
        Text(
            text = "每天在设定时间推送一条轻量通知，汇总今日待办、今日日程和 7 天内最近倒数日。点击右上角“完成”后生效。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ReportScheduleRow(
            title = "每天发送今日摘要",
            summary = "默认 08:00，不会全屏打扰",
            enabled = draft.enabled,
            timeText = draft.timeText,
            timeValid = draft.parsedTime != null,
            onEnabledChange = { onDraftChange(draft.copy(enabled = it)) },
            onTimeChange = { onDraftChange(draft.copy(timeText = it)) }
        )
        if (!draft.valid) {
            Text("时间格式需要是 HH:mm，例如 08:00。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AiReportPreferencesPanel(
    draft: PlanningAiConfigDraft,
    onDraftChange: (PlanningAiConfigDraft) -> Unit,
    onGenerateDailyReportNow: suspend () -> String?
) {
    var runningNow by remember { mutableStateOf(false) }
    var runMessage by remember { mutableStateOf<String?>(null) }
    var showHelpSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    PrefCard("AI 日报 / 周报") {
        Text(
            text = "日报会在每天设定时间生成并写入「AI 报告」归档（抽屉 → AI 报告）；周报会在每周日设定时间写入同一归档。AI 配置不可用时会用本地模板兜底。若未开启「精确闹钟」权限，系统可能延后生成。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ReportScheduleRow(
            title = "启用日报",
            summary = "每天生成，默认 22:00",
            enabled = draft.dailyReportEnabled,
            timeText = draft.dailyReportTimeText,
            timeValid = draft.dailyReportTime != null,
            onEnabledChange = {
                runMessage = null
                onDraftChange(draft.copy(dailyReportEnabled = it))
            },
            onTimeChange = {
                runMessage = null
                onDraftChange(draft.copy(dailyReportTimeText = it))
            }
        )
        ReportScheduleRow(
            title = "启用周报",
            summary = "每周日生成，默认 22:00",
            enabled = draft.weeklyReportEnabled,
            timeText = draft.weeklyReportTimeText,
            timeValid = draft.weeklyReportTime != null,
            onEnabledChange = {
                runMessage = null
                onDraftChange(draft.copy(weeklyReportEnabled = it))
            },
            onTimeChange = {
                runMessage = null
                onDraftChange(draft.copy(weeklyReportTimeText = it))
            }
        )
        CompactDropdownSetting(
            title = "报告保留时长",
            value = draft.retention.label,
            options = AiReportRetention.entries.map { it.label },
            onSelect = { label ->
                onDraftChange(
                    draft.copy(
                        retention = AiReportRetention.entries.firstOrNull { it.label == label }
                            ?: AiReportRetention.DAYS_90
                    )
                )
            },
            summary = "超过保留期的 AI 日报 / 周报会在生成新报告后自动清理；选“永久”则不自动删除。"
        )
        if (draft.reportTimeInvalid) {
            Text("时间格式需要是 HH:mm，例如 22:00。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
                        val finalMessage = message ?: "AI 日报已生成，并写入「AI 报告」归档。"
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
        runMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }

    if (showHelpSheet) {
        AiReportHelpSheet(onDismiss = { showHelpSheet = false })
    }
}

@Composable
private fun AiReportHelpSheet(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "AI 日报怎么用",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            AiReportHelpCard("什么是 AI 日报") {
                HelpBullet("AI 日报会汇总今天完成的待办、错过的待办、今日和明日日程、明日 DDL。")
                HelpBullet("只有当天存在日程签到记录时，日报才会统计投入分钟数；没有签到数据时不会写“投入 0 分钟”。")
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
                HelpBullet("3. 点击右上角「完成」一次性保存 AI 源和日报 / 周报设置。")
                HelpBullet("4. 首次使用建议点击「立即生成一次日报」验证链路。")
                HelpBullet("5. 报告生成后会有通知栏推送，点击通知可直达对应报告详情。")
            }
            AiReportHelpCard("报告在哪看") {
                HelpBullet("通知栏点击「AI 日报已生成」通知，会自动打开对应报告详情。")
                HelpBullet("也可以从抽屉进入「AI 报告」，再点击列表中的某条日报或周报查看详情。")
                HelpBullet("报告以归档形式保存，不占用规划台文档列表，可以随时翻看历史。")
            }
            AiReportHelpCard("常见问题") {
                HelpBullet("没收到报告：检查 AI 源测试连接、通知权限和 Android 12+ 的闹钟权限；手机深度休眠时可能延后，可点「立即生成一次日报」手动触发。")
                HelpBullet("报告很简短：通常是 AI 不可用时走了本地模板，只列出完成数量和明天 DDL，检查 AI 配置和网络。")
                HelpBullet("AI 写得不准：报告基于生成时的数据库快照，22:00 之后完成的任务不会进入当天日报；AI 也可能偶尔幻觉，严重错误需要继续改 prompt。")
            }
        }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    )
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
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            OutlinedTextField(
                value = timeText,
                onValueChange = { if (it.length <= 5) onTimeChange(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = !timeValid,
                enabled = enabled,
                label = { Text("生成时间") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                placeholder = { Text("22:00") }
            )
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
    var menuExpanded by remember { mutableStateOf(false) }
    val isComplete = provider.baseUrl.isNotBlank() && provider.apiKey.isNotBlank() && provider.model.isNotBlank()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEdit,
                onLongClick = onDelete
            ),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "${index + 1}. ${provider.name.ifBlank { "未命名服务" }}",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isComplete) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = if (isComplete) "已配置" else "待补全",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Text(
                        text = provider.baseUrl.ifBlank { "未填写 Base URL" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = provider.model.ifBlank { "未填写模型；编辑后可先获取模型列表" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (provider.supportsVision) {
                        Text(
                            text = "已开启图片识别支持",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Switch(checked = provider.enabled, onCheckedChange = onToggle)
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "更多操作")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("上移") },
                            enabled = index > 0,
                            onClick = {
                                menuExpanded = false
                                onMoveUp()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("下移") },
                            enabled = index < total - 1,
                            onClick = {
                                menuExpanded = false
                                onMoveDown()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
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

private fun desktopSyncConnectionTitle(
    settings: AppSettings,
    status: DesktopSyncStatus
): String {
    return when {
        !settings.desktopSyncEnabled -> "当前服务未运行"
        status.connected -> "已连接电脑端"
        status.running -> "等待电脑端输入访问密钥"
        else -> "正在启动服务，请稍候刷新连接地址"
    }
}

private fun desktopSyncConnectionSummary(
    settings: AppSettings,
    status: DesktopSyncStatus
): String {
    if (!settings.desktopSyncEnabled) return ""
    val remaining = status.secondsUntilAutoStop?.let(::formatDesktopSyncRemaining)
    return when {
        status.connected && remaining != null -> "已检测到电脑端授权心跳；如果电脑断开或停止访问，约 $remaining 后会自动关闭电脑同步。"
        status.connected -> "已检测到电脑端授权心跳。"
        status.running && remaining != null -> "服务已启动，等待电脑端用正确访问密钥连接；如果一直未连接，约 $remaining 后自动关闭。"
        status.running -> "服务已启动，等待电脑端用正确访问密钥连接。"
        else -> "同步开关已打开，但服务尚未报告运行状态。"
    }
}

private fun formatDesktopSyncRemaining(seconds: Long): String {
    val normalized = seconds.coerceAtLeast(0L)
    if (normalized < 60L) return "${normalized} 秒"
    val minutes = normalized / 60L
    val extraSeconds = normalized % 60L
    return if (extraSeconds == 0L) {
        "${minutes} 分钟"
    } else {
        "${minutes} 分 ${extraSeconds} 秒"
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
    var supportsVision by remember(provider.id) { mutableStateOf(provider.supportsVision) }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("此服务支持图片识别", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "只有确认模型支持视觉输入时再开启；规划台识图只会调用已开启此项的 AI 源。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = supportsVision, onCheckedChange = { supportsVision = it; clearTestResult() })
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
                Text(
                    text = "测试只发送 1 条固定消息，不自动重试或切换接口地址。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                testResult?.let { result ->
                    val (message, color) = when (result) {
                        is PlanningAiTestResult.Success -> {
                            "✓ 连接成功；配置模型：${result.model}；服务回答：${result.responseText}" to Color(0xFF1B7F3A)
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
                            enabled = enabled,
                            supportsVision = supportsVision
                        ).normalized()
                    )
                },
                enabled = !missingRequired
            ) {
                Text("完成")
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
private fun DataHealthPanel(
    report: DataHealthReport?,
    scanning: Boolean,
    cleaning: Boolean,
    message: String?,
    onScan: () -> Unit,
    onCleanSafeItems: () -> Unit
) {
    PrefCard("数据健康检查") {
        Text(
            text = "扫描长期使用后可能堆积的数据。安全清理只会删除 30 天前已完成待办、空规划文档、超出保留策略的 AI 报告；草稿节点和未处理过期待办只提示，不自动删除。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (report == null) {
            Text(
                text = "尚未检查。点击下方按钮后会统计可清理项和需要手动处理的项目。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            DataHealthMetricRow("已完成待办（超过 30 天）", report.oldCompletedTodos, safe = true)
            DataHealthMetricRow("空规划文档", report.emptyPlanningNotes, safe = true)
            DataHealthMetricRow("过期 AI 报告", report.expiredAiReports, safe = true)
            DataHealthMetricRow("过期草稿节点（超过 14 天）", report.staleDraftNodes, safe = false)
            DataHealthMetricRow("无提醒的过期待办", report.overdueTodosWithoutReminder, safe = false)
            Text(
                text = if (report.hasIssues) {
                    "可一键清理安全项 ${report.safeCleanableCount} 条；另有 ${report.attentionCount} 条需要手动查看。"
                } else {
                    "当前没有发现需要处理的数据健康问题。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (report.hasIssues) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        message?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (it.contains("失败")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onScan,
                enabled = !scanning && !cleaning,
                modifier = Modifier.weight(1f)
            ) {
                if (scanning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                }
                Text(if (scanning) "检查中" else "开始检查")
            }
            Button(
                onClick = onCleanSafeItems,
                enabled = !scanning && !cleaning && (report?.safeCleanableCount ?: 0) > 0,
                modifier = Modifier.weight(1f)
            ) {
                if (cleaning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                }
                Text(if (cleaning) "清理中" else "一键清理安全项")
            }
        }
    }
}

@Composable
private fun DataHealthMetricRow(
    label: String,
    count: Int,
    safe: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (safe) "✓" else "!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (safe) MaterialTheme.colorScheme.primary else Color(0xFFD08700)
            )
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${count} 条",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RemoteBackupInspectionCard(
    inspection: RemoteBackupInspection,
    restoring: Boolean,
    onRestore: () -> Unit
) {
    val remote = inspection.remote
    val local = inspection.local
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("远程快照已找到", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "导出时间：${formatRemoteBackupTime(remote.exportedAtMillis)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "服务器：${remote.todoCount} 条待办、${remote.eventCount} 条日程、${remote.planningNoteCount} 个规划文档、${remote.historyCount} 条历史事项",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "本机：${local.todoCount} 条待办、${local.eventCount} 条日程、${local.planningNoteCount} 个规划文档、${local.historyCount} 条历史事项",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val diff = inspection.diff
            Text(
                if (!diff.hasChanges) {
                    "差异：按事项 ID 对比，待办、日程和规划文档没有发现内容差异。"
                } else {
                    "差异：待办新增 ${diff.addedTodos}、缺失 ${diff.removedTodos}、变更 ${diff.changedTodos}；" +
                        "日程新增 ${diff.addedEvents}、缺失 ${diff.removedEvents}、变更 ${diff.changedEvents}；" +
                        "规划文档新增 ${diff.addedPlanningNotes}、缺失 ${diff.removedPlanningNotes}、变更 ${diff.changedPlanningNotes}。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (diff.hasChanges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "这里只查看摘要，不会自动覆盖本地数据。确认恢复前会先自动保存当前本地快照。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
            OutlinedButton(onClick = onRestore, enabled = !restoring, modifier = Modifier.fillMaxWidth()) {
                if (restoring) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("查看后恢复此快照")
            }
        }
    }
}

private fun formatRemoteBackupTime(millis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(millis))
}

@Composable
private fun RemoteSyncCredentialRows(
    deviceId: String,
    secret: String,
    context: android.content.Context
) {
    PrefCard("服务器模式连接信息") {
        Text(
            "把下面的设备 ID 和远程同步密钥填写到 Windows 客户端的服务器模式。密钥只在本机显示和使用，不会写入迁移包。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingsCopyRow("设备 ID", deviceId, context)
        SettingsCopyRow("远程同步密钥", secret, context)
    }
}

@Composable
private fun SettingsCopyRow(
    label: String,
    value: String,
    context: android.content.Context
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        OutlinedButton(onClick = {
            val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
            clipboard?.setPrimaryClip(android.content.ClipData.newPlainText(label, value))
            Toast.makeText(context, "${label}已复制", Toast.LENGTH_SHORT).show()
        }) {
            Text("复制")
        }
    }
}

@Composable
private fun DataHealthCleanConfirmDialog(
    report: DataHealthReport,
    cleaning: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认清理安全项？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "将清理 ${report.oldCompletedTodos} 条 30 天前已完成待办、${report.emptyPlanningNotes} 个空规划文档、${report.expiredAiReports} 条过期 AI 报告。此操作不可撤销，建议先导出 JSON 备份。",
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "不会自动删除过期草稿节点或未处理过期待办。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !cleaning
            ) {
                Text("确认清理", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !cleaning) {
                Text("取消")
            }
        }
    )
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
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
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
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        modifier = Modifier.padding(start = 58.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)
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
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            summary?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSectionDialog(
    title: String,
    onDismiss: () -> Unit,
    onDone: () -> Unit = onDismiss,
    doneEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onDone, enabled = doneEnabled) {
                    Text("完成")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun PrefCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
private fun SettingsSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
