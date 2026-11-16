package com.polisheasy.audio

import com.polisheasy.engine.Clip
import com.polisheasy.engine.DirectoryContent
import com.polisheasy.engine.checkContent
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** The command as `./gradlew :audio:makeClips` runs it: against a folder, with the voice service and the encoder faked. */
internal class MakeClipsCommandTest {
    private val root: File = createTempDirectory("make-clips").toFile()
    private val contentDir = File(root, CONTENT_DIR)
    private val audioDir = File(contentDir, "audio")
    private val manifestFile = File(audioDir, "clips.json")
    private val recordingsFile = File(root, RECORDINGS_FILE)
    private val voiceFile = File(root, VOICE_FILE)
    private val day = File(contentDir, "a1/unit-01/day-01.json")
    private val key = "sekret-klucz-123"

    /** Every text the fake voice was asked to speak, in order. */
    private val sent = mutableListOf<String>()

    /** Texts the fake voice fails on, with what it says. */
    private val refused = mutableMapOf<String, String>()

    /** How long the fake encoder says the Clip of a text is. */
    private val lengths = mutableMapOf<String, Duration>()
    private var voicesListed = 0

    private val voice = object : VoiceService {
        override fun speak(text: String): ByteArray {
            sent += text
            refused[text]?.let { throw VoiceFailure(it) }
            return text.toByteArray()
        }

        override fun names(): List<String> {
            voicesListed++
            return listOf("pl-PL-Chirp3-HD-Aoede", "pl-PL-Wavenet-A")
        }
    }

    /** Writes the voice's name and the text into the file, so a test can tell which voice made it. */
    private fun encoder(settings: VoiceSettings) = Encoder { wav: ByteArray, target: Path ->
        val text = wav.toString(Charsets.UTF_8)
        Files.writeString(target, "OggS ${settings.voice} $text")
        lengths[text] ?: 1.seconds
    }

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    private class Run(val exitCode: Int, val out: String, val err: String)

    private fun run(vararg args: String, withKey: Boolean = true): Run {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        sent.clear()
        val exitCode = runMakeClips(
            args.toList(),
            root,
            key.takeIf { withKey },
            { _, _ -> voice },
            ::encoder,
            PrintStream(out, true, Charsets.UTF_8),
            PrintStream(err, true, Charsets.UTF_8),
        )
        return Run(exitCode, out.toString(Charsets.UTF_8), err.toString(Charsets.UTF_8))
    }

    private fun writeVoice(name: String = "pl-PL-Chirp3-HD-Aoede") = voiceFile.writeText(
        """
        {"service": "google-cloud-text-to-speech", "voice": "$name", "languageCode": "pl-PL", "sampleRateHertz": 24000,
         "encoding": {"codec": "opus", "bitrateKbps": 32, "leadMs": 80, "tailMs": 200, "fadeOutMs": 20}}
        """.trimIndent(),
    )

    /** Writes a course whose one Daily Set speaks [texts], each as the prompt of a choice Exercise. */
    private fun writeCourse(vararg texts: String) {
        writeVoice()
        File(contentDir, "course.json").apply { parentFile.mkdirs() }.writeText(
            """
            {"levels": [{"id": "a1", "title": {"en": "A1"}, "units": [
              {"id": "a1-u01", "title": {"en": "Sample"}, "dailySets": ["a1/unit-01/day-01.json"]}]}],
             "audio": "audio/clips.json"}
            """.trimIndent(),
        )
        val exercises = texts.mapIndexed { index, text ->
            """{"type": "choose_translation", "id": "a1-u01-d01-e%02d", "prompt": "%s", "options": [{"en": "this"}, {"en": "that"}], "correctOption": 0, "items": []}"""
                .format(index + 1, text)
        }
        day.apply { parentFile.mkdirs() }.writeText("""{"id": "a1-u01-d01", "exercises": [${exercises.joinToString(",\n")}]}""")
    }

    /** Writes the course and makes its Clips, as an earlier run would have. */
    private fun courseWithClips(vararg texts: String) {
        writeCourse(*texts)
        assertEquals(0, run().exitCode)
    }

    private fun clips(): List<Clip> = fileJson.decodeFromString<Manifest>(manifestFile.readText()).clips

    private fun recordings(): Map<String, String> = fileJson.decodeFromString<Recordings>(recordingsFile.readText()).recordings

    private fun audioFiles(): List<String> = audioDir.list().orEmpty().filter { it != "clips.json" }.sorted()

    /** Everything on disk, to show that a run changed nothing. */
    private fun snapshot(): Map<String, String> =
        root.walkTopDown().filter { it.isFile }.associate { it.relativeTo(root).path to "${it.lastModified()} ${it.readText()}" }

    @Test
    fun `a course with no audio gets a Clip, a file and a fingerprint for each text it speaks`() {
        writeCourse("Dzień dobry", "tak", "Jak się masz?")

        val run = run()

        assertEquals(0, run.exitCode, run.err)
        assertEquals(listOf("Dzień dobry", "tak", "Jak się masz?"), sent)
        assertEquals(
            listOf(Clip("dzien-dobry", "Dzień dobry"), Clip("jak-sie-masz", "Jak się masz?"), Clip("tak", "tak")),
            clips(),
        )
        assertEquals(listOf("dzien-dobry.ogg", "jak-sie-masz.ogg", "tak.ogg"), audioFiles())
        assertEquals(setOf("dzien-dobry", "jak-sie-masz", "tak"), recordings().keys)
        assertTrue("3 Clips: 3 added, 0 recorded again, 0 removed, 0 kept." in run.out, run.out)
    }

    @Test
    fun `the manifest is written as it always was, two spaces deep and sorted by id`() {
        courseWithClips("tak", "Dzień dobry")

        assertEquals(
            """
            {
              "clips": [
                {
                  "id": "dzien-dobry",
                  "text": "Dzień dobry"
                },
                {
                  "id": "tak",
                  "text": "tak"
                }
              ]
            }

            """.trimIndent(),
            manifestFile.readText(),
        )
    }

    @Test
    fun `running again sends nothing and touches no file`() {
        courseWithClips("tak", "nie")
        val before = snapshot()

        val run = run()

        assertEquals(0, run.exitCode, run.err)
        assertEquals(emptyList(), sent)
        assertEquals(before, snapshot())
        assertTrue("Nothing was sent" in run.out, run.out)
    }

    @Test
    fun `only a text added to the content is sent`() {
        courseWithClips("tak", "nie")
        val takBefore = File(audioDir, "tak.ogg").lastModified()

        writeCourse("tak", "nie", "proszę")
        val run = run()

        assertEquals(0, run.exitCode, run.err)
        assertEquals(listOf("proszę"), sent)
        assertEquals(listOf("nie", "prosze", "tak"), clips().map { it.id })
        assertEquals(takBefore, File(audioDir, "tak.ogg").lastModified())
        assertTrue("Added prosze: proszę" in run.out, run.out)
    }

    @Test
    fun `a run that added Clips says to check the content and the Polish, and a run that only recorded again says only to commit`() {
        writeCourse("tak")
        val added = run()
        writeVoice("pl-PL-Wavenet-A")
        val recordedAgain = run()

        assertTrue(":engine:recordContentIds" in added.out && ":engine:checkContent" in added.out, added.out)
        assertTrue(":polishcheck:checkPolish" in added.out, added.out)
        assertTrue("Commit content/audio/ and audio-recordings.json" in added.out, added.out)
        assertFalse("checkPolish" in recordedAgain.out, recordedAgain.out)
        assertTrue("Commit content/audio/ and audio-recordings.json" in recordedAgain.out, recordedAgain.out)
    }

    @Test
    fun `a text whose punctuation changes is sent anew, and the old Clip, file and fingerprint go`() {
        courseWithClips("Jak się masz", "tak")
        val oldFingerprint = recordings().getValue("jak-sie-masz")

        writeCourse("Jak się masz?", "tak")
        val run = run()

        assertEquals(0, run.exitCode, run.err)
        assertEquals(listOf("Jak się masz?"), sent)
        assertEquals(listOf(Clip("jak-sie-masz", "Jak się masz?"), Clip("tak", "tak")), clips())
        assertEquals(listOf("jak-sie-masz.ogg", "tak.ogg"), audioFiles())
        assertTrue(File(audioDir, "jak-sie-masz.ogg").readText().endsWith("Jak się masz?"))
        assertTrue(recordings().getValue("jak-sie-masz") != oldFingerprint)
    }

    @Test
    fun `a text that differs only in capitals or spacing keeps its Clip and is not sent`() {
        courseWithClips("Dzień dobry")

        writeCourse("dzień  dobry")
        val run = run()

        assertEquals(0, run.exitCode, run.err)
        assertEquals(emptyList(), sent)
        assertEquals(listOf(Clip("dzien-dobry", "Dzień dobry")), clips())
    }

    @Test
    fun `a text no longer spoken loses its Clip, file and fingerprint, and nothing is sent`() {
        courseWithClips("tak", "nie")

        writeCourse("tak")
        val run = run(withKey = false)

        assertEquals(0, run.exitCode, run.err)
        assertEquals(emptyList(), sent)
        assertEquals(listOf(Clip("tak", "tak")), clips())
        assertEquals(listOf("tak.ogg"), audioFiles())
        assertEquals(setOf("tak"), recordings().keys)
        assertTrue("Removed nie: nie" in run.out, run.out)
    }

    @Test
    fun `an audio file no Clip names is removed`() {
        courseWithClips("tak")
        File(audioDir, "stary.ogg").writeText("OggS")

        val run = run(withKey = false)

        assertEquals(0, run.exitCode, run.err)
        assertEquals(listOf("tak.ogg"), audioFiles())
        assertTrue("Removed the stray file stary.ogg" in run.out, run.out)
    }

    @Test
    fun `a Clip whose file was deleted by hand is the only one sent, and keeps its id`() {
        writeCourse("Tak, proszę.", "nie")
        audioDir.mkdirs()
        // An id no slug would give, to show that it is kept and not worked out again.
        manifestFile.writeText("""{"clips": [{"id": "c7", "text": "Tak, proszę."}]}""")
        assertEquals(0, run().exitCode)
        File(audioDir, "c7.ogg").delete()

        val run = run()

        assertEquals(0, run.exitCode, run.err)
        assertEquals(listOf("Tak, proszę."), sent)
        assertEquals(listOf("c7", "nie"), clips().map { it.id })
        assertTrue(File(audioDir, "c7.ogg").isFile)
    }

    @Test
    fun `another voice in the settings file sends every Clip again under the same ids`() {
        courseWithClips("tak", "nie")
        val before = clips()

        writeVoice("pl-PL-Wavenet-A")
        val run = run()

        assertEquals(0, run.exitCode, run.err)
        assertEquals(setOf("tak", "nie"), sent.toSet())
        assertEquals(before, clips())
        assertEquals("OggS pl-PL-Wavenet-A tak", File(audioDir, "tak.ogg").readText())
        assertTrue("2 Clips: 0 added, 2 recorded again, 0 removed, 0 kept." in run.out, run.out)
    }

    @Test
    fun `laying the settings file out differently sends nothing`() {
        courseWithClips("tak")

        voiceFile.writeText(voiceFile.readText().replace(", ", ",\n  ").replace("\"encoding\"", "\"speakingRate\": 1.0, \"encoding\""))
        run()

        assertEquals(emptyList(), sent)
    }

    @Test
    fun `Clips that have files but no fingerprints, as the placeholder audio does, are all sent again under the same ids`() {
        writeCourse("tak", "nie")
        audioDir.mkdirs()
        manifestFile.writeText("""{"clips": [{"id": "nie", "text": "nie"}, {"id": "t1", "text": "tak"}]}""")
        File(audioDir, "nie.ogg").writeText("OggS piper")
        File(audioDir, "t1.ogg").writeText("OggS piper")

        val run = run()

        assertEquals(0, run.exitCode, run.err)
        assertEquals(setOf("tak", "nie"), sent.toSet())
        assertEquals(listOf("nie", "t1"), clips().map { it.id })
        assertEquals("OggS pl-PL-Chirp3-HD-Aoede tak", File(audioDir, "t1.ogg").readText())
        assertEquals(setOf("nie", "t1"), recordings().keys)
    }

    @Test
    fun `two texts with the same slug get x and x-2, and an id already given is never renumbered`() {
        courseWithClips("tak?")
        assertEquals(listOf(Clip("tak", "tak?")), clips())

        writeCourse("tak", "tak?", "tak!")
        run()

        assertEquals(listOf(Clip("tak", "tak?"), Clip("tak-2", "tak"), Clip("tak-3", "tak!")), clips())
    }

    @Test
    fun `without the key, something to make fails, names the variable and the document, and changes nothing`() {
        courseWithClips("tak")
        writeCourse("tak", "nie", "proszę")
        val before = snapshot()

        val run = run(withKey = false)

        assertEquals(1, run.exitCode)
        assertTrue("Making 2 Clips needs the voice service key, and GOOGLE_TTS_API_KEY is not set." in run.err, run.err)
        assertTrue("docs/audio.md, \"Getting the key\"," in run.err, run.err)
        assertTrue("Nothing was changed" in run.err, run.err)
        assertEquals(emptyList(), sent)
        assertEquals(before, snapshot())
    }

    @Test
    fun `a key of only spaces is no key`() {
        writeCourse("tak")
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()

        val exitCode = runMakeClips(emptyList(), root, "  ", { _, _ -> voice }, ::encoder, PrintStream(out), PrintStream(err))

        assertEquals(1, exitCode)
        assertTrue("GOOGLE_TTS_API_KEY is not set" in err.toString(), err.toString())
    }

    @Test
    fun `without the key, nothing to make passes`() {
        courseWithClips("tak")

        val run = run(withKey = false)

        assertEquals(0, run.exitCode, run.err)
        assertEquals("", run.err)
    }

    @Test
    fun `a dry run prints the counts and the characters, needs no key, sends nothing and changes nothing`() {
        courseWithClips("tak", "nie")
        writeCourse("tak", "Dzień dobry", "proszę")
        val before = snapshot()

        val run = run("--dry-run", withKey = false)

        assertEquals(0, run.exitCode, run.err)
        assertTrue("3 spoken texts: 1 to keep, 2 to make (2 new, 0 to record again), 1 Clip and 0 stray files to remove." in run.out, run.out)
        assertTrue("17 characters to send to the voice pl-PL-Chirp3-HD-Aoede." in run.out, run.out)
        assertTrue("Would add dzien-dobry: Dzień dobry" in run.out, run.out)
        assertTrue("Would remove nie: nie" in run.out, run.out)
        assertEquals(emptyList(), sent)
        assertEquals(before, snapshot())
    }

    @Test
    fun `when the voice fails part-way, what was finished is kept and recorded, and the next run sends only the rest`() {
        writeCourse("tak", "nie", "proszę")
        refused["nie"] = "Quota exceeded"

        val failed = run()

        assertEquals(1, failed.exitCode)
        assertEquals(listOf("tak", "nie"), sent)
        assertTrue("Stopped: Quota exceeded. 1 of 3 Clips made and recorded; 2 left." in failed.err, failed.err)
        assertEquals(listOf(Clip("tak", "tak")), clips())
        assertEquals(listOf("tak.ogg"), audioFiles())
        assertEquals(setOf("tak"), recordings().keys)

        refused.clear()
        val second = run()

        assertEquals(0, second.exitCode, second.err)
        assertEquals(listOf("nie", "proszę"), sent)
        assertEquals(listOf("nie", "prosze", "tak"), clips().map { it.id })
    }

    @Test
    fun `when recording again fails part-way, the Clips still waiting keep their old files and stay listed`() {
        courseWithClips("tak", "nie")
        writeVoice("pl-PL-Wavenet-A")
        // Clips are recorded again in the manifest's order, so "nie" is done before "tak" fails.
        refused["tak"] = "Quota exceeded"

        val failed = run()

        assertEquals(1, failed.exitCode)
        assertEquals(listOf("nie", "tak"), clips().map { it.id })
        assertEquals("OggS pl-PL-Chirp3-HD-Aoede tak", File(audioDir, "tak.ogg").readText())
        assertEquals(setOf("nie"), recordings().keys)

        refused.clear()
        run()

        assertEquals(listOf("tak"), sent)
        assertEquals("OggS pl-PL-Wavenet-A tak", File(audioDir, "tak.ogg").readText())
    }

    @Test
    fun `the key never appears in the output, even when the service's message contains it`() {
        writeCourse("tak")
        refused["tak"] = "API key $key is not valid"

        val run = run()

        assertEquals(1, run.exitCode)
        assertFalse(key in run.out + run.err, run.out + run.err)
        assertTrue("API key [the key] is not valid" in run.err, run.err)
    }

    @Test
    fun `a Clip that comes back far too short stops the run and leaves no file`() {
        writeCourse("tak", "nie")
        lengths["nie"] = 0.05.seconds

        val run = run()

        assertEquals(1, run.exitCode)
        assertTrue("The Clip of \"nie\" is 0.05 s long, which cannot be right" in run.err, run.err)
        assertEquals(listOf("tak.ogg"), audioFiles())
        assertEquals(listOf("tak"), clips().map { it.id })
    }

    @Test
    fun `a run lists the shortest Clips it made, shortest first`() {
        writeCourse("tak", "a", "Dzień dobry")
        lengths["a"] = 0.4.seconds
        lengths["tak"] = 0.6.seconds

        val run = run()

        val listed = run.out.lines().dropWhile { "The shortest" !in it }.drop(1).take(3).map { it.trim() }
        assertEquals(listOf("0.40 s  a: a", "0.60 s  tak: tak", "1.00 s  dzien-dobry: Dzień dobry"), listed)
    }

    @Test
    fun `after a run the content checker finds nothing wrong with the audio`() {
        writeCourse("Dzień dobry", "tak", "nie")
        run()
        writeCourse("Dzień dobry", "tak", "proszę")
        File(audioDir, "stary.ogg").writeText("OggS")
        run()

        val problems = checkContent(DirectoryContent(contentDir)).map { it.toString() }

        assertEquals(emptyList(), problems.filter { "Clip" in it || "audio" in it })
    }

    @Test
    fun `a Daily Set that cannot be read stops the run before any Clip is removed`() {
        courseWithClips("tak", "nie")
        day.writeText("{ not json")
        val before = snapshot()

        val run = run()

        assertEquals(1, run.exitCode)
        assertTrue("a1/unit-01/day-01.json: Cannot be read" in run.err, run.err)
        assertEquals(before, snapshot())
    }

    @Test
    fun `a missing settings file, or one that names another service, stops the run`() {
        writeCourse("tak")
        voiceFile.delete()
        val missing = run()
        voiceFile.writeText(
            """{"service": "azure", "voice": "x", "languageCode": "pl-PL", "sampleRateHertz": 24000,
                "encoding": {"codec": "opus", "bitrateKbps": 32, "leadMs": 80, "tailMs": 200, "fadeOutMs": 20}}""",
        )
        val other = run()

        assertEquals(1, missing.exitCode)
        assertTrue("audio-voice.json" in missing.err, missing.err)
        assertEquals(1, other.exitCode)
        assertTrue("\"azure\"" in other.err, other.err)
        assertEquals(emptyList(), sent)
    }

    @Test
    fun `an encoder that cannot work stops the run before anything is sent`() {
        writeCourse("tak")
        val err = ByteArrayOutputStream()
        val broken = object : Encoder {
            override fun encode(wav: ByteArray, target: Path): Duration = error("never asked")
            override fun problem() = "The ffmpeg command was not found"
        }

        val exitCode = runMakeClips(emptyList(), root, key, { _, _ -> voice }, { broken }, PrintStream(ByteArrayOutputStream()), PrintStream(err))

        assertEquals(1, exitCode)
        assertTrue("The ffmpeg command was not found. Nothing was sent" in err.toString(), err.toString())
        assertEquals(emptyList(), sent)
        assertFalse(manifestFile.exists())
    }

    @Test
    fun `--voices lists the service's voices and marks the one in use`() {
        writeCourse("tak")

        val run = run("--voices")

        assertEquals(0, run.exitCode, run.err)
        assertEquals(1, voicesListed)
        assertEquals(listOf("pl-PL-Chirp3-HD-Aoede   <- in audio-voice.json", "pl-PL-Wavenet-A"), run.out.trim().lines())
    }

    @Test
    fun `--voices fails when the voice in the settings file is not one the service offers, or when there is no key`() {
        writeCourse("tak")
        writeVoice("pl-PL-Nobody")

        val unknown = run("--voices")
        val noKey = run("--voices", withKey = false)

        assertEquals(1, unknown.exitCode)
        assertTrue("no pl-PL voice called \"pl-PL-Nobody\"" in unknown.err, unknown.err)
        assertEquals(1, noKey.exitCode)
        assertTrue("GOOGLE_TTS_API_KEY is not set" in noKey.err, noKey.err)
    }

    @Test
    fun `an unknown switch prints the usage`() {
        val run = run("--fast")

        assertEquals(2, run.exitCode)
        assertEquals("Usage: [--dry-run | --voices]", run.err.trim())
    }
}
