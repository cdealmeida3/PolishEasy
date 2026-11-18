package com.polisheasy.app

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performTextInput
import com.polisheasy.app.RecordingClips.Played
import com.polisheasy.app.ui.PolishEasyApp
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.engine.CourseEngine
import com.polisheasy.engine.FileLearnerProgress
import com.polisheasy.engine.FilePracticeProgress
import com.polisheasy.engine.AnkietaRound
import com.polisheasy.engine.ChainRound
import com.polisheasy.engine.GapRound
import com.polisheasy.engine.Gender
import com.polisheasy.engine.GenderRound
import com.polisheasy.engine.LabelRound
import com.polisheasy.engine.NumberRound
import com.polisheasy.engine.OppositesRound
import com.polisheasy.engine.WhoRound
import com.polisheasy.engine.VerbGroupRound
import com.polisheasy.engine.HobbyRound
import com.polisheasy.engine.LottoRound
import com.polisheasy.engine.TillRound
import com.polisheasy.engine.QuickRound
import com.polisheasy.engine.Register
import com.polisheasy.engine.RegisterRound
import com.polisheasy.engine.SpellRound
import com.polisheasy.engine.InMemoryLearnerProgress
import com.polisheasy.engine.InMemoryPracticeProgress
import com.polisheasy.engine.ListenRound
import com.polisheasy.engine.MemoryFace
import com.polisheasy.engine.loadCourse
import com.polisheasy.engine.MemoryRound
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
        // A locked Game does not open: its card is there, further down, and tapping it changes nothing.
        openGame(R.string.game_verb_challenge)
        assertTrue(viewModel?.screen is Screen.Practice)

        // The card that says what opens Practice starts the Daily Set.
        screen.onNodeWithText(text(R.string.home_start)).performScrollTo().performClick()
        shows(R.string.tip_label)
        playDailySetOne()
        tap(R.string.summary_back_home)

        tap(R.string.nav_practice)
        absent(R.string.practice_locked_title)
        shows(R.string.practice_open_note)
        openGame(R.string.game_verb_challenge)
        shows(R.string.setup_play)
    }

    @Test
    fun `a Round of the Verb challenge is played from its setup to its result, which becomes the Best`() {
        openPracticeAfterTodaysDailySet()
        openGame(R.string.game_verb_challenge)
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
        showsFurtherDown(text(R.string.practice_best_played, right(12), "być, ${text(R.string.option_choose)}"))
    }

    @Test
    fun `a better Round is a new Best, and a worse one leaves the Best as it was`() {
        openPracticeAfterTodaysDailySet()
        openGame(R.string.game_verb_challenge)
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
        openGame(R.string.game_verb_challenge)
        tap(R.string.setup_play)
        playOut()
        tap(R.string.result_done)

        openGame(R.string.game_verb_challenge)
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
        openGame(R.string.game_verb_challenge)
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
        openGame(R.string.game_verb_challenge)
        tap(R.string.setup_play)
        answerRight()
        screen.onNodeWithContentDescription(text(R.string.round_close)).performClick()

        shows(R.string.practice_open_note)
        screen.onNodeWithText(app.resources.getQuantityString(R.plurals.practice_rounds_today, 1, 1)).assertDoesNotExist()
        openGame(R.string.game_verb_challenge)
        shows(R.string.setup_no_best)
        // Back from the setup is Practice again.
        back()
        shows(R.string.practice_open_note)
    }

    @Test
    fun `the Best is kept when the app is closed, and Practice locks again the next day`() {
        openPracticeAfterTodaysDailySet()
        openGame(R.string.game_verb_challenge)
        tap(R.string.setup_play)
        playOut()
        tap(R.string.result_done)

        closeAndOpenApp()
        tap(R.string.nav_practice)
        showsFurtherDown(text(R.string.practice_best_played, right(13), "być, ${text(R.string.option_choose)}"))

        clock.moveToNextDay()
        closeAndOpenApp()
        tap(R.string.nav_practice)
        shows(R.string.practice_locked_title)
        screen.onNodeWithText(app.resources.getQuantityString(R.plurals.practice_rounds_today, 1, 1)).assertDoesNotExist()
    }

    @Test
    fun `a sentence is said whole as it is answered, and nothing is said with Sound off`() {
        openPracticeAfterTodaysDailySet()
        openGame(R.string.game_verb_challenge)
        tap(R.string.setup_play)
        clips.asked.clear()
        playOut()
        assertEquals(listOf(Played("audio/jestem-adam.ogg", slow = false)), clips.played)
        tap(R.string.result_done)

        tap(R.string.nav_settings)
        tap(R.string.settings_sound)
        tap(R.string.nav_practice)
        openGame(R.string.game_verb_challenge)
        tap(R.string.setup_play)
        clips.asked.clear()
        playOut()
        assertTrue(clips.played.isEmpty())
    }

    @Test
    fun `Picture cards shows the pictures of the words learned, and a card turns to show its word and say it`() {
        openPracticeAfterTodaysDailySet()
        openGame(R.string.game_picture_cards)

        // Daily Set 1 taught four words that have a picture; a picture is named by what it means.
        listOf("hello (polite); good morning", "thank you", "goodbye", "no").forEach { meaning ->
            screen.onNodeWithContentDescription(meaning).assertIsDisplayed()
        }
        screen.onNodeWithText("dzień dobry").assertDoesNotExist()
        clips.asked.clear()

        screen.onNodeWithContentDescription("hello (polite); good morning").performClick()
        shows("dzień dobry")
        val hello = Played("audio/dzien-dobry.ogg", slow = false)
        assertEquals(listOf(hello), clips.played)
        screen.onNodeWithContentDescription(text(R.string.audio_play)).performClick()
        assertEquals(listOf(hello, hello), clips.played)

        // Tapped again, it turns back to its picture.
        // (The card's own tap, not a touch in its middle, where the speaker is.)
        screen.onNodeWithText("dzień dobry").performSemanticsAction(SemanticsActions.OnClick)
        screen.onNodeWithText("dzień dobry").assertDoesNotExist()
        screen.onNodeWithContentDescription("hello (polite); good morning").assertIsDisplayed()

        // "All words" adds the words that have no picture, each shown by what it means.
        screen.onNodeWithText("I am").assertDoesNotExist()
        tap(R.string.option_words)
        tap("I am")
        shows("jestem")

        // Looking at cards is not a Round.
        back()
        shows(R.string.practice_open_note)
        screen.onNodeWithText(app.resources.getQuantityString(R.plurals.practice_rounds_today, 1, 1)).assertDoesNotExist()
    }

    @Test
    fun `with Sound off a Picture card turns in silence and has no speaker`() {
        openPracticeAfterTodaysDailySet()
        tap(R.string.nav_settings)
        tap(R.string.settings_sound)
        tap(R.string.nav_practice)
        openGame(R.string.game_picture_cards)
        clips.asked.clear()

        screen.onNodeWithContentDescription("thank you").performClick()

        shows("dziękuję")
        assertTrue(clips.played.isEmpty())
        screen.onNodeWithContentDescription(text(R.string.audio_play)).assertDoesNotExist()
    }

    @Test
    fun `Memory pairs is played by turning tiles two at a time, and the fewest turns is the Best`() {
        openPracticeAfterTodaysDailySet()
        openGame(R.string.game_memory_pairs)
        // Four pictures are too few for a board of them, and four Clips for one of sounds: the pairs are a word and its meaning.
        absent(R.string.option_picture_word)
        absent(R.string.option_sound_word)
        shows(R.string.option_word_meaning)
        tap(R.string.setup_play)
        shows(R.string.memory_instruction)
        shows(turns(0))
        clips.asked.clear()

        // Two tiles that are not a pair: a word, said as it turns, and the meaning of another.
        val word = board().tiles.indexOfFirst { it.face is MemoryFace.Word }
        val polish = (board().tiles[word].face as MemoryFace.Word).polish
        val other = board().tiles.indexOfFirst { it.pair != board().tiles[word].pair && it.face is MemoryFace.Meaning }
        val meaning = (board().tiles[other].face as MemoryFace.Meaning).text
        screen.waitForIdle()
        screen.mainClock.autoAdvance = false
        turn(word)
        screen.mainClock.advanceTimeBy(400)
        shows(polish)
        assertEquals(listOf(Played(checkNotNull(viewModel?.practiceClipOf(polish)).path, slow = false)), clips.played)
        turn(other)
        screen.mainClock.advanceTimeBy(400)
        shows(meaning)
        shows(turns(1))
        // After a moment they turn back.
        screen.mainClock.advanceTimeBy(Motion.MISS_MILLIS + 400)
        screen.onNodeWithText(polish).assertDoesNotExist()
        screen.onNodeWithText(meaning).assertDoesNotExist()
        screen.mainClock.autoAdvance = true

        // A pair found stays up; when every pair is, the Round ends with the turns it took.
        findEveryPair()
        screen.onNodeWithContentDescription(quantity(R.plurals.result_turns_sentence, 7)).assertIsDisplayed()
        showsFirstBest()

        // Fewer turns is a new Best.
        tap(R.string.result_play_again)
        findEveryPair()
        screen.onNodeWithContentDescription(quantity(R.plurals.result_turns_sentence, 6)).assertIsDisplayed()
        shows(R.string.result_new_best)
        tap(R.string.result_done)
        val played = listOf(text(R.string.option_word_meaning), text(R.string.option_all), text(R.string.option_pairs, 6)).joinToString(", ")
        shows(text(R.string.practice_best_played, quantity(R.plurals.practice_turns, 6), played))
    }

    @Test
    fun `with Sound off the tiles of Memory pairs turn in silence`() {
        openPracticeAfterTodaysDailySet()
        tap(R.string.nav_settings)
        tap(R.string.settings_sound)
        tap(R.string.nav_practice)
        openGame(R.string.game_memory_pairs)
        tap(R.string.setup_play)
        clips.asked.clear()

        findEveryPair()

        shows(R.string.result_title)
        assertTrue(clips.played.isEmpty())
    }

    @Test
    fun `Home offers a Game once today's Daily Set is done`() {
        openApp()
        absent(R.string.home_practice)
        tap(R.string.home_start)
        playDailySetOne()
        tap(R.string.summary_back_home)

        tap(R.string.home_practice)

        shows(R.string.practice_open_note)
    }

    @Test
    fun `Listen and tap says a word, and the picture tapped is marked right or wrong`() {
        openPracticeAfterTodaysDailySet()
        clips.asked.clear()
        openGame(R.string.game_listen_and_tap)
        shows(R.string.option_picture)
        shows(R.string.option_meaning)
        tap(R.string.setup_play)

        // The word is said as the question appears, and is not written.
        screen.waitForIdle()
        val first = checkNotNull(listening().question)
        val said = Played(checkNotNull(viewModel?.practiceClipOf(first.polish)).path, slow = false)
        assertEquals(listOf(said), clips.played)
        screen.onNodeWithText(first.polish).assertDoesNotExist()
        // The play button says it again.
        screen.onNodeWithContentDescription(text(R.string.audio_play)).performClick()
        assertEquals(listOf(said, said), clips.played)

        // A wrong picture: the word is written, and the Round waits for "Continue".
        screen.onNodeWithContentDescription(first.options.first { it.itemId != first.options[first.answer].itemId }.meaning).performClick()
        shows(first.polish)
        tap(R.string.action_continue)

        // The rest right: each moves on by itself, or by "Continue".
        while (viewModel?.screen is Screen.Round) {
            val asked = checkNotNull(listening().question)
            screen.onNodeWithContentDescription(asked.options[asked.answer].meaning).performClick()
            tap(R.string.action_continue)
        }
        showsScore(3, 4)
        showsFirstBest()
    }

    @Test
    fun `Listen and tap by meaning shows four meanings to tap`() {
        openPracticeAfterTodaysDailySet()
        openGame(R.string.game_listen_and_tap)
        tap(R.string.option_meaning)
        tap(R.string.setup_play)

        val asked = checkNotNull(listening().question)
        asked.options.forEach { shows(it.meaning) }
        tap(asked.options[asked.answer].meaning)
        shows(asked.polish)
    }

    @Test
    fun `with Sound off Listen and tap stays closed and says why`() {
        openPracticeAfterTodaysDailySet()
        tap(R.string.nav_settings)
        tap(R.string.settings_sound)
        tap(R.string.nav_practice)

        // Both Games that are only listening say so.
        screen.onAllNodesWithText(text(R.string.practice_needs_sound)).assertCountEquals(2)
        openGame(R.string.game_listen_and_tap)
        assertTrue(viewModel?.screen is Screen.Practice)
        openGame(R.string.game_sound_gaps)
        assertTrue(viewModel?.screen is Screen.Practice)
    }

    @Test
    fun `Sound gaps says a word and shows it with a gap, and the letters chosen fill it or are marked wrong`() {
        openPracticeAfterTodaysDailySet()
        clips.asked.clear()
        openGame(R.string.game_sound_gaps)
        tap(R.string.setup_play)
        screen.waitForIdle()

        // The word is said, and written with a gap in place of the letters asked for.
        val first = checkNotNull(gapped().question)
        val said = Played(checkNotNull(viewModel?.practiceClipOf(first.gap.polish)).path, slow = false)
        assertEquals(listOf(said), clips.played)
        screen.onNodeWithContentDescription(text(R.string.gaps_word, first.gap.before, first.gap.after)).assertIsDisplayed()
        shows(first.meaning)

        // Wrong letters: the gap is filled with the right ones, the word is said again, and the Round waits.
        tapLetters(first.options.first { it != first.gap.answer })
        screen.onNodeWithContentDescription(first.gap.polish).assertIsDisplayed()
        assertEquals(listOf(said, said), clips.played)
        tap(R.string.action_continue)

        while (viewModel?.screen is Screen.Round) {
            tapLetters(checkNotNull(gapped().question).gap.answer)
            tap(R.string.action_continue)
        }
        showsScore(3, 4)
        showsFirstBest()
    }

    @Test
    fun `Numbers says a number, and what is typed on the keypad is checked`() {
        openPracticeOverTheRealCourse()
        clips.asked.clear()
        openGame(R.string.game_numbers)
        tap(R.string.setup_play)
        screen.waitForIdle()

        // The number is said, and nothing is typed yet: there is nothing to check.
        val first = checkNotNull(numbers().question)
        assertEquals(first.words.map { Played(checkNotNull(viewModel?.practiceClipOf(it)).path, slow = false) }, clips.played)
        screen.onNodeWithContentDescription(text(R.string.numbers_typed_nothing)).assertIsDisplayed()
        screen.onNodeWithContentDescription(text(R.string.action_check)).assertIsNotEnabled()
        screen.onNodeWithText(first.polish).assertDoesNotExist()

        // A digit typed can be taken back.
        val wrong = if (first.answer == "1") "2" else "1"
        press("9")
        screen.onNodeWithContentDescription(text(R.string.numbers_typed, "9")).assertIsDisplayed()
        screen.onNodeWithContentDescription(text(R.string.keypad_erase)).performClick()
        press(wrong)
        screen.onNodeWithContentDescription(text(R.string.action_check)).performClick()

        // Wrong: the number is written out, in numerals and in words, and the Round waits.
        shows(text(R.string.numbers_answer, first.numerals, first.polish))
        screen.onNodeWithContentDescription(text(R.string.keypad_erase)).assertIsNotEnabled()
        tap(R.string.action_continue)

        typeTheRest()
        showsScore(9, 10)
        showsFirstBest()
    }

    @Test
    fun `Numbers from 20 to 100 says a ten and its unit one after the other`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_numbers)
        tap(R.string.option_listen)
        screen.onNodeWithText(text(R.string.option_to_100)).performScrollTo().performClick()
        clips.asked.clear()
        tap(R.string.setup_play)
        screen.waitForIdle()

        val round = numbers()
        assertTrue(round.run.questions.all { it.answer.toInt() in 20..100 })
        val first = checkNotNull(round.question)
        assertEquals(first.words.map { Played(checkNotNull(viewModel?.practiceClipOf(it)).path, slow = false) }, clips.played)
        typeTheRest()
        showsScore(10, 10)
    }

    @Test
    fun `Who's who shows a job, and the Learner finishes on jest with its instrumental`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_whos_who)
        tap(R.string.option_word)
        tap(R.string.setup_play)
        screen.waitForIdle()

        val total = whosWho().total
        val first = checkNotNull(whosWho().question)
        shows(R.string.whos_who_instruction)
        // The word is written above, and is one of the options too.
        screen.onAllNodesWithText(first.word).assertCountEquals(2)
        shows(first.meaning)
        shows(text(if (first.gender == Gender.MASCULINE) R.string.whos_who_he else R.string.whos_who_she))
        first.options.forEach { option -> screen.onNode(hasText(option) and hasClickAction()).assertExists() }

        // The word as it is written alone is wrong after jest: the instrumental is marked, and why.
        screen.onNode(hasText(first.word) and hasClickAction()).performClick()
        val sentence = text(if (first.gender == Gender.MASCULINE) R.string.whos_who_he_is else R.string.whos_who_she_is, first.answer)
        shows(sentence)
        shows(text(R.string.whos_who_after_jest, first.answer, first.word))
        tap(R.string.action_continue)

        while (viewModel?.screen is Screen.Round) {
            val asked = checkNotNull(whosWho().question)
            screen.onNode(hasText(asked.answer) and hasClickAction()).performClick()
            tap(R.string.action_continue)
        }
        showsScore(total - 1, total)
    }

    @Test
    fun `Three groups shows a verb, and the Learner sorts it by how ja and ty end`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_verb_groups)
        tap(R.string.option_with_ja)
        tap(R.string.setup_play)
        screen.waitForIdle()

        val round = verbGroups()
        val total = round.total
        val first = checkNotNull(round.question)
        shows(R.string.verb_groups_instruction)
        shows(first.verb)
        shows(first.meaning)
        shows(text(R.string.verb_groups_ja, first.ja))
        round.groups.forEach { group -> screen.onNode(hasText(group.endings) and hasClickAction()).assertExists() }

        // A wrong group: the right one is marked, and ja and ty are written.
        val wrong = round.groups.first { it != first.group }
        screen.onNode(hasText(wrong.endings) and hasClickAction()).performClick()
        shows(text(R.string.verb_groups_forms, first.ja, first.ty))
        tap(R.string.action_continue)

        while (viewModel?.screen is Screen.Round) {
            val asked = checkNotNull(verbGroups().question)
            screen.onNode(hasText(asked.group.endings) and hasClickAction()).performClick()
            tap(R.string.action_continue)
        }
        showsScore(total - 1, total)
    }

    @Test
    fun `Hobby charades shows a hobby's picture, and the Learner names it by choosing or typing`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_hobby_charades)
        tap(R.string.setup_play)
        screen.waitForIdle()

        val total = hobbies().total
        val first = checkNotNull(hobbies().question)
        shows(R.string.hobby_instruction)
        assertEquals(4, first.options.size)
        first.options.forEach { option -> screen.onNode(hasText(option) and hasClickAction()).assertExists() }

        // A wrong hobby: the right one is marked, and said with Lubię.
        screen.onNode(hasText(first.options.first { it != first.answer }) and hasClickAction()).performClick()
        shows(text(R.string.hobby_lubie, first.answer))
        tap(R.string.action_continue)

        while (viewModel?.screen is Screen.Round) {
            val asked = checkNotNull(hobbies().question)
            screen.onNode(hasText(asked.answer) and hasClickAction()).performScrollTo().performClick()
            tap(R.string.action_continue)
        }
        showsScore(total - 1, total)
        back()

        // Typed: the hobby is written in the field, accents and all.
        openGame(R.string.game_hobby_charades)
        tap(R.string.option_type)
        tap(R.string.setup_play)
        screen.waitForIdle()
        val typed = checkNotNull(hobbies().question)
        type(typed.answer)
        screen.onNodeWithText(text(R.string.action_check)).performClick()
        shows(text(R.string.hobby_lubie, typed.answer))
        assertEquals(true, hobbies().given?.correct)
    }

    @Test
    fun `Kiosk till writes a price, and the Learner pays it exactly with coins, taking back one too many`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_kiosk_till)
        tap(R.string.option_read)
        tap(R.string.setup_play)
        screen.waitForIdle()

        val total = till().total
        val first = checkNotNull(till().question)
        shows(R.string.till_read_instruction)
        shows(first.polish)
        shows(R.string.till_counter_empty)
        assertEquals(listOf(10, 20, 50, 100, 200, 500), till().money)

        // One coin too many, taken back from the counter, where it comes first.
        payWith(first.price)
        screen.onAllNodes(hasText(moneyLabel(10)) and hasClickAction()).onLast().performScrollTo().performClick()
        assertEquals(first.price + 10, till().onCounter)
        screen.onAllNodes(hasText(moneyLabel(10)) and hasClickAction()).onFirst().performScrollTo().performClick()
        assertEquals(first.price, till().onCounter)
        tap(R.string.till_pay)
        assertEquals(true, till().given?.correct)
        tap(R.string.action_continue)

        // Too little is wrong.
        val second = checkNotNull(till().question)
        payWith(second.price - 10)
        tap(R.string.till_pay)
        assertEquals(false, till().given?.correct)
        tap(R.string.action_continue)

        while (viewModel?.screen is Screen.Round) {
            payWith(checkNotNull(till().question).price)
            tap(R.string.till_pay)
            tap(R.string.action_continue)
        }
        showsScore(total - 1, total)
    }

    @Test
    fun `Lotto calls a number in words, and the Learner ticks it on the card`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_lotto)
        tap(R.string.option_read)
        screen.onNodeWithText(text(R.string.option_to_1000)).performScrollTo().performClick()
        tap(R.string.setup_play)
        screen.waitForIdle()

        val total = lotto().total
        val first = checkNotNull(lotto().question)
        shows(R.string.lotto_read_instruction)
        shows(first.polish)
        lotto().card.forEach { number -> screen.onNode(hasText("$number") and hasClickAction()).assertExists() }

        // A wrong number: the one called is marked, and stays ticked.
        val wrong = lotto().card.first { it != first.number }
        screen.onNode(hasText("$wrong") and hasClickAction()).performScrollTo().performClick()
        assertEquals(false, lotto().given?.correct)
        tap(R.string.action_continue)
        assertEquals(setOf(first.number), lotto().ticked)

        while (viewModel?.screen is Screen.Round) {
            screen.onNode(hasText("${checkNotNull(lotto().question).number}") and hasClickAction()).performScrollTo().performClick()
            tap(R.string.action_continue)
        }
        showsScore(total - 1, total)
    }

    @Test
    fun `Numbers read shows the words and says nothing, and a run is three numbers in a row`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_numbers)
        tap(R.string.option_read)
        tap(R.string.option_sums)
        clips.asked.clear()
        tap(R.string.setup_play)
        screen.waitForIdle()

        val sum = checkNotNull(numbers().question)
        shows(R.string.numbers_sum_read_instruction)
        shows(sum.polish)
        assertEquals(emptyList(), clips.played)
        sum.answer.forEach { press(it.toString()) }
        screen.onNodeWithContentDescription(text(R.string.action_check)).performClick()
        shows(sum.numerals)
        back()

        openGame(R.string.game_numbers)
        tap(R.string.option_listen)
        tap(R.string.option_runs)
        clips.asked.clear()
        tap(R.string.setup_play)
        screen.waitForIdle()
        shows(R.string.numbers_run_listen_instruction)
        assertEquals(3, clips.played.size)
        typeTheRest()
        showsScore(6, 6)
    }

    @Test
    fun `Mój, moja, moje shows a noun, and the word tapped is marked with the two written together`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_moj_moja_moje)
        clips.asked.clear()
        tap(R.string.setup_play)
        screen.waitForIdle()

        // The noun is written with its meaning, and is not said until it is answered.
        val first = checkNotNull(sorting().question)
        showsNoun(first.polish, first.meaning)
        screen.onAllNodesWithText(first.meaning).onFirst().assertIsDisplayed()
        assertEquals(emptyList(), clips.played)
        assertEquals(3, sorting().buckets.size)

        // The wrong word: the right one is marked, the two words are written together and said.
        tapBucket(sorting().buckets.first { it.gender != first.gender }.word)
        shows(first.phrase)
        val said = checkNotNull(viewModel?.practiceClipOf(first.phrase) ?: viewModel?.practiceClipOf(first.polish))
        assertEquals(listOf(Played(said.path, slow = false)), clips.played)
        // The noun is counted under the word it belongs to.
        screen.onNodeWithContentDescription(quantity(R.plurals.bucket_holds, 1), useUnmergedTree = true).assertIsDisplayed()
        tap(R.string.action_continue)

        while (viewModel?.screen is Screen.Round) {
            val asked = checkNotNull(sorting().question)
            tapBucket(sorting().buckets.first { it.gender == asked.gender }.word)
            tap(R.string.action_continue)
        }
        showsScore(9, 10)
        showsFirstBest()
    }

    @Test
    fun `Ten, ta, to sorts a noun under the word for this, and writes the two together`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_ten_ta_to)
        tap(R.string.setup_play)
        screen.waitForIdle()

        shows(R.string.ten_instruction)
        assertEquals(listOf("ten", "ta", "to"), sorting().buckets.map { it.word })
        val first = checkNotNull(sorting().question)
        showsNoun(first.polish, first.meaning)
        tapBucket(sorting().buckets.first { it.gender == first.gender }.word)
        shows(first.phrase)
        assertEquals(sorting().buckets.first { it.gender == first.gender }.word, first.phrase.substringBefore(' '))
    }

    @Test
    fun `Label the picture marks a picture to label, a right label sticks and is said, and a wrong one puts the right one on`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_label_it)
        clips.asked.clear()
        tap(R.string.setup_play)
        screen.waitForIdle()

        // The board: the first picture asked, the rest not labelled yet, every label lying below.
        val round = labelling()
        shows(R.string.label_choose_instruction)
        shows(round.spots[0].meaning)
        screen.onNodeWithContentDescription(text(R.string.label_picture_asked, round.spots[0].meaning)).assertExists()
        round.spots.drop(1).forEach { screen.onNodeWithContentDescription(text(R.string.label_picture_blank, it.meaning)).assertExists() }
        round.spots.forEach { screen.onNode(hasText(it.polish) and hasClickAction()).assertExists() }

        // Right: the label sticks in green and is said, and the next picture is asked by itself.
        tapLabel(round.spots[0].polish)
        screen.onNodeWithContentDescription(text(R.string.label_picture_right, round.spots[0].meaning, round.spots[0].polish)).assertExists()
        viewModel?.practiceClipOf(round.spots[0].polish)?.let { assertEquals(listOf(Played(it.path, slow = false)), clips.played) }
        screen.onNode(hasText(round.spots[0].polish) and hasClickAction()).assertDoesNotExist()
        screen.mainClock.advanceTimeBy(Motion.MOVE_ON_MILLIS + 100)
        screen.waitForIdle()

        // Wrong: the right label goes on in amber, and the one tapped stays below.
        tapLabel(round.spots[2].polish)
        screen.onNodeWithContentDescription(text(R.string.label_picture_missed, round.spots[1].meaning, round.spots[1].polish)).assertExists()
        tap(R.string.action_continue)
        screen.onNode(hasText(round.spots[2].polish) and hasClickAction()).assertExists()

        while (viewModel?.screen is Screen.Round) {
            tapLabel(checkNotNull(labelling().spot).polish)
            screen.mainClock.advanceTimeBy(Motion.MOVE_ON_MILLIS + 100)
            screen.waitForIdle()
        }
        showsScore(5, 6)
        showsFirstBest()
    }

    @Test
    fun `typed, Label the picture checks the word typed for the marked picture`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_label_it)
        tap(R.string.option_type)
        tap(R.string.setup_play)
        screen.waitForIdle()

        shows(R.string.label_type_instruction)
        val first = checkNotNull(labelling().spot)
        type(first.polish)
        tap(R.string.action_check)
        screen.onNodeWithContentDescription(text(R.string.label_picture_right, first.meaning, first.polish)).assertExists()
    }

    @Test
    fun `Mój, moja, moje can give the noun by its meaning alone, or by ear`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_moj_moja_moje)
        tap(R.string.option_meaning)
        tap(R.string.setup_play)

        val first = checkNotNull(sorting().question)
        shows(first.meaning)
        // Only the meaning, which is all that holds the noun's letters when it is written the same (sport is "sport").
        screen.onAllNodesWithText(first.polish).assertCountEquals(if (first.polish in first.meaning) 1 else 0)
        tapBucket(sorting().buckets.first { it.gender == first.gender }.word)
        shows(first.phrase)
        back()

        openGame(R.string.game_moj_moja_moje)
        tap(R.string.option_listen)
        clips.asked.clear()
        tap(R.string.setup_play)
        screen.waitForIdle()
        val heard = checkNotNull(sorting().question)
        assertEquals(listOf(Played(checkNotNull(viewModel?.practiceClipOf(heard.polish)).path, slow = false)), clips.played)
        screen.onNodeWithText(heard.polish).assertDoesNotExist()
        screen.onNodeWithText(heard.meaning).assertDoesNotExist()
    }

    @Test
    fun `Spell it lays out a word's letters, and tapped in order they spell it`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_spell_it)
        tap(R.string.option_meaning)
        clips.asked.clear()
        tap(R.string.setup_play)

        val first = checkNotNull(spelling().word)
        shows(first.meaning)
        screen.onNodeWithContentDescription(text(R.string.spell_empty, first.tiles.size)).assertIsDisplayed()

        // A letter that does not come first stays where it is, and is a slip.
        tapLetter(first.tiles.first { it != first.letters.first() })
        assertEquals(1, spelling().slips)
        screen.onNodeWithContentDescription(text(R.string.spell_empty, first.tiles.size)).assertIsDisplayed()

        // In order, each letter takes its place; whole, the word is said, and the Round waits because of the slip.
        tapLetter(first.letters.first())
        screen.onNodeWithContentDescription(text(R.string.spell_so_far, first.letters.first(), 1, first.tiles.size)).assertIsDisplayed()
        first.letters.drop(1).forEach(::tapLetter)
        screen.onNodeWithContentDescription(text(R.string.spell_whole, first.polish)).assertIsDisplayed()
        assertEquals(listOfNotNull(viewModel?.practiceClipOf(first.polish)?.let { Played(it.path, slow = false) }), clips.played)
        tap(R.string.action_continue)

        while (viewModel?.screen is Screen.Round) {
            checkNotNull(spelling().word).letters.forEach(::tapLetter)
            tap(R.string.action_continue)
        }
        showsScore(5, 6)
        showsFirstBest()
    }

    @Test
    fun `spelled aloud, a word's letters are said by name, and only its meaning is shown once it is whole`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_spell_it)
        tap(R.string.option_aloud)
        clips.asked.clear()
        tap(R.string.setup_play)
        screen.waitForIdle()

        // The letters' names are said in order, and nothing of the word is written.
        val first = checkNotNull(spelling().word)
        assertTrue(spelling().aloud)
        val names = first.spelled.map { name -> Played(checkNotNull(viewModel?.practiceClipOf(name)).path, slow = false) }
        assertEquals(names, clips.played)
        screen.onNodeWithText(first.meaning).assertDoesNotExist()
        shows(R.string.spell_aloud_instruction)

        // Again on the button.
        clips.asked.clear()
        screen.onNodeWithContentDescription(text(R.string.spell_aloud_again)).performClick()
        assertEquals(names, clips.played)

        // Whole, the word is said and its meaning written.
        clips.asked.clear()
        first.letters.forEach(::tapLetter)
        screen.onNodeWithContentDescription(text(R.string.spell_whole, first.polish)).assertIsDisplayed()
        shows(first.meaning)
        assertEquals(Played(checkNotNull(viewModel?.practiceClipOf(first.polish)).path, slow = false), clips.played.first())
    }

    @Test
    fun `Register sort shows a phrase alone, and a wrong register shows the right one and what it means`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_register_sort)
        tap(R.string.setup_play)

        // A phrase, with a minute on the clock and a button for each register.
        val first = checkNotNull(registers().card)
        shows(first.polish)
        screen.onNodeWithText(first.meaning).assertDoesNotExist()
        shows(text(R.string.quick_time, 1, 0))
        screen.onNodeWithContentDescription(quantity(R.plurals.quick_left, 60)).assertIsDisplayed()

        // Right: the next phrase is there at once, counted under its register.
        sortInto(first.register)
        shows(right(1))
        assertEquals(1, registers().sorted[first.register])

        // Wrong: the register it has and what it means are shown, and nothing more is answered until the Learner moves on.
        val second = checkNotNull(registers().card)
        clips.asked.clear()
        sortInto(registers().registers.first { it != second.register })
        shows(second.meaning)
        val said = when (second.register) {
            Register.FORMAL -> R.string.register_sort_missed_formal
            Register.INFORMAL -> R.string.register_sort_missed_informal
            Register.UNIVERSAL -> R.string.register_sort_missed_universal
        }
        shows(text(said, second.polish))
        screen.onNode(hasContentDescription(text(R.string.register_sort_button, second.register.mark, registerButtonWords(second.register), registers().sorted[second.register] ?: 0)) and hasClickAction())
            .assertIsNotEnabled()
        assertEquals(listOfNotNull(viewModel?.practiceClipOf(second.polish)?.let { Played(it.path, slow = false) }), clips.played)
        screen.mainClock.advanceTimeBy(2_000)
        shows(text(R.string.quick_time, 0, 58))
        tap(R.string.action_continue)

        // The time runs out: the result is the right answers.
        screen.mainClock.advanceTimeBy(58_000)
        screen.waitForIdle()
        screen.onNodeWithContentDescription(quantity(R.plurals.result_right_sentence, 1)).assertIsDisplayed()
        showsFirstBest()
    }

    @Test
    fun `Ankieta shows what a person says and fills in their form, a right answer in place and a wrong one put right`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_ankieta)
        tap(R.string.option_read)
        tap(R.string.setup_play)

        // Read: what the person says, in one bubble without its meaning, the first field marked, and its options.
        val round = ankieta()
        val person = checkNotNull(round.person)
        val meaning = person.says.joinToString(" ") { it.meaning }
        shows(person.says.joinToString(" ") { it.text })
        screen.onNodeWithText(meaning).assertDoesNotExist()
        shows(text(R.string.ankieta_person, 1, 3))
        val first = checkNotNull(round.question)
        round.form.drop(1).forEach { field -> screen.onNodeWithContentDescription(text(R.string.ankieta_field_blank, field.label)).assertExists() }
        first.options.forEach { screen.onNode(hasText(it) and hasClickAction()).assertExists() }

        // Right: the field is filled in, and the next is asked by itself.
        fillIn(first.right)
        screen.onNodeWithContentDescription(text(R.string.ankieta_field_right, first.label, first.right)).assertExists()
        screen.mainClock.advanceTimeBy(Motion.MOVE_ON_MILLIS + 100)
        screen.waitForIdle()

        // Wrong: the field is filled in with the right answer, and waits for Continue.
        val second = checkNotNull(ankieta().question)
        fillIn(second.options.first { it != second.right })
        screen.onNodeWithContentDescription(text(R.string.ankieta_field_missed, second.label, second.right)).assertExists()
        tap(R.string.action_continue)

        // The rest right. Once the form is filled in, what each line means is shown.
        while (ankieta().question?.person == 0) {
            val question = checkNotNull(ankieta().question)
            fillIn(question.right)
            if (ankieta().form.all { it.filled != null }) screen.onNodeWithText(meaning).assertExists()
            screen.mainClock.advanceTimeBy(Motion.MOVE_ON_MILLIS + 100)
            screen.waitForIdle()
        }
        shows(text(R.string.ankieta_person, 2, 3))
        while (viewModel?.screen is Screen.Round) {
            fillIn(checkNotNull(ankieta().question).right)
            screen.mainClock.advanceTimeBy(Motion.MOVE_ON_MILLIS + 100)
            screen.waitForIdle()
        }
        showsScore(14, 15)
        showsFirstBest()
    }

    @Test
    fun `Ankieta heard plays all a person says when they appear and again from the button, and writes none of it before the form is filled in`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_ankieta)
        tap(R.string.option_listen)
        clips.asked.clear()
        tap(R.string.setup_play)
        screen.waitForIdle()
        val person = checkNotNull(ankieta().person)
        val all = person.says.map { Played(checkNotNull(viewModel?.practiceClipOf(it.text)).path, slow = false) }
        assertEquals(all, clips.played)
        screen.onNodeWithText(person.says.first().text).assertDoesNotExist()
        clips.asked.clear()
        screen.onNodeWithContentDescription(text(R.string.audio_play_again)).performClick()
        screen.waitForIdle()
        assertEquals(all, clips.played)
    }

    @Test
    fun `the Quick round runs against a clock, and a wrong answer waits while it runs`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_quick_round)
        tap(R.string.option_meaning)
        tap(R.string.setup_play)

        // A word and a meaning, and a minute on the clock.
        val first = checkNotNull(quick().card)
        shows(first.polish)
        shows(first.shownMeaning)
        shows(text(R.string.quick_time, 1, 0))
        screen.onNodeWithContentDescription(quantity(R.plurals.quick_left, 60)).assertIsDisplayed()

        // Right: the next card is there at once.
        tap(if (first.matches) R.string.quick_match else R.string.quick_no_match)
        shows(right(1))
        val second = checkNotNull(quick().card)
        shows(second.polish)

        // Wrong: what the word means is shown, and nothing more is answered until the Learner moves on.
        tap(if (second.matches) R.string.quick_no_match else R.string.quick_match)
        shows(text(R.string.quick_missed, second.polish, second.meaning))
        screen.onNode(hasText(text(R.string.quick_match)) and hasClickAction()).assertIsNotEnabled()
        screen.mainClock.advanceTimeBy(2_000)
        shows(text(R.string.quick_time, 0, 58))
        tap(R.string.action_continue)
        shows(checkNotNull(quick().card).polish)

        // The time runs out: the result is the right answers.
        screen.mainClock.advanceTimeBy(58_000)
        screen.waitForIdle()
        screen.onNodeWithContentDescription(quantity(R.plurals.result_right_sentence, 1)).assertIsDisplayed()
        showsFirstBest()
    }

    @Test
    fun `Opposites snap runs against a clock, and a wrong answer shows the real opposite while it runs`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_opposites_snap)
        tap(R.string.setup_play)

        // Two words, and a minute on the clock.
        val first = checkNotNull(opposites().card)
        shows(R.string.opposites_instruction)
        shows(first.word)
        shows(first.shown)
        shows(text(R.string.quick_time, 1, 0))
        screen.onNodeWithContentDescription(quantity(R.plurals.quick_left, 60)).assertIsDisplayed()

        // Right: the next pair is there at once.
        tap(if (first.opposites) R.string.opposites_yes else R.string.opposites_no)
        shows(right(1))
        val second = checkNotNull(opposites().card)

        // Wrong: the real opposite is shown, and nothing more is answered until the Learner moves on.
        tap(if (second.opposites) R.string.opposites_no else R.string.opposites_yes)
        shows(text(R.string.opposites_missed, second.word, second.meaning, second.opposite, second.oppositeMeaning))
        screen.onNode(hasText(text(R.string.opposites_yes)) and hasClickAction()).assertIsNotEnabled()
        screen.mainClock.advanceTimeBy(2_000)
        shows(text(R.string.quick_time, 0, 58))
        tap(R.string.action_continue)
        shows(checkNotNull(opposites().card).word)

        // The time runs out: the result is the right answers.
        screen.mainClock.advanceTimeBy(58_000)
        screen.waitForIdle()
        screen.onNodeWithContentDescription(quantity(R.plurals.result_right_sentence, 1)).assertIsDisplayed()
        showsFirstBest()
    }

    @Test
    fun `the Verb challenge offers a conjugation of two learned verbs as a way to play`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_verb_challenge)
        screen.onNodeWithText("-ę, -isz/-ysz").performScrollTo().performClick()
        tap(R.string.setup_play)

        // Only verbs of that conjugation are asked: never być, nor mieć of -am, -asz.
        val asked = ((viewModel?.screen as Screen.Round).round as VerbRound).run.questions.map { it.verb }.toSet()
        assertTrue(asked.size > 1, "$asked")
        assertTrue(asked.none { it == "być" || it == "mieć" }, "$asked")
    }

    @Test
    fun `Phrase chains stays closed until three sentences have been built`() {
        openPracticeAfterTodaysDailySet()

        showsFurtherDown(text(R.string.game_phrase_chains_needs))
        openGame(R.string.game_phrase_chains)
        absent(R.string.setup_play)
    }

    @Test
    fun `a sentence is chained a word at a time, a wrong card is a slip, and the fewest slips is the Best`() {
        openPracticeOverTheRealCourse()
        openGame(R.string.game_phrase_chains)
        tap(R.string.option_read)
        tap(R.string.setup_play)

        // What to say is read; the chain starts empty.
        val sentence = checkNotNull(chaining().sentence)
        shows(sentence.meaning)
        shows(R.string.chain_hint)
        shows(slips(0))
        val order = sentence.polish.split(" ").map { word -> word.trim(',', '.', '?', '!', ':', '„', '”') }

        // A word that does not belong is a slip, and the card stays.
        tapCard(sentence.cards.first { it !in order })
        shows(slips(1))
        shows(R.string.chain_hint)

        // In order, each card joins the chain; whole, the sentence is said.
        clips.asked.clear()
        order.forEach(::tapCard)
        screen.onNodeWithContentDescription(text(R.string.chain_whole, order.joinToString(" "))).assertIsDisplayed()
        assertEquals(listOf(Played(checkNotNull(viewModel?.practiceClipOf(sentence.polish)).path, slow = false)), clips.played)
        tap(R.string.action_continue)

        chainTheRest()
        screen.onNodeWithContentDescription(quantity(R.plurals.result_slips_sentence, 1)).assertIsDisplayed()
        showsFirstBest()

        // No slips at all is a new Best.
        tap(R.string.result_play_again)
        chainTheRest()
        screen.onNodeWithContentDescription(quantity(R.plurals.result_slips_sentence, 0)).assertIsDisplayed()
        shows(R.string.result_new_best)
    }

    @Test
    fun `heard, a sentence is said as it appears and its meaning is shown once it is chained`() {
        openPracticeOverTheRealCourse()
        clips.asked.clear()
        openGame(R.string.game_phrase_chains)
        tap(R.string.setup_play)

        screen.waitForIdle()
        val sentence = checkNotNull(chaining().sentence)
        val said = Played(checkNotNull(viewModel?.practiceClipOf(sentence.polish)).path, slow = false)
        assertEquals(listOf(said), clips.played)
        screen.onNodeWithText(sentence.meaning).assertDoesNotExist()

        sentence.polish.split(" ").map { word -> word.trim(',', '.', '?', '!', ':', '„', '”') }.forEach(::tapCard)

        shows(sentence.meaning)
        // It was heard already: it is not said again.
        assertEquals(listOf(said), clips.played)
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
        val engine = CourseEngine(fixtureCourse(), FileLearnerProgress(File(folder.root, "progress.json")), clock, oneADay = { true })
        return CourseViewModel(app, engine, engine.practice(FilePracticeProgress(File(folder.root, "practice.json")), Random(7)))
    }

    /** Practice as a Learner who has completed the whole real course sees it, the last Daily Set today. */
    private fun openPracticeOverTheRealCourse() {
        val course = loadCourse { path -> app.assets.open(path).bufferedReader().use { it.readText() } }
        val progress = InMemoryLearnerProgress()
        course.path.forEachIndexed { index, dailySet ->
            progress.recordCompleted(dailySet.id, clock.today.minusDays((course.path.size - 1 - index).toLong()))
        }
        val engine = CourseEngine(course, progress, clock, oneADay = { true })
        viewModel = CourseViewModel(app, engine, engine.practice(InMemoryPracticeProgress(), Random(7)))
        screen.setContent {
            PolishEasyTheme {
                viewModel?.let { key(it) { PolishEasyApp(it, clips) } }
            }
        }
        tap(R.string.nav_practice)
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

    private fun listening(): ListenRound = (viewModel?.screen as Screen.Round).round as ListenRound

    private fun chaining(): ChainRound = (viewModel?.screen as Screen.Round).round as ChainRound

    /** Taps a card that says [word] and is not yet in the chain. */
    private fun tapCard(word: String) = screen.onAllNodes(hasText(word) and hasClickAction())[0].performScrollTo().performClick()

    /** Chains every sentence left in the Round the way it is written, moving on after each. */
    private fun chainTheRest() {
        while (viewModel?.screen is Screen.Round) {
            checkNotNull(chaining().sentence).polish.split(" ").map { word -> word.trim(',', '.', '?', '!', ':', '„', '”') }.forEach(::tapCard)
            tap(R.string.action_continue)
        }
    }

    private fun slips(count: Int): String = quantity(R.plurals.practice_slips, count)

    private fun spelling(): SpellRound = (viewModel?.screen as Screen.Round).round as SpellRound

    private fun quick(): QuickRound = (viewModel?.screen as Screen.Round).round as QuickRound

    private fun opposites(): OppositesRound = (viewModel?.screen as Screen.Round).round as OppositesRound

    private fun whosWho(): WhoRound = (viewModel?.screen as Screen.Round).round as WhoRound

    private fun verbGroups(): VerbGroupRound = (viewModel?.screen as Screen.Round).round as VerbGroupRound

    private fun hobbies(): HobbyRound = (viewModel?.screen as Screen.Round).round as HobbyRound

    private fun till(): TillRound = (viewModel?.screen as Screen.Round).round as TillRound

    private fun lotto(): LottoRound = (viewModel?.screen as Screen.Round).round as LottoRound

    /** The label on a coin or note of [grosze]: "50 gr", "2 zł". */
    private fun moneyLabel(grosze: Int): String = if (grosze < 100) text(R.string.till_grosze, grosze) else text(R.string.till_zloty, grosze / 100)

    /** Puts [grosze] on the counter with the fewest coins and notes, tapping their keys, which come after the counter. */
    private fun payWith(grosze: Int) {
        var left = grosze
        till().money.sortedDescending().forEach { coin ->
            while (left >= coin) {
                screen.onAllNodes(hasText(moneyLabel(coin)) and hasClickAction()).onLast().performScrollTo().performClick()
                left -= coin
            }
        }
    }

    private fun ankieta(): AnkietaRound = (viewModel?.screen as Screen.Round).round as AnkietaRound

    private fun labelling(): LabelRound = (viewModel?.screen as Screen.Round).round as LabelRound

    private fun tapLabel(word: String) = screen.onNode(hasText(word) and hasClickAction()).performScrollTo().performClick()

    /** Fills in the field being asked with the option that reads [answer]. */
    private fun fillIn(answer: String) = screen.onNode(hasText(answer) and hasClickAction()).performScrollTo().performClick()

    private fun registers(): RegisterRound = (viewModel?.screen as Screen.Round).round as RegisterRound

    /** Taps the button of [register], whatever it counts. */
    private fun sortInto(register: Register) {
        val count = registers().sorted[register] ?: 0
        screen.onNode(hasContentDescription(text(R.string.register_sort_button, register.mark, registerButtonWords(register), count)) and hasClickAction())
            .performClick()
    }

    private fun registerButtonWords(register: Register): String = text(
        when (register) {
            Register.FORMAL -> R.string.register_sort_formal
            Register.INFORMAL -> R.string.register_sort_informal
            Register.UNIVERSAL -> R.string.register_sort_universal
        },
    )

    /** Taps a tile that says [letter] and is not yet in the word. */
    private fun tapLetter(letter: String) = screen.onAllNodes(hasText(letter) and hasClickAction())[0].performScrollTo().performClick()

    private fun numbers(): NumberRound = (viewModel?.screen as Screen.Round).round as NumberRound

    private fun sorting(): GenderRound = (viewModel?.screen as Screen.Round).round as GenderRound

    /** Taps a key of the keypad. */
    private fun press(digit: String) = screen.onNode(hasText(digit) and hasClickAction()).performScrollTo().performClick()

    /** Types the answer to every question left in the Round and checks it, moving on after each. */
    private fun typeTheRest() {
        while (viewModel?.screen is Screen.Round) {
            checkNotNull(numbers().question).answer.forEach { press(it.toString()) }
            screen.onNodeWithContentDescription(text(R.string.action_check)).performClick()
            tap(R.string.action_continue)
        }
    }

    /** Taps the word for "my" that says [word]. */
    private fun tapBucket(word: String) = screen.onNode(hasText(word) and hasClickAction()).performScrollTo().performClick()

    /** Opens a Game from the Practice screen, wherever on it the Game's card is. */
    private fun openGame(name: Int) {
        screen.onNodeWithText(text(name)).performScrollTo().performClick()
    }

    private fun gapped(): GapRound = (viewModel?.screen as Screen.Round).round as GapRound

    private fun tapLetters(letters: String) = screen.onNode(hasText(letters) and hasClickAction()).performClick()

    private fun board(): MemoryRound = (viewModel?.screen as Screen.Round).round as MemoryRound

    /** Taps the tile at [tile], which is face down. */
    private fun turn(tile: Int) = screen.onNodeWithContentDescription(text(R.string.memory_tile, tile + 1)).performClick()

    /** Turns up the two tiles of each pair in turn, until the board is finished and gives way to the result. */
    private fun findEveryPair() {
        val board = board()
        board.tiles.indices.groupBy { board.tiles[it].pair }.values.forEach { pair -> pair.forEach(::turn) }
        // The finished board stands for a moment.
        screen.mainClock.advanceTimeBy(Motion.BOARD_DONE_MILLIS + 100)
        screen.waitForIdle()
    }

    private fun turns(count: Int): String = quantity(R.plurals.practice_turns, count)

    private fun quantity(id: Int, count: Int): String = app.resources.getQuantityString(id, count, count)

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

    /** Shows the noun [polish], which a [meaning] such as "sport" holds too: then it is on the screen twice. */
    private fun showsNoun(polish: String, meaning: String) {
        val nodes = screen.onAllNodesWithText(polish)
        nodes.assertCountEquals(if (polish in meaning) 2 else 1)
        nodes[0].assertIsDisplayed()
    }

    /** Shows [text] somewhere down the screen, scrolled to: the Games further down than the screen is high. */
    private fun showsFurtherDown(text: String) {
        screen.onNodeWithText(text).performScrollTo().assertIsDisplayed()
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
