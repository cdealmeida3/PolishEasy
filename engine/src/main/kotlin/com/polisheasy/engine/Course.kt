package com.polisheasy.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Text keyed by Base Language code, for example `{"en": "hi"}`. */
typealias Localized = Map<String, String>

data class Course(
    val levels: List<Level>,
    val items: List<Item> = emptyList(),
    /** Every Clip packaged with the course. */
    val clips: List<Clip> = emptyList(),
    /** Where the Clips' files are, relative to the content root: the directory of the file that lists them. */
    val clipDir: String = "audio",
) {
    /** Every Daily Set in Path order. */
    val path: List<DailySet> = levels.flatMap { level -> level.units.flatMap { it.dailySets } }

    /** Every Exercise on the Path by id. */
    val exercises: Map<String, Exercise> = path.flatMap { it.exercises }.associateBy { it.id }

    /** Every Item by id. */
    val itemsById: Map<String, Item> = items.associateBy { it.id }

    private val clipsByText: Map<String, Clip> = clips.associateBy { audioKey(it.text) }

    /** The Clip that says [text], whatever its capitals and spacing, or null if there is none. */
    fun clipFor(text: String): ClipView? = clipsByText[audioKey(text)]?.let { clip ->
        ClipView(if (clipDir.isEmpty()) "${clip.id}.ogg" else "$clipDir/${clip.id}.ogg")
    }
}

/** One word, phrase or verb form the Learner can know or not know. */
@Serializable
data class Item(
    /** Stable and separate from [text], so a spelling fix never orphans the Learner's progress. */
    val id: String,
    /** The Polish. */
    val text: String,
    val meaning: Localized,
)

data class Level(val id: String, val title: Localized, val units: List<CourseUnit>)

data class CourseUnit(val id: String, val title: Localized, val dailySets: List<DailySet>)

@Serializable
data class DailySet(val id: String, val tips: List<Tip> = emptyList(), val exercises: List<Exercise>)

/** Shown once, just before the Exercise whose id is [before]. */
@Serializable
data class Tip(
    val id: String,
    val before: String,
    val title: Localized,
    val body: Localized,
    /** Optional: a verb's forms, shown under the body. */
    val conjugation: ConjugationTable? = null,
)

@Serializable
sealed interface Exercise {
    val id: String

    /** Ids of the Items this Exercise practises. */
    val items: List<String>
}

@Serializable
@SerialName("choose_translation")
data class ChooseTranslation(
    override val id: String,
    /** The Polish word or sentence to translate. */
    val prompt: String,
    val options: List<Localized>,
    val correctOption: Int,
    override val items: List<String> = emptyList(),
) : Exercise

@Serializable
@SerialName("type_answer")
data class TypeAnswer(
    override val id: String,
    /** What to say in Polish, in each Base Language. */
    val prompt: Localized,
    /** Every Polish answer treated as correct, with its proper spelling. The first is shown when the Learner is wrong. */
    val acceptedAnswers: List<String>,
    override val items: List<String> = emptyList(),
) : Exercise

/**
 * The Learner builds a Polish sentence by putting word tiles in order. The tiles are the words of
 * the first Accepted Answer plus the [distractors]; they are never authored, so they cannot drift
 * out of step with the answer. Tiles show words exactly as written here, so write the answers
 * without a sentence capital, which would give the first word away, and capitalise only proper nouns.
 */
@Serializable
@SerialName("word_tiles")
data class WordTiles(
    override val id: String,
    /** What to say in Polish, in each Base Language. */
    val prompt: Localized,
    /** Every word order treated as correct, as whole sentences. The first is shown when the Learner is wrong. */
    val acceptedAnswers: List<String>,
    /** Single words offered as tiles that the first Accepted Answer does not need. */
    val distractors: List<String> = emptyList(),
    override val items: List<String> = emptyList(),
) : Exercise

/** The Learner types the form of [verb] for [person], optionally into the gap of [sentence]. */
@Serializable
@SerialName("conjugation")
data class Conjugation(
    override val id: String,
    /** The dictionary form, for example być. */
    val verb: String,
    /** What the verb means, in each Base Language. */
    val meaning: Localized,
    val person: Person,
    /** Optional: a Polish sentence with `___` where the form belongs. */
    val sentence: String? = null,
    /** The whole sentence in each Base Language. Required with [sentence]. */
    val sentenceMeaning: Localized? = null,
    /** Every form treated as correct. The first is shown when the Learner is wrong. */
    val acceptedAnswers: List<String>,
    override val items: List<String> = emptyList(),
) : Exercise

/** The Learner hears [text] and picks it from among the [distractors]. */
@Serializable
@SerialName("listen_choose")
data class ListenChoose(
    override val id: String,
    /** The Polish that is spoken, and the one right option. */
    val text: String,
    /** What [text] means, in each Base Language. The prompt of the reading form, so it must fit [text] and none of the [distractors]. */
    val meaning: Localized,
    /** The wrong options, in Polish. */
    val distractors: List<String>,
    override val items: List<String> = emptyList(),
) : Exercise

/** The Learner hears the first Accepted Answer and types it. */
@Serializable
@SerialName("listen_type")
data class ListenType(
    override val id: String,
    /** Every spelling treated as correct. The first is what is spoken, and is shown when the Learner is wrong. */
    val acceptedAnswers: List<String>,
    /** What the first Accepted Answer means, in each Base Language. The prompt of the reading form. */
    val meaning: Localized,
    /** Further answers accepted only in the reading form, where the Learner translates instead of copying what was said. Never spoken. */
    val readingAnswers: List<String> = emptyList(),
    override val items: List<String> = emptyList(),
) : Exercise
