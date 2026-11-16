package com.polisheasy.engine

import java.text.Normalizer
import java.util.Locale
import kotlin.random.Random
import kotlinx.serialization.Serializable

/** One recording of spoken Polish, packaged with the app as `<id>.ogg` beside the file that lists it. */
@Serializable
data class Clip(
    /** Stable, and the name of the file. */
    val id: String,
    /** The Polish that is spoken. */
    val text: String,
)

/** A Clip as the app plays it. [path] is relative to the content root, like every content file. */
data class ClipView(val path: String)

private val whitespace = Regex("\\s+")

/**
 * [text] in the form Clips are found by: composed letters, lower case, single spaces. Punctuation
 * is kept, because "tak?" is not spoken like "tak."
 */
internal fun audioKey(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFC).lowercase(Locale.ROOT).trim().replace(whitespace, " ")

/** The Polish this Exercise can speak: what a speaker icon or a listening Exercise plays. */
internal fun spoken(exercise: Exercise): List<String> = when (exercise) {
    is ChooseTranslation -> listOf(exercise.prompt)
    is TypeAnswer -> exercise.acceptedAnswers
    is WordTiles -> exercise.acceptedAnswers
    is Conjugation -> exercise.acceptedAnswers.map { form -> exercise.sentence?.let { filled(it, form) } ?: form }
    is ListenChoose -> listOf(exercise.text)
    is ListenType -> exercise.acceptedAnswers.take(1)
}

/** Every Polish text the course can speak, once each, in Path order. Each needs a Clip. */
fun Course.spokenTexts(): List<String> =
    path.flatMap { it.exercises }.flatMap(::spoken).distinctBy(::audioKey)

/**
 * The options of [exercise] in the order the Learner sees them: the spoken text and the
 * distractors, mixed. The order depends only on the Exercise, so it is the same every time the
 * Exercise is shown, and it is never the answer followed by the distractors as authored.
 */
internal fun optionsOf(exercise: ListenChoose): List<String> {
    val authored = listOf(exercise.text) + exercise.distractors
    val shuffled = authored.shuffled(Random(exercise.id.hashCode()))
    return if (shuffled == authored) shuffled.drop(1) + shuffled.take(1) else shuffled
}
