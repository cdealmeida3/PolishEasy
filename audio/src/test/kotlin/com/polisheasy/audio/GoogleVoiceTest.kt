package com.polisheasy.audio

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** [GoogleVoice] against a server on this machine that answers as the service would. */
internal class GoogleVoiceTest {
    private class Received(val method: String, val uri: String, val key: String?, val body: String)

    private val received = mutableListOf<Received>()

    /** The status and body of each answer in turn; the last is repeated. */
    private val answers = mutableListOf<Pair<Int, String>>()
    private val pauses = mutableListOf<Long>()

    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange: HttpExchange ->
            received += Received(
                exchange.requestMethod,
                exchange.requestURI.toString(),
                exchange.requestHeaders.getFirst("X-Goog-Api-Key"),
                exchange.requestBody.readBytes().toString(Charsets.UTF_8),
            )
            val (status, body) = answers[minOf(received.size, answers.size) - 1]
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        start()
    }

    private val settings = VoiceSettings(
        service = GOOGLE_SERVICE,
        voice = "pl-PL-Chirp3-HD-Aoede",
        languageCode = "pl-PL",
        sampleRateHertz = 24000,
        encoding = ClipEncoding("opus", 32, 80, 200, 20),
    )

    private fun voice(settings: VoiceSettings = this.settings) =
        GoogleVoice(settings, "the-key", "http://127.0.0.1:${server.address.port}", pause = { pauses += it })

    private fun sound(bytes: ByteArray) = 200 to """{"audioContent": "${Base64.getEncoder().encodeToString(bytes)}"}"""

    private fun refusal(status: Int, message: String) = status to """{"error": {"code": $status, "message": "$message"}}"""

    @AfterTest
    fun stop() = server.stop(0)

    @Test
    fun `speaking posts the text, the voice and the format, with the key in a header and not in the address`() {
        answers += sound(byteArrayOf(1, 2, 3))

        val wav = voice().speak("Dzień dobry")

        assertContentEquals(byteArrayOf(1, 2, 3), wav)
        val request = received.single()
        assertEquals("POST", request.method)
        assertEquals("/v1/text:synthesize", request.uri)
        assertEquals("the-key", request.key)
        val body = Json.parseToJsonElement(request.body).jsonObject
        assertEquals("Dzień dobry", body.getValue("input").jsonObject.getValue("text").jsonPrimitive.content)
        assertEquals("pl-PL", body.getValue("voice").jsonObject.getValue("languageCode").jsonPrimitive.content)
        assertEquals("pl-PL-Chirp3-HD-Aoede", body.getValue("voice").jsonObject.getValue("name").jsonPrimitive.content)
        val config = body.getValue("audioConfig").jsonObject
        assertEquals("LINEAR16", config.getValue("audioEncoding").jsonPrimitive.content)
        assertEquals("24000", config.getValue("sampleRateHertz").jsonPrimitive.content)
        assertFalse("speakingRate" in config, "The voice's own pace is asked for by saying nothing")
    }

    @Test
    fun `a pace other than the voice's own is sent`() {
        answers += sound(byteArrayOf(1))

        voice(settings.copy(speakingRate = 0.9)).speak("tak")

        val config = Json.parseToJsonElement(received.single().body).jsonObject.getValue("audioConfig").jsonObject
        assertEquals("0.9", config.getValue("speakingRate").jsonPrimitive.content)
    }

    @Test
    fun `a busy service is asked again after a pause`() {
        answers += refusal(429, "Slow down")
        answers += refusal(503, "Try later")
        answers += sound(byteArrayOf(7))

        val wav = voice().speak("tak")

        assertContentEquals(byteArrayOf(7), wav)
        assertEquals(3, received.size)
        assertEquals(listOf(1000L, 2000L), pauses)
    }

    @Test
    fun `a service that stays busy is given up on, with its message`() {
        answers += refusal(429, "Quota exceeded for the minute")

        val failure = assertFailsWith<VoiceFailure> { voice().speak("tak") }

        assertEquals(GoogleVoice.RETRIES + 1, received.size)
        assertTrue("Quota exceeded for the minute" in failure.message.orEmpty(), failure.message)
        assertTrue("Gave up after 6 tries" in failure.message.orEmpty(), failure.message)
    }

    @Test
    fun `a refusal that asking again cannot mend stops at once, with the service's message`() {
        answers += refusal(400, "API key not valid. Please pass a valid API key.")

        val failure = assertFailsWith<VoiceFailure> { voice().speak("tak") }

        assertEquals(1, received.size)
        assertEquals(emptyList(), pauses)
        assertEquals("The voice service refused (HTTP 400): API key not valid. Please pass a valid API key.", failure.message)
    }

    @Test
    fun `an answer with no sound is a failure`() {
        answers += 200 to "{}"

        assertFailsWith<VoiceFailure> { voice().speak("tak") }
    }

    @Test
    fun `the voices of the language are listed by name, with the key in a header`() {
        answers += 200 to """{"voices": [{"name": "pl-PL-Wavenet-A", "languageCodes": ["pl-PL"]}, {"name": "pl-PL-Chirp3-HD-Aoede"}]}"""

        val names = voice().names()

        assertEquals(listOf("pl-PL-Chirp3-HD-Aoede", "pl-PL-Wavenet-A"), names)
        val request = received.single()
        assertEquals("GET", request.method)
        assertEquals("/v1/voices?languageCode=pl-PL", request.uri)
        assertEquals("the-key", request.key)
    }
}
