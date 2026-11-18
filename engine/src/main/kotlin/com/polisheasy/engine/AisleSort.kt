package com.polisheasy.engine

import kotlin.random.Random

/** One aisle of Sort the aisles, as Item ids: the Item that [names] it (*nabiał*) and the [products] that belong in it. */
data class Aisle(val names: String, val products: List<String>)

/** An aisle's sign in a Round: its Item [id], its Polish and what it means. */
data class AisleSign(val id: String, val polish: String, val meaning: String)

/** One product of Sort the aisles: its Item, its Polish, what it means, its Picture if it has one, and the aisle it belongs in. */
data class AisleCard(
    val itemId: String,
    val polish: String,
    val meaning: String,
    val picture: Picture?,
    val aisle: String,
)

/**
 * A Round of Sort the aisles, chapter 9's Game, on the Quick round's clock: for [seconds] seconds
 * a product is shown, and the Learner sorts it into one of the supermarket's [aisles]. A right
 * answer brings the next product at once; a wrong one stays, with the aisle it belongs in, until
 * the Learner moves on, while the clock runs. The clock is the screen's: it calls [tick] once a
 * second. The result is the right answers, and the most is the Best.
 */
data class AisleRound(
    override val played: List<String>,
    override val seconds: Int,
    val cards: List<AisleCard>,
    /** The aisles of this Round, at most [AisleSort.AISLES], in the content's order. */
    val aisles: List<AisleSign>,
    val index: Int = 0,
    val right: Int = 0,
    /** How many seconds are left. */
    override val left: Int = seconds,
    /** The aisle given for the product being shown, set while that answer is wrong. */
    val missed: String? = null,
    /** How many products have been sorted right into each aisle, by its Item id, for the signs. */
    val sorted: Map<String, Int> = emptyMap(),
) : TimedRound {
    override val game: Game get() = Game.AISLE_SORT

    /** The clock, for the bar: the seconds gone of all there are. */
    override val done: Int get() = seconds - left
    override val total: Int get() = seconds
    override val finished: Boolean get() = left <= 0 || index >= cards.size

    /** The products change in place: the whole Round is one step. */
    override val step: Int get() = 0
    override val score: Score get() = Score(right)
    override val asked: List<String> get() = cards.take(index).map { it.itemId }.distinct()

    /** The product being shown, or null once the Round is finished. */
    val card: AisleCard? get() = if (finished) null else cards.getOrNull(index)

    /** The sign of the aisle the product being shown belongs in, or null once the Round is finished. */
    val rightAisle: AisleSign? get() = card?.let { card -> aisles.first { it.id == card.aisle } }

    /** Sorts the product being shown into the aisle [id]. No change while a wrong answer is shown, or for an aisle not in the Round. */
    fun answer(id: String): AisleRound {
        val card = card ?: return this
        if (missed != null || aisles.none { it.id == id }) return this
        return if (id == card.aisle) {
            copy(index = index + 1, right = right + 1, sorted = sorted + (id to (sorted[id] ?: 0) + 1))
        } else {
            copy(missed = id)
        }
    }

    /** Moves on from a wrong answer. No change if none is being shown. */
    fun next(): AisleRound = if (missed != null) copy(index = index + 1, missed = null) else this

    /** One second passes. At none left the Round is finished. */
    override fun tick(): AisleRound = if (finished) this else copy(left = left - 1)
}

/**
 * Sort the aisles: once three aisles of the content's `aisles` can be stocked (the aisle's name and
 * two of its products learned) with six products in all. A Round takes up to four of them, at
 * random, and deals their products.
 */
internal object AisleSort : GameRules {
    const val TIME = "time"
    const val SECONDS_PREFIX = "seconds-"
    val TIMES = listOf(60, 120)

    /** The fewest products and aisles the Game opens with, the fewest products an aisle needs to be stocked, and the most aisles of a Round. */
    const val LEAST = 6
    const val LEAST_AISLES = 3
    const val STOCKED = 2
    const val AISLES = 4

    /** How many products are dealt for each second: more than anyone answers. */
    private const val CARDS_A_SECOND = 2

    override val game = Game.AISLE_SORT

    /** The aisles whose name and at least [STOCKED] products are learned, each with its learned products, none in two aisles. */
    private fun stocked(learned: Learned): List<Pair<Item, List<Item>>> {
        val known = learned.items.associateBy { it.id }
        val aisles = learned.course.aisles
        val twice = aisles.flatMap { it.products.distinct() }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        return aisles.mapNotNull { aisle ->
            val name = known[aisle.names] ?: return@mapNotNull null
            val products = aisle.products.distinct().filter { it !in twice }.mapNotNull { known[it] }.distinctBy { it.text.lowercase() }
            if (products.size >= STOCKED) name to products else null
        }.distinctBy { it.first.id }
    }

    override fun state(learned: Learned, sound: Boolean): GameState {
        val aisles = stocked(learned)
        return if (aisles.size >= LEAST_AISLES && aisles.sumOf { it.second.size } >= LEAST) GameState.READY else GameState.NEEDS_MORE
    }

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> =
        listOf(choice(TIME, TIMES.map { OptionView("$SECONDS_PREFIX$it") }, wanted.getOrNull(0)))

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val seconds = selected[0].removePrefix(SECONDS_PREFIX).toInt()
        val all = stocked(learned)
        val chosen = all.shuffled(random).take(AISLES).map { it.first.id }.toSet()
        val aisles = all.filter { it.first.id in chosen }
        val products = aisles.flatMap { (name, products) -> products.map { it to name.id } }
        // Every product comes round before any comes again, those not seen today first.
        val order = mutableListOf<Pair<Item, String>>()
        order += random.draw(products, products.size, seen, avoidFirst) { it.first.id }
        while (order.size < seconds * CARDS_A_SECOND) {
            val again = products.shuffled(random)
            // The same product is never shown twice running.
            order += if (again.first().first.id == order.last().first.id) again.drop(1) + again.first() else again
        }
        val cards = order.take(seconds * CARDS_A_SECOND).map { (item, aisle) ->
            AisleCard(item.id, item.text, learned.localize(item.meaning), learned.course.pictures[item.id], aisle)
        }
        return AisleRound(selected, seconds, cards, aisles.map { (name, _) -> AisleSign(name.id, name.text, learned.localize(name.meaning)) })
    }
}
