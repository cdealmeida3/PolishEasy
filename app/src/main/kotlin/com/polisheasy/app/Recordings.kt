package com.polisheasy.app

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.polisheasy.engine.ListenAndRepeat
import com.polisheasy.engine.TrackLine
import java.io.File

/** What the player of the class recordings is doing, as the Listen screen shows it. */
data class PlayerState(
    /** The recording open, or null when none is. */
    val number: Int? = null,
    val playing: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
    val repeat: Boolean = false,
    /** Step mode: one line, a pause to say it aloud, then the next. */
    val step: Boolean = false,
    /** The line being heard or waited on, counted from 0, or null before the first and after the last. */
    val line: Int? = null,
    /** In step mode, while the player waits for the Learner to say the line. */
    val yourTurn: Boolean = false,
)

/** What a recording is called in the media notification. */
data class RecordingTitle(val title: String, val chapter: String)

/** What the screens ask of the player of the class recordings. */
interface Recordings {
    /** The screens watch it. */
    val state: PlayerState

    /** Opens recording [number] from [file], paused at its start, with its [lines] for step mode. */
    fun open(number: Int, file: File, title: RecordingTitle, lines: List<TrackLine>)

    fun playPause()

    /** Moves by [ms], back when it is less than 0. */
    fun skip(ms: Long)

    /** Goes to the start of line [index] of the recording, and plays. */
    fun line(index: Int)

    fun speed(value: Float)

    fun repeat(on: Boolean)

    fun step(on: Boolean)

    /** In step mode's turn, waits for the Learner instead of going on by itself: they are saying the line into the microphone. Play goes on. */
    fun hold()

    /** Stops and forgets the recording, as leaving its screen does. */
    fun close()

    /** Called once, when the app's screens are gone for good. Playing in the background ends with the notification. */
    fun release()
}

/**
 * The one player of the class recordings in the app, shared by the Listen screen and by
 * [ClassAudioService], which keeps it playing in the background with a media notification.
 */
internal object ClassAudioPlayback {
    private var player: ExoPlayer? = null

    fun player(context: Context): ExoPlayer = player ?: ExoPlayer.Builder(context.applicationContext).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
            /* handleAudioFocus = */ true,
        )
        // Headphones out: the recording pauses rather than coming out of the speaker.
        setHandleAudioBecomingNoisy(true)
        player = this
    }

    fun release() {
        player?.release()
        player = null
    }
}

/**
 * Plays the class recordings imported on the phone, through the shared player, with step mode
 * run here on the main thread so it carries on in the background. Use from the main thread.
 */
class RecordingPlayer(context: Context) : Recordings {
    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var controller: ListenableFuture<MediaController>? = null
    private var lines: List<TrackLine> = emptyList()
    private val player: ExoPlayer by lazy {
        ClassAudioPlayback.player(this.context).also { player ->
            player.addListener(
                object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) = update()
                },
            )
        }
    }

    override var state by mutableStateOf(PlayerState())
        private set

    private val tick = object : Runnable {
        override fun run() {
            update()
            stepOn()
            if (state.number != null) handler.postDelayed(this, TICK_MS)
        }
    }

    private val resume = Runnable {
        state = state.copy(yourTurn = false)
        val next = (state.line ?: -1) + 1
        when {
            next < lines.size -> playFrom(lines[next].startMs, next)
            state.repeat && lines.isNotEmpty() -> playFrom(lines.first().startMs, 0)
            else -> {
                player.pause()
                player.seekTo(0)
                state = state.copy(line = null)
            }
        }
    }

    override fun open(number: Int, file: File, title: RecordingTitle, lines: List<TrackLine>) {
        // The session service is bound while a recording is open, so playing goes on in the background.
        if (controller == null) {
            controller = MediaController.Builder(context, SessionToken(context, ComponentName(context, ClassAudioService::class.java))).buildAsync()
        }
        this.lines = lines
        handler.removeCallbacks(resume)
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(Uri.fromFile(file))
                .setMediaId(number.toString())
                .setMediaMetadata(MediaMetadata.Builder().setTitle(title.title).setArtist(title.chapter).build())
                .build(),
        )
        player.repeatMode = if (state.repeat && !state.step) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.setPlaybackParameters(PlaybackParameters(state.speed))
        player.prepare()
        state = state.copy(number = number, playing = false, positionMs = 0, line = null, yourTurn = false)
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    override fun playPause() {
        if (state.number == null) return
        if (state.yourTurn) {
            // Tapped during the pause: no waiting, on to the next line.
            handler.removeCallbacks(resume)
            resume.run()
            return
        }
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
        }
        update()
    }

    override fun skip(ms: Long) {
        if (state.number == null) return
        handler.removeCallbacks(resume)
        val to = (player.currentPosition + ms).coerceIn(0, player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
        player.seekTo(to)
        state = state.copy(yourTurn = false, line = ListenAndRepeat.lineAt(lines, to))
        update()
    }

    override fun line(index: Int) {
        val line = lines.getOrNull(index) ?: return
        handler.removeCallbacks(resume)
        state = state.copy(yourTurn = false)
        playFrom(line.startMs, index)
    }

    override fun hold() {
        if (state.yourTurn) handler.removeCallbacks(resume)
    }

    override fun speed(value: Float) {
        state = state.copy(speed = value)
        player.setPlaybackParameters(PlaybackParameters(value))
    }

    override fun repeat(on: Boolean) {
        state = state.copy(repeat = on)
        player.repeatMode = if (on && !state.step) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    override fun step(on: Boolean) {
        handler.removeCallbacks(resume)
        state = state.copy(step = on, yourTurn = false, line = ListenAndRepeat.lineAt(lines, player.currentPosition))
        // In step mode the player itself stops at the end, and step mode starts it again when it repeats.
        player.repeatMode = if (state.repeat && !on) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    override fun close() {
        handler.removeCallbacks(tick)
        handler.removeCallbacks(resume)
        if (state.number != null) {
            player.stop()
            player.clearMediaItems()
        }
        state = state.copy(number = null, playing = false, positionMs = 0, durationMs = 0, line = null, yourTurn = false)
    }

    override fun release() {
        handler.removeCallbacks(tick)
        handler.removeCallbacks(resume)
        controller?.let(MediaController::releaseFuture)
        controller = null
    }

    private fun playFrom(positionMs: Long, line: Int) {
        player.seekTo(positionMs)
        player.play()
        state = state.copy(line = line)
        update()
    }

    private fun update() {
        if (state.number == null) return
        state = state.copy(
            playing = player.isPlaying || state.yourTurn,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it != C.TIME_UNSET } ?: 0,
        )
    }

    /** In step mode, the line heard: pause at its end, and wait for the Learner to say it before the next. */
    private fun stepOn() {
        if (!state.step || state.yourTurn || !player.isPlaying) {
            if (!state.step && player.isPlaying) state = state.copy(line = ListenAndRepeat.lineAt(lines, player.currentPosition))
            return
        }
        val at = state.line ?: ListenAndRepeat.lineAt(lines, player.currentPosition) ?: return
        val line = lines[at]
        if (player.currentPosition >= line.endMs) {
            player.pause()
            state = state.copy(line = at, yourTurn = true, playing = true)
            handler.postDelayed(resume, ListenAndRepeat.pauseAfter(line, state.speed))
        } else if (state.line != at) {
            state = state.copy(line = at)
        }
    }

    private companion object {
        const val TICK_MS = 50L
    }
}

/**
 * Keeps the class recording playing while the app is in the background, with Android's media
 * notification to pause it or go back to the app.
 */
class ClassAudioService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, ClassAudioPlayback.player(this)).setSessionActivity(open).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiped away while paused: nothing is left to play.
        if (session?.player?.playWhenReady != true) stopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        ClassAudioPlayback.release()
        super.onDestroy()
    }
}
