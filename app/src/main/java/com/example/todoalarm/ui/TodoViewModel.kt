package com.example.todoalarm.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.ThemeMode
import com.example.todoalarm.data.TodoCategory
import com.example.todoalarm.data.TodoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class TodoUiState(
    val todayItems: List<TodoItem> = emptyList(),
    val upcomingItems: List<TodoItem> = emptyList(),
    val completedItems: List<TodoItem> = emptyList(),
    val overdueCount: Int = 0,
    val settings: AppSettings = AppSettings()
)

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TodoApplication
    private val repository = app.repository
    private val alarmScheduler = app.alarmScheduler
    private val reminderNotifier = app.reminderNotifier
    private val settingsStore = app.settingsStore

    val uiState = combine(repository.observeTodos(), settingsStore.settingsFlow) { items, settings ->
        val nowMillis = System.currentTimeMillis()
        val today = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val activeItems = items.filterNot { it.completed }.sortedBy { it.dueAtMillis }
        val completedItems = items.filter { it.completed }.sortedByDescending { it.completedAtMillis ?: it.createdAtMillis }

        TodoUiState(
            todayItems = activeItems.filter { dueDate(it) == today },
            upcomingItems = activeItems.filter { dueDate(it) != today },
            completedItems = completedItems,
            overdueCount = activeItems.count { it.dueAtMillis < nowMillis },
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodoUiState()
    )

    suspend fun addTodo(
        title: String,
        notes: String,
        dueAt: LocalDateTime,
        reminderAt: LocalDateTime?,
        category: TodoCategory,
        ringEnabled: Boolean,
        vibrateEnabled: Boolean,
        voiceEnabled: Boolean
    ): String? {
        if (title.isBlank()) return "标题不能为空"

        val now = System.currentTimeMillis()
        val dueAtMillis = dueAt.toEpochMillis()
        if (dueAtMillis <= now) return "DDL 必须晚于当前时间"

        val reminderAtMillis = reminderAt?.toEpochMillis()
        if (reminderAtMillis != null && reminderAtMillis <= now) return "提醒时间必须晚于当前时间"

        val todoItem = TodoItem(
            title = title.trim(),
            notes = notes.trim(),
            dueAtMillis = dueAtMillis,
            reminderAtMillis = reminderAtMillis,
            reminderEnabled = reminderAtMillis != null,
            ringEnabled = ringEnabled,
            vibrateEnabled = vibrateEnabled,
            voiceEnabled = voiceEnabled,
            categoryKey = category.key
        )

        val inserted = repository.addTodo(todoItem)
        if (inserted.reminderEnabled) alarmScheduler.schedule(inserted)
        return null
    }

    suspend fun updateTodo(
        original: TodoItem,
        title: String,
        notes: String,
        dueAt: LocalDateTime,
        reminderAt: LocalDateTime?,
        category: TodoCategory,
        ringEnabled: Boolean,
        vibrateEnabled: Boolean,
        voiceEnabled: Boolean
    ): String? {
        if (title.isBlank()) return "标题不能为空"

        val now = System.currentTimeMillis()
        val allowPastTime = original.completed
        val dueAtMillis = dueAt.toEpochMillis()
        if (!allowPastTime && dueAtMillis <= now) return "DDL 必须晚于当前时间"

        val reminderAtMillis = reminderAt?.toEpochMillis()
        if (!allowPastTime && reminderAtMillis != null && reminderAtMillis <= now) return "提醒时间必须晚于当前时间"

        val updated = repository.updateTodo(
            original.copy(
                title = title.trim(),
                notes = notes.trim(),
                dueAtMillis = dueAtMillis,
                reminderAtMillis = if (original.completed) null else reminderAtMillis,
                reminderEnabled = if (original.completed) false else reminderAtMillis != null,
                ringEnabled = ringEnabled,
                vibrateEnabled = vibrateEnabled,
                voiceEnabled = voiceEnabled,
                categoryKey = category.key
            )
        )

        if (updated.completed || !updated.reminderEnabled) {
            alarmScheduler.cancel(updated.id)
        } else {
            alarmScheduler.schedule(updated)
        }
        return null
    }

    fun completeTodo(todoItem: TodoItem) {
        viewModelScope.launch {
            repository.setCompleted(todoItem.id, true)
            alarmScheduler.cancel(todoItem.id)
            reminderNotifier.cancel(todoItem.id)
        }
    }

    fun restoreTodo(todoItem: TodoItem) {
        viewModelScope.launch {
            val restored = repository.setCompleted(todoItem.id, false)
            if (restored?.reminderEnabled == true) alarmScheduler.schedule(restored)
        }
    }

    fun deleteTodo(todoItem: TodoItem) {
        viewModelScope.launch {
            repository.deleteTodo(todoItem.id)
            alarmScheduler.cancel(todoItem.id)
            reminderNotifier.cancel(todoItem.id)
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        settingsStore.updateThemeMode(themeMode)
    }

    fun updateDefaultSnooze(minutes: Int) {
        settingsStore.updateDefaultSnooze(minutes)
    }

    private fun dueDate(item: TodoItem): LocalDate {
        return Instant.ofEpochMilli(item.dueAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }
}

private fun LocalDateTime.toEpochMillis(): Long {
    return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
