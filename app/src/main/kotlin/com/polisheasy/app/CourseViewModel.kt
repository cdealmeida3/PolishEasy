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

/** What the app is showing. Every rule behind it lives in the course engine. */
sealed interface Screen {
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
    ) : Screen

    data class Path(val path: PathView) : Screen

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
    data class Tip(val tip: TipView, val progress: SetProgress, val shown: Int, val replay: Boolean) : Screen

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
    ) : Screen

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
    ) : Screen
}

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

class CourseViewModel(application: Application, private val engine: CourseEngine) : AndroidViewModel(application) {
    /** The one Android builds the view model with: the packaged course and the Learner's saved progress. */
    constructor(application: Application) : this(application, courseEngine(application))

    private val settings = Settings(application)

    var screen: Screen by mutableStateOf(home())
        private set

    private var stepsShown = 0

    /** Set by the answer that completed today's Daily Set, for its Summary. */
    private var streakRaised: StreakRaised? = null

    fun startDailySet() = show(engine.startDailySet(sound = settings.soundOn))

    fun resumeDailySet() = show(engine.resumeDailySet(sound = settings.soundOn))

    fun openPath() {
        screen = Screen.Path(engine.path())
    }

    fun replay(dailySetId: String) = show(engine.startReplay(dailySetId, sound = settings.soundOn))

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
            replay -> openPath()
            // Home shows the Streak go up once more, as the Learner arrives from the Summary that raised it.
            current is Screen.Summary -> screen = home().copy(streakRaisedFrom = current.streakRaised?.from)
            else -> backToHome()
        }
    }

    fun openSettings() {
        screen = settingsScreen()
    }

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
            is Screen.Path -> screen = Screen.Path(engine.path())
            is Screen.Settings -> screen = settingsScreen()
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

    private fun home() = Screen.Home(engine.today(), engine.streak(), engine.thisWeek(), engine.nextDailySet())

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
            is Step.ShowTip -> Screen.Tip(step.tip, step.progress, shown = ++stepsShown, replay = step.replay)
            is Step.ShowExercise -> Screen.Player(
                step.exercise,
                step.progress,
                shown = ++stepsShown,
                review = step.review,
                replay = step.replay,
                exerciseId = step.exerciseId,
                canListen = engine.canListen(),
            )
            is Step.ShowSummary -> Screen.Summary(
                step.correctAnswers,
                step.exercises,
                step.itemsLearned,
                step.reviewOnly,
                step.replay,
                streakRaised = streakRaised.takeUnless { step.replay },
                week = engine.thisWeek(),
            ).also { streakRaised = null }
        }
    }
}
