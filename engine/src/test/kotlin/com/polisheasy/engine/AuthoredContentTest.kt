package com.polisheasy.engine

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthoredContentTest {
    private val contentDir = File(System.getProperty("contentDir"))
    private val course = loadCourse { path -> File(contentDir, path).readText() }

    @Test
    fun `every Daily Set shipped with the app can be played through to its Summary`() {
        assertTrue(course.path.isNotEmpty())
        val progress = InMemoryLearnerProgress()
        val clock = FakeClock()

        course.path.forEachIndexed { day, dailySet ->
            val correct = dailySet.exercises.map { exercise ->
                when (exercise) {
                    is ChooseTranslation -> Answer.Option(exercise.correctOption)
                }
            }
            // One Learner may complete only one Daily Set per day.
            clock.moveTo(march(2).plusDays(day.toLong()))
            val engine = CourseEngine(course, progress, clock)

            val steps = engine.play(engine.startDailySet(), correct)

            assertEquals(
                Step.ShowSummary(correctAnswers = dailySet.exercises.size, exercises = dailySet.exercises.size),
                steps.last(),
                "Daily Set ${dailySet.id}",
            )
            assertEquals(dailySet.tips.size, steps.count { it is Step.ShowTip }, "Tips of ${dailySet.id}")
        }

        assertEquals(Today.AllCompleted, CourseEngine(course, progress, clock).today())
    }
}
