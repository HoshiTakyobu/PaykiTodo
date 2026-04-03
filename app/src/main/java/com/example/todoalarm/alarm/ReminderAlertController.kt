package com.example.todoalarm.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import com.example.todoalarm.data.TodoItem
import java.util.Locale

internal class ReminderAlertController(
    private val context: Context
) : TextToSpeech.OnInitListener {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: String? = null

    fun start(todoItem: TodoItem) {
        stop()
        if (todoItem.ringEnabled) {
            startSound()
        }
        if (todoItem.vibrateEnabled) {
            ensureVibrator().vibrate(VibrationEffect.createWaveform(longArrayOf(0, 900, 450), 0))
        }
        if (todoItem.voiceEnabled) {
            val speech = "现在需要处理的任务是，${todoItem.title}"
            pendingSpeech = speech
            ensureTts()
            speakPendingIfReady()
        }
    }

    fun stop() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        textToSpeech?.stop()
    }

    fun shutdown() {
        stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        ttsReady = false
        pendingSpeech = null
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        textToSpeech?.language = Locale.SIMPLIFIED_CHINESE
        speakPendingIfReady()
    }

    private fun startSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, uri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
            isLooping = true
            prepare()
            start()
        }
    }

    private fun ensureVibrator(): Vibrator {
        val existing = vibrator
        if (existing != null) return existing
        val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = created
        return created
    }

    private fun ensureTts() {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(context, this)
        }
    }

    private fun speakPendingIfReady() {
        val speech = pendingSpeech ?: return
        if (!ttsReady) return
        textToSpeech?.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "todo_alarm")
    }
}
