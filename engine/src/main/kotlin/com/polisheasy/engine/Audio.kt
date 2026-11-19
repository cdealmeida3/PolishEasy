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
fun audioKey(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFC).lowercase(Locale.ROOT).trim().replace(whitespace, " ")

/** The Polish this Exercise can speak: what a speaker icon or a listening Exercise plays. */
internal fun spoken(exercise: Exercise): List<String> = when (exercise) {
    is ChooseTranslation -> listOf(exercise.prompt)
    is TypeAnswer -> exercise.acceptedAnswers
    is WordTiles -> exercise.acceptedAnswers
    is Conjugation -> exercise.acceptedAnswers.map { form -> exercise.sentence?.let { filled(it, form) } ?: form }
    is ListenChoose -> listOf(exercise.text)
    is ListenType -> exercise.acceptedAnswers.take(1)
    is FillGaps -> listOf(filledGaps(exercise.text, exercise.gaps.map { it.first() }))
    is MatchPairs -> exercise.pairs.map { it.polish }
    is TrueFalse -> exercise.lines
    is OrderLines -> exercise.lines
}

/** What a conversation of the Chat Game can say: each line of the person, and each right reply, said as the chat goes on with it. */
internal fun spoken(script: ChatScript): List<String> =
    script.turns.flatMap { turn -> listOf(turn.says) + turn.replies.filter { it.right }.map { it.text } }

/**
 * Every Polish text the course can speak, once each, in Path order, then the instructions, the
 * chats, what Ankieta's people say, the letters' names, Market list's goods, alone and in the
 * plural (*gruszka*, *gruszki*), Take the order's words and dishes, Negation flip's negations,
 * Family tree's people with "my" (*mój wujek*), the times Tell the time says (*wpół do dziewiątej*)
 * Week planner's plans, Story strip's stories, each told whole, and Who said it?'s lines.
 * Each needs a Clip.
 */
fun Course.spokenTexts(): List<String> =
    (
        path.flatMap { it.exercises }.flatMap(::spoken) +
            InstructionKind.entries.mapNotNull { instructions[it]?.polish } +
            chats.flatMap(::spoken) +
            ankieta.people.flatMap { person -> person.says.map { it.text } } +
            letterNames.values +
            market?.goods.orEmpty().mapNotNull { itemsById[it] }.flatMap { listOf(it.text) + it.forms } +
            restaurant?.let { listOf(it.forMe, it.same, it.and) + it.dishes }.orEmpty().mapNotNull { itemsById[it]?.text } +
            negations.map { it.no } +
            familyPhrases() +
            clockPhrases() +
            week?.plans.orEmpty().map { it.text } +
            stories.map { it.text } +
            speakers.map { it.text } +
            routes.map { it.text } +
            swaps.flatMap { listOf(it.text, it.short) } +
            packaging.map { it.text } +
            counting.map { it.text } +
            forecasts.map { it.text } +
            room?.scenes.orEmpty().map { it.text } +
            flatHunt.map { it.wish.text } +
            polandMap.map { it.says.text } +
            calendar.map { it.says.text }
        )
        .distinctBy(::audioKey)

/**
 * What the command that makes the Clips needs of the content: [texts] is every Polish text the
 * course can speak, once each, in Path order, and [manifest] is the path of the file that lists the
 * Clips, or null if `course.json` names none.
 */
class SpokenContent(
    val texts: List<String>,
    val manifest: String?,
    /** A listed file that could not be read, the manifest aside. Its texts are missing from [texts]. */
    val unreadable: List<ContentProblem>,
)

/**
 * Reads what the content speaks without judging it. Unlike [loadCourse] this accepts a listening
 * Exercise that has no Clip yet and a manifest that is not there yet, because the command that
 * calls it is what makes them.
 */
fun spokenContent(readFile: (path: String) -> String): SpokenContent {
    val content = readContent(readFile)
    return SpokenContent(
        texts = content.course().spokenTexts(),
        manifest = content.clipsFile,
        unreadable = content.unreadable.filter { it.file != content.clipsFile },
    )
}

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
