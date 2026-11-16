package com.polisheasy.engine

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReminderTest {
    private val hello = right.getValue("cześć")
    private val yes = right.getValue("tak")
    private val no = right.getValue("nie")

    private val lastMinute = LocalTime.of(23, 59)
    private val sevenInTheEvening = LocalTime.of(19, 0)

    private val abc = listOf(item("a"), item("b"), item("c"), item("d"), item("e"))
    private val d1 = day("d1", choose("d1-e1", "one", "a"), choose("d1-e2", "two", "b"), choose("d1-e3", "three", "c"))
    private val d2 = day("d2", choose("d2-e1", "four", "d"), choose("d2-e2", "five", "e"))

    /** The small course with Items: two new Daily Sets, then a Review-only Daily Set every day. */
    private fun smallCourseLearner() = ItemLearner(courseOf(abc, d1, d2))

    /** A Learner of the fixture course who completed its first Daily Set on 2 March. */
    private fun dayOneCompleted(): Learner = Learner().apply {
        val engine = openApp()
        engine.playToSummary(engine.startDailySet())
    }

    private fun warsawTime(date: LocalDate, time: LocalTime): ZonedDateTime = ZonedDateTime.of(date, time, warsaw)

    @Test
    fun `a new Learner's reminder is due`() {
        assertTrue(newLearnerEngine().reminderDue())
    }

    @Test
    fun `the reminder is due while today's Daily Set is in progress`() {
        val engine = newLearnerEngine()

        engine.play(engine.startDailySet(), listOf(hello))

        assertIs<Today.InProgress>(engine.today())
        assertTrue(engine.reminderDue())
    }

    @Test
    fun `the reminder is not due once today's Daily Set is completed`() {
        val learner = Learner()
        val engine = learner.openApp()

        engine.playToSummary(engine.startDailySet())

        assertFalse(engine.reminderDue())
        assertFalse(learner.openApp().reminderDue())
        learner.clock.moveTo(march(2), lastMinute)
        assertFalse(learner.openApp().reminderDue())
    }

    @Test
    fun `the reminder is due again from midnight`() {
        val learner = dayOneCompleted()

        learner.clock.moveTo(march(3), LocalTime.MIDNIGHT)

        assertTrue(learner.openApp().reminderDue())
    }

    @Test
    fun `a Daily Set started on an earlier day and not finished keeps the reminder due`() {
        val learner = Learner()
        val engine = learner.openApp()
        engine.play(engine.startDailySet(), listOf(hello))

        learner.clock.moveTo(march(3))

        assertTrue(learner.openApp().reminderDue())
    }

    @Test
    fun `a Daily Set finished after midnight silences that day's reminder`() {
        val learner = dayOneCompleted()
        learner.clock.moveTo(march(3), LocalTime.of(23, 50))
        val engine = learner.openApp()
        engine.play(engine.startDailySet(), listOf(yes))

        learner.clock.moveTo(march(4), LocalTime.of(0, 10))
        assertTrue(engine.reminderDue())

        engine.play(engine.next(), listOf(no))
        assertFalse(engine.reminderDue())
    }

    @Test
    fun `the reminder covers Review-only Daily Sets`() {
        val learner = smallCourseLearner()
        learner.playDay(march(2))
        learner.playDay(march(3))

        learner.clock.moveTo(march(4))
        val engine = learner.openApp()
        assertEquals(Today.ReviewOnlyAvailable, engine.today())
        assertTrue(engine.reminderDue())

        val next = learner.play(engine, learner.open(engine), answers = 2).last()
        assertIs<Today.InProgress>(engine.today())
        assertTrue(engine.reminderDue())

        learner.play(engine, next)
        assertFalse(engine.reminderDue())

        learner.clock.moveTo(march(5))
        assertTrue(learner.openApp().reminderDue())
    }

    @Test
    fun `the reminder is never due when nothing is left to do`() {
        val learner = dayOneCompleted()
        learner.clock.moveTo(march(3))
        val second = learner.openApp()
        second.playToSummary(second.startDailySet())

        learner.clock.moveTo(march(4))
        val engine = learner.openApp()

        assertEquals(Today.AllCompleted, engine.today())
        assertFalse(engine.reminderDue())
    }

    @Test
    fun `a Replay does not change the reminder`() {
        val learner = dayOneCompleted()
        val sameDay = learner.openApp()

        sameDay.playToSummary(sameDay.startReplay("a1-u01-d01"))
        assertFalse(sameDay.reminderDue())

        learner.clock.moveTo(march(3))
        val nextDay = learner.openApp()
        nextDay.playToSummary(nextDay.startReplay("a1-u01-d01"))
        assertTrue(nextDay.reminderDue())
    }

    @Test
    fun `a reminder time still ahead today is today`() {
        assertEquals(
            warsawTime(march(2), sevenInTheEvening),
            nextReminderTime(warsawTime(march(2), LocalTime.of(9, 0)), sevenInTheEvening),
        )
    }

    @Test
    fun `a reminder time already passed is tomorrow`() {
        assertEquals(
            warsawTime(march(3), sevenInTheEvening),
            nextReminderTime(warsawTime(march(2), LocalTime.of(20, 0)), sevenInTheEvening),
        )
    }

    @Test
    fun `at the reminder time itself the next one is tomorrow`() {
        assertEquals(
            warsawTime(march(3), sevenInTheEvening),
            nextReminderTime(warsawTime(march(2), sevenInTheEvening), sevenInTheEvening),
        )
    }

    @Test
    fun `a reminder time skipped by the clocks going forward comes after the change`() {
        val next = nextReminderTime(warsawTime(march(28), LocalTime.of(23, 0)), LocalTime.of(2, 30))

        assertEquals(warsawTime(march(29), LocalTime.of(3, 30)), next)
        assertEquals("+02:00", next.offset.id)
    }
}
