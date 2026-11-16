package com.polisheasy.engine

import java.time.LocalDate

/**
 * The Streak on [today]: how many consecutive calendar days, ending today or, while today has no
 * completion yet, yesterday, are among [completionDays].
 */
internal fun streakOn(today: LocalDate, completionDays: Set<LocalDate>): Int {
    val last = if (today in completionDays) today else today.minusDays(1)
    return generateSequence(last) { it.minusDays(1) }.takeWhile { it in completionDays }.count()
}

/** The longest run of consecutive calendar days among [completionDays], whenever it was. */
internal fun longestStreakIn(completionDays: Set<LocalDate>): Int =
    completionDays.filter { it.minusDays(1) !in completionDays }.maxOfOrNull { first ->
        generateSequence(first) { it.plusDays(1) }.takeWhile { it in completionDays }.count()
    } ?: 0
