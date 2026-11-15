package com.polisheasy.engine

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SavedProgressTest {
    private val directory: File = createTempDirectory("polisheasy-progress").toFile().apply { deleteOnExit() }
    private val file = File(directory, "progress.json")

    private val inProgress = DailySetInProgress(
        dailySetId = "a1-u01-d02",
        remaining = listOf("a1-u01-d02-e02", "a1-u01-d02-e01"),
        missed = setOf("a1-u01-d02-e01"),
        shownTips = setOf("a1-u01-d02-t01"),
    )

    /** What a newly opened app reads from the same file. */
    private fun reopened() = FileLearnerProgress(file)

    @Test
    fun `completed Daily Sets and their dates survive reopening`() {
        val progress = FileLearnerProgress(file)
        progress.recordCompleted("a1-u01-d01", on = march(2))
        progress.recordCompleted("a1-u01-d02", on = march(5))

        val expected = mapOf("a1-u01-d01" to march(2), "a1-u01-d02" to march(5))
        assertEquals(expected, progress.completedDailySets())
        assertEquals(expected, reopened().completedDailySets())
    }

    @Test
    fun `a Daily Set in progress survives reopening with its queue, misses and shown Tips`() {
        FileLearnerProgress(file).saveInProgress(inProgress)

        val saved = reopened().dailySetInProgress()

        assertEquals(inProgress, saved)
        assertEquals(listOf("a1-u01-d02-e02", "a1-u01-d02-e01"), saved?.remaining)
    }

    @Test
    fun `recording a completion clears the Daily Set in progress`() {
        val progress = FileLearnerProgress(file)
        progress.saveInProgress(inProgress)

        progress.recordCompleted("a1-u01-d02", on = march(3))

        assertNull(progress.dailySetInProgress())
        assertNull(reopened().dailySetInProgress())
        assertEquals(mapOf("a1-u01-d02" to march(3)), reopened().completedDailySets())
    }

    @Test
    fun `with no file progress is empty`() {
        val progress = FileLearnerProgress(file)

        assertEquals(emptyMap(), progress.completedDailySets())
        assertNull(progress.dailySetInProgress())
    }

    @Test
    fun `a malformed file gives empty progress and is moved aside`() {
        file.writeText("""{ "version": 1, "completed": { "a1-u01-d01": """)

        val progress = FileLearnerProgress(file)

        assertEquals(emptyMap(), progress.completedDailySets())
        assertNull(progress.dailySetInProgress())
        assertFalse(file.exists())
        assertTrue(File(directory, "progress.json.corrupt").exists())

        progress.recordCompleted("a1-u01-d01", on = march(2))
        assertEquals(mapOf("a1-u01-d01" to march(2)), reopened().completedDailySets())
    }

    @Test
    fun `a file with a field this version does not know still loads`() {
        file.writeText(
            """
            {
              "version": 2,
              "completed": { "a1-u01-d01": "2026-03-02" },
              "itemBoxes": { "czesc": 3 },
              "inProgress": { "dailySetId": "a1-u01-d02", "remaining": ["a1-u01-d02-e01"], "hints": 1 }
            }
            """,
        )

        val progress = FileLearnerProgress(file)

        assertEquals(mapOf("a1-u01-d01" to march(2)), progress.completedDailySets())
        assertEquals(DailySetInProgress("a1-u01-d02", remaining = listOf("a1-u01-d02-e01")), progress.dailySetInProgress())
    }

    @Test
    fun `a Daily Set can be played across closing and reopening the app`() {
        val clock = FakeClock()
        fun openApp() = CourseEngine(FixtureCourse.load(), reopened(), clock)

        val first = openApp()
        first.play(first.startDailySet(), listOf(wrongHello, right.getValue("do widzenia")))

        val second = openApp()
        assertEquals(Today.InProgress(SetProgress(correct = 1, total = 3)), second.today())
        val steps = second.playToSummary(second.resumeDailySet())
        assertEquals(listOf("Tip: Saying thank you", "dziękuję", "cześć", "Summary"), steps.map(::describe))
        assertEquals(Step.ShowSummary(correctAnswers = 2, exercises = 3), steps.last())

        assertIs<Today.DoneForToday>(openApp().today())
        clock.moveTo(march(3))
        assertEquals(Today.DailySetAvailable, openApp().today())
    }
}
