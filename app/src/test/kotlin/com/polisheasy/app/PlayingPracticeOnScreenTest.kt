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
import androidx.compose.ui.test.performTextInput
import com.polisheasy.app.RecordingClips.Played
import com.polisheasy.app.ui.PolishEasyApp
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.engine.CourseEngine
import com.polisheasy.engine.FileLearnerProgress
import com.polisheasy.engine.FilePracticeProgress
import com.polisheasy.engine.VerbRound
import com.polisheasy.engine.VerbStage
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
 * Practice on the real screens, over the real view model and course engine, played the way a
 * Learner plays it. The course is the fixture course, whose first Daily Set teaches the table of
 * "być". Each scenario checks what the Learner sees; the rules of the Games are tested in the engine.
 */
@RunWith(RobolectricTestRunner::class)
class PlayingPracticeOnScreenTest {
    @get:Rule(order = 0)
    val folder = TemporaryFolder()

    @get:Rule(order = 1)
    val hostActivity = object : ExternalResource() {
        override fun before() {
            shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
            Settings(app).welcomed = true
        }
    }

    @get:Rule(order = 2)
    val screen = createAndroidComposeRule<ComponentActivity>()

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val clock = SettableClock()
    private val clips = RecordingClips()

    /** The view model on screen. Replacing it is the app being closed and opened again. */
    private var viewModel: CourseViewModel? by mutableStateOf(null)

    @Test
    fun `Practice is locked until today's Daily Set is completed, and starts it`() {
        openApp()
        tap(R.string.nav_practice)
        shows(R.string.practice_locked_title)
        shows(R.string.game_verb_challenge)
        // A locked Game does not open.
        tap(R.string.game_verb_challenge)
        shows(R.string.practice_locked_title)

        // The card that says what opens Practice starts the Daily Set.
        tap(R.string.home_start)
        shows(R.string.tip_label)
        playDailySetOne()
        tap(R.string.summary_back_home)

        tap(R.string.nav_practice)
        absent(R.string.practice_locked_title)
        shows(R.string.practice_open_note)
        tap(R.string.game_verb_challenge)
        shows(R.string.setup_play)
    }

    @Test
    fun `a Round of the Verb challenge is played from its setup to its result, which becomes the Best`() {
        openPracticeAfterTodaysDailySet()
        tap(R.string.game_verb_challenge)
        shows(R.string.setup_no_best)
        shows("być")
        tap(R.string.setup_play)

        // Pick the form: a wrong one waits with the right one marked, a right one can be moved on from at once.
        shows(R.string.verb_pick_instruction)
        assertEquals(VerbStage.PICK, round().question?.stage)
        answerWrong()
        repeat(5) { answerRight() }

        // Fill the table: each tile goes into the row being asked for.
        shows(R.string.verb_table_instruction)
        repeat(6) { answerRight() }

        shows(R.string.verb_sentence_instruction)
        answerRight()

        shows(R.string.result_title)
        showsScore(12, 13)
        showsFirstBest()
        tap(R.string.result_done)

        // Practice shows the Best with the way it was played, and that a Round was played today.
        shows(app.resources.getQuantityString(R.plurals.practice_rounds_today, 1, 1))
        shows(text(R.string.practice_best_played, right(12), "być, ${text(R.string.option_choose)}"))
    }

    @Test
    fun `a better Round is a new Best, and a worse one leaves the Best as it was`() {
        openPracticeAfterTodaysDailySet()
        tap(R.string.game_verb_challenge)
        tap(R.string.setup_play)
        answerWrong()
        playOut()
        showsScore(12, 13)

        tap(R.string.result_play_again)
        playOut()
        showsScore(13, 13)
        shows(R.string.result_new_best)

        tap(R.string.result_play_again)
        answerWrong()
        answerWrong()
        playOut()
        showsScore(11, 13)
        absent(R.string.result_new_best)
        screen.onNodeWithContentDescription(text(R.string.result_best_sentence, right(13))).assertIsDisplayed()
    }

    @Test
    fun `each way of playing has its own Best, and typing takes the form typed`() {
        openPracticeAfterTodaysDailySet()
        tap(R.string.game_verb_challenge)
        tap(R.string.setup_play)
        playOut()
        tap(R.string.result_done)

        tap(R.string.game_verb_challenge)
        shows(text(R.string.practice_best, right(13)))
        tap(R.string.option_type)
        shows(R.string.setup_no_best)
        tap(R.string.setup_play)

        shows(R.string.verb_type_instruction)
        type("xyz")
        tap(R.string.action_check)
        shows(text(R.string.feedback_correct_answer, checkNotNull(round().question).answers.first()))
        tap(R.string.action_continue)
        playOut()
        showsScore(12, 13)
        showsFirstBest()
    }

    @Test
    fun `a right answer moves on by itself after a moment`() {
        openPracticeAfterTodaysDailySet()
        tap(R.string.game_verb_challenge)
        tap(R.string.setup_play)
        screen.waitForIdle()
        screen.mainClock.autoAdvance = false
        screen.onNode(hasText(rightForm()) and hasClickAction()).performClick()
        screen.mainClock.advanceTimeBy(Motion.MOVE_ON_MILLIS / 2)
        assertEquals(0, round().step)
        screen.mainClock.advanceTimeBy(Motion.MOVE_ON_MILLIS)
        screen.mainClock.autoAdvance = true
        screen.waitForIdle()
        assertEquals(1, round().step)
    }

    @Test
    fun `a Round left unfinished is not kept`() {
        openPracticeAfterTodaysDailySet()
        tap(R.string.game_verb_challenge)
        tap(R.string.setup_play)
        answerRight()
        screen.onNodeWithContentDescription(text(R.string.round_close)).performClick()

        shows(R.string.practice_open_note)
        screen.onNodeWithText(app.resources.getQuantityString(R.plurals.practice_rounds_today, 1, 1)).assertDoesNotExist()
        tap(R.string.game_verb_challenge)
        shows(R.string.setup_no_best)
        // Back from the setup is Practice again.
        back()
        shows(R.string.practice_open_note)
    }

    @Test
    fun `the Best is kept when the app is closed, and Practice locks again the next day`() {
        openPracticeAfterTodaysDailySet()
        tap(R.string.game_verb_challenge)
        tap(R.string.setup_play)
        playOut()
        tap(R.string.result_done)

        closeAndOpenApp()
        tap(R.string.nav_practice)
        shows(text(R.string.practice_best_played, right(13), "być, ${text(R.string.option_choose)}"))

        clock.moveToNextDay()
        closeAndOpenApp()
        tap(R.string.nav_practice)
        shows(R.string.practice_locked_title)
        screen.onNodeWithText(app.resources.getQuantityString(R.plurals.practice_rounds_today, 1, 1)).assertDoesNotExist()
    }

    @Test
    fun `a sentence is said whole as it is answered, and nothing is said with Sound off`() {
        openPracticeAfterTodaysDailySet()
        tap(R.string.game_verb_challenge)
        tap(R.string.setup_play)
        clips.asked.clear()
        playOut()
        assertEquals(listOf(Played("audio/jestem-adam.ogg", slow = false)), clips.played)
        tap(R.string.result_done)

        tap(R.string.nav_settings)
        tap(R.string.settings_sound)
        tap(R.string.nav_practice)
        tap(R.string.game_verb_challenge)
        tap(R.string.setup_play)
        clips.asked.clear()
        playOut()
        assertTrue(clips.played.isEmpty())
    }

    // Getting there

    private fun openApp() {
        viewModel = fixtureViewModel()
        screen.setContent {
            PolishEasyTheme {
                viewModel?.let { key(it) { PolishEasyApp(it, clips) } }
            }
        }
    }

    /** As if the app had been closed: a new view model and engine, with only the saved files kept. */
    private fun closeAndOpenApp() {
        viewModel = fixtureViewModel()
    }

    private fun fixtureViewModel(): CourseViewModel {
        val engine = CourseEngine(fixtureCourse(), FileLearnerProgress(File(folder.root, "progress.json")), clock)
        return CourseViewModel(app, engine, engine.practice(FilePracticeProgress(File(folder.root, "practice.json")), Random(7)))
    }

    private fun openPracticeAfterTodaysDailySet() {
        openApp()
        tap(R.string.home_start)
        playDailySetOne()
        tap(R.string.summary_back_home)
        tap(R.string.nav_practice)
    }

    /** Daily Set 1 of the fixture course, each Exercise answered correctly. */
    private fun playDailySetOne() {
        tap(R.string.action_continue)
        tap("hello (polite); good morning")
        tap(R.string.action_continue)
        listOf("to", "jest", "Adam").forEach(::tap)
        tap(R.string.action_check)
        tap(R.string.action_continue)
        listOf("dziękuję", null, "nie", "jestem").forEach { answer ->
            if (answer == null) tap("do widzenia") else {
                type(answer)
                tap(R.string.action_check)
            }
            tap(R.string.action_continue)
        }
    }

    // Playing a Round

    private fun round(): VerbRound = (viewModel?.screen as Screen.Round).round as VerbRound

    private fun rightForm(): String = checkNotNull(round().question).answers.first()

    private fun answerRight() = answer(rightForm())

    private fun answerWrong() = answer(checkNotNull(round().question).options.first { it != rightForm() })

    private fun answer(form: String) {
        if (round().typing) {
            type(form)
            tap(R.string.action_check)
        } else {
            screen.onNode(hasText(form) and hasClickAction()).performClick()
        }
        screen.waitForIdle()
        // A wrong answer waits for "Continue"; a right one can be moved on from without waiting.
        val playing = viewModel?.screen as? Screen.Round
        if ((playing?.round as? VerbRound)?.given != null) tap(R.string.action_continue)
        screen.waitForIdle()
    }

    /** Every question left, answered right. */
    private fun playOut() {
        while (viewModel?.screen is Screen.Round) answerRight()
    }

    // What the Learner does and sees

    private fun text(id: Int, vararg values: Any): String = app.getString(id, *values)

    private fun right(count: Int): String = app.resources.getQuantityString(R.plurals.practice_right, count, count)

    private fun tap(id: Int) = tap(text(id))

    private fun tap(text: String) {
        screen.onNodeWithText(text).performClick()
    }

    private fun type(letters: String) = screen.onNode(hasSetTextAction()).performTextInput(letters)

    private fun back() = screen.runOnUiThread { screen.activity.onBackPressedDispatcher.onBackPressed() }

    private fun shows(id: Int) = shows(text(id))

    private fun shows(text: String) {
        screen.onNodeWithText(text).assertIsDisplayed()
    }

    private fun absent(id: Int) = screen.onNodeWithText(text(id)).assertDoesNotExist()

    /** The words under the Best when it is the first: part of a fact that a screen reader hears as one sentence. */
    private fun showsFirstBest() {
        screen.onNodeWithText(text(R.string.result_first_best), useUnmergedTree = true).assertIsDisplayed()
    }

    /** The result is a small fact, said to a screen reader as a sentence. */
    private fun showsScore(correct: Int, of: Int) {
        screen.onNodeWithContentDescription(text(R.string.summary_accuracy, correct, of)).assertIsDisplayed()
    }
}
