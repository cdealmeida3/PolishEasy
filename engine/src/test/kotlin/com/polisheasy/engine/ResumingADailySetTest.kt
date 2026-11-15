package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ResumingADailySetTest {
    private val hello = right.getValue("cześć")
    private val goodbye = right.getValue("do widzenia")
    private val thanks = right.getValue("dziękuję")

    private val learner = Learner()

    @Test
    fun `once a Daily Set is started it is in progress with nothing answered`() {
        val engine = learner.openApp()

        engine.startDailySet()

        assertEquals(Today.InProgress(SetProgress(correct = 0, total = 3)), engine.today())
    }

    @Test
    fun `after each answer, reopening the app and resuming shows the same next Exercise`() {
        val engine = learner.openApp()

        val afterWrong = engine.play(engine.startDailySet(), listOf(wrongHello)).last()
        assertEquals(afterWrong, learner.openApp().resumeDailySet())

        engine.answer(goodbye)
        val afterCorrect = engine.next()
        assertEquals("Tip: Saying thank you", describe(afterCorrect))
        assertEquals(afterCorrect, learner.openApp().resumeDailySet())
    }

    @Test
    fun `a wrongly answered Exercise still returns at the end after reopening`() {
        val engine = learner.openApp()
        engine.play(engine.startDailySet(), listOf(wrongHello, goodbye))

        val reopened = learner.openApp()
        val steps = reopened.play(reopened.resumeDailySet(), listOf(thanks, hello))

        assertEquals(listOf("dziękuję", "cześć"), prompts(steps))
        assertIs<Step.ShowSummary>(steps.last())
    }

    @Test
    fun `resuming on a later day continues at the same Exercise`() {
        val engine = learner.openApp()
        val beforeClosing = engine.play(engine.startDailySet(), listOf(hello)).last()

        learner.clock.moveTo(march(5))

        assertEquals("do widzenia", describe(beforeClosing))
        assertEquals(beforeClosing, learner.openApp().resumeDailySet())
    }

    @Test
    fun `a Tip is shown again after resuming only if its Exercise was not yet answered`() {
        val engine = learner.openApp()
        // The first Tip was followed by an answer; the second was seen but its Exercise was not answered.
        val beforeClosing = engine.play(engine.startDailySet(), listOf(wrongHello, goodbye))
        assertEquals(
            listOf("Tip: Saying hello", "cześć", "do widzenia", "Tip: Saying thank you", "dziękuję"),
            beforeClosing.map(::describe),
        )

        val reopened = learner.openApp()
        val steps = reopened.play(reopened.resumeDailySet(), listOf(thanks, hello))

        assertEquals(listOf("Tip: Saying thank you", "dziękuję", "cześć", "Summary"), steps.map(::describe))
    }

    @Test
    fun `the progress bar after resuming is where it was before closing`() {
        val engine = learner.openApp()
        val beforeClosing = engine.play(engine.startDailySet(), listOf(hello, goodbye)).last()

        val resumed = learner.openApp().resumeDailySet()

        assertEquals(SetProgress(correct = 2, total = 3), (beforeClosing as Step.ShowExercise).progress)
        assertEquals(SetProgress(correct = 2, total = 3), (resumed as Step.ShowTip).progress)
    }

    @Test
    fun `the Summary of a resumed Daily Set counts a miss made before closing`() {
        val engine = learner.openApp()
        engine.play(engine.startDailySet(), listOf(wrongHello, goodbye))

        val reopened = learner.openApp()
        val steps = reopened.play(reopened.resumeDailySet(), listOf(thanks, hello))

        assertEquals(Step.ShowSummary(correctAnswers = 2, exercises = 3), steps.last())
    }

    @Test
    fun `resuming fails when no Daily Set is in progress`() {
        assertFailsWith<IllegalStateException> { learner.openApp().resumeDailySet() }

        val engine = learner.openApp()
        engine.playToSummary(engine.startDailySet())

        assertFailsWith<IllegalStateException> { learner.openApp().resumeDailySet() }
    }

    @Test
    fun `a saved Daily Set whose Exercises no longer exist in the content is ignored`() {
        learner.progress.saveInProgress(
            DailySetInProgress(
                dailySetId = "a1-u01-d01",
                remaining = listOf("a1-u01-d01-e02", "a1-u01-d01-e99"),
                missed = setOf("a1-u01-d01-e99"),
            ),
        )
        val engine = learner.openApp()

        assertEquals(Today.DailySetAvailable, engine.today())
        val steps = engine.play(engine.startDailySet(), listOf(hello, goodbye, thanks))
        assertEquals(
            listOf("Tip: Saying hello", "cześć", "do widzenia", "Tip: Saying thank you", "dziękuję", "Summary"),
            steps.map(::describe),
        )
        assertEquals(Step.ShowSummary(correctAnswers = 3, exercises = 3), steps.last())
    }
}
