package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CantListenTest {
    private val spoken = listOf("one", "tak", "be", "nie", "dobrze", "pa", "two", "three")

    /**
     * Day 1 has three listening Exercises, a Tip before the listen-and-type one, and one Exercise
     * for each of the Items a, b and c. Day 2 starts with a listening Exercise and has Review from
     * day 1; the day after day 3 is Review-only.
     */
    private val course = courseOf(
        listOf("a", "b", "c", "d", "e", "f").map { item(it) },
        spoken.map { clip(it) },
        DailySet(
            "d1",
            tips = listOf(Tip("d1-t1", before = "d1-e4", title = mapOf("en" to "Title"), body = mapOf("en" to "Body"))),
            exercises = listOf(
                choose("d1-e1", "one", "a"),
                listenChoose("d1-e2", "tak", "a"),
                typed("d1-e3", "say b", "be", "b"),
                listenType("d1-e4", "nie", "b").copy(readingAnswers = listOf("nie nie", "żaden")),
                listenChoose("d1-e5", "dobrze", "c"),
            ),
        ),
        day("d2", listenChoose("d2-e1", "pa", "d"), choose("d2-e2", "two", "e")),
        day("d3", choose("d3-e1", "three", "f")),
    )

    private val takOptions = optionsOf(listenChoose("d1-e2", "tak"))

    private fun ids(steps: List<Step>): List<String> = steps.filterIsInstance<Step.ShowExercise>().map { it.exerciseId }

    private fun views(steps: List<Step>): List<ExerciseView> = steps.filterIsInstance<Step.ShowExercise>().map { it.exercise }

    private fun assertNothingToHear(steps: List<Step>) = views(steps).forEach { view ->
        assertTrue(view !is ExerciseView.ListenChoose && view !is ExerciseView.ListenType, "$view is heard")
    }

    /** A new Learner who has answered day 1's first Exercise and is shown "tak" to listen to. */
    private fun atTak(learner: ItemLearner = ItemLearner(course)): CourseEngine {
        val engine = learner.openApp()
        val steps = learner.play(engine, engine.startDailySet(), answers = 1)
        assertIs<ExerciseView.ListenChoose>((steps.last() as Step.ShowExercise).exercise)
        return engine
    }

    @Test
    fun `Polish can be said without being asked for only while the Learner can listen`() {
        val before = ItemLearner(course).openApp()
        assertEquals(false, before.canListen())
        assertEquals(null, before.clipOf("tak"))

        val engine = atTak()
        assertEquals(true, engine.canListen())
        assertEquals(ClipView("audio/tak.ogg"), engine.clipOf("Tak"))
        assertEquals(null, engine.clipOf("a word with no Clip"))

        engine.cantListenNow()
        assertEquals(false, engine.canListen())
        assertEquals(null, engine.clipOf("tak"))

        val silent = ItemLearner(course).openApp()
        silent.startDailySet(sound = false)
        assertEquals(false, silent.canListen())
        assertEquals(null, silent.clipOf("tak"))
    }

    @Test
    fun `can't listen now shows the same Exercise as a choice of the Polish for its meaning`() {
        val learner = ItemLearner(course)
        val engine = learner.openApp()
        val heard = learner.play(engine, engine.startDailySet(), answers = 1).last() as Step.ShowExercise

        assertEquals(
            Step.ShowExercise(
                ExerciseView.ChoosePolish("tak in English", (heard.exercise as ExerciseView.ListenChoose).options),
                SetProgress(correct = 1, total = 5),
                exerciseId = "d1-e2",
            ),
            engine.cantListenNow(),
        )
        assertEquals(takOptions, (heard.exercise as ExerciseView.ListenChoose).options)
    }

    @Test
    fun `the reading form of a listen-and-choose Exercise is checked as when it is heard`() {
        val clip = ClipView("audio/tak.ogg")
        listOf(
            takOptions.indexOf("tak") to Feedback(correct = true, correctAnswer = "tak", audio = clip),
            takOptions.indexOf("inne") to Feedback(correct = false, correctAnswer = "tak", audio = clip),
            7 to Feedback(correct = false, correctAnswer = "tak", audio = clip),
        ).forEach { (index, expected) ->
            val engine = atTak()
            engine.cantListenNow()

            assertEquals(expected, engine.answer(Answer.Option(index)), "Option $index")
        }
    }

    @Test
    fun `every listening Exercise still to come is read, the others are unchanged, and the order and count do not change`() {
        val learner = ItemLearner(course)
        val engine = atTak(learner)

        val steps = learner.play(engine, engine.cantListenNow())

        assertEquals(listOf("d1-e2", "d1-e3", "d1-e4", "d1-e5"), ids(steps))
        assertEquals(
            listOf(
                ExerciseView.ChoosePolish("tak in English", takOptions),
                ExerciseView.TypeAnswer("say b"),
                ExerciseView.TypeAnswer("nie in English"),
                ExerciseView.ChoosePolish("dobrze in English", optionsOf(listenChoose("d1-e5", "dobrze"))),
            ),
            views(steps),
        )
        assertEquals(
            listOf(1, 2, 3, 4).map { SetProgress(correct = it, total = 5) },
            steps.filterIsInstance<Step.ShowExercise>().map { it.progress },
        )
        assertEquals(Step.ShowSummary(5, 5, listOf("a", "b", "c").map { ItemView(it, "$it in English") }), steps.last())
    }

    @Test
    fun `a Tip before a listening Exercise is still shown before its reading form`() {
        val learner = ItemLearner(course)
        val engine = atTak(learner)

        val steps = learner.play(engine, engine.cantListenNow())

        val tip = steps.indexOfFirst { it is Step.ShowTip }
        assertEquals(1, steps.count { it is Step.ShowTip })
        assertEquals("d1-e4", (steps[tip + 1] as Step.ShowExercise).exerciseId)
    }

    @Test
    fun `read, a listen-and-type Exercise accepts its Accepted Answers and its reading answers, and heard, only its Accepted Answers`() {
        fun correct(answer: String) = Feedback(correct = true, correctAnswer = answer)
        val wrong = Feedback(correct = false, correctAnswer = "nie")
        fun feedback(typed: String, read: Boolean): Feedback {
            val learner = ItemLearner(course)
            val engine = learner.openApp()
            val steps = learner.play(engine, engine.startDailySet(), answers = 3)
            assertIs<ExerciseView.ListenType>((steps.last() as Step.ShowExercise).exercise)
            if (read) engine.cantListenNow()
            return engine.answer(Answer.Typed(typed)).copy(audio = null)
        }

        // What was typed, what the Learner is told when it was read, and when it was heard.
        listOf(
            Triple("nie", correct("nie"), correct("nie")),
            Triple("Nie!", correct("nie"), correct("nie")),
            Triple("nie nie", correct("nie nie"), wrong),
            Triple("żaden", correct("żaden"), wrong),
            Triple("zaden", Feedback(correct = true, correctAnswer = "żaden", accentWarning = true), wrong),
            Triple("tak", wrong, wrong),
            Triple("", wrong, wrong),
        ).forEach { (typed, read, heard) ->
            assertEquals(read, feedback(typed, read = true), "\"$typed\", read")
            assertEquals(heard, feedback(typed, read = false), "\"$typed\", heard")
        }
    }

    @Test
    fun `the feedback of a listen-and-type Exercise that is read still carries the Clip of what would have been said`() {
        val learner = ItemLearner(course)
        val engine = atTak(learner)
        learner.play(engine, engine.cantListenNow(), answers = 2)

        assertEquals(ClipView("audio/nie.ogg"), engine.answer(Answer.Typed("nie nie")).audio)
    }

    @Test
    fun `opting out records nothing`() {
        val learner = ItemLearner(course)
        val engine = atTak(learner)
        val items = learner.progress.itemProgress()
        val saved = learner.progress.dailySetInProgress()
        val today = engine.today()

        engine.cantListenNow()

        assertEquals(items, learner.progress.itemProgress())
        assertEquals(saved?.copy(cantListen = true), learner.progress.dailySetInProgress())
        assertEquals(Today.InProgress(SetProgress(correct = 1, total = 5)), today)
        assertEquals(today, engine.today())
        assertEquals(today, learner.openApp().today())
    }

    @Test
    fun `a Daily Set finished by reading has the same Summary and the same Item progress as one finished by listening`() {
        val listener = ItemLearner(course)
        val reader = ItemLearner(course)

        val heard = listener.playDay(march(2))
        val engine = atTak(reader)
        val read = reader.play(engine, engine.cantListenNow())

        assertEquals(Step.ShowSummary(5, 5, listOf("a", "b", "c").map { ItemView(it, "$it in English") }), heard.last())
        assertEquals(heard.last(), read.last())
        assertEquals(listener.progress.itemProgress(), reader.progress.itemProgress())
        assertEquals(listener.progress.completedDailySets(), reader.progress.completedDailySets())
        assertEquals(null, reader.progress.dailySetInProgress())
    }

    @Test
    fun `a listening Exercise missed before opting out is one miss, however its reading form goes`() {
        val learner = ItemLearner(course)
        val engine = atTak(learner)
        engine.answer(Answer.Option(takOptions.indexOf("inne")))
        // "say b" is answered, then the Tip, then "nie" to listen to.
        val before = learner.play(engine, engine.next(), answers = 1)
        assertIs<ExerciseView.ListenType>((before.last() as Step.ShowExercise).exercise)

        val steps = learner.play(engine, engine.cantListenNow(), answers = 2)

        val returned = steps.last() as Step.ShowExercise
        assertEquals("d1-e2", returned.exerciseId)
        assertEquals(ExerciseView.ChoosePolish("tak in English", takOptions), returned.exercise)
        engine.answer(Answer.Option(takOptions.indexOf("inne")))
        assertEquals(returned, engine.next())
        engine.answer(Answer.Option(takOptions.indexOf("tak")))
        assertEquals(Step.ShowSummary(4, 5, listOf("a", "b", "c").map { ItemView(it, "$it in English") }), engine.next())
    }

    @Test
    fun `the choice survives closing the app, and a Tip not yet followed by an answer is shown again`() {
        val learner = ItemLearner(course)
        val engine = learner.openApp()
        val before = learner.play(engine, engine.startDailySet(), answers = 3)
        assertIs<Step.ShowTip>(before[before.size - 2])
        assertIs<ExerciseView.ListenType>((before.last() as Step.ShowExercise).exercise)
        engine.cantListenNow()

        val reopened = learner.openApp()
        val steps = learner.play(reopened, reopened.resumeDailySet())

        assertIs<Step.ShowTip>(steps.first())
        assertEquals(listOf("d1-e4", "d1-e5"), ids(steps))
        assertEquals(
            listOf(ExerciseView.TypeAnswer("nie in English"), ExerciseView.ChoosePolish("dobrze in English", optionsOf(listenChoose("d1-e5", "dobrze")))),
            views(steps),
        )
    }

    @Test
    fun `a Tip already followed by an answer is not shown again after opting out and closing the app`() {
        val learner = ItemLearner(course)
        val engine = learner.openApp()
        learner.play(engine, engine.startDailySet(), answers = 3)
        // The Tip was shown and "nie" is answered wrongly, so it returns after "dobrze".
        engine.answer(Answer.Typed("tak"))
        engine.next()
        engine.answer(Answer.Option(optionsOf(listenChoose("d1-e5", "dobrze")).indexOf("dobrze")))
        assertIs<ExerciseView.ListenType>((engine.next() as Step.ShowExercise).exercise)
        engine.cantListenNow()

        val reopened = learner.openApp()

        assertEquals(
            Step.ShowExercise(ExerciseView.TypeAnswer("nie in English"), SetProgress(correct = 4, total = 5), exerciseId = "d1-e4"),
            reopened.resumeDailySet(),
        )
    }

    @Test
    fun `the choice survives to a later day`() {
        val learner = ItemLearner(course)
        val engine = atTak(learner)
        learner.play(engine, engine.cantListenNow(), answers = 1)

        val steps = learner.playDay(march(5))

        assertEquals(listOf("d1-e3", "d1-e4", "d1-e5"), ids(steps))
        assertNothingToHear(steps)
        assertEquals(mapOf("d1" to march(5)), learner.progress.completedDailySets())
    }

    @Test
    fun `the next Daily Set is heard again`() {
        val learner = ItemLearner(course)
        val engine = atTak(learner)
        learner.play(engine, engine.cantListenNow())

        val steps = learner.playDay(march(3))

        assertEquals(
            ExerciseView.ListenChoose(ClipView("audio/pa.ogg"), optionsOf(listenChoose("d2-e1", "pa"))),
            views(steps).first(),
        )
        // Item c has no other Exercise, so the Review brings "dobrze" back, and it is heard.
        val dobrze = steps.filterIsInstance<Step.ShowExercise>().single { it.exerciseId == "d1-e5" }
        assertTrue(dobrze.review)
        assertIs<ExerciseView.ListenChoose>(dobrze.exercise)
        assertEquals(false, learner.progress.dailySetInProgress()?.cantListen ?: false)
    }

    @Test
    fun `Review Exercises that are listening ones are read too, and still marked as Review`() {
        val learner = ItemLearner(course)
        learner.playDay(march(2))
        learner.clock.moveTo(march(3))
        val engine = learner.openApp()
        assertIs<ExerciseView.ListenChoose>((engine.startDailySet() as Step.ShowExercise).exercise)

        val steps = learner.play(engine, engine.cantListenNow())

        assertNothingToHear(steps)
        val dobrze = steps.filterIsInstance<Step.ShowExercise>().single { it.exerciseId == "d1-e5" }
        assertTrue(dobrze.review)
        assertEquals(ExerciseView.ChoosePolish("dobrze in English", optionsOf(listenChoose("d1-e5", "dobrze"))), dobrze.exercise)
        val summary = steps.last() as Step.ShowSummary
        assertEquals(summary.exercises, summary.correctAnswers)
    }

    @Test
    fun `a Review-only Daily Set can be finished by reading`() {
        val learner = ItemLearner(course)
        (2..4).forEach { learner.playDay(march(it)) }
        learner.clock.moveTo(march(5))
        val engine = learner.openApp()
        assertEquals(Today.ReviewOnlyAvailable, engine.today())
        var step = engine.startDailySet()
        val before = mutableListOf<Step>()
        while ((step as Step.ShowExercise).exercise.let { it !is ExerciseView.ListenChoose && it !is ExerciseView.ListenType }) {
            before += learner.play(engine, step, answers = 1).first()
            step = engine.next()
        }

        val steps = learner.play(engine, engine.cantListenNow())

        assertEquals(step.exerciseId, (steps.first() as Step.ShowExercise).exerciseId)
        assertNothingToHear(steps)
        assertTrue(steps.filterIsInstance<Step.ShowExercise>().all { it.review })
        val summary = steps.last() as Step.ShowSummary
        assertTrue(summary.reviewOnly)
        assertEquals(summary.exercises, summary.correctAnswers)
        assertEquals(summary.exercises, before.size + ids(steps).size)
        assertEquals(Today.DoneForToday(march(6).atStartOfDay(warsaw), reviewOnlyNext = true), engine.today())
        assertEquals(4, engine.streak())
    }

    @Test
    fun `in a Replay the choice lasts for that Replay and saves nothing`() {
        val learner = ItemLearner(course)
        learner.playDay(march(2))
        learner.clock.moveTo(march(3))
        val engine = learner.openApp()
        engine.startDailySet()
        val saved = learner.progress.dailySetInProgress()
        val items = learner.progress.itemProgress()
        val replay = learner.play(engine, engine.startReplay("d1"), answers = 1)
        assertIs<ExerciseView.ListenChoose>((replay.last() as Step.ShowExercise).exercise)

        val steps = learner.play(engine, engine.cantListenNow())

        assertEquals(listOf("d1-e2", "d1-e3", "d1-e4", "d1-e5"), ids(steps))
        assertNothingToHear(steps)
        assertTrue(steps.all { it.isReplay })
        assertEquals(1, steps.count { it is Step.ShowTip })
        assertEquals(Step.ShowSummary(5, 5, replay = true), steps.last())
        assertEquals(saved, learner.progress.dailySetInProgress())
        assertEquals(false, saved?.cantListen)
        assertEquals(items, learner.progress.itemProgress())

        val again = learner.play(engine, engine.startReplay("d1"))
        assertEquals(3, views(again).count { it is ExerciseView.ListenChoose || it is ExerciseView.ListenType })
    }

    @Test
    fun `with sound off a Daily Set has the same Exercises in the same order, none of them a listening one, and nothing to play`() {
        val heard = ItemLearner(course).playDay(march(2))
        val learner = ItemLearner(course)
        val engine = learner.openApp()
        val steps = mutableListOf(engine.startDailySet(sound = false))
        val feedback = mutableListOf<Feedback>()
        while (steps.last() !is Step.ShowSummary) {
            val step = steps.last()
            if (step is Step.ShowExercise) {
                feedback += engine.answer(
                    when (step.exerciseId) {
                        "d1-e1" -> Answer.Option(0)
                        "d1-e2" -> Answer.Option(takOptions.indexOf("tak"))
                        "d1-e3" -> Answer.Typed("be")
                        "d1-e4" -> Answer.Typed("nie nie")
                        else -> Answer.Option(optionsOf(listenChoose("d1-e5", "dobrze")).indexOf("dobrze"))
                    },
                )
            }
            steps += engine.next()
        }

        assertEquals(ids(heard), ids(steps))
        assertNothingToHear(steps)
        assertEquals(ExerciseView.ChooseTranslation("one", listOf("right", "wrong"), promptAudio = null), views(steps).first())
        assertEquals(ClipView("audio/one.ogg"), (views(heard).first() as ExerciseView.ChooseTranslation).promptAudio)
        assertEquals(List(5) { null }, feedback.map { it.audio })
        assertTrue(feedback.all { it.correct })
        assertEquals(1, steps.count { it is Step.ShowTip })
        assertEquals(heard.last(), steps.last())
    }

    @Test
    fun `sound off is not remembered with the Daily Set`() {
        val learner = ItemLearner(course)
        val engine = learner.openApp()
        learner.play(engine, engine.startDailySet(sound = false), answers = 1)

        val reopened = learner.openApp()

        assertEquals(false, learner.progress.dailySetInProgress()!!.cantListen)
        assertEquals(
            Step.ShowExercise(
                ExerciseView.ListenChoose(ClipView("audio/tak.ogg"), takOptions),
                SetProgress(correct = 1, total = 5),
                exerciseId = "d1-e2",
            ),
            reopened.resumeDailySet(),
        )
    }

    @Test
    fun `a Daily Set resumed or replayed with sound off is read`() {
        val learner = ItemLearner(course)
        atTak(learner)
        val resumed = learner.openApp()

        val rest = learner.play(resumed, resumed.resumeDailySet(sound = false))

        assertEquals(listOf("d1-e2", "d1-e3", "d1-e4", "d1-e5"), ids(rest))
        assertNothingToHear(rest)
        assertEquals(false, learner.progress.dailySetInProgress()?.cantListen ?: false)

        val replay = learner.play(resumed, resumed.startReplay("d1", sound = false))

        assertEquals(5, ids(replay).size)
        assertNothingToHear(replay)
        assertEquals(null, (views(replay).first() as ExerciseView.ChooseTranslation).promptAudio)
    }

    @Test
    fun `a Review-only Daily Set started with sound off is read`() {
        val learner = ItemLearner(course)
        (2..4).forEach { learner.playDay(march(it)) }
        learner.clock.moveTo(march(5))
        val engine = learner.openApp()

        val steps = learner.play(engine, engine.startDailySet(sound = false))

        assertNothingToHear(steps)
        assertTrue(views(steps).any { it is ExerciseView.ChoosePolish })
        assertTrue((steps.last() as Step.ShowSummary).reviewOnly)
    }

    @Test
    fun `can't listen now fails when no Daily Set is being played`() {
        val learner = ItemLearner(course)
        assertFailsWith<IllegalStateException> { learner.openApp().cantListenNow() }

        val engine = learner.openApp()
        learner.play(engine, engine.startDailySet())

        assertFailsWith<IllegalStateException> { engine.cantListenNow() }
        assertEquals(null, learner.progress.dailySetInProgress())
    }
}
