package com.polisheasy.engine

import java.io.File
import java.util.Locale
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
    fun `played one per day, Unit 1 has 0, 5, 6 then 8 Review Exercises a day, and every day of Units 2 and 3 has 8`() {
        val days = playTheCourse(InMemoryLearnerProgress(), FakeClock())

        assertEquals(
            listOf(0, 5, 6, 8, 8, 8, 8) + List(8) { 8 } + List(8) { 8 },
            days.map { steps -> steps.count { it is Step.ShowExercise && it.review } },
        )
        assertEquals(
            listOf(8, 7, 6, 7, 6, 6, 6) + listOf(7, 7, 8, 7, 7, 6, 7, 7) + listOf(7, 7, 6, 7, 6, 6, 6, 6),
            days.map { steps -> (steps.last() as Step.ShowSummary).itemsLearned.size },
        )
    }

    @Test
    fun `the course has 3 Units of 7, 8 and 8 Daily Sets`() {
        assertEquals(listOf(7, 8, 8), course.levels.single().units.map { it.dailySets.size })
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
            val shown = lowerCaseWords(spoken(exercise).first())
            val listed = exercise.items.map { lowerCaseWords(course.itemsById.getValue(it).text) }

            assertEquals(emptySet(), shown - listed.flatten().toSet(), "Words of Exercise ${exercise.id} that none of its Items has")
            exercise.items.zip(listed).forEach { (id, itemWords) ->
                assertEquals(emptySet(), itemWords - shown, "Words of Item $id that Exercise ${exercise.id} does not use")
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
    fun `every Daily Set mixes at least 4 types of Exercise, and the course uses all 6`() {
        course.path.forEach { dailySet ->
            val types = dailySet.exercises.map { it::class }.distinct()
            assertTrue(types.size >= 4, "Daily Set ${dailySet.id} has only ${types.size} types of Exercise")
        }
        assertEquals(6, allExercises.map { it::class }.distinct().size)
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

    /** Every text the course can speak, each as its lower-case words. Covers every Accepted Answer. */
    private val spokenSentences: List<List<String>> =
        allExercises.flatMap(::spoken).distinct().map { text -> words(text).map { it.lowercase(Locale.ROOT) } }

    private val numbers = listOf(
        "zero", "jeden", "dwa", "trzy", "cztery", "pięć", "sześć", "siedem", "osiem", "dziewięć", "dziesięć",
        "jedenaście", "dwanaście", "trzynaście", "czternaście", "piętnaście", "szesnaście", "siedemnaście",
        "osiemnaście", "dziewiętnaście", "dwadzieścia",
    )
    private val yearWords = setOf("rok", "lata", "lat")
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
    fun `a number before rok, lata or lat is one that goes with it`() {
        yearWords.forEach { word -> assertTrue(course.items.any { it.text == word }, "$word is not an Item") }
        assertTrue(
            dailySetIntroducing("lata").tips.any { tip -> yearWords.all { it in lowerCaseWords(tip.body.getValue("en")) } },
            "No Tip of the Daily Set that introduces lata names rok, lata and lat",
        )

        val counted = spokenSentences.flatMap { sentence ->
            sentence.zipWithNext().filter { (number, word) -> number in numbers && word in yearWords }
        }
        counted.forEach { (number, word) ->
            val right = when (number) {
                "jeden" -> "rok"
                "dwa", "trzy", "cztery" -> "lata"
                else -> "lat"
            }
            assertEquals(right, word, "After $number")
        }
        assertEquals(yearWords, counted.map { it.second }.toSet())
    }

    @Test
    fun `a number is never used to count anything but years`() {
        val allowed = numbers + yearWords + listOf("plus", "to", "jest")
        spokenSentences.forEach { sentence ->
            sentence.zipWithNext().filter { (number, _) -> number in numbers }.forEach { (number, next) ->
                assertTrue(next in allowed, "\"${sentence.joinToString(" ")}\" has $next after $number")
            }
        }
    }

    @Test
    fun `mój, moja, moje, twój, twoja and twoje agree with the gender of the noun after them`() {
        val masculine = setOf("brat", "tata", "syn", "mąż", "dziadek", "problem", "numer", "czas")
        val feminine = setOf("mama", "siostra", "córka", "żona", "babcia", "rodzina")
        val neuter = setOf("dziecko", "pytanie", "rodzeństwo")
        val possessives = mapOf(
            "mój" to masculine, "twój" to masculine, "moja" to feminine, "twoja" to feminine, "moje" to neuter, "twoje" to neuter,
        )
        possessives.keys.forEach { word -> assertTrue(course.items.any { it.text == word }, "$word is not an Item") }
        assertTrue(
            dailySetIntroducing("mój").tips.any { tip -> lowerCaseWords(tip.body.getValue("en")).containsAll(setOf("mój", "moja")) },
            "No Tip of the Daily Set that introduces mój names mój and moja",
        )

        val used = mutableSetOf<String>()
        spokenSentences.filter { it.size > 1 }.forEach { sentence ->
            sentence.forEachIndexed { index, word ->
                val nouns = possessives[word] ?: return@forEachIndexed
                val noun = sentence.getOrNull(index + 1) ?: return@forEachIndexed
                assertTrue(noun in nouns, "\"${sentence.joinToString(" ")}\" has $word before $noun")
                used += word
            }
        }
        assertEquals(possessives.keys, used)
    }

    @Test
    fun `brata, siostrę, syna and córkę are only what someone has, and mieć is never negated with an object`() {
        val had = setOf("brata", "siostrę", "syna", "córkę")
        had.forEach { word -> assertTrue(course.items.any { it.text == word }, "$word is not an Item") }

        spokenSentences.forEach { sentence ->
            val text = sentence.joinToString(" ")
            sentence.forEachIndexed { index, word ->
                if (word in had && sentence.size > 1) {
                    val before = sentence.getOrNull(index - 1)
                    assertTrue(before in formsOfMiec + listOf("i", "rodzeństwo"), "\"$text\" has $word after $before")
                }
                // "nie ma za co" is a fixed phrase of Unit 1, not someone who has nothing.
                val fixedPhrase = sentence.drop(index).take(4) == listOf("nie", "ma", "za", "co")
                if (word == "nie" && sentence.getOrNull(index + 1) in formsOfMiec && !fixedPhrase) {
                    assertTrue(index + 2 >= sentence.size, "\"$text\" negates mieć with an object")
                }
            }
        }
    }
}
