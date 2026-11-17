package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuickRoundTest {
    private fun player(seed: Int = 1) = Player(course = wordsCourse, seed = seed)

    private fun Practice.quick(vararg wanted: String): QuickRound = start(setup(Game.QUICK_ROUND, wanted.toList())) as QuickRound

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.QUICK_ROUND }.state

    private fun QuickRound.answerRightly(): QuickRound = answer(checkNotNull(card).matches)

    private fun QuickRound.answerWrongly(): QuickRound = answer(!checkNotNull(card).matches)

    @Test
    fun `the Quick round opens with six learned words`() {
        // w1 teaches five words, w2 two more.
        assertEquals(GameState.NEEDS_MORE, stateOf(player().completed("w1")))
        assertEquals(GameState.READY, stateOf(player().completed("w1", "w2")))
    }

    @Test
    fun `a word is matched with a picture or with a meaning, for 60 seconds or 120`() {
        val practice = player().completed("w1", "w2").practice()
        val setup = practice.setup(Game.QUICK_ROUND)

        assertEquals(listOf("picture", "meaning"), setup.choices[0].options.map { it.id })
        assertEquals(listOf("seconds-60", "seconds-120"), setup.choices[1].options.map { it.id })
        assertEquals(60, practice.quick().seconds)
        assertEquals(120, practice.quick("meaning", "seconds-120").seconds)
        // With five pictures there are too few to match by picture.
        val few = Player(course = wordsCourse.copy(pictures = wordPictures - "kot")).completed("w1", "w2").practice()
        assertEquals(listOf("meaning"), few.setup(Game.QUICK_ROUND).choices[0].options.map { it.id })
    }

    @Test
    fun `a card shows a learned word beside its own picture or meaning, or another word's`() {
        val learned = setOf("kot", "pies", "dom", "tak", "bardzo", "ryba", "ptak")
        val practice = player().completed("w1", "w2").practice()

        val meant = practice.quick("meaning")
        assertFalse(meant.pictures)
        meant.cards.forEach { card ->
            assertTrue(card.itemId in learned)
            assertEquals("${card.itemId} in English", card.meaning)
            assertEquals(card.matches, card.shownMeaning == card.meaning, "$card")
            assertTrue(card.shownMeaning.removeSuffix(" in English") in learned)
            assertEquals(null, card.shownPicture)
        }
        // Both kinds of card come up, and more cards are dealt than anyone answers.
        assertTrue(meant.cards.any { it.matches } && meant.cards.any { !it.matches })
        assertEquals(120, meant.cards.size)

        val pictured = practice.quick("picture")
        assertTrue(pictured.pictures)
        pictured.cards.forEach { card ->
            // "bardzo" has no picture.
            assertTrue(card.itemId in learned - "bardzo")
            assertEquals(card.matches, card.shownPicture == wordPictures[card.itemId], "$card")
        }
    }

    @Test
    fun `every word comes round before any comes again, and none twice running`() {
        val round = player().completed("w1", "w2").practice().quick("meaning")
        val words = round.cards.map { it.itemId }

        assertEquals(7, words.take(7).toSet().size)
        assertEquals(7, words.subList(7, 14).toSet().size)
        assertTrue(words.zipWithNext().none { (one, next) -> one == next })
    }

    @Test
    fun `a right answer brings the next card at once`() {
        val start = player().completed("w1", "w2").practice().quick()

        val next = start.answerRightly()

        assertEquals(1, next.right)
        assertEquals(1, next.index)
        assertFalse(next.missed)
        assertEquals(start.cards[1], next.card)
        assertEquals(Score(1), next.score)
    }

    @Test
    fun `a wrong answer stays on its card until the Learner moves on, and is not counted as right`() {
        val start = player().completed("w1", "w2").practice().quick()

        val wrong = start.answerWrongly()
        assertTrue(wrong.missed)
        assertEquals(start.card, wrong.card)
        assertEquals(0, wrong.right)
        assertEquals(1, wrong.answered)
        // While it is shown, nothing is answered.
        assertEquals(wrong, wrong.answer(true))
        assertEquals(wrong, wrong.answer(false))

        val next = wrong.next()
        assertFalse(next.missed)
        assertEquals(start.cards[1], next.card)
        assertEquals(start, start.next())
    }

    @Test
    fun `the clock runs down a second at a time, and at none left the Round is finished`() {
        val start = player().completed("w1", "w2").practice().quick()
        assertEquals(60, start.left)
        assertEquals(0, start.done)
        assertEquals(60, start.total)
        assertEquals(0, start.step)

        var round = start.answerRightly().answerWrongly()
        repeat(59) { round = round.tick() }
        assertEquals(1, round.left)
        assertFalse(round.finished)
        // The clock runs while a wrong answer is shown.
        assertTrue(round.missed)

        val over = round.tick()
        assertTrue(over.finished)
        assertEquals(null, over.card)
        assertEquals(over, over.tick())
        assertEquals(over, over.answer(true))
        assertEquals(Score(1), over.score)
        assertEquals(setOf(start.cards[0].itemId, start.cards[1].itemId), over.asked.toSet() + start.cards[1].itemId)
    }

    @Test
    fun `the result is the right answers, and the most is the Best of that time`() {
        val player = player().completed("w1", "w2")
        val practice = player.practice()
        fun play(right: Int, vararg wanted: String): QuickRound {
            var round = practice.quick(*wanted)
            repeat(right) { round = round.answerRightly() }
            repeat(round.seconds) { round = round.tick() }
            return round
        }

        val first = practice.finish(play(3, "meaning", "seconds-60"))
        assertEquals(Score(3), first.score)
        assertTrue(first.first)
        // Fewer is not a new Best; more is.
        assertFalse(practice.finish(play(2, "meaning", "seconds-60")).newBest)
        assertTrue(practice.finish(play(5, "meaning", "seconds-60")).newBest)
        assertEquals(5, player.practice().setup(Game.QUICK_ROUND, listOf("meaning", "seconds-60")).best)
        assertEquals(null, player.practice().setup(Game.QUICK_ROUND, listOf("meaning", "seconds-120")).best)
    }
}
