package com.example.todoalarm.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoalarm.data.ScheduleTemplate
import com.example.todoalarm.data.ScheduleTemplateType
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.WeekStartMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private const val CalendarDayRange = 730
private const val CalendarOverscanDays = 1
private const val VisibleCalendarDayColumns = 3.0f

private enum class CalendarViewMode(val label: String) {
    THREE_DAY("三日视图"),
    DAY("单日视图"),
    MONTH("月视图"),
    AGENDA("列表视图")
}

@Composable
internal fun CalendarPanel(
    modifier: Modifier = Modifier,
    events: List<TodoItem>,
    groups: List<TaskGroup>,
    weekStartMode: WeekStartMode,
    scheduleTemplates: List<ScheduleTemplate>,
    onQuickCreateEvent: (LocalDateTime, LocalDateTime) -> Unit,
    onCreateEventAt: (LocalDateTime, LocalDateTime) -> Unit,
    onEditEvent: (TodoItem) -> Unit,
    onDeleteEvent: (TodoItem) -> Unit,
    onOpenBatchImport: () -> Unit,
    onSaveWeekAsTemplate: suspend (String, String, LocalDate) -> String?,
    onApplyTemplateToWeek: suspend (ScheduleTemplate, LocalDate) -> String?,
    onGenerateSemesterFromTemplate: suspend (ScheduleTemplate, LocalDate, LocalDate) -> String?,
    onDeleteTemplate: suspend (Long) -> String?
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val anchorDate = remember { LocalDate.now() }
    var selectedDateEpochDay by rememberSaveable { mutableStateOf(anchorDate.toEpochDay()) }
    val selectedDate = remember(selectedDateEpochDay) { LocalDate.ofEpochDay(selectedDateEpochDay) }
    var viewMode by rememberSaveable { mutableStateOf(CalendarViewMode.THREE_DAY) }
    val days = remember(anchorDate) { (-CalendarDayRange..CalendarDayRange).map { anchorDate.plusDays(it.toLong()) } }
    val dayIndexByDate = remember(days) { days.withIndex().associate { it.value to it.index } }
    val anchorDateIndex = remember(anchorDate, dayIndexByDate) { dayIndexByDate.getValue(anchorDate) }
    val verticalScroll = rememberScrollState()
    var didInitScroll by rememberSaveable { mutableStateOf(false) }
    var horizontalOffsetPx by rememberSaveable { mutableStateOf(0f) }
    var detailsTarget by remember { mutableStateOf<TodoItem?>(null) }
    var pendingDraft by remember { mutableStateOf<PendingCalendarDraft?>(null) }
    var showTemplateManager by remember { mutableStateOf(false) }
    var showSaveWeekTemplate by remember { mutableStateOf(false) }
    var templateAnchorWeekStart by remember { mutableStateOf(currentWeekStart(LocalDate.now())) }
    var showViewModeMenu by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }
    var monthSwipeDirection by remember { mutableStateOf(0L) }
    var agendaSwipeDirection by remember { mutableStateOf(0L) }
    var monthVerticalDirection by remember { mutableStateOf(0L) }
    var agendaVerticalDirection by remember { mutableStateOf(0L) }
    var agendaRefreshing by remember { mutableStateOf(false) }
    val currentMoment by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            delay(30_000L)
            value = LocalDateTime.now()
        }
    }
    val currentDate = currentMoment.toLocalDate()
    val currentDateIndex = dayIndexByDate[currentDate] ?: anchorDateIndex

    val allDayEvents = remember(events) {
        events.filter { it.allDay }.sortedBy { it.startAtMillis ?: it.dueAtMillis }
    }
    val timedEvents = remember(events) {
        events.filter { !it.allDay }
            .sortedBy { it.startAtMillis ?: it.dueAtMillis }
    }
    val timedEventSegmentsByDay = remember(timedEvents) {
        buildTimedEventSegmentsByDay(timedEvents)
    }
    val timedEventPlacementsByDay = remember(timedEventSegmentsByDay) {
        timedEventSegmentsByDay.mapValues { (_, segments) ->
            layoutTimedEventSegments(segments)
        }
    }

    val calendarBackground = MaterialTheme.colorScheme.background
    val todayHighlightColor = Color(0x334C8BF5)
    val todayHighlightTextColor = Color(0xFF73A8FF)

    Surface(modifier = modifier, color = calendarBackground) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            val timelineDayColumns = when (viewMode) {
                CalendarViewMode.DAY -> 1f
                CalendarViewMode.THREE_DAY -> 3f
                else -> 3f
            }
            val timeAxisWidth = 48.dp
            val viewportWidth = (maxWidth - timeAxisWidth).coerceAtLeast(180.dp)
            val viewportWidthPx = with(density) { viewportWidth.toPx() }
            val dayColumnWidth = viewportWidth / timelineDayColumns
            val dayColumnWidthPx = with(density) { dayColumnWidth.toPx() }
            val maxHorizontalOffsetPx = ((days.size * dayColumnWidthPx) - viewportWidthPx).coerceAtLeast(0f)
            val selectedIndex = (dayIndexByDate[selectedDate] ?: anchorDateIndex).coerceIn(0, days.lastIndex)
            val timelineMode = viewMode == CalendarViewMode.THREE_DAY || viewMode == CalendarViewMode.DAY
            val effectiveHorizontalOffsetPx = when {
                viewMode == CalendarViewMode.THREE_DAY -> horizontalOffsetPx.coerceIn(0f, maxHorizontalOffsetPx)
                viewMode == CalendarViewMode.DAY -> (selectedIndex * dayColumnWidthPx).coerceIn(0f, maxHorizontalOffsetPx)
                else -> horizontalOffsetPx
            }
            val visibleRange = when {
                viewMode == CalendarViewMode.THREE_DAY -> calculateVisibleDayRange(days.size, dayColumnWidthPx, viewportWidthPx, effectiveHorizontalOffsetPx)
                viewMode == CalendarViewMode.DAY -> selectedIndex..selectedIndex
                else -> selectedIndex..(selectedIndex + 2).coerceAtMost(days.lastIndex)
            }
            val visibleDays = remember(days, visibleRange) { days.subList(visibleRange.first, visibleRange.last + 1) }
            val visibleStart = days[visibleRange.first]
            val visibleEnd = days[visibleRange.last]
            val timelineHorizontalOffsetPx = effectiveHorizontalOffsetPx
            val hourHeight = 72.dp
            val boardHeight = hourHeight * 24
            val hourHeightPx = with(density) { hourHeight.toPx() }
            val headerMonthDate = when (viewMode) {
                CalendarViewMode.MONTH -> selectedDate.withDayOfMonth(1)
                else -> selectedDate
            }
            val visibleAllDayEvents = remember(allDayEvents, visibleStart, visibleEnd) {
                allDayEvents.filter { item -> overlapsDateRange(item, visibleStart, visibleEnd) }
            }
            val visibleTimedEventPlacements = remember(timedEventPlacementsByDay, visibleDays) {
                visibleDays.flatMap { day ->
                    timedEventPlacementsByDay[day].orEmpty().map { placement -> day to placement }
                }
            }
            val horizontalScrollableState = rememberScrollableState { delta ->
                if (viewMode != CalendarViewMode.THREE_DAY) return@rememberScrollableState 0f
                val previous = horizontalOffsetPx
                horizontalOffsetPx = (horizontalOffsetPx - delta).coerceIn(0f, maxHorizontalOffsetPx)
                previous - horizontalOffsetPx
            }

            fun selectDate(targetDate: LocalDate) {
                selectedDateEpochDay = targetDate.toEpochDay()
                pendingDraft = null
            }

            fun shiftViewBy(daysDelta: Long) {
                selectDate(selectedDate.plusDays(daysDelta))
            }

            fun openDatePicker() {
                DatePickerDialog(
                    context,
                    { _, year, month, day -> selectDate(LocalDate.of(year, month + 1, day)) },
                    selectedDate.year,
                    selectedDate.monthValue - 1,
                    selectedDate.dayOfMonth
                ).show()
            }

            LaunchedEffect(hourHeightPx) {
                if (!didInitScroll) {
                    val targetHour = (currentMoment.hour - 2).coerceAtLeast(0)
                    verticalScroll.scrollTo((targetHour * hourHeightPx).roundToInt())
                    horizontalOffsetPx = (((anchorDateIndex - 1) * dayColumnWidthPx).coerceIn(0f, maxHorizontalOffsetPx))
                    didInitScroll = true
                }
            }

            LaunchedEffect(selectedDate) {
                templateAnchorWeekStart = currentWeekStart(selectedDate)
            }

            LaunchedEffect(viewMode, selectedDate, dayColumnWidthPx, maxHorizontalOffsetPx) {
                if (viewMode == CalendarViewMode.DAY) {
                    horizontalOffsetPx = (selectedIndex * dayColumnWidthPx).coerceIn(0f, maxHorizontalOffsetPx)
                }
            }

            LaunchedEffect(viewMode, effectiveHorizontalOffsetPx, dayColumnWidthPx) {
                if (viewMode == CalendarViewMode.THREE_DAY) {
                    val centerIndex = (((effectiveHorizontalOffsetPx + viewportWidthPx / 2f) / dayColumnWidthPx).toInt())
                        .coerceIn(0, days.lastIndex)
                    val centeredDate = days[centerIndex]
                    if (centeredDate != selectedDate) {
                        selectedDateEpochDay = centeredDate.toEpochDay()
                    }
                }
            }

            LaunchedEffect(agendaRefreshing, selectedDate) {
                if (agendaRefreshing) {
                    delay(280L)
                    agendaRefreshing = false
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                CalendarBrowserHeader(
                    titleMonth = headerMonthDate,
                    viewMode = viewMode,
                    onPickDate = ::openDatePicker,
                    onToday = { selectDate(currentDate) },
                    onChangeView = {
                        viewMode = it
                        showViewModeMenu = false
                    },
                    onOpenViewMenu = { showViewModeMenu = true },
                    showViewModeMenu = showViewModeMenu,
                    onOpenActions = { showActionsMenu = true },
                    showActionsMenu = showActionsMenu,
                    onDismissActions = { showActionsMenu = false },
                    onQuickCreate = {
                        showActionsMenu = false
                        val startAt = currentMoment.withSecond(0).withNano(0).plusMinutes(2)
                        onCreateEventAt(startAt, startAt.plusMinutes(30))
                    },
                    onOpenBatchImport = {
                        showActionsMenu = false
                        onOpenBatchImport()
                    },
                    onOpenTemplateManager = {
                        showActionsMenu = false
                        templateAnchorWeekStart = currentWeekStart(selectedDate)
                        showTemplateManager = true
                    },
                    onSaveWeekTemplate = {
                        showActionsMenu = false
                        templateAnchorWeekStart = currentWeekStart(selectedDate)
                        showSaveWeekTemplate = true
                    }
                )

                when (viewMode) {
                    CalendarViewMode.MONTH -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(selectedDate) {
                                    detectVerticalDragGestures(
                                        onDragEnd = {
                                            val next = selectedDate.withDayOfMonth(1)
                                            selectDate(next.plusMonths(monthVerticalDirection).withDayOfMonth(1))
                                            monthVerticalDirection = 0L
                                        }
                                    ) { _, dragAmount ->
                                        monthVerticalDirection = when {
                                            dragAmount < -6f -> 1L
                                            dragAmount > 6f -> -1L
                                            else -> monthVerticalDirection
                                        }
                                    }
                                }
                        ) {
                            CalendarMonthGridView(
                                monthDate = selectedDate.withDayOfMonth(1),
                                selectedDate = selectedDate,
                                weekStartMode = weekStartMode,
                                events = events,
                                onSelectDate = ::selectDate,
                                onOpenDetails = { detailsTarget = it }
                            )
                        }
                    }

                    CalendarViewMode.THREE_DAY,
                    CalendarViewMode.DAY -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (viewMode == CalendarViewMode.THREE_DAY) {
                                        Modifier.scrollable(horizontalScrollableState, Orientation.Horizontal)
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                CalendarHeaderRow(
                                    timeAxisWidth = timeAxisWidth,
                                    viewportWidth = viewportWidth,
                                    dayColumnWidth = dayColumnWidth,
                                    dayColumnWidthPx = dayColumnWidthPx,
                                    days = days,
                                    visibleRange = visibleRange,
                                    currentDate = currentDate,
                                    horizontalOffsetPx = timelineHorizontalOffsetPx,
                                    todayHighlightColor = todayHighlightColor,
                                    todayHighlightTextColor = todayHighlightTextColor,
                                    dragModifier = Modifier
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
                                CalendarAllDaySection(
                                    timeAxisWidth = timeAxisWidth,
                                    viewportWidth = viewportWidth,
                                    dayColumnWidth = dayColumnWidth,
                                    dayColumnWidthPx = dayColumnWidthPx,
                                    visibleRange = visibleRange,
                                    horizontalOffsetPx = timelineHorizontalOffsetPx,
                                    events = visibleAllDayEvents,
                                    dayIndexByDate = dayIndexByDate,
                                    dragModifier = Modifier,
                                    onOpenDetails = { detailsTarget = it }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    CalendarTimeAxis(
                                        width = timeAxisWidth,
                                        hourHeight = hourHeight,
                                        verticalScroll = verticalScroll,
                                        currentMoment = currentMoment,
                                        showCurrentTime = currentDateIndex in visibleRange
                                    )
                                    CalendarTimedBoard(
                                        modifier = Modifier
                                            .width(viewportWidth),
                                        days = days,
                                        visibleRange = visibleRange,
                                        dayColumnWidth = dayColumnWidth,
                                        dayColumnWidthPx = dayColumnWidthPx,
                                        horizontalOffsetPx = timelineHorizontalOffsetPx,
                                        boardHeight = boardHeight,
                                        hourHeight = hourHeight,
                                        hourHeightPx = hourHeightPx,
                                        verticalScroll = verticalScroll,
                                        currentMoment = currentMoment,
                                        visibleEventPlacements = visibleTimedEventPlacements,
                                        dayIndexByDate = dayIndexByDate,
                                        pendingDraft = pendingDraft,
                                        onPendingDraftChange = { pendingDraft = it },
                                        onQuickCreateEvent = onQuickCreateEvent,
                                        onOpenDetails = { detailsTarget = it },
                                        currentDayIndex = currentDateIndex
                                    )
                                }
                            }
                        }
                    }

                    CalendarViewMode.AGENDA -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(selectedDate) {
                                    detectVerticalDragGestures(
                                        onDragEnd = {
                                            when (agendaVerticalDirection) {
                                                1L -> {
                                                    agendaRefreshing = true
                                                    shiftViewBy(7)
                                                }
                                                -1L -> {
                                                    agendaRefreshing = true
                                                    shiftViewBy(-7)
                                                }
                                            }
                                            agendaVerticalDirection = 0L
                                        }
                                    ) { _, dragAmount ->
                                        agendaVerticalDirection = when {
                                            dragAmount < -6f -> 1L
                                            dragAmount > 6f -> -1L
                                            else -> agendaVerticalDirection
                                        }
                                    }
                                }
                        ) {
                            CalendarAgendaView(
                                selectedDate = selectedDate,
                                weekStartMode = weekStartMode,
                                events = events,
                                agendaRefreshing = agendaRefreshing,
                                onSelectDate = ::selectDate,
                                onOpenDetails = { detailsTarget = it }
                            )
                        }
                    }
                }
            }
        }
    }

    detailsTarget?.let { item ->
        CalendarEventDetailsDialog(
            item = item,
            onDismiss = { detailsTarget = null },
            onEdit = {
                detailsTarget = null
                onEditEvent(item)
            },
            onDelete = {
                detailsTarget = null
                onDeleteEvent(item)
            }
        )
    }

    if (showTemplateManager) {
        CalendarTemplateManagerDialog(
            templates = scheduleTemplates,
            anchorWeekStart = templateAnchorWeekStart,
            onDismiss = { showTemplateManager = false },
            onApplyTemplate = onApplyTemplateToWeek,
            onGenerateSemester = onGenerateSemesterFromTemplate,
            onDeleteTemplate = onDeleteTemplate
        )
    }

    if (showSaveWeekTemplate) {
        SaveWeekTemplateDialog(
            weekStart = templateAnchorWeekStart,
            onDismiss = { showSaveWeekTemplate = false },
            onSave = onSaveWeekAsTemplate
        )
    }
}

@Composable
private fun CalendarBrowserHeader(
    titleMonth: LocalDate,
    viewMode: CalendarViewMode,
    onPickDate: () -> Unit,
    onToday: () -> Unit,
    onChangeView: (CalendarViewMode) -> Unit,
    onOpenViewMenu: () -> Unit,
    showViewModeMenu: Boolean,
    onOpenActions: () -> Unit,
    showActionsMenu: Boolean,
    onDismissActions: () -> Unit,
    onQuickCreate: () -> Unit,
    onOpenBatchImport: () -> Unit,
    onOpenTemplateManager: () -> Unit,
    onSaveWeekTemplate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onPickDate),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = titleMonth.format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "选择日期",
                        modifier = Modifier.padding(2.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CalendarHeaderActionButton(label = "今天", onClick = onToday)
                Box {
                    CalendarHeaderActionButton(
                        icon = Icons.Rounded.ViewWeek,
                        contentDescription = "切换视图",
                        onClick = onOpenViewMenu
                    )
                    DropdownMenu(
                        expanded = showViewModeMenu,
                        onDismissRequest = { onChangeView(viewMode) }
                    ) {
                        CalendarViewMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label) },
                                onClick = { onChangeView(mode) }
                            )
                        }
                    }
                }
                Box {
                    CalendarHeaderActionButton(
                        icon = Icons.Rounded.MoreHoriz,
                        contentDescription = "更多操作",
                        onClick = onOpenActions
                    )
                    DropdownMenu(
                        expanded = showActionsMenu,
                        onDismissRequest = onDismissActions
                    ) {
                        DropdownMenuItem(text = { Text("快速新建") }, onClick = onQuickCreate)
                        DropdownMenuItem(text = { Text("批量导入") }, onClick = onOpenBatchImport)
                        DropdownMenuItem(text = { Text("周模板") }, onClick = onOpenTemplateManager)
                        DropdownMenuItem(text = { Text("保存本周模板") }, onClick = onSaveWeekTemplate)
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewMode.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Box {
                    CalendarHeaderActionButton(
                        label = "切换",
                        icon = Icons.Rounded.KeyboardArrowDown,
                        onClick = onOpenViewMenu
                    )
                    DropdownMenu(
                        expanded = showViewModeMenu,
                        onDismissRequest = { onChangeView(viewMode) }
                    ) {
                        CalendarViewMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label) },
                                onClick = { onChangeView(mode) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthGridView(
    monthDate: LocalDate,
    selectedDate: LocalDate,
    weekStartMode: WeekStartMode,
    events: List<TodoItem>,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDetails: (TodoItem) -> Unit
) {
    val monthStart = monthDate.withDayOfMonth(1)
    val monthEnd = monthDate.withDayOfMonth(monthDate.lengthOfMonth())
    val gridStart = startOfWeek(monthStart, weekStartMode)
    val gridEnd = startOfWeek(monthEnd, weekStartMode).plusDays(6)
    val gridDays = remember(monthStart, gridEnd) {
        generateSequence(gridStart) { current ->
            current.plusDays(1).takeIf { !it.isAfter(gridEnd) }
        }.toList()
    }
    val rows = remember(gridDays) { gridDays.chunked(7) }
    val weekdayLabels = weekdayLabels(weekStartMode)
    val eventsByDate = remember(events) { buildEventsByDate(events) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weekdayLabels.forEach { label ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }

        rows.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                week.forEach { date ->
                    val isCurrentMonth = date.month == monthDate.month
                    val isSelected = date == selectedDate
                    val dayEvents = eventsByDate[date].orEmpty().take(4)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp)
                            .clickable { onSelectDate(date) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isSelected) Color(0xFF2F6CFF) else Color.Transparent
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = when {
                                            isSelected -> Color.White
                                            isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                dayEvents.forEach { item ->
                                    MiniMonthEventChip(item = item, onClick = { onOpenDetails(item) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMonthEventChip(
    item: TodoItem,
    onClick: () -> Unit
) {
    val tint = item.accentColorHex?.let(::colorFromHex) ?: MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .width(2.dp)
                .height(14.dp),
            shape = RoundedCornerShape(999.dp),
            color = tint
        ) {}
        Text(
            text = item.title,
            color = tint.copy(alpha = calendarVisualAlpha(item)),
            fontSize = 10.sp,
            lineHeight = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CalendarAgendaView(
    selectedDate: LocalDate,
    weekStartMode: WeekStartMode,
    events: List<TodoItem>,
    agendaRefreshing: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDetails: (TodoItem) -> Unit
) {
    val weekStart = startOfWeek(selectedDate, weekStartMode)
    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
    val grouped = remember(events) {
        buildEventsByDate(events)
    }
    val agendaDates = remember(weekStart) { weekDays }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (agendaRefreshing) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ) {
                Text(
                    text = "正在载入下一周...",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weekDays.forEach { date ->
                val isSelected = date == selectedDate
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectDate(date) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(date.dayOfWeek.shortLabel(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            date.dayOfMonth.toString(),
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Text(
            text = weekRangeLabel(selectedDate, weekStartMode),
            modifier = Modifier.padding(horizontal = 10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        agendaDates.forEach { date ->
            val dayEvents = grouped[date].orEmpty()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.width(54.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(date.dayOfWeek.shortLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dayEvents.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "本日暂无日程",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        dayEvents.forEach { item ->
                            AgendaEventCard(item = item, onClick = { onOpenDetails(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaEventCard(
    item: TodoItem,
    onClick: () -> Unit
) {
    val tint = item.accentColorHex?.let(::colorFromHex) ?: MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = tint.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier
                    .width(3.dp)
                    .height(42.dp),
                shape = RoundedCornerShape(999.dp),
                color = tint.copy(alpha = 0.95f)
            ) {}
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.title, fontWeight = FontWeight.Bold, color = tint.copy(alpha = 0.98f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    if (item.allDay) "全天" else timeRangeLabel(item),
                    color = tint.copy(alpha = 0.84f),
                    fontSize = 12.sp
                )
                item.location.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = tint.copy(alpha = 0.78f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SaveWeekTemplateDialog(
    weekStart: LocalDate,
    onDismiss: () -> Unit,
    onSave: suspend (String, String, LocalDate) -> String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember(weekStart) { mutableStateOf("第${weekStart.monthValue}月周模板 ${weekStart}") }
    var selectedType by remember { mutableStateOf(ScheduleTemplateType.WEEKLY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存本周模板") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "将 ${weekStart} 这一周内的日程保存为模板，后续可复制到任意周，或直接生成整学期循环日程。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("模板名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        ScheduleTemplateType.WEEKLY to "课程表模板",
                        ScheduleTemplateType.DUTY_WEEK to "值班周模板",
                        ScheduleTemplateType.SEMESTER to "学期模板"
                    ).forEach { (type, label) ->
                        CalendarHeaderActionButton(
                            label = label,
                            onClick = { selectedType = type }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val message = onSave(name, selectedType, weekStart)
                    if (message == null) {
                        Toast.makeText(context, "模板已保存", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CalendarTemplateManagerDialog(
    templates: List<ScheduleTemplate>,
    anchorWeekStart: LocalDate,
    onDismiss: () -> Unit,
    onApplyTemplate: suspend (ScheduleTemplate, LocalDate) -> String?,
    onGenerateSemester: suspend (ScheduleTemplate, LocalDate, LocalDate) -> String?,
    onDeleteTemplate: suspend (Long) -> String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var applyDate by remember { mutableStateOf(anchorWeekStart) }
    var showSemesterDialogFor by remember { mutableStateOf<ScheduleTemplate?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("周模板与学期模板") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day -> applyDate = LocalDate.of(year, month + 1, day) },
                            applyDate.year,
                            applyDate.monthValue - 1,
                            applyDate.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("目标周起始日期：$applyDate")
                }

                if (templates.isEmpty()) {
                    Text("当前还没有已保存的课程表或值班模板。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    templates.forEach { template ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(template.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    when (template.templateType) {
                                        ScheduleTemplateType.DUTY_WEEK -> "值班周模板"
                                        ScheduleTemplateType.SEMESTER -> "学期模板"
                                        else -> "课程表周模板"
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            val message = onApplyTemplate(template, applyDate)
                                            if (message == null) {
                                                Toast.makeText(context, "已复制模板到目标周", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            } else {
                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }) {
                                        Text("复制到目标周")
                                    }
                                    OutlinedButton(onClick = { showSemesterDialogFor = template }) {
                                        Text("生成整学期")
                                    }
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            val message = onDeleteTemplate(template.id)
                                            if (message == null) {
                                                Toast.makeText(context, "模板已删除", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }) {
                                        Text("删除")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        dismissButton = {}
    )

    showSemesterDialogFor?.let { template ->
        SemesterGeneratorDialog(
            template = template,
            initialWeekStart = applyDate,
            onDismiss = { showSemesterDialogFor = null },
            onGenerate = onGenerateSemester
        )
    }
}

@Composable
private fun SemesterGeneratorDialog(
    template: ScheduleTemplate,
    initialWeekStart: LocalDate,
    onDismiss: () -> Unit,
    onGenerate: suspend (ScheduleTemplate, LocalDate, LocalDate) -> String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var firstWeekStart by remember(initialWeekStart) { mutableStateOf(initialWeekStart) }
    var endDate by remember(initialWeekStart) { mutableStateOf(initialWeekStart.plusDays(112)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("学期级循环生成") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "基于模板“${template.name}”生成整学期的每周循环日程。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day -> firstWeekStart = LocalDate.of(year, month + 1, day) },
                            firstWeekStart.year,
                            firstWeekStart.monthValue - 1,
                            firstWeekStart.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("首周起始日期：$firstWeekStart")
                }
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day -> endDate = LocalDate.of(year, month + 1, day) },
                            endDate.year,
                            endDate.monthValue - 1,
                            endDate.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("学期结束日期：$endDate")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val message = onGenerate(template, firstWeekStart, endDate)
                    if (message == null) {
                        Toast.makeText(context, "已生成学期循环日程", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text("生成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CalendarHeaderActionButton(
    label: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    contentDescription: String? = label,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (label == null) 9.dp else 10.dp,
                vertical = 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            label?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CalendarHeaderRow(
    timeAxisWidth: Dp,
    viewportWidth: Dp,
    dayColumnWidth: Dp,
    dayColumnWidthPx: Float,
    days: List<LocalDate>,
    visibleRange: IntRange,
    currentDate: LocalDate,
    horizontalOffsetPx: Float,
    todayHighlightColor: Color,
    todayHighlightTextColor: Color,
    dragModifier: Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(timeAxisWidth), contentAlignment = Alignment.CenterStart) {
            Text(text = timezoneShortLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Box(
            modifier = Modifier
                .width(viewportWidth)
                .clipToBounds()
                .then(dragModifier)
        ) {
            visibleRange.forEach { dayIndex ->
                val day = days[dayIndex]
                val isToday = day == currentDate
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(dayLeftPx(dayIndex, dayColumnWidthPx, horizontalOffsetPx), 0)
                        }
                        .width(dayColumnWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = if (isToday) todayHighlightColor else Color.Transparent,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = day.dayOfWeek.shortLabel(),
                                color = if (isToday) todayHighlightTextColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                                fontSize = 13.sp,
                                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium
                            )
                            Text(
                                text = day.dayOfMonth.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) todayHighlightTextColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.94f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarAllDaySection(
    timeAxisWidth: Dp,
    viewportWidth: Dp,
    dayColumnWidth: Dp,
    dayColumnWidthPx: Float,
    visibleRange: IntRange,
    horizontalOffsetPx: Float,
    events: List<TodoItem>,
    dayIndexByDate: Map<LocalDate, Int>,
    dragModifier: Modifier,
    onOpenDetails: (TodoItem) -> Unit
) {
    val rowCount = events.size.coerceIn(1, 3)
    val density = LocalDensity.current
    val rowHeightPx = with(density) { 28.dp.roundToPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height((rowCount * 30).dp + 14.dp)
            .padding(vertical = 6.dp)
    ) {
        Box(modifier = Modifier.width(timeAxisWidth), contentAlignment = Alignment.TopStart) {
            Text(text = "全天", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            modifier = Modifier
                .width(viewportWidth)
                .clipToBounds()
                .then(dragModifier)
        ) {
            events.forEachIndexed { rowIndex, item ->
                if (rowIndex >= rowCount) return@forEachIndexed
                val startDate = item.startAtMillis?.let(::millisToDate) ?: return@forEachIndexed
                val endDateInclusive = item.endAtMillis?.let(::millisToDate)?.minusDays(1) ?: startDate
                val startIndex = (dayIndexByDate[startDate] ?: visibleRange.first).coerceAtLeast(visibleRange.first)
                val endIndex = (dayIndexByDate[endDateInclusive] ?: visibleRange.last).coerceAtMost(visibleRange.last)
                if (endIndex < visibleRange.first || startIndex > visibleRange.last || endIndex < startIndex) return@forEachIndexed

                val tint = item.accentColorHex?.let(::colorFromHex) ?: MaterialTheme.colorScheme.primary
                Surface(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = dayLeftPx(startIndex, dayColumnWidthPx, horizontalOffsetPx) + with(density) { 4.dp.roundToPx() },
                                y = rowIndex * rowHeightPx
                            )
                        }
                        .width((dayColumnWidth * (endIndex - startIndex + 1)) - 8.dp)
                        .height(24.dp)
                        .clickable { onOpenDetails(item) },
                    shape = RoundedCornerShape(10.dp),
                    color = tint.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, tint.copy(alpha = 0.24f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(3.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = tint.copy(alpha = 0.95f)
                        ) {}
                        Text(
                            text = item.title,
                            color = tint.copy(alpha = calendarVisualAlpha(item)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarTimeAxis(
    width: Dp,
    hourHeight: Dp,
    verticalScroll: androidx.compose.foundation.ScrollState,
    currentMoment: LocalDateTime,
    showCurrentTime: Boolean
) {
    val markerOffset = (hourHeight * ((currentMoment.hour * 60 + currentMoment.minute) / 60f)) - 10.dp

    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .verticalScroll(verticalScroll)
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(hourHeight * 24)
        ) {
            repeat(24) { hour ->
                Box(
                    modifier = Modifier
                        .offset(y = if (hour == 0) 0.dp else (hourHeight * hour) - 9.dp)
                        .height(18.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "%02d:00".format(hour),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 11.sp
                    )
                }
            }

            if (showCurrentTime) {
                Surface(
                    modifier = Modifier.offset(y = if (markerOffset < 0.dp) 0.dp else markerOffset),
                    color = Color(0xFFE53935),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = formatClockTime(currentMoment),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarTimedBoard(
    modifier: Modifier,
    days: List<LocalDate>,
    visibleRange: IntRange,
    dayColumnWidth: Dp,
    dayColumnWidthPx: Float,
    horizontalOffsetPx: Float,
    boardHeight: Dp,
    hourHeight: Dp,
    hourHeightPx: Float,
    verticalScroll: androidx.compose.foundation.ScrollState,
    currentMoment: LocalDateTime,
    visibleEventPlacements: List<Pair<LocalDate, TimedEventPlacement>>,
    dayIndexByDate: Map<LocalDate, Int>,
    pendingDraft: PendingCalendarDraft?,
    onPendingDraftChange: (PendingCalendarDraft?) -> Unit,
    onQuickCreateEvent: (LocalDateTime, LocalDateTime) -> Unit,
    onOpenDetails: (TodoItem) -> Unit,
    currentDayIndex: Int
) {
    val density = LocalDensity.current
    val outline = MaterialTheme.colorScheme.outline
    val latestHorizontalOffsetPx by rememberUpdatedState(horizontalOffsetPx)
    val latestPendingDraft by rememberUpdatedState(pendingDraft)

    Box(
        modifier = modifier
            .clipToBounds()
            .verticalScroll(verticalScroll)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(boardHeight)
                .pointerInput(days, dayColumnWidthPx, hourHeightPx) {
                    detectTapGestures { tapOffset ->
                        val dayIndex = ((tapOffset.x + latestHorizontalOffsetPx) / dayColumnWidthPx)
                            .toInt()
                            .coerceIn(0, days.lastIndex)
                        val rawMinutes = ((tapOffset.y / hourHeightPx) * 60f).roundToInt().coerceIn(0, 23 * 60 + 59)
                        val snappedMinutes = snapToQuarterHour(rawMinutes).coerceIn(0, 23 * 60 + 45)
                        val startAt = LocalDateTime.of(
                            days[dayIndex],
                            LocalTime.of(snappedMinutes / 60, snappedMinutes % 60)
                        )
                        val endAt = startAt.plusMinutes(30)
                        val nextDraft = PendingCalendarDraft(startAt, endAt)
                        if (latestPendingDraft == nextDraft) {
                            onQuickCreateEvent(startAt, endAt)
                            onPendingDraftChange(null)
                        } else {
                            onPendingDraftChange(nextDraft)
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                visibleRange.forEach { dayIndex ->
                    val x = dayLeftPx(dayIndex, dayColumnWidthPx, horizontalOffsetPx).toFloat()
                    drawLine(
                        color = outline.copy(alpha = 0.10f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height)
                    )
                }
                val endBoundaryX = dayLeftPx(visibleRange.last + 1, dayColumnWidthPx, horizontalOffsetPx).toFloat()
                drawLine(
                    color = outline.copy(alpha = 0.10f),
                    start = Offset(endBoundaryX, 0f),
                    end = Offset(endBoundaryX, size.height)
                )
                repeat(25) { hour ->
                    val y = hour * hourHeightPx
                    drawLine(
                        color = outline.copy(alpha = 0.30f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y)
                    )
                }
            }

            visibleEventPlacements.forEach { (day, placement) ->
                val dayIndex = dayIndexByDate[day] ?: return@forEach
                    TimedEventCard(
                        segment = placement.segment,
                        placement = placement,
                        dayIndex = dayIndex,
                        dayColumnWidth = dayColumnWidth,
                        dayColumnWidthPx = dayColumnWidthPx,
                        horizontalOffsetPx = horizontalOffsetPx,
                        hourHeight = hourHeight,
                        onClick = { onOpenDetails(placement.segment.item) }
                    )
            }

            pendingDraft?.let { draft ->
                PendingDraftCard(
                    dayIndex = dayIndexByDate[draft.startAt.toLocalDate()] ?: return@let,
                    dayColumnWidth = dayColumnWidth,
                    dayColumnWidthPx = dayColumnWidthPx,
                    horizontalOffsetPx = horizontalOffsetPx,
                    hourHeight = hourHeight,
                    draft = draft,
                    onClick = { onQuickCreateEvent(draft.startAt, draft.endAt) }
                )
            }

            CurrentTimeLine(
                currentDayIndex = currentDayIndex,
                currentMoment = currentMoment,
                dayColumnWidthPx = dayColumnWidthPx,
                horizontalOffsetPx = horizontalOffsetPx,
                boardHeightPx = with(density) { boardHeight.toPx() },
                hourHeightPx = hourHeightPx
            )
        }
    }
}

@Composable
private fun PendingDraftCard(
    dayIndex: Int,
    dayColumnWidth: Dp,
    dayColumnWidthPx: Float,
    horizontalOffsetPx: Float,
    hourHeight: Dp,
    draft: PendingCalendarDraft,
    onClick: () -> Unit
) {
    val startMinutes = draft.startAt.hour * 60 + draft.startAt.minute
    val durationMinutes = 30L
    val topOffset = hourHeight * (startMinutes / 60f)
    val accent = Color(0xFF4C8BF5)

    Box(
        modifier = Modifier
            .offset(
                x = with(LocalDensity.current) { dayLeftPx(dayIndex, dayColumnWidthPx, horizontalOffsetPx).toDp() } + 4.dp,
                y = topOffset
            )
            .width(dayColumnWidth - 8.dp)
            .height(hourHeight * (durationMinutes / 60f))
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 2.dp.toPx()
            val radius = 14.dp.toPx()
            drawRoundRect(
                color = accent.copy(alpha = 0.10f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
            )
            drawRoundRect(
                color = accent,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                style = Stroke(width = strokeWidth)
            )
        }
        Text(
            text = "新日程",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            color = accent,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun TimedEventCard(
    segment: TimedEventSegment,
    placement: TimedEventPlacement,
    dayIndex: Int,
    dayColumnWidth: Dp,
    dayColumnWidthPx: Float,
    horizontalOffsetPx: Float,
    hourHeight: Dp,
    onClick: () -> Unit
) {
    val item = segment.item
    val startDateTime = reminderAtMillisToDateTime(segment.startMillis)
    val endDateTime = reminderAtMillisToDateTime(segment.endMillis)
    val startMinutes = startDateTime.hour * 60 + startDateTime.minute
    val durationMinutes = java.time.Duration.between(startDateTime, endDateTime).toMinutes().coerceAtLeast(20)
    val tint = item.accentColorHex?.let(::colorFromHex) ?: MaterialTheme.colorScheme.primary
    val alpha = calendarVisualAlpha(item)
    val topOffset = hourHeight * (startMinutes / 60f)
    val eventWidth = (dayColumnWidth / placement.columnCount) - 8.dp
    val eventHeight = (hourHeight * (durationMinutes / 60f)).coerceAtLeast(48.dp)
    val baseLeft = with(LocalDensity.current) { dayLeftPx(dayIndex, dayColumnWidthPx, horizontalOffsetPx).toDp() }
    val leftOffset = baseLeft +
        (dayColumnWidth / placement.columnCount) * placement.columnIndex +
        4.dp
    val showLocation = durationMinutes >= 70 || eventHeight >= 72.dp || eventWidth >= 126.dp
    val titleMaxLines = when {
        eventHeight >= 144.dp -> 6
        eventHeight >= 108.dp -> 5
        showLocation -> 4
        else -> 5
    }
    val locationMaxLines = when {
        eventHeight >= 132.dp -> 3
        eventHeight >= 84.dp -> 2
        else -> 1
    }

    Surface(
        modifier = Modifier
            .offset(x = leftOffset, y = topOffset)
            .width(eventWidth)
            .height(eventHeight)
            .clickable(onClick = onClick)
            .alpha(alpha),
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp),
                shape = RoundedCornerShape(999.dp),
                color = tint.copy(alpha = 0.95f)
            ) {}
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(if (showLocation) 3.dp else 2.dp)
            ) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    color = tint.copy(alpha = 0.98f),
                    fontSize = 12.sp,
                    lineHeight = 13.sp,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
                if (showLocation && item.location.isNotBlank()) {
                    Text(
                        text = item.location,
                        color = tint.copy(alpha = 0.78f),
                        fontSize = 10.sp,
                        lineHeight = 11.sp,
                        maxLines = locationMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentTimeLine(
    currentDayIndex: Int,
    currentMoment: LocalDateTime,
    dayColumnWidthPx: Float,
    horizontalOffsetPx: Float,
    boardHeightPx: Float,
    hourHeightPx: Float
) {
    val minutes = currentMoment.hour * 60 + currentMoment.minute
    val y = (minutes / 60f) * hourHeightPx

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (currentDayIndex < 0 || y !in 0f..boardHeightPx) return@Canvas
        val splitX = currentDayIndex * dayColumnWidthPx - horizontalOffsetPx
        val lightEnd = splitX.coerceIn(0f, size.width)
        val darkStart = splitX.coerceIn(0f, size.width)

        if (lightEnd > 0f) {
            drawLine(
                color = Color(0xFFF3B2B2),
                start = Offset(0f, y),
                end = Offset(lightEnd, y),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        if (splitX < size.width) {
            drawLine(
                color = Color(0xFFE53935),
                start = Offset(darkStart, y),
                end = Offset(size.width, y),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        if (splitX in 0f..size.width) {
            drawCircle(
                color = Color(0xFFD32F2F),
                radius = 6.dp.toPx(),
                center = Offset(splitX, y)
            )
        }
    }
}

@Composable
private fun CalendarEventDetailsDialog(
    item: TodoItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                item.location.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = if (item.allDay) "全天 · ${formatDateRange(item)}" else formatDateTimeRange(item),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (item.notes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
                    ) {
                        Text(
                            text = item.notes,
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 19.sp
                        )
                    }
                }
                item.reminderAtMillis?.let {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
                    ) {
                        Text(
                            text = "提醒：${formatLocalDateTime(reminderAtMillisToDateTime(it))}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Text("编辑")
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Text("删除")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

private data class PendingCalendarDraft(
    val startAt: LocalDateTime,
    val endAt: LocalDateTime
)

private data class TimedEventSegment(
    val item: TodoItem,
    val startMillis: Long,
    val endMillis: Long
)

private data class TimedEventPlacement(
    val segment: TimedEventSegment,
    val columnIndex: Int,
    val columnCount: Int
)

private fun calculateVisibleDayRange(
    totalDays: Int,
    dayColumnWidthPx: Float,
    viewportWidthPx: Float,
    horizontalOffsetPx: Float
): IntRange {
    val visibleStart = ((horizontalOffsetPx / dayColumnWidthPx).toInt() - CalendarOverscanDays).coerceAtLeast(0)
    val visibleEnd = (((horizontalOffsetPx + viewportWidthPx) / dayColumnWidthPx).toInt() + CalendarOverscanDays)
        .coerceAtMost(totalDays - 1)
    return visibleStart..visibleEnd
}

private fun dayLeftPx(
    dayIndex: Int,
    dayColumnWidthPx: Float,
    horizontalOffsetPx: Float
): Int {
    return (dayIndex * dayColumnWidthPx - horizontalOffsetPx).roundToInt()
}

private fun layoutTimedEventSegments(segments: List<TimedEventSegment>): List<TimedEventPlacement> {
    if (segments.isEmpty()) return emptyList()
    val sorted = segments.sortedBy { it.startMillis }
    val result = mutableListOf<TimedEventPlacement>()
    var cluster = mutableListOf<Pair<TimedEventSegment, Int>>()
    var active = mutableListOf<Pair<TimedEventSegment, Int>>()
    var clusterMaxColumns = 1
    var clusterEnd = Long.MIN_VALUE

    fun flushCluster() {
        if (cluster.isEmpty()) return
        cluster.forEach { (segment, column) ->
            result += TimedEventPlacement(segment, column, clusterMaxColumns)
        }
        cluster = mutableListOf()
        active = mutableListOf()
        clusterMaxColumns = 1
        clusterEnd = Long.MIN_VALUE
    }

    sorted.forEach { segment ->
        val start = segment.startMillis
        val end = segment.endMillis
        if (cluster.isNotEmpty() && start >= clusterEnd) flushCluster()
        active = active.filterTo(mutableListOf()) { activeItem ->
            activeItem.first.endMillis > start
        }
        val usedColumns = active.map { it.second }.toSet()
        var column = 0
        while (column in usedColumns) column += 1
        cluster += segment to column
        active += segment to column
        clusterMaxColumns = max(clusterMaxColumns, active.size)
        clusterEnd = max(clusterEnd, end)
    }
    flushCluster()
    return result
}

private fun buildTimedEventSegmentsByDay(events: List<TodoItem>): Map<LocalDate, List<TimedEventSegment>> {
    if (events.isEmpty()) return emptyMap()
    val segmentsByDay = linkedMapOf<LocalDate, MutableList<TimedEventSegment>>()
    events.forEach { item ->
        val itemStart = item.startAtMillis ?: item.dueAtMillis
        val itemEnd = item.endAtMillis ?: (itemStart + 30 * 60_000L)
        val startDate = millisToDate(itemStart)
        val endDate = millisToDate(itemEnd - 1)
        var dayCursor = startDate
        while (!dayCursor.isAfter(endDate)) {
            item.toSegmentForDay(dayCursor)?.let { segment ->
                segmentsByDay.getOrPut(dayCursor) { mutableListOf() }.add(segment)
            }
            dayCursor = dayCursor.plusDays(1)
        }
    }
    return segmentsByDay.mapValues { (_, segments) -> segments.sortedBy { it.startMillis } }
}

private fun TodoItem.toSegmentForDay(day: LocalDate): TimedEventSegment? {
    val itemStart = startAtMillis ?: dueAtMillis
    val itemEnd = endAtMillis ?: (itemStart + 30 * 60_000L)
    val zoneId = ZoneId.systemDefault()
    val dayStart = day.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val dayEnd = day.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    if (itemEnd <= dayStart || itemStart >= dayEnd) return null
    val segmentStart = max(itemStart, dayStart)
    val segmentEnd = max(segmentStart + 60_000L, minOf(itemEnd, dayEnd))
    return TimedEventSegment(
        item = this,
        startMillis = segmentStart,
        endMillis = segmentEnd
    )
}

private fun overlapsDateRange(
    item: TodoItem,
    startDate: LocalDate,
    endDate: LocalDate
): Boolean {
    val start = item.startAtMillis ?: item.dueAtMillis
    val end = item.endAtMillis ?: start
    val itemStartDate = millisToDate(start)
    val itemEndDate = millisToDate(if (item.allDay) end - 1 else end)
    return !itemEndDate.isBefore(startDate) && !itemStartDate.isAfter(endDate)
}

private fun millisToDate(epochMillis: Long): LocalDate {
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
}

private fun formatDateTimeRange(item: TodoItem): String {
    val start = item.startAtMillis?.let(::reminderAtMillisToDateTime) ?: return "未设置时间"
    val end = item.endAtMillis?.let(::reminderAtMillisToDateTime) ?: start
    val dateFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
    return if (start.toLocalDate() == end.toLocalDate()) {
        "${start.format(dateFormatter)} ${formatClockTime(start)} - ${formatClockTime(end)}"
    } else {
        "${formatLocalDateTime(start)} - ${formatLocalDateTime(end)}"
    }
}

private fun formatDateRange(item: TodoItem): String {
    val start = item.startAtMillis?.let(::millisToDate) ?: return "未设置日期"
    val endExclusive = item.endAtMillis?.let(::millisToDate)?.minusDays(1) ?: start
    return if (start == endExclusive) start.toString() else "$start - $endExclusive"
}

private fun calendarVisualAlpha(item: TodoItem): Float {
    val now = System.currentTimeMillis()
    val end = item.endAtMillis ?: item.startAtMillis ?: item.dueAtMillis
    return if (end < now) 0.45f else 1f
}

private fun timezoneShortLabel(): String {
    val totalSeconds = ZoneId.systemDefault().rules.getOffset(Instant.now()).totalSeconds
    val totalHours = totalSeconds / 3600
    return if (totalHours >= 0) "GMT+$totalHours" else "GMT$totalHours"
}

private fun formatClockTime(dateTime: LocalDateTime): String {
    return dateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA))
}

private fun snapToQuarterHour(totalMinutes: Int): Int {
    return ((totalMinutes + 7) / 15) * 15
}

private fun weekdayLabels(weekStartMode: WeekStartMode): List<String> {
    return when (weekStartMode) {
        WeekStartMode.MONDAY -> listOf("一", "二", "三", "四", "五", "六", "日")
        WeekStartMode.SUNDAY -> listOf("日", "一", "二", "三", "四", "五", "六")
    }
}

private fun buildEventsByDate(events: List<TodoItem>): Map<LocalDate, List<TodoItem>> {
    if (events.isEmpty()) return emptyMap()
    val bucket = linkedMapOf<LocalDate, MutableList<TodoItem>>()
    events.sortedBy { it.startAtMillis ?: it.dueAtMillis }.forEach { item ->
        val startMillis = item.startAtMillis ?: item.dueAtMillis
        val endMillis = item.endAtMillis ?: startMillis
        var cursor = millisToDate(startMillis)
        val inclusiveEnd = millisToDate(if (item.allDay) endMillis - 1 else endMillis)
        while (!cursor.isAfter(inclusiveEnd)) {
            bucket.getOrPut(cursor) { mutableListOf() }.add(item)
            cursor = cursor.plusDays(1)
        }
    }
    return bucket
}

private fun timeRangeLabel(item: TodoItem): String {
    val start = item.startAtMillis?.let(::reminderAtMillisToDateTime) ?: return "未设置时间"
    val end = item.endAtMillis?.let(::reminderAtMillisToDateTime) ?: start
    return "${formatClockTime(start)} - ${formatClockTime(end)}"
}

private fun currentWeekStart(date: LocalDate): LocalDate {
    return date.minusDays((date.dayOfWeek.value - 1).toLong())
}

private fun startOfWeek(date: LocalDate, weekStartMode: WeekStartMode): LocalDate {
    val desired = when (weekStartMode) {
        WeekStartMode.MONDAY -> java.time.DayOfWeek.MONDAY
        WeekStartMode.SUNDAY -> java.time.DayOfWeek.SUNDAY
    }
    var cursor = date
    while (cursor.dayOfWeek != desired) {
        cursor = cursor.minusDays(1)
    }
    return cursor
}

private fun weekRangeLabel(date: LocalDate, weekStartMode: WeekStartMode): String {
    val start = startOfWeek(date, weekStartMode)
    val end = start.plusDays(6)
    return buildString {
        append(if (weekStartMode == WeekStartMode.MONDAY) "周一到周日" else "周日到周六")
        append(" · ")
        append(start.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)))
        append(" - ")
        append(end.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)))
    }
}

private fun java.time.DayOfWeek.shortLabel(): String = when (this) {
    java.time.DayOfWeek.MONDAY -> "周一"
    java.time.DayOfWeek.TUESDAY -> "周二"
    java.time.DayOfWeek.WEDNESDAY -> "周三"
    java.time.DayOfWeek.THURSDAY -> "周四"
    java.time.DayOfWeek.FRIDAY -> "周五"
    java.time.DayOfWeek.SATURDAY -> "周六"
    java.time.DayOfWeek.SUNDAY -> "周日"
}
