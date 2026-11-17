package com.polisheasy.engine

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One class recording of the book, as `content/kpk1/tracks.json` lists it: its [number] on the
 * publisher's discs (001 to 271), the chapter ([lesson]) and [section] letter it belongs to (A, B…,
 * P for Powtórzenie, none in chapter 0), the book's [exercise] number when it is an exercise, its
 * [title] as its tag gives it, and its length. The recording itself is never in the repository or
 * the app: the Learner imports it on the phone (ADR-0014).
 */
@Serializable
data class ClassTrack(
    val number: Int,
    val lesson: Int,
    val section: String? = null,
    val exercise: Int? = null,
    val title: String,
    val lengthMs: Long,
)

/** One line of a class recording, from one pause to the next: what step mode plays before the Learner repeats it. */
data class TrackLine(val startMs: Long, val endMs: Long) {
    val lengthMs: Long get() = endMs - startMs
}

@Serializable
internal data class TracksFile(val tracks: List<ClassTrack>)

/** Each track's lines, by track number, as `[start, end]` in milliseconds. */
@Serializable
internal data class SegmentsFile(val lines: Map<String, List<List<Long>>>)

@Serializable
internal data class ListeningFile(val tasks: List<ListeningTask>)

/**
 * A short task on a class recording, written for the app (never copied from the book): heard on
 * the Słuchaj part of a Lesson screen, never inside a Daily Set, so the Path never depends on the
 * import. It is about [track], or only its [lines] (first and last, counted from 1) when given.
 */
@Serializable
sealed interface ListeningTask {
    val id: String
    val track: Int
    val lines: List<Int>?
}

/** *Prawda czy nieprawda?*: is the Polish [statement] true of what was heard. */
@Serializable
@SerialName("true_false")
data class TrueFalseTask(
    override val id: String,
    override val track: Int,
    val statement: String,
    val meaning: Localized = emptyMap(),
    val answer: Boolean,
    override val lines: List<Int>? = null,
) : ListeningTask

/** A [question] in Polish with its [options], one of them right. */
@Serializable
@SerialName("choose")
data class ChooseTask(
    override val id: String,
    override val track: Int,
    val question: String,
    val meaning: Localized = emptyMap(),
    val options: List<String>,
    val correctOption: Int,
    override val lines: List<Int>? = null,
) : ListeningTask

/** *Co mówi lektor?*: a short dictation, such as a number, a name or a price, typed and checked like a typed answer. */
@Serializable
@SerialName("dictation")
data class DictationTask(
    override val id: String,
    override val track: Int,
    val question: String,
    val meaning: Localized = emptyMap(),
    val acceptedAnswers: List<String>,
    override val lines: List<Int>? = null,
) : ListeningTask

/** A recording as the Słuchaj part of a Lesson screen lists it. [imported] when its file is on the phone. */
data class RecordingView(
    val number: Int,
    val title: String,
    val exercise: Int?,
    val lengthMs: Long,
    val lines: Int,
    val tasks: Int,
    val imported: Boolean,
)

/** The recordings of one section of a chapter. [letter] as the book gives it, or null in chapter 0; [heading] its Polish heading when the app has the section. */
data class RecordingSectionView(val letter: String?, val heading: String?, val recordings: List<RecordingView>)

/** A chapter's Słuchaj: its recordings by section, and how many of them are on the phone. */
data class ListenView(val lesson: Int, val sections: List<RecordingSectionView>, val imported: Int, val total: Int)

/** A listening task as it is shown: its question, and what is asked of the Learner. */
sealed interface ListeningTaskView {
    val id: String
    val question: String
    val meaning: String?
    /** The lines it is about, counted from 1, or null for the whole recording. */
    val lines: IntRange?

    data class TrueFalse(override val id: String, override val question: String, override val meaning: String?, override val lines: IntRange?) : ListeningTaskView
    data class Choose(override val id: String, override val question: String, override val meaning: String?, override val lines: IntRange?, val options: List<String>) : ListeningTaskView
    data class Dictation(override val id: String, override val question: String, override val meaning: String?, override val lines: IntRange?) : ListeningTaskView
}

/** What a listening task is answered with. */
sealed interface ListeningAnswer {
    data class TrueFalse(val value: Boolean) : ListeningAnswer
    data class Choose(val option: Int) : ListeningAnswer
    data class Typed(val text: String) : ListeningAnswer
}

/** One recording opened in the player: what it is, its lines for step mode, and its tasks. */
data class RecordingPlayView(
    val track: ClassTrack,
    val lines: List<TrackLine>,
    val tasks: List<ListeningTaskView>,
    /** The recordings before and after it in its chapter, for the player's skip buttons. */
    val previous: Int?,
    val next: Int?,
)

/** How many recordings are on the phone, of how many, and the space they take. */
data class ClassAudioView(val imported: Int, val total: Int, val bytes: Long)

/** What an import did: [matched] files kept, and the names of those [unmatched]. */
data class ImportResult(val matched: Int, val unmatched: List<String>) {
    operator fun plus(other: ImportResult) = ImportResult(matched + other.matched, unmatched + other.unmatched)

    companion object {
        val NONE = ImportResult(0, emptyList())
    }
}

/**
 * The class recordings: the track list, the line timings, the listening tasks, and the recordings
 * imported on the phone, kept in [dir] as `NNN.mp3`. Nothing here is saved but the imported files.
 */
class ClassAudio(private val course: Course, private val dir: File, private val localize: (Localized) -> String = { it.values.firstOrNull().orEmpty() }) {
    private val byNumber = course.tracks.associateBy { it.number }

    /** The recordings imported so far, by track number. */
    fun imported(): Set<Int> = dir.listFiles().orEmpty().mapNotNull { file ->
        trackFileName.matchEntire(file.name)?.groupValues?.get(1)?.toInt()?.takeIf { it in byNumber && file.length() > 0 }
    }.toSet()

    fun view(): ClassAudioView {
        val numbers = imported()
        return ClassAudioView(numbers.size, course.tracks.size, numbers.sumOf { fileOf(it).length() })
    }

    /** The file of recording [number], whether or not it has been imported. */
    fun fileOf(number: Int): File = File(dir, "%03d.mp3".format(number))

    /** The recordings of the book's chapter [lesson] by section, in the book's order. */
    fun listen(lesson: Int): ListenView {
        val numbers = imported()
        val tracks = course.tracks.filter { it.lesson == lesson }
        val headings = course.levels.flatMap { it.units }.firstOrNull { it.number == lesson }?.dailySets
            ?.mapNotNull { it.section }?.filter { it.letter != null }?.associate { it.letter to it.heading }.orEmpty()
        val tasks = course.listening.groupingBy { it.track }.eachCount()
        val sections = tracks.groupBy { it.section }.map { (letter, inSection) ->
            RecordingSectionView(
                letter,
                letter?.let(headings::get),
                inSection.map { track ->
                    RecordingView(
                        track.number,
                        track.title,
                        track.exercise,
                        track.lengthMs,
                        lines = course.trackLines[track.number].orEmpty().size,
                        tasks = tasks[track.number] ?: 0,
                        imported = track.number in numbers,
                    )
                },
            )
        }
        return ListenView(lesson, sections, tracks.count { it.number in numbers }, tracks.size)
    }

    /** Recording [number] as the player opens it. */
    fun play(number: Int): RecordingPlayView {
        val track = checkNotNull(byNumber[number]) { "There is no recording $number" }
        val chapter = course.tracks.filter { it.lesson == track.lesson }.map { it.number }
        val at = chapter.indexOf(number)
        return RecordingPlayView(
            track,
            course.trackLines[number].orEmpty(),
            course.listening.filter { it.track == number }.map(::viewOf),
            previous = chapter.getOrNull(at - 1),
            next = chapter.getOrNull(at + 1),
        )
    }

    /** Whether [answer] is right for the task [taskId], as an Exercise's answer is checked. */
    fun check(taskId: String, answer: ListeningAnswer): Feedback {
        val task = checkNotNull(course.listening.firstOrNull { it.id == taskId }) { "There is no listening task $taskId" }
        return when (task) {
            is TrueFalseTask -> Feedback(
                correct = (answer as? ListeningAnswer.TrueFalse)?.value == task.answer,
                correctAnswer = if (task.answer) TRUE_MARK else FALSE_MARK,
            )
            is ChooseTask -> Feedback(
                correct = (answer as? ListeningAnswer.Choose)?.option == task.correctOption,
                correctAnswer = task.options[task.correctOption],
            )
            is DictationTask -> checkTyped((answer as? ListeningAnswer.Typed)?.text.orEmpty(), task.acceptedAnswers)
        }
    }

    private fun viewOf(task: ListeningTask): ListeningTaskView {
        val lines = task.lines?.let { it.first()..it.last() }
        return when (task) {
            is TrueFalseTask -> ListeningTaskView.TrueFalse(task.id, task.statement, meaningOf(task.meaning), lines)
            is ChooseTask -> ListeningTaskView.Choose(task.id, task.question, meaningOf(task.meaning), lines, task.options)
            is DictationTask -> ListeningTaskView.Dictation(task.id, task.question, meaningOf(task.meaning), lines)
        }
    }

    private fun meaningOf(meaning: Localized): String? = meaning.takeIf { it.isNotEmpty() }?.let(localize)?.takeIf { it.isNotBlank() }

    /**
     * Imports the class recordings in a zip, such as the publisher's `lekcja_05.zip`: each MP3 in
     * it that matches a track is copied into the recordings, in place of any copy already there.
     * A zip inside the zip is read too.
     */
    fun importZip(input: InputStream): ImportResult {
        var result = ImportResult.NONE
        val zip = ZipInputStream(input)
        while (true) {
            val entry = zip.nextEntry ?: break
            if (entry.isDirectory) continue
            val name = entry.name.substringAfterLast('/')
            result += when {
                name.startsWith(".") -> ImportResult.NONE
                name.endsWith(".zip", ignoreCase = true) -> importZip(NotClosing(zip))
                name.endsWith(".mp3", ignoreCase = true) -> importRecording(name, zip)
                else -> ImportResult.NONE
            }
        }
        return result
    }

    /** Imports one file the Learner picked or that was in a folder they picked: a recording, or a zip of them. */
    fun importFile(name: String, input: InputStream): ImportResult = when {
        name.endsWith(".zip", ignoreCase = true) -> importZip(input)
        name.endsWith(".mp3", ignoreCase = true) -> importRecording(name, input)
        else -> ImportResult.NONE
    }

    /** Copies the MP3 [name] from [input] into the recordings if it matches a track by its title tag or its file name. */
    fun importRecording(name: String, input: InputStream): ImportResult {
        dir.mkdirs()
        val part = File.createTempFile("import", ".part", dir)
        try {
            part.outputStream().use { input.copyTo(it) }
            val title = part.inputStream().use(::id3Title)
            val number = matchRecording(name, title, course.tracks)
                ?: return ImportResult(0, listOf(name))
            val target = fileOf(number)
            target.delete()
            if (!part.renameTo(target)) throw IOException("Cannot keep recording $number")
            return ImportResult(1, emptyList())
        } finally {
            part.delete()
        }
    }

    /** Removes every imported recording. */
    fun removeAll() {
        dir.listFiles().orEmpty().forEach { it.delete() }
    }

    /** A zip entry read as a stream of its own, which a nested zip must not close. */
    private class NotClosing(private val input: InputStream) : InputStream() {
        override fun read(): Int = input.read()
        override fun read(b: ByteArray, off: Int, len: Int): Int = input.read(b, off, len)
        override fun close() = Unit
    }

    companion object {
        const val TRUE_MARK = "P"
        const val FALSE_MARK = "N"
    }
}

private val trackFileName = Regex("""(\d{3})\.mp3""")

/** The title tag of the publisher's recordings: "064 Lekcja 05A, Ćwiczenie 1". */
private val titleTag = Regex("""^\s*(\d{1,3})\s+Lekcja\s+(\d{1,2})""", RegexOption.IGNORE_CASE)

/** A recording's file name: its number first, as in `064.mp3`. */
private val fileNumber = Regex("""^(\d{1,3})\b.*\.mp3$""", RegexOption.IGNORE_CASE)

/**
 * The track a recording is, from its ID3 [title] tag when it has one in the publisher's form (its
 * number, and its chapter, which must agree with the track list), else from its file [name]. Null
 * when neither names a track.
 */
fun matchRecording(name: String, title: String?, tracks: List<ClassTrack>): Int? {
    val byNumber = tracks.associateBy { it.number }
    title?.let(titleTag::find)?.let { tag ->
        val number = tag.groupValues[1].toInt()
        val lesson = tag.groupValues[2].toInt()
        return number.takeIf { byNumber[it]?.lesson == lesson }
    }
    val number = fileNumber.find(name.substringAfterLast('/'))?.groupValues?.get(1)?.toInt() ?: return null
    return number.takeIf { it in byNumber }
}

/**
 * The title (TIT2) of an MP3's ID3v2.3 or v2.4 tag, read from the start of the file, or null when
 * it has none. Only the tag is read; the sound after it is not.
 */
fun id3Title(input: InputStream): String? {
    val header = input.readExactly(10) ?: return null
    if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) return null
    val version = header[3].toInt()
    if (version != 3 && version != 4) return null
    val flags = header[5].toInt()
    var left = synchsafe(header, 6)
    // An extended header comes before the frames.
    if (flags and 0x40 != 0) {
        val size = input.readExactly(4) ?: return null
        val extended = if (version == 4) synchsafe(size, 0) - 4 else bigEndian(size, 0)
        input.skipFully(extended.toLong())
        left -= 4 + extended
    }
    while (left > 10) {
        val frame = input.readExactly(10) ?: return null
        if (frame[0] == 0.toByte()) return null // padding: no more frames
        val id = String(frame, 0, 4, Charsets.ISO_8859_1)
        val size = if (version == 4) synchsafe(frame, 4) else bigEndian(frame, 4)
        if (size < 0 || size > left) return null
        if (id == "TIT2") return decodeText(input.readExactly(size) ?: return null)
        input.skipFully(size.toLong())
        left -= 10 + size
    }
    return null
}

/** A text frame's body: an encoding byte, then the text in it. */
private fun decodeText(body: ByteArray): String? {
    if (body.isEmpty()) return null
    val text = when (body[0].toInt()) {
        0 -> String(body, 1, body.size - 1, Charsets.ISO_8859_1)
        1 -> String(body, 1, body.size - 1, Charsets.UTF_16)
        2 -> String(body, 1, body.size - 1, Charsets.UTF_16BE)
        3 -> String(body, 1, body.size - 1, Charsets.UTF_8)
        else -> return null
    }
    return text.trimEnd('\u0000').trim().takeIf { it.isNotEmpty() }
}

private fun synchsafe(bytes: ByteArray, at: Int): Int =
    (bytes[at].toInt() and 0x7f shl 21) or (bytes[at + 1].toInt() and 0x7f shl 14) or
        (bytes[at + 2].toInt() and 0x7f shl 7) or (bytes[at + 3].toInt() and 0x7f)

private fun bigEndian(bytes: ByteArray, at: Int): Int =
    (bytes[at].toInt() and 0xff shl 24) or (bytes[at + 1].toInt() and 0xff shl 16) or
        (bytes[at + 2].toInt() and 0xff shl 8) or (bytes[at + 3].toInt() and 0xff)

private fun InputStream.readExactly(count: Int): ByteArray? {
    val bytes = ByteArray(count)
    var read = 0
    while (read < count) {
        val n = read(bytes, read, count - read)
        if (n < 0) return null
        read += n
    }
    return bytes
}

private fun InputStream.skipFully(count: Long) {
    var left = count
    while (left > 0) {
        val skipped = skip(left)
        if (skipped <= 0) {
            if (read() < 0) return
            left--
        } else {
            left -= skipped
        }
    }
}

/** Step mode, the book's *posłuchać i powtórzyć*: how long the player waits after a line for the Learner to say it. */
object ListenAndRepeat {
    const val SHORTEST_PAUSE_MS = 1_500L
    const val LONGEST_PAUSE_MS = 8_000L

    /**
     * The pause after [line] played at [speed]: a little longer than the line took to hear, for
     * the Learner to say it aloud, never shorter than [SHORTEST_PAUSE_MS] or longer than [LONGEST_PAUSE_MS].
     */
    fun pauseAfter(line: TrackLine, speed: Float = 1f): Long =
        ((line.lengthMs / speed) * 1.2 + 800).toLong().coerceIn(SHORTEST_PAUSE_MS, LONGEST_PAUSE_MS)

    /** The line playing at [positionMs]: the one it is in, or the next one when it is in a pause. Null after the last. */
    fun lineAt(lines: List<TrackLine>, positionMs: Long): Int? =
        lines.indexOfFirst { positionMs < it.endMs }.takeIf { it >= 0 }
}
