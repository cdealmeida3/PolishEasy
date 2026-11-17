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
import com.polisheasy.engine.Game
import com.polisheasy.engine.GameState
import com.polisheasy.engine.PracticeView
import com.polisheasy.engine.PictureCardsView
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
    ) : Screen

    /** [today] is today's Daily Set as Home names it, for the stop the Learner is at; [started] when it is in progress. */
    data class Path(val path: PathView, val today: NextDailySetView? = null, val started: Boolean = false) : Screen

    /**
     * [notificationsAllowed] is whether Android lets the app show notifications at all.
     * [soundOn] is whether the app may play sound.
     */
    data class Settings(
        val reminderEnabled: Boolean,
        val reminderTime: LocalTime,
        val notificationsAllowed: Boolean,
        val soundOn: Boolean,
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

    /** Whether the Replay being played was started from Home, where it then goes back to. */
    private var replayFromHome = false

    private var stepsShown = 0

    private var roundsStarted = 0

    /** Set by the answer that completed today's Daily Set, for its Summary. */
    private var streakRaised: StreakRaised? = null

    fun startDailySet() = show(engine.startDailySet(sound = settings.soundOn))

    /** "Start" on the welcome: it is not shown again, and the Learner goes straight into their first Daily Set. */
    fun startFromWelcome() {
        settings.welcomed = true
        startDailySet()
    }

    fun resumeDailySet() = show(engine.resumeDailySet(sound = settings.soundOn))

    fun openPath() {
        screen = path()
    }

    private fun path() = Screen.Path(engine.path(), engine.nextDailySet(), started = engine.today() is Today.InProgress)

    fun replay(dailySetId: String) {
        replayFromHome = false
        show(engine.startReplay(dailySetId, sound = settings.soundOn))
    }

    /** Home's "Replay today's Daily Set": the one completed today, again, counting for nothing. */
    fun replayToday() {
        val dailySetId = engine.completedToday() ?: return
        replayFromHome = true
        show(engine.startReplay(dailySetId, sound = settings.soundOn))
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
        when {
            replay && replayFromHome -> backToHome()
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
            else -> Screen.GameSetup(practice.setup(game))
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
        screen = Screen.GameSetup(practice.setup(setup.game, wanted))
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

    /** A move in the Round being played, as the Round it leaves. The last one ends the Round and saves its result. */
    fun move(round: PracticeRound) {
        val playing = screen as? Screen.Round ?: return
        screen = if (round.finished) Screen.RoundResult(practice.finish(round), playing.setup) else playing.copy(round = round)
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
            ).also { streakRaised = null }
        }
    }
}
