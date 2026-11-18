package com.polisheasy.engine

import kotlin.random.Random

/**
 * One sentence of Negation flip: [yes] (*Lubię kawę.*) and its negation [no] (*Nie lubię kawy.*),
 * each with what it means, and the [items] both use, which must be learned for it to be played.
 */
data class Negation(val yes: String, val yesMeaning: Localized, val no: String, val noMeaning: Localized, val items: List<String>) {
    /**
     * The negation that keeps the object as it was, *Nie lubię kawę.*, *Ewa nie pije mleko.*: the
     * wrong option. After *nie* the object goes into the genitive, so this is never right.
     */
    val kept: String get() {
        val at = negatedAt
        return if (at <= 0) "Nie " + yes.replaceFirstChar { it.lowercase() } else yes.substring(0, at) + "nie " + yes.substring(at)
    }

    /** Where *nie* stands in [no]: what comes before it is the same in [yes] (*Ewa nie pije mleka.*), or -1. */
    val negatedAt: Int get() = Regex("(^|\\s)nie\\s", RegexOption.IGNORE_CASE).find(no)?.let { if (it.value.first().isWhitespace()) it.range.first + 1 else 0 }
        ?.takeIf { yes.startsWith(no.substring(0, it)) } ?: -1
}

/** One card of Negation flip: the sentence [yes], the two [options] to negate it, of which [right] is the one, and what that one means. */
data class FlipCard(val key: String, val yes: String, val options: List<String>, val right: Int, val meaning: String) {
    val no: String get() = options[right]
}

/**
 * A Round of Negation flip, chapter 10's Game, on the Quick round's clock: for [seconds] seconds
 * a sentence is shown, *Lubię kawę.*, and the Learner picks its negation from two: *Nie lubię
 * kawy.* or *Nie lubię kawę.* A right answer brings the next sentence at once; a wrong one stays,
 * with the right one, until the Learner moves on, while the clock runs. The clock is the screen's:
 * it calls [tick] once a second. The result is the right answers, and the most is the Best.
 */
data class FlipRound(
    override val played: List<String>,
    override val seconds: Int,
    val cards: List<FlipCard>,
    val index: Int = 0,
    val right: Int = 0,
    override val left: Int = seconds,
    /** The option picked for the card being shown, set while that answer is wrong. */
    val missed: Int? = null,
) : TimedRound {
    override val game: Game get() = Game.NEGATION_FLIP
    override val done: Int get() = seconds - left
    override val total: Int get() = seconds
    override val finished: Boolean get() = left <= 0 || index >= cards.size

    /** The cards change in place: the whole Round is one step. */
    override val step: Int get() = 0
    override val score: Score get() = Score(right)
    override val asked: List<String> get() = cards.take(index).map { it.key }.distinct()

    /** The card being shown, or null once the Round is finished. */
    val card: FlipCard? get() = if (finished) null else cards.getOrNull(index)

    /** Picks the option [option] of the card being shown. No change while a wrong answer is shown, or for no such option. */
    fun answer(option: Int): FlipRound {
        val card = card ?: return this
        if (missed != null || option !in card.options.indices) return this
        return if (option == card.right) copy(index = index + 1, right = right + 1) else copy(missed = option)
    }

    /** Moves on from a wrong answer. No change if none is being shown. */
    fun next(): FlipRound = if (missed != null) copy(index = index + 1, missed = null) else this

    /** One second passes. At none left the Round is finished. */
    override fun tick(): FlipRound = if (finished) this else copy(left = left - 1)
}

/**
 * Negation flip: once six of the content's `negations` have all their Items learned. A Round deals
 * them, each once before any comes again and never one twice running, with the right negation and
 * the one that keeps the object, in either order.
 */
internal object NegationFlip : GameRules {
    const val TIME = "time"
    const val SECONDS_PREFIX = "seconds-"
    val TIMES = listOf(60, 120)

    /** The fewest sentences the Game opens with. */
    const val LEAST = 6

    /** How many cards are dealt for each second: more than anyone answers. */
    private const val CARDS_A_SECOND = 2

    override val game = Game.NEGATION_FLIP

    private fun negations(learned: Learned): List<Negation> {
        val known = learned.items.map { it.id }.toSet()
        return learned.course.negations.filter { known.containsAll(it.items) }.distinctBy { it.yes }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (negations(learned).size >= LEAST) GameState.READY else GameState.NEEDS_MORE

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> =
        listOf(choice(TIME, TIMES.map { OptionView("$SECONDS_PREFIX$it") }, wanted.getOrNull(0)))

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val seconds = selected[0].removePrefix(SECONDS_PREFIX).toInt()
        val all = negations(learned)
        val order = mutableListOf<Negation>()
        order += random.draw(all, all.size, seen, avoidFirst) { it.yes }
        while (order.size < seconds * CARDS_A_SECOND) {
            val again = all.shuffled(random)
            order += if (again.first().yes == order.last().yes) again.drop(1) + again.first() else again
        }
        val cards = order.take(seconds * CARDS_A_SECOND).map { negation ->
            val right = random.nextInt(2)
            val options = if (right == 0) listOf(negation.no, negation.kept) else listOf(negation.kept, negation.no)
            FlipCard(negation.yes, negation.yes, options, right, learned.localize(negation.noMeaning))
        }
        return FlipRound(selected, seconds, cards)
    }
}
