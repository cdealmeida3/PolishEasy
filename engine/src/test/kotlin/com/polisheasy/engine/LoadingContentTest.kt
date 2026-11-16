package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LoadingContentTest {
    @Test
    fun `a Daily Set without Tips loads`() {
        val course = FixtureCourse.loadWithDayOne(dayOne(tips = null))

        assertEquals(emptyList(), course.path.first().tips)
    }

    @Test
    fun `a Daily Set with more than two Tips is rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            FixtureCourse.loadWithDayOne(dayOne(tips = listOf("e01", "e02", "e03")))
        }

        assertNamesTheDailySet(failure)
    }

    @Test
    fun `a Tip placed before an Exercise that is not in the Daily Set is rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            FixtureCourse.loadWithDayOne(dayOne(tips = listOf("e99")))
        }

        assertNamesTheDailySet(failure)
    }

    @Test
    fun `two Tips placed before the same Exercise are rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            FixtureCourse.loadWithDayOne(dayOne(tips = listOf("e02", "e02")))
        }

        assertNamesTheDailySet(failure)
    }

    @Test
    fun `a typing Exercise with no Accepted Answers is rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            FixtureCourse.loadWithDayOne(typingDay())
        }

        assertNamesTheDailySet(failure)
        assertTrue("a1-u01-d01-e01" in failure.message.orEmpty(), "Message was: ${failure.message}")
    }

    @Test
    fun `a typing Exercise with a blank or punctuation-only Accepted Answer is rejected`() {
        listOf("", "   ", "?!").forEach { blank ->
            val failure = assertFailsWith<IllegalArgumentException>("Accepted Answer \"$blank\"") {
                FixtureCourse.loadWithDayOne(typingDay("dziękuję", blank))
            }

            assertNamesTheDailySet(failure)
        }
    }

    @Test
    fun `a word tiles Exercise with no Accepted Answers is rejected`() {
        assertWordTilesRejected(tilesDay(accepted = emptyList()))
    }

    @Test
    fun `a word tiles Exercise with a blank or punctuation-only Accepted Answer is rejected`() {
        listOf("", "   ", "?!").forEach { blank ->
            assertWordTilesRejected(tilesDay(listOf("dzień dobry", blank)), "Accepted Answer \"$blank\"")
            assertWordTilesRejected(tilesDay(listOf(blank)), "Only Accepted Answer \"$blank\"")
        }
    }

    @Test
    fun `a word tiles Exercise whose first Accepted Answer is a single word is rejected`() {
        assertWordTilesRejected(tilesDay(listOf("dziękuję")))
        assertWordTilesRejected(tilesDay(listOf("dziękuję!"), distractors = listOf("tak", "nie")))
    }

    @Test
    fun `a word tiles Exercise with a distractor that is not exactly one word is rejected`() {
        listOf("na razie", "", "  ", "?").forEach { distractor ->
            assertWordTilesRejected(tilesDay(listOf("dzień dobry"), listOf("tak", distractor)), "Distractor \"$distractor\"")
        }
    }

    @Test
    fun `a word tiles Exercise with an Accepted Answer that cannot be built from its tiles is rejected`() {
        // A word that is not among the tiles.
        assertWordTilesRejected(tilesDay(listOf("dzień dobry", "dobry wieczór")))
        // A word needed twice, with one tile.
        assertWordTilesRejected(tilesDay(listOf("tak, proszę", "tak, tak, proszę")))
    }

    @Test
    fun `a word tiles Exercise whose second Accepted Answer uses a distractor word loads`() {
        val course = FixtureCourse.loadWithDayOne(
            tilesDay(listOf("dzień dobry", "Dobry wieczór"), distractors = listOf("wieczór")),
        )

        assertEquals(
            WordTiles(
                id = "a1-u01-d01-e01",
                prompt = mapOf("en" to "good morning, how are you?"),
                acceptedAnswers = listOf("dzień dobry", "Dobry wieczór"),
                distractors = listOf("wieczór"),
            ),
            course.path.first().exercises.single(),
        )
    }

    private fun assertWordTilesRejected(day: String, case: String? = null) {
        val failure = assertFailsWith<IllegalArgumentException>(case) { FixtureCourse.loadWithDayOne(day) }

        assertNamesTheDailySet(failure)
        assertTrue("a1-u01-d01-e01" in failure.message.orEmpty(), "Message was: ${failure.message}")
    }

    @Test
    fun `a conjugation Exercise with no Accepted Answers, a blank Accepted Answer or a blank verb is rejected`() {
        assertConjugationRejected(conjugationDay(accepted = emptyList()))
        listOf("", "   ", "?!").forEach { blank ->
            assertConjugationRejected(conjugationDay(listOf("jesteśmy", blank)), "Accepted Answer \"$blank\"")
        }
        listOf("", "  ").forEach { blank ->
            assertConjugationRejected(conjugationDay(listOf("jesteśmy"), verb = blank), "Verb \"$blank\"")
        }
    }

    @Test
    fun `a conjugation Exercise with a person that is not one of the six is rejected`() {
        listOf("ona", "we", "").forEach { person ->
            assertFailsWith<IllegalArgumentException>("Person \"$person\"") {
                FixtureCourse.loadWithDayOne(conjugationDay(listOf("jesteśmy"), person = person))
            }
        }
    }

    @Test
    fun `a conjugation Exercise with a sentence but no sentence meaning, or the reverse, is rejected`() {
        assertConjugationRejected(conjugationDay(listOf("jesteśmy"), sentence = "My ___ w Polsce."))
        assertConjugationRejected(conjugationDay(listOf("jesteśmy"), sentenceMeaning = "We are in Poland."))
    }

    @Test
    fun `a conjugation Exercise whose sentence does not have exactly one gap of three underscores is rejected`() {
        listOf(
            "My jesteśmy w Polsce.",
            "My ___ w ___.",
            "My __ w Polsce.",
            "My ____ w Polsce.",
            "My ______ w Polsce.",
            "My ___ w _ Polsce.",
        ).forEach { sentence ->
            assertConjugationRejected(
                conjugationDay(listOf("jesteśmy"), sentence = sentence, sentenceMeaning = "We are in Poland."),
                "Sentence \"$sentence\"",
            )
        }
    }

    @Test
    fun `a conjugation Exercise whose sentence is only the gap is rejected`() {
        listOf("___", " ___ .", "___?!").forEach { sentence ->
            assertConjugationRejected(
                conjugationDay(listOf("jesteśmy"), sentence = sentence, sentenceMeaning = "We are."),
                "Sentence \"$sentence\"",
            )
        }
    }

    @Test
    fun `a Tip whose conjugation table misses a person, has a blank form or has a blank verb is rejected`() {
        listOf(
            conjugationTable(forms = formsOfByc - "wy"),
            conjugationTable(forms = formsOfByc + ("oni" to " ")),
            conjugationTable(forms = formsOfByc + ("ja" to "")),
            conjugationTable(verb = " "),
        ).forEach { table ->
            val failure = assertFailsWith<IllegalArgumentException>(table) {
                FixtureCourse.loadWithDayOne(conjugationDay(listOf("jesteśmy"), tipTable = table))
            }

            assertNamesTheDailySet(failure)
            assertTrue("a1-u01-d01-t01" in failure.message.orEmpty(), "Message was: ${failure.message}")
        }
    }

    @Test
    fun `a conjugation Exercise with a sentence, and a Tip with a full conjugation table, load`() {
        val course = FixtureCourse.loadWithDayOne(
            conjugationDay(
                listOf("jesteśmy"),
                sentence = "My ___ w Polsce.",
                sentenceMeaning = "We are in Poland.",
                tipTable = conjugationTable(),
            ),
        )

        val dailySet = course.path.first()
        assertEquals(
            Conjugation(
                id = "a1-u01-d01-e01",
                verb = "być",
                meaning = mapOf("en" to "to be"),
                person = Person.WE,
                sentence = "My ___ w Polsce.",
                sentenceMeaning = mapOf("en" to "We are in Poland."),
                acceptedAnswers = listOf("jesteśmy"),
            ),
            dailySet.exercises.single(),
        )
        assertEquals(
            ConjugationTable(
                verb = "być",
                meaning = mapOf("en" to "to be"),
                forms = mapOf(
                    Person.I to "jestem",
                    Person.YOU to "jesteś",
                    Person.HE_SHE_IT to "jest",
                    Person.WE to "jesteśmy",
                    Person.YOU_PLURAL to "jesteście",
                    Person.THEY to "są",
                ),
            ),
            dailySet.tips.single().conjugation,
        )
    }

    private fun assertConjugationRejected(day: String, case: String? = null) {
        val failure = assertFailsWith<IllegalArgumentException>(case) { FixtureCourse.loadWithDayOne(day) }

        assertNamesTheDailySet(failure)
        assertTrue("a1-u01-d01-e01" in failure.message.orEmpty(), "Message was: ${failure.message}")
    }

    @Test
    fun `Items are loaded from the files the course lists, and each Exercise's Items are read`() {
        val course = loadWithItems(
            listOf(
                """{ "items": [ { "id": "dziekuje", "text": "dziękuję", "meaning": { "en": "thank you" } } ] }""",
                """{ "items": [ { "id": "tak", "text": "tak", "meaning": { "en": "yes" } } ] }""",
            ),
            taggedDay("d1", "d1-e1" to listOf("dziekuje", "tak"), "d1-e2" to emptyList()),
        )

        assertEquals(
            listOf(
                Item("dziekuje", "dziękuję", mapOf("en" to "thank you")),
                Item("tak", "tak", mapOf("en" to "yes")),
            ),
            course.items,
        )
        assertEquals(listOf(listOf("dziekuje", "tak"), emptyList()), course.path.single().exercises.map { it.items })
    }

    @Test
    fun `a course that lists no Item files has no Items`() {
        assertEquals(emptyList(), FixtureCourse.load().items)
    }

    @Test
    fun `an Exercise that lists an unknown Item is rejected, naming the Daily Set, the Exercise and the Item`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            loadWithItems(listOf(itemsFile("tak")), taggedDay("d1", "d1-e1" to listOf("tak"), "d1-e2" to listOf("nie")))
        }

        listOf("d1", "d1-e2", "nie").forEach { name ->
            assertTrue(name in failure.message.orEmpty(), "Message was: ${failure.message}")
        }
    }

    @Test
    fun `two Items with the same id are rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            loadWithItems(listOf(itemsFile("tak", "nie"), itemsFile("tak")), taggedDay("d1", "d1-e1" to listOf("tak")))
        }

        assertTrue("tak" in failure.message.orEmpty(), "Message was: ${failure.message}")
    }

    @Test
    fun `an Item with blank text is rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            loadWithItems(
                listOf("""{ "items": [ { "id": "tak", "text": " ", "meaning": { "en": "yes" } } ] }"""),
                taggedDay("d1", "d1-e1" to listOf("tak")),
            )
        }

        assertTrue("tak" in failure.message.orEmpty(), "Message was: ${failure.message}")
    }

    @Test
    fun `two Exercises with the same id in different Daily Sets are rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            loadWithItems(
                listOf(itemsFile("tak")),
                taggedDay("d1", "e1" to listOf("tak")),
                taggedDay("d2", "e2" to emptyList(), "e1" to emptyList()),
            )
        }

        assertTrue("e1" in failure.message.orEmpty(), "Message was: ${failure.message}")
    }

    @Test
    fun `a listen-and-choose Exercise with blank text, no distractors or a blank distractor is rejected`() {
        listOf("", "   ", "?!").forEach { blank ->
            assertListeningRejected(listenChooseDay(blank, listOf("sześć")), "Text \"$blank\"")
            assertListeningRejected(listenChooseDay("cześć", listOf("sześć", blank)), "Distractor \"$blank\"")
        }
        assertListeningRejected(listenChooseDay("cześć", emptyList()))
    }

    @Test
    fun `a listen-and-choose Exercise with two options the same apart from capitals and punctuation is rejected`() {
        assertListeningRejected(listenChooseDay("cześć", listOf("sześć", "Cześć!")))
        assertListeningRejected(listenChooseDay("cześć", listOf("sześć", "część", " SZEŚĆ ")))
    }

    @Test
    fun `a listen-and-choose Exercise whose distractor differs from the text only by an accent loads`() {
        val course = FixtureCourse.loadWithDayOne(listenChooseDay("proszę", listOf("prosze", "prószę")), clipsFile("proszę"))

        assertEquals(
            ListenChoose(id = "a1-u01-d01-e01", text = "proszę", distractors = listOf("prosze", "prószę")),
            course.path.first().exercises.single(),
        )
    }

    @Test
    fun `a listen-and-type Exercise with no Accepted Answers or a blank one is rejected`() {
        assertListeningRejected(listenTypeDay())
        listOf("", "   ", "?!").forEach { blank ->
            assertListeningRejected(listenTypeDay("dziękuję", blank), "Accepted Answer \"$blank\"")
            assertListeningRejected(listenTypeDay(blank), "Only Accepted Answer \"$blank\"")
        }
    }

    @Test
    fun `a listening Exercise whose spoken text has no Clip is rejected, naming the Daily Set, the Exercise and the text`() {
        listOf(
            // The Clip of a distractor does not count, nor that of a second Accepted Answer.
            listenChooseDay("cześć", listOf("sześć")) to clipsFile("sześć"),
            listenTypeDay("cześć", "czesc") to clipsFile("czesc"),
            // A course that lists no audio has no Clips.
            listenChooseDay("cześć", listOf("sześć")) to null,
            listenTypeDay("cześć") to null,
        ).forEach { (day, clips) ->
            val failure = assertFailsWith<IllegalArgumentException> { FixtureCourse.loadWithDayOne(day, clips) }

            assertEquals(
                "Daily Set a1-u01-d01: Exercise a1-u01-d01-e01 has no Clip for \"cześć\"",
                failure.message,
            )
        }
    }

    @Test
    fun `two Clips with the same id are rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            FixtureCourse.load("""{ "clips": [ { "id": "tak", "text": "tak" }, { "id": "tak", "text": "nie" } ] }""")
        }

        assertTrue("tak" in failure.message.orEmpty(), "Message was: ${failure.message}")
    }

    @Test
    fun `a Clip id that is not lower-case letters, digits and single hyphens is rejected`() {
        listOf("Dzien", "a b", "a/b", "a.ogg", "-a", "a-", "a--b", "", "dzień").forEach { id ->
            val failure = assertFailsWith<IllegalArgumentException>("Clip id \"$id\"") {
                FixtureCourse.load("""{ "clips": [ { "id": "$id", "text": "tak" } ] }""")
            }

            assertTrue("\"$id\"" in failure.message.orEmpty(), "Message was: ${failure.message}")
        }
    }

    @Test
    fun `a Clip with blank text is rejected`() {
        listOf("", "  ").forEach { blank ->
            val failure = assertFailsWith<IllegalArgumentException> {
                FixtureCourse.load("""{ "clips": [ { "id": "tak-2", "text": "$blank" } ] }""")
            }

            assertTrue("tak-2" in failure.message.orEmpty(), "Message was: ${failure.message}")
        }
    }

    @Test
    fun `two Clips for the same text apart from capitals and spacing are rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            FixtureCourse.load(
                """{ "clips": [ { "id": "one", "text": "dzień dobry" }, { "id": "two", "text": " Dzień  Dobry" } ] }""",
            )
        }

        assertTrue("one" in failure.message.orEmpty() && "two" in failure.message.orEmpty(), "Message was: ${failure.message}")
    }

    @Test
    fun `two Clips whose texts differ only by punctuation both load`() {
        val course = FixtureCourse.load(clipsFile("tak.", "tak?"))

        assertEquals(ClipView("audio/c1.ogg"), course.clipFor("tak."))
        assertEquals(ClipView("audio/c2.ogg"), course.clipFor("tak?"))
        assertEquals(null, course.clipFor("tak"))
    }

    @Test
    fun `a course that lists audio loads with its Clips, each one's file beside the manifest`() {
        val course = FixtureCourse.load(clipsFile("cześć", "dzień dobry"))

        assertEquals(listOf(Clip("c1", "cześć"), Clip("c2", "dzień dobry")), course.clips)
        assertEquals(ClipView("audio/c1.ogg"), course.clipFor("cześć"))
        assertEquals(ClipView("audio/c2.ogg"), course.clipFor("dzień dobry"))
    }

    @Test
    fun `a Clip manifest at the content root has its files there too`() {
        val files = mapOf(
            "course.json" to """
                { "audio": "clips.json", "levels": [ { "id": "a1", "title": { "en": "A1" }, "units": [] } ] }
            """,
            "clips.json" to clipsFile("tak"),
        )

        assertEquals(ClipView("c1.ogg"), loadCourse(files::getValue).clipFor("tak"))
    }

    @Test
    fun `a course that lists no audio has no Clips`() {
        val course = FixtureCourse.load()

        assertEquals(emptyList(), course.clips)
        assertEquals(null, course.clipFor("cześć"))
    }

    private fun assertListeningRejected(day: String, case: String? = null) {
        val failure = assertFailsWith<IllegalArgumentException>(case) {
            FixtureCourse.loadWithDayOne(day, clipsFile("cześć", "dziękuję"))
        }

        assertNamesTheDailySet(failure)
        assertTrue("a1-u01-d01-e01" in failure.message.orEmpty(), "Message was: ${failure.message}")
    }

    @Test
    fun `a course with problems in two Daily Sets is rejected once, naming both`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            loadWithItems(
                listOf(itemsFile("tak")),
                taggedDay("d1", "d1-e1" to listOf("nope")),
                taggedDay("d2", "d2-e1" to listOf("gone")),
            )
        }

        listOf("d1-e1", "nope", "d2-e1", "gone").forEach { name ->
            assertTrue(name in failure.message.orEmpty(), "Message was: ${failure.message}")
        }
        assertEquals(2, failure.message.orEmpty().lines().size, "Message was: ${failure.message}")
    }

    @Test
    fun `a choice Exercise whose correct option is not one of its options is rejected, naming the Daily Set and the Exercise`() {
        listOf(-1, 2).forEach { option ->
            val failure = assertFailsWith<IllegalArgumentException>("Option $option") {
                FixtureCourse.loadWithDayOne(
                    """{ "id": "a1-u01-d01", "exercises": [ { "type": "choose_translation", "id": "a1-u01-d01-e01",
                         "prompt": "tak", "options": [{ "en": "yes" }, { "en": "no" }], "correctOption": $option } ] }""",
                )
            }

            assertNamesTheDailySet(failure)
            assertTrue("a1-u01-d01-e01" in failure.message.orEmpty(), "Message was: ${failure.message}")
        }
    }

    @Test
    fun `a file that is missing is rejected, naming the file`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            loadCourse { path -> if (path == "course.json") "{ \"items\": [\"lost.json\"], \"levels\": [] }" else error("no file") }
        }

        assertTrue("lost.json" in failure.message.orEmpty(), "Message was: ${failure.message}")
    }

    @Test
    fun `a course that only breaks what the content checker asks of shipped content still loads`() {
        // Three Exercises and two, none listing an Item, Tips whose ids are their own: fine to run, not to ship.
        val course = FixtureCourse.loadWithDayOne(dayOne(tips = listOf("e01")))

        assertEquals(listOf(3, 2), course.path.map { it.exercises.size })
        assertEquals(emptyList(), course.path.flatMap { it.exercises }.flatMap { it.items })
        assertEquals(listOf("t0"), course.path.first().tips.map { it.id })
    }

    /** A course whose `course.json` lists these Item files and Daily Sets. */
    private fun loadWithItems(itemFiles: List<String>, vararg days: String): Course {
        val itemPaths = itemFiles.indices.map { "items-$it.json" }
        val dayPaths = days.indices.map { "day-$it.json" }
        fun quoted(paths: List<String>) = paths.joinToString { "\"$it\"" }
        val files = mapOf(
            "course.json" to """
                {
                  "items": [${quoted(itemPaths)}],
                  "levels": [
                    { "id": "a1", "title": { "en": "A1" }, "units": [
                      { "id": "a1-u01", "title": { "en": "Unit" }, "dailySets": [${quoted(dayPaths)}] }
                    ] }
                  ]
                }
            """,
        ) + itemPaths.zip(itemFiles) + dayPaths.zip(days)
        return loadCourse(files::getValue)
    }

    private fun itemsFile(vararg ids: String): String {
        val items = ids.joinToString { """{ "id": "$it", "text": "$it", "meaning": { "en": "$it in English" } }""" }
        return """{ "items": [$items] }"""
    }

    /** A Daily Set of choice Exercises, each given as its id and the Items it lists. */
    private fun taggedDay(id: String, vararg exercises: Pair<String, List<String>>): String {
        val exercisesJson = exercises.joinToString { (exerciseId, items) ->
            """{ "type": "choose_translation", "id": "$exerciseId", "prompt": "tak",
                 "options": [{ "en": "yes" }, { "en": "no" }], "correctOption": 0,
                 "items": [${items.joinToString { "\"$it\"" }}] }"""
        }
        return """{ "id": "$id", "exercises": [$exercisesJson] }"""
    }

    private fun assertNamesTheDailySet(failure: IllegalArgumentException) =
        assertTrue("a1-u01-d01" in failure.message.orEmpty(), "Message was: ${failure.message}")

    /** Day 1 with three Exercises and one Tip before each Exercise named in [tips]. */
    private fun dayOne(tips: List<String>?): String {
        val tipsJson = tips?.mapIndexed { index, before ->
            """{ "id": "t$index", "before": "$before", "title": { "en": "Title" }, "body": { "en": "Body" } }"""
        }?.joinToString(prefix = "\"tips\": [", postfix = "],")
        val exercises = listOf("e01", "e02", "e03").joinToString { id ->
            """{ "type": "choose_translation", "id": "$id", "prompt": "tak",
                 "options": [{ "en": "yes" }, { "en": "no" }], "correctOption": 0 }"""
        }
        return """{ "id": "a1-u01-d01", ${tipsJson.orEmpty()} "exercises": [$exercises] }"""
    }
}
