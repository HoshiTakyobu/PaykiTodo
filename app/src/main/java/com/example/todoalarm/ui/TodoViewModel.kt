package com.example.todoalarm.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.TodoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class TodoUiState(
    val todayItems: List<TodoItem> = emptyList(),
    val upcomingItems: List<TodoItem> = emptyList(),
    val overdueCount: Int = 0
)

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TodoApplication
    private val repository = app.repository
    private val alarmScheduler = app.alarmScheduler

    val uiState = repository.observeActiveTodos()
        .map { items ->
            val todayEpochDay = LocalDate.now().toEpochDay()
            TodoUiState(
                todayItems = items.filter { it.dueDateEpochDay == todayEpochDay },
                upcomingItems = items.filter { it.dueDateEpochDay != todayEpochDay },
                overdueCount = items.count { it.dueDateEpochDay < todayEpochDay }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodoUiState()
        )

    suspend fun addTodo(
        title: String,
        notes: String,
        dueDate: LocalDate,
        reminderAt: LocalDateTime?,
        ringEnabled: Boolean,
        vibrateEnabled: Boolean,
        voiceEnabled: Boolean
    ): String? {
        if (title.isBlank()) return "标题不能为空"

        val reminderAtMillis = reminderAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        if (reminderAtMillis != null && reminderAtMillis <= System.currentTimeMillis()) {
            return "提醒时间必须晚于当前时间"
        }

        val todoItem = TodoItem(
            title = title.trim(),
            notes = notes.trim(),
            dueDateEpochDay = dueDate.toEpochDay(),
            reminderAtMillis = reminderAtMillis,
            reminderEnabled = reminderAtMillis != null,
            ringEnabled = ringEnabled,
            vibrateEnabled = vibrateEnabled,
            voiceEnabled = voiceEnabled
        )

        val inserted = repository.addTodo(todoItem)
        if (inserted.reminderEnabled) {
            alarmScheduler.schedule(inserted)
        }
        return null
    }

    fun completeTodo(todoItem: TodoItem) {
        viewModelScope.launch {
            repository.setCompleted(todoItem.id, true)
            alarmScheduler.cancel(todoItem.id)
        }
    }
}
