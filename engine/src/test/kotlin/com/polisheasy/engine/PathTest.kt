package com.polisheasy.engine

import com.polisheasy.engine.DailySetState.COMPLETED
import com.polisheasy.engine.DailySetState.LOCKED
import com.polisheasy.engine.DailySetState.TODAY
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PathTest {
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

    /** Every Daily Set listed on the Path with its state, across all listed Levels. */
    private fun states(path: PathView): List<Pair<String, DailySetState>> =
        path.levels.flatMap { level -> level.units.flatMap { unit -> unit.dailySets.map { it.id to it.state } } }

    private fun levels(path: PathView): List<String> = path.levels.map { it.id }

    @Test
    fun `a new Learner's Path lists the first Level's Units in order, their Daily Sets numbered through the Level, the first one today's and the rest locked`() {
        assertEquals(
            PathView(
                listOf(
                    LevelView(
                        "a1",
                        "A1",
                        listOf(
                            UnitView(
                                "a1-u01",
                                number = 1,
                                title = "Greetings",
                                dailySets = listOf(DailySetView("d1", 1, TODAY), DailySetView("d2", 2, LOCKED)),
                            ),
                            UnitView("a1-u02", number = 2, title = "About me", dailySets = listOf(DailySetView("d3", 3, LOCKED))),
                        ),
                    ),
                ),
            ),
            learner().openApp().path(),
        )
    }

    @Test
    fun `a Daily Set's title on the Path is the title of its first Tip, and a Daily Set with no Tip has none`() {
        val first = Tip("d1-t1", before = "d1-e2", title = mapOf("en" to "Hello"), body = mapOf("en" to "Body"))
        val second = Tip("d1-t2", before = "d1-e1", title = mapOf("en" to "Goodbye"), body = mapOf("en" to "Body"))
        val titled = Course(listOf(levelOf("a1", "A1", unitOf("a1-u01", "Greetings", d1.copy(tips = listOf(first, second)), d2))), items)

        val dailySets = ItemLearner(titled).openApp().path().levels.single().units.single().dailySets

        assertEquals(listOf("Hello", null), dailySets.map { it.title })
    }

    @Test
    fun `a Daily Set that was started and not finished is still today's, the same day and on a later day`() {
        val learner = learner()
        val engine = learner.openApp()
        learner.play(engine, engine.startDailySet(), answers = 1)

        assertEquals(listOf("d1" to TODAY, "d2" to LOCKED, "d3" to LOCKED), states(learner.openApp().path()))

        learner.clock.moveTo(march(5))
        assertEquals(listOf("d1" to TODAY, "d2" to LOCKED, "d3" to LOCKED), states(learner.openApp().path()))
    }

    @Test
    fun `a completed Daily Set shows as completed, and the next one is locked until midnight and today's from then`() {
        val learner = learner()
        learner.playDay(march(2))

        learner.clock.moveTo(march(2), LocalTime.of(23, 59))
        assertEquals(listOf("d1" to COMPLETED, "d2" to LOCKED, "d3" to LOCKED), states(learner.openApp().path()))

        learner.clock.moveTo(march(3), LocalTime.MIDNIGHT)
        assertEquals(listOf("d1" to COMPLETED, "d2" to TODAY, "d3" to LOCKED), states(learner.openApp().path()))
    }

    @Test
    fun `after missed days only the next Daily Set is today's`() {
        val learner = learner()
        learner.playDay(march(2))

        learner.clock.moveTo(march(9))
        assertEquals(listOf("d1" to COMPLETED, "d2" to TODAY, "d3" to LOCKED), states(learner.openApp().path()))
    }

    @Test
    fun `the second Level is not on the Path until every Daily Set of the first is completed, and its first Daily Set is today's the day after`() {
        val learner = learner()
        learner.playDay(march(2))
        learner.playDay(march(3))
        assertEquals(listOf("a1"), levels(learner.openApp().path()))

        learner.playDay(march(4))
        val done = learner.openApp()
        assertEquals(listOf("a1", "a2"), levels(done.path()))
        assertEquals(
            listOf("d1" to COMPLETED, "d2" to COMPLETED, "d3" to COMPLETED, "d4" to LOCKED),
            states(done.path()),
        )
        assertEquals(1, done.path().levels.last().units.single().dailySets.single().number)
        assertEquals(Today.DoneForToday(march(5).atStartOfDay(warsaw), reviewOnlyNext = false), done.today())

        learner.clock.moveTo(march(5))
        val next = learner.openApp()
        assertEquals("d4" to TODAY, states(next.path()).last())
        assertEquals(listOf("five"), shown(listOf(next.startDailySet())))
    }

    @Test
    fun `once every Daily Set released is completed they all show as completed and none is today's, whatever the Review-only Daily Set is doing`() {
        val learner = learner()
        (2..5).forEach { learner.playDay(march(it)) }
        val allCompleted = listOf("d1" to COMPLETED, "d2" to COMPLETED, "d3" to COMPLETED, "d4" to COMPLETED)

        assertEquals(allCompleted, states(learner.openApp().path()))

        learner.clock.moveTo(march(6))
        val engine = learner.openApp()
        assertEquals(Today.ReviewOnlyAvailable, engine.today())
        assertEquals(allCompleted, states(engine.path()))

        learner.play(engine, engine.startDailySet(), answers = 1)
        assertIs<Today.InProgress>(engine.today())
        assertEquals(allCompleted, states(engine.path()))

        learner.play(engine, engine.resumeDailySet())
        assertIs<Today.DoneForToday>(engine.today())
        assertEquals(allCompleted, states(learner.openApp().path()))
    }
}
