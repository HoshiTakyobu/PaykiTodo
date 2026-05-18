package com.example.todoalarm.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.alarm.ActiveReminderStore
import com.example.todoalarm.alarm.DailyReportScheduler
import com.example.todoalarm.alarm.ReminderChainLogger
import com.example.todoalarm.alarm.ReminderDispatchTracker
import com.example.todoalarm.alarm.ReminderForegroundService
import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.AiReport
import com.example.todoalarm.data.AiReportRetention
import com.example.todoalarm.data.AiReportType
import com.example.todoalarm.data.CalendarEventDraft
import com.example.todoalarm.data.DEFAULT_PLANNING_REMINDER_MINUTES
import com.example.todoalarm.data.DailyReportGenerator
import com.example.todoalarm.data.DailyBoardSnapshotBuilder
import com.example.todoalarm.data.EventCheckIn
import com.example.todoalarm.data.PlanningAnnouncement
import com.example.todoalarm.data.PlanningAnnouncementParser
import com.example.todoalarm.data.PlanningImportCandidate
import com.example.todoalarm.data.PlanningImportResult
import com.example.todoalarm.data.PlanningLineMapping
import com.example.todoalarm.data.PlanningLineMatcher
import com.example.todoalarm.data.PlanningMarkdownParser
import com.example.todoalarm.data.PlanningNote
import com.example.todoalarm.data.PlanningParseResult
import com.example.todoalarm.data.PlanningParsedType
import com.example.todoalarm.data.PlanningPostponeScope
import com.example.todoalarm.data.PlanningRefreshScope
import com.example.todoalarm.data.PlanningOperationResult
import com.example.todoalarm.data.PlanningRecognitionService
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
import com.example.todoalarm.sync.DesktopSyncStatus
import com.example.todoalarm.sync.DesktopSyncService
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

data class TodoUiState(
    val groups: List<TaskGroup> = emptyList(),
    val selectedGroupIds: Set<Long> = emptySet(),
    val missedItems: List<TodoItem> = emptyList(),
    val todayItems: List<TodoItem> = emptyList(),
    val upcomingItems: List<TodoItem> = emptyList(),
    val calendarItems: List<TodoItem> = emptyList(),
    val countdownItems: List<TodoItem> = emptyList(),
    val activeAnnouncements: List<PlanningAnnouncement> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val currentQuote: String = QuoteRepository.seedQuotes.first(),
    val dataReady: Boolean = false,
    val desktopSyncStatus: DesktopSyncStatus = DesktopSyncStatus(
        enabled = false,
        running = false,
        port = 18765,
        token = "",
        ipAddresses = emptyList()
    )
)

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TodoApplication
    private val repository = app.repository
    private val alarmScheduler = app.alarmScheduler
    private val reminderNotifier = app.reminderNotifier
    private val settingsStore = app.settingsStore
    private val quoteRepository = app.quoteRepository

    private val quoteFlow = MutableStateFlow(QuoteRepository.seedQuotes)
    private val selectedGroupIdsFlow = MutableStateFlow<Set<Long>>(emptySet())
    private val desktopSyncRefreshTick = MutableStateFlow(0L)
    private val todayDateFlow = MutableStateFlow(LocalDate.now())
    private val calendarEventWindowFlow = MutableStateFlow(calendarEventWindowAround(LocalDate.now()))
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
            val note = repository.ensureDefaultPlanningNote()
            if (settingsStore.currentSettings().lastOpenedPlanningNoteId == null) {
                settingsStore.updateLastOpenedPlanningNoteId(note.id)
            }
        }
        viewModelScope.launch {
            while (true) {
                repository.markMissedTasks()
                dispatchDueReminders()
                delay(nextReminderPollDelayMillis())
            }
        }
        viewModelScope.launch {
            while (true) {
                todayDateFlow.value = LocalDate.now()
                delay(nextDateTickDelayMillis())
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeTodoItems = selectedGroupIdsFlow
        .flatMapLatest { groupIds -> repository.observeActiveTodoItems(groupIds) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val boardCalendarItems = todayDateFlow
        .flatMapLatest { date ->
            val (startMillis, endMillis) = boardEventRangeMillis(date)
            repository.observeActiveCalendarEventsInRange(startMillis, endMillis)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val countdownItems = todayDateFlow
        .flatMapLatest { date ->
            val (startMillis, _) = dayRangeMillis(date)
            repository.observeActiveCountdownItems(startMillis)
        }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyItems = selectedGroupIdsFlow
        .flatMapLatest { groupIds -> repository.observeHistoryTodoItems(groupIds) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val calendarItems = calendarEventWindowFlow
        .flatMapLatest { window ->
            val zone = ZoneId.systemDefault()
            repository.observeActiveCalendarEventsInRange(
                rangeStartMillis = window.startInclusive.atStartOfDay(zone).toInstant().toEpochMilli(),
                rangeEndMillis = window.endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val scheduleTemplates = repository.observeScheduleTemplates().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val reminderChainLogs = repository.observeRecentReminderChainLogs().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val planningNotes = repository.observePlanningNotes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val uiState = combine(
        repository.observeGroups(),
        repository.observePlanningNotesWithAnnouncementHints(),
        activeTodoItems,
        boardCalendarItems,
        countdownItems,
        settingsStore.settingsFlow,
        quoteFlow,
        selectedGroupIdsFlow,
        desktopSyncRefreshTick,
        todayDateFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val groups = values[0] as List<TaskGroup>
        @Suppress("UNCHECKED_CAST")
        val announcementNotes = values[1] as List<PlanningNote>
        @Suppress("UNCHECKED_CAST")
        val activeTaskItems = values[2] as List<TodoItem>
        @Suppress("UNCHECKED_CAST")
        val activeCalendarItems = values[3] as List<TodoItem>
        @Suppress("UNCHECKED_CAST")
        val activeCountdownItems = values[4] as List<TodoItem>
        val settings = values[5] as AppSettings
        @Suppress("UNCHECKED_CAST")
        val quotes = values[6] as List<String>
        @Suppress("UNCHECKED_CAST")
        val selectedGroupIds = values[7] as Set<Long>
        val today = values[9] as LocalDate
        val availableGroups = if (groups.isEmpty()) repository.ensureDefaultGroups() else groups
        val todoSections = classifyActiveTodoItems(activeTaskItems, today)
        val sortedCalendarItems = activeCalendarItems.sortedBy { it.startAtMillis ?: it.dueAtMillis }
        val availableQuotes = quotes.ifEmpty { QuoteRepository.seedQuotes }
        val currentQuote = availableQuotes[settings.quoteIndex.mod(availableQuotes.size)]
        val nowMillis = System.currentTimeMillis()

        TodoUiState(
            groups = availableGroups,
            selectedGroupIds = selectedGroupIds,
            missedItems = todoSections.missedItems,
            todayItems = todoSections.todayItems,
            upcomingItems = todoSections.upcomingItems,
            calendarItems = sortedCalendarItems,
            countdownItems = activeCountdownItems
                .filter { item -> DailyBoardSnapshotBuilder.countdownTargetMillis(item)?.let { it >= nowMillis } == true }
                .sortedBy { DailyBoardSnapshotBuilder.countdownTargetMillis(it) ?: Long.MAX_VALUE },
            activeAnnouncements = PlanningAnnouncementParser.activeAnnouncements(announcementNotes, today),
            settings = settings,
            currentQuote = currentQuote,
            dataReady = true,
            desktopSyncStatus = app.desktopSyncCoordinator.status()
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
        selectedGroupIdsFlow.value = if (groupId == null) {
            emptySet()
        } else {
            val current = selectedGroupIdsFlow.value
            if (groupId in current) current - groupId else current + groupId
        }
    }

    fun updateCalendarEventWindow(startInclusive: LocalDate, endExclusive: LocalDate) {
        if (!startInclusive.isBefore(endExclusive)) return
        val padded = CalendarEventWindow(
            startInclusive = startInclusive.minusDays(CalendarWindowPaddingDays),
            endExclusive = endExclusive.plusDays(CalendarWindowPaddingDays)
        )
        if (calendarEventWindowFlow.value != padded) {
            calendarEventWindowFlow.value = padded
        }
    }

    suspend fun getTodoById(todoId: Long): TodoItem? {
        return withContext(Dispatchers.IO) { repository.getTodo(todoId) }
    }

    suspend fun getTodoGroupIds(todoId: Long): List<Long> {
        return withContext(Dispatchers.IO) { repository.getGroupIdsForTodo(todoId) }
    }

    suspend fun getEventCheckIns(eventId: Long): List<EventCheckIn> {
        return withContext(Dispatchers.IO) { repository.getCheckInsForEvent(eventId) }
    }

    suspend fun checkInCalendarEvent(eventId: Long): String? {
        val checkIn = withContext(Dispatchers.IO) { repository.checkInEvent(eventId) }
        if (checkIn == null) return "无法签到：请确认日程未结束且已开启打卡追踪"
        autoBackupIfEnabled()
        return null
    }

    suspend fun checkOutCalendarEvent(eventId: Long): String? {
        val checkOut = withContext(Dispatchers.IO) { repository.checkOutEvent(eventId) }
        if (checkOut == null) return "当前没有进行中的签到"
        autoBackupIfEnabled()
        return null
    }

    fun observeAiReports(
        type: AiReportType?,
        limit: Int,
        query: String = "",
        startMillis: Long = Long.MIN_VALUE,
        endMillis: Long = Long.MAX_VALUE
    ): Flow<List<AiReport>> {
        val safeLimit = limit.coerceIn(1, 300)
        val safeQuery = query.trim().take(80)
        return if (safeQuery.isBlank() && startMillis == Long.MIN_VALUE && endMillis == Long.MAX_VALUE) {
            if (type == null) {
            repository.observeAiReports(safeLimit)
        } else {
            repository.observeAiReportsByType(type, safeLimit)
            }
        } else {
            repository.observeAiReportsFiltered(
                type = type,
                query = safeQuery,
                startMillis = startMillis,
                endMillis = endMillis,
                limit = safeLimit
            )
        }
    }

    suspend fun getAiReportById(reportId: Long): AiReport? {
        return withContext(Dispatchers.IO) { repository.getAiReportById(reportId) }
    }

    suspend fun parsePlanningMarkdown(markdown: String): PlanningParseResult {
        return PlanningRecognitionService.recognize(markdown = markdown, settings = settingsStore.currentSettings())
    }

    fun selectPlanningNote(noteId: Long) {
        settingsStore.updateLastOpenedPlanningNoteId(noteId)
    }

    suspend fun createPlanningNote(title: String): String? {
        val note = repository.createPlanningNote(title)
        settingsStore.updateLastOpenedPlanningNoteId(note.id)
        autoBackupIfEnabled()
        return null
    }

    suspend fun savePlanningNoteContent(noteId: Long, contentMarkdown: String): String? {
        val updated = repository.updatePlanningNoteContent(noteId, contentMarkdown) ?: return "规划文档不存在"
        settingsStore.updateLastOpenedPlanningNoteId(updated.id)
        autoBackupIfEnabled()
        return null
    }

    suspend fun renamePlanningNote(noteId: Long, title: String): String? {
        if (title.isBlank()) return "文档名称不能为空"
        repository.renamePlanningNote(noteId, title) ?: return "规划文档不存在"
        autoBackupIfEnabled()
        return null
    }

    suspend fun deletePlanningNote(noteId: Long): String? {
        repository.deletePlanningNote(noteId)
        val fallback = repository.ensureDefaultPlanningNote()
        settingsStore.updateLastOpenedPlanningNoteId(fallback.id)
        autoBackupIfEnabled()
        return null
    }

    suspend fun archivePlanningNote(noteId: Long): String? {
        repository.archivePlanningNote(noteId) ?: return "规划文档不存在"
        val fallback = repository.ensureDefaultPlanningNote()
        settingsStore.updateLastOpenedPlanningNoteId(fallback.id)
        autoBackupIfEnabled()
        return null
    }

    suspend fun importPlanningCandidates(
        candidates: List<PlanningImportCandidate>,
        selectedIds: Set<String>,
        currentMarkdown: String,
        activeNoteId: Long?
    ): PlanningImportResult {
        val selected = candidates.filter { it.id in selectedIds && it.importable }
        if (selected.isEmpty()) return PlanningImportResult(message = "没有可导入的规划条目")
        val groups = repository.getAllGroups().ifEmpty { repository.ensureDefaultGroups() }

        selected.forEachIndexed { index, candidate ->
            candidate.validate()?.let { return PlanningImportResult(message = "第 ${index + 1} 条：$it") }
            when (candidate.type) {
                PlanningParsedType.TODO -> validateDraft(candidate.toTodoDraft(groups), null, RecurrenceScope.CURRENT)
                PlanningParsedType.EVENT -> validateCalendarDraft(candidate.toEventDraft(groups), null, RecurrenceScope.CURRENT)
                else -> null
            }?.let { return PlanningImportResult(message = "第 ${index + 1} 条：$it") }
        }

        val batchId = UUID.randomUUID().toString()
        val importAtMillis = System.currentTimeMillis()
        val markdownLines = planningDocumentLines(currentMarkdown)
        val mappings = mutableListOf<PlanningLineMapping>()
        selected.forEach { candidate ->
            val sourceLine = markdownLines.getOrNull(candidate.lineNumber - 1) ?: candidate.sourceLine
            when (candidate.type) {
                PlanningParsedType.TODO -> {
                    val created = repository.createFromDraft(candidate.toTodoDraft(groups))
                    created.forEach { scheduleReminderOrDisable(it) }
                    mappings += created.mapNotNull { item ->
                        candidate.toPlanningLineMapping(
                            noteId = activeNoteId,
                            item = item,
                            sourceLine = sourceLine,
                            batchId = batchId,
                            timestamp = importAtMillis
                        )
                    }
                }
                PlanningParsedType.EVENT -> {
                    val eventDraft = candidate.toEventDraft(groups)
                    val createdEvents = repository.createCalendarEventFromDraft(eventDraft)
                    createdEvents.forEach { scheduleReminderOrDisable(it) }
                    mappings += createdEvents.mapNotNull { item ->
                        candidate.toPlanningLineMapping(
                            noteId = activeNoteId,
                            item = item,
                            sourceLine = sourceLine,
                            batchId = batchId,
                            timestamp = importAtMillis
                        )
                    }
                    if (candidate.createLinkedTodo) {
                        val linked = repository.createFromDraft(candidate.toLinkedTodoDraft(groups))
                        linked.forEach { scheduleReminderOrDisable(it) }
                        mappings += linked.mapNotNull { item ->
                            candidate.toPlanningLineMapping(
                                noteId = activeNoteId,
                                item = item,
                                sourceLine = sourceLine,
                                batchId = batchId,
                                timestamp = importAtMillis
                            )
                        }
                    }
                }
                else -> Unit
            }
        }
        val updatedMarkdown = PlanningMarkdownParser.markImportedLines(currentMarkdown, selected.map { it.lineNumber }.toSet())
        if (activeNoteId != null) {
            repository.insertPlanningMappings(mappings)
            repository.updatePlanningNoteContent(activeNoteId, updatedMarkdown)
            settingsStore.updateLastOpenedPlanningNoteId(activeNoteId)
        }
        autoBackupIfEnabled()
        return PlanningImportResult(importedCount = selected.size, updatedMarkdown = updatedMarkdown)
    }

    suspend fun syncPlanningMappings(noteId: Long, markdown: String): List<PlanningLineMapping> {
        return repository.syncPlanningMappingStatuses(noteId, markdown).mappings
    }

    suspend fun getPlanningMappings(noteId: Long): List<PlanningLineMapping> {
        return repository.getPlanningMappingsForNote(noteId)
    }

    suspend fun refreshPlanningImportedItems(
        noteId: Long,
        markdown: String,
        scope: PlanningRefreshScope,
        cursorLineNumber: Int?
    ): PlanningOperationResult {
        val result = repository.refreshPlanningImportedItems(
            noteId = noteId,
            markdown = markdown,
            wholeDocument = scope == PlanningRefreshScope.WHOLE_DOCUMENT,
            cursorLineNumber = cursorLineNumber
        )
        reschedulePlanningOperationItems(result)
        autoBackupIfEnabled()
        return result
    }

    suspend fun postponePlanningImportedItems(
        noteId: Long,
        markdown: String,
        startMappingId: Long?,
        offsetMinutes: Int,
        scope: PlanningPostponeScope
    ): PlanningOperationResult {
        val result = repository.postponePlanningImportedItems(
            noteId = noteId,
            markdown = markdown,
            startMappingId = startMappingId,
            offsetMinutes = offsetMinutes,
            scope = scope
        )
        reschedulePlanningOperationItems(result)
        autoBackupIfEnabled()
        return result
    }

    suspend fun undoLastPlanningOperation(noteId: Long, markdown: String): PlanningOperationResult {
        val result = repository.undoLastPlanningOperation(noteId, markdown)
        clearReminderArtifacts(result.affectedBeforeItems)
        result.affectedAfterItems.forEach { scheduleReminderOrDisable(it) }
        autoBackupIfEnabled()
        return result
    }

    suspend fun applyPlanningConflictDocument(noteId: Long, markdown: String, mappingId: Long): PlanningOperationResult {
        val result = repository.resolvePlanningConflictWithDocument(noteId, markdown, mappingId)
        reschedulePlanningOperationItems(result)
        autoBackupIfEnabled()
        return result
    }

    suspend fun applyPlanningConflictItem(noteId: Long, markdown: String, mappingId: Long): PlanningOperationResult {
        val result = repository.resolvePlanningConflictWithItem(noteId, markdown, mappingId)
        autoBackupIfEnabled()
        return result
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

    suspend fun importTodos(drafts: List<TodoDraft>): String? {
        if (drafts.isEmpty()) return "没有可导入的待办"

        drafts.forEachIndexed { index, draft ->
            validateDraft(
                draft = draft,
                original = null,
                scope = RecurrenceScope.CURRENT
            )?.let { error ->
                return "第 ${index + 1} 条：$error"
            }
        }

        drafts.forEach { draft ->
            val createdItems = repository.createFromDraft(draft)
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

    fun updateReminderAudioStrategy(
        channel: com.example.todoalarm.data.ReminderAudioChannel,
        internalVolumePercent: Int,
        boostSystemVolume: Boolean,
        boostVolumePercent: Int,
        workQuietModeEnabled: Boolean
    ) {
        settingsStore.updateReminderAudioStrategy(
            channel = channel,
            internalVolumePercent = internalVolumePercent,
            boostSystemVolume = boostSystemVolume,
            boostVolumePercent = boostVolumePercent,
            workQuietModeEnabled = workQuietModeEnabled
        )
    }

    fun updatePlanningAiConfig(
        enabled: Boolean,
        providerName: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ) {
        settingsStore.updatePlanningAiConfig(
            enabled = enabled,
            providerName = providerName,
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model
        )
    }

    fun updatePlanningAiProviders(
        enabled: Boolean,
        providers: List<com.example.todoalarm.data.PlanningAiProvider>
    ) {
        settingsStore.updatePlanningAiProviders(enabled, providers)
    }

    fun updateReportPreferences(
        dailyEnabled: Boolean,
        dailyHour: Int,
        dailyMinute: Int,
        weeklyEnabled: Boolean,
        weeklyHour: Int,
        weeklyMinute: Int,
        retention: AiReportRetention
    ) {
        settingsStore.updateReportPreferences(
            dailyEnabled = dailyEnabled,
            dailyHour = dailyHour,
            dailyMinute = dailyMinute,
            weeklyEnabled = weeklyEnabled,
            weeklyHour = weeklyHour,
            weeklyMinute = weeklyMinute,
            retention = retention
        )
        DailyReportScheduler.scheduleNext(app)
    }

    suspend fun generateDailyReportNow(): String? {
        runCatching { DailyReportGenerator.generateDaily(app) }
            .getOrElse { return "日报生成失败：${it.message ?: it::class.java.simpleName}" }
        autoBackupIfEnabled()
        return "AI 日报已生成，并写入「AI 报告」归档。"
    }

    suspend fun deleteAiReport(reportId: Long): String? {
        repository.deleteAiReport(reportId)
        autoBackupIfEnabled()
        return null
    }

    fun resetOnboarding() {
        settingsStore.resetOnboarding()
    }

    fun markOnboardingSeen() {
        settingsStore.markOnboardingSeen()
    }

    fun updateDesktopSyncEnabled(enabled: Boolean) {
        settingsStore.updateDesktopSyncEnabled(enabled)
        if (enabled) {
            DesktopSyncService.start(app)
            app.desktopSyncCoordinator.ensureRunning()
        } else {
            app.stopService(Intent(app, DesktopSyncService::class.java))
            app.desktopSyncCoordinator.stop()
        }
        desktopSyncRefreshTick.value = System.currentTimeMillis()
    }

    fun rotateDesktopSyncToken() {
        settingsStore.rotateDesktopSyncToken()
        if (settingsStore.currentSettings().desktopSyncEnabled) {
            app.desktopSyncCoordinator.ensureRunning()
        }
        desktopSyncRefreshTick.value = System.currentTimeMillis()
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
        selectedGroupIdsFlow.value = selectedGroupIdsFlow.value - groupId
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
            selectedGroupIdsFlow.value = emptySet()
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

    private fun PlanningImportCandidate.toTodoDraft(groups: List<TaskGroup>): TodoDraft {
        return TodoDraft(
            title = title,
            notes = notes,
            dueAt = dueAt,
            reminderAt = null,
            groupId = resolvePlanningGroupId(groupName, groups),
            ringEnabled = true,
            vibrateEnabled = true,
            countdownEnabled = countdownEnabled && dueAt != null,
            recurrence = recurrence,
            reminderOffsetsMinutes = if (dueAt == null) emptyList() else normalizedReminderOffsets().ifEmpty { listOf(DEFAULT_PLANNING_REMINDER_MINUTES) }
        )
    }

    private fun PlanningImportCandidate.toLinkedTodoDraft(groups: List<TaskGroup>): TodoDraft {
        val ddl = requireNotNull(endAt) { "Linked todo requires event end time" }
        return TodoDraft(
            title = title,
            notes = notes,
            dueAt = ddl,
            reminderAt = null,
            groupId = resolvePlanningGroupId(groupName, groups),
            ringEnabled = true,
            vibrateEnabled = true,
            recurrence = RecurrenceConfig(),
            reminderOffsetsMinutes = normalizedReminderOffsets().ifEmpty { listOf(DEFAULT_PLANNING_REMINDER_MINUTES) }
        )
    }

    private fun PlanningImportCandidate.toEventDraft(groups: List<TaskGroup>): CalendarEventDraft {
        val offsets = normalizedReminderOffsets().ifEmpty { listOf(DEFAULT_PLANNING_REMINDER_MINUTES) }
        return CalendarEventDraft(
            title = title,
            notes = notes,
            location = location,
            startAt = requireNotNull(startAt) { "Event requires startAt" },
            endAt = requireNotNull(endAt) { "Event requires endAt" },
            allDay = allDay,
            accentColorHex = groups.firstOrNull { it.name == groupName }?.colorHex ?: "#4E87E1",
            reminderMinutesBefore = offsets.firstOrNull() ?: DEFAULT_PLANNING_REMINDER_MINUTES,
            reminderOffsetsMinutes = offsets,
            ringEnabled = true,
            vibrateEnabled = true,
            reminderDeliveryMode = ReminderDeliveryMode.FULLSCREEN,
            countdownEnabled = countdownEnabled,
            recurrence = recurrence,
            groupId = resolvePlanningGroupId(groupName, groups)
        )
    }

    private fun PlanningImportCandidate.toPlanningLineMapping(
        noteId: Long?,
        item: TodoItem,
        sourceLine: String,
        batchId: String,
        timestamp: Long
    ): PlanningLineMapping? {
        val resolvedNoteId = noteId ?: return null
        return PlanningLineMapping(
            noteId = resolvedNoteId,
            contentFingerprint = PlanningLineMatcher.fingerprint(sourceLine),
            originalLineText = sourceLine,
            currentLineText = sourceLine,
            todoId = item.id.takeIf { item.isTodo },
            eventId = item.id.takeIf { item.isEvent },
            batchId = batchId,
            operationType = "IMPORT",
            createdAtMillis = timestamp,
            lastRefreshedAtMillis = timestamp,
            lastKnownLineNumber = lineNumber
        )
    }

    private fun planningDocumentLines(markdown: String): List<String> {
        return markdown.replace("\r\n", "\n").replace('\r', '\n').lines().let { lines ->
            if (lines.size > 1 && lines.last().isEmpty() && markdown.endsWith("\n")) lines.dropLast(1) else lines
        }
    }

    private suspend fun reschedulePlanningOperationItems(result: PlanningOperationResult) {
        clearReminderArtifacts(result.affectedBeforeItems)
        result.affectedAfterItems.forEach { scheduleReminderOrDisable(it) }
    }

    private fun resolvePlanningGroupId(groupName: String, groups: List<TaskGroup>): Long {
        if (groupName.isNotBlank()) {
            groups.firstOrNull { it.name.equals(groupName.trim(), ignoreCase = true) }?.let { return it.id }
        }
        return groups.firstOrNull { it.name == "例行" }?.id ?: groups.firstOrNull()?.id ?: 0L
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

        val reminderTriggerMillis = if (draft.dueAt == null) {
            emptyList()
        } else {
            draft.normalizedReminderOffsetsMinutes
                .map { requireNotNull(dueAtMillis) - it * 60_000L }
                .distinct()
        }
        if (draft.dueAt == null && draft.normalizedReminderOffsetsMinutes.isNotEmpty()) {
            return "未设置 DDL 的任务不能启用提醒"
        }
        if (!isHistory && original == null && reminderTriggerMillis.any { it <= now }) {
            return "提醒时间必须晚于当前时间"
        }
        if (reminderTriggerMillis.any { it > (dueAtMillis ?: Long.MAX_VALUE) }) {
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
                repository.updateTodo(item.copy(reminderEnabled = false, reminderAtMillis = null))
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

    private fun nextDateTickDelayMillis(): Long {
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val nextMidnight = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant()
        val untilNextDay = nextMidnight.toEpochMilli() - now.toEpochMilli()
        return untilNextDay.coerceAtLeast(1_000L).coerceAtMost(60_000L)
    }

    private fun dayRangeMillis(date: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    private fun boardEventRangeMillis(date: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endExclusive = date.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to endExclusive
    }

    private fun clearReminderArtifacts(items: List<TodoItem>) {
        items.forEach { item ->
            alarmScheduler.cancel(item.id)
            reminderNotifier.cancel(item.id)
            ReminderDispatchTracker.clear(app, item.id)
            ActiveReminderStore.clearIfMatches(app, item.id)
        }
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
        private const val CalendarWindowPaddingDays = 2L
    }
}

private data class CalendarEventWindow(
    val startInclusive: LocalDate,
    val endExclusive: LocalDate
)

private fun calendarEventWindowAround(date: LocalDate): CalendarEventWindow {
    return CalendarEventWindow(
        startInclusive = date.minusDays(2),
        endExclusive = date.plusDays(8)
    )
}
