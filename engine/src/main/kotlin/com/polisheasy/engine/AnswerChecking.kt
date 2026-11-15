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

/**
 * Checks the sentence the Learner built from word tiles, given as its [words] in order, against
 * the Accepted Answers of an Exercise. Only a word order listed as an Accepted Answer is correct.
 * There is no accent warning: the tiles carry the accents, so a tile that differs from the right
 * word only by an accent is a deliberate distractor.
 */
internal fun checkTiles(words: List<String>, acceptedAnswers: List<String>): Feedback {
    val built = normalizeAnswer(words.joinToString(" "))
    val matched = acceptedAnswers.firstOrNull { normalizeAnswer(it) == built }
    return Feedback(correct = matched != null, correctAnswer = matched ?: acceptedAnswers.first())
}

/** The words of [text] as word tiles show them: composed letters, case kept, no punctuation. */
internal fun words(text: String): List<String> =
    Normalizer.normalize(text, Normalizer.Form.NFC).withoutPunctuation().split(whitespace).filter { it.isNotEmpty() }

/** [text] in the form answers are compared in: composed letters, lower case, no punctuation, single spaces. */
internal fun normalizeAnswer(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFC)
        .lowercase(Locale.ROOT)
        .withoutPunctuation()
        .trim()
        .replace(whitespace, " ")

/** This text with a space in place of every character that is not a letter, a digit or whitespace. */
private fun String.withoutPunctuation(): String =
    map { if (it.isLetterOrDigit() || it.isWhitespace()) it else ' ' }.joinToString("")

private val whitespace = Regex("\\s+")

// An explicit map, because ł has no Unicode decomposition into l plus an accent.
private val unaccentedLetter = mapOf(
    'ą' to 'a', 'ć' to 'c', 'ę' to 'e', 'ł' to 'l', 'ń' to 'n', 'ó' to 'o', 'ś' to 's', 'ź' to 'z', 'ż' to 'z',
)

/** Lower-case [text] with every Polish accented letter replaced by its plain letter. */
private fun stripDiacritics(text: String): String =
    text.map { unaccentedLetter[it] ?: it }.joinToString("")
