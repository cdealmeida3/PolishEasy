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

    /** [review] marks an Exercise from the Review that ends the Daily Set. */
    data class ShowExercise(val exercise: ExerciseView, val progress: SetProgress, val review: Boolean = false) : Step

    /**
     * [correctAnswers] counts the Exercises answered correctly on the first attempt, Review included.
     * [itemsLearned] are the Items this Daily Set introduced.
     */
    data class ShowSummary(
        val correctAnswers: Int,
        val exercises: Int,
        val itemsLearned: List<ItemView> = emptyList(),
    ) : Step
}

/** An Item as the Learner sees it: the Polish and its meaning in the Base Language. */
data class ItemView(val text: String, val meaning: String)

/**
 * How far through the Daily Set the Learner is: Exercises answered correctly out of all of them,
 * its Review Exercises included.
 */
data class SetProgress(val correct: Int, val total: Int)

/** A Tip as the Learner sees it, in the Base Language. */
data class TipView(val title: String, val body: String, val conjugation: ConjugationTableView? = null)

/** A verb's forms for every person, in the usual order. */
data class ConjugationTableView(val verb: String, val meaning: String, val rows: List<ConjugationRow>)

data class ConjugationRow(val person: Person, val form: String)

/** A sentence with one gap where the verb form belongs. [meaning] is the whole sentence in the Base Language. */
data class GapSentence(val before: String, val after: String, val meaning: String)

/** An Exercise as the Learner sees it: Base Language text resolved, answer withheld. */
sealed interface ExerciseView {
    /** [promptAudio] is the Clip of the Polish [prompt], if it has one. */
    data class ChooseTranslation(
        val prompt: String,
        val options: List<String>,
        val promptAudio: ClipView? = null,
    ) : ExerciseView

    /** [prompt] is what to type in Polish, in the Base Language. */
    data class TypeAnswer(val prompt: String) : ExerciseView

    /** [prompt] is what to build in Polish, in the Base Language. [tiles] are the words to build it from, mixed. */
    data class WordTiles(val prompt: String, val tiles: List<String>) : ExerciseView

    /** [verb] is the dictionary form, [meaning] its Base Language meaning. The Learner types the form for [person]. */
    data class Conjugation(
        val verb: String,
        val meaning: String,
        val person: Person,
        val sentence: GapSentence? = null,
    ) : ExerciseView

    /** The Learner hears [audio] and picks, from [options], the Polish that was said. */
    data class ListenChoose(val audio: ClipView, val options: List<String>) : ExerciseView

    /** The Learner hears [audio] and types it. Nothing is shown, since the text is the answer. */
    data class ListenType(val audio: ClipView) : ExerciseView
}

/** The Learner's response to an Exercise. */
sealed interface Answer {
    data class Option(val index: Int) : Answer

    data class Typed(val text: String) : Answer

    /** The words of the tiles the Learner placed, in order. */
    data class Tiles(val words: List<String>) : Answer
}

/**
 * An answer that is right apart from its accents is [correct] with [accentWarning] set;
 * [correctAnswer] then shows the proper spelling. [audio] is the Clip of the Polish the feedback
 * is about, if it has one.
 */
data class Feedback(
    val correct: Boolean,
    val correctAnswer: String,
    val accentWarning: Boolean = false,
    val audio: ClipView? = null,
)

/**
 * All course rules. Screens ask it what to show and send it the Learner's answers;
 * it knows nothing about Android.
 */
class CourseEngine(
    private val course: Course,
    private val progress: LearnerProgress,
    private val clock: Clock,
    private val baseLanguage: String = "en",
    private val scheduler: ReviewScheduler = LeitnerScheduler(),
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

    /**
     * Starts the next Daily Set on the Path. Its Review is chosen now and saved with it, so the
     * Daily Set keeps the same Exercises however much later it is finished.
     * Fails unless [today] is [Today.DailySetAvailable].
     */
    fun startDailySet(): Step {
        check(today() == Today.DailySetAvailable) { "No Daily Set is available to start: ${today()}" }
        val completed = progress.completedDailySets()
        val set = course.path.first { it.id !in completed }
        val review = chooseReview(course, completed, progress.itemProgress(), set, LocalDate.now(clock))
        val state = DailySetInProgress(
            dailySetId = set.id,
            remaining = set.exercises.map { it.id } + review,
            review = review,
        )
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
     * The first attempt at an Exercise, and only the first, reschedules each of its Items.
     */
    fun answer(answer: Answer): Feedback {
        val playing = current()
        val set = playing.dailySet
        val state = playing.state
        check(state.remaining.isNotEmpty()) { "Every Exercise of Daily Set ${set.id} has already been answered" }
        val exercise = course.exercises.getValue(state.remaining.first())
        val feedback = check(exercise, answer).let { it.copy(audio = audioFor(exercise, it)) }
        val items = if (exercise.id in state.missed) emptyMap() else itemsAfter(exercise, feedback.correct)
        val rest = state.remaining.drop(1)
        playing.state = if (feedback.correct) {
            state.copy(remaining = rest)
        } else {
            state.copy(remaining = rest + exercise.id, missed = state.missed + exercise.id)
        }
        if (playing.state.remaining.isEmpty()) {
            progress.recordCompleted(set.id, on = LocalDate.now(clock), items)
        } else {
            progress.saveInProgress(playing.state, items)
        }
        return feedback
    }

    /** The progress of each Item of [exercise] after a first attempt at it today. */
    private fun itemsAfter(exercise: Exercise, correct: Boolean): Map<String, ItemProgress> {
        val today = LocalDate.now(clock)
        val completed = progress.completedDailySets()
        val stored = progress.itemProgress()
        return exercise.items.filter { it in course.itemsById }.associateWith { itemId ->
            ItemProgress(
                schedule = scheduler.afterAnswer(scheduleOf(itemId, course, completed, stored), correct, today),
                recentExercises = (stored[itemId]?.recentExercises.orEmpty() - exercise.id + exercise.id)
                    .takeLast(RECENT_EXERCISES),
            )
        }
    }

    fun next(): Step {
        val playing = current()
        val set = playing.dailySet
        val state = playing.state
        val total = total(playing)
        val exerciseId = state.remaining.firstOrNull()
            // First-attempt correct: every Exercise that was never answered wrongly.
            ?: return Step.ShowSummary(
                correctAnswers = total - state.missed.size,
                exercises = total,
                itemsLearned = course.introducedIn(set).map { ItemView(it.text, localize(it.meaning)) },
            )
        val tip = set.tips.firstOrNull { it.before == exerciseId && it.id !in state.shownTips }
        if (tip != null) {
            // Saved with the next answer: a Tip seen but not followed by an answer is shown again on resume.
            playing.state = state.copy(shownTips = state.shownTips + tip.id)
            return Step.ShowTip(view(tip), progressOf(playing))
        }
        return Step.ShowExercise(
            view(course.exercises.getValue(exerciseId)),
            progressOf(playing),
            review = exerciseId in state.review,
        )
    }

    /**
     * The saved Daily Set in progress, if it can still be played: its Daily Set is on the Path and
     * not completed, and every id it refers to still exists: its Exercises and Tips there, its
     * Review Exercises anywhere in the course. Otherwise the content changed in an update, and
     * the saved state is ignored.
     */
    private fun savedDailySet(): Playing? {
        val saved = progress.dailySetInProgress() ?: return null
        if (saved.dailySetId in progress.completedDailySets()) return null
        val set = course.path.firstOrNull { it.id == saved.dailySetId } ?: return null
        val exerciseIds = set.exercises.map { it.id }.toSet() + saved.review
        val tipIds = set.tips.map { it.id }.toSet()
        val playable = saved.remaining.isNotEmpty() &&
            course.exercises.keys.containsAll(saved.review) &&
            exerciseIds.containsAll(saved.remaining) &&
            exerciseIds.containsAll(saved.missed) &&
            tipIds.containsAll(saved.shownTips)
        return if (playable) Playing(set, saved) else null
    }

    private fun progressOf(playing: Playing): SetProgress {
        val total = total(playing)
        return SetProgress(correct = total - playing.state.remaining.size, total = total)
    }

    /** Every Exercise of the Daily Set being played: its New Material and its Review. */
    private fun total(playing: Playing): Int = playing.dailySet.exercises.size + playing.state.review.size

    private fun current(): Playing = checkNotNull(playing) { "No Daily Set has been started" }

    private fun view(exercise: Exercise): ExerciseView = when (exercise) {
        is ChooseTranslation -> ExerciseView.ChooseTranslation(
            prompt = exercise.prompt,
            options = exercise.options.map(::localize),
            promptAudio = course.clipFor(exercise.prompt),
        )
        is TypeAnswer -> ExerciseView.TypeAnswer(localize(exercise.prompt))
        is WordTiles -> ExerciseView.WordTiles(localize(exercise.prompt), tilesOf(exercise))
        is Conjugation -> ExerciseView.Conjugation(
            verb = exercise.verb,
            meaning = localize(exercise.meaning),
            person = exercise.person,
            sentence = exercise.sentence?.let { sentence ->
                // The loader guarantees one gap and a meaning for every sentence.
                val (before, after) = checkNotNull(gapOf(sentence)) { "Exercise ${exercise.id} has no single gap" }
                GapSentence(before, after, localize(checkNotNull(exercise.sentenceMeaning)))
            },
        )
        is ListenChoose -> ExerciseView.ListenChoose(clipOf(exercise, exercise.text), optionsOf(exercise))
        is ListenType -> ExerciseView.ListenType(clipOf(exercise, exercise.acceptedAnswers.first()))
    }

    /** The Clip a listening Exercise plays. The loader guarantees there is one. */
    private fun clipOf(exercise: Exercise, text: String): ClipView =
        checkNotNull(course.clipFor(text)) { "Exercise ${exercise.id} has no Clip for \"$text\"" }

    private fun view(tip: Tip): TipView = TipView(
        title = localize(tip.title),
        body = localize(tip.body),
        conjugation = tip.conjugation?.let { table ->
            ConjugationTableView(
                verb = table.verb,
                meaning = localize(table.meaning),
                rows = Person.entries.map { ConjugationRow(it, table.forms.getValue(it)) },
            )
        },
    )

    private fun check(exercise: Exercise, answer: Answer): Feedback = when (exercise) {
        is ChooseTranslation -> {
            val chosen = (answer as Answer.Option).index
            Feedback(
                correct = chosen == exercise.correctOption,
                correctAnswer = localize(exercise.options[exercise.correctOption]),
            )
        }
        is TypeAnswer -> checkTyped((answer as Answer.Typed).text, exercise.acceptedAnswers)
        is WordTiles -> checkTiles((answer as Answer.Tiles).words, exercise.acceptedAnswers)
        is Conjugation -> checkTyped((answer as Answer.Typed).text, exercise.acceptedAnswers)
        is ListenChoose -> Feedback(
            correct = optionsOf(exercise).getOrNull((answer as Answer.Option).index) == exercise.text,
            correctAnswer = exercise.text,
        )
        is ListenType -> checkTyped((answer as Answer.Typed).text, exercise.acceptedAnswers)
    }

    /** The Clip of the Polish that [feedback] on [exercise] is about, if it has one. */
    private fun audioFor(exercise: Exercise, feedback: Feedback): ClipView? = when (exercise) {
        // The answer shown is in the Base Language; the prompt has the speaker.
        is ChooseTranslation -> null
        is TypeAnswer, is WordTiles -> course.clipFor(feedback.correctAnswer)
        is Conjugation ->
            course.clipFor(exercise.sentence?.let { filled(it, feedback.correctAnswer) } ?: feedback.correctAnswer)
        // What was spoken, whichever spelling was matched.
        is ListenChoose -> course.clipFor(exercise.text)
        is ListenType -> course.clipFor(exercise.acceptedAnswers.first())
    }

    private fun localize(text: Localized): String = text.getValue(baseLanguage)
}
