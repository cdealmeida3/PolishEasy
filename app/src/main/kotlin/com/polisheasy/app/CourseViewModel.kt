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
import com.polisheasy.engine.InMemoryLearnerProgress
import com.polisheasy.engine.Step
import com.polisheasy.engine.Today
import com.polisheasy.engine.loadCourse
import java.time.Clock

/** What the app is showing. Every rule behind it lives in the course engine. */
sealed interface Screen {
    data class Home(val today: Today) : Screen

    data class Player(val exercise: ExerciseView, val feedback: Feedback? = null) : Screen

    data class Summary(val correctAnswers: Int, val exercises: Int) : Screen
}

class CourseViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = CourseEngine(
        course = loadCourse { path ->
            application.assets.open(path).bufferedReader().use { it.readText() }
        },
        // Progress is kept only while the app is open until saved progress is built.
        progress = InMemoryLearnerProgress(),
        clock = Clock.systemDefaultZone(),
    )

    var screen: Screen by mutableStateOf(Screen.Home(engine.today()))
        private set

    fun startDailySet() = show(engine.startDailySet())

    fun answer(answer: Answer) {
        val player = screen as? Screen.Player ?: return
        if (player.feedback != null) return
        screen = player.copy(feedback = engine.answer(answer))
    }

    fun continueAfterFeedback() = show(engine.next())

    fun backToHome() {
        screen = Screen.Home(engine.today())
    }

    private fun show(step: Step) {
        screen = when (step) {
            is Step.ShowExercise -> Screen.Player(step.exercise)
            is Step.ShowSummary -> Screen.Summary(step.correctAnswers, step.exercises)
        }
    }
}
