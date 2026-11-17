package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PracticeOpensAfterTodaysDailySetTest {
    @Test
    fun `Practice is locked for a Learner who has not done today's Daily Set`() {
        val player = Player()

        assertFalse(player.practice().view().open)
    }

    @Test
    fun `Practice is locked while today's Daily Set is in progress`() {
        val learner = Learner()
        val engine = learner.openApp()
        engine.startDailySet()

        assertFalse(engine.practiceOpen())
    }

    @Test
    fun `Practice opens once today's Daily Set is completed`() {
        val learner = Learner()
        val engine = learner.openApp()
        engine.playToSummary(engine.startDailySet())

        assertTrue(engine.practiceOpen())
        assertTrue(engine.practice(InMemoryPracticeProgress()).view().open)
    }

    @Test
    fun `Practice locks again at midnight`() {
        val player = Player().completed("d1")
        assertTrue(player.practice().view().open)

        player.clock.moveTo(march(3))

        assertFalse(player.practice().view().open)
    }

    @Test
    fun `a completed Review-only Daily Set opens Practice too`() {
        val player = Player().completed("d1", "d2", "d3")
        player.clock.moveTo(march(3))
        assertFalse(player.practice().view().open)

        player.progress.recordReviewOnlyCompleted(march(3))

        assertTrue(player.practice().view().open)
    }

    @Test
    fun `a Replay does not open Practice`() {
        val player = Player().completed("d1")
        player.clock.moveTo(march(3))
        val engine = player.engine

        engine.startReplay("d1")

        assertFalse(engine.practiceOpen())
    }

    @Test
    fun `Practice is open when nothing is left to do`() {
        // A course with no Items has nothing to review once its Path is completed.
        val learner = Learner()
        val engine = learner.openApp()
        engine.playToSummary(engine.startDailySet())
        learner.clock.moveTo(march(3))
        val nextDay = learner.openApp()
        nextDay.playToSummary(nextDay.startDailySet())
        learner.clock.moveTo(march(4))

        assertTrue(learner.openApp().practiceOpen())
    }

    @Test
    fun `no Round can be started while Practice is locked`() {
        val player = Player().completed("d1")
        val practice = player.practice()
        val setup = practice.setup(Game.VERB_CHALLENGE)
        player.clock.moveTo(march(3))

        assertFailsWith<IllegalStateException> { practice.start(setup) }
    }
}
