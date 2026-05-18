package com.example.todoalarm.widget

import android.app.AlarmManager
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

class CountdownWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_countdown_list)
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
            ids.forEach { id ->
                updateWidget(context, manager, id)
                manager.notifyAppWidgetViewDataChanged(id, R.id.widget_countdown_list)
            }
            if (ids.isNotEmpty()) {
                scheduleNextMinuteTick(context)
            } else {
                cancelMinuteTick(context)
            }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val openBoardIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 80_000,
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
                appWidgetId + 81_000,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                templateFlags
            )
            val minuteKey = System.currentTimeMillis() / 60_000L
            val serviceIntent = Intent(context, CountdownWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("paykitodo://countdown-widget/$appWidgetId/$minuteKey")
            }
            val views = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
                setRemoteAdapter(R.id.widget_countdown_list, serviceIntent)
                setEmptyView(R.id.widget_countdown_list, R.id.widget_countdown_empty)
                setOnClickPendingIntent(R.id.widget_countdown_root, openBoardIntent)
                setPendingIntentTemplate(R.id.widget_countdown_list, rowTemplateIntent)
            }
            manager.updateAppWidget(appWidgetId, views)
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
    }
}
