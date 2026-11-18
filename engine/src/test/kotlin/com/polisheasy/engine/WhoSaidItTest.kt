package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WhoSaidItTest {
    private val words = listOf(item("bylem", "byłem"), item("bylam", "byłam"), item("bylismy", "byliśmy"), item("bylysmy", "byłyśmy"), item("tu"), item("tam"), item("w-domu", "w domu"))

    private fun line(text: String, who: Speaker, vararg items: String) = SpeakerLine(text, mapOf("en" to "$text (en)"), who, items.toList())

    private val lines = listOf(
        line("Byłem tu.", Speaker.MAN, "bylem", "tu"), line("Byłem tam.", Speaker.MAN, "bylem", "tam"), line("Byłem w domu.", Speaker.MAN, "bylem", "w-domu"),
        line("Byłam tu.", Speaker.WOMAN, "bylam", "tu"), line("Byłam tam.", Speaker.WOMAN, "bylam", "tam"), line("Byłam w domu.", Speaker.WOMAN, "bylam", "w-domu"),
        line("Byliśmy tu.", Speaker.MEN, "bylismy", "tu"), line("Byliśmy tam.", Speaker.MEN, "bylismy", "tam"),
        line("Byłyśmy tu.", Speaker.WOMEN, "bylysmy", "tu"), line("Byłyśmy tam.", Speaker.WOMEN, "bylysmy", "tam"),
    )

    /** l1 teaches what a man and a woman say, l2 the groups. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *(words.take(2) + words.drop(4)).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l2", *words.drop(2).take(2).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
            ),
        ),
        words,
        speakers = lines,
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun Practice.round(): SpeakerRound = start(setup(Game.WHO_SAID_IT, listOf(WhoSaidIt.READ))) as SpeakerRound

    @Test
    fun `the speaker is heard in the verb's ending`() {
        assertEquals(setOf(Speaker.MAN), Speaker.heardIn("Wczoraj byłem w kinie."))
        assertEquals(setOf(Speaker.WOMAN), Speaker.heardIn("W niedzielę spałam i czytałam."))
        assertEquals(setOf(Speaker.MEN), Speaker.heardIn("Bardzo się martwiliśmy."))
        assertEquals(setOf(Speaker.WOMEN), Speaker.heardIn("Byłyśmy na treningu."))
        assertEquals(emptySet(), Speaker.heardIn("Ona była w domu."))
    }

    @Test
    fun `Who said it opens with four lines of a man and a woman, the groups offered once learned`() {
        val state = { p: Player -> p.practice().view().games.single { it.game == Game.WHO_SAID_IT }.state }
        assertEquals(GameState.NEEDS_MORE, state(player()))
        assertEquals(GameState.NEEDS_MORE, state(player().completed("l2")))
        assertEquals(GameState.READY, state(player().completed("l1")))
        assertEquals(listOf(Speaker.MAN, Speaker.WOMAN), player().completed("l1").practice().round().speakers)
        assertEquals(Speaker.entries, player().completed("l1", "l2").practice().round().speakers)
    }

    @Test
    fun `a Round asks eight lines, each speaker at least twice`() {
        repeat(10) { seed ->
            val round = player(seed).completed("l1", "l2").practice().round()
            assertEquals(WhoSaidIt.QUESTIONS, round.total)
            assertEquals(WhoSaidIt.QUESTIONS, round.run.questions.map { it.text }.toSet().size)
            Speaker.entries.forEach { speaker -> assertTrue(round.run.questions.count { it.who == speaker } >= 2, "$speaker in ${round.run.questions}") }
        }
    }

    @Test
    fun `the right speaker is right first try, a wrong one is kept until the Learner moves on`() {
        var round = player().completed("l1", "l2").practice().round()
        val first = checkNotNull(round.question)
        round = round.choose(round.speakers.indexOf(first.who))
        assertEquals(true, round.given?.correct)
        assertEquals(round, round.choose(0))
        round = round.next()
        val second = checkNotNull(round.question)
        round = round.choose(round.speakers.indexOfFirst { it != second.who })
        assertEquals(false, round.given?.correct)
        round = round.next()
        while (!round.finished) {
            val question = checkNotNull(round.question)
            round = round.choose(round.speakers.indexOf(question.who)).next()
        }
        assertEquals(Score(WhoSaidIt.QUESTIONS - 1, of = WhoSaidIt.QUESTIONS), round.score)
    }
}
