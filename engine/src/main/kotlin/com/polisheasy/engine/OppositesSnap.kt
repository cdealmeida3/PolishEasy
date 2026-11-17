package com.polisheasy.engine

import kotlin.random.Random

/**
 * One card of Opposites snap: two adjectives in the same form, [word] and [shown], and whether
 * they are [opposites]. [opposite] is what the opposite of [word] is, in that form, for the note
 * after a wrong answer; the meanings are each word's, without the forms the course lists after it.
 */
data class OppositesCard(
    val key: String,
    val word: String,
    val meaning: String,
    val shown: String,
    val shownMeaning: String,
    val opposite: String,
    val oppositeMeaning: String,
    val opposites: Boolean,
)

/**
 * A Round of Opposites snap, chapter 4's Game, on the Quick round's clock: for [seconds] seconds
 * two adjectives are shown side by side, in the same gender, and the Learner says whether they
 * are opposites, as *wesoły* and *smutny* are. A right answer brings the next pair at once; a wrong
 * one stays, with the word's real opposite, until the Learner moves on, while the clock runs. The
 * clock is the screen's: it calls [tick] once a second. The result is the right answers, and the
 * most is the Best.
 */
data class OppositesRound(
    override val played: List<String>,
    override val seconds: Int,
    val cards: List<OppositesCard>,
    val index: Int = 0,
    val right: Int = 0,
    /** How many seconds are left. */
    override val left: Int = seconds,
    /** Set while a wrong answer is being shown. */
    val missed: Boolean = false,
) : TimedRound {
    override val game: Game get() = Game.OPPOSITES_SNAP

    /** The clock, for the bar: the seconds gone of all there are. */
    override val done: Int get() = seconds - left
    override val total: Int get() = seconds
    override val finished: Boolean get() = left <= 0 || index >= cards.size

    /** The cards change in place: the whole Round is one step. */
    override val step: Int get() = 0
    override val score: Score get() = Score(right)
    override val asked: List<String> get() = cards.take(index).map { it.key }.distinct()

    /** The card being shown, or null once the Round is finished. */
    val card: OppositesCard? get() = if (finished) null else cards.getOrNull(index)

    /** Answers the card being shown: [opposites] is whether the Learner says the two are. No change while a wrong answer is shown. */
    fun answer(opposites: Boolean): OppositesRound {
        val card = card ?: return this
        if (missed) return this
        return if (opposites == card.opposites) copy(index = index + 1, right = right + 1) else copy(missed = true)
    }

    /** Moves on from a wrong answer. No change if none is being shown. */
    fun next(): OppositesRound = if (missed) copy(index = index + 1, missed = false) else this

    /** One second passes. At none left the Round is finished. */
    override fun tick(): OppositesRound = if (finished) this else copy(left = left - 1)
}

internal object OppositesSnap : GameRules {
    const val TIME = "time"
    const val SECONDS_PREFIX = "seconds-"
    val TIMES = listOf(60, 120)

    /** The fewest pairs of opposites the Game opens with. */
    const val LEAST = 3

    /** How many cards are dealt for each second: more than anyone answers. */
    private const val CARDS_A_SECOND = 2

    override val game = Game.OPPOSITES_SNAP

    /** The pairs of opposites whose two words are both learned, in the content's order. */
    private fun pairs(learned: Learned): List<Pair<Item, Item>> {
        val known = learned.items.associateBy { it.id }
        return learned.course.opposites.mapNotNull { (first, second) ->
            val one = known[first] ?: return@mapNotNull null
            val other = known[second] ?: return@mapNotNull null
            one to other
        }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (pairs(learned).size < LEAST) GameState.NEEDS_MORE else GameState.READY

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> =
        listOf(choice(TIME, TIMES.map { OptionView("$SECONDS_PREFIX$it") }, wanted.getOrNull(0)))

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val seconds = selected[0].removePrefix(SECONDS_PREFIX).toInt()
        val pairs = pairs(learned)
        val words = pairs.flatMap { listOf(it.first, it.second) }.distinctBy { it.id }
        /** Every word that is the opposite of [item], by any pair: never shown beside it as "not opposites". */
        fun oppositesOf(item: Item): Set<String> =
            pairs.flatMap { (one, other) -> listOfNotNull(other.id.takeIf { one.id == item.id }, one.id.takeIf { other.id == item.id }) }.toSet()
        fun key(pair: Pair<Item, Item>) = "${pair.first.id}|${pair.second.id}"

        // Every pair comes round before any comes again, those not seen today first.
        val order = mutableListOf<Pair<Item, Item>>()
        order += random.draw(pairs, pairs.size, seen, avoidFirst, ::key)
        while (order.size < seconds * CARDS_A_SECOND) {
            val again = pairs.shuffled(random)
            // The same pair is never shown twice running.
            order += if (key(again.first()) == key(order.last())) again.drop(1) + again.first() else again
        }
        val cards = order.take(seconds * CARDS_A_SECOND).map { pair ->
            // Either word of the pair may come first.
            val (word, opposite) = if (random.nextBoolean()) pair else pair.second to pair.first
            val others = words.filter { it.id != word.id && it.id !in oppositesOf(word) }
            val opposites = others.isEmpty() || random.nextBoolean()
            val shown = if (opposites) opposite else others.random(random)
            // All three in the same gender: the masculine, feminine or neuter, as each Item lists its forms.
            val form = random.nextInt(minOf(word.spellings.size, shown.spellings.size, opposite.spellings.size))
            OppositesCard(
                key(pair),
                word.spellings[form],
                plainMeaning(learned, word),
                shown.spellings[form],
                plainMeaning(learned, shown),
                opposite.spellings[form],
                plainMeaning(learned, opposite),
                opposites,
            )
        }
        return OppositesRound(selected, seconds, cards)
    }

    /** What [item] means, without the forms an adjective's meaning lists after it: "cheerful (wesoły, wesoła, wesołe)" is "cheerful". */
    private fun plainMeaning(learned: Learned, item: Item): String {
        val meaning = learned.localize(item.meaning)
        val open = meaning.lastIndexOf(" (")
        return if (open > 0 && meaning.endsWith(")") && item.text in meaning.substring(open)) meaning.substring(0, open) else meaning
    }
}
