package com.polisheasy.app

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.polisheasy.app.ui.PolishEasyApp
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.engine.Course
import com.polisheasy.engine.CourseEngine
import com.polisheasy.engine.FileCheckpointBests
import com.polisheasy.engine.FileLearnerProgress
import com.polisheasy.engine.InMemoryPracticeProgress
import com.polisheasy.engine.loadCourse
import java.io.File
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * The book's chapter 0 on screen, over the real course: the Path's shelves, the Lesson screen,
 * the Słownik, the Fiszki and the Test, played the way a Learner plays them, at their own pace.
 */
@RunWith(RobolectricTestRunner::class)
class PlayingALessonOnScreenTest {
    @get:Rule(order = 0)
    val folder = TemporaryFolder()

    @get:Rule(order = 1)
    val hostActivity = object : ExternalResource() {
        override fun before() {
            shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
            Settings(app).welcomed = true
            Settings(app).oneADay = false
        }
    }

    @get:Rule(order = 2)
    val screen = createAndroidComposeRule<ComponentActivity>()

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val clock = SettableClock()
    private val clips = RecordingClips()
    private val course: Course = loadCourse { path -> app.assets.open(path).bufferedReader().use { it.readText() } }
    private var viewModel: CourseViewModel? by mutableStateOf(null)

    @Test
    fun `the Path shows Starter then the book, and a chapter opens on its Lesson screen`() {
        openApp()
        tap(R.string.nav_path)

        shows("Starter")
        shows("Polski krok po kroku 1")
        lesson("Dzień dobry!")

        shows(text(R.string.lesson_label, 0))
        shows("Hello!")
        shows(R.string.lesson_look_up)
        // A look-up word says itself and shows what it means.
        screen.onNode(hasText("lekcja") and hasClickAction()).performScrollTo().performClick()
        shows("a lesson")
        assertTrue(clips.played.isNotEmpty())
        scrollTo("Polecenia")
        shows("Instructions")

        back()
        // A chapter still being written says it is on its way.
        lesson("Same problemy!")
        scrollTo(text(R.string.lesson_coming_title))
    }

    @Test
    fun `a section of chapter 0 is played from the Lesson screen, in Polish instructions, and the Lesson screen is come back to`() {
        openApp()
        tap(R.string.nav_path)
        lesson("Dzień dobry!")
        scrollTo("Polecenia")
        screen.onNode(hasText("Polecenia") and hasClickAction()).performClick()
        tap(R.string.action_continue)

        // The book's instruction, in Polish, with its English one tap away.
        shows("Proszę przetłumaczyć.")
        screen.onNodeWithText("Translate.").assertDoesNotExist()
        screen.onNodeWithContentDescription(text(R.string.instruction_translate)).performClick()
        shows("Translate.")

        playToTheEnd()
        tap(R.string.summary_back_home)
        // The Learner is back on the chapter's Lesson screen, the section ticked.
        shows(text(R.string.lesson_label, 0))
        scrollTo("Polecenia")
        screen.onNode(hasText("Polecenia") and hasText(text(R.string.path_completed), substring = true)).assertIsDisplayed()
    }

    @Test
    fun `the Słownik finds a word, the Fiszki turn and reschedule, and the Test keeps its Best and seals the chapter`() {
        openApp()
        tap(R.string.nav_path)
        lesson("Dzień dobry!")

        scrollTo(text(R.string.lesson_dictionary))
        tap(R.string.lesson_dictionary)
        type("naucz")
        shows("nauczyciel")
        shows("nauczycielka")
        screen.onNodeWithText("posłuchać").assertDoesNotExist()
        back()

        scrollTo(text(R.string.lesson_flashcards))
        tap(R.string.lesson_flashcards)
        shows(text(R.string.flashcards_count, 1, 30))
        screen.onNode(hasText("posłuchać") and hasClickAction()).performClick()
        tap(R.string.flashcards_knew)
        shows(text(R.string.flashcards_count, 2, 30))
        back()

        scrollTo(text(R.string.lesson_test))
        tap(R.string.lesson_test)
        shows(R.string.checkpoint_label)
        playToTheEnd()
        shows(R.string.checkpoint_passed)
        screen.onNodeWithContentDescription(text(R.string.checkpoint_best_sentence, 100)).assertIsDisplayed()
        tap(R.string.result_done)

        scrollTo(text(R.string.lesson_test))
        shows(text(R.string.lesson_test_best, 100))
        // The Best is kept when the app is opened again.
        viewModel = fixtureless()
        tap(R.string.nav_path)
        screen.onNodeWithContentDescription(text(R.string.lesson_sealed)).performScrollTo().assertIsDisplayed()
    }

    /** Answers every Exercise showing rightly, until the Daily Set or the Test ends. */
    private fun playToTheEnd() {
        var guard = 0
        while (guard++ < 200) {
            when (val showing = viewModel?.screen) {
                is Screen.Tip -> screen.runOnUiThread { viewModel?.continueToNext() }
                is Screen.Player -> screen.runOnUiThread {
                    viewModel?.answer(rightAnswer(course, showing.exerciseId, showing.exercise))
                    viewModel?.continueToNext()
                }
                else -> break
            }
            screen.waitForIdle()
        }
        screen.mainClock.advanceTimeBy(3_000)
        assertEquals(true, viewModel?.screen is Screen.Summary, "The Daily Set or Test should have ended")
    }

    private fun openApp() {
        viewModel = fixtureless()
        screen.setContent {
            PolishEasyTheme {
                viewModel?.let { key(it) { PolishEasyApp(it, clips) } }
            }
        }
    }

    /** The real course over progress and Bests kept in files, as the app keeps them. */
    private fun fixtureless(): CourseViewModel {
        val engine = CourseEngine(
            course,
            FileLearnerProgress(File(folder.root, "progress.json")),
            clock,
            oneADay = { Settings(app).oneADay },
            checkpoints = FileCheckpointBests(File(folder.root, "checkpoints.json")),
        )
        return CourseViewModel(app, engine, engine.practice(InMemoryPracticeProgress(), Random(7)))
    }

    private fun lesson(title: String) {
        screen.onNode(hasText(title) and hasClickAction()).performScrollTo().performClick()
        screen.waitForIdle()
    }

    private fun back() {
        screen.runOnUiThread { screen.activity.onBackPressedDispatcher.onBackPressed() }
        screen.waitForIdle()
    }

    private fun text(id: Int, vararg values: Any): String = app.getString(id, *values)

    private fun tap(id: Int) = tap(text(id))

    private fun tap(text: String) {
        screen.onNodeWithText(text).performClick()
    }

    private fun type(letters: String) = screen.onNode(hasSetTextAction()).performTextInput(letters)

    private fun scrollTo(text: String) {
        screen.onNodeWithText(text).performScrollTo()
    }

    private fun shows(id: Int) = shows(text(id))

    private fun shows(text: String) {
        screen.onNodeWithText(text).assertIsDisplayed()
    }
}
