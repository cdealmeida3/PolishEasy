package com.polisheasy.engine

import kotlin.random.Random

/** One phrase of Register sort: its Polish, what it means, and the register the book marks it with. */
data class RegisterCard(
    val itemId: String,
    val polish: String,
    val meaning: String,
    val register: Register,
)

/**
 * A Round of Register sort, on the Quick round's clock: for [seconds] seconds a phrase is shown,
 * and the Learner sorts it by who it is said to: formal (O, to *pan* or *pani*), informal (N, to
 * a friend) or universal (U, to anyone). A right answer brings the next phrase at once; a wrong
 * one stays, with the register the phrase has, until the Learner moves on, while the clock runs.
 * The clock is the screen's: it calls [tick] once a second. The result is the right answers, and
 * the most is the Best.
 */
data class RegisterRound(
    override val played: List<String>,
    override val seconds: Int,
    val cards: List<RegisterCard>,
    /** What can be sorted into, in the book's order: O and N, and U once a universal phrase is learned. */
    val registers: List<Register>,
    val index: Int = 0,
    val right: Int = 0,
    /** How many seconds are left. */
    override val left: Int = seconds,
    /** The register given for the phrase being shown, set while that answer is wrong. */
    val missed: Register? = null,
    /** How many phrases have been sorted right into each register, for the heads of the buttons. */
    val sorted: Map<Register, Int> = emptyMap(),
) : TimedRound {
    override val game: Game get() = Game.REGISTER_SORT

    /** The clock, for the bar: the seconds gone of all there are. */
    override val done: Int get() = seconds - left
    override val total: Int get() = seconds
    override val finished: Boolean get() = left <= 0 || index >= cards.size

    /** The phrases change in place: the whole Round is one step. */
    override val step: Int get() = 0
    override val score: Score get() = Score(right)
    override val asked: List<String> get() = cards.take(index).map { it.itemId }.distinct()

    /** The phrase being shown, or null once the Round is finished. */
    val card: RegisterCard? get() = if (finished) null else cards.getOrNull(index)

    /** Sorts the phrase being shown into [register]. No change while a wrong answer is shown, or for a register not offered. */
    fun answer(register: Register): RegisterRound {
        val card = card ?: return this
        if (missed != null || register !in registers) return this
        return if (register == card.register) {
            copy(index = index + 1, right = right + 1, sorted = sorted + (register to (sorted[register] ?: 0) + 1))
        } else {
            copy(missed = register)
        }
    }

    /** Moves on from a wrong answer. No change if none is being shown. */
    fun next(): RegisterRound = if (missed != null) copy(index = index + 1, missed = null) else this

    /** One second passes. At none left the Round is finished. */
    override fun tick(): RegisterRound = if (finished) this else copy(left = left - 1)
}

internal object RegisterSort : GameRules {
    const val TIME = "time"
    const val SECONDS_PREFIX = "seconds-"
    val TIMES = listOf(60, 120)

    /** The fewest phrases the Game opens with, of two registers at least. */
    const val LEAST = 6

    /** How many phrases are dealt for each second: more than anyone answers. */
    private const val CARDS_A_SECOND = 2

    override val game = Game.REGISTER_SORT

    /** The learned phrases the book marks with a register, once each by their Polish. */
    private fun phrases(learned: Learned): List<Item> = learned.items.filter { it.register != null }.distinctBy { it.text.lowercase() }

    override fun state(learned: Learned, sound: Boolean): GameState {
        val phrases = phrases(learned)
        val enough = phrases.size >= LEAST && phrases.mapNotNull { it.register }.toSet().size >= 2
        return if (enough) GameState.READY else GameState.NEEDS_MORE
    }

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> =
        listOf(choice(TIME, TIMES.map { OptionView("$SECONDS_PREFIX$it") }, wanted.getOrNull(0)))

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val seconds = selected[0].removePrefix(SECONDS_PREFIX).toInt()
        val phrases = phrases(learned)
        // Every phrase comes round before any comes again, those not seen today first.
        val order = mutableListOf<Item>()
        order += random.draw(phrases, phrases.size, seen, avoidFirst) { it.id }
        while (order.size < seconds * CARDS_A_SECOND) {
            val again = phrases.shuffled(random)
            // The same phrase is never shown twice running.
            order += if (again.first().id == order.last().id) again.drop(1) + again.first() else again
        }
        val cards = order.take(seconds * CARDS_A_SECOND).map { item ->
            RegisterCard(item.id, item.text, learned.localize(item.meaning), checkNotNull(item.register))
        }
        val registers = Register.entries.filter { register -> phrases.any { it.register == register } }
        return RegisterRound(selected, seconds, cards, registers)
    }
}
