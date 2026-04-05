package com.example.todoalarm.ui

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.todoalarm.alarm.ReminderForegroundService
import com.example.todoalarm.alarm.ReminderNotifier
import com.example.todoalarm.data.TodoCategory
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderActivity : ComponentActivity() {
    private val app by lazy { application as TodoApplication }
    private var todoItem by mutableStateOf<TodoItem?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
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
                    defaultSnoozeMinutes = settings.defaultSnoozeMinutes,
                    onComplete = ::completeTodo,
                    onSnooze = ::snooze
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadTodo()
    }

    private fun loadTodo() {
        val todoId = intent.getLongExtra(AlarmScheduler.EXTRA_TODO_ID, -1L)
        if (todoId <= 0L) {
            finish()
            return
        }
        lifecycleScope.launch {
            val item = app.repository.getTodo(todoId)
            if (item == null || item.completed) {
                ActiveReminderStore.clearIfMatches(this@ReminderActivity, todoId)
                ActiveReminderStore.clearActivityHandoff(this@ReminderActivity, todoId)
                finish()
                return@launch
            }
            ActiveReminderStore.clearActivityHandoff(this@ReminderActivity, todoId)
            todoItem = item
        }
    }

    private fun completeTodo() {
        val item = todoItem ?: return
        lifecycleScope.launch {
            app.repository.setCompleted(item.id, true)
            app.alarmScheduler.cancel(item.id)
            app.reminderNotifier.cancel(item.id)
            ActiveReminderStore.clearIfMatches(this@ReminderActivity, item.id)
            stopService(Intent(this@ReminderActivity, ReminderForegroundService::class.java))
            finish()
        }
    }

    private fun snooze(minutes: Int) {
        val item = todoItem ?: return
        lifecycleScope.launch {
            val nextReminder = nextMinuteAlignedReminder(minutes)
            app.alarmScheduler.cancel(item.id)
            val updated = app.repository.snoozeTodo(item.id, nextReminder)
            if (updated != null) {
                app.alarmScheduler.schedule(updated)
            }
            ActiveReminderStore.clearIfMatches(this@ReminderActivity, item.id)
            stopService(Intent(this@ReminderActivity, ReminderForegroundService::class.java))
            getSystemService(NotificationManager::class.java).cancel(ReminderNotifier.notificationId(item.id))
            finish()
        }
    }

    private fun nextMinuteAlignedReminder(minutes: Int): Long {
        val zoneId = ZoneId.systemDefault()
        val nextReminder = LocalDateTime.now(zoneId)
            .withSecond(0)
            .withNano(0)
            .plusMinutes(minutes.toLong())
        return nextReminder.atZone(zoneId).toInstant().toEpochMilli()
    }
}

@Composable
private fun ReminderScreen(
    todoItem: TodoItem?,
    defaultSnoozeMinutes: Int,
    onComplete: () -> Unit,
    onSnooze: (Int) -> Unit
) {
    var customMinutes by remember(defaultSnoozeMinutes) { mutableStateOf(defaultSnoozeMinutes.toString()) }
    val category = todoItem?.let { TodoCategory.fromKey(it.categoryKey) } ?: TodoCategory.ROUTINE
    val accent = reminderAccent(category)
    val outerScroll = rememberScrollState()
    val notesScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
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
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
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
                    text = "现在该做这件事了",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "完成、延后，或查看完整内容",
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
                        text = "${categoryEmoji(category)} ${categoryLabel(category)}",
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
                        label = "⏰ DDL",
                        value = formatLocalDateTime(reminderAtMillisToDateTime(item.dueAtMillis)),
                        accent = accent
                    )
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
                                text = "📝 备注",
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
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
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
                        onClick = onComplete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18794E))
                    ) {
                        Text("我已完成")
                    }
                    FilledTonalButton(
                        onClick = { onSnooze(5) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("延后 5 分钟")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onSnooze(defaultSnoozeMinutes) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("延后 $defaultSnoozeMinutes 分钟")
                    }
                    FilledTonalButton(
                        onClick = { onSnooze(30) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("延后 30 分钟")
                    }
                }

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
                                onValueChange = { customMinutes = it.filter(Char::isDigit) },
                                modifier = Modifier.weight(1f),
                                label = { Text("分钟数") },
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    val minutes = customMinutes.toIntOrNull()?.coerceIn(1, 180)
                                        ?: defaultSnoozeMinutes
                                    onSnooze(minutes)
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

        Spacer(modifier = Modifier.height(4.dp))
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

private fun categoryLabel(category: TodoCategory): String = when (category) {
    TodoCategory.IMPORTANT -> "重要"
    TodoCategory.URGENT -> "紧急"
    TodoCategory.FOCUS -> "专注"
    TodoCategory.ROUTINE -> "例行"
}

private fun categoryEmoji(category: TodoCategory): String = when (category) {
    TodoCategory.IMPORTANT -> "⭐"
    TodoCategory.URGENT -> "⚠️"
    TodoCategory.FOCUS -> "🎯"
    TodoCategory.ROUTINE -> "🧭"
}

private fun reminderAccent(category: TodoCategory): Color = when (category) {
    TodoCategory.IMPORTANT -> Color(0xFF8B5CF6)
    TodoCategory.URGENT -> Color(0xFFE11D48)
    TodoCategory.FOCUS -> Color(0xFF2563EB)
    TodoCategory.ROUTINE -> Color(0xFF0F766E)
}
