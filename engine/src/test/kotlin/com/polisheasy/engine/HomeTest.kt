package com.polisheasy.engine

import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** What Home shows beside [Today] and the Streak: the last seven days, and where the Learner is on the Path. */
class HomeTest {
    private val items = listOf(item("a"), item("b"), item("c"), item("d"), item("e"))
    private val d1 = day("d1", choose("d1-e1", "one", "a"), choose("d1-e2", "two", "b"))
    private val d2 = day("d2", choose("d2-e1", "three", "c"))
    private val d3 = day("d3", choose("d3-e1", "four", "d"))
    private val d4 = day("d4", choose("d4-e1", "five", "e"))

    /** Two Levels: A1 with two Units of two and one Daily Sets, then A2 with one Daily Set. */
    private val course = Course(
        listOf(
            levelOf("a1", "A1", unitOf("a1-u01", "Greetings", d1, d2), unitOf("a1-u02", "About me", d3)),
            levelOf("a2", "A2", unitOf("a2-u01", "Food", d4)),
        ),
        items,
    )

    private fun learner() = ItemLearner(course)

    /** The days of March among [days] that were completed. */
    private fun completed(days: List<DayView>): List<Int> = days.filter { it.completed }.map { it.date.dayOfMonth }

    @Test
    fun `a new Learner's last seven days end today, oldest first, and none is completed`() {
        val days = newLearnerEngine().lastSevenDays()

        assertEquals((6L downTo 0L).map { march(2).minusDays(it) }, days.map { it.date })
        assertEquals(emptyList(), completed(days))
    }

    @Test
    fun `today is completed as soon as its Daily Set is, and a Daily Set only started does not count`() {
        val learner = Learner()
        val engine = learner.openApp()

        engine.play(engine.startDailySet(), listOf(right.getValue("cześć")))
        assertEquals(emptyList(), completed(engine.lastSevenDays()))

        engine.playToSummary(engine.next())
        assertEquals(listOf(2), completed(engine.lastSevenDays()))
        assertEquals(listOf(2), completed(learner.openApp().lastSevenDays()))
    }

    @Test
    fun `a missed day stays empty between the days that were completed`() {
        val learner = learner()
        learner.playDay(march(2))
        learner.playDay(march(4))

        learner.clock.moveTo(march(5))
        val days = learner.openApp().lastSevenDays()
        assertEquals(march(5), days.last().date)
        assertEquals(listOf(2, 4), completed(days))
    }

    @Test
    fun `a day leaves the last seven once it is a week old, and the days turn over at midnight`() {
        val learner = learner()
        learner.playDay(march(2))

        learner.clock.moveTo(march(8), LocalTime.of(23, 59))
        assertEquals(listOf(2), completed(learner.openApp().lastSevenDays()))

        learner.clock.moveTo(march(9), LocalTime.MIDNIGHT)
        val days = learner.openApp().lastSevenDays()
        assertEquals(march(3), days.first().date)
        assertEquals(emptyList(), completed(days))
    }

    @Test
    fun `new and Review-only Daily Sets both complete a day, and a Replay does not`() {
        val learner = learner()
        (2..6).forEach { learner.playDay(march(it)) }
        assertEquals(setOf(march(6)), learner.progress.reviewOnlyDays())

        learner.clock.moveTo(march(7))
        val engine = learner.openApp()
        learner.play(engine, engine.startReplay("d1"))

        assertEquals(listOf(2, 3, 4, 5, 6), completed(engine.lastSevenDays()))
    }

    @Test
    fun `the last seven days run across the end of a month`() {
        val progress = InMemoryLearnerProgress()
        progress.recordReviewOnlyCompleted(on = LocalDate.of(2026, 2, 27))

        val days = ItemLearner(course, progress).openApp().lastSevenDays()
        assertEquals(LocalDate.of(2026, 2, 24), days.first().date)
        assertEquals(listOf(27), completed(days))
    }

    @Test
    fun `a new Learner's next Daily Set is the first of the first Unit, with the titles of its Tips`() {
        assertEquals(
            NextDailySetView(
                unitNumber = 1,
                unitTitle = "Greetings",
                numberInUnit = 1,
                dailySetsInUnit = 2,
                teaches = listOf("Saying hello", "Saying thank you"),
            ),
            newLearnerEngine().nextDailySet(),
        )
    }

    @Test
    fun `a Daily Set started and not finished is still the next one`() {
        val learner = learner()
        val engine = learner.openApp()
        learner.play(engine, engine.startDailySet(), answers = 1)

        assertEquals(NextDailySetView(1, "Greetings", numberInUnit = 1, dailySetsInUnit = 2), learner.openApp().nextDailySet())
    }

    @Test
    fun `completing a Daily Set makes the one after it next at once, although it unlocks tomorrow`() {
        val learner = learner()
        learner.playDay(march(2))

        val engine = learner.openApp()
        assertEquals(Today.DoneForToday(march(3).atStartOfDay(warsaw)), engine.today())
        assertEquals(NextDailySetView(1, "Greetings", numberInUnit = 2, dailySetsInUnit = 2), engine.nextDailySet())
    }

    @Test
    fun `after a Unit's last Daily Set the next one is the first of the next Unit, and after a Level's the first of the next Level`() {
        val learner = learner()
        learner.playDay(march(2))
        learner.playDay(march(3))
        assertEquals(NextDailySetView(2, "About me", numberInUnit = 1, dailySetsInUnit = 1), learner.openApp().nextDailySet())

        learner.playDay(march(4))
        assertEquals(NextDailySetView(1, "Food", numberInUnit = 1, dailySetsInUnit = 1), learner.openApp().nextDailySet())
    }

    @Test
    fun `once every Daily Set released is completed there is no next one, whatever the Review-only Daily Set is doing`() {
        val learner = learner()
        (2..5).forEach { learner.playDay(march(it)) }
        assertNull(learner.openApp().nextDailySet())

        learner.clock.moveTo(march(6))
        val engine = learner.openApp()
        learner.play(engine, engine.startDailySet(), answers = 1)
        assertNull(engine.nextDailySet())
    }

    @Test
    fun `a Replay does not change the next Daily Set`() {
        val learner = learner()
        learner.playDay(march(2))

        learner.clock.moveTo(march(3))
        val engine = learner.openApp()
        learner.play(engine, engine.startReplay("d1"))

        assertEquals(NextDailySetView(1, "Greetings", numberInUnit = 2, dailySetsInUnit = 2), engine.nextDailySet())
    }
}
