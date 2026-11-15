package com.polisheasy.engine

import java.text.Normalizer
import java.util.Locale

/**
 * Checks what the Learner typed against the Accepted Answers of an Exercise.
 *
 * Letter case, punctuation and extra whitespace never matter. An answer that matches only once
 * Polish accents are ignored on both sides is correct with an accent warning. Any other
 * difference is wrong: there is no typo tolerance.
 */
internal fun checkTyped(typed: String, acceptedAnswers: List<String>): Feedback {
    val normalized = normalizeAnswer(typed)
    acceptedAnswers.firstOrNull { normalizeAnswer(it) == normalized }
        ?.let { return Feedback(correct = true, correctAnswer = it) }
    val unaccented = stripDiacritics(normalized)
    // Blank input never matches: the loader rejects an Accepted Answer that normalises to nothing.
    acceptedAnswers.firstOrNull { stripDiacritics(normalizeAnswer(it)) == unaccented }
        ?.let { return Feedback(correct = true, correctAnswer = it, accentWarning = true) }
    return Feedback(correct = false, correctAnswer = acceptedAnswers.first())
}

/** [text] in the form answers are compared in: composed letters, lower case, no punctuation, single spaces. */
internal fun normalizeAnswer(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFC)
        .lowercase(Locale.ROOT)
        .map { if (it.isLetterOrDigit() || it.isWhitespace()) it else ' ' }
        .joinToString("")
        .trim()
        .replace(whitespace, " ")

private val whitespace = Regex("\\s+")

// An explicit map, because ł has no Unicode decomposition into l plus an accent.
private val unaccentedLetter = mapOf(
    'ą' to 'a', 'ć' to 'c', 'ę' to 'e', 'ł' to 'l', 'ń' to 'n', 'ó' to 'o', 'ś' to 's', 'ź' to 'z', 'ż' to 'z',
)

/** Lower-case [text] with every Polish accented letter replaced by its plain letter. */
private fun stripDiacritics(text: String): String =
    text.map { unaccentedLetter[it] ?: it }.joinToString("")
