package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FlatHuntTest {
    private val words = listOf(
        item("szukam", "szukam", "I am looking for"), item("kawalerka", "kawalerka", "a studio"), item("pokoj", "pokój", "a room"),
        item("cichy", "cichy", "quiet"), item("parter", "parter", "the ground floor"), item("taras", "taras", "a terrace"),
    )
    private val later = listOf(item("sloneczny", "słoneczny", "sunny"), item("ciemny", "ciemny", "dark"))

    private fun text(text: String, vararg items: String) = FlatText(text, mapOf("en" to text), items.toList())

    private fun hunt(id: String, right: Int, vararg ids: String) = FlatHuntCase(
        id,
        text("Szukam: $id.", "szukam", *ids),
        listOf(text("Kawalerka $id.", "kawalerka", *ids), text("Pokój $id.", "pokoj", *ids), text("Taras $id.", "taras", *ids)),
        right,
    )

    private val hunts = listOf(
        hunt("cichy", 0, "cichy"), hunt("parter", 1, "parter"), hunt("cichy-parter", 2, "cichy", "parter"),
        hunt("sloneczny", 0, "sloneczny"), hunt("ciemny", 1, "ciemny"),
    )

    /** l1 teaches what the first three hunts use, l2 what the last two do. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *words.map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l2", *later.map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
            ),
        ),
        words + later,
        flatHunt = hunts,
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun state(player: Player) = player.practice().view().games.single { it.game == Game.FLAT_HUNT }.state

    private fun round(seed: Int, vararg days: String): FlatHuntRound {
        val practice = player(seed).completed(*days).practice()
        return practice.start(practice.setup(Game.FLAT_HUNT, emptyList())) as FlatHuntRound
    }

    @Test
    fun `Flat hunt opens once three hunts have every Item of their wish and adverts learned`() {
        assertEquals(GameState.NEEDS_MORE, state(player()))
        assertEquals(GameState.READY, state(player().completed("l1")))
        assertEquals(3, round(0, "l1").total)
        assertEquals(FlatHunt.QUESTIONS, round(0, "l1", "l2").total)
    }

    @Test
    fun `each hunt shows its three adverts in some order, with the right one where it went`() {
        val orders = mutableSetOf<List<String>>()
        repeat(10) { seed ->
            round(seed, "l1", "l2").run.questions.forEach { question ->
                val hunt = hunts.single { it.id == question.key }
                assertEquals(hunt.adverts.map { it.text }.sorted(), question.adverts.map { it.text }.sorted())
                assertEquals(hunt.adverts[hunt.right].text, question.adverts[question.right].text)
                assertEquals(hunt.wish.text, question.wish)
                if (hunt.id == "cichy") orders += question.adverts.map { it.text }
            }
        }
        assertTrue(orders.size > 1, "The adverts are always in the same order")
    }

    @Test
    fun `an advert is tapped once, and the score counts the hunts right first try`() {
        var round = round(0, "l1")
        val first = round.question!!
        // No such advert: nothing changes.
        assertEquals(round, round.choose(FlatHunt.ADVERTS))
        val wrong = (first.right + 1) % FlatHunt.ADVERTS
        round = round.choose(wrong)
        assertEquals(false, round.given!!.correct)
        assertEquals(wrong, round.chosen)
        // Answered once: a second tap changes nothing.
        assertEquals(round, round.choose(first.right))
        round = round.next()
        while (!round.finished) round = round.choose(round.question!!.right).next()
        assertEquals(Score(2, of = 3), round.score)
        assertNull(round.question)
    }
}
