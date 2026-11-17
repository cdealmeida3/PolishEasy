package com.polisheasy.engine

import kotlin.random.Random

/**
 * One sentence of Phrase chains. [polish] is the sentence as it is heard, [meaning] what it means,
 * and [cards] its words and a few that do not belong, mixed.
 */
data class ChainSentence(
    val id: String,
    val polish: String,
    val meaning: String,
    val cards: List<String>,
    /** Every sentence the cards may be chained into. */
    internal val accepted: List<String>,
)

/**
 * A Round of Phrase chains: for each sentence its words lie mixed as cards, and the Learner taps
 * them in order. A right card joins the chain; a wrong one stays where it is and counts as a slip.
 * With [listen] the sentence is heard and only its own order is right; otherwise its meaning is
 * read, and every Accepted Answer is. The result is the slips, and the fewest is the Best.
 */
data class ChainRound(
    override val played: List<String>,
    val listen: Boolean,
    val sentences: List<ChainSentence>,
    val index: Int = 0,
    /** The cards chained so far, in order, as places in the sentence's cards. */
    val chain: List<Int> = emptyList(),
    val slips: Int = 0,
    /** The card last tapped, when it was wrong. Cleared by the next right one. */
    val missed: Int? = null,
) : Round {
    override val game: Game get() = Game.PHRASE_CHAINS
    override val done: Int get() = index + if (complete) 1 else 0
    override val total: Int get() = sentences.size
    override val finished: Boolean get() = index >= sentences.size
    override val step: Int get() = index
    override val score: Score get() = Score(slips)
    override val asked: List<String> get() = sentences.map { it.id }

    /** The sentence being chained, or null once the Round is finished. */
    val sentence: ChainSentence? get() = sentences.getOrNull(index)

    /** The Accepted Answer the chain has become, once it is whole. */
    val said: String?
        get() {
            val sentence = sentence ?: return null
            val built = chain.map { normalizeAnswer(sentence.cards[it]) }
            return sentence.accepted.firstOrNull { chainWords(it) == built }
        }

    /** Whether the chain is a whole sentence, waiting for the Learner to move on. */
    val complete: Boolean get() = said != null

    /**
     * Taps the card at [card]. It joins the chain if the chain with it starts a sentence the cards
     * can make; otherwise it is a slip. No change if it is not a card, is in the chain, or the
     * chain is whole.
     */
    fun tap(card: Int): ChainRound {
        val sentence = sentence ?: return this
        if (card !in sentence.cards.indices || card in chain || complete) return this
        val built = (chain + card).map { normalizeAnswer(sentence.cards[it]) }
        val right = sentence.accepted.any { chainWords(it).take(built.size) == built }
        return if (right) copy(chain = chain + card, missed = null) else copy(slips = slips + 1, missed = card)
    }

    /** Moves on from a whole chain. No change while it is not. */
    fun next(): ChainRound = if (complete) copy(index = index + 1, chain = emptyList(), missed = null) else this
}

/** The words of a sentence as they are compared: lower case, no punctuation. */
private fun chainWords(sentence: String): List<String> = words(sentence).map(::normalizeAnswer)

internal object PhraseChains : GameRules {
    const val CLUE = "clue"
    const val LISTEN = "listen"
    const val READ = "read"
    const val LEVEL = "length"
    const val SHORT = "short"
    const val MEDIUM = "medium"
    const val LONG = "long"
    const val MIXED = "mixed"

    /** The fewest sentences a level is offered with, and the most a Round has. */
    const val LEAST = 3
    const val SENTENCES = 5

    /** A short sentence has at most this many words, a medium one at most [MEDIUM_WORDS]. */
    const val SHORT_WORDS = 3
    const val MEDIUM_WORDS = 5

    override val game = Game.PHRASE_CHAINS

    /** The sentences of completed word tile Exercises, once each, of two words or more. */
    private fun sentences(learned: Learned): List<WordTiles> =
        learned.dailySets.flatMap { it.exercises }.filterIsInstance<WordTiles>()
            .filter { words(it.acceptedAnswers.first()).size > 1 }
            .distinctBy { normalizeAnswer(it.acceptedAnswers.first()) }

    private fun levelOf(sentence: WordTiles): String = when (words(sentence.acceptedAnswers.first()).size) {
        in 0..SHORT_WORDS -> SHORT
        in SHORT_WORDS + 1..MEDIUM_WORDS -> MEDIUM
        else -> LONG
    }

    /** The sentences of each level that has enough, and all of them as "mixed" when more than one has, or none. */
    private fun levels(learned: Learned, listen: Boolean): List<Pair<String, List<WordTiles>>> {
        val all = sentences(learned).filter { !listen || learned.course.clipFor(it.acceptedAnswers.first()) != null }
        if (all.size < LEAST) return emptyList()
        val levels = listOf(SHORT, MEDIUM, LONG).map { level -> level to all.filter { levelOf(it) == level } }.filter { it.second.size >= LEAST }
        return if (levels.size == 1 && levels.single().second.size == all.size) levels else levels + (MIXED to all)
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (levels(learned, listen = false).isEmpty()) GameState.NEEDS_MORE else GameState.READY

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        // Hearing the sentence is offered with sound on, where enough sentences have a Clip.
        val clues = listOf(LISTEN, READ).filter { it == READ || (sound && levels(learned, listen = true).isNotEmpty()) }
        val clue = choice(CLUE, clues.map { OptionView(it) }, wanted.getOrNull(0))
        val levels = levels(learned, listen = clue.selected == LISTEN)
        return listOf(clue, choice(LEVEL, levels.map { OptionView(it.first) }, wanted.getOrNull(1)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val listen = selected[0] == LISTEN
        val level = levels(learned, listen).first { it.first == selected[1] }.second
        val sentences = random.draw(level, SENTENCES, seen, avoidFirst) { it.id }.map { asked ->
            val first = asked.acceptedAnswers.first()
            val cards = (words(first) + asked.distractors).shuffled(random)
            val have = cards.map(::normalizeAnswer).groupingBy { it }.eachCount()
            // Heard, the sentence has one order. Read, it has every accepted one the cards can make.
            val accepted = if (listen) listOf(first) else asked.acceptedAnswers.filter { answer ->
                chainWords(answer).groupingBy { it }.eachCount().all { (word, count) -> have.getOrDefault(word, 0) >= count }
            }
            ChainSentence(asked.id, first, learned.localize(asked.prompt), cards, accepted)
        }
        return ChainRound(selected, listen, sentences)
    }
}
