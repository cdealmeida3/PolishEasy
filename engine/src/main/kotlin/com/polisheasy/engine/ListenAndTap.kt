package com.polisheasy.engine

import kotlin.random.Random

/** One thing to tap in Listen and tap: an Item as its [picture], where it is shown as one, or as its [meaning]. */
data class ListenOption(val itemId: String, val picture: Picture?, val meaning: String)

/** One question of Listen and tap: [polish] is said, and the option at [answer] is the one it names. */
data class ListenQuestion(val polish: String, val options: List<ListenOption>, val answer: Int)

/**
 * A Round of Listen and tap: a word is said and the Learner taps what it names, among four. With
 * [pictures] the options are pictures, otherwise meanings. The result is the answers right first try.
 */
data class ListenRound(
    override val played: List<String>,
    val pictures: Boolean,
    val run: Run<ListenQuestion>,
) : Round {
    override val game: Game get() = Game.LISTEN_AND_TAP
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.options[it.answer].itemId }

    /** The question being shown, or null once the Round is finished. */
    val question: ListenQuestion? get() = run.current

    /** The answer to the question being shown, until the Learner moves on. Its text is the Item tapped. */
    val given: Given? get() = run.given

    /** Answers with the option at [option]. No change if it is not one, or the question is answered. */
    fun choose(option: Int): ListenRound {
        val question = question ?: return this
        val tapped = question.options.getOrNull(option) ?: return this
        return copy(run = run.answer(tapped.itemId, option == question.answer))
    }

    /** Moves on from an answered question. */
    fun next(): ListenRound = copy(run = run.next())
}

internal object ListenAndTap : GameRules {
    const val MODE = "tap"
    const val PICTURE = "picture"
    const val MEANING = "meaning"
    const val DECK = "deck"

    /** How many options a question has, and so the fewest Items a deck can have. */
    const val OPTIONS = 4

    /** How many words a Round says, at most. */
    const val QUESTIONS = 10

    override val game = Game.LISTEN_AND_TAP

    /** The decks to play from, each of Items that have a Clip and can be told apart as they are shown. */
    private fun decks(learned: Learned, mode: String): List<Deck> {
        val pictures = mode == PICTURE
        return learned
            .decks(OPTIONS) { item -> learned.course.clipFor(item.text) != null && (!pictures || item.id in learned.course.pictures) }
            .map { Deck(it.id, it.label, learned.unalike(it.items, if (pictures) Alike.PICTURE else Alike.MEANING)) }
            .filter { it.items.size >= OPTIONS }
    }

    override fun state(learned: Learned, sound: Boolean): GameState = when {
        decks(learned, MEANING).isEmpty() -> GameState.NEEDS_MORE
        !sound -> GameState.NEEDS_SOUND
        else -> GameState.READY
    }

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val modes = listOf(PICTURE, MEANING).filter { decks(learned, it).isNotEmpty() }
        val mode = choice(MODE, modes.map { OptionView(it) }, wanted.getOrNull(0))
        val decks = decks(learned, mode.selected)
        return listOf(mode, ChoiceView(DECK, decks.map { it.option }, deck(decks, wanted.getOrNull(1)).id))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val (mode, deckId) = selected
        val pictures = mode == PICTURE
        val deck = deck(decks(learned, mode), deckId)
        fun option(item: Item) = ListenOption(item.id, learned.course.pictures[item.id].takeIf { pictures }, learned.localize(item.meaning))
        val questions = random.draw(deck.items, QUESTIONS, seen, avoidFirst) { it.id }.map { item ->
            val others = deck.items.filter { it.id != item.id }.shuffled(random).take(OPTIONS - 1)
            val options = (others + item).shuffled(random)
            ListenQuestion(item.text, options.map(::option), options.indexOf(item))
        }
        return ListenRound(selected, pictures, Run(questions))
    }
}
