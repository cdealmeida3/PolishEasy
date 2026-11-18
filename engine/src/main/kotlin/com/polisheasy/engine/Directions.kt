package com.polisheasy.engine

import kotlin.random.Random

/** A step of a route on the map of Directions: straight on to the next block, or a turn left or right and on to the next. */
enum class Move(val word: String) {
    STRAIGHT("prosto"),
    LEFT("lewo"),
    RIGHT("prawo"),
    ;

    companion object {
        /** The steps [text] names, in its order: *prosto*, *w lewo*, *w prawo*. */
        fun heardIn(text: String): List<Move> =
            text.lowercase().split(Regex("[^\\p{L}]+")).mapNotNull { word -> entries.firstOrNull { it.word == word } }
    }
}

/** A block of the map, [column] from the left and [row] from the top. */
data class Block(val column: Int, val row: Int)

/**
 * The map of Directions: [COLUMNS] blocks across and [ROWS] down, a place on each. Every route
 * starts at [START], below the middle of the map, facing up it.
 */
object CityMap {
    const val COLUMNS = 3
    const val ROWS = 2
    val START = Block(COLUMNS / 2, ROWS)

    /** Every block of the map, row by row from the top. */
    val blocks: List<Block> = (0 until ROWS).flatMap { row -> (0 until COLUMNS).map { Block(it, row) } }

    /** The block [moves] lead to from the start, or null when they leave the map or name no step. */
    fun walk(moves: List<Move>): Block? {
        if (moves.isEmpty()) return null
        var at = START
        var across = 0
        var down = -1
        moves.forEach { move ->
            when (move) {
                Move.LEFT -> across = down.also { down = -across }
                Move.RIGHT -> across = (-down).also { down = across }
                Move.STRAIGHT -> Unit
            }
            at = Block(at.column + across, at.row + down)
            if (at.column !in 0 until COLUMNS || at.row !in 0 until ROWS) return null
        }
        return at
    }
}

/** One route of Directions: [text] (*Proszę iść prosto, potem skręcić w lewo.*), what it means, and the [items] it uses. */
data class Route(val text: String, val meaning: Localized, val items: List<String>) {
    /** The block the route leads to, or null when it leaves the map. */
    val end: Block? get() = CityMap.walk(Move.heardIn(text))
}

/** A place on the map of a Round: its [block], and the Item it shows, its [picture] and its [text], *kościół*. */
data class MapPlace(val block: Block, val picture: Picture, val text: String, val meaning: String)

/** One question of Directions: a route, said or written in [text] with what it [means], and the place it leads to, [place] of the map. */
data class RouteQuestion(val key: String, val text: String, val meaning: String, val place: Int)

/**
 * A Round of Directions, chapter 14's Game. A map of a few blocks with a place on each, *kościół*,
 * *zamek*, *przystanek*; the Learner stands below it. A route is said, or with [listen] off
 * written, *Proszę iść prosto, potem skręcić w lewo.*, and the Learner taps the place it leads to.
 * The result is the answers right first try.
 */
data class DirectionsRound(
    override val played: List<String>,
    val listen: Boolean,
    val map: List<MapPlace>,
    val run: Run<RouteQuestion>,
) : Round {
    override val game: Game get() = Game.DIRECTIONS
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The route being asked, or null once the Round is finished. */
    val question: RouteQuestion? get() = run.current

    /** The answer to the route being asked, until the Learner moves on: the index of the place tapped. */
    val given: Given? get() = run.given

    /** The place tapped for the route being asked, until the Learner moves on. */
    val chosen: Int? get() = given?.text?.toIntOrNull()

    /** Taps the place at [index] of the map. No change for no such place, or once answered. */
    fun choose(index: Int): DirectionsRound {
        val question = question ?: return this
        if (index !in map.indices) return this
        return copy(run = run.answer(index.toString(), index == question.place))
    }

    /** Moves on from an answered route. */
    fun next(): DirectionsRound = copy(run = run.next())
}

/**
 * Directions: the routes of `routes` and the places of `places` in practice.json. It opens once
 * routes to [FEWEST] blocks have all their Items learned, and a place for every block of the map.
 */
internal object Directions : GameRules {
    const val WAY = "routes"
    const val LISTEN = "listen"
    const val READ = "read"

    /** How many routes a Round asks, at most, and to how many blocks learned routes must lead for it to open. */
    const val QUESTIONS = 5
    const val FEWEST = 3

    override val game = Game.DIRECTIONS

    private fun routes(learned: Learned, listen: Boolean): List<Route> {
        val known = learned.items.map { it.id }.toSet()
        return learned.course.routes.filter { known.containsAll(it.items) && it.end != null }
            .filter { !listen || learned.course.clipFor(it.text) != null }
    }

    private fun places(learned: Learned): List<Item> {
        val known = learned.items.map { it.id }.toSet()
        return learned.course.places.filter { it in known && it in learned.course.pictures }.mapNotNull(learned.course.itemsById::get)
    }

    private fun enough(learned: Learned, listen: Boolean) =
        routes(learned, listen).mapNotNull { it.end }.distinct().size >= FEWEST && places(learned).size >= CityMap.blocks.size

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (enough(learned, listen = false)) GameState.READY else GameState.NEEDS_MORE

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val ways = listOfNotNull(LISTEN.takeIf { sound && enough(learned, listen = true) }, READ)
        return listOf(choice(WAY, ways.map { OptionView(it) }, wanted.getOrNull(0)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val listen = selected[0] == LISTEN
        val all = routes(learned, listen)
        // One route to each block, those not asked today first, so no two questions have the same answer.
        val chosen = random.draw(all, all.size, seen, avoidFirst) { it.text }.distinctBy { it.end }.take(QUESTIONS)
        val map = CityMap.blocks.zip(places(learned).shuffled(random)).map { (block, item) ->
            MapPlace(block, learned.course.pictures.getValue(item.id), item.text, learned.localize(item.meaning))
        }
        val questions = chosen.map { route ->
            RouteQuestion(route.text, route.text, learned.localize(route.meaning), map.indexOfFirst { it.block == route.end })
        }
        return DirectionsRound(selected, listen, map, Run(questions))
    }
}
