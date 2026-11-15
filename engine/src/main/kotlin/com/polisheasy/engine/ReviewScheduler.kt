package com.polisheasy.engine

import java.time.LocalDate

/** Where an Item is in the Review schedule: its [box], the day it is next [due] and the day it was last answered. */
data class ItemSchedule(val box: Int, val due: LocalDate, val lastAnswered: LocalDate)

/** Decides when an Item is next due. Replaceable without touching content or stored Path progress. */
interface ReviewScheduler {
    /** The schedule of an Item after it is answered on [today]. [previous] is null for an Item never answered. */
    fun afterAnswer(previous: ItemSchedule?, correct: Boolean, today: LocalDate): ItemSchedule
}

/**
 * Five boxes, with gaps of 1, 2, 4, 8 and 16 days. A correct answer moves the Item up one box and
 * a wrong one returns it to box one.
 *
 * An Item moves up at most one box per calendar day. A new Item is practised by several Exercises
 * in the Daily Set that introduces it, and without this rule it would climb a box for each of
 * them on the day it was first seen. So a correct answer on a day the Item was already answered
 * changes nothing; a wrong answer always returns it to box one.
 */
class LeitnerScheduler : ReviewScheduler {
    override fun afterAnswer(previous: ItemSchedule?, correct: Boolean, today: LocalDate): ItemSchedule {
        if (previous == null || !correct) return inBox(FIRST_BOX, today)
        if (!previous.lastAnswered.isBefore(today)) return previous
        return inBox(minOf(previous.box + 1, GAP_DAYS.size), today)
    }

    private fun inBox(box: Int, today: LocalDate) =
        ItemSchedule(box, due = today.plusDays(GAP_DAYS[box - 1]), lastAnswered = today)

    private companion object {
        const val FIRST_BOX = 1

        /** Days until an Item is due again, by box. */
        val GAP_DAYS = listOf(1L, 2L, 4L, 8L, 16L)
    }
}
