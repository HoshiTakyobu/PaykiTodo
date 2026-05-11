package com.example.todoalarm.ui

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.alarm.ActiveReminderStore
import com.example.todoalarm.alarm.AlarmScheduler
import com.example.todoalarm.alarm.ReminderChainLogger
import com.example.todoalarm.alarm.ReminderForegroundService
import com.example.todoalarm.alarm.ReminderNotifier
import com.example.todoalarm.data.ReminderChainStage
import com.example.todoalarm.data.ReminderChainStatus
import com.example.todoalarm.data.RecurrenceScope
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderActivity : ComponentActivity() {
    private val app by lazy { application as TodoApplication }
    private var todoItem by mutableStateOf<TodoItem?>(null)
    private var taskGroup by mutableStateOf<ResolvedTaskGroup?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        loadTodo()

        setContent {
            val settings = app.settingsStore.settingsFlow.collectAsStateWithLifecycle().value
            TodoAlarmTheme(themeMode = settings.themeMode) {
                ReminderScreen(
                    todoItem = todoItem,
                    taskGroup = taskGroup,
                    defaultSnoozeMinutes = settings.defaultSnoozeMinutes,
                    onComplete = ::completeTodo,
                    onAcknowledge = ::acknowledgeEvent,
                    onSnooze = ::snooze,
                    onCancel = ::cancelTodo
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadTodo()
    }

    override fun onResume() {
        super.onResume()
        todoItem?.let {
            ActiveReminderStore.refreshActive(this, it.id)
            ReminderChainLogger.log(
                context = this,
                todoId = it.id,
                source = "ReminderActivity",
                stage = ReminderChainStage.REMINDER_ACTIVITY_RESUME,
                status = ReminderChainStatus.OK,
                reminderAtMillis = it.reminderAtMillis
            )
        }
    }

    private fun loadTodo() {
        val todoId = intent.getLongExtra(AlarmScheduler.EXTRA_TODO_ID, -1L)
        if (todoId <= 0L) {
            finish()
            return
        }
        lifecycleScope.launch {
            val item = app.repository.getTodo(todoId)
            if (item == null || item.isHistory) {
                ActiveReminderStore.clearIfMatches(this@ReminderActivity, todoId)
                ActiveReminderStore.clearActivityHandoff(this@ReminderActivity, todoId)
                finish()
                return@launch
            }

            ActiveReminderStore.clearActivityHandoff(this@ReminderActivity, todoId)
            ActiveReminderStore.refreshActive(this@ReminderActivity, todoId)
            todoItem = item
            taskGroup = resolveTaskGroup(item, app.repository.getGroup(item.groupId))
        }
    }

    private fun completeTodo() {
        val item = todoItem ?: return
        lifecycleScope.launch {
            ReminderChainLogger.log(
                context = this@ReminderActivity,
                todoId = item.id,
                source = "ReminderActivity",
                stage = ReminderChainStage.USER_COMPLETE,
                status = ReminderChainStatus.OK
            )
            app.repository.setCompleted(item.id, true)
            clearReminderArtifacts(listOf(item))
            closeReminder(item.id)
        }
    }

    private fun acknowledgeEvent() {
        val item = todoItem ?: return
        lifecycleScope.launch {
            ReminderChainLogger.log(
                context = this@ReminderActivity,
                todoId = item.id,
                source = "ReminderActivity",
                stage = ReminderChainStage.USER_COMPLETE,
                status = ReminderChainStatus.OK,
                message = "event_ack"
            )
            app.repository.acknowledgeCalendarEvent(item.id)
            clearReminderArtifacts(listOf(item))
            closeReminder(item.id)
        }
    }

    private fun snooze(minutes: Int) {
        val item = todoItem ?: return
        lifecycleScope.launch {
            ReminderChainLogger.log(
                context = this@ReminderActivity,
                todoId = item.id,
                source = "ReminderActivity",
                stage = ReminderChainStage.USER_SNOOZE,
                status = ReminderChainStatus.OK,
                message = "minutes=$minutes"
            )
            clearReminderArtifacts(listOf(item))
            val updated = app.repository.snoozeTodo(item.id, nextMinuteAlignedReminder(minutes))
            if (updated != null) {
                app.alarmScheduler.schedule(updated)
            }
            closeReminder(item.id)
        }
    }

    private fun cancelTodo(scope: RecurrenceScope) {
        val item = todoItem ?: return
        lifecycleScope.launch {
            ReminderChainLogger.log(
                context = this@ReminderActivity,
                todoId = item.id,
                source = "ReminderActivity",
                stage = ReminderChainStage.USER_CANCEL,
                status = ReminderChainStatus.OK,
                message = scope.name
            )
            val affected = app.repository.cancelTodo(item, scope)
            clearReminderArtifacts(affected.ifEmpty { listOf(item) })
            closeReminder(item.id)
        }
    }

    private fun clearReminderArtifacts(items: List<TodoItem>) {
        items.forEach { item ->
            app.alarmScheduler.cancel(item.id)
            app.reminderNotifier.cancel(item.id)
            ActiveReminderStore.clearIfMatches(this, item.id)
            ActiveReminderStore.clearActivityHandoff(this, item.id)
        }
    }

    private fun closeReminder(todoId: Long) {
        stopService(Intent(this, ReminderForegroundService::class.java))
        getSystemService(NotificationManager::class.java).cancel(ReminderNotifier.notificationId(todoId))
        finish()
    }

    private fun nextMinuteAlignedReminder(minutes: Int): Long {
        val zoneId = ZoneId.systemDefault()
        val nextReminder = LocalDateTime.now(zoneId)
            .withSecond(0)
            .withNano(0)
            .plusMinutes(minutes.toLong())
        return nextReminder.atZone(zoneId).toInstant().toEpochMilli()
    }

    companion object {
        fun createIntent(context: Context, todoId: Long): Intent {
            return Intent(context, ReminderActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                putExtra(AlarmScheduler.EXTRA_TODO_ID, todoId)
            }
        }
    }
}

@Composable
private fun ReminderScreen(
    todoItem: TodoItem?,
    taskGroup: ResolvedTaskGroup?,
    defaultSnoozeMinutes: Int,
    onComplete: () -> Unit,
    onAcknowledge: () -> Unit,
    onSnooze: (Int) -> Unit,
    onCancel: (RecurrenceScope) -> Unit
) {
    var customMinutes by remember(defaultSnoozeMinutes) { mutableStateOf(defaultSnoozeMinutes.toString()) }
    val customSnoozeValidation = remember(customMinutes) { parseSnoozeInput(customMinutes) }
    var showCancelScopeDialog by remember(todoItem?.id) { mutableStateOf(false) }
    val resolvedGroup = taskGroup
        ?: todoItem?.let { resolveTaskGroup(it, emptyList()) }
        ?: ResolvedTaskGroup(0, "例行", "#4CB782")
    val accent = colorFromHex(resolvedGroup.colorHex)
    val outerScroll = rememberScrollState()
    val notesScroll = rememberScrollState()
    val isEvent = todoItem?.isEvent == true
    val titleHeadline = if (isEvent) "这段日程快开始了" else "现在该处理这项任务了"
    val subHeadline = if (isEvent) {
        "提醒到了，确认即可。"
    } else {
        "请明确完成、延后或取消，不再保留忽略入口"
    }
    val primaryActionText = if (isEvent) "我知道了" else "我已完成"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .verticalScroll(outerScroll)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PaykiTodo 提醒",
                    color = accent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = titleHeadline,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subHeadline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        ElevatedCard(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = accent
                ) {
                    Text(
                        text = "${taskGroupEmoji(resolvedGroup)} ${resolvedGroup.name}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = todoItem?.title ?: "正在加载提醒内容...",
                    style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 30.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                todoItem?.let { item ->
                    ReminderMetaCard(
                        label = when {
                            item.isEvent -> "⏰ 时间"
                            item.hasDueDate -> "\u23F0 DDL"
                            else -> "\uD83D\uDDD2 状态"
                        },
                        value = if (item.isEvent) {
                            reminderEventTimeLabel(item)
                        } else if (item.hasDueDate) {
                            formatLocalDateTime(reminderAtMillisToDateTime(item.dueAtMillis))
                        } else {
                            "未设置 DDL"
                        },
                        accent = accent
                    )
                    if (item.isEvent && item.location.isNotBlank()) {
                        ReminderMetaCard(
                            label = "\uD83D\uDCCD 地点",
                            value = item.location,
                            accent = accent.copy(alpha = 0.92f)
                        )
                    }
                }

                if (!todoItem?.notes.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "\uD83D\uDCDD 备注",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(notesScroll)
                            ) {
                                Text(
                                    text = todoItem?.notes.orEmpty(),
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 23.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { if (isEvent) onAcknowledge() else onComplete() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18794E))
                    ) {
                        Text(primaryActionText)
                    }
                    if (!isEvent) {
                        FilledTonalButton(
                            onClick = { onSnooze(5) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("延后 5 分钟")
                        }
                    }
                }

                if (!isEvent) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (todoItem?.isRecurring == true) {
                                    showCancelScopeDialog = true
                                } else {
                                    onCancel(RecurrenceScope.CURRENT)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消任务")
                        }
                        FilledTonalButton(
                            onClick = { onSnooze(defaultSnoozeMinutes) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("延后 $defaultSnoozeMinutes 分钟")
                        }
                    }
                }

                if (!isEvent) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "自定义延后",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customMinutes,
                                    onValueChange = { customMinutes = it },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("延后时间") },
                                    placeholder = { Text("5 或 16:30") },
                                    isError = !customSnoozeValidation.isValid,
                                    supportingText = {
                                        Text(if (customSnoozeValidation.isValid) customSnoozeValidation.message else customSnoozeValidation.message)
                                    },
                                    singleLine = true
                                )
                                Button(
                                    enabled = customSnoozeValidation.isValid,
                                    onClick = {
                                        onSnooze(customSnoozeValidation.minutes)
                                    },
                                    modifier = Modifier.widthIn(min = 112.dp)
                                ) {
                                    Text("确认")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCancelScopeDialog) {
        AlertDialog(
            onDismissRequest = { showCancelScopeDialog = false },
            title = { Text("选择取消范围") },
            text = { Text("循环任务需要先确定取消这一次、当前及后续，还是整个系列。") },
            confirmButton = {
                Column {
                    RecurrenceScope.entries.forEach { scope ->
                        TextButton(
                            onClick = {
                                showCancelScopeDialog = false
                                onCancel(scope)
                            }
                        ) {
                            Text(scope.label)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelScopeDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

private fun reminderEventTimeLabel(item: TodoItem): String {
    if (item.allDay) {
        val start = item.startAtMillis?.let(::reminderAtMillisToDateTime)?.toLocalDate()
            ?: return "全天"
        val endExclusive = item.endAtMillis?.let(::reminderAtMillisToDateTime)?.toLocalDate()?.minusDays(1)
            ?: start
        return if (start == endExclusive) {
            "全天\n$start"
        } else {
            "全天\n$start - $endExclusive"
        }
    }
    val start = item.startAtMillis?.let(::reminderAtMillisToDateTime)
        ?: return item.dueDateTimeOrNull()?.let(::formatLocalDateTime) ?: "未设置时间"
    val end = item.endAtMillis?.let(::reminderAtMillisToDateTime) ?: start
    val dayPrefix = formatLocalDateTime(start).substringBefore(' ')
    val startTime = formatLocalDateTime(start).substringAfter(' ')
    val endTime = formatLocalDateTime(end).substringAfter(' ')
    return if (start.toLocalDate() == end.toLocalDate()) {
        "$dayPrefix\n$startTime - $endTime"
    } else {
        "${formatLocalDateTime(start)}\n${formatLocalDateTime(end)}"
    }
}

@Composable
private fun ReminderMetaCard(
    label: String,
    value: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = accent,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                color = accent,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
