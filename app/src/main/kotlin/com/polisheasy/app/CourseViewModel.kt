package com.polisheasy.app

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.polisheasy.engine.Answer
import com.polisheasy.engine.CourseEngine
import com.polisheasy.engine.DayView
import com.polisheasy.engine.ExerciseView
import com.polisheasy.engine.Feedback
import com.polisheasy.engine.FileLearnerProgress
import com.polisheasy.engine.ItemView
import com.polisheasy.engine.NextDailySetView
import com.polisheasy.engine.PathView
import com.polisheasy.engine.SetProgress
import com.polisheasy.engine.Step
import com.polisheasy.engine.DailySetState
import com.polisheasy.engine.TipView
import com.polisheasy.engine.Today
import com.polisheasy.engine.loadCourse
import java.io.File
import java.time.Clock
import java.time.LocalTime
import com.polisheasy.engine.ClipView
import com.polisheasy.engine.SoFarView
import com.polisheasy.engine.StepProgress
import com.polisheasy.engine.UnitCompletedView
import com.polisheasy.engine.FilePracticeProgress
import com.polisheasy.engine.MemoryRound
import com.polisheasy.engine.Game
import com.polisheasy.engine.GameState
import com.polisheasy.engine.PracticeView
import com.polisheasy.engine.PictureCardsView
import com.polisheasy.engine.CheckpointResultView
import com.polisheasy.engine.FileCheckpointBests
import com.polisheasy.engine.FlashcardView
import com.polisheasy.engine.InstructionView
import com.polisheasy.engine.LessonView
import com.polisheasy.engine.ShelfView
import com.polisheasy.engine.WordView
import com.polisheasy.engine.GameSetup as GameSetupView
import com.polisheasy.engine.Practice as PracticeGames
import com.polisheasy.engine.Round as PracticeRound
import com.polisheasy.engine.RoundResult as RoundResultView

/** What the app is showing. Every rule behind it lives in the course engine. */
sealed interface Screen {
    /** The welcome a new Learner sees once, before their first Daily Set. */
    data object Welcome : Screen

    /**
     * [week] is this calendar week, Monday to Sunday. [next] is where the Learner is
     * on the Path, or null once every Daily Set released so far is completed.
     */
    data class Home(
        val today: Today,
        val streak: Int,
        val week: List<DayView>,
        val next: NextDailySetView?,
        /** Set when the Learner has just come back from the Daily Set that raised the Streak from this number. */
        val streakRaisedFrom: Int? = null,
        /** What the Learner has built up so far. */
        val soFar: SoFarView = SoFarView(0, 0, 0),
        /** Whether a Daily Set of the Path was completed today, which can be replayed from here. */
        val replayToday: Boolean = false,
        /** Whether a new or Review-only Daily Set was completed today, so today counts for the Streak already. */
        val doneToday: Boolean = false,
        /** Whether Practice is open now. */
        val practiceOpen: Boolean = false,
    ) : Screen

    /**
     * [today] is today's Daily Set as Home names it, for the stop the Learner is at; [started] when it
     * is in progress. [oneADay] when one new Daily Set opens a day, so the stops ahead are locked.
     */
    data class Path(
        val path: PathView,
        val today: NextDailySetView? = null,
        val started: Boolean = false,
        val oneADay: Boolean = true,
        /** The Path as shelves: Starter, then the book, a card for each chapter. */
        val shelves: List<ShelfView> = emptyList(),
    ) : Screen

    /**
     * A chapter's Lesson screen, inside the Path tab. [path] is the Path, for the state of each of its
     * sections; [started] and [oneADay] as on [Path]. [inProgress] is the Daily Set in progress,
     * for the question asked before another is started in its place.
     */
    data class Lesson(
        val lesson: LessonView,
        val path: PathView,
        val today: NextDailySetView? = null,
        val started: Boolean = false,
        val oneADay: Boolean = true,
        val inProgress: String? = null,
    ) : Screen

    /** A chapter's Słownik: its words, each with its meaning and Clip. [sound] is whether they may be heard. */
    data class Dictionary(val lesson: LessonView, val words: List<WordView>, val sound: Boolean) : Screen

    /**
     * A chapter's Fiszki, one card at a time: [cards] in the order they come, the one at [index]
     * showing, [knew] of those turned so far known. [shown] is different for each card.
     */
    data class Flashcards(
        val lesson: LessonView,
        val cards: List<FlashcardView>,
        val index: Int = 0,
        val knew: Int = 0,
        val sound: Boolean = true,
        val shown: Int = 0,
    ) : Screen

    /**
     * [notificationsAllowed] is whether Android lets the app show notifications at all.
     * [soundOn] is whether the app may play sound. [oneADay] is whether one new Daily Set opens a day.
     */
    data class Settings(
        val reminderEnabled: Boolean,
        val reminderTime: LocalTime,
        val notificationsAllowed: Boolean,
        val soundOn: Boolean,
        val oneADay: Boolean = false,
    ) : Screen

    /** [shown] is different for each step shown. [replay] when the Tip belongs to a Replay. */
    data class Tip(
        val tip: TipView,
        val progress: SetProgress,
        val shown: Int,
        val replay: Boolean,
        /** How far through the Daily Set this is, in steps: what the bar shows. */
        val steps: StepProgress = StepProgress(0, 0),
    ) : Screen

    data class Player(
        val exercise: ExerciseView,
        val progress: SetProgress,
        /** Different for each step shown, so an Exercise that returns unchanged still starts fresh. */
        val shown: Int,
        /** Whether this Exercise is part of the Review that ends the Daily Set. */
        val review: Boolean,
        /** Whether this is a Replay, which saves nothing. */
        val replay: Boolean,
        /** The Exercise's stable identifier, for a mistake report. */
        val exerciseId: String,
        val feedback: Feedback? = null,
        /** The other side of the answer, shown with the feedback on a right one: its meaning, or the Polish for it. */
        val gloss: String? = null,
        /** Whether Polish may be played without the Learner tapping a speaker: sound is on and they can listen now. */
        val canListen: Boolean = false,
        /** How far through the Daily Set this is, in steps: what the bar shows. */
        val steps: StepProgress = StepProgress(0, 0),
        /** The Exercise's instruction as the book gives it, in Polish, or null when the course gives none. */
        val instruction: InstructionView? = null,
        /** Whether this Exercise is a question of a chapter's Test. */
        val checkpoint: Boolean = false,
    ) : Screen

    /** Practice. [today] is today's Daily Set, which the screen starts or continues while Practice is locked. */
    data class Practice(val view: PracticeView, val today: Today) : Screen

    /** Picture cards, to look at and listen to. [sound] is whether a card may say its word. */
    data class PictureCards(val view: PictureCardsView, val sound: Boolean) : Screen

    /** A Game before a Round, with the ways to play it. */
    data class GameSetup(val setup: GameSetupView) : Screen

    /** A Round being played. [number] is different for each Round started; [sound] is whether Polish may be said aloud. */
    data class Round(val round: PracticeRound, val setup: GameSetupView, val number: Int, val sound: Boolean) : Screen

    /** The end of a Round. [setup] is the way it was played, for "Play again". */
    data class RoundResult(val result: RoundResultView, val setup: GameSetupView) : Screen

    data class Summary(
        val correctAnswers: Int,
        val exercises: Int,
        val itemsLearned: List<ItemView>,
        /** Whether it was a Review-only Daily Set. */
        val reviewOnly: Boolean,
        /** Whether it was a Replay. */
        val replay: Boolean,
        /** The Streak this Daily Set raised, or null when it raised none, as a Replay never does. */
        val streakRaised: StreakRaised? = null,
        /** This calendar week, Monday to Sunday, for the Streak's dots. */
        val week: List<DayView> = emptyList(),
        /** The Reminder as this Summary offers it, or null when it does not: it is offered once, while it is off. */
        val reminder: ReminderOffer? = null,
        /** The Unit this Daily Set completed, or null when it completed none. */
        val unitCompleted: UnitCompletedView? = null,
        /** Whether the Summary offers to start the next Daily Set straight away, as it does at the Learner's own pace. */
        val startNext: Boolean = false,
        /** The end of a chapter's Test, which has its own result screen, or null for a Daily Set. */
        val checkpoint: CheckpointResultView? = null,
        /** The colour of the chapter the Test was for. */
        val accent: Int = 0,
    ) : Screen
}

/** The Reminder offered on a Summary: the [time] it would come at, and whether the Learner has taken it [on]. */
data class ReminderOffer(val time: LocalTime, val on: Boolean = false)

/** The Streak going up as a Daily Set is completed: what it was and what it is now. */
data class StreakRaised(val from: Int, val to: Int)

/**
 * The course engine over the packaged course and the Learner's saved progress, as it is now.
 * The screens and the Reminder both build theirs here, so they answer from the same rules.
 */
fun courseEngine(context: Context): CourseEngine = CourseEngine(
    course = loadCourse { path ->
        context.assets.open(path).bufferedReader().use { it.readText() }
    },
    progress = FileLearnerProgress(File(context.filesDir, "progress.json")),
    clock = Clock.systemDefaultZone(),
    oneADay = Settings(context).let { settings -> { settings.oneADay } },
    checkpoints = FileCheckpointBests(File(context.filesDir, "checkpoints.json")),
)

class CourseViewModel(
    application: Application,
    private val engine: CourseEngine,
    /** The Games, over the same course and progress, with the Bests kept in a file of their own. */
    private val practice: PracticeGames = engine.practice(FilePracticeProgress(File(application.filesDir, "practice.json"))),
) : AndroidViewModel(application) {
    /** The one Android builds the view model with: the packaged course and the Learner's saved progress. */
    constructor(application: Application) : this(application, courseEngine(application))

    private val settings = Settings(application)

    var screen: Screen by mutableStateOf(firstScreen())
        private set

    /** Whether the app may play sound. */
    val soundOn: Boolean get() = settings.soundOn

    /** Whether the Replay being played was started from Home, where it then goes back to. */
    private var replayFromHome = false

    /** The chapter whose Lesson screen started what is being played, where leaving it goes back to; null for Home and the Path. */
    private var fromLesson: String? = null

    private var stepsShown = 0

    private var roundsStarted = 0

    /** Set by the answer that completed today's Daily Set, for its Summary. */
    private var streakRaised: StreakRaised? = null

    fun startDailySet() {
        fromLesson = null
        show(engine.startDailySet(sound = settings.soundOn))
    }

    /** "Start" on the welcome: it is not shown again, and the Learner goes straight into their first Daily Set. */
    fun startFromWelcome() {
        settings.welcomed = true
        startDailySet()
    }

    fun resumeDailySet() {
        fromLesson = null
        show(engine.resumeDailySet(sound = settings.soundOn))
    }

    fun openPath() {
        screen = path()
    }

    private fun path() = Screen.Path(
        engine.path(),
        engine.nextDailySet(),
        started = engine.today() is Today.InProgress,
        oneADay = settings.oneADay,
        shelves = engine.shelves(),
    )

    /** A chapter tapped on the Path, or come back to: its Lesson screen. */
    fun openLesson(unitId: String) {
        screen = lessonScreen(unitId)
    }

    private fun lessonScreen(unitId: String): Screen.Lesson {
        val today = engine.today()
        val path = engine.path()
        return Screen.Lesson(
            engine.lesson(unitId),
            path,
            engine.nextDailySet(),
            started = today is Today.InProgress,
            oneADay = settings.oneADay,
            inProgress = engine.dailySetInProgress(),
        )
    }

    /** The unit of the Lesson screen showing, or of the Słownik or Fiszki opened from it. */
    private fun lessonShowing(): String? = when (val current = screen) {
        is Screen.Lesson -> current.lesson.unitId
        is Screen.Dictionary -> current.lesson.unitId
        is Screen.Flashcards -> current.lesson.unitId
        else -> null
    }

    /** A section on a Lesson screen: today's started or continued, a completed one replayed, an open one started. */
    fun openSection(dailySetId: String) {
        val unitId = lessonShowing() ?: return
        val state = engine.path().levels.flatMap { it.units }.flatMap { it.dailySets }.firstOrNull { it.id == dailySetId }?.state
        when (state) {
            DailySetState.COMPLETED -> {
                replayFromHome = false
                show(engine.startReplay(dailySetId, sound = settings.soundOn))
            }
            DailySetState.TODAY -> when (val today = engine.today()) {
                Today.DailySetAvailable -> show(engine.startDailySet(sound = settings.soundOn))
                is Today.InProgress -> if (today.reviewOnly) return openLesson(unitId) else show(engine.resumeDailySet(sound = settings.soundOn))
                else -> return openLesson(unitId)
            }
            DailySetState.OPEN -> show(engine.startDailySet(sound = settings.soundOn, dailySetId = dailySetId))
            DailySetState.LOCKED, null -> return
        }
        fromLesson = unitId
    }

    /** "Słownik" on a Lesson screen: every word of the chapter. */
    fun openDictionary() {
        val unitId = lessonShowing() ?: return
        screen = Screen.Dictionary(engine.lesson(unitId), engine.dictionary(unitId), sound = settings.soundOn)
    }

    /** "Fiszki" on a Lesson screen: the chapter's words as cards, those due for Review first. */
    fun openFlashcards() {
        val unitId = lessonShowing() ?: return
        screen = Screen.Flashcards(engine.lesson(unitId), engine.flashcards(unitId), sound = settings.soundOn, shown = ++stepsShown)
    }

    /** The card showing turned and answered: the Learner [knew] it or not. Its word is rescheduled, and the next card shows. */
    fun answerFlashcard(knew: Boolean) {
        val cards = screen as? Screen.Flashcards ?: return
        val card = cards.cards.getOrNull(cards.index) ?: return
        engine.answerFlashcard(card.word.itemId, knew)
        screen = cards.copy(index = cards.index + 1, knew = cards.knew + if (knew) 1 else 0, shown = ++stepsShown)
    }

    /** "Again" at the end of the Fiszki: the cards afresh, in the order they come now. */
    fun flashcardsAgain() = openFlashcards()

    /** "Test" on a Lesson screen: the chapter's Checkpoint, which saves nothing but its Best. */
    fun startCheckpoint() {
        val unitId = lessonShowing() ?: return
        fromLesson = unitId
        replayFromHome = false
        show(engine.startCheckpoint(unitId, sound = settings.soundOn))
    }

    /** "Try again" on a Test's result: the Test again, with other questions. */
    fun checkpointAgain() {
        val unitId = (screen as? Screen.Summary)?.checkpoint?.unitId ?: return
        fromLesson = unitId
        show(engine.startCheckpoint(unitId, sound = settings.soundOn))
    }

    /** Back from the Słownik or the Fiszki, to the Lesson screen they were opened from. */
    fun backToLesson() {
        val unitId = lessonShowing() ?: return openPath()
        openLesson(unitId)
    }

    fun replay(dailySetId: String) {
        replayFromHome = false
        fromLesson = null
        show(engine.startReplay(dailySetId, sound = settings.soundOn))
    }

    /** Home's "Replay today's Daily Set": the one completed today, again, counting for nothing. */
    fun replayToday() {
        val dailySetId = engine.completedToday() ?: return
        replayFromHome = true
        fromLesson = null
        show(engine.startReplay(dailySetId, sound = settings.soundOn))
    }

    /**
     * A Daily Set the Learner chose on the Path, at their own pace: started, or continued when it
     * is the one in progress. Any other one in progress is forgotten; the Path has asked first.
     */
    fun startChosen(dailySetId: String) {
        fromLesson = null
        show(engine.startDailySet(sound = settings.soundOn, dailySetId = dailySetId))
    }

    /** "Start the next one" on a Summary: the next Daily Set, straight away. */
    fun startNext() {
        if (engine.today() == Today.DailySetAvailable) {
            // The next one is played from where this one was: a Lesson screen is come back to.
            val lesson = fromLesson
            startDailySet()
            fromLesson = lesson
        } else {
            leaveDailySet()
        }
    }

    /** Today's Daily Set tapped on the Path: started or continued, as from Home. */
    fun openTodays() = when (val today = engine.today()) {
        Today.DailySetAvailable -> startDailySet()
        is Today.InProgress -> if (today.reviewOnly) openPath() else resumeDailySet()
        else -> openPath() // Midnight passed while the Path was showing; show it afresh.
    }

    /** Leaves a Tip, an Exercise or a Summary: a Replay goes back to the Path, anything else to Home. */
    fun leaveDailySet() {
        val current = screen
        val replay = when (current) {
            is Screen.Tip -> current.replay
            is Screen.Player -> current.replay
            is Screen.Summary -> current.replay
            else -> false
        }
        val lesson = fromLesson
        when {
            replay && replayFromHome -> backToHome()
            // A Daily Set, a Replay or a Test started from a Lesson screen goes back to it.
            lesson != null -> {
                fromLesson = null
                openLesson(lesson)
            }
            replay -> openPath()
            // Home shows the Streak go up once more, as the Learner arrives from the Summary that raised it.
            current is Screen.Summary -> screen = home().copy(streakRaisedFrom = current.streakRaised?.from)
            else -> backToHome()
        }
    }

    fun openSettings() {
        screen = settingsScreen()
    }

    fun openPractice() {
        screen = practiceScreen()
    }

    private fun practiceScreen() = Screen.Practice(practice.view(sound = settings.soundOn), engine.today())

    /** A Game tapped on the Practice screen: its cards, or what there is to choose before a Round, as it was last played. */
    fun openGame(game: Game) {
        screen = when (game) {
            Game.PICTURE_CARDS -> Screen.PictureCards(practice.pictureCards(), sound = settings.soundOn)
            else -> Screen.GameSetup(practice.setup(game, sound = settings.soundOn))
        }
    }

    /** Another deck of Picture cards chosen. */
    fun chooseDeck(deck: String) {
        val cards = screen as? Screen.PictureCards ?: return
        screen = cards.copy(view = practice.pictureCards(deck, cards.view.kinds.selected))
    }

    /** Pictures or every word chosen for the Picture cards. */
    fun chooseCards(kind: String) {
        val cards = screen as? Screen.PictureCards ?: return
        screen = cards.copy(view = practice.pictureCards(cards.view.decks.selected, kind))
    }

    /** One of the ways to play chosen: the option [option] for the choice at [choice]. */
    fun choose(choice: Int, option: String) {
        val setup = (screen as? Screen.GameSetup)?.setup ?: return
        val wanted = setup.selected.toMutableList().also { it[choice] = option }
        screen = Screen.GameSetup(practice.setup(setup.game, wanted, settings.soundOn))
    }

    /** "Play" before a Round, and "Play again" after one: a new Round, played the same way. */
    fun play() {
        val setup = when (val current = screen) {
            is Screen.GameSetup -> current.setup
            is Screen.RoundResult -> current.setup
            else -> return
        }
        // Midnight may have passed, or sound been turned off, since the Game was opened.
        val sound = settings.soundOn
        if (practice.view(sound).games.none { it.game == setup.game && it.state == GameState.READY } || !engine.practiceOpen()) {
            openPractice()
            return
        }
        screen = Screen.Round(practice.start(setup, sound), setup, number = ++roundsStarted, sound = sound)
    }

    /**
     * A move in the Round being played, as the Round it leaves. The last one ends the Round and
     * saves its result, except on a board, which is shown finished until [endRound].
     */
    fun move(round: PracticeRound) {
        val playing = screen as? Screen.Round ?: return
        screen = playing.copy(round = round)
        if (round !is MemoryRound) endRound()
    }

    /** Ends the Round being played, if it is finished, and saves its result. */
    fun endRound() {
        val playing = screen as? Screen.Round ?: return
        if (playing.round.finished) screen = Screen.RoundResult(practice.finish(playing.round), playing.setup)
    }

    /** Leaves a Game for the Practice screen. A Round left unfinished is not kept. */
    fun leaveGame() = openPractice()

    /** The Clip that says [polish] in a Game, if there is one. */
    fun practiceClipOf(polish: String): ClipView? = practice.clipOf(polish)

    /** Turns the Reminder on or off and sets or removes its alarm. */
    fun setReminderEnabled(enabled: Boolean) {
        settings.reminderEnabled = enabled
        Reminder.arm(getApplication())
        screen = settingsScreen()
    }

    fun setReminderTime(time: LocalTime) {
        settings.reminderTime = time
        Reminder.arm(getApplication())
        screen = settingsScreen()
    }

    /** "Remind me" on the Summary that offers the Reminder: it is turned on at the time it has, and the Summary says so. */
    fun acceptReminder() {
        val summary = screen as? Screen.Summary ?: return
        val offer = summary.reminder ?: return
        settings.reminderEnabled = true
        Reminder.arm(getApplication())
        screen = summary.copy(reminder = offer.copy(on = true))
    }

    /**
     * Turns One a day on or off. It takes effect at once: the engine and the Reminder read it each
     * time they decide, so there is nothing to re-arm.
     */
    fun setOneADay(on: Boolean) {
        settings.oneADay = on
        screen = settingsScreen()
    }

    /** Turns sound on or off. It takes effect when a Daily Set is next started, continued or replayed. */
    fun setSoundOn(on: Boolean) {
        settings.soundOn = on
        screen = settingsScreen()
    }

    /**
     * Asks again what the screen shows, so Home and the Path are right after midnight has passed,
     * and Settings is right when the Learner comes back from Android's own settings.
     */
    fun refresh() {
        when (val current = screen) {
            // Home keeps what it is in the middle of showing: arriving from the Summary is itself a moment that asks again.
            is Screen.Home -> screen = home().copy(streakRaisedFrom = current.streakRaisedFrom)
            is Screen.Path -> screen = path()
            is Screen.Lesson -> screen = lessonScreen(current.lesson.unitId)
            is Screen.Settings -> screen = settingsScreen()
            is Screen.Practice -> screen = practiceScreen()
            else -> Unit
        }
    }

    fun answer(answer: Answer) {
        val player = screen as? Screen.Player ?: return
        if (player.feedback != null) return
        val streakBefore = engine.streak()
        val gloss = engine.answerGloss()
        val feedback = engine.answer(answer)
        // The answer that completes the Daily Set is the one that raises the Streak.
        val streakNow = engine.streak()
        if (streakNow > streakBefore) streakRaised = StreakRaised(streakBefore, streakNow)
        screen = player.copy(feedback = feedback, gloss = gloss)
    }

    /**
     * "Can't listen now" on a listening Exercise not yet answered: the same Exercise in its reading
     * form, and no more listening in this Daily Set. Showing it anew stops the Clip that was playing.
     */
    fun cantListenNow() {
        val player = screen as? Screen.Player ?: return
        if (player.feedback != null) return
        show(engine.cantListenNow())
    }

    /** The Clip that says [polish], if there is one and the Learner can listen now. */
    fun clipOf(polish: String): ClipView? = engine.clipOf(polish)

    /** Moves on from a Tip or from the feedback on an answer. */
    fun continueToNext() = show(engine.next())

    fun backToHome() {
        screen = home()
    }

    /** A Learner who has done nothing yet is welcomed first, once. */
    private fun firstScreen(): Screen {
        val new = !settings.welcomed && engine.today() == Today.DailySetAvailable && engine.soFar().dailySetsCompleted == 0
        return if (new) Screen.Welcome else home()
    }

    private fun home() = Screen.Home(
        engine.today(),
        engine.streak(),
        engine.thisWeek(),
        engine.nextDailySet(),
        soFar = engine.soFar(),
        replayToday = engine.completedToday() != null,
        doneToday = engine.doneToday(),
        practiceOpen = engine.practiceOpen(),
    )

    /** The Reminder for a Summary to offer: once, at the end of a Daily Set that counted, while it is off. */
    private fun reminderOffer(replay: Boolean): ReminderOffer? {
        if (replay || settings.reminderEnabled || settings.reminderOffered) return null
        settings.reminderOffered = true
        return ReminderOffer(settings.reminderTime)
    }

    private fun settingsScreen() = Screen.Settings(
        reminderEnabled = settings.reminderEnabled,
        reminderTime = settings.reminderTime,
        notificationsAllowed = getApplication<Application>()
            .getSystemService(NotificationManager::class.java)
            .areNotificationsEnabled(),
        soundOn = settings.soundOn,
        oneADay = settings.oneADay,
    )

    private fun show(step: Step) {
        screen = when (step) {
            is Step.ShowTip -> Screen.Tip(step.tip, step.progress, shown = ++stepsShown, replay = step.replay, steps = engine.stepProgress())
            is Step.ShowExercise -> Screen.Player(
                step.exercise,
                step.progress,
                shown = ++stepsShown,
                review = step.review,
                replay = step.replay,
                exerciseId = step.exerciseId,
                canListen = engine.canListen(),
                steps = engine.stepProgress(),
                instruction = step.instruction,
                checkpoint = step.checkpoint,
            )
            is Step.ShowSummary -> Screen.Summary(
                step.correctAnswers,
                step.exercises,
                step.itemsLearned,
                step.reviewOnly,
                step.replay,
                streakRaised = streakRaised.takeUnless { step.replay },
                week = engine.thisWeek(),
                reminder = reminderOffer(step.replay),
                unitCompleted = engine.unitCompleted(),
                // A Daily Set can be started now only at the Learner's own pace, once one is done today.
                startNext = !step.replay && !step.reviewOnly && engine.today() == Today.DailySetAvailable,
                checkpoint = step.checkpoint,
                accent = step.checkpoint?.let { engine.lesson(it.unitId).accent } ?: 0,
            ).also { streakRaised = null }
        }
    }
}
