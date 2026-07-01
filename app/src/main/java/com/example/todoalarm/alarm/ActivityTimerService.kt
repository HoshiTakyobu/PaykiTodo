package com.example.todoalarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.todoalarm.R
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.ActivityRecord
import com.example.todoalarm.data.ActivityRecordState
import com.example.todoalarm.ui.MainActivity
import kotlinx.coroutines.launch

/**
 * 正向计时前台服务。扛杀进程：运行态存数据库，重启后据 lastResumeAtMillis 恢复。
 * 通知用 setUsesChronometer(true) 自动走秒，带「暂停/继续」「结束」动作。
 */
class ActivityTimerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        val app = application as? TodoApplication ?: return START_NOT_STICKY
        app.applicationScope.launch {
            val active = app.repository.getActiveActivityRecord()
            if (active != null) {
                val notification = buildNotification(active)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun buildNotification(record: ActivityRecord): Notification {
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )

        val pauseOrResumeAction = if (record.state == ActivityRecordState.RUNNING) {
            val pauseIntent = Intent(this, ActivityTimerReceiver::class.java).apply {
                action = ActivityTimerReceiver.ACTION_PAUSE_TIMER
            }
            val pausePendingIntent = PendingIntent.getBroadcast(
                this,
                1,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
            )
            NotificationCompat.Action(R.drawable.ic_pause, "暂停", pausePendingIntent)
        } else {
            val resumeIntent = Intent(this, ActivityTimerReceiver::class.java).apply {
                action = ActivityTimerReceiver.ACTION_RESUME_TIMER
            }
            val resumePendingIntent = PendingIntent.getBroadcast(
                this,
                2,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
            )
            NotificationCompat.Action(R.drawable.ic_play_arrow, "继续", resumePendingIntent)
        }

        val finishIntent = Intent(this, ActivityTimerReceiver::class.java).apply {
            action = ActivityTimerReceiver.ACTION_FINISH_TIMER
        }
        val finishPendingIntent = PendingIntent.getBroadcast(
            this,
            3,
            finishIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )
        val finishAction = NotificationCompat.Action(R.drawable.ic_stop, "结束", finishPendingIntent)

        val displayTitle = record.title.trim().ifBlank { "未命名记录" }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_payki_todo)
            .setContentTitle(displayTitle)
            .setContentText("正在记录...")
            .setUsesChronometer(true)
            .setWhen(record.lastResumeAtMillis ?: record.startAtMillis)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(pauseOrResumeAction)
            .addAction(finishAction)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "正向计时",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "记录实际活动时长的计时器通知"
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun immutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }

    companion object {
        private const val CHANNEL_ID = "activity_timer"
        private const val NOTIFICATION_ID = 54000

        fun start(context: Context) {
            val intent = Intent(context, ActivityTimerService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ActivityTimerService::class.java)
            context.stopService(intent)
        }

        /** 刷新通知（暂停/继续切换后调用）。 */
        fun refresh(context: Context) {
            start(context)
        }
    }
}
