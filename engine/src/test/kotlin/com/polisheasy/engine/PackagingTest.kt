package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackagingTest {
    private val words = listOf(
        item("puszka"), item("cola"), item("sloik", "słoik"), item("dzem-gen", "dżemu"), item("kostka"), item("maslo-gen", "masła"),
        item("butelka"), item("woda-gen", "wody"), item("tabliczka"), item("czekolada-gen", "czekolady"),
    )

    private fun phrase(text: String, container: String, thing: String, vararg also: String) =
        PackagePhrase(text, mapOf("en" to "$text (en)"), container, also.toList(), listOf(container, thing))

    private val phrases = listOf(
        phrase("puszka coli", "puszka", "cola", "butelka"),
        phrase("słoik dżemu", "sloik", "dzem-gen"),
        phrase("kostka masła", "kostka", "maslo-gen"),
        phrase("butelka wody", "butelka", "woda-gen", "puszka"),
        phrase("tabliczka czekolady", "tabliczka", "czekolada-gen", "kostka"),
    )

    /** l1 teaches four phrases' words, l2 the fifth's. */
    private val course = Course(
        listOf(levelOf("a1", day("l1", *words.take(8).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()), day("l2", *words.drop(8).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()))),
        words,
        packaging = phrases,
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun Practice.round(): PackagingRound = start(setup(Game.PACKAGING, emptyList())) as PackagingRound

    @Test
    fun `what is in the container is the phrase after its first word`() {
        assertEquals("coli", phrases[0].thing)
        assertEquals("czekolady", phrases[4].thing)
    }

    @Test
    fun `Packaging match opens once four phrases and four containers are learned`() {
        val state = { p: Player -> p.practice().view().games.single { it.game == Game.PACKAGING }.state }
        assertEquals(GameState.NEEDS_MORE, state(player()))
        assertEquals(GameState.READY, state(player().completed("l1")))
    }

    @Test
    fun `a container that also goes with the thing is never offered as wrong`() {
        repeat(10) { seed ->
            val round = player(seed).completed("l1", "l2").practice().round()
            round.run.questions.forEach { question ->
                val phrase = phrases.single { it.text == question.text }
                val offered = question.offered.map { it.item }
                assertEquals(phrase.container, offered[question.right])
                assertTrue(phrase.also.none { it in offered }, "${question.text}: $offered")
                assertEquals(offered.distinct(), offered)
            }
        }
    }

    @Test
    fun `the right container is right first try, and the score counts first tries`() {
        var round = player().completed("l1", "l2").practice().round()
        assertEquals(phrases.size, round.total)
        val first = checkNotNull(round.question)
        round = round.choose(first.right)
        assertEquals(true, round.given?.correct)
        assertEquals(round, round.choose(0))
        round = round.next()
        val second = checkNotNull(round.question)
        round = round.choose(second.offered.indices.first { it != second.right })
        assertEquals(false, round.given?.correct)
        round = round.next()
        while (!round.finished) round = round.choose(checkNotNull(round.question).right).next()
        assertEquals(Score(phrases.size - 1, of = phrases.size), round.score)
    }
}
