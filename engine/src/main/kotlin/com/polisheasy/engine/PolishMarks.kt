package com.polisheasy.engine

/** What stands on each side of Polish in a Tip's body: "*Dziękuję* is thank you". */
const val POLISH_MARK = '*'

private const val POLISH_LETTERS = "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ"
private val WORD = Regex("\\p{L}+")

/** A Tip's body as it is read, without its marks, and where in it the Polish is. */
data class MarkedText(val text: String, val polish: List<IntRange>)

/**
 * Reads the marks out of [marked]: the text between two [POLISH_MARK]s is Polish. A mark with no
 * partner marks nothing, which [marksProblem] reports before a course is shipped.
 */
fun readPolishMarks(marked: String): MarkedText {
    val text = StringBuilder()
    val polish = mutableListOf<IntRange>()
    var opened: Int? = null
    marked.forEach { char ->
        if (char != POLISH_MARK) {
            text.append(char)
        } else {
            val start = opened
            if (start == null) {
                opened = text.length
            } else {
                if (text.length > start) polish += start until text.length
                opened = null
            }
        }
    }
    return MarkedText(text.toString(), polish)
}

/**
 * The first thing wrong with the marks of a Tip's body, as the end of a sentence, or null: a mark
 * without its partner, a pair with nothing or only spaces at an edge between them, or a word
 * spelt with a Polish letter that stands outside every pair.
 */
fun marksProblem(marked: String): String? {
    if (marked.count { it == POLISH_MARK } % 2 != 0) return "has a $POLISH_MARK without its partner"
    val (text, polish) = readPolishMarks(marked)
    if (marked.contains("$POLISH_MARK$POLISH_MARK")) return "has a pair of $POLISH_MARK with nothing between them"
    polish.firstOrNull { text[it.first].isWhitespace() || text[it.last].isWhitespace() }?.let {
        return "marks \"${text.substring(it)}\" with a space at its edge"
    }
    WORD.findAll(text).firstOrNull { word -> word.value.any { it in POLISH_LETTERS } && polish.none { word.range.first in it } }?.let {
        return "has the Polish \"${it.value}\" outside the marks: write $POLISH_MARK${it.value}$POLISH_MARK"
    }
    return null
}
