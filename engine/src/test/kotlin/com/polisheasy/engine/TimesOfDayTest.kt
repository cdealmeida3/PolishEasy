package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TimesOfDayTest {
    private val times = listOf(
        TimeOfDay("rano", listOf(7, 8, 9)),
        TimeOfDay("przed-poludniem", listOf(10, 11)),
        TimeOfDay("w-poludnie", listOf(12), onTheHour = true),
        TimeOfDay("po-poludniu", listOf(13, 14, 15, 16)),
        TimeOfDay("wieczorem", listOf(19, 20, 21, 22)),
    )
    private val texts = mapOf("przed-poludniem" to "przed południem", "w-poludnie" to "w południe", "po-poludniu" to "po południu")
    private val items = times.map { item(it.item, texts[it.item] ?: it.item) }

    /** l1 teaches four times of day, l2 the evening. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *times.take(4).map { choose("e-${it.item}", it.item, it.item) }.toTypedArray()),
                day("l2", choose("e-wieczorem", "wieczorem", "wieczorem")),
            ),
        ),
        items,
        timesOfDay = times,
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun Practice.clock(seconds: Int = 60): ClockRound =
        start(setup(Game.TIMES_OF_DAY, listOf("${TimesOfDay.SECONDS_PREFIX}$seconds"))) as ClockRound

    @Test
    fun `Times of day opens with four learned, for 60 or 120 seconds`() {
        val state = { p: Player -> p.practice().view().games.single { it.game == Game.TIMES_OF_DAY }.state }
        assertEquals(GameState.NEEDS_MORE, state(player()))
        assertEquals(GameState.READY, state(player().completed("l1")))
        assertEquals(listOf("seconds-60", "seconds-120"), player().completed("l1").practice().setup(Game.TIMES_OF_DAY).choices[0].options.map { it.id })
    }

    @Test
    fun `each card is a time in its hours, on the hour where it must be, among the learned times, never one twice running`() {
        repeat(10) { seed ->
            val round = player(seed).completed("l1", "l2").practice().clock()
            assertTrue(round.cards.size >= 60)
            assertEquals(5, round.cards.take(5).map { it.key }.toSet().size)
            round.cards.zipWithNext().forEach { (one, next) -> assertTrue(one.key != next.key) }
            round.cards.forEach { card ->
                val time = times.single { it.item == card.key }
                assertTrue(card.hour in time.hours)
                assertTrue(card.minute in TimesOfDay.MINUTES)
                if (time.onTheHour) assertEquals(0, card.minute)
                assertEquals(TimesOfDay.OPTIONS, card.options.size)
                assertEquals(card.options.size, card.options.toSet().size)
                assertEquals(items.single { it.id == card.key }.text, card.answer)
                assertEquals("${card.key} in English", card.meaning)
            }
            assertTrue(round.cards.any { it.minute != 0 })
        }
        assertTrue(player().completed("l1").practice().clock().cards.none { it.key == "wieczorem" })
    }

    @Test
    fun `a right answer brings the next time, a wrong one waits, and the clock ends the Round`() {
        var round = player().completed("l1").practice().clock()
        val first = checkNotNull(round.card)
        round = round.answer(first.right)
        assertEquals(1, round.right)
        val second = checkNotNull(round.card)
        val wrong = (second.right + 1) % second.options.size
        round = round.answer(wrong)
        assertEquals(wrong, round.missed)
        assertEquals(round, round.answer(second.right))
        round = round.tick().next()
        assertNull(round.missed)
        assertEquals(59, round.left)
        repeat(59) { round = round.tick() }
        assertTrue(round.finished)
        assertNull(round.card)
        assertEquals(Score(1), round.score)
    }

    @Test
    fun `the loader reads the times of day and rejects an undefined Item, an hour out of the day and an hour in two`() {
        fun read(practice: String): Pair<List<String>, Course> {
            val files = mapOf(
                "course.json" to """{"levels": [], "items": ["items.json"], "practice": "practice.json"}""",
                "items.json" to """{"items": [{"id": "rano", "text": "rano", "meaning": {"en": "in the morning"}}]}""",
                "practice.json" to practice,
            )
            val content = readContent { files.getValue(it) }
            return loadingProblems(content).map { it.message } to content.course()
        }
        val (problems, course) = read("""{"timesOfDay": [{"item": "rano", "hours": [7, 8]}]}""")
        assertEquals(emptyList(), problems.filter { "time" in it || "hour" in it })
        assertEquals(listOf(TimeOfDay("rano", listOf(7, 8))), course.timesOfDay)
        assertEquals(
            listOf(
                "The time of day rano has the hour 24, which is not 0 to 23",
                "Times of day names Item noc, which is not defined",
                "The time of day noc has no hours",
                "The hour 8 is in more than one time of day",
            ),
            read("""{"timesOfDay": [{"item": "rano", "hours": [8, 24]}, {"item": "noc", "hours": []}, {"item": "w-dzien", "hours": [8]}]}""").first
                .filter { "time" in it.lowercase() || "hour" in it }.filter { "w-dzien" !in it },
        )
    }
}
