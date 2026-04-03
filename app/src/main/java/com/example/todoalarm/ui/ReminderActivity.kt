package com.example.todoalarm.ui

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.alarm.AlarmScheduler
import com.example.todoalarm.alarm.ReminderForegroundService
import com.example.todoalarm.alarm.ReminderNotifier
import com.example.todoalarm.data.TodoCategory
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import kotlinx.coroutines.launch
import java.util.Locale

class ReminderActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private val app by lazy { application as TodoApplication }
    private var todoItem by mutableStateOf<TodoItem?>(null)
    private var ringtone: Ringtone? = null
    private var textToSpeech: TextToSpeech? = null
    private var vibrator: Vibrator? = null
    private var ttsReady = false

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

        textToSpeech = TextToSpeech(this, this)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

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

    override fun onDestroy() {
        stopAlert()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        textToSpeech?.language = Locale.SIMPLIFIED_CHINESE
        todoItem?.let(::speakIfNeeded)
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
                finish()
                return@launch
            }
            stopService(Intent(this@ReminderActivity, ReminderForegroundService::class.java))
            getSystemService(NotificationManager::class.java).cancel(ReminderNotifier.notificationId(todoId))
            todoItem = item
            startAlert(item)
        }
    }

    private fun startAlert(item: TodoItem) {
        stopAlert()
        if (item.ringEnabled) {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = true
                }
                audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                play()
            }
        }
        if (item.vibrateEnabled) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 900, 450), 0))
        }
        speakIfNeeded(item)
    }

    private fun speakIfNeeded(item: TodoItem) {
        if (item.voiceEnabled && ttsReady) {
            textToSpeech?.speak(
                "现在需要处理的任务是，${item.title}",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "todo_alarm"
            )
        }
    }

    private fun stopAlert() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        textToSpeech?.stop()
    }

    private fun completeTodo() {
        val item = todoItem ?: return
        lifecycleScope.launch {
            app.repository.setCompleted(item.id, true)
            app.alarmScheduler.cancel(item.id)
            app.reminderNotifier.cancel(item.id)
            stopService(Intent(this@ReminderActivity, ReminderForegroundService::class.java))
            stopAlert()
            finish()
        }
    }

    private fun snooze(minutes: Int) {
        val item = todoItem ?: return
        lifecycleScope.launch {
            val nextReminder = System.currentTimeMillis() + minutes * 60_000L
            app.alarmScheduler.cancel(item.id)
            val updated = app.repository.snoozeTodo(item.id, nextReminder)
            if (updated != null) {
                app.alarmScheduler.schedule(updated)
            }
            stopService(Intent(this@ReminderActivity, ReminderForegroundService::class.java))
            getSystemService(NotificationManager::class.java).cancel(ReminderNotifier.notificationId(item.id))
            stopAlert()
            finish()
        }
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
    val outerScroll = rememberScrollState()
    val notesScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                )
            )
            .padding(24.dp)
            .verticalScroll(outerScroll),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${reminderEmoji(category)} 该做这件事了",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "只能完成或延后，不能直接关闭提醒。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReminderCategoryChip(category = category)
                    Text(
                        text = todoItem?.title ?: "正在载入任务...",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 28.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                todoItem?.let { item ->
                    ReminderMetaCard(
                        label = "⏰ DDL",
                        value = formatLocalDateTime(reminderAtMillisToDateTime(item.dueAtMillis)),
                        containerBrush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            )
                        ),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }

                if (!todoItem?.notes.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📝 备注",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                                    .verticalScroll(notesScroll)
                            ) {
                                Text(
                                    text = todoItem?.notes.orEmpty(),
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f)
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
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "自定义延后",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
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
                        onClick = { onSnooze(customMinutes.toIntOrNull()?.coerceIn(1, 180) ?: defaultSnoozeMinutes) },
                        modifier = Modifier.widthIn(min = 120.dp)
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderCategoryChip(category: TodoCategory) {
    val tint = categoryReminderTint(category)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = tint.copy(alpha = 0.16f)
    ) {
        Text(
            text = "${reminderEmoji(category)} ${category.label}",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = tint,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReminderMetaCard(
    label: String,
    value: String,
    containerBrush: Brush,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerBrush)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun reminderEmoji(category: TodoCategory): String = when (category) {
    TodoCategory.IMPORTANT -> "⭐"
    TodoCategory.URGENT -> "🚨"
    TodoCategory.FOCUS -> "🎯"
    TodoCategory.ROUTINE -> "🌿"
}

@Composable
private fun categoryReminderTint(category: TodoCategory): Color = when (category) {
    TodoCategory.IMPORTANT -> MaterialTheme.colorScheme.secondary
    TodoCategory.URGENT -> MaterialTheme.colorScheme.error
    TodoCategory.FOCUS -> MaterialTheme.colorScheme.primary
    TodoCategory.ROUTINE -> MaterialTheme.colorScheme.tertiary
}
