package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The book's own Exercises: fill the gaps from sticky notes, match the pairs, true or false about a
 * text read or heard, and put a dialogue in order. Each as the Learner sees it, how it is checked,
 * its reading form, Review, and what the loader and the content check say about it.
 */
class BookExercisesTest {
    private fun en(text: String): Localized = mapOf("en" to text)

    private val items = listOf(item("a"), item("b"), item("c"), item("d"))

    private val gaps = FillGaps(
        id = "a1-u01-d01-e01",
        text = "Proszę ___ i ___.",
        meaning = en("Listen and repeat, please."),
        gaps = listOf(listOf("posłuchać"), listOf("powtórzyć", "powtórzyć!")),
        distractors = listOf("napisać"),
        items = listOf("a"),
    )

    private val pairs = MatchPairs(
        id = "a1-u01-d01-e01",
        pairs = listOf(
            MatchPair("posłuchać", en("listen")),
            MatchPair("powtórzyć", en("repeat")),
            MatchPair("napisać", en("write")),
            MatchPair("przeczytać", en("read")),
        ),
        items = listOf("a", "b"),
    )

    private val text = listOf("— Dzień dobry!", "— Dzień dobry, pani profesor.", "— Proszę otworzyć książki.")

    private fun trueFalse(listen: Boolean) = TrueFalse(
        id = "a1-u01-d01-e01",
        lines = text,
        meaning = en("A lesson begins."),
        statements = listOf(
            Statement("To jest lekcja.", en("This is a lesson."), true),
            Statement("Pani profesor mówi „do widzenia”.", en("The teacher says goodbye."), false),
            Statement("Studenci otwierają książki.", en("The students open their books."), true),
        ),
        listen = listen,
        items = listOf("a"),
    )

    private val dialogue = OrderLines(
        id = "a1-u01-d01-e01",
        lines = listOf("— Dzień dobry!", "— Dzień dobry! Jak się pan nazywa?", "— Nazywam się Adam Nowak.", "— Bardzo mi miło."),
        meaning = en("Two people meet for the first time."),
        items = listOf("a"),
    )

    private val clips = (text + dialogue.lines + pairs.pairs.map { it.polish } + listOf("Proszę posłuchać i powtórzyć.")).distinct().map(::clip)

    private val sample = courseOf(items, clips)

    /** The Clip that says [polish]. */
    private fun heard(polish: String): ClipView = checkNotNull(sample.clipFor(polish))

    private fun engineFor(exercise: Exercise, sound: Boolean = true): Pair<CourseEngine, Step.ShowExercise> {
        val engine = CourseEngine(courseOf(items, clips, day("a1-u01-d01", exercise)), InMemoryLearnerProgress(), FakeClock())
        return engine to engine.startDailySet(sound = sound) as Step.ShowExercise
    }

    // fill_gaps

    @Test
    fun `a fill-the-gaps text is shown around its gaps, with sticky notes of each gap's word and the distractors, mixed`() {
        val view = assertIs<ExerciseView.FillGaps>(engineFor(gaps).second.exercise)

        assertEquals(listOf("Proszę ", " i ", "."), view.parts)
        assertEquals(2, view.gaps)
        assertEquals(listOf("napisać", "posłuchać", "powtórzyć"), view.notes.sorted())
        assertNotEquals(listOf("posłuchać", "powtórzyć", "napisać"), view.notes)
        assertEquals(view.notes, assertIs<ExerciseView.FillGaps>(engineFor(gaps).second.exercise).notes, "The same every time")
        assertEquals("Listen and repeat, please.", view.meaning)
    }

    @Test
    fun `the gaps filled right, from the notes or typed, are right, and the whole text is heard`() {
        val (engine, _) = engineFor(gaps)
        assertEquals("Listen and repeat, please.", engine.answerGloss())
        val feedback = engine.answer(Answer.Gaps(listOf("posłuchać", "Powtórzyć")))

        assertTrue(feedback.correct)
        assertFalse(feedback.accentWarning)
        assertEquals(listOf(true, true), feedback.parts)
        assertEquals(heard("Proszę posłuchać i powtórzyć."), feedback.audio)
    }

    @Test
    fun `a gap typed without its accents is right with a warning, and one wrong gap makes the answer wrong`() {
        val (accents, _) = engineFor(gaps)
        val warned = accents.answer(Answer.Gaps(listOf("posluchac", "powtorzyc")))
        assertTrue(warned.correct)
        assertTrue(warned.accentWarning)
        assertEquals("Proszę posłuchać i powtórzyć.", warned.correctAnswer)

        val (engine, _) = engineFor(gaps)
        val wrong = engine.answer(Answer.Gaps(listOf("posłuchać", "napisać")))
        assertFalse(wrong.correct)
        assertEquals(listOf(true, false), wrong.parts)
        assertEquals("Proszę posłuchać i powtórzyć.", wrong.correctAnswer)
        // Wrong, it comes back until it is right.
        assertIs<ExerciseView.FillGaps>((engine.next() as Step.ShowExercise).exercise)
    }

    // match_pairs

    @Test
    fun `the pairs are shown as the Polish in order and their meanings mixed, with where each meaning is`() {
        val view = assertIs<ExerciseView.MatchPairs>(engineFor(pairs).second.exercise)

        assertEquals(listOf("posłuchać", "powtórzyć", "napisać", "przeczytać"), view.left)
        assertNotEquals(listOf("listen", "repeat", "write", "read"), view.right)
        assertEquals(listOf("listen", "repeat", "write", "read"), view.match.map(view.right::get))
    }

    @Test
    fun `every pair joined with no wrong line is right, and a wrong line makes it come back`() {
        val (right, _) = engineFor(pairs)
        assertTrue(right.answer(Answer.Pairs(wrong = 0)).correct)

        val (engine, _) = engineFor(pairs)
        val wrong = engine.answer(Answer.Pairs(wrong = 2))
        assertFalse(wrong.correct)
        assertEquals("posłuchać: listen\npowtórzyć: repeat\nnapisać: write\nprzeczytać: read", wrong.correctAnswer)
        assertIs<ExerciseView.MatchPairs>((engine.next() as Step.ShowExercise).exercise)
    }

    // true_false

    @Test
    fun `a true-or-false text to read shows its lines and statements, with the book's instruction`() {
        val step = engineFor(trueFalse(listen = false)).second
        val view = assertIs<ExerciseView.TrueFalse>(step.exercise)

        assertFalse(view.heard)
        assertEquals(text, view.lines)
        assertEquals(listOf("To jest lekcja.", "Pani profesor mówi „do widzenia”.", "Studenci otwierają książki."), view.statements.map { it.text })
        assertEquals("The teacher says goodbye.", view.statements[1].meaning)
        assertEquals(3, view.clips.size, "A speaker on each line")
    }

    @Test
    fun `a heard text plays a Clip a line, and its reading form shows the lines when the Learner cannot listen`() {
        val (engine, step) = engineFor(trueFalse(listen = true))
        val heard = assertIs<ExerciseView.TrueFalse>(step.exercise)
        assertTrue(heard.heard)
        assertEquals(text.map(::heard), heard.clips)

        val reading = assertIs<ExerciseView.TrueFalse>((engine.cantListenNow() as Step.ShowExercise).exercise)
        assertFalse(reading.heard)
        assertEquals(text, reading.lines)

        val silent = assertIs<ExerciseView.TrueFalse>(engineFor(trueFalse(listen = true), sound = false).second.exercise)
        assertFalse(silent.heard)
        assertEquals(emptyList(), silent.clips)
    }

    @Test
    fun `each statement marked as it should be is right, and a wrong mark is wrong and says which`() {
        val (right, _) = engineFor(trueFalse(listen = false))
        assertTrue(right.answer(Answer.Marks(listOf(true, false, true))).correct)

        val (engine, _) = engineFor(trueFalse(listen = false))
        val wrong = engine.answer(Answer.Marks(listOf(true, true, true)))
        assertFalse(wrong.correct)
        assertEquals(listOf(true, false, true), wrong.parts)
        assertEquals("1 P · 2 N · 3 P", wrong.correctAnswer)
    }

    // order_lines

    @Test
    fun `a dialogue to order is shown mixed, the same each time, with its situation`() {
        val view = assertIs<ExerciseView.OrderLines>(engineFor(dialogue).second.exercise)

        assertEquals(dialogue.lines.sorted(), view.lines.sorted())
        assertNotEquals(dialogue.lines, view.lines)
        assertEquals(view.lines, assertIs<ExerciseView.OrderLines>(engineFor(dialogue).second.exercise).lines)
        assertEquals("Two people meet for the first time.", view.prompt)
    }

    @Test
    fun `the lines in order are right and the finished dialogue plays, and out of order is wrong`() {
        val (engine, _) = engineFor(dialogue)
        val right = engine.answer(Answer.Order(dialogue.lines.map { it.uppercase() }))
        assertTrue(right.correct)
        assertEquals(dialogue.lines.map(::heard), right.clips)

        val (wrongly, _) = engineFor(dialogue)
        val wrong = wrongly.answer(Answer.Order(dialogue.lines.reversed()))
        assertFalse(wrong.correct)
        assertEquals(dialogue.lines.joinToString("\n"), wrong.correctAnswer)

        val (silent, _) = engineFor(dialogue, sound = false)
        assertEquals(emptyList(), silent.answer(Answer.Order(dialogue.lines)).clips)
    }

    // Review and instructions

    @Test
    fun `each of the book's Exercises comes back in Review once its Item is due`() {
        listOf(gaps, pairs, trueFalse(listen = true), dialogue).forEach { exercise ->
            val learner = ItemLearner(
                courseOf(
                    items,
                    clips,
                    day("a1-u01-d01", exercise),
                    day("a1-u01-d02", choose("a1-u01-d02-e01", "b", "b")),
                    day("a1-u01-d03", choose("a1-u01-d03-e01", "c", "c")),
                ),
            )
            learner.playDay(march(2))
            val steps = learner.playDay(march(3)) + learner.playDay(march(4))
            val reviewed = steps.filterIsInstance<Step.ShowExercise>().filter { it.review }.map { it.exerciseId }
            assertTrue(exercise.id in reviewed, "${exercise::class.simpleName} reviewed in $reviewed")
        }
    }

    @Test
    fun `each has the book's instruction, a heard text its own`() {
        val instructions = mapOf(
            InstructionKind.FILL_GAPS to Instruction("Proszę uzupełnić.", en("Fill in the gaps.")),
            InstructionKind.MATCH_PAIRS to Instruction("Proszę połączyć.", en("Match them up.")),
            InstructionKind.TRUE_FALSE to Instruction("Prawda czy nieprawda?", en("True or false?")),
            InstructionKind.LISTEN_TRUE_FALSE to Instruction("Proszę posłuchać. Prawda czy nieprawda?", en("Listen. True or false?")),
            InstructionKind.ORDER_LINES to Instruction("Proszę ułożyć dialog.", en("Put the dialogue in order.")),
        )
        fun instructionOf(exercise: Exercise): String? {
            val course = courseOf(items, clips, day("a1-u01-d01", exercise)).copy(instructions = instructions)
            return (CourseEngine(course, InMemoryLearnerProgress(), FakeClock()).startDailySet() as Step.ShowExercise).instruction?.polish
        }

        assertEquals("Proszę uzupełnić.", instructionOf(gaps))
        assertEquals("Proszę połączyć.", instructionOf(pairs))
        assertEquals("Prawda czy nieprawda?", instructionOf(trueFalse(listen = false)))
        assertEquals("Proszę posłuchać. Prawda czy nieprawda?", instructionOf(trueFalse(listen = true)))
        assertEquals("Proszę ułożyć dialog.", instructionOf(dialogue))
    }

    @Test
    fun `everything they can say is listed for Clips`() {
        val course = courseOf(items, emptyList(), day("a1-u01-d01", gaps, pairs.copy(id = "p"), trueFalse(true).copy(id = "t"), dialogue.copy(id = "o")))

        val spoken = course.spokenTexts()
        assertTrue("Proszę posłuchać i powtórzyć." in spoken)
        assertTrue(pairs.pairs.all { it.polish in spoken })
        assertTrue(text.all { it in spoken })
        assertTrue(dialogue.lines.all { it in spoken })
    }

    // The loader and the content check

    private fun loadProblems(exercise: Exercise): List<ContentProblem> =
        checkContent(CheckerFixture.valid.withExercise("a1-u01-d01-e01") { exercise }.withClips { it + clips }.files(), null)

    private fun assertRejected(exercise: Exercise, word: String) {
        val problems = loadProblems(exercise)
        assertTrue(problems.any { it.id == exercise.id && word in it.message }, "Expected \"$word\" in:\n${problems.joinToString("\n")}")
    }

    private fun bookItems(exercise: Exercise): Exercise = when (exercise) {
        is FillGaps -> exercise.copy(items = listOf("czesc"))
        is MatchPairs -> exercise.copy(items = listOf("czesc"))
        is TrueFalse -> exercise.copy(items = listOf("czesc"))
        is OrderLines -> exercise.copy(items = listOf("czesc"))
        else -> exercise
    }

    @Test
    fun `well-made book Exercises pass the checks`() {
        listOf(gaps, pairs, trueFalse(listen = true), dialogue).forEach { exercise ->
            val problems = loadProblems(bookItems(exercise))
            assertEquals(emptyList(), problems.filter { it.id == exercise.id }, exercise::class.simpleName)
        }
    }

    @Test
    fun `the loader rejects gaps it could not fill, pairs and lines that read the same, and an empty text`() {
        assertRejected(bookItems(gaps.copy(text = "Proszę __ i ___.")), "not a gap")
        assertRejected(bookItems(gaps.copy(text = "Proszę ___.")), "answers for 2")
        assertRejected(bookItems(gaps.copy(text = "___ ___ ___ ___ ___ tak", gaps = List(5) { listOf("tak") })), "1 to 4")
        assertRejected(bookItems(gaps.copy(distractors = listOf("Posłuchać"))), "more than one note")
        assertRejected(bookItems(pairs.copy(pairs = pairs.pairs + MatchPair("Posłuchać!", en("hear")))), "more than one pair")
        assertRejected(bookItems(trueFalse(false).copy(statements = emptyList())), "no statements")
        assertRejected(bookItems(dialogue.copy(lines = dialogue.lines + "— dzień dobry")), "more than one line")
    }

    @Test
    fun `the content check keeps them the size the book sets them, and a true-or-false with both kinds`() {
        assertRejected(bookItems(pairs.copy(pairs = pairs.pairs.take(3))), "4 to 6")
        assertRejected(bookItems(trueFalse(false).copy(statements = trueFalse(false).statements.take(2))), "3 to 5")
        assertRejected(
            bookItems(trueFalse(false).copy(statements = trueFalse(false).statements.map { it.copy(`true` = true) })),
            "give both kinds",
        )
        assertRejected(bookItems(dialogue.copy(lines = dialogue.lines.take(2))), "3 to 6")
        assertRejected(bookItems(pairs.copy(pairs = pairs.pairs.map { it.copy(meaning = en("same")) })), "would read the same")
    }

    @Test
    fun `a heard text with a line that has no Clip cannot run`() {
        val problems = checkContent(
            CheckerFixture.valid.withExercise("a1-u01-d01-e01") { bookItems(trueFalse(listen = true)) }.files(),
            null,
        )
        assertTrue(problems.any { it.id == "a1-u01-d01-e01" && "has no Clip for \"— Dzień dobry!\"" in it.message }, problems.joinToString("\n"))
    }

    @Test
    fun `the Polish check reads every text they show, the gaps filled with each answer`() {
        val course = courseOf(items, clips, day("a1-u01-d01", gaps, pairs.copy(id = "p"), trueFalse(true).copy(id = "t"), dialogue.copy(id = "o")))
        val files = mapOf(
            "course.json" to contentJson.encodeToString(
                CourseFile(levels = listOf(LevelEntry("a1", en("A1"), listOf(UnitEntry("a1-u01", en("U"), listOf("d.json")))))),
            ),
            "d.json" to contentJson.encodeToString(course.path.single()),
        )
        val sentences = polishContent(
            object : ContentFiles {
                override fun read(path: String): String = files.getValue(path)
                override fun paths(): Set<String> = files.keys
            },
        ).sentences.map { it.text }

        assertTrue("Proszę posłuchać i powtórzyć." in sentences)
        assertTrue("Proszę posłuchać i powtórzyć!." in sentences, "Each gap's other answers too")
        assertTrue("napisać" in sentences)
        assertTrue("przeczytać" in sentences)
        assertTrue(text.all { it in sentences })
        assertTrue("Studenci otwierają książki." in sentences)
        assertTrue(dialogue.lines.all { it in sentences })
    }
}
