package com.example.todoalarm.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewDay
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.sync.canReadCalendarItem
import com.example.todoalarm.data.EventCheckIn
import com.example.todoalarm.data.LunarCalendar
import com.example.todoalarm.data.ScheduleTemplate
import com.example.todoalarm.data.ScheduleTemplateType
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.WeekStartMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.time.ZoneId
import com.example.todoalarm.data.RecurrenceScope
import com.example.todoalarm.data.calendarStartDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private const val CalendarDayRange = 730
private const val CalendarOverscanDays = 0
private const val CalendarPagerCenter = 5_000
private const val CalendarPagerPages = CalendarPagerCenter * 2 + 1
private const val CalendarVerticalOverscanHours = 1f
private const val VisibleCalendarDayColumns = 3.0f

private class CalendarDateWindow(
    private val anchorDate: LocalDate,
    private val rangeDays: Int
) {
    val size: Int = rangeDays * 2 + 1
    val lastIndex: Int = size - 1
    val anchorIndex: Int = rangeDays

    fun dateAt(index: Int): LocalDate {
        return anchorDate.plusDays((index - rangeDays).toLong())
    }

    fun indexOf(date: LocalDate): Int? {
        val index = rangeDays + java.time.temporal.ChronoUnit.DAYS.between(anchorDate, date).toInt()
        return index.takeIf { it in 0..lastIndex }
    }
}

private enum class CalendarViewMode(val label: String) {
    THREE_DAY("三日视图"),
    DAY("单日视图"),
    MONTH("月视图"),
    AGENDA("列表视图")
}

private data class CalendarRequestedEventRange(
    val startInclusive: LocalDate,
    val endExclusive: LocalDate
)

private fun calendarRequestedEventRange(
    viewMode: CalendarViewMode,
    selectedDate: LocalDate,
    visibleDays: List<LocalDate>,
    weekStartMode: WeekStartMode
): CalendarRequestedEventRange {
    return when (viewMode) {
        CalendarViewMode.THREE_DAY,
        CalendarViewMode.DAY -> {
            val start = visibleDays.minOrNull() ?: selectedDate
            val end = (visibleDays.maxOrNull() ?: selectedDate).plusDays(1)
            CalendarRequestedEventRange(start, end)
        }
        CalendarViewMode.MONTH -> {
            val monthStart = selectedDate.withDayOfMonth(1)
            val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
            CalendarRequestedEventRange(
                startInclusive = startOfWeek(monthStart, weekStartMode),
                endExclusive = startOfWeek(monthEnd, weekStartMode).plusDays(7)
            )
        }
        CalendarViewMode.AGENDA -> {
            val weekStart = startOfWeek(selectedDate, weekStartMode)
            CalendarRequestedEventRange(weekStart, weekStart.plusDays(7))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CalendarPanel(
    modifier: Modifier = Modifier,
    events: List<TodoItem>,
    weekStartMode: WeekStartMode,
    scheduleTemplates: List<ScheduleTemplate>,
    targetEventId: Long? = null,
    targetEventSerial: Int = 0,
    initialFocusDate: LocalDate? = null,
    initialFocusDateSerial: Int = 0,
    onVisibleDateRangeChange: (LocalDate, LocalDate) -> Unit = { _, _ -> },
    onQuickCreateEvent: (LocalDateTime, LocalDateTime) -> Unit,
    onCreateEventAt: (LocalDateTime, LocalDateTime) -> Unit,
    onEditEvent: (TodoItem) -> Unit,
    onGetEventById: suspend (Long) -> TodoItem?,
    onGetEventCheckIns: suspend (Long) -> List<EventCheckIn>,
    onCopyEvent: (TodoItem) -> Unit,
    onMoveEvent: (TodoItem, LocalDateTime, LocalDateTime, RecurrenceScope) -> Unit,
    onCancelEvent: (TodoItem) -> Unit,
    onDeleteEvent: (TodoItem) -> Unit,
    onOpenBatchImport: () -> Unit,
    onSaveWeekAsTemplate: suspend (String, String, LocalDate) -> String?,
    onApplyTemplateToWeek: suspend (ScheduleTemplate, LocalDate) -> String?,
    onGenerateSemesterFromTemplate: suspend (ScheduleTemplate, LocalDate, LocalDate) -> String?,
    onDeleteTemplate: suspend (Long) -> String?
) {
    val workspace = LocalWorkspacePreferences.current
    val displayZone = remember(workspace.settings.calendarDisplayTimeZoneId) { com.example.todoalarm.data.calendarZone(workspace.settings.calendarDisplayTimeZoneId) }
    val events = remember(events, workspace.settings.hiddenCalendarIds) { events.filter { it.calendarId !in workspace.settings.hiddenCalendarIds } }
    var showCalendars by remember { mutableStateOf(false) }
    if (showCalendars) CalendarManager { showCalendars = false }
    val context = LocalContext.current
    val density = LocalDensity.current
    val anchorDate = remember(displayZone) { LocalDate.now(displayZone) }
    var selectedDateEpochDay by rememberSaveable { mutableStateOf(anchorDate.toEpochDay()) }
    val selectedDate = remember(selectedDateEpochDay) { LocalDate.ofEpochDay(selectedDateEpochDay) }
    var viewMode by rememberSaveable { mutableStateOf(CalendarViewMode.THREE_DAY) }
    val dateWindow = remember(selectedDate.year) { CalendarDateWindow(selectedDate.withDayOfYear(1), CalendarDayRange) }
    val anchorDateIndex = dateWindow.anchorIndex
    val verticalScroll = rememberScrollState()
    var didInitScroll by rememberSaveable { mutableStateOf(false) }
    var horizontalOffsetPx by rememberSaveable { mutableStateOf(Float.NaN) }
    var detailsTarget by remember { mutableStateOf<TodoItem?>(null) }
    var pendingDraft by remember { mutableStateOf<PendingCalendarDraft?>(null) }
    var draggingEvent by remember { mutableStateOf<DraggingCalendarEvent?>(null) }
    var pendingCancelTarget by remember { mutableStateOf<TodoItem?>(null) }
    var showTemplateManager by remember { mutableStateOf(false) }
    var showSaveWeekTemplate by remember { mutableStateOf(false) }
    var templateAnchorWeekStart by remember { mutableStateOf(currentWeekStart(LocalDate.now())) }
    var showViewModeMenu by remember { mutableStateOf(false) }
    var pendingDeleteTarget by remember { mutableStateOf<TodoItem?>(null) }
    var handledTargetEventSerial by rememberSaveable { mutableStateOf(-1) }
    var calendarEventDragActive by remember { mutableStateOf(false) }
    var todayRequestSerial by remember { mutableStateOf(0) }
    val initialMoment = remember(displayZone) { LocalDateTime.now(displayZone) }
    val currentDate by produceState(initialValue = LocalDate.now(displayZone), displayZone) {
        while (true) {
            val now = LocalDateTime.now(displayZone)
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay().plusSeconds(1)
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(60_000L))
            value = LocalDate.now(displayZone)
        }
    }
    val currentDateIndex = dateWindow.indexOf(currentDate) ?: -1

    val eventsByDate = remember(events, displayZone) { buildEventsByDate(events, displayZone) }

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
            val maxHorizontalOffsetPx = ((dateWindow.size * dayColumnWidthPx) - viewportWidthPx).coerceAtLeast(0f)
            val selectedIndex = (dateWindow.indexOf(selectedDate) ?: anchorDateIndex).coerceIn(0, dateWindow.lastIndex)
            val selectedCenteredHorizontalOffsetPx =
                ((selectedIndex - 1).coerceAtLeast(0) * dayColumnWidthPx).coerceIn(0f, maxHorizontalOffsetPx)

            val horizontalOffsetProvider: () -> Float = {
                when (viewMode) {
                    CalendarViewMode.THREE_DAY -> if (horizontalOffsetPx.isNaN()) {
                        selectedCenteredHorizontalOffsetPx
                    } else {
                        horizontalOffsetPx.coerceIn(0f, maxHorizontalOffsetPx)
                    }
                    CalendarViewMode.DAY -> (selectedIndex * dayColumnWidthPx).coerceIn(0f, maxHorizontalOffsetPx)
                    else -> 0f
                }
            }
            val timelineFocusIndex by remember(
                viewMode,
                selectedIndex,
                viewportWidthPx,
                dayColumnWidthPx,
                dateWindow,
                horizontalOffsetProvider
            ) {
                derivedStateOf {
                    when (viewMode) {
                        CalendarViewMode.THREE_DAY -> (((horizontalOffsetProvider() + viewportWidthPx / 2f) / dayColumnWidthPx).toInt())
                            .coerceIn(0, dateWindow.lastIndex)
                        else -> selectedIndex
                    }
                }
            }
            val timelineFocusDate = dateWindow.dateAt(timelineFocusIndex)
            val visibleRange = when {
                viewMode == CalendarViewMode.THREE_DAY -> {
                    val start = (timelineFocusIndex - 1 - CalendarOverscanDays).coerceAtLeast(0)
                    val end = (timelineFocusIndex + 1 + CalendarOverscanDays).coerceAtMost(dateWindow.lastIndex)
                    start..end
                }
                viewMode == CalendarViewMode.DAY -> {
                    val start = (timelineFocusIndex - CalendarOverscanDays).coerceAtLeast(0)
                    val end = (timelineFocusIndex + CalendarOverscanDays).coerceAtMost(dateWindow.lastIndex)
                    start..end
                }
                else -> selectedIndex..(selectedIndex + 2).coerceAtMost(dateWindow.lastIndex)
            }
            val visibleDays = remember(dateWindow, visibleRange) { visibleRange.map(dateWindow::dateAt) }
            val requestedEventRange = remember(viewMode, selectedDate, visibleDays, weekStartMode) {
                calendarRequestedEventRange(
                    viewMode = viewMode,
                    selectedDate = selectedDate,
                    visibleDays = visibleDays,
                    weekStartMode = weekStartMode
                )
            }

            val visibleTimedEventPlacements = remember(events, visibleDays, displayZone) {
                buildTimedEventPlacementsForDays(events, visibleDays, displayZone)
            }

            val horizontalScrollableState = rememberScrollableState { delta ->
                if (viewMode != CalendarViewMode.THREE_DAY) return@rememberScrollableState 0f
                val previous = if (horizontalOffsetPx.isNaN()) {
                    selectedCenteredHorizontalOffsetPx
                } else {
                    horizontalOffsetPx.coerceIn(0f, maxHorizontalOffsetPx)
                }
                horizontalOffsetPx = (previous - delta).coerceIn(0f, maxHorizontalOffsetPx)
                previous - horizontalOffsetPx
            }

            val hourHeight = 72.dp
            val boardHeight = hourHeight * 24
            val hourHeightPx = with(density) { hourHeight.toPx() }
            val headerMonthDate = when (viewMode) {
                CalendarViewMode.MONTH -> selectedDate.withDayOfMonth(1)
                CalendarViewMode.THREE_DAY -> timelineFocusDate
                else -> selectedDate
            }
            fun selectDate(targetDate: LocalDate) {
                selectedDateEpochDay = targetDate.toEpochDay()
                pendingDraft = null
            }

            fun shiftViewBy(daysDelta: Long) {
                selectDate(selectedDate.plusDays(daysDelta))
            }

            fun revealDate(targetDate: LocalDate) {
                selectedDateEpochDay = targetDate.toEpochDay()
                pendingDraft = null
                horizontalOffsetPx = Float.NaN
            }

            fun changeView(mode: CalendarViewMode) {
                val focusDate = if (viewMode == CalendarViewMode.THREE_DAY) timelineFocusDate else selectedDate
                viewMode = mode
                showViewModeMenu = false
                pendingDraft = null
                if (mode == CalendarViewMode.DAY) {
                    selectedDateEpochDay = focusDate.toEpochDay()
                } else if (mode == CalendarViewMode.THREE_DAY) {
                    selectedDateEpochDay = focusDate.toEpochDay()
                }
            }

            fun openThreeDayFromDate(targetDate: LocalDate) {
                selectedDateEpochDay = targetDate.toEpochDay()
                pendingDraft = null
                viewMode = CalendarViewMode.THREE_DAY
            }

            LaunchedEffect(requestedEventRange.startInclusive, requestedEventRange.endExclusive) {
                onVisibleDateRangeChange(requestedEventRange.startInclusive, requestedEventRange.endExclusive)
            }

            LaunchedEffect(initialFocusDateSerial, initialFocusDate) {
                initialFocusDate?.let(::revealDate)
            }

            fun openDatePicker() {
                DatePickerDialog(
                    context,
                    { _, year, month, day -> revealDate(LocalDate.of(year, month + 1, day)) },
                    selectedDate.year,
                    selectedDate.monthValue - 1,
                    selectedDate.dayOfMonth
                ).show()
            }

            LaunchedEffect(hourHeightPx) {
                if (!didInitScroll) {
                    val targetHour = (initialMoment.hour - 4).coerceAtLeast(0)
                    val topLabelInsetPx = with(density) { 20.dp.toPx() }
                    verticalScroll.scrollTo(
                        ((targetHour * hourHeightPx) - topLabelInsetPx)
                            .roundToInt()
                            .coerceAtLeast(0)
                    )
                    didInitScroll = true
                }
            }

            LaunchedEffect(selectedDate) {
                templateAnchorWeekStart = currentWeekStart(selectedDate)
            }

            LaunchedEffect(viewMode, selectedDate, dayColumnWidthPx, maxHorizontalOffsetPx) {
                if (viewMode == CalendarViewMode.THREE_DAY) {
                    horizontalOffsetPx = selectedCenteredHorizontalOffsetPx
                }
            }

            fun openDetails(item: TodoItem) {
                pendingDraft = null
                detailsTarget = item
            }

            LaunchedEffect(targetEventSerial, targetEventId, events) {
                val id = targetEventId ?: return@LaunchedEffect
                if (handledTargetEventSerial == targetEventSerial) return@LaunchedEffect
                val target = events.firstOrNull { it.id == id && it.isEvent } ?: onGetEventById(id)?.takeIf { it.isEvent } ?: run {
                    handledTargetEventSerial = targetEventSerial
                    Toast.makeText(context, "日程已删除或不可访问", Toast.LENGTH_SHORT).show()
                    return@LaunchedEffect
                }
                if (target.calendarId in workspace.settings.hiddenCalendarIds) workspace.visibility(target.calendarId, true)
                handledTargetEventSerial = targetEventSerial
                val targetDate = target.startAtMillis
                    ?.let { java.time.Instant.ofEpochMilli(it).atZone(if (target.allDay) com.example.todoalarm.data.calendarZone(target.eventTimeZoneId) else displayZone).toLocalDate() }
                    ?: target.dueDate()
                revealDate(targetDate)
                openDetails(target)
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CalendarDefaultsButton()
                    TextButton(onClick = { showCalendars = true }) { Text("日历 · ${workspace.calendars.count { it.id !in workspace.settings.hiddenCalendarIds }}") }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    CalendarBrowserHeader(
                        titleMonth = headerMonthDate,
                        viewMode = viewMode,
                        onPickDate = ::openDatePicker,
                        onToday = {
                            revealDate(LocalDate.now(displayZone))
                            todayRequestSerial += 1
                        },
                        onChangeView = ::changeView,
                        onOpenViewMenu = { showViewModeMenu = true },
                        onDismissViewMenu = { showViewModeMenu = false },
                        showViewModeMenu = showViewModeMenu
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    when (viewMode) {
                    CalendarViewMode.MONTH -> key(Math.floorDiv(selectedDate.year, 50)) {
                        CalendarMonthPager(selectedDate = selectedDate, onSelectDate = ::selectDate) { month ->
                            CalendarMonthGridView(
                                monthDate = month,
                                selectedDate = selectedDate,
                                currentDate = currentDate,
                                weekStartMode = weekStartMode,
                                eventsByDate = eventsByDate,
                                onSelectDate = ::selectDate,
                                onOpenThreeDayAt = ::openThreeDayFromDate,
                                onOpenDetails = ::openDetails
                            )
                        }
                    }

                    CalendarViewMode.THREE_DAY,
                    CalendarViewMode.DAY -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(viewMode, selectedDate) {
                                    if (viewMode == CalendarViewMode.DAY) {
                                        var totalDrag = 0f
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                when {
                                                    totalDrag < -56f -> shiftViewBy(1)
                                                    totalDrag > 56f -> shiftViewBy(-1)
                                                }
                                                totalDrag = 0f
                                            }
                                        ) { change, dragAmount ->
                                            totalDrag += dragAmount
                                            change.consume()
                                        }
                                    }
                                }
                                .then(
                                    if (viewMode == CalendarViewMode.THREE_DAY) {
                                        Modifier.scrollable(horizontalScrollableState, Orientation.Horizontal)
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            val visibleAllDayEvents = remember(eventsByDate, visibleDays) {
                                visibleDays
                                    .flatMap { day -> eventsByDate[day].orEmpty() }
                                    .filter { it.allDay }
                                    .distinctBy { it.id }
                                    .sortedBy { it.startAtMillis ?: it.dueAtMillis }
                            }

                            Column(modifier = Modifier.fillMaxSize()) {
                                CalendarHeaderRow(
                                    timeAxisWidth = timeAxisWidth,
                                    viewportWidth = viewportWidth,
                                    dayColumnWidth = dayColumnWidth,
                                    dayColumnWidthPx = dayColumnWidthPx,
                                    dateWindow = dateWindow,
                                    visibleRange = visibleRange,
                                    currentDate = currentDate,
                                    horizontalOffsetProvider = horizontalOffsetProvider,
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
                                    horizontalOffsetProvider = horizontalOffsetProvider,
                                    events = visibleAllDayEvents,
                                    dateWindow = dateWindow,
                                    dragModifier = Modifier,
                                    onOpenDetails = ::openDetails
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
                                        referenceDate = selectedDate
                                    )
                                    CalendarTimedBoard(
                                        modifier = Modifier
                                            .width(viewportWidth),
                                        dateWindow = dateWindow,
                                        visibleRange = visibleRange,
                                        dayColumnWidth = dayColumnWidth,
                                        dayColumnWidthPx = dayColumnWidthPx,
                                        horizontalOffsetProvider = horizontalOffsetProvider,
                                        boardHeight = boardHeight,
                                        hourHeight = hourHeight,
                                        hourHeightPx = hourHeightPx,
                                        verticalScroll = verticalScroll,
                                        visibleEventPlacements = visibleTimedEventPlacements,
                                        pendingDraft = pendingDraft,
                                        onPendingDraftChange = { pendingDraft = it },
                                        onQuickCreateEvent = onQuickCreateEvent,
                                        onOpenDetails = ::openDetails,
                                        onMoveEvent = { item, startAt, endAt ->
                                            draggingEvent = DraggingCalendarEvent(item, startAt, endAt)
                                        },
                                        onEventDragActiveChange = { active -> calendarEventDragActive = active },
                                        currentDayIndex = currentDateIndex
                                    )
                                }
                            }
                        }
                    }

                    CalendarViewMode.AGENDA -> key(Math.floorDiv(selectedDate.year, 50), weekStartMode) {
                            CalendarAgendaView(
                                selectedDate = selectedDate,
                                currentDate = currentDate,
                                weekStartMode = weekStartMode,
                                eventsByDate = eventsByDate,
                                todayRequestSerial = todayRequestSerial,
                                onSelectDate = ::selectDate,
                                onOpenDetails = ::openDetails
                            )
                    }
                }
            }
        }
    }
    }

    detailsTarget?.let { target ->
        val item = events.firstOrNull { it.id == target.id } ?: target
        CalendarEventDetailsDialog(
            item = item,
            onGetEventById = onGetEventById,
            onGetEventCheckIns = onGetEventCheckIns,
            onDismiss = { detailsTarget = null },
            onCopy = { current ->
                detailsTarget = null
                onCopyEvent(current)
            },
            onEdit = { current ->
                detailsTarget = null
                onEditEvent(current)
            },
            onCancel = { current ->
                detailsTarget = null
                if (current.isRecurring) {
                    onCancelEvent(current)
                } else {
                    pendingCancelTarget = current
                }
            },
            onDelete = { current ->
                detailsTarget = null
                pendingDeleteTarget = current
            }
        )
    }
    draggingEvent?.let { dragging ->
        var moveScope by remember(dragging) { mutableStateOf(RecurrenceScope.CURRENT) }
        AlertDialog(onDismissRequest = { draggingEvent = null }, title = { Text("移动日程") }, text = {
            Column {
                Text("将“${dragging.item.title}”移动到 ${formatLocalDateTime(dragging.startAt)} - ${formatLocalDateTime(dragging.endAt)}？")
                if (dragging.item.isRecurring) PreferenceChoice("作用范围", moveScope.name, RecurrenceScope.entries.map { it.name to it.label }) { moveScope = RecurrenceScope.valueOf(it) }
            }
        }, confirmButton = { TextButton(onClick = {
            onMoveEvent(dragging.item, dragging.startAt, dragging.endAt, moveScope); draggingEvent = null
        }) { Text("确定") } }, dismissButton = { TextButton(onClick = { draggingEvent = null }) { Text("取消") } })
    }

    pendingCancelTarget?.let { item ->
        PaykiDecisionBottomSheet(
            title = "取消日程",
            message = "确定取消“${item.title}”吗？取消后会进入历史记录；这不是删除。",
            confirmLabel = "确认取消",
            confirmLabelColor = Color(0xFFD97706),
            onDismiss = { pendingCancelTarget = null },
            onConfirm = {
                pendingCancelTarget = null
                onCancelEvent(item)
            }
        )
    }

    pendingDeleteTarget?.let { item ->
        PaykiDecisionBottomSheet(
            title = "删除日程",
            message = "确定删除“${item.title}”吗？删除后无法恢复。",
            confirmLabel = "删除",
            confirmLabelColor = Color(0xFFD14343),
            onDismiss = { pendingDeleteTarget = null },
            onConfirm = {
                pendingDeleteTarget = null
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
    onDismissViewMenu: () -> Unit,
    showViewModeMenu: Boolean
) {
    val viewIcon = when (viewMode) {
        CalendarViewMode.THREE_DAY -> Icons.Rounded.ViewWeek
        CalendarViewMode.DAY -> Icons.Rounded.ViewDay
        CalendarViewMode.MONTH -> Icons.Rounded.CalendarMonth
        CalendarViewMode.AGENDA -> Icons.Rounded.ViewAgenda
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .clickable(onClick = onPickDate),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = titleMonth.format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)),
                    modifier = Modifier.widthIn(max = 142.dp),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "选择日期",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarHeaderActionButton(label = "今天", onClick = onToday)
                Box {
                    CalendarMenuActionButton(
                        icon = viewIcon,
                        contentDescription = viewMode.label,
                        onClick = onOpenViewMenu
                    )
                    DropdownMenu(
                        expanded = showViewModeMenu,
                        onDismissRequest = onDismissViewMenu
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
@OptIn(ExperimentalFoundationApi::class)
private fun CalendarMonthPager(
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    content: @Composable (LocalDate) -> Unit
) {
    val originEpoch = rememberSaveable { selectedDate.withDayOfMonth(1).toEpochDay() }
    val origin = LocalDate.ofEpochDay(originEpoch)
    val center = CalendarPagerCenter
    val pager = rememberPagerState(initialPage = center) { CalendarPagerPages }
    val targetPage = center + ChronoUnit.MONTHS.between(origin, selectedDate.withDayOfMonth(1)).toInt()
    val latestSelection by rememberUpdatedState(onSelectDate)
    val latestDate by rememberUpdatedState(selectedDate)
    LaunchedEffect(targetPage) {
        if (pager.settledPage != targetPage) pager.animateScrollToPage(targetPage)
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.drop(1).collect { page ->
            val month = origin.plusMonths((page - center).toLong())
            if (month != latestDate.withDayOfMonth(1)) latestSelection(month)
        }
    }
    VerticalPager(state = pager, modifier = Modifier.fillMaxSize().testTag("calendar-months"), beyondBoundsPageCount = 1) { page ->
        content(origin.plusMonths((page - center).toLong()))
    }
}

@Composable
private fun CalendarMonthGridView(
    monthDate: LocalDate,
    selectedDate: LocalDate,
    currentDate: LocalDate,
    weekStartMode: WeekStartMode,
    eventsByDate: Map<LocalDate, List<TodoItem>>,
    onSelectDate: (LocalDate) -> Unit,
    onOpenThreeDayAt: (LocalDate) -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weekdayLabels.forEach { label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        rows.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                week.forEach { date ->
                    val isCurrentMonth = date.month == monthDate.month
                    val isSelected = date == selectedDate
                    val isToday = date == currentDate
                    val lunarLabel = remember(date) { LunarCalendar.labelFor(date).displayText }
                    val dayEvents = eventsByDate[date].orEmpty().take(4)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clipToBounds()
                            .testTag("month-day-$date")
                            .pointerInput(date) {
                                detectTapGestures(
                                    onTap = { onSelectDate(date) },
                                    onDoubleTap = {
                                        onSelectDate(date)
                                        onOpenThreeDayAt(date)
                                    }
                                )
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            isToday -> Color(0x143E7BFA)
                            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
                                isToday -> Color(0x474C8BF5)
                                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = when {
                                        isSelected -> Color(0xFF2F6CFF)
                                        isToday -> Color(0x334C8BF5)
                                        else -> Color.Transparent
                                    }
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = when {
                                            isSelected -> Color.White
                                            isToday -> Color(0xFF73A8FF)
                                            isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = lunarLabel,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                                    },
                                    fontSize = 9.sp,
                                    lineHeight = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                dayEvents.forEach { item ->
                                    MiniMonthEventChip(
                                        item = item,
                                        onClick = { onOpenDetails(item) }
                                    )
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
@OptIn(ExperimentalFoundationApi::class)
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
            fontSize = 9.sp,
            lineHeight = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CalendarAgendaView(
    selectedDate: LocalDate,
    currentDate: LocalDate,
    weekStartMode: WeekStartMode,
    eventsByDate: Map<LocalDate, List<TodoItem>>,
    todayRequestSerial: Int,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDetails: (TodoItem) -> Unit
) {
    val weekStart = startOfWeek(selectedDate, weekStartMode)
    val agendaDates = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }
    val listState = rememberLazyListState()
    var highlightToday by remember { mutableStateOf(false) }
    val originEpoch = rememberSaveable(weekStartMode) { weekStart.toEpochDay() }
    val origin = LocalDate.ofEpochDay(originEpoch)
    val center = CalendarPagerCenter
    val pager = rememberPagerState(initialPage = center) { CalendarPagerPages }
    val targetPage = center + ChronoUnit.WEEKS.between(origin, weekStart).toInt()
    val latestDate by rememberUpdatedState(selectedDate)
    val latestSelection by rememberUpdatedState(onSelectDate)

    LaunchedEffect(targetPage) {
        if (pager.settledPage != targetPage) pager.animateScrollToPage(targetPage)
    }
    LaunchedEffect(pager, originEpoch) {
        snapshotFlow { pager.settledPage }.drop(1).collect { page ->
            val newWeek = origin.plusWeeks((page - center).toLong())
            val selectedWeek = startOfWeek(latestDate, weekStartMode)
            if (newWeek != selectedWeek) {
                latestSelection(newWeek.plusDays(ChronoUnit.DAYS.between(selectedWeek, latestDate)))
            }
        }
    }
    LaunchedEffect(selectedDate, todayRequestSerial) {
        highlightToday = false
        val index = ChronoUnit.DAYS.between(weekStart, selectedDate).toInt().coerceIn(0, 6)
        listState.animateScrollToItem(index)
        if (todayRequestSerial > 0 && selectedDate == LocalDate.now()) {
            highlightToday = true
            delay(1_000L)
            highlightToday = false
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth().testTag("calendar-weeks"),
            beyondBoundsPageCount = 1
        ) { page ->
            val weekDays = (0..6).map { origin.plusWeeks((page - center).toLong()).plusDays(it.toLong()) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weekDays.forEach { date ->
                    val isSelected = date == selectedDate
                    val isToday = date == currentDate
                    val lunarLabel = remember(date) { LunarCalendar.labelFor(date).displayText }
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onSelectDate(date) },
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            isToday -> Color(0x143E7BFA)
                            else -> Color.Transparent
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                date.dayOfWeek.shortLabel(),
                                fontSize = 12.sp,
                                color = if (isSelected || isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                date.dayOfMonth.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected || isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                lunarLabel, fontSize = 9.sp, lineHeight = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = weekRangeLabel(selectedDate, weekStartMode),
            modifier = Modifier.padding(horizontal = 10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("calendar-agenda"),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(agendaDates, key = { it.toEpochDay() }) { date ->
                val dayEvents = eventsByDate[date].orEmpty()
                val highlight by animateColorAsState(
                    if (date == selectedDate && highlightToday) Color.Gray.copy(alpha = 0.2f) else Color.Transparent,
                    label = "todayAgendaHighlight"
                )
                Row(
                    modifier = Modifier.fillMaxWidth().testTag("agenda-day-$date")
                        .background(highlight, RoundedCornerShape(8.dp))
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.width(42.dp).padding(top = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(date.dayOfWeek.shortLabel(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(date.dayOfMonth.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (dayEvents.isEmpty()) {
                            Text(
                                text = "本日暂无日程",
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            dayEvents.forEach { item -> AgendaEventCard(item = item, onClick = { onOpenDetails(item) }) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AgendaEventCard(
    item: TodoItem,
    onClick: () -> Unit
) {
    val tint = item.accentColorHex?.let(::colorFromHex) ?: MaterialTheme.colorScheme.primary
    val background = tint.copy(alpha = 0.14f).compositeOver(MaterialTheme.colorScheme.surface)
    val textColor = remember(tint, background) {
        val endpoint = if (ColorUtils.calculateContrast(Color.White.toArgb(), background.toArgb()) >
            ColorUtils.calculateContrast(Color.Black.toArgb(), background.toArgb())) Color.White else Color.Black
        (0..20).asSequence().map { step ->
            ColorUtils.blendARGB(tint.toArgb(), endpoint.toArgb(), step / 20f)
        }.first { candidate -> ColorUtils.calculateContrast(candidate, background.toArgb()) >= 4.5 }
            .let(::Color)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth().testTag("agenda-event-${item.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight().testTag("agenda-accent-${item.id}"),
                shape = RoundedCornerShape(999.dp),
                color = tint.copy(alpha = 0.95f)
            ) {}
            Column(modifier = Modifier.weight(1f).testTag("agenda-content-${item.id}"), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    item.title,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (item.allDay) "全天" else timeRangeLabel(item, com.example.todoalarm.data.calendarZone(LocalWorkspacePreferences.current.settings.calendarDisplayTimeZoneId)),
                    color = textColor,
                    fontSize = 13.sp,
                    lineHeight = 15.sp
                )
                item.location.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = textColor, fontSize = 13.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    var pendingDeleteTemplate by remember { mutableStateOf<ScheduleTemplate?>(null) }

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
                                    OutlinedButton(onClick = { pendingDeleteTemplate = template }) {
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

    pendingDeleteTemplate?.let { template ->
        PaykiDecisionBottomSheet(
            title = "删除模板",
            message = "确定删除“${template.name}”吗？删除后无法恢复。",
            confirmLabel = "删除",
            confirmLabelColor = Color(0xFFD14343),
            onDismiss = { pendingDeleteTemplate = null },
            onConfirm = {
                scope.launch {
                    pendingDeleteTemplate = null
                    val message = onDeleteTemplate(template.id)
                    if (message == null) {
                        Toast.makeText(context, "模板已删除", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (label == null) 8.dp else 8.dp,
                vertical = 5.dp
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
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CalendarMenuActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CalendarHeaderRow(
    timeAxisWidth: Dp,
    viewportWidth: Dp,
    dayColumnWidth: Dp,
    dayColumnWidthPx: Float,
    dateWindow: CalendarDateWindow,
    visibleRange: IntRange,
    currentDate: LocalDate,
    horizontalOffsetProvider: () -> Float,
    todayHighlightColor: Color,
    todayHighlightTextColor: Color,
    dragModifier: Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(timeAxisWidth), contentAlignment = Alignment.Center) {
            Text(
                text = timezoneShortLabel(com.example.todoalarm.data.calendarZone(LocalWorkspacePreferences.current.settings.calendarDisplayTimeZoneId)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier
                .width(viewportWidth)
                .clipToBounds()
                .then(dragModifier)
        ) {
            visibleRange.forEach { dayIndex ->
                val day = dateWindow.dateAt(dayIndex)
                val isToday = day == currentDate
                val lunarLabel = remember(day) { LunarCalendar.labelFor(day).displayText }
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(dayLeftPx(dayIndex, dayColumnWidthPx, horizontalOffsetProvider()), 0)
                        }
                        .width(dayColumnWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = if (isToday) todayHighlightColor else Color.Transparent,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
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
                            Text(
                                text = lunarLabel,
                                color = if (isToday) todayHighlightTextColor.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CalendarAllDaySection(
    timeAxisWidth: Dp,
    viewportWidth: Dp,
    dayColumnWidth: Dp,
    dayColumnWidthPx: Float,
    visibleRange: IntRange,
    horizontalOffsetProvider: () -> Float,
    events: List<TodoItem>,
    dateWindow: CalendarDateWindow,
    dragModifier: Modifier,
    onOpenDetails: (TodoItem) -> Unit
) {
    val rowCount = events.size.coerceIn(0, 6)
    val density = LocalDensity.current
    val rowHeightPx = with(density) { 28.dp.roundToPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (rowCount == 0) 0.dp else 36.dp, max = (6 * 30).dp + 14.dp)
            .padding(vertical = 4.dp)
    ) {
        if (rowCount > 0) {
            Box(modifier = Modifier.width(timeAxisWidth), contentAlignment = Alignment.TopStart) {
                Text(text = "全天", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Box(
            modifier = Modifier
                .width(viewportWidth)
                .clipToBounds()
                .then(dragModifier)
        ) {
            events.forEachIndexed { rowIndex, item ->
                if (rowIndex >= 6) return@forEachIndexed
                val eventZone = com.example.todoalarm.data.calendarZone(item.eventTimeZoneId)
                val startDate = item.startAtMillis?.let { java.time.Instant.ofEpochMilli(it).atZone(eventZone).toLocalDate() } ?: return@forEachIndexed
                val endDateInclusive = item.endAtMillis?.let { java.time.Instant.ofEpochMilli(it).atZone(eventZone).toLocalDate().minusDays(1) } ?: startDate
                val startIndex = (dateWindow.indexOf(startDate) ?: visibleRange.first).coerceAtLeast(visibleRange.first)
                val endIndex = (dateWindow.indexOf(endDateInclusive) ?: visibleRange.last).coerceAtMost(visibleRange.last)
                if (endIndex < visibleRange.first || startIndex > visibleRange.last || endIndex < startIndex) return@forEachIndexed

                val tint = item.accentColorHex?.let(::colorFromHex) ?: MaterialTheme.colorScheme.primary
                key(item.id, startDate, endDateInclusive) {
                    Surface(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = dayLeftPx(startIndex, dayColumnWidthPx, horizontalOffsetProvider()) + with(density) { 4.dp.roundToPx() },
                                    y = rowIndex * rowHeightPx
                                )
                            }
                            .width((dayColumnWidth * (endIndex - startIndex + 1)) - 8.dp)
                            .height(24.dp)
                            .clickable(onClick = { onOpenDetails(item) }),
                        shape = RoundedCornerShape(10.dp),
                        color = tint.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, tint.copy(alpha = 0.24f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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
                                fontSize = 13.sp,
                                lineHeight = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            if (events.size > 6) {
                Text(
                    text = "+${events.size - 6} more",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.offset(x = 8.dp, y = (6 * 28).dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarTimeAxis(
    width: Dp,
    hourHeight: Dp,
    verticalScroll: androidx.compose.foundation.ScrollState,
    referenceDate: LocalDate
) {
    val zone = com.example.todoalarm.data.calendarZone(LocalWorkspacePreferences.current.settings.calendarDisplayTimeZoneId)
    val auxiliaryZoneId = LocalWorkspacePreferences.current.settings.calendarAuxiliaryTimeZoneId
    val auxiliaryZone = auxiliaryZoneId.takeIf { it.isNotBlank() }?.let { com.example.todoalarm.data.calendarZone(it) }
    val currentMoment by produceState(initialValue = LocalDateTime.now(zone), zone) {
        while (true) {
            delay(60_000L)
            value = LocalDateTime.now(zone)
        }
    }
    val currentMinutes = currentMoment.hour * 60 + currentMoment.minute
    val currentTimeOffset = hourHeight * (currentMinutes / 60f)

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
                        .width(width)
                        .height(if (auxiliaryZone == null) 18.dp else 30.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                      Text(
                        text = "%02d:00".format(hour),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Medium
                      )
                      if (auxiliaryZone != null) Text(referenceDate.atTime(hour, 0).atZone(zone).withZoneSameInstant(auxiliaryZone).format(DateTimeFormatter.ofPattern("HH:mm")),
                          fontSize = 9.sp, lineHeight = 10.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .offset(x = 0.dp, y = currentTimeOffset - 12.dp),
                color = Color(0xFFE53935),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = formatClockTime(currentMoment),
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CalendarTimedBoard(
    modifier: Modifier,
    dateWindow: CalendarDateWindow,
    visibleRange: IntRange,
    dayColumnWidth: Dp,
    dayColumnWidthPx: Float,
    horizontalOffsetProvider: () -> Float,
    boardHeight: Dp,
    hourHeight: Dp,
    hourHeightPx: Float,
    verticalScroll: androidx.compose.foundation.ScrollState,
    visibleEventPlacements: List<Pair<LocalDate, TimedEventPlacement>>,
    pendingDraft: PendingCalendarDraft?,
    onPendingDraftChange: (PendingCalendarDraft?) -> Unit,
    onQuickCreateEvent: (LocalDateTime, LocalDateTime) -> Unit,
    onOpenDetails: (TodoItem) -> Unit,
    onMoveEvent: (TodoItem, LocalDateTime, LocalDateTime) -> Unit,
    onEventDragActiveChange: (Boolean) -> Unit,
    currentDayIndex: Int
) {
    val workspace = LocalWorkspacePreferences.current
    val density = LocalDensity.current
    val outline = MaterialTheme.colorScheme.outline
    val latestPendingDraft by rememberUpdatedState(pendingDraft)

    BoxWithConstraints(
        modifier = modifier.clipToBounds()
    ) {
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val minEventHeightPx = with(density) { 24.dp.toPx() }
        val verticalBucketSizePx = (hourHeightPx / 2f).coerceAtLeast(1f)
        val verticalScrollBucket by remember(verticalScroll, verticalBucketSizePx) {
            derivedStateOf { (verticalScroll.value / verticalBucketSizePx).toInt() }
        }
        val bucketedScrollPx = verticalScrollBucket * verticalBucketSizePx
        val verticalStartPx = (bucketedScrollPx - hourHeightPx * CalendarVerticalOverscanHours).coerceAtLeast(0f)
        val verticalEndPx = bucketedScrollPx + viewportHeightPx + hourHeightPx * CalendarVerticalOverscanHours
        val viewportEventPlacements = remember(
            visibleEventPlacements,
            verticalStartPx,
            verticalEndPx,
            hourHeightPx,
            minEventHeightPx
        ) {
            visibleEventPlacements.filter { (_, placement) ->
                placement.intersectsVerticalRange(
                    startPx = verticalStartPx,
                    endPx = verticalEndPx,
                    hourHeightPx = hourHeightPx,
                    minHeightPx = minEventHeightPx
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScroll)
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(boardHeight)
                .pointerInput(dateWindow, dayColumnWidthPx, hourHeightPx) {
                    detectTapGestures(
                        onLongPress = {
                            onPendingDraftChange(null)
                        },
                        onTap = { tapOffset ->
                            val dayIndex = ((tapOffset.x + horizontalOffsetProvider()) / dayColumnWidthPx)
                                .toInt()
                                .coerceIn(0, dateWindow.lastIndex)
                            val rawMinutes = ((tapOffset.y / hourHeightPx) * 60f).roundToInt().coerceIn(0, 23 * 60 + 59)
                            val snappedMinutes = snapToQuarterHour(rawMinutes).coerceIn(0, 23 * 60 + 45)
                            val startAt = LocalDateTime.of(
                                dateWindow.dateAt(dayIndex),
                                LocalTime.of(snappedMinutes / 60, snappedMinutes % 60)
                            )
                            val endAt = startAt.plusMinutes(workspace.settings.calendarDefaultDurationMinutes.toLong())
                            val nextDraft = PendingCalendarDraft(startAt, endAt)
                            if (latestPendingDraft == nextDraft) {
                                onQuickCreateEvent(startAt, endAt)
                                onPendingDraftChange(null)
                            } else {
                                onPendingDraftChange(nextDraft)
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val horizontalOffsetPx = horizontalOffsetProvider()
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

            viewportEventPlacements.forEach { (day, placement) ->
                val dayIndex = dateWindow.indexOf(day) ?: return@forEach
                key(placement.segment.item.id, day, placement.segment.startMillis, placement.segment.endMillis) {
                    TimedEventCard(
                        segment = placement.segment,
                        placement = placement,
                        dayIndex = dayIndex,
                        dayColumnWidth = dayColumnWidth,
                        dayColumnWidthPx = dayColumnWidthPx,
                        horizontalOffsetProvider = horizontalOffsetProvider,
                        hourHeight = hourHeight,
                        onClick = { onOpenDetails(placement.segment.item) },
                        onMove = onMoveEvent,
                        onDragActiveChange = onEventDragActiveChange
                    )
                }
            }

            pendingDraft?.let { draft ->
                PendingDraftCard(
                    dayIndex = dateWindow.indexOf(draft.startAt.toLocalDate()) ?: return@let,
                    dayColumnWidth = dayColumnWidth,
                    dayColumnWidthPx = dayColumnWidthPx,
                    horizontalOffsetProvider = horizontalOffsetProvider,
                    hourHeight = hourHeight,
                    draft = draft,
                    onClick = {
                        onQuickCreateEvent(draft.startAt, draft.endAt)
                        onPendingDraftChange(null)
                    }
                )
            }

            CurrentTimeLine(
                currentDayIndex = currentDayIndex,
                visibleRange = visibleRange,
                dayColumnWidthPx = dayColumnWidthPx,
                horizontalOffsetProvider = horizontalOffsetProvider,
                boardHeightPx = with(density) { boardHeight.toPx() },
                hourHeightPx = hourHeightPx
            )
        }
        }
    }
}

@Composable
private fun PendingDraftCard(
    dayIndex: Int,
    dayColumnWidth: Dp,
    dayColumnWidthPx: Float,
    horizontalOffsetProvider: () -> Float,
    hourHeight: Dp,
    draft: PendingCalendarDraft,
    onClick: () -> Unit
) {
    val startMinutes = draft.startAt.hour * 60 + draft.startAt.minute
    val durationMinutes = 30L
    val topOffset = hourHeight * (startMinutes / 60f)
    val accent = Color(0xFF4C8BF5)
    val density = LocalDensity.current
    val horizontalInsetPx = with(density) { 4.dp.roundToPx() }
    val topOffsetPx = with(density) { topOffset.roundToPx() }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = dayLeftPx(dayIndex, dayColumnWidthPx, horizontalOffsetProvider()) + horizontalInsetPx,
                    y = topOffsetPx
                )
            }
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
@OptIn(ExperimentalFoundationApi::class)
private fun TimedEventCard(
    segment: TimedEventSegment,
    placement: TimedEventPlacement,
    dayIndex: Int,
    dayColumnWidth: Dp,
    dayColumnWidthPx: Float,
    horizontalOffsetProvider: () -> Float,
    hourHeight: Dp,
    onClick: () -> Unit,
    onMove: (TodoItem, LocalDateTime, LocalDateTime) -> Unit,
    onDragActiveChange: (Boolean) -> Unit
) {
    val item = segment.item
    val startDateTime = segment.startDateTime
    val endDateTime = segment.endDateTime
    val startMinutes = segment.startMinutes
    val durationMinutes = segment.durationMinutes
    val tint = item.accentColorHex?.let(::colorFromHex) ?: MaterialTheme.colorScheme.primary
    val alpha = calendarVisualAlpha(item)
    val topOffset = hourHeight * (startMinutes / 60f)
    val eventWidth = (dayColumnWidth / placement.columnCount) - 8.dp
    val eventHeight = (hourHeight * (durationMinutes / 60f)).coerceAtLeast(24.dp)
    val duration = Duration.between(startDateTime, endDateTime)
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val columnOffsetPx = with(density) {
        (((dayColumnWidth / placement.columnCount) * placement.columnIndex) + 4.dp).roundToPx()
    }
    val topOffsetPx = with(density) { topOffset.roundToPx() }
    var dragTranslationX by remember(item.id) { mutableFloatStateOf(0f) }
    var dragTranslationY by remember(item.id) { mutableFloatStateOf(0f) }
    val showLocation = durationMinutes >= 60 || eventHeight >= 64.dp || eventWidth >= 118.dp
    val titleMaxLines = when {
        eventHeight >= 144.dp -> 6
        eventHeight >= 108.dp -> 5
        showLocation -> 4
        else -> 6
    }
    val locationMaxLines = when {
        eventHeight >= 132.dp -> 3
        eventHeight >= 84.dp -> 2
        else -> 1
    }

    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = dayLeftPx(dayIndex, dayColumnWidthPx, horizontalOffsetProvider()) + columnOffsetPx,
                    y = topOffsetPx
                )
            }
            .width(eventWidth)
            .height(eventHeight)
            .graphicsLayer {
                translationX = dragTranslationX
                translationY = dragTranslationY
            }
            .pointerInput(item.id, dayColumnWidthPx, hourHeight) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        onDragActiveChange(true)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        val movedDaySteps = (dragTranslationX / dayColumnWidthPx).roundToInt()
                        val movedMinutes = snapToQuarterHour(
                            ((dragTranslationY / with(density) { hourHeight.toPx() }) * 60f).roundToInt()
                        )
                        val newStart = startDateTime
                            .plusDays(movedDaySteps.toLong())
                            .plusMinutes(movedMinutes.toLong())
                        val newEnd = newStart.plus(duration)
                        dragTranslationX = 0f
                        dragTranslationY = 0f
                        if (newStart != startDateTime || newEnd != endDateTime) {
                            onMove(item, newStart, newEnd)
                        }
                        onDragActiveChange(false)
                    },
                    onDragCancel = {
                        dragTranslationX = 0f
                        dragTranslationY = 0f
                        onDragActiveChange(false)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    dragTranslationX += dragAmount.x
                    dragTranslationY += dragAmount.y
                }
            }
            .clickable(onClick = onClick)
            .alpha(alpha),
        shape = RoundedCornerShape(14.dp),
        color = tint.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp),
                shape = RoundedCornerShape(999.dp),
                color = tint.copy(alpha = 0.95f)
            ) {}
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(if (showLocation) 1.dp else 0.dp)
            ) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    color = tint.copy(alpha = 0.98f),
                    fontSize = 13.sp,
                    lineHeight = 13.5.sp,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
                if (showLocation && item.location.isNotBlank()) {
                    Text(
                        text = item.location,
                        color = tint.copy(alpha = 0.78f),
                        fontSize = 11.sp,
                        lineHeight = 11.5.sp,
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
    visibleRange: IntRange,
    dayColumnWidthPx: Float,
    horizontalOffsetProvider: () -> Float,
    boardHeightPx: Float,
    hourHeightPx: Float
) {
    val zone = com.example.todoalarm.data.calendarZone(LocalWorkspacePreferences.current.settings.calendarDisplayTimeZoneId)
    val currentMoment by produceState(initialValue = LocalDateTime.now(zone), zone) {
        while (true) {
            delay(60_000L)
            value = LocalDateTime.now(zone)
        }
    }
    val minutes = currentMoment.hour * 60 + currentMoment.minute
    val y = (minutes / 60f) * hourHeightPx
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (currentDayIndex < 0 || y !in 0f..boardHeightPx) return@Canvas
            val splitX = currentDayIndex * dayColumnWidthPx - horizontalOffsetProvider()

            val strokeWidth = 3.dp.toPx()
            val markerRadius = 6.dp.toPx()
            val isCurrentDayVisible = splitX in 0f..size.width

            if (!isCurrentDayVisible) {
                val lineColor = if (visibleRange.last < currentDayIndex) {
                    Color(0xFFF3B2B2)
                } else {
                    Color(0xFFE53935)
                }
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                return@Canvas
            }

            val lightEnd = splitX.coerceIn(0f, size.width)
            val darkStart = splitX.coerceIn(0f, size.width)

            if (lightEnd > 0f) {
                drawLine(
                    color = Color(0xFFF3B2B2),
                    start = Offset(0f, y),
                    end = Offset(lightEnd, y),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            if (splitX < size.width) {
                drawLine(
                    color = Color(0xFFE53935),
                    start = Offset(darkStart, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            drawCircle(
                color = Color(0xFFD32F2F),
                radius = markerRadius,
                center = Offset(splitX, y)
            )
        }
    }
}

@Composable
private fun CalendarEventDetailsDialog(
    item: TodoItem,
    onGetEventById: suspend (Long) -> TodoItem?,
    onGetEventCheckIns: suspend (Long) -> List<EventCheckIn>,
    onDismiss: () -> Unit,
    onCopy: (TodoItem) -> Unit,
    onEdit: (TodoItem) -> Unit,
    onCancel: (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as TodoApplication
    val accountSettings by app.settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val tint = item.accentColorHex?.let(::colorFromHex) ?: MaterialTheme.colorScheme.primary
    var displayItem by remember(item.id) { mutableStateOf(item) }
    val calendar = LocalWorkspacePreferences.current.calendars.firstOrNull { it.id == displayItem.calendarId }
    val editable = calendar?.id?.startsWith("shared:") != true || calendar.sharedRole.uppercase() in setOf("OWNER", "EDITOR")
    var loadedFor by remember(item.id) { mutableStateOf<List<Any?>?>(null) }
    var checkIns by remember(item.id) { mutableStateOf<List<EventCheckIn>>(emptyList()) }
    var checkInLoading by remember(item.id) { mutableStateOf(item.checkInEnabled) }
    var actionRunning by remember(item.id) { mutableStateOf(false) }
    var refreshSerial by remember(item.id) { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val activeCheckIn = checkIns.firstOrNull { it.checkOutAtMillis == null }
    val nowMillis by produceState(initialValue = System.currentTimeMillis(), activeCheckIn?.id) {
        while (activeCheckIn != null) {
            delay(60_000L)
            value = System.currentTimeMillis()
        }
    }
    val activeMinutes = activeCheckIn?.let { checkIn ->
        ((nowMillis - checkIn.checkInAtMillis).coerceAtLeast(0L) / 60_000L).toInt()
    } ?: 0
    val closedMinutes = maxOf(
        displayItem.totalCheckInMinutes,
        checkIns.filter { it.checkOutAtMillis != null }.sumOf { it.durationMinutes }
    )
    val totalInvestedMinutes = closedMinutes + activeMinutes

    var calendarResponseMessage by remember(item.id) { mutableStateOf<String?>(null) }
    val refreshKey = listOf(item, refreshSerial, calendar?.sharedRole, calendar?.sharedRevision, calendar?.membersJson,
        calendar?.shareMode, calendar?.sharedServerUrl, accountSettings.remoteSyncUsername,
        accountSettings.remoteSyncUserId, accountSettings.remoteSyncServerUrl)
    LaunchedEffect(refreshKey) {
        loadedFor = null
        val refreshed = onGetEventById(item.id)
        if (refreshed == null || !app.canReadCalendarItem(refreshed)) {
            Toast.makeText(context, "日程已删除或不可访问", Toast.LENGTH_SHORT).show()
            onDismiss()
            return@LaunchedEffect
        }
        displayItem = refreshed
        checkInLoading = displayItem.checkInEnabled
        checkIns = if (displayItem.checkInEnabled) onGetEventCheckIns(item.id) else emptyList()
        checkInLoading = false
        loadedFor = refreshKey
    }

    DisposableEffect(lifecycleOwner, item.id, item.checkInEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshSerial += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun useCurrentRecord(operation: (TodoItem) -> Unit) {
        if (actionRunning) return
        scope.launch {
            actionRunning = true
            try {
                val latest = onGetEventById(item.id)
                if (latest == null || !app.canReadCalendarItem(latest)) {
                    Toast.makeText(context, "日程已删除或不可访问", Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else operation(latest)
            } finally {
                actionRunning = false
            }
        }
    }

    if (loadedFor != refreshKey) return

    PaykiBottomSheet(
        onDismiss = onDismiss,
        showDragHandle = false,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(enabled = !actionRunning, onClick = { useCurrentRecord(onCopy) }) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "创建副本", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        bottomBar = {
            CalendarEventDetailsFixedActions(
                onCancel = { useCurrentRecord(onCancel) },
                onEdit = { useCurrentRecord(onEdit) },
                onDelete = { useCurrentRecord(onDelete) },
                enabled = editable && !actionRunning
            )
        },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (!editable) Text(if (calendar?.sharedRole == "REVOKED") "此日历的访问已撤销。" else "你拥有此日历的只读权限。", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Surface(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(20.dp),
                    shape = RoundedCornerShape(7.dp),
                    color = tint
                ) {}
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        text = displayItem.title,
                        color = tint,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (displayItem.allDay) calendarEventDateRangeLabel(displayItem) else formatDateTimeRangeWithoutTimezone(displayItem),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    recurrencePreviewLine(displayItem)?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))

            EventInfoRow(icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                text = LocalWorkspacePreferences.current.calendars.firstOrNull { it.id == displayItem.calendarId }?.name ?: "日历")
            EventInfoRow(icon = { Icon(Icons.Rounded.Place, contentDescription = null) },
                text = "${com.example.todoalarm.data.calendarZone(displayItem.eventTimeZoneId).id} · ${if (displayItem.eventBusyStatus == "FREE") "空闲" else "忙碌"} · ${when (displayItem.eventVisibility) { "PRIVATE" -> "私密"; "PUBLIC" -> "公开"; else -> "沿用日历设置" }}")
            calendar?.takeIf { it.id.startsWith("shared:") }?.let { source ->
                CalendarParticipantsDetails(source, displayItem.eventParticipantsJson,
                    restricted = source.sharedRole == "VIEWER" && displayItem.eventVisibility == "PRIVATE")
            }
            CalendarRsvpRow(displayItem) { message -> calendarResponseMessage = message; refreshSerial += 1 }
            calendarResponseMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            displayItem.location.takeIf { it.isNotBlank() }?.let {
                EventInfoRow(
                    icon = { Icon(Icons.Rounded.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    text = it
                )
            }

            eventReminderSummary(displayItem)?.let {
                EventInfoRow(
                    icon = { Icon(Icons.Rounded.NotificationsNone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    text = it
                )
            }

            if (displayItem.checkInEnabled) {
                val checkInContext = LocalContext.current
                EventCheckInSection(
                    tint = tint,
                    checkIns = checkIns,
                    activeCheckIn = activeCheckIn,
                    totalInvestedMinutes = totalInvestedMinutes,
                    nowMillis = nowMillis,
                    loading = checkInLoading,
                    onLaunchCheckIn = {
                        checkInContext.startActivity(CheckInActivity.createIntent(checkInContext, displayItem.id))
                    }
                )
            }

            displayItem.notes.takeIf { it.isNotBlank() }?.let {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) 0.34f else 0.48f)
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarEventDetailsFixedActions(
    onCancel: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    onClick = onCancel
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFD97706)
                    )
                    Text("取消", color = Color(0xFFD97706), maxLines = 1)
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    onClick = onEdit
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("修改", maxLines = 1)
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    onClick = onDelete
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFD14343)
                    )
                    Text("删除", color = Color(0xFFD14343), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun EventInfoRow(
    icon: @Composable () -> Unit,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.padding(top = 2.dp)) { icon() }
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun EventCheckInSection(
    tint: Color,
    checkIns: List<EventCheckIn>,
    activeCheckIn: EventCheckIn?,
    totalInvestedMinutes: Int,
    nowMillis: Long,
    loading: Boolean,
    onLaunchCheckIn: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = tint.copy(alpha = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) 0.10f else 0.18f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("打卡追踪", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = if (activeCheckIn == null) "未签到" else "签到中 · 已 ${formatCheckInMinutes(activeRunningMinutes(activeCheckIn, nowMillis))}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "总投入 ${formatCheckInMinutes(totalInvestedMinutes)}",
                    color = tint,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (loading) {
                Text("正在读取打卡记录…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (checkIns.isEmpty()) {
                Text("还没有签到记录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    checkIns.forEach { checkIn ->
                        Text(
                            text = formatCheckInRecord(checkIn, nowMillis),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeCheckIn == null) Color(0xFF2196F3) else Color(0xFF4CAF50)
                ),
                onClick = onLaunchCheckIn
            ) {
                Text(if (activeCheckIn == null) "去签到" else "查看签到")
            }
        }
    }
}

private fun recurrencePreviewLine(item: TodoItem): String? {
    if (!item.isRecurring) return null
    val start = item.calendarStartDateTime()
    val endDate = item.recurrenceEndDate
    val suffix = if (endDate == null) "永不截止" else "截止到${endDate.year}年${endDate.monthValue}月${endDate.dayOfMonth}日"
    val interval = item.recurrenceInterval
    return when (item.recurrenceTypeEnum) {
        com.example.todoalarm.data.RecurrenceType.DAILY -> "每隔${interval}天重复，$suffix"
        com.example.todoalarm.data.RecurrenceType.WEEKLY -> {
            val labels = com.example.todoalarm.data.storageStringToWeekdays(item.recurrenceWeekdays)
                .sortedBy { it.value }
                .joinToString("、") { it.shortLabel() }
            "每隔${interval}周的${labels}重复，$suffix"
        }
        com.example.todoalarm.data.RecurrenceType.MONTHLY_NTH_WEEKDAY -> "每隔${interval}月的${if (item.recurrenceMonthlyOrdinal == -1) "最后一个" else "第${item.recurrenceMonthlyOrdinal ?: ((start.dayOfMonth - 1) / 7 + 1)}个"}${start.dayOfWeek.shortLabel()}，$suffix"
        com.example.todoalarm.data.RecurrenceType.MONTHLY_DAY -> "每隔${interval}月${start.dayOfMonth}日重复，$suffix"
        com.example.todoalarm.data.RecurrenceType.YEARLY_DATE -> "每隔${interval}年${start.monthValue}月${start.dayOfMonth}日重复，$suffix"
        com.example.todoalarm.data.RecurrenceType.YEARLY_LUNAR_DATE -> "每隔${interval}年农历${com.example.todoalarm.data.LunarCalendar.labelFor(start.toLocalDate()).displayText}重复，$suffix"
        com.example.todoalarm.data.RecurrenceType.NONE -> null
    }
}

internal fun eventReminderSummary(item: TodoItem): String? {
    if (!item.reminderEnabled) return null
    val allDayRules = if (item.allDay) runCatching {
        com.example.todoalarm.data.decodeAllDayReminderRules(item.allDayReminderRulesJson)
    }.getOrDefault(emptyList()) else emptyList()
    val summaries = buildList {
        if (allDayRules.isNotEmpty()) {
            add("全天提醒：" + allDayRules.joinToString("、") { "${if (it.daysBefore == 0) "当天" else "提前${it.daysBefore}天"} ${"%02d:%02d".format(it.minuteOfDay / 60, it.minuteOfDay % 60)}" })
        }
        item.configuredReminderOffsetsMinutes.takeIf { allDayRules.isEmpty() && it.isNotEmpty() }?.let { offsets ->
            add("普通提醒：${offsets.sortedDescending().joinToString("、") { reminderOffsetLabel(it) }}")
        }
        item.configuredCheckInReminderOffsetsMinutes.takeIf { item.checkInEnabled && it.isNotEmpty() }?.let { offsets ->
            add("签到提醒：${offsets.sortedDescending().joinToString("、") { reminderOffsetLabel(it) }}")
        }
    }
    if (summaries.isNotEmpty()) {
        return summaries.joinToString("\n")
    }
    val reminderAt = item.reminderAtMillis?.let(::reminderAtMillisToDateTime) ?: return null
    return "提醒时间：${formatLocalDateTime(reminderAt)}"
}

private fun reminderOffsetLabel(minutes: Int): String {
    return when {
        minutes == 0 -> "开始时"
        minutes % (24 * 60) == 0 -> "提前${minutes / (24 * 60)}天"
        minutes % 60 == 0 -> "提前${minutes / 60}小时"
        else -> "提前${minutes}分钟"
    }
}

private fun formatCheckInRecord(checkIn: EventCheckIn, nowMillis: Long): String {
    val start = formatClockTime(reminderAtMillisToDateTime(checkIn.checkInAtMillis))
    val endMillis = checkIn.checkOutAtMillis
    return if (endMillis == null) {
        "● $start-进行中 已 ${formatCheckInMinutes(activeRunningMinutes(checkIn, nowMillis))}"
    } else {
        val end = formatClockTime(reminderAtMillisToDateTime(endMillis))
        "● $start-$end ${formatCheckInMinutes(checkIn.durationMinutes)}"
    }
}

private fun activeRunningMinutes(checkIn: EventCheckIn, nowMillis: Long): Int {
    return ((nowMillis - checkIn.checkInAtMillis).coerceAtLeast(0L) / 60_000L).toInt()
}

private fun formatCheckInMinutes(minutes: Int): String {
    val safeMinutes = minutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val restMinutes = safeMinutes % 60
    return when {
        hours > 0 && restMinutes > 0 -> "${hours}h${restMinutes}m"
        hours > 0 -> "${hours}h"
        else -> "${restMinutes}m"
    }
}

private fun formatDateTimeRangeWithoutTimezone(item: TodoItem): String {
    val zone = com.example.todoalarm.data.calendarZone(item.eventTimeZoneId)
    val start = item.startAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() } ?: return "未设置时间"
    val end = item.endAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() } ?: start
    return if (start.toLocalDate() == end.toLocalDate()) {
        "${start.monthValue}月${start.dayOfMonth}日 ${formatClockTime(start)} - ${formatClockTime(end)}"
    } else {
        "${start.monthValue}月${start.dayOfMonth}日 ${formatClockTime(start)} - ${end.monthValue}月${end.dayOfMonth}日 ${formatClockTime(end)}"
    }
}

private data class PendingCalendarDraft(
    val startAt: LocalDateTime,
    val endAt: LocalDateTime
)

private data class DraggingCalendarEvent(
    val item: TodoItem,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime
)

private data class TimedEventSegment(
    val item: TodoItem,
    val startMillis: Long,
    val endMillis: Long,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val startMinutes: Int,
    val durationMinutes: Int
)

private data class TimedEventPlacement(
    val segment: TimedEventSegment,
    val columnIndex: Int,
    val columnCount: Int
)

private fun TimedEventPlacement.intersectsVerticalRange(
    startPx: Float,
    endPx: Float,
    hourHeightPx: Float,
    minHeightPx: Float
): Boolean {
    val eventTopPx = (segment.startMinutes / 60f) * hourHeightPx
    val eventHeightPx = max(hourHeightPx * (segment.durationMinutes / 60f), minHeightPx)
    val eventBottomPx = eventTopPx + eventHeightPx
    return eventBottomPx >= startPx && eventTopPx <= endPx
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

private fun buildTimedEventPlacementsForDays(
    events: List<TodoItem>,
    days: List<LocalDate>,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<Pair<LocalDate, TimedEventPlacement>> {
    if (events.isEmpty() || days.isEmpty()) return emptyList()
    val daySet = days.toSet()
    val windowStart = days.minOrNull() ?: return emptyList()
    val windowEndExclusive = (days.maxOrNull() ?: windowStart).plusDays(1)
    val windowStartMillis = windowStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val windowEndMillis = windowEndExclusive.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val segmentsByDay = linkedMapOf<LocalDate, MutableList<TimedEventSegment>>()

    events.forEach { item ->
        if (item.allDay) return@forEach
        val startMillis = item.startAtMillis ?: item.dueAtMillis
        val endMillis = item.endAtMillis ?: (startMillis + 30 * 60_000L)
        if (endMillis <= windowStartMillis || startMillis >= windowEndMillis) return@forEach

        var cursor = millisToDate(startMillis.coerceAtLeast(windowStartMillis), zoneId)
        val inclusiveEnd = millisToDate((endMillis - 1).coerceAtLeast(startMillis), zoneId)
        while (!cursor.isAfter(inclusiveEnd) && cursor.isBefore(windowEndExclusive)) {
            if (cursor in daySet) {
                item.toSegmentForDay(cursor, zoneId)?.let { segment ->
                    segmentsByDay.getOrPut(cursor) { mutableListOf() } += segment
                }
            }
            cursor = cursor.plusDays(1)
        }
    }

    return days.flatMap { day ->
        val segments = segmentsByDay[day].orEmpty()
        layoutTimedEventSegments(segments.sortedBy { it.startMillis }).map { placement -> day to placement }
    }
}

private fun TodoItem.toSegmentForDay(day: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): TimedEventSegment? {
    val itemStart = startAtMillis ?: dueAtMillis
    val itemEnd = endAtMillis ?: (itemStart + 30 * 60_000L)
    val dayStart = day.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val dayEnd = day.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    if (itemEnd <= dayStart || itemStart >= dayEnd) return null
    val segmentStart = max(itemStart, dayStart)
    val segmentEnd = max(segmentStart + 60_000L, minOf(itemEnd, dayEnd))
    val segmentStartDateTime = Instant.ofEpochMilli(segmentStart).atZone(zoneId).toLocalDateTime()
    val segmentEndDateTime = Instant.ofEpochMilli(segmentEnd).atZone(zoneId).toLocalDateTime()
    val startMinutes = segmentStartDateTime.hour * 60 + segmentStartDateTime.minute
    val durationMinutes = Duration.between(segmentStartDateTime, segmentEndDateTime).toMinutes()
        .coerceAtLeast(5)
        .toInt()
    return TimedEventSegment(
        item = this,
        startMillis = segmentStart,
        endMillis = segmentEnd,
        startDateTime = segmentStartDateTime,
        endDateTime = segmentEndDateTime,
        startMinutes = startMinutes,
        durationMinutes = durationMinutes
    )
}

private fun millisToDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
    return Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
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

internal fun calendarEventDateRangeLabel(item: TodoItem): String {
    val zone = com.example.todoalarm.data.calendarZone(item.eventTimeZoneId)
    val start = item.startAtMillis?.let { millisToDate(it, zone) } ?: return "未设置日期"
    val endExclusive = item.endAtMillis?.let { millisToDate(it, zone) }?.minusDays(1) ?: start
    return if (start == endExclusive) start.toString() else "$start - $endExclusive"
}

private fun calendarVisualAlpha(item: TodoItem): Float {
    val now = System.currentTimeMillis()
    val end = item.endAtMillis ?: item.startAtMillis ?: item.dueAtMillis
    return if (end < now) 0.45f else 1f
}

private fun timezoneShortLabel(zone: ZoneId = ZoneId.systemDefault()): String {
    val offset = zone.rules.getOffset(Instant.now())
    val totalMinutes = offset.totalSeconds / 60
    val sign = if (totalMinutes >= 0) "+" else "-"
    val absMinutes = kotlin.math.abs(totalMinutes)
    val hours = absMinutes / 60
    val minutes = absMinutes % 60
    val suffix = if (minutes == 0) "UTC$sign$hours" else "UTC$sign$hours:${minutes.toString().padStart(2, '0')}"
    return "${zone.id.substringAfterLast('/')}\n$suffix"
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

private fun buildEventsByDate(events: List<TodoItem>, displayZone: ZoneId = ZoneId.systemDefault()): Map<LocalDate, List<TodoItem>> {
    if (events.isEmpty()) return emptyMap()
    val bucket = linkedMapOf<LocalDate, MutableList<TodoItem>>()
    events.sortedBy { it.startAtMillis ?: it.dueAtMillis }.forEach { item ->
        val startMillis = item.startAtMillis ?: item.dueAtMillis
        val endMillis = item.endAtMillis ?: startMillis
        val zone = if (item.allDay) com.example.todoalarm.data.calendarZone(item.eventTimeZoneId) else displayZone
        var cursor = millisToDate(startMillis, zone)
        val inclusiveEnd = millisToDate((endMillis - 1).coerceAtLeast(startMillis), zone)
        while (!cursor.isAfter(inclusiveEnd)) {
            bucket.getOrPut(cursor) { mutableListOf() }.add(item)
            cursor = cursor.plusDays(1)
        }
    }
    return bucket
}

private fun timeRangeLabel(item: TodoItem, zone: ZoneId = ZoneId.systemDefault()): String {
    val start = item.startAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() } ?: return "未设置时间"
    val end = item.endAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() } ?: start
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
