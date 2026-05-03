package com.example.todoalarm.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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
    ACTIVE("我的任务", Icons.Rounded.TaskAlt, "PaykiTodo"),
    CALENDAR("日历", Icons.Rounded.CalendarMonth, "Schedule"),
    HISTORY("历史记录", Icons.Rounded.History, "历史记录"),
    GROUPS("分组管理", Icons.Rounded.Folder, "分组管理"),
    SETTINGS("设置", Icons.Rounded.Settings, "设置"),
    ABOUT("关于", Icons.Rounded.Info, "关于")
}

@Composable
internal fun DashboardDrawer(
    current: DashboardSection,
    groups: List<TaskGroup>,
    selectedGroupId: Long?,
    selectedThemeMode: ThemeMode,
    onSelectSection: (DashboardSection) -> Unit,
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
                    DrawerExpandableHeader(
                        title = "我的任务",
                        selected = current == DashboardSection.ACTIVE,
                        expanded = taskExpanded,
                        onClick = {
                            taskExpanded = !taskExpanded
                            onSelectSection(DashboardSection.ACTIVE)
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
                    DashboardSection.entries.filter { it != DashboardSection.ACTIVE }.forEach { section ->
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
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Menu, contentDescription = "打开菜单", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
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
internal fun DashboardFab(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick) {
        Icon(Icons.Rounded.Add, contentDescription = "新增任务")
    }
}

@Composable
internal fun DashboardBody(
    section: DashboardSection,
    padding: PaddingValues,
    uiState: TodoUiState,
    permissions: PermissionSnapshot,
    onEdit: (TodoItem) -> Unit,
    onEditCalendarEvent: (TodoItem) -> Unit,
    onQuickCreateCalendarEvent: (LocalDateTime, LocalDateTime) -> Unit,
    onCompleteTodo: (TodoItem) -> Unit,
    onRestoreTodo: (TodoItem) -> Unit,
    onCancelTodo: (TodoItem) -> Unit,
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

    var missedExpanded by rememberSaveable { mutableStateOf(true) }
    var todayExpanded by rememberSaveable { mutableStateOf(true) }
    var upcomingExpanded by rememberSaveable(uiState.todayItems.isEmpty()) { mutableStateOf(uiState.todayItems.isEmpty()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = dashboardPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (section) {
            DashboardSection.ACTIVE -> {
                item {
                    GreetingCard(
                        quote = uiState.currentQuote,
                        onNextQuote = onNextQuote
                    )
                }

                if (uiState.missedItems.isNotEmpty()) {
                    item {
                        ExpandableSectionHeader(
                            title = "已错过",
                            expanded = missedExpanded,
                            onToggle = { missedExpanded = !missedExpanded }
                        )
                    }
                    if (missedExpanded) {
                        items(uiState.missedItems, key = { it.id }) { item ->
                            ActiveTodoCard(item, uiState.groups, { onEdit(item) }, { onCompleteTodo(item) }, { onCancelTodo(item) })
                        }
                    }
                }

                item {
                    ExpandableSectionHeader(
                        title = "今日待办",
                        expanded = todayExpanded,
                        onToggle = { todayExpanded = !todayExpanded }
                    )
                }
                if (todayExpanded) {
                    if (uiState.todayItems.isEmpty()) {
                        item { EmptyStateCard("今天还没有安排任务。") }
                    } else {
                        items(uiState.todayItems, key = { it.id }) { item ->
                            ActiveTodoCard(item, uiState.groups, { onEdit(item) }, { onCompleteTodo(item) }, { onCancelTodo(item) })
                        }
                    }
                }

                item {
                    ExpandableSectionHeader(
                        title = "计划中",
                        expanded = upcomingExpanded,
                        onToggle = { upcomingExpanded = !upcomingExpanded }
                    )
                }
                if (upcomingExpanded) {
                    if (uiState.upcomingItems.isEmpty()) {
                        item { EmptyStateCard("后续时间暂时没有新计划。") }
                    } else {
                        items(uiState.upcomingItems, key = { it.id }) { item ->
                            ActiveTodoCard(item, uiState.groups, { onEdit(item) }, { onCompleteTodo(item) }, { onCancelTodo(item) })
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

            DashboardSection.SETTINGS -> item {
                SettingsPanel(
                    settings = uiState.settings,
                    permissions = permissions,
                    defaultSnooze = uiState.settings.defaultSnoozeMinutes,
                    crashLog = permissions.lastCrashLog,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                    onRequestFullScreenPermission = onRequestFullScreenPermission,
                    onRequestNotificationPolicyAccess = onRequestNotificationPolicyAccess,
                    onRequestIgnoreBatteryOptimization = onRequestIgnoreBatteryOptimization,
                    onRequestAccessibilityService = onRequestAccessibilityService,
                    onWeekStartModeChange = onWeekStartModeChange,
                    onDefaultSnoozeChange = onDefaultSnoozeChange,
                    onDefaultCalendarReminderModeChange = onDefaultCalendarReminderModeChange,
                    onUseBuiltInReminderTone = onUseBuiltInReminderTone,
                    onPickSystemReminderTone = onPickSystemReminderTone,
                    onOpenWiki = onOpenWiki,
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

            DashboardSection.ABOUT -> item { AboutPanel(onOpenWiki = onOpenWiki) }
            DashboardSection.CALENDAR -> Unit
        }
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
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFF2D9BB),
                            Color(0xFFBCD4D7),
                            Color(0xFF607E77)
                        )
                    )
                )
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x55FFF1D9),
                radius = size.minDimension * 0.16f,
                center = center.copy(y = size.height * 0.23f)
            )

            val mountainBack = Path().apply {
                moveTo(0f, size.height * 0.62f)
                cubicTo(size.width * 0.12f, size.height * 0.52f, size.width * 0.28f, size.height * 0.48f, size.width * 0.42f, size.height * 0.54f)
                cubicTo(size.width * 0.58f, size.height * 0.62f, size.width * 0.74f, size.height * 0.44f, size.width, size.height * 0.56f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountainBack, color = Color(0x886B8A7A))

            val mountainMid = Path().apply {
                moveTo(0f, size.height * 0.72f)
                cubicTo(size.width * 0.16f, size.height * 0.66f, size.width * 0.32f, size.height * 0.63f, size.width * 0.5f, size.height * 0.71f)
                cubicTo(size.width * 0.67f, size.height * 0.78f, size.width * 0.84f, size.height * 0.66f, size.width, size.height * 0.73f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountainMid, color = Color(0xAA466259))

            val mountainFront = Path().apply {
                moveTo(0f, size.height * 0.8f)
                cubicTo(size.width * 0.14f, size.height * 0.76f, size.width * 0.34f, size.height * 0.75f, size.width * 0.46f, size.height * 0.83f)
                cubicTo(size.width * 0.62f, size.height * 0.91f, size.width * 0.82f, size.height * 0.81f, size.width, size.height * 0.84f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountainFront, color = Color(0xFF2A3F42))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x220C1F24), Color(0x5521372E), Color(0x330A1114))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f)
                .offset(y = (-52).dp),
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
                color = Color(0xE0183538)
            )
            Text(
                text = "专属于您的高效待办助手",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.5.sp),
                fontFamily = FontFamily.Cursive,
                color = Color(0xD92D4D45)
            )
        }

        Text(
            text = "© Copyright Hoshi Takyobu, 2026-2026",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xCC2D4D45)
        )
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp),
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
