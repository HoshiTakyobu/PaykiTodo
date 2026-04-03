package com.example.todoalarm.ui

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.todoalarm.data.ThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPanel(
    permissions: PermissionSnapshot,
    selectedThemeMode: ThemeMode,
    defaultSnooze: Int,
    ringEnabled: Boolean,
    vibrateEnabled: Boolean,
    voiceEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestFullScreenPermission: () -> Unit,
    onRequestNotificationPolicyAccess: () -> Unit,
    onRequestIgnoreBatteryOptimization: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDefaultSnoozeChange: (Int) -> Unit,
    onReminderDefaultsChange: (Boolean, Boolean, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PrefCard("提醒权限") {
            PermissionRow(Icons.Rounded.Notifications, "通知权限", permissions.notificationGranted, "去开启", onRequestNotificationPermission)
            PermissionRow(Icons.Rounded.Alarm, "精确闹钟", permissions.exactAlarmGranted, "去设置", onRequestExactAlarmPermission)
            PermissionRow(Icons.Rounded.TaskAlt, "全屏提醒", permissions.fullScreenGranted, "去设置", onRequestFullScreenPermission)
            PermissionRow(Icons.Rounded.Security, "免打扰穿透", permissions.dndAccessGranted, "去设置", onRequestNotificationPolicyAccess)
            PermissionRow(Icons.Rounded.Alarm, "忽略电池优化", permissions.batteryOptimizationIgnored, "去设置", onRequestIgnoreBatteryOptimization)
        }
        PrefCard("显示模式") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedThemeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text(mode.label) },
                        leadingIcon = { Icon(Icons.Rounded.DarkMode, contentDescription = null) }
                    )
                }
            }
        }
        PrefCard("默认延后时长") {
            Text("以 5 分钟为间隔选择 5-60 分钟。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            SnoozePicker(defaultSnooze = defaultSnooze, onValueChange = onDefaultSnoozeChange)
        }
        PrefCard("默认提醒方式") {
            SwitchRow("响铃", ringEnabled) { onReminderDefaultsChange(it, vibrateEnabled, voiceEnabled) }
            SwitchRow("震动", vibrateEnabled) { onReminderDefaultsChange(ringEnabled, it, voiceEnabled) }
            SwitchRow("语音播报", voiceEnabled) { onReminderDefaultsChange(ringEnabled, vibrateEnabled, it) }
        }
    }
}

@Composable
private fun SnoozePicker(
    defaultSnooze: Int,
    onValueChange: (Int) -> Unit
) {
    val values = (5..60 step 5).toList()
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            NumberPicker(context).apply {
                minValue = 0
                maxValue = values.lastIndex
                displayedValues = values.map { "$it 分钟" }.toTypedArray()
                wrapSelectorWheel = false
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                value = values.indexOf(normalizeSnooze(defaultSnooze))
                setOnValueChangedListener { _, _, newVal ->
                    onValueChange(values[newVal])
                }
            }
        },
        update = { picker ->
            val targetIndex = values.indexOf(normalizeSnooze(defaultSnooze))
            if (picker.value != targetIndex) picker.value = targetIndex
        }
    )
}

private fun normalizeSnooze(minutes: Int): Int {
    val clamped = minutes.coerceIn(5, 60)
    return if (clamped % 5 == 0) clamped else clamped - (clamped % 5)
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
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = if (granted) "当前状态：已开启" else "当前状态：未开启",
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            )
        }
        OutlinedButton(onClick = onClick, enabled = !granted) {
            Text(if (granted) "已完成" else actionLabel)
        }
    }
}
