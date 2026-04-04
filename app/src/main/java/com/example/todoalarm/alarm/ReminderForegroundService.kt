package com.example.todoalarm.alarm

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
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
    private lateinit var alertController: ReminderAlertController
    private var reminderNotifier: ReminderNotifier? = null
    private var activeTodoId: Long = -1L
    private var userPresentReceiver: BroadcastReceiver? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        alertController = ReminderAlertController(applicationContext)
    }

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
                ActiveReminderStore.clearIfMatches(this@ReminderForegroundService, todoId)
                stopSelf(startId)
                return@launch
            }

            ActiveReminderStore.markActive(this@ReminderForegroundService, todoId)
            val notifier = ReminderNotifier(this@ReminderForegroundService)
            reminderNotifier = notifier
            activeTodoId = todoId
            registerUserPresentReceiver()
            val notification = notifier.build(todoItem)
            startInForeground(todoId, notification)
            notifier.show(todoItem)
            alertController.start(todoItem)
            wakeDevice()
            repeat(4) {
                triggerReminderUi(notifier, todoItem.id)
                delay(1200L)
            }
            delay(115_000L)
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
        unregisterUserPresentReceiver()
        alertController.shutdown()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
        reminderNotifier = null
        activeTodoId = -1L
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

    private fun registerUserPresentReceiver() {
        unregisterUserPresentReceiver()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val todoId = activeTodoId
                val notifier = reminderNotifier
                if (todoId <= 0L || notifier == null) return
                triggerReminderUi(notifier, todoId)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }
        userPresentReceiver = receiver
    }

    private fun unregisterUserPresentReceiver() {
        val receiver = userPresentReceiver ?: return
        runCatching { unregisterReceiver(receiver) }
        userPresentReceiver = null
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
