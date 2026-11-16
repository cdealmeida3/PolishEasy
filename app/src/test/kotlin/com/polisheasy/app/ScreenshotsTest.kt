package com.polisheasy.app

import android.app.Application
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.polisheasy.app.ui.PolishEasyApp
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.components.NavigationFrame
import com.polisheasy.app.ui.components.NavigationTab
import com.polisheasy.app.ui.screens.HomeScreen
import com.polisheasy.app.ui.screens.SummaryScreen
import com.polisheasy.engine.CourseEngine
import com.polisheasy.engine.DayView
import com.polisheasy.engine.FileLearnerProgress
import com.polisheasy.engine.NextDailySetView
import com.polisheasy.engine.SetProgress
import com.polisheasy.engine.Today
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
        tap(R.string.nav_path)
        shot("real-02-path")
        back()
        tap(R.string.home_start)
        shot("real-03-first-step")
    }

    @Test
    fun `the Summary of a Review-only Daily Set and of a Replay in the light theme`() = summaryVariants("light")

    @Test
    @Config(qualifiers = "+night")
    fun `the Summary of a Review-only Daily Set and of a Replay in the dark theme`() = summaryVariants("dark")

    /** The Summaries the fixture course does not reach in one day, shown with made-up scores. */
    private fun summaryVariants(theme: String) {
        var replay by mutableStateOf(false)
        screen.setContent {
            PolishEasyTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    key(replay) {
                        SummaryScreen(
                            correctAnswers = if (replay) 6 else 9,
                            exercises = if (replay) 6 else 12,
                            itemsLearned = emptyList(),
                            reviewOnly = !replay,
                            replay = replay,
                            streakRaised = if (replay) null else StreakRaised(from = 11, to = 12),
                            onDone = {},
                        )
                    }
                }
            }
        }
        shot("$theme-18-summary-review-only")
        replay = true
        shot("$theme-19-summary-replay")
    }

    @Test
    fun `the states of Home the tour does not reach in the light theme`() = homeStates("light")

    @Test
    @Config(qualifiers = "+night")
    fun `the states of Home the tour does not reach in the dark theme`() = homeStates("dark")

    /** Home part of the way through a Unit and a week, and in its Review-only states, with made-up progress. */
    private fun homeStates(theme: String) {
        val unlock = clock.today.plusDays(1).atStartOfDay(clock.zone)
        val next = NextDailySetView(
            unitNumber = 2,
            unitTitle = "About me",
            numberInUnit = 4,
            dailySetsInUnit = 8,
            teaches = listOf("Czy and nie", "Pan and pani"),
        )
        // Yesterday and the three days before it were completed; the days before those were not.
        fun week(today: Boolean) = (6L downTo 0L).map { ago -> DayView(clock.today.minusDays(ago), if (ago == 0L) today else ago <= 4) }
        val states = listOf(
            Triple("20-home-in-progress", Today.InProgress(SetProgress(correct = 7, total = 18)), next),
            Triple("21-home-ready-mid-unit", Today.DailySetAvailable, next),
            Triple("22-home-done-mid-unit", Today.DoneForToday(unlock), next.copy(numberInUnit = 5, teaches = listOf("Mieć: to have", "Mam na imię"))),
            Triple("23-home-review-only-ready", Today.ReviewOnlyAvailable, null),
            Triple("24-home-review-only-in-progress", Today.InProgress(SetProgress(correct = 5, total = 20), reviewOnly = true), null),
            Triple("25-home-review-only-done", Today.DoneForToday(unlock, reviewOnlyNext = true), null),
            Triple("26-home-all-completed", Today.AllCompleted, null),
        )
        var showing by mutableStateOf(0)
        screen.setContent {
            PolishEasyTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val (_, today, position) = states[showing]
                    val done = today is Today.DoneForToday
                    key(showing) {
                        NavigationFrame(
                            tabs = listOf(
                                NavigationTab(app.getString(R.string.nav_home), R.drawable.ic_home),
                                NavigationTab(app.getString(R.string.nav_path), R.drawable.ic_route),
                                NavigationTab(app.getString(R.string.nav_settings), R.drawable.ic_settings),
                            ),
                            selected = 0,
                            onSelect = {},
                        ) {
                            HomeScreen(today, streak = if (done) 5 else 4, week = week(today = done), next = position, onStart = {}, onContinue = {})
                        }
                    }
                }
            }
        }
        states.forEachIndexed { index, (name, _, _) ->
            showing = index
            shot("$theme-$name")
        }
    }

    /** Daily Set 1 of the fixture course, with one wrong answer so the Exercise comes back at the end. */
    private fun fixtureCourseTour(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        open(CourseViewModel(app, CourseEngine(fixtureCourse(), FileLearnerProgress(File(folder.root, "progress.json")), clock)))
        shot("01-home-ready")
        tap(R.string.nav_settings)
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

        // The Summary part of the way through its celebration, and then at rest.
        screen.mainClock.autoAdvance = false
        screen.mainClock.advanceTimeBy(1_900)
        shot("15-summary-celebrating")
        screen.mainClock.autoAdvance = true
        shot("15-summary")
        tap(R.string.summary_back_home)
        shot("16-home-done")
        tap(R.string.nav_path)
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
