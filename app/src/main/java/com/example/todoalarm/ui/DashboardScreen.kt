package com.example.todoalarm.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.todoalarm.R
import com.example.todoalarm.data.CalendarEventDraft
import com.example.todoalarm.data.RecurrenceConfig
import com.example.todoalarm.data.RecurrenceScope
import com.example.todoalarm.data.ReminderDeliveryMode
import com.example.todoalarm.data.ScheduleTemplate
import com.example.todoalarm.data.ScheduleTemplateType
import com.example.todoalarm.data.ThemeMode
import com.example.todoalarm.data.TodoDraft
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.WeekStartMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

private enum class ScopeDialogMode {
    EDIT_TODO,
    CANCEL_TODO,
    EDIT_EVENT,
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
    permissions: PermissionSnapshot,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestFullScreenPermission: () -> Unit,
    onRequestNotificationPolicyAccess: () -> Unit,
    onRequestIgnoreBatteryOptimization: () -> Unit,
    onRequestAccessibilityService: () -> Unit,
    onAddTodo: suspend (TodoDraft) -> String?,
    onAddCalendarEvent: suspend (com.example.todoalarm.data.CalendarEventDraft) -> String?,
    onImportCalendarEvents: suspend (List<CalendarEventDraft>) -> String?,
    onUpdateTodo: suspend (TodoItem, TodoDraft, RecurrenceScope) -> String?,
    onUpdateCalendarEvent: suspend (TodoItem, com.example.todoalarm.data.CalendarEventDraft, RecurrenceScope) -> String?,
    onDeleteTodo: (TodoItem) -> Unit,
    onDeleteCalendarEvent: (TodoItem, RecurrenceScope) -> Unit,
    onCompleteTodo: (TodoItem) -> Unit,
    onRestoreTodo: (TodoItem) -> Unit,
    onCancelTodo: (TodoItem, RecurrenceScope) -> Unit,
    onSelectGroup: (Long?) -> Unit,
    onCreateGroup: suspend (String, String) -> String?,
    onUpdateGroup: suspend (com.example.todoalarm.data.TaskGroup) -> String?,
    onDeleteGroup: suspend (Long) -> String?,
    onThemeModeChange: (ThemeMode) -> Unit,
    onWeekStartModeChange: (WeekStartMode) -> Unit,
    onNextQuote: () -> Unit,
    onDefaultSnoozeChange: (Int) -> Unit,
    onDefaultCalendarReminderModeChange: (ReminderDeliveryMode) -> Unit,
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
    onAutoBackupChange: (Boolean) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var section by rememberSaveable { mutableStateOf(DashboardSection.ACTIVE) }
    var launchVisible by rememberSaveable { mutableStateOf(true) }
    var editorVisible by remember { mutableStateOf(false) }
    var editorKind by remember { mutableStateOf(EditorKind.TODO) }
    var editingItem by remember { mutableStateOf<TodoItem?>(null) }
    var calendarDraftSeed by remember { mutableStateOf<CalendarEventDraft?>(null) }
    var batchImportVisible by remember { mutableStateOf(false) }
    var editScope by remember { mutableStateOf(RecurrenceScope.CURRENT) }
    var scopeDialogTarget by remember { mutableStateOf<TodoItem?>(null) }
    var scopeDialogMode by remember { mutableStateOf<ScopeDialogMode?>(null) }
    var lastBackPressedAt by rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        delay(1600)
        launchVisible = false
    }

    BackHandler(enabled = !launchVisible) {
        when {
            editorVisible -> {
                editorVisible = false
                editingItem = null
                calendarDraftSeed = null
            }
            batchImportVisible -> batchImportVisible = false
            drawerState.isOpen -> scope.launch { drawerState.close() }
            section != DashboardSection.ACTIVE -> section = DashboardSection.ACTIVE
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

    if (launchVisible) {
        LaunchScreen()
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DashboardDrawer(
                current = section,
                groups = uiState.groups,
                selectedGroupId = uiState.selectedGroupId,
                selectedThemeMode = uiState.settings.themeMode,
                onSelectSection = { next ->
                    section = next
                },
                onSelectAllTasks = {
                    section = DashboardSection.ACTIVE
                    onSelectGroup(null)
                },
                onSelectGroup = {
                    section = DashboardSection.ACTIVE
                    onSelectGroup(it)
                },
                onThemeModeChange = onThemeModeChange
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (section == DashboardSection.CALENDAR) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.dashboard_bg),
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
                                    Color(0x26FFFFFF),
                                    Color(0x14000000),
                                    Color(0x2B000000)
                                )
                            )
                        )
                )
            }

            Scaffold(
                containerColor = if (section == DashboardSection.CALENDAR) MaterialTheme.colorScheme.background else Color.Transparent,
                topBar = {
                    DashboardTopBar(
                        title = section.topBarTitle,
                        onMenu = { scope.launch { drawerState.open() } }
                    )
                },
                floatingActionButton = {
                    if (section == DashboardSection.ACTIVE) {
                        DashboardFab {
                            editingItem = null
                            editorKind = if (section == DashboardSection.CALENDAR) {
                                EditorKind.CALENDAR
                            } else {
                                EditorKind.TODO
                            }
                            calendarDraftSeed = null
                            editScope = RecurrenceScope.CURRENT
                            editorVisible = true
                        }
                    }
                }
            ) { padding ->
                DashboardBody(
                    section = section,
                    padding = padding,
                    uiState = uiState,
                    permissions = permissions,
                    onEdit = { item ->
                        if (item.isRecurring && !item.isHistory) {
                            scopeDialogTarget = item
                            scopeDialogMode = ScopeDialogMode.EDIT_TODO
                        } else {
                            editorKind = EditorKind.TODO
                            editingItem = item
                            editScope = RecurrenceScope.CURRENT
                            editorVisible = true
                        }
                    },
                    onEditCalendarEvent = { item ->
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
                            ringEnabled = uiState.settings.defaultRingEnabled,
                            vibrateEnabled = uiState.settings.defaultVibrateEnabled,
                            reminderDeliveryMode = uiState.settings.defaultCalendarReminderMode,
                            recurrence = RecurrenceConfig()
                        )
                        editScope = RecurrenceScope.CURRENT
                        editorVisible = true
                    },
                    onCompleteTodo = onCompleteTodo,
                    onRestoreTodo = onRestoreTodo,
                    onCancelTodo = { item ->
                        if (item.isRecurring && !item.isHistory) {
                            scopeDialogTarget = item
                            scopeDialogMode = ScopeDialogMode.CANCEL_TODO
                        } else {
                            onCancelTodo(item, RecurrenceScope.CURRENT)
                        }
                    },
                    onDeleteCalendarEvent = { item ->
                        if (item.isRecurring) {
                            scopeDialogTarget = item
                            scopeDialogMode = ScopeDialogMode.DELETE_EVENT
                        } else {
                            onDeleteCalendarEvent(item, RecurrenceScope.CURRENT)
                        }
                    },
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
                    onNextQuote = onNextQuote,
                    onDefaultSnoozeChange = onDefaultSnoozeChange,
                    onDefaultCalendarReminderModeChange = onDefaultCalendarReminderModeChange,
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
                    onOpenCalendarBatchImport = { batchImportVisible = true }
                )
            }
        }
    }

    if (batchImportVisible) {
        CalendarBatchImportDialog(
            defaults = CalendarBatchImportDefaults(
                defaultReminderMinutesBefore = 5,
                defaultRingEnabled = true,
                defaultVibrateEnabled = true,
                defaultReminderDeliveryMode = ReminderDeliveryMode.NOTIFICATION
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
            defaultRingEnabled = editingItem?.ringEnabled ?: uiState.settings.defaultRingEnabled,
            defaultVibrateEnabled = editingItem?.vibrateEnabled ?: uiState.settings.defaultVibrateEnabled,
            onDismiss = {
                editorVisible = false
                editingItem = null
            },
            onDelete = {
                editingItem?.let {
                    onDeleteTodo(it)
                    editorVisible = false
                    editingItem = null
                    Toast.makeText(context, "任务已删除", Toast.LENGTH_SHORT).show()
                }
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
            defaultRingEnabled = editingItem?.ringEnabled ?: uiState.settings.defaultRingEnabled,
            defaultVibrateEnabled = editingItem?.vibrateEnabled ?: uiState.settings.defaultVibrateEnabled,
            defaultReminderDeliveryMode = editingItem?.reminderDeliveryModeEnum
                ?: uiState.settings.defaultCalendarReminderMode,
            onDismiss = {
                editorVisible = false
                editingItem = null
                calendarDraftSeed = null
            },
            onDelete = {
                val current = editingItem?.takeIf { it.isEvent } ?: return@CalendarEventEditorDialog
                editorVisible = false
                editingItem = null
                calendarDraftSeed = null
                if (current.isRecurring) {
                    scopeDialogTarget = current
                    scopeDialogMode = ScopeDialogMode.DELETE_EVENT
                } else {
                    onDeleteCalendarEvent(current, RecurrenceScope.CURRENT)
                    Toast.makeText(context, "日程已删除", Toast.LENGTH_SHORT).show()
                }
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
            }
        )
    }

    scopeDialogTarget?.let { item ->
        RecurrenceScopeDialog(
            title = when (scopeDialogMode) {
                ScopeDialogMode.EDIT_TODO, ScopeDialogMode.EDIT_EVENT -> "选择修改范围"
                ScopeDialogMode.CANCEL_TODO -> "选择取消范围"
                ScopeDialogMode.DELETE_EVENT -> "选择删除范围"
                null -> "选择范围"
            },
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
    onDismiss: () -> Unit,
    onSelect: (RecurrenceScope) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text("循环任务需要先确定这次操作影响的范围。") },
        confirmButton = {
            androidx.compose.foundation.layout.Column {
                RecurrenceScope.entries.forEach { scope ->
                    TextButton(onClick = { onSelect(scope) }) {
                        Text(scope.label)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

internal fun dashboardPadding() = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 10.dp)
