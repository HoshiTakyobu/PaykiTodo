package com.example.todoalarm.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.sync.canReadCalendarItem
import com.example.todoalarm.alarm.ActiveReminderStore
import com.example.todoalarm.alarm.DailyBriefScheduler
import com.example.todoalarm.alarm.DailyReportNotifier
import com.example.todoalarm.alarm.DailyReportScheduler
import com.example.todoalarm.alarm.OngoingEventNotifier
import com.example.todoalarm.alarm.ReminderChainLogger
import com.example.todoalarm.alarm.ReminderDispatchTracker
import com.example.todoalarm.alarm.ReminderForegroundService
import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.AiReport
import com.example.todoalarm.data.AiReportRetention
import com.example.todoalarm.data.AiReportType
import com.example.todoalarm.data.BackupSnapshot
import com.example.todoalarm.data.CalendarEventDraft
import com.example.todoalarm.data.DEFAULT_PLANNING_REMINDER_MINUTES
import com.example.todoalarm.data.DailyReportGenerator
import com.example.todoalarm.data.DailyBoardSnapshotBuilder
import com.example.todoalarm.data.DataHealthCleanResult
import com.example.todoalarm.data.DataHealthReport
import com.example.todoalarm.data.EventCheckIn
import com.example.todoalarm.data.GlobalSearchResult
import com.example.todoalarm.data.PlanningAnnouncement
import com.example.todoalarm.data.PlanningAnnouncementParser
import com.example.todoalarm.data.PlanningImportCandidate
import com.example.todoalarm.data.PlanningImportResult
import com.example.todoalarm.data.PlanningLineMapping
import com.example.todoalarm.data.PlanningLineMatcher
import com.example.todoalarm.data.PlanningMarkdownParser
import com.example.todoalarm.data.PlanningNode
import com.example.todoalarm.data.PlanningNodeChangeResult
import com.example.todoalarm.data.PlanningNodeDraft
import com.example.todoalarm.data.PlanningNodeEdit
import com.example.todoalarm.data.PlanningNodeSnapshot
import com.example.todoalarm.data.PlanningNote
import com.example.todoalarm.data.PlanningParseResult
import com.example.todoalarm.data.PlanningParsedType
import com.example.todoalarm.data.PlanningPostponeScope
import com.example.todoalarm.data.PlanningRefreshScope
import com.example.todoalarm.data.PlanningOperationResult
import com.example.todoalarm.data.PlanningRecognitionService
import com.example.todoalarm.data.QuickChecklistGroup
import com.example.todoalarm.data.QuickChecklistParser
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
import com.example.todoalarm.data.TodoRepository.GroupFilterMode
import com.example.todoalarm.data.RecurrenceConfig
import com.example.todoalarm.data.buildScheduleTemplatePayload
import com.example.todoalarm.data.parseScheduleTemplatePayload
import com.example.todoalarm.data.reminderTriggerTimesMillis
import com.example.todoalarm.data.toDraftsForWeek
import com.example.todoalarm.data.toJsonString
import com.example.todoalarm.data.toWeeklyRecurringDrafts
import com.example.todoalarm.data.toEpochMillis
import com.example.todoalarm.data.storageStringToWeekdays
import com.example.todoalarm.sync.DesktopSyncStatus
import com.example.todoalarm.sync.DesktopSyncService
import com.example.todoalarm.sync.AccountAuthResult
import com.example.todoalarm.sync.RemoteBackupInspection
import com.example.todoalarm.sync.diffAgainst
import com.example.todoalarm.sync.remoteSummary
import com.example.todoalarm.widget.TodoWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
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
    val quickChecklistGroups: List<QuickChecklistGroup> = emptyList(),
    val quickChecklistItems: List<TodoItem> = emptyList(),
    val selectedGroupIds: Set<Long> = emptySet(),
    val groupFilterMode: GroupFilterMode = GroupFilterMode.INTERSECTION,
    val missedItems: List<TodoItem> = emptyList(),
    val todayItems: List<TodoItem> = emptyList(),
    val upcomingItems: List<TodoItem> = emptyList(),
    val upcomingItemGroups: List<UpcomingTodoDisplayGroup> = emptyList(),
    val calendarItems: List<TodoItem> = emptyList(),
    val countdownItems: List<TodoItem> = emptyList(),
    val activeAnnouncements: List<PlanningAnnouncement> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val currentQuote: String = QuoteRepository.seedQuotes.first(),
    val dataReady: Boolean = false,
    val authenticated: Boolean = false
)

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TodoApplication
    private val repository = app.repository
    private val alarmScheduler = app.alarmScheduler
    private val reminderNotifier = app.reminderNotifier
    private val settingsStore = app.settingsStore
    private val quoteRepository = app.quoteRepository
    val calendars = repository.observeCalendars().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun saveCalendar(calendar: com.example.todoalarm.data.PlannerCalendar) { viewModelScope.launch { repository.saveCalendar(calendar); autoBackupIfEnabled() } }
    fun deleteCalendar(id: String) { viewModelScope.launch {
        repository.deleteCalendar(id)
        val settings = settingsStore.currentSettings()
        settingsStore.updateDisplayPreferences(settings.mainFont, settings.customFontPath, settings.countdownPastDays, settings.hiddenCalendarIds - id)
        autoBackupIfEnabled()
    } }
    fun setCountdownPastMode(id: Long, mode: String) { viewModelScope.launch { repository.setCountdownPastMode(id, mode); autoBackupIfEnabled() } }
    fun updateDisplayPreferences(font: String, path: String, days: Int, hidden: Set<String>) {
        settingsStore.updateDisplayPreferences(font, path, days, hidden)
    }

    private val quoteFlow = MutableStateFlow(QuoteRepository.seedQuotes)
    private val selectedGroupIdsFlow = MutableStateFlow<Set<Long>>(emptySet())
    private val groupFilterModeFlow = MutableStateFlow(GroupFilterMode.INTERSECTION)
    private val desktopSyncRefreshTick = MutableStateFlow(0L)
    private val authenticationTick = MutableStateFlow(0L)
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
                if (settingsStore.clearExpiredRemoteSessionIfNeeded()) {
                    app.desktopSyncCoordinator.stop()
                    app.stopService(Intent(app, DesktopSyncService::class.java))
                }
                authenticationTick.value = System.currentTimeMillis()
                delay(30_000L)
            }
        }
        viewModelScope.launch {
            while (true) {
                todayDateFlow.value = LocalDate.now()
                delay(nextDateTickDelayMillis())
            }
        }
        viewModelScope.launch {
            while (true) {
                if (settingsStore.currentSettings().desktopSyncEnabled) {
                    desktopSyncRefreshTick.value = System.currentTimeMillis()
                    delay(DESKTOP_SYNC_STATUS_REFRESH_MILLIS)
                } else {
                    delay(DESKTOP_SYNC_IDLE_REFRESH_MILLIS)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeTodoItems = combine(selectedGroupIdsFlow, groupFilterModeFlow) { groupIds, mode -> groupIds to mode }
        .flatMapLatest { (groupIds, mode) -> repository.observeActiveTodoItems(groupIds, mode) }
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
            repository.observeCountdownCandidates()
        }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyItems = combine(selectedGroupIdsFlow, groupFilterModeFlow) { groupIds, mode -> groupIds to mode }
        .flatMapLatest { (groupIds, mode) -> repository.observeHistoryTodoItems(groupIds, mode) }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val desktopSyncStatus = combine(settingsStore.settingsFlow, desktopSyncRefreshTick) { _, tick -> tick }
        .mapLatest {
            withContext(Dispatchers.IO) {
                app.desktopSyncCoordinator.status()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DesktopSyncStatus(
                enabled = false,
                running = false,
                port = 18765,
                token = "",
                ipAddresses = emptyList()
            )
        )

    val uiState = combine(
        repository.observeGroups(),
        repository.observePlanningNotesWithAnnouncementHints(),
        activeTodoItems,
        repository.observeQuickChecklistGroups(),
        repository.observeQuickChecklistItems(),
        boardCalendarItems,
        countdownItems,
        settingsStore.settingsFlow,
        quoteFlow,
        selectedGroupIdsFlow,
        groupFilterModeFlow,
        todayDateFlow,
        authenticationTick
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val groups = values[0] as List<TaskGroup>
        @Suppress("UNCHECKED_CAST")
        val announcementNotes = values[1] as List<PlanningNote>
        @Suppress("UNCHECKED_CAST")
        val activeTaskItems = values[2] as List<TodoItem>
        @Suppress("UNCHECKED_CAST")
        val quickChecklistGroups = values[3] as List<QuickChecklistGroup>
        @Suppress("UNCHECKED_CAST")
        val quickChecklistItems = values[4] as List<TodoItem>
        @Suppress("UNCHECKED_CAST")
        val activeCalendarItems = values[5] as List<TodoItem>
        @Suppress("UNCHECKED_CAST")
        val activeCountdownItems = values[6] as List<TodoItem>
        val settings = values[7] as AppSettings
        @Suppress("UNCHECKED_CAST")
        val quotes = values[8] as List<String>
        @Suppress("UNCHECKED_CAST")
        val selectedGroupIds = values[9] as Set<Long>
        val groupFilterMode = values[10] as GroupFilterMode
        val today = values[11] as LocalDate
        @Suppress("UNUSED_VARIABLE")
        val authTick = values[12] as Long
        val availableGroups = if (groups.isEmpty()) {
            withContext(Dispatchers.IO) { repository.ensureDefaultGroups() }
        } else {
            groups
        }

        withContext(Dispatchers.Default) {
            val todoSections = classifyActiveTodoItems(activeTaskItems, today)
            val sortedCalendarItems = activeCalendarItems.sortedBy { it.startAtMillis ?: it.dueAtMillis }
            val availableQuotes = quotes.ifEmpty { QuoteRepository.seedQuotes }
            val currentQuote = availableQuotes[settings.quoteIndex.mod(availableQuotes.size)]
            val nowMillis = System.currentTimeMillis()
            val visibleCountdownItems = activeCountdownItems
                .mapNotNull { item ->
                    val targetMillis = DailyBoardSnapshotBuilder.countdownTargetMillis(item)
                    if (targetMillis != null && com.example.todoalarm.data.CountdownVisibility.visible(item, nowMillis, settings.countdownPastDays)) item to targetMillis else null
                }
                .sortedBy { it.second }
                .map { it.first }

            TodoUiState(
                groups = availableGroups,
                quickChecklistGroups = quickChecklistGroups,
                quickChecklistItems = quickChecklistItems,
                selectedGroupIds = selectedGroupIds,
                groupFilterMode = groupFilterMode,
                missedItems = todoSections.missedItems,
                todayItems = todoSections.todayItems,
                upcomingItems = todoSections.upcomingItems,
                upcomingItemGroups = buildUpcomingTodoDisplayGroups(todoSections.upcomingItems),
                calendarItems = sortedCalendarItems,
                countdownItems = visibleCountdownItems,
                activeAnnouncements = PlanningAnnouncementParser.activeAnnouncements(announcementNotes, today),
                settings = settings,
                currentQuote = currentQuote,
                dataReady = true,
                authenticated = settingsStore.hasLocalAccountAccess()
            )
        }
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

    suspend fun createQuickChecklist(rawText: String): String? {
        val draft = QuickChecklistParser.parse(rawText)
        draft.diagnostics.firstOrNull { it.severity == com.example.todoalarm.data.QuickChecklistDiagnosticSeverity.ERROR }
            ?.let { return "第 ${it.sourceLine} 行：${it.message}" }
        return runCatching {
            repository.createQuickChecklist(draft)
            autoBackupIfEnabled()
            null
        }.getOrElse { error ->
            if (error.message?.contains("UNIQUE", ignoreCase = true) == true ||
                error.message?.contains("已存在", ignoreCase = true) == true
            ) {
                "相同内容的快速清单已经存在"
            } else {
                error.message ?: "快速清单创建失败"
            }
        }
    }

    fun toggleQuickChecklistGroup(group: QuickChecklistGroup) {
        viewModelScope.launch {
            repository.setQuickChecklistGroupCollapsed(group.id, !group.collapsed)
            autoBackupIfEnabled()
        }
    }

    fun toggleQuickChecklistTask(item: TodoItem) {
        viewModelScope.launch {
            repository.setQuickChecklistTaskCollapsed(item.id, !item.quickChecklistCollapsed)
            autoBackupIfEnabled()
        }
    }

    fun selectGroup(groupId: Long?) {
        selectedGroupIdsFlow.value = if (groupId == null) {
            emptySet()
        } else {
            val current = selectedGroupIdsFlow.value
            if (groupId in current) current - groupId else current + groupId
        }
        if (selectedGroupIdsFlow.value.size < 2) {
            groupFilterModeFlow.value = GroupFilterMode.INTERSECTION
        }
    }

    fun toggleGroupFilterMode() {
        groupFilterModeFlow.value = when (groupFilterModeFlow.value) {
            GroupFilterMode.INTERSECTION -> GroupFilterMode.UNION
            GroupFilterMode.UNION -> GroupFilterMode.INTERSECTION
        }
    }

    fun updateCalendarEventWindow(startInclusive: LocalDate, endExclusive: LocalDate) {
        if (!startInclusive.isBefore(endExclusive)) return
        nextCalendarEventWindow(
            current = calendarEventWindowFlow.value,
            requestStartInclusive = startInclusive,
            requestEndExclusive = endExclusive,
            paddingDays = CalendarWindowPaddingDays
        )?.let { nextWindow ->
            calendarEventWindowFlow.value = nextWindow
        }
    }

    suspend fun getTodoById(todoId: Long): TodoItem? {
        return withContext(Dispatchers.IO) {
            repository.getTodo(todoId)?.takeIf { app.canReadCalendarItem(it) }
        }
    }

    suspend fun getTodoGroupIds(todoId: Long): List<Long> {
        return withContext(Dispatchers.IO) { repository.getGroupIdsForTodo(todoId) }
    }

    suspend fun getEventCheckIns(eventId: Long): List<EventCheckIn> {
        return withContext(Dispatchers.IO) {
            if (getTodoById(eventId) == null) emptyList() else repository.getCheckInsForEvent(eventId)
        }
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

    suspend fun globalSearch(query: String): GlobalSearchResult {
        return withContext(Dispatchers.IO) { repository.globalSearch(query) }
    }

    suspend fun parsePlanningMarkdown(markdown: String, activeNoteId: Long? = null): PlanningParseResult {
        val documentDate = activeNoteId
            ?.let { repository.getPlanningNote(it)?.documentDateEpochDay }
            ?.let(LocalDate::ofEpochDay)
        return PlanningRecognitionService.recognize(
            markdown = markdown,
            settings = settingsStore.currentSettings(),
            defaultDate = documentDate,
            onSelectionChanged = { app.activatePlanningAiProvider(it) }
        )
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

    suspend fun openOrCreateTodayPlanningNote(): Long {
        val today = LocalDate.now()
        val datedNote = repository.getPlanningNoteByDocumentDate(today)
            ?: repository.getAllPlanningNotes()
                .filter { !it.archived }
                .firstOrNull { note -> planningTitleMatchesDate(note.title, today) }
        val note = datedNote ?: repository.createPlanningNote("${today.monthValue}月${today.dayOfMonth}日", today)
        settingsStore.updateLastOpenedPlanningNoteId(note.id)
        if (datedNote == null) autoBackupIfEnabled()
        return note.id
    }

    suspend fun savePlanningNoteContent(noteId: Long, contentMarkdown: String): String? {
        val updated = repository.updatePlanningNoteContent(noteId, contentMarkdown) ?: return "规划文档不存在"
        settingsStore.updateLastOpenedPlanningNoteId(updated.id)
        autoBackupIfEnabled()
        return null
    }

    fun observePlanningNodes(noteId: Long): Flow<List<PlanningNode>> {
        return repository.observePlanningNodesForNote(noteId)
    }

    suspend fun createPlanningNodeSnapshot(noteId: Long): PlanningNodeSnapshot {
        return repository.createPlanningNodeSnapshot(noteId)
    }

    suspend fun restorePlanningNodeSnapshot(snapshot: PlanningNodeSnapshot): String? {
        val result = repository.restorePlanningNodeSnapshot(snapshot)
        clearReminderArtifacts(result.affectedBeforeItems)
        result.affectedAfterItems.forEach { scheduleReminderOrDisable(it) }
        settingsStore.updateLastOpenedPlanningNoteId(snapshot.noteId)
        autoBackupIfEnabled()
        return null
    }

    suspend fun createPlanningNode(draft: PlanningNodeDraft): String? {
        if (draft.text.isBlank()) return "事项不能为空"
        val result = repository.createPlanningNode(
            draft,
            createEventEndTodo = settingsStore.currentSettings().planningEventEndTodoEnabled
        ) ?: return "规划节点创建失败"
        handlePlanningNodeChange(result)
        settingsStore.updateLastOpenedPlanningNoteId(result.node.noteId)
        if (!result.node.isDraft) autoBackupIfEnabled()
        return null
    }

    suspend fun updatePlanningNode(node: PlanningNode, edit: PlanningNodeEdit): String? {
        if (edit.text.isBlank()) return "事项不能为空"
        val beforeLinked = listOfNotNull(
            node.linkedTodoId?.let { repository.getTodo(it) },
            node.linkedEndTodoId?.let { repository.getTodo(it) }
        )
        val result = repository.updatePlanningNode(
            node.id,
            edit,
            createEventEndTodo = settingsStore.currentSettings().planningEventEndTodoEnabled
        ) ?: return "规划节点更新失败"
        clearReminderArtifacts(beforeLinked)
        clearReminderArtifacts(result.deletedLinkedItems)
        handlePlanningNodeChange(result)
        settingsStore.updateLastOpenedPlanningNoteId(result.node.noteId)
        if (!result.node.isDraft) autoBackupIfEnabled()
        return null
    }

    suspend fun publishPlanningNode(node: PlanningNode): String? {
        val result = repository.publishPlanningNode(
            node.id,
            createEventEndTodo = settingsStore.currentSettings().planningEventEndTodoEnabled
        ) ?: return "草稿发布失败，请检查时间格式"
        clearReminderArtifacts(result.deletedLinkedItems)
        handlePlanningNodeChange(result)
        settingsStore.updateLastOpenedPlanningNoteId(result.node.noteId)
        autoBackupIfEnabled()
        return "已发布 1 条"
    }

    suspend fun publishAllPlanningDrafts(noteId: Long): String? {
        val result = repository.publishAllPlanningDrafts(
            noteId,
            createEventEndTodo = settingsStore.currentSettings().planningEventEndTodoEnabled
        )
        result.published.forEach { change ->
            clearReminderArtifacts(change.deletedLinkedItems)
            handlePlanningNodeChange(change)
        }
        if (result.published.isNotEmpty()) {
            settingsStore.updateLastOpenedPlanningNoteId(noteId)
            autoBackupIfEnabled()
        }
        return "已发布 ${result.published.size} 条，${result.failedCount} 条失败"
    }

    suspend fun togglePlanningNodeCompleted(node: PlanningNode): String? {
        val result = repository.togglePlanningNodeCompleted(node.id) ?: return "规划节点不存在"
        handlePlanningNodeChange(result)
        if (!result.node.isDraft || result.affectedLinkedItems.isNotEmpty()) autoBackupIfEnabled()
        return null
    }

    suspend fun deletePlanningNode(node: PlanningNode): String? {
        val result = repository.deletePlanningNodeTree(
            node.id,
            createEventEndTodo = settingsStore.currentSettings().planningEventEndTodoEnabled
        )
        clearReminderArtifacts(result.deletedLinkedItems)
        result.affectedLinkedItems.forEach { scheduleReminderOrDisable(it) }
        if (!node.isDraft || result.deletedLinkedItems.isNotEmpty() || result.affectedLinkedItems.isNotEmpty()) autoBackupIfEnabled()
        return null
    }

    suspend fun reorderPlanningNodes(noteId: Long, parentNodeId: Long?, orderedNodeIds: List<Long>): String? {
        repository.reorderPlanningNodes(noteId, parentNodeId, orderedNodeIds)
        autoBackupIfEnabled()
        return null
    }

    suspend fun exportPlanningNodesMarkdown(noteId: Long): String {
        return repository.exportPlanningNodesToMarkdown(noteId)
    }

    suspend fun replacePlanningNodesFromMarkdown(noteId: Long, markdown: String): String? {
        val result = repository.replacePlanningNodesFromMarkdown(noteId, markdown)
        clearReminderArtifacts(result.deletedLinkedItems)
        result.created.forEach { change -> handlePlanningNodeChange(change) }
        settingsStore.updateLastOpenedPlanningNoteId(noteId)
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
        val deletedItems = repository.deletePlanningNote(noteId)
        clearReminderArtifacts(deletedItems)
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
                PlanningParsedType.TODO -> validateDraft(
                    draft = candidate.toTodoDraft(groups),
                    original = null,
                    scope = RecurrenceScope.CURRENT,
                    allowPastDueAtWithoutReminder = true
                )
                PlanningParsedType.EVENT -> validateCalendarDraft(
                    draft = candidate.toEventDraft(groups),
                    original = null,
                    scope = RecurrenceScope.CURRENT,
                    allowPastReminderTriggers = true
                )
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

    suspend fun importCapturePreview(
        preview: CapturePlanningPreview,
        candidates: List<PlanningImportCandidate>,
        selectedIds: Set<String>
    ): PlanningImportResult {
        return importPlanningCandidates(
            candidates = candidates,
            selectedIds = selectedIds,
            currentMarkdown = preview.currentMarkdown,
            activeNoteId = preview.activeNoteId
        )
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

    suspend fun updateCalendarEventMultiSlotBundleSharedFields(
        original: TodoItem,
        draft: CalendarEventDraft
    ): String? {
        if (original.multiSlotBundleId.isNullOrBlank()) {
            return updateCalendarEvent(original, draft, RecurrenceScope.CURRENT)
        }
        validateCalendarDraft(
            draft = draft,
            original = original,
            scope = RecurrenceScope.ALL
        )?.let { return it }

        val affectedItems = repository.getActiveEventsForMultiSlotBundle(original).ifEmpty { listOf(original) }
        clearReminderArtifacts(affectedItems)

        val updatedItems = repository.updateCalendarEventMultiSlotBundleSharedFields(original, draft)
        for (item in updatedItems) {
            scheduleReminderOrDisable(item)
        }
        autoBackupIfEnabled()
        return null
    }

    fun completeTodo(todoItem: TodoItem) {
        viewModelScope.launch {
            val result = repository.setCompletedWithResult(todoItem.id, true)
            clearReminderArtifacts(result?.affectedItems ?: listOf(todoItem))
            autoBackupIfEnabled()
        }
    }

    fun completeTodos(todoItems: List<TodoItem>) {
        val targets = todoItems
            .filter { it.isTodo && !it.completed && !it.canceled }
            .distinctBy { it.id }
        if (targets.isEmpty()) return
        viewModelScope.launch {
            val affectedItems = buildList {
                targets.forEach { item ->
                    val result = repository.setCompletedWithResult(item.id, true)
                    addAll(result?.affectedItems ?: listOf(item))
                }
            }.distinctBy { it.id }
            clearReminderArtifacts(affectedItems)
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

    fun cancelTodos(todoItems: List<TodoItem>) {
        val targets = todoItems
            .filter { it.isTodo && !it.completed && !it.canceled }
            .distinctBy { it.id }
        if (targets.isEmpty()) return
        viewModelScope.launch {
            val canceledItems = buildList {
                targets.forEach { item ->
                    // Batch cancellation deliberately affects only each selected occurrence.
                    addAll(repository.cancelTodo(item, RecurrenceScope.CURRENT))
                }
            }.distinctBy { it.id }
            clearReminderArtifacts(canceledItems)
            autoBackupIfEnabled()
        }
    }

    fun cancelCalendarEvent(todoItem: TodoItem, scope: RecurrenceScope = RecurrenceScope.CURRENT) {
        viewModelScope.launch {
            val canceledItems = repository.cancelCalendarEvent(todoItem, scope)
            clearReminderArtifacts(canceledItems.ifEmpty { listOf(todoItem) })
            autoBackupIfEnabled()
        }
    }

    fun restoreTodo(todoItem: TodoItem) {
        viewModelScope.launch {
            val restored = repository.restoreTodoFromHistory(
                item = todoItem,
                missedThresholdMillis = MISSED_THRESHOLD_MILLIS
            )
            restored.forEach { item -> scheduleReminderOrDisable(item) }
            autoBackupIfEnabled()
        }
    }

    fun deleteTodo(todoItem: TodoItem, scope: RecurrenceScope = RecurrenceScope.CURRENT) {
        viewModelScope.launch {
            val deletedItems = repository.deleteTodo(todoItem, scope)
            clearReminderArtifacts(deletedItems.ifEmpty { listOf(todoItem) })
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

    fun updateEventCheckInPreferences(
        autoCheckOutOnEnd: Boolean,
        showStatsOnComplete: Boolean,
        idleAutoCheckOutHours: Int = settingsStore.currentSettings().eventCheckInIdleAutoCheckOutHours
    ) {
        settingsStore.updateEventCheckInPreferences(
            autoCheckOutOnEnd = autoCheckOutOnEnd,
            showStatsOnComplete = showStatsOnComplete,
            idleAutoCheckOutHours = idleAutoCheckOutHours
        )
    }

    fun updateOngoingEventNotificationEnabled(enabled: Boolean) {
        settingsStore.updateOngoingEventNotificationEnabled(enabled)
        viewModelScope.launch(Dispatchers.IO) {
            repository.activeEventsForOngoingNotifications().forEach { event ->
                if (enabled) {
                    OngoingEventNotifier.schedule(app, event)
                } else {
                    OngoingEventNotifier.cancelAll(app, event.id)
                }
            }
        }
    }

    fun updatePlanningOutlinerPreferences(
        hintVisible: Boolean = settingsStore.currentSettings().planningOutlineHintVisible,
        eventEndTodoEnabled: Boolean = settingsStore.currentSettings().planningEventEndTodoEnabled
    ) {
        settingsStore.updatePlanningOutlinerPreferences(
            hintVisible = hintVisible,
            eventEndTodoEnabled = eventEndTodoEnabled
        )
    }

    fun updateDesktopSyncWifiKeepAlive(enabled: Boolean) {
        settingsStore.updateDesktopSyncWifiKeepAlive(enabled)
        if (settingsStore.currentSettings().desktopSyncEnabled) {
            DesktopSyncService.start(app)
        }
        desktopSyncRefreshTick.value = System.currentTimeMillis()
    }

    fun isAuthenticated(): Boolean = settingsStore.hasLocalAccountAccess()

    suspend fun loginAccount(username: String, password: String, turnstileToken: String?): AccountAuthResult {
        return withContext(Dispatchers.IO) {
            app.remoteSyncCoordinator.login(username, password, turnstileToken)
        }.also { result ->
            if (result.succeeded) authenticationTick.value = System.currentTimeMillis()
        }
    }

    suspend fun registerAccount(username: String, displayName: String, password: String, turnstileToken: String?): AccountAuthResult {
        return withContext(Dispatchers.IO) {
            app.remoteSyncCoordinator.register(username, displayName, password, turnstileToken)
        }.also { result ->
            if (result.succeeded) authenticationTick.value = System.currentTimeMillis()
        }
    }

    suspend fun recoverAccount(
        username: String,
        recoveryCode: String,
        newPassword: String,
        turnstileToken: String?
    ): String? {
        return app.remoteSyncCoordinator.recoverPassword(username, recoveryCode, newPassword, turnstileToken)
    }

    fun logoutRemoteAccount() {
        app.remoteSyncCoordinator.logout()
    }

    fun syncRemoteNow(): String {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { app.remoteSyncCoordinator.syncNow() }
        }
        return "已开始远程同步"
    }

    suspend fun inspectRemoteBackup(): RemoteBackupInspection? {
        val remoteSnapshot = app.remoteSyncCoordinator.downloadLatest() ?: return null
        val localSnapshot = repository.exportSnapshot(settingsStore.currentSettings())
        return RemoteBackupInspection(
            snapshot = remoteSnapshot,
            remote = remoteSnapshot.remoteSummary(),
            local = localSnapshot.remoteSummary(),
            diff = remoteSnapshot.diffAgainst(localSnapshot)
        )
    }

    suspend fun restoreRemoteBackup(inspection: RemoteBackupInspection): String? {
        return runCatching {
            restoreSnapshotWithSafetyBackup(inspection.snapshot, "远程快照")
            null
        }.getOrElse { it.message ?: "远程恢复失败" }
    }

    fun updateBoardCollapseState(
        countdown: Boolean = settingsStore.currentSettings().boardCountdownCollapsed,
        todayTodos: Boolean = settingsStore.currentSettings().boardTodayTodosCollapsed,
        todayEvents: Boolean = settingsStore.currentSettings().boardTodayEventsCollapsed,
        tomorrowEvents: Boolean = settingsStore.currentSettings().boardTomorrowEventsCollapsed,
        announcement: Boolean = settingsStore.currentSettings().boardAnnouncementCollapsed
    ) {
        settingsStore.updateBoardCollapseState(
            countdown = countdown,
            todayTodos = todayTodos,
            todayEvents = todayEvents,
            tomorrowEvents = tomorrowEvents,
            announcement = announcement
        )
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

    fun updateDailyBriefPreferences(
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        settingsStore.updateDailyBriefPreferences(
            enabled = enabled,
            hour = hour,
            minute = minute
        )
        DailyBriefScheduler.scheduleNext(app)
    }

    suspend fun generateDailyReportNow(): String? {
        val result = runCatching { DailyReportGenerator.generateDailyDetailed(app) }
            .getOrElse { return "日报生成失败：${it.message ?: it::class.java.simpleName}" }
        autoBackupIfEnabled()
        return if (result.report.isLocalFallback) {
            "AI 未成功生成，已改用本地模板：${result.aiFailureMessage ?: "请检查 AI 配置"}"
        } else {
            "AI 日报已生成，并写入「AI 报告」归档。"
        }
    }

    suspend fun deleteAiReport(reportId: Long): String? {
        val report = repository.getAiReportById(reportId)
        repository.deleteAiReport(reportId)
        report?.let {
            DailyReportNotifier.cancelReportNotification(
                context = app,
                reportId = it.id,
                weekly = it.type == AiReportType.WEEKLY
            )
        }
        autoBackupIfEnabled()
        return null
    }

    suspend fun inspectDataHealth(): DataHealthReport {
        return repository.inspectDataHealth(settingsStore.currentSettings().aiReportRetention)
    }

    suspend fun cleanSafeDataHealthItems(): DataHealthCleanResult {
        val result = repository.cleanSafeDataHealthItems(settingsStore.currentSettings().aiReportRetention)
        if (result.totalDeleted > 0) {
            autoBackupIfEnabled()
        }
        return result
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
            runCatching {
                DesktopSyncService.start(app)
                app.desktopSyncCoordinator.ensureRunning()
            }.onFailure {
                app.desktopSyncCoordinator.stop()
                settingsStore.updateDesktopSyncEnabled(false)
            }
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
                reminderDeliveryMode = ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION,
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
            message = "delaySeconds=$delaySeconds; mode=${ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION.name}"
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
        val zone = ZoneId.systemDefault()
        val weekStartMillis = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val weekEndExclusiveMillis = weekStart.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
        val weekEvents = repository.getActiveEventsOverlappingRange(weekStartMillis, weekEndExclusiveMillis)
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
            "迁移包导出完成"
        }.getOrElse { it.message ?: "导出失败" }
    }

    suspend fun importBackup(sourceUri: Uri): String? {
        return runCatching {
            val snapshot = app.backupManager.importFromUri(sourceUri)
            restoreSnapshotWithSafetyBackup(snapshot, "迁移包")
        }.getOrElse { it.message ?: "导入失败" }
    }

    private suspend fun restoreSnapshotWithSafetyBackup(
        snapshot: BackupSnapshot,
        sourceLabel: String
    ): String {
        val currentSettings = settingsStore.currentSettings()
        val currentSnapshot = repository.exportSnapshot(currentSettings)
        val externalSafetyBackup = currentSettings.backupDirectoryUri?.let { directoryUri ->
            runCatching { app.backupManager.autoBackupToDirectory(directoryUri, currentSnapshot) }.getOrNull()
        }
        if (externalSafetyBackup == null) {
            app.backupManager.autoBackupToInternalStorage(currentSnapshot)
        }

        clearReminderArtifacts(repository.getAllTodos())
        ActiveReminderStore.clear(app)
        app.stopService(Intent(app, ReminderForegroundService::class.java))

        repository.importSnapshot(snapshot)
        settingsStore.replaceAll(
            snapshot.settings.copy(
                backupDirectoryUri = currentSettings.backupDirectoryUri,
                autoBackupEnabled = currentSettings.autoBackupEnabled,
                remoteSyncEnabled = currentSettings.remoteSyncEnabled,
                remoteSyncServerUrl = currentSettings.remoteSyncServerUrl,
                remoteSyncDeviceId = currentSettings.remoteSyncDeviceId,
                remoteSyncSecret = currentSettings.remoteSyncSecret,
                desktopSyncEnabled = false,
                desktopSyncToken = currentSettings.desktopSyncToken,
                planningAiEnabled = currentSettings.planningAiEnabled,
                planningAiProviderName = currentSettings.planningAiProviderName,
                planningAiBaseUrl = currentSettings.planningAiBaseUrl,
                planningAiApiKey = currentSettings.planningAiApiKey,
                planningAiModel = currentSettings.planningAiModel,
                planningAiProviders = currentSettings.planningAiProviders
            )
        )
        selectedGroupIdsFlow.value = emptySet()
        refreshTaskStates()
        repository.futureReminderItems(System.currentTimeMillis()).forEach(app.alarmScheduler::schedule)
        DailyReportScheduler.scheduleNext(app)
        DailyBriefScheduler.scheduleNext(app)
        TodoWidgetProvider.notifyWidgetDataChanged(app)
        app.remoteSyncCoordinator.onLocalDataChanged()
        val todoCount = snapshot.tasks.count { it.isTodo }
        val eventCount = snapshot.tasks.count { it.isEvent }
        return "${sourceLabel}恢复完成：$todoCount 条待办、$eventCount 条日程、${snapshot.planningNotes.size} 个规划文档"
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
        val offsets = normalizedReminderOffsets()
        return TodoDraft(
            title = title,
            notes = notes,
            dueAt = dueAt,
            reminderAt = null,
            groupId = resolvePlanningGroupId(groupName, groups),
            ringEnabled = true,
            vibrateEnabled = true,
            reminderDeliveryMode = reminderDeliveryMode,
            countdownEnabled = countdownEnabled && dueAt != null,
            recurrence = recurrence,
            reminderOffsetsMinutes = if (dueAt == null) emptyList() else offsets
        )
    }

    private fun PlanningImportCandidate.toLinkedTodoDraft(groups: List<TaskGroup>): TodoDraft {
        val ddl = requireNotNull(endAt) { "Linked todo requires event end time" }
        val offsets = normalizedReminderOffsets()
        return TodoDraft(
            title = title,
            notes = notes,
            dueAt = ddl,
            reminderAt = null,
            groupId = resolvePlanningGroupId(groupName, groups),
            ringEnabled = true,
            vibrateEnabled = true,
            reminderDeliveryMode = reminderDeliveryMode,
            recurrence = RecurrenceConfig(),
            reminderOffsetsMinutes = offsets
        )
    }

    private fun PlanningImportCandidate.toEventDraft(groups: List<TaskGroup>): CalendarEventDraft {
        val offsets = normalizedReminderOffsets()
        return CalendarEventDraft(
            title = title,
            notes = notes,
            location = location,
            startAt = requireNotNull(startAt) { "Event requires startAt" },
            endAt = requireNotNull(endAt) { "Event requires endAt" },
            allDay = allDay,
            accentColorHex = groups.firstOrNull { it.name == groupName }?.colorHex ?: PaykiColorPresets.DefaultEvent,
            reminderMinutesBefore = offsets.firstOrNull(),
            reminderOffsetsMinutes = offsets,
            ringEnabled = true,
            vibrateEnabled = true,
            reminderDeliveryMode = reminderDeliveryMode,
            countdownEnabled = countdownEnabled,
            checkInEnabled = checkInEnabled,
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

    private suspend fun handlePlanningNodeChange(result: PlanningNodeChangeResult) {
        clearReminderArtifacts(result.deletedLinkedItems)
        val linkedItems = result.affectedLinkedItems.ifEmpty { result.linkedItem?.let { listOf(it) }.orEmpty() }
        linkedItems.forEach { linked ->
            if (linked.completed || linked.canceled) {
                clearReminderArtifacts(listOf(linked))
            } else {
                scheduleReminderOrDisable(linked)
            }
        }
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
        scope: RecurrenceScope,
        allowPastDueAtWithoutReminder: Boolean = false
    ): String? {
        if (draft.title.isBlank()) return "标题不能为空"

        val isHistory = original?.isHistory == true
        val now = System.currentTimeMillis()
        val dueAtMillis = draft.dueAt?.toEpochMillis()
        val normalizedReminderOffsets = draft.normalizedReminderOffsetsMinutes
        val allowHistoricalDueAt = allowPastDueAtWithoutReminder && normalizedReminderOffsets.isEmpty()
        if (!isHistory && original == null && dueAtMillis != null && dueAtMillis <= now && !allowHistoricalDueAt) {
            return "DDL 必须晚于当前时间"
        }

        val reminderTriggerMillis = if (draft.dueAt == null) {
            emptyList()
        } else {
            normalizedReminderOffsets
                .map { requireNotNull(dueAtMillis) - it * 60_000L }
                .distinct()
        }
        if (draft.dueAt == null && normalizedReminderOffsets.isNotEmpty()) {
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
        if (original?.isRecurring == true && scope == RecurrenceScope.CURRENT &&
            recurrenceSignatureChanged(original, draft.recurrence)
        ) {
            return "仅修改当前事件时不能变更循环规则；请改用“当前事件和此后所有事件”或“所有事件”"
        }
        return null
    }

    private fun validateCalendarDraft(
        draft: CalendarEventDraft,
        original: TodoItem?,
        scope: RecurrenceScope,
        allowPastReminderTriggers: Boolean = false
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
        val eventHasAlreadyStarted = startMillis <= now
        if (!allowPastReminderTriggers && !eventHasAlreadyStarted && reminderAtMillis.any { it <= now }) {
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
        if (original?.isRecurring == true && scope == RecurrenceScope.CURRENT &&
            recurrenceSignatureChanged(original, draft.recurrence)
        ) {
            return "仅修改当前事件时不能变更循环规则；请改用“当前事件和此后所有事件”或“所有事件”"
        }
        return null
    }

    private fun recurrenceSignatureChanged(original: TodoItem, recurrence: RecurrenceConfig): Boolean {
        return recurrence.enabled != original.isRecurring ||
            recurrence.type != original.recurrenceTypeEnum ||
            recurrence.weeklyDays != storageStringToWeekdays(original.recurrenceWeekdays) ||
            recurrence.endDate != original.recurrenceEndDate
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
        if (item.isEvent) {
            ReminderDispatchTracker.clear(app, item.id)
            if (!item.isHistory) {
                app.alarmScheduler.schedule(item)
            } else {
                app.alarmScheduler.cancel(item.id)
            }
            if (!item.reminderEnabled || item.isHistory) {
                clearDisabledReminderArtifacts(item.id)
            }
            return
        }
        if (item.isTodo && !item.hasDueDate) {
            clearDisabledReminderArtifacts(item.id)
            if (item.reminderEnabled || item.reminderAtMillis != null) {
                repository.updateTodo(item.copy(reminderEnabled = false, reminderAtMillis = null))
            }
            return
        }
        if (!item.reminderEnabled) {
            clearDisabledReminderArtifacts(item.id)
            return
        }
        ReminderDispatchTracker.clear(app, item.id)
        val scheduleMessage = alarmScheduler.schedule(item)
        if (scheduleMessage != null) {
            repository.updateTodo(item.copy(reminderEnabled = false))
            clearDisabledReminderArtifacts(item.id)
        }
    }

    private suspend fun dispatchDueReminders() {
        val now = System.currentTimeMillis()
        repository.dueReminderItems(now).forEach { item ->
            val reminderAtMillis = item.reminderTriggerTimesMillis()
                .lastOrNull { it <= now && alarmScheduler.isReminderScheduled(item.id, it) &&
                    !ReminderDispatchTracker.wasDispatched(app, item.id, it) }
                ?: return@forEach
            ReminderChainLogger.log(
                context = app,
                todoId = item.id,
                source = "TodoViewModel",
                stage = ReminderChainStage.POLL_DISPATCH,
                status = ReminderChainStatus.INFO,
                reminderAtMillis = reminderAtMillis
            )
            ReminderForegroundService.start(
                context = app,
                todoId = item.id,
                triggeredAtMillis = reminderAtMillis
            )
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
            ReminderForegroundService.dismiss(app, item.id)
        }
    }

    private fun clearDisabledReminderArtifacts(todoId: Long) {
        alarmScheduler.cancelReminderOnly(todoId)
        ReminderForegroundService.dismiss(app, todoId)
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
        private const val DESKTOP_SYNC_STATUS_REFRESH_MILLIS = 15_000L
        private const val DESKTOP_SYNC_IDLE_REFRESH_MILLIS = 60_000L
    }
}

internal data class CalendarEventWindow(
    val startInclusive: LocalDate,
    val endExclusive: LocalDate
)

internal fun nextCalendarEventWindow(
    current: CalendarEventWindow,
    requestStartInclusive: LocalDate,
    requestEndExclusive: LocalDate,
    paddingDays: Long
): CalendarEventWindow? {
    if (!requestStartInclusive.isBefore(requestEndExclusive)) return null
    val alreadyCovered = !requestStartInclusive.isBefore(current.startInclusive) &&
        !requestEndExclusive.isAfter(current.endExclusive)
    if (alreadyCovered) return null
    val next = CalendarEventWindow(
        startInclusive = requestStartInclusive.minusDays(paddingDays),
        endExclusive = requestEndExclusive.plusDays(paddingDays)
    )
    return next.takeIf { it != current }
}

private fun calendarEventWindowAround(date: LocalDate): CalendarEventWindow {
    return CalendarEventWindow(
        startInclusive = date.minusDays(2),
        endExclusive = date.plusDays(8)
    )
}

private fun planningTitleMatchesDate(title: String, date: LocalDate): Boolean {
    val normalized = title.trim()
    if (normalized.isBlank()) return false
    val candidates = setOf(
        "${date.monthValue}月${date.dayOfMonth}日",
        "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth),
        "${date.monthValue}.${date.dayOfMonth}",
        "${date.monthValue}/${date.dayOfMonth}"
    )
    return candidates.any { normalized.contains(it) }
}
