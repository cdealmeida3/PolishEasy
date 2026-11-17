package com.polisheasy.engine

import kotlin.random.Random

/** What a tile of Memory pairs shows when it is turned up. The two tiles of a pair show two different things. */
sealed interface MemoryFace {
    /** The Item's picture. [meaning] is what a screen reader says for it. */
    data class Image(val picture: Picture, val meaning: String) : MemoryFace

    /** The Polish, which is said as the tile turns. */
    data class Word(val polish: String) : MemoryFace

    /** What the Item means, in the Base Language. */
    data class Meaning(val text: String) : MemoryFace

    /** Nothing to read: [polish] is said as the tile turns, and again when it is tapped. */
    data class Sound(val polish: String) : MemoryFace
}

/** One tile of the board. Two tiles with the same [pair] belong together. */
data class MemoryTile(val pair: String, val face: MemoryFace)

/** How a tile of the board lies. */
enum class MemoryTileState {
    DOWN,

    /** Turned up, waiting for a second tile. */
    UP,

    /** One of two tiles turned up that are not a pair. They turn back. */
    MISSED,

    /** One of a pair that was found. It stays up. */
    FOUND,
}

/**
 * A Round of Memory pairs: tiles face down, two to each learned Item. Two are turned up at a
 * time. A pair stays up; two that are not a pair turn back, at once if a third tile is tapped. The
 * result is the turns taken, and the fewest is the Best.
 */
data class MemoryRound(
    override val played: List<String>,
    val tiles: List<MemoryTile>,
    /** The tiles turned up that are not a found pair: none, one, or two that missed. */
    val up: List<Int> = emptyList(),
    /** The pairs found so far. */
    val found: Set<String> = emptySet(),
    /** How many times a second tile has been turned up. */
    val turns: Int = 0,
) : Round {
    override val game: Game get() = Game.MEMORY_PAIRS
    override val done: Int get() = found.size
    override val total: Int get() = tiles.size / 2
    override val finished: Boolean get() = found.size == total

    /** The board never gives way to another: it is one step from start to end. */
    override val step: Int get() = 0
    override val score: Score get() = Score(turns)
    override val asked: List<String> get() = tiles.map { it.pair }.distinct()

    fun stateOf(tile: Int): MemoryTileState = when {
        tiles[tile].pair in found -> MemoryTileState.FOUND
        tile !in up -> MemoryTileState.DOWN
        up.size == 2 -> MemoryTileState.MISSED
        else -> MemoryTileState.UP
    }

    /**
     * Turns up the tile at [tile]. No change if it is not a tile, is already up alone, or belongs
     * to a found pair. When two that missed are up, they turn back first.
     */
    fun turn(tile: Int): MemoryRound {
        if (tile !in tiles.indices || tiles[tile].pair in found) return this
        return when (up.size) {
            0, 2 -> copy(up = listOf(tile))
            else -> {
                val first = up.single()
                when {
                    first == tile -> this
                    tiles[first].pair == tiles[tile].pair -> copy(up = emptyList(), found = found + tiles[tile].pair, turns = turns + 1)
                    else -> copy(up = listOf(first, tile), turns = turns + 1)
                }
            }
        }
    }

    /** Turns back two tiles that missed. No change otherwise. */
    fun settle(): MemoryRound = if (up.size == 2) copy(up = emptyList()) else this
}

internal object MemoryPairs : GameRules {
    const val KIND = "pairs"
    const val PICTURE_WORD = "picture-word"
    const val WORD_MEANING = "word-meaning"
    const val SOUND_WORD = "sound-word"
    const val DECK = "deck"
    const val SIZE = "size"

    /** The sizes of a board, in pairs. An option's id is "pairs-" and the number. */
    val SIZES = listOf(6, 9, 12)
    const val SIZE_PREFIX = "pairs-"

    override val game = Game.MEMORY_PAIRS

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (learned.distinct(learned.items, WORD_MEANING).size < SIZES.first()) GameState.NEEDS_MORE else GameState.READY

    /** The Items a kind of pair can be made of. */
    private fun usable(learned: Learned, kind: String): (Item) -> Boolean = when (kind) {
        PICTURE_WORD -> { item -> item.id in learned.course.pictures }
        SOUND_WORD -> { item -> learned.course.clipFor(item.text) != null }
        else -> { _ -> true }
    }

    /**
     * [items] less any whose tiles would look or sound like those of one before it: the same
     * Polish, the same meaning, or the same picture. On a board, every tile has one partner.
     */
    private fun Learned.distinct(items: List<Item>, kind: String): List<Item> {
        val words = items.distinctBy { audioKey(it.text) }
        return when (kind) {
            PICTURE_WORD -> words.distinctBy { course.pictures[it.id] }
            SOUND_WORD -> words
            else -> words.distinctBy { localize(it.meaning) }
        }
    }

    /** The decks a board of [kind] can be laid from, each with the Items that can share a board. */
    private fun decks(learned: Learned, kind: String): List<Deck> =
        learned.decks(SIZES.first(), usable(learned, kind))
            .map { Deck(it.id, it.label, learned.distinct(it.items, kind)) }
            .filter { it.items.size >= SIZES.first() }

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        // A kind is offered when there is enough to make a board of it; pairs with a sound, only when sound is on.
        val kinds = listOf(PICTURE_WORD, WORD_MEANING, SOUND_WORD).filter { kind ->
            (kind != SOUND_WORD || sound) && decks(learned, kind).isNotEmpty()
        }
        val kind = choice(KIND, kinds.map { OptionView(it) }, wanted.getOrNull(0))
        val decks = decks(learned, kind.selected)
        val deck = deck(decks, wanted.getOrNull(1))
        val sizes = SIZES.filter { it <= deck.items.size }.map { OptionView("$SIZE_PREFIX$it") }
        return listOf(kind, ChoiceView(DECK, decks.map { it.option }, deck.id), choice(SIZE, sizes, wanted.getOrNull(2)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val (kind, deckId, size) = selected
        val deck = deck(decks(learned, kind), deckId)
        val pairs = random.draw(deck.items, size.removePrefix(SIZE_PREFIX).toInt(), seen, null) { it.id }
        val tiles = pairs.flatMap { item ->
            val meaning = learned.localize(item.meaning)
            val (one, other) = when (kind) {
                PICTURE_WORD -> MemoryFace.Image(learned.course.pictures.getValue(item.id), meaning) to MemoryFace.Word(item.text)
                SOUND_WORD -> MemoryFace.Sound(item.text) to MemoryFace.Word(item.text)
                else -> MemoryFace.Word(item.text) to MemoryFace.Meaning(meaning)
            }
            listOf(MemoryTile(item.id, one), MemoryTile(item.id, other))
        }
        return MemoryRound(selected, tiles.shuffled(random))
    }
}
