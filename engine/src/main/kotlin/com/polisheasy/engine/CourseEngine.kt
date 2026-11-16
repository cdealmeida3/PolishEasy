package com.polisheasy.engine

import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime

/** What the Learner can do today. Home shows exactly one of these. */
sealed interface Today {
    data object DailySetAvailable : Today

    /** Every Daily Set released so far is completed, and today's Review-only Daily Set can be started. */
    data object ReviewOnlyAvailable : Today

    /** A Daily Set was started, today or earlier, and not finished. [reviewOnly] when it is a Review-only Daily Set. */
    data class InProgress(val progress: SetProgress, val reviewOnly: Boolean = false) : Today

    /**
     * A new or Review-only Daily Set was completed today. [nextUnlock] is the start of the next
     * local calendar day. [reviewOnlyNext] when every Daily Set released so far is completed, so
     * tomorrow's is a Review-only Daily Set.
     */
    data class DoneForToday(val nextUnlock: ZonedDateTime, val reviewOnlyNext: Boolean = false) : Today

    /** Every Daily Set released so far is completed and the course has nothing to review. */
    data object AllCompleted : Today
}

/** What the Daily Set player shows next. */
sealed interface Step {
    /** [replay] when this is a Replay, which saves nothing. */
    data class ShowTip(val tip: TipView, val progress: SetProgress, val replay: Boolean = false) : Step

    /**
     * [review] marks an Exercise from the Review that ends the Daily Set.
     * [replay] when this is a Replay, which saves nothing.
     * [exerciseId] is the Exercise's stable identifier, for a mistake report.
     */
    data class ShowExercise(
        val exercise: ExerciseView,
        val progress: SetProgress,
        val review: Boolean = false,
        val replay: Boolean = false,
        val exerciseId: String = "",
    ) : Step

    /**
     * [correctAnswers] counts the Exercises answered correctly on the first attempt, Review included.
     * [itemsLearned] are the Items this Daily Set introduced. [reviewOnly] when it was a
     * Review-only Daily Set, which introduces none. [replay] when this is a Replay, which saves
     * nothing and lists no Items either, since none was learned for the first time.
     */
    data class ShowSummary(
        val correctAnswers: Int,
        val exercises: Int,
        val itemsLearned: List<ItemView> = emptyList(),
        val reviewOnly: Boolean = false,
        val replay: Boolean = false,
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

    /**
     * [prompt] is what to type in Polish, in the Base Language. Also the reading form of a
     * listen-and-type Exercise, whose [prompt] is the meaning of what would have been said.
     */
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

    /**
     * The reading form of a listen-and-choose Exercise: the Learner reads [prompt], a meaning in
     * the Base Language, and picks the Polish for it from [options].
     */
    data class ChoosePolish(val prompt: String, val options: List<String>) : ExerciseView
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

    /**
     * [dailySet] is the one on the Path, or for a Review-only Daily Set an empty one that is not
     * on the Path, so that every Exercise comes from the Review saved in [state]. [replay] when it
     * is a Replay, whose [state] is never saved. [sound] is false when the app may play nothing; it
     * is never saved.
     */
    private class Playing(
        val dailySet: DailySet,
        var state: DailySetInProgress,
        val reviewOnly: Boolean = false,
        val replay: Boolean = false,
        val sound: Boolean = true,
    ) {
        /** Whether listening Exercises are shown in their reading form. */
        val reading: Boolean get() = !sound || state.cantListen
    }

    /** Whether any Exercise practises an Item, so a Review-only Daily Set would not be empty. */
    private val reviewable: Boolean by lazy {
        course.exercises.values.any { exercise -> exercise.items.any { it in course.itemsById } }
    }

    fun today(): Today {
        val completed = progress.completedDailySets()
        val today = LocalDate.now(clock)
        val pathCompleted = course.path.all { it.id in completed }
        if (pathCompleted && !reviewable) return Today.AllCompleted
        if (today in completed.values || today in progress.reviewOnlyDays()) {
            return Today.DoneForToday(
                nextUnlock = today.plusDays(1).atStartOfDay(clock.zone),
                reviewOnlyNext = pathCompleted,
            )
        }
        savedDailySet()?.let { return Today.InProgress(progressOf(it), reviewOnly = it.reviewOnly) }
        return if (pathCompleted) Today.ReviewOnlyAvailable else Today.DailySetAvailable
    }

    /**
     * The Learner's Streak: the consecutive calendar days on which a new or Review-only Daily Set
     * was completed. Until today's is completed it is the Streak earned up to yesterday; a day with
     * none resets it to zero. It is worked out from the saved completion days and nothing else, so
     * a Replay, which records no completion, cannot change it.
     */
    fun streak(): Int = streakOn(LocalDate.now(clock), completionDays())

    /**
     * The last seven calendar days, oldest first and ending today, each with whether a new or
     * Review-only Daily Set was completed on it. Like the [streak], it is worked out from the saved
     * completion days and nothing else, so a Replay cannot change it.
     */
    fun lastSevenDays(): List<DayView> = daysEnding(LocalDate.now(clock), completionDays(), days = 7)

    /** Every calendar day on which a new or Review-only Daily Set was completed. */
    private fun completionDays(): Set<LocalDate> = progress.completedDailySets().values.toSet() + progress.reviewOnlyDays()

    /**
     * The first Daily Set on the Path not yet completed, with its place in its Unit, or null once
     * every Daily Set released so far is completed. It is the same one whether it can be started
     * today, is in progress, or unlocks tomorrow; [today] says which.
     */
    fun nextDailySet(): NextDailySetView? {
        val completed = progress.completedDailySets().keys
        for (level in course.levels) {
            level.units.forEachIndexed { unitIndex, unit ->
                val index = unit.dailySets.indexOfFirst { it.id !in completed }
                if (index >= 0) {
                    return NextDailySetView(
                        unitNumber = unitIndex + 1,
                        unitTitle = localize(unit.title),
                        numberInUnit = index + 1,
                        dailySetsInUnit = unit.dailySets.size,
                        teaches = unit.dailySets[index].tips.map { localize(it.title) },
                    )
                }
            }
        }
        return null
    }

    /**
     * Whether the Learner should be reminded now: today's Daily Set, new or Review-only, can be
     * started or is in progress. Not once it is completed, and not when nothing is left to do.
     * A Replay never changes [today], so it never changes this.
     */
    fun reminderDue(): Boolean = when (today()) {
        Today.DailySetAvailable, Today.ReviewOnlyAvailable, is Today.InProgress -> true
        is Today.DoneForToday, Today.AllCompleted -> false
    }

    /**
     * The Path: every Level opened so far, with its Units and their Daily Sets. A Level opens when
     * every Daily Set of the Levels before it is completed. A Review-only Daily Set is not on it.
     */
    fun path(): PathView {
        val completed = progress.completedDailySets().keys
        val offered = today()
        val startable = offered == Today.DailySetAvailable || (offered is Today.InProgress && !offered.reviewOnly)
        val todays = if (startable) course.path.firstOrNull { it.id !in completed }?.id else null
        val opened = course.levels.filterIndexed { index, _ ->
            course.levels.take(index).all { earlier -> earlier.units.all { unit -> unit.dailySets.all { it.id in completed } } }
        }
        return PathView(
            opened.map { level ->
                var number = 0
                LevelView(
                    level.id,
                    localize(level.title),
                    level.units.mapIndexed { index, unit ->
                        UnitView(
                            unit.id,
                            number = index + 1,
                            title = localize(unit.title),
                            dailySets = unit.dailySets.map { set ->
                                val state = when (set.id) {
                                    in completed -> DailySetState.COMPLETED
                                    todays -> DailySetState.TODAY
                                    else -> DailySetState.LOCKED
                                }
                                DailySetView(set.id, ++number, state, title = set.tips.firstOrNull()?.let { localize(it.title) })
                            },
                        )
                    },
                )
            },
        )
    }

    /**
     * Starts what today offers: the next Daily Set on the Path or, once every one is completed, a
     * Review-only Daily Set. Its Review is chosen now and saved with it, so the Daily Set keeps
     * the same Exercises however much later it is finished.
     * Fails unless [today] is [Today.DailySetAvailable] or [Today.ReviewOnlyAvailable].
     * With [sound] false nothing is given to play: listening Exercises come in their reading form
     * and no Exercise or Feedback carries a Clip. It is not saved.
     */
    fun startDailySet(sound: Boolean = true): Step {
        val offered = today()
        check(offered == Today.DailySetAvailable || offered == Today.ReviewOnlyAvailable) {
            "No Daily Set is available to start: $offered"
        }
        val completed = progress.completedDailySets()
        if (offered == Today.ReviewOnlyAvailable) {
            val review = chooseReviewOnly(course, completed, progress.itemProgress(), LocalDate.now(clock))
            val state = DailySetInProgress(dailySetId = REVIEW_ONLY_ID, remaining = review, review = review)
            progress.saveInProgress(state)
            playing = reviewOnly(state, sound)
            return next()
        }
        val set = course.path.first { it.id !in completed }
        val review = chooseReview(course, completed, progress.itemProgress(), set, LocalDate.now(clock))
        val state = DailySetInProgress(
            dailySetId = set.id,
            remaining = set.exercises.map { it.id } + review,
            review = review,
        )
        progress.saveInProgress(state)
        playing = Playing(set, state, sound = sound)
        return next()
    }

    /**
     * Picks up the Daily Set in progress where it was left. Fails unless [today] is [Today.InProgress].
     * With [sound] false nothing is given to play: listening Exercises come in their reading form
     * and no Exercise or Feedback carries a Clip. It is not saved.
     */
    fun resumeDailySet(sound: Boolean = true): Step {
        check(today() is Today.InProgress) { "No Daily Set is in progress: ${today()}" }
        playing = checkNotNull(savedDailySet(sound))
        return next()
    }

    /**
     * Starts a Replay of a completed Daily Set: its Tips and New Material again, without Review.
     * A Replay is kept in memory only and saves nothing, so it cannot advance the Path, count
     * toward the Streak or reschedule an Item, and it is forgotten if it is left unfinished.
     * Fails unless [dailySetId] is a completed Daily Set on the Path.
     * With [sound] false nothing is given to play: listening Exercises come in their reading form
     * and no Exercise or Feedback carries a Clip. It is not saved.
     */
    fun startReplay(dailySetId: String, sound: Boolean = true): Step {
        val set = course.path.firstOrNull { it.id == dailySetId }
        check(set != null && dailySetId in progress.completedDailySets()) {
            "Daily Set $dailySetId is not completed, so it cannot be replayed"
        }
        playing = Playing(set, DailySetInProgress(set.id, remaining = set.exercises.map { it.id }), replay = true, sound = sound)
        return next()
    }

    /**
     * The Learner cannot listen: the Exercise being shown and every listening Exercise still to come
     * in this Daily Set are shown in their reading form, the same Exercise asked from its meaning
     * instead of its Clip. Returns the Exercise being shown again. Nothing is answered: no Item, no
     * miss and no count changes. The choice is saved with the Daily Set in progress and ends with
     * it; in a Replay it is kept in memory like the rest. Call it while an Exercise is shown.
     */
    fun cantListenNow(): Step {
        val playing = current()
        val exerciseId = playing.state.remaining.firstOrNull()
        check(exerciseId != null) { "Every Exercise of Daily Set ${playing.dailySet.id} has already been answered" }
        playing.state = playing.state.copy(cantListen = true)
        if (!playing.replay) {
            // The Tip before this Exercise is not yet followed by an answer, so it is not saved as shown.
            val unanswered = playing.dailySet.tips.filter { it.before == exerciseId }.map { it.id }.toSet()
            val saved = progress.dailySetInProgress()?.shownTips.orEmpty()
            progress.saveInProgress(playing.state.copy(shownTips = playing.state.shownTips - (unanswered - saved)))
        }
        return next()
    }

    /**
     * Checks the Learner's answer to the Exercise currently shown and saves where the Learner is.
     * A wrongly answered Exercise goes to the end of the queue and returns until it is answered
     * correctly. The Daily Set, new or Review-only, is completed, on the day of this answer, when
     * none remain.
     * The first attempt at an Exercise, and only the first, reschedules each of its Items.
     * A Replay is checked and queued the same way but nothing is saved.
     */
    fun answer(answer: Answer): Feedback {
        val playing = current()
        val set = playing.dailySet
        val state = playing.state
        check(state.remaining.isNotEmpty()) { "Every Exercise of Daily Set ${set.id} has already been answered" }
        val exercise = course.exercises.getValue(state.remaining.first())
        val feedback = check(exercise, answer, playing.reading)
            .let { it.copy(audio = if (playing.sound) audioFor(exercise, it) else null) }
        val items = if (playing.replay || exercise.id in state.missed) emptyMap() else itemsAfter(exercise, feedback.correct)
        val rest = state.remaining.drop(1)
        playing.state = if (feedback.correct) {
            state.copy(remaining = rest)
        } else {
            state.copy(remaining = rest + exercise.id, missed = state.missed + exercise.id)
        }
        if (playing.replay) return feedback
        if (playing.state.remaining.isEmpty()) {
            val today = LocalDate.now(clock)
            if (playing.reviewOnly) {
                progress.recordReviewOnlyCompleted(on = today, items)
            } else {
                progress.recordCompleted(set.id, on = today, items)
            }
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
                itemsLearned = if (playing.replay) {
                    emptyList()
                } else {
                    course.introducedIn(set).map { ItemView(it.text, localize(it.meaning)) }
                },
                reviewOnly = playing.reviewOnly,
                replay = playing.replay,
            )
        val tip = set.tips.firstOrNull { it.before == exerciseId && it.id !in state.shownTips }
        if (tip != null) {
            // Saved with the next answer: a Tip seen but not followed by an answer is shown again on resume.
            playing.state = state.copy(shownTips = state.shownTips + tip.id)
            return Step.ShowTip(view(tip), progressOf(playing), replay = playing.replay)
        }
        return Step.ShowExercise(
            view(course.exercises.getValue(exerciseId), playing),
            progressOf(playing),
            review = exerciseId in state.review,
            replay = playing.replay,
            exerciseId = exerciseId,
        )
    }

    /**
     * The saved Daily Set in progress, if it can still be played: its Daily Set is on the Path and
     * not completed, and every id it refers to still exists: its Exercises and Tips there, its
     * Review Exercises anywhere in the course. A Review-only Daily Set can be played only while
     * every Daily Set on the Path is completed, and only if all its Exercises still exist.
     * Otherwise the content changed in an update, and the saved state is ignored.
     */
    private fun savedDailySet(sound: Boolean = true): Playing? {
        val saved = progress.dailySetInProgress() ?: return null
        val completed = progress.completedDailySets()
        if (saved.dailySetId == REVIEW_ONLY_ID) {
            val playable = course.path.all { it.id in completed } &&
                saved.remaining.isNotEmpty() &&
                course.exercises.keys.containsAll(saved.review) &&
                saved.review.containsAll(saved.remaining) &&
                saved.review.containsAll(saved.missed)
            return if (playable) reviewOnly(saved, sound) else null
        }
        if (saved.dailySetId in completed) return null
        val set = course.path.firstOrNull { it.id == saved.dailySetId } ?: return null
        val exerciseIds = set.exercises.map { it.id }.toSet() + saved.review
        val tipIds = set.tips.map { it.id }.toSet()
        val playable = saved.remaining.isNotEmpty() &&
            course.exercises.keys.containsAll(saved.review) &&
            exerciseIds.containsAll(saved.remaining) &&
            exerciseIds.containsAll(saved.missed) &&
            tipIds.containsAll(saved.shownTips)
        return if (playable) Playing(set, saved, sound = sound) else null
    }

    private fun reviewOnly(state: DailySetInProgress, sound: Boolean): Playing =
        Playing(DailySet(REVIEW_ONLY_ID, exercises = emptyList()), state, reviewOnly = true, sound = sound)

    private fun progressOf(playing: Playing): SetProgress {
        val total = total(playing)
        return SetProgress(correct = total - playing.state.remaining.size, total = total)
    }

    /** Every Exercise of the Daily Set being played: its New Material and its Review. */
    private fun total(playing: Playing): Int = playing.dailySet.exercises.size + playing.state.review.size

    private fun current(): Playing = checkNotNull(playing) { "No Daily Set has been started" }

    /** [exercise] as [playing] shows it: a listening one in its reading form when it cannot be heard, and no Clip without sound. */
    private fun view(exercise: Exercise, playing: Playing): ExerciseView = when (exercise) {
        is ChooseTranslation -> ExerciseView.ChooseTranslation(
            prompt = exercise.prompt,
            options = exercise.options.map(::localize),
            promptAudio = if (playing.sound) course.clipFor(exercise.prompt) else null,
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
        is ListenChoose ->
            if (playing.reading) {
                ExerciseView.ChoosePolish(localize(exercise.meaning), optionsOf(exercise))
            } else {
                ExerciseView.ListenChoose(clipOf(exercise, exercise.text), optionsOf(exercise))
            }
        is ListenType ->
            if (playing.reading) {
                ExerciseView.TypeAnswer(localize(exercise.meaning))
            } else {
                ExerciseView.ListenType(clipOf(exercise, exercise.acceptedAnswers.first()))
            }
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

    /** [reading] when a listening Exercise was shown in its reading form, where a listen-and-type one also takes its reading answers. */
    private fun check(exercise: Exercise, answer: Answer, reading: Boolean): Feedback = when (exercise) {
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
        is ListenType -> checkTyped(
            (answer as Answer.Typed).text,
            if (reading) exercise.acceptedAnswers + exercise.readingAnswers else exercise.acceptedAnswers,
        )
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
