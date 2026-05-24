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
import com.example.todoalarm.data.DailyBoardSnapshotBuilder
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.MainActivity
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class CountdownWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CountdownWidgetFactory(applicationContext)
    }
}

private class CountdownWidgetFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {
    private var rows: List<CountdownWidgetRow> = emptyList()
    private var groups: List<TaskGroup> = emptyList()
    private var primaryText: Int = 0
    private var mutedText: Int = 0
    private var eventBlue: Int = 0
    private var todoGreen: Int = 0

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        loadColors()
        val app = context.applicationContext as TodoApplication
        val now = LocalDateTime.now()
        val nowMillis = System.currentTimeMillis()
        val loaded = runBlocking {
            val items = app.repository.getActiveItemsForBoardRange()
            groups = app.repository.getAllGroups()
            DailyBoardSnapshotBuilder.build(items = items, now = now)
                .countdownItems
                .filter { item -> DailyBoardSnapshotBuilder.countdownTargetMillis(item)?.let { it >= nowMillis } == true }
                .take(60)
        }
        rows = loaded.map { item ->
            CountdownWidgetRow(
                item = item,
                accentColor = countdownAccentColor(item),
                remainingPrimary = DailyBoardSnapshotBuilder.countdownRemainingDisplay(item, now)?.primary.orEmpty(),
                remainingSecondary = DailyBoardSnapshotBuilder.countdownRemainingDisplay(item, now)?.secondary.orEmpty(),
                meta = countdownMetaText(item)
            )
        }.filter { it.remainingPrimary.isNotBlank() }
    }

    override fun onDestroy() {
        rows = emptyList()
        groups = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = rows.getOrNull(position)
            ?: return RemoteViews(context.packageName, R.layout.widget_countdown_item)
        val item = row.item
        return RemoteViews(context.packageName, R.layout.widget_countdown_item).apply {
            setTextViewText(R.id.widget_countdown_primary, row.remainingPrimary)
            setTextViewText(R.id.widget_countdown_secondary, row.remainingSecondary)
            setTextViewText(R.id.widget_countdown_title, item.title)
            setTextViewText(R.id.widget_countdown_meta, row.meta)
            setTextColor(R.id.widget_countdown_primary, row.accentColor)
            setTextColor(R.id.widget_countdown_secondary, mutedText)
            setTextColor(R.id.widget_countdown_title, primaryText)
            setTextColor(R.id.widget_countdown_meta, mutedText)
            setInt(R.id.widget_countdown_strip, "setColorFilter", row.accentColor)
            setViewVisibility(R.id.widget_countdown_secondary, if (row.remainingSecondary.isBlank()) View.GONE else View.VISIBLE)
            setViewVisibility(R.id.widget_countdown_meta, if (row.meta.isBlank()) View.GONE else View.VISIBLE)
            setOnClickFillInIntent(R.id.widget_countdown_item_root, fillInIntent(item))
        }
    }

    override fun getLoadingView(): RemoteViews {
        if (mutedText == 0) loadColors()
        return RemoteViews(context.packageName, R.layout.widget_todo_empty_card).apply {
            setTextViewText(R.id.widget_empty_card_title, "⏳ 加载中…")
            setTextColor(R.id.widget_empty_card_title, mutedText)
        }
    }
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = rows.getOrNull(position)?.item?.id ?: position.toLong()
    override fun hasStableIds(): Boolean = true

    private fun fillInIntent(item: TodoItem): Intent {
        return Intent().apply {
            if (item.isEvent) {
                putExtra(MainActivity.EXTRA_OPEN_EVENT_ID, item.id)
            } else {
                putExtra(MainActivity.EXTRA_OPEN_TODO_ID, item.id)
            }
        }
    }

    private fun countdownAccentColor(item: TodoItem): Int {
        val color = if (item.isEvent) {
            item.accentColorHex
        } else {
            groups.firstOrNull { it.id == item.groupId }?.colorHex
        }
        return parseColorOrNull(color)
            ?: if (item.isEvent) eventBlue else todoGreen
    }

    private fun countdownMetaText(item: TodoItem): String {
        return if (item.isEvent) {
            val start = item.startAtMillis?.toLocalDateTime() ?: item.dueAtMillis.toLocalDateTime()
            val end = item.endAtMillis?.toLocalDateTime()
            when {
                item.allDay && end != null && end.toLocalDate().minusDays(1).isAfter(start.toLocalDate()) ->
                    "${start.format(MonthDayFormatter)}-${end.toLocalDate().minusDays(1).format(MonthDayFormatter)} 全天"
                item.allDay -> "${start.format(MonthDayFormatter)} 全天"
                end != null && end.toLocalDate() != start.toLocalDate() ->
                    "${start.format(CountdownDateTimeFormatter)}-${end.format(CountdownDateTimeFormatter)}"
                end != null -> "${start.format(CountdownDateTimeFormatter)}-${end.format(TimeFormatter)}"
                else -> start.format(CountdownDateTimeFormatter)
            }
        } else {
            val groupName = groups.firstOrNull { it.id == item.groupId }?.name ?: "默认"
            "DDL ${item.dueAtMillis.toLocalDateTime().format(CountdownDateTimeFormatter)} · $groupName"
        }
    }

    private fun Long.toLocalDateTime(): LocalDateTime {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    private fun parseColorOrNull(value: String?): Int? {
        return value
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
    }

    private fun loadColors() {
        primaryText = ContextCompat.getColor(context, R.color.widget_text_primary)
        mutedText = ContextCompat.getColor(context, R.color.widget_text_muted)
        eventBlue = ContextCompat.getColor(context, R.color.widget_event_blue)
        todoGreen = ContextCompat.getColor(context, R.color.widget_todo_green)
    }

    private data class CountdownWidgetRow(
        val item: TodoItem,
        val accentColor: Int,
        val remainingPrimary: String,
        val remainingSecondary: String,
        val meta: String
    )

    private val MonthDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
    private val CountdownDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)
    private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
}
