package com.polisheasy.app

import android.app.Application
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.polisheasy.app.ui.PolishEasyApp
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.engine.CourseEngine
import com.polisheasy.engine.FileLearnerProgress
import java.io.File
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A picture of every screen, written to `app/build/screenshots/` as one PNG per screen. CI uploads
 * them as the `screenshots` artifact, so a change to how the app looks can be seen without a phone
 * (docs/design.md).
 *
 * It walks the fixture course in the light and the dark theme, and opens the real course once.
 * The pictures show layout, colour and type. They do not show motion, the keyboard or sound.
 *
 * The activity's view is drawn into a bitmap. `captureToImage()` is not used: it times out here.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotsTest {
    @get:Rule(order = 0)
    val folder = TemporaryFolder()

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
    private var viewModel: CourseViewModel? by mutableStateOf(null)

    @Test
    fun `every screen of the fixture course in the light theme`() = fixtureCourseTour("light")

    @Test
    @Config(qualifiers = "+night")
    fun `every screen of the fixture course in the dark theme`() = fixtureCourseTour("dark")

    @Test
    fun `the real course on Home, on the Path and at its first step`() {
        open(CourseViewModel(app))
        shot("real-01-home")
        tap(R.string.home_path)
        shot("real-02-path")
        back()
        tap(R.string.home_start)
        shot("real-03-first-step")
    }

    /** Daily Set 1 of the fixture course, with one wrong answer so the Exercise comes back at the end. */
    private fun fixtureCourseTour(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        open(CourseViewModel(app, CourseEngine(fixtureCourse(), FileLearnerProgress(File(folder.root, "progress.json")), clock)))
        shot("01-home-ready")
        tap(R.string.home_settings)
        shot("02-settings")
        back()

        tap(R.string.home_start)
        shot("03-tip")
        tap(R.string.action_continue)

        shot("04-choose-translation")
        tap("good night")
        shot("05-feedback-wrong")
        tap(R.string.action_continue)

        shot("06-word-tiles")
        listOf("to", "jest").forEach(::tap)
        shot("07-word-tiles-building")
        tap("Adam")
        tap(R.string.action_check)
        shot("08-feedback-correct")
        tap(R.string.action_continue)

        type("dziekuje")
        shot("09-type-answer")
        tap(R.string.action_check)
        shot("10-feedback-accent-warning")
        tap(R.string.action_continue)

        shot("11-listen-choose")
        tap("do widzenia")
        tap(R.string.action_continue)

        shot("12-listen-type")
        type("nie")
        tap(R.string.action_check)
        tap(R.string.action_continue)

        shot("13-conjugation")
        type("jestem")
        tap(R.string.action_check)
        tap(R.string.action_continue)

        shot("14-exercise-returned")
        tap("hello (polite); good morning")
        tap(R.string.action_continue)

        shot("15-summary")
        tap(R.string.summary_back_home)
        shot("16-home-done")
        tap(R.string.home_path)
        shot("17-path")
    }

    private fun open(over: CourseViewModel) {
        viewModel = over
        screen.setContent {
            PolishEasyTheme {
                viewModel?.let { key(it) { PolishEasyApp(it, clips) } }
            }
        }
    }

    private fun shot(name: String) {
        screen.waitForIdle()
        val view = screen.activity.window.decorView
        val picture = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        screen.runOnUiThread { view.draw(Canvas(picture)) }
        val file = File(SCREENSHOTS.apply { mkdirs() }, "$name.png")
        file.outputStream().use { picture.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 0, "No picture was written to $file")
    }

    private fun tap(id: Int) = tap(app.getString(id))

    private fun tap(text: String) {
        screen.onNodeWithText(text).performClick()
    }

    private fun type(text: String) = screen.onNode(hasSetTextAction()).performTextInput(text)

    private fun back() = screen.runOnUiThread { screen.activity.onBackPressedDispatcher.onBackPressed() }

    private companion object {
        /** Gradle runs the tests from the `app` directory. */
        val SCREENSHOTS = File("build/screenshots")
    }
}
