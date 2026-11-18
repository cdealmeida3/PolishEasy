package com.polisheasy.engine

import kotlin.random.Random

/**
 * A person on Family tree's tree, seen from the Learner (*ja*): the grandparents, their children
 * (an uncle, an aunt and the father) with the mother, and the children of those: two cousins, a
 * brother, the Learner and a sister. [id] is stable: the content names the Items under it.
 */
enum class Relative(val id: String) {
    GRANDFATHER("grandfather"),
    GRANDMOTHER("grandmother"),
    UNCLE("uncle"),
    AUNT("aunt"),
    FATHER("father"),
    MOTHER("mother"),
    COUSIN("cousin"),
    COUSIN_GIRL("cousin-girl"),
    BROTHER("brother"),
    SISTER("sister"),
    ;

    companion object {
        fun of(id: String): Relative? = entries.firstOrNull { it.id == id }
    }
}

/** Family tree's family: the Item for the Learner, [me] (*ja*), and the Item for each [Relative] (*wujek*). */
data class Family(val me: String, val people: Map<Relative, String>)

/** One person on the tree: who they are, their word (*wujek*), what it means, and "my" with it (*mój wujek*). */
data class TreePerson(val relative: Relative, val itemId: String, val word: String, val meaning: String, val gender: Gender, val phrase: String)

/**
 * One question of Family tree: [person], marked on the tree to be named, or named to be found.
 * Named, the Learner picks from [options]: "my" and the word, the word with the wrong "my", and
 * two other people.
 */
data class TreeQuestion(val person: TreePerson, val options: List<String>)

/**
 * A Round of Family tree, chapter 11's Game, over the tree of [people] seen from [me] (*ja*). Each
 * question is one person: with [find] off a person is marked and the Learner says who it is, *mój
 * wujek*; with it on the Learner is told *mój wujek* and taps him on the tree. Each person comes
 * once. The result is the answers right first try.
 */
data class TreeRound(
    override val played: List<String>,
    val find: Boolean,
    val me: String,
    val people: List<TreePerson>,
    val run: Run<TreeQuestion>,
) : Round {
    override val game: Game get() = Game.FAMILY_TREE
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.person.relative.id }

    /** The question being shown, or null once the Round is finished. */
    val question: TreeQuestion? get() = run.current

    /** The answer to the question being shown, until the Learner moves on: the phrase chosen, or that of the person tapped. */
    val given: Given? get() = run.given

    /** The people named so far, written on the tree: those asked before, and the one asked once answered. */
    val named: Set<Relative> get() = run.questions.take(run.index + if (given != null) 1 else 0).map { it.person.relative }.toSet()

    /** Names the marked person with the option at [option]. No change when finding, for no such option, or once answered. */
    fun choose(option: Int): TreeRound {
        val question = question ?: return this
        if (find || given != null) return this
        val chosen = question.options.getOrNull(option) ?: return this
        return copy(run = run.answer(chosen, chosen == question.person.phrase))
    }

    /** Taps [relative] on the tree to find the person named. No change when naming, or once answered. */
    fun tap(relative: Relative): TreeRound {
        val question = question ?: return this
        if (!find || given != null) return this
        val tapped = people.firstOrNull { it.relative == relative } ?: return this
        return copy(run = run.answer(tapped.phrase, relative == question.person.relative))
    }

    /** Moves on from an answered question. */
    fun next(): TreeRound = copy(run = run.next())
}

/**
 * Family tree: the family the content lists under `family` in practice.json. It opens once every
 * Item of it is learned, with "my" in the masculine and the feminine, and each person's noun has
 * that gender under `genders`.
 */
internal object FamilyTree : GameRules {
    const val WAY = "way"
    const val NAME = "name"
    const val FIND = "find"

    /** How many options a person to be named shows. */
    const val OPTIONS = 4

    override val game = Game.FAMILY_TREE

    private val people = setOf(Gender.MASCULINE, Gender.FEMININE)

    private fun tree(learned: Learned): Pair<Item, List<TreePerson>>? {
        val course = learned.course
        val family = course.family ?: return null
        val known = learned.items.associateBy { it.id }
        val me = known[family.me] ?: return null
        if (Relative.entries.any { it !in family.people }) return null
        val persons = Relative.entries.map { relative ->
            val item = known[family.people.getValue(relative)] ?: return null
            val gender = course.genders[item.id]?.takeIf { it in people } ?: return null
            val my = known[course.possessives[gender] ?: return null] ?: return null
            TreePerson(relative, item.id, item.text, learned.localize(item.meaning), gender, "${my.text} ${item.text}")
        }
        return me to persons
    }

    override fun state(learned: Learned, sound: Boolean): GameState = if (tree(learned) != null) GameState.READY else GameState.NEEDS_MORE

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> =
        listOf(choice(WAY, listOf(OptionView(NAME), OptionView(FIND)), wanted.getOrNull(0)))

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val (me, persons) = checkNotNull(tree(learned))
        val course = learned.course
        val my = course.possessives.mapValues { (_, id) -> course.itemsById.getValue(id).text }
        val questions = random.draw(persons, persons.size, seen, avoidFirst) { it.relative.id }.map { person ->
            val other = if (person.gender == Gender.MASCULINE) Gender.FEMININE else Gender.MASCULINE
            val wrongMy = "${my.getValue(other)} ${person.word}"
            // Two other people, of the same gender first, so "my" alone does not give the answer away.
            val others = persons.filter { it.word != person.word }.shuffled(random).sortedBy { if (it.gender == person.gender) 0 else 1 }
                .map { it.phrase }.distinct().take(OPTIONS - 2)
            TreeQuestion(person, (listOf(person.phrase, wrongMy) + others).shuffled(random))
        }
        return TreeRound(selected, selected[0] == FIND, me.text, persons, Run(questions))
    }
}

/** What Family tree says: "my" and each relative's word, *mój wujek*, *moja ciocia*. Each needs a Clip. */
internal fun Course.familyPhrases(): List<String> {
    val family = family ?: return emptyList()
    return Relative.entries.mapNotNull { relative ->
        val item = family.people[relative]?.let(itemsById::get) ?: return@mapNotNull null
        val my = genders[item.id]?.let(possessives::get)?.let(itemsById::get) ?: return@mapNotNull null
        "${my.text} ${item.text}"
    }
}
