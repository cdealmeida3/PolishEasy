package com.polisheasy.audio

import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Makes an Opus file in an Ogg container with the `ffmpeg` command, and measures it with
 * `ffprobe`. Nothing of either ships in the app.
 */
class FfmpegEncoder(private val settings: VoiceSettings) : Encoder {
    private val encoding = settings.encoding

    override fun problem(): String? = when {
        encoding.codec != "opus" -> "audio-voice.json asks for the codec \"${encoding.codec}\", and only \"opus\" can be made"
        !runs("ffmpeg") || !runs("ffprobe") ->
            "The ffmpeg and ffprobe commands are needed to make a Clip's file, and they were not found. Install ffmpeg (with libopus)"
        else -> null
    }

    private fun runs(command: String): Boolean = try {
        ProcessBuilder(command, "-version").redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start().waitFor() == 0
    } catch (_: IOException) {
        false
    }

    override fun encode(wav: ByteArray, target: Path): Duration {
        fun ms(millis: Int) = String.format(Locale.ROOT, "%.3f", millis / 1000.0)
        // The end of the speech fades out, then silence goes before and after it.
        val filter = listOf(
            "areverse", "afade=t=in:d=${ms(encoding.fadeOutMs)}", "areverse",
            "adelay=${encoding.leadMs}:all=1", "apad=pad_dur=${ms(encoding.tailMs)}",
        ).joinToString(",")
        run(
            wav,
            "ffmpeg", "-v", "error", "-y", "-f", "wav", "-i", "pipe:0",
            "-af", filter, "-ac", "1", "-ar", settings.sampleRateHertz.toString(),
            "-c:a", "libopus", "-b:a", "${encoding.bitrateKbps}k", "-application", "voip",
            // No metadata and no encoder version, so the same sound always makes the same file.
            "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact",
            "-f", "ogg", target.toString(),
        )
        val shown = run(
            null,
            "ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", target.toString(),
        )
        return shown.trim().toDoubleOrNull()?.seconds ?: throw VoiceFailure("ffprobe could not measure the Clip it was given")
    }

    private fun run(input: ByteArray?, vararg command: String): String {
        val errors = File.createTempFile("clip", ".log")
        try {
            val process = try {
                ProcessBuilder(*command).redirectError(errors).start()
            } catch (problem: IOException) {
                throw VoiceFailure("The ${command.first()} command could not be run: ${problem.message}")
            }
            // A command that stops early closes its input, which is its failure to report, not this one's.
            try {
                process.outputStream.use { if (input != null) it.write(input) }
            } catch (_: IOException) {
            }
            val output = process.inputStream.readBytes().toString(Charsets.UTF_8)
            if (process.waitFor() != 0) {
                throw VoiceFailure("${command.first()} failed: ${errors.readText().trim().lineSequence().lastOrNull().orEmpty()}")
            }
            return output
        } finally {
            errors.delete()
        }
    }
}
