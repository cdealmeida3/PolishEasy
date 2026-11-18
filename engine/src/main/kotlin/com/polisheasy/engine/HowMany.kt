package com.polisheasy.engine

import kotlin.random.Random

/**
 * One phrase of How many?: [text] (*pięć butelek*) with what it means, the counted [item]
 * (*butelka*), how many it [count]s, and the [items] it uses.
 */
data class CountPhrase(val text: String, val meaning: Localized, val item: String, val count: Int, val items: List<String>) {
    /** The number word it starts with: *pięć*. */
    val number: String get() = text.substringBefore(' ')

    /** The word for the things counted: *butelek*. */
    val counted: String get() = text.substringAfter(' ')
}

/** The forms a counted thing takes: one, *butelka*; after *dwa* to *cztery*, *butelki*; from *pięć*, *butelek*. */
object Counting {
    /** How many a phrase may count: as many pictures as fit on the screen. */
    val COUNTS = 2..10

    /** Which of an Item's words follows [count]: its plural, the first of its forms, for two to four, and its genitive plural, the second, from five on. */
    fun formFor(count: Int): Int = if (count in 2..4) 0 else 1
}

/** One question of How many?: the [picture] shown [count] times, the phrases [offered] and which of them is [right]. */
data class CountQuestion(
    val key: String,
    val picture: Picture,
    val count: Int,
    val text: String,
    val meaning: String,
    val offered: List<String>,
    val right: Int,
)

/**
 * A Round of How many?, chapter 15's second Game. A picture is shown two to ten times, five
 * bottles, and the Learner taps the phrase that counts them: *pięć butelek*, not *pięć butelki* or
 * *pięć butelka*. Once answered the phrase is said. The result is the answers right first try.
 */
data class HowManyRound(
    override val played: List<String>,
    val run: Run<CountQuestion>,
) : Round {
    override val game: Game get() = Game.HOW_MANY
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The picture being counted, or null once the Round is finished. */
    val question: CountQuestion? get() = run.current

    /** The answer to the question being asked, until the Learner moves on: the phrase tapped. */
    val given: Given? get() = run.given

    /** Taps the phrase at [option] of those offered. No change for no such phrase, or once answered. */
    fun choose(option: Int): HowManyRound {
        val question = question ?: return this
        val phrase = question.offered.getOrNull(option) ?: return this
        return copy(run = run.answer(phrase, option == question.right))
    }

    /** Moves on from an answered question. */
    fun next(): HowManyRound = copy(run = run.next())
}

/**
 * How many?: the phrases of `counting` in practice.json, each counting an Item with a Picture. It
 * opens once [FEWEST] phrases have all their Items learned.
 */
internal object HowMany : GameRules {
    /** How many phrases a Round asks, at most, and how many must be learned for it to open. */
    const val QUESTIONS = 8
    const val FEWEST = 4

    override val game = Game.HOW_MANY

    private fun phrases(learned: Learned): List<CountPhrase> {
        val known = learned.items.map { it.id }.toSet()
        return learned.course.counting.filter { known.containsAll(it.items) && it.item in learned.course.pictures }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (phrases(learned).size >= FEWEST) GameState.READY else GameState.NEEDS_MORE

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val questions = random.draw(phrases(learned), QUESTIONS, seen, avoidFirst) { it.text }.map { phrase ->
            val item = learned.course.itemsById.getValue(phrase.item)
            val offered = (listOf(item.text) + item.forms.take(2)).map { "${phrase.number} $it" }.distinct().shuffled(random)
            CountQuestion(
                key = phrase.text,
                picture = learned.course.pictures.getValue(phrase.item),
                count = phrase.count,
                text = phrase.text,
                meaning = learned.localize(phrase.meaning),
                offered = offered,
                right = offered.indexOf(phrase.text),
            )
        }
        return HowManyRound(selected, Run(questions))
    }
}
