package com.polisheasy.audio

import java.nio.file.Path
import kotlin.time.Duration

/** Speaks Polish. Returns the sound as a WAV file's bytes, or throws a [VoiceFailure]. */
fun interface Voice {
    fun speak(text: String): ByteArray
}

/** A [Voice] at a service that offers several. */
interface VoiceService : Voice {
    /** The names of the Polish voices the service offers. */
    fun names(): List<String>
}

/** A Clip's file as measured: how long it plays and its loudest moment, in decibels below full scale (0 is as loud as a file can be). */
data class Measured(val length: Duration, val peakDb: Double)

/** Turns the WAV a [Voice] returned into a Clip's file. */
fun interface Encoder {
    /** Writes the Clip to [target] and measures it. Throws a [VoiceFailure] if it cannot. */
    fun encode(wav: ByteArray, target: Path): Measured

    /** What stops this encoder working at all, or null. Asked before anything is sent to the voice. */
    fun problem(): String? = null
}

/** Why a Clip could not be made, in words for whoever ran the command. */
class VoiceFailure(message: String) : Exception(message)
