package com.example.todoalarm.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.FocusSession
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FocusActivity : ComponentActivity() {
    private val app by lazy { application as TodoApplication }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = app.settingsStore.currentSettings()
        if (settings.focusKeepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        val title = intent.getStringExtra(EXTRA_FOCUS_TITLE)?.trim().orEmpty().ifBlank { "自由专注" }
        val minutes = intent.getIntExtra(EXTRA_FOCUS_MINUTES, settings.focusDefaultMinutes).coerceIn(1, 180)
        val todoId = intent.getLongExtra(EXTRA_FOCUS_TODO_ID, -1L).takeIf { it > 0L }

        setContent {
            val currentSettings = app.settingsStore.currentSettings()
            TodoAlarmTheme(themeMode = currentSettings.themeMode) {
                FocusScreen(
                    title = title,
                    todoId = todoId,
                    plannedMinutes = minutes,
                    extensionMinutes = currentSettings.focusExtensionMinutes,
                    blockNotifications = currentSettings.focusBlockNotifications,
                    onVibrate = ::vibrateFocusDone,
                    onRecord = { session -> app.repository.saveFocusSession(session) },
                    onClose = { finish() }
                )
            }
        }
    }

    private fun vibrateFocusDone() {
        val pattern = longArrayOf(0L, 200L, 200L, 400L)
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    companion object {
        const val EXTRA_FOCUS_TODO_ID = "extra_focus_todo_id"
        const val EXTRA_FOCUS_TITLE = "extra_focus_title"
        const val EXTRA_FOCUS_MINUTES = "extra_focus_minutes"

        fun createIntent(context: Context, todoId: Long?, title: String, minutes: Int): Intent {
            return Intent(context, FocusActivity::class.java).apply {
                todoId?.takeIf { it > 0L }?.let { putExtra(EXTRA_FOCUS_TODO_ID, it) }
                putExtra(EXTRA_FOCUS_TITLE, title)
                putExtra(EXTRA_FOCUS_MINUTES, minutes)
            }
        }
    }
}

@Composable
private fun FocusScreen(
    title: String,
    todoId: Long?,
    plannedMinutes: Int,
    extensionMinutes: Int,
    blockNotifications: Boolean,
    onVibrate: () -> Unit,
    onRecord: suspend (FocusSession) -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val startedAtMillis = remember { System.currentTimeMillis() }
    var remainingSeconds by remember(plannedMinutes) { mutableIntStateOf(plannedMinutes.coerceAtLeast(1) * 60) }
    var actualSeconds by remember { mutableIntStateOf(0) }
    var extensionCount by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var showFinishConfirm by remember { mutableStateOf(false) }
    var showAbandonConfirm by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf(false) }
    var recorded by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var recordError by remember { mutableStateOf<String?>(null) }

    fun actualMinutes(): Int = actualSeconds / 60

    fun record(completed: Boolean, afterRecorded: () -> Unit) {
        if (recorded) return
        recorded = true
        recording = true
        val session = FocusSession(
            todoId = todoId,
            title = title,
            plannedMinutes = plannedMinutes,
            actualMinutes = actualMinutes(),
            startedAtMillis = startedAtMillis,
            endedAtMillis = System.currentTimeMillis(),
            completed = completed,
            extensionCount = extensionCount
        )
        scope.launch {
            val result = runCatching { onRecord(session) }
            recording = false
            if (result.isSuccess) {
                afterRecorded()
            } else {
                recorded = false
                recordError = "专注记录保存失败，请重试。"
            }
        }
    }

    fun abandonAfterRecord() {
        showCompletionDialog = false
        showAbandonConfirm = false
        record(completed = false) {
            onClose()
        }
    }

    fun completeAfterRecord() {
        showCompletionDialog = false
        showFinishConfirm = false
        record(completed = true) {
            success = true
        }
    }

    LaunchedEffect(paused, showCompletionDialog, success, recording) {
        while (!paused && !showCompletionDialog && !success && !recording && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
            actualSeconds++
        }
        if (!paused && !showCompletionDialog && !success && !recording && remainingSeconds <= 0) {
            showCompletionDialog = true
            onVibrate()
        }
    }

    LaunchedEffect(success) {
        if (success) {
            delay(1500L)
            onClose()
        }
    }

    BackHandler(enabled = !success && !recording) {
        showAbandonConfirm = true
    }

    recordError?.let { message ->
        AlertDialog(
            onDismissRequest = { recordError = null },
            title = { Text("保存失败") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { recordError = null }) { Text("知道了") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (recording) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("正在保存专注记录") },
            text = { Text("请稍候，正在写入本次专注结果。") },
            confirmButton = {},
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (success) {
        FocusSuccessScreen(actualMinutes = actualMinutes(), extensionCount = extensionCount)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 22.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (blockNotifications) {
                Text(
                    text = "已按设置进入专注模式提醒压制偏好",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }

        FocusCountdownRing(
            remainingSeconds = remainingSeconds,
            actualSeconds = actualSeconds,
            extensionCount = extensionCount
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { paused = !paused },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (paused) "继续" else "暂停")
            }
            Button(
                onClick = { showFinishConfirm = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("完成")
            }
            OutlinedButton(
                onClick = { showAbandonConfirm = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("放弃")
            }
        }
    }

    if (showCompletionDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("专注完成") },
            text = { Text("已专注 ${actualMinutes()} 分钟。要完成、延续 ${extensionMinutes} 分钟，还是放弃？") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        abandonAfterRecord()
                    }) { Text("放弃", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = {
                        remainingSeconds += extensionMinutes.coerceIn(1, 30) * 60
                        extensionCount++
                        showCompletionDialog = false
                        paused = false
                    }) { Text("延续 ${extensionMinutes.coerceIn(1, 30)} 分钟") }
                    TextButton(onClick = {
                        completeAfterRecord()
                    }) { Text("完成") }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            title = { Text("提前结束？") },
            text = { Text("当前已专注 ${actualMinutes()} 分钟。确认完成本次专注吗？") },
            confirmButton = {
                TextButton(onClick = {
                    completeAfterRecord()
                }) { Text("完成") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirm = false }) { Text("继续专注") }
            }
        )
    }

    if (showAbandonConfirm) {
        AlertDialog(
            onDismissRequest = { showAbandonConfirm = false },
            title = { Text("确认放弃当前专注？") },
            text = { Text("已专注 ${actualMinutes()} 分钟，本次会记录为放弃。") },
            confirmButton = {
                TextButton(onClick = {
                    abandonAfterRecord()
                }) { Text("放弃", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun FocusCountdownRing(
    remainingSeconds: Int,
    actualSeconds: Int,
    extensionCount: Int
) {
    val totalSeconds = (actualSeconds + remainingSeconds).coerceAtLeast(1)
    val progress = (actualSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(278.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = size.copy(width = size.width - strokeWidth, height = size.height - strokeWidth)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = primary,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = formatFocusTime(remainingSeconds),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 72.sp,
                lineHeight = 78.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "已专注 ${actualSeconds / 60} 分钟 / 延续 ${extensionCount} 次",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun FocusSuccessScreen(actualMinutes: Int, extensionCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(96.dp)
                    .padding(22.dp)
            )
        }
        Text(
            text = "专注完成",
            modifier = Modifier.padding(top = 20.dp),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "已专注 ${actualMinutes} 分钟 · 延续 ${extensionCount} 次",
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun formatFocusTime(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val minutes = safe / 60
    val remain = safe % 60
    return "%02d:%02d".format(minutes, remain)
}
