package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewSchedulerTest {
    private val scheduler: ReviewScheduler = LeitnerScheduler()

    private val today = march(10)
    private val tomorrow = march(11)

    /** An Item in [box], last answered some days ago. */
    private fun inBox(box: Int) = ItemSchedule(box, due = march(9), lastAnswered = march(5))

    @Test
    fun `a first answer, right or wrong, puts the Item in box 1, due tomorrow`() {
        val expected = ItemSchedule(box = 1, due = tomorrow, lastAnswered = today)

        assertEquals(expected, scheduler.afterAnswer(previous = null, correct = true, today = today))
        assertEquals(expected, scheduler.afterAnswer(previous = null, correct = false, today = today))
    }

    @Test
    fun `a correct answer on a later day moves the Item up one box, with gaps of 2, 4, 8 and 16 days`() {
        val gapAfterLeaving = mapOf(1 to 2L, 2 to 4L, 3 to 8L, 4 to 16L)

        gapAfterLeaving.forEach { (box, gap) ->
            assertEquals(
                ItemSchedule(box = box + 1, due = today.plusDays(gap), lastAnswered = today),
                scheduler.afterAnswer(inBox(box), correct = true, today = today),
                "From box $box",
            )
        }
    }

    @Test
    fun `a correct answer in box 5 stays in box 5, due in 16 days`() {
        assertEquals(
            ItemSchedule(box = 5, due = today.plusDays(16), lastAnswered = today),
            scheduler.afterAnswer(inBox(5), correct = true, today = today),
        )
    }

    @Test
    fun `a wrong answer from any box returns the Item to box 1, due tomorrow`() {
        (1..5).forEach { box ->
            assertEquals(
                ItemSchedule(box = 1, due = tomorrow, lastAnswered = today),
                scheduler.afterAnswer(inBox(box), correct = false, today = today),
                "From box $box",
            )
        }
    }

    @Test
    fun `a second correct answer on the same day changes nothing`() {
        val first = scheduler.afterAnswer(inBox(2), correct = true, today = today)

        val second = scheduler.afterAnswer(first, correct = true, today = today)
        val third = scheduler.afterAnswer(second, correct = true, today = today)

        assertEquals(ItemSchedule(box = 3, due = today.plusDays(4), lastAnswered = today), first)
        assertEquals(first, second)
        assertEquals(first, third)
    }

    @Test
    fun `on one day a wrong answer after a correct one returns the Item to box 1, and a correct one after a wrong one leaves it there`() {
        val boxOne = ItemSchedule(box = 1, due = tomorrow, lastAnswered = today)

        val correctThenWrong = scheduler.afterAnswer(
            scheduler.afterAnswer(inBox(3), correct = true, today = today),
            correct = false,
            today = today,
        )
        val wrongThenCorrect = scheduler.afterAnswer(
            scheduler.afterAnswer(inBox(3), correct = false, today = today),
            correct = true,
            today = today,
        )

        assertEquals(boxOne, correctThenWrong)
        assertEquals(boxOne, wrongThenCorrect)
    }
}
