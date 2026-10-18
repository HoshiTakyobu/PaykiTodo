package com.example.todoalarm.sync

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.todoalarm.R
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.ui.MainActivity

object AccountExpiryNotifier {
    private const val ID = 19001
    private const val CHANNEL = "account_session"
    private fun alarm(context: Context) = PendingIntent.getBroadcast(context, ID,
        Intent(context, AccountExpiryReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun resetAndSchedule(context: Context) {
        context.getSharedPreferences("account_expiry", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSystemService(NotificationManager::class.java).cancel(ID)
        schedule(context)
    }

    fun schedule(context: Context) {
        val store = (context.applicationContext as TodoApplication).settingsStore
        if (!store.hasLocalAccountAccess()) return
        if (!store.hasValidRemoteSession()) { store.clearExpiredRemoteSessionIfNeeded(); return }
        val at = store.currentSettings().remoteSyncRefreshExpiresAt * 1000
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarm(context))
    }

    fun notifyIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("account_expiry", Context.MODE_PRIVATE)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "账号登录状态", NotificationManager.IMPORTANCE_DEFAULT))
        if (prefs.getBoolean("notified", false) || !manager.areNotificationsEnabled()) return
        val intent = Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_SETTINGS_SECTION, "ACCOUNT")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val click = PendingIntent.getActivity(context, ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        runCatching {
            manager.notify(ID, NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_payki_todo).setContentTitle("PaykiTodo 登录已过期，请重新登录")
                .setContentText("点击进入账号。本机数据和离线提醒保留，重新登录后继续同步。")
                .setStyle(NotificationCompat.BigTextStyle().bigText("点击进入账号重新登录。本机数据、离线提醒和待同步修改均已保留。"))
                .setContentIntent(click).setAutoCancel(true).setOnlyAlertOnce(true).build())
            prefs.edit().putBoolean("notified", true).apply()
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(alarm(context))
        context.getSystemService(NotificationManager::class.java).cancel(ID)
    }
}

class AccountExpiryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as TodoApplication).settingsStore.clearExpiredRemoteSessionIfNeeded()
    }
}
