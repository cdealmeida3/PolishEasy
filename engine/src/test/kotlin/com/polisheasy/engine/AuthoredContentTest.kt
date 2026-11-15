package com.polisheasy.engine

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthoredContentTest {
    private val contentDir = File(System.getProperty("contentDir"))
    private val course = loadCourse { path -> File(contentDir, path).readText() }

    /** The correct answer to the Exercise shown, found in the content by what the Learner sees. */
    private fun correctAnswer(view: ExerciseView): Answer {
        val exercises = course.exercises.values
        return when (view) {
            is ExerciseView.ChooseTranslation -> Answer.Option(
                exercises.filterIsInstance<ChooseTranslation>()
                    .first { it.prompt == view.prompt && it.options.map { option -> option.getValue("en") } == view.options }
                    .correctOption,
            )
            is ExerciseView.TypeAnswer -> Answer.Typed(
                exercises.filterIsInstance<TypeAnswer>()
                    .first { it.prompt.getValue("en") == view.prompt }
                    .acceptedAnswers.first(),
            )
        }
    }

    /** Plays today's Daily Set, Review included, answering everything correctly. Returns every Step seen. */
    private fun CourseEngine.playCorrectly(): List<Step> {
        val seen = mutableListOf(startDailySet())
        while (true) {
            when (val step = seen.last()) {
                is Step.ShowTip -> Unit
                is Step.ShowExercise -> answer(correctAnswer(step.exercise))
                is Step.ShowSummary -> return seen
            }
            seen += next()
        }
    }

    /** One Learner playing the whole course, one Daily Set per day from 2 March. Returns each day's Steps. */
    private fun playTheCourse(progress: LearnerProgress, clock: FakeClock): List<List<Step>> =
        course.path.indices.map { day ->
            clock.moveTo(march(2).plusDays(day.toLong()))
            CourseEngine(course, progress, clock).playCorrectly()
        }

    @Test
    fun `every Daily Set shipped with the app can be played through to its Summary`() {
        assertTrue(course.path.isNotEmpty())
        val progress = InMemoryLearnerProgress()
        val clock = FakeClock()

        val days = playTheCourse(progress, clock)

        course.path.zip(days).forEach { (dailySet, steps) ->
            val summary = steps.last() as Step.ShowSummary
            assertEquals(summary.exercises, summary.correctAnswers, "Daily Set ${dailySet.id}")
            assertTrue(summary.exercises >= dailySet.exercises.size, "Daily Set ${dailySet.id}")
            assertEquals(
                course.introducedIn(dailySet).map { ItemView(it.text, it.meaning.getValue("en")) },
                summary.itemsLearned,
                "Items learned in ${dailySet.id}",
            )
            assertEquals(dailySet.tips.size, steps.count { it is Step.ShowTip }, "Tips of ${dailySet.id}")
        }

        assertEquals(Today.AllCompleted, CourseEngine(course, progress, clock).today())
    }

    @Test
    fun `every Exercise shipped lists at least one Item, and every Item is listed by an Exercise`() {
        val exercises = course.path.flatMap { it.exercises }
        assertTrue(course.items.isNotEmpty())

        assertEquals(emptyList(), exercises.filter { it.items.isEmpty() }.map { it.id })
        assertEquals(course.items.map { it.id }.toSet(), exercises.flatMap { it.items }.toSet())
    }

    @Test
    fun `played one per day, day 1 has no Review, day 2 ends with 7 Review Exercises and day 3 with 4`() {
        val days = playTheCourse(InMemoryLearnerProgress(), FakeClock())

        assertEquals(listOf(0, 7, 4), days.map { steps -> steps.count { it is Step.ShowExercise && it.review } })
        assertEquals(
            listOf(7, 6, 0),
            days.map { steps -> (steps.last() as Step.ShowSummary).itemsLearned.size },
        )
    }

    @Test
    fun `every Accepted Answer shipped with the app is correct when typed exactly, with no accent warning`() {
        val typing = course.path.flatMap { it.exercises }.filterIsInstance<TypeAnswer>()
        assertTrue(typing.isNotEmpty())

        typing.forEach { exercise ->
            exercise.acceptedAnswers.forEach { accepted ->
                assertEquals(
                    Feedback(correct = true, correctAnswer = accepted),
                    checkTyped(accepted, exercise.acceptedAnswers),
                    "Exercise ${exercise.id}",
                )
            }
        }
    }
}
