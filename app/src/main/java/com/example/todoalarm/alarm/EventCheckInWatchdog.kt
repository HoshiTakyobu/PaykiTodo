package com.example.todoalarm.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.todoalarm.R
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.ui.MainActivity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object EventCheckInWatchdog {
    private const val CHANNEL_ID = "event_checkin_auto_checkout"
    private const val NOTIFICATION_BASE_ID = 53000
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)

    fun runOnce(context: Context) {
        val app = context.applicationContext as? TodoApplication ?: return
        appScopeRun(context) {
            val settings = app.settingsStore.currentSettings()
            val thresholdHours = settings.eventCheckInIdleAutoCheckOutHours
            if (thresholdHours <= 0) return@appScopeRun
            val nowMillis = System.currentTimeMillis()
            val thresholdMillis = thresholdHours * 60L * 60L * 1000L
            val activeCheckIns = app.repository.getAllActiveEventCheckIns()
            if (activeCheckIns.isEmpty()) return@appScopeRun
            ensureChannel(context)
            activeCheckIns.forEach { checkIn ->
                val event = app.repository.getTodo(checkIn.eventId)
                val checkoutMillis = when {
                    event == null -> checkIn.checkInAtMillis + thresholdMillis
                    event.endAtMillis != null && nowMillis - event.endAtMillis >= thresholdMillis -> event.endAtMillis
                    else -> null
                } ?: return@forEach
                val checkedOut = app.repository.checkOutEventCheckIn(checkIn, checkoutMillis)
                val title = event?.title ?: "已删除日程"
                val content = buildString {
                    append("“")
                    append(title)
                    append("” 在 ")
                    append(Instant.ofEpochMilli(checkoutMillis).atZone(ZoneId.systemDefault()).toLocalDateTime().format(formatter))
                    append(" 自动签退，本次投入 ")
                    append(checkedOut.durationMinutes)
                    append(" 分钟")
                }
                notifyAutoCheckout(context, checkIn.eventId, title, content)
            }
        }
    }

    private fun notifyAutoCheckout(context: Context, eventId: Long, title: String, content: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_EVENT_ID, eventId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_BASE_ID + eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_payki_todo)
            .setContentTitle("已自动签退")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_BASE_ID + eventId.toInt(), notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "PaykiTodo 自动签退",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "忘记签退时的自动兜底提醒"
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    private fun appScopeRun(context: Context, block: suspend () -> Unit) {
        val app = context.applicationContext as? TodoApplication ?: return
        app.applicationScope.launch {
            block()
        }
    }

    private fun immutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }
}
