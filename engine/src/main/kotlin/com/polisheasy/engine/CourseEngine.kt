package com.polisheasy.engine

import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime

/** What the Learner can do today. Home shows exactly one of these. */
sealed interface Today {
    data object DailySetAvailable : Today

    /** A Daily Set was started, today or earlier, and not finished. */
    data class InProgress(val progress: SetProgress) : Today

    /** A Daily Set was completed today. [nextUnlock] is the start of the next local calendar day. */
    data class DoneForToday(val nextUnlock: ZonedDateTime) : Today

    /** Every Daily Set released so far is completed. */
    data object AllCompleted : Today
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

    /** [prompt] is what to type in Polish, in the Base Language. */
    data class TypeAnswer(val prompt: String) : ExerciseView
}

/** The Learner's response to an Exercise. */
sealed interface Answer {
    data class Option(val index: Int) : Answer

    data class Typed(val text: String) : Answer
}

/**
 * An answer that is right apart from its accents is [correct] with [accentWarning] set;
 * [correctAnswer] then shows the proper spelling.
 */
data class Feedback(val correct: Boolean, val correctAnswer: String, val accentWarning: Boolean = false)

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
    /** The Daily Set being played, with how far the Learner is through it. */
    private var playing: Playing? = null

    private class Playing(val dailySet: DailySet, var state: DailySetInProgress)

    fun today(): Today {
        val completed = progress.completedDailySets()
        val today = LocalDate.now(clock)
        if (course.path.all { it.id in completed }) return Today.AllCompleted
        if (today in completed.values) {
            return Today.DoneForToday(nextUnlock = today.plusDays(1).atStartOfDay(clock.zone))
        }
        val saved = savedDailySet() ?: return Today.DailySetAvailable
        return Today.InProgress(progressOf(saved))
    }

    /** Starts the next Daily Set on the Path. Fails unless [today] is [Today.DailySetAvailable]. */
    fun startDailySet(): Step {
        check(today() == Today.DailySetAvailable) { "No Daily Set is available to start: ${today()}" }
        val completed = progress.completedDailySets()
        val set = course.path.first { it.id !in completed }
        val state = DailySetInProgress(dailySetId = set.id, remaining = set.exercises.map { it.id })
        progress.saveInProgress(state)
        playing = Playing(set, state)
        return next()
    }

    /** Picks up the Daily Set in progress where it was left. Fails unless [today] is [Today.InProgress]. */
    fun resumeDailySet(): Step {
        check(today() is Today.InProgress) { "No Daily Set is in progress: ${today()}" }
        playing = checkNotNull(savedDailySet())
        return next()
    }

    /**
     * Checks the Learner's answer to the Exercise currently shown and saves where the Learner is.
     * A wrongly answered Exercise goes to the end of the queue and returns until it is answered
     * correctly. The Daily Set is completed, on the day of this answer, when none remain.
     */
    fun answer(answer: Answer): Feedback {
        val playing = current()
        val set = playing.dailySet
        val state = playing.state
        check(state.remaining.isNotEmpty()) { "Every Exercise of Daily Set ${set.id} has already been answered" }
        val exercise = set.exercise(state.remaining.first())
        val feedback = check(exercise, answer)
        val rest = state.remaining.drop(1)
        playing.state = if (feedback.correct) {
            state.copy(remaining = rest)
        } else {
            state.copy(remaining = rest + exercise.id, missed = state.missed + exercise.id)
        }
        if (playing.state.remaining.isEmpty()) {
            progress.recordCompleted(set.id, on = LocalDate.now(clock))
        } else {
            progress.saveInProgress(playing.state)
        }
        return feedback
    }

    fun next(): Step {
        val playing = current()
        val set = playing.dailySet
        val state = playing.state
        val exerciseId = state.remaining.firstOrNull()
            // First-attempt correct: every Exercise that was never answered wrongly.
            ?: return Step.ShowSummary(set.exercises.size - state.missed.size, set.exercises.size)
        val tip = set.tips.firstOrNull { it.before == exerciseId && it.id !in state.shownTips }
        if (tip != null) {
            // Saved with the next answer: a Tip seen but not followed by an answer is shown again on resume.
            playing.state = state.copy(shownTips = state.shownTips + tip.id)
            return Step.ShowTip(TipView(localize(tip.title), localize(tip.body)), progressOf(playing))
        }
        return Step.ShowExercise(view(set.exercise(exerciseId)), progressOf(playing))
    }

    /**
     * The saved Daily Set in progress, if it can still be played: its Daily Set is on the Path and
     * not completed, and every id it refers to still exists there. Otherwise the content changed
     * in an update, and the saved state is ignored.
     */
    private fun savedDailySet(): Playing? {
        val saved = progress.dailySetInProgress() ?: return null
        if (saved.dailySetId in progress.completedDailySets()) return null
        val set = course.path.firstOrNull { it.id == saved.dailySetId } ?: return null
        val exerciseIds = set.exercises.map { it.id }.toSet()
        val tipIds = set.tips.map { it.id }.toSet()
        val playable = saved.remaining.isNotEmpty() &&
            exerciseIds.containsAll(saved.remaining) &&
            exerciseIds.containsAll(saved.missed) &&
            tipIds.containsAll(saved.shownTips)
        return if (playable) Playing(set, saved) else null
    }

    private fun progressOf(playing: Playing): SetProgress {
        val total = playing.dailySet.exercises.size
        return SetProgress(correct = total - playing.state.remaining.size, total = total)
    }

    private fun DailySet.exercise(id: String): Exercise = exercises.first { it.id == id }

    private fun current(): Playing = checkNotNull(playing) { "No Daily Set has been started" }

    private fun view(exercise: Exercise): ExerciseView = when (exercise) {
        is ChooseTranslation -> ExerciseView.ChooseTranslation(
            prompt = exercise.prompt,
            options = exercise.options.map(::localize),
        )
        is TypeAnswer -> ExerciseView.TypeAnswer(localize(exercise.prompt))
    }

    private fun check(exercise: Exercise, answer: Answer): Feedback = when (exercise) {
        is ChooseTranslation -> {
            val chosen = (answer as Answer.Option).index
            Feedback(
                correct = chosen == exercise.correctOption,
                correctAnswer = localize(exercise.options[exercise.correctOption]),
            )
        }
        is TypeAnswer -> checkTyped((answer as Answer.Typed).text, exercise.acceptedAnswers)
    }

    private fun localize(text: Localized): String = text.getValue(baseLanguage)
}
