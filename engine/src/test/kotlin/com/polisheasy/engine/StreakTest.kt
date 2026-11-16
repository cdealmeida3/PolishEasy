package com.polisheasy.engine

import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class StreakTest {
    private val hello = right.getValue("cześć")
    private val goodbye = right.getValue("do widzenia")
    private val thanks = right.getValue("dziękuję")
    private val yes = right.getValue("tak")
    private val no = right.getValue("nie")

    private val lastMinute = LocalTime.of(23, 59)

    private val abc = listOf(item("a"), item("b"), item("c"), item("d"), item("e"))
    private val d1 = day("d1", choose("d1-e1", "one", "a"), choose("d1-e2", "two", "b"), choose("d1-e3", "three", "c"))
    private val d2 = day("d2", choose("d2-e1", "four", "d"), choose("d2-e2", "five", "e"))

    /** The small course with Items: two new Daily Sets, then a Review-only Daily Set every day. */
    private fun smallCourseLearner(progress: LearnerProgress = InMemoryLearnerProgress()) =
        ItemLearner(courseOf(abc, d1, d2), progress)

    /** The small course played on each day from 2 March to [lastDay] March. */
    private fun smallCoursePlayedTo(lastDay: Int): ItemLearner =
        smallCourseLearner().apply { (2..lastDay).forEach { playDay(march(it)) } }

    /** A Learner of the fixture course who completed its first Daily Set on 2 March. */
    private fun dayOneCompleted(): Learner = Learner().apply {
        val engine = openApp()
        engine.playToSummary(engine.startDailySet())
    }

    @Test
    fun `a new Learner has a Streak of zero`() {
        assertEquals(0, newLearnerEngine().streak())
    }

    @Test
    fun `completing today's Daily Set makes the Streak one, and a Daily Set only started does not count`() {
        val learner = Learner()
        val engine = learner.openApp()

        engine.play(engine.startDailySet(), listOf(hello, goodbye))
        assertEquals(0, engine.streak())

        engine.play(engine.next(), listOf(thanks))
        assertEquals(1, engine.streak())
        assertEquals(1, learner.openApp().streak())
    }

    @Test
    fun `the next day the Streak earned up to yesterday is still shown, and completing that day's Daily Set raises it by one`() {
        val learner = dayOneCompleted()

        learner.clock.moveTo(march(3), LocalTime.MIDNIGHT)
        val engine = learner.openApp()
        assertEquals(1, engine.streak())

        engine.play(engine.startDailySet(), listOf(yes))
        assertEquals(1, engine.streak())

        engine.play(engine.next(), listOf(no))
        assertEquals(2, engine.streak())

        learner.clock.moveTo(march(3), lastMinute)
        assertEquals(2, learner.openApp().streak())
    }

    @Test
    fun `a day with no completed Daily Set resets the Streak to zero`() {
        val learner = dayOneCompleted()

        learner.clock.moveTo(march(4))
        val engine = learner.openApp()
        assertEquals(0, engine.streak())

        engine.playToSummary(engine.startDailySet())
        assertEquals(1, engine.streak())
    }

    @Test
    fun `the Streak lasts until the end of the day after the last completion`() {
        val learner = dayOneCompleted()

        learner.clock.moveTo(march(3), lastMinute)
        assertEquals(1, learner.openApp().streak())

        learner.clock.moveTo(march(4), LocalTime.MIDNIGHT)
        assertEquals(0, learner.openApp().streak())
    }

    @Test
    fun `Daily Sets completed just before and just after midnight are two days of Streak`() {
        val learner = Learner()

        learner.clock.moveTo(march(2), LocalTime.of(23, 50))
        val evening = learner.openApp()
        evening.playToSummary(evening.startDailySet())
        assertEquals(1, evening.streak())

        learner.clock.moveTo(march(3), LocalTime.of(0, 10))
        val night = learner.openApp()
        night.playToSummary(night.startDailySet())
        assertEquals(2, night.streak())
    }

    @Test
    fun `a Daily Set started before midnight and completed after it counts for the day it was completed`() {
        val learner = dayOneCompleted()

        learner.clock.moveTo(march(3), LocalTime.of(23, 50))
        val engine = learner.openApp()
        engine.play(engine.startDailySet(), listOf(yes))

        learner.clock.moveTo(march(4), LocalTime.of(0, 10))
        assertEquals(0, engine.streak())

        engine.play(engine.next(), listOf(no))
        assertEquals(1, engine.streak())
        assertEquals(march(4), learner.progress.completedDailySets()["a1-u01-d02"])
    }

    @Test
    fun `new and Review-only Daily Sets count together`() {
        val learner = smallCoursePlayedTo(lastDay = 5)
        assertEquals(setOf(march(4), march(5)), learner.progress.reviewOnlyDays())
        assertEquals(4, learner.openApp().streak())

        learner.clock.moveTo(march(6))
        assertEquals(4, learner.openApp().streak())

        learner.playDay(march(6))
        assertEquals(5, learner.openApp().streak())
    }

    @Test
    fun `a missed Review-only day resets the Streak, and the next Review-only Daily Set starts it again`() {
        val learner = smallCoursePlayedTo(lastDay = 5)

        learner.clock.moveTo(march(7))
        assertEquals(0, learner.openApp().streak())

        learner.playDay(march(7))
        assertEquals(1, learner.openApp().streak())

        learner.playDay(march(8))
        assertEquals(2, learner.openApp().streak())
    }

    @Test
    fun `a Review-only Daily Set left unfinished does not count`() {
        val learner = smallCoursePlayedTo(lastDay = 3)

        learner.clock.moveTo(march(4))
        val engine = learner.openApp()
        learner.play(engine, learner.open(engine), answers = 2)
        assertEquals(2, engine.streak())

        learner.clock.moveTo(march(5))
        assertEquals(0, learner.openApp().streak())
    }

    @Test
    fun `a Streak runs across the end of a month`() {
        val progress = InMemoryLearnerProgress()
        val firstDay = LocalDate.of(2026, 2, 25)
        (0L..5L).forEach { progress.recordReviewOnlyCompleted(on = firstDay.plusDays(it)) }
        val learner = smallCourseLearner(progress)

        assertEquals(6, learner.openApp().streak())

        learner.clock.moveTo(march(3))
        assertEquals(6, learner.openApp().streak())

        learner.clock.moveTo(march(4))
        assertEquals(0, learner.openApp().streak())
    }

    @Test
    fun `the day a Daily Set was completed counts once however it is recorded`() {
        val progress = InMemoryLearnerProgress()
        progress.recordCompleted("d1", on = march(2))
        progress.recordCompleted("d2", on = march(2))
        progress.recordReviewOnlyCompleted(on = march(2))

        assertEquals(1, smallCourseLearner(progress).openApp().streak())
    }
}
