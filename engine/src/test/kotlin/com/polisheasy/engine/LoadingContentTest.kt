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
