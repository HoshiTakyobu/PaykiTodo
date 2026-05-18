package com.example.todoalarm.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.example.todoalarm.R
import com.example.todoalarm.ui.MainActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class TodoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
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
            val ids = manager.getAppWidgetIds(ComponentName(context, TodoWidgetProvider::class.java))
            ids.forEach { id ->
                updateWidget(context, manager, id)
                manager.notifyAppWidgetViewDataChanged(id, R.id.widget_list)
            }
            CountdownWidgetProvider.notifyWidgetDataChanged(context)
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val openAppIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_OPEN_BOARD, true)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val templateFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val rowTemplateIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 10_000,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                templateFlags
            )
            val today = LocalDate.now()
            val serviceIntent = Intent(context, TodoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_WIDGET_DAY, today.toString())
                data = Uri.parse("paykitodo://widget/$appWidgetId/${today}")
            }
            val todayLabel = today.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA))
            val views = RemoteViews(context.packageName, R.layout.widget_todo).apply {
                setTextViewText(R.id.widget_board_subtitle, todayLabel)
                setRemoteAdapter(R.id.widget_list, serviceIntent)
                setEmptyView(R.id.widget_list, R.id.widget_empty)
                setOnClickPendingIntent(R.id.widget_root, openAppIntent)
                setPendingIntentTemplate(R.id.widget_list, rowTemplateIntent)
            }
            manager.updateAppWidget(appWidgetId, views)
        }

        private const val EXTRA_WIDGET_DAY = "paykitodo_widget_day"
    }
}
