package com.polisheasy.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.polisheasy.engine.Answer
import com.polisheasy.engine.CourseEngine
import com.polisheasy.engine.ExerciseView
import com.polisheasy.engine.Feedback
import com.polisheasy.engine.FileLearnerProgress
import com.polisheasy.engine.ItemView
import com.polisheasy.engine.PathView
import com.polisheasy.engine.SetProgress
import com.polisheasy.engine.Step
import com.polisheasy.engine.TipView
import com.polisheasy.engine.Today
import com.polisheasy.engine.loadCourse
import java.io.File
import java.time.Clock

/** What the app is showing. Every rule behind it lives in the course engine. */
sealed interface Screen {
    data class Home(val today: Today, val streak: Int) : Screen

    data class Path(val path: PathView) : Screen

    /** [replay] when the Tip belongs to a Replay. */
    data class Tip(val tip: TipView, val progress: SetProgress, val replay: Boolean) : Screen

    data class Player(
        val exercise: ExerciseView,
        val progress: SetProgress,
        /** Different each time an Exercise is shown, so one that returns unchanged still starts fresh. */
        val shown: Int,
        /** Whether this Exercise is part of the Review that ends the Daily Set. */
        val review: Boolean,
        /** Whether this is a Replay, which saves nothing. */
        val replay: Boolean,
        val feedback: Feedback? = null,
    ) : Screen

    data class Summary(
        val correctAnswers: Int,
        val exercises: Int,
        val itemsLearned: List<ItemView>,
        /** Whether it was a Review-only Daily Set. */
        val reviewOnly: Boolean,
        /** Whether it was a Replay. */
        val replay: Boolean,
    ) : Screen
}

class CourseViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = CourseEngine(
        course = loadCourse { path ->
            application.assets.open(path).bufferedReader().use { it.readText() }
        },
        progress = FileLearnerProgress(File(application.filesDir, "progress.json")),
        clock = Clock.systemDefaultZone(),
    )

    var screen: Screen by mutableStateOf(home())
        private set

    private var exercisesShown = 0

    fun startDailySet() = show(engine.startDailySet())

    fun resumeDailySet() = show(engine.resumeDailySet())

    fun openPath() {
        screen = Screen.Path(engine.path())
    }

    fun replay(dailySetId: String) = show(engine.startReplay(dailySetId))

    /** Today's Daily Set tapped on the Path: started or continued, as from Home. */
    fun openTodays() = when (val today = engine.today()) {
        Today.DailySetAvailable -> startDailySet()
        is Today.InProgress -> if (today.reviewOnly) openPath() else resumeDailySet()
        else -> openPath() // Midnight passed while the Path was showing; show it afresh.
    }

    /** Leaves a Tip, an Exercise or a Summary: a Replay goes back to the Path, anything else to Home. */
    fun leaveDailySet() {
        val replay = when (val current = screen) {
            is Screen.Tip -> current.replay
            is Screen.Player -> current.replay
            is Screen.Summary -> current.replay
            else -> false
        }
        if (replay) openPath() else backToHome()
    }

    /** Asks the engine again what today holds, so Home and the Path are right after midnight has passed. */
    fun refresh() {
        when (screen) {
            is Screen.Home -> screen = home()
            is Screen.Path -> screen = Screen.Path(engine.path())
            else -> Unit
        }
    }

    fun answer(answer: Answer) {
        val player = screen as? Screen.Player ?: return
        if (player.feedback != null) return
        screen = player.copy(feedback = engine.answer(answer))
    }

    /** Moves on from a Tip or from the feedback on an answer. */
    fun continueToNext() = show(engine.next())

    fun backToHome() {
        screen = home()
    }

    private fun home() = Screen.Home(engine.today(), engine.streak())

    private fun show(step: Step) {
        screen = when (step) {
            is Step.ShowTip -> Screen.Tip(step.tip, step.progress, step.replay)
            is Step.ShowExercise -> Screen.Player(
                step.exercise,
                step.progress,
                shown = ++exercisesShown,
                review = step.review,
                replay = step.replay,
            )
            is Step.ShowSummary -> Screen.Summary(
                step.correctAnswers,
                step.exercises,
                step.itemsLearned,
                step.reviewOnly,
                step.replay,
            )
        }
    }
}
