package com.polisheasy.engine

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReplayTest {
    private val items = listOf("a", "b", "c", "d", "e", "f", "g", "h", "i").map { item(it) }
    private val d1 = day("d1", choose("d1-e1", "one", "a"), choose("d1-e2", "two", "b"), choose("d1-e3", "three", "c"))
    private val d2 = day("d2", choose("d2-e1", "four", "d"), choose("d2-e2", "five", "e"))
    private val d3 = day("d3", choose("d3-e1", "six", "f"))
    private val d4 = day("d4", choose("d4-e1", "seven", "g"))
    private val d5 = day("d5", choose("d5-e1", "eight", "h"), choose("d5-e2", "nine", "i"))
    private val course = courseOf(items, d1, d2, d3, d4, d5)

    private val wrong = Answer.Option(1)

    private fun learner(progress: LearnerProgress = InMemoryLearnerProgress()) = ItemLearner(course, progress)

    @Test
    fun `a completed Daily Set is replayed from its first Tip through its New Material to a Summary`() {
        val learner = Learner()
        val engine = learner.openApp()
        engine.playToSummary(engine.startDailySet())

        val steps = engine.playToSummary(engine.startReplay("a1-u01-d01"))

        assertEquals(
            listOf("Tip: Saying hello", "cześć", "do widzenia", "Tip: Saying thank you", "dziękuję", "Summary"),
            steps.map(::describe),
        )
        assertTrue(steps.all { it.isReplay })
        assertEquals(
            listOf(0, 0, 1, 2, 2).map { SetProgress(correct = it, total = 3) },
            steps.dropLast(1).map { if (it is Step.ShowTip) it.progress else (it as Step.ShowExercise).progress },
        )
        assertEquals(
            Step.ShowSummary(correctAnswers = 3, exercises = 3, itemsLearned = emptyList(), reviewOnly = false, replay = true),
            steps.last(),
        )
    }

    @Test
    fun `a Replay has only the Daily Set's own Exercises, even when Items are due for Review`() {
        val learner = learner()
        learner.playDay(march(2))
        assertTrue(reviewed(learner.playDay(march(3))).isNotEmpty())

        learner.clock.moveTo(march(4))
        val engine = learner.openApp()
        val steps = learner.play(engine, engine.startReplay("d2"))

        assertEquals(listOf("four", "five"), shown(steps))
        assertEquals(emptyList(), reviewed(steps))
        assertEquals(SetProgress(correct = 0, total = 2), (steps.first() as Step.ShowExercise).progress)
        assertEquals(Step.ShowSummary(correctAnswers = 2, exercises = 2, replay = true), steps.last())
    }

    @Test
    fun `in a Replay a wrong answer shows the correct answer, and the Exercise returns at the end until it is answered correctly`() {
        val learner = learner()
        learner.playDay(march(2))
        val engine = learner.openApp()

        val first = engine.startReplay("d1")
        val feedback = engine.answer(wrong)
        val steps = listOf(first) + learner.play(engine, engine.next())

        assertEquals(Feedback(correct = false, correctAnswer = "right"), feedback)
        assertEquals(listOf("one", "two", "three", "one"), shown(steps))
        assertEquals(Step.ShowSummary(correctAnswers = 2, exercises = 3, replay = true), steps.last())
    }

    @Test
    fun `a Daily Set that is today's, locked or unknown cannot be replayed`() {
        val learner = learner()
        learner.playDay(march(2))
        learner.clock.moveTo(march(3))
        val engine = learner.openApp()
        val today = engine.today()
        val path = engine.path()

        listOf("d2", "d3", "nope", REVIEW_ONLY_ID).forEach { id ->
            assertFailsWith<IllegalStateException>(id) { engine.startReplay(id) }
        }

        assertEquals(Today.DailySetAvailable, today)
        assertEquals(today, engine.today())
        assertEquals(path, engine.path())
        assertEquals(listOf("four"), shown(listOf(engine.startDailySet())))
    }

    @Test
    fun `a Replay changes nothing that is saved`() {
        val file = File(createTempDirectory("polisheasy-replay").toFile().apply { deleteOnExit() }, "progress.json")
        val learner = learner(FileLearnerProgress(file))
        learner.playDay(march(2))
        learner.clock.moveTo(march(3))
        val engine = learner.openApp()
        learner.play(engine, engine.startDailySet(), answers = 1)
        val saved = file.readText()
        val today = engine.today()
        val streak = engine.streak()
        val path = engine.path()

        val steps = learner.play(engine, engine.startReplay("d1"), wrongFirst = setOf("one"))

        assertEquals(Step.ShowSummary(correctAnswers = 2, exercises = 3, replay = true), steps.last())
        assertEquals(saved, file.readText())
        assertEquals(today, engine.today())
        assertEquals(streak, engine.streak())
        assertEquals(path, engine.path())
        val reopened = ItemLearner(course, FileLearnerProgress(file), learner.clock).openApp()
        assertEquals(today, reopened.today())
        assertEquals(streak, reopened.streak())
        assertEquals(path, reopened.path())
    }

    @Test
    fun `a Replay does not count toward the Streak`() {
        val learner = learner()
        learner.playDay(march(2))

        learner.clock.moveTo(march(3))
        val engine = learner.openApp()
        assertEquals(1, engine.streak())
        learner.play(engine, engine.startReplay("d1"))

        assertEquals(1, engine.streak())
        assertEquals(Today.DailySetAvailable, engine.today())
        assertEquals(1, learner.openApp().streak())

        learner.clock.moveTo(march(4))
        assertEquals(0, learner.openApp().streak())
    }

    @Test
    fun `a Replay leaves every Item's Review schedule alone`() {
        val plain = learner()
        val replaying = learner()
        listOf(plain, replaying).forEach { learner -> (2..3).forEach { learner.playDay(march(it)) } }

        replaying.clock.moveTo(march(4))
        val engine = replaying.openApp()
        val replay = replaying.play(engine, engine.startReplay("d1"), wrongFirst = setOf("one", "two", "three"))
        assertEquals(Step.ShowSummary(correctAnswers = 0, exercises = 3, replay = true), replay.last())

        (4..6).forEach { day ->
            assertEquals(reviewed(plain.playDay(march(day))), reviewed(replaying.playDay(march(day))), "Review on $day March")
        }
        assertTrue(plain.progress.itemProgress().isNotEmpty())
        assertEquals(plain.progress.itemProgress(), replaying.progress.itemProgress())
    }

    @Test
    fun `a Replay does not disturb a Daily Set in progress`() {
        val learner = learner()
        learner.playDay(march(2))
        learner.clock.moveTo(march(3))
        val engine = learner.openApp()
        assertEquals(listOf("four", "five"), shown(learner.play(engine, engine.startDailySet(), answers = 1)))
        val inProgress = engine.today()
        assertEquals(1, (inProgress as Today.InProgress).progress.correct)

        learner.play(engine, engine.startReplay("d1"))

        assertEquals(inProgress, engine.today())
        assertEquals(listOf("five"), shown(listOf(engine.resumeDailySet())))
        val reopened = learner.openApp()
        assertEquals(inProgress, reopened.today())
        val rest = learner.play(reopened, reopened.resumeDailySet())
        assertEquals("five", shown(rest).first())
        assertTrue(rest.none { it.isReplay })
        assertEquals(mapOf("d1" to march(2), "d2" to march(3)), learner.progress.completedDailySets())
    }

    @Test
    fun `a Replay left unfinished is forgotten, and the next Daily Set starts as usual`() {
        val learner = learner()
        learner.playDay(march(2))
        learner.clock.moveTo(march(3))
        val engine = learner.openApp()
        val today = engine.today()

        learner.play(engine, engine.startReplay("d1"), answers = 1)
        assertEquals(today, learner.openApp().today())

        val steps = learner.play(engine, engine.startDailySet())
        assertEquals("four", shown(steps).first())
        assertTrue(steps.none { it.isReplay })
        assertEquals(
            listOf(DailySetState.COMPLETED, DailySetState.COMPLETED, DailySetState.LOCKED),
            engine.path().levels.single().units.single().dailySets.take(3).map { it.state },
        )
        assertEquals(2, engine.streak())
    }
}
