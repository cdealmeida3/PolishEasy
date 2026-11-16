package com.polisheasy.audio

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.io.path.createTempDirectory
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.DurationUnit
import org.junit.jupiter.api.Assumptions.assumeTrue

/** [FfmpegEncoder] with the real commands. Skipped on a machine that has no ffmpeg, as CI may not. */
internal class FfmpegEncoderTest {
    private val dir: File = createTempDirectory("encoder").toFile()
    private val settings = VoiceSettings(
        service = GOOGLE_SERVICE,
        voice = "any",
        languageCode = "pl-PL",
        sampleRateHertz = 24000,
        encoding = ClipEncoding("opus", bitrateKbps = 32, leadMs = 80, tailMs = 200, fadeOutMs = 20),
    )

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun ffmpegIsHere(): Boolean = try {
        ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start().waitFor() == 0
    } catch (_: IOException) {
        false
    }

    /** One second of a 440 Hz tone as a 16-bit mono WAV file, which is what the voice service sends. */
    private fun tone(): ByteArray {
        val rate = 24000
        val samples = ByteBuffer.allocate(rate * 2).order(ByteOrder.LITTLE_ENDIAN)
        repeat(rate) { samples.putShort((sin(2 * PI * 440 * it / rate) * 12000).toInt().toShort()) }
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray()).putInt(36 + rate * 2).put("WAVE".toByteArray())
        header.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
        header.put("data".toByteArray()).putInt(rate * 2)
        return ByteArrayOutputStream().apply { write(header.array()); write(samples.array()) }.toByteArray()
    }

    @Test
    fun `a WAV becomes a small Ogg file as long as the speech and the silence around it`() {
        assumeTrue(ffmpegIsHere())
        val target = File(dir, "tone.part")

        val length = FfmpegEncoder(settings).encode(tone(), target.toPath())

        assertEquals(1.28, length.toDouble(DurationUnit.SECONDS), 0.03)
        assertEquals("OggS", target.readBytes().take(4).toByteArray().toString(Charsets.US_ASCII))
        assertTrue(target.length() in 1000..12000, "The file is ${target.length()} bytes")
    }

    @Test
    fun `the same sound makes the same file`() {
        assumeTrue(ffmpegIsHere())
        val first = File(dir, "first.part")
        val second = File(dir, "second.part")

        FfmpegEncoder(settings).encode(tone(), first.toPath())
        FfmpegEncoder(settings).encode(tone(), second.toPath())

        assertContentEquals(first.readBytes(), second.readBytes())
    }

    @Test
    fun `sound that is not a WAV file is a failure in ffmpeg's words, and a codec other than Opus is a problem`() {
        assumeTrue(ffmpegIsHere())

        val failure = runCatching { FfmpegEncoder(settings).encode("not sound".toByteArray(), File(dir, "bad.part").toPath()) }.exceptionOrNull()

        assertTrue(failure is VoiceFailure && failure.message.orEmpty().startsWith("ffmpeg failed"), failure.toString())
        assertEquals(null, FfmpegEncoder(settings).problem())
        assertTrue("mp3" in FfmpegEncoder(settings.copy(encoding = settings.encoding.copy(codec = "mp3"))).problem().orEmpty())
    }
}
