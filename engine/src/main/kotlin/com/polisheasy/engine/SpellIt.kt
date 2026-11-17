package com.polisheasy.engine

import java.text.Normalizer
import kotlin.random.Random

/**
 * One word of Spell it: the Item's [polish], what it means, its [picture] when the Round shows
 * pictures, and its letters as [tiles], mixed.
 */
data class SpellWord(
    val itemId: String,
    val polish: String,
    val meaning: String,
    val picture: Picture?,
    val tiles: List<String>,
) {
    /** The letters of the word, in order. */
    val letters: List<String> get() = polish.map { it.toString() }
}

/**
 * A Round of Spell it: for each word its letters lie mixed as tiles, and the Learner taps them in
 * order. A right letter takes its place in the word; a wrong one stays where it is and is a slip.
 * The result is the words spelled with no slip.
 */
data class SpellRound(
    override val played: List<String>,
    val pictures: Boolean,
    val words: List<SpellWord>,
    val index: Int = 0,
    /** The tiles placed so far, in order, as places in the word's tiles. */
    val placed: List<Int> = emptyList(),
    /** How many wrong tiles have been tapped for the word being spelled. */
    val slips: Int = 0,
    /** The tile last tapped, when it was wrong. Cleared by the next right one. */
    val missed: Int? = null,
    /** For each word spelled so far, whether it was spelled with no slip. */
    val results: List<Boolean> = emptyList(),
) : Round {
    override val game: Game get() = Game.SPELL_IT
    override val done: Int get() = results.size
    override val total: Int get() = words.size
    override val finished: Boolean get() = index >= words.size
    override val step: Int get() = index
    override val score: Score get() = Score(results.count { it }, of = total)
    override val asked: List<String> get() = words.map { it.itemId }

    /** The word being spelled, or null once the Round is finished. */
    val word: SpellWord? get() = words.getOrNull(index)

    /** Whether the word is whole, waiting for the Learner to move on. */
    val complete: Boolean get() = word?.let { placed.size == it.tiles.size } ?: false

    /** The word once it is whole, as an answer: right if it was spelled with no slip. */
    val given: Given? get() = word?.takeIf { complete }?.let { Given(it.polish, slips == 0) }

    /**
     * Taps the tile at [tile]. It takes the next place in the word if it is the letter that
     * belongs there; otherwise it is a slip. No change if it is not a tile, is placed, or the
     * word is whole.
     */
    fun tap(tile: Int): SpellRound {
        val word = word ?: return this
        if (tile !in word.tiles.indices || tile in placed || complete) return this
        if (word.tiles[tile] != word.letters[placed.size]) return copy(slips = slips + 1, missed = tile)
        val whole = placed.size + 1 == word.tiles.size
        return copy(placed = placed + tile, missed = null, results = if (whole) results + (slips == 0) else results)
    }

    /** Moves on from a whole word. No change while it is not. */
    fun next(): SpellRound = if (complete) copy(index = index + 1, placed = emptyList(), slips = 0, missed = null) else this
}

internal object SpellIt : GameRules {
    const val FROM = "from"
    const val PICTURE = "picture"
    const val MEANING = "meaning"
    const val DECK = "deck"

    /** The fewest words a deck is offered with, and the most a Round has. */
    const val LEAST = 4
    const val WORDS = 6

    /** A word to spell has at least and at most this many letters. */
    const val SHORTEST = 3
    const val LONGEST = 9

    override val game = Game.SPELL_IT

    /** One word of small letters, long enough to be worth spelling and short enough for one row or two of tiles. */
    private fun spellable(item: Item): Boolean {
        val text = Normalizer.normalize(item.text, Normalizer.Form.NFC)
        return text.length in SHORTEST..LONGEST && text.all { it.isLetter() && it.isLowerCase() }
    }

    private fun decks(learned: Learned, from: String): List<Deck> {
        val pictures = from == PICTURE
        return learned
            .decks(LEAST) { item -> spellable(item) && (!pictures || item.id in learned.course.pictures) }
            .map { Deck(it.id, it.label, learned.unalike(it.items, if (pictures) Alike.PICTURE else Alike.MEANING)) }
            .filter { it.items.size >= LEAST }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (decks(learned, MEANING).isEmpty()) GameState.NEEDS_MORE else GameState.READY

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val ways = listOf(PICTURE, MEANING).filter { decks(learned, it).isNotEmpty() }
        val from = choice(FROM, ways.map { OptionView(it) }, wanted.getOrNull(0))
        val decks = decks(learned, from.selected)
        return listOf(from, ChoiceView(DECK, decks.map { it.option }, deck(decks, wanted.getOrNull(1)).id))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val pictures = selected[0] == PICTURE
        val deck = deck(decks(learned, selected[0]), selected[1])
        val words = random.draw(deck.items, WORDS, seen, avoidFirst) { it.id }.map { item ->
            val text = Normalizer.normalize(item.text, Normalizer.Form.NFC)
            val letters = text.map { it.toString() }
            // Mixed until the tiles do not already spell the word; a word of one letter repeated cannot be.
            var tiles = letters.shuffled(random)
            var tries = 0
            while (tiles == letters && tries++ < 20) tiles = letters.shuffled(random)
            SpellWord(item.id, text, learned.localize(item.meaning), learned.course.pictures[item.id].takeIf { pictures }, tiles)
        }
        return SpellRound(selected, pictures, words)
    }
}
