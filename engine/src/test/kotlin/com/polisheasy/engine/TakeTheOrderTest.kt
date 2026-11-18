package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TakeTheOrderTest {
    private val dishes = listOf("zurek", "rosol", "bigos", "golabki", "nalesniki", "sernik", "lody")
    private val texts = mapOf("zurek" to "żurek", "rosol" to "rosół", "golabki" to "gołąbki", "nalesniki" to "naleśniki")
    private val phrases = mapOf("dla-mnie" to "dla mnie", "to-samo" to "to samo", "a" to "a")
    private val items = phrases.map { (id, text) -> item(id, text) } + dishes.map { item(it, texts[it] ?: it) }

    /** l1 teaches five dishes and the phrases, l2 two more dishes. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *(dishes.take(5) + phrases.keys).map { choose("e-$it", it, it) }.toTypedArray()),
                day("l2", *dishes.drop(5).map { choose("e-$it", it, it) }.toTypedArray()),
            ),
        ),
        items,
        clips = items.map { clip(it.text) },
        pictures = mapOf("lody" to Picture.Image("ice-cream.webp")),
        restaurant = Restaurant("dla-mnie", "to-samo", "a", dishes),
    )

    private fun player(course: Course = this.course, seed: Int = 0) = Player(course = course, seed = seed)

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.TAKE_THE_ORDER }.state

    private fun Practice.table(vararg wanted: String, sound: Boolean = true): TableRound =
        start(setup(Game.TAKE_THE_ORDER, wanted.toList(), sound), sound) as TableRound

    @Test
    fun `Take the order opens with dla mnie, to samo, a and six dishes, for tables of two or three`() {
        assertEquals(GameState.NEEDS_MORE, stateOf(player().completed("l1")))
        assertEquals(GameState.READY, stateOf(player().completed("l1", "l2")))
        val choices = player().completed("l1", "l2").practice().setup(Game.TAKE_THE_ORDER).choices
        assertEquals(listOf(TakeTheOrder.LISTEN, TakeTheOrder.READ), choices[0].options.map { it.id })
        assertEquals(listOf(TakeTheOrder.TWO, TakeTheOrder.THREE), choices[1].options.map { it.id })
    }

    @Test
    fun `each guest says dla mnie and a dish, the next a dla mnie, and to samo is the dish before`() {
        var same = 0
        repeat(10) { seed ->
            val round = player(seed = seed).completed("l1", "l2").practice().table(TakeTheOrder.READ, TakeTheOrder.THREE)
            assertEquals(TakeTheOrder.QUESTIONS, round.run.questions.size)
            round.run.questions.forEach { question ->
                assertEquals(3, question.lines.size)
                assertEquals(3, question.dishes.size)
                question.lines.forEachIndexed { guest, words ->
                    val start = if (guest == 0) listOf("dla mnie") else listOf("a", "dla mnie")
                    assertEquals(start, words.dropLast(1))
                    val dish = items.single { it.id == question.dishes[guest] }
                    if (words.last() == "to samo") {
                        same++
                        assertTrue(guest > 0)
                        assertEquals(question.dishes[guest - 1], question.dishes[guest])
                    } else {
                        assertEquals(dish.text, words.last())
                        if (guest > 0) assertTrue(question.dishes[guest - 1] != question.dishes[guest])
                    }
                }
                assertEquals(TakeTheOrder.MENU, question.menu.size)
                assertEquals(question.menu.size, question.menu.map { it.id }.toSet().size)
                assertTrue(question.menu.map { it.id }.containsAll(question.dishes))
            }
            assertEquals(round.run.questions.map { it.key }.toSet().size, round.run.questions.size)
        }
        assertTrue(same > 0)
        val first = player().completed("l1", "l2").practice().table(TakeTheOrder.READ, TakeTheOrder.TWO).question!!
        assertEquals("Dla mnie ${items.single { it.id == first.dishes[0] }.text}.", first.polish[0])
    }

    @Test
    fun `dishes are written for each guest in turn, the last rubbed out, and only every guest's own dish is right`() {
        var round = player().completed("l1", "l2").practice().table(TakeTheOrder.READ, TakeTheOrder.TWO)
        val question = checkNotNull(round.question)
        val other = question.menu.first { it.id != question.dishes[0] }.id
        assertFalse(round.canServe)
        assertEquals(round, round.serve())
        assertEquals(round, round.write("pizza"))
        assertEquals(round, round.rubOut())
        round = round.write(other).write(question.dishes[1]).write(other)
        assertEquals(listOf(other, question.dishes[1]), round.pad)
        assertTrue(round.canServe)
        round = round.rubOut().rubOut().write(question.dishes[1]).write(question.dishes[0])
        // The right dishes for the wrong guests are wrong, unless both ordered the same.
        round = round.serve()
        assertEquals(question.dishes[0] == question.dishes[1], round.given?.correct)
        assertEquals(round, round.write(other))
        round = round.next()
        assertEquals(emptyList(), round.pad)
        while (!round.finished) {
            val table = checkNotNull(round.question)
            round = table.dishes.fold(round) { r, id -> r.write(id) }.serve()
            assertEquals(true, round.given?.correct)
            round = round.next()
        }
        val firstRight = if (question.dishes[0] == question.dishes[1]) TakeTheOrder.QUESTIONS else TakeTheOrder.QUESTIONS - 1
        assertEquals(Score(firstRight, of = TakeTheOrder.QUESTIONS), round.score)
    }

    @Test
    fun `heard, only dishes with Clips are ordered, and with sound off it is read`() {
        val noSoup = course.copy(clips = course.clips.filter { it.text != "żurek" })
        val practice = player(noSoup).completed("l1", "l2").practice()
        val round = practice.table(TakeTheOrder.LISTEN, TakeTheOrder.TWO)
        assertTrue(round.listen)
        assertTrue(round.run.questions.none { "zurek" in it.dishes || it.menu.any { dish -> dish.id == "zurek" } })
        assertEquals(listOf(TakeTheOrder.READ), practice.setup(Game.TAKE_THE_ORDER, sound = false).choices[0].options.map { it.id })
        val noSame = course.copy(clips = course.clips.filter { it.text != "to samo" })
        assertEquals(listOf(TakeTheOrder.READ), player(noSame).completed("l1", "l2").practice().setup(Game.TAKE_THE_ORDER).choices[0].options.map { it.id })
    }

    @Test
    fun `the loader reads the restaurant and rejects an undefined Item and a dish listed twice`() {
        fun read(practice: String): Pair<List<String>, Course> {
            val files = mapOf(
                "course.json" to """{"levels": [], "items": ["items.json"], "practice": "practice.json"}""",
                "items.json" to """{"items": [
                    {"id": "dla-mnie", "text": "dla mnie", "meaning": {"en": "for me"}},
                    {"id": "to-samo", "text": "to samo", "meaning": {"en": "the same"}},
                    {"id": "a", "text": "a", "meaning": {"en": "and"}},
                    {"id": "zurek", "text": "żurek", "meaning": {"en": "sour rye soup"}}
                ]}""",
                "practice.json" to practice,
            )
            val content = readContent { files.getValue(it) }
            return loadingProblems(content).map { it.message } to content.course()
        }
        val (_, course) = read("""{"restaurant": {"forMe": "dla-mnie", "same": "to-samo", "and": "a", "dishes": ["zurek"]}}""")
        assertEquals(Restaurant("dla-mnie", "to-samo", "a", listOf("zurek")), course.restaurant)
        assertEquals(
            listOf(
                "Take the order names Item i, which is not defined",
                "Take the order names Item bigos, which is not defined",
                "The dish zurek is listed more than once",
            ),
            read("""{"restaurant": {"forMe": "dla-mnie", "same": "to-samo", "and": "i", "dishes": ["zurek", "bigos", "zurek"]}}""").first
                .filter { "order" in it || "dish" in it },
        )
    }
}
