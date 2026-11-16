package com.polisheasy.audio

import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

const val GOOGLE_ENDPOINT = "https://texttospeech.googleapis.com"

/**
 * Google Cloud Text-to-Speech. The [key] travels only in a request header: never in a URL, where
 * a proxy or a log would keep it. One request at a time; a busy or failing service is asked
 * again up to [RETRIES] times, waiting longer each time.
 */
class GoogleVoice(
    private val settings: VoiceSettings,
    private val key: String,
    private val endpoint: String = GOOGLE_ENDPOINT,
    private val pause: (millis: Long) -> Unit = Thread::sleep,
) : VoiceService {
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build()

    override fun speak(text: String): ByteArray {
        val body = buildJsonObject {
            putJsonObject("input") { put("text", text) }
            putJsonObject("voice") {
                put("languageCode", settings.languageCode)
                put("name", settings.voice)
            }
            putJsonObject("audioConfig") {
                // LINEAR16 comes back as a WAV file, header included.
                put("audioEncoding", "LINEAR16")
                put("sampleRateHertz", settings.sampleRateHertz)
                // Not every voice takes a pace, so the voice's own pace is asked for by saying nothing.
                if (settings.speakingRate != 1.0) put("speakingRate", settings.speakingRate)
            }
        }
        val reply = send(request("/v1/text:synthesize").POST(HttpRequest.BodyPublishers.ofString(body.toString())))
        val sound = reply["audioContent"]?.jsonPrimitive?.content
            ?: throw VoiceFailure("The voice service answered without any sound")
        return try {
            Base64.getDecoder().decode(sound)
        } catch (_: IllegalArgumentException) {
            throw VoiceFailure("The voice service answered with sound that cannot be read")
        }
    }

    override fun names(): List<String> {
        val language = URLEncoder.encode(settings.languageCode, Charsets.UTF_8)
        val reply = send(request("/v1/voices?languageCode=$language").GET())
        return reply["voices"]?.jsonArray.orEmpty().mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }.sorted()
    }

    private fun request(path: String): HttpRequest.Builder =
        HttpRequest.newBuilder(URI.create(endpoint + path))
            .timeout(Duration.ofSeconds(60))
            .header("X-Goog-Api-Key", key)
            .header("Content-Type", "application/json; charset=utf-8")

    private fun send(builder: HttpRequest.Builder): JsonObject {
        var failure = ""
        repeat(RETRIES + 1) { attempt ->
            if (attempt > 0) pause(1000L shl (attempt - 1))
            val response = try {
                client.send(builder.build(), HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
            } catch (problem: IOException) {
                failure = "The voice service could not be reached: ${problem.message ?: problem.javaClass.simpleName}"
                return@repeat
            }
            val status = response.statusCode()
            if (status == 200) {
                return try {
                    Json.parseToJsonElement(response.body()).jsonObject
                } catch (_: IllegalArgumentException) {
                    throw VoiceFailure("The voice service answered with something that is not JSON")
                }
            }
            failure = "The voice service refused (HTTP $status): ${messageOf(response.body())}"
            if (status != 429 && status < 500) throw VoiceFailure(failure)
        }
        throw VoiceFailure("$failure. Gave up after ${RETRIES + 1} tries")
    }

    /** The service's own words for an error, or the start of whatever it sent. */
    private fun messageOf(body: String): String = try {
        Json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
    } catch (_: IllegalArgumentException) {
        null
    } ?: body.trim().take(200).ifEmpty { "no reason given" }

    companion object {
        const val RETRIES = 5
    }
}
