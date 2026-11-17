package com.polisheasy.engine

import kotlin.random.Random

/** One picture of a Label the picture board: an Item's [picture], its [polish] and its [meaning]. */
data class LabelSpot(val itemId: String, val polish: String, val meaning: String, val picture: Picture)

/**
 * One picture of the board as it stands: [current] when it is the one asked, and [labelled] once
 * it is answered, [right] when right first try. A wrong answer still puts the right label on.
 */
data class LabelOnPicture(val spot: LabelSpot, val current: Boolean, val labelled: Boolean, val right: Boolean?)

/**
 * A Round of Label the picture, chapter 3's Game: a board of pictures, each with an empty label
 * under it. They are labelled one at a time in the board's order: the Learner taps the word for
 * the one asked among the [labels] still lying below, or, [typed], types it. The board is one
 * step, filled in place. The result is the labels right first try.
 */
data class LabelRound(
    override val played: List<String>,
    val typed: Boolean,
    val spots: List<LabelSpot>,
    /** The labels to choose from, mixed: each is the index of the spot it belongs to. */
    val labels: List<Int>,
    val run: Run<Int>,
) : Round {
    override val game: Game get() = Game.LABEL_IT
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = 0
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = spots.map { it.itemId }

    /** The picture being asked, or null once the board is labelled. */
    val spot: LabelSpot? get() = run.current?.let { spots[it] }

    /** The answer to the picture being asked, until the Learner moves on: the word tapped or typed. */
    val given: Given? get() = run.given

    /** Every picture of the board with its label so far, in the board's order. */
    val board: List<LabelOnPicture>
        get() = spots.mapIndexed { index, spot ->
            // The pictures are asked in the board's order, so the picture at [index] is the question at [index].
            val right = run.results.getOrNull(index) ?: run.given?.correct?.takeIf { index == run.index }
            LabelOnPicture(spot, current = run.current == index, labelled = right != null, right = right)
        }

    /** Whether the label at [label] is on its picture already, so it no longer lies below. */
    fun placed(label: Int): Boolean = board.getOrNull(labels.getOrNull(label) ?: -1)?.labelled == true

    /** Labels the picture asked with the label at [label]. No change if it is not one, is placed, or the picture is answered. */
    fun choose(label: Int): LabelRound {
        val asked = run.current ?: return this
        if (run.given != null || typed) return this
        val belongs = labels.getOrNull(label) ?: return this
        if (placed(label)) return this
        return copy(run = run.answer(spots[belongs].polish, belongs == asked))
    }

    /**
     * Labels the picture asked with what was [typed], checked as a typed answer is: case and
     * punctuation do not count, and a word right but for its Polish marks is right. No change
     * when it is blank, or the picture is answered.
     */
    fun type(typed: String): LabelRound {
        val asked = run.current ?: return this
        if (run.given != null || !this.typed || typed.isBlank()) return this
        return copy(run = run.answer(typed.trim(), checkTyped(typed, listOf(spots[asked].polish)).correct))
    }

    /** Moves on from an answered picture. */
    fun next(): LabelRound = copy(run = run.next())
}

internal object LabelIt : GameRules {
    const val WAY = "label"
    const val CHOOSE = "choose"
    const val TYPE = "type"
    const val DECK = "deck"

    /** How many pictures a board has, and so the fewest Items a deck can have. */
    const val SPOTS = 6

    override val game = Game.LABEL_IT

    /** The decks to play from, each of Items with an image that can be told apart by it and by their Polish. */
    private fun decks(learned: Learned): List<Deck> = learned
        .decks(SPOTS) { item -> learned.course.pictures[item.id] is Picture.Image }
        .map { Deck(it.id, it.label, learned.unalike(it.items, Alike.PICTURE)) }
        .filter { it.items.size >= SPOTS }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (decks(learned).isEmpty()) GameState.NEEDS_MORE else GameState.READY

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val decks = decks(learned)
        return listOf(
            choice(WAY, listOf(OptionView(CHOOSE), OptionView(TYPE)), wanted.getOrNull(0)),
            ChoiceView(DECK, decks.map { it.option }, deck(decks, wanted.getOrNull(1)).id),
        )
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val deck = deck(decks(learned), selected[1])
        val spots = random.draw(deck.items, SPOTS, seen, avoidFirst) { it.id }.map { item ->
            LabelSpot(item.id, item.text, learned.localize(item.meaning), learned.course.pictures.getValue(item.id))
        }
        val order = spots.indices.toList()
        // Mixed until the labels do not lie in the board's order.
        var labels = order.shuffled(random)
        var tries = 0
        while (labels == order && tries++ < 20) labels = order.shuffled(random)
        return LabelRound(selected, selected[0] == TYPE, spots, labels, Run(order))
    }
}
