package com.example.todoalarm.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.todoalarm.R
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.DailyBoardSnapshotBuilder
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.MainActivity
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class CountdownWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> notifyWidgetDataChanged(context)
        }
    }

    companion object {
        private val MonthDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
        private val MonthDayTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)
        private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)

        fun notifyWidgetDataChanged(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CountdownWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val today = LocalDate.now()
            val (items, groups) = loadCountdownData(context)
            val targets = items
                .filter { item -> DailyBoardSnapshotBuilder.countdownTargetDate(item)?.let { !it.isBefore(today) } == true }
                .sortedBy { DailyBoardSnapshotBuilder.countdownTargetMillis(it) ?: Long.MAX_VALUE }
                .take(3)
            val views = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
                setTextViewText(R.id.widget_countdown_subtitle, "今天是 ${today.format(MonthDayFormatter)}")
                setTextViewText(R.id.widget_countdown_count, "${targets.size} 项")
                if (targets.isEmpty()) {
                    setViewVisibility(R.id.widget_countdown_empty, View.VISIBLE)
                } else {
                    setViewVisibility(R.id.widget_countdown_empty, View.GONE)
                }
                bindCountdownRow(context, appWidgetId, 0, targets.getOrNull(0), groups, today)
                bindCountdownRow(context, appWidgetId, 1, targets.getOrNull(1), groups, today)
                bindCountdownRow(context, appWidgetId, 2, targets.getOrNull(2), groups, today)
                setOnClickPendingIntent(
                    R.id.widget_countdown_root,
                    openIntent(context, appWidgetId, openCalendar = false, requestSalt = 90_000)
                )
            }
            manager.updateAppWidget(appWidgetId, views)
        }

        private fun loadCountdownData(context: Context): Pair<List<TodoItem>, List<TaskGroup>> {
            val app = context.applicationContext as TodoApplication
            return runBlocking {
                val items = app.repository.getActiveItemsForBoardRange()
                val groups = app.repository.getAllGroups()
                val snapshot = DailyBoardSnapshotBuilder.build(items = items)
                snapshot.countdownItems to groups
            }
        }

        private fun RemoteViews.bindCountdownRow(
            context: Context,
            appWidgetId: Int,
            index: Int,
            item: TodoItem?,
            groups: List<TaskGroup>,
            today: LocalDate
        ) {
            val rowId = RowIds[index]
            val stripId = StripIds[index]
            val checkId = CheckIds[index]
            val daysId = DaysIds[index]
            val remainingId = RemainingIds[index]
            val titleId = TitleIds[index]
            val metaId = MetaIds[index]
            if (item == null) {
                setViewVisibility(rowId, View.GONE)
                return
            }
            val targetDate = DailyBoardSnapshotBuilder.countdownTargetDate(item)
            val days = DailyBoardSnapshotBuilder.countdownDays(item, today)
            if (targetDate == null || days == null) {
                setViewVisibility(rowId, View.GONE)
                return
            }
            val accent = countdownAccentColor(context, item, groups)
            setViewVisibility(rowId, View.VISIBLE)
            setViewVisibility(checkId, if (item.isTodo) View.VISIBLE else View.GONE)
            setTextViewText(daysId, "${days.coerceAtLeast(0)}d")
            setTextViewText(remainingId, remainingCountdownText(item))
            setTextViewText(titleId, item.title)
            setTextViewText(metaId, countdownMetaText(item))
            setTextColor(daysId, accent)
            setTextColor(remainingId, ContextCompat.getColor(context, R.color.widget_text_muted))
            setTextColor(titleId, ContextCompat.getColor(context, R.color.widget_text_primary))
            setTextColor(metaId, ContextCompat.getColor(context, R.color.widget_text_muted))
            setInt(stripId, "setColorFilter", accent)
            setOnClickPendingIntent(
                rowId,
                openIntent(context, appWidgetId, openCalendar = item.isEvent, requestSalt = index)
            )
        }

        private fun remainingCountdownText(item: TodoItem): String {
            val targetMillis = DailyBoardSnapshotBuilder.countdownTargetMillis(item) ?: return "--"
            val remaining = Duration.ofMillis((targetMillis - System.currentTimeMillis()).coerceAtLeast(0L))
            val hours = remaining.toHours()
            val minutes = remaining.minusHours(hours).toMinutes()
            val seconds = remaining.minusHours(hours).minusMinutes(minutes).seconds
            return "${hours}h ${minutes}m ${seconds}s"
        }

        private fun countdownMetaText(item: TodoItem): String {
            return if (item.isEvent) {
                val start = item.startAtMillis?.toLocalDateTime() ?: item.dueAtMillis.toLocalDateTime()
                val end = item.endAtMillis?.toLocalDateTime()
                val time = when {
                    item.allDay && end != null && end.toLocalDate().minusDays(1).isAfter(start.toLocalDate()) ->
                        "${start.format(MonthDayFormatter)}-${end.toLocalDate().minusDays(1).format(MonthDayFormatter)} 全天"
                    item.allDay -> "${start.format(MonthDayFormatter)} 全天"
                    end != null && end.toLocalDate() != start.toLocalDate() ->
                        "${start.format(MonthDayTimeFormatter)}-${end.format(MonthDayTimeFormatter)}"
                    end != null -> "${start.format(MonthDayTimeFormatter)}-${end.format(TimeFormatter)}"
                    else -> start.format(MonthDayTimeFormatter)
                }
                "日程 · $time"
            } else {
                "待办 · DDL ${item.dueAtMillis.toLocalDateTime().format(MonthDayTimeFormatter)}"
            }
        }

        private fun Long.toLocalDateTime(): LocalDateTime {
            return Instant.ofEpochMilli(this)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
        }

        private fun countdownAccentColor(context: Context, item: TodoItem, groups: List<TaskGroup>): Int {
            val color = if (item.isEvent) {
                item.accentColorHex
            } else {
                groups.firstOrNull { it.id == item.groupId }?.colorHex
            }
            return color
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
                ?: ContextCompat.getColor(context, if (item.isEvent) R.color.widget_event_blue else R.color.widget_todo_green)
        }

        private fun openIntent(
            context: Context,
            appWidgetId: Int,
            openCalendar: Boolean,
            requestSalt: Int
        ): PendingIntent {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
            return PendingIntent.getActivity(
                context,
                appWidgetId * 100 + requestSalt,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    if (openCalendar) {
                        putExtra(MainActivity.EXTRA_OPEN_CALENDAR, true)
                    } else {
                        putExtra(MainActivity.EXTRA_OPEN_TASKS, true)
                    }
                },
                flags
            )
        }

        private val RowIds = intArrayOf(
            R.id.widget_countdown_row_1,
            R.id.widget_countdown_row_2,
            R.id.widget_countdown_row_3
        )
        private val StripIds = intArrayOf(
            R.id.widget_countdown_strip_1,
            R.id.widget_countdown_strip_2,
            R.id.widget_countdown_strip_3
        )
        private val CheckIds = intArrayOf(
            R.id.widget_countdown_check_1,
            R.id.widget_countdown_check_2,
            R.id.widget_countdown_check_3
        )
        private val DaysIds = intArrayOf(
            R.id.widget_countdown_days_1,
            R.id.widget_countdown_days_2,
            R.id.widget_countdown_days_3
        )
        private val RemainingIds = intArrayOf(
            R.id.widget_countdown_remaining_1,
            R.id.widget_countdown_remaining_2,
            R.id.widget_countdown_remaining_3
        )
        private val TitleIds = intArrayOf(
            R.id.widget_countdown_title_1,
            R.id.widget_countdown_title_2,
            R.id.widget_countdown_title_3
        )
        private val MetaIds = intArrayOf(
            R.id.widget_countdown_meta_1,
            R.id.widget_countdown_meta_2,
            R.id.widget_countdown_meta_3
        )
    }
}
