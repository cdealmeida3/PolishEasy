package com.polisheasy.engine

import kotlin.random.Random

/** The gender of a Polish noun. [id] is how the content writes it. */
enum class Gender(val id: String) {
    MASCULINE("m"),
    FEMININE("f"),
    NEUTER("n"),
    ;

    companion object {
        fun of(id: String): Gender? = entries.firstOrNull { it.id == id }
    }
}

/** One of the words to sort a noun under, such as "moja": the [gender] it goes with. */
data class GenderBucket(val gender: Gender, val word: String)

/**
 * One question of Mój, moja, moje or Ten, ta, to: a noun, by its [polish], its [meaning] and its
 * [picture] when it has one, and the [gender] that says which word goes with it. [phrase] is the
 * two together, such as "moja siostra" or "ta torba", shown once the question is answered.
 */
data class GenderQuestion(
    val itemId: String,
    val polish: String,
    val meaning: String,
    val picture: Picture?,
    val gender: Gender,
    val phrase: String,
)

/** How a noun is given in a Round of Mój, moja, moje or Ten, ta, to. */
enum class NounClue {
    /** The Polish word is written. */
    WORD,

    /** The Polish word is said, and written only once the question is answered. */
    LISTEN,

    /** Only what it means is written: the Learner has to know the word as well. */
    MEANING,
}

/**
 * A Round of Mój, moja, moje or of Ten, ta, to, its [game]: a noun is shown and the Learner taps
 * the word for "my", or for "this", that goes with it, one of the [buckets]. The result is the
 * answers right first try.
 */
data class GenderRound(
    override val game: Game,
    override val played: List<String>,
    val clue: NounClue,
    val buckets: List<GenderBucket>,
    val run: Run<GenderQuestion>,
) : Round {
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.itemId }

    /** The question being shown, or null once the Round is finished. */
    val question: GenderQuestion? get() = run.current

    /** The answer to the question being shown, until the Learner moves on. Its text is the word tapped. */
    val given: Given? get() = run.given

    /** How many of the nouns answered so far belong under the word for [gender]. */
    fun sorted(gender: Gender): Int = run.questions.take(done).count { it.gender == gender }

    /** Answers with the bucket at [bucket]. No change if there is none there, or the question is answered. */
    fun choose(bucket: Int): GenderRound {
        val question = question ?: return this
        val chosen = buckets.getOrNull(bucket) ?: return this
        return copy(run = run.answer(chosen.word, chosen.gender == question.gender))
    }

    /** Moves on from an answered question. */
    fun next(): GenderRound = copy(run = run.next())
}

/** Mój, moja, moje: nouns sorted under the word for "my" that goes with them. */
internal val MojMojaMoje = GenderSort(Game.MOJ_MOJA_MOJE) { it.possessives }

/** Ten, ta, to: chapter 3's Game, nouns sorted under the word for "this" that goes with them. */
internal val TenTaTo = GenderSort(Game.TEN_TA_TO) { it.demonstratives }

/**
 * The rules of a Game that sorts nouns by gender under the [words] the content gives for each
 * gender, such as *mój, moja, moje*. A noun waits until its own word is learned.
 */
internal class GenderSort(override val game: Game, private val words: (Course) -> Map<Gender, String>) : GameRules {
    companion object {
        const val CLUE = "noun"
        const val WORD = "word"
        const val LISTEN = "listen"
        const val MEANING = "meaning"

        /** The fewest nouns the Game opens with, and the most a Round asks. */
        const val LEAST = 4
        const val QUESTIONS = 10
    }

    /** The words the Learner has learned, in the order masculine, feminine, neuter. */
    private fun buckets(learned: Learned): List<GenderBucket> {
        val known = learned.items.associateBy { it.id }
        val words = words(learned.course)
        return Gender.entries.mapNotNull { gender -> known[words[gender]]?.let { GenderBucket(gender, it.text) } }
    }

    /** The learned nouns whose gender is known and has a bucket, and which can be told apart as they are given. */
    private fun nouns(learned: Learned, clue: String): List<Item> {
        val genders = buckets(learned).map { it.gender }.toSet()
        // One bucket is no choice.
        if (genders.size < 2) return emptyList()
        val nouns = learned.items.filter { learned.course.genders[it.id] in genders }
            .filter { clue != LISTEN || learned.course.clipFor(it.text) != null }
        return learned.unalike(nouns, if (clue == MEANING) Alike.MEANING else null)
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (nouns(learned, WORD).size < LEAST) GameState.NEEDS_MORE else GameState.READY

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val clues = listOf(WORD, LISTEN, MEANING).filter { (it != LISTEN || sound) && nouns(learned, it).size >= LEAST }
        return listOf(choice(CLUE, clues.map { OptionView(it) }, wanted.getOrNull(0)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val buckets = buckets(learned)
        val questions = random.draw(nouns(learned, selected[0]), QUESTIONS, seen, avoidFirst) { it.id }.map { noun ->
            val gender = learned.course.genders.getValue(noun.id)
            GenderQuestion(
                noun.id,
                noun.text,
                learned.localize(noun.meaning),
                learned.course.pictures[noun.id],
                gender,
                phrase = "${buckets.first { it.gender == gender }.word} ${noun.text}",
            )
        }
        val clue = when (selected[0]) {
            LISTEN -> NounClue.LISTEN
            MEANING -> NounClue.MEANING
            else -> NounClue.WORD
        }
        return GenderRound(game, selected, clue, buckets, Run(questions))
    }
}
