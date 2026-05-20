package com.example.todoalarm.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.todoalarm.R
import com.example.todoalarm.TodoApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

object BackgroundCaptureProcessor {
    fun processText(
        context: Context,
        text: String,
        title: String = "文本捕获",
        appendToActiveNote: Boolean = true
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        val app = context.applicationContext as TodoApplication
        val jobId = nextJobId()
        ensureChannel(app)
        notifyProcessing(app, jobId, title)
        app.applicationScope.launch {
            try {
                val result = CapturePlanningPipeline.captureTextToNodes(
                    app = app,
                    markdown = cleanText,
                    title = title,
                    appendToActiveNote = appendToActiveNote
                )
                notifyInserted(app, jobId, result)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                notifyFailure(app, jobId, error.message ?: "识别失败")
            }
        }
    }

    fun processPlanningNoteText(
        context: Context,
        noteId: Long?,
        text: String,
        title: String = "规划台识别"
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        val app = context.applicationContext as TodoApplication
        val jobId = nextJobId()
        ensureChannel(app)
        notifyProcessing(app, jobId, title)
        app.applicationScope.launch {
            try {
                val result = CapturePlanningPipeline.capturePlanningNoteTextToNodes(
                    app = app,
                    noteId = noteId,
                    markdown = cleanText,
                    title = title
                )
                notifyInserted(app, jobId, result)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                notifyFailure(app, jobId, error.message ?: "识别失败")
            }
        }
    }

    fun processImage(
        context: Context,
        uri: Uri,
        title: String = "拍照捕获"
    ) {
        processImages(context = context, uris = listOf(uri), title = title)
    }

    fun processImages(
        context: Context,
        uris: List<Uri>,
        title: String = "图片捕获"
    ) {
        if (uris.isEmpty()) return
        val app = context.applicationContext as TodoApplication
        val jobId = nextJobId()
        ensureChannel(app)
        notifyProcessing(app, jobId, title)
        app.applicationScope.launch {
            try {
                val result = CapturePlanningPipeline.captureImagesToNodes(
                    app = app,
                    uris = uris,
                    title = title
                )
                notifyInserted(app, jobId, result)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                notifyFailure(app, jobId, error.message ?: "识别失败")
            }
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CaptureChannelId,
            "PaykiTodo 捕获识别",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "分享、拍照、语音捕获后的后台识别结果"
        }
        manager.createNotificationChannel(channel)
    }

    fun processingToastMessage(context: Context): String {
        return if (canPostResultNotifications(context)) {
            "正在后台识别，稍后通知"
        } else {
            "正在后台识别；通知权限未开启，请稍后进规划台查看"
        }
    }

    fun canPostResultNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun notifyProcessing(context: Context, jobId: Int, title: String) {
        notifySafely(
            context = context,
            notificationId = jobId,
            builder = NotificationCompat.Builder(context, CaptureChannelId)
                .setSmallIcon(R.drawable.ic_stat_payki_todo)
                .setContentTitle("正在识别...")
                .setContentText(title)
                .setOngoing(true)
                .setProgress(0, 0, true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        )
    }

    private fun notifyInserted(context: Context, jobId: Int, result: CapturePlanningInsertResult) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_PLANNING_NOTE_ID, result.noteId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            jobId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notifySafely(
            context = context,
            notificationId = jobId,
            builder = NotificationCompat.Builder(context, CaptureChannelId)
                .setSmallIcon(R.drawable.ic_stat_payki_todo)
                .setContentTitle("已添加 ${result.importedCount} 条草稿")
                .setContentText("点击前往规划台检查并发布")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        )
    }

    private fun notifyFailure(context: Context, jobId: Int, message: String) {
        notifySafely(
            context = context,
            notificationId = jobId,
            builder = NotificationCompat.Builder(context, CaptureChannelId)
                .setSmallIcon(R.drawable.ic_stat_payki_todo)
                .setContentTitle("捕获识别失败")
                .setContentText(message.take(90))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        )
    }

    private fun notifySafely(
        context: Context,
        notificationId: Int,
        builder: NotificationCompat.Builder
    ) {
        if (!canPostResultNotifications(context)) {
            return
        }
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }
    }

    private fun nextJobId(): Int = NotificationBaseId + counter.getAndIncrement()

    const val CaptureChannelId = "capture_processing"
    private const val NotificationBaseId = 40_000
    private val counter = AtomicInteger(1)
}
