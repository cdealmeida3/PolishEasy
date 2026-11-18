package com.polisheasy.app

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
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
import com.polisheasy.engine.ChooseTask
import com.polisheasy.engine.Course
import com.polisheasy.engine.CourseEngine
import com.polisheasy.engine.DictationTask
import com.polisheasy.engine.FileLearnerProgress
import com.polisheasy.engine.InMemoryCheckpointBests
import com.polisheasy.engine.InMemoryPracticeProgress
import com.polisheasy.engine.TrueFalseTask
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
 * The class recordings on screen, over the real course: a chapter's Słuchaj before and after the
 * class's zips are added in Class audio, the player, and listening tasks. The zips are stood in for
 * by tiny MP3s with the publisher's tags, and the player by one that only keeps what it was asked.
 */
@RunWith(RobolectricTestRunner::class)
class ClassAudioOnScreenTest {
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
    private val recordings = RecordingRecordings()
    private val realCourse: Course = loadCourse { path -> app.assets.open(path).bufferedReader().use { it.readText() } }
    private var viewModel: CourseViewModel? by mutableStateOf(null)

    @Test
    fun `every chapter's Słuchaj lists its recordings by section, and says how to add them while none are on the phone`() {
        openApp()
        tap(R.string.nav_path)
        lesson("Dzień dobry!")

        scrollTo(text(R.string.listen_title))
        scrollTo(text(R.string.listen_none_title))
        scrollTo(text(R.string.listen_recordings))
        // Not on the phone yet: listed, and not to be tapped.
        screen.onNodeWithContentDescription("001", substring = true).performScrollTo().assertHasNoClickAction()

        back()
        // A chapter still being written lists its recordings too, by the book's sections.
        lesson("Karton czy pudełko?")
        scrollTo(text(R.string.listen_section, "A"))
        scrollTo(text(R.string.listen_section, "B"))
        screen.onNodeWithContentDescription("161", substring = true).performScrollTo().assertIsDisplayed()

        // "Add them" goes to Class audio, and back comes back to the chapter.
        screen.onNodeWithText(text(R.string.listen_add)).performScrollTo().performClick()
        shows(R.string.class_audio_title)
        showsOnPhone(0)
        back()
        shows("Karton czy pudełko?")
    }

    @Test
    fun `the class zips are added in Class audio from Settings, and a recording plays from its chapter's Słuchaj`() {
        openApp()
        tap(R.string.nav_settings)
        shows(text(R.string.settings_class_audio_note, 0, 271))
        tap(R.string.settings_class_audio)

        importZips(0..1)
        shows(app.resources.getQuantityString(R.plurals.class_audio_added, 28, 28))
        showsOnPhone(28)
        assertEquals(28, File(app.noBackupFilesDir, "class-audio").listFiles()!!.size, "Kept where no backup reaches")
        back()
        shows(text(R.string.settings_class_audio_note, 28, 271))

        tap(R.string.nav_path)
        lesson("Dzień dobry!")
        scrollTo(text(R.string.listen_title))
        shows(text(R.string.listen_some, 3, 3))
        screen.onNode(hasText("Ćwiczenie 2") and hasClickAction()).performScrollTo().performClick()

        // The player, with what the recording is.
        shows(text(R.string.listen_label, 2))
        assertEquals(listOf(2), recordings.opened)
        screen.onNodeWithContentDescription(text(R.string.listen_play)).performClick()
        screen.onNodeWithContentDescription(text(R.string.listen_pause)).assertIsDisplayed()
        screen.onNodeWithContentDescription(text(R.string.listen_back_5)).performClick()
        screen.onNodeWithContentDescription(text(R.string.listen_next_line)).performClick()
        tap(R.string.listen_speed_slower)
        screen.onNodeWithText(text(R.string.listen_step)).performScrollTo().performClick()
        screen.onNodeWithText(text(R.string.listen_repeat)).performScrollTo().performClick()
        assertEquals(listOf("open 2", "playPause", "skip -5000", "line 0", "speed 0.75", "step true", "repeat true"), recordings.asked)

        // Step mode waits for the Learner to say the line.
        screen.onNodeWithText(text(R.string.listen_your_turn)).assertDoesNotExist()
        recordings.state = recordings.state.copy(yourTurn = true, line = 1)
        screen.onNodeWithText(text(R.string.listen_your_turn)).performScrollTo().assertIsDisplayed()

        // On to the next recording of the chapter, and back to the chapter, which stops the player.
        screen.onNodeWithText(text(R.string.listen_next_recording, 3)).performScrollTo().performClick()
        shows(text(R.string.listen_label, 3))
        assertEquals(listOf(2, 3), recordings.opened)
        back()
        shows(text(R.string.lesson_label, 0))
        assertEquals("close", recordings.asked.last())
    }

    @Test
    fun `Remove deletes the recordings, and the chapter's list can no longer be played`() {
        openApp()
        tap(R.string.nav_settings)
        tap(R.string.settings_class_audio)
        importZips(0..0)
        showsOnPhone(3)

        screen.onNodeWithText(text(R.string.class_audio_remove)).performScrollTo().performClick()
        tap(R.string.class_audio_remove_confirm)
        showsOnPhone(0)
        assertTrue(File(app.noBackupFilesDir, "class-audio").listFiles().orEmpty().isEmpty())

        back()
        tap(R.string.nav_path)
        lesson("Dzień dobry!")
        screen.onNodeWithContentDescription("001", substring = true).performScrollTo().assertHasNoClickAction()
    }

    @Test
    fun `a recording's listening tasks are answered in place, true or false, a choice, and what was heard`() {
        val course = realCourse.copy(
            listening = listOf(
                TrueFalseTask("t1", 1, "Pani mówi „dzień dobry”.", mapOf("en" to "She says “dzień dobry”."), answer = true),
                ChooseTask("t2", 1, "Co mówi pani?", options = listOf("dziękuję", "przepraszam"), correctOption = 0, lines = listOf(1, 2)),
                DictationTask("t3", 1, "Jakie słowo pani mówi?", acceptedAnswers = listOf("proszę")),
            ),
        )
        openApp(course)
        tap(R.string.nav_settings)
        tap(R.string.settings_class_audio)
        importZips(0..0)
        back()
        tap(R.string.nav_path)
        lesson("Dzień dobry!")
        screen.onNode(hasText("Ćwiczenie 1") and hasClickAction()).performScrollTo().performClick()

        scrollTo(text(R.string.listen_tasks_title))
        screen.onNodeWithText(text(R.string.listen_true)).performScrollTo().performClick()
        screen.onNodeWithText(text(R.string.listen_right)).performScrollTo().assertIsDisplayed()

        screen.onNodeWithText("przepraszam").performScrollTo().performClick()
        screen.onNodeWithText(text(R.string.listen_wrong, "dziękuję")).performScrollTo().assertIsDisplayed()

        screen.onNode(hasSetTextAction()).performScrollTo().performTextInput("prosze")
        screen.onNodeWithText(text(R.string.listen_check)).performScrollTo().performClick()
        assertEquals(3, (viewModel?.screen as Screen.Listen).answered.size)
        assertTrue((viewModel?.screen as Screen.Listen).answered.getValue("t3").feedback.correct)
    }

    /** Adds the publisher's zips of chapters [lessons], picked in Class audio, and waits for them to be read. */
    private fun importZips(lessons: IntRange) {
        val zips = classZips(realCourse, lessons, folder.newFolder())
        screen.runOnUiThread { viewModel?.importClassAudio(files = zips.map(Uri::fromFile)) }
        // The files are read away from the main thread, which then shows what was added.
        var waited = 0
        while ((viewModel?.screen as? Screen.ClassAudio)?.importing != false && waited++ < 400) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(25)
        }
        assertEquals(false, (viewModel?.screen as? Screen.ClassAudio)?.importing, "The zips should have been read")
        screen.waitForIdle()
    }

    private fun openApp(course: Course = realCourse) {
        val engine = CourseEngine(
            course,
            FileLearnerProgress(File(folder.root, "progress.json")),
            clock,
            oneADay = { false },
            checkpoints = InMemoryCheckpointBests(),
        )
        viewModel = CourseViewModel(app, engine, engine.practice(InMemoryPracticeProgress(), Random(7)))
        screen.setContent {
            PolishEasyTheme {
                viewModel?.let { key(it) { PolishEasyApp(it, clips, recordings) } }
            }
        }
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

    /** Taps the thing that says [id], scrolled into sight first when it is in a list. */
    private fun tap(id: Int) {
        val node = screen.onNodeWithText(text(id))
        runCatching { node.performScrollTo() }
        node.performClick()
        screen.waitForIdle()
    }

    private fun scrollTo(text: String) {
        screen.onNodeWithText(text).performScrollTo()
    }

    /** Class audio says [count] of the 271 recordings are on the phone. */
    private fun showsOnPhone(count: Int) {
        screen.onNodeWithContentDescription("$count of 271 recordings on this phone", substring = true).assertIsDisplayed()
    }

    private fun shows(id: Int) = shows(text(id))

    private fun shows(text: String) {
        screen.onNodeWithText(text).assertIsDisplayed()
    }
}
