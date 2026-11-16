package com.polisheasy.engine

import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class DailyUnlockingTest {
    private val hello = right.getValue("cześć")
    private val goodbye = right.getValue("do widzenia")
    private val thanks = right.getValue("dziękuję")

    private val learner = Learner()

    @Test
    fun `after completing a Daily Set the next one unlocks at the following local midnight`() {
        val engine = learner.openApp()

        engine.playToSummary(engine.startDailySet())

        assertEquals(Today.DoneForToday(nextUnlock = march(3).atStartOfDay(warsaw)), engine.today())
    }

    @Test
    fun `later the same day another Daily Set cannot be started`() {
        val engine = learner.openApp()
        engine.playToSummary(engine.startDailySet())

        learner.clock.moveTo(march(2), LocalTime.of(23, 59))

        assertFailsWith<IllegalStateException> { learner.openApp().startDailySet() }
        assertIs<Today.DoneForToday>(learner.openApp().today())
    }

    @Test
    fun `the next calendar day the next Daily Set on the Path is available`() {
        val engine = learner.openApp()
        engine.playToSummary(engine.startDailySet())

        learner.clock.moveTo(march(3), LocalTime.MIDNIGHT)
        val nextDay = learner.openApp()

        assertEquals(Today.DailySetAvailable, nextDay.today())
        assertEquals(listOf("tak"), prompts(listOf(nextDay.startDailySet())))
    }

    @Test
    fun `after five missed days the Learner resumes at the next Daily Set in order`() {
        val engine = learner.openApp()
        engine.playToSummary(engine.startDailySet())

        learner.clock.moveTo(march(8))
        val back = learner.openApp()

        assertEquals(Today.DailySetAvailable, back.today())
        assertEquals(listOf("tak"), prompts(listOf(back.startDailySet())))
    }

    @Test
    fun `a Daily Set started before midnight and completed after it belongs to the day it was completed`() {
        learner.clock.moveTo(march(2), LocalTime.of(23, 50))
        val engine = learner.openApp()
        engine.play(engine.startDailySet(), listOf(hello, goodbye))

        learner.clock.moveTo(march(3), LocalTime.of(0, 10))
        engine.answer(thanks)

        assertEquals(mapOf("a1-u01-d01" to march(3)), learner.progress.completedDailySets())
        assertEquals(Today.DoneForToday(nextUnlock = march(4).atStartOfDay(warsaw)), engine.today())
    }

    @Test
    fun `a Daily Set left unfinished yesterday does not use up today`() {
        val engine = learner.openApp()
        engine.play(engine.startDailySet(), listOf(hello))

        learner.clock.moveTo(march(3))

        assertEquals(Today.InProgress(SetProgress(correct = 1, total = 3)), learner.openApp().today())
    }

    @Test
    fun `in a course with nothing to review, everything is completed once the last Daily Set is, that day and later`() {
        val dayOne = learner.openApp()
        dayOne.playToSummary(dayOne.startDailySet())
        learner.clock.moveTo(march(3))
        val dayTwo = learner.openApp()

        dayTwo.playToSummary(dayTwo.startDailySet())

        assertEquals(Today.AllCompleted, dayTwo.today())
        learner.clock.moveTo(march(10))
        assertEquals(Today.AllCompleted, learner.openApp().today())
    }
}
