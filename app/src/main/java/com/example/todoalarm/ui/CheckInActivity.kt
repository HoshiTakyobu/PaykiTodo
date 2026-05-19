package com.example.todoalarm.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.EventCheckIn
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val EXTRA_EVENT_ID = "check_in_event_id"

class CheckInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        if (eventId <= 0L) { finish(); return }
        val app = application as TodoApplication
        setContent {
            TodoAlarmTheme {
                CheckInScreen(
                    eventId = eventId,
                    loadEvent = { app.repository.getTodo(eventId) },
                    loadActiveCheckIn = { app.repository.getActiveCheckIn(eventId) },
                    loadCheckIns = { app.repository.getCheckInsForEvent(eventId) },
                    onCheckIn = {
                        app.repository.checkInEvent(eventId)?.also {
                            autoBackupIfEnabled(app)
                        }
                    },
                    onCheckOut = {
                        app.repository.checkOutEvent(eventId)?.also {
                            autoBackupIfEnabled(app)
                        }
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context, eventId: Long): Intent {
            return Intent(context, CheckInActivity::class.java).apply {
                putExtra(EXTRA_EVENT_ID, eventId)
            }
        }
    }
}

private enum class CheckInState { NOT_CHECKED_IN, CHECKED_IN, CHECKED_OUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckInScreen(
    eventId: Long,
    loadEvent: suspend () -> TodoItem?,
    loadActiveCheckIn: suspend () -> EventCheckIn?,
    loadCheckIns: suspend () -> List<EventCheckIn>,
    onCheckIn: suspend () -> EventCheckIn?,
    onCheckOut: suspend () -> EventCheckIn?,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var event by remember { mutableStateOf<TodoItem?>(null) }
    var state by remember { mutableStateOf(CheckInState.NOT_CHECKED_IN) }
    var activeCheckIn by remember { mutableStateOf<EventCheckIn?>(null) }
    var totalInvestedMinutes by remember { mutableLongStateOf(0L) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showCheckOutDialog by remember { mutableStateOf(false) }
    var operating by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        event = loadEvent()
        val active = loadActiveCheckIn()
        activeCheckIn = active
        val allRecords = loadCheckIns()
        val closedMinutes = allRecords.filter { it.checkOutAtMillis != null }.sumOf { it.durationMinutes.toLong() }
        totalInvestedMinutes = closedMinutes
        state = when {
            active != null -> CheckInState.CHECKED_IN
            allRecords.any { it.checkOutAtMillis != null } -> CheckInState.CHECKED_OUT
            else -> CheckInState.NOT_CHECKED_IN
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val currentTimeText = remember(nowMillis) {
        val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
        ldt.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    }

    val elapsedText = remember(nowMillis, activeCheckIn, totalInvestedMinutes, state) {
        val activeMinutes = if (state == CheckInState.CHECKED_IN && activeCheckIn != null) {
            ((nowMillis - activeCheckIn!!.checkInAtMillis).coerceAtLeast(0L) / 60_000L)
        } else 0L
        val total = totalInvestedMinutes + activeMinutes
        val h = total / 60
        val m = total % 60
        if (h > 0) "${h} h ${m} m" else "${m} m"
    }

    val buttonColor by animateColorAsState(
        targetValue = when (state) {
            CheckInState.NOT_CHECKED_IN -> Color(0xFF2196F3)
            CheckInState.CHECKED_IN -> Color(0xFF4CAF50)
            CheckInState.CHECKED_OUT -> Color(0xFFF44336)
        },
        label = "buttonColor"
    )

    val buttonText = when (state) {
        CheckInState.NOT_CHECKED_IN -> "签到"
        CheckInState.CHECKED_IN -> "已签到"
        CheckInState.CHECKED_OUT -> "再次签到"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = event?.title ?: "日程",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = currentTimeText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    when (state) {
                        CheckInState.NOT_CHECKED_IN,
                        CheckInState.CHECKED_OUT -> {
                            scope.launch {
                                operating = true
                                val result = onCheckIn()
                                if (result != null) {
                                    activeCheckIn = result
                                    state = CheckInState.CHECKED_IN
                                }
                                operating = false
                            }
                        }
                        CheckInState.CHECKED_IN -> {
                            showCheckOutDialog = true
                        }
                    }
                },
                enabled = !operating,
                modifier = Modifier.size(200.dp),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (state != CheckInState.NOT_CHECKED_IN) {
                Text(
                    text = "已计入打卡时间：$elapsedText",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showCheckOutDialog) {
        AlertDialog(
            onDismissRequest = { showCheckOutDialog = false },
            title = { Text("确认签退") },
            text = { Text("确定要签退吗？当前已计入打卡时间：$elapsedText") },
            confirmButton = {
                TextButton(onClick = {
                    showCheckOutDialog = false
                    scope.launch {
                        operating = true
                        val result = onCheckOut()
                        if (result != null) {
                            activeCheckIn = null
                            totalInvestedMinutes += result.durationMinutes.toLong()
                            state = CheckInState.CHECKED_OUT
                        }
                        operating = false
                    }
                }) { Text("确认签退") }
            },
            dismissButton = {
                TextButton(onClick = { showCheckOutDialog = false }) { Text("取消") }
            }
        )
    }
}

private suspend fun autoBackupIfEnabled(app: TodoApplication) {
    val settings = app.settingsStore.currentSettings()
    if (!settings.autoBackupEnabled) return
    val directoryUri = settings.backupDirectoryUri ?: return
    runCatching {
        withContext(Dispatchers.IO) {
            val snapshot = app.repository.exportSnapshot(settings)
            app.backupManager.autoBackupToDirectory(directoryUri, snapshot)
        }
    }
}
