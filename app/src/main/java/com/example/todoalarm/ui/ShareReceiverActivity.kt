package com.example.todoalarm.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val message = runCatching {
                enqueueSharedCapture(intent)
            }.getOrElse { error ->
                error.message ?: "没有可识别的内容"
            }
            Toast.makeText(this@ShareReceiverActivity, message, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private suspend fun enqueueSharedCapture(intent: Intent?): String {
        val action = intent?.action
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) {
            return "没有可识别的内容"
        }
        val type = intent.type.orEmpty()
        val text = extractSharedText(this, intent)
        if (text.isNotBlank() && (type.isBlank() || type.startsWith("text/"))) {
            BackgroundCaptureProcessor.processText(this, text, title = "分享文本")
            return BackgroundCaptureProcessor.processingToastMessage(this)
        }
        val imageUris = copySharedImageUrisToCache(intent)
        if (imageUris.isNotEmpty()) {
            BackgroundCaptureProcessor.processImages(this, imageUris, title = "分享图片")
            return BackgroundCaptureProcessor.processingToastMessage(this)
        }
        if (text.isNotBlank()) {
            BackgroundCaptureProcessor.processText(this, text, title = "分享文本")
            return BackgroundCaptureProcessor.processingToastMessage(this)
        }
        return "没有可识别的内容"
    }

    private suspend fun copySharedImageUrisToCache(intent: Intent): List<Uri> = withContext(Dispatchers.IO) {
        extractSharedImageUris(intent).mapNotNull { uri ->
            runCatching {
                val directory = File(cacheDir, "shared_capture_images").apply { mkdirs() }
                val file = File.createTempFile("paykitodo-share-", ".jpg", directory)
                contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                } ?: return@runCatching null
                Uri.fromFile(file)
            }.getOrNull()
        }
    }
}

private fun extractSharedText(context: Context, intent: Intent): String {
    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { if (it.isNotBlank()) return it }
    val clip = intent.clipData ?: return ""
    return buildString {
        for (index in 0 until clip.itemCount) {
            clip.getItemAt(index).coerceToText(context)?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let {
                if (isNotEmpty()) append('\n')
                append(it)
            }
        }
    }
}

private fun extractSharedImageUris(intent: Intent): List<Uri> {
    val uris = mutableListOf<Uri>()
    intent.parcelableExtraCompat(Intent.EXTRA_STREAM, Uri::class.java)?.let { uris += it }
    intent.parcelableArrayListExtraCompat(Intent.EXTRA_STREAM, Uri::class.java)?.let { uris += it }
    val clip = intent.clipData
    if (clip != null) {
        for (index in 0 until clip.itemCount) {
            clip.getItemAt(index).uri?.let { uris += it }
        }
    }
    return uris.distinct()
}

private fun <T : Parcelable> Intent.parcelableExtraCompat(name: String, clazz: Class<T>): T? {
    @Suppress("DEPRECATION")
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, clazz)
    } else {
        getParcelableExtra(name)
    }
}

private fun <T : Parcelable> Intent.parcelableArrayListExtraCompat(name: String, clazz: Class<T>): ArrayList<T>? {
    @Suppress("DEPRECATION")
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(name, clazz)
    } else {
        getParcelableArrayListExtra(name)
    }
}
