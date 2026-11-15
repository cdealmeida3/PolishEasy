package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlayingADailySetTest {
    private val hello = right.getValue("cześć")
    private val goodbye = right.getValue("do widzenia")
    private val thanks = right.getValue("dziękuję")

    @Test
    fun `a new Learner has a Daily Set ready to start`() {
        val engine = newLearnerEngine()

        assertEquals(Today.DailySetAvailable, engine.today())
    }

    @Test
    fun `starting the Daily Set shows the Tip for its first Exercise in the Base Language`() {
        val engine = newLearnerEngine()

        val step = engine.startDailySet()

        assertEquals(
            Step.ShowTip(
                TipView(title = "Saying hello", body = "Cześć is an informal hello."),
                SetProgress(correct = 0, total = 3),
            ),
            step,
        )
    }

    @Test
    fun `after the Tip comes the first Exercise in the Base Language`() {
        val engine = newLearnerEngine()
        engine.startDailySet()

        val step = engine.next()

        assertEquals(
            Step.ShowExercise(
                ExerciseView.ChooseTranslation(
                    prompt = "cześć",
                    options = listOf("thank you", "hi", "goodbye"),
                ),
                SetProgress(correct = 0, total = 3),
            ),
            step,
        )
    }

    @Test
    fun `a correct answer is confirmed`() {
        val engine = newLearnerEngine()
        engine.play(engine.startDailySet(), emptyList())

        val feedback = engine.answer(hello)

        assertEquals(Feedback(correct = true, correctAnswer = "hi"), feedback)
    }

    @Test
    fun `a wrong answer shows the correct one`() {
        val engine = newLearnerEngine()
        engine.play(engine.startDailySet(), emptyList())

        val feedback = engine.answer(wrongHello)

        assertEquals(Feedback(correct = false, correctAnswer = "hi"), feedback)
    }

    @Test
    fun `Tips appear just before their Exercise and Exercises follow authored order`() {
        val engine = newLearnerEngine()

        val steps = engine.play(engine.startDailySet(), listOf(hello, goodbye, thanks))

        assertEquals(
            listOf("Tip: Saying hello", "cześć", "do widzenia", "Tip: Saying thank you", "dziękuję", "Summary"),
            steps.map(::describe),
        )
    }

    @Test
    fun `a wrongly answered Exercise returns after the remaining Exercises`() {
        val engine = newLearnerEngine()

        val steps = engine.play(engine.startDailySet(), listOf(wrongHello, goodbye, thanks, hello))

        assertEquals(listOf("cześć", "do widzenia", "dziękuję", "cześć"), prompts(steps))
        assertIs<Step.ShowSummary>(steps.last())
    }

    @Test
    fun `a Tip is not shown again when its Exercise returns`() {
        val engine = newLearnerEngine()

        val steps = engine.play(engine.startDailySet(), listOf(wrongHello, goodbye, thanks, hello))

        assertEquals(1, steps.count { it is Step.ShowTip && it.tip.title == "Saying hello" })
    }

    @Test
    fun `an Exercise keeps returning until it is answered correctly`() {
        val engine = newLearnerEngine()

        val steps = engine.play(
            engine.startDailySet(),
            listOf(wrongHello, goodbye, thanks, wrongHello, wrongHello, hello),
        )

        assertEquals(listOf("cześć", "do widzenia", "dziękuję", "cześć", "cześć", "cześć"), prompts(steps))
        assertIs<Step.ShowSummary>(steps.last())
    }

    @Test
    fun `the Daily Set is completed only when every Exercise has been answered correctly`() {
        val engine = newLearnerEngine()

        engine.play(engine.startDailySet(), listOf(wrongHello, goodbye, thanks))
        assertIs<Today.InProgress>(engine.today())

        engine.answer(hello)
        assertIs<Today.DoneForToday>(engine.today())
    }

    @Test
    fun `progress counts Exercises answered correctly and does not move on a wrong answer`() {
        val engine = newLearnerEngine()

        val steps = engine.play(engine.startDailySet(), listOf(wrongHello, goodbye, thanks, hello))

        assertEquals(
            listOf(
                "Tip: Saying hello" to SetProgress(0, 3),
                "cześć" to SetProgress(0, 3),
                "do widzenia" to SetProgress(0, 3),
                "Tip: Saying thank you" to SetProgress(1, 3),
                "dziękuję" to SetProgress(1, 3),
                "cześć" to SetProgress(2, 3),
            ),
            steps.dropLast(1).map { step ->
                describe(step) to when (step) {
                    is Step.ShowTip -> step.progress
                    is Step.ShowExercise -> step.progress
                    is Step.ShowSummary -> error("The Summary should come last")
                }
            },
        )
    }

    @Test
    fun `the Summary counts every Exercise when all are answered correctly first time`() {
        val engine = newLearnerEngine()

        val steps = engine.play(engine.startDailySet(), listOf(hello, goodbye, thanks))

        assertEquals(Step.ShowSummary(correctAnswers = 3, exercises = 3), steps.last())
    }

    @Test
    fun `the Summary counts only Exercises answered correctly on the first attempt`() {
        val engine = newLearnerEngine()

        val steps = engine.play(engine.startDailySet(), listOf(wrongHello, goodbye, thanks, wrongHello, hello))

        assertEquals(Step.ShowSummary(correctAnswers = 2, exercises = 3), steps.last())
    }

    @Test
    fun `finishing the Daily Set means the Learner is done for today, even after reopening the app`() {
        val learner = Learner()
        val engine = learner.openApp()

        engine.play(engine.startDailySet(), listOf(hello, goodbye, thanks))

        assertIs<Today.DoneForToday>(engine.today())
        assertIs<Today.DoneForToday>(learner.openApp().today())
    }
}
