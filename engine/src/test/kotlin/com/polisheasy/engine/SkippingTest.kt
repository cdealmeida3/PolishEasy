package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** "Skip" on an Exercise: it leaves without an answer, changes no Item, and is not asked again. */
class SkippingTest {
    private val course = courseOf(
        listOf("a", "b", "c").map { item(it) },
        day("d1", typed("d1-e1", "say a", "a", "a"), typed("d1-e2", "say b", "b", "b"), typed("d1-e3", "say c", "c", "c")),
        day("d2", typed("d2-e1", "say a again", "a", "a")),
    )

    private fun exerciseId(step: Step): String = assertIs<Step.ShowExercise>(step).exerciseId

    @Test
    fun `a skipped Exercise is not asked again, reschedules nothing and is not counted right`() {
        val progress = InMemoryLearnerProgress()
        val engine = CourseEngine(course, progress, FakeClock())
        assertEquals("d1-e1", exerciseId(engine.startDailySet()))
        assertEquals("d1-e2", exerciseId(engine.skip()))
        assertEquals(emptyMap(), progress.itemProgress())

        engine.answer(Answer.Typed("wrong"))
        assertEquals("d1-e3", exerciseId(engine.next()))
        engine.answer(Answer.Typed("c"))
        // The one answered wrongly comes back, and is skipped too.
        assertEquals("d1-e2", exerciseId(engine.next()))
        val summary = assertIs<Step.ShowSummary>(engine.skip())
        assertEquals(1, summary.correctAnswers)
        assertEquals(3, summary.exercises)
        assertTrue("d1" in progress.completedDailySets(), "Skipping the last Exercise completes the Daily Set")
        assertEquals(setOf("b", "c"), progress.itemProgress().keys)
    }

    @Test
    fun `a skip is saved with the Daily Set in progress`() {
        val progress = InMemoryLearnerProgress()
        CourseEngine(course, progress, FakeClock()).apply {
            startDailySet()
            skip()
        }
        val again = CourseEngine(course, progress, FakeClock())
        assertEquals("d1-e2", exerciseId(again.resumeDailySet()))
        assertEquals(setOf("d1-e1"), progress.dailySetInProgress()?.skipped)
    }

    @Test
    fun `a skipped question of a Test counts as not right`() {
        val engine = CourseEngine(course, InMemoryLearnerProgress(), FakeClock())
        val unitId = course.levels.single().units.single().id
        var step = engine.startCheckpoint(unitId)
        while (step is Step.ShowExercise) step = engine.skip()
        val summary = assertIs<Step.ShowSummary>(step)
        assertEquals(0, summary.correctAnswers)
        assertEquals(0, summary.checkpoint?.percent)
    }
}
