package com.polisheasy.engine

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ItemsAndReviewTest {
    private val abc = listOf(item("a"), item("b"), item("c"), item("d"), item("e"))

    /** Box 1, answered on [answered] and due the next day. */
    private fun boxOne(answered: LocalDate) =
        ItemSchedule(box = 1, due = answered.plusDays(1), lastAnswered = answered)

    private fun LearnerProgress.scheduleOf(item: String): ItemSchedule? = itemProgress()[item]?.schedule

    @Test
    fun `a correct answer puts each Item of the Exercise in box 1, due tomorrow`() {
        val learner = ItemLearner(
            courseOf(abc, day("d1", choose("d1-e1", "one", "a"), choose("d1-e2", "two", "b", "c"))),
        )

        learner.playDay(march(2))

        assertEquals(
            mapOf(
                "a" to ItemProgress(boxOne(march(2)), recentExercises = listOf("d1-e1")),
                "b" to ItemProgress(boxOne(march(2)), recentExercises = listOf("d1-e2")),
                "c" to ItemProgress(boxOne(march(2)), recentExercises = listOf("d1-e2")),
            ),
            learner.progress.itemProgress(),
        )
    }

    @Test
    fun `an answer accepted with an accent warning counts as correct for its Item`() {
        val learner = ItemLearner(
            courseOf(
                abc,
                day("d1", choose("d1-e1", "one", "a")),
                day("d2", typed("d2-e1", "thank you", "dziękuję", "a")),
            ),
        )
        learner.playDay(march(2))
        learner.clock.moveTo(march(3))
        val engine = learner.openApp()
        engine.startDailySet()

        val feedback = engine.answer(Answer.Typed("dziekuje"))

        assertEquals(Feedback(correct = true, correctAnswer = "dziękuję", accentWarning = true), feedback)
        assertEquals(
            ItemSchedule(box = 2, due = march(5), lastAnswered = march(3)),
            learner.progress.scheduleOf("a"),
        )
    }

    @Test
    fun `only the first attempt at an Exercise counts for its Items`() {
        val learner = ItemLearner(courseOf(abc, day("d1", choose("d1-e1", "one", "a"))))

        val steps = learner.playDay(march(2), wrongFirst = setOf("one"))

        assertEquals(listOf("one", "one"), shown(steps))
        assertEquals(
            mapOf("a" to ItemProgress(boxOne(march(2)), recentExercises = listOf("d1-e1"))),
            learner.progress.itemProgress(),
        )
    }

    @Test
    fun `an Item in box 3 answered wrongly then correctly on the same day ends in box 1`() {
        val learner = ItemLearner(
            courseOf(
                abc,
                day("d1", choose("d1-e1", "one", "a")),
                day("d2", choose("d2-e1", "two", "a"), choose("d2-e2", "three", "a")),
            ),
        )
        learner.progress.recordCompleted(
            "d1",
            on = march(2),
            items = mapOf("a" to ItemProgress(ItemSchedule(box = 3, due = march(9), lastAnswered = march(2)))),
        )

        val steps = learner.playDay(march(9), wrongFirst = setOf("two"))

        assertEquals(listOf("two", "three", "two"), shown(steps))
        assertEquals(boxOne(march(9)), learner.progress.scheduleOf("a"))
    }

    @Test
    fun `the first Daily Set has no Review`() {
        val learner = ItemLearner(
            courseOf(abc, day("d1", choose("d1-e1", "one", "a"), choose("d1-e2", "two", "b"))),
        )

        val steps = learner.playDay(march(2))

        assertEquals(listOf("one", "two"), shown(steps))
        assertEquals(emptyList(), reviewed(steps))
        assertEquals(Step.ShowSummary(2, 2, itemsLearned = listOf(ItemView("a", "a in English"), ItemView("b", "b in English"))), steps.last())
    }

    @Test
    fun `the next day's Daily Set ends with a Review Exercise for each Item due, counted in its progress and Summary`() {
        val learner = ItemLearner(
            courseOf(
                abc,
                day("d1", choose("d1-e1", "one", "a"), choose("d1-e2", "two", "b")),
                day("d2", choose("d2-e1", "three", "c")),
            ),
        )
        learner.playDay(march(2))

        val steps = learner.playDay(march(3))

        assertEquals(listOf("three", "one", "two"), shown(steps))
        assertEquals(listOf("one", "two"), reviewed(steps))
        assertEquals(
            listOf(SetProgress(0, 3), SetProgress(1, 3), SetProgress(2, 3)),
            steps.filterIsInstance<Step.ShowExercise>().map { it.progress },
        )
        val summary = steps.last() as Step.ShowSummary
        assertEquals(3, summary.correctAnswers)
        assertEquals(3, summary.exercises)
    }

    @Test
    fun `an Item answered correctly in Review returns two days later, and one answered wrongly returns the next day`() {
        val learner = ItemLearner(
            courseOf(
                abc,
                day("d1", choose("d1-e1", "one", "a"), choose("d1-e2", "two", "b")),
                day("d2", choose("d2-e1", "three", "c")),
                day("d3", choose("d3-e1", "four", "d")),
                day("d4", choose("d4-e1", "five", "e")),
            ),
        )
        learner.playDay(march(2))

        val dayTwo = learner.playDay(march(3), wrongFirst = setOf("two"))
        val dayThree = learner.playDay(march(4))
        val dayFour = learner.playDay(march(5))

        assertEquals(listOf("one", "two", "two"), reviewed(dayTwo))
        // "one" moved to box 2 and is not due; "two" went back to box 1; "three" was new yesterday.
        assertEquals(listOf("two", "three"), reviewed(dayThree))
        assertEquals(listOf("one", "four"), reviewed(dayFour))
    }

    @Test
    fun `at most 8 Review Exercises are given, for the Items due earliest, ties in Path order`() {
        val items = (0..9).map { item("i$it") }
        val dayOne = day("d1", *(0..9).map { choose("d1-e$it", "p$it", "i$it") }.toTypedArray())
        val learner = ItemLearner(courseOf(items + item("x"), dayOne, day("d2", choose("d2-e1", "new", "x"))))
        val due = (0..9).associate { "i$it" to march(3) } + mapOf("i9" to march(1), "i4" to march(2))
        learner.progress.recordCompleted(
            "d1",
            on = march(2),
            items = due.mapValues { ItemProgress(ItemSchedule(box = 1, due = it.value, lastAnswered = march(1))) },
        )

        val steps = learner.playDay(march(5))

        assertEquals(listOf("p9", "p4", "p0", "p1", "p2", "p3", "p5", "p6"), reviewed(steps))
        assertEquals(9, (steps.last() as Step.ShowSummary).exercises)
    }

    @Test
    fun `an Item practised by today's New Material is not also given a Review Exercise`() {
        val learner = ItemLearner(
            courseOf(
                abc,
                day("d1", choose("d1-e1", "one", "a"), choose("d1-e2", "two", "b")),
                day("d2", choose("d2-e1", "three", "a"), choose("d2-e2", "four", "c")),
            ),
        )
        learner.playDay(march(2))

        val steps = learner.playDay(march(3))

        assertEquals(listOf("three", "four", "two"), shown(steps))
        assertEquals(listOf("two"), reviewed(steps))
    }

    @Test
    fun `an Item with two Exercises is reviewed with the one shown longest ago, then with the other`() {
        val learner = ItemLearner(
            courseOf(
                abc,
                day("d1", choose("d1-e1", "one", "a"), choose("d1-e2", "two", "a")),
                day("d2", choose("d2-e1", "three", "b")),
                day("d3", choose("d3-e1", "four", "c")),
            ),
        )
        learner.playDay(march(2))

        val firstReview = learner.playDay(march(3))
        // "a" is now in box 2, due again on 5 March.
        val secondReview = learner.playDay(march(5))

        assertEquals(listOf("one"), reviewed(firstReview))
        assertEquals(listOf("three", "two"), reviewed(secondReview))
        assertEquals(listOf("d1-e1", "d1-e2"), learner.progress.itemProgress().getValue("a").recentExercises)
    }

    @Test
    fun `an Exercise never shown to the Learner is preferred for Review`() {
        val learner = ItemLearner(
            courseOf(
                abc,
                day("d1", choose("d1-e1", "one", "a"), choose("d1-e2", "two", "a")),
                day("d2", choose("d2-e1", "three", "b")),
            ),
        )
        learner.progress.recordCompleted(
            "d1",
            on = march(2),
            items = mapOf("a" to ItemProgress(boxOne(march(2)), recentExercises = listOf("d1-e1"))),
        )

        assertEquals(listOf("two"), reviewed(learner.playDay(march(3))))
    }

    @Test
    fun `one Exercise tagged with two due Items appears once in the Review`() {
        val learner = ItemLearner(
            courseOf(
                abc,
                day("d1", choose("d1-e1", "one", "a", "b")),
                day("d2", choose("d2-e1", "two", "c")),
            ),
        )
        learner.playDay(march(2))

        val steps = learner.playDay(march(3))

        assertEquals(listOf("two", "one"), shown(steps))
        assertEquals(2, (steps.last() as Step.ShowSummary).exercises)
    }

    @Test
    fun `Items of a Daily Set completed before Items were tracked are due the day after it was completed`() {
        val learner = ItemLearner(
            courseOf(
                abc,
                day("d1", choose("d1-e1", "one", "a")),
                day("d2", choose("d2-e1", "two", "b")),
            ),
        )
        learner.progress.recordCompleted("d1", on = march(2))

        val steps = learner.playDay(march(3))

        assertEquals(listOf("one"), reviewed(steps))
        // It counted as box 1, so the correct answer moved it to box 2.
        assertEquals(ItemSchedule(box = 2, due = march(5), lastAnswered = march(3)), learner.progress.scheduleOf("a"))
    }

    @Test
    fun `resuming mid-Review, after reopening and on a later day, continues with the same Review Exercises`() {
        val learner = ItemLearner(
            courseOf(
                abc,
                day("d1", choose("d1-e1", "one", "a"), choose("d1-e2", "two", "b"), choose("d1-e3", "three", "c")),
                day("d2", choose("d2-e1", "four", "d")),
            ),
        )
        learner.playDay(march(2))
        learner.clock.moveTo(march(3))
        val engine = learner.openApp()

        val beforeClosing = learner.play(engine, engine.startDailySet(), answers = 2).last()

        assertEquals(
            Step.ShowExercise(
                ExerciseView.ChooseTranslation("two", listOf("right", "wrong")),
                SetProgress(correct = 2, total = 4),
                review = true,
            ),
            beforeClosing,
        )
        assertEquals(beforeClosing, learner.openApp().resumeDailySet())

        learner.clock.moveTo(march(6))
        assertEquals(Today.InProgress(SetProgress(correct = 2, total = 4)), learner.openApp().today())
        val later = learner.openApp()
        val steps = learner.play(later, later.resumeDailySet())

        assertEquals(beforeClosing, steps.first())
        assertEquals(listOf("two", "three"), reviewed(steps))
        assertEquals(4, (steps.last() as Step.ShowSummary).exercises)
    }

    @Test
    fun `a wrongly answered Review Exercise returns at the end and counts against the Summary`() {
        val learner = ItemLearner(
            courseOf(
                abc,
                day("d1", choose("d1-e1", "one", "a"), choose("d1-e2", "two", "b")),
                day("d2", choose("d2-e1", "three", "c")),
            ),
        )
        learner.playDay(march(2))

        val steps = learner.playDay(march(3), wrongFirst = setOf("one"))

        assertEquals(listOf("three", "one", "two", "one"), shown(steps))
        assertTrue(steps.filterIsInstance<Step.ShowExercise>().last().review)
        val summary = steps.last() as Step.ShowSummary
        assertEquals(2, summary.correctAnswers)
        assertEquals(3, summary.exercises)
    }

    @Test
    fun `missed New Material returns after the Review`() {
        val learner = ItemLearner(
            courseOf(
                abc,
                day("d1", choose("d1-e1", "one", "a")),
                day("d2", choose("d2-e1", "two", "b")),
            ),
        )
        learner.playDay(march(2))

        val steps = learner.playDay(march(3), wrongFirst = setOf("two"))

        assertEquals(listOf("two", "one", "two"), shown(steps))
        assertEquals(listOf("one"), reviewed(steps))
    }

    @Test
    fun `the Summary lists the Items the Daily Set introduces, in order of first appearance`() {
        val items = listOf(
            item("a", "cześć", "hi"),
            item("b", "tak", "yes"),
            item("c", "nie", "no"),
            item("d", "proszę", "please"),
        )
        val learner = ItemLearner(
            courseOf(
                items,
                day("d1", choose("d1-e1", "one", "b"), choose("d1-e2", "two", "a", "b")),
                day("d2", choose("d2-e1", "three", "d", "a"), choose("d2-e2", "four", "c")),
            ),
        )

        val dayOne = learner.playDay(march(2)).last() as Step.ShowSummary
        val dayTwo = learner.playDay(march(3)).last() as Step.ShowSummary

        assertEquals(listOf(ItemView("tak", "yes"), ItemView("cześć", "hi")), dayOne.itemsLearned)
        assertEquals(listOf(ItemView("proszę", "please"), ItemView("nie", "no")), dayTwo.itemsLearned)
    }

    @Test
    fun `Item progress has no Level in it`() {
        val course = Course(
            levels = listOf(
                levelOf("a1", day("d1", choose("d1-e1", "one", "a"))),
                levelOf("a2", day("d2", choose("d2-e1", "two", "a"))),
            ),
            items = abc,
        )
        val learner = ItemLearner(course)

        learner.playDay(march(2))
        learner.playDay(march(3))

        assertEquals(
            mapOf(
                "a" to ItemProgress(
                    ItemSchedule(box = 2, due = march(5), lastAnswered = march(3)),
                    recentExercises = listOf("d1-e1", "d2-e1"),
                ),
            ),
            learner.progress.itemProgress(),
        )
    }

    @Test
    fun `the scheduling rule can be replaced without touching content or stored Path progress`() {
        val course = courseOf(
            abc,
            day("d1", choose("d1-e1", "one", "a")),
            day("d2", choose("d2-e1", "two", "b")),
        )
        val alwaysDueToday = object : ReviewScheduler {
            override fun afterAnswer(previous: ItemSchedule?, correct: Boolean, today: LocalDate) =
                ItemSchedule(box = 9, due = today, lastAnswered = today)
        }
        val replaced = ItemLearner(course, scheduler = alwaysDueToday)
        val default = ItemLearner(course)

        listOf(replaced, default).forEach { learner ->
            learner.playDay(march(2))
            learner.playDay(march(3))
        }

        assertEquals(
            mapOf(
                "a" to ItemProgress(ItemSchedule(9, due = march(3), lastAnswered = march(3)), listOf("d1-e1")),
                "b" to ItemProgress(ItemSchedule(9, due = march(3), lastAnswered = march(3)), listOf("d2-e1")),
            ),
            replaced.progress.itemProgress(),
        )
        assertEquals(mapOf("d1" to march(2), "d2" to march(3)), replaced.progress.completedDailySets())
        assertEquals(default.progress.completedDailySets(), replaced.progress.completedDailySets())
    }
}
