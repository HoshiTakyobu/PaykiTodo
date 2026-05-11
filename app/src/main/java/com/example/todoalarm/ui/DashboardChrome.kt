package com.example.todoalarm.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoalarm.R
import com.example.todoalarm.data.CalendarEventDraft
import com.example.todoalarm.data.ReminderDeliveryMode
import com.example.todoalarm.data.ScheduleTemplate
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.ThemeMode
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.WeekStartMode
import com.example.todoalarm.ui.theme.PaykiGreetingFontFamily
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun DashboardBackgroundBrush(): Brush {
    val colors = MaterialTheme.colorScheme
    return Brush.linearGradient(
        colors = listOf(
            colors.primary.copy(alpha = 0.16f),
            colors.tertiary.copy(alpha = 0.13f),
            colors.secondary.copy(alpha = 0.10f),
            colors.surfaceVariant.copy(alpha = 0.12f),
            colors.background
        ),
        start = Offset.Zero,
        end = Offset(1300f, 2100f)
    )
}

internal enum class DashboardSection(
    val label: String,
    val icon: ImageVector,
    val topBarTitle: String
) {
    BOARD("每日看板", Icons.Rounded.ViewAgenda, "今日看板"),
    ACTIVE("我的任务", Icons.Rounded.TaskAlt, "我的任务"),
    CALENDAR("日历", Icons.Rounded.CalendarMonth, "Schedule"),
    HISTORY("历史记录", Icons.Rounded.History, "历史记录"),
    GROUPS("分组管理", Icons.Rounded.Folder, "分组管理"),
    SETTINGS("设置", Icons.Rounded.Settings, "设置")
}

@Composable
internal fun DashboardDrawer(
    current: DashboardSection,
    groups: List<TaskGroup>,
    selectedGroupId: Long?,
    selectedThemeMode: ThemeMode,
    onSelectSection: (DashboardSection) -> Unit,
    onActivateTasksSection: () -> Unit,
    onSelectAllTasks: () -> Unit,
    onSelectGroup: (Long) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    var taskExpanded by rememberSaveable { mutableStateOf(true) }
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.75f)
            .widthIn(min = 280.dp, max = 420.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 18.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_art),
                                contentDescription = "应用图标",
                                modifier = Modifier.size(38.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    Text(
                        "PaykiTodo",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                Column(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DrawerSectionButton(
                        section = DashboardSection.BOARD,
                        selected = current == DashboardSection.BOARD,
                        onClick = { onSelectSection(DashboardSection.BOARD) }
                    )
                    DrawerExpandableHeader(
                        icon = Icons.Rounded.TaskAlt,
                        title = "我的任务",
                        selected = current == DashboardSection.ACTIVE,
                        expanded = taskExpanded,
                        onClick = {
                            taskExpanded = !taskExpanded
                            onActivateTasksSection()
                        }
                    )
                    if (taskExpanded) {
                        DrawerTaskItem(
                            label = "全部任务",
                            selected = current == DashboardSection.ACTIVE && selectedGroupId == null,
                            colorHex = null,
                            onClick = onSelectAllTasks
                        )
                        groups.forEach { group ->
                            DrawerTaskItem(
                                label = group.name,
                                selected = current == DashboardSection.ACTIVE && selectedGroupId == group.id,
                                colorHex = group.colorHex,
                                onClick = { onSelectGroup(group.id) }
                            )
                        }
                    }
                    DashboardSection.entries.filter { it != DashboardSection.ACTIVE && it != DashboardSection.BOARD }.forEach { section ->
                        DrawerSectionButton(
                            section = section,
                            selected = current == section,
                            onClick = { onSelectSection(section) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                ThemeModeSwitcher(
                    selectedThemeMode = selectedThemeMode,
                    onThemeModeChange = onThemeModeChange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardTopBar(
    title: String,
    onMenu: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            IconButton(onClick = onMenu) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Menu, contentDescription = "打开菜单", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f))
                    }
                }
            }
        },
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.22f),
                        offset = Offset(0f, 2f),
                        blurRadius = 7f
                    )
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        actions = actions
    )
}

@Composable
internal fun TopBarActionPill(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.PostAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun DashboardBody(
    section: DashboardSection,
    settingsInitialSectionKey: String? = null,
    settingsInitialSectionSerial: Int = 0,
    padding: PaddingValues,
    uiState: TodoUiState,
    permissions: PermissionSnapshot,
    onEdit: (TodoItem) -> Unit,
    onEditCalendarEvent: (TodoItem) -> Unit,
    onQuickCreateCalendarEvent: (LocalDateTime, LocalDateTime) -> Unit,
    onMoveCalendarEvent: (TodoItem, LocalDateTime, LocalDateTime) -> Unit,
    onCompleteTodo: (TodoItem) -> Unit,
    onRestoreTodo: (TodoItem) -> Unit,
    onCancelTodo: (TodoItem) -> Unit,
    onDeleteTodo: (TodoItem) -> Unit,
    onDeleteCalendarEvent: (TodoItem) -> Unit,
    onCreateGroup: suspend (String, String) -> String?,
    onUpdateGroup: suspend (TaskGroup) -> String?,
    onDeleteGroup: suspend (Long) -> String?,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestFullScreenPermission: () -> Unit,
    onRequestNotificationPolicyAccess: () -> Unit,
    onRequestIgnoreBatteryOptimization: () -> Unit,
    onRequestAccessibilityService: () -> Unit,
    onWeekStartModeChange: (WeekStartMode) -> Unit,
    onNextQuote: () -> Unit,
    onDefaultSnoozeChange: (Int) -> Unit,
    onDefaultCalendarReminderModeChange: (ReminderDeliveryMode) -> Unit,
    onDesktopSyncEnabledChange: (Boolean) -> Unit,
    onRotateDesktopSyncToken: () -> Unit,
    onUseBuiltInReminderTone: () -> Unit,
    onPickSystemReminderTone: () -> Unit,
    onOpenWiki: () -> Unit,
    onRunReminderChainTest: suspend (Int) -> String?,
    onClearReminderDiagnostics: suspend () -> Unit,
    onSaveWeekAsScheduleTemplate: suspend (String, String, LocalDate) -> String?,
    onApplyScheduleTemplateToWeek: suspend (ScheduleTemplate, LocalDate) -> String?,
    onGenerateSemesterScheduleFromTemplate: suspend (ScheduleTemplate, LocalDate, LocalDate) -> String?,
    onDeleteScheduleTemplate: suspend (Long) -> String?,
    onPickBackupDirectory: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onAutoBackupChange: (Boolean) -> Unit,
    onOpenTodoBatchImport: () -> Unit,
    onOpenCalendarBatchImport: () -> Unit
) {
    if (section == DashboardSection.CALENDAR) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            CalendarPanel(
                modifier = Modifier.fillMaxSize(),
                events = uiState.calendarItems,
                groups = uiState.groups,
                weekStartMode = uiState.settings.weekStartMode,
                scheduleTemplates = uiState.scheduleTemplates,
                onQuickCreateEvent = onQuickCreateCalendarEvent,
                onCreateEventAt = onQuickCreateCalendarEvent,
                onEditEvent = onEditCalendarEvent,
                onMoveEvent = onMoveCalendarEvent,
                onDeleteEvent = onDeleteCalendarEvent,
                onOpenBatchImport = onOpenCalendarBatchImport,
                onSaveWeekAsTemplate = onSaveWeekAsScheduleTemplate,
                onApplyTemplateToWeek = onApplyScheduleTemplateToWeek,
                onGenerateSemesterFromTemplate = onGenerateSemesterScheduleFromTemplate,
                onDeleteTemplate = onDeleteScheduleTemplate
            )
        }
        return
    }

    if (section == DashboardSection.SETTINGS) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            SettingsPanel(
                settings = uiState.settings,
                permissions = permissions,
                defaultSnooze = uiState.settings.defaultSnoozeMinutes,
                crashLog = permissions.lastCrashLog,
                initialSectionKey = settingsInitialSectionKey,
                initialSectionSerial = settingsInitialSectionSerial,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                onRequestFullScreenPermission = onRequestFullScreenPermission,
                onRequestNotificationPolicyAccess = onRequestNotificationPolicyAccess,
                onRequestIgnoreBatteryOptimization = onRequestIgnoreBatteryOptimization,
                onRequestAccessibilityService = onRequestAccessibilityService,
                onWeekStartModeChange = onWeekStartModeChange,
                onDefaultSnoozeChange = onDefaultSnoozeChange,
                onDefaultCalendarReminderModeChange = onDefaultCalendarReminderModeChange,
                onDesktopSyncEnabledChange = onDesktopSyncEnabledChange,
                onRotateDesktopSyncToken = onRotateDesktopSyncToken,
                onUseBuiltInReminderTone = onUseBuiltInReminderTone,
                onPickSystemReminderTone = onPickSystemReminderTone,
                onOpenWiki = onOpenWiki,
                desktopSyncStatus = uiState.desktopSyncStatus,
                reminderChainLogs = uiState.reminderChainLogs,
                onRunReminderChainTest = onRunReminderChainTest,
                onClearReminderDiagnostics = onClearReminderDiagnostics,
                onPickBackupDirectory = onPickBackupDirectory,
                onExportBackup = onExportBackup,
                onImportBackup = onImportBackup,
                onAutoBackupChange = onAutoBackupChange,
                onCopyCrashLog = permissions.copyCrashLog,
                onClearCrashLog = permissions.clearCrashLog
            )
        }
        return
    }

    var missedExpanded by rememberSaveable { mutableStateOf(true) }
    var todayExpanded by rememberSaveable { mutableStateOf(true) }
    var upcomingExpanded by rememberSaveable(uiState.todayItems.isEmpty()) { mutableStateOf(uiState.todayItems.isEmpty()) }
    val boardMoment by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            delay(30_000L)
            value = LocalDateTime.now()
        }
    }
    val boardDate = boardMoment.toLocalDate()
    val boardTodoItems = remember(uiState.missedItems, uiState.todayItems) {
        (uiState.missedItems + uiState.todayItems)
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<TodoItem> { it.missed }
                    .thenBy { it.dueAtMillis }
            )
    }
    val todayScheduleItems = remember(uiState.calendarItems, boardDate, boardMoment) {
        uiState.calendarItems.filter { boardEventVisibleForToday(it, boardDate, boardMoment) }
            .sortedBy { it.startAtMillis ?: it.dueAtMillis }
    }
    val tomorrowScheduleItems = remember(uiState.calendarItems, boardDate) {
        val tomorrow = boardDate.plusDays(1)
        uiState.calendarItems.filter { boardEventOverlapsDay(it, tomorrow) }
            .sortedBy { it.startAtMillis ?: it.dueAtMillis }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = dashboardPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (section) {
            DashboardSection.BOARD -> {
                item {
                    CompactGreetingCard(
                        quote = uiState.currentQuote,
                        onNextQuote = onNextQuote
                    )
                }

                item {
                    DashboardQuickActionRow(
                        onOpenTodoBatchImport = onOpenTodoBatchImport,
                        onOpenCalendarBatchImport = onOpenCalendarBatchImport
                    )
                }

                item {
                    BoardBlockTitle("今日待办（${boardTodoItems.size}）")
                }
                if (boardTodoItems.isEmpty()) {
                    item { EmptyStateCard("今天还没有安排任务。") }
                } else {
                    items(boardTodoItems, key = { it.id }) { item ->
                        ActiveTodoCard(item, uiState.groups, { onEdit(item) }, { onCompleteTodo(item) }, { onCancelTodo(item) }, { onDeleteTodo(item) })
                    }
                }

                item {
                    BoardBlockTitle("今日日程（${todayScheduleItems.size}）")
                }
                item {
                    TodayScheduleBoardCard(
                        today = boardDate,
                        now = boardMoment,
                        todayEvents = todayScheduleItems,
                        tomorrowEvents = tomorrowScheduleItems,
                        onOpenEvent = onEditCalendarEvent
                    )
                }
            }

            DashboardSection.ACTIVE -> {
                item {
                    DashboardQuickActionRow(
                        onOpenTodoBatchImport = onOpenTodoBatchImport,
                        onOpenCalendarBatchImport = onOpenCalendarBatchImport
                    )
                }

                if (uiState.missedItems.isNotEmpty()) {
                    item {
                        ExpandableSectionHeader(
                            title = "已错过（${uiState.missedItems.size}）",
                            expanded = missedExpanded,
                            onToggle = { missedExpanded = !missedExpanded }
                        )
                    }
                    if (missedExpanded) {
                        items(uiState.missedItems, key = { it.id }) { item ->
                            ActiveTodoCard(item, uiState.groups, { onEdit(item) }, { onCompleteTodo(item) }, { onCancelTodo(item) }, { onDeleteTodo(item) })
                        }
                    }
                }

                item {
                    ExpandableSectionHeader(
                        title = "今日待办（${uiState.todayItems.size}）",
                        expanded = todayExpanded,
                        onToggle = { todayExpanded = !todayExpanded }
                    )
                }
                if (todayExpanded) {
                    if (uiState.todayItems.isEmpty()) {
                        item { EmptyStateCard("今天还没有安排任务。") }
                    } else {
                        items(uiState.todayItems, key = { it.id }) { item ->
                            ActiveTodoCard(item, uiState.groups, { onEdit(item) }, { onCompleteTodo(item) }, { onCancelTodo(item) }, { onDeleteTodo(item) })
                        }
                    }
                }

                item {
                    ExpandableSectionHeader(
                        title = "计划中（${uiState.upcomingItems.size}）",
                        expanded = upcomingExpanded,
                        onToggle = { upcomingExpanded = !upcomingExpanded }
                    )
                }
                if (upcomingExpanded) {
                    if (uiState.upcomingItems.isEmpty()) {
                        item { EmptyStateCard("后续时间暂时没有新计划。") }
                    } else {
                        items(uiState.upcomingItems, key = { it.id }) { item ->
                            ActiveTodoCard(item, uiState.groups, { onEdit(item) }, { onCompleteTodo(item) }, { onCancelTodo(item) }, { onDeleteTodo(item) })
                        }
                    }
                }
            }

            DashboardSection.HISTORY -> {
                if (uiState.historyItems.isEmpty()) {
                    item { EmptyStateCard("完成后的任务会保存在这里。") }
                } else {
                    items(uiState.historyItems, key = { it.id }) { item ->
                        CompletedTodoCard(item, uiState.groups, { onEdit(item) }, { onRestoreTodo(item) })
                    }
                }
            }

            DashboardSection.GROUPS -> item {
                GroupManagementPanel(
                    groups = uiState.groups,
                    onCreateGroup = onCreateGroup,
                    onUpdateGroup = onUpdateGroup,
                    onDeleteGroup = onDeleteGroup
                )
            }
            DashboardSection.CALENDAR -> Unit
            DashboardSection.SETTINGS -> Unit
        }
    }
}

@Composable
private fun DashboardQuickActionRow(
    onOpenTodoBatchImport: () -> Unit,
    onOpenCalendarBatchImport: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onOpenTodoBatchImport,
            modifier = Modifier.weight(1f)
        ) { Text("批量添加待办") }
        OutlinedButton(
            onClick = onOpenCalendarBatchImport,
            modifier = Modifier.weight(1f)
        ) { Text("批量添加日程") }
    }
}

@Composable
private fun ExpandableSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(title)
        Icon(
            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) "收起$title" else "展开$title",
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GreetingCard(
    quote: String,
    onNextQuote: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${timeGreeting()}，Payki",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = PaykiGreetingFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quote,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onNextQuote) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "更换短句", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun CompactGreetingCard(
    quote: String,
    onNextQuote: () -> Unit
) {
    var collapsed by rememberSaveable { mutableStateOf(false) }
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${timeGreeting()}，Payki",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PaykiGreetingFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!collapsed) {
                    Text(
                        text = quote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Visible
                    )
                }
            }
            IconButton(onClick = { collapsed = !collapsed }) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = if (collapsed) "展开欢迎语" else "折叠欢迎语",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = if (collapsed) 90f else 180f }
                )
            }
        }
    }
}

@Composable
private fun BoardBlockTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = 22.sp,
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.14f),
                offset = Offset(0f, 1.5f),
                blurRadius = 4f
            )
        ),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun TodayScheduleBoardCard(
    today: LocalDate,
    now: LocalDateTime,
    todayEvents: List<TodoItem>,
    tomorrowEvents: List<TodoItem>,
    onOpenEvent: (TodoItem) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.widthIn(min = 54.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = boardMonthLabel(today),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = boardWeekdayLabel(today),
                    color = Color(0xFF5A92FF),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = today.dayOfMonth.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (todayEvents.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = "今天暂无日程",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    todayEvents.forEach { item ->
                        BoardScheduleEventRow(
                            item = item,
                            now = now,
                            onClick = { onOpenEvent(item) }
                        )
                    }
                }

                if (tomorrowEvents.isNotEmpty()) {
                    Text(
                        text = "明天",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    tomorrowEvents.take(2).forEach { item ->
                        BoardScheduleEventRow(item = item, now = null, onClick = { onOpenEvent(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardScheduleEventRow(
    item: TodoItem,
    now: LocalDateTime?,
    onClick: () -> Unit
) {
    val tint = item.accentColorHex?.let(::colorFromHex) ?: MaterialTheme.colorScheme.primary
    val inProgress = now?.let { boardEventInProgress(item, it) } == true
    val gold = Color(0xFFFFC94A)
    val rowShape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (!inProgress) return@drawBehind
                val glowColor = gold.copy(alpha = 0.26f)
                drawRoundRect(
                    color = glowColor,
                    topLeft = Offset(-4.dp.toPx(), -3.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width + 8.dp.toPx(), size.height + 6.dp.toPx()),
                    cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
                )
            }
            .then(
                if (inProgress) {
                    Modifier.background(gold.copy(alpha = 0.08f), rowShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (inProgress) 10.dp else 0.dp, vertical = if (inProgress) 8.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = rowShape,
            color = Color.Transparent,
            border = if (inProgress) BorderStroke(1.4.dp, gold.copy(alpha = 0.92f)) else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (inProgress) 8.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(width = 5.dp, height = 64.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = if (inProgress) gold else tint.copy(alpha = 0.96f)
                ) {}
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (inProgress) gold else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = boardEventSecondaryText(item),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    item.location.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = 24.sp,
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.16f),
                offset = Offset(0f, 1.5f),
                blurRadius = 5f
            )
        ),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
internal fun EmptyStateCard(text: String) {
    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
        Text(
            text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
internal fun LaunchScreen() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val iconSize = 92.dp
        val sunCenterY = maxHeight * 0.23f
        val iconTop = sunCenterY - (iconSize / 2)
        val titleTop = sunCenterY + (iconSize / 2) + 18.dp

        Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFF5E6C6),
                            Color(0xFFD0EBF2),
                            Color(0xFF7FA5A7)
                        )
                    )
                )
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x55FFF6E5),
                radius = size.minDimension * 0.16f,
                center = center.copy(y = size.height * 0.23f)
            )

            drawCircle(
                color = Color(0x1AFFFFFF),
                radius = size.minDimension * 0.12f,
                center = center.copy(y = size.height * 0.23f),
                style = Stroke(width = 4.dp.toPx())
            )

            val mountainBack = Path().apply {
                moveTo(0f, size.height * 0.62f)
                cubicTo(size.width * 0.12f, size.height * 0.52f, size.width * 0.28f, size.height * 0.48f, size.width * 0.42f, size.height * 0.54f)
                cubicTo(size.width * 0.58f, size.height * 0.62f, size.width * 0.74f, size.height * 0.44f, size.width, size.height * 0.56f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountainBack, color = Color(0x7078A6AC))

            val mountainMid = Path().apply {
                moveTo(0f, size.height * 0.72f)
                cubicTo(size.width * 0.16f, size.height * 0.66f, size.width * 0.32f, size.height * 0.63f, size.width * 0.5f, size.height * 0.71f)
                cubicTo(size.width * 0.67f, size.height * 0.78f, size.width * 0.84f, size.height * 0.66f, size.width, size.height * 0.73f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountainMid, color = Color(0x9C5F8B90))

            val mountainFront = Path().apply {
                moveTo(0f, size.height * 0.8f)
                cubicTo(size.width * 0.14f, size.height * 0.76f, size.width * 0.34f, size.height * 0.75f, size.width * 0.46f, size.height * 0.83f)
                cubicTo(size.width * 0.62f, size.height * 0.91f, size.width * 0.82f, size.height * 0.81f, size.width, size.height * 0.84f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountainFront, color = Color(0xFF355258))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x12091920), Color(0x2A17313B), Color(0x18061115))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.85f)
                .offset(y = titleTop),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "PaykiTodo",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayLarge.copy(
                    shadow = Shadow(
                        color = Color(0x44111D1C),
                        offset = Offset(0f, 4f),
                        blurRadius = 12f
                    )
                ),
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF274B54)
            )
            Text(
                text = "专属于您的高效待办助手",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.5.sp),
                fontFamily = FontFamily.Cursive,
                color = Color(0xFF4A6670)
            )
        }

        Image(
            painter = painterResource(id = R.drawable.ic_launcher_art),
            contentDescription = "应用图标",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = iconTop)
                .size(iconSize),
            contentScale = ContentScale.Fit
        )

        Text(
            text = "© Copyright Hoshi Takyobu, 2026-2026",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xAA43616A)
        )
    }
    }
}

@Composable
private fun DrawerSectionButton(
    section: DashboardSection,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = section.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = section.label,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp),
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DrawerExpandableHeader(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DrawerTaskItem(
    label: String,
    selected: Boolean,
    colorHex: String?,
    onClick: () -> Unit
) {
    val tint = colorHex?.let(::colorFromHex) ?: MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(tint, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeModeSwitcher(
    selectedThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val systemDark = isSystemInDarkTheme()
    val currentIcon = when (selectedThemeMode) {
        ThemeMode.LIGHT -> Icons.Rounded.LightMode
        ThemeMode.DARK -> Icons.Rounded.DarkMode
        ThemeMode.SYSTEM -> if (systemDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode
    }
    val currentLabel = when (selectedThemeMode) {
        ThemeMode.LIGHT -> "浅色模式"
        ThemeMode.DARK -> "深色模式"
        ThemeMode.SYSTEM -> "跟随系统"
    }

    Box {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = currentIcon,
                    contentDescription = currentLabel,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 0.dp, y = (-8).dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                val icon = when (mode) {
                    ThemeMode.LIGHT -> Icons.Rounded.LightMode
                    ThemeMode.DARK -> Icons.Rounded.DarkMode
                    ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                }
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    onClick = {
                        expanded = false
                        onThemeModeChange(mode)
                    },
                    leadingIcon = { Icon(icon, contentDescription = null) }
                )
            }
        }
    }
}

private fun timeGreeting(): String = when (LocalTime.now().hour) {
    in 0..4 -> "凌晨好"
    in 5..10 -> "早上好"
    in 11..13 -> "中午好"
    in 14..17 -> "下午好"
    else -> "晚上好"
}

private fun boardEventOverlapsDay(item: TodoItem, date: LocalDate): Boolean {
    val start = item.startAtMillis?.let(::reminderAtMillisToDateTime)?.toLocalDate() ?: return false
    val end = if (item.allDay) {
        item.endAtMillis?.let(::reminderAtMillisToDateTime)?.toLocalDate()?.minusDays(1) ?: start
    } else {
        item.endAtMillis?.let(::reminderAtMillisToDateTime)?.toLocalDate() ?: start
    }
    return !date.isBefore(start) && !date.isAfter(end)
}

private fun boardEventVisibleForToday(item: TodoItem, today: LocalDate, now: LocalDateTime): Boolean {
    if (!boardEventOverlapsDay(item, today)) return false
    if (item.allDay) return true
    val end = item.endAtMillis?.let(::reminderAtMillisToDateTime)
        ?: item.startAtMillis?.let(::reminderAtMillisToDateTime)
        ?: return false
    return end.isAfter(now)
}

private fun boardEventInProgress(item: TodoItem, now: LocalDateTime): Boolean {
    val start = item.startAtMillis?.let(::reminderAtMillisToDateTime) ?: return false
    if (item.allDay) return boardEventOverlapsDay(item, now.toLocalDate())
    val end = item.endAtMillis?.let(::reminderAtMillisToDateTime) ?: start
    return !now.isBefore(start) && now.isBefore(end)
}

private fun boardEventSecondaryText(item: TodoItem): String {
    return if (item.allDay) {
        "全天"
    } else {
        val start = item.startAtMillis?.let(::reminderAtMillisToDateTime) ?: return "未设置时间"
        val end = item.endAtMillis?.let(::reminderAtMillisToDateTime) ?: start
        "${boardClockTime(start)} - ${boardClockTime(end)}"
    }
}

private fun boardWeekdayLabel(date: LocalDate): String = when (date.dayOfWeek.value) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    else -> "周日"
}

private fun boardMonthLabel(date: LocalDate): String = "${date.monthValue}月"

private fun boardClockTime(dateTime: LocalDateTime): String {
    return dateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA))
}
