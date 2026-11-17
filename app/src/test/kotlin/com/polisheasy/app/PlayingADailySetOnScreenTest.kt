package com.polisheasy.app

import kotlin.test.assertTrue
import androidx.compose.ui.test.performScrollTo
import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.polisheasy.app.RecordingClips.Played
import com.polisheasy.app.RecordingClips.Stopped
import com.polisheasy.app.ui.PolishEasyApp
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.components.CONFETTI_TAG
import com.polisheasy.engine.CourseEngine
import com.polisheasy.engine.FileLearnerProgress
import java.io.File
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
            // The welcome is shown once; these tests start after it, except the one about it.
            Settings(app).welcomed = true
            // Most of these tests are about One a day's rules; the ones about the Learner's own pace turn it off.
            Settings(app).oneADay = true
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
        // A Polish prompt is said as it appears, and again from its speaker.
        val hello = Played("audio/dzien-dobry.ogg", slow = false)
        assertEquals(listOf(hello), clips.played)
        screen.onNodeWithContentDescription(text(R.string.audio_play)).performClick()
        assertEquals(listOf(hello, hello), clips.played)
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
        shows(text(R.string.conjugation_chip, "być", text(R.string.person_i), "ja"))
        type("jestem")
        tap(R.string.action_check)
        correctThenContinue()

        shows(R.string.summary_title)
        showsFirstTry(6, 6)
        shows(R.string.summary_items_learned)
        showsStreakRaised(to = 1)
        screen.onNodeWithText("dziękuję").performScrollTo().assertIsDisplayed()
        screen.onNodeWithText("thank you").performScrollTo().assertIsDisplayed()
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
        shows(R.string.feedback_wrong)
        shows(text(R.string.feedback_correct_answer, "hello (polite); good morning"))
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
        showsFirstTry(5, 6)
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

        shows(R.string.feedback_accepted)
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
        // The answer to a Polish Exercise is said as its feedback appears.
        assertEquals(Played("audio/to-jest-adam.ogg", slow = false), clips.played.last())
        typeThankYou()
        clips.asked.clear()

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
    fun `the close button leaves a Daily Set, which Home then offers to continue`() {
        openApp()
        tap(R.string.home_start)
        tap(R.string.action_continue)
        chooseHello()

        screen.onNodeWithContentDescription(text(R.string.daily_set_close)).performClick()

        shows(text(R.string.home_in_progress, 1, 6))
        tap(R.string.home_continue)
        showsProgress(1)
        shows(R.string.word_tiles_instruction)
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
        tap(R.string.nav_settings)
        val sound = screen.onNodeWithText(text(R.string.settings_sound))
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
        showsCorrect()
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

        tap(R.string.nav_path)
        // Daily Set 1 is named by its first Tip; Daily Set 2 has no Tip, so its number stands in.
        shows(text(R.string.path_progress, 1, 2))
        shows(text(R.string.path_unit_progress, 1, 2))
        val completed = text(R.string.path_stop_note, text(R.string.path_stop_number, 1, 2), text(R.string.path_completed))
        val dailySetOne = screen.onNode(hasText("The verb być") and hasText(completed))
        dailySetOne.assertIsDisplayed()
        val dailySetTwo = hasText(text(R.string.path_daily_set, 2))
        screen.onNode(dailySetTwo and hasText(text(R.string.home_up_next)) and hasText(text(R.string.path_unlocks_tomorrow)))
            .assertIsDisplayed()
            .assert(!hasClickAction())

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
        noStreakRaised()
        screen.onNodeWithTag(CONFETTI_TAG).assertDoesNotExist()
        tap(R.string.summary_back_path)
        shows(R.string.path_unlocks_tomorrow)

        clock.moveToNextDay()
        back()
        shows(R.string.home_ready)
        tap(R.string.nav_path)
        // Today's stop opens today's Daily Set, as Start on Home does.
        screen.onNode(dailySetTwo and hasText(text(R.string.path_today))).assertIsDisplayed().performClick()
        shows(R.string.choose_translation_instruction)
        shows("tak")
    }

    @Test
    fun `a new Learner is welcomed once and goes straight into the first Daily Set`() {
        Settings(app).welcomed = false
        openApp()
        shows(R.string.welcome_title)
        tap(R.string.welcome_start)
        shows(R.string.tip_label)

        // Left part of the way, the app opens on Home from then on.
        back()
        shows(R.string.home_streak_start_title)
        closeAndOpenApp()
        shows(R.string.home_streak_start_title)
        absent(R.string.welcome_title)
    }

    @Test
    fun `Home counts nothing on day one, and what was built up once a Daily Set is completed`() {
        openApp()
        shows(R.string.home_streak_start_title)
        absent(R.string.home_so_far)
        absent(R.string.streak_title)

        tap(R.string.home_start)
        playDailySetOne()
        tap(R.string.summary_back_home)

        showsStreakOnHome(1)
        shows(R.string.home_so_far)
        screen.onNodeWithContentDescription(app.resources.getQuantityString(R.plurals.home_so_far_sets, 1, 1)).assertExists()
        screen.onNodeWithContentDescription(app.resources.getQuantityString(R.plurals.home_so_far_streak, 1, 1)).assertExists()
    }

    @Test
    fun `the first Summary offers the Reminder once, and taking it turns it on`() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        openApp()
        tap(R.string.home_start)
        playDailySetOne()

        screen.onNodeWithText(text(R.string.summary_reminder_action)).performScrollTo().performClick()
        absent(R.string.summary_reminder_action)
        assertTrue(Settings(app).reminderEnabled)
        // It is offered once: no later Summary asks again.
        assertTrue(Settings(app).reminderOffered)
    }

    @Test
    fun `today's Daily Set can be replayed from Home, which the Replay comes back to`() {
        openApp()
        tap(R.string.home_start)
        playDailySetOne()
        tap(R.string.summary_back_home)

        tap(R.string.home_replay_today)
        shows(R.string.tip_label)
        playDailySetOne()
        shows(R.string.summary_replay_title)
        noStreakRaised()
        tap(R.string.summary_back_path)
        shows(R.string.home_done_title)
        showsStreakOnHome(1)
    }

    @Test
    fun `the Summary celebrates with confetti that falls once and is gone`() {
        openApp()
        tap(R.string.home_start)
        playDailySetOneAndStopTheClockAtTheSummary()
        screen.mainClock.advanceTimeBy(1_500)
        screen.onNodeWithTag(CONFETTI_TAG).assertExists()

        screen.mainClock.autoAdvance = true
        shows(R.string.summary_title)
        screen.onNodeWithTag(CONFETTI_TAG).assertDoesNotExist()
        showsStreakRaised(to = 1)
    }

    @Test
    fun `with animations removed the Summary shows its final state at once and no confetti`() {
        Settings.Global.putFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        openApp()
        tap(R.string.home_start)
        playDailySetOneAndStopTheClockAtTheSummary()
        screen.mainClock.advanceTimeBy(100)

        shows(R.string.summary_title)
        showsFirstTry(6, 6)
        showsStreakRaised(to = 1)
        screen.onNodeWithTag(CONFETTI_TAG).assertDoesNotExist()
    }

    @Test
    fun `One a day is a switch in Settings, and turning it on is saved`() {
        Settings(app).oneADay = false
        openApp()
        tap(R.string.nav_settings)
        val oneADay = screen.onNodeWithText(text(R.string.settings_one_a_day))
        oneADay.performScrollTo().assertIsOff()

        oneADay.performClick()

        oneADay.assertIsOn()
        assertTrue(Settings(app).oneADay)
    }

    @Test
    fun `at the Learner's own pace the next Daily Set can be started from Home straight after one is completed`() {
        Settings(app).oneADay = false
        openApp()
        tap(R.string.home_start)
        playDailySetOne()
        shows(R.string.summary_start_next)
        tap(R.string.summary_back_home)

        shows(R.string.home_keep_going_headline)
        shows(R.string.home_keep_going_title)
        // The Streak card is heard as one sentence, so its note is looked for inside it.
        screen.onNodeWithText(text(R.string.home_streak_note_safe), useUnmergedTree = true).assertExists()
        absent(R.string.home_done)
        tap(R.string.home_start)

        shows(R.string.choose_translation_instruction)
        shows("tak")
    }

    @Test
    fun `at the Learner's own pace the Summary starts the next Daily Set`() {
        Settings(app).oneADay = false
        openApp()
        tap(R.string.home_start)
        playDailySetOne()

        tap(R.string.summary_start_next)

        shows("tak")
    }

    @Test
    fun `at the Learner's own pace a Daily Set ahead is started from the Path, after asking when another is in progress`() {
        Settings(app).oneADay = false
        openApp()
        tap(R.string.home_start)
        tap(R.string.action_continue)
        chooseHello()
        back()
        shows(text(R.string.home_in_progress, 1, 6))

        tap(R.string.nav_path)
        absent(R.string.path_unlocks_tomorrow)
        screen.onNode(hasText(text(R.string.path_daily_set, 2)) and hasClickAction()).performClick()
        shows(R.string.path_replace_title)
        shows(text(R.string.path_replace_note, "The verb być"))
        screen.onNode(hasText(text(R.string.path_replace_start)) and hasAnyAncestor(isDialog())).performClick()

        shows("tak")
        back()
        // Daily Set 1's progress is gone: Home offers Daily Set 2, in progress.
        shows(text(R.string.home_in_progress, 0, 2))
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
        CourseEngine(fixtureCourse(), FileLearnerProgress(File(folder.root, "progress.json")), clock, oneADay = { Settings(app).oneADay }),
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

    /** The clock stops as the Summary is asked for, so a test can look at it part of the way through. */
    private fun playDailySetOneAndStopTheClockAtTheSummary() {
        tap(R.string.action_continue)
        chooseHello()
        buildThisIsAdam()
        typeThankYou()
        chooseGoodbye()
        typeNo()
        type("jestem")
        tap(R.string.action_check)
        showsCorrect()
        screen.mainClock.autoAdvance = false
        tap(R.string.action_continue)
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

    /** A right answer is praised in Polish; a screen reader is told "Correct". */
    private fun showsCorrect() = screen.onNodeWithContentDescription(text(R.string.feedback_correct)).assertIsDisplayed()

    private fun correctThenContinue() {
        showsCorrect()
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

    private fun showsStreakOnHome(streak: Int) {
        val words = app.resources.getQuantityString(R.plurals.home_streak, streak, streak)
        screen.onNodeWithContentDescription(words, substring = true).assertIsDisplayed()
    }

    private fun showsStreakRaised(to: Int) {
        screen.onNodeWithContentDescription(app.resources.getQuantityString(R.plurals.home_streak, to, to)).assertIsDisplayed()
    }

    /** The score is a small fact on the Summary, said to a screen reader as a sentence. */
    private fun showsFirstTry(correct: Int, of: Int) {
        screen.onNodeWithContentDescription(text(R.string.summary_accuracy, correct, of)).assertIsDisplayed()
    }

    private fun noStreakRaised() = screen.onNodeWithText(text(R.string.streak_title)).assertDoesNotExist()

    private fun noSpeaker() = screen.onNodeWithContentDescription(text(R.string.audio_play)).assertDoesNotExist()

    /** The bar counts steps: the Tip that opens Daily Set 1, which has been read by now, and its six Exercises. */
    private fun showsProgress(correct: Int) {
        screen.onNodeWithContentDescription(text(R.string.progress_description, correct + 1, 7)).assertIsDisplayed()
    }

    /** What Home says once today's Daily Set is completed. */
    private fun doneForToday(): String = text(R.string.home_done)
}
