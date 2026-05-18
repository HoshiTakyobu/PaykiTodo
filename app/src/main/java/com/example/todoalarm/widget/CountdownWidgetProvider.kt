package com.example.todoalarm.widget

import android.app.AlarmManager
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
import java.time.LocalDateTime

class CountdownWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleNextMinuteTick(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_MINUTE_TICK,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                notifyWidgetDataChanged(context)
                scheduleNextMinuteTick(context)
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelMinuteTick(context)
    }

    companion object {
        private const val ACTION_MINUTE_TICK = "com.example.todoalarm.widget.COUNTDOWN_MINUTE_TICK"

        fun notifyWidgetDataChanged(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CountdownWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
            if (ids.isNotEmpty()) {
                scheduleNextMinuteTick(context)
            } else {
                cancelMinuteTick(context)
            }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val now = LocalDateTime.now()
            val (items, groups) = loadCountdownData(context)
            val targets = items
                .filter { item -> DailyBoardSnapshotBuilder.countdownTargetMillis(item)?.let { it >= System.currentTimeMillis() } == true }
                .sortedBy { DailyBoardSnapshotBuilder.countdownTargetMillis(it) ?: Long.MAX_VALUE }
                .take(3)
            val views = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
                if (targets.isEmpty()) {
                    setViewVisibility(R.id.widget_countdown_empty, View.VISIBLE)
                } else {
                    setViewVisibility(R.id.widget_countdown_empty, View.GONE)
                }
                bindCountdownRow(context, appWidgetId, 0, targets.getOrNull(0), groups, now)
                bindCountdownRow(context, appWidgetId, 1, targets.getOrNull(1), groups, now)
                bindCountdownRow(context, appWidgetId, 2, targets.getOrNull(2), groups, now)
                setOnClickPendingIntent(
                    R.id.widget_countdown_root,
                    openBoardIntent(context, appWidgetId, requestSalt = 90_000)
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
            now: LocalDateTime
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
            val remaining = DailyBoardSnapshotBuilder.countdownRemainingDisplay(item, now)
            if (remaining == null) {
                setViewVisibility(rowId, View.GONE)
                return
            }
            val accent = countdownAccentColor(context, item, groups)
            setViewVisibility(rowId, View.VISIBLE)
            setViewVisibility(checkId, if (item.isTodo) View.VISIBLE else View.GONE)
            setTextViewText(daysId, remaining.primary)
            setTextViewText(remainingId, remaining.secondary)
            setTextViewText(titleId, item.title)
            setTextViewText(metaId, "")
            setTextColor(daysId, accent)
            setTextColor(remainingId, ContextCompat.getColor(context, R.color.widget_text_muted))
            setTextColor(titleId, ContextCompat.getColor(context, R.color.widget_text_primary))
            setTextColor(metaId, ContextCompat.getColor(context, R.color.widget_text_muted))
            setInt(stripId, "setColorFilter", accent)
            setViewVisibility(remainingId, if (remaining.secondary.isBlank()) View.GONE else View.VISIBLE)
            setViewVisibility(metaId, View.GONE)
            setOnClickPendingIntent(
                rowId,
                openItemIntent(context, appWidgetId, item, requestSalt = index)
            )
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

        private fun openItemIntent(
            context: Context,
            appWidgetId: Int,
            item: TodoItem,
            requestSalt: Int
        ): PendingIntent {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
            return PendingIntent.getActivity(
                context,
                appWidgetId * 100 + requestSalt,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    if (item.isEvent) {
                        putExtra(MainActivity.EXTRA_OPEN_EVENT_ID, item.id)
                    } else {
                        putExtra(MainActivity.EXTRA_OPEN_TODO_ID, item.id)
                    }
                },
                flags
            )
        }

        private fun openBoardIntent(
            context: Context,
            appWidgetId: Int,
            requestSalt: Int
        ): PendingIntent {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
            return PendingIntent.getActivity(
                context,
                appWidgetId * 100 + requestSalt,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(MainActivity.EXTRA_OPEN_BOARD, true)
                },
                flags
            )
        }

        private fun scheduleNextMinuteTick(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CountdownWidgetProvider::class.java))
            if (ids.isEmpty()) {
                cancelMinuteTick(context)
                return
            }
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            val pendingIntent = minuteTickPendingIntent(context)
            val now = System.currentTimeMillis()
            val nextMinute = now - (now % 60_000L) + 60_000L
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMinute, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextMinute, pendingIntent)
                    }
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, nextMinute, pendingIntent)
                }
            } catch (_: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextMinute, pendingIntent)
            }
        }

        private fun cancelMinuteTick(context: Context) {
            context.getSystemService(AlarmManager::class.java)?.cancel(minuteTickPendingIntent(context))
        }

        private fun minuteTickPendingIntent(context: Context): PendingIntent {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
            return PendingIntent.getBroadcast(
                context,
                91_001,
                Intent(context, CountdownWidgetProvider::class.java).apply {
                    action = ACTION_MINUTE_TICK
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
