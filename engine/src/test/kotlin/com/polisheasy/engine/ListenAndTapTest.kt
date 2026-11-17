package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListenAndTapTest {
    /** The words course with a Clip for every word. */
    private val spoken = wordsCourse.copy(clips = wordsCourse.items.map { Clip("clip-${it.id}", it.text) })

    private fun player(course: Course = spoken) = Player(course = course)

    private fun Practice.listen(vararg wanted: String): ListenRound = start(setup(Game.LISTEN_AND_TAP, wanted.toList())) as ListenRound

    private fun Practice.choicesOf(vararg wanted: String) =
        setup(Game.LISTEN_AND_TAP, wanted.toList()).choices.associate { choice -> choice.id to choice.options.map { it.id } }

    private fun stateOf(player: Player, sound: Boolean = true) =
        player.practice().view(sound).games.single { it.game == Game.LISTEN_AND_TAP }.state

    private fun ListenRound.wrongOption(): Int = checkNotNull(question).let { asked -> asked.options.indices.first { it != asked.answer } }

    private fun ListenRound.playOut(wrong: Int = 0): ListenRound {
        var round = this
        var missed = 0
        while (!round.finished) {
            round = round.choose(if (missed++ < wrong) round.wrongOption() else checkNotNull(round.question).answer).next()
        }
        return round
    }

    @Test
    fun `Listen and tap opens once four learned Items have a Clip, and only with sound on`() {
        val quiet = wordsCourse.copy(clips = listOf("kot", "pies", "dom").map { Clip("clip-$it", it) })
        assertEquals(GameState.NEEDS_MORE, stateOf(player(quiet).completed("w1")))
        assertEquals(GameState.READY, stateOf(player().completed("w1")))
        assertEquals(GameState.NEEDS_SOUND, stateOf(player().completed("w1"), sound = false))
        // What is missing comes first: sound would not open it.
        assertEquals(GameState.NEEDS_MORE, stateOf(player(quiet).completed("w1"), sound = false))
    }

    @Test
    fun `it cannot be started with sound off`() {
        val practice = player().completed("w1").practice()

        assertFailsWith<IllegalStateException> { practice.start(practice.setup(Game.LISTEN_AND_TAP), sound = false) }
    }

    @Test
    fun `a word is said, and it is one of four pictures to tap`() {
        val round = player().completed("w1", "w2").practice().listen()

        assertEquals(listOf(ListenAndTap.PICTURE, Decks.ALL), round.played)
        assertTrue(round.pictures)
        // Six pictured words: each is asked once.
        assertEquals(6, round.total)
        assertEquals(setOf("kot", "pies", "dom", "tak", "ryba", "ptak"), round.asked.toSet())
        round.run.questions.forEach { question ->
            assertEquals(4, question.options.map { it.itemId }.toSet().size)
            val right = question.options[question.answer]
            assertEquals(question.polish, right.itemId)
            assertEquals(Picture.Image("pictures/${right.itemId}.webp"), right.picture)
            assertEquals("${right.itemId} in English", right.meaning)
        }
    }

    @Test
    fun `a Round says ten words at most`() {
        val round = player().completed("w1", "w2", "w3", "w4", "w5", "w6").practice().listen(ListenAndTap.MEANING)

        assertEquals(10, round.total)
        assertEquals(10, round.asked.toSet().size)
    }

    @Test
    fun `tapping meanings uses every learned word, with no pictures`() {
        val round = player().completed("w1").practice().listen(ListenAndTap.MEANING)

        assertFalse(round.pictures)
        assertEquals(setOf("kot", "pies", "dom", "tak", "bardzo"), round.asked.toSet())
        assertTrue(round.run.questions.all { question -> question.options.all { it.picture == null } })
    }

    @Test
    fun `pictures are offered only where four words that have one can be heard`() {
        val few = spoken.copy(pictures = wordPictures - "tak" - "dom")
        val practice = player(few).completed("w1").practice()

        assertEquals(listOf(ListenAndTap.MEANING), practice.choicesOf().getValue(ListenAndTap.MODE))
        assertEquals(
            listOf(ListenAndTap.PICTURE, ListenAndTap.MEANING),
            player().completed("w1").practice().choicesOf().getValue(ListenAndTap.MODE),
        )
    }

    @Test
    fun `it is played from a deck`() {
        val practice = player().completed("w1", "w2", "w3", "w4", "w5", "w6").practice()

        assertEquals(listOf(Decks.ALL, "u1", "u2", Decks.LATEST), practice.choicesOf().getValue(ListenAndTap.DECK))
        val family = practice.listen(ListenAndTap.PICTURE, "u2")
        assertEquals(setOf("jeden", "dwa", "mama", "tata", "brat", "siostra"), family.asked.toSet())
        // The other options come from the same deck.
        assertTrue(family.run.questions.all { question -> question.options.all { it.itemId in family.asked } })
    }

    @Test
    fun `two words that would look alike are not asked together`() {
        // "hej" means what "tak" does: tapping meanings, only one of them is used.
        val alike = spoken.copy(
            levels = listOf(levelOf("a1", day("w1", *listOf("kot", "pies", "dom", "tak", "hej").map { choose("e-$it", it, it) }.toTypedArray()))),
            items = spoken.items + item("hej", "hej", "tak in English"),
            clips = spoken.clips + Clip("clip-hej", "hej"),
        )

        val round = player(alike).completed("w1").practice().listen(ListenAndTap.MEANING)

        assertEquals(setOf("kot", "pies", "dom", "tak"), round.asked.toSet())
    }

    @Test
    fun `the first tap answers the question, right or wrong`() {
        val start = player().completed("w1", "w2").practice().listen()
        val asked = checkNotNull(start.question)

        val right = start.choose(asked.answer)
        assertEquals(Given(asked.options[asked.answer].itemId, correct = true), right.given)
        // A second tap changes nothing, and neither does a tap that is no option.
        assertEquals(right, right.choose(start.wrongOption()))
        assertEquals(start, start.choose(7))
        assertEquals(1, right.next().step)
        assertNull(right.next().given)

        val wrong = start.choose(start.wrongOption())
        assertEquals(Given(asked.options[start.wrongOption()].itemId, correct = false), wrong.given)
        // An unanswered question cannot be moved on from.
        assertEquals(start, start.next())
    }

    @Test
    fun `the result is the answers right first try, and the most is the Best`() {
        val practice = player().completed("w1", "w2").practice()

        val first = practice.finish(practice.listen().playOut(wrong = 2))
        assertEquals(RoundResult(Game.LISTEN_AND_TAP, listOf(ListenAndTap.PICTURE, Decks.ALL), Score(4, of = 6), best = 4, first = true), first)
        assertFalse(practice.finish(practice.listen().playOut(wrong = 3)).newBest)
        val better = practice.finish(practice.listen().playOut())
        assertTrue(better.newBest)
        assertEquals(6, better.best)
    }

    @Test
    fun `the next Round does not open with the word the last one opened with`() {
        val practice = player().completed("w1", "w2").practice()

        val firsts = (1..6).map { practice.listen().asked.first() }

        assertTrue(firsts.zipWithNext().none { (one, next) -> one == next })
    }
}
