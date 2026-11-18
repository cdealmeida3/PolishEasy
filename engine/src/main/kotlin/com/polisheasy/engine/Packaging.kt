package com.polisheasy.engine

import kotlin.random.Random

/**
 * One phrase of Packaging match: [text] (*puszka coli*) with what it means, the [container] Item it
 * starts with (*puszka*), the other containers that [also] go with what is in it (*butelka coli*),
 * and the [items] it uses.
 */
data class PackagePhrase(val text: String, val meaning: Localized, val container: String, val also: List<String>, val items: List<String>) {
    /** What is in the container, in the genitive: the phrase after its first word, *coli*. */
    val thing: String get() = text.substringAfter(' ')
}

/** A container offered in Packaging match: its Item and its word, *puszka*. */
data class Container(val item: String, val text: String)

/** One question of Packaging match: the phrase, what is in it, the containers [offered] and which of them is [right]. */
data class PackageQuestion(
    val key: String,
    val text: String,
    val thing: String,
    val meaning: String,
    val offered: List<Container>,
    val right: Int,
)

/**
 * A Round of Packaging match, chapter 15's Game. A gap and what is in it is written, *___ coli*,
 * and the Learner taps the container it comes in: *puszka*, *słoik*, *kostka*, *tabliczka*. Only one
 * of those offered goes with it. Once answered the whole phrase is written and said, *puszka coli*.
 * The result is the answers right first try.
 */
data class PackagingRound(
    override val played: List<String>,
    val run: Run<PackageQuestion>,
) : Round {
    override val game: Game get() = Game.PACKAGING
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The phrase being asked, or null once the Round is finished. */
    val question: PackageQuestion? get() = run.current

    /** The answer to the phrase being asked, until the Learner moves on: the container tapped. */
    val given: Given? get() = run.given

    /** Taps the container at [option] of those offered. No change for no such container, or once answered. */
    fun choose(option: Int): PackagingRound {
        val question = question ?: return this
        val container = question.offered.getOrNull(option) ?: return this
        return copy(run = run.answer(container.text, option == question.right))
    }

    /** Moves on from an answered phrase. */
    fun next(): PackagingRound = copy(run = run.next())
}

/**
 * Packaging match: the phrases of `packaging` in practice.json. It opens once [FEWEST] phrases have
 * all their Items learned and [OFFERED] of their containers are learned.
 */
internal object PackagingMatch : GameRules {
    /** How many phrases a Round asks, at most, and how many must be learned for it to open. */
    const val QUESTIONS = 8
    const val FEWEST = 4

    /** How many containers each question offers, the right one among them. */
    const val OFFERED = 4

    override val game = Game.PACKAGING

    private fun phrases(learned: Learned): List<PackagePhrase> {
        val known = learned.items.map { it.id }.toSet()
        return learned.course.packaging.filter { known.containsAll(it.items) && it.container in known }
    }

    /** The containers of the phrases whose Item is learned, once each. */
    private fun containers(learned: Learned): List<Container> {
        val known = learned.items.map { it.id }.toSet()
        return learned.course.packaging.map { it.container }.distinct().filter { it in known }
            .mapNotNull(learned.course.itemsById::get).map { Container(it.id, it.text) }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (phrases(learned).size >= FEWEST && containers(learned).size >= OFFERED) GameState.READY else GameState.NEEDS_MORE

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val containers = containers(learned)
        val questions = random.draw(phrases(learned), QUESTIONS, seen, avoidFirst) { it.text }.map { phrase ->
            val right = containers.single { it.item == phrase.container }
            val wrong = containers.filter { it.item != phrase.container && it.item !in phrase.also }.shuffled(random).take(OFFERED - 1)
            val offered = (wrong + right).shuffled(random)
            PackageQuestion(
                key = phrase.text,
                text = phrase.text,
                thing = phrase.thing,
                meaning = learned.localize(phrase.meaning),
                offered = offered,
                right = offered.indexOf(right),
            )
        }
        return PackagingRound(selected, Run(questions))
    }
}
