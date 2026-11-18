package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DirectionsTest {
    private val words = listOf("prosze", "isc", "prosto", "potem", "w-lewo", "w-prawo", "kosciol", "zamek", "most", "sklep", "bank", "apteka").map { item(it) }
    private val places = listOf("kosciol", "zamek", "most", "sklep", "bank", "apteka")

    private fun route(text: String, vararg items: String) = Route(text, mapOf("en" to "$text (en)"), items.toList())

    private val routes = listOf(
        route("Proszę iść prosto.", "prosze", "isc", "prosto"),
        route("Proszę iść prosto, potem w lewo.", "prosze", "isc", "prosto", "potem", "w-lewo"),
        route("Proszę iść prosto, potem w prawo.", "prosze", "isc", "prosto", "potem", "w-prawo"),
        route("Proszę iść prosto, potem w lewo, potem w prawo.", "prosze", "isc", "prosto", "potem", "w-lewo", "w-prawo"),
    )

    /** l1 teaches the routes and five places, l2 the sixth place. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *words.filter { it.id != "apteka" }.map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l2", choose("e-apteka", "apteka", "apteka")),
            ),
        ),
        words,
        pictures = words.associate { it.id to Picture.Glyph(it.id.take(1)) },
        routes = routes,
        places = places,
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun Practice.round(): DirectionsRound = start(setup(Game.DIRECTIONS, listOf(Directions.READ))) as DirectionsRound

    @Test
    fun `a route is walked from below the middle of the map, a turn then on to the next block`() {
        assertEquals(Block(1, 1), CityMap.walk(Move.heardIn("Proszę iść prosto.")))
        assertEquals(Block(0, 1), CityMap.walk(Move.heardIn("Proszę iść prosto, potem skręcić w lewo.")))
        assertEquals(Block(2, 0), CityMap.walk(Move.heardIn("Proszę iść prosto, w prawo, a potem w lewo.")))
        assertEquals(Block(1, 0), CityMap.walk(Move.heardIn("Proszę iść prosto, potem w lewo, w prawo i jeszcze raz w prawo.")))
        // A turn first leaves the map, and so does going on past its top.
        assertNull(CityMap.walk(Move.heardIn("Proszę skręcić w lewo.")))
        assertNull(CityMap.walk(Move.heardIn("Prosto, prosto i prosto.")))
        assertNull(CityMap.walk(Move.heardIn("Proszę.")))
    }

    @Test
    fun `Directions opens once routes to three blocks are learned, with a place for every block`() {
        val state = { p: Player -> p.practice().view().games.single { it.game == Game.DIRECTIONS }.state }
        assertEquals(GameState.NEEDS_MORE, state(player()))
        assertEquals(GameState.NEEDS_MORE, state(player().completed("l1")))
        assertEquals(GameState.NEEDS_MORE, state(player().completed("l2")))
        assertEquals(GameState.READY, state(player().completed("l1", "l2")))
        assertEquals(listOf(Directions.READ), player().completed("l1", "l2").practice().setup(Game.DIRECTIONS).choices[0].options.map { it.id })
    }

    @Test
    fun `a Round puts a place on every block and asks each route once, each to its own block`() {
        repeat(10) { seed ->
            val round = player(seed).completed("l1", "l2").practice().round()
            assertEquals(CityMap.blocks, round.map.map { it.block })
            assertEquals(places.toSet().size, round.map.map { it.text }.toSet().size)
            assertEquals(routes.size, round.total)
            assertEquals(round.total, round.run.questions.map { it.place }.toSet().size)
            round.run.questions.forEach { question ->
                assertEquals(routes.single { it.text == question.text }.end, round.map[question.place].block)
            }
        }
    }

    @Test
    fun `the right place is right first try, a wrong one is kept until the Learner moves on`() {
        var round = player().completed("l1", "l2").practice().round()
        val first = checkNotNull(round.question)
        round = round.choose(first.place)
        assertEquals(true, round.given?.correct)
        assertEquals(round, round.choose((first.place + 1) % round.map.size))
        round = round.next()
        val second = checkNotNull(round.question)
        round = round.choose((second.place + 1) % round.map.size)
        assertEquals(false, round.given?.correct)
        assertEquals((second.place + 1) % round.map.size, round.chosen)
        round = round.next()
        while (!round.finished) round = round.choose(checkNotNull(round.question).place).next()
        assertTrue(round.finished)
        assertEquals(Score(routes.size - 1, of = routes.size), round.score)
    }
}
