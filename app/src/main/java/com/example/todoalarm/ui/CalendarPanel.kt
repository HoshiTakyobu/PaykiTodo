package com.example.todoalarm.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private const val CalendarPageCount = 4001
private const val CalendarPageAnchor = CalendarPageCount / 2

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CalendarPanel(
    modifier: Modifier = Modifier,
    events: List<TodoItem>,
    onQuickCreateEvent: (LocalDateTime, LocalDateTime) -> Unit,
    onEditEvent: (TodoItem) -> Unit,
    onDeleteEvent: (TodoItem) -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val pagerState = rememberPagerState(
        initialPage = CalendarPageAnchor,
        pageCount = { CalendarPageCount }
    )
    val scope = rememberCoroutineScope()
    var detailsTarget by remember { mutableStateOf<TodoItem?>(null) }
    val centerDate = remember(today, pagerState.currentPage) {
        today.plusDays((pagerState.currentPage - CalendarPageAnchor).toLong())
    }
    val visibleDays = remember(centerDate) {
        listOf(centerDate.minusDays(1), centerDate, centerDate.plusDays(1))
    }
    val allDayEvents = remember(events, visibleDays) {
        events.filter { it.allDay && overlapsVisibleDays(it, visibleDays) }
            .sortedBy { it.startAtMillis ?: it.dueAtMillis }
    }
    val currentMoment by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            delay(30_000L)
            value = LocalDateTime.now()
        }
    }

    Surface(
        modifier = modifier,
        color = Color(0xFFF7F8FB)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            val timeAxisWidth = 54.dp
            val rightWidth = (maxWidth - timeAxisWidth).coerceAtLeast(180.dp)
            val dayColumnWidth = rightWidth / 3
            val hourHeight = 72.dp
            val boardHeight = hourHeight * 24
            val verticalScroll = rememberScrollState()
            val density = LocalDensity.current
            val hourHeightPx = with(density) { hourHeight.toPx() }

            LaunchedEffect(centerDate) {
                if (currentMoment.toLocalDate() in visibleDays) {
                    val targetHour = (currentMoment.hour - 2).coerceAtLeast(0)
                    verticalScroll.scrollTo((targetHour * hourHeightPx).roundToInt())
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = centerDate.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val targetDate = LocalDate.of(year, month + 1, day)
                                        val targetPage = (CalendarPageAnchor + (targetDate.toEpochDay() - today.toEpochDay()).toInt())
                                            .coerceIn(0, CalendarPageCount - 1)
                                        scope.launch { pagerState.animateScrollToPage(targetPage) }
                                    },
                                    centerDate.year,
                                    centerDate.monthValue - 1,
                                    centerDate.dayOfMonth
                                ).show()
                            }
                        )
                        IconButton(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val targetDate = LocalDate.of(year, month + 1, day)
                                        val targetPage = (CalendarPageAnchor + (targetDate.toEpochDay() - today.toEpochDay()).toInt())
                                            .coerceIn(0, CalendarPageCount - 1)
                                        scope.launch { pagerState.animateScrollToPage(targetPage) }
                                    },
                                    centerDate.year,
                                    centerDate.monthValue - 1,
                                    centerDate.dayOfMonth
                                ).show()
                            }
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = "选择日期")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) } }) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = "前一天")
                        }
                        IconButton(onClick = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(CalendarPageCount - 1)) } }) {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = "后一天")
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.width(timeAxisWidth),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = timezoneShortLabel(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    visibleDays.forEach { day ->
                        val isToday = day == currentMoment.toLocalDate()
                        Column(
                            modifier = Modifier.width(dayColumnWidth),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = day.dayOfWeek.shortLabel(),
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                            Text(
                                text = day.dayOfMonth.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))

                CalendarAllDayRow(
                    visibleDays = visibleDays,
                    allDayEvents = allDayEvents,
                    timeAxisWidth = timeAxisWidth,
                    dayColumnWidth = dayColumnWidth,
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
                        verticalScroll = verticalScroll
                    )

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .width(rightWidth)
                            .fillMaxHeight()
                    ) { page ->
                        val pageCenterDate = today.plusDays((page - CalendarPageAnchor).toLong())
                        val pageVisibleDays = remember(pageCenterDate) {
                            listOf(pageCenterDate.minusDays(1), pageCenterDate, pageCenterDate.plusDays(1))
                        }
                        val pageTimedEvents = remember(events, pageVisibleDays) {
                            events.filter { !it.allDay && overlapsVisibleDays(it, pageVisibleDays) }
                                .sortedBy { it.startAtMillis ?: it.dueAtMillis }
                        }
                        CalendarTimedBoard(
                            visibleDays = pageVisibleDays,
                            timedEvents = pageTimedEvents,
                            currentMoment = currentMoment,
                            dayColumnWidth = dayColumnWidth,
                            hourHeight = hourHeight,
                            boardHeight = boardHeight,
                            verticalScroll = verticalScroll,
                            onOpenDetails = { detailsTarget = it },
                            onQuickCreateEvent = onQuickCreateEvent
                        )
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
}

@Composable
private fun CalendarAllDayRow(
    visibleDays: List<LocalDate>,
    allDayEvents: List<TodoItem>,
    timeAxisWidth: Dp,
    dayColumnWidth: Dp,
    onOpenDetails: (TodoItem) -> Unit
) {
    val density = LocalDensity.current
    val timeAxisWidthPx = with(density) { timeAxisWidth.roundToPx() }
    val dayColumnWidthPx = with(density) { dayColumnWidth.roundToPx() }
    val rowHeightPx = with(density) { 28.dp.roundToPx() }
    val rowCount = allDayEvents.size.coerceAtLeast(1)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((rowCount * 28).dp + 8.dp)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier.width(timeAxisWidth),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = "全天",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        allDayEvents.forEachIndexed { rowIndex, item ->
            val startDate = item.startAtMillis?.let(::millisToDate) ?: return@forEachIndexed
            val endDateExclusive = item.endAtMillis?.let(::millisToDate)?.minusDays(1) ?: startDate
            val startIndex = visibleDays.indexOfFirst { !it.isBefore(startDate) }.takeIf { it >= 0 } ?: 0
            val endIndex = visibleDays.indexOfLast { !it.isAfter(endDateExclusive) }.takeIf { it >= 0 } ?: visibleDays.lastIndex
            if (endIndex < startIndex) return@forEachIndexed
            val tint = item.accentColorHex?.let(::colorFromHex) ?: MaterialTheme.colorScheme.primary

            Surface(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = timeAxisWidthPx + startIndex * dayColumnWidthPx + 4,
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

@Composable
private fun CalendarTimeAxis(
    width: Dp,
    hourHeight: Dp,
    verticalScroll: androidx.compose.foundation.ScrollState
) {
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .verticalScroll(verticalScroll),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        repeat(24) { hour ->
            Box(
                modifier = Modifier.height(hourHeight),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = "%02d:00".format(hour),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun CalendarTimedBoard(
    visibleDays: List<LocalDate>,
    timedEvents: List<TodoItem>,
    currentMoment: LocalDateTime,
    dayColumnWidth: Dp,
    hourHeight: Dp,
    boardHeight: Dp,
    verticalScroll: androidx.compose.foundation.ScrollState,
    onOpenDetails: (TodoItem) -> Unit,
    onQuickCreateEvent: (LocalDateTime, LocalDateTime) -> Unit
) {
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val dayColumnWidthPx = with(density) { dayColumnWidth.toPx() }
    val boardHeightPx = with(density) { boardHeight.toPx() }
    val outline = MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(verticalScroll)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(boardHeight)
                .pointerInput(visibleDays) {
                    detectTapGestures { offset ->
                        val dayIndex = (offset.x / dayColumnWidthPx).toInt().coerceIn(0, visibleDays.lastIndex)
                        val rawMinutes = ((offset.y / hourHeightPx) * 60f).roundToInt().coerceIn(0, 23 * 60 + 59)
                        val snappedMinutes = ((rawMinutes + 7) / 15) * 15
                        val safeMinutes = snappedMinutes.coerceIn(0, 23 * 60 + 45)
                        val startAt = LocalDateTime.of(
                            visibleDays[dayIndex],
                            LocalTime.of(safeMinutes / 60, safeMinutes % 60)
                        )
                        onQuickCreateEvent(startAt, startAt.plusMinutes(30))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                visibleDays.indices.forEach { index ->
                    val left = index * dayColumnWidthPx
                    drawLine(
                        color = outline.copy(alpha = 0.12f),
                        start = Offset(left, 0f),
                        end = Offset(left, size.height)
                    )
                }
                drawLine(
                    color = outline.copy(alpha = 0.12f),
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height)
                )
                repeat(25) { hour ->
                    val y = hour * hourHeightPx
                    drawLine(
                        color = outline.copy(alpha = 0.26f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y)
                    )
                }
            }

            visibleDays.forEachIndexed { dayIndex, day ->
                val dayEvents = timedEvents.filter { event ->
                    val start = event.startAtMillis ?: return@filter false
                    Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate() == day
                }
                val placements = layoutTimedEvents(dayEvents)
                placements.forEach { placement ->
                    TimedEventCard(
                        item = placement.item,
                        placement = placement,
                        visibleDayIndex = dayIndex,
                        dayColumnWidth = dayColumnWidth,
                        hourHeight = hourHeight,
                        onClick = { onOpenDetails(placement.item) }
                    )
                }
            }

            if (currentMoment.toLocalDate() in visibleDays) {
                CurrentTimeLine(
                    visibleDays = visibleDays,
                    currentMoment = currentMoment,
                    dayColumnWidth = dayColumnWidth,
                    boardHeightPx = boardHeightPx,
                    hourHeightPx = hourHeightPx
                )
            }
        }
    }
}

@Composable
private fun TimedEventCard(
    item: TodoItem,
    placement: TimedEventPlacement,
    visibleDayIndex: Int,
    dayColumnWidth: Dp,
    hourHeight: Dp,
    onClick: () -> Unit
) {
    val start = item.startAtMillis ?: return
    val end = item.endAtMillis ?: return
    val startDateTime = reminderAtMillisToDateTime(start)
    val endDateTime = reminderAtMillisToDateTime(end)
    val startMinutes = startDateTime.hour * 60 + startDateTime.minute
    val durationMinutes = Duration.between(startDateTime, endDateTime).toMinutes().coerceAtLeast(20)
    val tint = item.accentColorHex?.let(::colorFromHex) ?: MaterialTheme.colorScheme.primary
    val alpha = calendarVisualAlpha(item)
    val topOffset = hourHeight * (startMinutes / 60f)
    val eventWidth = (dayColumnWidth / placement.columnCount) - 8.dp
    val leftOffset = dayColumnWidth * visibleDayIndex +
        (dayColumnWidth / placement.columnCount) * placement.columnIndex +
        4.dp

    Surface(
        modifier = Modifier
            .offset(x = leftOffset, y = topOffset)
            .width(eventWidth)
            .height((hourHeight * (durationMinutes / 60f)).coerceAtLeast(40.dp))
            .clickable(onClick = onClick)
            .alpha(alpha),
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.16f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                color = tint,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (item.location.isNotBlank()) {
                Text(
                    text = item.location,
                    color = tint.copy(alpha = 0.82f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CurrentTimeLine(
    visibleDays: List<LocalDate>,
    currentMoment: LocalDateTime,
    dayColumnWidth: Dp,
    boardHeightPx: Float,
    hourHeightPx: Float
) {
    val minutes = currentMoment.hour * 60 + currentMoment.minute
    val y = (minutes / 60f) * hourHeightPx
    val todayIndex = visibleDays.indexOf(currentMoment.toLocalDate())
    val dayWidthPx = with(LocalDensity.current) { dayColumnWidth.toPx() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (todayIndex < 0 || y !in 0f..boardHeightPx) return@Canvas
        val splitX = todayIndex * dayWidthPx
        if (splitX > 0f) {
            drawLine(
                color = Color(0xFFF3B2B2),
                start = Offset(0f, y),
                end = Offset(splitX, y),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawLine(
            color = Color(0xFFE53935),
            start = Offset(splitX, y),
            end = Offset(size.width, y),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color(0xFFD32F2F),
            radius = 6.dp.toPx(),
            center = Offset(splitX, y)
        )
    }
}

@Composable
private fun CalendarEventDetailsDialog(
    item: TodoItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
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
                    Text(
                        text = item.notes,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
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

private data class TimedEventPlacement(
    val item: TodoItem,
    val columnIndex: Int,
    val columnCount: Int
)

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
            result += TimedEventPlacement(item = item, columnIndex = column, columnCount = clusterMaxColumns)
        }
        cluster = mutableListOf()
        active = mutableListOf()
        clusterMaxColumns = 1
        clusterEnd = Long.MIN_VALUE
    }

    sorted.forEach { item ->
        val start = item.startAtMillis ?: item.dueAtMillis
        val end = item.endAtMillis ?: (start + 30 * 60_000L)
        if (cluster.isNotEmpty() && start >= clusterEnd) {
            flushCluster()
        }

        active = active.filterTo(mutableListOf()) { activeItem ->
            val activeEnd = activeItem.first.endAtMillis
                ?: ((activeItem.first.startAtMillis ?: activeItem.first.dueAtMillis) + 30 * 60_000L)
            activeEnd > start
        }
        val usedColumns = active.map { it.second }.toSet()
        var column = 0
        while (column in usedColumns) {
            column += 1
        }
        cluster += item to column
        active += item to column
        clusterMaxColumns = max(clusterMaxColumns, active.size)
        clusterEnd = max(clusterEnd, end)
    }
    flushCluster()
    return result
}

private fun overlapsVisibleDays(item: TodoItem, visibleDays: List<LocalDate>): Boolean {
    val start = item.startAtMillis ?: item.dueAtMillis
    val end = item.endAtMillis ?: start
    val startDate = millisToDate(start)
    val endDate = millisToDate(if (item.allDay) end - 1 else end)
    return visibleDays.any { day -> !day.isBefore(startDate) && !day.isAfter(endDate) }
}

private fun millisToDate(epochMillis: Long): LocalDate {
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
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
    return if (start == endExclusive) {
        start.toString()
    } else {
        "$start - $endExclusive"
    }
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
