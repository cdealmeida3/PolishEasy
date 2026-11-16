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
    fun `Item progress survives reopening with its box, dates and recently shown Exercises`() {
        val items = mapOf(
            "czesc" to ItemProgress(
                ItemSchedule(box = 3, due = march(9), lastAnswered = march(5)),
                recentExercises = listOf("a1-u01-d01-e02", "a1-u01-d03-e04"),
            ),
            "tak" to ItemProgress(ItemSchedule(box = 1, due = march(6), lastAnswered = march(5))),
        )
        val progress = FileLearnerProgress(file)

        progress.saveInProgress(inProgress, items)

        assertEquals(items, progress.itemProgress())
        assertEquals(items, reopened().itemProgress())
    }

    @Test
    fun `Items given with the queue or the completion are stored with it, merged with those already stored`() {
        val first = ItemProgress(ItemSchedule(box = 1, due = march(3), lastAnswered = march(2)))
        val moved = ItemProgress(ItemSchedule(box = 2, due = march(5), lastAnswered = march(3)), listOf("a1-u01-d01-e01"))
        val progress = FileLearnerProgress(file)

        progress.saveInProgress(inProgress, mapOf("czesc" to first, "tak" to first))
        progress.recordCompleted("a1-u01-d02", on = march(3), items = mapOf("tak" to moved, "nie" to first))

        val expected = mapOf("czesc" to first, "tak" to moved, "nie" to first)
        assertEquals(expected, progress.itemProgress())
        assertEquals(expected, reopened().itemProgress())
        assertEquals(mapOf("a1-u01-d02" to march(3)), reopened().completedDailySets())
        assertNull(reopened().dailySetInProgress())

        reopened().saveInProgress(inProgress)
        assertEquals(expected, reopened().itemProgress())
        assertEquals(inProgress, reopened().dailySetInProgress())
    }

    @Test
    fun `the Review Exercises chosen for a Daily Set in progress survive reopening`() {
        val withReview = inProgress.copy(
            remaining = inProgress.remaining + "a1-u01-d01-e01",
            review = listOf("a1-u01-d01-e01"),
        )

        FileLearnerProgress(file).saveInProgress(withReview)

        assertEquals(withReview, reopened().dailySetInProgress())
    }

    @Test
    fun `a file written before Items were tracked loads with empty Item progress and its completions intact`() {
        file.writeText(
            """
            {
              "version": 1,
              "completed": { "a1-u01-d01": "2026-03-02" },
              "inProgress": { "dailySetId": "a1-u01-d02", "remaining": ["a1-u01-d02-e01"], "missed": [], "shownTips": [] }
            }
            """,
        )

        val progress = FileLearnerProgress(file)

        assertEquals(emptyMap(), progress.itemProgress())
        assertEquals(mapOf("a1-u01-d01" to march(2)), progress.completedDailySets())
        assertEquals(
            DailySetInProgress("a1-u01-d02", remaining = listOf("a1-u01-d02-e01"), review = emptyList()),
            progress.dailySetInProgress(),
        )
    }

    @Test
    fun `a file with an unreadable Item date is moved aside like any other corrupt file`() {
        file.writeText(
            """
            {
              "version": 1,
              "completed": { "a1-u01-d01": "2026-03-02" },
              "items": { "czesc": { "box": 2, "due": "soon", "lastAnswered": "2026-03-02" } }
            }
            """,
        )

        val progress = FileLearnerProgress(file)

        assertEquals(emptyMap(), progress.itemProgress())
        assertEquals(emptyMap(), progress.completedDailySets())
        assertFalse(file.exists())
        assertTrue(File(directory, "progress.json.corrupt").exists())
    }

    @Test
    fun `Review-only days survive reopening`() {
        val progress = FileLearnerProgress(file)
        progress.recordReviewOnlyCompleted(on = march(5))
        progress.recordReviewOnlyCompleted(on = march(4))

        assertEquals(setOf(march(4), march(5)), progress.reviewOnlyDays())
        assertEquals(setOf(march(4), march(5)), reopened().reviewOnlyDays())

        // Other changes keep them.
        reopened().saveInProgress(inProgress)
        reopened().recordCompleted("a1-u01-d02", on = march(6))
        assertEquals(setOf(march(4), march(5)), reopened().reviewOnlyDays())
    }

    @Test
    fun `recording a Review-only completion clears the Daily Set in progress and stores its Items, and leaves the completed Daily Sets alone`() {
        val moved = ItemProgress(ItemSchedule(box = 2, due = march(6), lastAnswered = march(4)), listOf("a1-u01-d01-e01"))
        val progress = FileLearnerProgress(file)
        progress.recordCompleted("a1-u01-d01", on = march(2))
        progress.saveInProgress(DailySetInProgress(REVIEW_ONLY_ID, remaining = listOf("a1-u01-d01-e01"), review = listOf("a1-u01-d01-e01")))

        progress.recordReviewOnlyCompleted(on = march(4), items = mapOf("czesc" to moved))

        listOf(progress, reopened()).forEach { saved ->
            assertNull(saved.dailySetInProgress())
            assertEquals(mapOf("czesc" to moved), saved.itemProgress())
            assertEquals(mapOf("a1-u01-d01" to march(2)), saved.completedDailySets())
            assertEquals(setOf(march(4)), saved.reviewOnlyDays())
        }
    }

    @Test
    fun `a file written before Review-only days existed loads with none and its other progress intact`() {
        file.writeText(
            """
            {
              "version": 1,
              "completed": { "a1-u01-d01": "2026-03-02" },
              "inProgress": { "dailySetId": "a1-u01-d02", "remaining": ["a1-u01-d02-e01"] },
              "items": { "czesc": { "box": 2, "due": "2026-03-05", "lastAnswered": "2026-03-03" } }
            }
            """,
        )

        val progress = FileLearnerProgress(file)

        assertEquals(emptySet(), progress.reviewOnlyDays())
        assertEquals(mapOf("a1-u01-d01" to march(2)), progress.completedDailySets())
        assertEquals(DailySetInProgress("a1-u01-d02", remaining = listOf("a1-u01-d02-e01")), progress.dailySetInProgress())
        assertEquals(
            mapOf("czesc" to ItemProgress(ItemSchedule(box = 2, due = march(5), lastAnswered = march(3)))),
            progress.itemProgress(),
        )
        assertTrue(file.exists())
    }

    @Test
    fun `a file with an unreadable Review-only day is moved aside like any other corrupt file`() {
        file.writeText(
            """
            {
              "version": 1,
              "completed": { "a1-u01-d01": "2026-03-02" },
              "reviewOnlyDays": ["2026-03-04", "someday"]
            }
            """,
        )

        val progress = FileLearnerProgress(file)

        assertEquals(emptySet(), progress.reviewOnlyDays())
        assertEquals(emptyMap(), progress.completedDailySets())
        assertFalse(file.exists())
        assertTrue(File(directory, "progress.json.corrupt").exists())
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
