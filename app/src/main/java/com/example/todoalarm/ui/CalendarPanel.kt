package com.example.todoalarm.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import com.example.todoalarm.data.TodoItem
import kotlinx.coroutines.delay
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

@Composable
internal fun CalendarPanel(
    modifier: Modifier = Modifier,
    events: List<TodoItem>,
    onQuickCreateEvent: (LocalDateTime, LocalDateTime) -> Unit,
    onEditEvent: (TodoItem) -> Unit,
    onDeleteEvent: (TodoItem) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val today = remember { LocalDate.now() }
    val days = remember(today) { (-CalendarDayRange..CalendarDayRange).map { today.plusDays(it.toLong()) } }
    val dayIndexByDate = remember(days) { days.withIndex().associate { it.value to it.index } }
    val todayIndex = remember(today, dayIndexByDate) { dayIndexByDate.getValue(today) }
    val verticalScroll = rememberScrollState()
    var didInitScroll by rememberSaveable { mutableStateOf(false) }
    var horizontalOffsetPx by rememberSaveable { mutableStateOf(0f) }
    var detailsTarget by remember { mutableStateOf<TodoItem?>(null) }
    var pendingDraft by remember { mutableStateOf<PendingCalendarDraft?>(null) }
    val currentMoment by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            delay(30_000L)
            value = LocalDateTime.now()
        }
    }

    val allDayEvents = remember(events) {
        events.filter { it.allDay }.sortedBy { it.startAtMillis ?: it.dueAtMillis }
    }
    val timedEventsByDay = remember(events) {
        events.filter { !it.allDay }
            .sortedBy { it.startAtMillis ?: it.dueAtMillis }
            .groupBy { item -> millisToDate(item.startAtMillis ?: item.dueAtMillis) }
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
            val timeAxisWidth = 54.dp
            val viewportWidth = (maxWidth - timeAxisWidth).coerceAtLeast(180.dp)
            val viewportWidthPx = with(density) { viewportWidth.toPx() }
            val dayColumnWidth = viewportWidth / 3
            val dayColumnWidthPx = with(density) { dayColumnWidth.toPx() }
            val maxHorizontalOffsetPx = ((days.size * dayColumnWidthPx) - viewportWidthPx).coerceAtLeast(0f)
            val clampedHorizontalOffsetPx = horizontalOffsetPx.coerceIn(0f, maxHorizontalOffsetPx)
            val visibleRange = remember(clampedHorizontalOffsetPx, dayColumnWidthPx, viewportWidthPx, days.size) {
                calculateVisibleDayRange(days.size, dayColumnWidthPx, viewportWidthPx, clampedHorizontalOffsetPx)
            }
            val visibleDays = remember(days, visibleRange) { days.slice(visibleRange) }
            val visibleStart = days[visibleRange.first]
            val visibleEnd = days[visibleRange.last]
            val hourHeight = 72.dp
            val boardHeight = hourHeight * 24
            val hourHeightPx = with(density) { hourHeight.toPx() }
            val centerDayIndex = (((clampedHorizontalOffsetPx + viewportWidthPx / 2f) / dayColumnWidthPx).toInt())
                .coerceIn(0, days.lastIndex)
            val headerMonthDate = days[centerDayIndex]
            val visibleAllDayEvents = remember(allDayEvents, visibleStart, visibleEnd) {
                allDayEvents.filter { item -> overlapsDateRange(item, visibleStart, visibleEnd) }
            }
            val visibleTimedEventsByDay = remember(timedEventsByDay, visibleDays) {
                visibleDays.associateWith { day -> timedEventsByDay[day].orEmpty() }
            }
            val horizontalScrollableState = rememberScrollableState { delta ->
                val previous = horizontalOffsetPx
                horizontalOffsetPx = (horizontalOffsetPx - delta).coerceIn(0f, maxHorizontalOffsetPx)
                previous - horizontalOffsetPx
            }

            fun jumpToDate(targetDate: LocalDate) {
                val targetIndex = dayIndexByDate[targetDate] ?: return
                horizontalOffsetPx = (targetIndex * dayColumnWidthPx).coerceIn(0f, maxHorizontalOffsetPx)
            }

            LaunchedEffect(dayColumnWidthPx, hourHeightPx, maxHorizontalOffsetPx) {
                if (!didInitScroll) {
                    horizontalOffsetPx = (((todayIndex - 1) * dayColumnWidthPx).coerceIn(0f, maxHorizontalOffsetPx))
                    val targetHour = (currentMoment.hour - 2).coerceAtLeast(0)
                    verticalScroll.scrollTo((targetHour * hourHeightPx).roundToInt())
                    didInitScroll = true
                } else if (horizontalOffsetPx > maxHorizontalOffsetPx) {
                    horizontalOffsetPx = maxHorizontalOffsetPx
                }
            }

            LaunchedEffect(clampedHorizontalOffsetPx, verticalScroll.value) {
                pendingDraft = null
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = headerMonthDate.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day -> jumpToDate(LocalDate.of(year, month + 1, day)) },
                                    headerMonthDate.year,
                                    headerMonthDate.monthValue - 1,
                                    headerMonthDate.dayOfMonth
                                ).show()
                            }
                        )
                        IconButton(onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day -> jumpToDate(LocalDate.of(year, month + 1, day)) },
                                headerMonthDate.year,
                                headerMonthDate.monthValue - 1,
                                headerMonthDate.dayOfMonth
                            ).show()
                        }) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = "选择日期")
                        }
                    }
                    Row {
                        IconButton(onClick = {
                            horizontalOffsetPx = (clampedHorizontalOffsetPx - dayColumnWidthPx).coerceAtLeast(0f)
                        }) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = "向前查看")
                        }
                        IconButton(onClick = {
                            horizontalOffsetPx = (clampedHorizontalOffsetPx + dayColumnWidthPx).coerceAtMost(maxHorizontalOffsetPx)
                        }) {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = "向后查看")
                        }
                    }
                }

                CalendarHeaderRow(
                    timeAxisWidth = timeAxisWidth,
                    viewportWidth = viewportWidth,
                    dayColumnWidth = dayColumnWidth,
                    dayColumnWidthPx = dayColumnWidthPx,
                    days = days,
                    visibleRange = visibleRange,
                    currentDate = currentMoment.toLocalDate(),
                    horizontalOffsetPx = clampedHorizontalOffsetPx,
                    todayHighlightColor = todayHighlightColor,
                    todayHighlightTextColor = todayHighlightTextColor,
                    dragModifier = Modifier.scrollable(horizontalScrollableState, Orientation.Horizontal)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
                CalendarAllDaySection(
                    timeAxisWidth = timeAxisWidth,
                    viewportWidth = viewportWidth,
                    dayColumnWidth = dayColumnWidth,
                    dayColumnWidthPx = dayColumnWidthPx,
                    visibleRange = visibleRange,
                    horizontalOffsetPx = clampedHorizontalOffsetPx,
                    events = visibleAllDayEvents,
                    dayIndexByDate = dayIndexByDate,
                    dragModifier = Modifier.scrollable(horizontalScrollableState, Orientation.Horizontal),
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
                        showCurrentTime = todayIndex in visibleRange
                    )
                    CalendarTimedBoard(
                        modifier = Modifier
                            .width(viewportWidth)
                            .scrollable(horizontalScrollableState, Orientation.Horizontal),
                        days = days,
                        visibleRange = visibleRange,
                        dayColumnWidth = dayColumnWidth,
                        dayColumnWidthPx = dayColumnWidthPx,
                        horizontalOffsetPx = clampedHorizontalOffsetPx,
                        boardHeight = boardHeight,
                        hourHeight = hourHeight,
                        hourHeightPx = hourHeightPx,
                        verticalScroll = verticalScroll,
                        currentMoment = currentMoment,
                        eventsByDay = visibleTimedEventsByDay,
                        dayIndexByDate = dayIndexByDate,
                        pendingDraft = pendingDraft,
                        onPendingDraftChange = { pendingDraft = it },
                        onQuickCreateEvent = onQuickCreateEvent,
                        onOpenDetails = { detailsTarget = it }
                    )
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
    val rowCount = events.size.coerceAtLeast(1)
    val density = LocalDensity.current
    val rowHeightPx = with(density) { 28.dp.roundToPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height((rowCount * 28).dp + 10.dp)
            .padding(vertical = 4.dp)
    ) {
        Box(modifier = Modifier.width(timeAxisWidth), contentAlignment = Alignment.TopStart) {
            Text(text = "全天", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .width(viewportWidth)
                .clipToBounds()
                .then(dragModifier)
        ) {
            events.forEachIndexed { rowIndex, item ->
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
                        .height(22.dp)
                        .clickable { onOpenDetails(item) },
                    shape = RoundedCornerShape(8.dp),
                    color = tint.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = item.title,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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
    eventsByDay: Map<LocalDate, List<TodoItem>>,
    dayIndexByDate: Map<LocalDate, Int>,
    pendingDraft: PendingCalendarDraft?,
    onPendingDraftChange: (PendingCalendarDraft?) -> Unit,
    onQuickCreateEvent: (LocalDateTime, LocalDateTime) -> Unit,
    onOpenDetails: (TodoItem) -> Unit
) {
    val density = LocalDensity.current
    val outline = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .clipToBounds()
            .verticalScroll(verticalScroll)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(boardHeight)
                .pointerInput(visibleRange, horizontalOffsetPx, pendingDraft) {
                    detectTapGestures { tapOffset ->
                        val dayIndex = ((tapOffset.x + horizontalOffsetPx) / dayColumnWidthPx)
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
                        if (pendingDraft == nextDraft) {
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

            visibleRange.forEach { dayIndex ->
                val day = days[dayIndex]
                val placements = layoutTimedEvents(eventsByDay[day].orEmpty())
                placements.forEach { placement ->
                    TimedEventCard(
                        item = placement.item,
                        placement = placement,
                        dayIndex = dayIndex,
                        dayColumnWidth = dayColumnWidth,
                        dayColumnWidthPx = dayColumnWidthPx,
                        horizontalOffsetPx = horizontalOffsetPx,
                        hourHeight = hourHeight,
                        onClick = { onOpenDetails(placement.item) }
                    )
                }
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
                days = days,
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

    Surface(
        modifier = Modifier
            .offset(
                x = with(LocalDensity.current) { dayLeftPx(dayIndex, dayColumnWidthPx, horizontalOffsetPx).toDp() } + 4.dp,
                y = topOffset
            )
            .width(dayColumnWidth - 8.dp)
            .height((hourHeight * (durationMinutes / 60f)).coerceAtLeast(42.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0x264C8BF5)
    ) {
        Text(
            text = "新日程",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            color = Color(0xFF3C6FE0),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun TimedEventCard(
    item: TodoItem,
    placement: TimedEventPlacement,
    dayIndex: Int,
    dayColumnWidth: Dp,
    dayColumnWidthPx: Float,
    horizontalOffsetPx: Float,
    hourHeight: Dp,
    onClick: () -> Unit
) {
    val start = item.startAtMillis ?: return
    val end = item.endAtMillis ?: return
    val startDateTime = reminderAtMillisToDateTime(start)
    val endDateTime = reminderAtMillisToDateTime(end)
    val startMinutes = startDateTime.hour * 60 + startDateTime.minute
    val durationMinutes = java.time.Duration.between(startDateTime, endDateTime).toMinutes().coerceAtLeast(20)
    val tint = item.accentColorHex?.let(::colorFromHex) ?: MaterialTheme.colorScheme.primary
    val alpha = calendarVisualAlpha(item)
    val topOffset = hourHeight * (startMinutes / 60f)
    val eventWidth = (dayColumnWidth / placement.columnCount) - 8.dp
    val baseLeft = with(LocalDensity.current) { dayLeftPx(dayIndex, dayColumnWidthPx, horizontalOffsetPx).toDp() }
    val leftOffset = baseLeft +
        (dayColumnWidth / placement.columnCount) * placement.columnIndex +
        4.dp

    Surface(
        modifier = Modifier
            .offset(x = leftOffset, y = topOffset)
            .width(eventWidth)
            .height((hourHeight * (durationMinutes / 60f)).coerceAtLeast(42.dp))
            .clickable(onClick = onClick)
            .alpha(alpha),
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.16f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                color = tint,
                fontSize = 13.sp,
                lineHeight = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (item.location.isNotBlank()) {
                Text(
                    text = item.location,
                    color = tint.copy(alpha = 0.82f),
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CurrentTimeLine(
    days: List<LocalDate>,
    currentMoment: LocalDateTime,
    dayColumnWidthPx: Float,
    horizontalOffsetPx: Float,
    boardHeightPx: Float,
    hourHeightPx: Float
) {
    val minutes = currentMoment.hour * 60 + currentMoment.minute
    val y = (minutes / 60f) * hourHeightPx
    val todayIndex = days.indexOf(currentMoment.toLocalDate())

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (todayIndex < 0 || y !in 0f..boardHeightPx) return@Canvas
        val splitX = todayIndex * dayColumnWidthPx - horizontalOffsetPx
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.title, fontWeight = FontWeight.Bold)
                item.location.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (item.allDay) "全天 · ${formatDateRange(item)}" else formatDateTimeRange(item),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (item.notes.isNotBlank()) {
                    Text(text = item.notes, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                }
                item.reminderAtMillis?.let {
                    Text(
                        text = "提醒：${formatLocalDateTime(reminderAtMillisToDateTime(it))}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

private data class TimedEventPlacement(
    val item: TodoItem,
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

private fun layoutTimedEvents(events: List<TodoItem>): List<TimedEventPlacement> {
    if (events.isEmpty()) return emptyList()
    val sorted = events.sortedBy { it.startAtMillis ?: it.dueAtMillis }
    val result = mutableListOf<TimedEventPlacement>()
    var cluster = mutableListOf<Pair<TodoItem, Int>>()
    var active = mutableListOf<Pair<TodoItem, Int>>()
    var clusterMaxColumns = 1
    var clusterEnd = Long.MIN_VALUE

    fun flushCluster() {
        if (cluster.isEmpty()) return
        cluster.forEach { (item, column) ->
            result += TimedEventPlacement(item, column, clusterMaxColumns)
        }
        cluster = mutableListOf()
        active = mutableListOf()
        clusterMaxColumns = 1
        clusterEnd = Long.MIN_VALUE
    }

    sorted.forEach { item ->
        val start = item.startAtMillis ?: item.dueAtMillis
        val end = item.endAtMillis ?: (start + 30 * 60_000L)
        if (cluster.isNotEmpty() && start >= clusterEnd) flushCluster()
        active = active.filterTo(mutableListOf()) { activeItem ->
            val activeEnd = activeItem.first.endAtMillis
                ?: ((activeItem.first.startAtMillis ?: activeItem.first.dueAtMillis) + 30 * 60_000L)
            activeEnd > start
        }
        val usedColumns = active.map { it.second }.toSet()
        var column = 0
        while (column in usedColumns) column += 1
        cluster += item to column
        active += item to column
        clusterMaxColumns = max(clusterMaxColumns, active.size)
        clusterEnd = max(clusterEnd, end)
    }
    flushCluster()
    return result
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

private fun java.time.DayOfWeek.shortLabel(): String = when (this) {
    java.time.DayOfWeek.MONDAY -> "周一"
    java.time.DayOfWeek.TUESDAY -> "周二"
    java.time.DayOfWeek.WEDNESDAY -> "周三"
    java.time.DayOfWeek.THURSDAY -> "周四"
    java.time.DayOfWeek.FRIDAY -> "周五"
    java.time.DayOfWeek.SATURDAY -> "周六"
    java.time.DayOfWeek.SUNDAY -> "周日"
}
