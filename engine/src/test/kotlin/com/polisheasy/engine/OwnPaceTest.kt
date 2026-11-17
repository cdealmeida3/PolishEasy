package com.polisheasy.engine

import com.polisheasy.engine.DailySetState.COMPLETED
import com.polisheasy.engine.DailySetState.OPEN
import com.polisheasy.engine.DailySetState.TODAY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** With One a day off, the default: the Learner goes at their own pace. */
class OwnPaceTest {
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

    private fun learner() = ItemLearner(course, oneADay = false)

    private fun states(path: PathView): List<Pair<String, DailySetState>> =
        path.levels.flatMap { level -> level.units.flatMap { unit -> unit.dailySets.map { it.id to it.state } } }

    /** Starts [dailySetId], or the next one when it is null, and plays it to its Summary. */
    private fun ItemLearner.complete(dailySetId: String? = null): Step.ShowSummary {
        val engine = openApp()
        val steps = play(engine, engine.startDailySet(dailySetId = dailySetId))
        return assertIs<Step.ShowSummary>(steps.last())
    }

    @Test
    fun `the next Daily Set can be started as soon as one is completed, and three in a day count as one day of the Streak`() {
        val learner = learner()

        learner.complete()
        assertEquals(Today.DailySetAvailable, learner.openApp().today())
        learner.complete()
        learner.complete()

        val engine = learner.openApp()
        assertEquals(setOf("d1", "d2", "d3"), learner.progress.completedDailySets().keys)
        assertEquals(Today.DailySetAvailable, engine.today())
        assertEquals(1, engine.streak())
        assertEquals(3, engine.soFar().dailySetsCompleted)
        assertTrue(engine.doneToday())
    }

    @Test
    fun `any Daily Set not yet completed can be started, a later one first and then the earlier ones`() {
        val learner = learner()

        learner.complete("d3")
        assertEquals(setOf("d3"), learner.progress.completedDailySets().keys)
        // Home still offers the first one not completed.
        assertEquals(1 to 1, learner.openApp().nextDailySet()!!.let { it.unitNumber to it.numberInUnit })

        learner.complete("d2")
        learner.complete("d1")
        assertEquals(setOf("d1", "d2", "d3"), learner.progress.completedDailySets().keys)
        assertEquals("Food", learner.openApp().nextDailySet()!!.unitTitle)
    }

    @Test
    fun `starting another Daily Set while one is in progress forgets the one in progress`() {
        val learner = learner()
        val engine = learner.openApp()
        learner.play(engine, engine.startDailySet(), answers = 1)

        engine.startDailySet(dailySetId = "d3")

        assertEquals("d3", learner.progress.dailySetInProgress()?.dailySetId)
        val reopened = learner.openApp()
        assertIs<Today.InProgress>(reopened.today())
        assertEquals("About me", reopened.nextDailySet()!!.unitTitle)
        assertEquals(listOf("d1" to OPEN, "d2" to OPEN, "d3" to TODAY, "d4" to OPEN), states(reopened.path()))
    }

    @Test
    fun `starting the Daily Set in progress by its id picks it up where it was left`() {
        val learner = learner()
        val engine = learner.openApp()
        learner.play(engine, engine.startDailySet(), answers = 1)

        val step = learner.openApp().startDailySet(dailySetId = "d1")

        assertEquals("two", (assertIs<Step.ShowExercise>(step).exercise as ExerciseView.ChooseTranslation).prompt)
    }

    @Test
    fun `the Path has every Level open, nothing locked, and the Daily Set the Learner is at as today's`() {
        val learner = learner()

        val path = learner.openApp().path()

        assertEquals(listOf("a1", "a2"), path.levels.map { it.id })
        assertEquals(listOf("d1" to TODAY, "d2" to OPEN, "d3" to OPEN, "d4" to OPEN), states(path))

        learner.complete("d2")
        assertEquals(listOf("d1" to TODAY, "d2" to COMPLETED, "d3" to OPEN, "d4" to OPEN), states(learner.openApp().path()))
    }

    @Test
    fun `a Daily Set cannot be chosen with One a day on, nor one that is completed or not on the Path`() {
        val learner = learner()
        learner.complete()

        assertFailsWith<IllegalStateException> { learner.openApp().startDailySet(dailySetId = "d1") }
        assertFailsWith<IllegalStateException> { learner.openApp().startDailySet(dailySetId = "nowhere") }

        learner.oneADay = true
        assertFailsWith<IllegalStateException> { learner.openApp().startDailySet(dailySetId = "d2") }
    }

    @Test
    fun `a Unit done out of order is completed by whichever of its Daily Sets is completed last`() {
        val learner = learner()

        learner.complete("d2")
        val engine = learner.openApp()
        assertNull(engine.unitCompleted())
        learner.play(engine, engine.startDailySet(dailySetId = "d1"))

        assertEquals(UnitCompletedView(1, "Greetings"), engine.unitCompleted())
    }

    @Test
    fun `with every Daily Set completed a Review-only Daily Set is offered again as soon as one is finished`() {
        val learner = learner()
        repeat(4) { learner.complete() }
        assertEquals(Today.ReviewOnlyAvailable, learner.openApp().today())

        val summary = learner.complete()

        assertTrue(summary.reviewOnly)
        assertEquals(Today.ReviewOnlyAvailable, learner.openApp().today())
        assertTrue(learner.complete().reviewOnly)
    }

    @Test
    fun `Practice is open before anything is done today`() {
        assertTrue(learner().openApp().practiceOpen())
    }

    @Test
    fun `the Reminder is due until something is completed today, and again the next day`() {
        val learner = learner()
        assertTrue(learner.openApp().reminderDue())

        learner.complete()
        assertFalse(learner.openApp().reminderDue())

        learner.clock.moveTo(march(3))
        assertTrue(learner.openApp().reminderDue())
    }

    @Test
    fun `the Reminder is not due when nothing is left to do`() {
        val learner = ItemLearner(Course(listOf(levelOf("a1", day("x", choose("x-e1", "x")))), emptyList()), oneADay = false)
        learner.complete()

        learner.clock.moveTo(march(3))

        assertEquals(Today.AllCompleted, learner.openApp().today())
        assertFalse(learner.openApp().reminderDue())
    }

    @Test
    fun `turning One a day on after a completion today gives done for today, and off again the next one`() {
        val learner = learner()
        learner.complete()
        val engine = learner.openApp()

        learner.oneADay = true
        assertIs<Today.DoneForToday>(engine.today())
        assertTrue(engine.practiceOpen())

        learner.oneADay = false
        assertEquals(Today.DailySetAvailable, engine.today())
    }
}
