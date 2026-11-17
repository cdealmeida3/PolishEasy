package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LabelItTest {
    private val pictured = listOf("zeszyt", "torba", "plecak", "klucz", "mapa", "okno", "krzesło")

    /** p1 teaches six pictured words and one with no picture, p2 a seventh pictured word and a number drawn as a glyph. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("p1", *(pictured.take(6) + "tak").map { choose("e-$it", it, it) }.toTypedArray()),
                day("p2", choose("e-krzesło", "krzesło", "krzesło"), choose("e-siedem", "siedem", "siedem")),
            ),
        ),
        (pictured + "tak" + "siedem").map { item(it) },
        pictures = pictured.associateWith { Picture.Image("pictures/$it.webp") } + ("siedem" to Picture.Glyph("7")),
    )

    private fun player() = Player(course = course)

    private fun Practice.label(way: String = "choose"): LabelRound = start(setup(Game.LABEL_IT, listOf(way))) as LabelRound

    /** Labels every picture, right unless it is one of the first [wrong]. */
    private fun LabelRound.playOut(wrong: Int = 0): LabelRound {
        var round = this
        var missed = 0
        while (!round.finished) {
            val asked = round.run.current!!
            val right = round.labels.indexOf(asked)
            val label = if (missed++ < wrong) round.labels.indices.first { it != right && !round.placed(it) } else right
            round = round.choose(label).next()
        }
        return round
    }

    @Test
    fun `the Game opens with six words that have an image, and a number drawn as a numeral is no picture to label`() {
        fun state(vararg done: String) = player().completed(*done).practice().view().games.single { it.game == Game.LABEL_IT }.state
        assertEquals(GameState.NEEDS_MORE, state())
        assertEquals(GameState.READY, state("p1"))
        val round = player().completed("p1", "p2").practice().label()
        assertTrue(round.spots.none { it.itemId == "siedem" || it.itemId == "tak" })
        assertEquals(6, round.spots.size)
    }

    @Test
    fun `a board asks its pictures in order, with every label lying below, mixed`() {
        val round = player().completed("p1").practice().label()
        assertEquals(pictured.take(6).toSet(), round.spots.map { it.itemId }.toSet())
        assertEquals(round.spots.indices.toSet(), round.labels.toSet())
        assertTrue(round.labels != round.spots.indices.toList())
        assertEquals(round.spots[0], round.spot)
        assertEquals(listOf(true, false, false, false, false, false), round.board.map { it.current })
        assertTrue(round.board.none { it.labelled })
        // The board is one step, filled in place.
        assertEquals(0, round.step)
    }

    @Test
    fun `a right label sticks, a wrong one puts the right label on and stays below, and each picture is answered once`() {
        var round = player().completed("p1").practice().label()
        val right = round.labels.indexOf(0)
        round = round.choose(right)
        assertEquals(Given(round.spots[0].polish, correct = true), round.given)
        assertEquals(true, round.board[0].right)
        assertTrue(round.placed(right))
        // Answered once: another tap changes nothing.
        assertEquals(round, round.choose(round.labels.indexOf(1)))
        round = round.next()
        // A placed label cannot be chosen again.
        assertEquals(round, round.choose(right))

        val wrong = round.labels.indexOf(2)
        round = round.choose(wrong)
        assertEquals(Given(round.spots[2].polish, correct = false), round.given)
        assertEquals(false, round.board[1].right)
        assertTrue(round.board[1].labelled)
        assertFalse(round.placed(wrong))
        assertTrue(round.placed(round.labels.indexOf(1)))
        round = round.next()

        val finished = round.playOut()
        assertTrue(finished.finished)
        assertNull(finished.spot)
        assertTrue(finished.board.all { it.labelled })
        assertEquals(Score(5, of = 6), finished.score)
    }

    @Test
    fun `typed, a word right but for its Polish marks is right, and anything else is wrong`() {
        var round = player().completed("p1", "p2").practice().label("type")
        assertTrue(round.typed)
        // Typed, the labels below are not tapped.
        assertEquals(round, round.choose(0))
        assertEquals(round, round.type("  "))
        val first = checkNotNull(round.spot)
        round = round.type(first.polish.uppercase().replace('ł', 'l').replace('Ł', 'L') + "!")
        assertEquals(true, round.given?.correct)
        round = round.next().type("nie wiem")
        assertEquals(false, round.given?.correct)
    }

    @Test
    fun `the result is the labels right first try, kept as the Best of each way`() {
        val practice = player().completed("p1").practice()
        val worse = practice.finish(practice.label().playOut(wrong = 2))
        assertEquals(4, worse.best)
        val better = practice.finish(practice.label().playOut())
        assertEquals(6, better.best)
        assertTrue(better.newBest)
        assertEquals(listOf("choose", "type"), practice.setup(Game.LABEL_IT).choices.first().options.map { it.id })
    }
}
