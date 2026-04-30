package com.example.todoalarm.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.todoalarm.R
import com.example.todoalarm.data.TodoItem

internal class ReminderAlertController(
    private val context: Context
) {
    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null

    fun start(todoItem: TodoItem) {
        stop()
        if (todoItem.ringEnabled) {
            playConfiguredClip()
        }
        if (todoItem.vibrateEnabled) {
            ensureVibrator().vibrate(VibrationEffect.createWaveform(longArrayOf(0, 260, 100, 260), -1))
        }
    }

    fun stop() {
        mediaPlayer?.runCatching {
            if (isPlaying) {
                stop()
            }
            reset()
            release()
        }
        mediaPlayer = null
        vibrator?.cancel()
    }

    fun shutdown() {
        stop()
    }

    private fun playConfiguredClip() {
        val settings = (context.applicationContext as com.example.todoalarm.TodoApplication).settingsStore.currentSettings()
        val toneUri = settings.reminderToneUri
        if (!toneUri.isNullOrBlank()) {
            if (playFromUri(Uri.parse(toneUri))) {
                return
            }
        }
        playBuiltInClip()
    }

    private fun playBuiltInClip() {
        val afd = runCatching { context.resources.openRawResourceFd(R.raw.remind_vocal) }.getOrNull() ?: return
        val player = MediaPlayer()
        runCatching {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.isLooping = false
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            player.setOnCompletionListener {
                it.reset()
                it.release()
                if (mediaPlayer === it) {
                    mediaPlayer = null
                }
            }
            player.setOnErrorListener { mp, _, _ ->
                mp.reset()
                mp.release()
                if (mediaPlayer === mp) {
                    mediaPlayer = null
                }
                true
            }
            player.prepare()
            player.start()
            mediaPlayer = player
        }.onFailure {
            player.release()
        }.also {
            afd.close()
        }
    }

    private fun playFromUri(uri: Uri): Boolean {
        val player = MediaPlayer()
        return runCatching {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.isLooping = false
            player.setDataSource(context, uri)
            player.setOnCompletionListener {
                it.reset()
                it.release()
                if (mediaPlayer === it) {
                    mediaPlayer = null
                }
            }
            player.setOnErrorListener { mp, _, _ ->
                mp.reset()
                mp.release()
                if (mediaPlayer === mp) {
                    mediaPlayer = null
                }
                true
            }
            player.prepare()
            player.start()
            mediaPlayer = player
        }.fold(
            onSuccess = { true },
            onFailure = {
                player.release()
                false
            }
        )
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
}
