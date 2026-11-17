package com.polisheasy.engine

import kotlin.random.Random

/**
 * A Round against the clock: [seconds] in all, [left] still to go. The clock is the screen's: it
 * calls [tick] once a second, and at none left the Round is finished.
 */
interface TimedRound : Round {
    val seconds: Int
    val left: Int

    /** One second passes. */
    fun tick(): TimedRound
}

/**
 * One card of the Quick round: a Polish word and, beside it, a meaning or a picture that is its
 * own or another word's. [matches] is whether it is its own; [meaning] is what the word does mean.
 */
data class QuickCard(
    val itemId: String,
    val polish: String,
    val meaning: String,
    val shownMeaning: String,
    val shownPicture: Picture?,
    val matches: Boolean,
)

/**
 * A Round of the Quick round, the one Game with a clock: for [seconds] seconds a word is shown
 * beside a meaning or a picture, and the Learner says whether they match. A right answer brings
 * the next card at once; a wrong one stays, with what the word means, until the Learner moves on,
 * while the clock runs. The clock is the screen's: it calls [tick] once a second. The result is
 * the right answers, and the most is the Best.
 */
data class QuickRound(
    override val played: List<String>,
    val pictures: Boolean,
    override val seconds: Int,
    val cards: List<QuickCard>,
    val index: Int = 0,
    val right: Int = 0,
    /** How many seconds are left. */
    override val left: Int = seconds,
    /** Set while a wrong answer is being shown. */
    val missed: Boolean = false,
) : TimedRound {
    override val game: Game get() = Game.QUICK_ROUND

    /** The clock, for the bar: the seconds gone of all there are. */
    override val done: Int get() = seconds - left
    override val total: Int get() = seconds
    override val finished: Boolean get() = left <= 0 || index >= cards.size

    /** The cards change in place: the whole Round is one step. */
    override val step: Int get() = 0
    override val score: Score get() = Score(right)
    override val asked: List<String> get() = cards.take(index).map { it.itemId }.distinct()

    /** The card being shown, or null once the Round is finished. */
    val card: QuickCard? get() = if (finished) null else cards.getOrNull(index)

    /** How many cards have been answered. */
    val answered: Int get() = index + if (missed) 1 else 0

    /** Answers the card being shown: [match] is whether the Learner says the two go together. No change while a wrong answer is shown. */
    fun answer(match: Boolean): QuickRound {
        val card = card ?: return this
        if (missed) return this
        return if (match == card.matches) copy(index = index + 1, right = right + 1) else copy(missed = true)
    }

    /** Moves on from a wrong answer. No change if none is being shown. */
    fun next(): QuickRound = if (missed) copy(index = index + 1, missed = false) else this

    /** One second passes. At none left the Round is finished. */
    override fun tick(): QuickRound = if (finished) this else copy(left = left - 1)
}

internal object QuickRoundRules : GameRules {
    const val SHOW = "match"
    const val PICTURE = "picture"
    const val MEANING = "meaning"
    const val TIME = "time"
    const val SECONDS_PREFIX = "seconds-"
    val TIMES = listOf(60, 120)

    /** The fewest words the Game opens with. */
    const val LEAST = 6

    /** How many cards are dealt for each second: more than anyone answers. */
    private const val CARDS_A_SECOND = 2

    override val game = Game.QUICK_ROUND

    /** The learned Items that can be told apart by what is shown beside them. */
    private fun items(learned: Learned, show: String): List<Item> {
        val pictures = show == PICTURE
        return learned.unalike(learned.items.filter { !pictures || it.id in learned.course.pictures }, if (pictures) Alike.PICTURE else Alike.MEANING)
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (items(learned, MEANING).size < LEAST) GameState.NEEDS_MORE else GameState.READY

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val shows = listOf(PICTURE, MEANING).filter { items(learned, it).size >= LEAST }
        return listOf(
            choice(SHOW, shows.map { OptionView(it) }, wanted.getOrNull(0)),
            choice(TIME, TIMES.map { OptionView("$SECONDS_PREFIX$it") }, wanted.getOrNull(1)),
        )
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val pictures = selected[0] == PICTURE
        val seconds = selected[1].removePrefix(SECONDS_PREFIX).toInt()
        val items = items(learned, selected[0])
        // Every word comes round before any comes again, those not seen today first.
        val order = mutableListOf<Item>()
        order += random.draw(items, items.size, seen, avoidFirst) { it.id }
        while (order.size < seconds * CARDS_A_SECOND) {
            val again = items.shuffled(random)
            // The same word is never shown twice running.
            order += if (again.first().id == order.last().id) again.drop(1) + again.first() else again
        }
        val cards = order.take(seconds * CARDS_A_SECOND).map { item ->
            val matches = random.nextBoolean()
            val shown = if (matches) item else items.filter { it.id != item.id }.random(random)
            QuickCard(
                item.id,
                item.text,
                learned.localize(item.meaning),
                learned.localize(shown.meaning),
                learned.course.pictures[shown.id].takeIf { pictures },
                matches,
            )
        }
        return QuickRound(selected, pictures, seconds, cards)
    }
}
