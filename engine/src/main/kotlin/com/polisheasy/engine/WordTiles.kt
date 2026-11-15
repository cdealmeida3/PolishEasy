package com.polisheasy.engine

import kotlin.random.Random

/**
 * The tiles of [exercise] in the order the Learner sees them: the words of its first Accepted
 * Answer and its distractors, mixed. The order depends only on the Exercise, so it is the same
 * every time the Exercise is shown, and it is never the answer followed by the distractors.
 */
internal fun tilesOf(exercise: WordTiles): List<String> {
    val authored = words(exercise.acceptedAnswers.first()) + exercise.distractors.flatMap(::words)
    val shuffled = authored.shuffled(Random(exercise.id.hashCode()))
    // Turning the list by one changes the order of any tiles that are not all the same word.
    return if (shuffled == authored) shuffled.drop(1) + shuffled.take(1) else shuffled
}

/**
 * Which tiles the Learner has moved from the bank into the answer, in the order they were placed.
 * A tile is named by its position in [tiles], so two tiles with the same word stay separate.
 */
data class TileSelection(val tiles: List<String>, val placed: List<Int> = emptyList()) {
    /** Moves [tile] to the end of the answer. No change if it is already there or is not a tile. */
    fun place(tile: Int): TileSelection =
        if (tile in tiles.indices && tile !in placed) copy(placed = placed + tile) else this

    /** Returns [tile] from the answer to its place in the bank. No change if it is not in the answer. */
    fun takeBack(tile: Int): TileSelection = if (tile in placed) copy(placed = placed - tile) else this

    fun isPlaced(tile: Int): Boolean = tile in placed

    /** The sentence built so far. */
    val answer: Answer.Tiles get() = Answer.Tiles(placed.map(tiles::get))
}
