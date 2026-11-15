package com.polisheasy.engine

import java.time.LocalDate
import kotlinx.serialization.Serializable

/** A Daily Set the Learner has started and not finished, by stable ids only. */
@Serializable
data class DailySetInProgress(
    val dailySetId: String,
    /** Ids of Exercises still to be answered correctly; the head is the one being shown. */
    val remaining: List<String>,
    /** Ids of Exercises answered wrongly at least once. */
    val missed: Set<String> = emptySet(),
    /** Ids of Tips already shown. */
    val shownTips: Set<String> = emptySet(),
)

/** What the app remembers about a Learner between launches. */
interface LearnerProgress {
    /** Completed Daily Sets by id, with the calendar day each was completed. */
    fun completedDailySets(): Map<String, LocalDate>

    /** Records the completion and forgets the Daily Set in progress, as one change. */
    fun recordCompleted(dailySetId: String, on: LocalDate)

    fun dailySetInProgress(): DailySetInProgress?

    fun saveInProgress(state: DailySetInProgress)
}

/** Progress that is lost when the process ends. For tests. */
class InMemoryLearnerProgress : LearnerProgress {
    private val completed = mutableMapOf<String, LocalDate>()
    private var inProgress: DailySetInProgress? = null

    override fun completedDailySets(): Map<String, LocalDate> = completed.toMap()

    override fun recordCompleted(dailySetId: String, on: LocalDate) {
        completed[dailySetId] = on
        inProgress = null
    }

    override fun dailySetInProgress(): DailySetInProgress? = inProgress

    override fun saveInProgress(state: DailySetInProgress) {
        inProgress = state
    }
}
