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
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.MainActivity
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
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

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        loadWidgetColors()
        val app = context.applicationContext as TodoApplication
        rows = runBlocking {
            val items = app.repository.getActiveItemsForBoardRange()
            val notes = app.repository.getAllPlanningNotes()
            val snapshot = DailyBoardSnapshotBuilder.build(items = items, planningNotes = notes)
            buildRows(snapshot)
        }
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = rows.getOrNull(position) ?: return emptyRowViews(emptyRow(-1L, "今日看板暂无内容"))
        return when (row.type) {
            WidgetRowType.SECTION -> sectionViews(row)
            WidgetRowType.EMPTY -> emptyRowViews(row)
            WidgetRowType.TODO -> todoViews(row)
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

    private fun sectionViews(row: WidgetBoardRow): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_todo_section).apply {
            setTextViewText(R.id.widget_section_title, row.title)
            setTextViewText(R.id.widget_section_meta, row.meta)
            setTextColor(R.id.widget_section_title, if (row.highlight) headerText else darkText)
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
            setTextViewText(R.id.widget_task_time, row.meta)
            setTextViewText(R.id.widget_task_title, row.title)
            setTextViewText(R.id.widget_task_badge, row.trailing)
            setTextColor(R.id.widget_task_time, row.metaColor)
            setTextColor(R.id.widget_task_title, row.titleColor)
            setTextColor(R.id.widget_task_badge, row.trailingColor)
            setViewVisibility(R.id.widget_task_time, if (row.meta.isBlank()) View.GONE else View.VISIBLE)
            setViewVisibility(R.id.widget_task_badge, if (row.trailing.isBlank()) View.GONE else View.VISIBLE)
        }
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

    private fun buildRows(snapshot: DailyBoardSnapshot): List<WidgetBoardRow> {
        val output = mutableListOf<WidgetBoardRow>()
        val today = snapshot.date
        val tomorrow = today.plusDays(1)

        output += sectionRow(
            stableId = -20_000L,
            title = "今日看板",
            meta = today.format(fullDateFormatter),
            highlight = false
        )

        snapshot.announcements.take(2).forEachIndexed { index, announcement ->
            output += WidgetBoardRow(
                stableId = -10_000L - index,
                type = WidgetRowType.ANNOUNCEMENT,
                sourceNoteId = announcement.sourceNoteId,
                title = "${announcement.rangeLabel()} · ${announcement.text}",
                titleColor = darkText
            )
        }

        output += sectionRow(-1L, "今日待办（${snapshot.todoItems.size}）", highlight = true)
        if (snapshot.todoItems.isEmpty()) {
            output += emptyRow(-2L, "今天还没有安排任务。")
        } else {
            snapshot.todoItems.take(6).forEach { item ->
                output += WidgetBoardRow(
                    stableId = item.id,
                    type = WidgetRowType.TODO,
                    itemId = item.id,
                    title = item.title,
                    meta = todoTimeLabel(item),
                    trailing = if (item.missed) "!" else "",
                    titleColor = darkText,
                    metaColor = if (item.missed) danger else mutedText,
                    trailingColor = danger
                )
            }
        }

        output += sectionRow(-3L, "今日日程（${snapshot.allTodayEvents.size}）", highlight = true)
        if (snapshot.visibleTodayEvents.isEmpty()) {
            output += emptyRow(
                stableId = -4L,
                title = if (snapshot.allTodayEvents.isNotEmpty()) "太棒了！今天的日程都结束了~" else "今天暂无日程"
            )
        } else {
            snapshot.visibleTodayEvents.take(4).forEach { event ->
                output += eventRow(
                    stableId = 1_000_000L + event.id,
                    item = event,
                    date = today,
                    dayLabel = "今天",
                    inProgress = DailyBoardSnapshotBuilder.eventInProgress(event, snapshot.now)
                )
            }
        }

        output += sectionRow(-5L, "明天", highlight = true)
        if (snapshot.tomorrowEvents.isEmpty()) {
            output += emptyRow(-6L, "明天暂无日程")
        } else {
            snapshot.tomorrowEvents.take(4).forEach { event ->
                output += eventRow(
                    stableId = 2_000_000L + event.id,
                    item = event,
                    date = tomorrow,
                    dayLabel = "明天",
                    inProgress = false
                )
            }
        }
        return output.take(40)
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
            titleColor = if (highlight) headerText else darkText,
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
        inProgress: Boolean
    ): WidgetBoardRow {
        val accent = if (inProgress) orange else eventAccentColor(item)
        return WidgetBoardRow(
            stableId = stableId,
            type = WidgetRowType.EVENT,
            itemId = item.id,
            title = item.title,
            meta = DailyBoardSnapshotBuilder.eventSecondaryText(item),
            location = item.location.takeIf { it.isNotBlank() }?.let { "@$it" }.orEmpty(),
            date = date,
            dayLabel = dayLabel,
            titleColor = if (inProgress) orange else darkText,
            metaColor = darkText,
            accentColor = accent,
            highlight = inProgress
        )
    }

    private fun todoTimeLabel(item: TodoItem): String {
        if (!item.hasDueDate) return "待办"
        return Instant.ofEpochMilli(item.dueAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(timeFormatter)
    }

    private fun eventAccentColor(item: TodoItem): Int {
        return item.accentColorHex
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
            ?: eventBlue
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
                WidgetRowType.TODO -> putExtra(MainActivity.EXTRA_OPEN_TODO_ID, itemId)
                WidgetRowType.EVENT -> putExtra(MainActivity.EXTRA_OPEN_EVENT_ID, itemId)
                WidgetRowType.ANNOUNCEMENT -> putExtra(MainActivity.EXTRA_OPEN_PLANNING_NOTE_ID, sourceNoteId)
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
    }

    private enum class WidgetRowType {
        SECTION,
        EMPTY,
        TODO,
        EVENT,
        ANNOUNCEMENT
    }

    private data class WidgetBoardRow(
        val stableId: Long,
        val type: WidgetRowType,
        val itemId: Long = 0L,
        val sourceNoteId: Long = 0L,
        val title: String,
        val meta: String = "",
        val trailing: String = "",
        val location: String = "",
        val date: LocalDate? = null,
        val dayLabel: String = "",
        val titleColor: Int = 0,
        val metaColor: Int = 0,
        val trailingColor: Int = 0,
        val accentColor: Int = 0,
        val highlight: Boolean = false
    )
}
