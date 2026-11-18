package com.polisheasy.engine

import kotlin.random.Random

/**
 * Who says a sentence in the past, which its verb shows by its ending: a man (*byłem*), a woman
 * (*byłam*), a group with a man in it (*byliśmy*) or women only (*byłyśmy*).
 */
enum class Speaker(val id: String, val ending: String) {
    MAN("man", "łem"),
    WOMAN("woman", "łam"),
    MEN("men", "liśmy"),
    WOMEN("women", "łyśmy"),
    ;

    companion object {
        fun of(id: String): Speaker? = entries.firstOrNull { it.id == id }

        /** The speakers whose endings end a word of [text], ignoring *się*. */
        fun heardIn(text: String): Set<Speaker> {
            val words = text.lowercase().split(Regex("[^\\p{L}]+")).filter { it.isNotEmpty() }
            return entries.filter { speaker -> words.any { it.endsWith(speaker.ending) } }.toSet()
        }
    }
}

/** One line of Who said it?: [text] in the past (*Wczoraj byłam w kinie.*), what it means, who says it, and the [items] it uses. */
data class SpeakerLine(val text: String, val meaning: Localized, val who: Speaker, val items: List<String>)

/** One question of Who said it?: a line, said or written in [text] with what it [means], said by [who]. */
data class SpeakerQuestion(val key: String, val text: String, val meaning: String, val who: Speaker)

/**
 * A Round of Who said it?, chapter 13's second Game. A sentence in the past is said, or with
 * [listen] off written, and the Learner taps who said it, from [speakers]: a man, a woman, a group
 * with a man in it or women only. The verb's ending tells: *byłem*, *byłam*, *byliśmy*, *byłyśmy*.
 * The result is the answers right first try.
 */
data class SpeakerRound(
    override val played: List<String>,
    val listen: Boolean,
    val speakers: List<Speaker>,
    val run: Run<SpeakerQuestion>,
) : Round {
    override val game: Game get() = Game.WHO_SAID_IT
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.key }

    /** The line being asked, or null once the Round is finished. */
    val question: SpeakerQuestion? get() = run.current

    /** The answer to the line being asked, until the Learner moves on: the id of the speaker tapped. */
    val given: Given? get() = run.given

    /** Taps the speaker at [option]. No change for no such speaker, or once answered. */
    fun choose(option: Int): SpeakerRound {
        val question = question ?: return this
        val speaker = speakers.getOrNull(option) ?: return this
        return copy(run = run.answer(speaker.id, speaker == question.who))
    }

    /** Moves on from an answered line. */
    fun next(): SpeakerRound = copy(run = run.next())
}

/**
 * Who said it?: the lines of `speakers` in practice.json. It opens once [FEWEST] lines have all
 * their Items learned, said by a man and by a woman at least; the groups are offered once a line of
 * theirs is learned.
 */
internal object WhoSaidIt : GameRules {
    const val WAY = "lines"
    const val LISTEN = "listen"
    const val READ = "read"

    /** How many lines a Round asks, at most, and how many must be learned for it to open. */
    const val QUESTIONS = 8
    const val FEWEST = 4

    override val game = Game.WHO_SAID_IT

    private fun lines(learned: Learned, listen: Boolean): List<SpeakerLine> {
        val known = learned.items.map { it.id }.toSet()
        return learned.course.speakers.filter { known.containsAll(it.items) }
            .filter { !listen || learned.course.clipFor(it.text) != null }.distinctBy { it.text }
    }

    private fun enough(lines: List<SpeakerLine>) =
        lines.size >= FEWEST && lines.map { it.who }.containsAll(listOf(Speaker.MAN, Speaker.WOMAN))

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (enough(lines(learned, listen = false))) GameState.READY else GameState.NEEDS_MORE

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val ways = listOfNotNull(LISTEN.takeIf { sound && enough(lines(learned, listen = true)) }, READ)
        return listOf(choice(WAY, ways.map { OptionView(it) }, wanted.getOrNull(0)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val listen = selected[0] == LISTEN
        val all = lines(learned, listen)
        val speakers = Speaker.entries.filter { speaker -> all.any { it.who == speaker } }
        // Each speaker in turn, those not asked today first, so a Round is never all one speaker.
        val bySpeaker = speakers.map { speaker -> random.draw(all.filter { it.who == speaker }, QUESTIONS, seen, null) { it.text } }
        val mixed = (0 until QUESTIONS).flatMap { turn -> bySpeaker.mapNotNull { it.getOrNull(turn) } }.take(QUESTIONS).shuffled(random)
        val ordered = if (mixed.size > 1 && mixed.first().text == avoidFirst) mixed.drop(1) + mixed.first() else mixed
        val questions = ordered.map { SpeakerQuestion(it.text, it.text, learned.localize(it.meaning), it.who) }
        return SpeakerRound(selected, listen, speakers, Run(questions))
    }
}
