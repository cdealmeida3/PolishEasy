package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BuildingFromWordTilesTest {
    private val sentence = "dzień dobry, jak się masz?"
    private val sentenceWords = listOf("dzień", "dobry", "jak", "się", "masz")

    @Test
    fun `a word tiles Exercise is shown with its prompt and, as tiles, the words of the first Accepted Answer plus the distractors`() {
        val engine = engineWith(listOf("Cześć, jak się masz?", "jak się masz, Cześć?"), distractors = listOf("tak", "nie"))

        val step = engine.startDailySet() as Step.ShowExercise
        val view = step.exercise as ExerciseView.WordTiles

        assertEquals("good morning, how are you?", view.prompt)
        assertEquals(SetProgress(correct = 0, total = 1), step.progress)
        // Punctuation is removed and case is kept.
        assertEquals(listOf("Cześć", "jak", "się", "masz", "tak", "nie").sorted(), view.tiles.sorted())
    }

    @Test
    fun `the tiles are not in the authored order, and are the same every time the Exercise is shown`() {
        val distractors = listOf("tak", "nie")
        val first = engineWith(listOf(sentence), distractors)
        val shown = first.startDailySet()
        val tiles = tilesOf(shown)

        assertNotEquals(sentenceWords + distractors, tiles)
        assertEquals(tiles, tilesOf(engineWith(listOf(sentence), distractors).startDailySet()))

        first.answer(Answer.Tiles(listOf("tak")))
        assertEquals(shown, first.next())
    }

    @Test
    fun `two tiles are never handed over already in order`() {
        // Whatever the shuffle does with two tiles, the answer is not left ready-made.
        (1..20).forEach { n ->
            val exercise = WordTiles("exercise-$n", mapOf("en" to "good morning"), listOf("dzień dobry"))

            assertEquals(listOf("dobry", "dzień"), tilesOf(exercise), "Exercise ${exercise.id}")
        }
        // Tiles that are all the same word have only one order.
        assertEquals(listOf("tak", "tak"), tilesOf(WordTiles("e", mapOf("en" to "yes, yes"), listOf("tak, tak"))))
    }

    @Test
    fun `a word that appears twice in the answer has two tiles`() {
        val engine = engineWith(listOf("tak, tak, proszę"))

        assertEquals(listOf("proszę", "tak", "tak"), tilesOf(engine.startDailySet()).sorted())
    }

    @Test
    fun `building the Accepted Answer is correct and shows it as authored`() {
        assertEquals(
            Feedback(correct = true, correctAnswer = sentence),
            check(sentenceWords, listOf(sentence)),
        )
    }

    @Test
    fun `each word order listed as an Accepted Answer is correct, and the one built is shown`() {
        val accepted = listOf("dobrze, dziękuję", "dziękuję, dobrze")

        assertEquals(Feedback(correct = true, correctAnswer = "dobrze, dziękuję"), check(listOf("dobrze", "dziękuję"), accepted))
        assertEquals(Feedback(correct = true, correctAnswer = "dziękuję, dobrze"), check(listOf("dziękuję", "dobrze"), accepted))
    }

    @Test
    fun `a different order of the right words is wrong and shows the first Accepted Answer`() {
        assertEquals(
            Feedback(correct = false, correctAnswer = sentence),
            check(listOf("dobry", "dzień", "jak", "się", "masz"), listOf(sentence)),
        )
    }

    @Test
    fun `an answer with a distractor in it, with a word left out, or with no words is wrong`() {
        val wrong = Feedback(correct = false, correctAnswer = sentence)
        val distractors = listOf("tak")

        assertEquals(wrong, check(sentenceWords + "tak", listOf(sentence), distractors))
        assertEquals(wrong, check(listOf("dzień", "dobry", "jak", "tak", "masz"), listOf(sentence), distractors))
        assertEquals(wrong, check(sentenceWords.dropLast(1), listOf(sentence), distractors))
        assertEquals(wrong, check(emptyList(), listOf(sentence), distractors))
    }

    @Test
    fun `a tile that differs from the right word only by an accent is wrong, with no accent warning`() {
        assertEquals(
            Feedback(correct = false, correctAnswer = "dzień dobry"),
            check(listOf("dzien", "dobry"), listOf("dzień dobry"), distractors = listOf("dzien")),
        )
    }

    @Test
    fun `a wrongly built Exercise returns until it is built correctly, and counts against the Summary`() {
        val engine = engineWith(listOf("dzień dobry"))
        val exercise = engine.startDailySet()

        assertFalse(engine.answer(Answer.Tiles(listOf("dobry", "dzień"))).correct)
        assertEquals(exercise, engine.next())
        assertTrue(engine.answer(Answer.Tiles(listOf("dzień", "dobry"))).correct)

        assertEquals(Step.ShowSummary(correctAnswers = 0, exercises = 1), engine.next())
    }

    @Test
    fun `tapping a tile moves it to the end of the answer, and tapping it there returns it to the bank`() {
        val bank = TileSelection(listOf("dobry", "tak", "dzień"))

        val built = bank.place(2).place(0)
        assertEquals(Answer.Tiles(listOf("dzień", "dobry")), built.answer)
        assertEquals(listOf(true, false, true), (0..2).map(built::isPlaced))

        // A placed tile cannot be placed again, and there is no tile outside the bank.
        assertEquals(built, built.place(2))
        assertEquals(built, built.place(3))
        assertEquals(built, built.place(-1))

        val takenBack = built.takeBack(2)
        assertEquals(Answer.Tiles(listOf("dobry")), takenBack.answer)
        assertFalse(takenBack.isPlaced(2))
        assertEquals(Answer.Tiles(listOf("dobry", "dzień")), takenBack.place(2).answer)

        // A tile still in the bank cannot be taken back.
        assertEquals(takenBack, takenBack.takeBack(1))
    }

    @Test
    fun `two tiles with the same word are placed and taken back each on its own`() {
        val bank = TileSelection(listOf("tak", "proszę", "tak"))

        val one = bank.place(2)
        assertEquals(listOf(false, false, true), (0..2).map(one::isPlaced))

        val both = one.place(0)
        assertEquals(Answer.Tiles(listOf("tak", "tak")), both.answer)

        val other = both.takeBack(2)
        assertEquals(listOf(true, false, false), (0..2).map(other::isPlaced))
        assertEquals(Answer.Tiles(listOf("tak")), other.answer)
    }

    private fun tilesOf(step: Step): List<String> = ((step as Step.ShowExercise).exercise as ExerciseView.WordTiles).tiles

    /** The feedback on building [words] for a word tiles Exercise with these Accepted Answers. */
    private fun check(words: List<String>, accepted: List<String>, distractors: List<String> = emptyList()): Feedback {
        val engine = engineWith(accepted, distractors)
        engine.startDailySet()
        return engine.answer(Answer.Tiles(words))
    }

    /** A new Learner whose first Daily Set is one word tiles Exercise. */
    private fun engineWith(accepted: List<String>, distractors: List<String> = emptyList()): CourseEngine =
        CourseEngine(FixtureCourse.loadWithDayOne(tilesDay(accepted, distractors)), InMemoryLearnerProgress(), FakeClock(), oneADay = { true })
}

/** Day 1 as a single word tiles Exercise, "good morning, how are you?". */
fun tilesDay(accepted: List<String>, distractors: List<String> = emptyList()): String {
    fun quoted(texts: List<String>) = texts.joinToString { "\"$it\"" }
    return """
        {
          "id": "a1-u01-d01",
          "exercises": [
            { "type": "word_tiles", "id": "a1-u01-d01-e01", "prompt": { "en": "good morning, how are you?" },
              "acceptedAnswers": [${quoted(accepted)}], "distractors": [${quoted(distractors)}] }
          ]
        }
    """
}
