package com.polisheasy.engine

import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthoredContentTest {
    private val contentDir = File(System.getProperty("contentDir"))
    private val course = loadCourse { path -> File(contentDir, path).readText() }

    /** The correct answer to the Exercise shown, whichever form it is shown in. */
    private fun correctAnswer(step: Step.ShowExercise): Answer =
        when (val exercise = course.exercises.getValue(step.exerciseId)) {
            is ChooseTranslation -> Answer.Option(exercise.correctOption)
            is TypeAnswer -> Answer.Typed(exercise.acceptedAnswers.first())
            is WordTiles -> Answer.Tiles(words(exercise.acceptedAnswers.first()))
            is Conjugation -> Answer.Typed(exercise.acceptedAnswers.first())
            is ListenChoose -> {
                val options = when (val view = step.exercise) {
                    is ExerciseView.ListenChoose -> view.options
                    is ExerciseView.ChoosePolish -> view.options
                    else -> error("Exercise ${exercise.id} is shown as $view")
                }
                Answer.Option(options.indexOf(exercise.text))
            }
            is ListenType -> Answer.Typed(exercise.acceptedAnswers.first())
            else -> bookRight(exercise)
        }

    /**
     * Plays from [first], by default the start of today's Daily Set, Review included, answering
     * everything correctly. Returns every Step seen.
     */
    private fun CourseEngine.playCorrectly(first: Step = startDailySet()): List<Step> {
        val seen = mutableListOf(first)
        while (true) {
            when (val step = seen.last()) {
                is Step.ShowTip -> Unit
                is Step.ShowExercise -> assertTrue(answer(correctAnswer(step)).correct, "Exercise ${step.exerciseId}")
                is Step.ShowSummary -> return seen
            }
            seen += next()
        }
    }

    /**
     * One Learner playing the whole course, one Daily Set per day from 2 March, with or without
     * [sound]. Returns each day's Steps.
     */
    private fun playTheCourse(progress: LearnerProgress, clock: FakeClock, sound: Boolean = true): List<List<Step>> =
        course.path.indices.map { day ->
            clock.moveTo(march(2).plusDays(day.toLong()))
            CourseEngine(course, progress, clock, oneADay = { true }).run { playCorrectly(startDailySet(sound)) }
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

        val dayAfter = march(2).plusDays(course.path.size.toLong())
        assertEquals(
            Today.DoneForToday(nextUnlock = dayAfter.atStartOfDay(warsaw), reviewOnlyNext = true),
            CourseEngine(course, progress, clock, oneADay = { true }).today(),
        )
        assertEquals(course.path.size, CourseEngine(course, progress, clock, oneADay = { true }).streak())
    }

    @Test
    fun `after the course, each of the next 7 days has a Review-only Daily Set of 20 different Exercises that plays through to its Summary`() {
        val progress = InMemoryLearnerProgress()
        val clock = FakeClock()
        playTheCourse(progress, clock)
        val lastDay = march(2).plusDays(course.path.size - 1L)

        (1L..7L).forEach { day ->
            clock.moveTo(lastDay.plusDays(day))
            val engine = CourseEngine(course, progress, clock, oneADay = { true })
            assertEquals(Today.ReviewOnlyAvailable, engine.today(), "Day $day after the course")

            val steps = engine.playCorrectly()

            val exercises = steps.filterIsInstance<Step.ShowExercise>()
            assertTrue(steps.none { it is Step.ShowTip }, "Day $day after the course")
            assertEquals(20, exercises.size, "Day $day after the course")
            assertTrue(exercises.all { it.review }, "Day $day after the course")
            assertEquals(20, exercises.map { it.exercise }.distinct().size, "Day $day after the course")
            assertEquals(Step.ShowSummary(20, 20, emptyList(), reviewOnly = true), steps.last(), "Day $day after the course")
        }
        assertEquals(course.path.size, progress.completedDailySets().size)
        assertEquals(7, progress.reviewOnlyDays().size)
        assertEquals(course.path.size + 7, CourseEngine(course, progress, clock, oneADay = { true }).streak())
    }

    @Test
    fun `with sound off, every Daily Set shipped and the next 7 Review-only Daily Sets play to their Summary with the same Exercises and nothing to hear`() {
        fun play(sound: Boolean): List<List<Step>> {
            val progress = InMemoryLearnerProgress()
            val clock = FakeClock()
            val lastDay = march(2).plusDays(course.path.size - 1L)
            return playTheCourse(progress, clock, sound) + (1L..7L).map { day ->
                clock.moveTo(lastDay.plusDays(day))
                CourseEngine(course, progress, clock, oneADay = { true }).run { playCorrectly(startDailySet(sound)) }
            }
        }
        fun ids(steps: List<Step>) = steps.filterIsInstance<Step.ShowExercise>().map { it.exerciseId }
        val heard = play(sound = true)
        val silent = play(sound = false)

        assertEquals(course.path.size + 7, silent.size)
        silent.zip(heard).forEachIndexed { index, (steps, withSound) ->
            val day = "Day ${index + 1}"
            assertEquals(ids(withSound), ids(steps), day)
            assertEquals(withSound.count { it is Step.ShowTip }, steps.count { it is Step.ShowTip }, day)
            steps.filterIsInstance<Step.ShowExercise>().forEach { step ->
                val view = step.exercise
                assertTrue(view !is ExerciseView.ListenChoose && view !is ExerciseView.ListenType, "$day shows ${step.exerciseId} to be heard")
                if (view is ExerciseView.ChooseTranslation) assertEquals(null, view.promptAudio, "$day, ${step.exerciseId}")
            }
            val summary = steps.last() as Step.ShowSummary
            assertEquals(summary.exercises, summary.correctAnswers, day)
        }
        // Every listening Exercise was read at least once, on the day that introduces it.
        val read = silent.flatMap(::ids).toSet()
        assertTrue(allExercises.filter { it is ListenChoose || it is ListenType }.all { it.id in read })
    }

    @Test
    fun `with sound off, no feedback on any Exercise shipped carries a Clip`() {
        val progress = InMemoryLearnerProgress()
        val clock = FakeClock()
        course.path.forEachIndexed { day, dailySet -> progress.recordCompleted(dailySet.id, on = march(2).plusDays(day.toLong())) }
        clock.moveTo(march(2).plusDays(course.path.size.toLong()))
        val engine = CourseEngine(course, progress, clock, oneADay = { true })

        course.path.forEach { dailySet ->
            var step = engine.startReplay(dailySet.id, sound = false)
            while (step !is Step.ShowSummary) {
                if (step is Step.ShowExercise) {
                    assertEquals(null, engine.answer(correctAnswer(step)).audio, "Exercise ${step.exerciseId}")
                }
                step = engine.next()
            }
        }
    }

    @Test
    fun `every reading answer shipped is correct when its Exercise is read and typed exactly, with no accent warning, and wrong when it is heard`() {
        val withReadingAnswers = allExercises.filterIsInstance<ListenType>().filter { it.readingAnswers.isNotEmpty() }
        assertTrue(withReadingAnswers.isNotEmpty())
        val progress = InMemoryLearnerProgress()
        course.path.forEachIndexed { day, dailySet -> progress.recordCompleted(dailySet.id, on = march(2).plusDays(day.toLong())) }
        val clock = FakeClock().apply { moveTo(march(2).plusDays(course.path.size.toLong())) }

        /** What the Learner is told for [typed] at [exercise], reached in a Replay of its Daily Set. */
        fun feedback(exercise: ListenType, typed: String, sound: Boolean): Feedback {
            val engine = CourseEngine(course, progress, clock, oneADay = { true })
            var step = engine.startReplay(course.path.first { exercise in it.exercises }.id, sound)
            while (true) {
                if (step is Step.ShowExercise) {
                    if (step.exerciseId == exercise.id) return engine.answer(Answer.Typed(typed))
                    engine.answer(correctAnswer(step))
                }
                step = engine.next()
            }
        }

        withReadingAnswers.forEach { exercise ->
            exercise.readingAnswers.forEach { answer ->
                assertEquals(
                    Feedback(correct = true, correctAnswer = answer),
                    feedback(exercise, answer, sound = false),
                    "Exercise ${exercise.id}, read",
                )
                assertEquals(false, feedback(exercise, answer, sound = true).correct, "Exercise ${exercise.id}, heard")
            }
        }
    }

    @Test
    fun `a listen-and-type Exercise that is read looks like no typing Exercise with other answers`() {
        fun answers(all: List<String>) = all.map(::normalizeAnswer).toSet()
        val typing = allExercises.filterIsInstance<TypeAnswer>()
            .map { Triple(it.id, it.prompt.getValue("en"), answers(it.acceptedAnswers)) }
        val read = allExercises.filterIsInstance<ListenType>()
            .map { Triple(it.id, it.meaning.getValue("en"), answers(it.acceptedAnswers + it.readingAnswers)) }
        assertTrue(read.isNotEmpty())

        (typing + read).groupBy { it.second.trim().lowercase(Locale.ROOT) }.values
            // Two typing Exercises alone are not about the reading form.
            .filter { alike -> alike.any { it in read } }
            .forEach { alike ->
                assertEquals(1, alike.map { it.third }.distinct().size, "Exercises ${alike.map { it.first }} ask \"${alike.first().second}\"")
            }
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
    fun `played one per day, Unit 1 has 0, 5, 6 then 8 Review Exercises a day, and every later day has 8`() {
        val days = playTheCourse(InMemoryLearnerProgress(), FakeClock())

        assertEquals(
            listOf(0, 5, 6, 8, 8, 8, 8) + List(8) { 8 } + List(8) { 8 } + List(5) { 8 } + List(9) { 8 } + List(12) { 8 } + List(10) { 8 } + List(10) { 8 } + List(14) { 8 } + List(17) { 8 } + List(15) { 8 } + List(17) { 8 } + List(17) { 8 } + List(14) { 8 },
            days.map { steps -> steps.count { it is Step.ShowExercise && it.review } },
        )
        assertEquals(
            listOf(8, 7, 6, 7, 6, 6, 6) + listOf(7, 7, 8, 7, 7, 6, 7, 7) + listOf(7, 7, 6, 7, 6, 6, 6, 6) +
                listOf(8, 8, 8, 3, 0) + listOf(5, 0, 7, 6, 8, 8, 1, 1, 0) +
                listOf(6, 7, 4, 4, 7, 6, 4, 8, 0, 5, 7, 0) + listOf(8, 8, 7, 4, 8, 8, 6, 8, 4, 0) +
                listOf(8, 8, 8, 7, 8, 8, 8, 8, 8, 0) + listOf(3, 5, 5, 7, 7, 8, 8, 8, 6, 6, 4, 8, 8, 0) +
                listOf(7, 7, 6, 8, 3, 6, 8, 8, 7, 7, 5, 8, 8, 7, 8, 8, 5) +
                listOf(8, 8, 8, 8, 8, 8, 8, 8, 8, 7, 8, 8, 8, 8, 0) +
                listOf(8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 0) +
                listOf(8, 8, 8, 8, 6, 6, 8, 8, 8, 6, 4, 1, 7, 8, 8, 6, 4) +
                listOf(8, 8, 8, 8, 7, 8, 8, 8, 8, 7, 8, 8, 8, 8),
            days.map { steps -> (steps.last() as Step.ShowSummary).itemsLearned.size },
        )
    }

    @Test
    fun `a new Learner's Path starts with Starter, Units 1 to 3 of 7, 8 and 8 Daily Sets, numbered 1 to 23, the first one today's`() {
        val level = CourseEngine(course, InMemoryLearnerProgress(), FakeClock(), oneADay = { true }).path().levels.first()

        assertEquals("Starter", level.title)
        assertEquals(listOf(1, 2, 3), level.units.map { it.number })
        assertEquals(listOf("Sounds and greetings", "About me", "Numbers and family"), level.units.map { it.title })
        assertEquals(listOf(7, 8, 8), level.units.map { it.dailySets.size })
        val dailySets = level.units.flatMap { it.dailySets }
        assertEquals(course.levels.first().units.flatMap { u -> u.dailySets.map { it.id } }, dailySets.map { it.id })
        assertEquals((1..23).toList(), dailySets.map { it.number })
        assertEquals(
            listOf(DailySetState.TODAY) + List(22) { DailySetState.LOCKED },
            dailySets.map { it.state },
        )
    }

    @Test
    fun `the book's Level has chapters 0 to 26 with their Polish titles, chapter 0 has five sections, chapter 1 nine, chapter 2 twelve, chapter 3 ten, chapter 4 ten and chapter 5 fourteen`() {
        val shelves = CourseEngine(course, InMemoryLearnerProgress(), FakeClock()).shelves()

        assertEquals(listOf("a1", "kpk1"), shelves.map { it.id })
        val book = shelves[1].lessons
        assertEquals((0..26).toList(), book.map { it.number })
        assertEquals("Dzień dobry!", book[0].title)
        assertEquals("Pierwszy dzień w szkole", book[1].title)
        assertEquals("The first day at school", book[1].subtitle)
        assertEquals(5, book[0].sections)
        assertEquals(9, book[1].sections)
        assertEquals("Cześć, skąd jesteś?", book[2].title)
        assertEquals(12, book[2].sections)
        assertEquals("Mami, kto to jest?", book[3].title)
        assertEquals(10, book[3].sections)
        assertEquals("Jaki jesteś?", book[4].title)
        assertEquals(10, book[4].sections)
        assertEquals("Jesteś instruktorem tanga?", book[5].title)
        assertEquals(14, book[5].sections)
        assertEquals("Co robisz? Nudzę się!", book[6].title)
        assertEquals(17, book[6].sections)
        assertEquals("Małe zakupy", book[7].title)
        assertEquals(15, book[7].sections)
        assertEquals("Mami, jesteś głodna?", book[8].title)
        assertEquals(17, book[8].sections)
        assertEquals("Lubisz marchewkę?", book[9].title)
        assertEquals(17, book[9].sections)
        assertEquals("Uwielbiam polskie jedzenie!", book[10].title)
        assertEquals(14, book[10].sections)
        assertTrue(book.take(11).none { it.coming })
        assertTrue(book.drop(11).all { it.coming })
        assertEquals(Strand.entries.toSet(), course.levels[1].units[1].strands.keys)
    }

    @Test
    fun `every Daily Set of Units 1 to 3 has a title on the Path, and no two in a Unit share one`() {
        val units = CourseEngine(course, InMemoryLearnerProgress(), FakeClock(), oneADay = { true }).path().levels.first().units

        units.forEach { unit ->
            val titles = unit.dailySets.map { it.title }
            assertTrue(titles.none { it.isNullOrBlank() }, "Unit ${unit.number} has a Daily Set with no title: $titles")
            assertEquals(titles.distinct(), titles, "Unit ${unit.number}")
        }
        assertEquals("How Polish is read", units.first().dailySets.first().title)
    }

    @Test
    fun `every Daily Set shipped with the app can be replayed to its Summary, and nothing saved changes`() {
        val progress = InMemoryLearnerProgress()
        val clock = FakeClock()
        course.path.forEachIndexed { day, dailySet -> progress.recordCompleted(dailySet.id, on = march(2).plusDays(day.toLong())) }
        clock.moveTo(march(2).plusDays(course.path.size.toLong()))
        val completed = progress.completedDailySets()
        val engine = CourseEngine(course, progress, clock, oneADay = { true })

        course.path.forEach { dailySet ->
            val steps = engine.playCorrectly(engine.startReplay(dailySet.id))

            assertTrue(steps.all { it.isReplay }, "Daily Set ${dailySet.id}")
            assertEquals(dailySet.tips.size, steps.count { it is Step.ShowTip }, "Tips of ${dailySet.id}")
            assertEquals(
                Step.ShowSummary(dailySet.exercises.size, dailySet.exercises.size, replay = true),
                steps.last(),
                "Daily Set ${dailySet.id}",
            )
        }

        assertEquals(completed, progress.completedDailySets())
        assertEquals(emptySet(), progress.reviewOnlyDays())
        assertEquals(null, progress.dailySetInProgress())
        assertEquals(emptyMap(), progress.itemProgress())
    }

    @Test
    fun `Starter has 3 Units of 7, 8 and 8 Daily Sets`() {
        assertEquals(listOf(7, 8, 8), course.levels.first().units.map { it.dailySets.size })
    }

    @Test
    fun `every form of być, mieć and mówić is an Item, introduced after a Tip that shows its verb's table, and practised by a conjugation Exercise`() {
        val verbs = setOf("być", "mieć", "mówić")
        val tables = course.path.flatMap { it.tips }.mapNotNull { it.conjugation }.filter { it.verb in verbs }.distinct()
        assertEquals(verbs, tables.map { it.verb }.toSet())

        tables.forEach { table ->
            table.forms.forEach { (person, form) ->
                val item = course.items.singleOrNull { it.text == form }
                assertTrue(item != null, "The form $form of ${table.verb} is not the text of exactly one Item")
                val dailySet = course.path.first { item in course.introducedIn(it) }
                val position = { id: String -> dailySet.exercises.indexOfFirst { it.id == id } }
                val introduction = dailySet.exercises.indexOfFirst { item!!.id in it.items }
                assertTrue(
                    dailySet.tips.any { it.conjugation?.verb == table.verb && position(it.before) <= introduction },
                    "${dailySet.id} introduces $form without first showing the table of ${table.verb}",
                )
                assertTrue(
                    allExercises.any { it is Conjugation && it.verb == table.verb && it.person == person && item!!.id in it.items },
                    "No conjugation Exercise of ${table.verb} practises $form",
                )
            }
        }
    }

    @Test
    fun `Unit 2 reuses the six być Item ids an early build shipped`() {
        assertEquals(
            mapOf(
                "byc-jestem" to "jestem", "byc-jestes" to "jesteś", "byc-jest" to "jest",
                "byc-jestesmy" to "jesteśmy", "byc-jestescie" to "jesteście", "byc-sa" to "są",
            ),
            course.items.filter { it.id.startsWith("byc-") }.associate { it.id to it.text },
        )
    }

    @Test
    fun `no two conjugation Exercises look the same to the Learner`() {
        val alike = allExercises.filterIsInstance<Conjugation>().groupBy { Triple(it.verb, it.person, it.sentence) }
        alike.values.filter { it.size > 1 }.forEach { group ->
            assertTrue(false, "Conjugation Exercises ${group.map { it.id }} look the same")
        }
    }

    // The authoring rules of docs/authoring.md that a test can hold the content to.

    private val allExercises = course.path.flatMap { it.exercises }

    private fun lowerCaseWords(text: String): Set<String> = words(text).map { it.lowercase(Locale.ROOT) }.toSet()

    @Test
    fun `every Item is introduced by a translation choice that shows its text, lists only that Item and is answered by its meaning`() {
        course.path.forEach { dailySet ->
            course.introducedIn(dailySet).forEach { item ->
                val first = dailySet.exercises.first { item.id in it.items }
                assertTrue(
                    first is ChooseTranslation && first.prompt == item.text && first.items == listOf(item.id) &&
                        first.options[first.correctOption] == item.meaning,
                    "Item ${item.id} is first practised by Exercise ${first.id}, which does not introduce it",
                )
            }
        }
    }

    @Test
    fun `every word a Learner must read, hear or build comes from an Item its Exercise lists, and every listed Item is there in full`() {
        allExercises.forEach { exercise ->
            // What the Learner is shown, hears, or has to produce: for typing and tiles, the first Accepted Answer.
            // A text or a dialogue is read whole, so every word of it counts, but it may hold words, such as names, that no Item has.
            val text = exercise is TrueFalse || exercise is OrderLines
            val shown = if (text || exercise is MatchPairs) spoken(exercise).flatMap(::lowerCaseWords).toSet() else lowerCaseWords(spoken(exercise).first())
            // An Item is there in full when the words of its text, or of one of its forms, all are.
            val listed = exercise.items.map { id -> course.itemsById.getValue(id).spellings.map(::lowerCaseWords) }

            if (!text) assertEquals(emptySet(), shown - listed.flatten().flatten().toSet(), "Words of Exercise ${exercise.id} that none of its Items has")
            exercise.items.zip(listed).forEach { (id, spellings) ->
                assertTrue(spellings.any { (it - shown).isEmpty() }, "Words of Item $id that Exercise ${exercise.id} does not use: ${spellings.first() - shown}")
            }
        }
    }

    @Test
    fun `every Item is practised by 2 types of Exercise on the day it is introduced and by 3 Exercises in all`() {
        course.path.forEach { dailySet ->
            course.introducedIn(dailySet).forEach { item ->
                val sameDay = dailySet.exercises.filter { item.id in it.items }.map { it::class }.distinct()
                assertTrue(sameDay.size >= 2, "Item ${item.id} is practised by only one type of Exercise in ${dailySet.id}")
                val inAll = allExercises.count { item.id in it.items }
                assertTrue(inAll >= 3, "Item ${item.id} is practised by only $inAll Exercises")
            }
        }
    }

    @Test
    fun `every Item is practised by an Exercise that is not a listening one`() {
        course.items.forEach { item ->
            assertTrue(
                allExercises.any { item.id in it.items && it !is ListenChoose && it !is ListenType },
                "Item ${item.id} can only be practised by listening",
            )
        }
    }

    @Test
    fun `every Daily Set mixes at least 4 types of Exercise, and the course uses all 10`() {
        course.path.forEach { dailySet ->
            val types = dailySet.exercises.map { it::class }.distinct()
            assertTrue(types.size >= 4, "Daily Set ${dailySet.id} has only ${types.size} types of Exercise")
        }
        assertEquals(10, allExercises.map { it::class }.distinct().size)
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

    // What Unit 3 (numbers and family) must hold.

    /**
     * Every text the course can speak, and every reading answer, which is typed but never spoken,
     * each as its lower-case words. Covers every Accepted Answer.
     */
    private val spokenSentences: List<List<String>> =
        allExercises.flatMap { spoken(it) + (it as? ListenType)?.readingAnswers.orEmpty() }
            // One sentence at a time, so that "Nie, nie ma. Jest kanapka." ends its nie ma.
            .flatMap { text -> text.split(Regex("(?<=[.!?])\\s+")) }
            .distinct().map { text -> words(text).map { it.lowercase(Locale.ROOT) } }

    private val numbers = listOf(
        "zero", "jeden", "dwa", "trzy", "cztery", "pięć", "sześć", "siedem", "osiem", "dziewięć", "dziesięć",
        "jedenaście", "dwanaście", "trzynaście", "czternaście", "piętnaście", "szesnaście", "siedemnaście",
        "osiemnaście", "dziewiętnaście", "dwadzieścia",
    )
    private val tens = listOf(
        "dwadzieścia", "trzydzieści", "czterdzieści", "pięćdziesiąt", "sześćdziesiąt", "siedemdziesiąt", "osiemdziesiąt", "dziewięćdziesiąt", "sto",
    )
    private val hundreds = listOf("dwieście", "trzysta", "czterysta", "pięćset", "sześćset", "siedemset", "osiemset", "dziewięćset", "tysiąc")
    private val allNumbers = numbers + tens + hundreds
    private val yearWords = setOf("rok", "lata", "lat")
    private val moneyWords = mapOf("złoty" to listOf("złoty", "złote", "złotych"), "grosz" to listOf("grosz", "grosze", "groszy"))
    private val formsOfMiec = setOf("mam", "masz", "ma", "mamy", "macie", "mają")

    private fun dailySetIntroducing(text: String): DailySet {
        val item = course.items.single { it.text == text }
        return course.path.first { item in course.introducedIn(it) }
    }

    @Test
    fun `each number from 0 to 20 is an Item that the Learner reads, hears and types`() {
        numbers.forEach { number ->
            val item = course.items.singleOrNull { it.text == number }
            assertTrue(item != null, "$number is not the text of exactly one Item")
            val practice = allExercises.filter { item!!.id in it.items }
            assertTrue(practice.any { it is ChooseTranslation }, "$number is never read")
            assertTrue(practice.any { it is ListenChoose || it is ListenType }, "$number is never heard")
            assertTrue(practice.any { it is TypeAnswer || it is ListenType }, "$number is never typed")
        }
    }

    @Test
    fun `a number before złoty or grosz takes the form that goes with it`() {
        moneyWords.forEach { (word, forms) -> assertTrue(course.items.any { it.text == word && it.forms == forms.drop(1) }, "$word is not an Item with $forms") }
        val counted = spokenSentences.flatMap { sentence ->
            sentence.indices.drop(1).filter { index -> sentence[index - 1] in allNumbers && moneyWords.values.any { sentence[index] in it } }.map { index ->
                val forms = moneyWords.values.single { sentence[index] in it }
                val one = sentence[index - 1] == "jeden" && sentence.getOrNull(index - 2) !in allNumbers
                val right = when {
                    one -> forms[0]
                    sentence[index - 1] in setOf("dwa", "trzy", "cztery") -> forms[1]
                    else -> forms[2]
                }
                assertEquals(right, sentence[index], "\"${sentence.joinToString(" ")}\"")
                sentence[index]
            }
        }
        assertTrue(counted.containsAll(setOf("złote", "złotych", "groszy")))
    }

    @Test
    fun `a number before rok, lata or lat is one that goes with it`() {
        yearWords.forEach { word -> assertTrue(course.items.any { it.text == word }, "$word is not an Item") }
        assertTrue(
            dailySetIntroducing("lata").tips.any { tip -> yearWords.all { it in lowerCaseWords(tip.body.getValue("en")) } },
            "No Tip of the Daily Set that introduces lata names rok, lata and lat",
        )

        // The number is the word before, or the two words before when a ten comes first: dwadzieścia jeden lat.
        val counted = spokenSentences.flatMap { sentence ->
            sentence.indices.drop(1).filter { sentence[it] in yearWords && sentence[it - 1] in allNumbers }.map { index ->
                val number = listOfNotNull(sentence.getOrNull(index - 2)?.takeIf { it in tens }, sentence[index - 1])
                number to sentence[index]
            }
        }
        counted.forEach { (number, word) ->
            val right = when {
                number == listOf("jeden") -> "rok"
                number.last() in setOf("dwa", "trzy", "cztery") -> "lata"
                else -> "lat"
            }
            assertEquals(right, word, "After ${number.joinToString(" ")}")
        }
        assertEquals(yearWords, counted.map { it.second }.toSet())
    }

    @Test
    fun `a number counts only years, money, lottery tickets and, since chapter 9, things in the plural, dwie a feminine one`() {
        // A house number is joined to its flat's with przez: dziesięć przez trzy is 10/3.
        val allowed = allNumbers + yearWords + moneyWords.values.flatten() + listOf("plus", "minus", "razy", "równa", "to", "jest", "przez", "kupon")
        // Chapter 9 counts things: jeden pomidor, dwa banany, dwie papryki, trzy słodkie pomarańcze, dwa kilo. The thing is the
        // plural form of an Item with a gender, perhaps after an adjective; dwie goes with a feminine one, dwa with the others.
        val plurals = course.items.filter { it.id in course.genders && it.forms.size == 1 }.associate { it.forms.single() to course.genders.getValue(it.id) }
        val adjectives = course.items.filter { it.forms.size == 2 }.flatMap { it.forms }.toSet()
        val counted = mutableSetOf<String>()
        spokenSentences.forEach { sentence ->
            sentence.indices.filter { sentence[it] in allNumbers + "dwie" }.forEach { index ->
                val next = sentence.getOrNull(index + 1) ?: return@forEach
                if (next in allowed || next == "kilo") return@forEach
                val thing = if (next in adjectives) sentence.getOrNull(index + 2) else next
                // Jeden goes with one masculine thing: jeden pomidor.
                val gender = if (sentence[index] == "jeden") course.items.singleOrNull { it.text == thing }?.let { course.genders[it.id] }?.takeIf { it == Gender.MASCULINE } else plurals[thing]
                assertTrue(gender != null, "\"${sentence.joinToString(" ")}\" has $next after ${sentence[index]}")
                if (sentence[index] in setOf("dwa", "dwie")) {
                    assertEquals(if (gender == Gender.FEMININE) "dwie" else "dwa", sentence[index], "\"${sentence.joinToString(" ")}\"")
                }
                counted += sentence[index]
            }
        }
        assertTrue(counted.containsAll(setOf("dwa", "dwie", "trzy", "cztery")))
    }

    @Test
    fun `mój, moja, moje, twój, twoja and twoje agree with the gender of the noun after them`() {
        val masculine = setOf("brat", "tata", "syn", "mąż", "dziadek", "problem", "numer", "czas", "chłopak", "adres", "plecak", "słownik", "kolega", "gospodarz", "kot", "owoc")
        val feminine = setOf("mama", "siostra", "córka", "żona", "babcia", "rodzina", "dziewczyna", "torba", "koleżanka", "dentystka", "gospodyni", "potrawa")
        val neuter = setOf("dziecko", "pytanie", "rodzeństwo", "hobby")
        val possessives = mapOf(
            "mój" to masculine, "twój" to masculine, "moja" to feminine, "twoja" to feminine, "moje" to neuter, "twoje" to neuter,
        )
        val adjectives = course.items.filter { it.forms.size == 2 && (it.text.endsWith("y") || it.text.endsWith("i")) }
            .flatMap { listOf(it.text) + it.forms }.toSet()
        possessives.keys.forEach { word -> assertTrue(course.items.any { it.text == word }, "$word is not an Item") }
        assertTrue(
            dailySetIntroducing("mój").tips.any { tip -> lowerCaseWords(tip.body.getValue("en")).containsAll(setOf("mój", "moja")) },
            "No Tip of the Daily Set that introduces mój names mój and moja",
        )

        val used = mutableSetOf<String>()
        spokenSentences.filter { it.size > 1 }.forEach { sentence ->
            sentence.forEachIndexed { index, word ->
                val nouns = possessives[word] ?: return@forEachIndexed
                // An adjective may come between: moja ulubiona potrawa.
                val next = sentence.getOrNull(index + 1) ?: return@forEachIndexed
                val noun = if (next in adjectives) sentence.getOrNull(index + 2) ?: return@forEachIndexed else next
                assertTrue(noun in nouns, "\"${sentence.joinToString(" ")}\" has $word before $noun")
                used += word
            }
        }
        assertEquals(possessives.keys, used)
    }

    @Test
    fun `brata, siostrę, syna and córkę are only what someone has or knows, and a negated mieć takes the genitive`() {
        val had = setOf("brata", "siostrę", "syna", "córkę")
        had.forEach { word -> assertTrue(course.items.any { it.text == word }, "$word is not an Item") }
        val fixedPhrases = listOf(listOf("nie", "ma", "za", "co"), listOf("nie", "ma", "sprawy"))
        val negatedObjects = setOf("ani", "aparatu", "bananów", "brata", "chleba", "drobnych", "fasoli", "kantoru", "kompotu", "kwiaciarni", "pieniędzy", "piwa", "problemu", "soku", "wody")

        spokenSentences.forEach { sentence ->
            val text = sentence.joinToString(" ")
            sentence.forEachIndexed { index, word ->
                if (word in had && sentence.size > 1) {
                    val before = sentence.getOrNull(index - 1)
                    // Since chapter 7 an adjective in the accusative can come between: mam inteligentnego brata, znam twojego brata.
                    val agrees = before != null && (before.endsWith("ego") || before.endsWith("ą"))
                    assertTrue(agrees || before in formsOfMiec + listOf("i", "rodzeństwo"), "\"$text\" has $word after $before")
                }
                // "nie ma za co" and "nie ma sprawy" are fixed phrases of Unit 1, not someone who has nothing.
                val fixedPhrase = fixedPhrases.any { phrase -> sentence.drop(index).take(phrase.size) == phrase }
                if (word == "nie" && sentence.getOrNull(index + 1) in formsOfMiec && !fixedPhrase) {
                    // Chapter 7 brings nie ma with the genitive: each one is checked by hand and listed here.
                    assertTrue(index + 2 >= sentence.size || sentence[index + 2] in negatedObjects, "\"$text\" negates mieć with an object not known to be genitive")
                }
            }
        }
    }

    @Test
    fun `every form of być, mieć and mówić is listed for the Polish check as a verb form claim on an Item`() {
        val claims = polishContent(DirectoryContent(contentDir)).verbForms

        listOf("być", "mieć", "mówić").forEach { verb ->
            Person.entries.forEach { person ->
                assertTrue(
                    claims.any { claim -> claim.verb == verb && claim.person == person && claim.uses.any { it.role == PolishRole.ITEM } },
                    "No Item is claimed to be the form of $verb for ${person.pronouns}",
                )
            }
        }
    }
}
