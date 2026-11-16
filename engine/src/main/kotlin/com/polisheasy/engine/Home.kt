package com.polisheasy.engine

import java.time.LocalDate

/** One calendar day as Home shows it. [completed] when a new or Review-only Daily Set was completed on it. */
data class DayView(val date: LocalDate, val completed: Boolean)

/**
 * Where the Learner is on the Path: the first Daily Set not yet completed, whether it can be
 * started today or unlocks tomorrow. [unitNumber] counts the Level's Units from 1 and
 * [numberInUnit] the Unit's Daily Sets from 1, so [numberInUnit] minus one of the Unit's
 * [dailySetsInUnit] are completed. [teaches] are the titles of its Tips, in order.
 */
data class NextDailySetView(
    val unitNumber: Int,
    val unitTitle: String,
    val numberInUnit: Int,
    val dailySetsInUnit: Int,
    val teaches: List<String> = emptyList(),
)

/** The [days] calendar days ending [today], oldest first, each with whether it is among [completionDays]. */
internal fun daysEnding(today: LocalDate, completionDays: Set<LocalDate>, days: Int): List<DayView> =
    (days - 1 downTo 0).map { ago -> today.minusDays(ago.toLong()).let { DayView(it, it in completionDays) } }
