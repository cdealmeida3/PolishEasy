package com.polisheasy.engine

import kotlin.random.Random

/**
 * Pronoun grid's table in the content: the [persons] down its side, Items such as *ja*, *ty*,
 * *on*, and its [columns].
 */
data class PronounTable(val persons: List<String>, val columns: List<PronounColumn>)

/**
 * One column of Pronoun grid's table: its stable [id], the Polish [question] at its head
 * (*z kim?*), what that means, and the Item that answers it for each person (*ze mną*), null
 * where the Path teaches none.
 */
data class PronounColumn(val id: String, val question: String, val meaning: Localized, val cells: List<String?>)

/**
 * A column as a Round shows it: its [question] (*z kim?*), what it means without the example in
 * brackets, and the form for each person, null where it is not learned.
 */
data class GridColumn(val id: String, val question: String, val meaning: String, val cells: List<String?>)

/** One square of Pronoun grid to fill: the [person]'s row, the [column], the right [form] and the forms [offered]. */
data class GridQuestion(val key: String, val person: Int, val column: Int, val form: String, val offered: List<String>)

/** What a square of the table holds now. */
enum class GridSquare {
    /** No form is learned for it: it stays empty and is not asked. */
    NONE,

    /** Still to be asked. */
    WAITING,

    /** The one being asked. */
    ASKED,

    /** Filled right first try. */
    RIGHT,

    /** Filled after a wrong answer. */
    MISSED,
}

/**
 * A Round of Pronoun grid, chapter 17's Game. The table of the pronouns is shown with its
 * persons down the side (*ja*, *ty*, *on*…) and one or two columns, each headed by its question
 * (*komu?*, *z kim?*); every square whose form is learned is empty. One square at a time is
 * asked, and the Learner taps its form from those offered: *ona* and *z kim?* is *z nią*. An
 * answered square keeps its form. The result is the squares right first try.
 */
data class PronounGridRound(
    override val played: List<String>,
    /** The persons down the table's side, as written: ja, ty, on, ona, my, wy, oni. */
    val persons: List<String>,
    val columns: List<GridColumn>,
    val run: Run<GridQuestion>,
) : Round {
    override val game: Game get() = Game.PRONOUN_GRID
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The square being asked, or null once the Round is finished. */
    val question: GridQuestion? get() = run.current

    /** The answer to the square being asked, until the Learner moves on: the form tapped. */
    val given: Given? get() = run.given

    /** What the square of [person] under [column] holds now. */
    fun square(person: Int, column: Int): GridSquare {
        if (columns.getOrNull(column)?.cells?.getOrNull(person) == null) return GridSquare.NONE
        val at = run.questions.indexOfFirst { it.person == person && it.column == column }
        return when {
            at < 0 -> GridSquare.NONE
            at < run.results.size -> if (run.results[at]) GridSquare.RIGHT else GridSquare.MISSED
            at == run.index -> GridSquare.ASKED
            else -> GridSquare.WAITING
        }
    }

    /** Taps the form at [option] of those offered. No change for no such form, or once answered. */
    fun choose(option: Int): PronounGridRound {
        val question = question ?: return this
        val form = question.offered.getOrNull(option) ?: return this
        return copy(run = run.answer(form, form == question.form))
    }

    /** Moves on from an answered square. */
    fun next(): PronounGridRound = copy(run = run.next())
}

/**
 * Pronoun grid: the table of `pronounGrid` in practice.json. A column is ready once [FEWEST] of
 * its forms are learned, and the Game opens with the first ready column. Ways to play: one
 * column, or, with two ready, [MIXED], which shows two of them side by side. A Round asks every
 * learned square of the columns shown, in a random order, each with [OFFERED] forms to choose
 * from: the right one and others of the table, first those of the same column.
 */
internal object PronounGrid : GameRules {
    const val WAY = "pronouns"
    const val MIXED = "mixed"

    /** How many forms of a column must be learned for it to be played. */
    const val FEWEST = 4

    /** How many forms each square offers. */
    const val OFFERED = 4

    /** How many columns [MIXED] shows. */
    const val MIXED_COLUMNS = 2

    override val game = Game.PRONOUN_GRID

    private fun columns(learned: Learned): List<GridColumn> {
        val known = learned.items.associateBy { it.id }
        return learned.course.pronounGrid?.columns.orEmpty().map { column ->
            // The meaning without its example in brackets, which a note under the table has no room for.
            GridColumn(column.id, column.question, learned.localize(column.meaning).substringBefore(" (").trim(), column.cells.map { id -> id?.let(known::get)?.text })
        }.filter { column -> column.cells.count { it != null } >= FEWEST }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (columns(learned).isNotEmpty()) GameState.READY else GameState.NEEDS_MORE

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val ready = columns(learned)
        val options = ready.map { OptionView(it.id, it.question) } + listOfNotNull(OptionView(MIXED).takeIf { ready.size >= MIXED_COLUMNS })
        return listOf(choice(WAY, options, wanted.getOrNull(0)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val ready = columns(learned)
        val shown = when (val way = selected.getOrNull(0)) {
            MIXED -> ready.shuffled(random).take(MIXED_COLUMNS).sortedBy(ready::indexOf)
            else -> listOf(ready.firstOrNull { it.id == way } ?: ready.first())
        }
        val persons = learned.course.pronounGrid?.persons.orEmpty().map { learned.course.itemsById[it]?.text ?: it }
        val squares = shown.flatMapIndexed { column, it -> it.cells.mapIndexedNotNull { person, form -> form?.let { Triple(person, column, it) } } }
        val key = { square: Triple<Int, Int, String> -> "${shown[square.second].id}:${persons.getOrElse(square.first) { "${square.first}" }}" }
        val questions = random.draw(squares, squares.size, seen, avoidFirst, key).map { square ->
            val (person, column, form) = square
            val sameColumn = shown[column].cells.filterNotNull()
            val others = shown.flatMap { it.cells.filterNotNull() }
            val wrong = (sameColumn.shuffled(random) + others.shuffled(random)).distinct().filter { it != form }.take(OFFERED - 1)
            GridQuestion(key(square), person, column, form, (wrong + form).shuffled(random))
        }
        return PronounGridRound(selected, persons, shown, Run(questions))
    }
}
