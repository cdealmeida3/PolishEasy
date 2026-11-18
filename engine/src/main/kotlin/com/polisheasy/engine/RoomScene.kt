package com.polisheasy.engine

import kotlin.random.Random

/**
 * The room of chapter 18's Games, `room` in practice.json: the [places] in it, furniture and parts of
 * a room such as *łóżko*, *szafa*, *ściana*; the [positions], each a row of the verbs for where a
 * thing is, *stoi*, *leży*, *wisi* and their plurals; and the [scenes] that put a thing somewhere.
 */
data class Room(val places: List<String>, val positions: List<List<String>>, val scenes: List<RoomSceneLine>)

/**
 * One sentence of the room: [text] (*Lampa stoi na biurku.*) with what it means, the [thing] it puts
 * somewhere, the [place] it puts it, one of the room's places, the [verb] that says how (*stoi*),
 * one of its positions, and the [items] it uses.
 */
data class RoomSceneLine(val text: String, val meaning: Localized, val thing: String, val place: String, val verb: String, val items: List<String>)

/** A place on the board of Room scene: its Item, its Polish (*łóżko*) and what it means. */
data class RoomPlace(val item: String, val text: String, val meaning: String)

/** One question of Room scene: a sentence, said or written in [text] with what it means, the thing it moves (as the sentence says it, its Item and its Picture, if it has one) and the [place] of the board it goes to. */
data class SceneQuestion(val key: String, val text: String, val meaning: String, val thing: String, val thingItem: String, val picture: Picture?, val place: Int)

/**
 * A Round of Room scene, chapter 18's Game, after the book's room. The board is a room of a few
 * places, *łóżko*, *biurko*, *ściana*. A sentence is said, or with [listen] off written, *Lampa stoi na
 * biurku.*, and the Learner taps the place it puts its thing; once answered, the thing is there, so
 * the room fills as the Round goes on. The result is the answers right first try.
 */
data class RoomSceneRound(
    override val played: List<String>,
    val listen: Boolean,
    val board: List<RoomPlace>,
    val run: Run<SceneQuestion>,
) : Round {
    override val game: Game get() = Game.ROOM_SCENE
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The sentence being asked, or null once the Round is finished. */
    val question: SceneQuestion? get() = run.current

    /** The answer to the sentence being asked, until the Learner moves on: the index of the place tapped. */
    val given: Given? get() = run.given

    /** The place tapped for the sentence being asked, until the Learner moves on. */
    val chosen: Int? get() = given?.text?.toIntOrNull()

    /** The things already put on the place at [index]: those of the sentences answered, the one being asked included once answered. */
    fun things(index: Int): List<SceneQuestion> =
        run.questions.take(run.results.size + if (run.given != null) 1 else 0).filter { it.place == index }

    /** Taps the place at [index] of the board. No change for no such place, or once answered. */
    fun choose(index: Int): RoomSceneRound {
        val question = question ?: return this
        if (index !in board.indices) return this
        return copy(run = run.answer(index.toString(), index == question.place))
    }

    /** Moves on from an answered sentence. */
    fun next(): RoomSceneRound = copy(run = run.next())
}

/** The sentences of the room whose Items are all learned, and the places learned. */
internal fun Learned.roomScenes(): List<RoomSceneLine> {
    val known = items.map { it.id }.toSet()
    val room = course.room ?: return emptyList()
    return room.scenes.filter { known.containsAll(it.items) && it.place in known && it.thing in known && it.verb in known }
}

/**
 * Room scene: the room of `room` in practice.json. It opens once learned sentences put things on
 * [FEWEST] different places and [BOARD] places are learned. A Round asks up to [QUESTIONS]
 * sentences, each to a place of its own, those not asked today first, on a board of [BOARD] places.
 */
internal object RoomScene : GameRules {
    const val WAY = "scenes"
    const val LISTEN = "listen"
    const val READ = "read"

    /** How many sentences a Round asks, at most. */
    const val QUESTIONS = 5

    /** To how many places learned sentences must lead for the Game to open. */
    const val FEWEST = 3

    /** How many places the board has. */
    const val BOARD = 6

    override val game = Game.ROOM_SCENE

    private fun scenes(learned: Learned, listen: Boolean): List<RoomSceneLine> =
        learned.roomScenes().filter { !listen || learned.course.clipFor(it.text) != null }

    private fun places(learned: Learned): List<Item> {
        val known = learned.items.map { it.id }.toSet()
        return learned.course.room?.places.orEmpty().filter { it in known }.mapNotNull(learned.course.itemsById::get)
    }

    private fun enough(learned: Learned, listen: Boolean) =
        scenes(learned, listen).map { it.place }.distinct().size >= FEWEST && places(learned).size >= BOARD

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (enough(learned, listen = false)) GameState.READY else GameState.NEEDS_MORE

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val ways = listOfNotNull(LISTEN.takeIf { sound && enough(learned, listen = true) }, READ)
        return listOf(choice(WAY, ways.map { OptionView(it) }, wanted.getOrNull(0)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val listen = selected[0] == LISTEN
        val all = scenes(learned, listen)
        // One sentence to each place, those not asked today first, so no two questions have the same answer.
        val chosen = random.draw(all, all.size, seen, avoidFirst) { it.text }.distinctBy { it.place }.take(QUESTIONS)
        val asked = chosen.map { it.place }
        val others = places(learned).filter { it.id !in asked }.shuffled(random)
        val board = (asked.mapNotNull(learned.course.itemsById::get) + others).take(BOARD).shuffled(random)
            .map { RoomPlace(it.id, it.text, learned.localize(it.meaning)) }
        val questions = chosen.map { scene ->
            val thing = learned.course.itemsById.getValue(scene.thing)
            SceneQuestion(scene.text, scene.text, learned.localize(scene.meaning), said(scene.text), thing.id, learned.course.pictures[thing.id], board.indexOfFirst { it.item == scene.place })
        }
        return RoomSceneRound(selected, listen, board, Run(questions))
    }

    /** The thing as [text] says it, its first word: *zdjęcia* in *Zdjęcia wiszą na ścianie.* Every sentence of the room starts with its thing. */
    private fun said(text: String): String = text.substringBefore(' ').trim { !it.isLetter() }.lowercase()
}
