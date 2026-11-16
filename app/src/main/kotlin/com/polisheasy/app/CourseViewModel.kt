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
import com.polisheasy.engine.SetProgress
import com.polisheasy.engine.Step
import com.polisheasy.engine.TipView
import com.polisheasy.engine.Today
import com.polisheasy.engine.loadCourse
import java.io.File
import java.time.Clock

/** What the app is showing. Every rule behind it lives in the course engine. */
sealed interface Screen {
    data class Home(val today: Today) : Screen

    data class Tip(val tip: TipView, val progress: SetProgress) : Screen

    data class Player(
        val exercise: ExerciseView,
        val progress: SetProgress,
        /** Different each time an Exercise is shown, so one that returns unchanged still starts fresh. */
        val shown: Int,
        /** Whether this Exercise is part of the Review that ends the Daily Set. */
        val review: Boolean,
        val feedback: Feedback? = null,
    ) : Screen

    data class Summary(
        val correctAnswers: Int,
        val exercises: Int,
        val itemsLearned: List<ItemView>,
        /** Whether it was a Review-only Daily Set. */
        val reviewOnly: Boolean,
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

    var screen: Screen by mutableStateOf(Screen.Home(engine.today()))
        private set

    private var exercisesShown = 0

    fun startDailySet() = show(engine.startDailySet())

    fun resumeDailySet() = show(engine.resumeDailySet())

    /** Asks the engine again what today holds, so Home is right after midnight has passed. */
    fun refreshHome() {
        if (screen is Screen.Home) screen = Screen.Home(engine.today())
    }

    fun answer(answer: Answer) {
        val player = screen as? Screen.Player ?: return
        if (player.feedback != null) return
        screen = player.copy(feedback = engine.answer(answer))
    }

    /** Moves on from a Tip or from the feedback on an answer. */
    fun continueToNext() = show(engine.next())

    fun backToHome() {
        screen = Screen.Home(engine.today())
    }

    private fun show(step: Step) {
        screen = when (step) {
            is Step.ShowTip -> Screen.Tip(step.tip, step.progress)
            is Step.ShowExercise -> Screen.Player(step.exercise, step.progress, shown = ++exercisesShown, review = step.review)
            is Step.ShowSummary -> Screen.Summary(step.correctAnswers, step.exercises, step.itemsLearned, step.reviewOnly)
        }
    }
}
