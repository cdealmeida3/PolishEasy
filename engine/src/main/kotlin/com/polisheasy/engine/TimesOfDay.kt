package com.polisheasy.engine

import kotlin.random.Random

/**
 * One time of day of Times of day: the Item that says it (*rano*), the [hours] of the clock it
 * covers, 0 to 23, and whether it is only [onTheHour] (*w południe* is 12:00, not 12:30).
 */
data class TimeOfDay(val item: String, val hours: List<Int>, val onTheHour: Boolean = false)

/** One card of Times of day: a time on the clock, [hour] and [minute], and the [options] to say when it is, of which [right] is the one, with what it means. */
data class ClockCard(val key: String, val hour: Int, val minute: Int, val options: List<String>, val right: Int, val meaning: String) {
    val answer: String get() = options[right]
}

/**
 * A Round of Times of day, chapter 11's second Game, on the Quick round's clock: for [seconds]
 * seconds a time is shown, 7:30, and the Learner says when in the day it is: *rano*, *po
 * południu*, *w nocy*. A right answer brings the next time at once; a wrong one stays, with the
 * right one, until the Learner moves on, while the clock runs. The screen calls [tick] once a
 * second. The result is the right answers, and the most is the Best.
 */
data class ClockRound(
    override val played: List<String>,
    override val seconds: Int,
    val cards: List<ClockCard>,
    val index: Int = 0,
    val right: Int = 0,
    override val left: Int = seconds,
    /** The option picked for the card being shown, set while that answer is wrong. */
    val missed: Int? = null,
) : TimedRound {
    override val game: Game get() = Game.TIMES_OF_DAY
    override val done: Int get() = seconds - left
    override val total: Int get() = seconds
    override val finished: Boolean get() = left <= 0 || index >= cards.size

    /** The cards change in place: the whole Round is one step. */
    override val step: Int get() = 0
    override val score: Score get() = Score(right)
    override val asked: List<String> get() = cards.take(index).map { it.key }.distinct()

    /** The card being shown, or null once the Round is finished. */
    val card: ClockCard? get() = if (finished) null else cards.getOrNull(index)

    /** Picks the option [option] of the card being shown. No change while a wrong answer is shown, or for no such option. */
    fun answer(option: Int): ClockRound {
        val card = card ?: return this
        if (missed != null || option !in card.options.indices) return this
        return if (option == card.right) copy(index = index + 1, right = right + 1) else copy(missed = option)
    }

    /** Moves on from a wrong answer. No change if none is being shown. */
    fun next(): ClockRound = if (missed != null) copy(index = index + 1, missed = null) else this

    /** One second passes. At none left the Round is finished. */
    override fun tick(): ClockRound = if (finished) this else copy(left = left - 1)
}

/**
 * Times of day: once four of the content's `timesOfDay` are learned. A Round deals them, each once
 * before any comes again and never one twice running, each at an hour it covers, with up to four
 * learned times of day to choose from.
 */
internal object TimesOfDay : GameRules {
    const val TIME = "time"
    const val SECONDS_PREFIX = "seconds-"
    val TIMES = listOf(60, 120)

    /** The fewest times of day the Game opens with, and the most options a card shows. */
    const val LEAST = 4
    const val OPTIONS = 4

    /** The minutes a time not on the hour may show. */
    val MINUTES = listOf(0, 15, 30, 45)

    /** How many cards are dealt for each second: more than anyone answers. */
    private const val CARDS_A_SECOND = 2

    override val game = Game.TIMES_OF_DAY

    private fun times(learned: Learned): List<Pair<TimeOfDay, Item>> {
        val known = learned.items.associateBy { it.id }
        return learned.course.timesOfDay.mapNotNull { time -> known[time.item]?.let { time to it } }
            .filter { (time, _) -> time.hours.isNotEmpty() }.distinctBy { it.second.text.lowercase() }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (times(learned).size >= LEAST) GameState.READY else GameState.NEEDS_MORE

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> =
        listOf(choice(TIME, TIMES.map { OptionView("$SECONDS_PREFIX$it") }, wanted.getOrNull(0)))

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val seconds = selected[0].removePrefix(SECONDS_PREFIX).toInt()
        val all = times(learned)
        val key = { pair: Pair<TimeOfDay, Item> -> pair.second.id }
        val order = mutableListOf<Pair<TimeOfDay, Item>>()
        order += random.draw(all, all.size, seen, avoidFirst, key)
        while (order.size < seconds * CARDS_A_SECOND) {
            val again = all.shuffled(random)
            order += if (key(again.first()) == key(order.last())) again.drop(1) + again.first() else again
        }
        val cards = order.take(seconds * CARDS_A_SECOND).map { (time, item) ->
            val others = all.filter { it.second.id != item.id }.shuffled(random).take(OPTIONS - 1).map { it.second.text }
            val options = (others + item.text).shuffled(random)
            ClockCard(
                item.id,
                hour = time.hours.random(random),
                minute = if (time.onTheHour) 0 else MINUTES.random(random),
                options = options,
                right = options.indexOf(item.text),
                meaning = learned.localize(item.meaning),
            )
        }
        return ClockRound(selected, seconds, cards)
    }
}
