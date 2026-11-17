package com.polisheasy.engine

import kotlin.random.Random

/** What a question of Numbers asks for. */
enum class NumberKind {
    /** One number. */
    SINGLE,

    /** Three digits in a row, as in a telephone number. */
    RUN,

    /** Two numbers with "plus" between them: the answer is what they come to. */
    SUM,
}

/**
 * One question of Numbers. [words] are the Polish words said, or read, in order. [answer] is what
 * is typed, in digits. [numerals] is the question as it is written once answered, such as "14",
 * "4 7 2" or "2 + 6 = 8".
 */
data class NumberQuestion(
    val kind: NumberKind,
    val words: List<String>,
    val answer: String,
    val numerals: String,
    internal val key: String = "",
) {
    /** A run is typed digit for digit, zeros included; a number may have a zero in front. */
    val exact: Boolean get() = kind == NumberKind.RUN

    /** The words as one line of Polish. */
    val polish: String get() = words.joinToString(" ")
}

/**
 * A Round of Numbers: a number, a run of three or a sum is said, or with [listen] off written in
 * words, and the Learner types it in digits on a keypad. [typed] is what is on the keypad's
 * display. The result is the answers right first try.
 */
data class NumberRound(
    override val played: List<String>,
    val listen: Boolean,
    val run: Run<NumberQuestion>,
    val typed: String = "",
) : Round {
    override val game: Game get() = Game.NUMBERS
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The question being shown, or null once the Round is finished. */
    val question: NumberQuestion? get() = run.current

    /** The answer to the question being shown, until the Learner moves on. Its text is the digits typed. */
    val given: Given? get() = run.given

    /** Whether there is something typed to check. */
    val canCheck: Boolean get() = given == null && typed.isNotEmpty() && question != null

    /** Types [digit]. No change if it is not a digit, the display is full or the question is answered. */
    fun press(digit: Int): NumberRound =
        if (digit !in 0..9 || given != null || question == null || typed.length >= Numbers.MOST_DIGITS) this else copy(typed = typed + digit)

    /** Takes back the last digit typed. */
    fun erase(): NumberRound = if (given != null) this else copy(typed = typed.dropLast(1))

    /** Answers with what is typed. No change if nothing is. */
    fun check(): NumberRound {
        val question = question ?: return this
        if (!canCheck) return this
        val right = if (question.exact) typed == question.answer else typed.toInt() == question.answer.toInt()
        return copy(run = run.answer(typed, right))
    }

    /** Moves on from an answered question, with the display empty. */
    fun next(): NumberRound = if (given == null) this else copy(run = run.next(), typed = "")
}

/** The number a learned Item stands for: the Items whose Picture is a numeral. */
internal fun Learned.numbers(): Map<Int, Item> =
    items.mapNotNull { item -> (course.pictures[item.id] as? Picture.Glyph)?.text?.toIntOrNull()?.let { it to item } }
        .reversed().toMap().toSortedMap()

/** The learned Item that says "plus": the one whose Picture is the sign. */
internal fun Learned.plus(): Item? = items.firstOrNull { (course.pictures[it.id] as? Picture.Glyph)?.text == "+" }

internal object Numbers : GameRules {
    const val CLUE = "number"
    const val LISTEN = "listen"
    const val READ = "read"
    const val LEVEL = "level"
    const val TO_10 = "to-10"
    const val TO_20 = "to-20"
    const val RUNS = "runs"
    const val SUMS = "sums"

    /** The fewest numbers, digits or sums a level is offered with. */
    const val LEAST = 4

    /** How many questions a Round has, at most: single numbers, runs of three, and sums. */
    const val QUESTIONS = 10
    const val RUN_QUESTIONS = 6
    const val SUM_QUESTIONS = 8

    const val RUN_LENGTH = 3

    /** The most digits the keypad's display holds. */
    const val MOST_DIGITS = 3

    override val game = Game.NUMBERS

    /** The questions each level can ask, for each level with enough of them. Heard, only numbers that have a Clip. */
    private fun levels(learned: Learned, listen: Boolean): List<Pair<String, List<NumberQuestion>>> {
        fun heard(item: Item) = !listen || learned.course.clipFor(item.text) != null
        val numbers = learned.numbers().filterValues(::heard)
        fun single(number: Int) = NumberQuestion(NumberKind.SINGLE, listOf(numbers.getValue(number).text), "$number", "$number", key = "$number")
        val small = numbers.keys.filter { it <= 10 }
        val plus = learned.plus()?.takeIf(::heard)
        val sums = if (plus == null) emptyList() else numbers.keys.filter { it > 0 }.flatMap { a ->
            numbers.keys.filter { b -> b > 0 && a + b in numbers }.map { b ->
                NumberQuestion(NumberKind.SUM, listOf(numbers.getValue(a).text, plus.text, numbers.getValue(b).text), "${a + b}", "$a + $b = ${a + b}", key = "$a+$b")
            }
        }
        // A run is made when the Round starts: here, one question for each digit there is to make runs of.
        val digits = numbers.keys.filter { it <= 9 }
        return listOf(
            TO_10 to small.map(::single),
            // Offered once there is a number above ten to tell it from the level below.
            TO_20 to if (numbers.keys.any { it > 10 }) numbers.keys.map(::single) else emptyList(),
            RUNS to digits.map(::single),
            SUMS to sums,
        ).filter { it.second.size >= LEAST }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (levels(learned, listen = false).isEmpty()) GameState.NEEDS_MORE else GameState.READY

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val clues = listOf(LISTEN, READ).filter { it == READ || (sound && levels(learned, listen = true).isNotEmpty()) }
        val clue = choice(CLUE, clues.map { OptionView(it) }, wanted.getOrNull(0))
        val levels = levels(learned, listen = clue.selected == LISTEN)
        return listOf(clue, choice(LEVEL, levels.map { OptionView(it.first) }, wanted.getOrNull(1)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val listen = selected[0] == LISTEN
        val level = levels(learned, listen).first { it.first == selected[1] }
        val questions = when (level.first) {
            RUNS -> runs(level.second, random, seen, avoidFirst)
            SUMS -> random.draw(level.second, SUM_QUESTIONS, seen, avoidFirst) { it.key }
            else -> random.draw(level.second, QUESTIONS, seen, avoidFirst) { it.key }
        }
        return NumberRound(selected, listen, Run(questions))
    }

    /** Runs of three of [digits], no two the same, none asked today while another can be made, and no run of one digit three times. */
    private fun runs(digits: List<NumberQuestion>, random: Random, seen: Set<String>, avoidFirst: String?): List<NumberQuestion> {
        val runs = mutableListOf<NumberQuestion>()
        var tries = 0
        while (runs.size < RUN_QUESTIONS && tries++ < 200) {
            val drawn = List(RUN_LENGTH) { digits.random(random) }
            val answer = drawn.joinToString("") { it.answer }
            val key = "run:$answer"
            val fresh = tries > 100 || (key !in seen && (runs.isNotEmpty() || key != avoidFirst))
            if (drawn.distinct().size > 1 && fresh && runs.none { it.key == key }) {
                runs += NumberQuestion(NumberKind.RUN, drawn.flatMap { it.words }, answer, drawn.joinToString(" ") { it.answer }, key = key)
            }
        }
        return runs
    }
}
