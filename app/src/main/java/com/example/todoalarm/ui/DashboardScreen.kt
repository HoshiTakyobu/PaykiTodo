package com.example.todoalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.ThemeMode
import com.example.todoalarm.data.TodoCategory
import com.example.todoalarm.data.TodoItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class DashboardSection(val label: String, val icon: ImageVector) {
    ACTIVE("待处理", Icons.Rounded.TaskAlt),
    HISTORY("历史记录", Icons.Rounded.History),
    SETTINGS("设置", Icons.Rounded.Settings)
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
    onAddTodo: suspend (String, String, java.time.LocalDateTime, java.time.LocalDateTime?, TodoCategory, Boolean, Boolean, Boolean) -> String?,
    onUpdateTodo: suspend (TodoItem, String, String, java.time.LocalDateTime, java.time.LocalDateTime?, TodoCategory, Boolean, Boolean, Boolean) -> String?,
    onCompleteTodo: (TodoItem) -> Unit,
    onRestoreTodo: (TodoItem) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDefaultSnoozeChange: (Int) -> Unit,
    onReminderDefaultsChange: (Boolean, Boolean, Boolean) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var section by rememberSaveable { mutableStateOf(DashboardSection.ACTIVE) }
    var launchVisible by rememberSaveable { mutableStateOf(true) }
    var editorVisible by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }

    LaunchedEffect(Unit) {
        delay(1600)
        launchVisible = false
    }

    if (launchVisible) {
        LaunchScreen()
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DashboardDrawer(current = section) { next ->
                section = next
                scope.launch { drawerState.close() }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DashboardBackgroundBrush())
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = { DashboardTopBar { scope.launch { drawerState.open() } } },
                floatingActionButton = {
                    if (section == DashboardSection.ACTIVE) {
                        DashboardFab {
                            editingTodo = null
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
                    onEdit = {
                        editingTodo = it
                        editorVisible = true
                    },
                    onCompleteTodo = onCompleteTodo,
                    onRestoreTodo = onRestoreTodo,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                    onRequestFullScreenPermission = onRequestFullScreenPermission,
                    onRequestNotificationPolicyAccess = onRequestNotificationPolicyAccess,
                    onRequestIgnoreBatteryOptimization = onRequestIgnoreBatteryOptimization,
                    onThemeModeChange = onThemeModeChange,
                    onDefaultSnoozeChange = onDefaultSnoozeChange,
                    onReminderDefaultsChange = onReminderDefaultsChange
                )
            }
        }
    }

    if (editorVisible) {
        TodoEditorDialog(
            initialTodo = editingTodo,
            defaultRingEnabled = editingTodo?.ringEnabled ?: uiState.settings.defaultRingEnabled,
            defaultVibrateEnabled = editingTodo?.vibrateEnabled ?: uiState.settings.defaultVibrateEnabled,
            defaultVoiceEnabled = editingTodo?.voiceEnabled ?: uiState.settings.defaultVoiceEnabled,
            onDismiss = {
                editorVisible = false
                editingTodo = null
            },
            onConfirm = { title, notes, dueAt, reminderAt, category, ring, vibrate, voice ->
                scope.launch {
                    val current = editingTodo
                    val error = if (current == null) {
                        onAddTodo(title, notes, dueAt, reminderAt, category, ring, vibrate, voice)
                    } else {
                        onUpdateTodo(current, title, notes, dueAt, reminderAt, category, ring, vibrate, voice)
                    }
                    if (error == null) {
                        editorVisible = false
                        editingTodo = null
                        snackbarHostState.showSnackbar(if (current == null) "任务已创建" else "任务已更新")
                    } else {
                        snackbarHostState.showSnackbar(error)
                    }
                }
            }
        )
    }
}

internal fun dashboardPadding() = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
