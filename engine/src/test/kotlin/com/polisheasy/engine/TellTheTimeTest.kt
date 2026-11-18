package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TellTheTimeTest {
    private val hourNames = listOf(
        "pierwsza", "druga", "trzecia", "czwarta", "piąta", "szósta", "siódma", "ósma", "dziewiąta", "dziesiąta", "jedenasta", "dwunasta",
    )
    private val hours = hourNames.mapIndexed { index, name -> item("h${index + 1}", name).copy(forms = listOf(name.dropLast(1) + "ej")) }
    private val words = listOf(item("po-godz", "po"), item("za-godz", "za"), item("wpol-do", "wpół do"), item("kwadrans"))
    private val numbers = listOf(item("piec", "pięć"), item("dziesiec", "dziesięć"), item("dwadziescia", "dwadzieścia"))

    /** l1 teaches four hours, l2 the rest, l3 past, to, half and a quarter, l4 the numbers. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *hours.take(4).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l2", *hours.drop(4).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l3", *words.map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l4", *numbers.map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
            ),
        ),
        hours + words + numbers,
        pictures = mapOf("piec" to Picture.Glyph("5"), "dziesiec" to Picture.Glyph("10"), "dwadziescia" to Picture.Glyph("20")),
        clock = ClockWords(hours.map { it.id }, "po-godz", "za-godz", "wpol-do", "kwadrans"),
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun Practice.round(way: String = TellTheTime.READ): TimeRound = start(setup(Game.TELL_THE_TIME, listOf(way))) as TimeRound

    private fun said(hour: Int, minute: Int) = course.clockTimes(course.itemsById::get, mapOf(5 to numbers[0], 10 to numbers[1], 20 to numbers[2]))
        .single { it.hour == hour && it.minute == minute }.polish

    @Test
    fun `a time is said on the hour, past, half to the next hour and to it`() {
        assertEquals("ósma", said(8, 0))
        assertEquals("pięć po ósmej", said(8, 5))
        assertEquals("kwadrans po ósmej", said(8, 15))
        assertEquals("dwadzieścia po ósmej", said(8, 20))
        assertEquals("wpół do dziewiątej", said(8, 30))
        assertEquals("za dwadzieścia dziewiąta", said(8, 40))
        assertEquals("za kwadrans dziewiąta", said(8, 45))
        assertEquals("za pięć dziewiąta", said(8, 55))
        assertEquals("wpół do pierwszej", said(12, 30))
        assertEquals("za pięć pierwsza", said(12, 55))
        assertEquals(12 * CLOCK_MINUTES.size, course.clockPhrases().toSet().size)
    }

    @Test
    fun `Tell the time opens with four hours, told or found`() {
        val state = { p: Player -> p.practice().view().games.single { it.game == Game.TELL_THE_TIME }.state }
        assertEquals(GameState.NEEDS_MORE, state(player()))
        assertEquals(GameState.NEEDS_MORE, state(player().completed("l3", "l4")))
        assertEquals(GameState.READY, state(player().completed("l1")))
        assertEquals(listOf(TellTheTime.READ, TellTheTime.FIND), player().completed("l1").practice().setup(Game.TELL_THE_TIME).choices[0].options.map { it.id })
    }

    @Test
    fun `with only the hours learned, every time asked is on the hour`() {
        val round = player().completed("l1").practice().round()
        assertEquals(4, round.total)
        round.run.questions.forEach { question ->
            assertEquals(0, question.time.minute)
            assertTrue(question.time.hour in 1..4)
        }
    }

    @Test
    fun `a Round asks eight times, each with four different options, the time among them and the hour either side first`() {
        repeat(10) { seed ->
            val round = player(seed).completed("l1", "l2", "l3", "l4").practice().round()
            assertEquals(TellTheTime.QUESTIONS, round.total)
            assertEquals(TellTheTime.QUESTIONS, round.run.questions.map { it.time.key }.toSet().size)
            round.run.questions.forEach { question ->
                assertEquals(TellTheTime.OPTIONS, question.options.size)
                assertEquals(TellTheTime.OPTIONS, question.options.map { it.key }.toSet().size)
                assertEquals(TellTheTime.OPTIONS, question.options.map { it.polish }.toSet().size)
                assertTrue(question.time in question.options)
                val time = question.time
                val next = time.hour % 12 + 1
                // The hour after is always a time that can be said, so it is always offered.
                assertTrue(question.options.any { it.hour == next && it.minute == time.minute }, "${time.polish}: ${question.options.map { it.polish }}")
            }
        }
    }

    @Test
    fun `the right option is right first try, a wrong one is kept until the Learner moves on`() {
        var round = player().completed("l1", "l2", "l3", "l4").practice().round(TellTheTime.FIND)
        assertTrue(!round.read)
        val first = checkNotNull(round.question)
        round = round.choose(first.options.indexOf(first.time))
        assertEquals(true, round.given?.correct)
        assertEquals(round, round.choose(0))
        round = round.next()
        val second = checkNotNull(round.question)
        round = round.choose(second.options.indexOfFirst { it != second.time })
        assertEquals(false, round.given?.correct)
        round = round.next()
        while (!round.finished) {
            val question = checkNotNull(round.question)
            round = round.choose(question.options.indexOf(question.time)).next()
        }
        assertEquals(Score(TellTheTime.QUESTIONS - 1, of = TellTheTime.QUESTIONS), round.score)
    }
}
