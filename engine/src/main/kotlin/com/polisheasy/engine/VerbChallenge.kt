package com.polisheasy.engine

import kotlin.random.Random

/** The three stages of a Round of the Verb challenge, in order. */
enum class VerbStage {
    /** The verb and a person; pick or type the form. */
    PICK,

    /** The verb's empty table; put each form in its row. */
    TABLE,

    /** A sentence with a gap; pick or type the form that fills it. */
    SENTENCE,
}

/**
 * One question of the Verb challenge. [answers] are the forms taken as right, the first being the
 * one shown. [options] are the forms to choose from, every one a real form of [verb]; at the
 * [VerbStage.TABLE] stage they are the tiles, the same for each of its questions.
 */
data class VerbQuestion(
    val stage: VerbStage,
    val verb: String,
    val meaning: String,
    val person: Person,
    val answers: List<String>,
    val options: List<String>,
    val sentence: GapSentence? = null,
    internal val key: String = "",
)

/** A row of the table being filled: the [form] once the Learner has been asked for it. */
data class VerbTableRow(val person: Person, val form: String? = null, val state: VerbRowState = VerbRowState.EMPTY)

enum class VerbRowState {
    /** Not asked yet. */
    EMPTY,

    /** The row being asked for now. */
    ASKED,

    /** Filled by the Learner's first try. */
    RIGHT,

    /** Filled with the right form after a wrong try. */
    MISSED,
}

/**
 * A Round of the Verb challenge: [VerbStage.PICK], then [VerbStage.TABLE], then
 * [VerbStage.SENTENCE]. With [typed] the form is typed at the first and last stages instead of
 * chosen. The result is the answers right first try.
 */
data class VerbRound(
    override val played: List<String>,
    val typed: Boolean,
    val run: Run<VerbQuestion>,
) : Round {
    override val game: Game get() = Game.VERB_CHALLENGE
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    /** Each question is a step of its own, except the table, which stays where it is while its rows are filled. */
    override val step: Int
        get() = if (question?.stage == VerbStage.TABLE) tableQuestions().first().index else run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The question being shown, or null once the Round is finished. */
    val question: VerbQuestion? get() = run.current

    /** The answer to the question being shown, until the Learner moves on. */
    val given: Given? get() = run.given

    /** Whether the question being shown is answered by typing. */
    val typing: Boolean get() = typed && question?.stage != VerbStage.TABLE

    /** The table of the stage being shown, each row as far as it has been filled; empty at the other stages. */
    val table: List<VerbTableRow>
        get() {
            val now = question?.takeIf { it.stage == VerbStage.TABLE } ?: return emptyList()
            val rows = tableQuestions().associateBy { it.value.person }
            return Person.entries.mapNotNull { person ->
                val (index, asked) = rows[person] ?: return@mapNotNull null
                val result = run.results.getOrNull(index)
                when {
                    result != null -> VerbTableRow(person, asked.answers.first(), if (result) VerbRowState.RIGHT else VerbRowState.MISSED)
                    asked === now -> VerbTableRow(person, state = VerbRowState.ASKED)
                    else -> VerbTableRow(person)
                }
            }
        }

    /** For each tile of the table stage, whether its form is already in a row. */
    val tilesUsed: List<Boolean>
        get() {
            val tiles = question?.takeIf { it.stage == VerbStage.TABLE }?.options ?: return emptyList()
            val used = BooleanArray(tiles.size)
            tableQuestions().filter { run.results.getOrNull(it.index) != null }.forEach { (_, filled) ->
                val tile = tiles.indices.firstOrNull { !used[it] && tiles[it] == filled.answers.first() }
                if (tile != null) used[tile] = true
            }
            return used.toList()
        }

    /** Answers with the option or tile at [option]. No change if it is not one, is used, or the question is answered. */
    fun choose(option: Int): VerbRound {
        val question = question ?: return this
        val text = question.options.getOrNull(option) ?: return this
        if (question.stage == VerbStage.TABLE && tilesUsed[option]) return this
        return copy(run = run.answer(text, question.answers.any { normalizeAnswer(it) == normalizeAnswer(text) }))
    }

    /** Answers with what the Learner typed. As in an Exercise, a form right apart from its accents is taken as right. */
    fun type(text: String): VerbRound {
        val question = question ?: return this
        return copy(run = run.answer(text, checkTyped(text, question.answers).correct))
    }

    /** Moves on from an answered question. */
    fun next(): VerbRound = copy(run = run.next())

    private fun tableQuestions(): List<IndexedValue<VerbQuestion>> =
        run.questions.withIndex().filter { it.value.stage == VerbStage.TABLE }
}

/** A verb the Learner can be challenged on: its table, and the gap sentences of completed Exercises that use it. */
internal class LearnedVerb(val table: ConjugationTable, val sentences: List<Conjugation>) {
    /** The table's forms, once each, in the order of the persons. */
    val forms: List<String> = Person.entries.mapNotNull { table.forms[it] }.distinct()
}

/** The verbs whose table a completed Daily Set has shown, in the order they were met. */
internal fun Learned.verbs(): List<LearnedVerb> {
    val sentences = dailySets.flatMap { it.exercises }.filterIsInstance<Conjugation>().filter { it.sentence != null }
    return dailySets.flatMap { it.tips }.mapNotNull { it.conjugation }.distinctBy { audioKey(it.verb) }.map { table ->
        LearnedVerb(table, sentences.filter { audioKey(it.verb) == audioKey(table.verb) }.distinctBy { audioKey(it.sentence.orEmpty()) })
    }
}

internal object VerbChallenge : GameRules {
    const val VERB = "verb"
    const val LEVEL = "level"
    const val MIXED = "mixed"
    const val CHOOSE = "choose"
    const val TYPE = "type"

    /** How many questions each stage asks, at most. */
    const val PICK_QUESTIONS = 6
    const val SENTENCE_QUESTIONS = 4

    /** How many forms a question offers to choose from, at most. */
    private const val OPTIONS = 4

    override val game = Game.VERB_CHALLENGE

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (learned.verbs().isEmpty()) GameState.NEEDS_MORE else GameState.READY

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val verbs = learned.verbs().map { OptionView(it.table.verb, label = it.table.verb) }
        // Mixed needs two verbs to mix.
        val mixed = if (verbs.size > 1) listOf(OptionView(MIXED)) else emptyList()
        return listOf(
            choice(VERB, verbs + mixed, wanted.getOrNull(0)),
            choice(LEVEL, listOf(OptionView(CHOOSE), OptionView(TYPE)), wanted.getOrNull(1)),
        )
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val all = learned.verbs()
        val verbs = all.filter { it.table.verb == selected[0] }.ifEmpty { all }
        fun question(stage: VerbStage, verb: LearnedVerb, person: Person, answers: List<String>, options: List<String>, sentence: Conjugation? = null) =
            VerbQuestion(
                stage,
                verb.table.verb,
                learned.localize(verb.table.meaning),
                person,
                answers,
                options,
                sentence = sentence?.let { asked ->
                    val (before, after) = checkNotNull(gapOf(checkNotNull(asked.sentence)))
                    GapSentence(before, after, learned.localize(checkNotNull(asked.sentenceMeaning)))
                },
                key = sentence?.id ?: "$stage:${verb.table.verb}:$person",
            )

        /** [answers] among up to three other forms of [verb], mixed. Every option is a real form of the verb. */
        fun optionsFor(verb: LearnedVerb, answers: List<String>): List<String> {
            val right = answers.map(::normalizeAnswer).toSet()
            val others = verb.forms.filter { normalizeAnswer(it) !in right }.shuffled(random).take(OPTIONS - 1)
            return (others + answers.first()).shuffled(random)
        }

        val pairs = verbs.flatMap { verb -> Person.entries.filter { it in verb.table.forms }.map { verb to it } }
        val pick = random.draw(pairs, PICK_QUESTIONS, seen, avoidFirst) { (verb, person) -> "${VerbStage.PICK}:${verb.table.verb}:$person" }
            .map { (verb, person) ->
                val form = verb.table.forms.getValue(person)
                question(VerbStage.PICK, verb, person, listOf(form), optionsFor(verb, listOf(form)))
            }

        val tabled = verbs.random(random)
        val persons = Person.entries.filter { it in tabled.table.forms }
        val tiles = persons.map { tabled.table.forms.getValue(it) }.shuffled(random)
        val table = persons.shuffled(random).map { person ->
            question(VerbStage.TABLE, tabled, person, listOf(tabled.table.forms.getValue(person)), tiles)
        }

        val sentences = random.draw(verbs.flatMap { verb -> verb.sentences.map { verb to it } }, SENTENCE_QUESTIONS, seen, null) { it.second.id }
            .map { (verb, asked) ->
                question(VerbStage.SENTENCE, verb, asked.person, asked.acceptedAnswers, optionsFor(verb, asked.acceptedAnswers), asked)
            }

        return VerbRound(selected, typed = selected[1] == TYPE, run = Run(pick + table + sentences))
    }
}
