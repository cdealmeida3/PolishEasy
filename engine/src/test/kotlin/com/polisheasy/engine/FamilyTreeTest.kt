package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FamilyTreeTest {
    private val words = mapOf(
        Relative.GRANDFATHER to "dziadek", Relative.GRANDMOTHER to "babcia", Relative.UNCLE to "wujek", Relative.AUNT to "ciocia",
        Relative.FATHER to "tata", Relative.MOTHER to "mama", Relative.COUSIN to "kuzyn", Relative.COUSIN_GIRL to "kuzynka",
        Relative.BROTHER to "brat", Relative.SISTER to "siostra",
    )
    private val feminine = setOf("babcia", "ciocia", "mama", "kuzynka", "siostra")
    private val genders = words.values.associateWith { if (it in feminine) Gender.FEMININE else Gender.MASCULINE }
    private val items = (words.values + listOf("ja", "moj", "moja")).map { item(it, if (it == "moj") "mój" else it) }

    /** l1 teaches "me", "my" and every relative but the cousins, l2 the cousins. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *(items.map { it.id } - setOf("kuzyn", "kuzynka")).map { choose("e-$it", it, it) }.toTypedArray()),
                day("l2", choose("e-kuzyn", "kuzyn", "kuzyn"), choose("e-kuzynka", "kuzynka", "kuzynka")),
            ),
        ),
        items,
        genders = genders,
        possessives = mapOf(Gender.MASCULINE to "moj", Gender.FEMININE to "moja"),
        family = Family("ja", words),
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun Practice.tree(way: String): TreeRound = start(setup(Game.FAMILY_TREE, listOf(way))) as TreeRound

    @Test
    fun `Family tree opens once the whole family and my are learned, to name or to find`() {
        val state = { p: Player -> p.practice().view().games.single { it.game == Game.FAMILY_TREE }.state }
        assertEquals(GameState.NEEDS_MORE, state(player().completed("l1")))
        assertEquals(GameState.READY, state(player().completed("l1", "l2")))
        assertEquals(listOf(FamilyTree.NAME, FamilyTree.FIND), player().completed("l1", "l2").practice().setup(Game.FAMILY_TREE).choices[0].options.map { it.id })
        val noGender = course.copy(genders = genders - "kuzynka")
        val practice = Player(course = noGender).completed("l1", "l2").practice()
        assertEquals(GameState.NEEDS_MORE, practice.view().games.single { it.game == Game.FAMILY_TREE }.state)
    }

    @Test
    fun `each relative is asked once, with my and the word, the word with the wrong my, and two others`() {
        repeat(10) { seed ->
            val round = player(seed).completed("l1", "l2").practice().tree(FamilyTree.NAME)
            assertEquals("ja", round.me)
            assertEquals(Relative.entries.toSet(), round.run.questions.map { it.person.relative }.toSet())
            assertEquals(Relative.entries.size, round.total)
            round.run.questions.forEach { question ->
                val person = question.person
                val my = if (person.word in feminine) "moja" else "mój"
                val wrong = if (person.word in feminine) "mój" else "moja"
                assertEquals("$my ${person.word}", person.phrase)
                assertEquals(FamilyTree.OPTIONS, question.options.size)
                assertEquals(question.options.size, question.options.toSet().size)
                assertTrue(person.phrase in question.options)
                assertTrue("$wrong ${person.word}" in question.options)
                val others = question.options - setOf(person.phrase, "$wrong ${person.word}")
                others.forEach { other -> assertTrue(round.people.any { it.phrase == other && it.relative != person.relative }, other) }
            }
        }
    }

    @Test
    fun `named, an option is chosen, and found, the person is tapped, and each comes onto the tree once answered`() {
        var round = player().completed("l1", "l2").practice().tree(FamilyTree.NAME)
        val first = checkNotNull(round.question)
        assertEquals(emptySet(), round.named)
        assertEquals(round, round.tap(first.person.relative))
        val wrong = first.options.indexOfFirst { it != first.person.phrase }
        round = round.choose(wrong)
        assertEquals(false, round.given?.correct)
        assertEquals(setOf(first.person.relative), round.named)
        assertEquals(round, round.choose(first.options.indexOf(first.person.phrase)))
        round = round.next()
        while (!round.finished) {
            val question = checkNotNull(round.question)
            round = round.choose(question.options.indexOf(question.person.phrase)).next()
        }
        assertEquals(Score(Relative.entries.size - 1, of = Relative.entries.size), round.score)
        assertEquals(Relative.entries.toSet(), round.named)

        var find = player().completed("l1", "l2").practice().tree(FamilyTree.FIND)
        assertTrue(find.find)
        val asked = checkNotNull(find.question).person
        assertEquals(find, find.choose(0))
        val other = Relative.entries.first { it != asked.relative }
        find = find.tap(other)
        assertFalse(checkNotNull(find.given).correct)
        assertEquals(find.people.single { it.relative == other }.phrase, find.given?.text)
        find = find.next()
        find = find.tap(checkNotNull(find.question).person.relative)
        assertEquals(true, find.given?.correct)
    }

    @Test
    fun `the phrases are spoken texts, and the loader rejects a relative not named, an unknown one and one without a gender`() {
        assertTrue("mój wujek" in course.familyPhrases() && "moja ciocia" in course.familyPhrases())
        assertTrue(course.familyPhrases().all { it in course.spokenTexts() })
        fun read(family: String): Pair<List<String>, Course> {
            val files = mapOf(
                "course.json" to """{"levels": [], "items": ["items.json"], "practice": "practice.json"}""",
                "items.json" to """{"items": [${(words.values + "ja").joinToString { """{"id": "$it", "text": "$it", "meaning": {"en": "$it"}}""" }}]}""",
                "practice.json" to """{"genders": {${words.values.filter { it != "brat" }.joinToString { "\"$it\": \"${if (it in feminine) "f" else "m"}\"" }}}, "family": $family}""",
            )
            val content = readContent { files.getValue(it) }
            return loadingProblems(content).map { it.message } to content.course()
        }
        val all = """{"me": "ja", ${words.entries.joinToString { "\"${it.key.id}\": \"${it.value}\"" }}}"""
        val (problems, read) = read(all)
        assertEquals(listOf("The relative brat has no gender \"m\" or \"f\" under genders"), problems.filter { "amily" in it || "relative" in it })
        assertEquals(Family("ja", words), read.family)
        assertEquals(
            listOf(
                "Family tree has no relative pet",
                "Family tree names no Item for sister",
                "Family tree names Item kot, which is not defined",
                "The relative brat has no gender \"m\" or \"f\" under genders",
                "The relative kot has no gender \"m\" or \"f\" under genders",
            ),
            read(all.replace(", \"sister\": \"siostra\"", ", \"pet\": \"kot\"")).first.filter { "amily" in it || "relative" in it },
        )
    }
}
