package com.polisheasy.engine

import kotlin.random.Random

/**
 * The eight parts of the country on Poland map's compass, as a 3 by 3 grid without its middle:
 * [key] is how practice.json names it, [polish] is said in a cell, *na północy*, and [meaning]
 * is what that means.
 */
enum class Compass(val key: String, val polish: String, val meaning: String) {
    NORTH_WEST("north-west", "na północnym zachodzie", "in the north-west"),
    NORTH("north", "na północy", "in the north"),
    NORTH_EAST("north-east", "na północnym wschodzie", "in the north-east"),
    WEST("west", "na zachodzie", "in the west"),
    EAST("east", "na wschodzie", "in the east"),
    SOUTH_WEST("south-west", "na południowym zachodzie", "in the south-west"),
    SOUTH("south", "na południu", "in the south"),
    SOUTH_EAST("south-east", "na południowym wschodzie", "in the south-east"),
    ;

    companion object {
        /** The part named [key] in practice.json, or null for no such part. */
        fun of(key: String): Compass? = entries.firstOrNull { it.key == key }
    }
}

/**
 * One place of Poland map, `polandMap` in practice.json: the [place] as the question shows it,
 * *Gdańsk*, the [region] of Poland where it lies, and the sentence that [says] so, *Gdańsk leży
 * na północy, nad morzem.*
 */
data class PolandMapPlace(val id: String, val place: String, val region: Compass, val says: FlatText)

/** One question of Poland map: the [place] to find, where it lies, and the sentence that says so with what it means. */
data class PolandMapQuestion(val key: String, val place: String, val region: Compass, val says: String, val meaning: String)

/**
 * A Round of Poland map, chapter 20's Game, after the book's map of Poland. A city, a range of
 * mountains or a sea is named, *Gdańsk*, and the Learner taps the part of the country where it
 * lies on a compass of eight, *na północy*. Once answered the sentence that says so is said, and
 * a note gives what it means. The result is the places right first try.
 */
data class PolandMapRound(
    override val played: List<String>,
    val run: Run<PolandMapQuestion>,
) : Round {
    override val game: Game get() = Game.POLAND_MAP
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The place being asked, or null once the Round is finished. */
    val question: PolandMapQuestion? get() = run.current

    /** The answer to the place being asked, until the Learner moves on: the part tapped. */
    val given: Given? get() = run.given

    /** The part of the country tapped for the place being asked, until the Learner moves on. */
    val chosen: Compass? get() = given?.text?.let(Compass::of)

    /** Taps [region]. No change once answered. */
    fun choose(region: Compass): PolandMapRound {
        val question = question ?: return this
        return copy(run = run.answer(region.key, region == question.region))
    }

    /** Moves on from an answered place. */
    fun next(): PolandMapRound = copy(run = run.next())
}

/**
 * Poland map: the places of `polandMap` in practice.json. A place counts once every Item of its
 * sentence is learned, and the Game opens with [FEWEST]. A Round asks up to [QUESTIONS], those not
 * asked today first.
 */
internal object PolandMap : GameRules {
    /** How many places a Round asks, at most. */
    const val QUESTIONS = 6

    /** How many places must be learned for the Game to open. */
    const val FEWEST = 4

    override val game = Game.POLAND_MAP

    private fun places(learned: Learned): List<PolandMapPlace> {
        val known = learned.items.map { it.id }.toSet()
        return learned.course.polandMap.filter { known.containsAll(it.says.items) }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (places(learned).size >= FEWEST) GameState.READY else GameState.NEEDS_MORE

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val questions = random.draw(places(learned), QUESTIONS, seen, avoidFirst) { it.id }.map { place ->
            PolandMapQuestion(place.id, place.place, place.region, place.says.text, learned.localize(place.says.meaning))
        }
        return PolandMapRound(selected, Run(questions))
    }
}
