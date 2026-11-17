package com.polisheasy.engine

import java.time.LocalDate
import kotlinx.serialization.Serializable

/** A Daily Set the Learner has started and not finished, by stable ids only. */
@Serializable
data class DailySetInProgress(
    /** The Daily Set's id, or [REVIEW_ONLY_ID] for a Review-only Daily Set, which is all [review]. */
    val dailySetId: String,
    /** Ids of Exercises still to be answered correctly; the head is the one being shown. */
    val remaining: List<String>,
    /** Ids of Exercises answered wrongly at least once. */
    val missed: Set<String> = emptySet(),
    /** Ids of Tips already shown. */
    val shownTips: Set<String> = emptySet(),
    /** Ids of the Review Exercises chosen for this Daily Set when it was started. They follow its own in [remaining]. */
    val review: List<String> = emptyList(),
    /** The Learner chose "can't listen now": every listening Exercise still to come in this Daily Set is shown in its reading form. */
    val cantListen: Boolean = false,
)

/** What is remembered about one Item: when it is next due, and the Exercises last shown for it, oldest first. */
data class ItemProgress(val schedule: ItemSchedule, val recentExercises: List<String> = emptyList())

/** What the app remembers about a Learner between launches. */
interface LearnerProgress {
    /** Completed Daily Sets by id, with the calendar day each was completed. */
    fun completedDailySets(): Map<String, LocalDate>

    /**
     * Records the completion and forgets the Daily Set in progress, as one change. [items] are the
     * Items the last answer changed, stored in the same change.
     */
    fun recordCompleted(dailySetId: String, on: LocalDate, items: Map<String, ItemProgress> = emptyMap())

    /** The calendar days on which a Review-only Daily Set was completed. */
    fun reviewOnlyDays(): Set<LocalDate>

    /**
     * Records that a Review-only Daily Set was completed [on] this day and forgets the Daily Set in
     * progress, as one change. [items] are the Items the last answer changed, stored in the same change.
     */
    fun recordReviewOnlyCompleted(on: LocalDate, items: Map<String, ItemProgress> = emptyMap())

    fun dailySetInProgress(): DailySetInProgress?

    /** Saves where the Learner is, together with the [items] the answer changed, as one change. */
    fun saveInProgress(state: DailySetInProgress, items: Map<String, ItemProgress> = emptyMap())

    /** Progress on every Item answered so far, by Item id. It knows nothing of Levels. */
    fun itemProgress(): Map<String, ItemProgress>

    /** Saves [items] answered outside a Daily Set, such as on a flashcard, as one change. */
    fun recordItems(items: Map<String, ItemProgress>)
}

/** Progress that is lost when the process ends. For tests. */
class InMemoryLearnerProgress : LearnerProgress {
    private val completed = mutableMapOf<String, LocalDate>()
    private val reviewOnly = mutableSetOf<LocalDate>()
    private var inProgress: DailySetInProgress? = null
    private val items = mutableMapOf<String, ItemProgress>()

    override fun completedDailySets(): Map<String, LocalDate> = completed.toMap()

    override fun recordCompleted(dailySetId: String, on: LocalDate, items: Map<String, ItemProgress>) {
        completed[dailySetId] = on
        inProgress = null
        this.items += items
    }

    override fun reviewOnlyDays(): Set<LocalDate> = reviewOnly.toSet()

    override fun recordReviewOnlyCompleted(on: LocalDate, items: Map<String, ItemProgress>) {
        reviewOnly += on
        inProgress = null
        this.items += items
    }

    override fun dailySetInProgress(): DailySetInProgress? = inProgress

    override fun saveInProgress(state: DailySetInProgress, items: Map<String, ItemProgress>) {
        inProgress = state
        this.items += items
    }

    override fun itemProgress(): Map<String, ItemProgress> = items.toMap()

    override fun recordItems(items: Map<String, ItemProgress>) {
        this.items += items
    }
}
