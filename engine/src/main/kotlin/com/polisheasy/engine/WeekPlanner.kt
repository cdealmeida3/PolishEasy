package com.polisheasy.engine

import kotlin.random.Random

/**
 * One plan of Week planner, a line of a diary: [text] (*W piątek o dwudziestej: impreza.*) and
 * what it means, its [day] (the day's Item), its time, [hour] 0 to 23 and [minute], and the
 * [items] it uses, which must be learned for it to be played.
 */
data class WeekPlan(val text: String, val meaning: Localized, val day: String, val hour: Int, val minute: Int, val items: List<String>) {
    /** What is planned, written in the diary: the Polish after the colon, *impreza*. */
    val what: String get() = text.substringAfter(": ").trimEnd('.', '!')

    /** The time as a diary writes it: 20:00, 6:30. */
    val time: String get() = clockTime(hour, minute)
}

/** A time as a diary writes it: 20:00, 6:30. */
internal fun clockTime(hour: Int, minute: Int): String = "$hour:${minute.toString().padStart(2, '0')}"

/** What Week planner's diary is: the Items of the days, Monday first, and the plans. */
data class Week(val days: List<String>, val plans: List<WeekPlan>)

/**
 * One question of Week planner: a plan, said or written in [text] with what it [means], on the
 * diary's day [day] (0 is Monday) at [time], which is among the four [times]. [what] is written
 * in the diary once it is answered.
 */
data class PlanQuestion(val key: String, val text: String, val meaning: String, val day: Int, val time: String, val what: String, val times: List<String>)

/**
 * A Round of Week planner, chapter 12's second Game, after the book's diary. A plan is said, or
 * with [listen] off written: *W piątek o dwudziestej: impreza.* The Learner taps its day in the
 * diary of [days] (Monday first), then its time from four, and it is written in. Each plan is on a
 * day of its own. The result is the plans put in the right day at the right time first try.
 */
data class PlannerRound(
    override val played: List<String>,
    val listen: Boolean,
    val days: List<String>,
    val run: Run<PlanQuestion>,
    /** The day tapped for the plan being asked, before its time is chosen. */
    val day: Int? = null,
) : Round {
    override val game: Game get() = Game.WEEK_PLANNER
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The plan being asked, or null once the Round is finished. */
    val question: PlanQuestion? get() = run.current

    /** The answer to the plan being asked, until the Learner moves on: the day and time chosen. */
    val given: Given? get() = run.given

    /** The plans written in the diary: those asked before, and the one being asked once answered, each in its own day. */
    val written: List<PlanQuestion> get() = run.questions.take(run.index + if (given != null) 1 else 0)

    /** Taps the day at [index] for the plan being asked; another tap changes it. No change for no such day, or once answered. */
    fun pickDay(index: Int): PlannerRound {
        if (question == null || given != null || index !in days.indices) return this
        return copy(day = index)
    }

    /** Chooses the time at [option] for the day tapped. No change before a day is tapped, for no such time, or once answered. */
    fun pickTime(option: Int): PlannerRound {
        val question = question ?: return this
        val day = day ?: return this
        if (given != null) return this
        val time = question.times.getOrNull(option) ?: return this
        return copy(run = run.answer("${days[day]} $time", day == question.day && time == question.time))
    }

    /** Moves on from an answered plan. */
    fun next(): PlannerRound = if (given == null) this else copy(run = run.next(), day = null)
}

/**
 * Week planner: the diary of `week` in practice.json. It opens once every day is learned and
 * [PLANS] plans have all their Items learned, on [PLANS] different days.
 */
internal object WeekPlanner : GameRules {
    const val WAY = "plans"
    const val LISTEN = "listen"
    const val READ = "read"

    /** How many plans a Round asks, each on a day of its own, and how many times each offers. */
    const val PLANS = 4
    const val TIMES = 4

    override val game = Game.WEEK_PLANNER

    private fun days(learned: Learned): List<Item>? {
        val week = learned.course.week ?: return null
        val known = learned.items.associateBy { it.id }
        return week.days.map { known[it] ?: return null }
    }

    private fun plans(learned: Learned, listen: Boolean): List<WeekPlan> {
        val week = learned.course.week ?: return emptyList()
        val known = learned.items.map { it.id }.toSet()
        return week.plans.filter { known.containsAll(it.items) && it.day in week.days }
            .filter { !listen || learned.course.clipFor(it.text) != null }.distinctBy { it.text }
    }

    private fun enough(plans: List<WeekPlan>) = plans.map { it.day }.distinct().size >= PLANS

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (days(learned) != null && enough(plans(learned, listen = false))) GameState.READY else GameState.NEEDS_MORE

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val ways = listOfNotNull(LISTEN.takeIf { sound && enough(plans(learned, listen = true)) }, READ)
        return listOf(choice(WAY, ways.map { OptionView(it) }, wanted.getOrNull(0)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val listen = selected[0] == LISTEN
        val days = checkNotNull(days(learned))
        val all = plans(learned, listen)
        // One plan a day: those not asked today first, then any.
        val order = random.draw(all, all.size, seen, avoidFirst) { it.text }
        val chosen = order.distinctBy { it.day }.take(PLANS)
        val questions = chosen.map { plan ->
            val right = plan.time
            // The times a slip away: an hour before or after, half an hour either side.
            val alike = listOf(plan.hour - 1 to plan.minute, plan.hour + 1 to plan.minute, plan.hour to (plan.minute + 30) % 60, plan.hour - 1 to (plan.minute + 30) % 60, plan.hour + 1 to 0)
                .filter { (hour, _) -> hour in 0..23 }.map { (hour, minute) -> clockTime(hour, minute) }.filter { it != right }.distinct().shuffled(random)
            PlanQuestion(
                key = plan.text,
                text = plan.text,
                meaning = learned.localize(plan.meaning),
                day = days.indexOfFirst { it.id == plan.day },
                time = right,
                what = plan.what,
                times = (alike.take(TIMES - 1) + right).shuffled(random),
            )
        }
        return PlannerRound(selected, listen, days.map { it.text }, Run(questions))
    }
}
