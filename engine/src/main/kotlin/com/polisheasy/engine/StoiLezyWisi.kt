package com.polisheasy.engine

import kotlin.random.Random

/**
 * One question of Stoi, leży czy wisi?: the sentence of the room with its verb gapped, [gapped]
 * (*Obraz ___ na ścianie.*), the whole [text] and what it means, the thing it is about and its Picture, if it has one, and the verbs
 * [offered], the row of the room's positions the right one is in, *stoi*, *leży*, *wisi*.
 */
data class PositionQuestion(
    val key: String,
    val gapped: String,
    val text: String,
    val meaning: String,
    val thing: String,
    val picture: Picture?,
    val offered: List<String>,
    val right: Int,
)

/**
 * A Round of Stoi, leży czy wisi?, chapter 18's second Game, after the book's *Uwaga!* on how a thing
 * is where it is. A sentence of the room is written with its verb gapped, *Obraz ___ na ścianie.*, and
 * the Learner taps the verb: *stoi*, *leży* or *wisi*, or their plurals when the thing is more than
 * one. Once answered the sentence is whole and said. The result is the answers right first try.
 */
data class PositionsRound(
    override val played: List<String>,
    val run: Run<PositionQuestion>,
) : Round {
    override val game: Game get() = Game.STOI_LEZY_WISI
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The sentence being asked, or null once the Round is finished. */
    val question: PositionQuestion? get() = run.current

    /** The answer to the sentence being asked, until the Learner moves on: the verb tapped. */
    val given: Given? get() = run.given

    /** Taps the verb at [option] of those offered. No change for no such verb, or once answered. */
    fun choose(option: Int): PositionsRound {
        val question = question ?: return this
        val verb = question.offered.getOrNull(option) ?: return this
        return copy(run = run.answer(verb, option == question.right))
    }

    /** Moves on from an answered sentence. */
    fun next(): PositionsRound = copy(run = run.next())
}

/**
 * Stoi, leży czy wisi?: the sentences of `room` in practice.json. It opens once [FEWEST] of them
 * have all their Items learned, and every verb of the row each is in. A Round asks up to [QUESTIONS],
 * those not asked today first.
 */
internal object StoiLezyWisi : GameRules {
    /** How many sentences a Round asks, at most, and how many must be learned for it to open. */
    const val QUESTIONS = 8
    const val FEWEST = 4

    /** The gap written for the verb. */
    const val GAP = "___"

    override val game = Game.STOI_LEZY_WISI

    private fun scenes(learned: Learned): List<Pair<RoomSceneLine, List<String>>> {
        val known = learned.items.map { it.id }.toSet()
        val rows = learned.course.room?.positions.orEmpty()
        return learned.roomScenes().mapNotNull { scene -> rows.firstOrNull { scene.verb in it }?.takeIf { known.containsAll(it) }?.let { scene to it } }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (scenes(learned).size >= FEWEST) GameState.READY else GameState.NEEDS_MORE

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val questions = random.draw(scenes(learned), QUESTIONS, seen, avoidFirst) { it.first.text }.map { (scene, row) ->
            val offered = row.map { learned.course.itemsById.getValue(it).text }
            val verb = learned.course.itemsById.getValue(scene.verb).text
            PositionQuestion(
                key = scene.text,
                gapped = gap(scene.text, verb),
                text = scene.text,
                meaning = learned.localize(scene.meaning),
                thing = scene.thing,
                picture = learned.course.pictures[scene.thing],
                offered = offered,
                right = offered.indexOf(verb),
            )
        }
        return PositionsRound(selected, Run(questions))
    }

    /** [text] with the word [verb] written as the gap: *Obraz ___ na ścianie.* */
    fun gap(text: String, verb: String): String = Regex("(?<!\\p{L})${Regex.escape(verb)}(?!\\p{L})").replaceFirst(text, GAP)
}
