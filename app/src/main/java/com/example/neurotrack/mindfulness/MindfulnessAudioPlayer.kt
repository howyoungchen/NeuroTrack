package com.example.neurotrack.mindfulness

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

class MindfulnessAudioPlayer {
    private val playing = AtomicBoolean(false)
    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null

    fun start() {
        if (!playing.compareAndSet(false, true)) return
        val sampleRate = 22_050
        val buffer = createAmbientLoop(sampleRate)
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBuffer, buffer.size * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audioTrack = track
        track.play()
        playbackThread = thread(name = "mindfulness-audio", isDaemon = true) {
            runCatching {
                while (playing.get()) {
                    track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                }
            }
        }
    }

    fun stop() {
        if (!playing.getAndSet(false)) return
        audioTrack?.pause()
        audioTrack?.flush()
        audioTrack?.release()
        audioTrack = null
        playbackThread = null
    }

    private fun createAmbientLoop(sampleRate: Int): ShortArray {
        val seconds = 8
        return ShortArray(sampleRate * seconds) { index ->
            val time = index.toDouble() / sampleRate
            val envelope = 0.55 - 0.35 * kotlin.math.cos(2.0 * PI * time / seconds)
            val pad = sin(2.0 * PI * 174.0 * time) * 0.38 +
                sin(2.0 * PI * 220.0 * time) * 0.24 +
                sin(2.0 * PI * 261.63 * time) * 0.12
            (pad * envelope * Short.MAX_VALUE * 0.16).toInt().toShort()
        }
    }
}
