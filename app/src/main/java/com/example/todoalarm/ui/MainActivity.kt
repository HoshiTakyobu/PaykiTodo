package com.example.todoalarm.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PermissionSnapshot(
    val notificationGranted: Boolean = true,
    val exactAlarmGranted: Boolean = true,
    val fullScreenGranted: Boolean = true
)

class MainActivity : ComponentActivity() {
    private val viewModel: TodoViewModel by viewModels()
    private var permissions by mutableStateOf(PermissionSnapshot())

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshPermissions()

        setContent {
            TodoAlarmTheme {
                MainScreen(
                    viewModel = viewModel,
                    permissions = permissions,
                    onRequestNotificationPermission = ::requestNotificationPermission,
                    onRequestExactAlarmPermission = ::openExactAlarmSettings,
                    onRequestFullScreenPermission = ::openFullScreenSettings
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun refreshPermissions() {
        val app = application as TodoApplication
        val notificationManager = getSystemService(NotificationManager::class.java)

        permissions = PermissionSnapshot(
            notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            },
            exactAlarmGranted = app.alarmScheduler.canScheduleExactAlarms(),
            fullScreenGranted = if (Build.VERSION.SDK_INT >= 34) {
                notificationManager.canUseFullScreenIntent()
            } else {
                true
            }
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }
    }

    private fun openFullScreenSettings() {
        if (Build.VERSION.SDK_INT >= 34) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
            )
        }
    }
}

@Composable
private fun MainScreen(
    viewModel: TodoViewModel,
    permissions: PermissionSnapshot,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestFullScreenPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "新建待办")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TodayHeader(todayCount = uiState.todayItems.size, overdueCount = uiState.overdueCount)
            }
            item {
                PermissionPanel(
                    permissions = permissions,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                    onRequestFullScreenPermission = onRequestFullScreenPermission
                )
            }
            item {
                SectionTitle(title = "今日待办")
            }
            if (uiState.todayItems.isEmpty()) {
                item {
                    EmptyStateCard(text = "今天没有待办。点右下角新增一个。")
                }
            } else {
                items(uiState.todayItems, key = { it.id }) { item ->
                    TodoCard(item = item, onComplete = { viewModel.completeTodo(item) })
                }
            }
            item {
                SectionTitle(title = "其他待办")
            }
            if (uiState.upcomingItems.isEmpty()) {
                item {
                    EmptyStateCard(text = "没有其他未完成待办。")
                }
            } else {
                items(uiState.upcomingItems, key = { it.id }) { item ->
                    TodoCard(item = item, onComplete = { viewModel.completeTodo(item) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddTodoDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, notes, dueDate, reminderAt, ringEnabled, vibrateEnabled, voiceEnabled ->
                scope.launch {
                    val error = viewModel.addTodo(
                        title = title,
                        notes = notes,
                        dueDate = dueDate,
                        reminderAt = reminderAt,
                        ringEnabled = ringEnabled,
                        vibrateEnabled = vibrateEnabled,
                        voiceEnabled = voiceEnabled
                    )
                    if (error == null) {
                        showAddDialog = false
                        snackbarHostState.showSnackbar("待办已保存")
                    } else {
                        snackbarHostState.showSnackbar(error)
                    }
                }
            }
        )
    }

    LaunchedEffect(
        permissions.notificationGranted,
        permissions.exactAlarmGranted,
        permissions.fullScreenGranted
    ) {
        if (!permissions.notificationGranted || !permissions.exactAlarmGranted || !permissions.fullScreenGranted) {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
}

@Composable
private fun TodayHeader(todayCount: Int, overdueCount: Int) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "今日待办",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "今天有 $todayCount 项，逾期 $overdueCount 项",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "提醒会在你设置的时间以全屏方式弹出，且只能延后关闭。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionPanel(
    permissions: PermissionSnapshot,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestFullScreenPermission: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "权限检查",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            PermissionRow(
                icon = { Icon(Icons.Rounded.Notifications, contentDescription = null) },
                label = "通知权限",
                granted = permissions.notificationGranted,
                actionLabel = "开启",
                onClick = onRequestNotificationPermission
            )
            PermissionRow(
                icon = { Icon(Icons.Rounded.Alarm, contentDescription = null) },
                label = "精确闹钟",
                granted = permissions.exactAlarmGranted,
                actionLabel = "设置",
                onClick = onRequestExactAlarmPermission
            )
            PermissionRow(
                icon = { Icon(Icons.Rounded.TaskAlt, contentDescription = null) },
                label = "全屏提醒",
                granted = permissions.fullScreenGranted,
                actionLabel = "设置",
                onClick = onRequestFullScreenPermission
            )
        }
    }
}

@Composable
private fun PermissionRow(
    icon: @Composable () -> Unit,
    label: String,
    granted: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        icon()
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (granted) "已开启" else "未开启",
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
        if (!granted) {
            OutlinedButton(onClick = onClick) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodoCard(
    item: TodoItem,
    onComplete: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Checkbox(
                    checked = false,
                    onCheckedChange = { onComplete() }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "DDL: ${formatLocalDate(LocalDate.ofEpochDay(item.dueDateEpochDay))}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (item.reminderAtMillis != null) {
                            "提醒: ${formatLocalDateTime(reminderAtMillisToDateTime(item.reminderAtMillis))}"
                        } else {
                            "提醒: 未设置"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (item.notes.isNotBlank()) {
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.ringEnabled) {
                    AssistChip(onClick = {}, label = { Text("铃声") })
                }
                if (item.vibrateEnabled) {
                    AssistChip(onClick = {}, label = { Text("震动") })
                }
                if (item.voiceEnabled) {
                    AssistChip(onClick = {}, label = { Text("语音") })
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(text: String) {
    Card {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddTodoDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        notes: String,
        dueDate: LocalDate,
        reminderAt: LocalDateTime?,
        ringEnabled: Boolean,
        vibrateEnabled: Boolean,
        voiceEnabled: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(LocalDate.now()) }
    var reminderEnabled by remember { mutableStateOf(true) }
    var reminderAt by remember {
        mutableStateOf(
            LocalDateTime.now()
                .plusMinutes(10)
                .withSecond(0)
                .withNano(0)
        )
    }
    var ringEnabled by remember { mutableStateOf(true) }
    var vibrateEnabled by remember { mutableStateOf(true) }
    var voiceEnabled by remember { mutableStateOf(false) }

    fun openDueDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                dueDate = LocalDate.of(year, month + 1, day)
            },
            dueDate.year,
            dueDate.monthValue - 1,
            dueDate.dayOfMonth
        ).show()
    }

    fun openReminderPicker() {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val pickedDate = LocalDate.of(year, month + 1, day)
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        reminderAt = LocalDateTime.of(
                            pickedDate.year,
                            pickedDate.monthValue,
                            pickedDate.dayOfMonth,
                            hour,
                            minute
                        )
                    },
                    reminderAt.hour,
                    reminderAt.minute,
                    true
                ).show()
            },
            reminderAt.year,
            reminderAt.monthValue - 1,
            reminderAt.dayOfMonth
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建待办") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = ::openDueDatePicker,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("DDL: ${formatLocalDate(dueDate)}")
                }
                OutlinedButton(
                    onClick = { reminderEnabled = !reminderEnabled },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (reminderEnabled) "提醒已开启" else "提醒已关闭")
                }
                if (reminderEnabled) {
                    OutlinedButton(
                        onClick = ::openReminderPicker,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("提醒时间: ${formatLocalDateTime(reminderAt)}")
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = ringEnabled,
                        onClick = { ringEnabled = !ringEnabled },
                        label = { Text("铃声") }
                    )
                    FilterChip(
                        selected = vibrateEnabled,
                        onClick = { vibrateEnabled = !vibrateEnabled },
                        label = { Text("震动") }
                    )
                    FilterChip(
                        selected = voiceEnabled,
                        onClick = { voiceEnabled = !voiceEnabled },
                        label = { Text("语音") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title,
                        notes,
                        dueDate,
                        if (reminderEnabled) reminderAt else null,
                        ringEnabled,
                        vibrateEnabled,
                        voiceEnabled
                    )
                }
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

fun reminderAtMillisToDateTime(reminderAtMillis: Long): LocalDateTime {
    return Instant.ofEpochMilli(reminderAtMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
}

fun formatLocalDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA))
}

fun formatLocalDateTime(dateTime: LocalDateTime): String {
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA))
}
