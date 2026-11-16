package com.polisheasy.engine

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReviewOnlyDailySetTest {
    private val abc = listOf(item("a"), item("b"), item("c"), item("d"), item("e"))
    private val d1 = day("d1", choose("d1-e1", "one", "a"), choose("d1-e2", "two", "b"), choose("d1-e3", "three", "c"))
    private val d2 = day("d2", choose("d2-e1", "four", "d"), choose("d2-e2", "five", "e"))
    private val d3 = day("d3", choose("d3-e1", "six", "f"))

    /**
     * The small course, completed on 2 and 3 March with every answer correct. Items a, b and c
     * were reviewed on 3 March, so they are in box 2 and due on 5 March; d and e are in box 1 and
     * due on 4 March. All five were last answered on 3 March.
     */
    private fun finishedSmallCourse(): ItemLearner = ItemLearner(courseOf(abc, d1, d2)).apply {
        playDay(march(2))
        playDay(march(3))
    }

    /** The same Learner after an update that added a third Daily Set. */
    private fun ItemLearner.afterNewContent() = ItemLearner(courseOf(abc + item("f"), d1, d2, d3), progress, clock)

    private fun midnight(date: LocalDate) = date.atStartOfDay(warsaw)

    private fun LearnerProgress.scheduleOf(item: String): ItemSchedule? = itemProgress()[item]?.schedule

    /** A course of one Daily Set with Items i0 to i29, each with one Exercise, p0 to p29. */
    private fun thirtyItems(schedules: Map<String, ItemSchedule>): ItemLearner {
        val items = (0..29).map { item("i$it") }
        val learner = ItemLearner(courseOf(items, day("d1", *(0..29).map { choose("d1-e$it", "p$it", "i$it") }.toTypedArray())))
        learner.progress.recordCompleted("d1", on = march(2), items = schedules.mapValues { ItemProgress(it.value) })
        return learner
    }

    @Test
    fun `the day the last released Daily Set is completed, the Learner is done for today and a Review-only Daily Set is next`() {
        val learner = finishedSmallCourse()

        assertEquals(Today.DoneForToday(nextUnlock = midnight(march(4)), reviewOnlyNext = true), learner.openApp().today())
        assertFailsWith<IllegalStateException> { learner.openApp().startDailySet() }
    }

    @Test
    fun `the next day a Review-only Daily Set is available, made entirely of Review, due Items first and then those answered longest ago`() {
        val learner = finishedSmallCourse()
        learner.clock.moveTo(march(4))
        assertEquals(Today.ReviewOnlyAvailable, learner.openApp().today())

        val steps = learner.playDay(march(4))

        assertEquals(listOf("four", "five", "one", "two", "three"), shown(steps))
        assertEquals(shown(steps), reviewed(steps))
        assertTrue(steps.none { it is Step.ShowTip })
        assertEquals(
            Step.ShowSummary(correctAnswers = 5, exercises = 5, itemsLearned = emptyList(), reviewOnly = true),
            steps.last(),
        )
    }

    @Test
    fun `it has 20 Exercises - the Items due, earliest first, then the Items answered longest ago, ties in Path order`() {
        val due = mapOf("i7" to march(4), "i3" to march(6), "i20" to march(8), "i12" to march(9), "i29" to march(10))
        val learner = thirtyItems(
            (0..29).associate { n ->
                val dueOn = due["i$n"]
                "i$n" to if (dueOn != null) {
                    ItemSchedule(box = 1, due = dueOn, lastAnswered = march(9))
                } else {
                    ItemSchedule(box = 3, due = march(20), lastAnswered = march(3 + n % 4))
                }
            },
        )

        val steps = learner.playDay(march(10))

        assertEquals(
            listOf("p7", "p3", "p20", "p12", "p29") +
                // Last answered on 3 March, then on 4 March, then the first two of those on 5 March.
                listOf("p0", "p4", "p8", "p16", "p24", "p28") +
                listOf("p1", "p5", "p9", "p13", "p17", "p21", "p25") +
                listOf("p2", "p6"),
            shown(steps),
        )
        assertEquals(REVIEW_ONLY_SIZE, (steps.last() as Step.ShowSummary).exercises)
    }

    @Test
    fun `when more than 20 Items are due, the 20 due earliest are given`() {
        val learner = thirtyItems(
            (0..29).associate { n ->
                val dueOn = when {
                    n == 24 -> march(5)
                    n == 10 -> march(6)
                    n < 25 -> march(9)
                    else -> march(20)
                }
                "i$n" to ItemSchedule(box = 1, due = dueOn, lastAnswered = march(2))
            },
        )

        val steps = learner.playDay(march(10))

        assertEquals(listOf("p24", "p10") + ((0..9) + (11..18)).map { "p$it" }, shown(steps))
        assertEquals(20, (steps.last() as Step.ShowSummary).exercises)
    }

    @Test
    fun `an Item is practised with the Exercise shown longest ago, so Review-only days vary the Exercise`() {
        val learner = ItemLearner(courseOf(abc, day("d1", choose("d1-e1", "one", "a"), choose("d1-e2", "two", "a"))))
        learner.playDay(march(2))

        assertEquals(listOf("one"), shown(learner.playDay(march(3))))
        assertEquals(listOf("two"), shown(learner.playDay(march(4))))
        assertEquals(listOf("one"), shown(learner.playDay(march(5))))
    }

    @Test
    fun `an Exercise that lists two Items is given once`() {
        val learner = ItemLearner(courseOf(abc, day("d1", choose("d1-e1", "one", "a", "b"), choose("d1-e2", "two", "c"))))
        learner.playDay(march(2))

        val steps = learner.playDay(march(3))

        assertEquals(listOf("one", "two"), shown(steps))
        assertEquals(2, (steps.last() as Step.ShowSummary).exercises)
    }

    @Test
    fun `answers in a Review-only Daily Set reschedule their Items like any other answers`() {
        val learner = finishedSmallCourse()

        learner.playDay(march(4), wrongFirst = setOf("one"))

        val progress = learner.progress
        assertEquals(ItemSchedule(box = 2, due = march(6), lastAnswered = march(4)), progress.scheduleOf("d"))
        assertEquals(ItemSchedule(box = 2, due = march(6), lastAnswered = march(4)), progress.scheduleOf("e"))
        assertEquals(ItemSchedule(box = 1, due = march(5), lastAnswered = march(4)), progress.scheduleOf("a"))
        // Not yet due, but answered correctly all the same.
        assertEquals(ItemSchedule(box = 3, due = march(8), lastAnswered = march(4)), progress.scheduleOf("b"))
        assertEquals(ItemSchedule(box = 3, due = march(8), lastAnswered = march(4)), progress.scheduleOf("c"))
    }

    @Test
    fun `a wrongly answered Exercise returns at the end, and the Review-only Daily Set is completed only when every Exercise is answered correctly`() {
        val learner = finishedSmallCourse()
        learner.clock.moveTo(march(4))
        val engine = learner.openApp()

        val steps = learner.play(engine, engine.startDailySet(), answers = 5, wrongFirst = setOf("four"))

        assertEquals(listOf("four", "five", "one", "two", "three", "four"), shown(steps))
        assertEquals(Today.InProgress(SetProgress(correct = 4, total = 5), reviewOnly = true), engine.today())
        assertEquals(emptySet(), learner.progress.reviewOnlyDays())

        val rest = learner.play(engine, steps.last())

        assertEquals(Step.ShowSummary(correctAnswers = 4, exercises = 5, reviewOnly = true), rest.last())
        assertEquals(setOf(march(4)), learner.progress.reviewOnlyDays())
    }

    @Test
    fun `one Review-only Daily Set per day - done for today once completed, and another the next day`() {
        val learner = finishedSmallCourse()
        val path = learner.progress.completedDailySets()

        learner.playDay(march(4))

        assertEquals(Today.DoneForToday(nextUnlock = midnight(march(5)), reviewOnlyNext = true), learner.openApp().today())
        assertFailsWith<IllegalStateException> { learner.openApp().startDailySet() }
        assertEquals(setOf(march(4)), learner.progress.reviewOnlyDays())
        assertEquals(mapOf("d1" to march(2), "d2" to march(3)), path)
        assertEquals(path, learner.progress.completedDailySets())

        learner.clock.moveTo(march(5))
        assertEquals(Today.ReviewOnlyAvailable, learner.openApp().today())
        learner.playDay(march(5))
        assertEquals(setOf(march(4), march(5)), learner.progress.reviewOnlyDays())
    }

    @Test
    fun `a Review-only Daily Set left unfinished resumes on a later day with the same Exercises and belongs to the day it is completed`() {
        val learner = finishedSmallCourse()
        learner.clock.moveTo(march(4))
        val engine = learner.openApp()
        val left = learner.play(engine, engine.startDailySet(), answers = 2).last()

        val reopened = learner.openApp()
        assertEquals(Today.InProgress(SetProgress(correct = 2, total = 5), reviewOnly = true), reopened.today())
        assertEquals(left, reopened.resumeDailySet())

        val steps = learner.playDay(march(6))

        assertEquals(listOf("one", "two", "three"), shown(steps))
        assertEquals(Step.ShowSummary(correctAnswers = 5, exercises = 5, reviewOnly = true), steps.last())
        assertEquals(setOf(march(6)), learner.progress.reviewOnlyDays())
        assertEquals(Today.DoneForToday(nextUnlock = midnight(march(7)), reviewOnlyNext = true), learner.openApp().today())
    }

    @Test
    fun `when the content gains a Daily Set, the Learner's next Daily Set is the first new one and all progress is kept`() {
        val before = finishedSmallCourse()
        before.playDay(march(4))
        before.playDay(march(5))

        val learner = before.afterNewContent()

        // A Review-only Daily Set was completed today, so the new Daily Set waits for tomorrow.
        assertEquals(Today.DoneForToday(nextUnlock = midnight(march(6)), reviewOnlyNext = false), learner.openApp().today())
        learner.clock.moveTo(march(6))
        assertEquals(Today.DailySetAvailable, learner.openApp().today())

        // On 9 March d and e are due; a, b and c are not.
        val untouched = learner.progress.itemProgress().getValue("a")
        val steps = learner.playDay(march(9))

        assertEquals(listOf("six", "four", "five"), shown(steps))
        assertEquals(listOf("four", "five"), reviewed(steps))
        assertEquals(
            Step.ShowSummary(3, 3, itemsLearned = listOf(ItemView("f", "f in English")), reviewOnly = false),
            steps.last(),
        )
        assertEquals(mapOf("d1" to march(2), "d2" to march(3), "d3" to march(9)), learner.progress.completedDailySets())
        assertEquals(setOf(march(4), march(5)), learner.progress.reviewOnlyDays())
        assertEquals(untouched, learner.progress.itemProgress().getValue("a"))
        assertEquals(Today.DoneForToday(nextUnlock = midnight(march(10)), reviewOnlyNext = true), learner.openApp().today())
    }

    @Test
    fun `a Review-only Daily Set left unfinished is dropped when the content gains a Daily Set`() {
        val before = finishedSmallCourse()
        before.clock.moveTo(march(4))
        val engine = before.openApp()
        before.play(engine, engine.startDailySet(), answers = 2)

        val learner = before.afterNewContent()

        assertEquals(Today.DailySetAvailable, learner.openApp().today())
        assertEquals(listOf("six"), shown(listOf(learner.openApp().startDailySet())))
        // The two answers given before the update still count.
        assertEquals(ItemSchedule(box = 2, due = march(6), lastAnswered = march(4)), learner.progress.scheduleOf("d"))
        assertEquals(ItemSchedule(box = 2, due = march(6), lastAnswered = march(4)), learner.progress.scheduleOf("e"))
        assertEquals(emptySet(), learner.progress.reviewOnlyDays())
    }

    @Test
    fun `a saved Review-only Daily Set whose Exercises no longer exist is ignored`() {
        val learner = finishedSmallCourse()
        learner.clock.moveTo(march(4))
        learner.progress.saveInProgress(
            DailySetInProgress(REVIEW_ONLY_ID, remaining = listOf("gone"), review = listOf("gone")),
        )

        assertEquals(Today.ReviewOnlyAvailable, learner.openApp().today())
        assertEquals(listOf("four", "five", "one", "two", "three"), shown(learner.playDay(march(4))))
    }
}
