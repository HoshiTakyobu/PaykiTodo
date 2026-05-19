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
                val preview = CapturePlanningPipeline.recognizeText(
                    app = app,
                    markdown = cleanText,
                    title = title,
                    appendToActiveNote = appendToActiveNote
                )
                if (preview.importableCount <= 0) {
                    notifyFailure(app, jobId, "未能识别出待办或日程")
                } else {
                    val stored = CapturePlanningStore.put(preview)
                    notifyCompleted(app, jobId, stored)
                }
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
                val preview = CapturePlanningPipeline.recognizePlanningNoteText(
                    app = app,
                    noteId = noteId,
                    markdown = cleanText,
                    title = title
                )
                if (preview.importableCount <= 0) {
                    notifyFailure(app, jobId, "未能识别出待办或日程")
                } else {
                    val stored = CapturePlanningStore.put(preview)
                    notifyCompleted(app, jobId, stored)
                }
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
                val preview = CapturePlanningPipeline.recognizeImages(
                    app = app,
                    uris = uris,
                    title = title
                )
                if (preview.importableCount <= 0) {
                    notifyFailure(app, jobId, "未能识别出待办或日程")
                } else {
                    val stored = CapturePlanningStore.put(preview)
                    notifyCompleted(app, jobId, stored)
                }
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

    private fun notifyCompleted(context: Context, jobId: Int, preview: CapturePlanningPreview) {
        val intent = ShareReceiverActivity.previewIntent(context, preview.id)
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
                .setContentTitle("已识别 ${preview.importableCount} 条待办/日程")
                .setContentText("点击查看并确认导入")
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
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
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
