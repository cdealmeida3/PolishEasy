package com.polisheasy.audio

import com.polisheasy.engine.Clip
import com.polisheasy.engine.DirectoryContent
import com.polisheasy.engine.spokenContent
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The environment variable the voice service's key is read from. */
const val KEY_VARIABLE = "GOOGLE_TTS_API_KEY"
const val CONTENT_DIR = "content"
const val VOICE_FILE = "audio-voice.json"
const val RECORDINGS_FILE = "audio-recordings.json"

/** Where the owner of the project reads how to get the key. */
const val KEY_DOCUMENT = "docs/audio.md, \"Getting the key\","

private const val USAGE = "Usage: [--dry-run | --voices]"
private const val DRY_RUN = "--dry-run"
private const val VOICES = "--voices"

/** A Clip outside this range is silence or a runaway, not speech. */
private val SENSIBLE = 0.2.seconds..20.seconds

/**
 * A Clip whose loudest moment is quieter than this is silence. The voice now and then answers a
 * one-syllable word with a quarter of a second of nothing; speech peaks above -10 dB.
 */
private const val SILENT_BELOW_DB = -30.0

/** How many times the voice is asked for one text before its silence stops the run. */
private const val ASKS = 6

/** How many of the shortest Clips a run lists, to be listened to first. */
private const val SHORTEST = 10

@Serializable
internal data class Manifest(val clips: List<Clip> = emptyList())

/** For each Clip id, the fingerprint of the text and voice settings that made its file. */
@Serializable
internal data class Recordings(val recordings: Map<String, String> = emptyMap())

// Two spaces, as the manifest has always been written, so a run that changes nothing leaves no diff.
@OptIn(ExperimentalSerializationApi::class)
internal val fileJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

/** What [file] holds, [empty] when it is not there, or null when it cannot be read. */
private inline fun <reified T> read(file: File, empty: T?): T? = try {
    if (file.isFile) fileJson.decodeFromString<T>(file.readText()) else empty
} catch (_: IllegalArgumentException) {
    // A SerializationException is one of these.
    null
}

private fun seconds(duration: Duration) = String.format(Locale.ROOT, "%.2f s", duration.toDouble(DurationUnit.SECONDS))

private fun count(number: Int, one: String) = "$number ${if (number == 1) one else one + "s"}"

/**
 * The command that makes the Clips, as `./gradlew :audio:makeClips` runs it. [root] is the
 * repository: the content is in `content/`, the voice settings in `audio-voice.json` and the
 * fingerprints in `audio-recordings.json`. [key] is the voice service's key, or null when the
 * environment has none; it is needed only when there is something to make.
 *
 * Returns the exit code: 0 when every Clip is in place (or, with `--dry-run`, always once the
 * plan is printed), 1 when something stopped the run, 2 when [args] are wrong.
 */
fun runMakeClips(
    args: List<String>,
    root: File,
    key: String?,
    service: (VoiceSettings, String) -> VoiceService,
    encoder: (VoiceSettings) -> Encoder,
    out: PrintStream,
    err: PrintStream,
): Int {
    if (args.size > 1 || args.any { it != DRY_RUN && it != VOICES }) {
        err.println(USAGE)
        return 2
    }
    val secret = key?.trim()?.takeIf { it.isNotEmpty() }

    /** [text] with the key blanked, in case a service or a proxy echoes it. */
    fun safe(text: String) = if (secret == null) text else text.replace(secret, "[the key]")

    fun stop(message: String): Int {
        err.println(safe(message))
        return 1
    }

    val settings = read<VoiceSettings>(File(root, VOICE_FILE), null)
        ?: return stop("$VOICE_FILE, which says which voice makes the Clips, is missing or cannot be read. Nothing was changed.")
    if (settings.service != GOOGLE_SERVICE) {
        return stop("$VOICE_FILE names the service \"${settings.service}\", and the only one this command can call is \"$GOOGLE_SERVICE\". Nothing was changed.")
    }

    if (VOICES in args) {
        if (secret == null) {
            return stop("Listing the voices needs the voice service key, and $KEY_VARIABLE is not set. $KEY_DOCUMENT says how to get one and where to put it.")
        }
        val names = try {
            service(settings, secret).names()
        } catch (failure: VoiceFailure) {
            return stop(failure.message.orEmpty())
        }
        names.forEach { out.println(if (it == settings.voice) "$it   <- in $VOICE_FILE" else it) }
        if (settings.voice !in names) {
            return stop("The service has no ${settings.languageCode} voice called \"${settings.voice}\", which is the one in $VOICE_FILE.")
        }
        return 0
    }

    val contentDir = File(root, CONTENT_DIR)
    val content = spokenContent(DirectoryContent(contentDir)::read)
    if (content.unreadable.isNotEmpty()) {
        content.unreadable.forEach(err::println)
        return stop("The content cannot be read, so no Clip was made or removed. Run ./gradlew :engine:checkContent")
    }
    val manifestPath = content.manifest
        ?: return stop("course.json names no file that lists the Clips (\"audio\": \"audio/clips.json\"). Nothing was changed.")
    val manifestFile = File(contentDir, manifestPath)
    val audioDir = manifestFile.absoluteFile.parentFile
    val recordingsFile = File(root, RECORDINGS_FILE)
    val manifest = read(manifestFile, Manifest())
        ?: return stop("$manifestPath cannot be read. Nothing was changed.")
    val recordings = read(recordingsFile, Recordings())
        ?: return stop("$RECORDINGS_FILE cannot be read. Nothing was changed. If it cannot be mended, delete it: every Clip is then made again.")

    fun fileOf(id: String) = File(audioDir, "$id.ogg")
    fun audioFiles() = audioDir.listFiles { file -> file.isFile && file.extension == "ogg" }.orEmpty().map { it.nameWithoutExtension }.toSet()

    val plan = planClips(content.texts, manifest.clips, recordings.recordings, audioFiles(), settings::fingerprint)
    val fresh = plan.make.count { it.reason == Reason.NEW }
    val again = plan.make.size - fresh
    out.println(
        "${count(content.texts.size, "spoken text")}: ${plan.keep.size} to keep, ${plan.make.size} to make " +
            "($fresh new, $again to record again), ${count(plan.remove.size, "Clip")} and ${count(plan.strays.size, "stray file")} to remove.",
    )
    out.println("${count(plan.characters, "character")} to send to the voice ${settings.voice}.")
    if (DRY_RUN in args) {
        plan.make.filter { it.reason == Reason.NEW }.forEach { out.println("Would add ${it.clip.id}: ${it.clip.text}") }
        plan.remove.forEach { out.println("Would remove ${it.id}: ${it.text}") }
        plan.strays.forEach { out.println("Would remove the stray file $it.ogg") }
        out.println("Dry run: nothing was sent and nothing was changed.")
        return 0
    }
    if (plan.nothingToDo) {
        out.println("Every Clip is in place. Nothing was sent and nothing was changed.")
        return 0
    }

    var voice: Voice? = null
    val encode = encoder(settings)
    if (plan.make.isNotEmpty()) {
        if (secret == null) {
            return stop(
                "Making ${count(plan.make.size, "Clip")} needs the voice service key, and $KEY_VARIABLE is not set. " +
                    "$KEY_DOCUMENT says how to get one and where to put it. Nothing was changed. --dry-run shows what would be made.",
            )
        }
        encode.problem()?.let { return stop("$it. Nothing was sent and nothing was changed.") }
        voice = service(settings, secret)
    }

    // Make. A failure stops the making but not the tidying, so what was finished is kept and recorded.
    audioDir.mkdirs()
    val made = mutableListOf<Pair<ClipToMake, Duration>>()
    var failure: String? = null
    for (wanted in plan.make) {
        val clip = wanted.clip
        // Not ".ogg", so a run that is killed never leaves half a Clip that looks whole.
        val part = File(audioDir, "${clip.id}.part")
        try {
            // The voice does not answer the same way twice, so silence is worth asking about again.
            var measured: Measured
            var asked = 0
            do {
                measured = encode.encode(checkNotNull(voice).speak(clip.text), part.toPath())
                asked++
            } while (measured.peakDb < SILENT_BELOW_DB && asked < ASKS)
            if (measured.peakDb < SILENT_BELOW_DB) {
                throw VoiceFailure("The voice answered \"${clip.text}\" with silence $ASKS times running")
            }
            val length = measured.length
            if (length !in SENSIBLE) throw VoiceFailure("The Clip of \"${clip.text}\" is ${seconds(length)} long, which cannot be right")
            Files.move(part.toPath(), fileOf(clip.id).toPath(), StandardCopyOption.REPLACE_EXISTING)
            made += wanted to length
        } catch (problem: VoiceFailure) {
            failure = problem.message
        } catch (problem: IOException) {
            failure = "The file of Clip ${clip.id} could not be written: ${problem.message}"
        } finally {
            part.delete()
        }
        if (failure != null) break
    }

    // A Clip that was listed and is still spoken stays listed even if its turn never came: its old file is still the best there is.
    val madeIds = made.map { it.first.clip.id }.toSet()
    val waiting = plan.make.filter { it.clip.id !in madeIds }
    val clips = (plan.keep + made.map { it.first.clip } + waiting.filter { it.reason != Reason.NEW }.map { it.clip }).sortedBy { it.id }
    val listed = clips.map { it.id }.toSet()
    val removedFiles = (audioFiles() - listed).sorted().onEach { fileOf(it).delete() }

    val fingerprints = (plan.keep.map { it.id to recordings.recordings.getValue(it.id) } + made.map { (wanted, _) -> wanted.clip.id to settings.fingerprint(wanted.clip.text) })
        .sortedBy { it.first }.toMap()
    manifestFile.writeText(fileJson.encodeToString(Manifest.serializer(), Manifest(clips)) + "\n")
    recordingsFile.writeText(fileJson.encodeToString(Recordings.serializer(), Recordings(fingerprints)) + "\n")

    val added = made.filter { it.first.reason == Reason.NEW }
    added.forEach { (wanted, _) -> out.println("Added ${wanted.clip.id}: ${wanted.clip.text}") }
    plan.remove.forEach { out.println("Removed ${it.id}: ${it.text}") }
    (removedFiles - plan.remove.map { it.id }.toSet()).forEach { out.println("Removed the stray file $it.ogg") }
    out.println(
        "${count(clips.size, "Clip")}: ${added.size} added, ${made.size - added.size} recorded again, " +
            "${plan.remove.size} removed, ${plan.keep.size} kept.",
    )
    if (made.isNotEmpty()) {
        out.println("The shortest, to listen to first:")
        made.sortedBy { it.second }.take(SHORTEST).forEach { (wanted, length) -> out.println("  ${seconds(length)}  ${wanted.clip.id}: ${wanted.clip.text}") }
    }

    if (failure != null) {
        return stop(
            "Stopped: $failure. ${made.size} of ${count(plan.make.size, "Clip")} made and recorded; ${waiting.size} left. " +
                "Run the command again to carry on from here.",
        )
    }

    out.println("Next:")
    if (added.isNotEmpty()) {
        out.println("  ./gradlew :engine:recordContentIds   (if Exercises or Items were added)")
        out.println("  ./gradlew :engine:checkContent")
        out.println("  ./gradlew :polishcheck:checkPolish   (new Polish needs a recorded review, or CI fails: docs/polish-check.md)")
    }
    out.println("  Commit ${CONTENT_DIR}/${manifestPath.substringBeforeLast('/', "")}/ and $RECORDINGS_FILE.")
    return 0
}
