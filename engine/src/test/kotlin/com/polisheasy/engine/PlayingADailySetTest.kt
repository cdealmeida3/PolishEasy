package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayingADailySetTest {
    @Test
    fun `a new Learner has a Daily Set ready to start`() {
        val engine = newLearnerEngine()

        assertEquals(Today.DailySetAvailable, engine.today())
    }

    @Test
    fun `starting the Daily Set shows its first Exercise in the Base Language`() {
        val engine = newLearnerEngine()

        val step = engine.startDailySet()

        assertEquals(
            Step.ShowExercise(
                ExerciseView.ChooseTranslation(
                    prompt = "cześć",
                    options = listOf("thank you", "hi", "goodbye"),
                ),
            ),
            step,
        )
    }

    @Test
    fun `a correct answer is confirmed and leads to the Summary`() {
        val engine = newLearnerEngine()
        engine.startDailySet()

        val feedback = engine.answer(Answer.Option(1))

        assertEquals(Feedback(correct = true, correctAnswer = "hi"), feedback)
        assertEquals(Step.ShowSummary(correctAnswers = 1, exercises = 1), engine.next())
    }

    @Test
    fun `a wrong answer shows the correct one`() {
        val engine = newLearnerEngine()
        engine.startDailySet()

        val feedback = engine.answer(Answer.Option(0))

        assertEquals(Feedback(correct = false, correctAnswer = "hi"), feedback)
        assertEquals(Step.ShowSummary(correctAnswers = 0, exercises = 1), engine.next())
    }

    @Test
    fun `finishing the Daily Set means the Learner is done for today, even after reopening the app`() {
        val course = FixtureCourse.load()
        val progress = InMemoryLearnerProgress()
        val clock = FakeClock()
        val engine = CourseEngine(course, progress, clock)
        engine.startDailySet()

        engine.answer(Answer.Option(1))

        assertEquals(Today.DoneForToday, engine.today())
        assertEquals(Today.DoneForToday, CourseEngine(course, progress, clock).today())
    }
}
