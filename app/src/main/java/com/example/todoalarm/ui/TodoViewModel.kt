package com.example.todoalarm.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.alarm.ActiveReminderStore
import com.example.todoalarm.alarm.ReminderChainLogger
import com.example.todoalarm.alarm.ReminderDispatchTracker
import com.example.todoalarm.alarm.ReminderForegroundService
import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.CalendarEventDraft
import com.example.todoalarm.data.RecurrenceScope
import com.example.todoalarm.data.RecurrenceType
import com.example.todoalarm.data.ReminderChainLog
import com.example.todoalarm.data.ReminderChainStage
import com.example.todoalarm.data.ReminderChainStatus
import com.example.todoalarm.data.ReminderDeliveryMode
import com.example.todoalarm.data.ScheduleTemplate
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.ThemeMode
import com.example.todoalarm.data.WeekStartMode
import com.example.todoalarm.data.TodoDraft
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.RecurrenceConfig
import com.example.todoalarm.data.buildScheduleTemplatePayload
import com.example.todoalarm.data.parseScheduleTemplatePayload
import com.example.todoalarm.data.reminderTriggerTimesMillis
import com.example.todoalarm.data.toDraftsForWeek
import com.example.todoalarm.data.toJsonString
import com.example.todoalarm.data.toWeeklyRecurringDrafts
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
    val calendarItems: List<TodoItem> = emptyList(),
    val reminderChainLogs: List<ReminderChainLog> = emptyList(),
    val scheduleTemplates: List<ScheduleTemplate> = emptyList(),
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
                dispatchDueReminders()
                delay(nextReminderPollDelayMillis())
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
        val activeTaskItems = filteredItems
            .filter { it.isTodo && it.isActive }
            .sortedBy { it.dueAtMillis }
        val activeCalendarItems = items
            .filter { it.isEvent && it.isActive }
            .sortedBy { it.startAtMillis ?: it.dueAtMillis }
        val historyItems = filteredItems
            .filter { it.isTodo && it.isHistory }
            .sortedByDescending { it.completedAtMillis ?: it.canceledAtMillis ?: it.createdAtMillis }
        val availableQuotes = quotes.ifEmpty { QuoteRepository.seedQuotes }
        val currentQuote = availableQuotes[settings.quoteIndex.mod(availableQuotes.size)]

        TodoUiState(
            groups = availableGroups,
            selectedGroupId = selectedGroupId,
            missedItems = activeTaskItems.filter { it.missed },
            todayItems = activeTaskItems.filter { it.hasDueDate && !it.missed && dueDate(it) == today },
            upcomingItems = activeTaskItems.filter { !it.missed && (!it.hasDueDate || dueDate(it).isAfter(today)) },
            historyItems = historyItems,
            calendarItems = activeCalendarItems,
            reminderChainLogs = repository.getRecentReminderChainLogs(),
            scheduleTemplates = repository.getScheduleTemplates(),
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

    suspend fun addCalendarEvent(draft: CalendarEventDraft): String? {
        validateCalendarDraft(
            draft = draft,
            original = null,
            scope = RecurrenceScope.CURRENT
        )?.let { return it }

        val createdItems = repository.createCalendarEventFromDraft(draft)
        for (item in createdItems) {
            scheduleReminderOrDisable(item)
        }
        autoBackupIfEnabled()
        return null
    }

    suspend fun importCalendarEvents(drafts: List<CalendarEventDraft>): String? {
        if (drafts.isEmpty()) return "没有可导入的日程"

        drafts.forEachIndexed { index, draft ->
            validateCalendarDraft(
                draft = draft,
                original = null,
                scope = RecurrenceScope.CURRENT
            )?.let { error ->
                return "第 ${index + 1} 条：$error"
            }
        }

        drafts.forEach { draft ->
            val createdItems = repository.createCalendarEventFromDraft(draft)
            for (item in createdItems) {
                scheduleReminderOrDisable(item)
            }
        }
        autoBackupIfEnabled()
        return null
    }

    suspend fun updateCalendarEvent(
        original: TodoItem,
        draft: CalendarEventDraft,
        scope: RecurrenceScope
    ): String? {
        validateCalendarDraft(
            draft = draft,
            original = original,
            scope = scope
        )?.let { return it }

        val affectedItems = repository.getActiveItemsForScope(original, scope)
        clearReminderArtifacts(affectedItems)

        val updatedItems = repository.updateCalendarEventFromDraft(original, draft, scope)
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
                    missed = todoItem.hasDueDate && todoItem.dueAtMillis < now - MISSED_THRESHOLD_MILLIS,
                    missedAtMillis = if (todoItem.hasDueDate && todoItem.dueAtMillis < now - MISSED_THRESHOLD_MILLIS) now else null,
                    reminderEnabled = todoItem.reminderTriggerTimesMillis().any { it > now }
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

    fun deleteCalendarEvent(todoItem: TodoItem, scope: RecurrenceScope = RecurrenceScope.CURRENT) {
        viewModelScope.launch {
            val deletedItems = repository.deleteCalendarEvent(todoItem, scope)
            clearReminderArtifacts(deletedItems.ifEmpty { listOf(todoItem) })
            autoBackupIfEnabled()
        }
    }

    fun acknowledgeCalendarEvent(todoItem: TodoItem) {
        viewModelScope.launch {
            repository.acknowledgeCalendarEvent(todoItem.id)
            clearReminderArtifacts(listOf(todoItem))
            autoBackupIfEnabled()
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        settingsStore.updateThemeMode(themeMode)
    }

    fun updateWeekStartMode(weekStartMode: WeekStartMode) {
        settingsStore.updateWeekStartMode(weekStartMode)
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

    fun updateDefaultCalendarReminderMode(mode: ReminderDeliveryMode) {
        settingsStore.updateDefaultCalendarReminderMode(mode)
    }

    fun updateReminderTone(uri: String, name: String?) {
        settingsStore.updateReminderTone(uri, name)
    }

    fun useBuiltInReminderTone() {
        settingsStore.useBuiltInReminderTone()
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

    suspend fun runReminderChainTest(delaySeconds: Int = 15): String? {
        val triggerAt = System.currentTimeMillis() + delaySeconds.coerceIn(5, 120) * 1000L
        val dueAt = triggerAt + 60_000L
        val groups = repository.getAllGroups().ifEmpty { repository.ensureDefaultGroups() }
        val created = repository.createFromDraft(
            TodoDraft(
                title = "提醒链路测试",
                notes = "这是一条用于验证提醒派发链路的测试任务。",
                dueAt = Instant.ofEpochMilli(dueAt).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                reminderAt = Instant.ofEpochMilli(triggerAt).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                groupId = groups.firstOrNull { it.name == "例行" }?.id ?: groups.firstOrNull()?.id ?: 0L,
                ringEnabled = true,
                vibrateEnabled = true,
                recurrence = RecurrenceConfig()
            )
        ).firstOrNull() ?: return "测试提醒创建失败"

        ReminderChainLogger.log(
            context = app,
            todoId = created.id,
            source = "TodoViewModel",
            stage = ReminderChainStage.TEST_CREATED,
            status = ReminderChainStatus.OK,
            reminderAtMillis = created.reminderTriggerTimesMillis().minOrNull(),
            message = "delaySeconds=$delaySeconds"
        )
        scheduleReminderOrDisable(created)
        return null
    }

    suspend fun clearReminderDiagnostics() {
        repository.clearReminderChainLogs()
    }

    suspend fun saveWeekAsScheduleTemplate(
        templateName: String,
        templateType: String,
        weekStart: LocalDate
    ): String? {
        if (templateName.isBlank()) return "模板名称不能为空"
        val groups = repository.getAllGroups().ifEmpty { repository.ensureDefaultGroups() }
        val weekEvents = repository.getAllTodos()
            .filter { it.isEvent && it.isActive }
            .filter { overlapsWeek(it, weekStart) }
        if (weekEvents.isEmpty()) return "当前这一周没有可保存的日程"

        val payload = buildScheduleTemplatePayload(
            weekStart = weekStart,
            items = weekEvents,
            groupsById = groups.associateBy { it.id }
        )
        repository.upsertScheduleTemplate(
            ScheduleTemplate(
                name = templateName.trim(),
                templateType = templateType,
                payloadJson = payload.toJsonString(),
                accentColorHex = weekEvents.firstOrNull()?.accentColorHex
            )
        )
        autoBackupIfEnabled()
        return null
    }

    suspend fun applyScheduleTemplateToWeek(
        template: ScheduleTemplate,
        targetWeekStart: LocalDate
    ): String? {
        val payload = runCatching { parseScheduleTemplatePayload(template.payloadJson) }
            .getOrElse { return it.message ?: "模板解析失败" }
        val drafts = payload.toDraftsForWeek(targetWeekStart)
        if (drafts.isEmpty()) return "模板中没有可复制的日程"
        return importCalendarEvents(drafts)
    }

    suspend fun generateSemesterScheduleFromTemplate(
        template: ScheduleTemplate,
        firstWeekStart: LocalDate,
        recurrenceEndDate: LocalDate
    ): String? {
        if (recurrenceEndDate.isBefore(firstWeekStart)) {
            return "学期结束日期不能早于起始周"
        }
        val payload = runCatching { parseScheduleTemplatePayload(template.payloadJson) }
            .getOrElse { return it.message ?: "模板解析失败" }
        val drafts = payload.toWeeklyRecurringDrafts(firstWeekStart, recurrenceEndDate)
        if (drafts.isEmpty()) return "模板中没有可生成的日程"
        return importCalendarEvents(drafts)
    }

    suspend fun deleteScheduleTemplate(templateId: Long): String? {
        repository.deleteScheduleTemplate(templateId)
        autoBackupIfEnabled()
        return null
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
            val existingItems = repository.getAllTodos()
            currentSettings.backupDirectoryUri?.let { directoryUri ->
                app.backupManager.autoBackupToDirectory(directoryUri, currentSnapshot)
            } ?: run {
                app.backupManager.autoBackupToInternalStorage(currentSnapshot)
            }

            clearReminderArtifacts(existingItems)
            ActiveReminderStore.clear(app)
            app.stopService(Intent(app, ReminderForegroundService::class.java))

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
        val dueAtMillis = draft.dueAt?.toEpochMillis()
        if (!isHistory && original == null && dueAtMillis != null && dueAtMillis <= now) {
            return "DDL 必须晚于当前时间"
        }

        val reminderAtMillis = draft.reminderAt?.toEpochMillis()
        if (draft.dueAt == null && reminderAtMillis != null) {
            return "未设置 DDL 的任务不能启用提醒"
        }
        if (!isHistory && original == null && reminderAtMillis != null && reminderAtMillis <= now) {
            return "提醒时间必须晚于当前时间"
        }
        if (reminderAtMillis != null && dueAtMillis != null && reminderAtMillis > dueAtMillis) {
            return "提醒时间不能晚于 DDL"
        }

        val recurrence = draft.recurrence
        if (recurrence.enabled) {
            val dueAt = draft.dueAt ?: return "循环任务必须设置 DDL"
            if (recurrence.type == RecurrenceType.NONE) return "请选择循环规则"
            val endDate = recurrence.endDate ?: return "请设置循环截止日期"
            val canTerminateSeriesEarly = original?.isRecurring == true && scope != RecurrenceScope.CURRENT
            if (!canTerminateSeriesEarly && endDate.isBefore(dueAt.toLocalDate())) {
                return "循环截止日期不能早于首次任务日期"
            }
            if (recurrence.type == RecurrenceType.WEEKLY && recurrence.weeklyDays.isEmpty()) {
                return "每周循环至少选择一天"
            }
        }
        if (original?.isRecurring == true && draft.dueAt == null) {
            return "循环任务必须保留 DDL"
        }
        return null
    }

    private fun validateCalendarDraft(
        draft: CalendarEventDraft,
        original: TodoItem?,
        scope: RecurrenceScope
    ): String? {
        if (draft.title.isBlank()) return "日程标题不能为空"
        val now = System.currentTimeMillis()
        val startMillis = draft.startAt.toEpochMillis()
        val endMillis = if (draft.allDay) {
            draft.endAt.toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else {
            draft.endAt.toEpochMillis()
        }
        if (endMillis <= startMillis) return "结束时间必须晚于开始时间"

        val reminderMinutesBefore = draft.normalizedReminderOffsetsMinutes
        if (reminderMinutesBefore.any { it < 0 }) {
            return "提醒时间不能为负数"
        }
        val reminderAtMillis = draft.reminderTriggerTimesMillis()
        if (reminderAtMillis.any { it <= now }) {
            return "提醒时间必须晚于当前时间"
        }

        val recurrence = draft.recurrence
        if (recurrence.enabled) {
            if (recurrence.type == RecurrenceType.NONE) return "请选择循环规则"
            val endDate = recurrence.endDate ?: return "请设置循环截止日期"
            val canTerminateSeriesEarly = original?.isRecurring == true && scope != RecurrenceScope.CURRENT
            if (!canTerminateSeriesEarly && endDate.isBefore(draft.startAt.toLocalDate())) {
                return "循环截止日期不能早于首次日程日期"
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
        if (item.isTodo && !item.hasDueDate) {
            if (item.reminderEnabled || item.reminderAtMillis != null) {
                ReminderDispatchTracker.clear(app, item.id)
                repository.updateTodo(item.copy(reminderEnabled = false, reminderAtMillis = null, reminderOffsetsCsv = ""))
            }
            return
        }
        if (!item.reminderEnabled) return
        ReminderDispatchTracker.clear(app, item.id)
        val scheduleMessage = alarmScheduler.schedule(item)
        if (scheduleMessage != null) {
            repository.updateTodo(item.copy(reminderEnabled = false))
        }
    }

    private suspend fun dispatchDueReminders() {
        val now = System.currentTimeMillis()
        repository.dueReminderItems(now).forEach { item ->
            val reminderAtMillis = item.reminderTriggerTimesMillis()
                .firstOrNull { it <= now && !ReminderDispatchTracker.wasDispatched(app, item.id, it) }
                ?: return@forEach
            ReminderDispatchTracker.markDispatched(app, item.id, reminderAtMillis)
            ReminderChainLogger.log(
                context = app,
                todoId = item.id,
                source = "TodoViewModel",
                stage = ReminderChainStage.POLL_DISPATCH,
                status = ReminderChainStatus.INFO,
                reminderAtMillis = reminderAtMillis
            )
            ReminderForegroundService.start(app, item.id)
        }
    }

    private suspend fun nextReminderPollDelayMillis(): Long {
        val now = System.currentTimeMillis()
        val nextReminderAt = repository.nextReminderItem()?.reminderTriggerTimesMillis()?.filter { it >= now }?.minOrNull()
            ?: return 15_000L
        val untilNext = nextReminderAt - now
        return when {
            untilNext <= 1_500L -> 1_000L
            untilNext <= 10_000L -> 2_000L
            untilNext <= 60_000L -> 5_000L
            else -> 15_000L
        }
    }

    private fun clearReminderArtifacts(items: List<TodoItem>) {
        items.forEach { item ->
            alarmScheduler.cancel(item.id)
            reminderNotifier.cancel(item.id)
            ReminderDispatchTracker.clear(app, item.id)
            ActiveReminderStore.clearIfMatches(app, item.id)
        }
    }

    private fun dueDate(item: TodoItem): LocalDate {
        return item.dueDate()
    }

    private fun overlapsWeek(item: TodoItem, weekStart: LocalDate): Boolean {
        val startMillis = item.startAtMillis ?: item.dueAtMillis
        val endMillis = item.endAtMillis ?: startMillis
        val eventStart = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val eventEnd = Instant.ofEpochMilli(endMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val weekEnd = weekStart.plusDays(6)
        return !eventEnd.isBefore(weekStart) && !eventStart.isAfter(weekEnd)
    }

    companion object {
        private const val MISSED_THRESHOLD_MILLIS = 60_000L
    }
}
