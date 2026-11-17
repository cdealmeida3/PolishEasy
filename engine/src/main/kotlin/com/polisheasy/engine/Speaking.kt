package com.polisheasy.engine

import java.text.Normalizer
import java.util.Locale

/**
 * Whether what a speech recognizer [heard] says one of the [accepted] texts, and if so which.
 *
 * A recognizer is not a keyboard, so this is lenient where typing is strict: case, punctuation and
 * Polish marks never matter, and one word may be near rather than right (*dziekuje* for
 * *dziękuję*, *jestam* for *jestem*): no more than two letters added, left out or changed, and
 * fewer than half of the word. Every other word must be there, in order. [heard] is what the
 * recognizer offers, best first; any of them will do. Null when none says any accepted text.
 */
fun saidAs(heard: List<String>, accepted: List<String>): String? {
    val wanted = accepted.map { it to spokenWords(it) }.filter { it.second.isNotEmpty() }
    heard.map(::spokenWords).filter { it.isNotEmpty() }.forEach { said ->
        wanted.firstOrNull { (_, words) -> near(said, words) }?.let { return it.first }
    }
    return null
}

/** The words of [text] as speech is compared: lower case, no punctuation, no Polish marks. */
internal fun spokenWords(text: String): List<String> =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(marks, "")
        .replace('ł', 'l')
        .replace('Ł', 'l')
        .lowercase(Locale.ROOT)
        .map { if (it.isLetterOrDigit()) it else ' ' }
        .joinToString("")
        .split(' ')
        .filter { it.isNotEmpty() }

private val marks = Regex("\\p{Mn}+")

/** The same words in the same order, with at most one of them only near. */
private fun near(said: List<String>, wanted: List<String>): Boolean {
    if (said.size != wanted.size) return false
    val differing = said.indices.filter { said[it] != wanted[it] }
    if (differing.size > 1) return false
    val index = differing.singleOrNull() ?: return true
    val distance = editDistance(said[index], wanted[index])
    return distance <= MOST_LETTERS && distance * 2 < wanted[index].length
}

/** How many letters, at most, a near word may have added, left out or changed. */
private const val MOST_LETTERS = 2

/** The fewest letters added, left out or changed to make [from] into [to]. */
internal fun editDistance(from: String, to: String): Int {
    var previous = IntArray(to.length + 1) { it }
    from.forEachIndexed { i, a ->
        val current = IntArray(to.length + 1)
        current[0] = i + 1
        to.forEachIndexed { j, b ->
            current[j + 1] = minOf(previous[j + 1] + 1, current[j] + 1, previous[j] + if (a == b) 0 else 1)
        }
        previous = current
    }
    return previous[to.length]
}
