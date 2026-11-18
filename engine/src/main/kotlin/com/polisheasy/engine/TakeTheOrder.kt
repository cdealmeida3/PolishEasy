package com.polisheasy.engine

import kotlin.random.Random

/**
 * What Take the order needs from the content, as Item ids: [forMe] (*dla mnie*), [same] (*to
 * samo*), [and] (*a*), which turns to the next guest, and the [dishes] of the menu.
 */
data class Restaurant(val forMe: String, val same: String, val and: String, val dishes: List<String>)

/** A dish on Take the order's menu: its Item [id], its Polish, what it [means], and its Picture if it has one. */
data class MenuDish(val id: String, val polish: String, val means: String, val picture: Picture?)

/**
 * One table of Take the order: what each guest says, in words (*a*, *dla mnie*, *to samo*), the
 * dish each guest wants, by Item id, and the [menu] they are picked from. [key] names the dishes,
 * and where a guest said *to samo*.
 */
data class TableQuestion(
    val key: String,
    val lines: List<List<String>>,
    val dishes: List<String>,
    val menu: List<MenuDish>,
) {
    /** Each guest's words as a sentence: a capital first and a full stop. */
    val polish: List<String> get() = lines.map { words -> words.joinToString(" ").replaceFirstChar { it.uppercase() } + "." }
}

/**
 * A Round of Take the order, chapter 10's Game: a table of guests orders, heard or, with [listen]
 * off, read (*Dla mnie żurek. A dla mnie to samo.*), and the Learner, the waiter, writes each
 * guest's dish on the [pad], in turn, from the menu. A guest who says *to samo* wants what the
 * guest before them ordered. Served, the table is right if every guest has their dish. The result
 * is the tables served right first try.
 */
data class TableRound(
    override val played: List<String>,
    val listen: Boolean,
    val run: Run<TableQuestion>,
    /** The dish written for each guest so far, by Item id, guest by guest. */
    val pad: List<String> = emptyList(),
) : Round {
    override val game: Game get() = Game.TAKE_THE_ORDER
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The table being served, or null once the Round is finished. */
    val question: TableQuestion? get() = run.current

    /** The answer for the table being shown, until the Learner moves on. Its text is the pad's Item ids, joined by commas. */
    val given: Given? get() = run.given

    /** Whether every guest has a dish on the pad, so the table can be served. */
    val canServe: Boolean get() = given == null && question?.let { pad.size == it.dishes.size } == true

    /** Writes the dish [id] of the menu for the next guest. No change if it is not on the menu, every guest has one, or the table is served. */
    fun write(id: String): TableRound {
        val question = question ?: return this
        if (given != null || question.menu.none { it.id == id } || pad.size >= question.dishes.size) return this
        return copy(pad = pad + id)
    }

    /** Rubs out the last dish written. No change if there is none or the table is served. */
    fun rubOut(): TableRound = if (given != null || pad.isEmpty()) this else copy(pad = pad.dropLast(1))

    /** Serves the table: right if each guest has the dish they ordered. */
    fun serve(): TableRound {
        val question = question ?: return this
        if (!canServe) return this
        return copy(run = run.answer(pad.joinToString(","), pad == question.dishes))
    }

    /** Moves on from a served table, with the pad empty. */
    fun next(): TableRound = if (given == null) this else copy(run = run.next(), pad = emptyList())
}

/**
 * Take the order: once *dla mnie*, *to samo* and *a* are learned and six dishes of the content's
 * `restaurant`, tables of two or three guests. The first says *dla mnie* and a dish; each after
 * says *a dla mnie* and a dish other than the one before, or *to samo*. The menu holds the dishes
 * ordered and others, [MENU] in all.
 */
internal object TakeTheOrder : GameRules {
    const val CLUE = "clue"
    const val LISTEN = "listen"
    const val READ = "read"
    const val TABLE = "table"
    const val TWO = "two-guests"
    const val THREE = "three-guests"

    /** The fewest dishes the Game opens with, the tables a Round serves, and the dishes on the menu. */
    const val LEAST = 6
    const val QUESTIONS = 5
    const val MENU = 6

    /** How many tables are made up to draw a Round's from: enough that one is rarely seen twice. */
    private const val CANDIDATES = 60

    override val game = Game.TAKE_THE_ORDER

    /** The words that order, *dla mnie*, *to samo* and *a*, once learned and, heard, said by Clips. */
    private class Words(val forMe: Item, val same: Item, val and: Item)

    private fun words(learned: Learned, listen: Boolean): Words? {
        val restaurant = learned.course.restaurant ?: return null
        val known = learned.items.associateBy { it.id }
        val words = listOf(restaurant.forMe, restaurant.same, restaurant.and).map { known[it] ?: return null }
        if (listen && words.any { learned.course.clipFor(it.text) == null }) return null
        return Words(words[0], words[1], words[2])
    }

    /** The learned dishes of the menu, each spelled once and, heard, said by a Clip. */
    private fun dishes(learned: Learned, listen: Boolean): List<Item> {
        val restaurant = learned.course.restaurant ?: return emptyList()
        val known = learned.items.associateBy { it.id }
        return restaurant.dishes.distinct().mapNotNull { known[it] }
            .filter { !listen || learned.course.clipFor(it.text) != null }
            .distinctBy { it.text.lowercase() }
    }

    private fun ready(learned: Learned, listen: Boolean): Boolean = words(learned, listen) != null && dishes(learned, listen).size >= LEAST

    override fun state(learned: Learned, sound: Boolean): GameState = if (ready(learned, listen = false)) GameState.READY else GameState.NEEDS_MORE

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val clues = listOf(LISTEN, READ).filter { it == READ || (sound && ready(learned, listen = true)) }
        return listOf(
            choice(CLUE, clues.map { OptionView(it) }, wanted.getOrNull(0)),
            choice(TABLE, listOf(OptionView(TWO), OptionView(THREE)), wanted.getOrNull(1)),
        )
    }

    /** One table's orders: each guest's dish, and whether they said *to samo*. */
    private class Table(val dishes: List<Item>, val same: List<Boolean>) {
        val key: String get() = dishes.indices.joinToString("+") { if (same[it]) "=" else dishes[it].id }
    }

    private fun table(dishes: List<Item>, guests: Int, random: Random): Table {
        val ordered = mutableListOf(dishes.random(random))
        val same = mutableListOf(false)
        repeat(guests - 1) {
            if (random.nextInt(SAME_ONE_IN) == 0) {
                ordered += ordered.last()
                same += true
            } else {
                ordered += dishes.filter { it.id != ordered.last().id }.random(random)
                same += false
            }
        }
        return Table(ordered, same)
    }

    /** One guest in [SAME_ONE_IN] after the first says *to samo*. */
    private const val SAME_ONE_IN = 3

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val listen = selected[0] == LISTEN
        val guests = if (selected[1] == THREE) 3 else 2
        val words = checkNotNull(words(learned, listen))
        val dishes = dishes(learned, listen)
        val menu = dishes.map { MenuDish(it.id, it.text, learned.localize(it.meaning), learned.course.pictures[it.id]) }
        val candidates = List(CANDIDATES) { table(dishes, guests, random) }.distinctBy { it.key }
        val tables = random.draw(candidates, QUESTIONS, seen, avoidFirst) { it.key }
        val questions = tables.map { table ->
            val lines = table.dishes.indices.map { guest ->
                val order = if (table.same[guest]) words.same.text else table.dishes[guest].text
                listOfNotNull(words.and.text.takeIf { guest > 0 }, words.forMe.text, order)
            }
            val ordered = menu.filter { dish -> table.dishes.any { it.id == dish.id } }
            val others = menu.filter { it !in ordered }.shuffled(random).take(MENU - ordered.size)
            TableQuestion(table.key, lines, table.dishes.map { it.id }, (ordered + others).shuffled(random))
        }
        return TableRound(selected, listen, Run(questions))
    }
}
