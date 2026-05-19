package com.example.todoalarm.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import java.util.Locale

class VoiceCaptureActivity : ComponentActivity() {
    private var transcript by mutableStateOf("")
    private var listening by mutableStateOf(false)
    private var statusText by mutableStateOf("准备听写")
    private var speechRecognizer: SpeechRecognizer? = null

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startSpeechRecognition()
        } else {
            Toast.makeText(this, "需要麦克风权限", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodoAlarmTheme {
                VoiceCaptureScreen(
                    transcript = transcript,
                    listening = listening,
                    statusText = statusText,
                    onFinish = {
                        val text = transcript.trim()
                        if (text.isBlank()) {
                            Toast.makeText(this, "没有可识别的内容", Toast.LENGTH_SHORT).show()
                        } else {
                            BackgroundCaptureProcessor.processText(this, text, title = "语音捕获")
                            Toast.makeText(this, BackgroundCaptureProcessor.processingToastMessage(this), Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    },
                    onCancel = { finish() }
                )
            }
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "当前系统不支持语音识别", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognition()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onDestroy() {
        speechRecognizer?.run {
            runCatching { stopListening() }
            destroy()
        }
        speechRecognizer = null
        super.onDestroy()
    }

    private fun startSpeechRecognition() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                    statusText = "正在听..."
                }

                override fun onBeginningOfSpeech() {
                    listening = true
                    statusText = "正在记录..."
                }

                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    listening = false
                    statusText = "语音结束，可点击完成"
                }

                override fun onError(error: Int) {
                    listening = false
                    statusText = speechErrorText(error)
                }

                override fun onResults(results: Bundle?) {
                    listening = false
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (text.isNotBlank()) transcript = text
                    statusText = if (transcript.isBlank()) "没有听到内容" else "识别完成，可点击完成"
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { transcript = it }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINA.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "说出待办或日程")
            })
        }
    }

    private fun speechErrorText(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "没有听清，可以取消后重试"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有听到内容"
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "语音识别网络异常"
            SpeechRecognizer.ERROR_AUDIO -> "录音异常"
            else -> "语音识别失败：$error"
        }
    }
}

@Composable
private fun VoiceCaptureScreen(
    transcript: String,
    listening: Boolean,
    statusText: String,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(132.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (listening) 0.18f else 0.10f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(58.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                if (listening) {
                    Spacer(Modifier.height(10.dp))
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = statusText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = transcript.ifBlank { "请说：明天下午三点到五点开会" },
            modifier = Modifier.fillMaxWidth(),
            color = if (transcript.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = if (transcript.isBlank()) FontWeight.Normal else FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(36.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(modifier = Modifier.weight(1f), onClick = onCancel) {
                Text("取消")
            }
            Button(
                modifier = Modifier.weight(1f),
                enabled = transcript.isNotBlank(),
                onClick = onFinish
            ) {
                Text("完成")
            }
        }
    }
}
