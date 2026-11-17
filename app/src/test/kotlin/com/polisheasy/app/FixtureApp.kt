package com.polisheasy.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.polisheasy.engine.Course
import com.polisheasy.engine.loadCourse
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The course the screen tests play, from `src/test/resources/fixture-course`: one Unit of two
 * Daily Sets, the first with a Tip and exactly one Exercise of each type. It is written in the
 * same files as the real course, so the tests know every right answer and an edit to the real
 * course cannot break them. Its pictures are the one thing borrowed: it names images of the real
 * course, which the app reads from its packaged files.
 */
fun fixtureCourse(): Course = loadCourse { path ->
    val file = checkNotNull(Course::class.java.classLoader?.getResource("fixture-course/$path")) {
        "The fixture course has no file $path"
    }
    file.readText()
}

/** A clock a test sets, so it can move to the next day. It starts at noon on a fixed day. */
class SettableClock(private val zone: ZoneId = ZoneId.of("Europe/Warsaw")) : Clock() {
    var now: ZonedDateTime = LocalDate.of(2026, 3, 2).atTime(12, 0).atZone(zone)

    val today: LocalDate get() = now.toLocalDate()

    fun moveToNextDay() {
        now = now.plusDays(1)
    }

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = SettableClock(zone).also { it.now = now.withZoneSameInstant(zone) }

    override fun instant(): Instant = now.toInstant()
}

/** Stands in for the Clip player: nothing is heard, and everything the screens asked for is kept in order. */
class RecordingClips : Clips {
    sealed interface Asked

    data class Played(val path: String, val slow: Boolean) : Asked

    data object Stopped : Asked

    val asked = mutableListOf<Asked>()

    /** Every Clip the screens asked to hear, in order. */
    val played: List<Played> get() = asked.filterIsInstance<Played>()

    override fun play(path: String, slow: Boolean) {
        asked += Played(path, slow)
    }

    override fun playAll(paths: List<String>) {
        asked += paths.map { Played(it, slow = false) }
    }

    override fun stop() {
        asked += Stopped
    }

    override val playing = false

    override fun release() = Unit
}

/**
 * Stands in for the player of the class recordings: nothing is heard, what the screens asked for
 * is kept in order, and a test sets what it is doing through [state].
 */
class RecordingRecordings : Recordings {
    override var state by androidx.compose.runtime.mutableStateOf(PlayerState())

    val asked = mutableListOf<String>()

    /** The recordings opened, in order. */
    val opened = mutableListOf<Int>()

    override fun open(number: Int, file: java.io.File, title: RecordingTitle, lines: List<com.polisheasy.engine.TrackLine>) {
        opened += number
        asked += "open $number"
        state = state.copy(number = number, playing = false, positionMs = 0, durationMs = 0, line = null, yourTurn = false)
    }

    override fun playPause() {
        asked += "playPause"
        state = state.copy(playing = !state.playing)
    }

    override fun skip(ms: Long) {
        asked += "skip $ms"
    }

    override fun line(index: Int) {
        asked += "line $index"
        state = state.copy(line = index)
    }

    override fun speed(value: Float) {
        asked += "speed $value"
        state = state.copy(speed = value)
    }

    override fun repeat(on: Boolean) {
        asked += "repeat $on"
        state = state.copy(repeat = on)
    }

    override fun step(on: Boolean) {
        asked += "step $on"
        state = state.copy(step = on)
    }

    override fun hold() {
        asked += "hold"
    }

    override fun close() {
        asked += "close"
        state = state.copy(number = null, playing = false)
    }

    override fun release() = Unit
}

/**
 * The publisher's zip of each chapter in [lessons], as on the class's discs, written into [dir]:
 * a folder "Lekcja NN" of recordings, stood in for by tiny MP3s that carry only the same title tag.
 */
fun classZips(course: Course, lessons: IntRange, dir: java.io.File): List<java.io.File> =
    course.tracks.filter { it.lesson in lessons }.groupBy { it.lesson }.map { (lesson, tracks) ->
        java.io.File(dir, "lekcja_%02d.zip".format(lesson)).also { file ->
            java.util.zip.ZipOutputStream(file.outputStream()).use { zip ->
                tracks.forEach { track ->
                    zip.putNextEntry(java.util.zip.ZipEntry("Lekcja %02d/%03d.mp3".format(lesson, track.number)))
                    zip.write(taggedMp3("%03d Lekcja %02d%s, %s".format(track.number, lesson, track.section.orEmpty(), track.title)))
                    zip.closeEntry()
                }
            }
        }
    }

/** A tiny MP3: an ID3v2.3 tag with [title] in UTF-16, then one frame header of sound. */
fun taggedMp3(title: String): ByteArray {
    val text = byteArrayOf(1, 0xFF.toByte(), 0xFE.toByte()) + title.toByteArray(Charsets.UTF_16LE) + byteArrayOf(0, 0)
    val size = text.size
    val frame = "TIT2".toByteArray(Charsets.ISO_8859_1) +
        byteArrayOf((size shr 24).toByte(), (size shr 16).toByte(), (size shr 8).toByte(), size.toByte(), 0, 0) + text
    val tag = frame.size
    val header = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 3, 0, 0) +
        byteArrayOf((tag shr 21 and 0x7f).toByte(), (tag shr 14 and 0x7f).toByte(), (tag shr 7 and 0x7f).toByte(), (tag and 0x7f).toByte())
    return header + frame + byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x00)
}

/**
 * Stands in for the speech recognizer: whether it is [available] is the test's to set, the
 * microphone is always permitted, and what is heard is what the test says with [hear].
 */
class FakeSpeech(available: Boolean = true) : Speech {
    override var available by androidx.compose.runtime.mutableStateOf(available)
    override val permitted = true
    override var listening by androidx.compose.runtime.mutableStateOf(false)
        private set

    private var onHeard: ((List<String>) -> Unit)? = null

    /** How many times the screens started listening. */
    var listened = 0
        private set

    override fun listen(onHeard: (List<String>) -> Unit) {
        listened++
        listening = true
        this.onHeard = onHeard
    }

    /** The Learner said something the recognizer understood as [guesses], best first; none for nothing understood. */
    fun hear(vararg guesses: String) {
        val heard = checkNotNull(onHeard) { "Nothing is listening" }
        onHeard = null
        listening = false
        heard(guesses.toList())
    }

    override fun stop() {
        listening = false
        onHeard = null
    }

    override fun refused() {
        available = false
    }

    override fun release() = Unit
}
