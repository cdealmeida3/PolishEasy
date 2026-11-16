package com.polisheasy.engine

import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** What Home shows beside [Today] and the Streak: this week's days, and where the Learner is on the Path. */
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
    fun `a new Learner's week runs Monday to Sunday with today marked, the rest still to come and none completed`() {
        // The fixed day the tests start on, 2 March 2026, is a Monday.
        val days = newLearnerEngine().thisWeek()

        assertEquals((0L..6L).map { march(2).plusDays(it) }, days.map { it.date })
        assertEquals(listOf(march(2)), days.filter { it.today }.map { it.date })
        assertEquals((3..8).map(::march), days.filter { it.upcoming }.map { it.date })
        assertEquals(emptyList(), completed(days))
    }

    @Test
    fun `today is completed as soon as its Daily Set is, and a Daily Set only started does not count`() {
        val learner = Learner()
        val engine = learner.openApp()

        engine.play(engine.startDailySet(), listOf(right.getValue("cześć")))
        assertEquals(emptyList(), completed(engine.thisWeek()))

        engine.playToSummary(engine.next())
        assertEquals(listOf(2), completed(engine.thisWeek()))
        assertEquals(listOf(2), completed(learner.openApp().thisWeek()))
    }

    @Test
    fun `a missed day stays empty between the days that were completed, and is not a day to come`() {
        val learner = learner()
        learner.playDay(march(2))
        learner.playDay(march(4))

        learner.clock.moveTo(march(5))
        val days = learner.openApp().thisWeek()
        assertEquals(march(5), days.single { it.today }.date)
        assertEquals(listOf(2, 4), completed(days))
        assertEquals(listOf(6, 7, 8), days.filter { it.upcoming }.map { it.date.dayOfMonth })
    }

    @Test
    fun `the week stays the same through Sunday and a new one starts at midnight on Monday`() {
        val learner = learner()
        learner.playDay(march(2))

        learner.clock.moveTo(march(8), LocalTime.of(23, 59))
        val sunday = learner.openApp().thisWeek()
        assertEquals(listOf(2), completed(sunday))
        assertEquals(march(8), sunday.last().date)
        assertEquals(emptyList(), sunday.filter { it.upcoming })

        learner.clock.moveTo(march(9), LocalTime.MIDNIGHT)
        val days = learner.openApp().thisWeek()
        assertEquals(march(9), days.first().date)
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

        assertEquals(listOf(2, 3, 4, 5, 6), completed(engine.thisWeek()))
    }

    @Test
    fun `the week runs across the end of a month`() {
        val progress = InMemoryLearnerProgress()
        progress.recordReviewOnlyCompleted(on = LocalDate.of(2026, 2, 27))

        val learner = ItemLearner(course, progress)
        learner.clock.moveTo(LocalDate.of(2026, 2, 28))
        val days = learner.openApp().thisWeek()
        assertEquals(LocalDate.of(2026, 2, 23), days.first().date)
        assertEquals(march(1), days.last().date)
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

    @Test
    fun `days before the Learner's first completed day are never days missed`() {
        val learner = learner()
        learner.clock.moveTo(march(4))
        val before = learner.openApp().thisWeek()
        assertEquals(listOf(2, 3), before.filter { it.beforeStart }.map { it.date.dayOfMonth })

        learner.playDay(march(4))
        learner.clock.moveTo(march(6))
        val days = learner.openApp().thisWeek()
        assertEquals(listOf(2, 3), days.filter { it.beforeStart }.map { it.date.dayOfMonth })
        // Thursday the 5th came after the first day and had no Daily Set: that one was missed.
        assertEquals(listOf(5), days.filter { !it.completed && !it.today && !it.upcoming && !it.beforeStart }.map { it.date.dayOfMonth })

        learner.clock.moveTo(march(9))
        assertEquals(emptyList(), learner.openApp().thisWeek().filter { it.beforeStart })
    }

    @Test
    fun `what the Learner has built up so far counts words, Daily Sets and the longest Streak, and a Replay adds nothing`() {
        val learner = learner()
        assertEquals(SoFarView(wordsLearned = 0, dailySetsCompleted = 0, longestStreak = 0), learner.openApp().soFar())

        learner.playDay(march(2))
        learner.playDay(march(3))
        assertEquals(SoFarView(wordsLearned = 3, dailySetsCompleted = 2, longestStreak = 2), learner.openApp().soFar())

        // A day missed: the Streak starts again, and the longest one stays.
        learner.playDay(march(5))
        val engine = learner.openApp()
        assertEquals(1, engine.streak())
        assertEquals(SoFarView(wordsLearned = 4, dailySetsCompleted = 3, longestStreak = 2), engine.soFar())

        learner.play(engine, engine.startReplay("d1"))
        assertEquals(SoFarView(wordsLearned = 4, dailySetsCompleted = 3, longestStreak = 2), engine.soFar())
    }

    @Test
    fun `a Review-only Daily Set counts as a Daily Set and teaches no new word`() {
        val learner = learner()
        (2..6).forEach { learner.playDay(march(it)) }

        assertEquals(SoFarView(wordsLearned = 5, dailySetsCompleted = 5, longestStreak = 5), learner.openApp().soFar())
    }

    @Test
    fun `the Daily Set completed today can be replayed from Home, and a Review-only one or yesterday's cannot`() {
        val learner = learner()
        assertNull(learner.openApp().completedToday())

        learner.playDay(march(2))
        assertEquals("d1", learner.openApp().completedToday())

        learner.clock.moveTo(march(3))
        assertNull(learner.openApp().completedToday())

        (3..6).forEach { learner.playDay(march(it)) }
        assertNull(learner.openApp().completedToday())
    }

    @Test
    fun `a Daily Set's progress counts its Tips as steps, and moves when the first Tip is left`() {
        val engine = newLearnerEngine()
        check(engine.startDailySet() is Step.ShowTip)
        val steps = engine.stepProgress()
        assertEquals(0, steps.done)

        // Moving on from the Tip is the first step done, before any Exercise is answered.
        val after = engine.next()
        assertEquals(StepProgress(done = 1, total = steps.total), engine.stepProgress())

        engine.playToSummary(after)
        assertEquals(StepProgress(done = steps.total, total = steps.total), engine.stepProgress())
    }

    @Test
    fun `the Daily Set that ends a Unit completes it, and a Replay of it does not`() {
        val learner = learner()
        learner.playDay(march(2))
        learner.clock.moveTo(march(3))
        val engine = learner.openApp()
        assertNull(engine.unitCompleted())

        learner.play(engine, engine.startDailySet(), answers = 0)
        assertNull(engine.unitCompleted())
        learner.play(engine, engine.next())
        assertEquals(UnitCompletedView(number = 1, title = "Greetings"), engine.unitCompleted())

        learner.play(engine, engine.startReplay("d2"))
        assertNull(engine.unitCompleted())
    }
}
