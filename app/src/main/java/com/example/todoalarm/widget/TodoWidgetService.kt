package com.example.todoalarm.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.todoalarm.R
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.TodoItem
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
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
    private var todos: List<TodoItem> = emptyList()
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val app = context.applicationContext as TodoApplication
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        todos = runBlocking {
            app.repository.getAllTodos()
                .asSequence()
                .filter { item ->
                    item.isTodo &&
                        item.isActive &&
                        item.hasDueDate &&
                        item.dueAtMillis in start until end
                }
                .sortedBy { it.dueAtMillis }
                .take(5)
                .toList()
        }
    }

    override fun onDestroy() {
        todos = emptyList()
    }

    override fun getCount(): Int = todos.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = todos.getOrNull(position) ?: return RemoteViews(context.packageName, R.layout.widget_todo_item)
        val time = Instant.ofEpochMilli(item.dueAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(timeFormatter)
        return RemoteViews(context.packageName, R.layout.widget_todo_item).apply {
            setTextViewText(R.id.widget_item_time, time)
            setTextViewText(R.id.widget_item_title, item.title)
            setTextViewText(R.id.widget_item_dot, if (item.completed) "●" else "○")
            setOnClickFillInIntent(R.id.widget_item_root, Intent())
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = todos.getOrNull(position)?.id ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}
