package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SpellItTest {
    private fun player() = Player(course = wordsCourse)

    private fun Practice.spell(vararg wanted: String): SpellRound = start(setup(Game.SPELL_IT, wanted.toList())) as SpellRound

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.SPELL_IT }.state

    /** The tile to tap next: the first one not placed that has the letter that belongs. */
    private fun SpellRound.rightTile(): Int {
        val word = checkNotNull(word)
        return word.tiles.indices.first { it !in placed && word.tiles[it] == word.letters[placed.size] }
    }

    /** A tile not placed that has another letter than the one that belongs, or null when every tile left has it. */
    private fun SpellRound.wrongTile(): Int? {
        val word = checkNotNull(word)
        return word.tiles.indices.firstOrNull { it !in placed && word.tiles[it] != word.letters[placed.size] }
    }

    private fun SpellRound.spellWord(): SpellRound {
        var round = this
        while (!round.complete) round = round.tap(round.rightTile())
        return round
    }

    /** Spells every word, the first [slipped] of them after one wrong tile. */
    private fun SpellRound.playOut(slipped: Int = 0): SpellRound {
        var round = this
        var missed = 0
        while (!round.finished) {
            if (missed++ < slipped) round = round.tap(checkNotNull(round.wrongTile()))
            round = round.spellWord().next()
        }
        return round
    }

    @Test
    fun `Spell it opens with four learned words that can be spelled`() {
        // w2 teaches two words, and w3 two more.
        assertEquals(GameState.NEEDS_MORE, stateOf(player().completed("w2")))
        assertEquals(GameState.READY, stateOf(player().completed("w2", "w3")))
    }

    @Test
    fun `a word is spelled from its picture or from its meaning`() {
        val practice = player().completed("w1").practice()

        // kot, pies, dom and tak have a picture; "bardzo" has none and is spelled from its meaning.
        assertEquals(listOf("picture", "meaning"), practice.setup(Game.SPELL_IT).choices[0].options.map { it.id })
        val pictured = practice.spell("picture")
        assertTrue(pictured.pictures)
        assertEquals(setOf("kot", "pies", "dom", "tak"), pictured.asked.toSet())
        assertTrue(pictured.words.all { it.picture == wordPictures[it.itemId] })

        val meant = practice.spell("meaning")
        assertEquals(setOf("kot", "pies", "dom", "tak", "bardzo"), meant.asked.toSet())
        assertTrue(meant.words.all { it.picture == null && it.meaning == "${it.itemId} in English" })
    }

    @Test
    fun `only one word of small letters, three to nine of them, is spelled`() {
        val course = Course(
            listOf(levelOf("a1", day("s1", *listOf("kot", "pies", "dom", "ryba", "bo", "Adam", "do jutra", "dziewiętnaście", "co?").map { choose("e-$it", it, it) }.toTypedArray()))),
            listOf("kot", "pies", "dom", "ryba", "bo", "Adam", "do jutra", "dziewiętnaście", "co?").map { item(it) },
        )

        val round = Player(course = course).completed("s1").practice().spell()

        assertEquals(setOf("kot", "pies", "dom", "ryba"), round.asked.toSet())
    }

    @Test
    fun `a Round is six words at most, each with its letters as tiles, mixed`() {
        val round = player().completed("w1", "w2", "w3", "w4", "w5", "w6").practice().spell("meaning")

        assertEquals(SpellIt.WORDS, round.total)
        assertEquals(round.asked.size, round.asked.toSet().size)
        round.words.forEach { word ->
            assertEquals(word.letters.sorted(), word.tiles.sorted())
            assertNotEquals(word.letters, word.tiles, "${word.polish} is laid out already spelled")
        }
    }

    @Test
    fun `Polish letters are tiles of their own`() {
        val course = Course(
            listOf(levelOf("a1", day("s1", *listOf("cześć", "żona", "mąż", "pięć").map { choose("e-$it", it, it) }.toTypedArray()))),
            listOf("cześć", "żona", "mąż", "pięć").map { item(it) },
        )

        val round = Player(course = course).completed("s1").practice().spell()

        assertEquals(listOf("c", "e", "z", "ć", "ś"), round.words.single { it.polish == "cześć" }.tiles.sorted())
        assertTrue("ą" in round.words.single { it.polish == "mąż" }.tiles)
    }

    @Test
    fun `the right letter takes its place, and a wrong one stays and is a slip`() {
        val start = player().completed("w1").practice().spell("meaning")
        val wrong = checkNotNull(start.wrongTile())

        val slipped = start.tap(wrong)
        assertEquals(emptyList(), slipped.placed)
        assertEquals(1, slipped.slips)
        assertEquals(wrong, slipped.missed)

        val right = slipped.tap(slipped.rightTile())
        assertEquals(1, right.placed.size)
        assertEquals(null, right.missed)
        // A tile in the word is not tapped again, and there is no tile beyond the last.
        assertEquals(right, right.tap(right.placed.single()))
        assertEquals(right, right.tap(99))
        // The word is not whole: there is nothing to move on from.
        assertEquals(right, right.next())
    }

    @Test
    fun `with a letter written twice, either tile of it will do`() {
        val course = Course(
            listOf(levelOf("a1", day("s1", *listOf("mama", "tata", "kot", "dom").map { choose("e-$it", it, it) }.toTypedArray()))),
            listOf("mama", "tata", "kot", "dom").map { item(it) },
        )
        var round = Player(course = course).completed("s1").practice().spell()
        while (checkNotNull(round.word).polish != "mama") round = round.spellWord().next()
        val tiles = checkNotNull(round.word).tiles

        // The last "m" tile first: it is as good as the other.
        val first = round.tap(tiles.lastIndexOf("m"))
        assertEquals(0, first.slips)
        assertEquals(listOf(tiles.lastIndexOf("m")), first.placed)
        assertEquals("mama", first.spellWord().given?.text)
    }

    @Test
    fun `a whole word is right if it was spelled with no slip, and the next starts clean`() {
        val start = player().completed("w1").practice().spell("meaning")

        val clean = start.spellWord()
        assertTrue(clean.complete)
        assertEquals(Given(checkNotNull(start.word).polish, true), clean.given)
        assertEquals(1, clean.done)
        // Whole, no tile does anything.
        assertEquals(clean, clean.tap(0))

        val slipped = start.tap(checkNotNull(start.wrongTile())).spellWord()
        assertEquals(false, slipped.given?.correct)
        val next = slipped.next()
        assertEquals(1, next.step)
        assertEquals(0, next.slips)
        assertEquals(emptyList(), next.placed)
        assertEquals(null, next.given)
    }

    @Test
    fun `the result is the words spelled with no slip, and the most is the Best`() {
        val player = player().completed("w1")
        val practice = player.practice()

        val first = practice.finish(practice.spell("meaning").playOut(slipped = 2))
        assertEquals(Score(3, of = 5), first.score)
        assertTrue(first.first)
        assertTrue(practice.finish(practice.spell("meaning").playOut()).newBest)
        assertEquals(5, player.practice().setup(Game.SPELL_IT, listOf("meaning")).best)
        assertEquals(null, player.practice().setup(Game.SPELL_IT, listOf("picture")).best)
    }

    /** [wordsCourse] with a name for every letter of its words, each with a Clip, except those in [unsaid]. */
    private fun spelledCourse(unsaid: Set<String> = emptySet()): Course {
        val names = "abdeikmoprstyz".toSet().associate { it.toString() to "$it-name" }
        return wordsCourse.copy(letterNames = names, clips = names.filterKeys { it !in unsaid }.values.map { clip(it) })
    }

    @Test
    fun `a word can be spelled aloud, letter by letter, only with sound on and every letter's name said`() {
        val practice = Player(course = spelledCourse()).completed("w1").practice()

        assertEquals(listOf("picture", "meaning", "aloud"), practice.setup(Game.SPELL_IT).choices[0].options.map { it.id })
        assertEquals(listOf("picture", "meaning"), practice.setup(Game.SPELL_IT, sound = false).choices[0].options.map { it.id })
        // With no names for the letters there is nothing to say.
        assertEquals(listOf("picture", "meaning"), player().completed("w1").practice().setup(Game.SPELL_IT).choices[0].options.map { it.id })
    }

    @Test
    fun `spelled aloud, a word shows nothing of itself, and its letters' names are said in order`() {
        val round = Player(course = spelledCourse()).completed("w1").practice().spell("aloud")

        assertTrue(round.aloud)
        round.words.forEach { word ->
            assertEquals(null, word.picture)
            assertEquals(word.letters.map { "$it-name" }, word.spelled)
        }
        // It is played like any other: the right tiles in order make the word whole, and right.
        assertEquals(true, round.spellWord().given?.correct)
        assertEquals(false, Player(course = spelledCourse()).completed("w1").practice().spell("meaning").aloud)
    }

    @Test
    fun `a word with a letter whose name has no Clip is not spelled aloud`() {
        // With no Clip for k, w1 leaves pies, dom and bardzo: too few to spell aloud.
        val few = Player(course = spelledCourse(unsaid = setOf("k"))).completed("w1").practice()
        assertEquals(listOf("picture", "meaning"), few.setup(Game.SPELL_IT).choices[0].options.map { it.id })
        // w2 adds ryba (and ptak, which has a k): four words, none of them with a k.
        val round = Player(course = spelledCourse(unsaid = setOf("k"))).completed("w1", "w2").practice().spell("aloud")
        assertEquals(setOf("pies", "dom", "bardzo", "ryba"), round.words.map { it.polish }.toSet())
    }
}
