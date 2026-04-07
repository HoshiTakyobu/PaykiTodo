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
import com.example.todoalarm.data.RecurrenceScope
import com.example.todoalarm.data.ThemeMode
import com.example.todoalarm.data.TodoDraft
import com.example.todoalarm.data.TodoItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ScopeDialogMode {
    EDIT,
    CANCEL
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
    onUpdateTodo: suspend (TodoItem, TodoDraft, RecurrenceScope) -> String?,
    onDeleteTodo: (TodoItem) -> Unit,
    onCompleteTodo: (TodoItem) -> Unit,
    onRestoreTodo: (TodoItem) -> Unit,
    onCancelTodo: (TodoItem, RecurrenceScope) -> Unit,
    onSelectGroup: (Long?) -> Unit,
    onCreateGroup: suspend (String, String) -> String?,
    onUpdateGroup: suspend (com.example.todoalarm.data.TaskGroup) -> String?,
    onDeleteGroup: suspend (Long) -> String?,
    onThemeModeChange: (ThemeMode) -> Unit,
    onNextQuote: () -> Unit,
    onDefaultSnoozeChange: (Int) -> Unit,
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
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
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
                editingTodo = null
            }
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
                    scope.launch { drawerState.close() }
                },
                onSelectAllTasks = {
                    section = DashboardSection.ACTIVE
                    onSelectGroup(null)
                    scope.launch { drawerState.close() }
                },
                onSelectGroup = {
                    section = DashboardSection.ACTIVE
                    onSelectGroup(it)
                    scope.launch { drawerState.close() }
                },
                onThemeModeChange = onThemeModeChange
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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

            Scaffold(
                containerColor = Color.Transparent,
                topBar = { DashboardTopBar { scope.launch { drawerState.open() } } },
                floatingActionButton = {
                    if (section == DashboardSection.ACTIVE) {
                        DashboardFab {
                            editingTodo = null
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
                            scopeDialogMode = ScopeDialogMode.EDIT
                        } else {
                            editingTodo = item
                            editScope = RecurrenceScope.CURRENT
                            editorVisible = true
                        }
                    },
                    onCompleteTodo = onCompleteTodo,
                    onRestoreTodo = onRestoreTodo,
                    onCancelTodo = { item ->
                        if (item.isRecurring && !item.isHistory) {
                            scopeDialogTarget = item
                            scopeDialogMode = ScopeDialogMode.CANCEL
                        } else {
                            onCancelTodo(item, RecurrenceScope.CURRENT)
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
                    onNextQuote = onNextQuote,
                    onDefaultSnoozeChange = onDefaultSnoozeChange,
                    onPickBackupDirectory = onPickBackupDirectory,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    onAutoBackupChange = onAutoBackupChange
                )
            }
        }
    }

    if (editorVisible) {
        TodoEditorDialog(
            initialTodo = editingTodo,
            groups = uiState.groups,
            defaultRingEnabled = editingTodo?.ringEnabled ?: uiState.settings.defaultRingEnabled,
            defaultVibrateEnabled = editingTodo?.vibrateEnabled ?: uiState.settings.defaultVibrateEnabled,
            onDismiss = {
                editorVisible = false
                editingTodo = null
            },
            onDelete = {
                editingTodo?.let {
                    onDeleteTodo(it)
                    editorVisible = false
                    editingTodo = null
                    Toast.makeText(context, "任务已删除", Toast.LENGTH_SHORT).show()
                }
            },
            onConfirm = { draft ->
                scope.launch {
                    val current = editingTodo
                    val message = if (current == null) {
                        onAddTodo(draft)
                    } else {
                        onUpdateTodo(current, draft, editScope)
                    }

                    if (message == null) {
                        editorVisible = false
                        editingTodo = null
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

    scopeDialogTarget?.let { item ->
        RecurrenceScopeDialog(
            title = if (scopeDialogMode == ScopeDialogMode.EDIT) "选择修改范围" else "选择范围",
            onDismiss = {
                scopeDialogTarget = null
                scopeDialogMode = null
            },
            onSelect = { selectedScope ->
                when (scopeDialogMode) {
                    ScopeDialogMode.EDIT -> {
                        editScope = selectedScope
                        editingTodo = item
                        editorVisible = true
                    }
                    ScopeDialogMode.CANCEL -> onCancelTodo(item, selectedScope)
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
