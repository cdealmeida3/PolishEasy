package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AisleSortTest {
    private val aisles = listOf(
        Aisle("nabial", listOf("mleko", "ser", "maslo")),
        Aisle("pieczywo", listOf("chleb", "bulka")),
        Aisle("slodycze", listOf("czekolada", "cukierki")),
        Aisle("napoje", listOf("sok", "woda")),
        Aisle("alkohole", listOf("piwo", "wino")),
    )
    private val all = aisles.flatMap { listOf(it.names) + it.products }

    /** l1 stocks two aisles, l2 a third, l3 the other two. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *(aisles[0].products + "nabial" + aisles[1].products + "pieczywo").map { choose("e-$it", it, it) }.toTypedArray()),
                day("l2", *(aisles[2].products + "slodycze").map { choose("e-$it", it, it) }.toTypedArray()),
                day("l3", *(aisles[3].products + "napoje" + aisles[4].products + "alkohole").map { choose("e-$it", it, it) }.toTypedArray()),
            ),
        ),
        all.map { item(it) },
        pictures = mapOf("mleko" to Picture.Image("milk.webp")),
        aisles = aisles,
    )

    private fun player(course: Course = this.course, seed: Int = 0) = Player(course = course, seed = seed)

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.AISLE_SORT }.state

    private fun Practice.aisles(seconds: Int = 60): AisleRound =
        start(setup(Game.AISLE_SORT, listOf("${AisleSort.SECONDS_PREFIX}$seconds"))) as AisleRound

    @Test
    fun `Sort the aisles opens once three aisles are stocked with six products`() {
        assertEquals(GameState.NEEDS_MORE, stateOf(player().completed("l1")))
        assertEquals(GameState.READY, stateOf(player().completed("l1", "l2")))
        assertEquals(listOf("seconds-60", "seconds-120"), player().completed("l1", "l2").practice().setup(Game.AISLE_SORT).choices[0].options.map { it.id })
    }

    @Test
    fun `a Round takes up to four aisles, in the content's order, and deals only their products, never one twice running`() {
        repeat(10) { seed ->
            val round = player(seed = seed).completed("l1", "l2", "l3").practice().aisles()
            assertEquals(AisleSort.AISLES, round.aisles.size)
            assertEquals(aisles.map { it.names }.filter { name -> round.aisles.any { it.id == name } }, round.aisles.map { it.id })
            round.cards.forEach { card ->
                assertTrue(card.itemId in aisles.single { it.names == card.aisle }.products)
                assertTrue(round.aisles.any { it.id == card.aisle })
            }
            round.cards.zipWithNext().forEach { (one, next) -> assertTrue(one.itemId != next.itemId) }
            // Every product comes round before any comes again.
            val products = round.cards.map { it.itemId }.toSet()
            assertEquals(products, round.cards.take(products.size).map { it.itemId }.toSet())
        }
        val three = player().completed("l1", "l2").practice().aisles()
        assertEquals(listOf("nabial", "pieczywo", "slodycze"), three.aisles.map { it.id })
        assertEquals(Picture.Image("milk.webp"), three.cards.first { it.itemId == "mleko" }.picture)
        assertNull(three.cards.first { it.itemId == "ser" }.picture)
    }

    @Test
    fun `a right aisle brings the next product, a wrong one waits with the right aisle, and the clock ends the Round`() {
        var round = player().completed("l1", "l2").practice().aisles()
        val first = checkNotNull(round.card)
        round = round.answer(first.aisle)
        assertEquals(1, round.right)
        assertEquals(1, round.sorted[first.aisle])
        val second = checkNotNull(round.card)
        val wrong = round.aisles.first { it.id != second.aisle }.id
        round = round.answer(wrong)
        assertEquals(wrong, round.missed)
        assertEquals(second.aisle, round.rightAisle?.id)
        assertEquals(round, round.answer(second.aisle))
        assertEquals(round, round.answer("mrozonki"))
        round = round.next()
        assertNull(round.missed)
        assertEquals(2, round.index)
        assertEquals(Score(1), round.score)
        repeat(60) { round = round.tick() }
        assertTrue(round.finished)
        assertNull(round.card)
        assertEquals(round, round.tick())
    }

    @Test
    fun `a product in two aisles is never dealt`() {
        val twice = course.copy(aisles = aisles.take(3) + Aisle("napoje", listOf("sok", "woda", "mleko")))
        repeat(5) { seed ->
            val round = player(twice, seed).completed("l1", "l2", "l3").practice().aisles()
            assertTrue(round.cards.none { it.itemId == "mleko" })
        }
    }

    @Test
    fun `the loader reads the aisles and rejects an undefined Item, an aisle without products, and an aisle or product listed twice`() {
        fun read(practice: String): Pair<List<String>, Course> {
            val files = mapOf(
                "course.json" to """{"levels": [], "items": ["items.json"], "practice": "practice.json"}""",
                "items.json" to """{"items": [
                    {"id": "nabial", "text": "nabiał", "meaning": {"en": "dairy"}},
                    {"id": "napoje", "text": "napoje", "meaning": {"en": "drinks"}},
                    {"id": "mleko", "text": "mleko", "meaning": {"en": "milk"}},
                    {"id": "sok", "text": "sok", "meaning": {"en": "juice"}}
                ]}""",
                "practice.json" to practice,
            )
            val content = readContent { files.getValue(it) }
            return loadingProblems(content).map { it.message } to content.course()
        }
        val (_, course) = read("""{"aisles": [{"aisle": "nabial", "products": ["mleko"]}, {"aisle": "napoje", "products": ["sok"]}]}""")
        assertEquals(listOf(Aisle("nabial", listOf("mleko")), Aisle("napoje", listOf("sok"))), course.aisles)
        assertEquals(
            listOf(
                "The aisle nabial names Item ser, which is not defined",
                "The aisle napoje has no products",
                "The aisle nabial is listed more than once",
                "The product mleko is in more than one aisle, or listed twice",
            ),
            read(
                """{"aisles": [{"aisle": "nabial", "products": ["mleko", "ser"]}, {"aisle": "napoje", "products": []}, {"aisle": "nabial", "products": ["mleko"]}]}""",
            ).first.filter { "aisle" in it || "product" in it },
        )
    }
}
