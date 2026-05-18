package com.example.todoalarm.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.todoalarm.R
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.ui.FocusActivity
import com.example.todoalarm.ui.MainActivity
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class FocusWidgetProvider : AppWidgetProvider() {
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
        fun notifyWidgetDataChanged(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FocusWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val app = context.applicationContext as TodoApplication
            val settings = app.settingsStore.currentSettings()
            val stats = runBlocking { app.repository.getTodayFocusSessionStats(LocalDate.now()) }
            val views = RemoteViews(context.packageName, R.layout.widget_focus).apply {
                setTextViewText(R.id.widget_focus_subtitle, "今日 ${stats.completedMinutes} 分钟 · ${stats.totalCount} 次专注")
                setTextViewText(R.id.widget_focus_minutes, settings.focusDefaultMinutes.toString())
                setOnClickPendingIntent(R.id.widget_focus_root, openFocusPageIntent(context, appWidgetId))
                setOnClickPendingIntent(R.id.widget_focus_start, startFreeFocusIntent(context, appWidgetId, settings.focusDefaultMinutes))
            }
            manager.updateAppWidget(appWidgetId, views)
        }

        private fun openFocusPageIntent(context: Context, appWidgetId: Int): PendingIntent {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
            return PendingIntent.getActivity(
                context,
                appWidgetId * 100 + 71_000,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(MainActivity.EXTRA_OPEN_FOCUS, true)
                },
                flags
            )
        }

        private fun startFreeFocusIntent(context: Context, appWidgetId: Int, minutes: Int): PendingIntent {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
            return PendingIntent.getActivity(
                context,
                appWidgetId * 100 + 71_001,
                FocusActivity.createIntent(
                    context = context,
                    todoId = null,
                    title = "自由专注",
                    minutes = minutes
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
                flags
            )
        }
    }
}
