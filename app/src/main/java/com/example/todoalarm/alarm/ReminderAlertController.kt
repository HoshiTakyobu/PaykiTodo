package com.example.todoalarm.alarm

import android.content.Context
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.todoalarm.R
import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.ReminderAudioChannel
import com.example.todoalarm.data.TodoItem

internal class ReminderAlertController(
    private val context: Context
) {
    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var boostedStream: Int? = null
    private var boostedOriginalVolume: Int? = null

    fun start(todoItem: TodoItem) {
        stop()
        val settings = currentSettings()
        if (todoItem.ringEnabled && !settings.workQuietModeEnabled) {
            playConfiguredClip()
        }
        val shouldVibrate = todoItem.vibrateEnabled || settings.workQuietModeEnabled
        if (shouldVibrate) {
            val pattern = if (settings.workQuietModeEnabled) {
                longArrayOf(0, 420, 90, 420, 90, 620)
            } else {
                longArrayOf(0, 260, 100, 260)
            }
            ensureVibrator().vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    fun stop() {
        ringtone?.runCatching {
            if (isPlaying) stop()
        }
        ringtone = null
        mediaPlayer?.runCatching {
            if (isPlaying) {
                stop()
            }
            reset()
            release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        restoreBoostedVolume()
    }

    fun shutdown() {
        stop()
    }

    private fun playConfiguredClip() {
        val settings = currentSettings()
        val toneUri = settings.reminderToneUri
        if (!toneUri.isNullOrBlank()) {
            if (playRingtoneFromUri(Uri.parse(toneUri), settings) || playFromUri(Uri.parse(toneUri), settings)) {
                return
            }
        }
        playBuiltInClip(settings)
    }

    private fun playRingtoneFromUri(uri: Uri, settings: AppSettings): Boolean {
        return runCatching {
            val target = RingtoneManager.getRingtone(context, uri) ?: return false
            @Suppress("DEPRECATION")
            target.streamType = audioStreamFor(settings.reminderAudioChannel)
            maybeBoostSystemVolume(settings)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                target.volume = settings.reminderInternalVolumePercent.toPlayerVolume()
            }
            target.play()
            ringtone = target
            true
        }.getOrDefault(false)
    }

    private fun playBuiltInClip(settings: AppSettings) {
        val afd = runCatching { context.resources.openRawResourceFd(R.raw.remind_vocal) }.getOrNull() ?: return
        val player = MediaPlayer()
        runCatching {
            player.setAudioAttributes(
                audioAttributesFor(settings.reminderAudioChannel)
            )
            player.isLooping = false
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            player.setVolume(settings.reminderInternalVolumePercent.toPlayerVolume(), settings.reminderInternalVolumePercent.toPlayerVolume())
            player.setOnCompletionListener {
                it.reset()
                it.release()
                if (mediaPlayer === it) {
                    mediaPlayer = null
                }
                restoreBoostedVolume()
            }
            player.setOnErrorListener { mp, _, _ ->
                mp.reset()
                mp.release()
                if (mediaPlayer === mp) {
                    mediaPlayer = null
                }
                restoreBoostedVolume()
                true
            }
            player.prepare()
            maybeBoostSystemVolume(settings)
            player.start()
            mediaPlayer = player
        }.onFailure {
            player.release()
            restoreBoostedVolume()
        }.also {
            afd.close()
        }
    }

    private fun playFromUri(uri: Uri, settings: AppSettings): Boolean {
        val player = MediaPlayer()
        return runCatching {
            player.setAudioAttributes(
                audioAttributesFor(settings.reminderAudioChannel)
            )
            player.isLooping = false
            player.setDataSource(context, uri)
            player.setVolume(settings.reminderInternalVolumePercent.toPlayerVolume(), settings.reminderInternalVolumePercent.toPlayerVolume())
            player.setOnCompletionListener {
                it.reset()
                it.release()
                if (mediaPlayer === it) {
                    mediaPlayer = null
                }
                restoreBoostedVolume()
            }
            player.setOnErrorListener { mp, _, _ ->
                mp.reset()
                mp.release()
                if (mediaPlayer === mp) {
                    mediaPlayer = null
                }
                restoreBoostedVolume()
                true
            }
            player.prepare()
            maybeBoostSystemVolume(settings)
            player.start()
            mediaPlayer = player
        }.fold(
            onSuccess = { true },
            onFailure = {
                player.release()
                restoreBoostedVolume()
                false
            }
        )
    }

    private fun currentSettings(): AppSettings {
        return (context.applicationContext as com.example.todoalarm.TodoApplication).settingsStore.currentSettings()
    }

    private fun audioAttributesFor(channel: ReminderAudioChannel): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(audioUsageFor(channel))
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }

    private fun audioUsageFor(channel: ReminderAudioChannel): Int {
        return when (channel) {
            ReminderAudioChannel.ALARM -> AudioAttributes.USAGE_ALARM
            ReminderAudioChannel.ACCESSIBILITY -> AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
            ReminderAudioChannel.NOTIFICATION -> AudioAttributes.USAGE_NOTIFICATION
            ReminderAudioChannel.MEDIA -> AudioAttributes.USAGE_MEDIA
        }
    }

    private fun audioStreamFor(channel: ReminderAudioChannel): Int {
        @Suppress("DEPRECATION")
        return when (channel) {
            ReminderAudioChannel.ALARM -> AudioManager.STREAM_ALARM
            ReminderAudioChannel.ACCESSIBILITY -> AudioManager.STREAM_ACCESSIBILITY
            ReminderAudioChannel.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
            ReminderAudioChannel.MEDIA -> AudioManager.STREAM_MUSIC
        }
    }

    private fun maybeBoostSystemVolume(settings: AppSettings) {
        if (!settings.reminderBoostSystemVolume) return
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return
        val stream = audioStreamFor(settings.reminderAudioChannel)
        val max = audioManager.getStreamMaxVolume(stream).coerceAtLeast(1)
        val target = ((max * settings.reminderBoostVolumePercent) + 99) / 100
        val normalizedTarget = target.coerceIn(0, max)
        val current = audioManager.getStreamVolume(stream)
        if (boostedStream == null) {
            boostedStream = stream
            boostedOriginalVolume = current
        }
        if (current < normalizedTarget) {
            audioManager.setStreamVolume(stream, normalizedTarget, 0)
        }
    }

    private fun restoreBoostedVolume() {
        val stream = boostedStream ?: return
        val original = boostedOriginalVolume ?: return
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return
        runCatching { audioManager.setStreamVolume(stream, original, 0) }
        boostedStream = null
        boostedOriginalVolume = null
    }

    private fun Int.toPlayerVolume(): Float = (coerceIn(0, 100) / 100f).coerceIn(0f, 1f)

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
