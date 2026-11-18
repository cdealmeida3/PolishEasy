package com.polisheasy.engine

import kotlin.random.Random

/** A square of a Word search grid, by its [row] and [column], counted from 0 at the top left. */
data class Cell(val row: Int, val column: Int)

/**
 * A word hidden in a Word search grid: its Item [id], its [word] as the Item writes it, the
 * [clue] it is found by (what it means), and where its [letters] are, from its first to its last.
 */
data class HiddenWord(val id: String, val word: String, val clue: String, val letters: List<Cell>)

/**
 * A Round of Word search, chapter 16's Game: a grid of letters hides learned words, across from
 * left to right or down from top to bottom, and beside it is what each means. The Learner taps a
 * word's first letter and then its last, either way round, to mark it. [first] is the letter
 * tapped first, waiting for its last. A word counts right first try when it is marked with no
 * wrong mark since the word before; one the Learner asks to be shown counts as missed. The
 * Round is finished once every word is found.
 */
data class WordSearchRound(
    override val played: List<String>,
    /** The grid's letters, row by row, in capitals. */
    val grid: List<List<Char>>,
    val words: List<HiddenWord>,
    /** The words found so far, by Item id, in the order they were found. */
    val found: List<String> = emptyList(),
    /** The words shown rather than found, by Item id. */
    val shown: Set<String> = emptySet(),
    val first: Cell? = null,
    /** The last wrong mark, from its first letter to its last, until the next tap. */
    val wrong: List<Cell>? = null,
    /** Whether a wrong mark was made since the last word was found. */
    private val slipped: Boolean = false,
    private val firstTry: Int = 0,
) : Round {
    override val game: Game get() = Game.WORD_SEARCH
    override val done: Int get() = found.size
    override val total: Int get() = words.size
    override val finished: Boolean get() = found.size == words.size
    override val step: Int get() = 0
    override val score: Score get() = Score(firstTry, of = total)
    override val asked: List<String> get() = words.map { it.id }

    /** The word found last, or null before the first. */
    val last: HiddenWord? get() = found.lastOrNull()?.let { id -> words.single { it.id == id } }

    /** Taps the letter at [cell]: the first of a mark, or its last. Tapping the first again lets it go. */
    fun tap(cell: Cell): WordSearchRound {
        if (finished || cell.row !in grid.indices || cell.column !in grid[cell.row].indices) return this
        val start = first ?: return copy(first = cell, wrong = null)
        if (start == cell) return copy(first = null)
        val line = line(start, cell) ?: return copy(first = null, wrong = listOf(start, cell), slipped = true)
        // A word spelled by the squares counts wherever it is, even where the filling letters spell it by chance.
        val spelled = line.map { grid[it.row][it.column] }.joinToString("")
        val word = words.firstOrNull { it.id !in found && (it.word.uppercase() == spelled || it.word.uppercase() == spelled.reversed()) }
            ?: return copy(first = null, wrong = line, slipped = true)
        val letters = if (word.word.uppercase() == spelled) line else line.reversed()
        return copy(
            words = words.map { if (it.id == word.id) it.copy(letters = letters) else it },
            found = found + word.id,
            first = null,
            wrong = null,
            slipped = false,
            firstTry = if (slipped) firstTry else firstTry + 1,
        )
    }

    /** Shows a word not yet found, counted as missed. */
    fun show(): WordSearchRound {
        val word = words.firstOrNull { it.id !in found } ?: return this
        return copy(found = found + word.id, shown = shown + word.id, first = null, wrong = null, slipped = false)
    }

    /** The squares from [from] to [to] when they are in one row or one column, else null. */
    private fun line(from: Cell, to: Cell): List<Cell>? = when {
        from.row == to.row -> (if (from.column < to.column) from.column..to.column else from.column downTo to.column).map { Cell(from.row, it) }
        from.column == to.column -> (if (from.row < to.row) from.row..to.row else from.row downTo to.row).map { Cell(it, from.column) }
        else -> null
    }
}

/**
 * Word search: the Items of `wordSearch` in practice.json, each one word. It opens once [WORDS] of
 * them are learned. A Round hides [WORDS] of them in a grid as wide as the longest and at least
 * [SMALLEST] letters, the rest of it filled with Polish letters.
 */
internal object WordSearch : GameRules {
    const val WORDS = 6
    const val SMALLEST = 8

    /** The longest word a grid can hide, so twelve letters still fit across a phone. */
    const val LONGEST = 12

    /** The letters that fill the grid where no word is. */
    private const val ALPHABET = "AĄBCĆDEĘFGHIJKLŁMNŃOÓPRSŚTUWYZŹŻ"

    override val game = Game.WORD_SEARCH

    private fun words(learned: Learned): List<Item> {
        val known = learned.items.associateBy { it.id }
        return learned.course.wordSearch.mapNotNull(known::get).filter { it.text.all(Char::isLetter) && it.text.length <= LONGEST }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (words(learned).size >= WORDS) GameState.READY else GameState.NEEDS_MORE

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val items = random.draw(words(learned), WORDS, seen, avoidFirst) { it.id }.sortedByDescending { it.text.length }
        val size = maxOf(SMALLEST, items.maxOf { it.text.length })
        // Placing the longest first leaves room for the rest; a grid that does not take them all is tried again.
        repeat(ATTEMPTS) {
            val grid = MutableList(size) { MutableList<Char?>(size) { null } }
            val placed = items.map { item -> place(grid, item.text.uppercase(), random) ?: return@repeat }
            val words = items.zip(placed).map { (item, letters) -> HiddenWord(item.id, item.text, clue(learned, item), letters) }
            val letters = grid.map { row -> row.map { it ?: ALPHABET[random.nextInt(ALPHABET.length)] } }
            return WordSearchRound(selected, letters, words.sortedBy { it.letters.first().row * size + it.letters.first().column })
        }
        error("Word search could not place ${items.map { it.text }} in $size by $size")
    }

    /** What a word is found by: its meaning, without what it adds in brackets. */
    private fun clue(learned: Learned, item: Item): String = learned.localize(item.meaning).substringBefore(" (").trim()

    /** Writes [word] into [grid] across or down where it fits, sharing only letters that agree, and gives its squares; null if it fits nowhere. */
    private fun place(grid: MutableList<MutableList<Char?>>, word: String, random: Random): List<Cell>? {
        val size = grid.size
        val spots = listOf(false, true).flatMap { down ->
            (0 until size).flatMap { row -> (0 until size).map { column -> Triple(row, column, down) } }
        }.filter { (row, column, down) -> (if (down) row else column) + word.length <= size }.shuffled(random)
        spots.forEach { (row, column, down) ->
            val cells = word.indices.map { if (down) Cell(row + it, column) else Cell(row, column + it) }
            if (cells.withIndex().all { (index, cell) -> grid[cell.row][cell.column].let { it == null || it == word[index] } }) {
                cells.forEachIndexed { index, cell -> grid[cell.row][cell.column] = word[index] }
                return cells
            }
        }
        return null
    }

    private const val ATTEMPTS = 50
}
