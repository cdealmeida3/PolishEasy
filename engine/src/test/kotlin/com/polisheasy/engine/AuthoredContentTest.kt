package com.polisheasy.engine

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthoredContentTest {
    private val contentDir = File(System.getProperty("contentDir"))
    private val course = loadCourse { path -> File(contentDir, path).readText() }

    /** The correct answer to the Exercise shown, found in the content by what the Learner sees. */
    private fun correctAnswer(view: ExerciseView): Answer {
        val exercises = course.exercises.values
        return when (view) {
            is ExerciseView.ChooseTranslation -> Answer.Option(
                exercises.filterIsInstance<ChooseTranslation>()
                    .first { it.prompt == view.prompt && it.options.map { option -> option.getValue("en") } == view.options }
                    .correctOption,
            )
            is ExerciseView.TypeAnswer -> Answer.Typed(
                exercises.filterIsInstance<TypeAnswer>()
                    .first { it.prompt.getValue("en") == view.prompt }
                    .acceptedAnswers.first(),
            )
            is ExerciseView.WordTiles -> Answer.Tiles(
                words(
                    exercises.filterIsInstance<WordTiles>()
                        .first { it.prompt.getValue("en") == view.prompt && tilesOf(it).sorted() == view.tiles.sorted() }
                        .acceptedAnswers.first(),
                ),
            )
            is ExerciseView.Conjugation -> Answer.Typed(
                exercises.filterIsInstance<Conjugation>()
                    .first {
                        it.verb == view.verb && it.person == view.person &&
                            it.sentence?.let(::gapOf) == view.sentence?.let { gap -> gap.before to gap.after }
                    }
                    .acceptedAnswers.first(),
            )
            is ExerciseView.ListenChoose -> Answer.Option(
                view.options.indexOf(
                    exercises.filterIsInstance<ListenChoose>()
                        .first { course.clipFor(it.text) == view.audio && optionsOf(it).sorted() == view.options.sorted() }
                        .text,
                ),
            )
            is ExerciseView.ListenType -> Answer.Typed(
                exercises.filterIsInstance<ListenType>()
                    .first { course.clipFor(it.acceptedAnswers.first()) == view.audio }
                    .acceptedAnswers.first(),
            )
        }
    }

    /** Plays today's Daily Set, Review included, answering everything correctly. Returns every Step seen. */
    private fun CourseEngine.playCorrectly(): List<Step> {
        val seen = mutableListOf(startDailySet())
        while (true) {
            when (val step = seen.last()) {
                is Step.ShowTip -> Unit
                is Step.ShowExercise -> answer(correctAnswer(step.exercise))
                is Step.ShowSummary -> return seen
            }
            seen += next()
        }
    }

    /** One Learner playing the whole course, one Daily Set per day from 2 March. Returns each day's Steps. */
    private fun playTheCourse(progress: LearnerProgress, clock: FakeClock): List<List<Step>> =
        course.path.indices.map { day ->
            clock.moveTo(march(2).plusDays(day.toLong()))
            CourseEngine(course, progress, clock).playCorrectly()
        }

    @Test
    fun `every Daily Set shipped with the app can be played through to its Summary`() {
        assertTrue(course.path.isNotEmpty())
        val progress = InMemoryLearnerProgress()
        val clock = FakeClock()

        val days = playTheCourse(progress, clock)

        course.path.zip(days).forEach { (dailySet, steps) ->
            val summary = steps.last() as Step.ShowSummary
            assertEquals(summary.exercises, summary.correctAnswers, "Daily Set ${dailySet.id}")
            assertTrue(summary.exercises >= dailySet.exercises.size, "Daily Set ${dailySet.id}")
            assertEquals(
                course.introducedIn(dailySet).map { ItemView(it.text, it.meaning.getValue("en")) },
                summary.itemsLearned,
                "Items learned in ${dailySet.id}",
            )
            assertEquals(dailySet.tips.size, steps.count { it is Step.ShowTip }, "Tips of ${dailySet.id}")
        }

        assertEquals(Today.AllCompleted, CourseEngine(course, progress, clock).today())
    }

    @Test
    fun `the Authored Content shipped with the app has no problems`() {
        val released = contentJson.decodeFromString(ReleasedIds.serializer(), File(System.getProperty("releasedIds")).readText())

        val problems = checkContent(DirectoryContent(contentDir), released)

        assertEquals(emptyList(), problems, problems.joinToString("\n"))
        assertTrue(course.items.isNotEmpty())
        assertTrue(course.path.flatMap { it.exercises }.any { it is ListenChoose })
    }

    @Test
    fun `played one per day, day 1 has no Review, day 2 ends with 7 Review Exercises, day 3 with 4, day 4 with none, day 5 with 3 and day 6 with 6`() {
        val days = playTheCourse(InMemoryLearnerProgress(), FakeClock())

        assertEquals(listOf(0, 7, 4, 0, 3, 6), days.map { steps -> steps.count { it is Step.ShowExercise && it.review } })
        assertEquals(
            listOf(7, 6, 0, 0, 6, 0),
            days.map { steps -> (steps.last() as Step.ShowSummary).itemsLearned.size },
        )
    }

    @Test
    fun `every Accepted Answer shipped with the app is correct when typed exactly, with no accent warning`() {
        val typing = course.path.flatMap { it.exercises }.filterIsInstance<TypeAnswer>()
        assertTrue(typing.isNotEmpty())

        typing.forEach { exercise ->
            exercise.acceptedAnswers.forEach { accepted ->
                assertEquals(
                    Feedback(correct = true, correctAnswer = accepted),
                    checkTyped(accepted, exercise.acceptedAnswers),
                    "Exercise ${exercise.id}",
                )
            }
        }
    }

    @Test
    fun `every Accepted Answer of every listen-and-type Exercise shipped is correct when typed exactly, with no accent warning`() {
        val listening = course.path.flatMap { it.exercises }.filterIsInstance<ListenType>()
        assertTrue(listening.isNotEmpty())

        listening.forEach { exercise ->
            exercise.acceptedAnswers.forEach { accepted ->
                assertEquals(
                    Feedback(correct = true, correctAnswer = accepted),
                    checkTyped(accepted, exercise.acceptedAnswers),
                    "Exercise ${exercise.id}",
                )
            }
        }
    }

    @Test
    fun `every Clip's file is real audio`() {
        // The content checker sees that each file is there. Only this test opens it.
        assertTrue(course.clips.isNotEmpty())
        course.clips.forEach { clip ->
            val file = File(contentDir, checkNotNull(course.clipFor(clip.text)).path)
            assertTrue(file.length() > 500, "The file of Clip ${clip.id} is only ${file.length()} bytes")
            assertEquals("OggS", String(file.readBytes().copyOf(4), Charsets.ISO_8859_1), "The file of Clip ${clip.id}")
        }
    }

    @Test
    fun `every Accepted Answer of every word tiles Exercise shipped is correct when built from its tiles`() {
        val wordTiles = course.path.flatMap { it.exercises }.filterIsInstance<WordTiles>()
        assertTrue(wordTiles.isNotEmpty())

        wordTiles.forEach { exercise ->
            exercise.acceptedAnswers.forEach { accepted ->
                // Each word is taken from the bank, so a word used twice needs two tiles.
                val bank = tilesOf(exercise).toMutableList()
                val built = words(accepted).map { word ->
                    val tile = bank.firstOrNull { it.equals(word, ignoreCase = true) }
                    assertTrue(tile != null, "Exercise ${exercise.id} has no tile left for \"$word\" of \"$accepted\"")
                    bank.remove(tile)
                    tile
                }

                assertEquals(
                    Feedback(correct = true, correctAnswer = accepted),
                    checkTiles(built, exercise.acceptedAnswers),
                    "Exercise ${exercise.id}",
                )
            }
        }
    }

    @Test
    fun `every Accepted Answer of every conjugation Exercise shipped is correct when typed exactly, with no accent warning`() {
        val conjugations = course.path.flatMap { it.exercises }.filterIsInstance<Conjugation>()
        assertTrue(conjugations.isNotEmpty())

        conjugations.forEach { exercise ->
            exercise.acceptedAnswers.forEach { accepted ->
                assertEquals(
                    Feedback(correct = true, correctAnswer = accepted),
                    checkTyped(accepted, exercise.acceptedAnswers),
                    "Exercise ${exercise.id}",
                )
            }
        }
    }

    @Test
    fun `every conjugation table shipped agrees with the conjugation Exercises of its verb`() {
        val tables = course.path.flatMap { it.tips }.mapNotNull { it.conjugation }
        val conjugations = course.path.flatMap { it.exercises }.filterIsInstance<Conjugation>()
        assertTrue(tables.isNotEmpty())

        tables.forEach { table ->
            conjugations.filter { it.verb == table.verb }.forEach { exercise ->
                val form = table.forms.getValue(exercise.person)
                assertEquals(
                    Feedback(correct = true, correctAnswer = form),
                    checkTyped(form, exercise.acceptedAnswers),
                    "Exercise ${exercise.id} against the table of ${table.verb}",
                )
            }
        }
    }
}
