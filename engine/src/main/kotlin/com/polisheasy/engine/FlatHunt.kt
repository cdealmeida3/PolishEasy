package com.polisheasy.engine

import kotlin.random.Random

/**
 * One hunt of Flat hunt, `flatHunt` in practice.json: someone's [wish], *Mam psa. Szukam mieszkania
 * na parterze.*, the [adverts] they read, and the [right] one, the only advert that fits the wish.
 */
data class FlatHuntCase(val id: String, val wish: FlatText, val adverts: List<FlatText>, val right: Int)

/** A text of a hunt, the wish or an advert: its Polish, what it means, and the Items it uses. */
data class FlatText(val text: String, val meaning: Localized, val items: List<String>)

/** An advert as a question shows it: its Polish and what it means. */
data class FlatAdvert(val text: String, val meaning: String)

/** One question of Flat hunt: the [wish] with what it means, the [adverts] in the order shown, and the [right] one. */
data class FlatHuntQuestion(val key: String, val wish: String, val wishMeaning: String, val adverts: List<FlatAdvert>, val right: Int)

/**
 * A Round of Flat hunt, chapter 19's Game, after the book's adverts. Someone says what flat or room
 * they are looking for, *Mam psa. Szukam mieszkania na parterze.*, and the Learner reads three
 * adverts and taps the one that fits. Once answered the wish is said, and a note gives what it and
 * the right advert mean. The result is the hunts right first try.
 */
data class FlatHuntRound(
    override val played: List<String>,
    val run: Run<FlatHuntQuestion>,
) : Round {
    override val game: Game get() = Game.FLAT_HUNT
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The hunt being asked, or null once the Round is finished. */
    val question: FlatHuntQuestion? get() = run.current

    /** The answer to the hunt being asked, until the Learner moves on: the index of the advert tapped. */
    val given: Given? get() = run.given

    /** The advert tapped for the hunt being asked, until the Learner moves on. */
    val chosen: Int? get() = given?.text?.toIntOrNull()

    /** Taps the advert at [index]. No change for no such advert, or once answered. */
    fun choose(index: Int): FlatHuntRound {
        val question = question ?: return this
        if (index !in question.adverts.indices) return this
        return copy(run = run.answer(index.toString(), index == question.right))
    }

    /** Moves on from an answered hunt. */
    fun next(): FlatHuntRound = copy(run = run.next())
}

/**
 * Flat hunt: the hunts of `flatHunt` in practice.json. A hunt counts once every Item of its wish
 * and its adverts is learned, and the Game opens with [FEWEST]. A Round asks up to [QUESTIONS],
 * those not asked today first, each with its adverts in a new order.
 */
internal object FlatHunt : GameRules {
    /** How many hunts a Round asks, at most. */
    const val QUESTIONS = 4

    /** How many hunts must be learned for the Game to open. */
    const val FEWEST = 3

    /** How many adverts each hunt has. */
    const val ADVERTS = 3

    override val game = Game.FLAT_HUNT

    private fun hunts(learned: Learned): List<FlatHuntCase> {
        val known = learned.items.map { it.id }.toSet()
        return learned.course.flatHunt.filter { hunt -> (listOf(hunt.wish) + hunt.adverts).all { known.containsAll(it.items) } }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (hunts(learned).size >= FEWEST) GameState.READY else GameState.NEEDS_MORE

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val questions = random.draw(hunts(learned), QUESTIONS, seen, avoidFirst) { it.id }.map { hunt ->
            val order = hunt.adverts.indices.shuffled(random)
            FlatHuntQuestion(
                key = hunt.id,
                wish = hunt.wish.text,
                wishMeaning = learned.localize(hunt.wish.meaning),
                adverts = order.map { FlatAdvert(hunt.adverts[it].text, learned.localize(hunt.adverts[it].meaning)) },
                right = order.indexOf(hunt.right),
            )
        }
        return FlatHuntRound(selected, Run(questions))
    }
}
