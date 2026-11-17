package com.polisheasy.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The book's grammar tables, as a Tip shows them and the Grammar shelf keeps them: endings in red
// boxes, a gender icon over a column, singular and plural, and the cases with their questions.

/** What a Tip is, as the book sets it apart: a plain note, an *Uwaga!* (watch out) or a *Zapamiętaj!* (remember). */
@Serializable
enum class TipKind {
    @SerialName("note")
    NOTE,

    @SerialName("uwaga")
    UWAGA,

    @SerialName("zapamietaj")
    ZAPAMIETAJ,
}

/** The genders the book marks over a column with an icon. [mark] is how the book abbreviates it. */
@Serializable
enum class TableGender(val mark: String) {
    @SerialName("m") MASCULINE("m"),
    @SerialName("f") FEMININE("f"),
    @SerialName("n") NEUTER("n"),

    /** Męskoosobowy: the plural of a group with a man in it. */
    @SerialName("mo") MASCULINE_PERSONAL("mo"),

    /** Niemęskoosobowy: every other plural. */
    @SerialName("nmo") NON_MASCULINE_PERSONAL("nmo"),
}

/** The seven cases, as the book abbreviates them and asks them. */
@Serializable
enum class GrammarCase(val mark: String, val polishName: String, val question: String) {
    @SerialName("M") NOMINATIVE("M.", "mianownik", "kto? co?"),
    @SerialName("D") GENITIVE("D.", "dopełniacz", "kogo? czego?"),
    @SerialName("C") DATIVE("C.", "celownik", "komu? czemu?"),
    @SerialName("B") ACCUSATIVE("B.", "biernik", "kogo? co?"),
    @SerialName("N") INSTRUMENTAL("N.", "narzędnik", "(z) kim? (z) czym?"),
    @SerialName("Msc") LOCATIVE("Msc.", "miejscownik", "o kim? o czym?"),
    @SerialName("W") VOCATIVE("W.", "wołacz", "o!"),
}

/** A column of a grammar table: its [header], such as *l.poj.* or *on*, and the [gender] icon over it. */
@Serializable
data class TableColumn(val header: String? = null, val gender: TableGender? = null)

/**
 * A row of a grammar table: its [case], whose mark and question head it, or a [header] of its own,
 * and its [cells], one per column. An ending is written in square brackets, `student[ka]`, and
 * shown in a red box.
 */
@Serializable
data class TableRow(val case: GrammarCase? = null, val header: String? = null, val cells: List<String>)

/** A grammar table under a Tip, as the book draws one. */
@Serializable
data class GrammarTable(
    /** What the table shows, in each Base Language. */
    val title: Localized = emptyMap(),
    val columns: List<TableColumn> = emptyList(),
    val rows: List<TableRow>,
)

/** One cell as the Learner sees it: its [text] without brackets, and where in it the [ending] is. */
data class TableCellView(val text: String, val ending: IntRange? = null)

data class TableColumnView(val header: String?, val gender: TableGender?)

/** A row's head: the case's mark and [question], or a header. */
data class TableRowView(val header: String?, val question: String?, val case: GrammarCase?, val cells: List<TableCellView>)

/** A grammar table, or a verb's forms, as a Tip shows it. [verb] is set for a conjugation table, whose rows are the persons. */
data class GrammarTableView(
    val title: String?,
    val columns: List<TableColumnView>,
    val rows: List<TableRowView>,
    val verb: String? = null,
)

private val ENDING = Regex("\\[([^\\[\\]]*)]")

/** A cell as written, `student[ka]`, read: its text and the ending in it. Only the first bracket marks. */
internal fun readCell(cell: String): TableCellView {
    val match = ENDING.find(cell) ?: return TableCellView(cell)
    val text = cell.replaceRange(match.range, match.groupValues[1]).replace("[", "").replace("]", "")
    val start = match.range.first
    val ending = match.groupValues[1]
    return TableCellView(text, if (ending.isEmpty()) null else start until start + ending.length)
}

/** The text of [cell] without its brackets. */
internal fun cellText(cell: String): String = cell.replace("[", "").replace("]", "")

/** The first thing about [table] that could not be shown, as the end of a sentence, or null. */
internal fun grammarTableProblem(table: GrammarTable): String? {
    val width = table.rows.firstOrNull()?.cells?.size ?: return "has a table with no rows"
    return when {
        width == 0 -> "has a table row with no cells"
        table.rows.any { it.cells.size != width } -> "has table rows of different lengths"
        table.columns.isNotEmpty() && table.columns.size != width ->
            "has a table with ${table.columns.size} columns and $width cells to a row"
        table.rows.any { it.case != null && it.header != null } -> "has a table row with both a case and a header"
        table.rows.flatMap { it.cells }.any { it.count { c -> c == '[' } != it.count { c -> c == ']' } || it.count { c -> c == '[' } > 1 } ->
            "has a table cell whose ending is not one pair of [ ]"
        table.rows.flatMap { it.cells }.any { cellText(it).isBlank() } -> "has a blank table cell"
        else -> null
    }
}

/** [table] as a Tip shows it, its [title] in the Base Language. */
internal fun viewOf(table: GrammarTable, title: String?): GrammarTableView = GrammarTableView(
    title = title?.takeIf { it.isNotBlank() },
    columns = table.columns.map { TableColumnView(it.header, it.gender) },
    rows = table.rows.map { row ->
        TableRowView(row.header ?: row.case?.mark, row.case?.question, row.case, row.cells.map(::readCell))
    },
)

/** A verb's forms as a grammar table: a row for each person, headed by the pronouns. */
internal fun viewOf(table: ConjugationTable, meaning: String): GrammarTableView = GrammarTableView(
    title = "${table.verb} · $meaning",
    columns = emptyList(),
    rows = Person.entries.map { person -> TableRowView(person.pronouns, null, null, listOf(TableCellView(table.forms.getValue(person)))) },
    verb = table.verb,
)

/** One chapter on the Grammar shelf: its table Tips from the sections completed, and how many are still to come. */
data class GrammarChapterView(
    val unitId: String,
    val number: Int?,
    val title: String,
    val accent: Int,
    val tips: List<TipView>,
    val toCome: Int,
)
