package com.example.todoalarm.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.todoalarm.data.TodoItem
import kotlin.math.PI
import kotlin.math.sin

internal class ReminderAlertController(
    private val context: Context
) {
    private var vibrator: Vibrator? = null
    private var audioTrack: AudioTrack? = null
    private val handler = Handler(Looper.getMainLooper())
    private var releaseRunnable: Runnable? = null

    fun start(todoItem: TodoItem) {
        stop()
        if (todoItem.ringEnabled) {
            playSingleMelody()
        }
        if (todoItem.vibrateEnabled) {
            ensureVibrator().vibrate(VibrationEffect.createWaveform(longArrayOf(0, 260, 100, 260), -1))
        }
    }

    fun stop() {
        releaseRunnable?.let(handler::removeCallbacks)
        releaseRunnable = null
        audioTrack?.runCatching {
            if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                stop()
            }
        }
        audioTrack?.release()
        audioTrack = null
        vibrator?.cancel()
    }

    fun shutdown() {
        stop()
    }

    private fun playSingleMelody() {
        val sampleRate = 22050
        val melody = listOf(
            Note(293.66, 170), // Re
            Note(440.00, 170), // La
            Note(392.00, 170), // Sol
            Note(587.33, 240)  // Re (high)
        )
        val pcm = buildMelodyPcm(melody, sampleRate)
        if (pcm.isEmpty()) return

        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBuffer, pcm.size * 2)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcm, 0, pcm.size)
        track.play()
        audioTrack = track
        val durationMs = ((pcm.size * 1000L) / sampleRate) + 150L
        releaseRunnable = Runnable { stop() }.also {
            handler.postDelayed(it, durationMs)
        }
    }

    private fun buildMelodyPcm(melody: List<Note>, sampleRate: Int): ShortArray {
        if (melody.isEmpty()) return shortArrayOf()
        val gapMs = 35
        val totalSamples = melody.sumOf { millisToSamples(it.durationMs + gapMs, sampleRate) }
        val data = ShortArray(totalSamples)
        var cursor = 0
        val amplitude = 0.55

        for (note in melody) {
            val noteSamples = millisToSamples(note.durationMs, sampleRate)
            for (i in 0 until noteSamples) {
                val t = i / sampleRate.toDouble()
                val envelope = when {
                    i < noteSamples * 0.15 -> i / (noteSamples * 0.15)
                    i > noteSamples * 0.85 -> (noteSamples - i) / (noteSamples * 0.15)
                    else -> 1.0
                }.coerceIn(0.0, 1.0)
                val sample = (sin(2.0 * PI * note.frequency * t) * envelope * amplitude * Short.MAX_VALUE).toInt()
                data[cursor++] = sample.toShort()
            }
            val gapSamples = millisToSamples(gapMs, sampleRate)
            repeat(gapSamples) {
                data[cursor++] = 0
            }
        }
        return data
    }

    private fun millisToSamples(ms: Int, sampleRate: Int): Int {
        return (ms * sampleRate / 1000.0).toInt()
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

    private data class Note(
        val frequency: Double,
        val durationMs: Int
    )
}
