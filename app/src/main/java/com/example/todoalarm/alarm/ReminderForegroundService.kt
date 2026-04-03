package com.example.todoalarm.alarm

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.example.todoalarm.TodoApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReminderForegroundService : Service() {
    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.Main.immediate + serviceJob)
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val todoId = intent?.getLongExtra(AlarmScheduler.EXTRA_TODO_ID, -1L) ?: -1L
        if (todoId <= 0L) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        scope.launch {
            val app = application as TodoApplication
            val todoItem = app.repository.getTodo(todoId)
            if (todoItem == null || todoItem.completed || !todoItem.reminderEnabled) {
                stopSelf(startId)
                return@launch
            }

            val notifier = ReminderNotifier(this@ReminderForegroundService)
            val notification = notifier.build(todoItem)
            startInForeground(todoId, notification)
            notifier.show(todoItem)
            startAlert(todoItem)
            wakeDevice()
            triggerReminderUi(notifier, todoItem.id)
            delay(900L)
            triggerReminderUi(notifier, todoItem.id)
            delay(1500L)
            triggerReminderUi(notifier, todoItem.id)
            delay(45_000L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAlert()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun startInForeground(todoId: Long, notification: Notification) {
        val id = ReminderNotifier.notificationId(todoId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(id, notification)
        }
    }

    private fun startAlert(todoItem: com.example.todoalarm.data.TodoItem) {
        stopAlert()
        if (todoItem.ringEnabled) {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = true
                }
                audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                play()
            }
        }
        if (todoItem.vibrateEnabled) {
            val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            vibrator = v
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 350), 0))
        }
    }

    private fun stopAlert() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun triggerReminderUi(notifier: ReminderNotifier, todoId: Long) {
        runCatching { notifier.reminderPendingIntent(todoId).send() }
        runCatching {
            startActivity(
                notifier.createReminderIntent(todoId).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
        }
    }

    private fun wakeDevice() {
        val powerManager = getSystemService(PowerManager::class.java) ?: return
        val lock = wakeLock ?: powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "PaykiTodo:ReminderWakeLock"
        ).also { wakeLock = it }
        if (!lock.isHeld) {
            @Suppress("DEPRECATION")
            lock.acquire(10_000L)
        }
    }

    companion object {
        fun start(context: android.content.Context, todoId: Long) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ReminderForegroundService::class.java).putExtra(AlarmScheduler.EXTRA_TODO_ID, todoId)
            )
        }
    }
}
