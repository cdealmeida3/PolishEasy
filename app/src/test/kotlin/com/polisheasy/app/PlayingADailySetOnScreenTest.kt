package com.polisheasy.app

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.polisheasy.app.RecordingClips.Played
import com.polisheasy.app.RecordingClips.Stopped
import com.polisheasy.app.ui.PolishEasyApp
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.engine.CourseEngine
import com.polisheasy.engine.FileLearnerProgress
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * The real screens, the real view model and the real course engine, played the way a Learner
 * plays them. Only the course (see [fixtureCourse]), the clock and the Clip player are replaced.
 * Each scenario checks what the Learner sees; the rules behind it are tested in the engine.
 */
@RunWith(RobolectricTestRunner::class)
class PlayingADailySetOnScreenTest {
    @get:Rule(order = 0)
    val folder = TemporaryFolder()

    /**
     * The empty activity the app is put in. It is made known to the test's Android here, so that
     * no build of the app has to declare an activity only tests use.
     */
    @get:Rule(order = 1)
    val hostActivity = object : ExternalResource() {
        override fun before() {
            shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
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
    fun `a new Learner plays a Daily Set from Home to the Summary`() {
        openApp()
        shows(R.string.home_ready)
        tap(R.string.home_start)

        shows(R.string.tip_label)
        shows(text(R.string.conjugation_table_title, "być", "to be"))
        shows("jesteśmy")
        tap(R.string.action_continue)

        showsProgress(0)
        shows(R.string.choose_translation_instruction)
        screen.onNodeWithContentDescription(text(R.string.audio_play)).performClick()
        assertEquals(listOf(Played("audio/dzien-dobry.ogg", slow = false)), clips.played)
        tap("hello (polite); good morning")
        correctThenContinue()

        showsProgress(1)
        shows(R.string.word_tiles_instruction)
        listOf("to", "jest", "Adam").forEach(::tap)
        tap(R.string.action_check)
        correctThenContinue()

        showsProgress(2)
        shows(R.string.type_answer_instruction)
        type("dzi")
        tapPolishKey("ę")
        type("kuj")
        tapPolishKey("ę")
        tap(R.string.action_check)
        correctThenContinue()

        showsProgress(3)
        shows(R.string.listen_choose_instruction)
        tap("do widzenia")
        correctThenContinue()

        showsProgress(4)
        shows(R.string.listen_type_instruction)
        type("nie")
        tap(R.string.action_check)
        correctThenContinue()

        showsProgress(5)
        shows(R.string.conjugation_instruction)
        shows(text(R.string.conjugation_person, text(R.string.person_i), "ja"))
        type("jestem")
        tap(R.string.action_check)
        correctThenContinue()

        shows(R.string.summary_title)
        shows(text(R.string.summary_accuracy, 6, 6))
        shows(R.string.summary_items_learned)
        shows("dziękuję")
        shows("thank you")
        tap(R.string.summary_back_home)

        shows(doneForToday())
        absent(R.string.home_start)
    }

    @Test
    fun `a wrong answer shows the right one and the Exercise comes back at the end`() {
        openApp()
        tap(R.string.home_start)
        tap(R.string.action_continue)

        tap("good night")
        shows(text(R.string.feedback_wrong, "hello (polite); good morning"))
        tap(R.string.action_continue)
        buildThisIsAdam()
        typeThankYou()
        chooseGoodbye()
        typeNo()
        typeIAm()

        absent(R.string.summary_title)
        shows(R.string.choose_translation_instruction)
        chooseHello()

        shows(R.string.summary_title)
        shows(text(R.string.summary_accuracy, 5, 6))
    }

    @Test
    fun `an answer missing its accents is accepted with the proper spelling shown`() {
        openApp()
        tap(R.string.home_start)
        tap(R.string.action_continue)
        chooseHello()
        buildThisIsAdam()

        type("dziekuje")
        tap(R.string.action_check)

        shows(text(R.string.feedback_accent_warning, "dziękuję"))
        tap(R.string.action_continue)
        showsProgress(3)
    }

    @Test
    fun `a listening Exercise plays its Clip, again and slowly`() {
        openApp()
        tap(R.string.home_start)
        tap(R.string.action_continue)
        chooseHello()
        buildThisIsAdam()
        typeThankYou()

        shows(R.string.listen_choose_instruction)
        val clip = "audio/do-widzenia.ogg"
        assertEquals(listOf(Played(clip, slow = false)), clips.played)

        screen.onNodeWithContentDescription(text(R.string.audio_play_again)).performClick()
        tap(R.string.audio_slow)
        assertEquals(
            listOf(Played(clip, slow = false), Played(clip, slow = false), Played(clip, slow = true)),
            clips.played,
        )

        // Moving on stops it, and the next listening Exercise plays its own.
        clips.asked.clear()
        chooseGoodbye()
        shows(R.string.listen_type_instruction)
        assertEquals(listOf(Stopped, Played("audio/nie.ogg", slow = false)), clips.asked)
    }

    @Test
    fun `can't listen now turns the listening Exercises into reading ones and nothing is played after it`() {
        openApp()
        tap(R.string.home_start)
        tap(R.string.action_continue)
        chooseHello()
        buildThisIsAdam()
        typeThankYou()

        tap(R.string.cant_listen_now)
        clips.asked.clear()

        shows(R.string.choose_polish_instruction)
        shows("goodbye")
        absent(R.string.cant_listen_now)
        chooseGoodbye()

        shows(R.string.type_answer_instruction)
        shows("no")
        typeNo()
        typeIAm()

        shows(R.string.summary_title)
        assertEquals(emptyList(), clips.played)
    }

    @Test
    fun `with Sound off in Settings a Daily Set shows no speaker and plays nothing`() {
        openApp()
        tap(R.string.home_settings)
        val sound = screen.onNodeWithContentDescription(text(R.string.settings_sound))
        sound.performClick()
        sound.assertIsOff()
        back()

        tap(R.string.home_start)
        tap(R.string.action_continue)
        shows(R.string.choose_translation_instruction)
        noSpeaker()
        chooseHello()
        buildThisIsAdam()

        type("dziękuję")
        tap(R.string.action_check)
        shows(R.string.feedback_correct)
        noSpeaker()
        tap(R.string.action_continue)

        shows(R.string.choose_polish_instruction)
        chooseGoodbye()
        shows(R.string.type_answer_instruction)
        typeNo()
        typeIAm()

        shows(R.string.summary_title)
        assertEquals(emptyList(), clips.played)
    }

    @Test
    fun `leaving mid-set and coming back continues at the same Exercise`() {
        openApp()
        tap(R.string.home_start)
        tap(R.string.action_continue)
        chooseHello()
        buildThisIsAdam()
        shows(R.string.type_answer_instruction)

        back()
        shows(text(R.string.home_in_progress, 2, 6))

        closeAndOpenApp()
        shows(text(R.string.home_in_progress, 2, 6))
        tap(R.string.home_continue)

        shows(R.string.type_answer_instruction)
        shows("thank you")
        showsProgress(2)
    }

    @Test
    fun `the Path shows what is completed, a Replay is played, and the next Daily Set opens the next day`() {
        openApp()
        tap(R.string.home_start)
        playDailySetOne()
        tap(R.string.summary_back_home)

        tap(R.string.home_path)
        val dailySetOne = screen.onNode(hasText(text(R.string.path_daily_set, 1)) and hasText(text(R.string.path_completed)))
        dailySetOne.assertIsDisplayed()
        shows(text(R.string.path_daily_set, 2))
        shows(R.string.path_locked)

        dailySetOne.performClick()
        shows(R.string.tip_label)
        tap(R.string.action_continue)
        shows(R.string.replay_label)
        chooseHello()
        buildThisIsAdam()
        typeThankYou()
        chooseGoodbye()
        typeNo()
        typeIAm()
        shows(R.string.summary_replay_title)
        tap(R.string.summary_back_path)
        shows(R.string.path_locked)

        clock.moveToNextDay()
        back()
        shows(R.string.home_ready)
        tap(R.string.home_path)
        screen.onNode(hasText(text(R.string.path_daily_set, 2)) and hasText(text(R.string.path_today))).assertIsDisplayed()
        back()

        tap(R.string.home_start)
        shows(R.string.choose_translation_instruction)
        shows("tak")
    }

    @Test
    fun `the packaged course opens`() {
        // The real course from the app's assets. Nothing here depends on what it teaches.
        openApp(CourseViewModel(app))
        shows(R.string.home_ready)
        tap(R.string.home_start)

        val steps = listOf(
            R.string.tip_label,
            R.string.choose_translation_instruction,
            R.string.word_tiles_instruction,
            R.string.type_answer_instruction,
            R.string.listen_choose_instruction,
            R.string.listen_type_instruction,
            R.string.conjugation_instruction,
        )
        val showing = steps.sumOf { screen.onAllNodes(hasText(text(it))).fetchSemanticsNodes().size }
        assertEquals(1, showing, "A Tip or an Exercise of the first Daily Set should be showing")
    }

    // The app

    private fun openApp(over: CourseViewModel = fixtureViewModel()) {
        viewModel = over
        screen.setContent {
            PolishEasyTheme {
                viewModel?.let { key(it) { PolishEasyApp(it, clips) } }
            }
        }
    }

    /** As if the app had been closed: a new view model and engine, with only the progress file kept. */
    private fun closeAndOpenApp() {
        viewModel = fixtureViewModel()
    }

    private fun fixtureViewModel() = CourseViewModel(
        app,
        CourseEngine(fixtureCourse(), FileLearnerProgress(File(folder.root, "progress.json")), clock),
    )

    private fun back() = screen.runOnUiThread { screen.activity.onBackPressedDispatcher.onBackPressed() }

    // Playing Daily Set 1 of the fixture course, each Exercise answered correctly

    private fun playDailySetOne() {
        tap(R.string.action_continue)
        chooseHello()
        buildThisIsAdam()
        typeThankYou()
        chooseGoodbye()
        typeNo()
        typeIAm()
    }

    private fun chooseHello() {
        tap("hello (polite); good morning")
        correctThenContinue()
    }

    private fun buildThisIsAdam() {
        listOf("to", "jest", "Adam").forEach(::tap)
        tap(R.string.action_check)
        correctThenContinue()
    }

    private fun typeThankYou() = typeAndCheck("dziękuję")

    /** Listen and choose, or its reading form: the options are the same. */
    private fun chooseGoodbye() {
        tap("do widzenia")
        correctThenContinue()
    }

    /** Listen and type, or its reading form: the answer is the same. */
    private fun typeNo() = typeAndCheck("nie")

    private fun typeIAm() = typeAndCheck("jestem")

    private fun typeAndCheck(answer: String) {
        type(answer)
        tap(R.string.action_check)
        correctThenContinue()
    }

    private fun correctThenContinue() {
        shows(R.string.feedback_correct)
        tap(R.string.action_continue)
    }

    // What the Learner does and sees

    private fun text(id: Int, vararg values: Any): String = app.getString(id, *values)

    private fun tap(id: Int) = tap(text(id))

    private fun tap(text: String) {
        screen.onNodeWithText(text).performClick()
    }

    private fun type(letters: String) = screen.onNode(hasSetTextAction()).performTextInput(letters)

    private fun tapPolishKey(letter: String) {
        screen.onNodeWithContentDescription(text(R.string.polish_key_description, letter)).performClick()
    }

    private fun shows(id: Int) = shows(text(id))

    private fun shows(text: String) {
        screen.onNodeWithText(text).assertIsDisplayed()
    }

    private fun absent(id: Int) = screen.onNodeWithText(text(id)).assertDoesNotExist()

    private fun noSpeaker() = screen.onNodeWithContentDescription(text(R.string.audio_play)).assertDoesNotExist()

    private fun showsProgress(correct: Int) {
        screen.onNodeWithContentDescription(text(R.string.progress_description, correct, 6)).assertIsDisplayed()
    }

    /** What Home says once today's Daily Set is completed: the next one unlocks at the start of tomorrow. */
    private fun doneForToday(): String {
        val unlock = clock.today.plusDays(1).atStartOfDay(clock.zone)
        return text(R.string.home_done, DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(unlock))
    }
}
