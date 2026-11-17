package com.polisheasy.engine

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The book's class recordings: the track list and line timings in the content, the import of the
 * publisher's zips on the phone, step mode and the listening tasks. The recordings themselves are
 * stood in for by tiny MP3s that carry only the same tags, since the real ones never leave the phone.
 */
class ClassAudioTest {
    private val contentDir = File(System.getProperty("contentDir"))
    private val course = loadCourse { path -> File(contentDir, path).readText() }
    private val phone = createTempDirectory("class-audio").toFile()
    private val audio = ClassAudio(course, File(phone, "class-audio"))

    @AfterTest
    fun cleanUp() {
        phone.deleteRecursively()
    }

    @Test
    fun `the content lists all 271 recordings, each with its chapter, its lines and a title, and every chapter has some`() {
        assertEquals((1..271).toList(), course.tracks.map { it.number })
        assertEquals((0..26).toSet(), course.tracks.map { it.lesson }.toSet())
        course.tracks.forEach { track ->
            val lines = course.trackLines[track.number].orEmpty()
            assertTrue(lines.isNotEmpty(), "Recording ${track.number} has lines for step mode")
            assertTrue(lines.all { it.endMs > it.startMs && it.endMs <= track.lengthMs + 1_000 }, "Recording ${track.number}'s lines are within it")
            assertTrue(track.title.isNotBlank())
        }
        // The book's exercise number, for the Słuchaj list.
        assertEquals(4, course.tracks.first { it.number == 6 }.exercise)
        assertNull(course.tracks.first { it.number == 4 }.exercise, "Dialog 1 is no exercise")
    }

    @Test
    fun `every chapter's Słuchaj lists its recordings by section in the book's order, before any is imported`() {
        val chapter1 = audio.listen(1)
        assertEquals(listOf("A", "B", "C", "D", "E", "F", "P"), chapter1.sections.map { it.letter })
        assertEquals(listOf(4, 5, 6), chapter1.sections.first().recordings.map { it.number })
        assertEquals(25, chapter1.total)
        assertEquals(0, chapter1.imported)
        assertTrue(chapter1.sections.flatMap { it.recordings }.none { it.imported })

        // Chapter 0 has no section letters; its sections come from the app, so its recordings stand alone.
        val chapter0 = audio.listen(0)
        assertEquals(listOf(null), chapter0.sections.map { it.letter })
        assertEquals(listOf(1, 2, 3), chapter0.sections.single().recordings.map { it.number })

        (0..26).forEach { lesson ->
            val listen = audio.listen(lesson)
            assertTrue(listen.total > 0, "Chapter $lesson has recordings")
            assertEquals(course.tracks.filter { it.lesson == lesson }.map { it.number }, listen.sections.flatMap { it.recordings }.map { it.number })
        }
    }

    @Test
    fun `importing the 27 zips matches 271 of 271, and importing them again replaces them`() {
        val zips = publishersZips()
        assertEquals(27, zips.size)

        val result = zips.fold(ImportResult.NONE) { sum, zip -> sum + audio.importZip(ByteArrayInputStream(zip)) }

        assertEquals(271, result.matched)
        assertEquals(emptyList(), result.unmatched)
        assertEquals(ClassAudioView(271, 271, File(phone, "class-audio").listFiles()!!.sumOf { it.length() }), audio.view())
        assertEquals((1..271).toSet(), audio.imported())
        assertEquals(25, audio.listen(1).imported)
        assertTrue(audio.listen(1).sections.flatMap { it.recordings }.all { it.imported })

        val again = audio.importZip(ByteArrayInputStream(zips[5]))
        assertEquals(course.tracks.count { it.lesson == 5 }, again.matched)
        assertEquals(271, audio.view().imported)
        assertEquals(271, File(phone, "class-audio").listFiles()!!.size, "Nothing but the recordings is left behind")
    }

    @Test
    fun `a recording is matched by its title tag, or by its file name when it has no tag, and a stray file is not kept`() {
        val noTag = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x00)
        assertEquals(1, audio.importFile("064.mp3", ByteArrayInputStream(noTag)).matched)
        assertEquals(setOf(64), audio.imported())

        // The tag wins over the name.
        assertEquals(1, audio.importFile("track.mp3", ByteArrayInputStream(mp3("065 Lekcja 05B, Ćwiczenie 1"))).matched)
        assertTrue(65 in audio.imported())

        // A tag whose chapter is not the track's is not this book's recording.
        val wrong = audio.importFile("066.mp3", ByteArrayInputStream(mp3("066 Lekcja 07B, Dialog 1")))
        assertEquals(ImportResult(0, listOf("066.mp3")), wrong)
        assertEquals(ImportResult(0, listOf("notes.mp3")), audio.importFile("notes.mp3", ByteArrayInputStream(noTag)))
        assertEquals(ImportResult.NONE, audio.importFile("cover.jpg", ByteArrayInputStream(noTag)))
        assertEquals(setOf(64, 65), audio.imported())
        assertEquals(2, File(phone, "class-audio").listFiles()!!.size, "A file that matched nothing is not kept")

        audio.removeAll()
        assertEquals(ClassAudioView(0, 271, 0), audio.view())
    }

    @Test
    fun `a folder of recordings, or a zip of zips, is imported the same way`() {
        val folder = publishersZips().take(2)
        val outer = ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                folder.forEachIndexed { index, inner ->
                    zip.putNextEntry(ZipEntry("Klasa/lekcja_0$index.zip"))
                    zip.write(inner)
                    zip.closeEntry()
                }
            }
        }.toByteArray()
        val result = audio.importFile("all.zip", ByteArrayInputStream(outer))
        assertEquals(course.tracks.count { it.lesson <= 1 }, result.matched)
    }

    @Test
    fun `the title tag is read from ID3 2·3 and 2·4 in each text encoding`() {
        assertEquals("064 Lekcja 05A, Ćwiczenie 1", id3Title(ByteArrayInputStream(mp3("064 Lekcja 05A, Ćwiczenie 1"))))
        assertEquals("Dzień dobry", id3Title(ByteArrayInputStream(mp3("Dzień dobry", version = 4, encoding = 3))))
        assertEquals("Lekcja", id3Title(ByteArrayInputStream(mp3("Lekcja", encoding = 0))))
        assertNull(id3Title(ByteArrayInputStream(byteArrayOf(1, 2, 3))))
        assertNull(id3Title(ByteArrayInputStream(mp3(null))))
    }

    @Test
    fun `step mode plays a line, then waits a little longer than it took for the Learner to say it`() {
        val short = TrackLine(1_000, 1_400)
        val long = TrackLine(0, 4_000)
        assertEquals(ListenAndRepeat.SHORTEST_PAUSE_MS, ListenAndRepeat.pauseAfter(short))
        assertEquals(5_600, ListenAndRepeat.pauseAfter(long))
        // Slowed down, the line takes longer to hear, so the pause is longer too.
        assertEquals(7_200, ListenAndRepeat.pauseAfter(long, speed = 0.75f))
        assertEquals(ListenAndRepeat.LONGEST_PAUSE_MS, ListenAndRepeat.pauseAfter(TrackLine(0, 20_000)))

        val lines = course.trackLines.getValue(1)
        assertEquals(0, ListenAndRepeat.lineAt(lines, 0))
        assertEquals(1, ListenAndRepeat.lineAt(lines, lines[0].endMs + 1))
        assertNull(ListenAndRepeat.lineAt(lines, lines.last().endMs + 1))

        val play = audio.play(5)
        assertEquals(4, play.previous)
        assertEquals(6, play.next)
        assertEquals(course.trackLines.getValue(5), play.lines)
        assertNull(audio.play(4).previous, "The chapter's first recording has nothing before it")
    }

    @Test
    fun `a listening task is checked, true or false, a choice, or a dictation typed as an answer is`() {
        val tasks = course.copy(
            listening = listOf(
                TrueFalseTask("t1", 4, "Pani ma na imię Anna.", mapOf("en" to "Her name is Anna."), answer = true, lines = listOf(1, 2)),
                ChooseTask("t2", 4, "Skąd on jest?", options = listOf("z Polski", "z Hiszpanii"), correctOption = 1),
                DictationTask("t3", 5, "Jaki to numer?", acceptedAnswers = listOf("siedem")),
            ),
        )
        val withTasks = ClassAudio(tasks, File(phone, "class-audio"))

        assertEquals(2, withTasks.listen(1).sections.first().recordings.first().tasks)
        val shown = withTasks.play(4).tasks
        assertEquals(ListeningTaskView.TrueFalse("t1", "Pani ma na imię Anna.", "Her name is Anna.", 1..2), shown[0])
        assertEquals(listOf("z Polski", "z Hiszpanii"), (shown[1] as ListeningTaskView.Choose).options)

        assertTrue(withTasks.check("t1", ListeningAnswer.TrueFalse(true)).correct)
        assertEquals(Feedback(false, "P"), withTasks.check("t1", ListeningAnswer.TrueFalse(false)))
        assertEquals(Feedback(false, "z Hiszpanii"), withTasks.check("t2", ListeningAnswer.Choose(0)))
        assertTrue(withTasks.check("t3", ListeningAnswer.Typed("Siedem!")).correct)
        assertTrue(withTasks.check("t3", ListeningAnswer.Typed("siedem")).correct)
        assertFalse(withTasks.check("t3", ListeningAnswer.Typed("osiem")).correct)
    }

    @Test
    fun `a listening task the engine could not run is rejected when the course loads`() {
        val broken = """
            {"tasks": [
              {"type": "choose", "id": "a", "track": 4, "question": "Kto?", "options": ["on"], "correctOption": 0},
              {"type": "choose", "id": "b", "track": 4, "question": "Kto?", "options": ["on", "ona"], "correctOption": 2},
              {"type": "dictation", "id": "c", "track": 5, "question": "Ile?", "acceptedAnswers": []},
              {"type": "true_false", "id": "d", "track": 999, "statement": "Tak.", "answer": true},
              {"type": "true_false", "id": "e", "track": 4, "statement": "Tak.", "answer": true, "lines": [1, 99]},
              {"type": "true_false", "id": "e", "track": 4, "statement": " ", "answer": false}
            ]}
        """.trimIndent()
        val failure = assertFailsWith<IllegalArgumentException> {
            loadCourse { path -> if (path == "kpk1/listening.json") broken else File(contentDir, path).readText() }
        }
        val message = failure.message.orEmpty()
        listOf(
            "Listening task a has fewer than two options",
            "Listening task b has correctOption 2, which is not one of its 2 options",
            "Listening task c has no Accepted Answers",
            "Listening task d is about class recording 999, which is not listed",
            "Listening task e gives the lines [1, 99]",
            "More than one listening task has the id e",
            "Listening task e has no statement",
        ).forEach { expected -> assertTrue(expected in message, "\"$expected\" in:\n$message") }
    }

    @Test
    fun `an MP3 in the content fails the content check`() {
        val files = object : ContentFiles {
            val real = DirectoryContent(contentDir)
            override fun read(path: String) = real.read(path)
            override fun paths() = real.paths() + "kpk1/audio/064.mp3"
        }
        val problems = checkContent(files)
        assertTrue(problems.any { it.file == "kpk1/audio/064.mp3" && "ADR-0014" in it.message }, problems.joinToString("\n"))
    }

    @Test
    fun `no MP3 is anywhere in the repository`() {
        val root = contentDir.parentFile
        val skipped = setOf(".git", ".gradle", "build", ".idea", ".kotlin")
        val found = root.walkTopDown()
            .onEnter { it.name !in skipped }
            .filter { it.isFile && it.extension.equals("mp3", ignoreCase = true) }
            .map { it.relativeTo(root).path }
            .toList()
        assertEquals(emptyList(), found, "The class recordings stay on the phone (ADR-0014)")
    }

    /** The publisher's 27 zips, each a folder "Lekcja NN" of recordings, stood in for by tag-only MP3s. */
    private fun publishersZips(): List<ByteArray> = course.tracks.groupBy { it.lesson }.toSortedMap().map { (lesson, tracks) ->
        ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                val folder = "Lekcja %02d".format(lesson)
                tracks.forEach { track ->
                    zip.putNextEntry(ZipEntry("$folder/%03d.mp3".format(track.number)))
                    val section = track.section.orEmpty()
                    zip.write(mp3("%03d Lekcja %02d%s, %s".format(track.number, lesson, section, track.title)))
                    zip.closeEntry()
                }
                zip.putNextEntry(ZipEntry("__MACOSX/$folder/._001.mp3"))
                zip.write(byteArrayOf(0))
                zip.closeEntry()
            }
        }.toByteArray()
    }

    /** A tiny MP3: an ID3 tag with an album frame and, when [title] is given, a title frame, then one frame of sound. */
    private fun mp3(title: String?, version: Int = 3, encoding: Int = 1): ByteArray {
        fun frame(id: String, text: String, encoding: Int): ByteArray {
            val body = byteArrayOf(encoding.toByte()) + when (encoding) {
                0 -> text.toByteArray(Charsets.ISO_8859_1)
                1 -> text.toByteArray(Charsets.UTF_16LE).let { byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + it + byteArrayOf(0, 0) }
                else -> text.toByteArray(Charsets.UTF_8)
            }
            val size = body.size
            val sizeBytes = if (version == 4) {
                byteArrayOf((size shr 21 and 0x7f).toByte(), (size shr 14 and 0x7f).toByte(), (size shr 7 and 0x7f).toByte(), (size and 0x7f).toByte())
            } else {
                byteArrayOf((size shr 24).toByte(), (size shr 16).toByte(), (size shr 8).toByte(), size.toByte())
            }
            return id.toByteArray(Charsets.ISO_8859_1) + sizeBytes + byteArrayOf(0, 0) + body
        }
        val frames = frame("TALB", "Polski krok po kroku", 0) + (title?.let { frame("TIT2", it, encoding) } ?: ByteArray(0)) + ByteArray(16)
        val size = frames.size
        val header = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), version.toByte(), 0, 0) +
            byteArrayOf((size shr 21 and 0x7f).toByte(), (size shr 14 and 0x7f).toByte(), (size shr 7 and 0x7f).toByte(), (size and 0x7f).toByte())
        return header + frames + byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x00)
    }
}
