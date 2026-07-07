package com.vidi.droidxadrez

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Tiny real-time synth for short UI feedback beeps — no audio files. */
object SoundEngine {
    private const val PREFS = "xadrez_prefs"
    private const val KEY_SOUND = "sound_on"
    private const val SAMPLE_RATE = 44100

    private lateinit var prefs: SharedPreferences
    var isOn: Boolean = true
        private set

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        isOn = prefs.getBoolean(KEY_SOUND, true)
    }

    fun toggleSound(): Boolean {
        isOn = !isOn
        prefs.edit().putBoolean(KEY_SOUND, isOn).apply()
        return isOn
    }

    private fun tone(freqHz: Double, durationSec: Double, gain: Double = 0.16) {
        if (!isOn) return
        val frameCount = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(frameCount)
        for (i in 0 until frameCount) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = gain * exp(-4.0 * (t / durationSec))
            val value = sin(2 * PI * freqHz * t) * envelope
            samples[i] = (value * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        track.write(samples, 0, samples.size)
        track.setNotificationMarkerPosition(frameCount)
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(t: AudioTrack) { t.release() }
            override fun onPeriodicNotification(t: AudioTrack) {}
        })
        track.play()
    }

    /** Each note is (freqHz, durationSec, gain, delayMs before it starts). */
    private fun playSequence(vararg notes: List<Double>) {
        for (note in notes) {
            val (freq, dur, gain, delayMs) = note
            CoroutineScope(Dispatchers.Default).launch {
                kotlinx.coroutines.delay(delayMs.toLong())
                tone(freq, dur, gain)
            }
        }
    }

    fun playMove() = playSequence(listOf(320.0, 0.09, 0.16, 0.0))
    fun playCapture() = playSequence(listOf(180.0, 0.14, 0.18, 0.0))
    fun playCheck() = playSequence(listOf(520.0, 0.1, 0.15, 0.0), listOf(660.0, 0.12, 0.15, 90.0))
    fun playEnd() = playSequence(listOf(440.0, 0.15, 0.18, 0.0), listOf(330.0, 0.25, 0.18, 140.0))
    fun playClick() = playSequence(listOf(700.0, 0.05, 0.1, 0.0))
}
