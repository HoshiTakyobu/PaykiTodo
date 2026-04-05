package com.example.todoalarm.ui

import android.graphics.Paint
import android.widget.NumberPicker
import android.widget.EditText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun SettingsPanel(
    permissions: PermissionSnapshot,
    defaultSnooze: Int,
    crashLog: String?,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestFullScreenPermission: () -> Unit,
    onRequestNotificationPolicyAccess: () -> Unit,
    onRequestIgnoreBatteryOptimization: () -> Unit,
    onRequestAccessibilityService: () -> Unit,
    onDefaultSnoozeChange: (Int) -> Unit,
    onCopyCrashLog: () -> Unit,
    onClearCrashLog: () -> Unit
) {
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showCrashDialog by remember(crashLog) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PrefCard("提醒权限") {
            PermissionRow(
                icon = Icons.Rounded.Notifications,
                label = "通知权限",
                granted = permissions.notificationGranted,
                onClick = onRequestNotificationPermission
            )
            PermissionRow(
                icon = Icons.Rounded.Alarm,
                label = "精确闹钟",
                granted = permissions.exactAlarmGranted,
                onClick = onRequestExactAlarmPermission
            )
            PermissionRow(
                icon = Icons.Rounded.TaskAlt,
                label = "全屏提醒",
                granted = permissions.fullScreenGranted,
                onClick = onRequestFullScreenPermission
            )
            PermissionRow(
                icon = Icons.Rounded.Security,
                label = "免打扰穿透",
                granted = permissions.dndAccessGranted,
                onClick = onRequestNotificationPolicyAccess
            )
            PermissionRow(
                icon = Icons.Rounded.Alarm,
                label = "忽略电池优化",
                granted = permissions.batteryOptimizationIgnored,
                onClick = onRequestIgnoreBatteryOptimization
            )
            PermissionRow(
                icon = Icons.Rounded.Security,
                label = "辅助功能提醒",
                granted = permissions.accessibilityServiceEnabled,
                onClick = onRequestAccessibilityService
            )
        }

        PrefCard("默认延后时长") {
            Text(
                text = "点击按钮后再选择默认延后时长，避免上下滑动页面时误触。",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = { showSnoozeDialog = true }) {
                Text("当前：${normalizeSnooze(defaultSnooze)} 分钟")
            }
        }

        PrefCard("崩溃日志") {
            Text(
                text = if (crashLog.isNullOrBlank()) {
                    "当前没有记录到新的异常退出日志。"
                } else {
                    "已记录最近一次异常退出日志，可直接查看并复制给我。"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { showCrashDialog = true },
                    enabled = !crashLog.isNullOrBlank()
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null)
                    Text("查看日志")
                }
                OutlinedButton(
                    onClick = onClearCrashLog,
                    enabled = !crashLog.isNullOrBlank()
                ) {
                    Text("清空日志")
                }
            }
        }
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
}

@Composable
private fun SnoozePickerDialog(
    defaultSnooze: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val values = (5..60 step 5).toList()
    var selectedMinutes by remember(defaultSnooze) { mutableIntStateOf(normalizeSnooze(defaultSnooze)) }
    val pickerTextColor = MaterialTheme.colorScheme.onSurface.toArgb()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择默认延后时长") },
        text = {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    NumberPicker(context).apply {
                        minValue = 0
                        maxValue = values.lastIndex
                        displayedValues = values.map { "$it 分钟" }.toTypedArray()
                        wrapSelectorWheel = false
                        descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                        value = values.indexOf(selectedMinutes)
                        applyTextColor(pickerTextColor)
                        setOnValueChangedListener { _, _, newVal ->
                            selectedMinutes = values[newVal]
                        }
                    }
                },
                update = { picker ->
                    val targetIndex = values.indexOf(selectedMinutes)
                    if (picker.value != targetIndex) {
                        picker.value = targetIndex
                    }
                    picker.applyTextColor(pickerTextColor)
                }
            )
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

private fun NumberPicker.applyTextColor(color: Int) {
    for (index in 0 until childCount) {
        (getChildAt(index) as? EditText)?.setTextColor(color)
    }
    runCatching {
        val field = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
        field.isAccessible = true
        (field.get(this) as? Paint)?.color = color
    }
    invalidate()
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
