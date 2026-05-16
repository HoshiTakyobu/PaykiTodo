package com.example.todoalarm.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.todoalarm.R
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.DailyBoardSnapshot
import com.example.todoalarm.data.DailyBoardSnapshotBuilder
import com.example.todoalarm.data.TodoItem
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodoWidgetFactory(applicationContext)
    }
}

private class TodoWidgetFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {
    private var rows: List<WidgetBoardRow> = emptyList()
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val app = context.applicationContext as TodoApplication
        rows = runBlocking {
            val items = app.repository.getAllTodos()
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
        val row = rows.getOrNull(position) ?: return RemoteViews(context.packageName, R.layout.widget_todo_item)
        return RemoteViews(context.packageName, R.layout.widget_todo_item).apply {
            setTextViewText(R.id.widget_item_time, row.leading)
            setTextViewText(R.id.widget_item_title, row.title)
            setTextViewText(R.id.widget_item_dot, row.trailing)
            setTextColor(R.id.widget_item_time, row.leadingColor)
            setTextColor(R.id.widget_item_title, row.titleColor)
            setTextColor(R.id.widget_item_dot, row.trailingColor)
            setViewVisibility(R.id.widget_item_time, if (row.leading.isBlank()) View.GONE else View.VISIBLE)
            setViewVisibility(R.id.widget_item_dot, if (row.trailing.isBlank()) View.GONE else View.VISIBLE)
            setOnClickFillInIntent(R.id.widget_item_root, Intent())
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = rows.getOrNull(position)?.stableId ?: position.toLong()
    override fun hasStableIds(): Boolean = true

    private fun buildRows(snapshot: DailyBoardSnapshot): List<WidgetBoardRow> {
        val output = mutableListOf<WidgetBoardRow>()
        snapshot.announcements.take(4).forEachIndexed { index, announcement ->
            output += WidgetBoardRow(
                stableId = -10_000L - index,
                leading = "公告",
                title = "${announcement.rangeLabel()} · ${announcement.text}",
                trailing = "",
                leadingColor = Orange,
                titleColor = DarkText
            )
        }

        output += WidgetBoardRow.header(-1L, "今日待办（${snapshot.todoItems.size}）")
        if (snapshot.todoItems.isEmpty()) {
            output += WidgetBoardRow.empty(-2L, "今天还没有安排任务。")
        } else {
            snapshot.todoItems.forEach { item ->
                output += WidgetBoardRow(
                    stableId = item.id,
                    leading = todoTimeLabel(item),
                    title = item.title,
                    trailing = if (item.missed) "!" else "○",
                    leadingColor = if (item.missed) Danger else MutedText,
                    titleColor = DarkText,
                    trailingColor = if (item.missed) Danger else Orange
                )
            }
        }

        output += WidgetBoardRow.header(-3L, "今日日程（${snapshot.allTodayEvents.size}）")
        if (snapshot.visibleTodayEvents.isEmpty()) {
            output += WidgetBoardRow.empty(
                stableId = -4L,
                title = if (snapshot.allTodayEvents.isNotEmpty()) "太棒了！今天的日程都结束了~" else "今天暂无日程"
            )
        } else {
            snapshot.visibleTodayEvents.forEach { event ->
                output += WidgetBoardRow(
                    stableId = 1_000_000L + event.id,
                    leading = DailyBoardSnapshotBuilder.eventSecondaryText(event),
                    title = event.title,
                    trailing = if (DailyBoardSnapshotBuilder.eventInProgress(event, snapshot.now)) "●" else "",
                    leadingColor = MutedText,
                    titleColor = DarkText,
                    trailingColor = Orange
                )
            }
        }

        output += WidgetBoardRow.header(-5L, "明天")
        if (snapshot.tomorrowEvents.isEmpty()) {
            output += WidgetBoardRow.empty(-6L, "明天暂无日程")
        } else {
            snapshot.tomorrowEvents.forEach { event ->
                output += WidgetBoardRow(
                    stableId = 2_000_000L + event.id,
                    leading = DailyBoardSnapshotBuilder.eventSecondaryText(event),
                    title = event.title,
                    trailing = "",
                    leadingColor = MutedText,
                    titleColor = DarkText
                )
            }
        }
        return output.take(40)
    }

    private fun todoTimeLabel(item: TodoItem): String {
        if (!item.hasDueDate) return "待办"
        return Instant.ofEpochMilli(item.dueAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(timeFormatter)
    }

    private data class WidgetBoardRow(
        val stableId: Long,
        val leading: String,
        val title: String,
        val trailing: String,
        val leadingColor: Int = MutedText,
        val titleColor: Int = DarkText,
        val trailingColor: Int = Orange
    ) {
        companion object {
            fun header(stableId: Long, title: String): WidgetBoardRow {
                return WidgetBoardRow(
                    stableId = stableId,
                    leading = "",
                    title = title,
                    trailing = "",
                    titleColor = HeaderText
                )
            }

            fun empty(stableId: Long, title: String): WidgetBoardRow {
                return WidgetBoardRow(
                    stableId = stableId,
                    leading = "",
                    title = title,
                    trailing = "",
                    titleColor = MutedText
                )
            }
        }
    }

    companion object {
        private val DarkText = Color.rgb(42, 32, 21)
        private val MutedText = Color.rgb(123, 90, 50)
        private val HeaderText = Color.rgb(92, 58, 14)
        private val Orange = Color.rgb(255, 179, 71)
        private val Danger = Color.rgb(202, 67, 67)
    }
}
