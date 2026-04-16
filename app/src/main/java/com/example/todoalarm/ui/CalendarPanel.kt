package com.example.todoalarm.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoalarm.data.TodoItem
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

private val CalendarPalette = listOf(
    "#4E87E1", "#4CB782", "#FF6B4A", "#BF7B4D", "#8B5CF6", "#0F766E", "#D97706", "#E11D48"
)

@Composable
internal fun CalendarPanel(
    events: List<TodoItem>,
    onEditEvent: (TodoItem) -> Unit,
    onDeleteEvent: (TodoItem) -> Unit
) {
    val today = remember { LocalDate.now() }
    var centerEpochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var detailsTarget by remember { mutableStateOf<TodoItem?>(null) }
    val centerDate = LocalDate.ofEpochDay(centerEpochDay)
    val visibleDays = remember(centerEpochDay) {
        listOf(centerDate.minusDays(1), centerDate, centerDate.plusDays(1))
    }
    val allDayEvents = remember(events, centerEpochDay) {
        events.filter { it.allDay && overlapsVisibleDays(it, visibleDays) }.sortedBy { it.startAtMillis ?: it.dueAtMillis }
    }
    val timedEvents = remember(events, centerEpochDay) {
        events.filter { !it.allDay && overlapsVisibleDays(it, visibleDays) }.sortedBy { it.startAtMillis ?: it.dueAtMillis }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ElevatedCard(shape = RoundedCornerShape(28.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "日历",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = weekRangeLabel(visibleDays.first(), visibleDays.last()),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { centerEpochDay = LocalDate.now().toEpochDay() }) {
                            Text("今天")
                        }
                        IconButton(onClick = { centerEpochDay = centerDate.minusDays(1).toEpochDay() }) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = "前一天")
                        }
                        IconButton(onClick = { centerEpochDay = centerDate.plusDays(1).toEpochDay() }) {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = "后一天")
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    visibleDays.forEach { day ->
                        val isToday = day == LocalDate.now()
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isToday) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = day.dayOfWeek.shortLabel(),
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        WeekScheduleBoard(
            visibleDays = visibleDays,
            allDayEvents = allDayEvents,
            timedEvents = timedEvents,
            onOpenDetails = { detailsTarget = it }
        )
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
private fun WeekScheduleBoard(
    visibleDays: List<LocalDate>,
    allDayEvents: List<TodoItem>,
    timedEvents: List<TodoItem>,
    onOpenDetails: (TodoItem) -> Unit
) {
    val hourHeight = 72.dp
    val timeColumnWidth = 48.dp
    val dayColumnWidth = 112.dp
    val boardHeight = hourHeight * 24
    val verticalScroll = rememberScrollState()
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val boardHeightPx = with(density) { boardHeight.toPx() }
    val dayColumnWidthPx = with(density) { dayColumnWidth.toPx() }
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    LaunchedEffect(visibleDays) {
        val today = LocalDate.now()
        if (today in visibleDays) {
            val now = LocalTime.now()
            val target = ((now.hour - 2).coerceAtLeast(0) * hourHeightPx).roundToInt()
            verticalScroll.scrollTo(target)
        }
    }

    ElevatedCard(shape = RoundedCornerShape(28.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (allDayEvents.isNotEmpty()) {
                AllDayEventsStrip(
                    visibleDays = visibleDays,
                    events = allDayEvents,
                    timeColumnWidth = timeColumnWidth,
                    dayColumnWidth = dayColumnWidth,
                    onOpenDetails = onOpenDetails
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(verticalScroll)
            ) {
                Column(
                    modifier = Modifier.width(timeColumnWidth),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    repeat(24) { hour ->
                        Box(
                            modifier = Modifier.height(hourHeight),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = "%02d:00".format(hour),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(boardHeight)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        visibleDays.indices.forEach { index ->
                            val left = index * dayColumnWidthPx
                            drawLine(
                                color = outlineVariant.copy(alpha = 0.45f),
                                start = Offset(left, 0f),
                                end = Offset(left, size.height)
                            )
                        }
                        drawLine(
                            color = outlineVariant.copy(alpha = 0.45f),
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height)
                        )
                        repeat(25) { hour ->
                            val y = hour * hourHeightPx
                            drawLine(
                                color = outlineVariant.copy(alpha = if (hour % 6 == 0) 0.5f else 0.28f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y)
                            )
                        }
                    }

                    visibleDays.forEachIndexed { index, day ->
                        val dayEvents = timedEvents.filter { event ->
                            val start = event.startAtMillis ?: return@filter false
                            Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate() == day
                        }
                        val placements = layoutTimedEvents(dayEvents)
                        placements.forEach { placement ->
                            TimedEventCard(
                                item = placement.item,
                                placement = placement,
                                visibleDayIndex = index,
                                dayColumnWidth = dayColumnWidth,
                                hourHeight = hourHeight,
                                onClick = { onOpenDetails(placement.item) }
                            )
                        }
                    }

                    if (LocalDate.now() in visibleDays) {
                        CurrentTimeLine(
                            visibleDays = visibleDays,
                            dayColumnWidth = dayColumnWidth,
                            boardHeightPx = boardHeightPx,
                            hourHeightPx = hourHeightPx
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllDayEventsStrip(
    visibleDays: List<LocalDate>,
    events: List<TodoItem>,
    timeColumnWidth: Dp,
    dayColumnWidth: Dp,
    onOpenDetails: (TodoItem) -> Unit
) {
    val density = LocalDensity.current
    val timeColumnWidthPx = with(density) { timeColumnWidth.roundToPx() }
    val dayColumnWidthPx = with(density) { dayColumnWidth.roundToPx() }
    val rowHeightPx = with(density) { 30.dp.roundToPx() }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(timeColumnWidth), contentAlignment = Alignment.Center) {
                Text("全天", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Text(
                text = "全天日程",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((events.size * 30).coerceAtLeast(36).dp)
        ) {
            events.forEachIndexed { rowIndex, item ->
                val startDate = item.startAtMillis?.let { millisToDate(it) } ?: return@forEachIndexed
                val endDateExclusive = item.endAtMillis?.let { millisToDate(it) }?.minusDays(1) ?: startDate
                val startIndex = visibleDays.indexOfFirst { !it.isBefore(startDate) }
                    .takeIf { it >= 0 } ?: 0
                val endIndex = visibleDays.indexOfLast { !it.isAfter(endDateExclusive) }
                    .takeIf { it >= 0 } ?: visibleDays.lastIndex
                if (endIndex < startIndex) return@forEachIndexed
                val tint = item.accentColorHex?.let(::colorFromHex) ?: MaterialTheme.colorScheme.primary
                Surface(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = timeColumnWidthPx + startIndex * dayColumnWidthPx + 4,
                                y = rowIndex * rowHeightPx
                            )
                        }
                        .width(dayColumnWidth * (endIndex - startIndex + 1) - 8.dp)
                        .height(24.dp)
                        .clickable { onOpenDetails(item) },
                    shape = RoundedCornerShape(12.dp),
                    color = tint.copy(alpha = calendarVisualAlpha(item) * 0.22f)
                ) {
                    Text(
                        text = item.title,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
            .height((hourHeight * (durationMinutes / 60f)).coerceAtLeast(28.dp))
            .clickable(onClick = onClick)
            .alpha(alpha),
        shape = RoundedCornerShape(16.dp),
        color = tint.copy(alpha = 0.18f)
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
                fontSize = 12.sp,
                maxLines = if (durationMinutes >= 90) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${shortTime(startDateTime)} - ${shortTime(endDateTime)}",
                color = tint.copy(alpha = 0.92f),
                fontSize = 11.sp,
                maxLines = 1
            )
            if (item.location.isNotBlank()) {
                Text(
                    text = item.location,
                    color = tint.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    maxLines = if (durationMinutes >= 120) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CurrentTimeLine(
    visibleDays: List<LocalDate>,
    dayColumnWidth: Dp,
    boardHeightPx: Float,
    hourHeightPx: Float
) {
    val now = LocalDateTime.now()
    val minutes = now.hour * 60 + now.minute
    val y = (minutes / 60f) * hourHeightPx
    val todayIndex = visibleDays.indexOf(LocalDate.now())
    val dayWidthPx = with(LocalDensity.current) { dayColumnWidth.toPx() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (y !in 0f..boardHeightPx) return@Canvas
        val splitX = todayIndex * dayWidthPx
        if (splitX > 0f) {
            drawLine(
                color = Color(0xFFF5A4A4),
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
                    text = if (item.allDay) {
                        "全天 · ${formatDateRange(item)}"
                    } else {
                        formatDateTimeRange(item)
                    },
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
            val activeEnd = activeItem.first.endAtMillis ?: ((activeItem.first.startAtMillis ?: activeItem.first.dueAtMillis) + 30 * 60_000L)
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

private fun shortTime(dateTime: LocalDateTime): String {
    return dateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA))
}

private fun weekRangeLabel(start: LocalDate, end: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
    return "${start.format(formatter)} - ${end.format(formatter)}"
}

private fun formatDateTimeRange(item: TodoItem): String {
    val start = item.startAtMillis?.let(::reminderAtMillisToDateTime) ?: return "未设置时间"
    val end = item.endAtMillis?.let(::reminderAtMillisToDateTime) ?: start
    val dateFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
    return if (start.toLocalDate() == end.toLocalDate()) {
        "${start.format(dateFormatter)} ${shortTime(start)} - ${shortTime(end)}"
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

private fun LocalDate.dayOfWeekShort(): String = dayOfWeek.shortLabel()

private fun java.time.DayOfWeek.shortLabel(): String = when (this) {
    java.time.DayOfWeek.MONDAY -> "周一"
    java.time.DayOfWeek.TUESDAY -> "周二"
    java.time.DayOfWeek.WEDNESDAY -> "周三"
    java.time.DayOfWeek.THURSDAY -> "周四"
    java.time.DayOfWeek.FRIDAY -> "周五"
    java.time.DayOfWeek.SATURDAY -> "周六"
    java.time.DayOfWeek.SUNDAY -> "周日"
}
