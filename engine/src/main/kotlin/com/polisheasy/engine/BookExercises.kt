package com.polisheasy.engine

import kotlin.random.Random
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The Exercises the book sets on its pages, beside the Daily Set's own: fill the gaps from sticky
// notes, match the pairs, true or false about a text, and put a dialogue in order.

/**
 * The Learner fills the gaps of [text], each written `___`, from sticky notes or by typing. The
 * notes are the first Accepted Answer of each gap and the [distractors], mixed; like word tiles
 * they are never authored apart from the answers.
 */
@Serializable
@SerialName("fill_gaps")
data class FillGaps(
    override val id: String,
    /** Polish with 1 to 4 gaps, each written `___`. */
    val text: String,
    /** The whole text in each Base Language, gaps filled. */
    val meaning: Localized,
    /** For each gap in order, every word treated as correct. The first is on a note and shown when the Learner is wrong. */
    val gaps: List<List<String>>,
    /** Further notes that fit no gap. */
    val distractors: List<String> = emptyList(),
    override val items: List<String> = emptyList(),
) : Exercise

/** One pair of a [MatchPairs]: a Polish word or phrase and what it means. */
@Serializable
data class MatchPair(val polish: String, val meaning: Localized)

/** The Learner joins each Polish word to its meaning, a line at a time. */
@Serializable
@SerialName("match_pairs")
data class MatchPairs(
    override val id: String,
    /** 4 to 6 pairs. */
    val pairs: List<MatchPair>,
    override val items: List<String> = emptyList(),
) : Exercise

/** A statement about the text of a [TrueFalse], in Polish, with its [meaning] and whether it is [true]. */
@Serializable
data class Statement(val text: String, val meaning: Localized, val `true`: Boolean)

/**
 * The Learner reads [lines], or with [listen] hears them, a Clip a line, and marks each of the
 * [statements] P (prawda, true) or N (nieprawda, false). Heard, its reading form shows the lines.
 */
@Serializable
@SerialName("true_false")
data class TrueFalse(
    override val id: String,
    /** The Polish text, a line or a speaker's turn each. */
    val lines: List<String>,
    /** What the text is about, in each Base Language, said above it. */
    val meaning: Localized,
    /** 3 to 5. */
    val statements: List<Statement>,
    /** Whether the lines are heard instead of read, while the Learner can listen. */
    val listen: Boolean = false,
    override val items: List<String> = emptyList(),
) : Exercise

/** The Learner puts the [lines] of a short dialogue in order. Once answered, the finished dialogue plays. */
@Serializable
@SerialName("order_lines")
data class OrderLines(
    override val id: String,
    /** 3 to 6 lines, in the right order. */
    val lines: List<String>,
    /** What the dialogue is, in each Base Language: the situation the Learner orders it by. */
    val meaning: Localized,
    override val items: List<String> = emptyList(),
) : Exercise

/** A statement of a true-or-false Exercise as the Learner sees it: the Polish and, a tap away, its meaning. */
data class StatementView(val text: String, val meaning: String)

/** The most gaps a fill-the-gaps text may have. */
internal const val MAX_GAPS = 4

private val gapRun = Regex("_+")

/**
 * The text of [text] around its gaps: one more part than there are gaps, spaces kept. Null when a
 * run of underscores is not a gap of exactly three.
 */
internal fun gapParts(text: String): List<String>? {
    val runs = gapRun.findAll(text).toList()
    if (runs.any { it.value != GAP }) return null
    val parts = mutableListOf<String>()
    var start = 0
    runs.forEach { run ->
        parts += text.substring(start, run.range.first)
        start = run.range.last + 1
    }
    parts += text.substring(start)
    return parts
}

/** [text] with [words] in its gaps, in order. */
internal fun filledGaps(text: String, words: List<String>): String {
    val parts = gapParts(text) ?: return text
    return buildString {
        parts.forEachIndexed { index, part ->
            append(part)
            if (index < parts.lastIndex) append(words.getOrElse(index) { GAP })
        }
    }
}

/** [authored] mixed by [seed], and never in the order written when it has two things that differ. */
internal fun <T> mixed(authored: List<T>, seed: String): List<T> {
    val shuffled = authored.shuffled(Random(seed.hashCode()))
    return if (shuffled == authored) shuffled.drop(1) + shuffled.take(1) else shuffled
}

/** The sticky notes of [exercise] as the Learner sees them: each gap's word and the distractors, mixed. */
internal fun notesOf(exercise: FillGaps): List<String> =
    mixed(exercise.gaps.map { it.first() } + exercise.distractors, exercise.id)

/** The meanings of [exercise] in the order the right column shows them. */
internal fun meaningOrder(exercise: MatchPairs): List<Int> = mixed(exercise.pairs.indices.toList(), exercise.id)

/** The lines of [exercise] mixed, as the Learner first sees them. */
internal fun mixedLines(exercise: OrderLines): List<String> = mixed(exercise.lines, exercise.id)

/** What P and N say in Feedback: the marks the book uses, *prawda* and *nieprawda*. */
internal fun mark(value: Boolean): String = if (value) ClassAudio.TRUE_MARK else ClassAudio.FALSE_MARK

/** Checks the words in the gaps of [exercise], each like a typed answer. Wrong unless every gap is right. */
internal fun checkGaps(exercise: FillGaps, words: List<String>): Feedback {
    val each = exercise.gaps.mapIndexed { index, accepted -> checkTyped(words.getOrElse(index) { "" }, accepted) }
    return Feedback(
        correct = each.all { it.correct },
        correctAnswer = filledGaps(exercise.text, each.map { it.correctAnswer }),
        accentWarning = each.all { it.correct } && each.any { it.accentWarning },
        parts = each.map { it.correct },
    )
}

/** Right when every statement of [exercise] is marked as it should be. */
internal fun checkMarks(exercise: TrueFalse, marks: List<Boolean>): Feedback {
    val each = exercise.statements.mapIndexed { index, statement -> marks.getOrNull(index) == statement.`true` }
    return Feedback(
        correct = each.all { it },
        correctAnswer = exercise.statements.mapIndexed { index, it -> "${index + 1} ${mark(it.`true`)}" }.joinToString(" · "),
        parts = each,
    )
}

/** Right when the lines of [exercise] are in the order written, punctuation and case aside. */
internal fun checkOrder(exercise: OrderLines, lines: List<String>): Feedback {
    val each = exercise.lines.mapIndexed { index, line -> lines.getOrNull(index)?.let(::normalizeAnswer) == normalizeAnswer(line) }
    return Feedback(correct = each.all { it } && lines.size == exercise.lines.size, correctAnswer = exercise.lines.joinToString("\n"), parts = each)
}
