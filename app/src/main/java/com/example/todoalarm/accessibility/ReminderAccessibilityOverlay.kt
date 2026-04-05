package com.example.todoalarm.accessibility

import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.accessibilityservice.AccessibilityService
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.alarm.ActiveReminderStore
import com.example.todoalarm.alarm.AlarmScheduler
import com.example.todoalarm.alarm.ReminderForegroundService
import com.example.todoalarm.alarm.ReminderNotifier
import com.example.todoalarm.data.TodoCategory
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.ReminderActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReminderAccessibilityOverlay(
    private val service: AccessibilityService
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val app: TodoApplication
        get() = service.application as TodoApplication
    private val windowManager: WindowManager
        get() = service.getSystemService(WindowManager::class.java)

    private var overlayView: View? = null
    private var overlayTodoId: Long = -1L
    private var lastForegroundAttemptAtElapsed: Long = 0L

    fun showFor(todoId: Long) {
        if (overlayTodoId == todoId && overlayView != null) {
            requestReminderForeground(todoId)
            return
        }

        serviceScope.launch {
            val item = withContext(Dispatchers.IO) { app.repository.getTodo(todoId) }
            if (item == null || item.completed || !item.reminderEnabled) {
                hide(todoId)
                ActiveReminderStore.clearIfMatches(service, todoId)
                return@launch
            }

            hide()
            val root = buildOverlay(item)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            runCatching { windowManager.addView(root, params) }
                .onSuccess {
                    overlayView = root
                    overlayTodoId = item.id
                    root.announceForAccessibility("${categoryEmoji(item)} ${item.title}")
                    requestReminderForeground(item.id)
                }
        }
    }

    fun hide(todoId: Long? = null) {
        if (todoId != null && todoId != overlayTodoId) return
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        overlayTodoId = -1L
    }

    fun destroy() {
        hide()
        serviceScope.cancel()
    }

    private fun buildOverlay(item: TodoItem): View {
        val context = service
        val accent = categoryColor(item)
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#C0121A2A"))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                setColor(Color.parseColor("#FFF8FBFF"))
                setStroke(dp(2), accent)
            }
            setPadding(dp(22), dp(22), dp(22), dp(22))
        }

        val badge = TextView(context).apply {
            text = "${categoryEmoji(item)} ${categoryLabel(item)}"
            setTextColor(Color.WHITE)
            setTextSize(16f)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(accent)
            }
            setPadding(dp(12), dp(7), dp(12), dp(7))
        }
        card.addView(badge)

        val title = TextView(context).apply {
            text = "到时间了，${item.title}"
            setTextColor(Color.parseColor("#FF10243D"))
            setTextSize(24f)
            typeface = Typeface.DEFAULT_BOLD
            setLineSpacing(0f, 1.08f)
            setPadding(0, dp(14), 0, 0)
        }
        card.addView(title)

        val ddl = TextView(context).apply {
            text = "⏰ DDL: ${formatDateTime(item.dueAtMillis)}"
            setTextColor(accent)
            setTextSize(16f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(10), 0, 0)
        }
        card.addView(ddl)

        if (item.notes.isNotBlank()) {
            val notesTitle = TextView(context).apply {
                text = "备注"
                setTextColor(Color.parseColor("#FF10243D"))
                setTextSize(15f)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(16), 0, dp(8))
            }
            card.addView(notesTitle)

            val notesScroll = ScrollView(context).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(18).toFloat()
                    setColor(Color.parseColor("#FFEAF1F8"))
                }
                isFillViewport = true
            }
            val notesText = TextView(context).apply {
                text = item.notes
                setTextColor(Color.parseColor("#FF324A67"))
                setTextSize(16f)
                setLineSpacing(0f, 1.15f)
                ellipsize = TextUtils.TruncateAt.END
                setPadding(dp(14), dp(14), dp(14), dp(14))
            }
            notesScroll.addView(
                notesText,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
            card.addView(
                notesScroll,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(180)
                )
            )
        }

        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, 0)
        }

        val openButton = actionButton("打开提醒", accent).apply {
            setOnClickListener { requestReminderForeground(item.id) }
        }
        val completeButton = actionButton("我已完成", Color.parseColor("#FF18794E")).apply {
            setOnClickListener { completeTodo(item.id) }
        }
        val snoozeButton = actionButton("延后 5 分钟", Color.parseColor("#FF8A5A00")).apply {
            setOnClickListener { snoozeTodo(item.id, 5) }
        }

        actionRow.addView(openButton, weightedParams())
        actionRow.addView(spaceView())
        actionRow.addView(completeButton, weightedParams())
        card.addView(actionRow)

        card.addView(spaceView(heightDp = 10))
        card.addView(snoozeButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        root.addView(
            card,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ).apply {
                marginStart = dp(20)
                marginEnd = dp(20)
            }
        )
        return root
    }

    private fun requestReminderForeground(todoId: Long) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastForegroundAttemptAtElapsed < 350L) return
        lastForegroundAttemptAtElapsed = now

        openReminderActivity(todoId)
        serviceScope.launch {
            delay(550L)
            if (overlayTodoId == todoId) {
                openReminderActivity(todoId)
            }
        }
    }

    private fun openReminderActivity(todoId: Long) {
        runCatching {
            service.startActivity(
                Intent(service, ReminderActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    )
                    putExtra(AlarmScheduler.EXTRA_TODO_ID, todoId)
                }
            )
        }
    }

    private fun completeTodo(todoId: Long) {
        serviceScope.launch {
            withContext(Dispatchers.IO) {
                val item = app.repository.getTodo(todoId) ?: return@withContext
                app.repository.setCompleted(item.id, true)
                app.alarmScheduler.cancel(item.id)
                app.reminderNotifier.cancel(item.id)
                ActiveReminderStore.clearIfMatches(service, item.id)
            }
            service.stopService(Intent(service, ReminderForegroundService::class.java))
            service.getSystemService(NotificationManager::class.java)
                .cancel(ReminderNotifier.notificationId(todoId))
            hide(todoId)
            Toast.makeText(service, "任务已完成", Toast.LENGTH_SHORT).show()
        }
    }

    private fun snoozeTodo(todoId: Long, minutes: Int) {
        serviceScope.launch {
            withContext(Dispatchers.IO) {
                val item = app.repository.getTodo(todoId) ?: return@withContext
                app.alarmScheduler.cancel(item.id)
                val updated = app.repository.snoozeTodo(item.id, nextMinuteAlignedReminder(minutes))
                if (updated != null) {
                    app.alarmScheduler.schedule(updated)
                }
                app.reminderNotifier.cancel(item.id)
                ActiveReminderStore.clearIfMatches(service, item.id)
            }
            service.stopService(Intent(service, ReminderForegroundService::class.java))
            service.getSystemService(NotificationManager::class.java)
                .cancel(ReminderNotifier.notificationId(todoId))
            hide(todoId)
            Toast.makeText(service, "已延后 5 分钟", Toast.LENGTH_SHORT).show()
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

    private fun weightedParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }

    private fun spaceView(heightDp: Int = 0): View {
        return View(service).apply {
            layoutParams = LinearLayout.LayoutParams(dp(10), if (heightDp == 0) 0 else dp(heightDp))
        }
    }

    private fun actionButton(label: String, color: Int): Button {
        return Button(service).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(color)
            }
            minHeight = dp(52)
            minimumHeight = dp(52)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
    }

    private fun dp(value: Int): Int {
        val density = service.resources.displayMetrics.density
        return (value * density).toInt()
    }

    private fun categoryLabel(item: TodoItem): String = when (TodoCategory.fromKey(item.categoryKey)) {
        TodoCategory.IMPORTANT -> "重要"
        TodoCategory.URGENT -> "紧急"
        TodoCategory.FOCUS -> "专注"
        TodoCategory.ROUTINE -> "例行"
    }

    private fun categoryEmoji(item: TodoItem): String = when (TodoCategory.fromKey(item.categoryKey)) {
        TodoCategory.IMPORTANT -> "⭐"
        TodoCategory.URGENT -> "⚠️"
        TodoCategory.FOCUS -> "🎯"
        TodoCategory.ROUTINE -> "🧭"
    }

    private fun categoryColor(item: TodoItem): Int = when (TodoCategory.fromKey(item.categoryKey)) {
        TodoCategory.IMPORTANT -> Color.parseColor("#FF8B5CF6")
        TodoCategory.URGENT -> Color.parseColor("#FFE11D48")
        TodoCategory.FOCUS -> Color.parseColor("#FF2563EB")
        TodoCategory.ROUTINE -> Color.parseColor("#FF0F766E")
    }

    private fun formatDateTime(epochMillis: Long): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(formatter)
    }
}
