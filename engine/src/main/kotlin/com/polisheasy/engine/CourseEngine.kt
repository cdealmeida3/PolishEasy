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
    data class ShowTip(val tip: TipView, val progress: SetProgress) : Step

    data class ShowExercise(val exercise: ExerciseView, val progress: SetProgress) : Step

    /** [correctAnswers] counts the Exercises answered correctly on the first attempt. */
    data class ShowSummary(val correctAnswers: Int, val exercises: Int) : Step
}

/** How far through the Daily Set the Learner is: Exercises answered correctly out of all of them. */
data class SetProgress(val correct: Int, val total: Int)

/** A Tip as the Learner sees it, in the Base Language. */
data class TipView(val title: String, val body: String)

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

    /** Exercises still to be answered correctly; the head is the one being shown. */
    private val queue = ArrayDeque<Exercise>()
    private val pendingTips = mutableListOf<Tip>()

    /** Ids of Exercises answered wrongly at least once. */
    private val missed = mutableSetOf<String>()
    private var firstAttemptCorrect = 0

    fun today(): Today =
        if (nextDailySet() == null) Today.DoneForToday else Today.DailySetAvailable

    fun startDailySet(): Step {
        val set = checkNotNull(nextDailySet()) { "No Daily Set is available" }
        dailySet = set
        queue.clear()
        queue.addAll(set.exercises)
        pendingTips.clear()
        pendingTips.addAll(set.tips)
        missed.clear()
        firstAttemptCorrect = 0
        return next()
    }

    /**
     * Checks the Learner's answer to the Exercise currently shown. A wrongly answered
     * Exercise goes to the end of the queue and returns until it is answered correctly.
     */
    fun answer(answer: Answer): Feedback {
        val set = current()
        check(queue.isNotEmpty()) { "Every Exercise of Daily Set ${set.id} has already been answered" }
        val exercise = queue.removeFirst()
        val feedback = check(exercise, answer)
        if (feedback.correct) {
            if (exercise.id !in missed) firstAttemptCorrect++
        } else {
            missed += exercise.id
            queue.addLast(exercise)
        }
        if (queue.isEmpty()) progress.recordCompleted(set.id, on = LocalDate.now(clock))
        return feedback
    }

    fun next(): Step {
        val set = current()
        val exercise = queue.firstOrNull()
            ?: return Step.ShowSummary(firstAttemptCorrect, set.exercises.size)
        val progress = SetProgress(correct = set.exercises.size - queue.size, total = set.exercises.size)
        val tip = pendingTips.firstOrNull { it.before == exercise.id }
        if (tip != null) {
            pendingTips -= tip
            return Step.ShowTip(TipView(localize(tip.title), localize(tip.body)), progress)
        }
        return Step.ShowExercise(view(exercise), progress)
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
