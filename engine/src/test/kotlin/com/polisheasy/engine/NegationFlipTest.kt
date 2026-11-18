package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NegationFlipTest {
    private fun negation(yes: String, no: String, vararg items: String) =
        Negation(yes, mapOf("en" to "yes: $yes"), no, mapOf("en" to "no: $no"), items.toList())

    private val negations = listOf(
        negation("Lubię kawę.", "Nie lubię kawy.", "lubie", "kawe", "nie", "kawy"),
        negation("Piję herbatę.", "Nie piję herbaty.", "pije", "herbate", "nie", "herbaty"),
        negation("Mam wodę.", "Nie mam wody.", "mam", "wode", "nie", "wody"),
        negation("Jem ser.", "Nie jem sera.", "jem", "ser", "nie", "sera"),
        negation("Jem chleb.", "Nie jem chleba.", "jem", "chleb", "nie", "chleba"),
        negation("Mam sok.", "Nie mam soku.", "mam", "sok", "nie", "soku"),
        negation("Ewa pije mleko.", "Ewa nie pije mleka.", "ewa", "pije", "mleko", "nie", "mleka"),
    )
    private val ids = negations.flatMap { it.items }.distinct()

    /** l1 teaches what the first six need, l2 what Ewa's needs. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *ids.filter { it != "ewa" && it != "mleko" && it != "mleka" }.map { choose("e-$it", it, it) }.toTypedArray()),
                day("l2", *listOf("ewa", "mleko", "mleka").map { choose("e-$it", it, it) }.toTypedArray()),
            ),
        ),
        ids.map { item(it) },
        negations = negations,
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun Practice.flip(seconds: Int = 60): FlipRound =
        start(setup(Game.NEGATION_FLIP, listOf("${NegationFlip.SECONDS_PREFIX}$seconds"))) as FlipRound

    @Test
    fun `the wrong option keeps the object, nie put where the negation has it`() {
        assertEquals("Nie lubię kawę.", negations[0].kept)
        assertEquals(0, negations[0].negatedAt)
        assertEquals("Ewa nie pije mleko.", negations[6].kept)
        assertEquals(4, negations[6].negatedAt)
        assertEquals(-1, negation("Ewa pije mleko.", "Adam nie pije mleka.").negatedAt)
    }

    @Test
    fun `Negation flip opens with six sentences learned, for 60 or 120 seconds`() {
        val state = { p: Player -> p.practice().view().games.single { it.game == Game.NEGATION_FLIP }.state }
        assertEquals(GameState.NEEDS_MORE, state(player()))
        assertEquals(GameState.READY, state(player().completed("l1")))
        assertEquals(listOf("seconds-60", "seconds-120"), player().completed("l1").practice().setup(Game.NEGATION_FLIP).choices[0].options.map { it.id })
    }

    @Test
    fun `cards deal the learned sentences, each before any comes again and never one twice running, the negation one of two`() {
        repeat(10) { seed ->
            val round = player(seed).completed("l1").practice().flip()
            assertTrue(round.cards.size >= 60)
            assertTrue(round.cards.none { it.yes == "Ewa pije mleko." })
            assertEquals(6, round.cards.take(6).map { it.yes }.toSet().size)
            round.cards.zipWithNext().forEach { (one, next) -> assertTrue(one.yes != next.yes) }
            round.cards.forEach { card ->
                val negation = negations.single { it.yes == card.yes }
                assertEquals(setOf(negation.no, negation.kept), card.options.toSet())
                assertEquals(negation.no, card.no)
                assertEquals("no: ${negation.no}", card.meaning)
            }
            assertTrue(round.cards.map { it.right }.toSet() == setOf(0, 1))
        }
        assertTrue(player().completed("l1", "l2").practice().flip().cards.any { it.yes == "Ewa pije mleko." })
    }

    @Test
    fun `a right answer brings the next card, a wrong one waits, and the clock ends the Round`() {
        var round = player().completed("l1").practice().flip()
        val first = checkNotNull(round.card)
        round = round.answer(first.right)
        assertEquals(1, round.right)
        val second = checkNotNull(round.card)
        assertTrue(first !== second)
        round = round.answer(1 - second.right)
        assertEquals(1 - second.right, round.missed)
        assertEquals(round, round.answer(second.right))
        assertEquals(second, round.card)
        round = round.tick().next()
        assertNull(round.missed)
        assertEquals(1, round.right)
        assertEquals(59, round.left)
        repeat(59) { round = round.tick() }
        assertTrue(round.finished)
        assertNull(round.card)
        assertEquals(Score(1), round.score)
    }

    @Test
    fun `the loader reads the negations and rejects an undefined Item and a negation without nie`() {
        fun read(practice: String): Pair<List<String>, Course> {
            val files = mapOf(
                "course.json" to """{"levels": [], "items": ["items.json"], "practice": "practice.json"}""",
                "items.json" to """{"items": [
                    {"id": "nie", "text": "nie", "meaning": {"en": "not"}},
                    {"id": "kawy", "text": "kawy", "meaning": {"en": "coffee (genitive)"}}
                ]}""",
                "practice.json" to practice,
            )
            val content = readContent { files.getValue(it) }
            return loadingProblems(content).map { it.message } to content.course()
        }
        val entry = """{"yes": "Lubię kawę.", "yesMeaning": {"en": "I like coffee."}, "no": "Nie lubię kawy.", "noMeaning": {"en": "I don't like coffee."}, "items": ["nie", "kawy"]}"""
        val (problems, course) = read("""{"negations": [$entry]}""")
        assertEquals(emptyList(), problems.filter { "negation" in it })
        assertEquals(listOf("Nie lubię kawy."), course.negations.map { it.no })
        val wrong = """{"yes": "Mam kawę.", "yesMeaning": {"en": "I have coffee."}, "no": "Brak kawy.", "noMeaning": {"en": "No coffee."}, "items": ["brak"]}"""
        assertEquals(
            listOf(
                "The negation of \"Mam kawę.\" names Item brak, which is not defined",
                "The negation of \"Mam kawę.\" must say nie where the sentence goes on as it was",
                "The sentence \"Lubię kawę.\" is listed more than once under negations",
            ),
            read("""{"negations": [$entry, $wrong, $entry]}""").first.filter { "negation" in it },
        )
    }
}
