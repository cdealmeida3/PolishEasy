package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MemoryPairsTest {
    /** The words course with a Clip for every word, so pairs can be made of a sound. */
    private val spoken = wordsCourse.copy(clips = wordsCourse.items.map { Clip("clip-${it.id}", it.text) })

    private fun dayOfSame(id: String, vararg items: String): DailySet =
        DailySet(id, exercises = items.mapIndexed { index, item -> choose("$id-e${index + 1}", item, item) })

    private fun player(course: Course = spoken) = Player(course = course)

    private fun Practice.memory(vararg wanted: String, sound: Boolean = true): MemoryRound =
        start(setup(Game.MEMORY_PAIRS, wanted.toList(), sound), sound) as MemoryRound

    private fun Practice.choicesOf(vararg wanted: String, sound: Boolean = true) =
        setup(Game.MEMORY_PAIRS, wanted.toList(), sound).choices.associate { choice -> choice.id to choice.options.map { it.id } }

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.MEMORY_PAIRS }.state

    /** The two tiles of each pair, by pair. */
    private fun MemoryRound.pairs(): Map<String, List<Int>> = tiles.indices.groupBy { tiles[it].pair }

    /** Two tiles that are not a pair, neither of them found. */
    private fun MemoryRound.miss(): Pair<Int, Int> {
        val left = pairs().filterKeys { it !in found }.values.toList()
        return left[0][0] to left[1][0]
    }

    private fun MemoryRound.findPair(pair: String): MemoryRound = pairs().getValue(pair).fold(this) { round, tile -> round.turn(tile) }

    private fun MemoryRound.playOut(misses: Int = 0): MemoryRound {
        var round = this
        repeat(misses) { round = round.miss().let { (one, other) -> round.turn(one).turn(other) } }
        asked.forEach { round = round.findPair(it) }
        return round
    }

    @Test
    fun `Memory pairs opens once six Items are learned`() {
        assertEquals(GameState.NEEDS_MORE, stateOf(player().completed("w1")))
        assertEquals(GameState.READY, stateOf(player().completed("w1", "w2")))
    }

    @Test
    fun `a board is two tiles for each of its Items, a picture and its word`() {
        val round = player().completed("w1", "w2").practice().memory()

        assertEquals(listOf(MemoryPairs.PICTURE_WORD, Decks.ALL, "pairs-6"), round.played)
        assertEquals(12, round.tiles.size)
        assertEquals(6, round.total)
        assertEquals(setOf("kot", "pies", "dom", "tak", "ryba", "ptak"), round.asked.toSet())
        assertEquals(
            setOf(MemoryFace.Image(Picture.Image("pictures/kot.webp"), "kot in English"), MemoryFace.Word("kot")),
            round.pairs().getValue("kot").map { round.tiles[it].face }.toSet(),
        )
        assertTrue(round.tiles.indices.all { round.stateOf(it) == MemoryTileState.DOWN })
    }

    @Test
    fun `the tiles are mixed, and differently each Round`() {
        val practice = player().completed("w1", "w2").practice()
        val first = practice.memory()
        val second = practice.memory()

        assertFalse(first.tiles.map { it.pair } == first.tiles.map { it.pair }.sorted())
        assertFalse(first.tiles == second.tiles)
    }

    @Test
    fun `pairs can be a word and its meaning, or a sound and its word`() {
        val practice = player().completed("w1", "w2").practice()

        val meanings = practice.memory(MemoryPairs.WORD_MEANING)
        // Every learned Item can be used, with a picture or not: seven, where six have a picture.
        assertEquals(listOf("pairs-6"), practice.choicesOf(MemoryPairs.WORD_MEANING).getValue(MemoryPairs.SIZE))
        assertTrue(setOf("kot", "pies", "dom", "tak", "bardzo", "ryba", "ptak").containsAll(meanings.asked))
        val pair = meanings.asked.first()
        assertEquals(
            setOf(MemoryFace.Word(pair), MemoryFace.Meaning("$pair in English")),
            meanings.pairs().getValue(pair).map { meanings.tiles[it].face }.toSet(),
        )

        val sounds = practice.memory(MemoryPairs.SOUND_WORD)
        val heard = sounds.asked.first()
        assertEquals(setOf(MemoryFace.Sound(heard), MemoryFace.Word(heard)), sounds.pairs().getValue(heard).map { sounds.tiles[it].face }.toSet())
    }

    @Test
    fun `a kind of pair is offered only when there is enough to make a board of it`() {
        // Five pictures: not enough for pictures. No Clips: nothing to hear.
        val bare = wordsCourse.copy(pictures = wordPictures - "tak")
        assertEquals(listOf(MemoryPairs.WORD_MEANING), player(bare).completed("w1", "w2").practice().choicesOf().getValue(MemoryPairs.KIND))

        val practice = player().completed("w1", "w2").practice()
        assertEquals(
            listOf(MemoryPairs.PICTURE_WORD, MemoryPairs.WORD_MEANING, MemoryPairs.SOUND_WORD),
            practice.choicesOf().getValue(MemoryPairs.KIND),
        )
        // With sound off there is nothing to hear.
        assertEquals(listOf(MemoryPairs.PICTURE_WORD, MemoryPairs.WORD_MEANING), practice.choicesOf(sound = false).getValue(MemoryPairs.KIND))
        assertEquals(MemoryPairs.PICTURE_WORD, practice.memory(MemoryPairs.SOUND_WORD, sound = false).played.first())
    }

    @Test
    fun `the decks and sizes are those the kind of pair can fill`() {
        val practice = player().completed("w1", "w2", "w3", "w4", "w5", "w6").practice()

        // Twelve pictures: every size, and both Units have six.
        assertEquals(
            mapOf(
                MemoryPairs.KIND to listOf(MemoryPairs.PICTURE_WORD, MemoryPairs.WORD_MEANING, MemoryPairs.SOUND_WORD),
                MemoryPairs.DECK to listOf(Decks.ALL, "u1", "u2"),
                MemoryPairs.SIZE to listOf("pairs-6", "pairs-9", "pairs-12"),
            ),
            practice.choicesOf(),
        )
        // A Unit of six pictures fills only the small board, and a size it cannot fill gives way.
        assertEquals(listOf("pairs-6"), practice.choicesOf(MemoryPairs.PICTURE_WORD, "u1", "pairs-12").getValue(MemoryPairs.SIZE))
        val round = practice.memory(MemoryPairs.PICTURE_WORD, "u2", "pairs-12")
        assertEquals(listOf(MemoryPairs.PICTURE_WORD, "u2", "pairs-6"), round.played)
        assertEquals(setOf("jeden", "dwa", "mama", "tata", "brat", "siostra"), round.asked.toSet())
        // As words, the first Unit has seven.
        assertEquals(24, practice.memory(MemoryPairs.WORD_MEANING, Decks.ALL, "pairs-12").tiles.size)
    }

    @Test
    fun `no two pairs of a board look alike`() {
        // "hej" means what "tak" does, and "Kot" is "kot" again: neither can share a board with it.
        val alike = spoken.copy(
            levels = listOf(levelOf("a1", dayOfSame("w1", "kot", "pies", "dom", "tak", "ryba", "ptak", "hej", "kot-2"))),
            items = spoken.items + item("hej", "hej", "tak in English") + item("kot-2", "Kot", "a tomcat"),
        )
        val practice = player(alike).completed("w1").practice()

        repeat(5) {
            assertEquals(setOf("kot", "pies", "dom", "tak", "ryba", "ptak"), practice.memory(MemoryPairs.WORD_MEANING).asked.toSet())
        }
        assertEquals(listOf("pairs-6"), practice.choicesOf(MemoryPairs.WORD_MEANING).getValue(MemoryPairs.SIZE))
    }

    @Test
    fun `turning up the two tiles of a pair finds it, and it stays up`() {
        val start = player().completed("w1", "w2").practice().memory()
        val (one, other) = start.pairs().getValue("kot")

        val first = start.turn(one)
        assertEquals(MemoryTileState.UP, first.stateOf(one))
        assertEquals(0, first.turns)

        val found = first.turn(other)
        assertEquals(setOf("kot"), found.found)
        assertEquals(listOf(MemoryTileState.FOUND, MemoryTileState.FOUND), listOf(found.stateOf(one), found.stateOf(other)))
        assertEquals(1, found.turns)
        assertEquals(1, found.done)
        // A found tile cannot be turned again.
        assertSame(found, found.turn(one))
    }

    @Test
    fun `two tiles that are not a pair count as a turn and turn back`() {
        val start = player().completed("w1", "w2").practice().memory()
        val (one, other) = start.miss()

        val missed = start.turn(one).turn(other)
        assertEquals(listOf(MemoryTileState.MISSED, MemoryTileState.MISSED), listOf(missed.stateOf(one), missed.stateOf(other)))
        assertEquals(1, missed.turns)
        assertTrue(missed.found.isEmpty())

        val settled = missed.settle()
        assertEquals(listOf(MemoryTileState.DOWN, MemoryTileState.DOWN), listOf(settled.stateOf(one), settled.stateOf(other)))
        assertEquals(1, settled.turns)
        // With nothing to turn back, nothing changes.
        assertSame(settled, settled.settle())
    }

    @Test
    fun `tapping a third tile turns the two that missed back at once`() {
        val start = player().completed("w1", "w2").practice().memory()
        val (one, other) = start.miss()
        val third = start.tiles.indices.first { it != one && it != other }

        val next = start.turn(one).turn(other).turn(third)

        assertEquals(
            listOf(MemoryTileState.DOWN, MemoryTileState.DOWN, MemoryTileState.UP),
            listOf(next.stateOf(one), next.stateOf(other), next.stateOf(third)),
        )
        assertEquals(1, next.turns)
    }

    @Test
    fun `tapping the tile that is already up, or no tile at all, changes nothing`() {
        val round = player().completed("w1", "w2").practice().memory().turn(0)

        assertSame(round, round.turn(0))
        assertSame(round, round.turn(-1))
        assertSame(round, round.turn(99))
    }

    @Test
    fun `the Round ends when every pair is found, and its result is the turns it took`() {
        val practice = player().completed("w1", "w2").practice()
        val round = practice.memory().playOut(misses = 3)

        assertTrue(round.finished)
        assertEquals(Score(9), round.score)
        assertEquals(
            RoundResult(Game.MEMORY_PAIRS, listOf(MemoryPairs.PICTURE_WORD, Decks.ALL, "pairs-6"), Score(9), best = 9, first = true),
            practice.finish(round),
        )
    }

    @Test
    fun `the Best is the fewest turns, kept for each way of playing`() {
        val practice = player().completed("w1", "w2").practice()
        practice.finish(practice.memory().playOut(misses = 3))

        val worse = practice.finish(practice.memory().playOut(misses = 5))
        assertEquals(9, worse.best)
        assertFalse(worse.newBest)

        val better = practice.finish(practice.memory().playOut(misses = 1))
        assertEquals(7, better.best)
        assertTrue(better.newBest)

        practice.finish(practice.memory(MemoryPairs.WORD_MEANING).playOut(misses = 4))
        assertEquals(7, practice.setup(Game.MEMORY_PAIRS, listOf(MemoryPairs.PICTURE_WORD)).best)
        assertEquals(10, practice.setup(Game.MEMORY_PAIRS, listOf(MemoryPairs.WORD_MEANING)).best)
        // The Practice screen shows the way it was last played.
        assertEquals(10, practice.view().games.single { it.game == Game.MEMORY_PAIRS }.best)
    }

    @Test
    fun `an unfinished board cannot be ended`() {
        val practice = player().completed("w1", "w2").practice()

        assertFailsWith<IllegalStateException> { practice.finish(practice.memory().findPair("kot")) }
    }

    @Test
    fun `a board of a Round just played is of other Items where there are enough`() {
        val practice = player().completed("w1", "w2", "w3", "w4", "w5", "w6").practice()

        val first = practice.memory().asked.toSet()
        val second = practice.memory().asked.toSet()

        // Twelve pictured Items, six to a board: the second board is the other six.
        assertTrue(first.intersect(second).isEmpty())
    }
}
