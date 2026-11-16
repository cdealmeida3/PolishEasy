package com.polisheasy.audio

import com.polisheasy.engine.audioKey
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

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
    /**
     * Texts the voice says wrongly as they are written, each with the spelling that is sent to it
     * instead. The Clip still belongs to the text as the course writes it.
     */
    val saidAs: Map<String, String> = emptyMap(),
) {
    private val saidAsByKey: Map<String, String> by lazy { saidAs.mapKeys { audioKey(it.key) } }

    /** What is sent to the voice for [text]: the text itself, unless [saidAs] gives it another spelling. */
    fun said(text: String): String = saidAsByKey[audioKey(text)] ?: text
}

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
 * The fingerprint of the Clip these settings make of [text]. It changes when the text, what is
 * sent to the voice for it, or any other setting does, and not when `audio-voice.json` is only
 * laid out differently. Adding a field to [VoiceSettings] changes every fingerprint, so every
 * Clip is then made again. [VoiceSettings.saidAs] is the exception: it counts only through what
 * it sends for this text, so a new entry makes that one Clip again and no other.
 */
fun VoiceSettings.fingerprint(text: String): String {
    val settings = JsonObject(canonical.encodeToJsonElement(VoiceSettings.serializer(), this).jsonObject - "saidAs")
    val bytes = (settings.toString() + "\n" + said(text)).toByteArray(Charsets.UTF_8)
    return MessageDigest.getInstance("SHA-256").digest(bytes).take(8).joinToString("") { "%02x".format(it) }
}
