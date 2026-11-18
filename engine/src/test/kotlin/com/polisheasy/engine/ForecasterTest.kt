package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ForecasterTest {
    private val signs = listOf("slonce", "deszcz", "snieg", "wiatr", "mgla", "burza", "mroz")
    private val words = signs.map { item(it) } + listOf(item("jutro"), item("piec", "pięć"), item("stopni"))

    private fun forecast(text: String, degrees: Int, vararg weather: String) =
        Forecast(text, mapOf("en" to "$text (en)"), weather.toList(), degrees, weather.toList() + listOf("jutro", "piec", "stopni"))

    private val forecasts = listOf(
        forecast("Jutro słońce, pięć stopni.", 5, "slonce"),
        forecast("Jutro deszcz i wiatr, pięć stopni.", 5, "deszcz", "wiatr"),
        forecast("Jutro śnieg, minus pięć.", -5, "snieg"),
        forecast("Jutro mgła, zero.", 0, "mgla"),
        forecast("Jutro burza i deszcz.", 20, "burza", "deszcz"),
    )

    private val pictures = signs.associateWith { Picture.Glyph(it) as Picture }

    /** l1 teaches five signs and the words, so four forecasts; l2 burza and mróz. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *words.filter { it.id != "mroz" && it.id != "burza" }.map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l2", choose("e-burza", "burza", "burza"), choose("e-mroz", "mroz", "mroz")),
            ),
        ),
        words,
        pictures = pictures,
        forecasts = forecasts + forecast("Jutro mróz.", -10, "mroz"),
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun Practice.round(): ForecasterRound = start(setup(Game.FORECASTER, listOf(Forecaster.READ))) as ForecasterRound

    @Test
    fun `Forecaster opens once four forecasts and six signs are learned`() {
        val state = { p: Player -> p.practice().view().games.single { it.game == Game.FORECASTER }.state }
        assertEquals(GameState.NEEDS_MORE, state(player()))
        // Four forecasts, but only five signs for the board.
        assertEquals(GameState.NEEDS_MORE, state(player().completed("l1")))
        assertEquals(GameState.READY, state(player().completed("l1", "l2")))
    }

    @Test
    fun `the board holds the signs wanted among others, and the temperatures the right one, its mirror and others in order`() {
        repeat(10) { seed ->
            val round = player(seed).completed("l1", "l2").practice().round()
            round.run.questions.forEach { question ->
                val board = question.board.map { it.id }
                assertEquals(Forecaster.BOARD, board.size)
                assertEquals(board.distinct(), board)
                assertTrue(board.containsAll(question.wanted), "${question.text}: $board")
                assertEquals(Forecaster.TEMPERATURES, question.temperatures.size)
                assertEquals(question.temperatures.sorted(), question.temperatures)
                assertEquals(question.temperatures.distinct(), question.temperatures)
                assertTrue(question.degrees in question.temperatures)
                if (question.degrees != 0) assertTrue(-question.degrees in question.temperatures, "${question.text}: ${question.temperatures}")
            }
        }
    }

    @Test
    fun `a card is right only with exactly the signs wanted and the temperature, and the score counts first tries`() {
        var round = player().completed("l1", "l2").practice().round()
        val first = checkNotNull(round.question)
        assertEquals(false, round.canSend)
        first.wanted.forEach { round = round.place(it) }
        assertEquals(false, round.canSend)
        round = round.pick(first.degrees).send()
        assertEquals(true, round.given?.correct)
        assertEquals(round, round.place(first.board.first().id))
        round = round.next()
        assertEquals(emptyList(), round.placed)
        assertEquals(null, round.temperature)
        val second = checkNotNull(round.question)
        // One sign too many is wrong, though the temperature is right.
        second.wanted.forEach { round = round.place(it) }
        round = round.place(second.board.first { it.id !in second.wanted }.id).pick(second.degrees).send()
        assertEquals(false, round.given?.correct)
        round = round.next()
        while (!round.finished) {
            val question = checkNotNull(round.question)
            question.wanted.forEach { round = round.place(it) }
            round = round.pick(question.degrees).send().next()
        }
        assertEquals(Score(round.total - 1, of = round.total), round.score)
    }

    @Test
    fun `a sign tapped twice comes off the card`() {
        var round = player().completed("l1", "l2").practice().round()
        val sign = checkNotNull(round.question).board.first().id
        round = round.place(sign)
        assertEquals(listOf(sign), round.placed)
        round = round.place(sign)
        assertEquals(emptyList(), round.placed)
    }
}
