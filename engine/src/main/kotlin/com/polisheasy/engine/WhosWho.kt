package com.polisheasy.engine

import kotlin.random.Random

/** How a job is given in a Round of Who's who. */
enum class JobClue {
    /** Its Picture: a man or a woman at work. */
    PICTURE,

    /** The Polish word for the job, as it is written alone. */
    WORD,
}

/**
 * One question of Who's who: a job, by its [picture] or its [word] and [meaning], done by a man or
 * a woman ([gender]). The Learner finishes *On jest …* or *Ona jest …* with one of [options]: the
 * job in the instrumental ([answer]), the job as it is written alone, and other jobs in the
 * instrumental.
 */
data class WhoQuestion(
    val itemId: String,
    val word: String,
    val meaning: String,
    val picture: Picture?,
    val gender: Gender,
    val answer: String,
    val options: List<String>,
)

/**
 * A Round of Who's who, chapter 5's Game: a job is shown and the Learner says who the person is,
 * *on jest lekarzem*, by choosing the job's form after *jest*. The result is the answers right
 * first try.
 */
data class WhoRound(
    override val played: List<String>,
    val clue: JobClue,
    val run: Run<WhoQuestion>,
) : Round {
    override val game: Game get() = Game.WHOS_WHO
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.itemId }

    /** The question being shown, or null once the Round is finished. */
    val question: WhoQuestion? get() = run.current

    /** The answer to the question being shown, until the Learner moves on. Its text is the option chosen. */
    val given: Given? get() = run.given

    /** Answers with the option at [option]. No change if there is none there, or the question is answered. */
    fun choose(option: Int): WhoRound {
        val question = question ?: return this
        if (given != null) return this
        val chosen = question.options.getOrNull(option) ?: return this
        return copy(run = run.answer(chosen, chosen == question.answer))
    }

    /** Moves on from an answered question. */
    fun next(): WhoRound = copy(run = run.next())
}

/**
 * Who's who: the jobs the content lists under `jobs` in practice.json, each a noun Item for a man or
 * a woman whose first form is its instrumental (*lekarz*, *lekarzem*). It opens once the Item the
 * content names as `after` (*kim*, which brings the instrumental) is learned, with four jobs.
 */
internal object WhosWho : GameRules {
    const val CLUE = "job"
    const val PICTURE = "picture"
    const val WORD = "word"

    /** The fewest jobs the Game opens with, the most a Round asks, and how many options each shows. */
    const val LEAST = 4
    const val QUESTIONS = 10
    const val OPTIONS = 4

    override val game = Game.WHOS_WHO

    private val people = setOf(Gender.MASCULINE, Gender.FEMININE)

    /** The learned jobs, given by their Picture or their word, no two alike as they are shown. */
    private fun jobs(learned: Learned, clue: String): List<Item> {
        val course = learned.course
        val after = course.jobsAfter ?: return emptyList()
        if (learned.items.none { it.id == after }) return emptyList()
        val jobs = learned.items.filter { it.id in course.jobs && it.forms.isNotEmpty() && course.genders[it.id] in people }
            .filter { clue != PICTURE || it.id in course.pictures }
        return learned.unalike(jobs, if (clue == PICTURE) Alike.PICTURE else Alike.MEANING)
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (jobs(learned, WORD).size < LEAST) GameState.NEEDS_MORE else GameState.READY

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val clues = listOf(PICTURE, WORD).filter { jobs(learned, it).size >= LEAST }
        return listOf(choice(CLUE, clues.map { OptionView(it) }, wanted.getOrNull(0)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val genders = learned.course.genders
        // The other options come from every learned job, pictured or not.
        val all = jobs(learned, WORD)
        val questions = random.draw(jobs(learned, selected[0]), QUESTIONS, seen, avoidFirst) { it.id }.map { job ->
            val gender = genders.getValue(job.id)
            val answer = job.forms.first()
            // Jobs of the same gender first, so the ending alone does not give the answer away.
            val others = all.filter { it.id != job.id }.shuffled(random).sortedBy { if (genders[it.id] == gender) 0 else 1 }
                .map { it.forms.first() }.filter { it != answer && it != job.text }.distinct().take(OPTIONS - 2)
            WhoQuestion(
                job.id,
                job.text,
                learned.localize(job.meaning),
                learned.course.pictures[job.id],
                gender,
                answer,
                options = (listOf(answer, job.text) + others).shuffled(random),
            )
        }
        val clue = if (selected[0] == PICTURE) JobClue.PICTURE else JobClue.WORD
        return WhoRound(selected, clue, Run(questions))
    }
}
