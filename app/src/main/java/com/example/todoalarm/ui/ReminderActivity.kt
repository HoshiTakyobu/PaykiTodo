package com.example.todoalarm.ui

import android.app.NotificationManager
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.alarm.AlarmScheduler
import com.example.todoalarm.alarm.ReminderNotifier
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReminderActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private val app by lazy { application as TodoApplication }
    private var todoItem by mutableStateOf<TodoItem?>(null)
    private var ringtone: Ringtone? = null
    private var textToSpeech: TextToSpeech? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            }
        )

        textToSpeech = TextToSpeech(this, this)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        loadTodo()

        setContent {
            TodoAlarmTheme {
                ReminderScreen(
                    todoItem = todoItem,
                    onSnooze = ::snooze
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadTodo()
    }

    override fun onInit(status: Int) {
        val currentItem = todoItem ?: return
        if (status == TextToSpeech.SUCCESS && currentItem.voiceEnabled) {
            textToSpeech?.language = Locale.SIMPLIFIED_CHINESE
            textToSpeech?.speak(
                "现在需要做的任务是 ${currentItem.title}",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "todo_alarm"
            )
        }
    }

    override fun onDestroy() {
        stopAlert()
        textToSpeech?.shutdown()
        textToSpeech = null
        super.onDestroy()
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
            todoItem = item
            startAlert(item)
        }
    }

    private fun startAlert(item: TodoItem) {
        stopAlert()

        if (item.ringEnabled) {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(this, alarmUri)?.apply {
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
            val pattern = longArrayOf(0, 900, 500)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        }

        if (item.voiceEnabled) {
            textToSpeech?.speak(
                "现在需要做的任务是 ${item.title}",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "todo_alarm_repeat"
            )
        }
    }

    private fun stopAlert() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        textToSpeech?.stop()
    }

    private fun snooze(minutes: Long) {
        val item = todoItem ?: return
        lifecycleScope.launch {
            val nextReminder = System.currentTimeMillis() + minutes * 60_000
            val updated = app.repository.snoozeTodo(item.id, nextReminder)
            if (updated != null) {
                app.alarmScheduler.schedule(updated)
            }
            getSystemService(NotificationManager::class.java)
                .cancel(ReminderNotifier.notificationId(item.id))
            stopAlert()
            finish()
        }
    }
}

@Composable
private fun ReminderScreen(
    todoItem: TodoItem?,
    onSnooze: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "全屏提醒",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = todoItem?.title ?: "载入中...",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (!todoItem?.notes.isNullOrBlank()) {
                    Text(text = todoItem?.notes.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                }
                todoItem?.let {
                    Text(text = "DDL: ${formatDueDate(it.dueDateEpochDay)}")
                    Text(
                        text = "原提醒时间: ${
                            it.reminderAtMillis?.let { millis -> formatLocalDateTime(reminderAtMillisToDateTime(millis)) }
                                ?: "未设置"
                        }"
                    )
                }
            }
        }

        Text(
            text = "这个提醒页不能直接关闭，只能延后。",
            style = MaterialTheme.typography.bodyLarge
        )

        Button(
            onClick = { onSnooze(10) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("延后 10 分钟")
        }

        Button(
            onClick = { onSnooze(30) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("延后 30 分钟")
        }
    }
}

private fun formatDueDate(epochDay: Long): String {
    return LocalDate.ofEpochDay(epochDay)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA))
}
