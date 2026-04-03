package com.example.todoalarm.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.ui.theme.TodoAlarmTheme

data class PermissionSnapshot(
    val notificationGranted: Boolean = true,
    val exactAlarmGranted: Boolean = true,
    val fullScreenGranted: Boolean = true,
    val dndAccessGranted: Boolean = true,
    val batteryOptimizationIgnored: Boolean = false
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
                    onAddTodo = viewModel::addTodo,
                    onUpdateTodo = viewModel::updateTodo,
                    onCompleteTodo = viewModel::completeTodo,
                    onRestoreTodo = viewModel::restoreTodo,
                    onThemeModeChange = viewModel::updateThemeMode,
                    onDefaultSnoozeChange = viewModel::updateDefaultSnooze,
                    onReminderDefaultsChange = viewModel::updateReminderDefaults
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
}
