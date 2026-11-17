package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PhraseChainsTest {
    private fun sentence(id: String, answer: String, vararg others: String, distractors: List<String> = emptyList()) =
        WordTiles(id, mapOf("en" to "$answer in English"), listOf(answer) + others, distractors)

    /**
     * Day c1 has three short sentences, one of them twice and one of a single word; day c2 two
     * medium ones and a second word order; day c3 a third medium one and three long ones.
     */
    private val days = arrayOf(
        day(
            "c1",
            sentence("s1", "to jest Adam", distractors = listOf("nie")),
            sentence("s2", "cześć, do jutra", "pa, do jutra", distractors = listOf("pa", "tak")),
            sentence("s3", "tak, proszę"),
            sentence("s3-again", "Tak, proszę."),
            sentence("one", "dziękuję"),
        ),
        day(
            "c2",
            sentence("m1", "to jest moja mama", "moja mama to jest"),
            sentence("m2", "ja mam na imię Adam", "mam na imię Adam"),
        ),
        day(
            "c3",
            sentence("m3", "on nie jest z Polski"),
            sentence("l1", "to jest mój brat i moja siostra"),
            sentence("l2", "czy to jest twój dom czy nie"),
            sentence("l3", "ja nie mam czasu ale ona ma"),
        ),
    )

    private val chains = courseOf(emptyList(), *days)

    /** The same course with a Clip for the first Accepted Answer of every sentence. */
    private val spoken = chains.copy(clips = days.flatMap { it.exercises }.filterIsInstance<WordTiles>().map { clip(it.acceptedAnswers.first()) })

    private fun player(course: Course = spoken) = Player(course = course)

    private fun Practice.chain(vararg wanted: String, sound: Boolean = true): ChainRound =
        start(setup(Game.PHRASE_CHAINS, wanted.toList(), sound), sound) as ChainRound

    private fun Practice.choicesOf(vararg wanted: String, sound: Boolean = true) =
        setup(Game.PHRASE_CHAINS, wanted.toList(), sound).choices.associate { choice -> choice.id to choice.options.map { it.id } }

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.PHRASE_CHAINS }.state

    /** A card that says [word] and is not yet in the chain. */
    private fun ChainRound.card(word: String): Int =
        checkNotNull(sentence).cards.indices.first { checkNotNull(sentence).cards[it] == word && it !in chain }

    /** Taps the cards that spell [answer], in order. */
    private fun ChainRound.build(answer: String): ChainRound = words(answer).fold(this) { round, word -> round.tap(round.card(word)) }

    /** Chains every sentence the way it is first written, with [slips] wrong taps on the first. */
    private fun ChainRound.playOut(slips: Int = 0): ChainRound {
        var round = this
        repeat(slips) {
            val sentence = checkNotNull(round.sentence)
            round = round.tap(sentence.cards.indices.first { sentence.cards[it] != words(sentence.polish).first() })
        }
        while (!round.finished) round = round.build(checkNotNull(round.sentence).polish).next()
        return round
    }

    @Test
    fun `Phrase chains opens once three sentences have been built in completed Daily Sets`() {
        val two = courseOf(emptyList(), day("c1", sentence("s1", "to jest Adam"), sentence("s2", "tak, proszę"), sentence("one", "dziękuję")))
        assertEquals(GameState.NEEDS_MORE, stateOf(player(two).completed("c1")))
        assertEquals(GameState.READY, stateOf(player().completed("c1")))
    }

    @Test
    fun `a Round is up to five sentences the Learner has built, each once`() {
        val round = player().completed("c1", "c2", "c3").practice().chain(PhraseChains.LISTEN, PhraseChains.MIXED)

        assertEquals(5, round.total)
        assertEquals(5, round.asked.toSet().size)
        // A sentence written twice is one sentence, and a single word is not a chain.
        assertFalse("s3-again" in round.asked)
        assertFalse("one" in round.asked)
        assertEquals(listOf("s1", "s2", "s3"), player().completed("c1").practice().chain().asked.sorted())
    }

    @Test
    fun `the cards are the words of the sentence and the words that do not belong, mixed`() {
        val round = player().completed("c1").practice().chain()
        val adam = round.sentences.single { it.id == "s1" }

        assertEquals(listOf("Adam", "jest", "nie", "to"), adam.cards.sorted())
        assertEquals("to jest Adam", adam.polish)
        assertEquals("to jest Adam in English", adam.meaning)
        val hello = round.sentences.single { it.id == "s2" }
        assertEquals(listOf("cześć", "do", "jutra", "pa", "tak"), hello.cards.sorted())
    }

    @Test
    fun `a right card joins the chain, and the whole chain waits to be moved on from`() {
        var round = player().completed("c1").practice().chain()
        val polish = checkNotNull(round.sentence).polish
        val first = words(polish).first()

        round = round.tap(round.card(first))
        assertEquals(listOf(first), round.chain.map { checkNotNull(round.sentence).cards[it] })
        assertFalse(round.complete)
        // A card in the chain cannot be tapped again, and an unfinished chain cannot be moved on from.
        assertSame(round, round.tap(round.chain.single()))
        assertSame(round, round.next())
        assertSame(round, round.tap(99))

        round = round.copy(chain = emptyList()).build(polish)
        assertTrue(round.complete)
        assertEquals(polish, round.said)
        assertEquals(1, round.done)
        assertEquals(0, round.step)
        // Nothing more can be chained on.
        assertSame(round, round.tap((checkNotNull(round.sentence).cards.indices - round.chain.toSet()).firstOrNull() ?: 0))

        val next = round.next()
        assertEquals(1, next.step)
        assertTrue(next.chain.isEmpty())
    }

    @Test
    fun `a wrong card stays where it is and is a slip`() {
        val start = player().completed("c1").practice().chain()
        val sentence = checkNotNull(start.sentence)
        val wrong = sentence.cards.indices.first { sentence.cards[it] != words(sentence.polish).first() && sentence.accepted.none { answer -> words(answer).first() == sentence.cards[it] } }

        val slipped = start.tap(wrong)
        assertTrue(slipped.chain.isEmpty())
        assertEquals(1, slipped.slips)
        assertEquals(wrong, slipped.missed)
        // The same wrong card again is another slip; the next right one clears the mark.
        assertEquals(2, slipped.tap(wrong).slips)
        val right = slipped.tap(slipped.card(words(sentence.polish).first()))
        assertNull(right.missed)
        assertEquals(1, right.slips)
    }

    @Test
    fun `read, every accepted word order completes a chain`() {
        val practice = player().completed("c1", "c2").practice()
        fun at(id: String): ChainRound {
            var round = practice.chain(PhraseChains.READ, PhraseChains.MIXED)
            while (checkNotNull(round.sentence).id != id) round = round.build(checkNotNull(round.sentence).polish).next()
            return round
        }

        val other = at("m1").build("moja mama to jest")
        assertTrue(other.complete)
        assertEquals("moja mama to jest", other.said)
        assertEquals(0, other.slips)

        // "pa" is a word that does not belong to the first answer, and starts the second.
        val bye = at("s2").build("pa, do jutra")
        assertEquals("pa, do jutra", bye.said)

        // A shorter accepted order is whole as soon as it is built.
        val short = at("m2").build("mam na imię Adam")
        assertTrue(short.complete)
        assertEquals("mam na imię Adam", short.said)
    }

    @Test
    fun `heard, only the order that was said is right`() {
        val practice = player().completed("c1", "c2").practice()
        var round = practice.chain(PhraseChains.LISTEN, PhraseChains.MIXED)
        while (checkNotNull(round.sentence).id != "m1") round = round.build(checkNotNull(round.sentence).polish).next()

        assertTrue(round.listen)
        val slipped = round.tap(round.card("moja"))
        assertEquals(1, slipped.slips)
        assertTrue(slipped.build("to jest moja mama").complete)
    }

    @Test
    fun `an accepted order the cards cannot make is not offered as a way through`() {
        // The second answer needs "ona", which is not among the cards.
        val course = courseOf(
            emptyList(),
            day("c1", sentence("s1", "to jest Ewa", "ona to Ewa"), sentence("s2", "tak, proszę"), sentence("s3", "nie, dziękuję")),
        )
        var round = player(course).completed("c1").practice().chain(PhraseChains.READ)
        while (checkNotNull(round.sentence).id != "s1") round = round.build(checkNotNull(round.sentence).polish).next()

        assertEquals(listOf("to jest Ewa"), checkNotNull(round.sentence).accepted)
        assertTrue(round.build("to jest Ewa").complete)
    }

    @Test
    fun `the lengths offered are those with three sentences, and all of them together`() {
        val practice = player().completed("c1", "c2", "c3").practice()

        assertEquals(
            mapOf(
                PhraseChains.CLUE to listOf(PhraseChains.LISTEN, PhraseChains.READ),
                PhraseChains.LEVEL to listOf(PhraseChains.SHORT, PhraseChains.MEDIUM, PhraseChains.LONG, PhraseChains.MIXED),
            ),
            practice.choicesOf(),
        )
        assertEquals(setOf("m1", "m2", "m3"), practice.chain(PhraseChains.LISTEN, PhraseChains.MEDIUM).asked.toSet())
        assertEquals(setOf("l1", "l2", "l3"), practice.chain(PhraseChains.READ, PhraseChains.LONG).asked.toSet())

        // Two medium sentences are not a level; one level alone needs no "mixed".
        val earlier = player().completed("c1", "c2").practice()
        assertEquals(listOf(PhraseChains.SHORT, PhraseChains.MIXED), earlier.choicesOf().getValue(PhraseChains.LEVEL))
        assertEquals(listOf(PhraseChains.SHORT), player().completed("c1").practice().choicesOf().getValue(PhraseChains.LEVEL))
    }

    @Test
    fun `with sound off, or no Clips, the sentence is read`() {
        val practice = player().completed("c1").practice()
        assertEquals(listOf(PhraseChains.READ), practice.choicesOf(sound = false).getValue(PhraseChains.CLUE))
        assertFalse(practice.chain(PhraseChains.LISTEN, sound = false).listen)

        assertEquals(listOf(PhraseChains.READ), player(chains).completed("c1").practice().choicesOf().getValue(PhraseChains.CLUE))
    }

    @Test
    fun `the result is the slips, and the fewest is the Best`() {
        val practice = player().completed("c1").practice()

        val first = practice.finish(practice.chain().playOut(slips = 3))
        assertEquals(RoundResult(Game.PHRASE_CHAINS, listOf(PhraseChains.LISTEN, PhraseChains.SHORT), Score(3), best = 3, first = true), first)

        val worse = practice.finish(practice.chain().playOut(slips = 4))
        assertEquals(3, worse.best)
        assertFalse(worse.newBest)

        val clean = practice.finish(practice.chain().playOut())
        assertEquals(0, clean.best)
        assertTrue(clean.newBest)
        assertEquals(0, practice.view().games.single { it.game == Game.PHRASE_CHAINS }.best)
    }

    @Test
    fun `only sentences of completed Daily Sets are chained`() {
        val practice = player().completed("c1", "c2").practice()

        repeat(5) {
            assertTrue(setOf("s1", "s2", "s3", "m1", "m2").containsAll(practice.chain(PhraseChains.READ, PhraseChains.MIXED).asked))
        }
    }
}
