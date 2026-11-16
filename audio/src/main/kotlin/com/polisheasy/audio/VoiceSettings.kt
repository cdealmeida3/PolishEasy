package com.polisheasy.audio

import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The only service the command knows how to call. */
const val GOOGLE_SERVICE = "google-cloud-text-to-speech"

/** What `audio-voice.json` records: everything that decides how a Clip sounds. */
@Serializable
data class VoiceSettings(
    val service: String,
    /** The service's name for the voice, as `--voices` lists it. */
    val voice: String,
    val languageCode: String,
    val sampleRateHertz: Int,
    /** 1.0 is the voice's own pace. */
    val speakingRate: Double = 1.0,
    val encoding: ClipEncoding,
)

/** How the voice's sound becomes a Clip's file. */
@Serializable
data class ClipEncoding(
    val codec: String,
    val bitrateKbps: Int,
    /** Silence before the speech, so a player never clips the first sound. */
    val leadMs: Int,
    /** Silence after the speech. */
    val tailMs: Int,
    /** The end of the speech fades out over this long, in case the voice stops mid-sound. */
    val fadeOutMs: Int,
)

// Every field is written, so a setting left at its default and the same setting spelt out fingerprint alike.
private val canonical = Json { encodeDefaults = true }

/**
 * The fingerprint of the Clip these settings make of [text]. It changes when the text or any
 * setting does, and not when `audio-voice.json` is only laid out differently. Adding a field to
 * [VoiceSettings] changes every fingerprint, so every Clip is then made again.
 */
fun VoiceSettings.fingerprint(text: String): String {
    val bytes = (canonical.encodeToString(VoiceSettings.serializer(), this) + "\n" + text).toByteArray(Charsets.UTF_8)
    return MessageDigest.getInstance("SHA-256").digest(bytes).take(8).joinToString("") { "%02x".format(it) }
}
