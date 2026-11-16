package com.polisheasy.engine

import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

/**
 * One calendar day as Home shows it. [completed] when a new or Review-only Daily Set was completed
 * on it. [today] for the day it is now, and [upcoming] for a day still to come, which is never a
 * day missed.
 */
data class DayView(val date: LocalDate, val completed: Boolean, val today: Boolean = false, val upcoming: Boolean = false)

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

/** The calendar week [today] is in, Monday to Sunday, each day with whether it is among [completionDays]. */
internal fun weekOf(today: LocalDate, completionDays: Set<LocalDate>): List<DayView> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0L..6L).map { monday.plusDays(it) }.map { day ->
        DayView(day, completed = day in completionDays, today = day == today, upcoming = day > today)
    }
}
