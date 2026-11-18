package com.polisheasy.engine

import kotlin.random.Random

/** The words for him, her, it and them that Pronoun swap offers: alone after a verb, and after *na*. */
object Pronouns {
    val ALONE = listOf("go", "ją", "je", "ich")
    val AFTER_NA = listOf("niego", "nią", "nich")

    /** The pronouns to choose from for [noun] in [text]: those after *na* when it stands just before the noun. */
    fun offeredFor(text: String, noun: String): List<String> {
        val before = text.substringBefore(noun).trimEnd().substringAfterLast(' ').lowercase()
        return if (before == "na") AFTER_NA else ALONE
    }
}

/**
 * One sentence of Pronoun swap: [text] (*Kocham tatę.*) with what it means, the [noun] it names
 * (*tatę*), the [pronoun] said in its place (*go*), what the shorter sentence [short] means, and the
 * [items] both use.
 */
data class SwapSentence(val text: String, val meaning: Localized, val noun: String, val pronoun: String, val shortMeaning: Localized, val items: List<String>) {
    /** The sentence with the pronoun in the noun's place: *Kocham go.* */
    val short: String get() = text.replaceFirst(noun, pronoun)

    /** The pronouns offered for the noun. */
    val offered: List<String> get() = Pronouns.offeredFor(text, noun)
}

/** One question of Pronoun swap: the sentence, cut around its noun, the pronouns [offered], and the right one. */
data class SwapQuestion(
    val key: String,
    val text: String,
    val before: String,
    val noun: String,
    val after: String,
    val meaning: String,
    val short: String,
    val shortMeaning: String,
    val offered: List<String>,
    val pronoun: String,
)

/**
 * A Round of Pronoun swap, chapter 14's second Game. A sentence is written with its noun marked,
 * *Kocham **tatę**.*, and with [listen] on also said, and the Learner taps the word that says it
 * shorter: *go*, *ją*, *je* or *ich*, or after *na*, *niego*, *nią* or *nich*. Once answered the
 * shorter sentence is written, *Kocham go.* The result is the answers right first try.
 */
data class SwapRound(
    override val played: List<String>,
    val listen: Boolean,
    val run: Run<SwapQuestion>,
) : Round {
    override val game: Game get() = Game.PRONOUN_SWAP
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The sentence being asked, or null once the Round is finished. */
    val question: SwapQuestion? get() = run.current

    /** The answer to the sentence being asked, until the Learner moves on: the pronoun tapped. */
    val given: Given? get() = run.given

    /** Taps the pronoun at [option] of those offered. No change for no such pronoun, or once answered. */
    fun choose(option: Int): SwapRound {
        val question = question ?: return this
        val pronoun = question.offered.getOrNull(option) ?: return this
        return copy(run = run.answer(pronoun, pronoun == question.pronoun))
    }

    /** Moves on from an answered sentence. */
    fun next(): SwapRound = copy(run = run.next())
}

/**
 * Pronoun swap: the sentences of `swaps` in practice.json. It opens once [FEWEST] sentences have
 * all their Items learned.
 */
internal object PronounSwap : GameRules {
    const val WAY = "sentences"
    const val LISTEN = "listen"
    const val READ = "read"

    /** How many sentences a Round asks, at most, and how many must be learned for it to open. */
    const val QUESTIONS = 8
    const val FEWEST = 4

    override val game = Game.PRONOUN_SWAP

    private fun sentences(learned: Learned, listen: Boolean): List<SwapSentence> {
        val known = learned.items.map { it.id }.toSet()
        return learned.course.swaps.filter { known.containsAll(it.items) }
            .filter { !listen || learned.course.clipFor(it.text) != null }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (sentences(learned, listen = false).size >= FEWEST) GameState.READY else GameState.NEEDS_MORE

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val ways = listOfNotNull(LISTEN.takeIf { sound && sentences(learned, listen = true).size >= FEWEST }, READ)
        return listOf(choice(WAY, ways.map { OptionView(it) }, wanted.getOrNull(0)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val listen = selected[0] == LISTEN
        val questions = random.draw(sentences(learned, listen), QUESTIONS, seen, avoidFirst) { it.text }.map { sentence ->
            val at = sentence.text.indexOf(sentence.noun)
            SwapQuestion(
                key = sentence.text,
                text = sentence.text,
                before = sentence.text.substring(0, at),
                noun = sentence.noun,
                after = sentence.text.substring(at + sentence.noun.length),
                meaning = learned.localize(sentence.meaning),
                short = sentence.short,
                shortMeaning = learned.localize(sentence.shortMeaning),
                offered = sentence.offered,
                pronoun = sentence.pronoun,
            )
        }
        return SwapRound(selected, listen, Run(questions))
    }
}
