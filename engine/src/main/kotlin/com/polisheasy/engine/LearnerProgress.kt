package com.polisheasy.engine

import java.time.LocalDate

/** What the app remembers about a Learner between launches. */
interface LearnerProgress {
    /** Completed Daily Sets by id, with the calendar day each was completed. */
    fun completedDailySets(): Map<String, LocalDate>

    fun recordCompleted(dailySetId: String, on: LocalDate)
}

class InMemoryLearnerProgress : LearnerProgress {
    private val completed = mutableMapOf<String, LocalDate>()

    override fun completedDailySets(): Map<String, LocalDate> = completed.toMap()

    override fun recordCompleted(dailySetId: String, on: LocalDate) {
        completed[dailySetId] = on
    }
}
