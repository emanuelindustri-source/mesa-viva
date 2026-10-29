package com.mesaviva.app.shell

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Audio nativo de Mesa Viva.
 *
 * Todos los sonidos y la música se sintetizan en el dispositivo: no se
 * descarga ni se copia ninguna pista externa. Así la app sigue funcionando
 * offline y no arrastra licencias de audio.
 */
object NativeAudio {

    enum class Sfx { Tap, Card, Capture, Promote, Win, Lose, Dice, Draw, Chat }

    private const val RATE = 22050
    private const val MUSIC_SECONDS = 12.8

    private val main = Handler(Looper.getMainLooper())
    private var musicTrack: AudioTrack? = null
    private var musicThread: Thread? = null

    @Volatile
    private var musicRunning = false

    fun play(context: Context, sfx: Sfx, enabled: Boolean = true) {
        if (!enabled) return
        val samples = when (sfx) {
            Sfx.Tap -> tone(880.0, 0.045, 0.18)
            Sfx.Card -> sweep(660.0, 220.0, 0.11, 0.20) + noiseBurst(0.06, 0.10)
            Sfx.Capture -> sweep(520.0, 260.0, 0.18, 0.24)
            Sfx.Promote -> arpeggio(doubleArrayOf(523.25, 659.25, 783.99, 1046.5), 0.09, 0.22)
            Sfx.Win -> arpeggio(doubleArrayOf(523.25, 659.25, 783.99, 1046.5, 1318.5), 0.11, 0.26)
            Sfx.Lose -> arpeggio(doubleArrayOf(392.0, 329.63, 261.63), 0.14, 0.22)
            Sfx.Dice -> dice()
            Sfx.Draw -> arpeggio(doubleArrayOf(440.0, 554.37), 0.12, 0.18)
            Sfx.Chat -> arpeggio(doubleArrayOf(740.0, 880.0), 0.06, 0.15)
        }
        playSamples(samples)
    }

    fun startMusic(context: Context, festivalId: String, enabled: Boolean) {
        if (!enabled) {
            stopMusic()
            return
        }
        if (musicRunning) return
        musicRunning = true
        val thread = Thread {
            val samples = buildMusic(festivalId)
            if (!musicRunning) return@Thread
            val track = try {
                buildTrack(samples, loop = true)
            } catch (_: Exception) {
                null
            }
            main.post {
                if (track == null) {
                    musicRunning = false
                    return@post
                }
                if (!musicRunning) {
                    runCatching { track.release() }
                    return@post
                }
                musicTrack = track
                runCatching { track.play() }
            }
        }
        thread.isDaemon = true
        musicThread = thread
        thread.start()
    }

    fun stopMusic() {
        musicRunning = false
        val track = musicTrack
        musicTrack = null
        main.post {
            runCatching {
                track?.stop()
                track?.release()
            }
        }
        musicThread = null
    }

    private fun playSamples(samples: ShortArray) {
        if (samples.isEmpty()) return
        val track = try {
            buildTrack(samples, loop = false)
        } catch (_: Exception) {
            return
        }
        runCatching { track.play() }
        val duration = samples.size * 1000L / RATE + 180
        main.postDelayed({
            runCatching {
                track.stop()
                track.release()
            }
        }, duration)
    }

    private fun buildTrack(samples: ShortArray, loop: Boolean): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(if (loop) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_GAME)
                    .setContentType(if (loop) AudioAttributes.CONTENT_TYPE_MUSIC else AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        if (loop) track.setLoopPoints(0, samples.size, -1)
        track.setVolume(if (loop) 0.32f else 0.75f)
        return track
    }

    /* ------------------------------------------------------------ síntesis */

    private fun tone(freq: Double, seconds: Double, volume: Double): ShortArray {
        val count = (seconds * RATE).toInt().coerceAtLeast(1)
        return ShortArray(count) { index ->
            val t = index.toDouble() / RATE
            val envelope = exp(-5.0 * t / seconds)
            (sin(2 * PI * freq * t) * volume * envelope * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun sweep(from: Double, to: Double, seconds: Double, volume: Double): ShortArray {
        val count = (seconds * RATE).toInt().coerceAtLeast(1)
        var phase = 0.0
        return ShortArray(count) { index ->
            val t = index.toDouble() / RATE
            val freq = from + (to - from) * (t / seconds)
            phase += 2 * PI * freq / RATE
            val envelope = exp(-3.5 * t / seconds)
            (sin(phase) * volume * envelope * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun arpeggio(freqs: DoubleArray, step: Double, volume: Double): ShortArray {
        val perNote = (step * RATE).toInt().coerceAtLeast(1)
        val total = perNote * freqs.size + (RATE * 0.25).toInt()
        return ShortArray(total) { index ->
            val note = (index / perNote).coerceAtMost(freqs.lastIndex)
            val local = (index - note * perNote).toDouble() / RATE
            val envelope = exp(-4.0 * local / step)
            val value = sin(2 * PI * freqs[note] * local) * 0.55 +
                sin(2 * PI * freqs[note] * 2 * local) * 0.15
            (value * volume * envelope * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun noiseBurst(seconds: Double, volume: Double): ShortArray {
        val count = (seconds * RATE).toInt().coerceAtLeast(1)
        val random = Random(7)
        var smooth = 0f
        return ShortArray(count) { index ->
            val t = index.toDouble() / RATE
            val raw = random.nextFloat() * 2f - 1f
            smooth = smooth * 0.6f + raw * 0.4f
            val envelope = exp(-8.0 * t / seconds)
            (smooth * volume * envelope * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun dice(): ShortArray {
        val count = (0.32 * RATE).toInt()
        val random = Random(11)
        return ShortArray(count) { index ->
            val t = index.toDouble() / RATE
            val click = if (index % (RATE / 18) < 260) 1.0 else 0.0
            val raw = (random.nextFloat() * 2f - 1f) * 0.3f
            val envelope = exp(-6.0 * t / 0.32)
            ((raw * click + sin(2 * PI * 180 * t) * 0.4 * click) * envelope * 0.35 * Short.MAX_VALUE)
                .toInt().toShort()
        }
    }

    /**
     * Música original y procedural. Cada festividad cambia la escala, el
     * tempo y el timbre; no hay samples ni melodías con derechos de autor.
     */
    private fun buildMusic(festivalId: String): ShortArray {
        val root = when (festivalId) {
            "halloween" -> 220.0
            "navidad" -> 261.63
            "reyes" -> 293.66
            "san-valentin" -> 246.94
            "san-patricio" -> 293.66
            "independencia" -> 261.63
            "san-juan" -> 246.94
            else -> 261.63
        }
        val scale = when (festivalId) {
            "halloween" -> intArrayOf(0, 2, 3, 5, 7, 8, 10)
            "san-patricio" -> intArrayOf(0, 2, 4, 5, 7, 9, 10)
            else -> intArrayOf(0, 2, 4, 5, 7, 9, 11)
        }
        val total = (MUSIC_SECONDS * RATE).toInt()
        val buffer = FloatArray(total)
        val beat = 0.4
        val random = Random(festivalId.hashCode().toLong())
        val melody = intArrayOf(0, 2, 4, 2, 3, 1, 0, 4, 5, 3, 2, 1, 0, 2, 4, 5)

        for (step in 0 until (MUSIC_SECONDS / beat).toInt()) {
            val noteIndex = melody[step % melody.size]
            val octave = when (step % 8) {
                5, 6 -> 1
                else -> 0
            }
            val semitone = scale[noteIndex % scale.size] + 12 * octave
            val freq = root * Math.pow(2.0, semitone / 12.0)
            val start = (step * beat * RATE).toInt()
            val length = (beat * 1.7 * RATE).toInt()
            for (offset in 0 until length) {
                val index = start + offset
                if (index >= total) break
                val t = offset.toDouble() / RATE
                val envelope = exp(-3.2 * t / beat)
                val value = sin(2 * PI * freq * t) * 0.20 +
                    sin(2 * PI * freq * 2 * t) * 0.045
                buffer[index] += (value * envelope).toFloat()
            }
            // Bajo cada dos tiempos.
            if (step % 2 == 0) {
                val bassStart = start
                val bassLength = (beat * 1.8 * RATE).toInt()
                for (offset in 0 until bassLength) {
                    val index = bassStart + offset
                    if (index >= total) break
                    val t = offset.toDouble() / RATE
                    val envelope = exp(-2.2 * t / beat)
                    buffer[index] += (sin(2 * PI * root / 2 * t) * 0.10 * envelope).toFloat()
                }
            }
            // Percusión suave de madera.
            if (step % 2 == 1) {
                val percStart = start
                for (offset in 0 until (0.05 * RATE).toInt()) {
                    val index = percStart + offset
                    if (index >= total) break
                    val t = offset.toDouble() / RATE
                    val envelope = exp(-40.0 * t)
                    buffer[index] += ((random.nextFloat() * 2f - 1f) * 0.030 * envelope).toFloat()
                }
            }
        }

        // Fundido de entrada y salida para el bucle.
        val fade = (0.35 * RATE).toInt()
        for (index in 0 until fade) {
            val factor = index.toFloat() / fade
            buffer[index] *= factor
            buffer[total - 1 - index] *= factor
        }

        var peak = 0.01f
        for (value in buffer) {
            val absolute = if (value < 0) -value else value
            if (absolute > peak) peak = absolute
        }
        val gain = 0.72f / peak
        return ShortArray(total) { index ->
            (buffer[index] * gain * Short.MAX_VALUE).toInt().coerceIn(
                Short.MIN_VALUE.toInt(),
                Short.MAX_VALUE.toInt(),
            ).toShort()
        }
    }
}
