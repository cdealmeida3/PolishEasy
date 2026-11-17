package com.polisheasy.engine

import kotlin.random.Random
import kotlinx.serialization.Serializable

/**
 * The forms of the Ankieta Game, as `content/kpk1/ankieta.json` writes them: the [fields] a form
 * asks for, in order, and the [people] who fill it in. A chapter that teaches more to ask adds
 * fields, and a chapter adds people who say it.
 */
@Serializable
data class AnkietaFile(
    val fields: List<AnkietaField> = emptyList(),
    val people: List<AnkietaPerson> = emptyList(),
)

/** One line of the form: its Polish [label], such as *Skąd?*, and what it [meaning]s. */
@Serializable
data class AnkietaField(val id: String, val label: String, val meaning: Localized)

/**
 * Someone who introduces themselves: what they [says], line by line, and the form filled in from
 * it, as [answers] by field id. A field this person does not answer is left off the form. The
 * options for a field are the right answer, any [wrong] ones written for it, and the answers of the
 * other people the Learner can meet. The person is met once every one of their [items] is learned.
 */
@Serializable
data class AnkietaPerson(
    val id: String,
    val items: List<String> = emptyList(),
    val says: List<AnkietaLine> = emptyList(),
    val answers: Map<String, String> = emptyMap(),
    val wrong: Map<String, List<String>> = emptyMap(),
)

/** A line someone says, and what it means. */
@Serializable
data class AnkietaLine(val text: String, val meaning: Localized)

/** A line as a Round shows it: its Polish and its meaning. */
data class AnkietaSaid(val text: String, val meaning: String)

/**
 * One field of one person's form to fill in: the field's [label] and [meaning], the [options] to
 * choose from, and the one at [answer] that is right. [person] is the index of the person in the Round.
 */
data class AnkietaQuestion(
    val person: Int,
    val field: String,
    val label: String,
    val meaning: String,
    val options: List<String>,
    val answer: Int,
) {
    val right: String get() = options[answer]
}

/** A person as a Round shows them: what they say. */
data class AnkietaCard(val id: String, val says: List<AnkietaSaid>)

/**
 * A Round of Ankieta: a few people introduce themselves, heard ([listen]) or read, and for each the
 * Learner fills in a form, one field at a time, choosing what the person said. A right answer
 * fills the field; a wrong one shows the right one, which fills it, and waits. The result is the
 * fields right first try.
 */
data class AnkietaRound(
    override val played: List<String>,
    val listen: Boolean,
    val people: List<AnkietaCard>,
    val run: Run<AnkietaQuestion>,
) : Round {
    override val game: Game get() = Game.ANKIETA
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished

    /** Each person is one step: their form fills in place. */
    override val step: Int get() = question?.person ?: people.size
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = people.map { it.id }

    /** The field being filled in, or null once the Round is finished. */
    val question: AnkietaQuestion? get() = run.current

    /** The person whose form is being filled in. */
    val person: AnkietaCard? get() = question?.let { people[it.person] }

    /** The answer to the field being shown, until the Learner moves on. */
    val given: Given? get() = run.given

    /**
     * The form of the person being shown, field by field: each with what it was filled in with
     * (the right answer, also after a wrong one), whether it was right first try, or nothing yet.
     */
    val form: List<AnkietaLineOfForm>
        get() {
            val at = question ?: return emptyList()
            return run.questions.withIndex().filter { it.value.person == at.person }.map { (index, question) ->
                val answered = index < run.index || (index == run.index && run.given != null)
                AnkietaLineOfForm(
                    label = question.label,
                    meaning = question.meaning,
                    filled = if (answered) question.right else null,
                    right = if (answered) run.results[index] else null,
                    current = index == run.index,
                )
            }
        }

    /** Answers the field being shown with the option at [option]. No change if it is not one, or the field is answered. */
    fun choose(option: Int): AnkietaRound {
        val question = question ?: return this
        val chosen = question.options.getOrNull(option) ?: return this
        return copy(run = run.answer(chosen, option == question.answer))
    }

    /** Moves on from an answered field. */
    fun next(): AnkietaRound = copy(run = run.next())
}

/** A field of the form being filled in: [filled] once answered, [right] whether first try, [current] while being asked. */
data class AnkietaLineOfForm(
    val label: String,
    val meaning: String,
    val filled: String?,
    val right: Boolean?,
    val current: Boolean,
)

internal object Ankieta : GameRules {
    const val WAY = "intro"
    const val LISTEN = "listen"
    const val READ = "read"

    /** The fewest people the Game opens with, so each field has answers to choose among. */
    const val LEAST = 3

    /** How many people a Round meets, at most. */
    const val PEOPLE = 3

    /** How many options a field offers, at most. */
    const val OPTIONS = 4

    override val game = Game.ANKIETA

    /** The people whose every Item is learned and who answer at least one field, in the content's order. */
    private fun people(learned: Learned): List<AnkietaPerson> {
        val known = learned.items.map { it.id }.toSet()
        val fields = learned.course.ankieta.fields.map { it.id }.toSet()
        return learned.course.ankieta.people.filter { person ->
            person.says.isNotEmpty() && person.answers.keys.any { it in fields } && known.containsAll(person.items)
        }
    }

    /** Whether everything [person] says can be heard. */
    private fun heard(learned: Learned, person: AnkietaPerson): Boolean = person.says.all { learned.course.clipFor(it.text) != null }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (people(learned).size >= LEAST) GameState.READY else GameState.NEEDS_MORE

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val people = people(learned)
        val ways = listOfNotNull(LISTEN.takeIf { sound && people.count { heard(learned, it) } >= LEAST }, READ)
        return listOf(choice(WAY, ways.map { OptionView(it) }, wanted.getOrNull(0)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val listen = selected[0] == LISTEN
        val all = people(learned)
        val people = random.draw(all.filter { !listen || heard(learned, it) }, PEOPLE, seen, avoidFirst) { it.id }
        val fields = learned.course.ankieta.fields
        val questions = people.withIndex().flatMap { (index, person) ->
            fields.filter { it.id in person.answers }.map { field ->
                val right = person.answers.getValue(field.id)
                val others = all.filter { it.id != person.id }.mapNotNull { it.answers[field.id] }.shuffled(random)
                val wrong = (person.wrong[field.id].orEmpty().shuffled(random) + others)
                    .filter { !it.equals(right, ignoreCase = true) }
                    .distinctBy { it.lowercase() }
                    .take(OPTIONS - 1)
                val options = (wrong + right).shuffled(random)
                AnkietaQuestion(index, field.id, field.label, learned.localize(field.meaning), options, options.indexOf(right))
            }
        }
        val cards = people.map { person -> AnkietaCard(person.id, person.says.map { AnkietaSaid(it.text, learned.localize(it.meaning)) }) }
        return AnkietaRound(selected, listen, cards, Run(questions))
    }
}
