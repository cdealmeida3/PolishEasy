package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PronounGridTest {
    private val persons = listOf(item("ja", "ja", "I"), item("ty", "ty", "you"), item("on", "on", "he"), item("ona", "ona", "she"), item("my", "my", "we"))
    private val withWhom = listOf(item("ze-mna", "ze mną", "with me"), item("z-toba", "z tobą", "with you"), item("z-nim", "z nim", "with him"), item("z-nia", "z nią", "with her"), item("z-nami", "z nami", "with us"))
    private val toWhom = listOf(item("mi", "mi", "to me"), item("ci", "ci", "to you"), item("mu", "mu", "to him"), item("jej-dat", "jej", "to her"))

    /** l1 teaches four squares of z kim?, l2 the fifth and three of komu?, l3 the fourth of komu?. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *withWhom.take(4).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l2", *(withWhom.drop(4) + toWhom.take(3)).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l3", choose("e-jej", "jej", "jej-dat")),
            ),
        ),
        persons + withWhom + toWhom,
        pronounGrid = PronounTable(
            persons.map { it.id },
            listOf(
                PronounColumn("komu", "komu?", mapOf("en" to "to whom?"), toWhom.map { it.id } + null),
                PronounColumn("z-kim", "z kim?", mapOf("en" to "with whom?"), withWhom.map { it.id }),
            ),
        ),
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun Practice.round(way: String): PronounGridRound = start(setup(Game.PRONOUN_GRID, listOf(way))) as PronounGridRound

    private fun state(player: Player) = player.practice().view().games.single { it.game == Game.PRONOUN_GRID }.state

    @Test
    fun `Pronoun grid opens once four squares of a column are learned, and offers each ready column, then mixed`() {
        assertEquals(GameState.NEEDS_MORE, state(player()))
        assertEquals(GameState.READY, state(player().completed("l1")))
        val practice = player().completed("l1", "l2").practice()
        assertEquals(listOf("z-kim"), practice.setup(Game.PRONOUN_GRID, emptyList()).choices.single().options.map { it.id })
        val all = player().completed("l1", "l2", "l3").practice().setup(Game.PRONOUN_GRID, emptyList()).choices.single().options
        assertEquals(listOf("komu", "z-kim", PronounGrid.MIXED), all.map { it.id })
        assertEquals(listOf("komu?", "z kim?", null), all.map { it.label })
    }

    @Test
    fun `a column's Round asks every learned square once, offering its form among others of the column`() {
        repeat(10) { seed ->
            val round = player(seed).completed("l1", "l2").practice().round("z-kim")
            assertEquals(listOf("z kim?"), round.columns.map { it.question })
            assertEquals(listOf("ja", "ty", "on", "ona", "my"), round.persons)
            assertEquals(5, round.total)
            assertEquals((0..4).toSet(), round.run.questions.map { it.person }.toSet())
            round.run.questions.forEach { question ->
                assertEquals(withWhom[question.person].text, question.form)
                assertEquals(PronounGrid.OFFERED, question.offered.size)
                assertTrue(question.form in question.offered)
                assertEquals(question.offered.size, question.offered.distinct().size)
                assertTrue(question.offered.all { form -> withWhom.any { it.text == form } })
            }
        }
    }

    @Test
    fun `a square is asked, then filled right or missed, and the score counts the squares right first try`() {
        var round = player().completed("l1", "l2").practice().round("z-kim")
        val first = round.question!!
        assertEquals(GridSquare.ASKED, round.square(first.person, 0))
        round = round.choose(first.offered.indexOf(first.form))
        assertTrue(round.given!!.correct)
        // Answered once: a second tap changes nothing.
        assertEquals(round, round.choose(0))
        round = round.next()
        assertEquals(GridSquare.RIGHT, round.square(first.person, 0))
        val second = round.question!!
        round = round.choose(second.offered.indexOfFirst { it != second.form }).next()
        assertEquals(GridSquare.MISSED, round.square(second.person, 0))
        assertEquals(GridSquare.ASKED, round.square(round.question!!.person, 0))
        while (!round.finished) round = round.choose(round.question!!.offered.indexOf(round.question!!.form)).next()
        assertEquals(Score(4, of = 5), round.score)
    }

    @Test
    fun `mixed shows two columns side by side, and a square no form fills is never asked`() {
        val round = player().completed("l1", "l2", "l3").practice().round(PronounGrid.MIXED)
        assertEquals(listOf("komu?", "z kim?"), round.columns.map { it.question })
        assertEquals(9, round.total)
        assertEquals(GridSquare.NONE, round.square(4, 0))
        assertTrue(round.run.questions.none { it.column == 0 && it.person == 4 })
    }
}
