package com.example.todoalarm.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.example.todoalarm.R
import com.example.todoalarm.data.CalendarEventDraft
import com.example.todoalarm.data.DataHealthCleanResult
import com.example.todoalarm.data.DataHealthReport
import com.example.todoalarm.data.AiReport
import com.example.todoalarm.data.AiReportType
import com.example.todoalarm.data.AiReportRetention
import com.example.todoalarm.data.EventCheckIn
import com.example.todoalarm.data.EventCheckInCompletionSummary
import com.example.todoalarm.data.GlobalSearchResult
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
import com.example.todoalarm.data.PlanningAiProvider
import com.example.todoalarm.data.PlanningPostponeScope
import com.example.todoalarm.data.PlanningRefreshScope
import com.example.todoalarm.data.RecurrenceConfig
import com.example.todoalarm.data.RecurrenceScope
import com.example.todoalarm.data.ReminderDeliveryMode
import com.example.todoalarm.data.ReminderAudioChannel
import com.example.todoalarm.data.ReminderChainLog
import com.example.todoalarm.data.ScheduleTemplate
import com.example.todoalarm.data.ScheduleTemplateType
import com.example.todoalarm.data.storageStringToWeekdays
import com.example.todoalarm.data.ThemeMode
import com.example.todoalarm.data.TodoDraft
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.WeekStartMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

private enum class ScopeDialogMode {
    EDIT_TODO,
    CANCEL_TODO,
    DELETE_TODO,
    EDIT_EVENT,
    CANCEL_EVENT,
    DELETE_EVENT
}

private enum class EditorKind {
    TODO,
    CALENDAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: TodoUiState,
    desktopSyncStatus: StateFlow<com.example.todoalarm.sync.DesktopSyncStatus>,
    planningNotes: StateFlow<List<PlanningNote>>,
    observeAiReports: (AiReportType?, Int, String, Long, Long) -> Flow<List<AiReport>>,
    onGetAiReport: suspend (Long) -> AiReport?,
    onGlobalSearch: suspend (String) -> GlobalSearchResult,
    historyItems: StateFlow<List<TodoItem>>,
    calendarItems: StateFlow<List<TodoItem>>,
    scheduleTemplates: StateFlow<List<ScheduleTemplate>>,
    reminderChainLogs: StateFlow<List<ReminderChainLog>>,
    permissions: PermissionSnapshot,
    launchRoute: DashboardLaunchRoute? = null,
    launchRouteSerial: Int = 0,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestFullScreenPermission: () -> Unit,
    onRequestNotificationPolicyAccess: () -> Unit,
    onRequestIgnoreBatteryOptimization: () -> Unit,
    onRequestAccessibilityService: () -> Unit,
    onAddTodo: suspend (TodoDraft) -> String?,
    onAddCalendarEvent: suspend (com.example.todoalarm.data.CalendarEventDraft) -> String?,
    onImportTodos: suspend (List<TodoDraft>) -> String?,
    onImportCalendarEvents: suspend (List<CalendarEventDraft>) -> String?,
    onGetTodoById: suspend (Long) -> TodoItem?,
    onGetTodoGroupIds: suspend (Long) -> List<Long>,
    onGetEventCheckIns: suspend (Long) -> List<EventCheckIn>,
    onCompleteCalendarEvent: suspend (Long) -> EventCheckInCompletionSummary?,
    onCalendarVisibleDateRangeChange: (LocalDate, LocalDate) -> Unit,
    onUpdateTodo: suspend (TodoItem, TodoDraft, RecurrenceScope) -> String?,
    onUpdateCalendarEvent: suspend (TodoItem, com.example.todoalarm.data.CalendarEventDraft, RecurrenceScope) -> String?,
    onUpdateCalendarEventMultiSlotBundleSharedFields: suspend (TodoItem, com.example.todoalarm.data.CalendarEventDraft) -> String?,
    onDeleteTodo: (TodoItem, RecurrenceScope) -> Unit,
    onDeleteCalendarEvent: (TodoItem, RecurrenceScope) -> Unit,
    onCompleteTodo: (TodoItem) -> Unit,
    onCompleteTodos: (List<TodoItem>) -> Unit,
    onCancelTodos: (List<TodoItem>) -> Unit,
    onRestoreTodo: (TodoItem) -> Unit,
    onCancelTodo: (TodoItem, RecurrenceScope) -> Unit,
    onCancelCalendarEvent: (TodoItem, RecurrenceScope) -> Unit,
    onSelectGroup: (Long?) -> Unit,
    onToggleGroupFilterMode: () -> Unit,
    onCreateGroup: suspend (String, String) -> String?,
    onUpdateGroup: suspend (com.example.todoalarm.data.TaskGroup) -> String?,
    onDeleteGroup: suspend (Long) -> String?,
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
    onThemeModeChange: (ThemeMode) -> Unit,
    onWeekStartModeChange: (WeekStartMode) -> Unit,
    onNextQuote: () -> Unit,
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
    onDismissOnboarding: () -> Unit,
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
    onBoardCollapseStateChange: (Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var section by rememberSaveable { mutableStateOf(DashboardSection.BOARD) }
    var reopenDrawerAfterThemeChange by rememberSaveable { mutableStateOf(false) }
    var launchVisible by rememberSaveable { mutableStateOf(true) }
    var editorVisible by remember { mutableStateOf(false) }
    var editorKind by remember { mutableStateOf(EditorKind.TODO) }
    var editingItem by remember { mutableStateOf<TodoItem?>(null) }
    var editingTodoGroupIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var calendarDraftSeed by remember { mutableStateOf<CalendarEventDraft?>(null) }
    var todoBatchImportVisible by remember { mutableStateOf(false) }
    var batchImportVisible by remember { mutableStateOf(false) }
    var editScope by remember { mutableStateOf(RecurrenceScope.CURRENT) }
    var scopeDialogTarget by remember { mutableStateOf<TodoItem?>(null) }
    var scopeDialogMode by remember { mutableStateOf<ScopeDialogMode?>(null) }
    var lastBackPressedAt by rememberSaveable { mutableLongStateOf(0L) }
    var boardVisited by rememberSaveable { mutableStateOf(false) }
    var handledTodoLaunchSerial by rememberSaveable { mutableStateOf(-1) }
    var handledEventLaunchSerial by rememberSaveable { mutableStateOf(-1) }
    var previewTodoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var previewTodoSerial by rememberSaveable { mutableStateOf(0) }
    var quickPreviewTodo by remember { mutableStateOf<TodoItem?>(null) }
    var previewEventId by rememberSaveable { mutableStateOf<Long?>(null) }
    var previewEventSerial by rememberSaveable { mutableStateOf(0) }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var searchAiReportTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var searchAiReportTargetSerial by rememberSaveable { mutableStateOf(0) }
    var highlightedPlanningNodeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var highlightedPlanningNodeSerial by rememberSaveable { mutableStateOf(0) }
    var calendarFocusDateEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var todoBatchSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedTodoIds by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var batchSelectableTodoItems by remember { mutableStateOf(emptyList<TodoItem>()) }
    var showBatchCancelConfirm by rememberSaveable { mutableStateOf(false) }
    val selectedTodoIdSet = remember(selectedTodoIds) { selectedTodoIds.toSet() }
    val allVisibleTodosSelected = batchSelectableTodoItems.isNotEmpty() &&
        selectedTodoIds.size == batchSelectableTodoItems.size
    val defaultReminderRing = if (uiState.settings.workQuietModeEnabled) false else uiState.settings.defaultRingEnabled
    val defaultReminderVibrate = if (uiState.settings.workQuietModeEnabled) true else uiState.settings.defaultVibrateEnabled

    fun openTodoEditor(item: TodoItem) {
        if (item.isRecurring && !item.isHistory) {
            scopeDialogTarget = item
            scopeDialogMode = ScopeDialogMode.EDIT_TODO
        } else {
            editorKind = EditorKind.TODO
            editingItem = item
            editScope = RecurrenceScope.CURRENT
            editorVisible = true
        }
    }

    fun openCalendarEventEditor(item: TodoItem) {
        if (item.isRecurring) {
            scopeDialogTarget = item
            scopeDialogMode = ScopeDialogMode.EDIT_EVENT
        } else {
            editorKind = EditorKind.CALENDAR
            editingItem = item
            calendarDraftSeed = null
            editScope = RecurrenceScope.CURRENT
            editorVisible = true
        }
    }

    fun openTodoPreview(item: TodoItem) {
        quickPreviewTodo = item
        previewTodoId = null
    }

    fun requestCancelTodo(item: TodoItem) {
        if (item.isRecurring && !item.isHistory) {
            scopeDialogTarget = item
            scopeDialogMode = ScopeDialogMode.CANCEL_TODO
        } else {
            onCancelTodo(item, RecurrenceScope.CURRENT)
        }
    }

    fun requestDeleteTodo(item: TodoItem) {
        if (item.isRecurring && !item.isHistory) {
            scopeDialogTarget = item
            scopeDialogMode = ScopeDialogMode.DELETE_TODO
        } else {
            onDeleteTodo(item, RecurrenceScope.CURRENT)
            Toast.makeText(context, "任务已删除", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestCancelCalendarEvent(item: TodoItem) {
        if (item.isRecurring && !item.isHistory) {
            scopeDialogTarget = item
            scopeDialogMode = ScopeDialogMode.CANCEL_EVENT
        } else {
            onCancelCalendarEvent(item, RecurrenceScope.CURRENT)
            Toast.makeText(context, "日程已取消", Toast.LENGTH_SHORT).show()
        }
    }

    fun exitTodoBatchSelection() {
        showBatchCancelConfirm = false
        todoBatchSelectionMode = false
        selectedTodoIds = emptyList()
    }

    LaunchedEffect(launchRouteSerial) {
        when {
            launchRoute?.settingsSectionKey != null -> {
                calendarFocusDateEpochDay = null
                section = DashboardSection.SETTINGS
            }
            launchRoute?.openTasks == true -> {
                calendarFocusDateEpochDay = null
                section = DashboardSection.ACTIVE
            }
            launchRoute?.openCalendar == true -> {
                calendarFocusDateEpochDay = null
                section = DashboardSection.CALENDAR
            }
            launchRoute?.targetEventId != null -> section = DashboardSection.CALENDAR
            launchRoute?.targetPlanningNoteId != null -> {
                calendarFocusDateEpochDay = null
                section = DashboardSection.PLANNING
                onSelectPlanningNote(launchRoute.targetPlanningNoteId)
            }
            launchRoute?.targetAiReportId != null -> {
                calendarFocusDateEpochDay = null
                section = DashboardSection.AI_REPORTS
            }
            launchRoute?.openBoard == true -> {
                calendarFocusDateEpochDay = null
                section = DashboardSection.BOARD
            }
        }
    }

    LaunchedEffect(launchRouteSerial, launchRoute?.targetTodoId) {
        val targetTodoId = launchRoute?.targetTodoId ?: return@LaunchedEffect
        if (handledTodoLaunchSerial == launchRouteSerial) return@LaunchedEffect
        val target = onGetTodoById(targetTodoId)
            ?.takeIf { it.isTodo }
            ?: return@LaunchedEffect
        handledTodoLaunchSerial = launchRouteSerial
        section = if (target.isHistory) DashboardSection.HISTORY else DashboardSection.ACTIVE
        openTodoPreview(target)
        previewTodoSerial = launchRouteSerial
    }

    LaunchedEffect(launchRouteSerial, launchRoute?.targetEventId) {
        val targetEventId = launchRoute?.targetEventId ?: return@LaunchedEffect
        if (handledEventLaunchSerial == launchRouteSerial) return@LaunchedEffect
        val target = onGetTodoById(targetEventId)
            ?.takeIf { it.isEvent }
            ?: return@LaunchedEffect
        handledEventLaunchSerial = launchRouteSerial
        val targetDate = target.eventStartDate() ?: target.dueDate()
        calendarFocusDateEpochDay = targetDate.toEpochDay()
        onCalendarVisibleDateRangeChange(targetDate.minusDays(1), targetDate.plusDays(2))
        section = DashboardSection.CALENDAR
    }

    LaunchedEffect(uiState.dataReady) {
        if (uiState.dataReady) {
            launchVisible = false
        }
    }

    LaunchedEffect(Unit) {
        delay(800)
        launchVisible = false
    }

    LaunchedEffect(section) {
        if (section == DashboardSection.BOARD && boardVisited) {
            onNextQuote()
        }
        if (section == DashboardSection.BOARD) {
            boardVisited = true
        }
    }

    LaunchedEffect(uiState.settings.themeMode, reopenDrawerAfterThemeChange) {
        if (reopenDrawerAfterThemeChange) {
            if (!drawerState.isOpen) {
                drawerState.open()
            }
            reopenDrawerAfterThemeChange = false
        }
    }

    LaunchedEffect(section, batchSelectableTodoItems) {
        if (section != DashboardSection.ACTIVE && section != DashboardSection.BOARD) {
            exitTodoBatchSelection()
        } else {
            val selectableIds = batchSelectableTodoItems.mapTo(mutableSetOf()) { it.id }
            selectedTodoIds = selectedTodoIds.filter { it in selectableIds }
        }
    }

    BackHandler(enabled = !launchVisible) {
        when {
            todoBatchSelectionMode -> exitTodoBatchSelection()
            editorVisible -> {
                editorVisible = false
                editingItem = null
                editingTodoGroupIds = emptyList()
                calendarDraftSeed = null
            }
            todoBatchImportVisible -> todoBatchImportVisible = false
            batchImportVisible -> batchImportVisible = false
            quickPreviewTodo != null -> quickPreviewTodo = null
            drawerState.isOpen -> scope.launch { drawerState.close() }
            section != DashboardSection.BOARD -> section = DashboardSection.BOARD
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressedAt <= 1500L) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressedAt = now
                    Toast.makeText(context, "再按一次退出应用", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(editorVisible, editorKind, editingItem?.id) {
        val target = editingItem?.takeIf { editorVisible && editorKind == EditorKind.TODO && it.isTodo }
        editingTodoGroupIds = if (target == null) {
            emptyList()
        } else {
            onGetTodoGroupIds(target.id).ifEmpty { listOf(target.groupId).filter { it > 0 } }
        }
    }

    if (launchVisible) {
        LaunchScreen()
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DashboardDrawer(
                current = section,
                selectedThemeMode = uiState.settings.themeMode,
                onSelectSection = { next ->
                    scope.launch {
                        section = next
                        drawerState.close()
                    }
                },
                onThemeModeChange = { mode ->
                    reopenDrawerAfterThemeChange = drawerState.isOpen
                    onThemeModeChange(mode)
                },
            )
        }
    ) {
        val baseColorScheme = MaterialTheme.colorScheme
        val darkMode = baseColorScheme.background.luminance() < 0.5f
        val pageColorScheme = if (section == DashboardSection.ACTIVE && darkMode) {
            baseColorScheme.copy(
                background = Color(0xFF142933),
                surface = Color(0xFF1D3640),
                surfaceVariant = Color(0xFF294854),
                outlineVariant = Color(0xFF49636D)
            )
        } else {
            baseColorScheme
        }
        MaterialTheme(
            colorScheme = pageColorScheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val usesWorkspaceBackground = section == DashboardSection.CALENDAR ||
                    section == DashboardSection.ACTIVE ||
                    section == DashboardSection.SETTINGS
                if (usesWorkspaceBackground) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                } else {
                    Image(
                        painter = painterResource(id = if (darkMode) R.drawable.dashboard_bg_dark else R.drawable.dashboard_bg_light),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        if (darkMode) Color(0x3A081723) else Color(0x0FFFFFFF),
                                        if (darkMode) Color(0x240B1822) else Color(0x06000000),
                                        if (darkMode) Color(0x16040B10) else Color(0x12000000)
                                    )
                                )
                            )
                    )
                }

                Scaffold(
                    containerColor = if (usesWorkspaceBackground) MaterialTheme.colorScheme.background else Color.Transparent,
                    topBar = {
                        DashboardTopBar(
                            title = if (todoBatchSelectionMode) {
                                "多个待办 · 已选 ${selectedTodoIds.size} 项"
                            } else {
                                section.topBarTitle
                            },
                            navigationIcon = if (todoBatchSelectionMode) Icons.AutoMirrored.Rounded.ArrowBack else null,
                            navigationContentDescription = if (todoBatchSelectionMode) "退出批量选择" else "打开菜单",
                            onNavigationClick = if (todoBatchSelectionMode) {
                                { exitTodoBatchSelection() }
                            } else {
                                { scope.launch { drawerState.open() } }
                            },
                            actions = {
                                if (todoBatchSelectionMode) {
                                    TextButton(
                                        onClick = {
                                            selectedTodoIds = if (allVisibleTodosSelected) {
                                                emptyList()
                                            } else {
                                                batchSelectableTodoItems.map { it.id }
                                            }
                                        },
                                        enabled = batchSelectableTodoItems.isNotEmpty()
                                    ) {
                                        Text(if (allVisibleTodosSelected) "清空" else "全选")
                                    }
                                } else if (section == DashboardSection.BOARD) {
                                    IconButton(onClick = { searchVisible = true }) {
                                        Icon(Icons.Rounded.Search, contentDescription = "全局搜索")
                                    }
                                }
                            }
                        )
                    },
                    floatingActionButtonPosition = if (todoBatchSelectionMode) FabPosition.Center else FabPosition.End,
                    floatingActionButton = {
                        if (todoBatchSelectionMode) {
                            TodoBatchFloatingActions(
                                selectedCount = selectedTodoIds.size,
                                onCancelSelected = { showBatchCancelConfirm = true },
                                onCompleteSelected = {
                                    val selectedItems = batchSelectableTodoItems.filter { it.id in selectedTodoIdSet }
                                    if (selectedItems.isNotEmpty()) {
                                        onCompleteTodos(selectedItems)
                                        exitTodoBatchSelection()
                                    }
                                }
                            )
                        } else if (section == DashboardSection.ACTIVE) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SmallFloatingActionButton(
                                    onClick = { todoBatchImportVisible = true },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Icon(Icons.Rounded.PostAdd, contentDescription = "批量添加待办")
                                }
                                FloatingActionButton(onClick = {
                                    editingItem = null
                                    editorKind = EditorKind.TODO
                                    calendarDraftSeed = null
                                    editScope = RecurrenceScope.CURRENT
                                    editorVisible = true
                                }) {
                                    Text("+")
                                }
                            }
                        }
                    }
                ) { padding ->
                DashboardBody(
                    section = section,
                    settingsInitialSectionKey = launchRoute?.settingsSectionKey,
                    settingsInitialSectionSerial = launchRouteSerial,
                    targetEventId = previewEventId ?: launchRoute?.targetEventId,
                    targetEventSerial = if (previewEventId != null) previewEventSerial else launchRouteSerial,
                    calendarFocusDate = calendarFocusDateEpochDay?.let(LocalDate::ofEpochDay),
                    calendarFocusDateSerial = launchRouteSerial,
                    targetAiReportId = searchAiReportTargetId ?: launchRoute?.targetAiReportId,
                    targetAiReportSerial = if (searchAiReportTargetId != null) searchAiReportTargetSerial else launchRouteSerial,
                    padding = padding,
                    uiState = uiState,
                    desktopSyncStatus = desktopSyncStatus,
                    planningNotes = planningNotes,
                    observeAiReports = observeAiReports,
                    onGetAiReport = onGetAiReport,
                    onGetTodoById = onGetTodoById,
                    onGetEventCheckIns = onGetEventCheckIns,
                    onCompleteCalendarEvent = onCompleteCalendarEvent,
                    historyItems = historyItems,
                    calendarItems = calendarItems,
                    scheduleTemplates = scheduleTemplates,
                    onCalendarVisibleDateRangeChange = onCalendarVisibleDateRangeChange,
                    reminderChainLogs = reminderChainLogs,
                    permissions = permissions,
                    onEdit = { openTodoEditor(it) },
                    onEditCalendarEvent = { openCalendarEventEditor(it) },
                    previewTodoId = previewTodoId,
                    previewTodoSerial = previewTodoSerial,
                    onPreviewTodoConsumed = {
                        previewTodoId = null
                    },
                    onNavigateTasks = {
                        calendarFocusDateEpochDay = null
                        section = DashboardSection.ACTIVE
                    },
                    onNavigateCalendar = {
                        calendarFocusDateEpochDay = null
                        section = DashboardSection.CALENDAR
                    },
                    onPreviewCalendarEvent = { item ->
                        val targetDate = item.eventStartDate() ?: item.dueDate()
                        calendarFocusDateEpochDay = targetDate.toEpochDay()
                        onCalendarVisibleDateRangeChange(targetDate.minusDays(1), targetDate.plusDays(2))
                        previewEventId = item.id
                        previewEventSerial += 1
                        section = DashboardSection.CALENDAR
                    },
                    onQuickCreateCalendarEvent = { startAt, endAt ->
                        editorKind = EditorKind.CALENDAR
                        editingItem = null
                        calendarDraftSeed = CalendarEventDraft(
                            title = "",
                            notes = "",
                            location = "",
                            startAt = startAt,
                            endAt = endAt,
                            allDay = false,
                            accentColorHex = "#4E87E1",
                            reminderMinutesBefore = 15,
                            reminderOffsetsMinutes = listOf(15),
                            ringEnabled = defaultReminderRing,
                            vibrateEnabled = defaultReminderVibrate,
                            reminderDeliveryMode = uiState.settings.defaultCalendarReminderMode,
                            recurrence = RecurrenceConfig()
                        )
                        editScope = RecurrenceScope.CURRENT
                        editorVisible = true
                    },
                    onMoveCalendarEvent = { item, startAt, endAt ->
                        scope.launch {
                            val recurrence = if (item.isRecurring) {
                                RecurrenceConfig(
                                    enabled = true,
                                    type = item.recurrenceTypeEnum,
                                    weeklyDays = storageStringToWeekdays(item.recurrenceWeekdays),
                                    endDate = item.recurrenceEndDate
                                )
                            } else {
                                RecurrenceConfig()
                            }
                            val draft = CalendarEventDraft(
                                title = item.title,
                                notes = item.notes,
                                location = item.location,
                                startAt = startAt,
                                endAt = endAt,
                                allDay = item.allDay,
                                accentColorHex = item.accentColorHex ?: "#4E87E1",
                                reminderMinutesBefore = item.configuredReminderOffsetsMinutes.minOrNull(),
                                reminderOffsetsMinutes = item.configuredReminderOffsetsMinutes,
                                ringEnabled = item.ringEnabled,
                                vibrateEnabled = item.vibrateEnabled,
                                reminderDeliveryMode = item.reminderDeliveryModeEnum,
                                countdownEnabled = item.countdownEnabled,
                                checkInEnabled = item.checkInEnabled,
                                recurrence = recurrence,
                                groupId = item.groupId
                            )
                            val targetScope = if (item.isRecurring) RecurrenceScope.CURRENT_AND_FUTURE else RecurrenceScope.CURRENT
                            val message = onUpdateCalendarEvent(item, draft, targetScope)
                            Toast.makeText(
                                context,
                                message ?: if (item.isRecurring) "已调整本次及后续日程" else "日程已移动",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onCompleteTodo = onCompleteTodo,
                    todoBatchSelectionMode = todoBatchSelectionMode,
                    selectedTodoIds = selectedTodoIdSet,
                    onToggleTodoSelection = { itemId ->
                        selectedTodoIds = selectedTodoIds.toggleId(itemId)
                    },
                    onEnterTodoSelection = { itemId ->
                        todoBatchSelectionMode = true
                        selectedTodoIds = (selectedTodoIds + itemId).distinct()
                    },
                    onBatchSelectableItemsChange = { items ->
                        batchSelectableTodoItems = items
                    },
                    onRestoreTodo = onRestoreTodo,
                    onCancelTodo = ::requestCancelTodo,
                    onCancelCalendarEvent = ::requestCancelCalendarEvent,
                    onDeleteTodo = ::requestDeleteTodo,
                    onDeleteCalendarEvent = { item ->
                        if (item.isRecurring) {
                            scopeDialogTarget = item
                            scopeDialogMode = ScopeDialogMode.DELETE_EVENT
                        } else {
                            onDeleteCalendarEvent(item, RecurrenceScope.CURRENT)
                        }
                    },
                    onSelectGroup = onSelectGroup,
                    onToggleGroupFilterMode = onToggleGroupFilterMode,
                    onCreateGroup = onCreateGroup,
                    onUpdateGroup = onUpdateGroup,
                    onDeleteGroup = onDeleteGroup,
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
                    onDeleteAiReport = onDeleteAiReport,
                    onLaunchCheckIn = onLaunchCheckIn,
                    onResetOnboarding = onResetOnboarding,
                    onDesktopSyncEnabledChange = onDesktopSyncEnabledChange,
                    onDesktopSyncWifiKeepAliveChange = onDesktopSyncWifiKeepAliveChange,
                    onRotateDesktopSyncToken = onRotateDesktopSyncToken,
                    onUseBuiltInReminderTone = onUseBuiltInReminderTone,
                    onPickSystemReminderTone = onPickSystemReminderTone,
                    onOpenWiki = onOpenWiki,
                    onRunReminderChainTest = onRunReminderChainTest,
                    onClearReminderDiagnostics = onClearReminderDiagnostics,
                    onSaveWeekAsScheduleTemplate = onSaveWeekAsScheduleTemplate,
                    onApplyScheduleTemplateToWeek = onApplyScheduleTemplateToWeek,
                    onGenerateSemesterScheduleFromTemplate = onGenerateSemesterScheduleFromTemplate,
                    onDeleteScheduleTemplate = onDeleteScheduleTemplate,
                    onPickBackupDirectory = onPickBackupDirectory,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    onAutoBackupChange = onAutoBackupChange,
                    onInspectDataHealth = onInspectDataHealth,
                    onCleanSafeDataHealthItems = onCleanSafeDataHealthItems,
                    onOpenCalendarBatchImport = { batchImportVisible = true },
                    onSelectPlanningNote = onSelectPlanningNote,
                    onCreatePlanningNote = onCreatePlanningNote,
                    onSavePlanningNote = onSavePlanningNote,
                    onRenamePlanningNote = onRenamePlanningNote,
                    onDeletePlanningNote = onDeletePlanningNote,
                    onArchivePlanningNote = onArchivePlanningNote,
                    onOpenTodayPlanningNote = onOpenTodayPlanningNote,
                    observePlanningNodes = observePlanningNodes,
                    onCreatePlanningNode = onCreatePlanningNode,
                    onUpdatePlanningNode = onUpdatePlanningNode,
                    onTogglePlanningNode = onTogglePlanningNode,
                    onPublishPlanningNode = onPublishPlanningNode,
                    onPublishAllPlanningDrafts = onPublishAllPlanningDrafts,
                    onDeletePlanningNode = onDeletePlanningNode,
                    onReorderPlanningNodes = onReorderPlanningNodes,
                    onCreatePlanningNodeSnapshot = onCreatePlanningNodeSnapshot,
                    onRestorePlanningNodeSnapshot = onRestorePlanningNodeSnapshot,
                    highlightedPlanningNodeId = highlightedPlanningNodeId,
                    highlightedPlanningNodeSerial = highlightedPlanningNodeSerial,
                    onOpenPlanningLinkedItem = { itemId ->
                        scope.launch {
                            val item = onGetTodoById(itemId) ?: return@launch
                            if (item.isEvent) {
                                val targetDate = item.eventStartDate() ?: item.dueDate()
                                calendarFocusDateEpochDay = targetDate.toEpochDay()
                                onCalendarVisibleDateRangeChange(targetDate.minusDays(1), targetDate.plusDays(2))
                                previewEventId = item.id
                                previewEventSerial += 1
                                section = DashboardSection.CALENDAR
                            } else {
                                openTodoPreview(item)
                            }
                        }
                    },
                    onExportPlanningNodesMarkdown = onExportPlanningNodesMarkdown,
                    onReplacePlanningNodesFromMarkdown = onReplacePlanningNodesFromMarkdown,
                    onParsePlanningMarkdown = onParsePlanningMarkdown,
                    onImportPlanningCandidates = onImportPlanningCandidates,
                    onSyncPlanningMappings = onSyncPlanningMappings,
                    onGetPlanningMappings = onGetPlanningMappings,
                    onRefreshPlanningImportedItems = onRefreshPlanningImportedItems,
                    onPostponePlanningImportedItems = onPostponePlanningImportedItems,
                    onUndoLastPlanningOperation = onUndoLastPlanningOperation,
                    onApplyPlanningConflictDocument = onApplyPlanningConflictDocument,
                    onApplyPlanningConflictItem = onApplyPlanningConflictItem,
                    onDismissOnboarding = onDismissOnboarding,
                    onNavigatePlanning = { section = DashboardSection.PLANNING },
                    onBoardCollapseStateChange = onBoardCollapseStateChange
                )
            }
        }
    }
    }

    if (showBatchCancelConfirm) {
        PaykiDecisionBottomSheet(
            title = "取消所选待办",
            message = "将所选 ${selectedTodoIds.size} 项分别取消，并写入历史记录。循环待办只取消当前选中的这一次。",
            confirmLabel = "确认取消",
            confirmLabelColor = Color(0xFFD97706),
            onDismiss = { showBatchCancelConfirm = false },
            onConfirm = {
                val selectedItems = batchSelectableTodoItems.filter { it.id in selectedTodoIdSet }
                showBatchCancelConfirm = false
                if (selectedItems.isNotEmpty()) {
                    onCancelTodos(selectedItems)
                    exitTodoBatchSelection()
                }
            }
        )
    }

    if (todoBatchImportVisible) {
        TodoBatchImportDialog(
            groups = uiState.groups,
            defaults = TodoBatchImportDefaults(
                defaultGroupId = uiState.groups.firstOrNull { it.name == "例行" }?.id ?: uiState.groups.firstOrNull()?.id ?: 0L,
                defaultRingEnabled = defaultReminderRing,
                defaultVibrateEnabled = defaultReminderVibrate
            ),
            onDismiss = { todoBatchImportVisible = false },
            onImport = { drafts ->
                scope.launch {
                    val message = onImportTodos(drafts)
                    if (message == null) {
                        todoBatchImportVisible = false
                        Toast.makeText(context, "已导入 ${drafts.size} 条待办", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (searchVisible) {
        SearchPanel(
            groups = uiState.groups,
            onSearch = onGlobalSearch,
            onDismiss = { searchVisible = false },
            onOpenTodo = { item ->
                openTodoPreview(item)
                section = if (item.isHistory) DashboardSection.HISTORY else DashboardSection.ACTIVE
                searchVisible = false
            },
            onOpenEvent = { item ->
                val targetDate = item.eventStartDate() ?: item.dueDate()
                calendarFocusDateEpochDay = targetDate.toEpochDay()
                onCalendarVisibleDateRangeChange(targetDate.minusDays(1), targetDate.plusDays(2))
                previewEventId = item.id
                previewEventSerial += 1
                section = DashboardSection.CALENDAR
                searchVisible = false
            },
            onOpenPlanningNode = { result ->
                onSelectPlanningNote(result.node.noteId)
                highlightedPlanningNodeId = result.node.id
                highlightedPlanningNodeSerial += 1
                section = DashboardSection.PLANNING
                searchVisible = false
            },
            onOpenAiReport = { report ->
                searchAiReportTargetId = report.id
                searchAiReportTargetSerial += 1
                section = DashboardSection.AI_REPORTS
                searchVisible = false
            }
        )
    }

    quickPreviewTodo?.let { item ->
        TodoDetailsDialog(
            item = item,
            groups = uiState.groups,
            onDismiss = { quickPreviewTodo = null },
            showCreated = true,
            showStatusTime = item.isHistory,
            onEdit = {
                quickPreviewTodo = null
                openTodoEditor(item)
            },
            onCancel = if (!item.isTodo || item.completed || item.canceled) {
                null
            } else {
                {
                    quickPreviewTodo = null
                    requestCancelTodo(item)
                }
            },
            onDelete = if (!item.isTodo || item.completed || item.canceled) {
                null
            } else {
                {
                    quickPreviewTodo = null
                    requestDeleteTodo(item)
                }
            },
            onRestore = if (item.completed || item.canceled) {
                {
                    quickPreviewTodo = null
                    onRestoreTodo(item)
                }
            } else {
                null
            }
        )
    }

    if (batchImportVisible) {
        CalendarBatchImportDialog(
            defaults = CalendarBatchImportDefaults(
                defaultReminderMinutesBefore = 5,
                defaultRingEnabled = defaultReminderRing,
                defaultVibrateEnabled = defaultReminderVibrate,
                defaultReminderDeliveryMode = uiState.settings.defaultCalendarReminderMode
            ),
            onDismiss = { batchImportVisible = false },
            onImport = { drafts ->
                scope.launch {
                    val message = onImportCalendarEvents(drafts)
                    if (message == null) {
                        batchImportVisible = false
                        Toast.makeText(context, "已导入 ${drafts.size} 条日程", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (editorVisible && editorKind == EditorKind.TODO) {
        TodoEditorDialog(
            initialTodo = editingItem?.takeIf { it.isTodo },
            groups = uiState.groups,
            initialGroupIds = editingTodoGroupIds,
            defaultRingEnabled = editingItem?.ringEnabled ?: defaultReminderRing,
            defaultVibrateEnabled = editingItem?.vibrateEnabled ?: defaultReminderVibrate,
            onDismiss = {
                editorVisible = false
                editingItem = null
                editingTodoGroupIds = emptyList()
            },
            onConfirm = { draft ->
                scope.launch {
                    val current = editingItem?.takeIf { it.isTodo }
                    val message = if (current == null) {
                        onAddTodo(draft)
                    } else {
                        onUpdateTodo(current, draft, editScope)
                    }

                    if (message == null) {
                        editorVisible = false
                        editingItem = null
                        editingTodoGroupIds = emptyList()
                        Toast.makeText(
                            context,
                            if (current == null) "任务已创建" else "任务已更新",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (editorVisible && editorKind == EditorKind.CALENDAR) {
        CalendarEventEditorDialog(
            initialEvent = editingItem?.takeIf { it.isEvent },
            initialDraft = calendarDraftSeed,
            groups = uiState.groups,
            defaultRingEnabled = editingItem?.ringEnabled ?: defaultReminderRing,
            defaultVibrateEnabled = editingItem?.vibrateEnabled ?: defaultReminderVibrate,
            defaultReminderDeliveryMode = editingItem?.reminderDeliveryModeEnum
                ?: uiState.settings.defaultCalendarReminderMode,
            onDismiss = {
                editorVisible = false
                editingItem = null
                calendarDraftSeed = null
            },
            onConfirm = { draft ->
                scope.launch {
                    val current = editingItem?.takeIf { it.isEvent }
                    val message = if (current == null) {
                        onAddCalendarEvent(draft)
                    } else {
                        onUpdateCalendarEvent(current, draft, editScope)
                    }

                    if (message == null) {
                        editorVisible = false
                        editingItem = null
                        calendarDraftSeed = null
                        Toast.makeText(
                            context,
                            if (current == null) "日程已创建" else "日程已更新",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onConfirmMultiple = { drafts ->
                scope.launch {
                    val current = editingItem?.takeIf { it.isEvent }
                    val message = if (current == null) {
                        onImportCalendarEvents(drafts)
                    } else {
                        "编辑已有日程暂不支持批量修改多个时间段"
                    }

                    if (message == null) {
                        editorVisible = false
                        editingItem = null
                        calendarDraftSeed = null
                        Toast.makeText(context, "已创建 ${drafts.size} 个每周时间段", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onConfirmMultiSlotBundleShared = { draft ->
                scope.launch {
                    val current = editingItem?.takeIf { it.isEvent }
                    val message = if (current == null) {
                        "新增日程不需要同步已有多时间段组"
                    } else {
                        onUpdateCalendarEventMultiSlotBundleSharedFields(current, draft)
                    }

                    if (message == null) {
                        editorVisible = false
                        editingItem = null
                        calendarDraftSeed = null
                        Toast.makeText(context, "已同步同组时间段", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    scopeDialogTarget?.let { item ->
        val mode = scopeDialogMode
        RecurrenceScopeDialog(
            title = recurrenceScopeDialogTitle(mode),
            subtitle = recurrenceScopeDialogSubtitle(mode),
            options = recurrenceScopeOptions(mode),
            onDismiss = {
                scopeDialogTarget = null
                scopeDialogMode = null
            },
            onSelect = { selectedScope ->
                when (scopeDialogMode) {
                    ScopeDialogMode.EDIT_TODO -> {
                        editScope = selectedScope
                        editorKind = EditorKind.TODO
                        editingItem = item
                        editorVisible = true
                    }
                    ScopeDialogMode.EDIT_EVENT -> {
                        editScope = selectedScope
                        editorKind = EditorKind.CALENDAR
                        editingItem = item
                        editorVisible = true
                    }
                    ScopeDialogMode.CANCEL_TODO -> onCancelTodo(item, selectedScope)
                    ScopeDialogMode.CANCEL_EVENT -> {
                        onCancelCalendarEvent(item, selectedScope)
                        Toast.makeText(context, "日程已取消", Toast.LENGTH_SHORT).show()
                    }
                    ScopeDialogMode.DELETE_TODO -> {
                        onDeleteTodo(item, selectedScope)
                        Toast.makeText(context, "任务已删除", Toast.LENGTH_SHORT).show()
                    }
                    ScopeDialogMode.DELETE_EVENT -> {
                        onDeleteCalendarEvent(item, selectedScope)
                        Toast.makeText(context, "日程已删除", Toast.LENGTH_SHORT).show()
                    }
                    null -> Unit
                }
                scopeDialogTarget = null
                scopeDialogMode = null
            }
        )
    }
}

@Composable
private fun RecurrenceScopeDialog(
    title: String,
    subtitle: String,
    options: List<RecurrenceScopeOptionUi>,
    onDismiss: () -> Unit,
    onSelect: (RecurrenceScope) -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    )
                ) {
                    androidx.compose.foundation.layout.Column {
                        options.forEachIndexed { index, option ->
                            RecurrenceScopeOptionRow(
                                index = index + 1,
                                option = option,
                                onClick = { onSelect(option.scope) }
                            )
                            if (index != options.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(start = 58.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
                                )
                            }
                        }
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurrenceScopeOptionRow(
    index: Int,
    option: RecurrenceScopeOptionUi,
    onClick: () -> Unit
) {
    val titleColor = if (option.destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    androidx.compose.material3.Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (index > 0) {
                androidx.compose.material3.Surface(
                    modifier = Modifier.size(32.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (option.destructive) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.58f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                    }
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = index.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = titleColor
                        )
                    }
                }
            }
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = titleColor
                )
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class RecurrenceScopeOptionUi(
    val scope: RecurrenceScope,
    val title: String,
    val description: String,
    val destructive: Boolean = false
)

private fun recurrenceScopeDialogTitle(mode: ScopeDialogMode?): String {
    return when (mode) {
        ScopeDialogMode.CANCEL_TODO -> "取消循环待办"
        ScopeDialogMode.CANCEL_EVENT -> "取消循环日程"
        ScopeDialogMode.DELETE_TODO -> "删除循环待办"
        ScopeDialogMode.DELETE_EVENT -> "删除循环日程"
        ScopeDialogMode.EDIT_TODO, ScopeDialogMode.EDIT_EVENT -> "选择修改范围"
        null -> "选择范围"
    }
}

private fun recurrenceScopeDialogSubtitle(mode: ScopeDialogMode?): String {
    return when (mode) {
        ScopeDialogMode.CANCEL_TODO, ScopeDialogMode.CANCEL_EVENT -> "取消会进入历史记录，不等于删除。"
        ScopeDialogMode.DELETE_TODO, ScopeDialogMode.DELETE_EVENT -> "删除不会进入历史记录，无法通过历史记录恢复。"
        ScopeDialogMode.EDIT_TODO, ScopeDialogMode.EDIT_EVENT -> "决定这次编辑影响哪一部分循环事项。"
        null -> "循环事项需要先确定这次操作影响的范围。"
    }
}

private fun recurrenceScopeOptions(mode: ScopeDialogMode?): List<RecurrenceScopeOptionUi> {
    return when (mode) {
        ScopeDialogMode.CANCEL_TODO -> listOf(
            RecurrenceScopeOptionUi(RecurrenceScope.CURRENT, "仅取消这一次", "这次进入历史记录，后续任务照常生成。"),
            RecurrenceScopeOptionUi(RecurrenceScope.CURRENT_AND_FUTURE, "从这次起停止", "保留过去记录，并停止本次及后续任务。"),
            RecurrenceScopeOptionUi(RecurrenceScope.ALL, "取消整个系列", "所有已生成和后续任务都标记为已取消。")
        )
        ScopeDialogMode.CANCEL_EVENT -> listOf(
            RecurrenceScopeOptionUi(RecurrenceScope.CURRENT, "仅取消这一次", "只取消这次日程，后续日程照常生成。"),
            RecurrenceScopeOptionUi(RecurrenceScope.CURRENT_AND_FUTURE, "从这次起停止", "保留过去记录，并停止本次及后续日程。"),
            RecurrenceScopeOptionUi(RecurrenceScope.ALL, "取消整个系列", "整个循环日程都会标记为已取消。")
        )
        ScopeDialogMode.DELETE_TODO -> listOf(
            RecurrenceScopeOptionUi(RecurrenceScope.CURRENT, "仅删除这一次", "直接移除，不写入历史记录。", destructive = true),
            RecurrenceScopeOptionUi(RecurrenceScope.CURRENT_AND_FUTURE, "从这次起删除", "删除本次和未来任务，过去记录保留。", destructive = true),
            RecurrenceScopeOptionUi(RecurrenceScope.ALL, "删除整个系列", "永久删除整个循环系列。", destructive = true)
        )
        ScopeDialogMode.DELETE_EVENT -> listOf(
            RecurrenceScopeOptionUi(RecurrenceScope.CURRENT, "仅删除这一次", "直接移除，不写入历史记录。", destructive = true),
            RecurrenceScopeOptionUi(RecurrenceScope.CURRENT_AND_FUTURE, "从这次起删除", "删除本次和未来日程，过去记录保留。", destructive = true),
            RecurrenceScopeOptionUi(RecurrenceScope.ALL, "删除整个系列", "永久删除整个循环系列。", destructive = true)
        )
        ScopeDialogMode.EDIT_TODO -> listOf(
            RecurrenceScopeOptionUi(RecurrenceScope.CURRENT, "仅修改这一次", "后续任务继续使用原来的循环规则。"),
            RecurrenceScopeOptionUi(RecurrenceScope.CURRENT_AND_FUTURE, "从这次起应用", "本次及后续任务使用新的内容和时间。"),
            RecurrenceScopeOptionUi(RecurrenceScope.ALL, "修改整个系列", "所有循环任务统一使用新的设置。")
        )
        ScopeDialogMode.EDIT_EVENT -> listOf(
            RecurrenceScopeOptionUi(RecurrenceScope.CURRENT, "仅修改这一次", "后续日程继续使用原来的循环规则。"),
            RecurrenceScopeOptionUi(RecurrenceScope.CURRENT_AND_FUTURE, "从这次起应用", "本次及后续日程使用新的内容和时间。"),
            RecurrenceScopeOptionUi(RecurrenceScope.ALL, "修改整个系列", "所有循环日程统一使用新的设置。")
        )
        null -> RecurrenceScope.entries.map { scope ->
            RecurrenceScopeOptionUi(
                scope = scope,
                title = when (scope) {
                    RecurrenceScope.CURRENT -> "仅当前"
                    RecurrenceScope.CURRENT_AND_FUTURE -> "当前及之后"
                    RecurrenceScope.ALL -> "全部循环"
                },
                description = ""
            )
        }
    }
}

internal fun dashboardPadding(bottom: Dp = 10.dp) = androidx.compose.foundation.layout.PaddingValues(
    start = 20.dp,
    top = 10.dp,
    end = 20.dp,
    bottom = bottom
)
