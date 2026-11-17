package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SoundGapsTest {
    private fun gapsOf(text: String, realWords: Set<String> = emptySet()) =
        soundGaps(item("word", text), realWords).map { Triple(it.before, it.answer, it.after) }

    @Test
    fun `a gap is where one of a set of sounds that are alike is written`() {
        assertEquals(listOf(Triple("", "sz", "ukam")), gapsOf("szukam").filter { it.second == "sz" })
        assertEquals(
            listOf(Triple("", "cz", "eść"), Triple("cze", "ś", "ć"), Triple("cześ", "ć", "")),
            gapsOf("cześć").filter { it.second != "e" },
        )
        assertEquals(listOf(Triple("", "ł", "adny")), gapsOf("ładny").filter { it.second == "ł" })
        assertEquals(listOf(Triple("dzie", "ń", "")), gapsOf("dzień").filter { it.second == "ń" })
    }

    @Test
    fun `two letters written for one sound are one piece`() {
        // The s of sz, the z of rz and cz, the c of ch and the d of dź are not gaps of their own.
        assertEquals(listOf("sz"), gapsOf("sz").map { it.second })
        assertEquals(emptyList(), gapsOf("rz"))
        assertEquals(emptyList(), gapsOf("ch"))
        assertEquals(listOf("dź"), gapsOf("dź").map { it.second })
        assertEquals(listOf("dż", "e"), gapsOf("dżem").map { it.second })
    }

    @Test
    fun `a spelling softened by the i after it is not a gap`() {
        // "si" is said as ś, "ci" as ć, "zi" as ź, "ni" as ń, "dzi" as dź: the letters do not show the sound.
        assertFalse("s" in gapsOf("siedem").map { it.second })
        assertFalse("c" in gapsOf("babcia").map { it.second })
        assertFalse("z" in gapsOf("zima").map { it.second })
        assertFalse("n" in gapsOf("nie").map { it.second })
        assertFalse("dz" in gapsOf("dzień").map { it.second })
        // Before any other letter it is a gap.
        assertTrue("s" in gapsOf("syn").map { it.second })
    }

    @Test
    fun `a capital is not a gap`() {
        assertEquals(listOf("a"), gapsOf("Adam").map { it.second })
        assertEquals(emptyList(), gapsOf("Z"))
    }

    @Test
    fun `in a phrase the gap is in one word, and the wrong options respell only that word`() {
        val gap = soundGaps(item("p", "do jutra")).single()

        assertEquals(Triple("do jutr", "a", ""), Triple(gap.before, gap.answer, gap.after))
        assertEquals(listOf("jutrą"), gap.wrongWords)
        assertEquals(listOf("śukam", "sukam"), soundGaps(item("w", "szukam")).first { it.answer == "sz" }.wrongWords)
    }

    @Test
    fun `a gap whose wrong option would spell a real word is left out`() {
        // "kasa" is a word: the sz of "kasza" is not asked. Its a's still are.
        assertEquals(listOf("a", "a"), gapsOf("kasza", realWords = setOf("kasa")).map { it.second })
        assertEquals(listOf("a", "sz", "a"), gapsOf("kasza").map { it.second })
        // Capitals in the Item do not hide a clash.
        assertEquals(listOf("a", "a"), gapsOf("Kasza", realWords = setOf("kasa")).map { it.second }.takeLast(2))
        assertFalse("sz" in gapsOf("Kasza", realWords = setOf("kasa")).map { it.second })
    }

    // The Game

    /** Eight words with a hard sound, a word with none ("oko") and a word with no Clip ("ryż"). */
    private val words = listOf("szukam", "proszę", "nasz", "wasz", "ładny", "miły", "długo", "lubi", "oko", "ryż")
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("g1", *words.take(4).map { choose("e-$it", it, it) }.toTypedArray()),
                day("g2", *words.drop(4).map { choose("e-$it", it, it) }.toTypedArray()),
            ),
        ),
        words.map { item(it) },
        clips = (words - "ryż").map { clip(it) },
    )

    private fun player(course: Course = this.course) = Player(course = course)

    private fun Practice.gaps(vararg wanted: String): GapRound = start(setup(Game.SOUND_GAPS, wanted.toList())) as GapRound

    private fun stateOf(player: Player, sound: Boolean = true) =
        player.practice().view(sound).games.single { it.game == Game.SOUND_GAPS }.state

    private fun GapRound.playOut(wrong: Int = 0): GapRound {
        var round = this
        var missed = 0
        while (!round.finished) {
            val asked = checkNotNull(round.question)
            val right = asked.options.indexOf(asked.gap.answer)
            round = round.choose(if (missed++ < wrong) (right + 1) % asked.options.size else right).next()
        }
        return round
    }

    @Test
    fun `Sound gaps opens once four learned words with a Clip have a hard sound, and only with sound on`() {
        val quiet = course.copy(clips = listOf("szukam", "proszę", "nasz").map { clip(it) })
        assertEquals(GameState.NEEDS_MORE, stateOf(player(quiet).completed("g1")))
        assertEquals(GameState.READY, stateOf(player().completed("g1")))
        assertEquals(GameState.NEEDS_SOUND, stateOf(player().completed("g1"), sound = false))
        assertFailsWith<IllegalStateException> {
            player().completed("g1").practice().let { it.start(it.setup(Game.SOUND_GAPS), sound = false) }
        }
    }

    @Test
    fun `a sound is offered with four words to drill it, and all of them together`() {
        val early = player().completed("g1").practice().setup(Game.SOUND_GAPS).choices.single()
        // All four words have sz, ś or s: that one sound is all there is, so there is no "mixed".
        assertEquals(listOf(OptionView("sz", "sz · ś · s")), early.options)

        val later = player().completed("g1", "g2").practice().setup(Game.SOUND_GAPS).choices.single()
        // Three words have an e or an ę, and that is too few.
        assertEquals(listOf("sz", "l", "a", SoundGaps.MIXED), later.options.map { it.id })
        assertEquals("l · ł", later.options[1].label)
    }

    @Test
    fun `a Round asks each word once, with a gap of the sound chosen`() {
        val practice = player().completed("g1", "g2").practice()

        val round = practice.gaps("l")
        assertEquals(setOf("ładny", "miły", "długo", "lubi"), round.asked.toSet())
        assertTrue(round.run.questions.all { it.gap.group == SoundGroup.L })
        assertTrue(round.run.questions.all { it.options == listOf("l", "ł") })

        val mixed = practice.gaps(SoundGaps.MIXED)
        // "oko" has no hard sound, and "ryż" cannot be heard.
        assertEquals(setOf("szukam", "proszę", "nasz", "wasz", "ładny", "miły", "długo", "lubi"), mixed.asked.toSet())
        mixed.run.questions.forEach { question ->
            assertEquals(question.gap.itemId, question.gap.before + question.gap.answer + question.gap.after)
            assertEquals("${question.gap.itemId} in English", question.meaning)
        }
    }

    @Test
    fun `the letters chosen answer the question, right or wrong`() {
        val start = player().completed("g1").practice().gaps()
        val asked = checkNotNull(start.question)
        val right = asked.options.indexOf(asked.gap.answer)

        assertEquals(Given(asked.gap.answer, correct = true), start.choose(right).given)
        val wrong = (right + 1) % asked.options.size
        assertEquals(Given(asked.options[wrong], correct = false), start.choose(wrong).given)
        // A second choice changes nothing, nor one that is no option, nor moving on unanswered.
        assertEquals(start.choose(right), start.choose(right).choose(wrong))
        assertEquals(start, start.choose(9))
        assertEquals(start, start.next())
        assertEquals(1, start.choose(right).next().step)
    }

    @Test
    fun `the result is the answers right first try, and the most is the Best`() {
        val practice = player().completed("g1").practice()

        val first = practice.finish(practice.gaps().playOut(wrong = 1))
        assertEquals(RoundResult(Game.SOUND_GAPS, listOf("sz"), Score(3, of = 4), best = 3, first = true), first)
        assertTrue(practice.finish(practice.gaps().playOut()).newBest)
    }
}
