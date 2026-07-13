package com.example.neurotrack.mindfulness

import android.content.Context
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.annotation.RawRes
import java.util.Locale

class MindfulnessAudioPlayer(
    private val context: Context,
) {
    private var mediaPlayer: MediaPlayer? = null

    val currentPositionMillis: Int
        get() = runCatching { mediaPlayer?.currentPosition ?: 0 }.getOrDefault(0)

    val durationMillis: Int
        get() = runCatching { mediaPlayer?.duration ?: 0 }.getOrDefault(0)

    val isPlaying: Boolean
        get() = runCatching { mediaPlayer?.isPlaying == true }.getOrDefault(false)

    fun start(
        @RawRes audioResId: Int,
        languageTag: String,
        onCompletion: () -> Unit,
        onError: () -> Unit,
    ): Int {
        stop()
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        val localizedContext = context.createConfigurationContext(configuration)
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val player = MediaPlayer.create(localizedContext, audioResId, attributes, 0)
            ?: error("Unable to open mindfulness audio resource")
        mediaPlayer = player
        player.setOnCompletionListener { completedPlayer ->
            if (mediaPlayer === completedPlayer) onCompletion()
        }
        player.setOnErrorListener { failedPlayer, _, _ ->
            if (mediaPlayer === failedPlayer) onError()
            true
        }
        player.start()
        return player.duration.coerceAtLeast(0)
    }

    fun togglePlayback(): Boolean {
        val player = mediaPlayer ?: return false
        if (player.isPlaying) player.pause() else player.start()
        return player.isPlaying
    }

    fun restart() {
        val player = mediaPlayer ?: return
        player.seekTo(0)
        player.start()
    }

    fun stop() {
        val player = mediaPlayer ?: return
        mediaPlayer = null
        runCatching { player.stop() }
        player.reset()
        player.release()
    }
}
