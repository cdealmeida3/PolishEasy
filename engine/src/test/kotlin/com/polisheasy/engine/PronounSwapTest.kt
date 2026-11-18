package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class PronounSwapTest {
    private val words = listOf(item("kocham"), item("tata-acc", "tatę"), item("go"), item("czytam"), item("ksiazka-acc", "książkę"), item("ja-acc", "ją"),
        item("czekam"), item("na-nia", "na nią"), item("ciocia-acc", "ciocię"), item("znam"), item("rodzice-acc", "rodziców"), item("ich-acc", "ich"))

    private fun swap(text: String, noun: String, pronoun: String, vararg items: String) =
        SwapSentence(text, mapOf("en" to "$text (en)"), noun, pronoun, mapOf("en" to "short (en)"), items.toList())

    private val swaps = listOf(
        swap("Kocham tatę.", "tatę", "go", "kocham", "tata-acc", "go"),
        swap("Czytam książkę.", "książkę", "ją", "czytam", "ksiazka-acc", "ja-acc"),
        swap("Czekam na ciocię.", "ciocię", "nią", "czekam", "na-nia", "ciocia-acc"),
        swap("Znam rodziców.", "rodziców", "ich", "znam", "rodzice-acc", "ich-acc"),
    )

    /** l1 teaches three sentences' words, l2 the fourth's. */
    private val course = Course(
        listOf(levelOf("a1", day("l1", *words.take(9).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()), day("l2", *words.drop(9).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()))),
        words,
        swaps = swaps,
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun Practice.round(): SwapRound = start(setup(Game.PRONOUN_SWAP, listOf(PronounSwap.READ))) as SwapRound

    @Test
    fun `the noun is swapped for its pronoun, those after na offered after na`() {
        assertEquals("Kocham go.", swaps[0].short)
        assertEquals("Czekam na nią.", swaps[2].short)
        assertEquals(Pronouns.ALONE, swaps[0].offered)
        assertEquals(Pronouns.AFTER_NA, swaps[2].offered)
    }

    @Test
    fun `Pronoun swap opens once four sentences are learned`() {
        val state = { p: Player -> p.practice().view().games.single { it.game == Game.PRONOUN_SWAP }.state }
        assertEquals(GameState.NEEDS_MORE, state(player()))
        assertEquals(GameState.NEEDS_MORE, state(player().completed("l1")))
        assertEquals(GameState.READY, state(player().completed("l1", "l2")))
    }

    @Test
    fun `a question cuts the sentence around its noun, and the right pronoun is right first try`() {
        var round = player().completed("l1", "l2").practice().round()
        assertEquals(swaps.size, round.total)
        round.run.questions.forEach { assertEquals(it.text, it.before + it.noun + it.after) }
        val first = checkNotNull(round.question)
        round = round.choose(first.offered.indexOf(first.pronoun))
        assertEquals(true, round.given?.correct)
        assertEquals(round, round.choose(0))
        round = round.next()
        val second = checkNotNull(round.question)
        round = round.choose(second.offered.indexOfFirst { it != second.pronoun })
        assertEquals(false, round.given?.correct)
        round = round.next()
        while (!round.finished) {
            val question = checkNotNull(round.question)
            round = round.choose(question.offered.indexOf(question.pronoun)).next()
        }
        assertEquals(Score(swaps.size - 1, of = swaps.size), round.score)
    }
}
