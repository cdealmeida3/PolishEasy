package com.polisheasy.engine

import java.time.Clock
import java.time.LocalDate

/** What the Learner can do today. */
sealed interface Today {
    data object DailySetAvailable : Today

    data object DoneForToday : Today
}

/** What the Daily Set player shows next. */
sealed interface Step {
    data class ShowExercise(val exercise: ExerciseView) : Step

    data class ShowSummary(val correctAnswers: Int, val exercises: Int) : Step
}

/** An Exercise as the Learner sees it: Base Language text resolved, answer withheld. */
sealed interface ExerciseView {
    data class ChooseTranslation(val prompt: String, val options: List<String>) : ExerciseView
}

/** The Learner's response to an Exercise. */
sealed interface Answer {
    data class Option(val index: Int) : Answer
}

data class Feedback(val correct: Boolean, val correctAnswer: String)

/**
 * All course rules. Screens ask it what to show and send it the Learner's answers;
 * it knows nothing about Android.
 */
class CourseEngine(
    private val course: Course,
    private val progress: LearnerProgress,
    private val clock: Clock,
    private val baseLanguage: String = "en",
) {
    private var dailySet: DailySet? = null
    private var position = 0
    private var correctAnswers = 0

    fun today(): Today =
        if (nextDailySet() == null) Today.DoneForToday else Today.DailySetAvailable

    fun startDailySet(): Step {
        dailySet = checkNotNull(nextDailySet()) { "No Daily Set is available" }
        position = 0
        correctAnswers = 0
        return next()
    }

    /** Checks the Learner's answer to the Exercise currently shown. */
    fun answer(answer: Answer): Feedback {
        val exercise = current().exercises[position]
        val feedback = check(exercise, answer)
        if (feedback.correct) correctAnswers++
        position++
        if (position == current().exercises.size) {
            progress.recordCompleted(current().id, on = LocalDate.now(clock))
        }
        return feedback
    }

    fun next(): Step {
        val set = current()
        val exercise = set.exercises.getOrNull(position)
            ?: return Step.ShowSummary(correctAnswers, set.exercises.size)
        return Step.ShowExercise(view(exercise))
    }

    /** The first Daily Set on the Path the Learner has not completed. */
    private fun nextDailySet(): DailySet? {
        val completed = progress.completedDailySets()
        return course.path.firstOrNull { it.id !in completed }
    }

    private fun current(): DailySet = checkNotNull(dailySet) { "No Daily Set has been started" }

    private fun view(exercise: Exercise): ExerciseView = when (exercise) {
        is ChooseTranslation -> ExerciseView.ChooseTranslation(
            prompt = exercise.prompt,
            options = exercise.options.map(::localize),
        )
    }

    private fun check(exercise: Exercise, answer: Answer): Feedback = when (exercise) {
        is ChooseTranslation -> {
            val chosen = (answer as Answer.Option).index
            Feedback(
                correct = chosen == exercise.correctOption,
                correctAnswer = localize(exercise.options[exercise.correctOption]),
            )
        }
    }

    private fun localize(text: Localized): String = text.getValue(baseLanguage)
}
