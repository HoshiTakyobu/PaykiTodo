package com.example.todoalarm.ui

import android.Manifest
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.PowerManager
import android.media.RingtoneManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todoalarm.CrashLogger
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.accessibility.ReminderAccessibilityService
import com.example.todoalarm.alarm.ActiveReminderStore
import com.example.todoalarm.alarm.AlarmScheduler
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import kotlinx.coroutines.launch

data class PermissionSnapshot(
    val notificationGranted: Boolean = true,
    val exactAlarmGranted: Boolean = true,
    val fullScreenGranted: Boolean = true,
    val dndAccessGranted: Boolean = true,
    val batteryOptimizationIgnored: Boolean = false,
    val accessibilityServiceEnabled: Boolean = false,
    val lastCrashLog: String? = null,
    val copyCrashLog: () -> Unit = {},
    val clearCrashLog: () -> Unit = {}
)

class MainActivity : ComponentActivity() {
    private val viewModel: TodoViewModel by viewModels()
    private var permissions by mutableStateOf(PermissionSnapshot())
    private var lastCrashLog by mutableStateOf<String?>(null)
    private var lastReminderRoutingTodoId: Long = -1L
    private var lastReminderRoutingAt: Long = 0L

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshPermissions()
    }

    private val backupDirectoryLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.takePersistableUriPermission(
            uri,
            FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION
        )
        viewModel.updateBackupDirectoryUri(uri.toString())
        Toast.makeText(this, "备份目录已设置", Toast.LENGTH_SHORT).show()
    }

    private val exportBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            val message = viewModel.exportBackupNow(uri) ?: "导出失败"
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            val message = viewModel.importBackup(uri) ?: "导入失败"
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.parcelableExtraCompat(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        if (uri != null) {
            val ringtone = RingtoneManager.getRingtone(this, uri)
            val title = ringtone?.getTitle(this)
            viewModel.updateReminderTone(uri.toString(), title)
            Toast.makeText(this, "提醒提示音已更新", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lastCrashLog = CrashLogger.readLastCrash(this)
        refreshPermissions()

        if (!CrashLogger.readPendingCrash(this).isNullOrBlank()) {
            CrashLogger.markCrashNoticeShown(this)
            Toast.makeText(this, "检测到上次异常退出，可在设置页查看崩溃日志。", Toast.LENGTH_LONG).show()
        }
        maybeRouteToReminder()

        setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            TodoAlarmTheme(themeMode = uiState.settings.themeMode) {
                DashboardScreen(
                    uiState = uiState,
                    permissions = permissions,
                    onRequestNotificationPermission = ::requestNotificationPermission,
                    onRequestExactAlarmPermission = ::openExactAlarmSettings,
                    onRequestFullScreenPermission = ::openFullScreenSettings,
                    onRequestNotificationPolicyAccess = ::openNotificationPolicySettings,
                    onRequestIgnoreBatteryOptimization = ::requestIgnoreBatteryOptimization,
                    onRequestAccessibilityService = ::openAccessibilitySettings,
                    onAddTodo = viewModel::addTodo,
                    onAddCalendarEvent = viewModel::addCalendarEvent,
                    onImportCalendarEvents = viewModel::importCalendarEvents,
                    onUpdateTodo = viewModel::updateTodo,
                    onUpdateCalendarEvent = viewModel::updateCalendarEvent,
                    onDeleteTodo = viewModel::deleteTodo,
                    onDeleteCalendarEvent = viewModel::deleteCalendarEvent,
                    onCompleteTodo = viewModel::completeTodo,
                    onRestoreTodo = viewModel::restoreTodo,
                    onCancelTodo = viewModel::cancelTodo,
                    onSelectGroup = viewModel::selectGroup,
                    onCreateGroup = viewModel::createGroup,
                    onUpdateGroup = viewModel::updateGroup,
                    onDeleteGroup = viewModel::deleteGroup,
                    onThemeModeChange = viewModel::updateThemeMode,
                    onWeekStartModeChange = viewModel::updateWeekStartMode,
                    onNextQuote = viewModel::showNextQuote,
                    onDefaultSnoozeChange = viewModel::updateDefaultSnooze,
                    onDefaultCalendarReminderModeChange = viewModel::updateDefaultCalendarReminderMode,
                    onDesktopSyncEnabledChange = viewModel::updateDesktopSyncEnabled,
                    onRotateDesktopSyncToken = viewModel::rotateDesktopSyncToken,
                    onUseBuiltInReminderTone = {
                        viewModel.useBuiltInReminderTone()
                        Toast.makeText(this, "已切换为内置提醒音", Toast.LENGTH_SHORT).show()
                    },
                    onPickSystemReminderTone = ::openReminderTonePicker,
                    onOpenWiki = ::openInAppWiki,
                    onRunReminderChainTest = { seconds -> viewModel.runReminderChainTest(seconds) },
                    onClearReminderDiagnostics = { viewModel.clearReminderDiagnostics() },
                    onSaveWeekAsScheduleTemplate = { name, type, weekStart ->
                        viewModel.saveWeekAsScheduleTemplate(name, type, weekStart)
                    },
                    onApplyScheduleTemplateToWeek = { template, weekStart ->
                        viewModel.applyScheduleTemplateToWeek(template, weekStart)
                    },
                    onGenerateSemesterScheduleFromTemplate = { template, firstWeekStart, endDate ->
                        viewModel.generateSemesterScheduleFromTemplate(template, firstWeekStart, endDate)
                    },
                    onDeleteScheduleTemplate = { templateId ->
                        viewModel.deleteScheduleTemplate(templateId)
                    },
                    onPickBackupDirectory = { backupDirectoryLauncher.launch(null) },
                    onExportBackup = { exportBackupLauncher.launch("PaykiTodo-backup.json") },
                    onImportBackup = { importBackupLauncher.launch(arrayOf("application/json")) },
                    onAutoBackupChange = viewModel::updateAutoBackupEnabled
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        CrashLogger.markLaunchSuccessful(this)
        refreshPermissions()
        viewModel.refreshTaskStates()
        maybeRouteToReminder()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        maybeRouteToReminder()
    }

    private fun refreshPermissions() {
        val app = application as TodoApplication
        val notificationManager = getSystemService(NotificationManager::class.java)
        val powerManager = getSystemService(PowerManager::class.java)

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
            },
            dndAccessGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.isNotificationPolicyAccessGranted
            } else {
                true
            },
            batteryOptimizationIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager.isIgnoringBatteryOptimizations(packageName)
            } else {
                true
            },
            accessibilityServiceEnabled = isReminderAccessibilityEnabled(),
            lastCrashLog = lastCrashLog,
            copyCrashLog = ::copyCrashLog,
            clearCrashLog = ::clearCrashLog
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

    private fun openNotificationPolicySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            val listIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            val detailIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            val pm = packageManager
            when {
                requestIntent.resolveActivity(pm) != null -> startActivity(requestIntent)
                listIntent.resolveActivity(pm) != null -> startActivity(listIntent)
                else -> startActivity(detailIntent)
            }
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openReminderTonePicker() {
        val existingUri = viewModel.uiState.value.settings.reminderToneUri?.let(Uri::parse)
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "选择通知提示音")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
        }
        ringtonePickerLauncher.launch(intent)
    }

    private fun openInAppWiki() {
        startActivity(Intent(this, WikiActivity::class.java))
    }

    private fun copyCrashLog() {
        val crashLog = lastCrashLog ?: return
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("PaykiTodoCrashLog", crashLog))
        Toast.makeText(this, "崩溃日志已复制。", Toast.LENGTH_SHORT).show()
    }

    private fun clearCrashLog() {
        CrashLogger.clearLastCrash(this)
        lastCrashLog = null
        refreshPermissions()
        Toast.makeText(this, "崩溃日志已清空。", Toast.LENGTH_SHORT).show()
    }

    private fun isReminderAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        if (enabledServices.isBlank()) return false

        val expected = ComponentName(this, ReminderAccessibilityService::class.java).flattenToString()
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun maybeRouteToReminder() {
        val explicitTodoId = intent?.getLongExtra(AlarmScheduler.EXTRA_TODO_ID, -1L) ?: -1L
        val activeTodoId = ActiveReminderStore.getActiveTodoId(this)
        val targetTodoId = when {
            explicitTodoId > 0L -> explicitTodoId
            activeTodoId > 0L -> activeTodoId
            else -> -1L
        }
        if (targetTodoId <= 0L) {
            lastReminderRoutingTodoId = -1L
            return
        }

        val now = System.currentTimeMillis()
        if (lastReminderRoutingTodoId == targetTodoId && now - lastReminderRoutingAt < 1_500L) {
            return
        }
        lastReminderRoutingTodoId = targetTodoId
        lastReminderRoutingAt = now
        if (explicitTodoId > 0L) {
            setIntent(Intent(intent).apply { removeExtra(AlarmScheduler.EXTRA_TODO_ID) })
        }
        startActivity(ReminderActivity.createIntent(this, targetTodoId))
    }

    private fun <T : Parcelable> Intent.parcelableExtraCompat(key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, clazz)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key)
        }
    }
}
