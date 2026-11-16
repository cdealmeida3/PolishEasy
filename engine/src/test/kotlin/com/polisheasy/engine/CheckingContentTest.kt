package com.polisheasy.engine

import com.polisheasy.engine.CheckerFixture.Companion.CLIPS
import com.polisheasy.engine.CheckerFixture.Companion.DAY_1
import com.polisheasy.engine.CheckerFixture.Companion.DAY_2
import com.polisheasy.engine.CheckerFixture.Companion.DAY_3
import com.polisheasy.engine.CheckerFixture.Companion.ITEMS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Each broken course changes one thing in [CheckerFixture.valid], and the test names every problem that follows. */
internal class CheckingContentTest {
    private val valid = CheckerFixture.valid
    private val shipped = CheckerFixture.shipped

    private fun problems(fixture: CheckerFixture, released: ReleasedIds? = null): List<ContentProblem> =
        checkContent(fixture.files(), released)

    private fun where(problems: List<ContentProblem>): List<Pair<String, String?>> = problems.map { it.file to it.id }

    /** Asserts that [fixture] has exactly one problem, at [file] and [id], whose message has [word] in it. */
    private fun assertOnlyProblem(
        fixture: CheckerFixture,
        file: String,
        id: String?,
        word: String,
        released: ReleasedIds? = null,
        case: String = "",
    ) {
        val found = problems(fixture, released)
        assertEquals(listOf(file to id), where(found), "$case\n${found.joinToString("\n")}")
        assertTrue(word in found.single().message, "$case: expected \"$word\" in: ${found.single().message}")
    }

    private fun assertProblemsAt(fixture: CheckerFixture, vararg expected: Pair<String, String?>, released: ReleasedIds? = null) {
        val found = problems(fixture, released)
        assertEquals(expected.toList().sortedBy { it.toString() }, where(found).sortedBy { it.toString() }, found.joinToString("\n"))
    }

    private fun en(text: String): Localized = mapOf("en" to text)

    private val extraItem = Item("extra", "extra", en("extra"))

    // 1 and 2

    @Test
    fun `a valid course has no problems`() {
        assertEquals(emptyList(), problems(valid, shipped))
    }

    @Test
    fun `a course broken in three files reports all three problems, each with its file and identifier`() {
        val broken = valid
            .withExercise("a1-u01-d01-e01") { (it as ChooseTranslation).copy(correctOption = 9) }
            .withItems { it + extraItem }
            .without("audio/c1.ogg")

        assertProblemsAt(broken, DAY_1 to "a1-u01-d01-e01", ITEMS to "extra", CLIPS to "c1")
    }

    @Test
    fun `a problem reads as its file, its identifier and what is wrong`() {
        assertEquals("day.json: e1: is broken", ContentProblem("day.json", "e1", "is broken").toString())
        assertEquals("day.json: is broken", ContentProblem("day.json", null, "is broken").toString())
    }

    // 3: identifiers used twice

    @Test
    fun `an Exercise id used in two Daily Sets is reported at the second file, naming the first`() {
        val found = problems(valid.withExercise("a1-u01-d02-e01") { (it as ChooseTranslation).copy(id = "a1-u01-d01-e01") })

        // The same Exercise also breaks the rule that its id starts with its Daily Set's.
        assertEquals(listOf(DAY_2 to "a1-u01-d01-e01", DAY_2 to "a1-u01-d01-e01"), where(found), found.joinToString("\n"))
        assertTrue(found.any { "More than one Exercise" in it.message && DAY_1 in it.message }, found.joinToString("\n"))
    }

    @Test
    fun `an Item id used in two Item files is reported at the second file, naming the first`() {
        val more = "a1/unit-01/more-items.json"
        val broken = valid
            .withFile(more, """{ "items": [ { "id": "tak", "text": "tak", "meaning": { "en": "yes" } } ] }""")
            .withIndex { it.copy(items = it.items + more) }

        assertOnlyProblem(broken, more, "tak", ITEMS)
    }

    @Test
    fun `a Clip id used twice is reported once`() {
        val broken = valid
            .withClips { clips -> clips.map { if (it.id == "c2") it.copy(id = "c1") else it } }
            .without("audio/c2.ogg")

        assertOnlyProblem(broken, CLIPS, "c1", "More than one Clip")
    }

    @Test
    fun `a Tip id used in two Daily Sets is reported at the second file, naming the first`() {
        val copy = Tip("a1-u01-d01-t01", before = "a1-u01-d02-e01", title = en("Title"), body = en("Body"))
        val found = problems(valid.withDailySet(DAY_2) { it.copy(tips = listOf(copy)) })

        assertEquals(listOf(DAY_2 to "a1-u01-d01-t01", DAY_2 to "a1-u01-d01-t01"), where(found), found.joinToString("\n"))
        assertTrue(found.any { "More than one Tip" in it.message && DAY_1 in it.message }, found.joinToString("\n"))
    }

    @Test
    fun `a Daily Set id, a Unit id and a Level id used twice are each reported`() {
        val twoDays = problems(valid.withDailySet(DAY_2) { it.copy(id = "a1-u01-d01") })
        assertEquals(
            listOf(DAY_2 to "a1-u01-d01"),
            where(twoDays.filter { "More than one Daily Set" in it.message && DAY_1 in it.message }),
            twoDays.joinToString("\n"),
        )

        val twoUnits = problems(valid.withLevel { it.copy(units = it.units + UnitEntry("a1-u01", en("Again"), emptyList())) })
        assertEquals(
            listOf(COURSE_FILE to "a1-u01"),
            where(twoUnits.filter { "More than one Unit" in it.message }),
            twoUnits.joinToString("\n"),
        )

        val twoLevels = problems(valid.withIndex { it.copy(levels = it.levels + LevelEntry("a1", en("Again"), emptyList())) })
        assertEquals(
            listOf(COURSE_FILE to "a1"),
            where(twoLevels.filter { "More than one Level" in it.message }),
            twoLevels.joinToString("\n"),
        )
    }

    @Test
    fun `a Daily Set file listed twice is reported once`() {
        assertOnlyProblem(valid.withUnit { it.copy(dailySets = it.dailySets + DAY_1) }, DAY_1, "a1-u01-d01", "more than once")
    }

    // 4: answers

    @Test
    fun `an Exercise of any answered type with no Accepted Answer is reported`() {
        val none = emptyList<String>()
        listOf<Triple<String, String, (Exercise) -> Exercise>>(
            Triple(DAY_1, "a1-u01-d01-e02") { (it as TypeAnswer).copy(acceptedAnswers = none) },
            Triple(DAY_1, "a1-u01-d01-e03") { (it as WordTiles).copy(acceptedAnswers = none) },
            Triple(DAY_2, "a1-u01-d02-e06") { (it as Conjugation).copy(acceptedAnswers = none) },
            Triple(DAY_1, "a1-u01-d01-e06") { (it as ListenType).copy(acceptedAnswers = none) },
        ).forEach { (file, id, edit) ->
            assertOnlyProblem(valid.withExercise(id, edit), file, id, "no Accepted Answers", case = id)
        }
    }

    @Test
    fun `a blank Accepted Answer is reported`() {
        assertOnlyProblem(
            valid.withExercise("a1-u01-d01-e02") { (it as TypeAnswer).copy(acceptedAnswers = listOf("dziękuję", " ")) },
            DAY_1,
            "a1-u01-d01-e02",
            "blank Accepted Answer",
        )
    }

    @Test
    fun `a choice Exercise whose correct option is not one of its options is reported`() {
        listOf(-1, 2).forEach { option ->
            assertOnlyProblem(
                valid.withExercise("a1-u01-d01-e01") { (it as ChooseTranslation).copy(correctOption = option) },
                DAY_1,
                "a1-u01-d01-e01",
                "correctOption $option",
            )
        }
    }

    @Test
    fun `a choice Exercise with one option, or with two options that read the same, is reported`() {
        assertOnlyProblem(
            valid.withExercise("a1-u01-d02-e01") { (it as ChooseTranslation).copy(options = it.options.take(1)) },
            DAY_2,
            "a1-u01-d02-e01",
            "fewer than 2 options",
        )
        assertOnlyProblem(
            valid.withExercise("a1-u01-d02-e01") { (it as ChooseTranslation).copy(options = listOf(en("yes"), en(" Yes"))) },
            DAY_2,
            "a1-u01-d02-e01",
            "read the same",
        )
    }

    // 5 and 6: Items

    @Test
    fun `an Exercise that lists an unknown Item is reported`() {
        assertOnlyProblem(
            valid.withExercise("a1-u01-d01-e01") { (it as ChooseTranslation).copy(items = listOf("czesc", "nope")) },
            DAY_1,
            "a1-u01-d01-e01",
            "nope",
        )
    }

    @Test
    fun `an Exercise that lists no Item is reported`() {
        assertOnlyProblem(
            valid.withExercise("a1-u01-d02-e07") { (it as ChooseTranslation).copy(items = emptyList()) },
            DAY_2,
            "a1-u01-d02-e07",
            "lists no Item",
        )
    }

    @Test
    fun `an Item practised by one Exercise, or by none, is reported at its Items file`() {
        val one = valid.withItems { it + extraItem }
            .withExercise("a1-u01-d01-e01") { (it as ChooseTranslation).copy(items = it.items + "extra") }
        assertOnlyProblem(one, ITEMS, "extra", "1 Exercise")

        assertOnlyProblem(valid.withItems { it + extraItem }, ITEMS, "extra", "no Exercise")
    }

    // 7: audio

    @Test
    fun `an Accepted Answer with no Clip is reported, quoting the text`() {
        assertOnlyProblem(
            valid.withExercise("a1-u01-d02-e03") { (it as TypeAnswer).copy(acceptedAnswers = listOf("cześć", "czesc")) },
            DAY_2,
            "a1-u01-d02-e03",
            "\"czesc\"",
        )
    }

    @Test
    fun `a listening Exercise whose text has no Clip is reported once`() {
        assertOnlyProblem(
            valid.withExercise("a1-u01-d01-e06") { (it as ListenType).copy(acceptedAnswers = listOf("no to nie")) },
            DAY_1,
            "a1-u01-d01-e06",
            "\"no to nie\"",
        )
    }

    @Test
    fun `a Clip with no audio file is reported`() {
        assertOnlyProblem(valid.without("audio/c1.ogg"), CLIPS, "c1", "c1.ogg")
    }

    @Test
    fun `a Clip that nothing speaks is reported`() {
        val broken = valid.withClips { it + Clip("c9", "dobranoc") }.withFile("audio/c9.ogg", "OggS")

        assertOnlyProblem(broken, CLIPS, "c9", "dobranoc")
    }

    @Test
    fun `a stray file beside the Clip manifest is reported`() {
        assertOnlyProblem(valid.withFile("audio/notes.txt", "to do"), CLIPS, null, "audio/notes.txt")
    }

    // 8: Base Language text

    @Test
    fun `text that is missing or blank in the Base Language is reported wherever the Learner would read it`() {
        fun cases(text: Localized): List<Triple<CheckerFixture, String, String>> = listOf(
            Triple(valid.withLevel { it.copy(title = text) }, COURSE_FILE, "a1"),
            Triple(valid.withUnit { it.copy(title = text) }, COURSE_FILE, "a1-u01"),
            Triple(valid.withDailySet(DAY_1) { day -> day.copy(tips = day.tips.map { it.copy(title = text) }) }, DAY_1, "a1-u01-d01-t01"),
            Triple(valid.withDailySet(DAY_1) { day -> day.copy(tips = day.tips.map { it.copy(body = text) }) }, DAY_1, "a1-u01-d01-t01"),
            Triple(
                valid.withExercise("a1-u01-d01-e01") { (it as ChooseTranslation).copy(options = listOf(it.options[0], text)) },
                DAY_1,
                "a1-u01-d01-e01",
            ),
            Triple(valid.withExercise("a1-u01-d01-e02") { (it as TypeAnswer).copy(prompt = text) }, DAY_1, "a1-u01-d01-e02"),
            Triple(valid.withExercise("a1-u01-d01-e03") { (it as WordTiles).copy(prompt = text) }, DAY_1, "a1-u01-d01-e03"),
            Triple(valid.withExercise("a1-u01-d01-e04") { (it as Conjugation).copy(meaning = text) }, DAY_1, "a1-u01-d01-e04"),
            Triple(valid.withExercise("a1-u01-d01-e04") { (it as Conjugation).copy(sentenceMeaning = text) }, DAY_1, "a1-u01-d01-e04"),
            Triple(valid.withItems { items -> items.map { if (it.id == "tak") it.copy(meaning = text) else it } }, ITEMS, "tak"),
        )

        listOf("absent" to mapOf("pt" to "olá"), "blank" to en("  ")).forEach { (how, text) ->
            cases(text).forEachIndexed { index, (broken, file, id) ->
                assertOnlyProblem(broken, file, id, "\"en\"", case = "Case $index, $how")
            }
        }
    }

    @Test
    fun `a Tip's conjugation table with no meaning in the Base Language is reported`() {
        val table = ConjugationTable("być", emptyMap(), Person.entries.associateWith { "jestem" })

        assertOnlyProblem(
            valid.withDailySet(DAY_1) { day -> day.copy(tips = day.tips.map { it.copy(conjugation = table) }) },
            DAY_1,
            "a1-u01-d01-t01",
            "\"en\"",
        )
    }

    // 9: sizes

    @Test
    fun `a Daily Set with one Exercise too few or one too many is reported`() {
        listOf(DAILY_SET_EXERCISES.first - 1, DAILY_SET_EXERCISES.last + 1).forEach { size ->
            assertOnlyProblem(
                valid.withNewDailySet(DAY_3, CheckerFixture.fillerDay(size)),
                DAY_3,
                "a1-u01-d03",
                "$size Exercises",
            )
        }
        listOf(DAILY_SET_EXERCISES.first, DAILY_SET_EXERCISES.last).forEach { size ->
            assertEquals(emptyList(), problems(valid.withNewDailySet(DAY_3, CheckerFixture.fillerDay(size))), "$size Exercises")
        }
    }

    @Test
    fun `a Daily Set that introduces one Item too many is reported`() {
        fun withNewItems(count: Int): CheckerFixture {
            val extra = (1..count).map { Item("new-$it", "new $it", en("new $it")) }
            return valid.withItems { it + extra }
                .withExercise("a1-u01-d01-e01") { (it as ChooseTranslation).copy(items = it.items + extra.map(Item::id)) }
                .withExercise("a1-u01-d01-e02") { (it as TypeAnswer).copy(items = it.items + extra.map(Item::id)) }
        }
        val room = MAX_NEW_ITEMS - CheckerFixture.NEW_ITEMS_ON_DAY_1

        assertEquals(emptyList(), problems(withNewItems(room)))
        assertOnlyProblem(withNewItems(room + 1), DAY_1, "a1-u01-d01", "${MAX_NEW_ITEMS + 1} new Items")
    }

    @Test
    fun `a Daily Set with three Tips is reported`() {
        val more = listOf("e02", "e03").mapIndexed { index, exercise ->
            Tip("a1-u01-d01-t0${index + 2}", before = "a1-u01-d01-$exercise", title = en("Title"), body = en("Body"))
        }

        assertOnlyProblem(valid.withDailySet(DAY_1) { it.copy(tips = it.tips + more) }, DAY_1, "a1-u01-d01", "3 Tips")
    }

    // 10: belonging

    @Test
    fun `a Daily Set file that no Unit lists, and an Item file that the course does not list, are reported`() {
        val day = "a1/unit-01/day-09.json"
        assertOnlyProblem(valid.withFile(day, valid.texts.getValue(DAY_1)), day, null, COURSE_FILE)

        val items = "a1/unit-02/items.json"
        assertOnlyProblem(valid.withFile(items, """{ "items": [] }"""), items, null, COURSE_FILE)
    }

    @Test
    fun `a Unit with no Daily Sets and a Level with no Units are reported`() {
        assertOnlyProblem(
            valid.withLevel { it.copy(units = it.units + UnitEntry("a1-u02", en("Empty"), emptyList())) },
            COURSE_FILE,
            "a1-u02",
            "no Daily Sets",
        )
        assertOnlyProblem(
            valid.withIndex { it.copy(levels = it.levels + LevelEntry("a2", en("A2"), emptyList())) },
            COURSE_FILE,
            "a2",
            "no Units",
        )
    }

    @Test
    fun `an identifier that does not start with its parent's is reported`() {
        // Renaming a parent leaves its children claiming the old one.
        assertProblemsAt(
            valid.withUnit { it.copy(id = "b1-u01") },
            COURSE_FILE to "b1-u01",
            DAY_1 to "a1-u01-d01",
            DAY_2 to "a1-u01-d02",
        )
        val dayTwo = contentJson.decodeFromString<DailySet>(valid.texts.getValue(DAY_2))
        assertProblemsAt(
            valid.withDailySet(DAY_2) { it.copy(id = "a1-u02-d02") },
            DAY_2 to "a1-u02-d02",
            *dayTwo.exercises.map { DAY_2 to it.id }.toTypedArray(),
        )
        assertOnlyProblem(
            valid.withExercise("a1-u01-d01-e07") { (it as ChooseTranslation).copy(id = "x-e07") },
            DAY_1,
            "x-e07",
            "does not start with a1-u01-d01-",
        )
        assertOnlyProblem(
            valid.withDailySet(DAY_1) { day -> day.copy(tips = day.tips.map { it.copy(id = "t01") }) },
            DAY_1,
            "t01",
            "does not start with a1-u01-d01-",
        )
    }

    // 11 and 12: files that cannot be read

    @Test
    fun `a listed file that cannot be read is one problem with no identifier, and nothing else is blamed for it`() {
        val text = valid.texts.getValue(DAY_2)
        listOf(
            "missing" to valid.without(DAY_2),
            "not JSON" to valid.withFile(DAY_2, "{ not json"),
            "unknown Exercise type" to valid.withFile(DAY_2, text.replace("listen_type", "listen_tipe")),
            "misspelt field" to valid.withFile(DAY_2, text.replaceFirst("\"exercises\"", "\"exercizes\"")),
        ).forEach { (case, broken) ->
            assertOnlyProblem(broken, DAY_2, null, "Cannot be read", case = case)
        }

        assertOnlyProblem(valid.without(ITEMS), ITEMS, null, "Cannot be read")
        assertOnlyProblem(valid.withFile(CLIPS, "[]"), CLIPS, null, "Cannot be read")
    }

    @Test
    fun `the other files are still checked when one cannot be read`() {
        val broken = valid
            .withExercise("a1-u01-d01-e01") { (it as ChooseTranslation).copy(correctOption = 5) }
            .without(DAY_2)

        assertProblemsAt(broken, DAY_2 to null, DAY_1 to "a1-u01-d01-e01")
    }

    @Test
    fun `a course file that cannot be read is the only problem`() {
        assertOnlyProblem(valid.withFile(COURSE_FILE, "{ not json"), COURSE_FILE, null, "Cannot be read", released = shipped)
        assertOnlyProblem(valid.without(COURSE_FILE), COURSE_FILE, null, "Cannot be read", released = shipped)
    }

    // 13: identifiers that have shipped

    private fun withoutProsze(fixture: CheckerFixture, renamedTo: String? = null): CheckerFixture {
        fun swap(items: List<String>) = items.mapNotNull { if (it == "prosze") renamedTo else it }
        return listOf("a1-u01-d01-e07", "a1-u01-d02-e07", "a1-u01-d02-e08")
            .fold(fixture) { changed, id -> changed.withExercise(id) { (it as ChooseTranslation).copy(items = swap(it.items)) } }
            .withItems { items -> items.mapNotNull { if (it.id == "prosze") renamedTo?.let { id -> it.copy(id = id) } else it } }
    }

    private val withoutLastExercise = valid.withDailySet(DAY_2) { it.copy(exercises = it.exercises.dropLast(1)) }

    @Test
    fun `an Exercise or an Item that shipped and is gone is reported at the record of shipped identifiers`() {
        assertOnlyProblem(withoutLastExercise, RELEASED_IDS_FILE, "a1-u01-d02-e08", "Exercise shipped before", released = shipped)
        assertOnlyProblem(withoutProsze(valid), RELEASED_IDS_FILE, "prosze", "Item shipped before", released = shipped)
    }

    @Test
    fun `a Daily Set, a Unit and a Level that shipped and are gone are reported`() {
        fun gone(fixture: CheckerFixture) =
            problems(fixture, shipped).filter { it.file == RELEASED_IDS_FILE }.map { it.id }

        val noDayTwo = valid.without(DAY_2).withUnit { it.copy(dailySets = listOf(DAY_1)) }
        assertEquals(listOf("a1-u01-d02") + (1..8).map { "a1-u01-d02-e0$it" }, gone(noDayTwo))

        assertTrue("a1-u01" in gone(valid.withLevel { it.copy(units = emptyList()) }))
        assertTrue("a1" in gone(valid.withIndex { it.copy(levels = emptyList()) }))
    }

    @Test
    fun `a renamed Item is reported as one identifier gone and one not recorded`() {
        val renamed = withoutProsze(valid, renamedTo = "prosze-2")

        assertProblemsAt(renamed, RELEASED_IDS_FILE to "prosze", ITEMS to "prosze-2", released = shipped)
    }

    @Test
    fun `a new Exercise that is not recorded is reported at its Daily Set file, saying how to record it`() {
        val added = valid.withDailySet(DAY_2) {
            it.copy(exercises = it.exercises + (it.exercises.first() as ChooseTranslation).copy(id = "a1-u01-d02-e09"))
        }

        assertOnlyProblem(added, DAY_2, "a1-u01-d02-e09", "recordContentIds", released = shipped)
    }

    @Test
    fun `nothing that shipped is reported as gone while a file cannot be read`() {
        assertOnlyProblem(valid.without(DAY_2), DAY_2, null, "Cannot be read", released = shipped)
    }

    @Test
    fun `without a record of shipped identifiers, neither rule applies`() {
        assertEquals(emptyList(), problems(withoutProsze(valid, renamedTo = "prosze-2")))
        assertEquals(emptyList(), problems(withoutLastExercise))
    }

    // 14: the record itself

    @Test
    fun `the identifiers of a course are its Levels, Units, Daily Sets, Exercises and Items, sorted`() {
        assertEquals(
            ReleasedIds(
                levels = listOf("a1"),
                units = listOf("a1-u01"),
                dailySets = listOf("a1-u01-d01", "a1-u01-d02"),
                exercises = (1..7).map { "a1-u01-d01-e0$it" } + (1..8).map { "a1-u01-d02-e0$it" },
                items = listOf("byc-jestem", "czesc", "dziekuje", "dzien-dobry", "nie", "prosze", "tak"),
            ),
            identifiersOf(valid.files()),
        )
    }

    @Test
    fun `recording adds new identifiers, keeps those the content no longer has, and sorts`() {
        val recorded = ReleasedIds(items = listOf("zebra", "alfa"), exercises = listOf("e1"))
        val current = ReleasedIds(levels = listOf("a1"), items = listOf("mango", "alfa"))

        assertEquals(
            ReleasedIds(levels = listOf("a1"), exercises = listOf("e1"), items = listOf("alfa", "mango", "zebra")),
            recorded.with(current),
        )
    }
}
