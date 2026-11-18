package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LottoTest {
    private val units = listOf("jeden", "dwa", "trzy", "cztery", "pięć", "sześć", "siedem", "osiem", "dziewięć")
    private val tens = listOf("dziesięć", "dwadzieścia", "trzydzieści")
    private val hundreds = listOf("sto", "dwieście", "trzysta", "czterysta", "pięćset", "sześćset", "siedemset", "osiemset", "dziewięćset")

    /** l1 teaches 1 to 9 and the tens to 30, l2 the hundreds, l3 tysiąc. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *(units + tens).map { choose("e-$it", it, it) }.toTypedArray()),
                day("l2", *hundreds.map { choose("e-$it", it, it) }.toTypedArray()),
                day("l3", choose("e-tysiac", "tysiąc", "tysiąc")),
            ),
        ),
        (units + tens + hundreds + "tysiąc").map { item(it) },
        clips = (units + tens + hundreds + "tysiąc").map { clip(it) },
        pictures = units.withIndex().associate { (i, name) -> name to Picture.Glyph("${i + 1}") } +
            tens.withIndex().associate { (i, name) -> name to Picture.Glyph("${(i + 1) * 10}") } +
            hundreds.withIndex().associate { (i, name) -> name to Picture.Glyph("${(i + 1) * 100}") } +
            mapOf("tysiąc" to Picture.Glyph("1000")),
    )

    private fun player(course: Course = this.course, seed: Int = 0) = Player(course = course, seed = seed)

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.LOTTO }.state

    private fun Practice.lotto(vararg wanted: String, sound: Boolean = true): LottoRound =
        start(setup(Game.LOTTO, wanted.toList(), sound), sound) as LottoRound

    @Test
    fun `Lotto opens once a card of round hundreds can be said`() {
        assertEquals(GameState.NEEDS_MORE, stateOf(player().completed("l1")))
        // Sto and the units and tens say many numbers, but the Game waits for the hundreds.
        val stoOnly = course.copy(pictures = course.pictures.filterKeys { it !in hundreds.drop(1) })
        assertEquals(GameState.NEEDS_MORE, stateOf(player(stoOnly).completed("l1", "l2")))
        assertEquals(GameState.READY, stateOf(player().completed("l1", "l2")))
        assertEquals(GameState.READY, player().completed("l2").practice().view(sound = false).games.single { it.game == Game.LOTTO }.state)
    }

    @Test
    fun `the round hundreds are a level, and any number up to a thousand another once it can be said`() {
        val hundredsOnly = player().completed("l2").practice()
        assertEquals(listOf(Lotto.HUNDREDS), hundredsOnly.setup(Game.LOTTO, listOf(Lotto.READ)).choices[1].options.map { it.id })
        val all = player().completed("l1", "l2", "l3").practice()
        assertEquals(listOf(Lotto.HUNDREDS, Lotto.TO_1000), all.setup(Game.LOTTO, listOf(Lotto.READ)).choices[1].options.map { it.id })
    }

    @Test
    fun `a number is said as its hundred and the rest, and the card holds every number called, smallest first`() {
        repeat(10) { seed ->
            val round = player(seed = seed).completed("l1", "l2", "l3").practice().lotto(Lotto.READ, Lotto.TO_1000)
            assertEquals(Lotto.CARD, round.card.toSet().size)
            assertEquals(round.card.sorted(), round.card)
            assertEquals(Lotto.CALLS, round.run.questions.map { it.number }.toSet().size)
            round.run.questions.forEach { question ->
                assertTrue(question.number in round.card)
                assertEquals(say(question.number), question.words, "${question.number}")
            }
        }
    }

    /** The words of [n] in this course: its hundred, then a unit, a ten, or a ten and a unit; no teens are learned. */
    private fun say(n: Int): List<String> {
        if (n == 1000) return listOf("tysiąc")
        val rest = n % 100
        return listOf(hundreds[n / 100 - 1]) + when {
            rest == 0 -> emptyList()
            rest < 10 -> listOf(units[rest - 1])
            rest % 10 == 0 -> listOf(tens[rest / 10 - 1])
            rest > 20 -> listOf(tens[rest / 10 - 1], units[rest % 10 - 1])
            else -> error("$n cannot be said")
        }
    }

    @Test
    fun `a number is ticked once on the card, and the result is the numbers ticked right first try`() {
        var round = player().completed("l1", "l2", "l3").practice().lotto(Lotto.READ, Lotto.TO_1000)
        val first = checkNotNull(round.question).number
        assertEquals(round, round.tick(1))
        assertEquals(round, round.next())
        val wrong = round.card.first { it != first }
        round = round.tick(wrong)
        assertEquals(Given("$wrong", false), round.given)
        assertEquals(round, round.tick(first))
        round = round.next()
        assertEquals(setOf(first), round.ticked)
        assertEquals(round, round.tick(first))
        while (!round.finished) round = round.tick(checkNotNull(round.question).number).next()
        assertEquals(Score(Lotto.CALLS - 1, of = Lotto.CALLS), round.score)
        assertEquals(round.run.questions.map { it.number }.toSet(), round.ticked)
    }

    @Test
    fun `heard, only numbers with a Clip of every word are called, and with sound off it is read`() {
        val noSto = course.copy(clips = course.clips.filter { it.text != "sto" })
        val practice = player(noSto).completed("l1", "l2", "l3").practice()
        val round = practice.lotto(Lotto.LISTEN, Lotto.TO_1000)
        assertTrue(round.listen)
        assertTrue(round.run.questions.none { "sto" in it.words })
        assertTrue(round.card.none { it in 100..199 })
        assertEquals(listOf(Lotto.READ), practice.setup(Game.LOTTO, sound = false).choices[0].options.map { it.id })
    }
}
