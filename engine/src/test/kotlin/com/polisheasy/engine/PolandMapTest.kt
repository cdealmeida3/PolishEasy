package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PolandMapTest {
    private val words = listOf(
        item("lezy", "leży", "it lies"), item("polnoc", "północ", "the north"), item("poludnie", "południe", "the south"),
        item("gdansk", "Gdańsk", "Gdańsk"), item("krakow", "Kraków", "Kraków"), item("lublin", "Lublin", "Lublin"), item("wschod", "wschód", "the east"),
    )
    private val later = listOf(item("poznan", "Poznań", "Poznań"), item("zachod", "zachód", "the west"))

    private fun place(id: String, place: String, region: Compass, vararg items: String) =
        PolandMapPlace(id, place, region, FlatText("$place leży ${region.polish}.", mapOf("en" to "$place is ${region.meaning}."), items.toList()))

    private val places = listOf(
        place("gdansk", "Gdańsk", Compass.NORTH, "gdansk", "lezy", "polnoc"),
        place("krakow", "Kraków", Compass.SOUTH, "krakow", "lezy", "poludnie"),
        place("lublin", "Lublin", Compass.EAST, "lublin", "lezy", "wschod"),
        place("poznan", "Poznań", Compass.WEST, "poznan", "lezy", "zachod"),
    )

    /** l1 teaches what the first three places use, l2 what the last one does. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *words.map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l2", *later.map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
            ),
        ),
        words + later,
        polandMap = places,
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun state(player: Player) = player.practice().view().games.single { it.game == Game.POLAND_MAP }.state

    private fun round(seed: Int, vararg days: String): PolandMapRound {
        val practice = player(seed).completed(*days).practice()
        return practice.start(practice.setup(Game.POLAND_MAP, emptyList())) as PolandMapRound
    }

    @Test
    fun `Poland map opens once four places have every Item of their sentence learned`() {
        assertEquals(GameState.NEEDS_MORE, state(player().completed("l1")))
        assertEquals(GameState.READY, state(player().completed("l1", "l2")))
        assertEquals(4, round(0, "l1", "l2").total)
    }

    @Test
    fun `each question names its place and says where it lies`() {
        round(0, "l1", "l2").run.questions.forEach { question ->
            val place = places.single { it.id == question.key }
            assertEquals(place.place, question.place)
            assertEquals(place.region, question.region)
            assertEquals(place.says.text, question.says)
        }
    }

    @Test
    fun `a part of the compass is tapped once, and the score counts the places right first try`() {
        var round = round(0, "l1", "l2")
        val first = round.question!!
        val wrong = Compass.entries.first { it != first.region }
        round = round.choose(wrong)
        assertEquals(false, round.given!!.correct)
        assertEquals(wrong, round.chosen)
        // Answered once: a second tap changes nothing.
        assertEquals(round, round.choose(first.region))
        round = round.next()
        while (!round.finished) round = round.choose(round.question!!.region).next()
        assertEquals(Score(3, of = 4), round.score)
        assertNull(round.question)
    }
}
