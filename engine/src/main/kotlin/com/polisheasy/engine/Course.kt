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
    /** The picture of each Item that has one, by Item id. Only the Games of Practice show them. */
    val pictures: Map<String, Picture> = emptyMap(),
    /** Real Polish words, in lower case, that a wrong option of Sound gaps would spell. Such a gap is never asked. */
    val soundGapRealWords: Set<String> = emptySet(),
    /** The gender of each noun Item that Practice sorts by gender, by Item id. */
    val genders: Map<String, Gender> = emptyMap(),
    /** The Item that says "my" with a noun of each gender, by its id. */
    val possessives: Map<Gender, String> = emptyMap(),
    /** The instruction every Exercise of a kind shows, in Polish as in the book, by [InstructionKind]. */
    val instructions: Map<InstructionKind, Instruction> = emptyMap(),
    /** The book's class recordings, in their order. Their files are imported on the phone, never packaged. */
    val tracks: List<ClassTrack> = emptyList(),
    /** Each class recording's lines, pause to pause, by track number. */
    val trackLines: Map<Int, List<TrackLine>> = emptyMap(),
    /** The listening tasks on the class recordings. */
    val listening: List<ListeningTask> = emptyList(),
    /** The conversations of the Chat Game, in the content's order. */
    val chats: List<ChatScript> = emptyList(),
    /** The forms and people of the Ankieta Game. */
    val ankieta: AnkietaFile = AnkietaFile(),
    /** What each small letter is called when a word is spelled aloud (*b* is *be*), for Spell it. */
    val letterNames: Map<String, String> = emptyMap(),
    /** The Item that says "this" with a noun of each gender, by its id, for Ten, ta, to. */
    val demonstratives: Map<Gender, String> = emptyMap(),
    /** Pairs of adjectives that are opposites, as Item ids, for Opposites snap. A word may be in more than one pair. */
    val opposites: List<Pair<String, String>> = emptyList(),
    /** The jobs of Who's who, as Item ids: each noun's first form is its instrumental (*lekarz*, *lekarzem*). */
    val jobs: List<String> = emptyList(),
    /** The Item Who's who waits for, which brings the instrumental (*kim*), or null if the content has no jobs. */
    val jobsAfter: String? = null,
    /**
     * The hobbies of Hobby charades, each the Item ids of its words in order (*jeździć*,
     * *na rowerze*); its Picture is that of its last Item with one.
     */
    val hobbies: List<List<String>> = emptyList(),
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
    /** Where the book marks one: whether the phrase is formal, informal or both (O, N, U). */
    val register: Register? = null,
    /**
     * The word's other forms that an Exercise may use in its place, as an adjective's feminine and
     * neuter (*nowa*, *nowe* of *nowy*): the Learner who knows the Item knows them too.
     */
    val forms: List<String> = emptyList(),
) {
    /** Every way the Item may be written: its text and then its [forms]. */
    val spellings: List<String> get() = listOf(text) + forms
}

/** The book's register marks: O oficjalnie, N nieoficjalnie, U uniwersalnie. */
@Serializable
enum class Register(val mark: String) {
    @SerialName("formal")
    FORMAL("O"),

    @SerialName("informal")
    INFORMAL("N"),

    @SerialName("universal")
    UNIVERSAL("U"),
}

/** The three strands of a lesson in the book: K Komunikacja, L Leksyka, G Gramatyka. */
@Serializable
enum class Strand(val mark: String) {
    @SerialName("K")
    COMMUNICATION("K"),

    @SerialName("L")
    VOCABULARY("L"),

    @SerialName("G")
    GRAMMAR("G"),
}

data class Level(val id: String, val title: Localized, val units: List<CourseUnit>)

/**
 * A Unit. In the book's Level it is a chapter: [number] is its lesson number, [polishTitle] the
 * book's title (then [title] is the English under it), [accent] its colour, [strands] what the
 * contents say it teaches, [lookUp] the Items of its "Sprawdź w słowniku" and [coming] when it has
 * no content yet.
 */
data class CourseUnit(
    val id: String,
    val title: Localized,
    val dailySets: List<DailySet>,
    val number: Int? = null,
    val polishTitle: String? = null,
    val accent: Int = 0,
    val strands: Map<Strand, List<String>> = emptyMap(),
    val lookUp: List<String> = emptyList(),
    val coming: Boolean = false,
)

/**
 * A Daily Set. In the book's Level it is a section of a chapter: [section] gives its letter and
 * heading as in the book.
 */
@Serializable
data class DailySet(
    val id: String,
    val tips: List<Tip> = emptyList(),
    val exercises: List<Exercise>,
    val section: Section? = null,
)

/** A section of a chapter of the book: its [letter] (A, B, C…), the book's [heading] in Polish, its [meaning], and the [strands] it covers. */
@Serializable
data class Section(
    val letter: String? = null,
    val heading: String,
    val meaning: Localized = emptyMap(),
    val strands: List<Strand> = emptyList(),
)

/** What an Exercise asks, in Polish as the book writes it ([polish]), and in the Base Language. */
@Serializable
data class Instruction(val polish: String, val meaning: Localized)

/** The kinds of Exercise as the Learner sees them, each with its own instruction. A reading form has its own. */
@Serializable
enum class InstructionKind {
    @SerialName("choose_translation")
    CHOOSE_TRANSLATION,

    @SerialName("type_answer")
    TYPE_ANSWER,

    @SerialName("word_tiles")
    WORD_TILES,

    @SerialName("conjugation")
    CONJUGATION,

    @SerialName("listen_choose")
    LISTEN_CHOOSE,

    @SerialName("listen_type")
    LISTEN_TYPE,

    @SerialName("choose_polish")
    CHOOSE_POLISH,

    @SerialName("fill_gaps")
    FILL_GAPS,

    @SerialName("match_pairs")
    MATCH_PAIRS,

    @SerialName("true_false")
    TRUE_FALSE,

    /** A true-or-false Exercise whose text is heard. */
    @SerialName("listen_true_false")
    LISTEN_TRUE_FALSE,

    @SerialName("order_lines")
    ORDER_LINES,
}

/** Shown once, just before the Exercise whose id is [before]. */
@Serializable
data class Tip(
    val id: String,
    val before: String,
    val title: Localized,
    val body: Localized,
    /** Optional: a verb's forms, shown under the body as a table. The Verb challenge reads them. */
    val conjugation: ConjugationTable? = null,
    /** A plain note, or one of the book's callouts. */
    val kind: TipKind = TipKind.NOTE,
    /** Optional: a grammar table, shown under the body. A Tip with a table of either kind is kept on the Grammar shelf. */
    val table: GrammarTable? = null,
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
