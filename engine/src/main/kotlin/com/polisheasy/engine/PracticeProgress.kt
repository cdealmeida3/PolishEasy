package com.polisheasy.engine

import java.io.File
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What the app remembers about Practice between launches: the Learner's Best in each way of
 * playing each Game, how they last played each Game, and how many Rounds they played on the last
 * day they played. It is kept apart from [LearnerProgress], so nothing in Practice can damage
 * progress on the Path.
 */
interface PracticeProgress {
    /** The Best saved under [key], or null when that way of playing has no finished Round yet. */
    fun best(key: String): Int?

    /**
     * Saves a Round finished [on] this day: [best] under [key] when it is given, [played] as the
     * way the Game [gameId] was last played, and one more Round for the day. All as one change.
     */
    fun recordRound(gameId: String, played: List<String>, key: String, best: Int?, on: LocalDate)

    /** The way [gameId] was last played, or null when it never was. */
    fun lastPlayed(gameId: String): List<String>?

    /** How many Rounds were finished on [day]. */
    fun roundsOn(day: LocalDate): Int
}

/** Practice progress that is lost when the process ends. For tests. */
class InMemoryPracticeProgress : PracticeProgress {
    private val bests = mutableMapOf<String, Int>()
    private val last = mutableMapOf<String, List<String>>()
    private var day: LocalDate? = null
    private var rounds = 0

    override fun best(key: String): Int? = bests[key]

    override fun recordRound(gameId: String, played: List<String>, key: String, best: Int?, on: LocalDate) {
        if (best != null) bests[key] = best
        last[gameId] = played
        rounds = roundsOn(on) + 1
        day = on
    }

    override fun lastPlayed(gameId: String): List<String>? = last[gameId]

    override fun roundsOn(day: LocalDate): Int = if (day == this.day) rounds else 0
}

/** The stored document. Games and their ways of playing are named by stable ids; the day is an ISO string. */
@Serializable
private data class StoredPractice(
    val version: Int = 1,
    val bests: Map<String, Int> = emptyMap(),
    val last: Map<String, List<String>> = emptyMap(),
    /** The last day a Round was finished, and how many were finished on it. */
    val day: String? = null,
    val rounds: Int = 0,
)

// Unknown keys are ignored so a later version can add fields without breaking this one.
private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Practice progress saved in one JSON file, `practice.json`, in the same way as
 * [FileLearnerProgress]: read once, rewritten whole through a temporary file, and moved aside if
 * it cannot be read.
 */
class FilePracticeProgress(file: File) : PracticeProgress {
    private val saved = SavedFile(file)
    private var stored: StoredPractice = saved.read(StoredPractice()) { text ->
        json.decodeFromString<StoredPractice>(text).also { it.day?.let(LocalDate::parse) }
    }

    override fun best(key: String): Int? = stored.bests[key]

    override fun recordRound(gameId: String, played: List<String>, key: String, best: Int?, on: LocalDate) {
        val next = stored.copy(
            bests = if (best == null) stored.bests else stored.bests + (key to best),
            last = stored.last + (gameId to played),
            day = on.toString(),
            rounds = roundsOn(on) + 1,
        )
        saved.write(json.encodeToString(next))
        stored = next
    }

    override fun lastPlayed(gameId: String): List<String>? = stored.last[gameId]

    override fun roundsOn(day: LocalDate): Int = if (stored.day == day.toString()) stored.rounds else 0
}
