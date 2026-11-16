package com.polisheasy.app

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer

private const val SLOW_SPEED = 0.6f

/** Plays the Clips packaged with the app. Use from the main thread. */
class ClipPlayer(context: Context) {
    private val player = ExoPlayer.Builder(context.applicationContext).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
            /* handleAudioFocus = */ true,
        )
    }

    /** Plays the Clip at [path] in the app's assets from its start, in place of whatever is playing. */
    fun play(path: String, slow: Boolean = false) {
        player.setMediaItem(MediaItem.fromUri("asset:///$path"))
        // Slowed in software with the pitch kept, so it sounds the same on every phone.
        player.setPlaybackParameters(PlaybackParameters(if (slow) SLOW_SPEED else 1f))
        player.prepare()
        player.play()
    }

    fun stop() = player.stop()

    fun release() = player.release()
}
