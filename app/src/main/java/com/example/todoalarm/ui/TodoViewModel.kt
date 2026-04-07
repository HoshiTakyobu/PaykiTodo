package com.example.todoalarm.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.alarm.ActiveReminderStore
import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.RecurrenceScope
import com.example.todoalarm.data.RecurrenceType
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.ThemeMode
import com.example.todoalarm.data.TodoDraft
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.toEpochMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TodoUiState(
    val groups: List<TaskGroup> = emptyList(),
    val selectedGroupId: Long? = null,
    val missedItems: List<TodoItem> = emptyList(),
    val todayItems: List<TodoItem> = emptyList(),
    val upcomingItems: List<TodoItem> = emptyList(),
    val historyItems: List<TodoItem> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val currentQuote: String = QuoteRepository.seedQuotes.first()
)

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TodoApplication
    private val repository = app.repository
    private val alarmScheduler = app.alarmScheduler
    private val reminderNotifier = app.reminderNotifier
    private val settingsStore = app.settingsStore
    private val quoteRepository = app.quoteRepository

    private val quoteFlow = MutableStateFlow(QuoteRepository.seedQuotes)
    private val selectedGroupIdFlow = MutableStateFlow<Long?>(null)
    private var quoteRefreshJob: Job? = null

    init {
        viewModelScope.launch {
            val localQuotes = quoteRepository.loadQuotes()
            if (localQuotes.isNotEmpty()) {
                quoteFlow.value = localQuotes
            }
            refreshQuotesIfNeeded(force = false)
        }
        viewModelScope.launch {
            while (true) {
                repository.markMissedTasks()
                delay(15_000L)
            }
        }
    }

    val uiState = combine(
        repository.observeTodos(),
        repository.observeGroups(),
        settingsStore.settingsFlow,
        quoteFlow,
        selectedGroupIdFlow
    ) { items, groups, settings, quotes, selectedGroupId ->
        val nowMillis = System.currentTimeMillis()
        val today = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val availableGroups = if (groups.isEmpty()) repository.ensureDefaultGroups() else groups
        val filteredItems = if (selectedGroupId == null) {
            items
        } else {
            items.filter { it.groupId == selectedGroupId }
        }
        val activeItems = filteredItems
            .filter { it.isActive }
            .sortedBy { it.dueAtMillis }
        val historyItems = filteredItems
            .filter { it.isHistory }
            .sortedByDescending { it.completedAtMillis ?: it.canceledAtMillis ?: it.createdAtMillis }
        val availableQuotes = quotes.ifEmpty { QuoteRepository.seedQuotes }
        val currentQuote = availableQuotes[settings.quoteIndex.mod(availableQuotes.size)]

        TodoUiState(
            groups = availableGroups,
            selectedGroupId = selectedGroupId,
            missedItems = activeItems.filter { it.missed },
            todayItems = activeItems.filter { !it.missed && dueDate(it) == today },
            upcomingItems = activeItems.filter { !it.missed && dueDate(it).isAfter(today) },
            historyItems = historyItems,
            settings = settings,
            currentQuote = currentQuote
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodoUiState()
    )

    fun refreshTaskStates() {
        viewModelScope.launch {
            repository.ensureDefaultGroups()
            repository.markMissedTasks()
        }
    }

    fun selectGroup(groupId: Long?) {
        selectedGroupIdFlow.value = groupId
    }

    suspend fun addTodo(draft: TodoDraft): String? {
        validateDraft(
            draft = draft,
            original = null,
            scope = RecurrenceScope.CURRENT
        )?.let { return it }

        val createdItems = repository.createFromDraft(draft)
        for (item in createdItems) {
            scheduleReminderOrDisable(item)
        }
        autoBackupIfEnabled()
        return null
    }

    suspend fun updateTodo(
        original: TodoItem,
        draft: TodoDraft,
        scope: RecurrenceScope
    ): String? {
        validateDraft(
            draft = draft,
            original = original,
            scope = scope
        )?.let { return it }

        val affectedItems = repository.getActiveItemsForScope(original, scope)
        clearReminderArtifacts(affectedItems)

        val updatedItems = repository.updateFromDraft(original, draft, scope)
        for (item in updatedItems) {
            scheduleReminderOrDisable(item)
        }
        autoBackupIfEnabled()
        return null
    }

    fun completeTodo(todoItem: TodoItem) {
        viewModelScope.launch {
            repository.setCompleted(todoItem.id, true)
            clearReminderArtifacts(listOf(todoItem))
            autoBackupIfEnabled()
        }
    }

    fun cancelTodo(todoItem: TodoItem, scope: RecurrenceScope = RecurrenceScope.CURRENT) {
        viewModelScope.launch {
            val canceledItems = repository.cancelTodo(todoItem, scope)
            clearReminderArtifacts(canceledItems)
            autoBackupIfEnabled()
        }
    }

    fun restoreTodo(todoItem: TodoItem) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val restored = repository.updateTodo(
                todoItem.copy(
                    completed = false,
                    completedAtMillis = null,
                    canceled = false,
                    canceledAtMillis = null,
                    missed = todoItem.dueAtMillis < now - MISSED_THRESHOLD_MILLIS,
                    missedAtMillis = if (todoItem.dueAtMillis < now - MISSED_THRESHOLD_MILLIS) now else null,
                    reminderEnabled = todoItem.reminderAtMillis?.let { it > now } == true
                )
            )
            scheduleReminderOrDisable(restored)
            autoBackupIfEnabled()
        }
    }

    fun deleteTodo(todoItem: TodoItem) {
        viewModelScope.launch {
            repository.deleteTodo(todoItem.id)
            clearReminderArtifacts(listOf(todoItem))
            autoBackupIfEnabled()
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        settingsStore.updateThemeMode(themeMode)
    }

    fun showNextQuote() {
        val availableQuotes = quoteFlow.value.ifEmpty { QuoteRepository.seedQuotes }
        val nextIndex = (settingsStore.currentSettings().quoteIndex + 1).mod(availableQuotes.size)
        settingsStore.updateQuoteIndex(nextIndex)
        refreshQuotesIfNeeded(force = false)
    }

    fun updateDefaultSnooze(minutes: Int) {
        settingsStore.updateDefaultSnooze(minutes)
    }

    suspend fun createGroup(name: String, colorHex: String): String? {
        if (name.isBlank()) return "分组名称不能为空"
        repository.createGroup(name, colorHex)
        autoBackupIfEnabled()
        return null
    }

    suspend fun updateGroup(group: TaskGroup): String? {
        if (group.name.isBlank()) return "分组名称不能为空"
        repository.updateGroup(group.copy(name = group.name.trim()))
        autoBackupIfEnabled()
        return null
    }

    suspend fun deleteGroup(groupId: Long): String? {
        val deleted = repository.deleteGroup(groupId)
        if (!deleted) return "该分组下仍有关联任务，暂时不能直接删除"
        if (selectedGroupIdFlow.value == groupId) {
            selectedGroupIdFlow.value = null
        }
        autoBackupIfEnabled()
        return null
    }

    fun updateBackupDirectoryUri(uri: String?) {
        settingsStore.updateBackupDirectoryUri(uri)
    }

    fun updateAutoBackupEnabled(enabled: Boolean) {
        settingsStore.updateAutoBackupEnabled(enabled)
    }

    suspend fun exportBackupNow(targetUri: Uri): String? {
        return runCatching {
            val snapshot = repository.exportSnapshot(settingsStore.currentSettings())
            app.backupManager.exportToUri(targetUri, snapshot)
            "导出完成"
        }.getOrElse { it.message ?: "导出失败" }
    }

    suspend fun importBackup(sourceUri: Uri): String? {
        return runCatching {
            val currentSettings = settingsStore.currentSettings()
            val currentSnapshot = repository.exportSnapshot(currentSettings)
            currentSettings.backupDirectoryUri?.let { directoryUri ->
                app.backupManager.autoBackupToDirectory(directoryUri, currentSnapshot)
            } ?: run {
                app.backupManager.autoBackupToInternalStorage(currentSnapshot)
            }

            val snapshot = app.backupManager.importFromUri(sourceUri)
            repository.importSnapshot(snapshot)
            settingsStore.replaceAll(snapshot.settings)
            selectedGroupIdFlow.value = null
            refreshTaskStates()
            repository.futureReminderItems(System.currentTimeMillis()).forEach(app.alarmScheduler::schedule)
            "导入完成"
        }.getOrElse { it.message ?: "导入失败" }
    }

    private fun refreshQuotesIfNeeded(force: Boolean) {
        if (quoteRefreshJob?.isActive == true) return
        quoteRefreshJob = viewModelScope.launch {
            quoteRepository.refreshQuotesIfNeeded(force)?.let { refreshedQuotes ->
                if (refreshedQuotes.isNotEmpty()) {
                    quoteFlow.value = refreshedQuotes
                }
            }
        }
    }

    private fun validateDraft(
        draft: TodoDraft,
        original: TodoItem?,
        scope: RecurrenceScope
    ): String? {
        if (draft.title.isBlank()) return "标题不能为空"

        val isHistory = original?.isHistory == true
        val now = System.currentTimeMillis()
        val dueAtMillis = draft.dueAt.toEpochMillis()
        if (!isHistory && original == null && dueAtMillis <= now) {
            return "DDL 必须晚于当前时间"
        }

        val reminderAtMillis = draft.reminderAt?.toEpochMillis()
        if (!isHistory && original == null && reminderAtMillis != null && reminderAtMillis <= now) {
            return "提醒时间必须晚于当前时间"
        }
        if (reminderAtMillis != null && reminderAtMillis > dueAtMillis) {
            return "提醒时间不能晚于 DDL"
        }

        val recurrence = draft.recurrence
        if (recurrence.enabled) {
            if (recurrence.type == RecurrenceType.NONE) return "请选择循环规则"
            val endDate = recurrence.endDate ?: return "请设置循环截止日期"
            val canTerminateSeriesEarly = original?.isRecurring == true && scope != RecurrenceScope.CURRENT
            if (!canTerminateSeriesEarly && endDate.isBefore(draft.dueAt.toLocalDate())) {
                return "循环截止日期不能早于首次任务日期"
            }
            if (recurrence.type == RecurrenceType.WEEKLY && recurrence.weeklyDays.isEmpty()) {
                return "每周循环至少选择一天"
            }
        }
        return null
    }

    private suspend fun autoBackupIfEnabled() {
        val settings = settingsStore.currentSettings()
        if (!settings.autoBackupEnabled) return
        val directoryUri = settings.backupDirectoryUri ?: return
        runCatching {
            val snapshot = repository.exportSnapshot(settings)
            app.backupManager.autoBackupToDirectory(directoryUri, snapshot)
        }
    }

    private suspend fun scheduleReminderOrDisable(item: TodoItem) {
        if (!item.reminderEnabled) return
        val scheduleMessage = alarmScheduler.schedule(item)
        if (scheduleMessage != null) {
            repository.updateTodo(item.copy(reminderEnabled = false))
        }
    }

    private fun clearReminderArtifacts(items: List<TodoItem>) {
        items.forEach { item ->
            alarmScheduler.cancel(item.id)
            reminderNotifier.cancel(item.id)
            ActiveReminderStore.clearIfMatches(app, item.id)
        }
    }

    private fun dueDate(item: TodoItem): LocalDate {
        return Instant.ofEpochMilli(item.dueAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    companion object {
        private const val MISSED_THRESHOLD_MILLIS = 60_000L
    }
}
