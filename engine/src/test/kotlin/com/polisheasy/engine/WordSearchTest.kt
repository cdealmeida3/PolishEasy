package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WordSearchTest {
    private val words = listOf(
        item("styczen", "styczeń", "January (w styczniu: in January)"), item("luty", "luty", "February"), item("marzec", "marzec", "March"),
        item("kwiecien", "kwiecień", "April"), item("maj", "maj", "May"), item("pazdziernik", "październik", "October"), item("zima", "zima", "winter"),
    )

    /** l1 teaches five words, l2 the other two. */
    private val course = Course(
        listOf(levelOf("a1", day("l1", *words.take(5).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()), day("l2", *words.drop(5).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()))),
        words,
        wordSearch = words.map { it.id },
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun Practice.round(): WordSearchRound = start(setup(Game.WORD_SEARCH, emptyList())) as WordSearchRound

    private fun WordSearchRound.mark(word: HiddenWord): WordSearchRound = tap(word.letters.first()).tap(word.letters.last())

    @Test
    fun `Word search opens once six of its words are learned`() {
        val state = { p: Player -> p.practice().view().games.single { it.game == Game.WORD_SEARCH }.state }
        assertEquals(GameState.NEEDS_MORE, state(player().completed("l1")))
        assertEquals(GameState.READY, state(player().completed("l1", "l2")))
    }

    @Test
    fun `every word is in the grid, across or down, the grid as wide as the longest word, and its clue is its meaning without brackets`() {
        repeat(20) { seed ->
            val round = player(seed).completed("l1", "l2").practice().round()
            assertEquals(WordSearch.WORDS, round.total)
            val size = maxOf(WordSearch.SMALLEST, round.words.maxOf { it.word.length })
            assertEquals(size, round.grid.size)
            round.grid.forEach { assertEquals(size, it.size) }
            round.words.forEach { word ->
                assertEquals(word.word.uppercase(), word.letters.joinToString("") { round.grid[it.row][it.column].toString() })
                val rows = word.letters.map { it.row }.distinct()
                val columns = word.letters.map { it.column }.distinct()
                assertTrue(rows.size == 1 || columns.size == 1, word.word)
            }
            assertTrue(round.words.none { '(' in it.clue })
        }
        val round = player().completed("l1", "l2").practice().round()
        round.words.singleOrNull { it.id == "styczen" }?.let { assertEquals("January", it.clue) }
    }

    @Test
    fun `a word is found from its first letter to its last or back, and the score counts words found with no wrong mark before them`() {
        var round = player().completed("l1", "l2").practice().round()
        val (first, second) = round.words
        round = round.mark(first)
        assertEquals(listOf(first.id), round.found)
        // Back to front counts too.
        round = round.tap(second.letters.last()).tap(second.letters.first())
        assertEquals(listOf(first.id, second.id), round.found)
        // A mark that is not in one line, or spells no word, is wrong.
        val third = round.words[2]
        val corner = Cell(0, 0)
        val diagonal = Cell(1, 1)
        round = round.tap(corner).tap(diagonal)
        assertEquals(listOf(corner, diagonal), round.wrong)
        round = round.mark(third)
        assertEquals(null, round.wrong)
        round.words.filter { it.id !in round.found }.drop(1).forEach { round = round.mark(it) }
        // The last one is shown, not found.
        round = round.show()
        assertTrue(round.finished)
        assertEquals(Score(WordSearch.WORDS - 2, of = WordSearch.WORDS), round.score)
    }

    @Test
    fun `tapping the first letter again lets it go`() {
        var round = player().completed("l1", "l2").practice().round()
        round = round.tap(Cell(2, 3))
        assertEquals(Cell(2, 3), round.first)
        round = round.tap(Cell(2, 3))
        assertEquals(null, round.first)
        assertEquals(null, round.wrong)
    }
}
