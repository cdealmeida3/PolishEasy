package com.polisheasy.engine

import java.io.File
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** How many questions a Checkpoint asks at most. Fewer when the chapter has fewer Exercises. */
const val CHECKPOINT_QUESTIONS = 20

/** The share of right answers, in percent, at which a Checkpoint is passed and its chapter gets a seal. */
const val CHECKPOINT_PASS_PERCENT = 80

/**
 * What the app remembers about Checkpoints between launches: the Learner's Best in each chapter's,
 * in percent. Kept apart from [LearnerProgress], like the Bests of Practice, so a Checkpoint can
 * never damage progress on the Path.
 */
interface CheckpointBests {
    /** The Best of the Checkpoint of Unit [unitId], in percent, or null when it was never finished. */
    fun best(unitId: String): Int?

    /** Saves [percent] as the Best of Unit [unitId]'s Checkpoint. */
    fun record(unitId: String, percent: Int)
}

/** Checkpoint Bests that are lost when the process ends. For tests. */
class InMemoryCheckpointBests : CheckpointBests {
    private val bests = mutableMapOf<String, Int>()

    override fun best(unitId: String): Int? = bests[unitId]

    override fun record(unitId: String, percent: Int) {
        bests[unitId] = percent
    }
}

@Serializable
private data class StoredCheckpoints(val version: Int = 1, val bests: Map<String, Int> = emptyMap())

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Checkpoint Bests saved in one JSON file, `checkpoints.json`, in the same way as [FileLearnerProgress]. */
class FileCheckpointBests(file: File) : CheckpointBests {
    private val saved = SavedFile(file)
    private var stored: StoredCheckpoints = saved.read(StoredCheckpoints()) { json.decodeFromString<StoredCheckpoints>(it) }

    override fun best(unitId: String): Int? = stored.bests[unitId]

    override fun record(unitId: String, percent: Int) {
        val next = stored.copy(bests = stored.bests + (unitId to percent))
        saved.write(json.encodeToString(next))
        stored = next
    }
}

/**
 * The end of a Checkpoint: [correct] of [questions] right at the first try, that as [percent],
 * the [best] kept so far (this one included), whether this is a [newBest], and whether it [passed].
 */
data class CheckpointResultView(
    val unitId: String,
    val title: String,
    val correct: Int,
    val questions: Int,
    val percent: Int,
    val best: Int,
    val newBest: Boolean,
    val passed: Boolean,
)

/** The share of [correct] in [questions], in whole percent, rounded down so 79.9 % never passes. */
internal fun percentOf(correct: Int, questions: Int): Int = if (questions == 0) 0 else correct * 100 / questions

/**
 * The questions of a Checkpoint over [exercises], the Exercises of one chapter: up to [limit] of
 * them, mixed by kind and covering as many Items as there are room for. [random] decides which.
 */
internal fun checkpointQuestions(exercises: List<Exercise>, random: Random, limit: Int = CHECKPOINT_QUESTIONS): List<String> {
    val shuffled = exercises.shuffled(random)
    // One Exercise per Item first, so the Test covers the chapter, then the rest.
    val seen = mutableSetOf<String>()
    val (covering, others) = shuffled.partition { exercise -> exercise.items.any { seen.add(it) } }
    val ordered = covering + others
    // Deal the kinds in turn, so no kind of Exercise fills the Test.
    val byKind = ordered.groupBy { it::class }.values.map { it.toMutableList() }.toMutableList()
    val chosen = mutableListOf<Exercise>()
    while (chosen.size < limit && byKind.any { it.isNotEmpty() }) {
        byKind.forEach { kind -> if (chosen.size < limit && kind.isNotEmpty()) chosen += kind.removeAt(0) }
    }
    return chosen.shuffled(random).map { it.id }
}
