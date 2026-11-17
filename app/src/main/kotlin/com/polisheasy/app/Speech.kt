package com.polisheasy.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * What the screens ask for when the Learner is to be heard saying Polish: *Powtórz na głos*.
 * Speaking never blocks anything: where it cannot be offered, no microphone is shown.
 */
interface Speech {
    /**
     * Whether a microphone can be offered: the phone has a speech recognizer, and the Learner did
     * not refuse the microphone (or has given it since). The screens watch it.
     */
    val available: Boolean

    /** Whether the app may use the microphone now, or has to ask first. */
    val permitted: Boolean

    /** Whether the Learner is being listened to right now. The screens watch it. */
    val listening: Boolean

    /**
     * Listens for Polish once. [onHeard] gets what may have been said, best guess first, or nothing
     * when nothing was understood or the recognizer failed. Stops whatever listening was going on.
     */
    fun listen(onHeard: (List<String>) -> Unit)

    fun stop()

    /** The Learner refused the microphone: it is not offered again. */
    fun refused()

    /** Called once, when the app's screens are gone for good. */
    fun release()
}

/** The Speech the screens use, provided by the app. Without one, nothing is offered. */
val LocalSpeech = staticCompositionLocalOf<Speech> { NoSpeech }

/** No speech at all: no microphone is offered. */
object NoSpeech : Speech {
    override val available = false
    override val permitted = false
    override val listening = false
    override fun listen(onHeard: (List<String>) -> Unit) = onHeard(emptyList())
    override fun stop() = Unit
    override fun refused() = Unit
    override fun release() = Unit
}

/**
 * Android's speech recognizer in Polish, offline where the phone has the language. Use from the
 * main thread.
 */
class AndroidSpeech(context: Context) : Speech {
    private val context = context.applicationContext
    private val settings = Settings(this.context)
    private var recognizer: SpeechRecognizer? = null
    private var refusedNow by mutableStateOf(settings.microphoneRefused)

    override val permitted: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override val available: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context) && (permitted || !refusedNow)

    override var listening by mutableStateOf(false)
        private set

    override fun listen(onHeard: (List<String>) -> Unit) {
        stop()
        val listener = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        listener.setRecognitionListener(
            object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    listening = false
                    onHeard(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty())
                }

                override fun onError(error: Int) {
                    listening = false
                    onHeard(emptyList())
                }

                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            },
        )
        listening = true
        listener.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, POLISH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, POLISH)
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, GUESSES),
        )
    }

    override fun stop() {
        if (listening) recognizer?.cancel()
        listening = false
    }

    override fun refused() {
        settings.microphoneRefused = true
        refusedNow = true
    }

    override fun release() {
        recognizer?.destroy()
        recognizer = null
    }

    private companion object {
        const val POLISH = "pl-PL"

        /** How many guesses the recognizer offers; any of them may match. */
        const val GUESSES = 5
    }
}

/** The phone's speech recognizer, kept while the app's screens are. */
@Composable
fun rememberSpeech(): Speech {
    val context = LocalContext.current
    return remember { AndroidSpeech(context) }
}
