package com.example.ui.screens

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object GameSoundEffects {
    private fun playTone(
        frequency1: Double,
        frequency2: Double,
        durationMs: Int,
        type: String = "sine"
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val sampleRate = 22050
                val numSamples = (durationMs * sampleRate / 1000)
                if (numSamples <= 0) return@launch
                val buffer = ShortArray(numSamples)
                
                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val freq = frequency1 + (frequency2 - frequency1) * progress
                    val angle = 2.0 * Math.PI * i.toDouble() / (sampleRate / freq)
                    
                    val value = when (type) {
                        "square" -> if (Math.sin(angle) >= 0) 4000 else -4000
                        "triangle" -> {
                            val x = (angle / (2.0 * Math.PI)) % 1.0
                            val v = if (x < 0.5) 4.0 * x - 1.0 else 3.0 - 4.0 * x
                            (v * 5000).toInt()
                        }
                        else -> (Math.sin(angle) * 6000).toInt() // sine
                    }
                    buffer[i] = value.toShort()
                }
                
                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    numSamples * 2,
                    AudioTrack.MODE_STATIC
                )
                
                audioTrack.write(buffer, 0, numSamples)
                audioTrack.play()
                
                delay(durationMs + 50L)
                try {
                    audioTrack.stop()
                } catch (e: Exception) {}
                audioTrack.release()
            } catch (e: Exception) {
                android.util.Log.e("GameSoundEffects", "Error playing tone: ${e.message}", e)
            }
        }
    }

    fun playTap() {
        playTone(600.0, 800.0, 70, "sine")
    }

    fun playMatch() {
        playTone(523.25, 1046.50, 160, "triangle") // C5 to C6 quick sweep
    }

    fun playCombo() {
        playTone(880.0, 1760.0, 240, "square") // Sparkly quick chime
    }

    fun playPowerUp() {
        playTone(440.0, 1320.0, 300, "sine") // Ascending whoosh
    }

    fun playExplosion() {
        playTone(180.0, 45.0, 350, "square") // Falling growl for bomb
    }

    fun playSuccess() {
        playTone(587.33, 1174.66, 250, "triangle") // D5 to D6 cheerful chirp
    }

    fun playFailure() {
        playTone(320.0, 120.0, 300, "sine") // Sad descending buzz
    }
}
