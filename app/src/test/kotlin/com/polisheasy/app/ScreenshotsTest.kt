package com.polisheasy.app

import androidx.compose.ui.test.performScrollTo
import com.polisheasy.engine.UnitCompletedView
import com.polisheasy.engine.SoFarView
import android.app.Application
import android.content.ComponentName
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.polisheasy.app.ui.PolishEasyApp
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.components.NavigationFrame
import com.polisheasy.app.ui.components.NavigationTab
import com.polisheasy.app.ui.screens.HomeScreen
import com.polisheasy.app.ui.screens.SettingsScreen
import com.polisheasy.app.ui.screens.SummaryScreen
import com.polisheasy.engine.CourseEngine
import com.polisheasy.engine.DayView
import com.polisheasy.engine.FileLearnerProgress
import com.polisheasy.engine.NextDailySetView
import com.polisheasy.engine.SetProgress
import com.polisheasy.engine.Today
import java.io.File
import kotlin.random.Random
import com.polisheasy.engine.FilePracticeProgress
import com.polisheasy.engine.InMemoryLearnerProgress
import com.polisheasy.engine.InMemoryCheckpointBests
import com.polisheasy.engine.InMemoryPracticeProgress
import com.polisheasy.engine.loadCourse
import com.polisheasy.engine.ChainRound
import com.polisheasy.engine.GapRound
import com.polisheasy.engine.GenderRound
import com.polisheasy.engine.NumberRound
import com.polisheasy.engine.QuickRound
import com.polisheasy.engine.RegisterRound
import com.polisheasy.engine.Register
import com.polisheasy.engine.SpellRound
import com.polisheasy.engine.ListenRound
import com.polisheasy.engine.AnkietaRound
import com.polisheasy.engine.LabelRound
import com.polisheasy.engine.ChatRound
import com.polisheasy.engine.MemoryFace
import com.polisheasy.engine.MemoryRound
import com.polisheasy.engine.VerbRound
import com.polisheasy.engine.VerbStage
import java.time.LocalTime
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
import com.polisheasy.engine.ItemView
import com.polisheasy.engine.ClipView

/**
 * A picture of every screen, written to `app/build/screenshots/` as one PNG per screen. CI uploads
 * them as the `screenshots` artifact, so a change to how the app looks can be seen without a phone
 * (docs/design.md).
 *
 * It walks the fixture course in the light and the dark theme, and opens the real course once.
 * The `large-` pictures are the light theme again at the largest font size a phone offers, to show
 * that nothing is cut off. The pictures show layout, colour and type. They do not show motion, the
 * keyboard or sound.
 *
 * Every picture is also checked for what a picture cannot show: that each thing a finger can tap
 * has a name a screen reader can say, and is at least 48 dp across and high.
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
            // The welcome is shown once; the tour shows it and then starts after it.
            Settings(app).welcomed = true
        }
    }

    @get:Rule(order = 2)
    val screen = createAndroidComposeRule<ComponentActivity>()

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val clock = SettableClock()
    private val clips = RecordingClips()
    private val recordings = RecordingRecordings()
    private val speech = FakeSpeech()
    private var viewModel: CourseViewModel? by mutableStateOf(null)

    /** How much larger than usual the text is. Set before anything is shown. */
    private var fontScale = 1f

    @Test
    fun `every screen of the fixture course in the light theme`() = fixtureCourseTour("light")

    @Test
    @Config(qualifiers = "+night")
    fun `every screen of the fixture course in the dark theme`() = fixtureCourseTour("dark")

    @Test
    fun `every screen of the fixture course at the largest font size`() = atTheLargestFontSize { fixtureCourseTour("large") }

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

    @Test
    fun `the Summary of a Review-only Daily Set and of a Replay at the largest font size`() = atTheLargestFontSize { summaryVariants("large") }

    /** The Summaries the fixture course does not reach in one day, shown with made-up scores: a Review-only one, a Replay, and a Streak reaching a week with a long list of words. */
    private fun summaryVariants(theme: String) {
        // A Thursday, with Monday to Wednesday completed and today just now.
        val week = (0L..6L).map { day -> DayView(clock.today.plusDays(day), completed = day <= 3, today = day == 3L, upcoming = day > 3) }
        val words = listOf(
            ItemView("mam", "I have"), ItemView("masz", "you have"), ItemView("ma", "he, she or it has"),
            ItemView("mieć", "to have"), ItemView("brat", "brother"), ItemView("siostra", "sister"),
        )
        var showing by mutableStateOf(0)
        screen.setContent {
            Shown {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    key(showing) {
                        val replay = showing == 1
                        SummaryScreen(
                            correctAnswers = if (replay) 6 else 9,
                            exercises = if (replay) 6 else 12,
                            itemsLearned = if (showing >= 2) words else emptyList(),
                            unitCompleted = if (showing == 3) UnitCompletedView(2, "About me") else null,
                            reviewOnly = showing == 0,
                            replay = replay,
                            streakRaised = when (showing) {
                                0 -> StreakRaised(from = 11, to = 12)
                                2 -> StreakRaised(from = 6, to = 7)
                                3 -> StreakRaised(from = 15, to = 16)
                                else -> null
                            },
                            week = week,
                            clipOf = { ClipView("audio/word.ogg") },
                            onPlay = {},
                            onDone = {},
                        )
                    }
                }
            }
        }
        shot("$theme-18-summary-review-only")
        showing = 1
        shot("$theme-19-summary-replay")
        showing = 2
        shot("$theme-19-summary-streak-milestone")
        showing = 3
        shot("$theme-19-summary-unit-complete")
    }

    @Test
    fun `the states of Home the tour does not reach in the light theme`() = homeStates("light")

    @Test
    @Config(qualifiers = "+night")
    fun `the states of Home the tour does not reach in the dark theme`() = homeStates("dark")

    @Test
    fun `the states of Home the tour does not reach at the largest font size`() = atTheLargestFontSize { homeStates("large") }

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
        // A Thursday: Tuesday and Wednesday were completed, Monday was not, and three days are still to come.
        fun week(today: Boolean) = (0L..6L).map { day -> DayView(clock.today.plusDays(day), if (day == 3L) today else day in 1L..2L, today = day == 3L, upcoming = day > 3) }
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
            Shown {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val (_, today, position) = states[showing]
                    val done = today is Today.DoneForToday
                    key(showing) {
                        NavigationFrame(
                            tabs = listOf(
                                NavigationTab(app.getString(R.string.nav_home), R.drawable.ic_home),
                                NavigationTab(app.getString(R.string.nav_path), R.drawable.ic_route),
                                NavigationTab(app.getString(R.string.nav_practice), R.drawable.ic_stadia_controller),
                                NavigationTab(app.getString(R.string.nav_practice), R.drawable.ic_stadia_controller),
                            NavigationTab(app.getString(R.string.nav_settings), R.drawable.ic_settings),
                            ),
                            selected = 0,
                            onSelect = {},
                        ) {
                            HomeScreen(
                                today,
                                streak = if (done) 5 else 4,
                                week = week(today = done),
                                next = position,
                                onStart = {},
                                onContinue = {},
                                soFar = SoFarView(wordsLearned = 58, dailySetsCompleted = if (done) 12 else 11, longestStreak = 9),
                                onReplayToday = if (done && position != null) ({}) else null,
                            )
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

    @Test
    fun `the Path's shelves, a Lesson screen and its Powtórzenie in the light theme`() = bookTour("light")

    @Test
    @Config(qualifiers = "+night")
    fun `the Path's shelves, a Lesson screen and its Powtórzenie in the dark theme`() = bookTour("dark")

    @Test
    fun `the Path's shelves, a Lesson screen and its Powtórzenie at the largest font size`() = atTheLargestFontSize { bookTour("large") }

    /**
     * The real course with made-up progress: the Path part of the way through Starter and a Starter
     * Unit's Lesson screen, then Starter completed, chapter 0's Lesson screen with its look-up words,
     * its Słownik, its Fiszki and its Test, with the Polish instruction and its English, a Test not
     * passed and one passed, the chapter sealed, and a chapter still coming.
     */
    private fun bookTour(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")
        val course = loadCourse { path -> app.assets.open(path).bufferedReader().use { it.readText() } }
        val progress = InMemoryLearnerProgress()
        val starter = course.levels.first().units.flatMap { it.dailySets }
        starter.take(10).forEachIndexed { index, dailySet -> progress.recordCompleted(dailySet.id, clock.today.minusDays(10L - index)) }
        val engine = CourseEngine(course, progress, clock, oneADay = { false }, checkpoints = InMemoryCheckpointBests())
        val model = CourseViewModel(app, engine, engine.practice(InMemoryPracticeProgress(), Random(7)))
        open(model)
        tap(R.string.nav_path)
        shot("27-path-shelves")
        screen.onNode(hasText(app.getString(R.string.lesson_card_current)) and hasClickAction()).performClick()
        shot("28-lesson-starter")
        screen.onNodeWithText("The plural of mówić").performScrollTo()
        shot("28-lesson-starter-sections")
        back()

        starter.drop(10).forEach { progress.recordCompleted(it.id, clock.today.minusDays(1)) }
        back()
        tap(R.string.nav_path)
        shot("29-path-book")
        screen.onNode(hasText("Dzień dobry!") and hasClickAction()).performClick()
        shot("30-lesson-book")
        screen.onNode(hasText("lekcja") and hasClickAction()).performScrollTo().performClick()
        shot("30-lesson-book-look-up")
        screen.onNodeWithText("Polecenia").performScrollTo()
        shot("30-lesson-book-sections")
        screen.onNodeWithText(app.getString(R.string.lesson_dictionary)).performScrollTo()
        shot("30-lesson-book-review")

        screen.onNodeWithText(app.getString(R.string.lesson_dictionary)).performScrollTo().performClick()
        shot("34-dictionary")
        type("naucz")
        shot("34-dictionary-found")
        back()

        screen.onNodeWithText(app.getString(R.string.lesson_flashcards)).performScrollTo().performClick()
        shot("35-flashcards")
        model.screen.let { it as Screen.Flashcards }.let { cards ->
            screen.onNode(hasText(cards.cards[0].word.text) and hasClickAction()).performClick()
        }
        screen.mainClock.advanceTimeBy(1_000)
        shot("35-flashcards-turned")
        while ((model.screen as Screen.Flashcards).let { it.index < it.cards.size }) {
            screen.runOnUiThread { model.answerFlashcard(knew = (model.screen as Screen.Flashcards).index % 3 != 0) }
        }
        shot("35-flashcards-done")
        back()

        screen.onNodeWithText(app.getString(R.string.lesson_test)).performScrollTo().performClick()
        shot("36-test-question")
        screen.onNodeWithContentDescription(app.getString(R.string.instruction_translate)).performClick()
        shot("36-test-instruction-english")
        playTest(model, course, wrongly = 5)
        screen.mainClock.advanceTimeBy(3_000)
        shot("37-test-not-passed")
        tap(R.string.checkpoint_again)
        playTest(model, course, wrongly = 1)
        screen.mainClock.autoAdvance = false
        screen.mainClock.advanceTimeBy(3_000)
        screen.mainClock.autoAdvance = true
        shot("37-test-passed")
        tap(R.string.result_done)
        screen.onNodeWithText(app.getString(R.string.lesson_test)).performScrollTo()
        shot("38-lesson-sealed")
        back()
        shot("38-path-sealed")
        screen.onNode(hasText("Jesteś instruktorem tanga?") and hasClickAction()).performScrollTo().performClick()
        shot("39-lesson-coming")
    }

    @Test
    fun `Class audio, a chapter's Słuchaj and the player in the light theme`() = classAudioTour("light")

    @Test
    @Config(qualifiers = "+night")
    fun `Class audio, a chapter's Słuchaj and the player in the dark theme`() = classAudioTour("dark")

    @Test
    fun `Class audio, a chapter's Słuchaj and the player at the largest font size`() = atTheLargestFontSize { classAudioTour("large") }

    /**
     * The class recordings over the real course: chapter 0's Słuchaj before any recording is on the
     * phone, Class audio empty and after the zips of chapters 0 and 1 are added, the Słuchaj of
     * chapter 0 and of chapter 1 (by the book's sections), and the player of a
     * recording: at rest, playing in step mode with the Learner's turn, and its listening tasks
     * before and after they are answered.
     */
    private fun classAudioTour(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")
        val real = loadCourse { path -> app.assets.open(path).bufferedReader().use { it.readText() } }
        val course = real.copy(
            listening = listOf(
                com.polisheasy.engine.TrueFalseTask("t1", 4, "Pani ma na imię Anna.", mapOf("en" to "Her name is Anna."), answer = true),
                com.polisheasy.engine.ChooseTask("t2", 4, "Skąd jest pan?", mapOf("en" to "Where is he from?"), listOf("z Polski", "z Hiszpanii", "z Niemiec"), 1, lines = listOf(2, 4)),
                com.polisheasy.engine.DictationTask("t3", 4, "Jaki to numer pokoju?", mapOf("en" to "Which room number is it?"), listOf("siedem", "7")),
            ),
        )
        val engine = CourseEngine(course, InMemoryLearnerProgress(), clock, oneADay = { false }, checkpoints = InMemoryCheckpointBests())
        val model = CourseViewModel(
            app,
            engine,
            engine.practice(InMemoryPracticeProgress(), Random(7)),
            classAudioDir = folder.newFolder("class-audio-$theme"),
        )
        open(model)
        tap(R.string.nav_path)
        screen.onNode(hasText("Dzień dobry!") and hasClickAction()).performScrollTo().performClick()
        screen.onNodeWithText(app.getString(R.string.listen_title)).performScrollTo()
        screen.onNodeWithText(app.getString(R.string.listen_add)).performScrollTo()
        shot("121-listen-none")
        screen.onNodeWithText(app.getString(R.string.listen_add)).performClick()
        shot("122-class-audio-empty")
        val zips = classZips(real, 0..1, folder.newFolder("zips-$theme"))
        screen.runOnUiThread { model.importClassAudio(files = zips.map(android.net.Uri::fromFile)) }
        var waited = 0
        while ((model.screen as? Screen.ClassAudio)?.importing != false && waited++ < 400) {
            shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(25)
        }
        shot("122-class-audio-added")
        back()
        screen.onNodeWithText(app.getString(R.string.listen_recordings)).performScrollTo()
        shot("123-listen-chapter-0")
        back()
        screen.onNode(hasText("Pierwszy dzień w szkole") and hasClickAction()).performScrollTo().performClick()
        screen.onAllNodes(hasText(app.getString(R.string.listen_section, "A"), substring = true))[0].performScrollTo()
        shot("123-listen-chapter-1")
        screen.onNode(hasText("004") and hasClickAction()).performScrollTo().performClick()
        shot("124-player")
        recordings.state = recordings.state.copy(playing = true, positionMs = 6_400, durationMs = 13_000, step = true, line = 2, yourTurn = true, speed = 0.75f)
        shot("124-player-your-turn")
        recordings.state = recordings.state.copy(yourTurn = false, step = false, positionMs = 9_000)
        screen.onNodeWithText(app.getString(R.string.listen_tasks_title)).performScrollTo()
        shot("125-listen-tasks")
        screen.onNodeWithText(app.getString(R.string.listen_true)).performScrollTo().performClick()
        screen.onNodeWithText("z Polski").performScrollTo().performClick()
        screen.onNode(hasSetTextAction()).performScrollTo().performTextInput("siedem")
        screen.onNodeWithText(app.getString(R.string.listen_check)).performScrollTo().performClick()
        shot("125-listen-tasks-answered")
        back()
        back()
        tap(R.string.nav_settings)
        screen.onNodeWithText(app.getString(R.string.settings_class_audio)).performScrollTo()
        shot("126-settings-class-audio")
    }

    @Test
    fun `chapter 1 of the book over the real course in the light theme`() = chapter1Tour("light")

    @Test
    @Config(qualifiers = "+night")
    fun `chapter 1 of the book over the real course in the dark theme`() = chapter1Tour("dark")

    @Test
    fun `chapter 1 of the book over the real course at the largest font size`() = atTheLargestFontSize { chapter1Tour("large") }

    /**
     * Chapter 1 over the real course: its Lesson screen at the top with its Słownik, its sections
     * and Test, then section A's register table Tip and its first Exercise.
     */
    private fun chapter1Tour(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")
        val course = loadCourse { path -> app.assets.open(path).bufferedReader().use { it.readText() } }
        val engine = CourseEngine(course, InMemoryLearnerProgress(), clock, oneADay = { false }, checkpoints = InMemoryCheckpointBests())
        val model = CourseViewModel(app, engine, engine.practice(InMemoryPracticeProgress(), Random(7)))
        open(model)
        screen.runOnUiThread { model.openLesson("kpk1-l01") }
        screen.waitForIdle()
        shot("147-chapter-1")
        screen.onAllNodes(hasText("Liczebniki 0–10", substring = true), useUnmergedTree = true)[1].performScrollTo()
        shot("147-chapter-1-sections")
        screen.onNodeWithText(app.getString(R.string.lesson_test)).performScrollTo()
        shot("147-chapter-1-test")
        screen.runOnUiThread { model.openSection("kpk1-l01-a1") }
        screen.waitForIdle()
        shot("148-chapter-1-tip")
        tap(R.string.action_continue)
        shot("148-chapter-1-first")
    }

    @Test
    fun `chapter 2 of the book over the real course in the light theme`() = chapter2Tour("light")

    @Test
    @Config(qualifiers = "+night")
    fun `chapter 2 of the book over the real course in the dark theme`() = chapter2Tour("dark")

    @Test
    fun `chapter 2 of the book over the real course at the largest font size`() = atTheLargestFontSize { chapter2Tour("large") }

    /**
     * Chapter 2 over the real course: its Lesson screen at the top and its sections, then section
     * C's Tip on być with its table and its first Exercise, and section F's Tip on żeby.
     */
    private fun chapter2Tour(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")
        val course = loadCourse { path -> app.assets.open(path).bufferedReader().use { it.readText() } }
        val engine = CourseEngine(course, InMemoryLearnerProgress(), clock, oneADay = { false }, checkpoints = InMemoryCheckpointBests())
        val model = CourseViewModel(app, engine, engine.practice(InMemoryPracticeProgress(), Random(7)))
        open(model)
        screen.runOnUiThread { model.openLesson("kpk1-l02") }
        screen.waitForIdle()
        shot("157-chapter-2")
        screen.onAllNodes(hasText("Liczebniki 11–29", substring = true), useUnmergedTree = true).onFirst().performScrollTo()
        shot("157-chapter-2-sections")
        screen.runOnUiThread { model.openSection("kpk1-l02-c") }
        screen.waitForIdle()
        shot("158-chapter-2-byc")
        tap(R.string.action_continue)
        shot("158-chapter-2-first")
        screen.runOnUiThread {
            model.openLesson("kpk1-l02")
            model.openSection("kpk1-l02-f2")
        }
        screen.waitForIdle()
        shot("159-chapter-2-zeby")
    }

    @Test
    fun `chapter 3 of the book over the real course in the light theme`() = chapter3Tour("light")

    @Test
    @Config(qualifiers = "+night")
    fun `chapter 3 of the book over the real course in the dark theme`() = chapter3Tour("dark")

    @Test
    fun `chapter 3 of the book over the real course at the largest font size`() = atTheLargestFontSize { chapter3Tour("large") }

    /**
     * Chapter 3 over the real course: its Lesson screen at the top and its sections, then section
     * A's first Exercise, section C's Tip on ten, ta, to with its gender table, and
     * section D's Tip on the colours and section B's on czy.
     */
    private fun chapter3Tour(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")
        val course = loadCourse { path -> app.assets.open(path).bufferedReader().use { it.readText() } }
        val engine = CourseEngine(course, InMemoryLearnerProgress(), clock, oneADay = { false }, checkpoints = InMemoryCheckpointBests())
        val model = CourseViewModel(app, engine, engine.practice(InMemoryPracticeProgress(), Random(7)))
        open(model)
        screen.runOnUiThread { model.openLesson("kpk1-l03") }
        screen.waitForIdle()
        shot("166-chapter-3")
        screen.onAllNodes(hasText("Kolory", substring = true), useUnmergedTree = true).onFirst().performScrollTo()
        shot("166-chapter-3-sections")
        screen.runOnUiThread { model.openSection("kpk1-l03-a2") }
        screen.waitForIdle()
        tap(R.string.action_continue)
        shot("167-chapter-3-first")
        screen.runOnUiThread {
            model.openLesson("kpk1-l03")
            model.openSection("kpk1-l03-c1")
        }
        screen.waitForIdle()
        shot("168-chapter-3-ten-ta-to")
        screen.runOnUiThread {
            model.openLesson("kpk1-l03")
            model.openSection("kpk1-l03-d1")
        }
        screen.waitForIdle()
        shot("169-chapter-3-colours")
        screen.runOnUiThread {
            model.openLesson("kpk1-l03")
            model.openSection("kpk1-l03-b")
        }
        screen.waitForIdle()
        shot("169-chapter-3-czy")
    }

    @Test
    fun `the book's Exercises, a table Tip and the Grammar shelf in the light theme`() = bookExercisesTour("light")

    @Test
    @Config(qualifiers = "+night")
    fun `the book's Exercises, a table Tip and the Grammar shelf in the dark theme`() = bookExercisesTour("dark")

    @Test
    fun `the book's Exercises, a table Tip and the Grammar shelf at the largest font size`() = atTheLargestFontSize { bookExercisesTour("large") }

    /**
     * Chapter 0's section Ćwiczenia over the real course: pairs matched (one wrong, shaken off, then
     * the lines drawn), gaps filled from sticky notes and typed, a dialogue put in order, a text
     * heard and read to mark true or false, an Uwaga callout; then section W szkole's Tip with its
     * gender table, and the Grammar shelf from the Lesson screen and from Settings.
     */
    private fun bookExercisesTour(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        fun press(text: String) {
            val node = screen.onNode(hasText(text) and hasClickAction())
            runCatching { node.performScrollTo() }
            node.performClick()
            screen.waitForIdle()
        }

        fun player() = viewModel?.screen as Screen.Player

        val course = loadCourse { path -> app.assets.open(path).bufferedReader().use { it.readText() } }
        val engine = CourseEngine(course, InMemoryLearnerProgress(), clock, oneADay = { false }, checkpoints = InMemoryCheckpointBests())
        val model = CourseViewModel(app, engine, engine.practice(InMemoryPracticeProgress(), Random(7)))
        open(model)
        screen.runOnUiThread {
            model.openLesson("kpk1-l00")
            model.openSection("kpk1-l00-e")
        }
        screen.waitForIdle()
        tap(R.string.action_continue)

        // Match the pairs.
        shot("127-match-pairs")
        screen.mainClock.autoAdvance = false
        press("posłuchać")
        press("repeat")
        screen.mainClock.advanceTimeBy(120)
        shot("127-match-pairs-wrong")
        screen.mainClock.autoAdvance = true
        listOf("posłuchać" to "listen", "powtórzyć" to "repeat", "przeczytać" to "read").forEach { (word, meaning) ->
            press(word)
            press(meaning)
        }
        shot("127-match-pairs-joined")
        listOf("napisać" to "write", "uzupełnić" to "fill in").forEach { (word, meaning) ->
            press(word)
            press(meaning)
        }
        shot("127-match-pairs-answered")
        tap(R.string.action_continue)

        // Fill the gaps from the sticky notes.
        shot("128-fill-gaps")
        press("posłuchać")
        shot("128-fill-gaps-one")
        press("powtórzyć")
        press(app.getString(R.string.action_check))
        shot("128-fill-gaps-right")
        tap(R.string.action_continue)
        screen.runOnUiThread { model.answer(rightAnswer(course, player().exerciseId, player().exercise)) }
        tap(R.string.action_continue)

        // Put the dialogue in order.
        listOf("Dzień dobry!", "Dzień dobry! Proszę imię i nazwisko.").forEach(::press)
        shot("129-order-lines")
        listOf("Ewa Nowak.", "Dziękuję.").forEach(::press)
        press(app.getString(R.string.action_check))
        shot("129-order-lines-right")
        tap(R.string.action_continue)

        // A text heard, then read, to mark true or false.
        shot("130-true-false-heard")
        screen.runOnUiThread { model.cantListenNow() }
        screen.waitForIdle()
        shot("130-true-false-read")
        listOf(true, false, true, false).forEachIndexed { index, value ->
            val label = if (value) R.string.true_false_true else R.string.true_false_false
            screen.onNodeWithContentDescription(app.getString(label, index + 1)).performScrollTo().performClick()
        }
        press(app.getString(R.string.action_check))
        shot("130-true-false-answered")
        tap(R.string.action_continue)

        // A gap typed instead.
        press(app.getString(R.string.fill_gaps_type))
        type("nauczyciel")
        shot("131-fill-gaps-typing")
        screen.runOnUiThread { model.answer(rightAnswer(course, player().exerciseId, player().exercise)) }
        tap(R.string.action_continue)

        // The Uwaga callout before the people words.
        shot("132-tip-uwaga")

        // The table Tip of W szkole, and the Grammar shelf once it is done.
        screen.runOnUiThread {
            model.openLesson("kpk1-l00")
            model.openSection("kpk1-l00-c")
        }
        screen.waitForIdle()
        shot("133-tip-table")
        while (true) {
            when (val showing = model.screen) {
                is Screen.Tip -> screen.runOnUiThread { model.continueToNext() }
                is Screen.Player -> screen.runOnUiThread {
                    if (showing.feedback == null) model.answer(rightAnswer(course, showing.exerciseId, showing.exercise)) else model.continueToNext()
                }
                else -> break
            }
            screen.waitForIdle()
        }
        screen.runOnUiThread { model.openLesson("kpk1-l00") }
        screen.waitForIdle()
        screen.onNodeWithText(app.getString(R.string.lesson_grammar_note, 1, 1)).performScrollTo()
        shot("134-lesson-grammar")
        screen.onNodeWithText(app.getString(R.string.lesson_grammar_note, 1, 1)).performClick()
        shot("134-grammar-chapter")
        back()
        back()
        tap(R.string.nav_settings)
        screen.onNodeWithText(app.getString(R.string.grammar_title)).performScrollTo()
        shot("135-settings-grammar")
        screen.onNodeWithText(app.getString(R.string.grammar_title)).performClick()
        shot("135-grammar-shelf")
    }

    /** Plays the Test showing to its end, answering the first [wrongly] questions wrongly and the rest rightly. */
    private fun playTest(model: CourseViewModel, course: com.polisheasy.engine.Course, wrongly: Int) {
        var asked = 0
        while (true) {
            val player = model.screen as? Screen.Player ?: return
            val answer = rightAnswer(course, player.exerciseId, player.exercise)
            screen.runOnUiThread {
                model.answer(if (asked++ < wrongly) wrongAnswer(answer) else answer)
                model.continueToNext()
            }
        }
    }

    @Test
    fun `Settings with the Reminder on in the light theme`() = settingsStates("light")

    @Test
    @Config(qualifiers = "+night")
    fun `Settings with the Reminder on in the dark theme`() = settingsStates("dark")

    @Test
    fun `Settings with the Reminder on at the largest font size`() = atTheLargestFontSize { settingsStates("large") }

    /** Settings with the Reminder on, and then with Android refusing to show its notification. */
    private fun settingsStates(theme: String) {
        var allowed by mutableStateOf(true)
        screen.setContent {
            Shown {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NavigationFrame(
                        tabs = listOf(
                            NavigationTab(app.getString(R.string.nav_home), R.drawable.ic_home),
                            NavigationTab(app.getString(R.string.nav_path), R.drawable.ic_route),
                            NavigationTab(app.getString(R.string.nav_practice), R.drawable.ic_stadia_controller),
                            NavigationTab(app.getString(R.string.nav_settings), R.drawable.ic_settings),
                        ),
                        selected = 3,
                        onSelect = {},
                    ) {
                        SettingsScreen(
                            reminderEnabled = true,
                            reminderTime = LocalTime.of(19, 30),
                            notificationsAllowed = allowed,
                            soundOn = false,
                            onReminderEnabled = {},
                            onReminderTime = {},
                            onSoundOn = {},
                            onReport = {},
                        )
                    }
                }
            }
        }
        shot("$theme-31-settings-reminder-on")
        allowed = false
        shot("$theme-32-settings-notifications-off")
    }

    @Test
    fun `the fixture course at the Learner's own pace in the light theme`() = ownPaceTour("light")

    @Test
    @Config(qualifiers = "+night")
    fun `the fixture course at the Learner's own pace in the dark theme`() = ownPaceTour("dark")

    @Test
    fun `the fixture course at the Learner's own pace at the largest font size`() = atTheLargestFontSize { ownPaceTour("large") }

    /**
     * One a day off: the Path with nothing locked, then with a Daily Set in progress, the Summary
     * that offers the next one, and Home once today counts already.
     */
    private fun ownPaceTour(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        open(CourseViewModel(app, CourseEngine(fixtureCourse(), FileLearnerProgress(File(folder.root, "progress.json")), clock, oneADay = { false })))
        tap(R.string.nav_path)
        tap("A little of everything")
        shot("70-own-pace-path")
        back()
        back()

        tap(R.string.home_start)
        tap(R.string.action_continue)
        tap("hello (polite); good morning")
        tap(R.string.action_continue)
        back()
        tap(R.string.nav_path)
        tap("A little of everything")
        // The one in progress is where the Learner is; the question before replacing it is a dialog, which a picture of the screen leaves out.
        shot("71-own-pace-path-in-progress")
        back()
        back()

        tap(R.string.home_continue)
        listOf("to", "jest", "Adam").forEach(::tap)
        tap(R.string.action_check)
        tap(R.string.action_continue)
        type("dziękuję")
        tap(R.string.action_check)
        tap(R.string.action_continue)
        tap("do widzenia")
        tap(R.string.action_continue)
        type("nie")
        tap(R.string.action_check)
        tap(R.string.action_continue)
        type("jestem")
        tap(R.string.action_check)
        tap(R.string.action_continue)
        // The Summary at rest, after its celebration.
        screen.mainClock.autoAdvance = false
        screen.mainClock.advanceTimeBy(3_000)
        screen.mainClock.autoAdvance = true
        shot("72-own-pace-summary")
        tap(R.string.summary_back_home)
        // Home shows the Streak go up once more; the picture is of it at rest.
        screen.mainClock.advanceTimeBy(2_000)
        shot("73-own-pace-home-keep-going")
    }

    /** Daily Set 1 of the fixture course, with one wrong answer so the Exercise comes back at the end. */
    private fun fixtureCourseTour(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        fun fixture() = CourseViewModel(app, CourseEngine(fixtureCourse(), FileLearnerProgress(File(folder.root, "progress.json")), clock, oneADay = { true }))
        Settings(app).welcomed = false
        open(fixture())
        shot("00-welcome")
        Settings(app).welcomed = true
        viewModel = fixture()
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
        shot("13-conjugation-typed")
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
        // Home shows the Streak go up once more; the picture is of it at rest.
        screen.mainClock.advanceTimeBy(2_000)
        shot("16-home-done")
        tap(R.string.nav_path)
        shot("17-path")
        tap("A little of everything")
        shot("17-lesson")
    }

    @Test
    fun `the real course's Picture cards in the light theme`() = realPictureCards("light")

    @Test
    @Config(qualifiers = "+night")
    fun `the real course's Picture cards in the dark theme`() = realPictureCards("dark")

    @Test
    fun `the real course's Picture cards at the largest font size`() = atTheLargestFontSize { realPictureCards("large") }

    /** Practice as a Learner who has completed the whole real course sees it: every Game open, every picture. */
    private fun realPictureCards(theme: String) {
        val course = loadCourse { path -> app.assets.open(path).bufferedReader().use { it.readText() } }
        val progress = InMemoryLearnerProgress()
        course.path.forEachIndexed { index, dailySet ->
            progress.recordCompleted(dailySet.id, clock.today.minusDays((course.path.size - 1 - index).toLong()))
        }
        val engine = CourseEngine(course, progress, clock, oneADay = { true })
        open(CourseViewModel(app, engine, engine.practice(InMemoryPracticeProgress(), Random(7))))
        tap(R.string.nav_practice)
        shot("$theme-60-practice-real")
        tap(R.string.game_picture_cards)
        shot("$theme-61-picture-cards-real")
        screen.onNode(hasText(course.levels.first().units.last().title.getValue("en")) and hasClickAction()).performClick()
        shot("$theme-62-picture-cards-real-unit")
    }

    @Test
    fun `Memory pairs over the real course in the light theme`() = memoryPairs("light")

    @Test
    @Config(qualifiers = "+night")
    fun `Memory pairs over the real course in the dark theme`() = memoryPairs("dark")

    @Test
    fun `Memory pairs over the real course at the largest font size`() = atTheLargestFontSize { memoryPairs("large") }

    /**
     * Memory pairs as a Learner who has completed the whole real course plays it: before a Round, a
     * board of pictures new, part found, with two tiles that are not a pair, and finished; the
     * result; a board of sounds, and the largest board, of words and their meanings.
     */
    private fun memoryPairs(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        fun board() = (viewModel?.screen as Screen.Round).round as MemoryRound

        fun pairs() = board().let { board -> board.tiles.indices.groupBy { board.tiles[it].pair }.values.toList() }

        /** Taps the tile at [tile], which is face down, and lets it turn. */
        fun turn(tile: Int) {
            val node = screen.onNodeWithContentDescription(app.getString(R.string.memory_tile, tile + 1))
            // A scroll is animated, so with the clock held it would never end: let the clock run for it alone.
            if (runCatching { node.assertIsDisplayed() }.isFailure) {
                val held = !screen.mainClock.autoAdvance
                screen.mainClock.autoAdvance = true
                node.performScrollTo()
                screen.mainClock.autoAdvance = !held
            }
            node.performClick()
            screen.mainClock.advanceTimeBy(400)
        }

        val course = loadCourse { path -> app.assets.open(path).bufferedReader().use { it.readText() } }
        val progress = InMemoryLearnerProgress()
        course.path.forEachIndexed { index, dailySet ->
            progress.recordCompleted(dailySet.id, clock.today.minusDays((course.path.size - 1 - index).toLong()))
        }
        val engine = CourseEngine(course, progress, clock, oneADay = { true })
        open(CourseViewModel(app, engine, engine.practice(InMemoryPracticeProgress(), Random(7))))
        tap(R.string.nav_practice)
        tap(R.string.game_memory_pairs)
        shot("63-memory-setup")
        tap(R.string.setup_play)
        shot("64-memory-board")

        // The clock is held, so two tiles that are not a pair stay up to be pictured.
        screen.waitForIdle()
        screen.mainClock.autoAdvance = false
        val pictures = pairs()
        pictures.take(2).forEach { pair -> pair.forEach(::turn) }
        turn(pictures[2][0])
        shot("65-memory-found")
        turn(pictures[3][0])
        shot("66-memory-not-a-pair")
        // After a moment the two turn back.
        screen.mainClock.advanceTimeBy(Motion.MISS_MILLIS + 400)
        pictures.drop(2).forEach { pair -> pair.forEach(::turn) }
        shot("67-memory-finished")
        screen.mainClock.advanceTimeBy(Motion.BOARD_DONE_MILLIS + 100)
        screen.mainClock.autoAdvance = true
        shot("68-memory-result")

        // Pairs of a sound and its word, on the middle board.
        tap(R.string.result_done)
        tap(R.string.game_memory_pairs)
        tap(R.string.option_sound_word)
        tap(app.getString(R.string.option_pairs, 9))
        shot("69-memory-setup-sounds")
        tap(R.string.setup_play)
        screen.waitForIdle()
        screen.mainClock.autoAdvance = false
        val sounds = pairs()
        sounds.take(3).forEach { pair -> pair.forEach(::turn) }
        turn(sounds[3].first { board().tiles[it].face is MemoryFace.Sound })
        shot("70-memory-sounds")
        screen.mainClock.autoAdvance = true

        // Pairs of a word and its meaning, on the largest board.
        screen.onNodeWithContentDescription(app.getString(R.string.round_close)).performClick()
        tap(R.string.game_memory_pairs)
        tap(R.string.option_word_meaning)
        tap(app.getString(R.string.option_pairs, 12))
        tap(R.string.setup_play)
        screen.waitForIdle()
        screen.mainClock.autoAdvance = false
        val words = pairs()
        words.take(4).forEach { pair -> pair.forEach(::turn) }
        turn(words[4][0])
        turn(words[5][0])
        shot("71-memory-words")
        screen.mainClock.autoAdvance = true
    }

    @Test
    fun `Phrase chains and Listen and tap over the real course in the light theme`() = chainsAndListening("light")

    @Test
    @Config(qualifiers = "+night")
    fun `Phrase chains and Listen and tap over the real course in the dark theme`() = chainsAndListening("dark")

    @Test
    fun `Phrase chains and Listen and tap over the real course at the largest font size`() = atTheLargestFontSize { chainsAndListening("large") }

    /**
     * Phrase chains heard and read: before a Round, a chain new, part made with a slip, and whole,
     * and the result. Listen and tap by picture and by meaning: a question new, answered right and
     * answered wrong. Then Practice with sound off, where Listen and tap says why it is closed.
     */
    private fun chainsAndListening(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        fun chain() = (viewModel?.screen as Screen.Round).round as ChainRound

        fun listening() = (viewModel?.screen as Screen.Round).round as ListenRound

        fun order() = checkNotNull(chain().sentence).polish.split(" ").map { word -> word.trim(',', '.', '?', '!') }

        fun tapCard(word: String) = screen.onAllNodes(hasText(word) and hasClickAction())[0].performScrollTo().performClick()

        /** Answers with the option that is right, or with one that is not, and holds the answered question for its picture. */
        fun answer(correctly: Boolean) {
            val asked = checkNotNull(listening().question)
            val option = asked.options.indices.first { (it == asked.answer) == correctly }
            screen.waitForIdle()
            screen.mainClock.autoAdvance = false
            if (listening().pictures) {
                screen.onNodeWithContentDescription(asked.options[option].meaning).performScrollTo().performClick()
            } else {
                screen.onNode(hasText(asked.options[option].meaning) and hasClickAction()).performScrollTo().performClick()
            }
            screen.mainClock.advanceTimeBy(500)
        }

        fun moveOn() {
            screen.mainClock.autoAdvance = true
            tap(R.string.action_continue)
        }

        val course = loadCourse { path -> app.assets.open(path).bufferedReader().use { it.readText() } }
        val progress = InMemoryLearnerProgress()
        course.path.forEachIndexed { index, dailySet ->
            progress.recordCompleted(dailySet.id, clock.today.minusDays((course.path.size - 1 - index).toLong()))
        }
        val engine = CourseEngine(course, progress, clock, oneADay = { true })
        open(CourseViewModel(app, engine, engine.practice(InMemoryPracticeProgress(), Random(7))))
        tap(R.string.nav_practice)

        // Phrase chains, heard, on medium sentences.
        openGame(R.string.game_phrase_chains)
        tap(R.string.option_medium)
        shot("72-chains-setup")
        tap(R.string.setup_play)
        shot("73-chains-heard")
        order().take(2).forEach(::tapCard)
        tapCard(checkNotNull(chain().sentence).cards.first { it !in order() })
        shot("74-chains-slip")
        screen.waitForIdle()
        screen.mainClock.autoAdvance = false
        order().drop(2).forEach(::tapCard)
        screen.mainClock.advanceTimeBy(500)
        shot("75-chains-whole")
        screen.mainClock.autoAdvance = true
        tap(R.string.action_continue)
        while (viewModel?.screen is Screen.Round) {
            order().forEach(::tapCard)
            tap(R.string.action_continue)
        }
        shot("76-chains-result")

        // Read, on long sentences.
        tap(R.string.result_done)
        openGame(R.string.game_phrase_chains)
        tap(R.string.option_read)
        tap(R.string.option_long)
        tap(R.string.setup_play)
        shot("77-chains-read")
        order().take(3).forEach(::tapCard)
        shot("78-chains-read-part")
        screen.onNodeWithContentDescription(app.getString(R.string.round_close)).performClick()

        // Listen and tap, by picture.
        openGame(R.string.game_listen_and_tap)
        shot("79-listen-setup")
        tap(R.string.setup_play)
        shot("80-listen-pictures")
        answer(correctly = true)
        shot("81-listen-pictures-right")
        moveOn()
        answer(correctly = false)
        shot("82-listen-pictures-wrong")
        moveOn()
        screen.onNodeWithContentDescription(app.getString(R.string.round_close)).performClick()

        // By meaning.
        openGame(R.string.game_listen_and_tap)
        tap(R.string.option_meaning)
        tap(R.string.setup_play)
        shot("83-listen-meanings")
        answer(correctly = false)
        shot("84-listen-meanings-wrong")
        moveOn()
        screen.onNodeWithContentDescription(app.getString(R.string.round_close)).performClick()

        // With sound off, Listen and tap is closed and says why.
        tap(R.string.nav_settings)
        tap(R.string.settings_sound)
        tap(R.string.nav_practice)
        screen.onNodeWithText(app.getString(R.string.game_listen_and_tap)).performScrollTo()
        shot("85-practice-sound-off")
    }

    @Test
    fun `Sound gaps over the real course in the light theme`() = soundGaps("light")

    @Test
    @Config(qualifiers = "+night")
    fun `Sound gaps over the real course in the dark theme`() = soundGaps("dark")

    @Test
    fun `Sound gaps over the real course at the largest font size`() = atTheLargestFontSize { soundGaps("large") }

    /** Sound gaps: before a Round, a question new, answered right and answered wrong, in three sounds, and the result. */
    private fun soundGaps(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        fun gapped() = (viewModel?.screen as Screen.Round).round as GapRound

        /** Chooses the right letters, or wrong ones, and holds the answered question for its picture. */
        fun answer(correctly: Boolean) {
            val asked = checkNotNull(gapped().question)
            val letters = asked.options.first { (it == asked.gap.answer) == correctly }
            screen.waitForIdle()
            screen.mainClock.autoAdvance = false
            screen.onNode(hasText(letters) and hasClickAction()).performScrollTo().performClick()
            screen.mainClock.advanceTimeBy(500)
        }

        fun moveOn() {
            screen.mainClock.autoAdvance = true
            tap(R.string.action_continue)
        }

        val course = loadCourse { path -> app.assets.open(path).bufferedReader().use { it.readText() } }
        val progress = InMemoryLearnerProgress()
        course.path.forEachIndexed { index, dailySet ->
            progress.recordCompleted(dailySet.id, clock.today.minusDays((course.path.size - 1 - index).toLong()))
        }
        val engine = CourseEngine(course, progress, clock, oneADay = { true })
        open(CourseViewModel(app, engine, engine.practice(InMemoryPracticeProgress(), Random(7))))
        tap(R.string.nav_practice)

        openGame(R.string.game_sound_gaps)
        shot("86-gaps-setup")
        tap(R.string.setup_play)
        shot("87-gaps-question")
        answer(correctly = true)
        shot("88-gaps-right")
        moveOn()
        answer(correctly = false)
        shot("89-gaps-wrong")
        moveOn()
        screen.onNodeWithContentDescription(app.getString(R.string.round_close)).performClick()

        // Two letters to choose from, and a phrase.
        openGame(R.string.game_sound_gaps)
        tap("l · ł")
        tap(R.string.setup_play)
        shot("90-gaps-two-letters")
        screen.onNodeWithContentDescription(app.getString(R.string.round_close)).performClick()
        openGame(R.string.game_sound_gaps)
        tap(R.string.option_mixed)
        tap(R.string.setup_play)
        while (' ' !in checkNotNull(gapped().question).gap.polish) {
            answer(correctly = true)
            moveOn()
        }
        shot("91-gaps-phrase")
        while (viewModel?.screen is Screen.Round) {
            answer(correctly = true)
            moveOn()
        }
        shot("92-gaps-result")
    }

    @Test
    fun `Chat over the real course in the light theme`() = chat("light")

    @Test
    @Config(qualifiers = "+night")
    fun `Chat over the real course in the dark theme`() = chat("dark")

    @Test
    fun `Chat over the real course at the largest font size`() = atTheLargestFontSize { chat("large") }

    /** Chat: before a Round, the person typing, a line with replies, a wrong reply put right, typing a reply, saying one, and the result. */
    private fun chat(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        fun round() = (viewModel?.screen as Screen.Round).round as ChatRound

        val course = loadCourse { path -> app.assets.open(path).bufferedReader().use { it.readText() } }

        fun right(): String = course.chats.first { it.person == round().person }.turns.first { it.says == round().lines.last().text }.replies.first { it.right }.text

        fun arrive() {
            screen.mainClock.advanceTimeBy(Motion.TYPING_MILLIS + 100)
            screen.waitForIdle()
        }

        fun choose(text: String) {
            screen.onNode(hasText(text) and hasClickAction()).performScrollTo().performClick()
            screen.mainClock.advanceTimeBy(300)
        }

        openRealPractice()
        openGame(R.string.game_chat)
        shot("136-chat-setup")
        tap("At the school office")
        tap(R.string.setup_play)
        screen.waitForIdle()
        shot("137-chat-typing")
        arrive()
        shot("138-chat-replies")
        choose("Cześć!")
        shot("139-chat-wrong")
        tap(R.string.action_continue)
        arrive()
        choose(right())
        tap(R.string.action_continue)
        arrive()
        shot("140-chat-going")
        while (viewModel?.screen is Screen.Round && round().replying) {
            choose(right())
            tap(R.string.action_continue)
            arrive()
        }
        screen.mainClock.advanceTimeBy(Motion.CHAT_DONE_MILLIS + 100)
        screen.waitForIdle()
        shot("141-chat-result")
        tap(R.string.result_done)

        // Typing, and saying.
        openGame(R.string.game_chat)
        tap("A new classmate")
        tap(R.string.option_type)
        tap(R.string.setup_play)
        arrive()
        type("Cześć! Jestem Adam.")
        shot("142-chat-type")
        leaveRound()
        openGame(R.string.game_chat)
        // With seven Chats listed, the ways to reply are below the fold at the largest font.
        screen.onNodeWithText(app.getString(R.string.option_say)).performScrollTo().performClick()
        tap(R.string.setup_play)
        arrive()
        shot("143-chat-say")
        screen.onNodeWithContentDescription(app.getString(R.string.speak_mic)).performClick()
        screen.waitForIdle()
        shot("144-chat-listening")
        screen.runOnIdle { speech.hear("czesc") }
        screen.mainClock.advanceTimeBy(300)
        shot("145-chat-misheard")
        tap(R.string.action_continue)
        arrive()
        screen.onNodeWithContentDescription(app.getString(R.string.speak_mic)).performClick()
        screen.runOnIdle { speech.hear("mam na imie adam") }
        screen.mainClock.advanceTimeBy(300)
        shot("146-chat-said")
        leaveRound()
    }

    /** Practice over the real course, for a Learner who has completed all of it, the last Daily Set today. */
    private fun openRealPractice() {
        val course = loadCourse { path -> app.assets.open(path).bufferedReader().use { it.readText() } }
        val progress = InMemoryLearnerProgress()
        course.path.forEachIndexed { index, dailySet ->
            progress.recordCompleted(dailySet.id, clock.today.minusDays((course.path.size - 1 - index).toLong()))
        }
        val engine = CourseEngine(course, progress, clock, oneADay = { true })
        open(CourseViewModel(app, engine, engine.practice(InMemoryPracticeProgress(), Random(7))))
        tap(R.string.nav_practice)
    }

    private fun leaveRound() {
        screen.mainClock.autoAdvance = true
        screen.onNodeWithContentDescription(app.getString(R.string.round_close)).performClick()
    }

    @Test
    fun `Numbers over the real course in the light theme`() = numbers("light")

    @Test
    @Config(qualifiers = "+night")
    fun `Numbers over the real course in the dark theme`() = numbers("dark")

    @Test
    fun `Numbers over the real course at the largest font size`() = atTheLargestFontSize { numbers("large") }

    /** Numbers: before a Round, a number heard, typed, right and wrong, a run, a sum read, and the result. */
    private fun numbers(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        fun round() = (viewModel?.screen as Screen.Round).round as NumberRound

        fun press(digit: Char) = screen.onNode(hasText(digit.toString()) and hasClickAction()).performScrollTo().performClick()

        /** Types the right answer, or a wrong one. */
        fun type(correctly: Boolean) {
            val answer = checkNotNull(round().question).answer
            (if (correctly) answer else if (answer == "12") "2" else "12").forEach(::press)
        }

        /** Checks what is typed and holds the answered question for its picture. */
        fun check() {
            screen.waitForIdle()
            screen.mainClock.autoAdvance = false
            screen.onNodeWithContentDescription(app.getString(R.string.action_check)).performScrollTo().performClick()
            screen.mainClock.advanceTimeBy(500)
        }

        fun moveOn() {
            screen.mainClock.autoAdvance = true
            tap(R.string.action_continue)
        }

        openRealPractice()
        openGame(R.string.game_numbers)
        tap(R.string.option_to_20)
        shot("93-numbers-setup")
        tap(R.string.setup_play)
        shot("94-numbers-heard")
        type(correctly = true)
        shot("95-numbers-typed")
        check()
        shot("96-numbers-right")
        moveOn()
        type(correctly = false)
        check()
        shot("97-numbers-wrong")
        leaveRound()

        openGame(R.string.game_numbers)
        tap(R.string.option_runs)
        tap(R.string.setup_play)
        type(correctly = true)
        check()
        shot("98-numbers-run")
        leaveRound()

        openGame(R.string.game_numbers)
        tap(R.string.option_read)
        tap(R.string.option_sums)
        tap(R.string.setup_play)
        shot("99-numbers-sum-read")
        while (viewModel?.screen is Screen.Round) {
            type(correctly = true)
            check()
            moveOn()
        }
        shot("100-numbers-result")
    }

    @Test
    fun `Mój, moja, moje over the real course in the light theme`() = mojMojaMoje("light")

    @Test
    @Config(qualifiers = "+night")
    fun `Mój, moja, moje over the real course in the dark theme`() = mojMojaMoje("dark")

    @Test
    fun `Mój, moja, moje over the real course at the largest font size`() = atTheLargestFontSize { mojMojaMoje("large") }

    /** Mój, moja, moje: before a Round, a noun with a picture and one without, right and wrong, by meaning, by ear, and the result. */
    private fun mojMojaMoje(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        fun round() = (viewModel?.screen as Screen.Round).round as GenderRound

        /** Taps the right word, or a wrong one, and holds the answered question for its picture. */
        fun answer(correctly: Boolean) {
            val asked = checkNotNull(round().question)
            val word = round().buckets.first { (it.gender == asked.gender) == correctly }.word
            screen.waitForIdle()
            screen.mainClock.autoAdvance = false
            screen.onNode(hasText(word) and hasClickAction()).performScrollTo().performClick()
            screen.mainClock.advanceTimeBy(500)
        }

        fun moveOn() {
            screen.mainClock.autoAdvance = true
            tap(R.string.action_continue)
        }

        /** Answers rightly until the noun shown has a picture, or has none. */
        fun moveToANoun(pictured: Boolean) {
            while ((checkNotNull(round().question).picture != null) != pictured) {
                answer(correctly = true)
                moveOn()
            }
        }

        openRealPractice()
        openGame(R.string.game_moj_moja_moje)
        shot("101-moj-setup")
        tap(R.string.setup_play)
        moveToANoun(pictured = true)
        shot("102-moj-question")
        answer(correctly = true)
        shot("103-moj-right")
        moveOn()
        moveToANoun(pictured = false)
        answer(correctly = false)
        shot("104-moj-wrong")
        leaveRound()

        openGame(R.string.game_moj_moja_moje)
        tap(R.string.option_meaning)
        tap(R.string.setup_play)
        moveToANoun(pictured = true)
        shot("105-moj-meaning")
        leaveRound()

        openGame(R.string.game_moj_moja_moje)
        tap(R.string.option_listen)
        tap(R.string.setup_play)
        shot("106-moj-heard")
        while (viewModel?.screen is Screen.Round) {
            answer(correctly = true)
            moveOn()
        }
        shot("107-moj-result")
    }

    @Test
    fun `Spell it over the real course in the light theme`() = spellIt("light")

    @Test
    @Config(qualifiers = "+night")
    fun `Spell it over the real course in the dark theme`() = spellIt("dark")

    @Test
    fun `Spell it over the real course at the largest font size`() = atTheLargestFontSize { spellIt("large") }

    /** Spell it: before a Round, a word new, part spelled, whole, whole after a slip, from a meaning, and the result. */
    private fun spellIt(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        fun round() = (viewModel?.screen as Screen.Round).round as SpellRound

        fun tapLetter(letter: String) = screen.onAllNodes(hasText(letter) and hasClickAction())[0].performScrollTo().performClick()

        /** Spells what is left of the word and holds it, whole, for its picture. */
        fun spell() {
            val word = checkNotNull(round().word)
            val left = word.letters.drop(round().placed.size)
            left.dropLast(1).forEach(::tapLetter)
            screen.waitForIdle()
            screen.mainClock.autoAdvance = false
            tapLetter(left.last())
            screen.mainClock.advanceTimeBy(500)
        }

        fun moveOn() {
            screen.mainClock.autoAdvance = true
            tap(R.string.action_continue)
        }

        openRealPractice()
        openGame(R.string.game_spell_it)
        shot("108-spell-setup")
        tap(R.string.setup_play)
        shot("109-spell-question")
        checkNotNull(round().word).letters.take(2).forEach(::tapLetter)
        shot("110-spell-part")
        spell()
        shot("111-spell-whole")
        moveOn()
        val second = checkNotNull(round().word)
        tapLetter(second.tiles.first { it != second.letters.first() })
        spell()
        shot("112-spell-slipped")
        leaveRound()

        openGame(R.string.game_spell_it)
        tap(R.string.option_meaning)
        tap(R.string.setup_play)
        // The longest word of the Round, to show its tiles wrap.
        val longest = round().words.maxOf { it.tiles.size }
        while (checkNotNull(round().word).tiles.size != longest) {
            spell()
            moveOn()
        }
        tapLetter(checkNotNull(round().word).letters.first())
        shot("113-spell-meaning")
        while (viewModel?.screen is Screen.Round) {
            spell()
            moveOn()
        }
        shot("114-spell-result")
    }

    @Test
    fun `the Quick round over the real course in the light theme`() = quickRound("light")

    @Test
    @Config(qualifiers = "+night")
    fun `the Quick round over the real course in the dark theme`() = quickRound("dark")

    @Test
    fun `the Quick round over the real course at the largest font size`() = atTheLargestFontSize { quickRound("large") }

    /** The Quick round: before a Round, a card with a picture, a wrong answer, part of the way through, with a meaning, and the result. */
    private fun quickRound(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        fun round() = (viewModel?.screen as Screen.Round).round as QuickRound

        fun answer(correctly: Boolean) {
            val matches = checkNotNull(round().card).matches
            tap(if (matches == correctly) R.string.quick_match else R.string.quick_no_match)
        }

        openRealPractice()
        openGame(R.string.game_quick_round)
        shot("115-quick-setup")
        tap(R.string.setup_play)
        shot("116-quick-card")
        repeat(4) { answer(correctly = true) }
        screen.mainClock.advanceTimeBy(21_000)
        shot("117-quick-under-way")
        answer(correctly = false)
        shot("118-quick-wrong")
        leaveRound()

        openGame(R.string.game_quick_round)
        tap(R.string.option_meaning)
        tap(R.string.setup_play)
        repeat(7) { answer(correctly = true) }
        screen.mainClock.advanceTimeBy(45_000)
        shot("119-quick-meaning")
        screen.mainClock.advanceTimeBy(15_000)
        shot("120-quick-result")
    }

    @Test
    fun `Register sort over the real course in the light theme`() = registerSort("light")

    @Test
    @Config(qualifiers = "+night")
    fun `Register sort over the real course in the dark theme`() = registerSort("dark")

    @Test
    fun `Register sort over the real course at the largest font size`() = atTheLargestFontSize { registerSort("large") }

    /** Register sort: before a Round, a phrase, part of the way through, a wrong answer, and the result. */
    @Test
    fun `Ankieta over the real course in the light theme`() = ankieta("light")

    @Test
    @Config(qualifiers = "+night")
    fun `Ankieta over the real course in the dark theme`() = ankieta("dark")

    @Test
    fun `Ankieta over the real course at the largest font size`() = atTheLargestFontSize { ankieta("large") }

    /** Ankieta: before a Round, a person read with the first field asked, a field put right, the filled form, heard, and the result. */
    private fun ankieta(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        fun round() = (viewModel?.screen as Screen.Round).round as AnkietaRound

        fun fill(correctly: Boolean = true) {
            val question = checkNotNull(round().question)
            val option = if (correctly) question.right else question.options.first { it != question.right }
            screen.onNode(hasText(option) and hasClickAction()).performScrollTo().performClick()
            screen.waitForIdle()
        }

        fun moveOn() {
            screen.mainClock.advanceTimeBy(Motion.MOVE_ON_MILLIS + 100)
            screen.waitForIdle()
        }

        openRealPractice()
        openGame(R.string.game_ankieta)
        shot("160-ankieta-setup")
        screen.onNodeWithText(app.getString(R.string.option_read)).performClick()
        tap(R.string.setup_play)
        shot("161-ankieta-read")
        fill()
        moveOn()
        fill(correctly = false)
        shot("162-ankieta-wrong")
        tap(R.string.action_continue)
        fill()
        moveOn()
        fill()
        moveOn()
        fill()
        shot("163-ankieta-filled")
        moveOn()
        repeat(10) {
            fill()
            moveOn()
        }
        shot("164-ankieta-result")
        tap(R.string.result_done)
        openGame(R.string.game_ankieta)
        screen.onNodeWithText(app.getString(R.string.option_listen)).performClick()
        tap(R.string.setup_play)
        shot("165-ankieta-heard")
    }

    @Test
    fun `Label the picture and Ten, ta, to over the real course in the light theme`() = chapter3Games("light")

    @Test
    @Config(qualifiers = "+night")
    fun `Label the picture and Ten, ta, to over the real course in the dark theme`() = chapter3Games("dark")

    @Test
    fun `Label the picture and Ten, ta, to over the real course at the largest font size`() = atTheLargestFontSize { chapter3Games("large") }

    /**
     * Chapter 3's Games. Label the picture: before a Round, the board with the first picture
     * asked, a label put right, the board half labelled, typing, and the result. Ten, ta, to: a
     * noun, and a noun sorted.
     */
    private fun chapter3Games(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        fun round() = (viewModel?.screen as Screen.Round).round as LabelRound

        fun label(correctly: Boolean = true) {
            val asked = checkNotNull(round().spot)
            val word = if (correctly) asked.polish else round().spots.first { it != asked && !round().board[round().spots.indexOf(it)].labelled }.polish
            screen.onNode(hasText(word) and hasClickAction()).performScrollTo().performClick()
            screen.waitForIdle()
        }

        fun moveOn() {
            screen.mainClock.advanceTimeBy(Motion.MOVE_ON_MILLIS + 100)
            screen.waitForIdle()
        }

        openRealPractice()
        openGame(R.string.game_label_it)
        shot("170-label-setup")
        tap(R.string.setup_play)
        shot("171-label-board")
        label()
        moveOn()
        label(correctly = false)
        shot("172-label-wrong")
        tap(R.string.action_continue)
        label()
        moveOn()
        shot("173-label-half")
        repeat(3) {
            label()
            moveOn()
        }
        shot("174-label-result")
        tap(R.string.result_done)
        openGame(R.string.game_label_it)
        screen.onNodeWithText(app.getString(R.string.option_type)).performScrollTo().performClick()
        tap(R.string.setup_play)
        screen.onNode(hasSetTextAction()).performTextInput(checkNotNull(round().spot).polish.dropLast(1))
        shot("175-label-type")
        leaveRound()

        openGame(R.string.game_ten_ta_to)
        tap(R.string.setup_play)
        screen.waitForIdle()
        shot("176-ten-ta-to")
        val sorting = (viewModel?.screen as Screen.Round).round as GenderRound
        val noun = checkNotNull(sorting.question)
        screen.onNode(hasText(sorting.buckets.first { it.gender == noun.gender }.word) and hasClickAction()).performScrollTo().performClick()
        screen.waitForIdle()
        shot("177-ten-ta-to-sorted")
    }

    private fun registerSort(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        fun round() = (viewModel?.screen as Screen.Round).round as RegisterRound

        fun words(register: Register) = app.getString(
            when (register) {
                Register.FORMAL -> R.string.register_sort_formal
                Register.INFORMAL -> R.string.register_sort_informal
                Register.UNIVERSAL -> R.string.register_sort_universal
            },
        )

        fun answer(correctly: Boolean) {
            val right = checkNotNull(round().card).register
            val register = if (correctly) right else round().registers.first { it != right }
            val count = round().sorted[register] ?: 0
            screen.onNodeWithContentDescription(app.getString(R.string.register_sort_button, register.mark, words(register), count)).performClick()
        }

        openRealPractice()
        openGame(R.string.game_register_sort)
        shot("149-register-setup")
        tap(R.string.setup_play)
        shot("150-register-phrase")
        repeat(5) { answer(correctly = true) }
        screen.mainClock.advanceTimeBy(18_000)
        shot("151-register-under-way")
        answer(correctly = false)
        shot("152-register-wrong")
        tap(R.string.action_continue)
        repeat(3) { answer(correctly = true) }
        screen.mainClock.advanceTimeBy(45_000)
        shot("153-register-result")
    }

    @Test
    fun `Spell it aloud over the real course in the light theme`() = spellAloud("light")

    @Test
    @Config(qualifiers = "+night")
    fun `Spell it aloud over the real course in the dark theme`() = spellAloud("dark")

    @Test
    fun `Spell it aloud over the real course at the largest font size`() = atTheLargestFontSize { spellAloud("large") }

    /** Spell it, spelled aloud: the choice before a Round, a word heard and part spelled, and the word whole with its meaning. */
    private fun spellAloud(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        fun round() = (viewModel?.screen as Screen.Round).round as SpellRound

        fun tapLetter(letter: String) = screen.onAllNodes(hasText(letter) and hasClickAction())[0].performScrollTo().performClick()

        openRealPractice()
        openGame(R.string.game_spell_it)
        tap(R.string.option_aloud)
        shot("154-spell-aloud-setup")
        tap(R.string.setup_play)
        val word = checkNotNull(round().word)
        word.letters.take(2).forEach(::tapLetter)
        shot("155-spell-aloud-part")
        word.letters.drop(2).dropLast(1).forEach(::tapLetter)
        screen.waitForIdle()
        screen.mainClock.autoAdvance = false
        tapLetter(word.letters.last())
        screen.mainClock.advanceTimeBy(500)
        shot("156-spell-aloud-whole")
        screen.mainClock.autoAdvance = true
    }

    @Test
    fun `Practice and a Round of the Verb challenge in the light theme`() = practiceTour("light")

    @Test
    @Config(qualifiers = "+night")
    fun `Practice and a Round of the Verb challenge in the dark theme`() = practiceTour("dark")

    @Test
    fun `Practice and a Round of the Verb challenge at the largest font size`() = atTheLargestFontSize { practiceTour("large") }

    /**
     * Practice locked and open, a Game before a Round, each stage of the Verb challenge chosen and
     * typed, right and wrong, and the result as a first Best and as a new one.
     */
    private fun practiceTour(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        fun round() = (viewModel?.screen as Screen.Round).round as VerbRound

        fun right() = checkNotNull(round().question).answers.first()

        fun wrong() = checkNotNull(round().question).options.first { it != right() }

        /** Answers with [form] and stays on the answered question, for its picture. */
        fun answer(form: String) {
            if (round().typing) {
                type(form)
                tap(R.string.action_check)
            } else {
                screen.onNode(hasText(form) and hasClickAction()).performClick()
            }
        }

        fun moveOn() {
            screen.waitForIdle()
            if (((viewModel?.screen as? Screen.Round)?.round as? VerbRound)?.given != null) tap(R.string.action_continue)
            screen.waitForIdle()
        }

        fun playOut() {
            while (viewModel?.screen is Screen.Round) {
                answer(right())
                moveOn()
            }
        }

        val engine = CourseEngine(fixtureCourse(), FileLearnerProgress(File(folder.root, "progress.json")), clock, oneADay = { true })
        open(CourseViewModel(app, engine, engine.practice(FilePracticeProgress(File(folder.root, "practice.json")), Random(7))))
        tap(R.string.nav_practice)
        shot("40-practice-locked")

        // The card that says what opens Practice starts today's Daily Set; it is played through.
        tap(R.string.home_start)
        tap(R.string.action_continue)
        tap("hello (polite); good morning")
        tap(R.string.action_continue)
        listOf("to", "jest", "Adam").forEach(::tap)
        tap(R.string.action_check)
        tap(R.string.action_continue)
        listOf("dziękuję", null, "nie", "jestem").forEach { typed ->
            if (typed == null) tap("do widzenia") else {
                type(typed)
                tap(R.string.action_check)
            }
            tap(R.string.action_continue)
        }
        tap(R.string.summary_back_home)
        screen.mainClock.advanceTimeBy(2_000)
        tap(R.string.nav_practice)
        shot("41-practice-open")

        openGame(R.string.game_verb_challenge)
        shot("42-game-setup")
        tap(R.string.setup_play)

        // The clock is held while an answered question is pictured, so a right answer does not move on by itself.
        shot("43-verb-pick")
        screen.mainClock.autoAdvance = false
        answer(wrong())
        screen.mainClock.advanceTimeBy(600)
        shot("44-verb-pick-wrong")
        screen.mainClock.autoAdvance = true
        moveOn()
        screen.mainClock.autoAdvance = false
        answer(right())
        screen.mainClock.advanceTimeBy(600)
        shot("45-verb-pick-right")
        screen.mainClock.autoAdvance = true
        moveOn()
        repeat(4) {
            answer(right())
            moveOn()
        }

        shot("46-verb-table")
        repeat(2) {
            answer(right())
            moveOn()
        }
        screen.mainClock.autoAdvance = false
        answer(wrong())
        screen.mainClock.advanceTimeBy(600)
        shot("47-verb-table-missed")
        screen.mainClock.autoAdvance = true
        moveOn()
        repeat(3) {
            answer(right())
            moveOn()
        }

        shot("48-verb-sentence")
        screen.mainClock.autoAdvance = false
        answer(right())
        screen.mainClock.advanceTimeBy(600)
        shot("49-verb-sentence-right")
        screen.mainClock.autoAdvance = true
        moveOn()
        shot("50-result-first-best")

        tap(R.string.result_play_again)
        playOut()
        shot("51-result-new-best")
        tap(R.string.result_done)
        shot("52-practice-played")

        // The same Game, typed: its own Best.
        openGame(R.string.game_verb_challenge)
        tap(R.string.option_type)
        shot("53-game-setup-type")
        tap(R.string.setup_play)
        type(right().dropLast(1))
        shot("54-verb-type")
        screen.mainClock.autoAdvance = false
        tap(R.string.action_check)
        screen.mainClock.advanceTimeBy(600)
        shot("55-verb-type-wrong")
        screen.mainClock.autoAdvance = true
        moveOn()
        screen.mainClock.autoAdvance = false
        answer(right())
        screen.mainClock.advanceTimeBy(600)
        shot("56-verb-type-right")
        screen.mainClock.autoAdvance = true
        moveOn()
        while (round().question?.stage != VerbStage.SENTENCE) {
            answer(right())
            moveOn()
        }
        type(right())
        shot("57-verb-sentence-typing")

        // Picture cards: the pictures, and two cards turned to their words.
        screen.onNodeWithContentDescription(app.getString(R.string.round_close)).performClick()
        openGame(R.string.game_picture_cards)
        shot("58-picture-cards")
        listOf("hello (polite); good morning", "no").forEach { screen.onNodeWithContentDescription(it).performClick() }
        shot("59-picture-cards-turned")
        tap(R.string.option_words)
        shot("59-picture-cards-words")
    }

    /** Taps a Game's card on the Practice screen, which may be below the fold at a large font size. */
    private fun openGame(name: Int) {
        screen.onNodeWithText(app.getString(name)).performScrollTo().performClick()
    }

    private fun open(over: CourseViewModel) {
        viewModel = over
        screen.setContent {
            Shown {
                viewModel?.let { key(it) { PolishEasyApp(it, clips, recordings, speech) } }
            }
        }
    }

    private fun atTheLargestFontSize(show: () -> Unit) {
        fontScale = LARGEST_FONT_SCALE
        show()
    }

    /** The app's theme, with text at [fontScale]: the scale is applied the way Android applies the phone's setting. */
    @Composable
    private fun Shown(content: @Composable () -> Unit) {
        val context = LocalContext.current
        val density = remember(fontScale) {
            val scaled = Configuration(context.resources.configuration).also { it.fontScale = fontScale }
            Density(context.createConfigurationContext(scaled))
        }
        CompositionLocalProvider(LocalDensity provides density) { PolishEasyTheme(content) }
    }

    /**
     * What a screen reader and a finger need of the screen as it stands: everything that can be
     * tapped says what it is, in words or a description, and is at least 48 dp each way.
     */
    private fun checkEverythingTappable(picture: String) {
        val least = with(screen.density) { 48.dp.toPx() } - 1f
        screen.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            if (node.config.getOrNull(SemanticsProperties.Disabled) != null) return@forEach
            val words = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
                node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
            val input = node.config.getOrNull(SemanticsProperties.EditableText) != null
            assertTrue(input || words.any { it.isNotBlank() }, "$picture: something tappable at ${node.boundsInRoot} has no name")
            val touch = node.touchBoundsInRoot
            // A thing partly scrolled out of sight is cut to what shows; its whole size is checked where it shows whole.
            val whole = node.boundsInRoot.width >= node.size.width - 1f && node.boundsInRoot.height >= node.size.height - 1f
            assertTrue(
                !whole || (touch.width >= least && touch.height >= least),
                "$picture: \"${words.firstOrNull()}\" is ${touch.width} x ${touch.height} px to a finger, under 48 dp",
            )
        }
    }

    private fun shot(name: String) {
        screen.waitForIdle()
        checkEverythingTappable(name)
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

    private fun back() {
        screen.runOnUiThread { screen.activity.onBackPressedDispatcher.onBackPressed() }
        screen.waitForIdle()
    }

    private companion object {
        /** Gradle runs the tests from the `app` directory. */
        val SCREENSHOTS = File("build/screenshots")

        /** The top of Android's font size setting. */
        const val LARGEST_FONT_SCALE = 2f
    }
}
