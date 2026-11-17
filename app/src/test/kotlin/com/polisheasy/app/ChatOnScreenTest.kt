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
import com.polisheasy.app.RecordingClips.Played
import com.polisheasy.app.ui.PolishEasyApp
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.engine.ChatRound
import com.polisheasy.engine.ChatScript
import com.polisheasy.engine.Course
import com.polisheasy.engine.CourseEngine
import com.polisheasy.engine.InMemoryLearnerProgress
import com.polisheasy.engine.InMemoryPracticeProgress
import com.polisheasy.engine.loadCourse
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * The Chat Game and *Powtórz na głos* on the real screens, over the real course, as a Learner
 * who has done every Daily Set plays them. The recognizer is a stand-in the test speaks into.
 */
@RunWith(RobolectricTestRunner::class)
class ChatOnScreenTest {
    @get:Rule(order = 0)
    val hostActivity = object : ExternalResource() {
        override fun before() {
            shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
            Settings(app).welcomed = true
        }
    }

    @get:Rule(order = 1)
    val screen = createAndroidComposeRule<ComponentActivity>()

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val clock = SettableClock()
    private val clips = RecordingClips()
    private val speech = FakeSpeech()
    private lateinit var course: Course
    private var viewModel: CourseViewModel? by mutableStateOf(null)

    @Test
    fun `a chat is played by choosing, a wrong reply is put right gently, and the result becomes the Best`() {
        openChat("At the school office", R.string.option_choose)

        // The person is typing, then says the first line, and it is heard.
        shows(text(R.string.chat_typing, "Pani Nowak"), byDescription = true)
        settle()
        shows("Dzień dobry! Słucham.")
        assertTrue(Played(clipOf("Dzień dobry! Słucham."), slow = false) in clips.played)
        shows(R.string.chat_choose_instruction)

        // The wrong register: the right reply is shown, and why, and the chat waits.
        reply("Cześć!")
        shows(text(R.string.feedback_correct_answer, "Dzień dobry!"))
        screen.onNode(hasText("is for friends and family", substring = true)).assertIsDisplayed()
        tap(R.string.action_continue)
        settle()

        // The right reply joins the chat in place of the wrong one, and the person goes on.
        shows("Jak pan ma na imię?")
        while (viewModel?.screen is Screen.Round && chat().replying) {
            reply(rightReply())
            tap(R.string.action_continue)
            settle()
        }
        shows("Miłego dnia!")
        // The last line stands a moment before the result.
        screen.mainClock.advanceTimeBy(Motion.CHAT_DONE_MILLIS + 100)
        screen.waitForIdle()
        shows(R.string.result_title)
        shows(text(R.string.summary_accuracy, 4, 5), byDescription = true)
        tap(R.string.result_done)
        // The Chat's card is below the Words Games, further down than the screen is high.
        screen.onNodeWithText(text(R.string.practice_best_played, right(4), "At the school office, ${text(R.string.option_choose)}"))
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `a chat is played by typing, with any accepted answer, and a typed answer in the wrong register gets its note`() {
        openChat("A new classmate", R.string.option_type)
        settle()
        shows("Cześć! Jestem Ewa. A ty?")

        type("Dzień dobry! Nazywam się Adam.")
        tap(R.string.action_check)
        shows(text(R.string.feedback_correct_answer, "Cześć! Jestem Adam."))
        screen.onNode(hasText("Ewa is a classmate", substring = true)).assertIsDisplayed()
        tap(R.string.action_continue)
        settle()

        // Another of the accepted answers, without its accents: right, with a word on the accents.
        shows("Miło mi! Jesteś z Polski?")
        type("Nie, z Anglii.")
        tap(R.string.action_check)
        tap(R.string.action_continue)
        settle()
        shows("Jestem z Polski. Mówisz po polsku?")
        type("Tak, troche.")
        tap(R.string.action_check)
        shows(text(R.string.feedback_accent_warning, "Tak, trochę."))
        tap(R.string.action_continue)
        settle()

        while (viewModel?.screen is Screen.Round && chat().replying) {
            type(rightReply())
            tap(R.string.action_check)
            tap(R.string.action_continue)
            settle()
        }
        screen.mainClock.advanceTimeBy(Motion.CHAT_DONE_MILLIS + 100)
        screen.waitForIdle()
        shows(text(R.string.summary_accuracy, 3, 4), byDescription = true)
    }

    @Test
    fun `a chat is played by saying it, leniently, and choosing instead is always there`() {
        openChat("At the school office", R.string.option_say)
        settle()
        shows(R.string.chat_say_instruction)

        // Nothing understood: nothing changes, and the Learner can try again.
        screen.onNodeWithContentDescription(text(R.string.speak_mic)).performClick()
        screen.runOnIdle { speech.hear() }
        shows(R.string.speak_heard_nothing)
        assertEquals(0, chat().done)

        // Said with a word a little off and no accents: understood.
        screen.onNodeWithContentDescription(text(R.string.speak_mic)).performClick()
        screen.runOnIdle { speech.hear("dzien dobri") }
        screen.waitForIdle()
        assertEquals(1, chat().score.value)
        tap(R.string.action_continue)
        settle()

        // Choosing instead shows the replies for this turn only.
        tap(R.string.chat_choose_instead)
        shows(R.string.chat_choose_instruction)
        reply(rightReply())
        tap(R.string.action_continue)
        settle()
        shows(R.string.chat_say_instruction)
        assertEquals(2, speech.listened)
    }

    @Test
    fun `without a recognizer there is no saying it, and Chat is played by choosing or typing`() {
        speech.available = false
        openPractice()
        openGame(R.string.game_chat)
        shows(R.string.option_choose)
        shows(R.string.option_type)
        screen.onNodeWithText(text(R.string.option_say)).assertDoesNotExist()
    }

    // The way there

    private fun openPractice() {
        course = loadCourse { path -> app.assets.open(path).bufferedReader().use { it.readText() } }
        val progress = InMemoryLearnerProgress()
        course.path.forEachIndexed { index, dailySet ->
            progress.recordCompleted(dailySet.id, clock.today.minusDays((course.path.size - 1 - index).toLong()))
        }
        val engine = CourseEngine(course, progress, clock, oneADay = { true })
        viewModel = CourseViewModel(app, engine, engine.practice(InMemoryPracticeProgress(), Random(7)))
        screen.setContent {
            PolishEasyTheme {
                viewModel?.let { key(it) { PolishEasyApp(it, clips, speech = speech) } }
            }
        }
        tap(R.string.nav_practice)
    }

    private fun openChat(title: String, way: Int) {
        openPractice()
        openGame(R.string.game_chat)
        shows(R.string.choice_chat)
        tap(title)
        tap(way)
        clips.asked.clear()
        tap(R.string.setup_play)
        screen.waitForIdle()
    }

    private fun openGame(name: Int) {
        screen.onNodeWithText(text(name)).performScrollTo().performClick()
    }

    // What the Learner does and sees

    private fun chat(): ChatRound = (viewModel?.screen as Screen.Round).round as ChatRound

    private fun script(): ChatScript = course.chats.first { it.person == chat().person }

    /** The reply that leads on from the person's last line. */
    private fun rightReply(): String = script().turns.first { it.says == chat().lines.last().text }.replies.first { it.right }.text

    /** Lets the person's next line be typed and arrive. */
    private fun settle() {
        screen.mainClock.advanceTimeBy(Motion.TYPING_MILLIS + 100)
        screen.waitForIdle()
    }

    private fun reply(text: String) {
        screen.onNode(hasText(text) and hasClickAction()).performScrollTo().performClick()
        screen.waitForIdle()
    }

    private fun clipOf(polish: String): String = checkNotNull(viewModel?.practiceClipOf(polish)).path

    private fun type(letters: String) = screen.onNode(hasSetTextAction()).performTextInput(letters)

    private fun text(id: Int, vararg values: Any): String = app.getString(id, *values)

    private fun right(count: Int): String = app.resources.getQuantityString(R.plurals.practice_right, count, count)

    private fun tap(id: Int) = tap(text(id))

    private fun tap(text: String) {
        screen.onNodeWithText(text).performClick()
        screen.waitForIdle()
    }

    private fun shows(id: Int) = shows(text(id))

    private fun shows(text: String, byDescription: Boolean = false) {
        if (byDescription) screen.onNodeWithContentDescription(text).assertIsDisplayed() else screen.onNodeWithText(text).assertIsDisplayed()
    }
}
