package com.example.todoalarm.ui

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.delay
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
                    onCheckInEvent = ::checkInEvent,
                    onSnooze = ::snooze,
                    onPostponeDueAt = ::postponeDueAt,
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

    private fun checkInEvent() {
        val item = todoItem ?: return
        lifecycleScope.launch {
            val checkIn = app.repository.checkInEvent(item.id)
            if (checkIn != null) {
                app.repository.acknowledgeCalendarEvent(item.id)
            }
            ReminderChainLogger.log(
                context = this@ReminderActivity,
                todoId = item.id,
                source = "ReminderActivity",
                stage = ReminderChainStage.USER_COMPLETE,
                status = if (checkIn == null) ReminderChainStatus.WARN else ReminderChainStatus.OK,
                message = if (checkIn == null) "event_check_in_failed" else "event_check_in"
            )
            if (checkIn != null) {
                clearReminderArtifacts(listOf(item))
                closeReminder(item.id)
            } else {
                Toast.makeText(this@ReminderActivity, "无法签到：请确认日程已开启打卡追踪", Toast.LENGTH_SHORT).show()
            }
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

    private fun postponeDueAt(targetDueAt: LocalDateTime) {
        val item = todoItem ?: return
        val targetMillis = targetDueAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        lifecycleScope.launch {
            ReminderChainLogger.log(
                context = this@ReminderActivity,
                todoId = item.id,
                source = "ReminderActivity",
                stage = ReminderChainStage.USER_SNOOZE,
                status = ReminderChainStatus.OK,
                message = "postponeDueAt=$targetDueAt"
            )
            clearReminderArtifacts(listOf(item))
            val updated = app.repository.postponeTodoDueAt(item.id, targetMillis)
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
    onCheckInEvent: () -> Unit,
    onSnooze: (Int) -> Unit,
    onPostponeDueAt: (LocalDateTime) -> Unit,
    onCancel: (RecurrenceScope) -> Unit
) {
    var customMinutes by remember(defaultSnoozeMinutes) { mutableStateOf(defaultSnoozeMinutes.toString()) }
    val customSnoozeValidation = remember(customMinutes) { parseSnoozeInput(customMinutes) }
    var ddlPostponeInput by remember(todoItem?.id) { mutableStateOf("") }
    val currentDueAt = todoItem
        ?.takeIf { it.isTodo && it.hasDueDate }
        ?.dueDateTimeOrNull()
    val ddlPostponeValidation = remember(ddlPostponeInput, currentDueAt) {
        currentDueAt?.let { parseDdlPostponeInput(ddlPostponeInput, it) }
    }
    var helpTopic by remember { mutableStateOf<InputSyntaxHelpTopic?>(null) }
    var showCancelScopeDialog by remember(todoItem?.id) { mutableStateOf(false) }
    var notesExpanded by remember(todoItem?.id) { mutableStateOf(false) }
    var contentVisible by remember(todoItem?.id) { mutableStateOf(false) }
    val resolvedGroup = taskGroup
        ?: todoItem?.let { resolveTaskGroup(it, emptyList()) }
        ?: ResolvedTaskGroup(0, "例行", "#4CB782")
    val accent = colorFromHex(resolvedGroup.colorHex)
    val outerScroll = rememberScrollState()
    val isEvent = todoItem?.isEvent == true
    val alarmMode = todoItem?.alarmMode == true
    val alarmPulseAlpha by rememberInfiniteTransition(label = "alarmModePulse").animateFloat(
        initialValue = 0.18f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 820),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alarmModePulseAlpha"
    )
    LaunchedEffect(todoItem?.id) {
        contentVisible = false
        delay(80)
        contentVisible = true
    }
    val mainTimeLabel = reminderMainTimeLabel(todoItem)
    val secondaryTimeLabel = reminderSecondaryTimeLabel(todoItem)
    val subHeadline = if (alarmMode) {
        "闹钟模式已开启：完成、延后或取消后才会停止响铃。"
    } else if (isEvent) {
        "提醒到了，确认或签到即可。"
    } else {
        "请明确完成、延后或取消。"
    }
    val primaryActionText = if (isEvent) "我知道了" else "我已完成"
    val titleText = todoItem?.title ?: "正在加载提醒内容..."

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
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 5 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (alarmMode) {
                        Color(0xFFD32F2F).copy(alpha = alarmPulseAlpha)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                    }
                ) {
                    Text(
                        text = if (alarmMode) "闹钟模式 · 操作后停止响铃" else "${taskGroupEmoji(resolvedGroup)} ${resolvedGroup.name}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (alarmMode) Color.White else accent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = mainTimeLabel,
                    style = MaterialTheme.typography.displayMedium.copy(lineHeight = 58.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (alarmMode) Color(0xFFD32F2F) else accent,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = titleText,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.displaySmall.copy(lineHeight = 42.sp),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = secondaryTimeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subHeadline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                todoItem?.takeIf { it.isEvent && it.location.isNotBlank() }?.let { item ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = accent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "📍 ${item.location}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                todoItem?.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { notesExpanded = !notesExpanded },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "📝 备注",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (notesExpanded) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (notesExpanded) "收起备注" else "点按展开",
                                style = MaterialTheme.typography.labelMedium,
                                color = accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(340, delayMillis = 90)) + slideInVertically(tween(340, delayMillis = 90)) { it / 4 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { if (isEvent) onAcknowledge() else onComplete() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (alarmMode) Color(0xFFD32F2F) else Color(0xFF18794E)
                        )
                    ) {
                        Text(primaryActionText)
                    }
                    if (!isEvent) {
                        FilledTonalButton(
                            onClick = { onSnooze(5) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text("延后 5 分钟")
                        }
                    } else if (todoItem?.checkInEnabled == true) {
                        FilledTonalButton(
                            onClick = onCheckInEvent,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = accent.copy(alpha = 0.14f),
                                contentColor = accent
                            )
                        ) {
                            Text("签到")
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
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text("取消任务")
                        }
                        FilledTonalButton(
                            onClick = { onSnooze(10) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text("延后 10 分钟")
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
                                text = "自定义延后提醒",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "只调整下一次提醒，不改变任务 DDL。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("延后时间")
                                            InputSyntaxHelpIconButton(
                                                topic = InputSyntaxHelpTopic.Snooze,
                                                onClick = { helpTopic = InputSyntaxHelpTopic.Snooze }
                                            )
                                        }
                                    },
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
                                    modifier = Modifier
                                        .widthIn(min = 112.dp)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Text("确认")
                                }
                            }
                        }
                    }
                }

                if (!isEvent && currentDueAt != null) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.38f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "DDL 推迟",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "会真正修改任务 DDL；目标必须晚于当前 DDL（${formatLocalDateTime(currentDueAt)}）。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = ddlPostponeInput,
                                    onValueChange = { ddlPostponeInput = it },
                                    modifier = Modifier.weight(1f),
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("新的 DDL")
                                            InputSyntaxHelpIconButton(
                                                topic = InputSyntaxHelpTopic.DdlPostpone,
                                                onClick = { helpTopic = InputSyntaxHelpTopic.DdlPostpone }
                                            )
                                        }
                                    },
                                    placeholder = { Text("30分钟 或 16:30") },
                                    isError = ddlPostponeInput.isNotBlank() && ddlPostponeValidation?.isValid == false,
                                    supportingText = {
                                        val validation = ddlPostponeValidation
                                        Text(
                                            when {
                                                ddlPostponeInput.isBlank() -> "例：30分钟、往后推45分钟、16:30、2026-05-22 16:30"
                                                validation?.isValid == true -> validation.message
                                                else -> validation?.message.orEmpty()
                                            }
                                        )
                                    },
                                    singleLine = true
                                )
                                Button(
                                    enabled = ddlPostponeValidation?.isValid == true,
                                    onClick = {
                                        ddlPostponeValidation?.targetDueAt?.let(onPostponeDueAt)
                                    },
                                    modifier = Modifier
                                        .widthIn(min = 112.dp)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Text("推迟 DDL")
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

    helpTopic?.let { topic ->
        InputSyntaxHelpDialog(topic = topic, onDismiss = { helpTopic = null })
    }
}

private fun reminderMainTimeLabel(item: TodoItem?): String {
    if (item == null) return "--:--"
    if (item.isEvent) {
        if (item.allDay) return "全天"
        val start = item.startAtMillis?.let(::reminderAtMillisToDateTime)
            ?: item.dueDateTimeOrNull()
            ?: return "现在"
        return "%02d:%02d".format(start.hour, start.minute)
    }
    val dueAt = item.dueDateTimeOrNull() ?: return "现在"
    return "%02d:%02d".format(dueAt.hour, dueAt.minute)
}

private fun reminderSecondaryTimeLabel(item: TodoItem?): String {
    if (item == null) return "正在读取提醒内容"
    if (item.isEvent) {
        return reminderEventTimeLabel(item).replace("\n", " · ")
    }
    return item.dueDateTimeOrNull()?.let { dueAt ->
        "DDL：${formatLocalDateTime(dueAt)}"
    } ?: "无 DDL 任务"
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
