package com.polisheasy.engine

import kotlin.random.Random

/**
 * One question of Hobby charades: a hobby acted out by its [picture], and the Polish that names it,
 * [answer] (*jeździć na rowerze*), among [options]: other hobbies, those with the same verb first.
 * [key] is the hobby's Item ids joined.
 */
data class HobbyQuestion(
    val key: String,
    val picture: Picture,
    val answer: String,
    val options: List<String>,
)

/**
 * A Round of Hobby charades, chapter 6's Game: a hobby's picture is shown and the Learner names
 * it, choosing among four or, with [typed], typing it. Once answered, *Lubię …* is said with it.
 * The result is the answers right first try.
 */
data class HobbyRound(
    override val played: List<String>,
    val typed: Boolean,
    val run: Run<HobbyQuestion>,
) : Round {
    override val game: Game get() = Game.HOBBY_CHARADES
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The question being shown, or null once the Round is finished. */
    val question: HobbyQuestion? get() = run.current

    /** The answer to the question being shown, until the Learner moves on. Its text is what was chosen or typed. */
    val given: Given? get() = run.given

    /** Answers with the option at [option]. No change if there is none there, the Round is typed, or the question is answered. */
    fun choose(option: Int): HobbyRound {
        val question = question ?: return this
        if (typed || given != null) return this
        val chosen = question.options.getOrNull(option) ?: return this
        return copy(run = run.answer(chosen, chosen == question.answer))
    }

    /** Answers with what the Learner typed. As in an Exercise, an answer right apart from its accents is taken as right. */
    fun type(text: String): HobbyRound {
        val question = question ?: return this
        if (!typed || given != null || text.isBlank()) return this
        return copy(run = run.answer(text, checkTyped(text, listOf(question.answer)).correct))
    }

    /** Moves on from an answered question. */
    fun next(): HobbyRound = copy(run = run.next())
}

/**
 * Hobby charades: the hobbies the content lists under `hobbies` in practice.json whose Items are
 * all learned. It opens with four of them.
 */
internal object HobbyCharades : GameRules {
    const val ANSWER = "answer"
    const val CHOOSE = "choose"
    const val TYPE = "type"

    /** The fewest hobbies the Game opens with, the most a Round asks, and how many options each shows. */
    const val LEAST = 4
    const val QUESTIONS = 10
    const val OPTIONS = 4

    override val game = Game.HOBBY_CHARADES

    private class Hobby(val key: String, val verb: String, val polish: String, val picture: Picture)

    /** The learned hobbies, each with a Picture, no two with the same Polish or the same Picture. */
    private fun hobbies(learned: Learned): List<Hobby> {
        val course = learned.course
        val known = learned.items.map { it.id }.toSet()
        return course.hobbies.filter { hobby -> hobby.all { it in known } }.mapNotNull { hobby ->
            val picture = hobby.lastOrNull { it in course.pictures }?.let { course.pictures.getValue(it) } ?: return@mapNotNull null
            Hobby(hobby.joinToString(" "), hobby.first(), hobby.joinToString(" ") { course.itemsById.getValue(it).text }, picture)
        }.distinctBy { it.polish }.distinctBy { it.picture }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (hobbies(learned).size < LEAST) GameState.NEEDS_MORE else GameState.READY

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> =
        listOf(choice(ANSWER, listOf(OptionView(CHOOSE), OptionView(TYPE)), wanted.getOrNull(0)))

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val hobbies = hobbies(learned)
        val questions = random.draw(hobbies, QUESTIONS, seen, avoidFirst) { it.key }.map { hobby ->
            // Hobbies with the same verb first, so the verb alone does not give the answer away.
            val others = hobbies.filter { it.key != hobby.key }.shuffled(random).sortedBy { if (it.verb == hobby.verb) 0 else 1 }
                .map { it.polish }.take(OPTIONS - 1)
            HobbyQuestion(hobby.key, hobby.picture, hobby.polish, (listOf(hobby.polish) + others).shuffled(random))
        }
        return HobbyRound(selected, selected[0] == TYPE, Run(questions))
    }
}
