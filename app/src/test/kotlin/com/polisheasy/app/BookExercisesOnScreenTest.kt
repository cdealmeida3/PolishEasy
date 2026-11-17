package com.polisheasy.app

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.polisheasy.app.RecordingClips.Played
import com.polisheasy.app.ui.PolishEasyApp
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.engine.Course
import com.polisheasy.engine.CourseEngine
import com.polisheasy.engine.FileLearnerProgress
import com.polisheasy.engine.InMemoryCheckpointBests
import com.polisheasy.engine.InMemoryPracticeProgress
import com.polisheasy.engine.loadCourse
import java.io.File
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
 * The book's own Exercises on screen, over the real chapter 0: its section Ćwiczenia played by
 * tapping, with pairs matched, gaps filled from notes and typed, a dialogue put in order and a
 * heard text marked true or false; and the Grammar shelf with the chapter's table.
 */
@RunWith(RobolectricTestRunner::class)
class BookExercisesOnScreenTest {
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
    fun `the section Ćwiczenia is played by matching pairs, filling gaps, ordering a dialogue and marking a heard text`() {
        openApp()
        screen.runOnUiThread {
            viewModel?.openLesson("kpk1-l00")
            viewModel?.openSection("kpk1-l00-e")
        }
        screen.waitForIdle()
        shows("Exercises as the book sets them")
        tap(R.string.action_continue)

        // Match the pairs: a wrong meaning is shaken off and nothing is joined; the right ones are.
        shows("Proszę połączyć.")
        tap("posłuchać")
        tap("repeat")
        screen.onNodeWithContentDescription("posłuchać, ${text(R.string.match_pairs_joined)}", substring = true).assertDoesNotExist()
        listOf("posłuchać" to "listen", "powtórzyć" to "repeat", "przeczytać" to "read", "napisać" to "write", "uzupełnić" to "fill in").forEach { (word, meaning) ->
            tap(word)
            tap(meaning)
        }
        // One wrong line: it comes back at the end.
        shows(R.string.feedback_wrong)
        tap(R.string.action_continue)

        // Fill the gaps from the sticky notes.
        shows("Proszę uzupełnić.")
        screen.onNodeWithText(text(R.string.action_check)).assertIsNotEnabled()
        tapNote("posłuchać")
        tapNote("powtórzyć")
        tap(R.string.action_check)
        correctThenContinue()
        assertTrue(Played("audio/prosze-posluchac-i-powtorzyc-2.ogg", slow = false) in clips.played, "The whole text is heard: ${clips.played}")

        // Six pairs, all right the first time.
        listOf("lekcja" to "a lesson", "dialog" to "a dialogue", "tekst" to "a text", "ćwiczenie" to "an exercise", "strona" to "a page", "polecenie" to "an instruction").forEach { (word, meaning) ->
            tap(word)
            tap(meaning)
        }
        correctThenContinue()

        // Put the dialogue in order; the finished dialogue plays.
        shows("Proszę ułożyć dialog.")
        clips.asked.clear()
        listOf("Dzień dobry!", "Dzień dobry! Proszę imię i nazwisko.", "Ewa Nowak.", "Dziękuję.").forEach(::tap)
        tap(R.string.action_check)
        assertEquals(4, clips.played.size, "The dialogue plays line by line: ${clips.played}")

        // A text heard, a Clip a line, as it opens; then each statement marked P or N.
        clips.asked.clear()
        correctThenContinue()
        shows("Proszę posłuchać. Prawda czy nieprawda?")
        assertEquals(3, clips.played.size)
        listOf(true, false, true, true).forEachIndexed { index, value ->
            val label = if (value) R.string.true_false_true else R.string.true_false_false
            screen.onNodeWithContentDescription(text(label, index + 1)).performScrollTo().performClick()
        }
        tap(R.string.action_check)
        correctThenContinue()

        // A gap typed instead of filled from a note.
        shows("Proszę uzupełnić.")
        tap(R.string.fill_gaps_type)
        screen.onNode(hasSetTextAction()).performTextInput("nauczyciel")
        tap(R.string.fill_gaps_put)
        tapNote("nauczycielka")
        tap(R.string.action_check)
        correctThenContinue()
    }

    @Test
    fun `a gap gives its note back when tapped, and a wrong gap is marked`() {
        openApp()
        screen.runOnUiThread {
            viewModel?.openLesson("kpk1-l00")
            viewModel?.openSection("kpk1-l00-e")
        }
        screen.waitForIdle()
        tap(R.string.action_continue)
        // On to the first gaps, past the pairs.
        val pairs = assertIs<Screen.Player>(viewModel?.screen)
        screen.runOnUiThread { viewModel?.answer(rightAnswer(course, pairs.exerciseId, pairs.exercise)) }
        tap(R.string.action_continue)

        tapNote("napisać")
        screen.onNodeWithContentDescription(text(R.string.fill_gaps_gap_filled, 1, "napisać")).performClick()
        screen.onNodeWithContentDescription(text(R.string.fill_gaps_gap, 1)).assertIsDisplayed()
        tapNote("napisać")
        tapNote("powtórzyć")
        tap(R.string.action_check)
        shows(R.string.feedback_wrong)
        assertEquals(listOf(false, true), (viewModel?.screen as Screen.Player).feedback?.parts)
    }

    @Test
    fun `the Grammar shelf keeps the chapter's table once its section is done, from the Lesson screen and from Settings`() {
        openApp()
        tap(R.string.nav_path)
        screen.onNode(hasText("Dzień dobry!") and hasClickAction()).performScrollTo().performClick()
        screen.waitForIdle()
        screen.onNodeWithText(text(R.string.lesson_grammar_note, 0, 1)).performScrollTo().performClick()
        screen.waitForIdle()
        shows(R.string.grammar_none)
        back()

        // W szkole played to its end: its table is on the shelf.
        screen.runOnUiThread {
            viewModel?.openLesson("kpk1-l00")
            viewModel?.openSection("kpk1-l00-c")
        }
        playToTheEnd()
        screen.runOnUiThread { viewModel?.openLesson("kpk1-l00") }
        screen.waitForIdle()
        screen.onNodeWithText(text(R.string.lesson_grammar_note, 1, 1)).performScrollTo().performClick()
        screen.waitForIdle()
        shows("A man and a woman")
        screen.onNodeWithText("Men and women").assertIsDisplayed()
        screen.onNodeWithContentDescription(text(R.string.gender_feminine)).performScrollTo().assertIsDisplayed()
        back()
        shows("Dzień dobry!")

        screen.runOnUiThread { viewModel?.openSettings() }
        screen.waitForIdle()
        screen.onNodeWithText(text(R.string.grammar_title)).performScrollTo().performClick()
        screen.waitForIdle()
        screen.onNodeWithText(text(R.string.grammar_chapter, 0, "Dzień dobry!")).performScrollTo().assertIsDisplayed()
        back()
        screen.onNodeWithText(text(R.string.settings_class_audio)).performScrollTo().assertIsDisplayed()
    }

    /** Answers everything right until the Daily Set's Summary. */
    private fun playToTheEnd() {
        repeat(200) {
            when (val showing = viewModel?.screen) {
                is Screen.Tip -> screen.runOnUiThread { viewModel?.continueToNext() }
                is Screen.Player -> screen.runOnUiThread {
                    if (showing.feedback == null) viewModel?.answer(rightAnswer(course, showing.exerciseId, showing.exercise)) else viewModel?.continueToNext()
                }
                else -> return
            }
            screen.waitForIdle()
        }
    }

    private fun openApp() {
        val engine = CourseEngine(course, FileLearnerProgress(File(folder.root, "progress.json")), clock, oneADay = { false }, checkpoints = InMemoryCheckpointBests())
        viewModel = CourseViewModel(app, engine, engine.practice(InMemoryPracticeProgress(), Random(7)))
        screen.setContent {
            PolishEasyTheme {
                viewModel?.let { key(it) { PolishEasyApp(it, clips, RecordingRecordings()) } }
            }
        }
    }

    private fun correctThenContinue() {
        val feedback = (viewModel?.screen as? Screen.Player)?.feedback
        assertEquals(true, feedback?.correct, "$feedback")
        tap(R.string.action_continue)
    }

    private fun tapNote(word: String) {
        screen.onNode(hasText(word) and hasClickAction()).performScrollTo().performClick()
        screen.waitForIdle()
    }

    private fun back() {
        screen.runOnUiThread { screen.activity.onBackPressedDispatcher.onBackPressed() }
        screen.waitForIdle()
    }

    private fun text(id: Int, vararg values: Any): String = app.getString(id, *values)

    private fun tap(id: Int) = tap(text(id))

    private fun tap(text: String) {
        val node = screen.onNode(hasText(text) and hasClickAction())
        runCatching { node.performScrollTo() }
        node.performClick()
        screen.waitForIdle()
    }

    private fun shows(id: Int) = shows(text(id))

    private fun shows(text: String) {
        val node = screen.onNodeWithText(text)
        runCatching { node.performScrollTo() }
        node.assertIsDisplayed()
    }
}
