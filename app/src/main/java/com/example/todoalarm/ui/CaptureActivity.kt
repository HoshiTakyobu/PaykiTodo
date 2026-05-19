package com.example.todoalarm.ui

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File

class CaptureActivity : ComponentActivity() {
    private var pendingPhotoUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingPhotoUri
        if (success && uri != null) {
            BackgroundCaptureProcessor.processImage(this, uri, title = "拍照捕获")
            Toast.makeText(this, BackgroundCaptureProcessor.processingToastMessage(this), Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = createCaptureUri()
        pendingPhotoUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun createCaptureUri(): Uri {
        val directory = File(cacheDir, "capture_images").apply { mkdirs() }
        val file = File.createTempFile("paykitodo-capture-", ".jpg", directory)
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    }
}
