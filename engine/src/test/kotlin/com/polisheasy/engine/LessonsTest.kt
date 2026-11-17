package com.polisheasy.engine

import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The Lesson screen, the Słownik, the Fiszki and the Checkpoint, over the content shipped with the app. */
class LessonsTest {
    private val contentDir = File(System.getProperty("contentDir"))
    private val course = loadCourse { path -> File(contentDir, path).readText() }
    private val chapter0 = "kpk1-l00"

    private fun engine(
        progress: LearnerProgress = InMemoryLearnerProgress(),
        clock: FakeClock = FakeClock(),
        checkpoints: CheckpointBests = InMemoryCheckpointBests(),
    ) = CourseEngine(course, progress, clock, checkpoints = checkpoints)

    /** The right answer to the Exercise [step] shows. */
    private fun right(step: Step.ShowExercise): Answer = when (val exercise = course.exercises.getValue(step.exerciseId)) {
        is ChooseTranslation -> Answer.Option(exercise.correctOption)
        is TypeAnswer -> Answer.Typed(exercise.acceptedAnswers.first())
        is WordTiles -> Answer.Tiles(words(exercise.acceptedAnswers.first()))
        is Conjugation -> Answer.Typed(exercise.acceptedAnswers.first())
        is ListenChoose -> Answer.Option(
            when (val view = step.exercise) {
                is ExerciseView.ListenChoose -> view.options
                is ExerciseView.ChoosePolish -> view.options
                else -> error("$view")
            }.indexOf(exercise.text),
        )
        is ListenType -> Answer.Typed(exercise.acceptedAnswers.first())
        else -> bookRight(exercise)
    }

    /** A wrong answer of the same kind. */
    private fun wrong(step: Step.ShowExercise): Answer = when (val answer = right(step)) {
        is Answer.Option -> Answer.Option(if (answer.index == 0) 1 else 0)
        is Answer.Typed -> Answer.Typed("xyz")
        is Answer.Tiles -> Answer.Tiles(listOf("xyz"))
        else -> bookWrong(answer)
    }

    /** Plays from [first] to the Summary, answering wrongly where [wrongly] says so. Returns every Step. */
    private fun CourseEngine.play(first: Step, wrongly: (Int) -> Boolean = { false }): List<Step> {
        val seen = mutableListOf(first)
        var asked = 0
        while (true) {
            when (val step = seen.last()) {
                is Step.ShowTip -> Unit
                is Step.ShowExercise -> answer(if (wrongly(asked++)) wrong(step) else right(step))
                is Step.ShowSummary -> return seen
            }
            seen += next()
        }
    }

    @Test
    fun `chapter 0's Lesson screen has its banner, its L strand and five sections, none done`() {
        val lesson = engine().lesson(chapter0)

        assertEquals(0, lesson.number)
        assertEquals("Dzień dobry!", lesson.title)
        assertEquals("Hello!", lesson.subtitle)
        assertEquals(listOf(Strand.VOCABULARY), lesson.strands.map { it.strand })
        assertEquals(StrandView(Strand.VOCABULARY, listOf("podstawowe słowa i zwroty", "polecenia"), 0, 5), lesson.strands.single())
        assertEquals(listOf("Polecenia", "W klasie", "W szkole", "Proszę, dziękuję, przepraszam", "Ćwiczenia"), lesson.sections.map { it.heading })
        assertEquals(listOf("1", "2", "3", "4", "5"), lesson.sections.map { it.letter })
        assertEquals(listOf(8, 8, 8, 3, 0), lesson.sections.map { it.newWords })
        assertTrue(lesson.sections.none { it.state == DailySetState.COMPLETED || it.inProgress })
        assertEquals(listOf("posłuchać", "przeczytać", "lekcja", "nauczyciel"), lesson.lookUp.map { it.text })
        assertTrue(lesson.lookUp.all { it.audio != null }, "Every look-up word can be heard")
        assertEquals(CheckpointView(best = null, questions = 20, sealed = false), lesson.checkpoint)
        assertEquals(false, lesson.coming)
        assertEquals(0 to 1, lesson.grammarDone to lesson.grammarTotal, "The table of men and women in W szkole")
    }

    @Test
    fun `a completed section ticks its strand and its row`() {
        val progress = InMemoryLearnerProgress()
        progress.recordCompleted("kpk1-l00-a", on = march(2))

        val lesson = engine(progress).lesson(chapter0)

        assertEquals(1, lesson.strands.single().done)
        assertEquals(DailySetState.COMPLETED, lesson.sections.first().state)
    }

    @Test
    fun `a chapter with no content yet is coming, with no sections and no Test`() {
        val lesson = engine().lesson("kpk1-l26")

        assertTrue(lesson.coming)
        assertEquals("Same problemy!", lesson.title)
        assertEquals(emptyList(), lesson.sections)
        assertEquals(0, lesson.checkpoint.questions)
        assertEquals(3, lesson.strands.size)
    }

    @Test
    fun `the Słownik of chapter 0 has every word it introduces, a word met in Starter included, each with its Clip`() {
        val words = engine().dictionary(chapter0)

        assertEquals("posłuchać", words.first().text)
        assertTrue(words.any { it.text == "proszę" }, "proszę, met in Starter, is introduced again in chapter 0")
        assertEquals(30, words.size)
        assertEquals(words.map { it.itemId }.distinct(), words.map { it.itemId })
        assertTrue(words.all { it.audio != null })
    }

    @Test
    fun `Starter's Słownik shows the register the book marks`() {
        val words = engine().dictionary("a1-u01").associateBy { it.text }

        assertEquals(Register.FORMAL, words.getValue("dzień dobry").register)
        assertEquals(Register.INFORMAL, words.getValue("cześć").register)
        assertNull(words.getValue("tak").register)
    }

    @Test
    fun `the Fiszki put due words first, then those met, then those not met, and a turned card reschedules its Item`() {
        val progress = InMemoryLearnerProgress()
        val clock = FakeClock()
        val engine = engine(progress, clock)
        progress.recordCompleted("kpk1-l00-a", on = march(2))
        clock.moveTo(march(3))

        val cards = engine.flashcards(chapter0)

        val due = cards.takeWhile { it.due }
        // The eight words section 1 introduces and proszę, which it uses, were met yesterday and are due today.
        assertEquals(9, due.size)
        assertTrue(cards.drop(due.size).none { it.due })
        assertTrue(cards.drop(due.size).dropWhile { it.met }.none { it.met })

        engine.answerFlashcard(due.first().word.itemId, knew = true)
        assertEquals(2, progress.itemProgress().getValue(due.first().word.itemId).schedule.box)
        engine.answerFlashcard(due[1].word.itemId, knew = false)
        assertEquals(1, progress.itemProgress().getValue(due[1].word.itemId).schedule.box)
        assertEquals(march(4), progress.itemProgress().getValue(due[1].word.itemId).schedule.due)
    }

    @Test
    fun `a Checkpoint asks up to 20 of the chapter's Exercises, of every kind it has, each once, and keeps its Best`() {
        val checkpoints = InMemoryCheckpointBests()
        val progress = InMemoryLearnerProgress()
        val engine = engine(progress, checkpoints = checkpoints)

        val steps = engine.play(engine.startCheckpoint(chapter0, random = Random(1)))
        val asked = steps.filterIsInstance<Step.ShowExercise>()

        assertEquals(20, asked.size)
        assertEquals(asked.map { it.exerciseId }.distinct(), asked.map { it.exerciseId })
        assertTrue(asked.all { it.exerciseId.startsWith("$chapter0-") && it.checkpoint && !it.replay })
        val kinds = course.levels[1].units[0].dailySets.flatMap { it.exercises }.map { it::class }.toSet()
        assertEquals(kinds, asked.map { course.exercises.getValue(it.exerciseId)::class }.toSet())
        assertTrue(steps.none { it is Step.ShowTip })
        val result = assertNotNull((steps.last() as Step.ShowSummary).checkpoint)
        assertEquals(CheckpointResultView(chapter0, "Dzień dobry!", 20, 20, 100, 100, newBest = true, passed = true), result)
        assertEquals(100, checkpoints.best(chapter0))
        assertTrue(engine.lesson(chapter0).checkpoint.sealed)
        assertTrue(engine.shelves()[1].lessons[0].sealed)
        // A Checkpoint changes nothing on the Path, in Review or in the Streak.
        assertEquals(emptyMap(), progress.completedDailySets())
        assertEquals(emptyMap(), progress.itemProgress())
        assertNull(progress.dailySetInProgress())
    }

    @Test
    fun `a wrong answer in a Checkpoint is not asked again, a lower score keeps the Best, and under 80 percent does not pass`() {
        val checkpoints = InMemoryCheckpointBests()
        checkpoints.record(chapter0, 90)
        val engine = engine(checkpoints = checkpoints)

        val steps = engine.play(engine.startCheckpoint(chapter0, random = Random(2))) { it < 5 }

        assertEquals(20, steps.count { it is Step.ShowExercise })
        val result = assertNotNull((steps.last() as Step.ShowSummary).checkpoint)
        assertEquals(15, result.correct)
        assertEquals(75, result.percent)
        assertEquals(90, result.best)
        assertEquals(false, result.newBest)
        assertEquals(false, result.passed)
        assertEquals(90, checkpoints.best(chapter0))
    }

    @Test
    fun `a Checkpoint's Best survives a restart`() {
        val file = Files.createTempDirectory("checkpoints").resolve("checkpoints.json").toFile()
        val engine = engine(checkpoints = FileCheckpointBests(file))
        engine.play(engine.startCheckpoint("a1-u01", random = Random(3))) { it == 0 }

        val again = engine(checkpoints = FileCheckpointBests(file))

        assertEquals(95, again.lesson("a1-u01").checkpoint.best)
        assertTrue(again.lesson("a1-u01").checkpoint.sealed)
    }

    @Test
    fun `every Exercise shows its instruction in Polish with its English and its Clip, in every form`() {
        val engine = engine()
        val shown = mutableSetOf<String>()
        listOf(true, false).forEach { sound ->
            listOf(chapter0, "a1-u02").forEach { unit ->
                engine.play(engine.startCheckpoint(unit, sound = sound, random = Random(4))).filterIsInstance<Step.ShowExercise>().forEach { step ->
                    val instruction = assertNotNull(step.instruction, step.exerciseId)
                    assertTrue(instruction.polish.startsWith("Proszę ") || instruction.polish == "Prawda czy nieprawda?", instruction.polish)
                    assertTrue(instruction.meaning.isNotBlank())
                    assertEquals(sound, instruction.audio != null, step.exerciseId)
                    shown += step.exercise::class.simpleName!!
                }
            }
        }
        assertTrue(
            shown.containsAll(setOf("ChooseTranslation", "TypeAnswer", "WordTiles", "Conjugation", "ListenChoose", "ListenType", "ChoosePolish")),
            "$shown",
        )
    }

    @Test
    fun `a translation choice of a phrase the book marks carries its register`() {
        val engine = engine()
        val step = engine.startDailySet() as Step.ShowTip
        val first = generateSequence(engine.next()) { s ->
            if (s is Step.ShowExercise) {
                engine.answer(right(s))
            }
            engine.next()
        }.filterIsInstance<Step.ShowExercise>().first { (it.exercise as? ExerciseView.ChooseTranslation)?.prompt == "dzień dobry" }

        assertNotNull(step)
        assertEquals(Register.FORMAL, (first.exercise as ExerciseView.ChooseTranslation).register)
    }

    @Test
    fun `the shelves are Starter then the book, the current lesson is the first not finished`() {
        val progress = InMemoryLearnerProgress()
        course.levels[0].units.flatMap { it.dailySets }.forEach { progress.recordCompleted(it.id, on = march(2)) }

        val shelves = engine(progress).shelves()

        assertEquals(listOf("Starter", "Polski krok po kroku 1"), shelves.map { it.title })
        assertEquals(listOf(1, 2, 3), shelves[0].lessons.map { it.place })
        assertTrue(shelves[0].lessons.all { it.sectionsDone == it.sections && !it.current })
        assertEquals(chapter0, shelves[1].lessons.single { it.current }.unitId)
    }

    @Test
    fun `the Słownik finds a word by its Polish without its marks, or by its meaning`() {
        val words = engine().dictionary(chapter0)

        assertEquals(listOf("przetłumaczyć"), words.matching("przetlumacz").map { it.text })
        assertEquals(listOf("koleżanka"), words.matching("KOLEZANKA").map { it.text })
        assertTrue(words.matching("teacher").map { it.text }.containsAll(listOf("nauczyciel", "nauczycielka")))
        assertEquals(words, words.matching("  "))
        assertEquals(emptyList(), words.matching("xyz"))
    }
}
