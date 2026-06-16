package com.example.todoalarm.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.todoalarm.R
import com.example.todoalarm.data.AiReport
import com.example.todoalarm.data.AiReportType
import com.example.todoalarm.data.AiReportRetention
import com.example.todoalarm.data.CalendarEventDraft
import com.example.todoalarm.data.DataHealthCleanResult
import com.example.todoalarm.data.DataHealthReport
import com.example.todoalarm.data.DailyBoardSnapshotBuilder
import com.example.todoalarm.data.EventCheckIn
import com.example.todoalarm.data.EventCheckInCompletionSummary
import com.example.todoalarm.data.PlanningAiProvider
import com.example.todoalarm.data.PlanningImportCandidate
import com.example.todoalarm.data.PlanningImportResult
import com.example.todoalarm.data.PlanningLineMapping
import com.example.todoalarm.data.PlanningNode
import com.example.todoalarm.data.PlanningNodeDraft
import com.example.todoalarm.data.PlanningNodeEdit
import com.example.todoalarm.data.PlanningNodeSnapshot
import com.example.todoalarm.data.PlanningNote
import com.example.todoalarm.data.PlanningOperationResult
import com.example.todoalarm.data.PlanningParseResult
import com.example.todoalarm.data.PlanningPostponeScope
import com.example.todoalarm.data.PlanningRefreshScope
import com.example.todoalarm.data.ReminderDeliveryMode
import com.example.todoalarm.data.ReminderAudioChannel
import com.example.todoalarm.data.ReminderChainLog
import com.example.todoalarm.data.ScheduleTemplate
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.ThemeMode
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.TodoRepository
import com.example.todoalarm.data.WeekStartMode
import com.example.todoalarm.ui.theme.PaykiGreetingFontFamily
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val UpcomingRecurringExpandedLimit = 30

@Composable
internal fun DashboardBackgroundBrush(): Brush {
    val colors = MaterialTheme.colorScheme
    return Brush.linearGradient(
        colors = listOf(
            colors.primary.copy(alpha = 0.16f),
            colors.tertiary.copy(alpha = 0.13f),
            colors.secondary.copy(alpha = 0.10f),
            colors.surfaceVariant.copy(alpha = 0.12f),
            colors.background
        ),
        start = Offset.Zero,
        end = Offset(1300f, 2100f)
    )
}

internal enum class DashboardSection(
    val label: String,
    val icon: ImageVector,
    val topBarTitle: String
) {
    BOARD("每日看板", Icons.Rounded.ViewAgenda, "每日看板"),
    ACTIVE("待办", Icons.Rounded.TaskAlt, "待办"),
    CALENDAR("日历", Icons.Rounded.CalendarMonth, "日历"),
    PLANNING("规划台", Icons.Rounded.PostAdd, "规划台"),
    AI_REPORTS("AI 报告", Icons.Rounded.Insights, "AI 报告"),
    HISTORY("历史记录", Icons.Rounded.History, "历史记录"),
    SETTINGS("设置", Icons.Rounded.Settings, "设置")
}

@Composable
internal fun DashboardDrawer(
    current: DashboardSection,
    selectedThemeMode: ThemeMode,
    onSelectSection: (DashboardSection) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    var moreExpanded by rememberSaveable { mutableStateOf(false) }
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.75f)
            .widthIn(min = 280.dp, max = 420.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 18.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_art_transparent),
                                contentDescription = "应用图标",
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Text(
                        "PaykiTodo",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                Column(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(
                        DashboardSection.BOARD,
                        DashboardSection.ACTIVE,
                        DashboardSection.CALENDAR,
                        DashboardSection.PLANNING,
                        DashboardSection.SETTINGS
                    ).forEach { section ->
                        DrawerSectionButton(
                            section = section,
                            selected = current == section,
                            onClick = { onSelectSection(section) }
                        )
                    }
                    DrawerSectionButton(
                        label = "更多",
                        icon = Icons.Rounded.MoreHoriz,
                        selected = false,
                        onClick = { moreExpanded = !moreExpanded },
                        trailingContent = {
                            Icon(
                                imageVector = if (moreExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (moreExpanded) "折叠更多" else "展开更多",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    )
                    if (moreExpanded) {
                        listOf(DashboardSection.AI_REPORTS, DashboardSection.HISTORY).forEach { section ->
                            DrawerSectionButton(
                                section = section,
                                selected = current == section,
                                onClick = { onSelectSection(section) }
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                ThemeModeSwitcher(
                    selectedThemeMode = selectedThemeMode,
                    onThemeModeChange = onThemeModeChange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardTopBar(
    title: String,
    onMenu: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            IconButton(onClick = onMenu) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Menu, contentDescription = "打开菜单", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f))
                    }
                }
            }
        },
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.22f),
                        offset = Offset(0f, 2f),
                        blurRadius = 7f
                    )
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        actions = actions
    )
}

@Composable
internal fun TopBarActionPill(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.PostAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun DashboardBody(
    section: DashboardSection,
    settingsInitialSectionKey: String? = null,
    settingsInitialSectionSerial: Int = 0,
    targetEventId: Long? = null,
    targetEventSerial: Int = 0,
    calendarFocusDate: LocalDate? = null,
    calendarFocusDateSerial: Int = 0,
    targetAiReportId: Long? = null,
    targetAiReportSerial: Int = 0,
    highlightedPlanningNodeId: Long? = null,
    highlightedPlanningNodeSerial: Int = 0,
    padding: PaddingValues,
    uiState: TodoUiState,
    planningNotes: StateFlow<List<PlanningNote>>,
    observeAiReports: (AiReportType?, Int, String, Long, Long) -> Flow<List<AiReport>>,
    onGetAiReport: suspend (Long) -> AiReport?,
    onGetTodoById: suspend (Long) -> TodoItem?,
    onGetEventCheckIns: suspend (Long) -> List<EventCheckIn>,
    onCompleteCalendarEvent: suspend (Long) -> EventCheckInCompletionSummary?,
    historyItems: StateFlow<List<TodoItem>>,
    calendarItems: StateFlow<List<TodoItem>>,
    scheduleTemplates: StateFlow<List<ScheduleTemplate>>,
    onCalendarVisibleDateRangeChange: (LocalDate, LocalDate) -> Unit,
    reminderChainLogs: StateFlow<List<ReminderChainLog>>,
    permissions: PermissionSnapshot,
    onEdit: (TodoItem) -> Unit,
    onEditCalendarEvent: (TodoItem) -> Unit,
    previewTodoId: Long? = null,
    previewTodoSerial: Int = 0,
    onPreviewTodoConsumed: () -> Unit = {},
    onNavigateTasks: () -> Unit = {},
    onNavigateCalendar: () -> Unit = {},
    onPreviewCalendarEvent: (TodoItem) -> Unit = {},
    onQuickCreateCalendarEvent: (LocalDateTime, LocalDateTime) -> Unit,
    onMoveCalendarEvent: (TodoItem, LocalDateTime, LocalDateTime) -> Unit,
    onCompleteTodo: (TodoItem) -> Unit,
    onRestoreTodo: (TodoItem) -> Unit,
    onCancelTodo: (TodoItem) -> Unit,
    onCancelCalendarEvent: (TodoItem) -> Unit,
    onDeleteTodo: (TodoItem) -> Unit,
    onDeleteCalendarEvent: (TodoItem) -> Unit,
    onSelectGroup: (Long?) -> Unit,
    onToggleGroupFilterMode: () -> Unit,
    onCreateGroup: suspend (String, String) -> String?,
    onUpdateGroup: suspend (TaskGroup) -> String?,
    onDeleteGroup: suspend (Long) -> String?,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestFullScreenPermission: () -> Unit,
    onRequestNotificationPolicyAccess: () -> Unit,
    onRequestIgnoreBatteryOptimization: () -> Unit,
    onRequestAccessibilityService: () -> Unit,
    onWeekStartModeChange: (WeekStartMode) -> Unit,
    onDefaultSnoozeChange: (Int) -> Unit,
    onDefaultCalendarReminderModeChange: (ReminderDeliveryMode) -> Unit,
    onEventCheckInPreferencesChange: (Boolean, Boolean) -> Unit,
    onEventCheckInIdleAutoCheckOutHoursChange: (Int) -> Unit,
    onOngoingEventNotificationEnabledChange: (Boolean) -> Unit,
    onPlanningOutlinerPreferencesChange: (Boolean, Boolean) -> Unit,
    onReminderAudioStrategyChange: (ReminderAudioChannel, Int, Boolean, Int, Boolean) -> Unit,
    onPlanningAiProvidersChange: (Boolean, List<PlanningAiProvider>) -> Unit,
    onReportPreferencesChange: (Boolean, Int, Int, Boolean, Int, Int, AiReportRetention) -> Unit,
    onDailyBriefPreferencesChange: (Boolean, Int, Int) -> Unit,
    onGenerateDailyReportNow: suspend () -> String?,
    onDeleteAiReport: suspend (Long) -> String?,
    onLaunchCheckIn: (Long) -> Unit,
    onResetOnboarding: () -> Unit,
    onDesktopSyncEnabledChange: (Boolean) -> Unit,
    onDesktopSyncWifiKeepAliveChange: (Boolean) -> Unit,
    onRotateDesktopSyncToken: () -> Unit,
    onUseBuiltInReminderTone: () -> Unit,
    onPickSystemReminderTone: () -> Unit,
    onOpenWiki: () -> Unit,
    onRunReminderChainTest: suspend (Int) -> String?,
    onClearReminderDiagnostics: suspend () -> Unit,
    onSaveWeekAsScheduleTemplate: suspend (String, String, LocalDate) -> String?,
    onApplyScheduleTemplateToWeek: suspend (ScheduleTemplate, LocalDate) -> String?,
    onGenerateSemesterScheduleFromTemplate: suspend (ScheduleTemplate, LocalDate, LocalDate) -> String?,
    onDeleteScheduleTemplate: suspend (Long) -> String?,
    onPickBackupDirectory: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onAutoBackupChange: (Boolean) -> Unit,
    onInspectDataHealth: suspend () -> DataHealthReport,
    onCleanSafeDataHealthItems: suspend () -> DataHealthCleanResult,
    onOpenCalendarBatchImport: () -> Unit,
    onSelectPlanningNote: (Long) -> Unit,
    onCreatePlanningNote: suspend (String) -> String?,
    onSavePlanningNote: suspend (Long, String) -> String?,
    onRenamePlanningNote: suspend (Long, String) -> String?,
    onDeletePlanningNote: suspend (Long) -> String?,
    onArchivePlanningNote: suspend (Long) -> String?,
    onOpenTodayPlanningNote: suspend () -> Long,
    observePlanningNodes: (Long) -> Flow<List<PlanningNode>>,
    onCreatePlanningNode: suspend (PlanningNodeDraft) -> String?,
    onUpdatePlanningNode: suspend (PlanningNode, PlanningNodeEdit) -> String?,
    onTogglePlanningNode: suspend (PlanningNode) -> String?,
    onPublishPlanningNode: suspend (PlanningNode) -> String?,
    onPublishAllPlanningDrafts: suspend (Long) -> String?,
    onDeletePlanningNode: suspend (PlanningNode) -> String?,
    onReorderPlanningNodes: suspend (Long, Long?, List<Long>) -> String?,
    onCreatePlanningNodeSnapshot: suspend (Long) -> PlanningNodeSnapshot,
    onRestorePlanningNodeSnapshot: suspend (PlanningNodeSnapshot) -> String?,
    onOpenPlanningLinkedItem: (Long) -> Unit,
    onExportPlanningNodesMarkdown: suspend (Long) -> String,
    onReplacePlanningNodesFromMarkdown: suspend (Long, String) -> String?,
    onParsePlanningMarkdown: suspend (String, Long?) -> PlanningParseResult,
    onImportPlanningCandidates: suspend (List<PlanningImportCandidate>, Set<String>, String, Long?) -> PlanningImportResult,
    onSyncPlanningMappings: suspend (Long, String) -> List<PlanningLineMapping>,
    onGetPlanningMappings: suspend (Long) -> List<PlanningLineMapping>,
    onRefreshPlanningImportedItems: suspend (Long, String, PlanningRefreshScope, Int?) -> PlanningOperationResult,
    onPostponePlanningImportedItems: suspend (Long, String, Long?, Int, PlanningPostponeScope) -> PlanningOperationResult,
    onUndoLastPlanningOperation: suspend (Long, String) -> PlanningOperationResult,
    onApplyPlanningConflictDocument: suspend (Long, String, Long) -> PlanningOperationResult,
    onApplyPlanningConflictItem: suspend (Long, String, Long) -> PlanningOperationResult,
    onDismissOnboarding: () -> Unit = {},
    onNavigatePlanning: () -> Unit = {},
    onBoardCollapseStateChange: (Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit = { _, _, _, _, _ -> }
) {
    if (section == DashboardSection.CALENDAR) {
        val allCalendarItems by calendarItems.collectAsStateWithLifecycle()
        val templates by scheduleTemplates.collectAsStateWithLifecycle()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            CalendarPanel(
                modifier = Modifier.fillMaxSize(),
                events = allCalendarItems,
                weekStartMode = uiState.settings.weekStartMode,
                scheduleTemplates = templates,
                targetEventId = targetEventId,
                targetEventSerial = targetEventSerial,
                initialFocusDate = calendarFocusDate,
                initialFocusDateSerial = calendarFocusDateSerial,
                onVisibleDateRangeChange = onCalendarVisibleDateRangeChange,
                onQuickCreateEvent = onQuickCreateCalendarEvent,
                onCreateEventAt = onQuickCreateCalendarEvent,
                onEditEvent = onEditCalendarEvent,
                onGetEventById = onGetTodoById,
                onGetEventCheckIns = onGetEventCheckIns,
                onCompleteEvent = onCompleteCalendarEvent,
                onMoveEvent = onMoveCalendarEvent,
                onCancelEvent = onCancelCalendarEvent,
                onDeleteEvent = onDeleteCalendarEvent,
                onOpenBatchImport = onOpenCalendarBatchImport,
                onSaveWeekAsTemplate = onSaveWeekAsScheduleTemplate,
                onApplyTemplateToWeek = onApplyScheduleTemplateToWeek,
                onGenerateSemesterFromTemplate = onGenerateSemesterScheduleFromTemplate,
                onDeleteTemplate = onDeleteScheduleTemplate
            )
        }
        return
    }

    if (section == DashboardSection.AI_REPORTS) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            AiReportPanel(
                observeReports = observeAiReports,
                onGetReport = onGetAiReport,
                targetReportId = targetAiReportId,
                targetReportSerial = targetAiReportSerial,
                onDeleteReport = onDeleteAiReport
            )
        }
        return
    }

    if (section == DashboardSection.SETTINGS) {
        val diagnostics by reminderChainLogs.collectAsStateWithLifecycle()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            SettingsPanel(
                settings = uiState.settings,
                permissions = permissions,
                defaultSnooze = uiState.settings.defaultSnoozeMinutes,
                crashLog = permissions.lastCrashLog,
                initialSectionKey = settingsInitialSectionKey,
                initialSectionSerial = settingsInitialSectionSerial,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                onRequestFullScreenPermission = onRequestFullScreenPermission,
                onRequestNotificationPolicyAccess = onRequestNotificationPolicyAccess,
                onRequestIgnoreBatteryOptimization = onRequestIgnoreBatteryOptimization,
                onRequestAccessibilityService = onRequestAccessibilityService,
                onWeekStartModeChange = onWeekStartModeChange,
                onDefaultSnoozeChange = onDefaultSnoozeChange,
                onDefaultCalendarReminderModeChange = onDefaultCalendarReminderModeChange,
                onEventCheckInPreferencesChange = onEventCheckInPreferencesChange,
                onEventCheckInIdleAutoCheckOutHoursChange = onEventCheckInIdleAutoCheckOutHoursChange,
                onOngoingEventNotificationEnabledChange = onOngoingEventNotificationEnabledChange,
                onPlanningOutlinerPreferencesChange = onPlanningOutlinerPreferencesChange,
                onReminderAudioStrategyChange = onReminderAudioStrategyChange,
                onPlanningAiProvidersChange = onPlanningAiProvidersChange,
                onReportPreferencesChange = onReportPreferencesChange,
                onDailyBriefPreferencesChange = onDailyBriefPreferencesChange,
                onGenerateDailyReportNow = onGenerateDailyReportNow,
                onResetOnboarding = onResetOnboarding,
                onDesktopSyncEnabledChange = onDesktopSyncEnabledChange,
                onDesktopSyncWifiKeepAliveChange = onDesktopSyncWifiKeepAliveChange,
                onRotateDesktopSyncToken = onRotateDesktopSyncToken,
                onUseBuiltInReminderTone = onUseBuiltInReminderTone,
                onPickSystemReminderTone = onPickSystemReminderTone,
                onOpenWiki = onOpenWiki,
                desktopSyncStatus = uiState.desktopSyncStatus,
                reminderChainLogs = diagnostics,
                onRunReminderChainTest = onRunReminderChainTest,
                onClearReminderDiagnostics = onClearReminderDiagnostics,
                onPickBackupDirectory = onPickBackupDirectory,
                onExportBackup = onExportBackup,
                onImportBackup = onImportBackup,
                onAutoBackupChange = onAutoBackupChange,
                onInspectDataHealth = onInspectDataHealth,
                onCleanSafeDataHealthItems = onCleanSafeDataHealthItems,
                onCopyCrashLog = permissions.copyCrashLog,
                onClearCrashLog = permissions.clearCrashLog
            )
        }
        return
    }

    if (section == DashboardSection.PLANNING) {
        val notes by planningNotes.collectAsStateWithLifecycle()
        val activeNote = notes.firstOrNull { it.id == uiState.settings.lastOpenedPlanningNoteId }
            ?: notes.firstOrNull()
        val planningNodes by produceState(initialValue = emptyList<PlanningNode>(), activeNote?.id) {
            val noteId = activeNote?.id ?: run {
                value = emptyList()
                return@produceState
            }
            observePlanningNodes(noteId).collect { value = it }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PlanningDeskPanel(
                notes = notes,
                activeNote = activeNote,
                nodes = planningNodes,
                groups = uiState.groups,
                planningAiProviders = uiState.settings.planningAiProviders,
                outlineHintVisible = uiState.settings.planningOutlineHintVisible,
                onSelectNote = onSelectPlanningNote,
                onCreateNote = onCreatePlanningNote,
                onSaveNote = onSavePlanningNote,
                onRenameNote = onRenamePlanningNote,
                onDeleteNote = onDeletePlanningNote,
                onArchiveNote = onArchivePlanningNote,
                onOpenTodayNote = onOpenTodayPlanningNote,
                onCreateNode = onCreatePlanningNode,
                onUpdateNode = onUpdatePlanningNode,
                onToggleNode = onTogglePlanningNode,
                onPublishNode = onPublishPlanningNode,
                onPublishAllDrafts = onPublishAllPlanningDrafts,
                onDeleteNode = onDeletePlanningNode,
                onReorderNodes = onReorderPlanningNodes,
                onCreateNodeSnapshot = onCreatePlanningNodeSnapshot,
                onRestoreNodeSnapshot = onRestorePlanningNodeSnapshot,
                onOpenLinkedItem = onOpenPlanningLinkedItem,
                onExportNodesMarkdown = onExportPlanningNodesMarkdown,
                onReplaceNodesFromMarkdown = onReplacePlanningNodesFromMarkdown,
                onParse = onParsePlanningMarkdown,
                onImport = onImportPlanningCandidates,
                onSyncMappings = onSyncPlanningMappings,
                onGetMappings = onGetPlanningMappings,
                onRefreshImportedItems = onRefreshPlanningImportedItems,
                onPostponeImportedItems = onPostponePlanningImportedItems,
                onUndoLastOperation = onUndoLastPlanningOperation,
                onApplyConflictDocument = onApplyPlanningConflictDocument,
                onApplyConflictItem = onApplyPlanningConflictItem,
                highlightedPlanningNodeId = highlightedPlanningNodeId,
                highlightedPlanningNodeSerial = highlightedPlanningNodeSerial,
                isNewUser = !uiState.settings.hasSeenOnboarding
            )
        }
        return
    }

    var missedExpanded by rememberSaveable { mutableStateOf(true) }
    var todayExpanded by rememberSaveable { mutableStateOf(true) }
    var upcomingExpanded by rememberSaveable(uiState.todayItems.isEmpty()) { mutableStateOf(uiState.todayItems.isEmpty()) }
    var expandedRecurringSeriesIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var countdownCollapsed by rememberSaveable { mutableStateOf(uiState.settings.boardCountdownCollapsed) }
    var boardTodosCollapsed by rememberSaveable { mutableStateOf(uiState.settings.boardTodayTodosCollapsed) }
    var boardTodayEventsCollapsed by rememberSaveable { mutableStateOf(uiState.settings.boardTodayEventsCollapsed) }
    var boardTomorrowEventsCollapsed by rememberSaveable { mutableStateOf(uiState.settings.boardTomorrowEventsCollapsed) }
    var boardAnnouncementCollapsed by rememberSaveable { mutableStateOf(uiState.settings.boardAnnouncementCollapsed) }
    val boardMoment by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            delay(30_000L)
            value = LocalDateTime.now()
        }
    }
    LaunchedEffect(
        countdownCollapsed,
        boardTodosCollapsed,
        boardTodayEventsCollapsed,
        boardTomorrowEventsCollapsed,
        boardAnnouncementCollapsed
    ) {
        onBoardCollapseStateChange(
            countdownCollapsed,
            boardTodosCollapsed,
            boardTodayEventsCollapsed,
            boardTomorrowEventsCollapsed,
            boardAnnouncementCollapsed
        )
    }
    val boardDate = boardMoment.toLocalDate()
    val boardTodoItems = remember(uiState.missedItems, uiState.todayItems) {
        (uiState.missedItems + uiState.todayItems)
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<TodoItem> { it.missed }
                    .thenBy { it.dueAtMillis }
            )
    }
    val allTodayScheduleItems = remember(uiState.calendarItems, boardDate) {
        uiState.calendarItems.filter { DailyBoardSnapshotBuilder.eventOverlapsDay(it, boardDate) }
            .sortedBy { it.startAtMillis ?: it.dueAtMillis }
    }
    val todayScheduleItems = remember(allTodayScheduleItems, boardMoment) {
        allTodayScheduleItems.filter { DailyBoardSnapshotBuilder.eventVisibleForToday(it, boardDate, boardMoment) }
    }
    val visibleCountdownItems = remember(uiState.countdownItems, boardMoment) {
        uiState.countdownItems
            .filter { item -> DailyBoardSnapshotBuilder.countdownTargetMillis(item)?.let { it >= System.currentTimeMillis() } == true }
            .sortedBy { DailyBoardSnapshotBuilder.countdownTargetMillis(it) ?: Long.MAX_VALUE }
            .take(5)
    }
    val tomorrowScheduleItems = remember(uiState.calendarItems, boardDate) {
        val tomorrow = boardDate.plusDays(1)
        uiState.calendarItems.filter { DailyBoardSnapshotBuilder.eventOverlapsDay(it, tomorrow) }
            .sortedBy { it.startAtMillis ?: it.dueAtMillis }
    }
    val activeAnnouncements = uiState.activeAnnouncements
    val expandedRecurringSeries = expandedRecurringSeriesIds.toSet()
    var previewTodoTarget by remember { mutableStateOf<TodoItem?>(null) }
    LaunchedEffect(previewTodoId, previewTodoSerial, uiState.todayItems, uiState.missedItems, uiState.upcomingItems) {
        val targetId = previewTodoId ?: return@LaunchedEffect
        previewTodoTarget = (uiState.missedItems + uiState.todayItems + uiState.upcomingItems)
            .firstOrNull { it.id == targetId && it.isTodo }
        if (previewTodoTarget != null) {
            onPreviewTodoConsumed()
        }
    }
    val completedHistoryItems = if (section == DashboardSection.HISTORY) {
        historyItems.collectAsStateWithLifecycle().value
    } else {
        emptyList()
    }
    val groupsById = remember(uiState.groups) { uiState.groups.associateBy { it.id } }
    val resolvedTodoGroups = remember(
        groupsById,
        boardTodoItems,
        uiState.missedItems,
        uiState.todayItems,
        uiState.upcomingItems,
        completedHistoryItems
    ) {
        buildMap {
            fun addItems(items: List<TodoItem>) {
                items.forEach { item ->
                    put(item.id, resolveTaskGroup(item, groupsById))
                }
            }
            addItems(boardTodoItems)
            addItems(uiState.missedItems)
            addItems(uiState.todayItems)
            addItems(uiState.upcomingItems)
            addItems(completedHistoryItems)
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = dashboardPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (section) {
            DashboardSection.BOARD -> {
                if (activeAnnouncements.isNotEmpty()) {
                    item {
                        CollapsibleBoardCard(
                            title = "公告（${activeAnnouncements.size}）",
                            collapsed = boardAnnouncementCollapsed,
                            onToggle = { boardAnnouncementCollapsed = !boardAnnouncementCollapsed }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                activeAnnouncements.forEach { announcement ->
                                    AnnouncementBanner(
                                        text = announcement.text,
                                        rangeLabel = announcement.rangeLabel()
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    CompactGreetingCard(quote = uiState.currentQuote)
                }

                if (visibleCountdownItems.isNotEmpty()) {
                    item {
                        CollapsibleBoardCard(
                            title = "倒数日（${visibleCountdownItems.size}）",
                            collapsed = countdownCollapsed,
                            onToggle = { countdownCollapsed = !countdownCollapsed },
                            onNavigate = onNavigateTasks
                        ) {
                            CountdownBoardCard(
                                items = visibleCountdownItems,
                                now = boardMoment,
                                groups = uiState.groups,
                                onOpenTodo = { previewTodoTarget = it },
                                onOpenEvent = onPreviewCalendarEvent
                            )
                        }
                    }
                }

                item {
                    BoardBlockTitle(
                        title = "今日待办（${boardTodoItems.size}）",
                        collapsed = boardTodosCollapsed,
                        onToggle = { boardTodosCollapsed = !boardTodosCollapsed },
                        onNavigate = onNavigateTasks
                    )
                }
                if (!boardTodosCollapsed) {
                    if (boardTodoItems.isEmpty()) {
                        item {
                            Box(modifier = Modifier.clickable(onClick = onNavigateTasks)) {
                                EmptyStateCard("今天还没有安排任务。")
                            }
                        }
                    } else {
                        items(
                            items = boardTodoItems,
                            key = { it.id },
                            contentType = { "active-todo-card" }
                        ) { item ->
                            ActiveTodoCard(
                                item = item,
                                groups = uiState.groups,
                                resolvedGroup = resolvedTodoGroups[item.id],
                                forceShowDetailsKey = if (previewTodoId == item.id) previewTodoSerial else 0,
                                onEdit = { onEdit(item) },
                                onComplete = { onCompleteTodo(item) },
                                onCancel = { onCancelTodo(item) },
                                onDelete = { onDeleteTodo(item) }
                            )
                        }
                    }
                }

                item {
                    BoardBlockTitle(
                        title = "今日日程（${todayScheduleItems.size}）",
                        collapsed = boardTodayEventsCollapsed,
                        onToggle = { boardTodayEventsCollapsed = !boardTodayEventsCollapsed },
                        onNavigate = onNavigateCalendar
                    )
                }
                if (!boardTodayEventsCollapsed || !boardTomorrowEventsCollapsed) {
                    item {
                        TodayScheduleBoardCard(
                            today = boardDate,
                            now = boardMoment,
                            hasTodayEvents = allTodayScheduleItems.isNotEmpty(),
                            todayEvents = todayScheduleItems,
                            tomorrowEvents = tomorrowScheduleItems,
                            todayCollapsed = boardTodayEventsCollapsed,
                            tomorrowCollapsed = boardTomorrowEventsCollapsed,
                            onToggleTomorrow = { boardTomorrowEventsCollapsed = !boardTomorrowEventsCollapsed },
                            onOpenEvent = onPreviewCalendarEvent,
                            onGetEventCheckIns = onGetEventCheckIns,
                            onLaunchCheckIn = onLaunchCheckIn,
                            onNavigatePlanning = onNavigatePlanning,
                            onNavigateCalendar = onNavigateCalendar
                        )
                    }
                }

                if (!uiState.settings.hasSeenOnboarding) {
                    item {
                        OnboardingCard(onDismiss = onDismissOnboarding)
                    }
                }
            }

            DashboardSection.ACTIVE -> {
                item {
                    TodoFilterBar(
                        groups = uiState.groups,
                        selectedGroupIds = uiState.selectedGroupIds,
                        groupFilterMode = uiState.groupFilterMode,
                        onSelectGroup = onSelectGroup,
                        onToggleGroupFilterMode = onToggleGroupFilterMode,
                        onCreateGroup = onCreateGroup,
                        onUpdateGroup = onUpdateGroup,
                        onDeleteGroup = onDeleteGroup
                    )
                }

                if (
                    uiState.selectedGroupIds.size >= 2 &&
                    uiState.groupFilterMode == TodoRepository.GroupFilterMode.INTERSECTION &&
                    uiState.missedItems.isEmpty() &&
                    uiState.todayItems.isEmpty() &&
                    uiState.upcomingItems.isEmpty()
                ) {
                    item {
                        EmptyStateCard(
                            text = "当前没有同时属于所有选中分组的待办",
                            actionLabel = "切换为并集显示",
                            onAction = onToggleGroupFilterMode
                        )
                    }
                }

                if (uiState.missedItems.isNotEmpty()) {
                    item {
                        ExpandableSectionHeader(
                            title = "已错过（${uiState.missedItems.size}）",
                            expanded = missedExpanded,
                            onToggle = { missedExpanded = !missedExpanded }
                        )
                    }
                    if (missedExpanded) {
                        items(
                            items = uiState.missedItems,
                            key = { it.id },
                            contentType = { "active-todo-card" }
                        ) { item ->
                            ActiveTodoCard(
                                item = item,
                                groups = uiState.groups,
                                resolvedGroup = resolvedTodoGroups[item.id],
                                onEdit = { onEdit(item) },
                                onComplete = { onCompleteTodo(item) },
                                onCancel = { onCancelTodo(item) },
                                onDelete = { onDeleteTodo(item) }
                            )
                        }
                    }
                }

                item {
                    ExpandableSectionHeader(
                        title = "今日待办（${uiState.todayItems.size}）",
                        expanded = todayExpanded,
                        onToggle = { todayExpanded = !todayExpanded }
                    )
                }
                if (todayExpanded) {
                    if (uiState.todayItems.isEmpty()) {
                        item { EmptyStateCard("今天还没有安排任务。") }
                    } else {
                        items(
                            items = uiState.todayItems,
                            key = { it.id },
                            contentType = { "active-todo-card" }
                        ) { item ->
                            ActiveTodoCard(
                                item = item,
                                groups = uiState.groups,
                                resolvedGroup = resolvedTodoGroups[item.id],
                                onEdit = { onEdit(item) },
                                onComplete = { onCompleteTodo(item) },
                                onCancel = { onCancelTodo(item) },
                                onDelete = { onDeleteTodo(item) }
                            )
                        }
                    }
                }

                item {
                    ExpandableSectionHeader(
                        title = "计划中（${uiState.upcomingItems.size}）",
                        expanded = upcomingExpanded,
                        onToggle = { upcomingExpanded = !upcomingExpanded }
                    )
                }
                if (upcomingExpanded) {
                    if (uiState.upcomingItems.isEmpty()) {
                        item { EmptyStateCard("后续时间暂时没有新计划。") }
                    } else {
                        uiState.upcomingItemGroups.forEach { group ->
                            val seriesId = group.seriesId
                            if (group.isCollapsibleRecurringSeries && seriesId != null) {
                                val expanded = seriesId in expandedRecurringSeries
                                item(key = "series-summary-$seriesId") {
                                    RecurringTodoSeriesCard(
                                        representative = group.representative,
                                        instanceCount = group.items.size,
                                        groups = uiState.groups,
                                        resolvedGroup = resolvedTodoGroups[group.representative.id],
                                        expanded = expanded,
                                        onToggle = {
                                            expandedRecurringSeriesIds = if (expanded) {
                                                expandedRecurringSeriesIds - seriesId
                                            } else {
                                                (expandedRecurringSeriesIds + seriesId).distinct()
                                            }
                                        }
                                    )
                                }
                                if (expanded) {
                                    val visibleSeriesItems = group.items.take(UpcomingRecurringExpandedLimit)
                                    items(
                                        items = visibleSeriesItems,
                                        key = { "series-$seriesId-${it.id}" },
                                        contentType = { "active-todo-card" }
                                    ) { item ->
                                        ActiveTodoCard(
                                            item = item,
                                            groups = uiState.groups,
                                            resolvedGroup = resolvedTodoGroups[item.id],
                                            onEdit = { onEdit(item) },
                                            onComplete = { onCompleteTodo(item) },
                                            onCancel = { onCancelTodo(item) },
                                            onDelete = { onDeleteTodo(item) }
                                        )
                                    }
                                    val hiddenCount = group.items.size - visibleSeriesItems.size
                                    if (hiddenCount > 0) {
                                        item(
                                            key = "series-limit-$seriesId",
                                            contentType = "recurring-series-limit"
                                        ) {
                                            RecurringSeriesLimitNotice(
                                                hiddenCount = hiddenCount,
                                                visibleCount = UpcomingRecurringExpandedLimit
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(
                                    items = group.items,
                                    key = { it.id },
                                    contentType = { "active-todo-card" }
                                ) { item ->
                                    ActiveTodoCard(
                                        item = item,
                                        groups = uiState.groups,
                                        resolvedGroup = resolvedTodoGroups[item.id],
                                        onEdit = { onEdit(item) },
                                        onComplete = { onCompleteTodo(item) },
                                        onCancel = { onCancelTodo(item) },
                                        onDelete = { onDeleteTodo(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            DashboardSection.HISTORY -> {
                if (completedHistoryItems.isEmpty()) {
                    item { EmptyStateCard("完成或取消后的事项会保存在这里。") }
                } else {
                    items(
                        items = completedHistoryItems,
                        key = { it.id },
                        contentType = { "completed-history-card" }
                    ) { item ->
                        CompletedTodoCard(
                            item = item,
                            groups = uiState.groups,
                            onEdit = {
                                if (item.isEvent) {
                                    onEditCalendarEvent(item)
                                } else {
                                    onEdit(item)
                                }
                            },
                            onRestore = { onRestoreTodo(item) },
                            resolvedGroup = resolvedTodoGroups[item.id]
                        )
                    }
                }
            }
            DashboardSection.CALENDAR -> Unit
            DashboardSection.PLANNING -> Unit
            DashboardSection.AI_REPORTS -> Unit
            DashboardSection.SETTINGS -> Unit
        }
    }
    previewTodoTarget?.let { item ->
        TodoDetailsDialog(
            item = item,
            groups = uiState.groups,
            onDismiss = { previewTodoTarget = null },
            showCreated = true,
            showStatusTime = item.isHistory,
            onEdit = {
                previewTodoTarget = null
                onEdit(item)
            },
            onCancel = if (!item.isTodo || item.completed || item.canceled) {
                null
            } else {
                {
                    previewTodoTarget = null
                    onCancelTodo(item)
                }
            },
            onDelete = if (!item.isTodo || item.completed || item.canceled) {
                null
            } else {
                {
                    previewTodoTarget = null
                    onDeleteTodo(item)
                }
            },
            onRestore = if (item.completed || item.canceled) {
                {
                    previewTodoTarget = null
                    onRestoreTodo(item)
                }
            } else {
                null
            }
        )
    }
}

@Composable
private fun ExpandableSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(title)
        Icon(
            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) "收起$title" else "展开$title",
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RecurringTodoSeriesCard(
    representative: TodoItem,
    instanceCount: Int,
    groups: List<TaskGroup>,
    resolvedGroup: ResolvedTaskGroup? = null,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val group = remember(representative.id, representative.groupId, representative.categoryKey, representative.itemType, representative.accentColorHex, resolvedGroup, groups) {
        resolvedGroup ?: resolveTaskGroup(representative, groups)
    }
    val accent = colorFromHex(group.colorHex)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(58.dp)
                    .background(accent, RoundedCornerShape(999.dp))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = representative.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        representative.recurrenceTypeEnum.label,
                        "还有 ${instanceCount - 1} 个未来实例",
                        representative.dueDateTimeOrNull()?.let { "最近 ${formatLocalDateTime(it)}" }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = accent.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "${instanceCount} 次",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = accent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "折叠循环实例" else "展开循环实例",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecurringSeriesLimitNotice(
    hiddenCount: Int,
    visibleCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    ) {
        Text(
            text = "已展开前 ${visibleCount} 个未来实例，另有 ${hiddenCount} 个已折叠以保持滑动流畅。",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 18.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnnouncementBanner(text: String, rangeLabel: String? = null) {
    val dark = isSystemInDarkTheme()
    val background = if (dark) Color(0xFFCC8030) else Color(0xFFFFB347)
    val foreground = Color(0xFF5C3A0E)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(22.dp),
        color = background
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Campaign,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = rangeLabel?.takeIf { it.isNotBlank() }?.let { "$it · $text" } ?: text,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = foreground
            )
        }
    }
}

@Composable
private fun CompactGreetingCard(
    quote: String
) {
    var collapsed by rememberSaveable { mutableStateOf(true) }
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${timeGreeting()}，Payki",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PaykiGreetingFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!collapsed) {
                    Text(
                        text = quote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Visible
                    )
                }
            }
            IconButton(onClick = { collapsed = !collapsed }) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = if (collapsed) "展开欢迎语" else "折叠欢迎语",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = if (collapsed) 90f else 180f }
                )
            }
        }
    }
}

@Composable
private fun OnboardingCard(onDismiss: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("首次使用提示", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("右下角 + 先记一条待办；抽屉里的「规划台」适合先把近期事情写下来。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text("正式使用前，去设置里检查提醒权限，避免错过 DDL。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onDismiss
                ) {
                    Text("知道了", modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CountdownBoardCard(
    items: List<TodoItem>,
    now: LocalDateTime,
    groups: List<TaskGroup>,
    onOpenTodo: (TodoItem) -> Unit,
    onOpenEvent: (TodoItem) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "倒数日",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "关键目标不要等到最后一天才想起来",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${items.size} 项",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            items.forEach { item ->
                CountdownTargetRow(
                    item = item,
                    now = now,
                    groups = groups,
                    onClick = {
                        if (item.isEvent) onOpenEvent(item) else onOpenTodo(item)
                    }
                )
            }
        }
    }
}

@Composable
private fun CountdownTargetRow(
    item: TodoItem,
    now: LocalDateTime,
    groups: List<TaskGroup>,
    onClick: () -> Unit
) {
    val group = resolveTaskGroup(item, groups)
    val accent = colorFromHex(if (item.isEvent) item.accentColorHex ?: group.colorHex else group.colorHex)
    DailyBoardSnapshotBuilder.countdownTargetDate(item) ?: return
    val remaining = DailyBoardSnapshotBuilder.countdownRemainingDisplay(item, now) ?: return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = if (isSystemInDarkTheme()) 0.18f else 0.10f),
        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.32f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = remaining.primary,
                    color = accent,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1
                )
                if (remaining.secondary.isNotBlank()) {
                    Text(
                        text = remaining.secondary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .background(accent, RoundedCornerShape(999.dp))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = countdownMetaText(item, group),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun countdownMetaText(item: TodoItem, group: ResolvedTaskGroup): String {
    return if (item.isEvent) {
        val start = item.startAtMillis?.toLocalDateTime() ?: item.dueAtMillis.toLocalDateTime()
        val end = item.endAtMillis?.toLocalDateTime()
        val time = when {
            item.allDay && end != null && end.toLocalDate().minusDays(1).isAfter(start.toLocalDate()) ->
                "${start.format(CountdownMonthDayFormatter)}-${end.toLocalDate().minusDays(1).format(CountdownMonthDayFormatter)} 全天"
            item.allDay -> "${start.format(CountdownMonthDayFormatter)} 全天"
            end != null && end.toLocalDate() != start.toLocalDate() ->
                "${start.format(CountdownDateTimeFormatter)}-${end.format(CountdownDateTimeFormatter)}"
            end != null -> "${start.format(CountdownDateTimeFormatter)}-${end.format(CountdownTimeFormatter)}"
            else -> start.format(CountdownDateTimeFormatter)
        }
        time
    } else {
        "DDL ${item.dueAtMillis.toLocalDateTime().format(CountdownDateTimeFormatter)} · ${group.name}"
    }
}

private fun Long.toLocalDateTime(): LocalDateTime {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
}

private val CountdownMonthDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
private val CountdownDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)
private val CountdownTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)

@Composable
private fun BoardBlockTitle(
    title: String,
    collapsed: Boolean? = null,
    onToggle: (() -> Unit)? = null,
    onNavigate: (() -> Unit)? = null
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.3f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onNavigate != null, onClick = { onNavigate?.invoke() }),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = if (isDarkTheme) 0.55f else 0.14f),
                    offset = Offset(0f, 1.5f),
                    blurRadius = if (isDarkTheme) 10f else 4f
                )
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (collapsed != null && onToggle != null) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (collapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                    contentDescription = if (collapsed) "展开$title" else "收起$title",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CollapsibleBoardCard(
    title: String,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onNavigate: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoardBlockTitle(title = title, collapsed = collapsed, onToggle = onToggle, onNavigate = onNavigate)
        if (!collapsed) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayScheduleBoardCard(
    today: LocalDate,
    now: LocalDateTime,
    hasTodayEvents: Boolean,
    todayEvents: List<TodoItem>,
    tomorrowEvents: List<TodoItem>,
    todayCollapsed: Boolean,
    tomorrowCollapsed: Boolean,
    onToggleTomorrow: () -> Unit,
    onOpenEvent: (TodoItem) -> Unit,
    onGetEventCheckIns: suspend (Long) -> List<EventCheckIn>,
    onLaunchCheckIn: (Long) -> Unit,
    onNavigatePlanning: () -> Unit = {},
    onNavigateCalendar: () -> Unit = {}
) {
    ElevatedCard(
        modifier = Modifier.clickable(onClick = onNavigateCalendar),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.widthIn(min = 54.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = boardMonthLabel(today),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = boardWeekdayLabel(today),
                    color = Color(0xFF5A92FF),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = today.dayOfMonth.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!todayCollapsed) {
                    if (todayEvents.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (hasTodayEvents) Color(0xFFFFC94A).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                            border = if (hasTodayEvents) BorderStroke(0.8.dp, Color(0xFFFFC94A).copy(alpha = 0.46f)) else null
                        ) {
                            Text(
                                text = if (hasTodayEvents) "太棒了！今天的日程都结束了~" else "今天暂无日程",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                color = if (hasTodayEvents) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        todayEvents.forEach { item ->
                            BoardScheduleEventRow(
                                item = item,
                                now = now,
                                onGetEventCheckIns = onGetEventCheckIns,
                                onLaunchCheckIn = onLaunchCheckIn,
                                onClick = { onOpenEvent(item) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "明天",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onToggleTomorrow) {
                        Icon(
                            imageVector = if (tomorrowCollapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                            contentDescription = if (tomorrowCollapsed) "展开明日日程" else "收起明日日程",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!tomorrowCollapsed) {
                    if (tomorrowEvents.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                            onClick = onNavigatePlanning
                        ) {
                            Text(
                                text = "明天暂无日程 · 去规划台安排一下？",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        tomorrowEvents.take(2).forEach { item ->
                            BoardScheduleEventRow(item = item, now = null, onClick = { onOpenEvent(item) })
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun BoardScheduleEventRow(
    item: TodoItem,
    now: LocalDateTime?,
    onGetEventCheckIns: (suspend (Long) -> List<EventCheckIn>)? = null,
    onLaunchCheckIn: ((Long) -> Unit)? = null,
    onClick: () -> Unit
) {
    val tint = item.accentColorHex?.let(::colorFromHex) ?: MaterialTheme.colorScheme.primary
    val inProgress = now?.let { DailyBoardSnapshotBuilder.eventInProgress(item, it) } == true
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshSerial by remember(item.id) { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner, item.id, item.checkInEnabled, inProgress) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && item.checkInEnabled && inProgress) {
                refreshSerial += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val checkIns by produceState(
        initialValue = emptyList<EventCheckIn>(),
        item.id,
        item.checkInEnabled,
        inProgress,
        refreshSerial
    ) {
        value = if (item.checkInEnabled && inProgress && onGetEventCheckIns != null) {
            onGetEventCheckIns(item.id)
        } else {
            emptyList()
        }
    }
    val activeCheckIn = checkIns.firstOrNull { it.checkOutAtMillis == null }
    val checkInStatus = remember(item.checkInEnabled, inProgress, activeCheckIn, now) {
        when {
            !item.checkInEnabled || !inProgress -> null
            activeCheckIn == null -> "未签到"
            else -> "签到中 · 已 ${formatBoardCheckInMinutes(activeCheckIn, now)}"
        }
    }
    val gold = Color(0xFFFFC94A)
    val rowShape = RoundedCornerShape(18.dp)
    val rowColor = if (inProgress) gold else tint
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = rowShape,
        color = if (inProgress) rowColor.copy(alpha = 0.055f) else Color.Transparent,
        border = if (inProgress) BorderStroke(1.4.dp, rowColor.copy(alpha = 0.92f)) else null
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (inProgress) 2.dp else 0.dp,
                    top = if (inProgress) 8.dp else 3.dp,
                    end = if (inProgress) 5.dp else 0.dp,
                    bottom = if (inProgress) 8.dp else 3.dp
                ),
            shape = RoundedCornerShape(14.dp),
            color = if (inProgress) rowColor.copy(alpha = 0.015f) else Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(start = 0.dp, top = 12.dp, end = 10.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .background(
                            color = rowColor.copy(alpha = 0.96f),
                            shape = RoundedCornerShape(999.dp)
                        )
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (inProgress) gold else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = DailyBoardSnapshotBuilder.eventSecondaryText(item),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    item.location.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (checkInStatus != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = checkInStatus,
                                color = if (activeCheckIn == null) MaterialTheme.colorScheme.onSurfaceVariant else gold,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (onLaunchCheckIn != null) {
                                TextButton(
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (activeCheckIn == null) Color(0xFF2196F3) else Color(0xFF4CAF50)
                                    ),
                                    onClick = { onLaunchCheckIn(item.id) }
                                ) {
                                    Text(if (activeCheckIn == null) "去签到" else "查看")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatBoardCheckInMinutes(checkIn: EventCheckIn, now: LocalDateTime?): String {
    val nowMillis = now?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis()
    val minutes = ((nowMillis - checkIn.checkInAtMillis).coerceAtLeast(0L) / 60_000L).toInt()
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 -> "${hours}h${rest}m"
        hours > 0 -> "${hours}h"
        else -> "${rest}m"
    }
}

@Composable
internal fun SectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = 24.sp,
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.16f),
                offset = Offset(0f, 1.5f),
                blurRadius = 5f
            )
        ),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
internal fun EmptyStateCard(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
internal fun LaunchScreen() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val iconSize = 92.dp
        val sunCenterY = maxHeight * 0.23f
        val iconTop = sunCenterY - (iconSize / 2)
        val titleTop = sunCenterY + (iconSize / 2) + 18.dp

        Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFF5E6C6),
                            Color(0xFFD0EBF2),
                            Color(0xFF7FA5A7)
                        )
                    )
                )
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x55FFF6E5),
                radius = size.minDimension * 0.16f,
                center = center.copy(y = size.height * 0.23f)
            )

            drawCircle(
                color = Color(0x1AFFFFFF),
                radius = size.minDimension * 0.12f,
                center = center.copy(y = size.height * 0.23f),
                style = Stroke(width = 4.dp.toPx())
            )

            val mountainBack = Path().apply {
                moveTo(0f, size.height * 0.62f)
                cubicTo(size.width * 0.12f, size.height * 0.52f, size.width * 0.28f, size.height * 0.48f, size.width * 0.42f, size.height * 0.54f)
                cubicTo(size.width * 0.58f, size.height * 0.62f, size.width * 0.74f, size.height * 0.44f, size.width, size.height * 0.56f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountainBack, color = Color(0x7078A6AC))

            val mountainMid = Path().apply {
                moveTo(0f, size.height * 0.72f)
                cubicTo(size.width * 0.16f, size.height * 0.66f, size.width * 0.32f, size.height * 0.63f, size.width * 0.5f, size.height * 0.71f)
                cubicTo(size.width * 0.67f, size.height * 0.78f, size.width * 0.84f, size.height * 0.66f, size.width, size.height * 0.73f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountainMid, color = Color(0x9C5F8B90))

            val mountainFront = Path().apply {
                moveTo(0f, size.height * 0.8f)
                cubicTo(size.width * 0.14f, size.height * 0.76f, size.width * 0.34f, size.height * 0.75f, size.width * 0.46f, size.height * 0.83f)
                cubicTo(size.width * 0.62f, size.height * 0.91f, size.width * 0.82f, size.height * 0.81f, size.width, size.height * 0.84f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountainFront, color = Color(0xFF355258))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x12091920), Color(0x2A17313B), Color(0x18061115))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.85f)
                .offset(y = titleTop),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "PaykiTodo",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayLarge.copy(
                    shadow = Shadow(
                        color = Color(0x44111D1C),
                        offset = Offset(0f, 4f),
                        blurRadius = 12f
                    )
                ),
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF274B54)
            )
            Text(
                text = "专属于您的高效待办助手",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.5.sp),
                fontFamily = FontFamily.Cursive,
                color = Color(0xFF4A6670)
            )
        }

        Image(
            painter = painterResource(id = R.drawable.ic_launcher_art_transparent),
            contentDescription = "应用图标",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = iconTop)
                .size(iconSize),
            contentScale = ContentScale.Fit
        )

        Text(
            text = "© Copyright Hoshi Takyobu, 2026-2026",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xAA43616A)
        )
    }
    }
}

@Composable
private fun DrawerSectionButton(
    section: DashboardSection,
    selected: Boolean,
    onClick: () -> Unit,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    DrawerSectionButton(
        label = section.label,
        icon = section.icon,
        selected = selected,
        onClick = onClick,
        trailingContent = trailingContent
    )
}

@Composable
private fun DrawerSectionButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp),
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            trailingContent?.invoke(this)
        }
    }
}

@Composable
private fun ThemeModeSwitcher(
    selectedThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val systemDark = isSystemInDarkTheme()
    val currentIcon = when (selectedThemeMode) {
        ThemeMode.LIGHT -> Icons.Rounded.LightMode
        ThemeMode.DARK -> Icons.Rounded.DarkMode
        ThemeMode.SYSTEM -> if (systemDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode
    }
    val currentLabel = when (selectedThemeMode) {
        ThemeMode.LIGHT -> "浅色模式"
        ThemeMode.DARK -> "深色模式"
        ThemeMode.SYSTEM -> "跟随系统"
    }

    Box {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = currentIcon,
                    contentDescription = currentLabel,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 0.dp, y = (-8).dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                val icon = when (mode) {
                    ThemeMode.LIGHT -> Icons.Rounded.LightMode
                    ThemeMode.DARK -> Icons.Rounded.DarkMode
                    ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                }
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    onClick = {
                        expanded = false
                        onThemeModeChange(mode)
                    },
                    leadingIcon = { Icon(icon, contentDescription = null) }
                )
            }
        }
    }
}

private fun timeGreeting(): String = when (LocalTime.now().hour) {
    in 0..4 -> "凌晨好"
    in 5..10 -> "早上好"
    in 11..13 -> "中午好"
    in 14..17 -> "下午好"
    else -> "晚上好"
}

private fun boardWeekdayLabel(date: LocalDate): String = when (date.dayOfWeek.value) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    else -> "周日"
}

private fun boardMonthLabel(date: LocalDate): String = "${date.monthValue}月"
