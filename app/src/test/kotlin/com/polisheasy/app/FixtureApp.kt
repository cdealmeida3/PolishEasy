package com.polisheasy.app

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

    override fun stop() {
        asked += Stopped
    }

    override val playing = false

    override fun release() = Unit
}
