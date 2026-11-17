package com.polisheasy.engine

import java.text.Normalizer
import java.util.Locale
import kotlin.random.Random

/** A set of spellings that sound alike to a new ear, such as sz, ś and s. [id] is stable. */
enum class SoundGroup(val id: String, val letters: List<String>) {
    SZ("sz", listOf("sz", "ś", "s")),
    CZ("cz", listOf("cz", "ć", "c")),
    Z("z", listOf("ż", "ź", "z")),
    DZ("dz", listOf("dz", "dź", "dż")),
    L("l", listOf("l", "ł")),
    E("e", listOf("e", "ę")),
    A("a", listOf("a", "ą")),
    N("n", listOf("n", "ń")),
}

/**
 * A place in a learned Item's Polish where one of a [group]'s spellings is written: the text
 * [before] it, the [answer], and the text [after]. Nothing is written in the gap but the answer,
 * so a wrong spelling is never shown as a word.
 */
data class SoundGap(val itemId: String, val polish: String, val group: SoundGroup, val before: String, val answer: String, val after: String) {
    /** The word the gap is in, as each wrong option would spell it. */
    val wrongWords: List<String>
        get() {
            val start = before.substringAfterLast(' ')
            val end = after.substringBefore(' ')
            return group.letters.filter { it != answer }.map { start + it + end }
        }
}

/** The letters Polish writes as two: each is one sound, and one piece of a word. */
private val digraphs = listOf("dź", "dż", "dz", "sz", "cz", "rz", "ch")

/** [text] in pieces, each a letter or a pair of letters written for one sound, or anything else as it is. */
private fun pieces(text: String): List<String> {
    val pieces = mutableListOf<String>()
    var at = 0
    while (at < text.length) {
        val pair = digraphs.firstOrNull { text.startsWith(it, at, ignoreCase = true) }
        val piece = pair?.let { text.substring(at, at + it.length) } ?: text[at].toString()
        pieces += piece
        at += piece.length
    }
    return pieces
}

/** The spellings that mean another sound before an i: "si" is said as ś, "ci" as ć, and so on. */
private val softened = setOf("s", "c", "z", "n", "dz")

/**
 * Every gap [item]'s Polish offers. A capital is never a gap, since a name would give itself
 * away, nor a spelling softened by the i after it, which is not said the way it is written. A gap
 * one of whose wrong options would spell a word in [realWords] is left out.
 */
internal fun soundGaps(item: Item, realWords: Set<String> = emptySet()): List<SoundGap> {
    val text = Normalizer.normalize(item.text, Normalizer.Form.NFC)
    val pieces = pieces(text)
    return pieces.indices.mapNotNull { index ->
        val piece = pieces[index]
        val group = SoundGroup.entries.firstOrNull { piece in it.letters } ?: return@mapNotNull null
        if (piece in softened && pieces.getOrNull(index + 1)?.startsWith("i", ignoreCase = true) == true) return@mapNotNull null
        SoundGap(item.id, text, group, pieces.take(index).joinToString(""), piece, pieces.drop(index + 1).joinToString(""))
            .takeIf { gap -> gap.wrongWords.none { it.lowercase(Locale.ROOT) in realWords } }
    }
}

/** Every gap Sound gaps can ask of this course, for the Polish check: none of their wrong options may spell a real word. */
fun Course.soundGaps(): List<SoundGap> = items.flatMap { soundGaps(it, soundGapRealWords) }

/** One question of Sound gaps: the [gap], what the Item means, and the spellings to choose from. */
data class GapQuestion(val gap: SoundGap, val meaning: String) {
    val options: List<String> get() = gap.group.letters
}

/**
 * A Round of Sound gaps: a learned word is said and shown with a gap where a hard sound is
 * written, and the Learner chooses the letters that belong there. The result is the answers right
 * first try.
 */
data class GapRound(
    override val played: List<String>,
    val run: Run<GapQuestion>,
) : Round {
    override val game: Game get() = Game.SOUND_GAPS
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished
    override val step: Int get() = run.index
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = run.questions.map { it.gap.itemId }

    /** The question being shown, or null once the Round is finished. */
    val question: GapQuestion? get() = run.current

    /** The answer to the question being shown, until the Learner moves on. Its text is the letters chosen. */
    val given: Given? get() = run.given

    /** Answers with the letters at [option]. No change if there are none there, or the question is answered. */
    fun choose(option: Int): GapRound {
        val question = question ?: return this
        val letters = question.options.getOrNull(option) ?: return this
        return copy(run = run.answer(letters, letters == question.gap.answer))
    }

    /** Moves on from an answered question. */
    fun next(): GapRound = copy(run = run.next())
}

internal object SoundGaps : GameRules {
    const val SOUNDS = "sounds"
    const val MIXED = "mixed"

    /** The fewest Items a sound is offered with, and the most words a Round says. */
    const val LEAST = 4
    const val QUESTIONS = 10

    override val game = Game.SOUND_GAPS

    /** The gaps of every learned Item that has a Clip, by Item. */
    private fun gaps(learned: Learned): List<List<SoundGap>> =
        learned.unalike(learned.items, by = null)
            .filter { learned.course.clipFor(it.text) != null }
            .map { soundGaps(it, learned.course.soundGapRealWords) }
            .filter { it.isNotEmpty() }

    /** Each sound with enough Items to drill it, and all of them as "mixed" when more than one has, or none. */
    private fun levels(learned: Learned): List<Pair<OptionView, List<List<SoundGap>>>> {
        val all = gaps(learned)
        if (all.size < LEAST) return emptyList()
        val sounds = SoundGroup.entries.map { group ->
            OptionView(group.id, group.letters.joinToString(" · ")) to all.map { gaps -> gaps.filter { it.group == group } }.filter { it.isNotEmpty() }
        }.filter { it.second.size >= LEAST }
        return if (sounds.size == 1 && sounds.single().second.size == all.size) sounds else sounds + (OptionView(MIXED) to all)
    }

    override fun state(learned: Learned, sound: Boolean): GameState = when {
        levels(learned).isEmpty() -> GameState.NEEDS_MORE
        !sound -> GameState.NEEDS_SOUND
        else -> GameState.READY
    }

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> =
        listOf(choice(SOUNDS, levels(learned).map { it.first }, wanted.getOrNull(0)))

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val level = levels(learned).first { it.first.id == selected[0] }.second
        val questions = random.draw(level, QUESTIONS, seen, avoidFirst) { it.first().itemId }.map { gaps ->
            val gap = gaps.random(random)
            GapQuestion(gap, learned.localize(learned.course.itemsById.getValue(gap.itemId).meaning))
        }
        return GapRound(selected, Run(questions))
    }
}
