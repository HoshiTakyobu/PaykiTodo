package com.example.todoalarm.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.alarm.ActiveReminderStore
import com.example.todoalarm.alarm.ReminderForegroundService
import com.example.todoalarm.alarm.ReminderNotifier
import com.example.todoalarm.data.TodoCategory
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.ResolvedTaskGroup
import com.example.todoalarm.ui.resolveTaskGroup
import com.example.todoalarm.ui.taskGroupEmoji
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    fun showFor(todoId: Long) {
        if (overlayTodoId == todoId && overlayView != null) return

        serviceScope.launch {
            val item = withContext(Dispatchers.IO) { app.repository.getTodo(todoId) }
            if (item == null || item.isHistory || !item.reminderEnabled) {
                hide(todoId)
                ActiveReminderStore.clearIfMatches(service, todoId)
                return@launch
            }

            val resolvedGroup = withContext(Dispatchers.IO) {
                resolveTaskGroup(item, app.repository.getGroup(item.groupId))
            }
            hide()
            val root = buildOverlay(item, resolvedGroup)
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
                    root.announceForAccessibility("到时间了，${item.title}")
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

    private fun buildOverlay(item: TodoItem, group: ResolvedTaskGroup): View {
        val accent = Color.parseColor(group.colorHex)

        val root = FrameLayout(service).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.parseColor("#F4E7FB"),
                    Color.parseColor("#E8F2FF"),
                    Color.parseColor("#F8FBFF")
                )
            )
            setPadding(dp(20), dp(24), dp(20), dp(24))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val content = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        content.addView(
            TextView(service).apply {
                text = "PaykiTodo 提醒"
                setTextColor(accent)
                setTextSize(16f)
                typeface = Typeface.DEFAULT_BOLD
            }
        )

        content.addView(
            TextView(service).apply {
                text = "该做这项任务了"
                setTextColor(Color.parseColor("#10243D"))
                setTextSize(30f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setLineSpacing(0f, 1.08f)
                setPadding(0, dp(10), 0, dp(10))
            }
        )

        val card = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(30).toFloat()
                setColor(Color.parseColor("#FFFDFEFF"))
                setStroke(dp(2), accent)
            }
            setPadding(dp(22), dp(22), dp(22), dp(22))
        }

        card.addView(
            TextView(service).apply {
                text = "${taskGroupEmoji(group)} ${group.name}"
                setTextColor(Color.WHITE)
                setTextSize(15f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(accent)
                }
                setPadding(dp(12), dp(6), dp(12), dp(6))
            }
        )

        card.addView(
            TextView(service).apply {
                text = item.title
                setTextColor(Color.parseColor("#10243D"))
                setTextSize(26f)
                typeface = Typeface.DEFAULT_BOLD
                setLineSpacing(0f, 1.1f)
                setPadding(0, dp(16), 0, 0)
            }
        )

        card.addView(
            TextView(service).apply {
                text = "⏰ DDL: ${formatDateTime(item.dueAtMillis)}"
                setTextColor(accent)
                setTextSize(17f)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(16), 0, 0)
            }
        )

        if (item.notes.isNotBlank()) {
            card.addView(
                TextView(service).apply {
                    text = "备注"
                    setTextColor(Color.parseColor("#10243D"))
                    setTextSize(15f)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, dp(16), 0, dp(8))
                }
            )

            val notesScroll = ScrollView(service).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(Color.parseColor("#EEF4FB"))
                }
                isFillViewport = true
            }
            notesScroll.addView(
                TextView(service).apply {
                    text = item.notes
                    setTextColor(Color.parseColor("#364A63"))
                    setTextSize(16f)
                    setLineSpacing(0f, 1.15f)
                    setPadding(dp(14), dp(14), dp(14), dp(14))
                },
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
            card.addView(
                notesScroll,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(240)
                )
            )
        }

        val actionRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, 0)
        }
        actionRow.addView(
            filledButton("我已完成", Color.parseColor("#18794E")).apply {
                setOnClickListener { completeTodo(item.id) }
            },
            weightedParams()
        )
        actionRow.addView(spaceView())
        actionRow.addView(
            filledButton("延后 5 分钟", Color.parseColor("#A16207")).apply {
                setOnClickListener { snoozeTodo(item.id, 5) }
            },
            weightedParams()
        )
        card.addView(actionRow)

        val customRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        val customInput = EditText(service).apply {
            hint = "自定义分钟"
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextColor(Color.parseColor("#10243D"))
            setHintTextColor(Color.parseColor("#6B7280"))
            setTextSize(16f)
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#FFFFFFFF"))
                setStroke(dp(1), Color.parseColor("#C8D7EA"))
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        customRow.addView(
            customInput,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        customRow.addView(spaceView())
        customRow.addView(
            filledButton("延后", accent).apply {
                setOnClickListener {
                    val minutes = customInput.text?.toString()?.trim()?.toIntOrNull()
                    if (minutes == null || minutes !in 1..180) {
                        Toast.makeText(service, "请输入 1 到 180 分钟", Toast.LENGTH_SHORT).show()
                    } else {
                        snoozeTodo(item.id, minutes)
                    }
                }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        card.addView(customRow)

        content.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
        return root
    }

    private fun completeTodo(todoId: Long) {
        serviceScope.launch {
            withContext(Dispatchers.IO) {
                val item = app.repository.getTodo(todoId) ?: return@withContext
                app.repository.setCompleted(item.id, true)
                app.alarmScheduler.cancel(item.id)
                app.reminderNotifier.cancel(item.id)
                ActiveReminderStore.clearIfMatches(service, item.id)
                ActiveReminderStore.clearActivityHandoff(service, item.id)
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
                ActiveReminderStore.clearActivityHandoff(service, item.id)
            }
            service.stopService(Intent(service, ReminderForegroundService::class.java))
            service.getSystemService(NotificationManager::class.java)
                .cancel(ReminderNotifier.notificationId(todoId))
            hide(todoId)
            Toast.makeText(service, "已延后 $minutes 分钟", Toast.LENGTH_SHORT).show()
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

    private fun filledButton(label: String, color: Int): Button {
        return Button(service).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(color)
            }
            minHeight = dp(54)
            minimumHeight = dp(54)
        }
    }

    private fun weightedParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }

    private fun spaceView(): View {
        return View(service).apply {
            layoutParams = LinearLayout.LayoutParams(dp(10), 0)
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
        TodoCategory.IMPORTANT -> Color.parseColor("#8B5CF6")
        TodoCategory.URGENT -> Color.parseColor("#E11D48")
        TodoCategory.FOCUS -> Color.parseColor("#2563EB")
        TodoCategory.ROUTINE -> Color.parseColor("#0F766E")
    }

    private fun formatDateTime(epochMillis: Long): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(formatter)
    }
}
