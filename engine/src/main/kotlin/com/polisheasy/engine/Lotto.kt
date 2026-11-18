package com.polisheasy.engine

import kotlin.random.Random

/** One number called in Lotto: the [number], and the Polish [words] that say it. */
data class LottoQuestion(
    val number: Int,
    val words: List<String>,
) {
    /** The words as one line of Polish. */
    val polish: String get() = words.joinToString(" ")
}

/**
 * A Round of Lotto, chapter 7's Game: a card of numbers from a hundred to a thousand, and numbers
 * called one at a time, said or with [listen] off written in words. The Learner ticks each on the
 * [card], smallest first. [ticked] are the numbers called so far, once each is answered. The
 * result is the numbers ticked right first try.
 */
data class LottoRound(
    override val played: List<String>,
    val listen: Boolean,
    val card: List<Int>,
    val run: Run<LottoQuestion>,
    val ticked: Set<Int> = emptySet(),
) : Round {
    override val game: Game get() = Game.LOTTO
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { "${it.number}" }

    /** The number being called, or null once the Round is finished. */
    val question: LottoQuestion? get() = run.current

    /** The answer to the number being called, until the Learner moves on. Its text is the number ticked. */
    val given: Given? get() = run.given

    /** Ticks [number] on the card. No change if it is not on the card, is already ticked, or the call is answered. */
    fun tick(number: Int): LottoRound {
        val question = question ?: return this
        if (given != null || number !in card || number in ticked) return this
        return copy(run = run.answer("$number", number == question.number))
    }

    /** Moves on from an answered call; the number called stays ticked. */
    fun next(): LottoRound {
        val question = question ?: return this
        return if (given == null) this else copy(run = run.next(), ticked = ticked + question.number)
    }
}

/**
 * Lotto: once hundreds are learned, a card of numbers from a hundred to a thousand said in learned
 * words. Its levels: the round hundreds, *sto* to *tysiąc*; or any number up to a thousand, where
 * the numbers on the card that are not called are made to be mistaken for those that are.
 */
internal object Lotto : GameRules {
    const val CLUE = "number"
    const val LISTEN = "listen"
    const val READ = "read"
    const val LEVEL = "level"
    const val HUNDREDS = "hundreds"
    const val TO_1000 = "to-1000"

    /** How many numbers a card has, and how many of them are called. */
    const val CARD = 9
    const val CALLS = 5

    override val game = Game.LOTTO

    /** The numbers each level can call, with their words; heard, only those whose every word has a Clip. */
    private fun levels(learned: Learned, listen: Boolean): List<Pair<String, Map<Int, List<String>>>> {
        val numbers = learned.numbers()
        fun heard(words: List<String>) = !listen || words.all { learned.course.clipFor(it) != null }
        val said = (100..1000).mapNotNull { n -> wordsOf(n, numbers)?.takeIf(::heard)?.let { n to it } }.toMap()
        // The Game waits for the hundreds, a card of them; any number up to a thousand is a level once more can be said.
        val hundreds = said.filterKeys { it % 100 == 0 }
        if (hundreds.size < CARD) return emptyList()
        return listOf(HUNDREDS to hundreds, TO_1000 to if (said.size > hundreds.size) said else emptyMap())
            .filter { it.second.size >= CARD }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (levels(learned, listen = false).isEmpty()) GameState.NEEDS_MORE else GameState.READY

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val clues = listOf(LISTEN, READ).filter { it == READ || (sound && levels(learned, listen = true).isNotEmpty()) }
        val clue = choice(CLUE, clues.map { OptionView(it) }, wanted.getOrNull(0))
        val levels = levels(learned, listen = clue.selected == LISTEN)
        return listOf(clue, choice(LEVEL, levels.map { OptionView(it.first) }, wanted.getOrNull(1)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val listen = selected[0] == LISTEN
        val said = levels(learned, listen).first { it.first == selected[1] }.second
        val calls = random.draw(said.keys.toList(), CALLS, seen, avoidFirst) { "$it" }
        val card = calls.toMutableSet()
        // Each called number brings one that sounds like it: the same rest under another hundred, or the same hundred with another ten.
        calls.shuffled(random).forEach { called ->
            if (card.size >= CARD) return@forEach
            val alike = said.keys.filter { it !in card && (it % 100 == called % 100 || (it / 100 == called / 100 && it % 10 == called % 10)) }
            alike.randomOrNull(random)?.let { card += it }
        }
        said.keys.shuffled(random).forEach { if (card.size < CARD) card += it }
        val questions = calls.map { LottoQuestion(it, said.getValue(it)) }
        return LottoRound(selected, listen, card.sorted(), Run(questions))
    }
}
