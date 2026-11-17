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
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.polisheasy.app.ui.PolishEasyApp
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.components.NavigationFrame
import com.polisheasy.app.ui.components.NavigationTab
import com.polisheasy.app.ui.screens.HomeScreen
import com.polisheasy.app.ui.screens.PathScreen
import com.polisheasy.app.ui.screens.SettingsScreen
import com.polisheasy.app.ui.screens.SummaryScreen
import com.polisheasy.engine.CourseEngine
import com.polisheasy.engine.DailySetState
import com.polisheasy.engine.DailySetView
import com.polisheasy.engine.DayView
import com.polisheasy.engine.FileLearnerProgress
import com.polisheasy.engine.LevelView
import com.polisheasy.engine.NextDailySetView
import com.polisheasy.engine.PathView
import com.polisheasy.engine.SetProgress
import com.polisheasy.engine.Today
import com.polisheasy.engine.UnitView
import java.io.File
import kotlin.random.Random
import com.polisheasy.engine.FilePracticeProgress
import com.polisheasy.engine.InMemoryLearnerProgress
import com.polisheasy.engine.InMemoryPracticeProgress
import com.polisheasy.engine.loadCourse
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
    fun `the Path part of the way along in the light theme`() = pathStates("light")

    @Test
    @Config(qualifiers = "+night")
    fun `the Path part of the way along in the dark theme`() = pathStates("dark")

    @Test
    fun `the Path part of the way along at the largest font size`() = atTheLargestFontSize { pathStates("large") }

    /** The Path with the real course's titles and made-up progress: today's Daily Set ready, then done, then every one completed. */
    private fun pathStates(theme: String) {
        val units = listOf(
            "Sounds and greetings" to listOf("How Polish is read", "sz and cz", "ę and ą", "ł", "ś and ć", "ź and dź", "The Polish alphabet"),
            "About me" to listOf(
                "Być: to be", "On, ona, ono", "The plural of być", "Czy and nie",
                "Mieć: to have", "The plural of mieć", "Mówić: to speak", "The plural of mówić",
            ),
            "Numbers and family" to listOf(
                "Counting to five", "Six to ten", "Mam … lat", "The teens",
                "Masculine and feminine", "Twój and twoja", "The third gender: neuter", "Babcia and dziadek",
            ),
        )

        /** The Path with the first [completed] Daily Sets done, and the one after them today's if [ready]. */
        fun path(completed: Int, ready: Boolean): PathView {
            var number = 0
            return PathView(
                listOf(
                    LevelView(
                        "a1",
                        "A1",
                        units.mapIndexed { index, (title, dailySets) ->
                            UnitView(
                                "a1-u0${index + 1}",
                                index + 1,
                                title,
                                dailySets.map { name ->
                                    val state = when {
                                        number < completed -> DailySetState.COMPLETED
                                        number == completed && ready -> DailySetState.TODAY
                                        else -> DailySetState.LOCKED
                                    }
                                    DailySetView("d${number + 1}", ++number, state, name)
                                },
                            )
                        },
                    ),
                ),
            )
        }
        val states = listOf(
            "27-path-today" to path(completed = 10, ready = true),
            "28-path-done-for-today" to path(completed = 11, ready = false),
            "29-path-unit-starts" to path(completed = 15, ready = true),
            "30-path-all-completed" to path(completed = 23, ready = false),
        )
        var showing by mutableStateOf(0)
        screen.setContent {
            Shown {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    key(showing) {
                        NavigationFrame(
                            tabs = listOf(
                                NavigationTab(app.getString(R.string.nav_home), R.drawable.ic_home),
                                NavigationTab(app.getString(R.string.nav_path), R.drawable.ic_route),
                                NavigationTab(app.getString(R.string.nav_practice), R.drawable.ic_stadia_controller),
                                NavigationTab(app.getString(R.string.nav_practice), R.drawable.ic_stadia_controller),
                            NavigationTab(app.getString(R.string.nav_settings), R.drawable.ic_settings),
                            ),
                            selected = 1,
                            onSelect = {},
                        ) {
                            PathScreen(
                                states[showing].second,
                                onReplay = {},
                                onOpenTodays = {},
                                today = NextDailySetView(2, "About me", 4, 8, teaches = listOf("Czy and nie", "Pan and pani")).takeIf { showing == 0 },
                            )
                        }
                    }
                }
            }
        }
        states.forEachIndexed { index, (name, _) ->
            showing = index
            shot("$theme-$name")
        }
        // A Unit still ahead is only its heading until it is tapped open.
        showing = 0
        screen.onNodeWithText("Numbers and family").performScrollTo()
        shot("$theme-27-path-unit-ahead")
        screen.onNodeWithText("Numbers and family").performClick()
        screen.onNodeWithText("The teens").performScrollTo()
        shot("$theme-27-path-unit-opened")
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

    /** Daily Set 1 of the fixture course, with one wrong answer so the Exercise comes back at the end. */
    private fun fixtureCourseTour(theme: String) {
        fun shot(name: String) = this.shot("$theme-$name")

        fun fixture() = CourseViewModel(app, CourseEngine(fixtureCourse(), FileLearnerProgress(File(folder.root, "progress.json")), clock))
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
        val engine = CourseEngine(course, progress, clock)
        open(CourseViewModel(app, engine, engine.practice(InMemoryPracticeProgress(), Random(7))))
        tap(R.string.nav_practice)
        shot("$theme-60-practice-real")
        tap(R.string.game_picture_cards)
        shot("$theme-61-picture-cards-real")
        screen.onNode(hasText(course.levels.first().units.last().title.getValue("en")) and hasClickAction()).performClick()
        shot("$theme-62-picture-cards-real-unit")
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

        val engine = CourseEngine(fixtureCourse(), FileLearnerProgress(File(folder.root, "progress.json")), clock)
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

        tap(R.string.game_verb_challenge)
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
        tap(R.string.game_verb_challenge)
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
        tap(R.string.game_picture_cards)
        shot("58-picture-cards")
        listOf("hello (polite); good morning", "no").forEach { screen.onNodeWithContentDescription(it).performClick() }
        shot("59-picture-cards-turned")
        tap(R.string.option_words)
        shot("59-picture-cards-words")
    }

    private fun open(over: CourseViewModel) {
        viewModel = over
        screen.setContent {
            Shown {
                viewModel?.let { key(it) { PolishEasyApp(it, clips) } }
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

    private fun back() = screen.runOnUiThread { screen.activity.onBackPressedDispatcher.onBackPressed() }

    private companion object {
        /** Gradle runs the tests from the `app` directory. */
        val SCREENSHOTS = File("build/screenshots")

        /** The top of Android's font size setting. */
        const val LARGEST_FONT_SCALE = 2f
    }
}
