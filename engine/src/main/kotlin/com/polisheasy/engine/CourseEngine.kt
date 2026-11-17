package com.polisheasy.engine

import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.random.Random

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
        /** What the Exercise asks, in Polish as the book words it, with its translation. Null when the course gives none. */
        val instruction: InstructionView? = null,
        /** [checkpoint] when this is a question of a Checkpoint, which keeps only its Best. */
        val checkpoint: Boolean = false,
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
        /** The result, when this was a Checkpoint. */
        val checkpoint: CheckpointResultView? = null,
    ) : Step
}

/** An Exercise's instruction: the [polish] as the book words it, its [meaning] in the Base Language, and its Clip. */
data class InstructionView(val polish: String, val meaning: String, val audio: ClipView? = null)

/** An Item as the Learner sees it: the Polish and its meaning in the Base Language. */
data class ItemView(val text: String, val meaning: String)

/**
 * How far through the Daily Set the Learner is: Exercises answered correctly out of all of them,
 * its Review Exercises included.
 */
data class SetProgress(val correct: Int, val total: Int)

/**
 * How far through the Daily Set the Learner is, counted in steps: the Tips read and the Exercises
 * answered correctly, out of all of them. It moves from the first tap, on a Tip.
 */
data class StepProgress(val done: Int, val total: Int)

/** A Unit the Learner has just completed: its number in its Level, from 1, and its title. */
data class UnitCompletedView(val number: Int, val title: String)

/** A Tip as the Learner sees it, in the Base Language. [polish] is where in [body] the Polish is, so it can be set apart. */
data class TipView(
    val title: String,
    val body: String,
    val conjugation: ConjugationTableView? = null,
    val polish: List<IntRange> = emptyList(),
)

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
        /** Whether the Polish is formal, informal or both, where the book marks it. */
        val register: Register? = null,
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
 *
 * [oneADay] is the Learner's One a day setting, asked afresh each time it matters so that a change
 * takes effect at once. Off, the default, the Learner goes at their own pace: any Daily Set not yet
 * completed can be started at any time and Practice is always open. On, at most one new or
 * Review-only Daily Set can be completed per calendar day, the next one only, and Practice opens
 * once it is done.
 */
class CourseEngine(
    private val course: Course,
    private val progress: LearnerProgress,
    private val clock: Clock,
    private val baseLanguage: String = "en",
    private val scheduler: ReviewScheduler = LeitnerScheduler(),
    private val oneADay: () -> Boolean = { false },
    /** Where the Best of each chapter's Checkpoint is kept. */
    private val checkpoints: CheckpointBests = InMemoryCheckpointBests(),
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
        /** The Unit whose Checkpoint this is, or null. A Checkpoint saves nothing but its Best, and asks each question once. */
        val checkpoint: String? = null,
    ) {
        /** Set once the Checkpoint's Best has been saved, so it is saved once. */
        var result: CheckpointResultView? = null

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
        if (oneADay() && doneToday()) {
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
     * The calendar week today is in, Monday to Sunday as a Polish calendar shows it, each day with
     * whether a new or Review-only Daily Set was completed on it. Like the [streak], it is worked
     * out from the saved completion days and nothing else, so a Replay cannot change it.
     */
    fun thisWeek(): List<DayView> = weekOf(LocalDate.now(clock), completionDays())

    /**
     * What the Learner has built up so far. A Replay records nothing, so it changes none of it, and
     * a Review-only Daily Set counts as a Daily Set and teaches no new word.
     */
    fun soFar(): SoFarView {
        val completed = progress.completedDailySets().keys
        return SoFarView(
            wordsLearned = course.path.filter { it.id in completed }.flatMap { it.itemIds() }.distinct().count { it in course.itemsById },
            dailySetsCompleted = course.path.count { it.id in completed } + progress.reviewOnlyDays().size,
            longestStreak = longestStreakIn(completionDays()),
        )
    }

    /** The Daily Set on the Path that was completed today, which Home offers to replay, or null when there is none. */
    fun completedToday(): String? {
        val today = LocalDate.now(clock)
        val completed = progress.completedDailySets()
        return course.path.lastOrNull { completed[it.id] == today }?.id
    }

    /**
     * Whether a new or Review-only Daily Set was completed today, so today counts toward the
     * Streak already. A Replay records no completion, so it never makes this true.
     */
    /** The id of the Daily Set of the Path in progress, or null when none is, or when it is a Review-only one. */
    fun dailySetInProgress(): String? = progress.dailySetInProgress()?.dailySetId?.takeUnless { it == REVIEW_ONLY_ID }

    fun doneToday(): Boolean {
        val today = LocalDate.now(clock)
        return today in progress.completedDailySets().values || today in progress.reviewOnlyDays()
    }

    /** Every calendar day on which a new or Review-only Daily Set was completed. */
    private fun completionDays(): Set<LocalDate> = progress.completedDailySets().values.toSet() + progress.reviewOnlyDays()

    /**
     * The Daily Set the Learner is at, with its place in its Unit, or null once every Daily Set
     * released so far is completed: the one in progress, or else the first on the Path not yet
     * completed. It is the same one whether it can be started today, is in progress, or unlocks
     * tomorrow; [today] says which. Only at their own pace can the one in progress be a later one.
     */
    fun nextDailySet(): NextDailySetView? {
        val at = atDailySet() ?: return null
        for (level in course.levels) {
            level.units.forEachIndexed { unitIndex, unit ->
                val index = unit.dailySets.indexOfFirst { it.id == at }
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

    /** The id of the Daily Set on the Path the Learner is at: the one in progress, or else the first not yet completed. */
    private fun atDailySet(): String? {
        val completed = progress.completedDailySets().keys
        val first = course.path.firstOrNull { it.id !in completed }?.id ?: return null
        return savedDailySet()?.takeUnless { it.reviewOnly }?.dailySet?.id ?: first
    }

    /**
     * Whether the Learner should be reminded now: nothing, new or Review-only, has been completed
     * today and something is left to do. A Replay records no completion, so it never changes this.
     */
    fun reminderDue(): Boolean = !doneToday() && today() != Today.AllCompleted

    /**
     * Whether Practice is open. At the Learner's own pace it always is. With [oneADay] it opens once
     * today's new or Review-only Daily Set is completed, or when nothing is left to do, and locks
     * again at midnight with everything else. A Replay never changes [today], so it never opens it.
     */
    fun practiceOpen(): Boolean = !oneADay() || when (today()) {
        is Today.DoneForToday, Today.AllCompleted -> true
        Today.DailySetAvailable, Today.ReviewOnlyAvailable, is Today.InProgress -> false
    }

    /**
     * Practice over this course and this Learner's progress, which it only reads. A Round saves
     * its Game's Best in [bests] and nothing else. [random] decides what a Round asks.
     */
    fun practice(bests: PracticeProgress, random: Random = Random.Default): Practice =
        Practice(course, progress, bests, clock, baseLanguage, random, ::practiceOpen)

    /**
     * The Path: every Level opened so far, with its Units and their Daily Sets. A Review-only Daily
     * Set is not on it. At the Learner's own pace every Level is open, the Daily Set they are at is
     * [DailySetState.TODAY] and every other one not yet completed is [DailySetState.OPEN]. With
     * [oneADay] a Level opens when every Daily Set of the Levels before it is completed, today's
     * Daily Set, if it can be started or continued, is [DailySetState.TODAY], and the rest are locked.
     */
    fun path(): PathView {
        val completed = progress.completedDailySets().keys
        val ownPace = !oneADay()
        val todays = if (ownPace) {
            atDailySet()
        } else {
            val offered = today()
            val startable = offered == Today.DailySetAvailable || (offered is Today.InProgress && !offered.reviewOnly)
            if (startable) atDailySet() else null
        }
        val opened = if (ownPace) {
            course.levels
        } else {
            course.levels.filterIndexed { index, _ ->
                course.levels.take(index).all { earlier -> earlier.units.all { unit -> unit.dailySets.all { it.id in completed } } }
            }
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
                                    else -> if (ownPace) DailySetState.OPEN else DailySetState.LOCKED
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
     *
     * With [dailySetId], at the Learner's own pace only, starts that Daily Set instead: any on the
     * Path not yet completed, in any order. A different Daily Set in progress is forgotten, since
     * one is kept in progress at a time; the one in progress itself is picked up where it was left.
     * Fails with [oneADay], or for a Daily Set that is completed or not on the Path.
     */
    fun startDailySet(sound: Boolean = true, dailySetId: String? = null): Step {
        if (dailySetId != null) return startChosen(dailySetId, sound)
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
        return start(course.path.first { it.id !in completed }, sound)
    }

    private fun startChosen(dailySetId: String, sound: Boolean): Step {
        check(!oneADay()) { "Only the next Daily Set can be started with One a day on" }
        val set = course.path.firstOrNull { it.id == dailySetId }
        check(set != null && dailySetId !in progress.completedDailySets()) {
            "Daily Set $dailySetId is completed or not on the Path, so it cannot be started"
        }
        val saved = savedDailySet(sound)
        if (saved != null && !saved.reviewOnly && saved.dailySet.id == dailySetId) {
            playing = saved
            return next()
        }
        return start(set, sound)
    }

    /** Starts [set] afresh, with its Review chosen now, in place of anything saved in progress. */
    private fun start(set: DailySet, sound: Boolean): Step {
        val completed = progress.completedDailySets()
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
        } else if (playing.checkpoint != null) {
            // A Checkpoint asks each question once: a wrong answer counts and is not asked again.
            state.copy(remaining = rest, missed = state.missed + exercise.id)
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
        tipShowing = false
        val exerciseId = state.remaining.firstOrNull()
            ?: playing.checkpoint?.let { unitId ->
                return Step.ShowSummary(
                    correctAnswers = total - state.missed.size,
                    exercises = total,
                    replay = true,
                    checkpoint = checkpointResult(playing, unitId, correct = total - state.missed.size, questions = total),
                )
            }
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
            tipShowing = true
            return Step.ShowTip(view(tip), progressOf(playing), replay = playing.replay)
        }
        val view = view(course.exercises.getValue(exerciseId), playing)
        return Step.ShowExercise(
            view,
            progressOf(playing),
            review = exerciseId in state.review,
            replay = playing.replay && playing.checkpoint == null,
            exerciseId = exerciseId,
            instruction = instructionOf(view, playing),
            checkpoint = playing.checkpoint != null,
        )
    }

    /** The instruction of [view], with its Clip when the Learner can listen. */
    private fun instructionOf(view: ExerciseView, playing: Playing): InstructionView? {
        val kind = when (view) {
            is ExerciseView.ChooseTranslation -> InstructionKind.CHOOSE_TRANSLATION
            is ExerciseView.TypeAnswer -> InstructionKind.TYPE_ANSWER
            is ExerciseView.WordTiles -> InstructionKind.WORD_TILES
            is ExerciseView.Conjugation -> InstructionKind.CONJUGATION
            is ExerciseView.ListenChoose -> InstructionKind.LISTEN_CHOOSE
            is ExerciseView.ListenType -> InstructionKind.LISTEN_TYPE
            is ExerciseView.ChoosePolish -> InstructionKind.CHOOSE_POLISH
        }
        val instruction = course.instructions[kind] ?: return null
        return InstructionView(
            instruction.polish,
            localize(instruction.meaning),
            audio = if (playing.sound) course.clipFor(instruction.polish) else null,
        )
    }

    /** Saves a finished Checkpoint's Best, once, and says how it went. */
    private fun checkpointResult(playing: Playing, unitId: String, correct: Int, questions: Int): CheckpointResultView {
        playing.result?.let { return it }
        val percent = percentOf(correct, questions)
        val before = checkpoints.best(unitId)
        val newBest = before == null || percent > before
        if (newBest) checkpoints.record(unitId, percent)
        return CheckpointResultView(
            unitId = unitId,
            title = lessons.titleOf(lessons.unit(unitId)),
            correct = correct,
            questions = questions,
            percent = percent,
            best = maxOf(percent, before ?: 0),
            newBest = newBest,
            passed = percent >= CHECKPOINT_PASS_PERCENT,
        ).also { playing.result = it }
    }

    private val lessons: Lessons by lazy {
        Lessons(course, progress, checkpoints, clock, scheduler, ::localize) {
            path().levels.flatMap { level -> level.units.flatMap { it.dailySets } }.associate { it.id to it.state }
        }
    }

    /** The Path as shelves of lessons: Starter, then the book's chapters. */
    fun shelves(): List<ShelfView> = lessons.shelves()

    /** The Lesson screen of Unit [unitId]. */
    fun lesson(unitId: String): LessonView = lessons.lesson(unitId)

    /** The Słownik of Unit [unitId]: every word it teaches. */
    fun dictionary(unitId: String): List<WordView> = lessons.dictionary(unitId)

    /** The book's class recordings, with those the Learner imported kept in [dir]. */
    fun classAudio(dir: java.io.File): ClassAudio = ClassAudio(course, dir, ::localize)

    /** The Fiszki of Unit [unitId], due words first. */
    fun flashcards(unitId: String): List<FlashcardView> = lessons.flashcards(unitId)

    /** A flashcard turned: the Learner [knew] the word of Item [itemId] or not yet. Reschedules the Item for Review. */
    fun answerFlashcard(itemId: String, knew: Boolean) = lessons.answerFlashcard(itemId, knew)

    /**
     * Starts the Checkpoint of Unit [unitId]: up to [CHECKPOINT_QUESTIONS] of its Exercises,
     * mixed, each asked once. Like a Replay it is kept in memory and changes nothing on the Path,
     * in the Streak or in Review; at its end its Best is saved. It can be taken at any time.
     */
    fun startCheckpoint(unitId: String, sound: Boolean = true, random: Random = Random.Default): Step {
        val questions = checkpointQuestions(lessons.exercisesOf(unitId), random)
        check(questions.isNotEmpty()) { "Unit $unitId has no Exercises to test" }
        val set = DailySet("checkpoint-$unitId", exercises = questions.map(course.exercises::getValue))
        playing = Playing(set, DailySetInProgress(set.id, remaining = questions), replay = true, sound = sound, checkpoint = unitId)
        return next()
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

    /** Whether the step last given by [next] was a Tip, which is then being read and not yet done. */
    private var tipShowing = false

    /**
     * How far through the Daily Set being played the Learner is, in steps. A Tip counts once the
     * Learner has moved on from it, so the bar moves with the first tap. Call it while a Daily Set
     * is being played.
     */
    fun stepProgress(): StepProgress {
        val playing = current()
        val tips = playing.dailySet.tips.map { it.id }.toSet()
        val read = playing.state.shownTips.count { it in tips } - if (tipShowing) 1 else 0
        val exercises = total(playing)
        return StepProgress(done = read + exercises - playing.state.remaining.size, total = tips.size + exercises)
    }

    /**
     * The Unit the Daily Set just played completed, or null: when it left a Daily Set of its Unit
     * still to do, is not finished, or was a Replay or a Review-only Daily Set. Daily Sets done out
     * of order complete their Unit with whichever of them is completed last.
     */
    fun unitCompleted(): UnitCompletedView? {
        val playing = playing ?: return null
        if (playing.replay || playing.reviewOnly || playing.state.remaining.isNotEmpty()) return null
        val completed = progress.completedDailySets()
        for (level in course.levels) {
            level.units.forEachIndexed { index, unit ->
                if (unit.dailySets.any { it.id == playing.dailySet.id }) {
                    return if (unit.dailySets.all { it.id in completed }) UnitCompletedView(index + 1, localize(unit.title)) else null
                }
            }
        }
        return null
    }

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
            register = exercise.items.singleOrNull()?.let(course.itemsById::get)
                ?.takeIf { audioKey(it.text) == audioKey(exercise.prompt) }?.register,
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

    private fun view(tip: Tip): TipView {
        val body = readPolishMarks(localize(tip.body))
        return TipView(
            title = localize(tip.title),
            body = body.text,
            conjugation = tip.conjugation?.let { table ->
                ConjugationTableView(
                    verb = table.verb,
                    meaning = localize(table.meaning),
                    rows = Person.entries.map { ConjugationRow(it, table.forms.getValue(it)) },
                )
            },
            polish = body.polish,
        )
    }

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

    /**
     * The other side of the answer to the Exercise being shown: what the answer means when it is
     * Polish, or the Polish it is the meaning of. Shown with the feedback on a right answer, as one
     * more look at the pair. Null when no Exercise is being shown or it has nothing to add.
     */
    fun answerGloss(): String? {
        val exerciseId = playing?.state?.remaining?.firstOrNull() ?: return null
        return glossOf(course.exercises.getValue(exerciseId))
    }

    /**
     * Whether the Learner can hear the Daily Set being played: it has sound and they have not said
     * "can't listen now". Only then may anything play that the Learner did not ask for by a tap on
     * a speaker.
     */
    fun canListen(): Boolean = playing?.reading == false

    /**
     * The Clip that says [polish], a word or a sentence, whatever its capitals and spacing. Null
     * when there is none or when the Learner cannot listen (see [canListen]). It lets the screens
     * say the Polish they show, such as a word tile as it is placed.
     */
    fun clipOf(polish: String): ClipView? = if (canListen()) course.clipFor(polish) else null

    private fun glossOf(exercise: Exercise): String? = when (exercise) {
        is ChooseTranslation -> exercise.prompt
        is TypeAnswer -> localize(exercise.prompt)
        is WordTiles -> localize(exercise.prompt)
        // A form alone has no meaning to show; in a sentence it has the sentence's.
        is Conjugation -> exercise.sentenceMeaning?.let(::localize)
        is ListenChoose -> localize(exercise.meaning)
        is ListenType -> localize(exercise.meaning)
    }

    private fun localize(text: Localized): String = text.getValue(baseLanguage)
}
