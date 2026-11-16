package com.polisheasy.app

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer

private const val SLOW_SPEED = 0.6f

/** What the screens ask for when a Clip is to be heard. */
interface Clips {
    /** Plays the Clip at [path] in the app's assets from its start, in place of whatever is playing. */
    fun play(path: String, slow: Boolean = false)

    fun stop()

    /** Called once, when the app's screens are gone for good. */
    fun release()
}

/** Plays the Clips packaged with the app. Use from the main thread. */
class ClipPlayer(context: Context) : Clips {
    private val player = ExoPlayer.Builder(context.applicationContext).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
            /* handleAudioFocus = */ true,
        )
    }

    override fun play(path: String, slow: Boolean) {
        player.setMediaItem(MediaItem.fromUri("asset:///$path"))
        // Slowed in software with the pitch kept, so it sounds the same on every phone.
        player.setPlaybackParameters(PlaybackParameters(if (slow) SLOW_SPEED else 1f))
        player.prepare()
        player.play()
    }

    override fun stop() = player.stop()

    override fun release() = player.release()
}
