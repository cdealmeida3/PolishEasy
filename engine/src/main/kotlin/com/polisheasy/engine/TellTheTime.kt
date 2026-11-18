package com.polisheasy.engine

import kotlin.random.Random

/**
 * What the content names for telling the time, for Tell the time: the Items of the hours one to twelve,
 * in order, each with the form that goes after *po* and *wpół do* (*ósma*, *ósmej*), and the
 * Items that say past (*po*), to (*za*), half to (*wpół do*) and a quarter (*kwadrans*).
 */
data class ClockWords(val hours: List<String>, val past: String, val to: String, val half: String, val quarter: String)

/** A time on a clock face, [hour] 1 to 12 and [minute], and how it is said in Polish: *pięć po ósmej*. */
data class SaidTime(val hour: Int, val minute: Int, val polish: String) {
    val key: String get() = "$hour:$minute"
}

/** One question of Tell the time: the [time] asked, and the four [options] to answer it with, the time among them. */
data class TimeQuestion(val time: SaidTime, val options: List<SaidTime>)

/**
 * A Round of Tell the time, chapter 12's Game. With [read] on a clock face shows a time and the Learner
 * says it, from four: *wpół do dziewiątej*, *wpół do ósmej*, *za pięć ósma*. With it off the time
 * is said and written, and the Learner finds it among four clock faces. The result is the answers
 * right first try.
 */
data class TimeRound(
    override val played: List<String>,
    val read: Boolean,
    val run: Run<TimeQuestion>,
) : Round {
    override val game: Game get() = Game.TELL_THE_TIME
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.time.key }

    /** The question being shown, or null once the Round is finished. */
    val question: TimeQuestion? get() = run.current

    /** The answer to the question being shown, until the Learner moves on. Its text is the key of the time chosen. */
    val given: Given? get() = run.given

    /** Chooses the option at [option]. No change for no such option, or once answered. */
    fun choose(option: Int): TimeRound {
        val question = question ?: return this
        val chosen = question.options.getOrNull(option) ?: return this
        return copy(run = run.answer(chosen.key, chosen.key == question.time.key))
    }

    /** Moves on from an answered question. */
    fun next(): TimeRound = copy(run = run.next())
}

/**
 * Tell the time: the hours and words of `clock` in practice.json, with the numbers five, ten and
 * twenty. It opens once four hours are learned, and asks the times that can be said in learned
 * words: on the hour at first, then past, half to and to the hour as their words are learned.
 */
internal object TellTheTime : GameRules {
    const val WAY = "clock"
    const val READ = "tell"
    const val FIND = "find-clock"

    /** How many times a Round asks at most, and how many options each shows. */
    const val QUESTIONS = 8
    const val OPTIONS = 4

    /** The fewest hours the Game opens with. */
    const val LEAST = 4

    override val game = Game.TELL_THE_TIME

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (times(learned).map { it.hour }.distinct().size >= LEAST) GameState.READY else GameState.NEEDS_MORE

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> =
        listOf(choice(WAY, listOf(OptionView(READ), OptionView(FIND)), wanted.getOrNull(0)))

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val all = times(learned)
        val byKey = all.associateBy { it.key }
        val asked = random.draw(all, QUESTIONS, seen, avoidFirst) { it.key }
        val questions = asked.map { time ->
            // The times one slip away first: always the next hour (wpół do dziewiątej for wpół do ósmej), then the hour
            // before, past for to, or the hour itself.
            val next = time.hour % 12 + 1
            val before = (time.hour + 10) % 12 + 1
            val slips = listOf("$before:${time.minute}", "${time.hour}:${(60 - time.minute) % 60}", "${time.hour}:0", "$next:0").shuffled(random)
            val alike = (listOf("$next:${time.minute}") + slips).mapNotNull(byKey::get).filter { it.key != time.key }.distinctBy { it.key }
            val others = all.filter { it.key != time.key && it !in alike }.shuffled(random)
            TimeQuestion(time, ((alike + others).take(OPTIONS - 1) + time).shuffled(random))
        }
        return TimeRound(selected, selected[0] == READ, Run(questions))
    }

    private fun times(learned: Learned): List<SaidTime> {
        val known = learned.items.associateBy { it.id }
        return learned.course.clockTimes(known::get, learned.numbers())
    }
}

/** The minutes Tell the time shows: on the hour, five, ten, a quarter and twenty past, half past, and twenty, a quarter, ten and five to. */
internal val CLOCK_MINUTES = listOf(0, 5, 10, 15, 20, 30, 40, 45, 50, 55)

/**
 * Every time on a clock face that can be said with the Items [item] finds and the [numbers]: on
 * the hour *ósma*; past *pięć po ósmej*, *kwadrans po ósmej*; half *wpół do dziewiątej*; to *za
 * dwadzieścia dziewiąta*, *za kwadrans dziewiąta*.
 */
internal fun Course.clockTimes(item: (String) -> Item?, numbers: Map<Int, Item>): List<SaidTime> {
    val words = clock ?: return emptyList()
    val hours = words.hours.mapIndexedNotNull { index, id -> item(id)?.takeIf { it.forms.isNotEmpty() }?.let { index + 1 to it } }.toMap()
    fun minutes(minute: Int): String? = if (minute == 15) item(words.quarter)?.text else numbers[minute]?.text
    return hours.keys.sorted().flatMap { hour ->
        val now = hours.getValue(hour)
        val next = hours[hour % 12 + 1]
        CLOCK_MINUTES.mapNotNull { minute ->
            val said = when {
                minute == 0 -> listOf(now.text)
                minute < 30 -> listOf(minutes(minute), item(words.past)?.text, now.forms.first())
                minute == 30 -> listOf(item(words.half)?.text, next?.forms?.first())
                else -> listOf(item(words.to)?.text, minutes(60 - minute), next?.text)
            }
            if (said.any { it == null }) null else SaidTime(hour, minute, said.joinToString(" "))
        }
    }
}

/** Every time Tell the time can show, said with every Item of the course. */
internal fun Course.allClockTimes(): List<SaidTime> {
    val numbers = items.mapNotNull { item -> (pictures[item.id] as? Picture.Glyph)?.text?.toIntOrNull()?.let { it to item } }.reversed().toMap()
    return clockTimes(itemsById::get, numbers)
}

/** What Tell the time says, every time it can show. Each needs a Clip. */
internal fun Course.clockPhrases(): List<String> = allClockTimes().map { it.polish }
