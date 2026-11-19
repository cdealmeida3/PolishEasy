package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CalendarTest {
    private val words = listOf(
        item("jest", "jest", "is"), item("pierwszy", "pierwszy", "first"), item("drugi", "drugi", "second"),
        item("stycznia", "stycznia", "of January"), item("maja", "maja", "of May"), item("nowy-rok", "Nowy Rok", "New Year's Day"),
        item("swieto", "święto", "a holiday"), item("dzien", "dzień", "a day"),
    )
    private val later = listOf(item("czerwca", "czerwca", "of June"), item("dziecka", "Dziecka", "of the Child"))

    private fun text(text: String, vararg items: String) = FlatText(text, mapOf("en" to text), items.toList())

    private fun page(id: String, date: String, holiday: String, month: String, vararg also: String) = CalendarPage(
        id,
        date,
        holiday,
        listOf(text("pierwszego $month", "pierwszy", month), text("pierwszy $month", "pierwszy", month), text("drugiego $month", "drugi", month)),
        0,
        text("$holiday jest pierwszego $month.", "jest", "pierwszy", month, *also),
    )

    private val pages = listOf(
        page("nowy-rok", "01.01", "Nowy Rok", "stycznia", "nowy-rok"),
        page("swieto-maja", "01.05", "Święto", "maja", "swieto"),
        page("dzien-stycznia", "01.01", "Dzień", "stycznia", "dzien"),
        page("dzien-maja", "01.05", "Dzień", "maja", "dzien"),
        page("dzien-dziecka", "01.06", "Dzień Dziecka", "czerwca", "dzien", "dziecka"),
    )

    /** l1 teaches what the first four pages use, l2 what the last one does. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *words.map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l2", *later.map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
            ),
        ),
        words + later,
        calendar = pages,
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun state(player: Player) = player.practice().view().games.single { it.game == Game.CALENDAR }.state

    private fun round(seed: Int, vararg days: String): CalendarRound {
        val practice = player(seed).completed(*days).practice()
        return practice.start(practice.setup(Game.CALENDAR, emptyList())) as CalendarRound
    }

    @Test
    fun `Calendar opens once four pages have every Item of their sentence and options learned`() {
        assertEquals(GameState.NEEDS_MORE, state(player()))
        assertEquals(GameState.READY, state(player().completed("l1")))
        assertEquals(4, round(0, "l1").total)
        assertEquals(5, round(0, "l1", "l2").total)
    }

    @Test
    fun `each question shows its date and holiday, with the right option among those shown`() {
        repeat(5) { seed ->
            round(seed, "l1", "l2").run.questions.forEach { question ->
                val page = pages.single { it.id == question.key }
                assertEquals(page.date, question.date)
                assertEquals(page.holiday, question.holiday)
                assertEquals(page.says.text, question.says)
                assertEquals(page.options.map { it.text }.toSet(), question.options.toSet())
                assertEquals(page.options[page.right].text, question.options[question.right])
            }
        }
    }

    @Test
    fun `an option is tapped once, and the score counts the pages right first try`() {
        var round = round(0, "l1", "l2")
        val first = round.question!!
        val wrong = first.options.indices.first { it != first.right }
        round = round.choose(wrong)
        assertEquals(false, round.given!!.correct)
        assertEquals(wrong, round.chosen)
        // Answered once: a second tap changes nothing.
        assertEquals(round, round.choose(first.right))
        round = round.next()
        while (!round.finished) round = round.choose(round.question!!.right).next()
        assertEquals(Score(4, of = 5), round.score)
        assertNull(round.question)
    }
}
