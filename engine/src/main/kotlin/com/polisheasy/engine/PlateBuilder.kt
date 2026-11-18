package com.polisheasy.engine

import kotlin.random.Random

/**
 * What Plate builder needs from the content, as Item ids: [and], which joins two dishes (*i*), and
 * the [dishes] it can ask for.
 */
data class Plates(val and: String, val dishes: List<Dish>)

/**
 * One dish of Plate builder: a [base] with something added, said by [with] (*z masłem*), whose
 * [topping] is the thing added (*masło*). The base and the topping each have a Picture.
 */
data class Dish(val base: String, val with: String, val topping: String)

/** A food on Plate builder's tray: its Item [id], its [picture], and what it [means], for a screen reader. */
data class PlateFood(val id: String, val picture: Picture, val means: String)

/**
 * One order of Plate builder: the dishes said in [words] (*chleb z masłem i herbata z cytryną*),
 * the foods [wanted] on the plate, as Item ids, and the [tray] they are picked from. [key] is the
 * dishes' Item ids joined.
 */
data class PlateQuestion(
    val key: String,
    val words: List<String>,
    val wanted: Set<String>,
    val tray: List<PlateFood>,
) {
    /** The words as a sentence: a capital first and a full stop. */
    val polish: String get() = words.joinToString(" ").replaceFirstChar { it.uppercase() } + "."
}

/**
 * A Round of Plate builder, chapter 8's Game: an order is said, or with [listen] off written, and
 * the Learner puts on the plate the foods it needs, from the tray: *chleb z masłem* is bread and
 * butter. [placed] is what is on the plate, in the order it was put there. Served, the order is
 * right if the plate holds exactly what it needs. The result is the orders served right first try.
 */
data class PlateRound(
    override val played: List<String>,
    val listen: Boolean,
    val run: Run<PlateQuestion>,
    val placed: List<String> = emptyList(),
) : Round {
    override val game: Game get() = Game.PLATE_BUILDER
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The order being shown, or null once the Round is finished. */
    val question: PlateQuestion? get() = run.current

    /** The answer to the order being shown, until the Learner moves on. Its text is the Item ids on the plate, joined by commas. */
    val given: Given? get() = run.given

    /** Whether there is something on the plate to serve. */
    val canServe: Boolean get() = given == null && placed.isNotEmpty() && question != null

    /** Puts the food [id] of the tray on the plate, or takes it off if it is there. No change if it is not on the tray or the order is answered. */
    fun place(id: String): PlateRound {
        val question = question ?: return this
        if (given != null || question.tray.none { it.id == id }) return this
        return copy(placed = if (id in placed) placed - id else placed + id)
    }

    /** Serves the plate: right if it holds exactly the foods the order needs. */
    fun serve(): PlateRound {
        val question = question ?: return this
        if (!canServe) return this
        return copy(run = run.answer(placed.joinToString(","), placed.toSet() == question.wanted))
    }

    /** Moves on from a served order, with the plate empty. */
    fun next(): PlateRound = if (given == null) this else copy(run = run.next(), placed = emptyList())
}

/**
 * Plate builder: once dishes of the content's `plates` are learned (the base, the phrase with *z*
 * and the topping, each with a Picture), orders of one dish, or of two joined by *i*. The tray
 * holds the foods wanted and others like them: what else goes with the same base, and what else
 * the same topping goes on.
 */
internal object PlateBuilder : GameRules {
    const val CLUE = "order"
    const val LISTEN = "listen"
    const val READ = "read"
    const val LEVEL = "level"
    const val ONE = "one-dish"
    const val TWO = "two-dishes"

    /** The fewest dishes the Game opens with, and the most orders a Round asks. */
    const val LEAST = 4
    const val QUESTIONS = 6

    /** How many foods the tray holds for one dish, and for two. */
    const val TRAY_ONE = 6
    const val TRAY_TWO = 8

    override val game = Game.PLATE_BUILDER

    private class Served(val base: Item, val with: Item, val topping: Item, val basePicture: Picture, val toppingPicture: Picture)

    /** The dishes whose Items are all learned and whose base and topping have different Pictures; heard, only those whose words have Clips. */
    private fun dishes(learned: Learned, listen: Boolean): List<Served> {
        val plates = learned.course.plates ?: return emptyList()
        val known = learned.items.associateBy { it.id }
        val pictures = learned.course.pictures
        return plates.dishes.mapNotNull { dish ->
            val base = known[dish.base] ?: return@mapNotNull null
            val with = known[dish.with] ?: return@mapNotNull null
            val topping = known[dish.topping] ?: return@mapNotNull null
            val basePicture = pictures[dish.base] ?: return@mapNotNull null
            val toppingPicture = pictures[dish.topping] ?: return@mapNotNull null
            if (basePicture == toppingPicture) return@mapNotNull null
            if (listen && listOf(base, with).any { learned.course.clipFor(it.text) == null }) return@mapNotNull null
            Served(base, with, topping, basePicture, toppingPicture)
        }.distinctBy { it.base.id to it.with.id }
    }

    /** The Item *i*, once learned and, heard, said by a Clip. */
    private fun and(learned: Learned, listen: Boolean): Item? {
        val id = learned.course.plates?.and ?: return null
        return learned.items.firstOrNull { it.id == id }?.takeIf { !listen || learned.course.clipFor(it.text) != null }
    }

    /** The orders each level can ask: one dish, and two dishes with four different foods. */
    private fun levels(learned: Learned, listen: Boolean): List<Pair<String, List<List<Served>>>> {
        val dishes = dishes(learned, listen)
        if (dishes.size < LEAST) return emptyList()
        val one = ONE to dishes.map { listOf(it) }
        val and = and(learned, listen) ?: return listOf(one)
        val pairs = dishes.flatMapIndexed { index, first ->
            dishes.drop(index + 1).filter { second ->
                second.base.id != first.base.id &&
                    setOf(first.basePicture, first.toppingPicture, second.basePicture, second.toppingPicture).size == 4
            }.map { listOf(first, it) }
        }
        return if (pairs.size >= LEAST) listOf(one, TWO to pairs) else listOf(one)
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
        val dishes = dishes(learned, listen)
        val level = levels(learned, listen).first { it.first == selected[1] }
        val and = and(learned, listen)
        val size = if (level.first == ONE) TRAY_ONE else TRAY_TWO
        val orders = random.draw(level.second, QUESTIONS, seen, avoidFirst) { order -> order.joinToString("+") { "${it.base.id}:${it.with.id}" } }
        val questions = orders.map { order ->
            val words = order.map { listOf(it.base.text, it.with.text) }.reduce { first, second -> first + checkNotNull(and).text + second }
            val wanted = order.flatMap { listOf(food(learned, it.base, it.basePicture), food(learned, it.topping, it.toppingPicture)) }
            PlateQuestion(
                order.joinToString("+") { "${it.base.id}:${it.with.id}" },
                words,
                wanted.map { it.id }.toSet(),
                (wanted + others(learned, dishes, order, wanted, size - wanted.size, random)).shuffled(random),
            )
        }
        return PlateRound(selected, listen, Run(questions))
    }

    private fun food(learned: Learned, item: Item, picture: Picture) = PlateFood(item.id, picture, learned.localize(item.meaning))

    /**
     * [count] foods of the learned dishes to put beside those [wanted]: first those that go with
     * the same base or on the same topping in another dish, then any, none with a Picture already there.
     */
    private fun others(learned: Learned, dishes: List<Served>, order: List<Served>, wanted: List<PlateFood>, count: Int, random: Random): List<PlateFood> {
        val all = dishes.flatMap { listOf(food(learned, it.base, it.basePicture), food(learned, it.topping, it.toppingPicture)) }.distinctBy { it.id }
        val bases = order.map { it.base.id }.toSet()
        val toppings = order.map { it.topping.id }.toSet()
        val alike = dishes.flatMap { dish ->
            when {
                dish.base.id in bases -> listOf(dish.topping.id)
                dish.topping.id in toppings -> listOf(dish.base.id)
                else -> emptyList()
            }
        }.toSet()
        val (near, far) = all.shuffled(random).partition { it.id in alike }
        val chosen = mutableListOf<PlateFood>()
        (near + far).forEach { food ->
            if (chosen.size < count && (wanted + chosen).none { it.id == food.id || it.picture == food.picture }) chosen += food
        }
        return chosen
    }
}
