package com.example.todoalarm.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import com.example.todoalarm.R
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.DailyBoardSnapshot
import com.example.todoalarm.data.DailyBoardSnapshotBuilder
import com.example.todoalarm.data.EventCheckIn
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.MainActivity
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodoWidgetFactory(applicationContext)
    }
}

private class TodoWidgetFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {
    private var rows: List<WidgetBoardRow> = emptyList()
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月", Locale.CHINA)
    private val fullDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
    private var darkText: Int = 0
    private var mutedText: Int = 0
    private var headerText: Int = 0
    private var orange: Int = 0
    private var danger: Int = 0
    private var eventBlue: Int = 0
    private var todoFallback: Int = 0

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        loadWidgetColors()
        val app = context.applicationContext as TodoApplication
        rows = runBlocking {
            val items = app.repository.getActiveItemsForBoardRange()
            val groups = app.repository.getAllGroups()
            val notes = app.repository.getPlanningNotesWithAnnouncementHints()
            val snapshot = DailyBoardSnapshotBuilder.build(items = items, planningNotes = notes)
            val activeCheckIns = app.repository.getActiveCheckInsForEvents(
                snapshot.visibleTodayEvents
                    .filter { event ->
                        event.checkInEnabled && DailyBoardSnapshotBuilder.eventInProgress(event, snapshot.now)
                    }
                    .map { it.id }
            ).associateBy { it.eventId }
            buildRows(
                snapshot = snapshot,
                groups = groups,
                activeCheckIns = activeCheckIns
            )
        }
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = rows.getOrNull(position) ?: return emptyRowViews(emptyRow(-1L, "今日看板暂无内容"))
        return when (row.type) {
            WidgetRowType.GREETING -> greetingViews(row)
            WidgetRowType.SECTION -> sectionViews(row)
            WidgetRowType.EMPTY -> emptyRowViews(row)
            WidgetRowType.TODO -> todoViews(row)
            WidgetRowType.SCHEDULE -> scheduleViews(row)
            WidgetRowType.EVENT -> eventViews(row)
            WidgetRowType.ANNOUNCEMENT -> announcementViews(row)
        }.apply {
            setOnClickFillInIntent(R.id.widget_item_root, row.fillInIntent())
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = WidgetRowType.entries.size
    override fun getItemId(position: Int): Long = rows.getOrNull(position)?.stableId ?: position.toLong()
    override fun hasStableIds(): Boolean = true

    private fun greetingViews(row: WidgetBoardRow): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_todo_greeting_card).apply {
            setTextViewText(R.id.widget_greeting_title, row.title)
            setTextViewText(R.id.widget_greeting_quote, row.meta)
            setTextColor(R.id.widget_greeting_title, darkText)
            setTextColor(R.id.widget_greeting_quote, mutedText)
        }
    }

    private fun sectionViews(row: WidgetBoardRow): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_todo_section).apply {
            setTextViewText(R.id.widget_section_title, row.title)
            setTextViewText(R.id.widget_section_meta, row.meta)
            setTextColor(R.id.widget_section_title, darkText)
            setTextColor(R.id.widget_section_meta, mutedText)
            setViewVisibility(R.id.widget_section_meta, if (row.meta.isBlank()) View.GONE else View.VISIBLE)
        }
    }

    private fun emptyRowViews(row: WidgetBoardRow): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_todo_empty_card).apply {
            setTextViewText(R.id.widget_empty_card_title, row.title)
            setTextColor(R.id.widget_empty_card_title, row.titleColor)
        }
    }

    private fun todoViews(row: WidgetBoardRow): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_todo_task_card).apply {
            setTextViewText(R.id.widget_task_group, row.groupName)
            setTextViewText(R.id.widget_task_time, row.meta)
            setTextViewText(R.id.widget_task_title, row.title)
            setTextViewText(R.id.widget_task_notes, row.notes)
            setTextViewText(R.id.widget_task_badge, row.trailing)
            setTextColor(R.id.widget_task_group, row.accentColor)
            setTextColor(R.id.widget_task_time, row.metaColor)
            setTextColor(R.id.widget_task_title, row.titleColor)
            setTextColor(R.id.widget_task_notes, mutedText)
            setTextColor(R.id.widget_task_badge, row.trailingColor)
            setInt(R.id.widget_task_strip, "setColorFilter", row.accentColor)
            setViewVisibility(R.id.widget_task_time, if (row.meta.isBlank()) View.GONE else View.VISIBLE)
            setViewVisibility(R.id.widget_task_notes, if (row.notes.isBlank()) View.GONE else View.VISIBLE)
            setViewVisibility(R.id.widget_task_badge, if (row.trailing.isBlank()) View.GONE else View.VISIBLE)
        }
    }

    private fun scheduleViews(row: WidgetBoardRow): RemoteViews {
        val today = row.date ?: LocalDate.now()
        return RemoteViews(context.packageName, R.layout.widget_todo_schedule_card).apply {
            setTextViewText(R.id.widget_schedule_month, today.format(monthFormatter))
            setTextViewText(R.id.widget_schedule_weekday, weekdayLabel(today))
            setTextViewText(R.id.widget_schedule_day, today.dayOfMonth.toString())
            setTextViewText(R.id.widget_schedule_today_message, row.title)
            setTextViewText(R.id.widget_schedule_tomorrow_message, row.trailing)
            setTextColor(R.id.widget_schedule_month, mutedText)
            setTextColor(R.id.widget_schedule_weekday, eventBlue)
            setTextColor(R.id.widget_schedule_day, darkText)
            setTextColor(R.id.widget_schedule_today_message, if (row.highlight) darkText else mutedText)
            setTextColor(R.id.widget_schedule_tomorrow_label, mutedText)
            setTextColor(R.id.widget_schedule_tomorrow_message, if (row.trailing.isBlank()) mutedText else headerText)

            bindScheduleEvent(
                rootId = R.id.widget_schedule_today_event_1,
                stripId = R.id.widget_schedule_today_event_1_strip,
                titleId = R.id.widget_schedule_today_event_1_title,
                timeId = R.id.widget_schedule_today_event_1_time,
                locationId = R.id.widget_schedule_today_event_1_location,
                statusId = R.id.widget_schedule_today_event_1_status,
                row = row.events.getOrNull(0)
            )
            bindScheduleEvent(
                rootId = R.id.widget_schedule_today_event_2,
                stripId = R.id.widget_schedule_today_event_2_strip,
                titleId = R.id.widget_schedule_today_event_2_title,
                timeId = R.id.widget_schedule_today_event_2_time,
                locationId = R.id.widget_schedule_today_event_2_location,
                statusId = R.id.widget_schedule_today_event_2_status,
                row = row.events.getOrNull(1)
            )
            bindScheduleEvent(
                rootId = R.id.widget_schedule_tomorrow_event_1,
                stripId = R.id.widget_schedule_tomorrow_event_1_strip,
                titleId = R.id.widget_schedule_tomorrow_event_1_title,
                timeId = R.id.widget_schedule_tomorrow_event_1_time,
                locationId = R.id.widget_schedule_tomorrow_event_1_location,
                statusId = R.id.widget_schedule_tomorrow_event_1_status,
                row = row.tomorrowEvents.getOrNull(0)
            )
            bindScheduleEvent(
                rootId = R.id.widget_schedule_tomorrow_event_2,
                stripId = R.id.widget_schedule_tomorrow_event_2_strip,
                titleId = R.id.widget_schedule_tomorrow_event_2_title,
                timeId = R.id.widget_schedule_tomorrow_event_2_time,
                locationId = R.id.widget_schedule_tomorrow_event_2_location,
                statusId = R.id.widget_schedule_tomorrow_event_2_status,
                row = row.tomorrowEvents.getOrNull(1)
            )

            setViewVisibility(R.id.widget_schedule_today_message, if (row.events.isEmpty()) View.VISIBLE else View.GONE)
            setViewVisibility(R.id.widget_schedule_tomorrow_message, if (row.tomorrowEvents.isEmpty()) View.VISIBLE else View.GONE)
        }
    }

    private fun RemoteViews.bindScheduleEvent(
        rootId: Int,
        stripId: Int,
        titleId: Int,
        timeId: Int,
        locationId: Int,
        statusId: Int,
        row: WidgetBoardRow?
    ) {
        if (row == null) {
            setViewVisibility(rootId, View.GONE)
            return
        }
        setViewVisibility(rootId, View.VISIBLE)
        setInt(
            rootId,
            "setBackgroundResource",
            if (row.highlight) R.drawable.widget_schedule_event_active_background else R.drawable.widget_schedule_event_background
        )
        setTextViewText(titleId, row.title)
        setTextViewText(timeId, row.meta)
        setTextViewText(locationId, row.location)
        setTextViewText(statusId, row.checkInStatus)
        setTextColor(titleId, row.titleColor)
        setTextColor(timeId, darkText)
        setTextColor(locationId, mutedText)
        setTextColor(statusId, row.accentColor)
        setInt(stripId, "setColorFilter", row.accentColor)
        setViewVisibility(locationId, if (row.location.isBlank()) View.GONE else View.VISIBLE)
        setViewVisibility(statusId, if (row.checkInStatus.isBlank()) View.GONE else View.VISIBLE)
        setOnClickFillInIntent(rootId, row.fillInIntent())
    }

    private fun eventViews(row: WidgetBoardRow): RemoteViews {
        val date = row.date ?: LocalDate.now()
        return RemoteViews(context.packageName, R.layout.widget_todo_event_card).apply {
            setTextViewText(R.id.widget_event_month, date.format(monthFormatter))
            setTextViewText(R.id.widget_event_weekday, weekdayLabel(date))
            setTextViewText(R.id.widget_event_day, date.dayOfMonth.toString())
            setTextViewText(R.id.widget_event_day_label, row.dayLabel)
            setTextViewText(R.id.widget_event_title, row.title)
            setTextViewText(R.id.widget_event_time, row.meta)
            setTextViewText(R.id.widget_event_location, row.location)
            setTextColor(R.id.widget_event_day_label, if (row.highlight) orange else mutedText)
            setTextColor(R.id.widget_event_title, if (row.highlight) orange else darkText)
            setTextColor(R.id.widget_event_time, darkText)
            setTextColor(R.id.widget_event_location, mutedText)
            setInt(R.id.widget_event_strip, "setBackgroundColor", row.accentColor)
            setViewVisibility(R.id.widget_event_day_label, if (row.dayLabel.isBlank()) View.GONE else View.VISIBLE)
            setViewVisibility(R.id.widget_event_location, if (row.location.isBlank()) View.GONE else View.VISIBLE)
        }
    }

    private fun announcementViews(row: WidgetBoardRow): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_todo_announcement_card).apply {
            setTextViewText(R.id.widget_announcement_text, row.title)
        }
    }

    private fun buildRows(
        snapshot: DailyBoardSnapshot,
        groups: List<TaskGroup>,
        activeCheckIns: Map<Long, EventCheckIn>
    ): List<WidgetBoardRow> {
        val output = mutableListOf<WidgetBoardRow>()
        val today = snapshot.date

        snapshot.announcements.take(2).forEachIndexed { index, announcement ->
            output += WidgetBoardRow(
                stableId = -10_000L - index,
                type = WidgetRowType.ANNOUNCEMENT,
                sourceNoteId = announcement.sourceNoteId,
                title = "${announcement.rangeLabel()} · ${announcement.text}",
                titleColor = darkText
            )
        }

        output += WidgetBoardRow(
            stableId = -20_000L,
            type = WidgetRowType.GREETING,
            title = "${timeGreeting(snapshot.now.hour)}，Payki",
            meta = "今天是 ${today.format(fullDateFormatter)}，先处理最关键的一步。",
            titleColor = darkText,
            metaColor = mutedText
        )

        output += sectionRow(-1L, "今日待办（${snapshot.todoItems.size}）", highlight = true)
        if (snapshot.todoItems.isEmpty()) {
            output += emptyRow(-2L, "今天还没有安排任务。")
        } else {
            snapshot.todoItems.forEach { item ->
                val group = groups.firstOrNull { it.id == item.groupId }
                output += WidgetBoardRow(
                    stableId = item.id,
                    type = WidgetRowType.TODO,
                    itemId = item.id,
                    title = item.title,
                    groupName = group?.name ?: "默认",
                    notes = item.notes.trim(),
                    meta = todoTimeLabel(item),
                    trailing = if (item.missed) "!" else "",
                    titleColor = darkText,
                    metaColor = if (item.missed) danger else mutedText,
                    trailingColor = danger,
                    accentColor = todoAccentColor(item, groups)
                )
            }
        }

        output += sectionRow(-3L, "今日日程（${snapshot.visibleTodayEvents.size}）", highlight = true)
        output += scheduleRow(snapshot, activeCheckIns)
        return output.take(60)
    }

    private fun sectionRow(
        stableId: Long,
        title: String,
        meta: String = "",
        highlight: Boolean
    ): WidgetBoardRow {
        return WidgetBoardRow(
            stableId = stableId,
            type = WidgetRowType.SECTION,
            title = title,
            meta = meta,
            titleColor = darkText,
            metaColor = mutedText,
            highlight = highlight
        )
    }

    private fun emptyRow(stableId: Long, title: String): WidgetBoardRow {
        return WidgetBoardRow(
            stableId = stableId,
            type = WidgetRowType.EMPTY,
            title = title,
            titleColor = mutedText
        )
    }

    private fun eventRow(
        stableId: Long,
        item: TodoItem,
        date: LocalDate,
        dayLabel: String,
        inProgress: Boolean,
        activeCheckIn: EventCheckIn? = null,
        now: LocalDateTime? = null
    ): WidgetBoardRow {
        val accent = if (inProgress) orange else eventAccentColor(item)
        return WidgetBoardRow(
            stableId = stableId,
            type = WidgetRowType.EVENT,
            itemId = item.id,
            title = item.title,
            meta = DailyBoardSnapshotBuilder.eventSecondaryText(item),
            location = item.location.trim(),
            date = date,
            dayLabel = dayLabel,
            titleColor = if (inProgress) orange else darkText,
            metaColor = darkText,
            accentColor = accent,
            highlight = inProgress,
            checkInStatus = activeCheckIn
                ?.takeIf { item.checkInEnabled && inProgress }
                ?.let { "⏱ 签到中 ${formatCheckInMinutes(it, now)}" }
                .orEmpty()
        )
    }

    private fun scheduleRow(
        snapshot: DailyBoardSnapshot,
        activeCheckIns: Map<Long, EventCheckIn>
    ): WidgetBoardRow {
        val todayMessage = if (snapshot.visibleTodayEvents.isEmpty()) {
            if (snapshot.allTodayEvents.isNotEmpty()) "太棒了！今天的日程都结束了~" else "今天暂无日程"
        } else {
            ""
        }
        val tomorrowMessage = if (snapshot.tomorrowEvents.isEmpty()) "明天暂无日程 · 去规划台安排一下？" else ""
        return WidgetBoardRow(
            stableId = -4L,
            type = WidgetRowType.SCHEDULE,
            title = todayMessage,
            trailing = tomorrowMessage,
            date = snapshot.date,
            titleColor = darkText,
            metaColor = mutedText,
            highlight = snapshot.allTodayEvents.isNotEmpty() && snapshot.visibleTodayEvents.isEmpty(),
            events = snapshot.visibleTodayEvents.take(2).map { event ->
                val inProgress = DailyBoardSnapshotBuilder.eventInProgress(event, snapshot.now)
                eventRow(
                    stableId = 1_000_000L + event.id,
                    item = event,
                    date = snapshot.date,
                    dayLabel = "今天",
                    inProgress = inProgress,
                    activeCheckIn = activeCheckIns[event.id],
                    now = snapshot.now
                )
            },
            tomorrowEvents = snapshot.tomorrowEvents.take(2).map { event ->
                eventRow(
                    stableId = 2_000_000L + event.id,
                    item = event,
                    date = snapshot.date.plusDays(1),
                    dayLabel = "明天",
                    inProgress = false
                )
            }
        )
    }

    private fun todoTimeLabel(item: TodoItem): String {
        if (!item.hasDueDate) return "待办"
        val dueTime = Instant.ofEpochMilli(item.dueAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(timeFormatter)
        return "⏰ DDL $dueTime"
    }

    private fun todoAccentColor(item: TodoItem, groups: List<TaskGroup>): Int {
        val groupColor = groups.firstOrNull { it.id == item.groupId }?.colorHex
        return parseColorOrNull(groupColor) ?: todoFallback
    }

    private fun eventAccentColor(item: TodoItem): Int {
        return parseColorOrNull(item.accentColorHex)
            ?: eventBlue
    }

    private fun parseColorOrNull(value: String?): Int? {
        return value
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
    }

    private fun formatCheckInMinutes(checkIn: EventCheckIn, now: LocalDateTime?): String {
        val nowMillis = now?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis()
        val minutes = ((nowMillis - checkIn.checkInAtMillis).coerceAtLeast(0L) / 60_000L).toInt()
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours > 0 && rest > 0 -> "${hours}h${rest}m"
            hours > 0 -> "${hours}h"
            else -> "${rest}m"
        }
    }

    private fun timeGreeting(hour: Int): String {
        return when (hour) {
            in 5..10 -> "早上好"
            in 11..13 -> "中午好"
            in 14..17 -> "下午好"
            in 18..22 -> "晚上好"
            else -> "夜深了"
        }
    }

    private fun weekdayLabel(date: LocalDate): String {
        return when (date.dayOfWeek.value) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            else -> "周日"
        }
    }

    private fun WidgetBoardRow.fillInIntent(): Intent {
        return Intent().apply {
            when (this@fillInIntent.type) {
                WidgetRowType.TODO -> putExtra(MainActivity.EXTRA_OPEN_TASKS, true)
                WidgetRowType.EVENT,
                WidgetRowType.SCHEDULE -> putExtra(MainActivity.EXTRA_OPEN_CALENDAR, true)
                WidgetRowType.ANNOUNCEMENT -> putExtra(MainActivity.EXTRA_OPEN_PLANNING_NOTE_ID, sourceNoteId)
                WidgetRowType.GREETING,
                WidgetRowType.SECTION,
                WidgetRowType.EMPTY -> putExtra(MainActivity.EXTRA_OPEN_BOARD, true)
            }
        }
    }

    private fun loadWidgetColors() {
        darkText = ContextCompat.getColor(context, R.color.widget_text_primary)
        mutedText = ContextCompat.getColor(context, R.color.widget_text_muted)
        headerText = ContextCompat.getColor(context, R.color.widget_header)
        orange = ContextCompat.getColor(context, R.color.widget_accent)
        danger = ContextCompat.getColor(context, R.color.widget_danger)
        eventBlue = ContextCompat.getColor(context, R.color.widget_event_blue)
        todoFallback = ContextCompat.getColor(context, R.color.widget_todo_green)
    }

    private enum class WidgetRowType {
        GREETING,
        SECTION,
        EMPTY,
        TODO,
        SCHEDULE,
        EVENT,
        ANNOUNCEMENT
    }

    private data class WidgetBoardRow(
        val stableId: Long,
        val type: WidgetRowType,
        val itemId: Long = 0L,
        val sourceNoteId: Long = 0L,
        val title: String,
        val groupName: String = "",
        val notes: String = "",
        val meta: String = "",
        val trailing: String = "",
        val location: String = "",
        val date: LocalDate? = null,
        val dayLabel: String = "",
        val titleColor: Int = 0,
        val metaColor: Int = 0,
        val trailingColor: Int = 0,
        val accentColor: Int = 0,
        val highlight: Boolean = false,
        val checkInStatus: String = "",
        val opensCalendar: Boolean = false,
        val events: List<WidgetBoardRow> = emptyList(),
        val tomorrowEvents: List<WidgetBoardRow> = emptyList()
    )
}
