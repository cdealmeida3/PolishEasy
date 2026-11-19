package com.polisheasy.engine

import kotlin.random.Random

/**
 * One page of Calendar, `calendar` in practice.json: a [date] as a calendar prints it, *14.02*, the
 * [holiday] on it as the question shows it, *Walentynki*, the ways of saying when it is to choose
 * from, [options], with the [right] one, *czternastego lutego*, and the sentence that [says] so,
 * *Walentynki są czternastego lutego.*
 */
data class CalendarPage(val id: String, val date: String, val holiday: String, val options: List<FlatText>, val right: Int, val says: FlatText)

/** One question of Calendar: the [date] and [holiday] shown, the [options] in the order shown, the [right] one, and the sentence that says so with what it means. */
data class CalendarQuestion(val key: String, val date: String, val holiday: String, val options: List<String>, val right: Int, val says: String, val meaning: String)

/**
 * A Round of Calendar, chapter 21's Game, after the book's calendar of holidays. A page of a
 * calendar shows a date and the holiday on it, *14.02, Walentynki*, and the Learner taps how to
 * say when it is: the ordinal and the month in the genitive, *czternastego lutego*, not
 * *czternasty lutego* nor *czwartego lutego*. Once answered the sentence that says so is said, and
 * a note gives what it means. The result is the pages right first try.
 */
data class CalendarRound(
    override val played: List<String>,
    val run: Run<CalendarQuestion>,
) : Round {
    override val game: Game get() = Game.CALENDAR
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The page being asked, or null once the Round is finished. */
    val question: CalendarQuestion? get() = run.current

    /** The answer to the page being asked, until the Learner moves on: the index of the option tapped. */
    val given: Given? get() = run.given

    /** The option tapped for the page being asked, until the Learner moves on. */
    val chosen: Int? get() = given?.text?.toIntOrNull()

    /** Taps the option at [index]. No change for no such option, or once answered. */
    fun choose(index: Int): CalendarRound {
        val question = question ?: return this
        if (index !in question.options.indices) return this
        return copy(run = run.answer(index.toString(), index == question.right))
    }

    /** Moves on from an answered page. */
    fun next(): CalendarRound = copy(run = run.next())
}

/**
 * Calendar: the pages of `calendar` in practice.json. A page counts once every Item of its
 * sentence and its options is learned, and the Game opens with [FEWEST]. A Round asks up to
 * [QUESTIONS], those not asked today first, each with its options in a new order.
 */
internal object CalendarRules : GameRules {
    /** How many pages a Round asks, at most. */
    const val QUESTIONS = 6

    /** How many pages must be learned for the Game to open. */
    const val FEWEST = 4

    /** How many options each page has. */
    const val OPTIONS = 3

    override val game = Game.CALENDAR

    private fun pages(learned: Learned): List<CalendarPage> {
        val known = learned.items.map { it.id }.toSet()
        return learned.course.calendar.filter { page -> (page.options + page.says).all { known.containsAll(it.items) } }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (pages(learned).size >= FEWEST) GameState.READY else GameState.NEEDS_MORE

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val questions = random.draw(pages(learned), QUESTIONS, seen, avoidFirst) { it.id }.map { page ->
            val order = page.options.indices.shuffled(random)
            CalendarQuestion(
                key = page.id,
                date = page.date,
                holiday = page.holiday,
                options = order.map { page.options[it].text },
                right = order.indexOf(page.right),
                says = page.says.text,
                meaning = learned.localize(page.says.meaning),
            )
        }
        return CalendarRound(selected, Run(questions))
    }
}
