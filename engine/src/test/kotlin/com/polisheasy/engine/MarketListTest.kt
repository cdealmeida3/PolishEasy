package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarketListTest {
    private val numbers = listOf("jeden", "jedna", "jedno", "dwa", "dwie", "trzy", "cztery")
    private val goods = listOf("gruszka", "banan", "jablko", "cebula", "pomidor", "truskawka", "ogorek")
    private val texts = mapOf("jablko" to "jabłko", "ogorek" to "ogórek")
    private val plurals = mapOf("gruszka" to "gruszki", "banan" to "banany", "jablko" to "jabłka", "cebula" to "cebule", "pomidor" to "pomidory", "truskawka" to "truskawki", "ogorek" to "ogórki")
    private val genders = mapOf(
        "gruszka" to Gender.FEMININE, "banan" to Gender.MASCULINE, "jablko" to Gender.NEUTER, "cebula" to Gender.FEMININE, "pomidor" to Gender.MASCULINE,
        "truskawka" to Gender.FEMININE, "ogorek" to Gender.MASCULINE,
    )
    private val items = (numbers + "i").map { item(it) } +
        goods.map { item(it, texts[it] ?: it).copy(forms = listOf(plurals.getValue(it))) }

    /** l1 teaches four goods and the numbers but jedna and dwie, l2 those two, three more goods and *i*. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *(goods.take(4) + numbers - "jedna" - "dwie").map { choose("e-$it", it, it) }.toTypedArray()),
                day("l2", *listOf("jedna", "dwie", "pomidor", "truskawka", "ogorek", "i").map { choose("e-$it", it, it) }.toTypedArray()),
            ),
        ),
        items,
        clips = items.flatMap { listOf(it.text) + it.forms }.map { clip(it) },
        pictures = goods.associateWith { Picture.Image("$it.webp") },
        genders = genders,
        market = Market(
            "i",
            listOf(
                mapOf(Gender.MASCULINE to "jeden", Gender.FEMININE to "jedna", Gender.NEUTER to "jedno"),
                mapOf(Gender.MASCULINE to "dwa", Gender.FEMININE to "dwie", Gender.NEUTER to "dwa"),
                Gender.entries.associateWith { "trzy" },
                Gender.entries.associateWith { "cztery" },
            ),
            goods,
        ),
    )

    private fun player(course: Course = this.course, seed: Int = 0) = Player(course = course, seed = seed)

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.MARKET_LIST }.state

    private fun Practice.market(vararg wanted: String, sound: Boolean = true): MarketRound =
        start(setup(Game.MARKET_LIST, wanted.toList(), sound), sound) as MarketRound

    @Test
    fun `Market list opens once every number word is learned, dwie too, and two goods are a level once i is learned`() {
        assertEquals(GameState.NEEDS_MORE, stateOf(player().completed("l1")))
        assertEquals(GameState.READY, stateOf(player().completed("l1", "l2")))
        val levels = player().completed("l1", "l2").practice().setup(Game.MARKET_LIST, listOf(MarketList.READ)).choices[1].options.map { it.id }
        assertEquals(listOf(MarketList.ONE, MarketList.TWO), levels)
        val noAnd = course.copy(market = course.market?.copy(and = "oraz"))
        val one = player(noAnd).completed("l1", "l2").practice().setup(Game.MARKET_LIST, listOf(MarketList.READ)).choices[1].options.map { it.id }
        assertEquals(listOf(MarketList.ONE), one)
    }

    @Test
    fun `a list says a number that agrees with the good, the good plural from two on, and the stall holds it among others`() {
        val forGender = mapOf(
            Gender.MASCULINE to listOf("jeden", "dwa", "trzy", "cztery"),
            Gender.FEMININE to listOf("jedna", "dwie", "trzy", "cztery"),
            Gender.NEUTER to listOf("jedno", "dwa", "trzy", "cztery"),
        )
        val counts = mutableSetOf<Int>()
        repeat(10) { seed ->
            val round = player(seed = seed).completed("l1", "l2").practice().market(MarketList.READ, MarketList.ONE)
            round.run.questions.forEach { question ->
                val (good, count) = question.wanted.entries.single()
                counts += count
                val item = items.single { it.id == good }
                assertEquals(listOf(forGender.getValue(genders.getValue(good))[count - 1], if (count == 1) item.text else item.forms.single()), question.words)
                assertEquals(MarketList.STALL, question.stall.size)
                assertTrue(question.stall.any { it.id == good })
                assertEquals(question.stall.size, question.stall.map { it.picture }.toSet().size)
            }
            assertEquals(round.run.questions.map { it.key }.toSet().size, round.run.questions.size)
        }
        assertEquals(setOf(1, 2, 3, 4), counts)
    }

    @Test
    fun `two goods are joined by i`() {
        repeat(10) { seed ->
            val round = player(seed = seed).completed("l1", "l2").practice().market(MarketList.READ, MarketList.TWO)
            round.run.questions.forEach { question ->
                assertEquals(5, question.words.size)
                assertEquals("i", question.words[2])
                assertEquals(2, question.wanted.size)
                assertTrue(question.stall.map { it.id }.containsAll(question.wanted.keys))
            }
        }
    }

    @Test
    fun `goods go in the basket one at a time, up to four, come out again, and only exactly the list is right`() {
        var round = player().completed("l1", "l2").practice().market(MarketList.READ, MarketList.ONE)
        val question = checkNotNull(round.question)
        val (good, count) = question.wanted.entries.single()
        val other = question.stall.first { it.id != good }.id
        assertFalse(round.canPay)
        assertEquals(round, round.pay())
        assertEquals(round, round.add("nothing"))
        assertEquals(round, round.takeOut(good))
        repeat(MarketList.MOST + 1) { round = round.add(other) }
        assertEquals(MarketList.MOST, round.counts[other])
        repeat(MarketList.MOST) { round = round.takeOut(other) }
        assertEquals(emptyList(), round.basket)
        round = round.add(good).add(other).add(good)
        assertEquals(listOf(good, good, other), round.inBasket)
        // As many as the list asks, and one good it does not ask for.
        round = round.takeOut(good).takeOut(good)
        repeat(count) { round = round.add(good) }
        assertTrue(round.canPay)
        round = round.pay()
        assertEquals(false, round.given?.correct)
        assertEquals(round, round.add(good))
        round = round.next()
        assertEquals(emptyList(), round.basket)
        while (!round.finished) {
            val list = checkNotNull(round.question)
            round = list.wanted.entries.fold(round) { r, (id, n) -> (1..n).fold(r) { s, _ -> s.add(id) } }.pay()
            assertEquals(true, round.given?.correct)
            round = round.next()
        }
        assertEquals(Score(MarketList.QUESTIONS - 1, of = MarketList.QUESTIONS), round.score)
    }

    @Test
    fun `heard, only goods whose words have Clips are asked for, and with sound off it is read`() {
        val noPears = course.copy(clips = course.clips.filter { it.text != "gruszki" })
        val practice = player(noPears).completed("l1", "l2").practice()
        val round = practice.market(MarketList.LISTEN, MarketList.ONE)
        assertTrue(round.listen)
        assertTrue(round.run.questions.none { "gruszki" in it.words }, "${round.run.questions.map { it.words }}")
        assertEquals(listOf(MarketList.READ), practice.setup(Game.MARKET_LIST, sound = false).choices[0].options.map { it.id })
    }

    @Test
    fun `the loader reads the market and rejects an undefined Item, a number without every gender, and a good without a gender, picture or plural, or listed twice`() {
        fun read(practice: String): Pair<List<String>, Course> {
            val files = mapOf(
                "course.json" to """{"levels": [], "items": ["items.json"], "practice": "practice.json", "pictures": "pictures/pictures.json"}""",
                "items.json" to """{"items": [
                    {"id": "i", "text": "i", "meaning": {"en": "and"}},
                    {"id": "jeden", "text": "jeden", "meaning": {"en": "one"}},
                    {"id": "jedna", "text": "jedna", "meaning": {"en": "one"}},
                    {"id": "jedno", "text": "jedno", "meaning": {"en": "one"}},
                    {"id": "gruszka", "text": "gruszka", "meaning": {"en": "a pear"}, "forms": ["gruszki"]},
                    {"id": "kiwi", "text": "kiwi", "meaning": {"en": "a kiwi"}},
                    {"id": "banan", "text": "banan", "meaning": {"en": "a banana"}, "forms": ["banany"]}
                ]}""",
                "pictures/pictures.json" to """{"pictures": [
                    {"item": "gruszka", "image": "pear.webp"},
                    {"item": "kiwi", "image": "kiwi.webp"}
                ]}""",
                "practice.json" to practice,
            )
            val content = readContent { files.getValue(it) }
            return loadingProblems(content).map { it.message } to content.course()
        }
        val (_, course) = read(
            """{"genders": {"gruszka": "f"}, "market": {"and": "i", "numbers": [{"m": "jeden", "f": "jedna", "n": "jedno"}], "goods": ["gruszka"]}}""",
        )
        assertEquals(Market("i", listOf(mapOf(Gender.MASCULINE to "jeden", Gender.FEMININE to "jedna", Gender.NEUTER to "jedno")), listOf("gruszka")), course.market)
        assertEquals(
            listOf(
                "Market list names Item oraz, which is not defined",
                "Market list's number 2 must give its word for m, f and n",
                "The good kiwi needs its plural as its only form",
                "The good banan has no gender under genders",
                "The good banan has no picture",
                "The good gruszka is listed more than once",
            ),
            read(
                """{"genders": {"gruszka": "f", "kiwi": "n"}, "market": {"and": "oraz", "numbers": [{"m": "jeden", "f": "jedna", "n": "jedno"}, {"m": "jeden"}],
                    "goods": ["gruszka", "kiwi", "banan", "gruszka"]}}""",
            ).first.filter { "Market" in it || "good" in it },
        )
    }
}
