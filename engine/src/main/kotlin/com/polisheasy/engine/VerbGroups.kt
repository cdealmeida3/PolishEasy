package com.polisheasy.engine

import kotlin.random.Random

/** The book's three conjugations, named by the endings of *ja* and *ty*, as chapter 6 heads them. */
enum class VerbGroup(/** The endings, as the book writes them. */ val endings: String) {
    /** *znam, znasz*; *umiem, umiesz*; *mam, masz*. */
    M_SZ("-m, -sz"),

    /** *piszę, piszesz*; *gotuję, gotujesz*. */
    E_ESZ("-ę, -esz"),

    /** *robię, robisz*; *tańczę, tańczysz*. */
    E_ISZ("-ę, -isz / -ysz"),
    ;

    internal companion object {
        /** The book's conjugation of a table's [ConjugationTable.group], or null for a verb that follows none. */
        fun of(group: String?): VerbGroup? = when (group) {
            "-am, -asz", "-em, -esz" -> M_SZ
            "-ę, -esz" -> E_ESZ
            "-ę, -isz/-ysz" -> E_ISZ
            else -> null
        }
    }
}

/**
 * One question of Three groups: a learned [verb] in its dictionary form, with its [meaning], and
 * the [group] it belongs to. [ja] and [ty] are its forms for *ja* and *ty*, shown once answered,
 * and with the *ja* clue shown before.
 */
data class VerbGroupQuestion(
    val verb: String,
    val meaning: String,
    val group: VerbGroup,
    val ja: String,
    val ty: String,
)

/**
 * A Round of Three groups, chapter 6's Game: a verb is shown and the Learner sorts it into the
 * conjugation it follows, one of [groups]. With [withJa] its *ja* form is shown too. The result is
 * the answers right first try.
 */
data class VerbGroupRound(
    override val played: List<String>,
    val withJa: Boolean,
    /** What can be sorted into, in the book's order: the conjugations of the verbs learned. */
    val groups: List<VerbGroup>,
    val run: Run<VerbGroupQuestion>,
) : Round {
    override val game: Game get() = Game.VERB_GROUPS
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.verb }

    /** The question being shown, or null once the Round is finished. */
    val question: VerbGroupQuestion? get() = run.current

    /** The answer to the question being shown, until the Learner moves on. Its text is the group's name. */
    val given: Given? get() = run.given

    /** The group the Learner chose for the question being shown, once answered. */
    val chosen: VerbGroup? get() = given?.let { given -> VerbGroup.entries.firstOrNull { it.name == given.text } }

    /** Sorts the verb into [group]. No change if it is not offered, or the question is answered. */
    fun choose(group: VerbGroup): VerbGroupRound {
        val question = question ?: return this
        if (given != null || group !in groups) return this
        return copy(run = run.answer(group.name, group == question.group))
    }

    /** Moves on from an answered question. */
    fun next(): VerbGroupRound = copy(run = run.next())
}

/**
 * Three groups: the verbs whose table a completed Daily Set has shown, sorted into the book's
 * three conjugations. It opens with four verbs of two conjugations or more.
 */
internal object VerbGroups : GameRules {
    const val CLUE = "show"
    const val VERB = "verb"
    const val WITH_JA = "with-ja"

    /** The fewest verbs the Game opens with, and the most a Round asks. */
    const val LEAST = 4
    const val QUESTIONS = 10

    override val game = Game.VERB_GROUPS

    private class Sorted(val verb: LearnedVerb, val group: VerbGroup)

    /** The learned verbs that follow one of the three conjugations, with their *ja* and *ty* forms. */
    private fun verbs(learned: Learned): List<Sorted> = learned.verbs().mapNotNull { verb ->
        val group = VerbGroup.of(verb.table.group) ?: return@mapNotNull null
        if (verb.table.forms[Person.I] == null || verb.table.forms[Person.YOU] == null) return@mapNotNull null
        Sorted(verb, group)
    }

    override fun state(learned: Learned, sound: Boolean): GameState {
        val verbs = verbs(learned)
        return if (verbs.size >= LEAST && verbs.map { it.group }.toSet().size >= 2) GameState.READY else GameState.NEEDS_MORE
    }

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> =
        listOf(choice(CLUE, listOf(OptionView(VERB), OptionView(WITH_JA)), wanted.getOrNull(0)))

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val verbs = verbs(learned)
        val questions = random.draw(verbs, QUESTIONS, seen, avoidFirst) { it.verb.table.verb }.map { sorted ->
            val table = sorted.verb.table
            VerbGroupQuestion(
                table.verb,
                learned.localize(table.meaning),
                sorted.group,
                ja = table.forms.getValue(Person.I),
                ty = table.forms.getValue(Person.YOU),
            )
        }
        val groups = VerbGroup.entries.filter { group -> verbs.any { it.group == group } }
        return VerbGroupRound(selected, selected[0] == WITH_JA, groups, Run(questions))
    }
}
