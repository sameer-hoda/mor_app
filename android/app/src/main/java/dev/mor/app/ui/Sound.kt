package dev.mor.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * Synthesized UI sound cues, mirrored 1:1 from morzip.js's Sound engine:
 *   tick   dual square  1567.98 .035 g.05 + 1200 .03 g.025  (wheel rotation)
 *   select D[i%5] sine .22 g.12 + octave ×2 .12 g.03        (card select)
 *   next   440 tri .09 g.06 + 659.25 tri .16 g.07 @+60ms
 *   back   659.25 tri .09 g.05 + 440 tri .14 g.05 @+60ms
 *   settle 196 tri .09 g.09 + 392 tri .05 g.03 @+12ms
 *   done   D5 F#5 A5 D6 sine .5 g.08 @ 0/90/180/270ms       (plan)
 *   chime  880 sine .6 g.06 + 1318.51 .6 g.04 @+80ms        (notify)
 * D = [587.33, 659.25, 739.99, 880, 987.77]
 * Audio starts only after the first user gesture (ensure()); mute is persisted.
 */
object MorSound {
    private const val SR = 44100
    private const val NOTE_OFFSET_MS = 60L

    private var track: AudioTrack? = null
    private var playhead = 0
    private var muted: Boolean? = null
    private var ctxRef: Context? = null

    fun ensure(context: Context) {
        if (track != null) return
        ctxRef = context.applicationContext
        if (muted == null) muted = ctxRef!!
            .getSharedPreferences("mor", Context.MODE_PRIVATE)
            .getBoolean("morV2.sound.muted", false)
        if (muted == true) return
        runCatching {
            val min = AudioTrack.getMinBufferSize(
                SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT
            ).coerceAtLeast(SR / 4)
            track = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(SR)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                min * 4,
                AudioTrack.MODE_STREAM,
                AudioTrack.PERFORMANCE_MODE_LOW_LATENCY
            ).also { it.play() }
        }
    }

    fun isMuted(context: Context): Boolean {
        if (muted == null) ensure(context)
        return muted == true
    }

    fun toggleMute(context: Context): Boolean {
        ensure(context)
        muted = !(muted == true)
        (ctxRef ?: context).applicationContext
            .getSharedPreferences("mor", Context.MODE_PRIVATE)
            .edit().putBoolean("morV2.sound.muted", muted == true).apply()
        return muted == true
    }

    private class Note(
        val f: Double, val osc: String, val dur: Double, val gain: Double, val atMs: Int
    )

    private fun cue(vararg notes: Note) {
        val t = track ?: return
        if (muted == true) return
        val totalMs = (notes.maxOf { it.atMs + it.dur * 1000 } + 30).toInt()
        val n = totalMs * SR / 1000
        val buf = FloatArray(n)
        for (note in notes) {
            val start = note.atMs * SR / 1000
            val len = (note.dur * SR).toInt()
            for (i in 0 until len) {
                val idx = start + i
                if (idx >= n) break
                val s = i.toFloat() / SR
                val phase = 2 * PI * note.f * s
                val w = when (note.osc) {
                    "square" -> if (sin(phase) >= 0) 1.0 else -1.0
                    "triangle" -> 2.0 / PI * asinFast(sin(phase))
                    else -> sin(phase)
                }
                val attack = (0.004f * SR).toInt()
                val env = if (i < attack) i.toFloat() / attack
                else exp(-(s - 0.004) / (note.dur * 0.35)).toFloat()
                buf[idx] += (w * env * note.gain).toFloat()
                if (buf[idx] > 1f) buf[idx] = 1f
                if (buf[idx] < -1f) buf[idx] = -1f
            }
        }
        runCatching {
            t.write(buf, 0, n, AudioTrack.WRITE_NON_BLOCKING)
            playhead += n
        }
    }

    private fun asinFast(x: Double) = kotlin.math.asin(x.coerceIn(-1.0, 1.0))

    fun tickWheel() = cue(
        Note(1567.98, "square", 0.035, 0.05, 0),
        Note(1200.0, "square", 0.03, 0.025, 0),
    )
    fun next() = cue(
        Note(440.0, "triangle", 0.09, 0.06, 0),
        Note(659.25, "triangle", 0.16, 0.07, NOTE_OFFSET_MS.toInt()),
    )
    fun back() = cue(
        Note(659.25, "triangle", 0.09, 0.05, 0),
        Note(440.0, "triangle", 0.14, 0.05, NOTE_OFFSET_MS.toInt()),
    )
    fun settle() = cue(
        Note(196.0, "triangle", 0.09, 0.09, 0),
        Note(392.0, "triangle", 0.05, 0.03, 12),
    )

    private val ds = doubleArrayOf(587.33, 659.25, 739.99, 880.0, 987.77)
    fun select(i: Int): Int {
        val f = ds[(i.coerceAtLeast(0)) % ds.size]
        cue(
            Note(f, "sine", 0.22, 0.12, 0),
            Note(f * 2, "sine", 0.12, 0.03, 0),
        )
        return i.coerceAtLeast(0) % ds.size
    }
    fun done() = cue(
        Note(587.33, "sine", 0.5, 0.08, 0),
        Note(739.99, "sine", 0.5, 0.08, 90),
        Note(880.0, "sine", 0.5, 0.08, 180),
        Note(1174.66, "sine", 0.5, 0.08, 270),
    )
    fun chime() = cue(
        Note(880.0, "sine", 0.6, 0.06, 0),
        Note(1318.51, "sine", 0.6, 0.04, 80),
    )
}
