package com.polisheasy.engine

import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Each chapter's own Games and Chats on its Lesson screen, and every Game open from the start, over the course shipped with the app. */
class ChapterGamesTest {
    private val contentDir = File(System.getProperty("contentDir"))
    private val course = loadCourse { path -> File(contentDir, path).readText() }
    private val chapters = course.levels.flatMap { it.units }.filter { it.number != null && it.dailySets.isNotEmpty() }

    private fun practice(everything: Boolean, progress: LearnerProgress = InMemoryLearnerProgress()): Practice =
        CourseEngine(course, progress, FakeClock(), everythingOpen = { everything }).practice(InMemoryPracticeProgress(), Random(3))

    @Test
    fun `every Game of a chapter is listed on that chapter, and every Chat on exactly one`() {
        val practice = practice(everything = false)
        val units = course.levels.flatMap { it.units }
        val listed = units.associate { it.id to practice.chapter(it.id) }
        val written = chapters.mapNotNull { it.number }.toSet()
        Game.entries.filter { it.chapter in written }.forEach { game ->
            val on = listed.filterValues { view -> view.games.any { it.game == game } }.keys
            assertEquals(listOf(chapters.single { it.number == game.chapter }.id), on.toList(), "$game")
        }
        course.chats.forEach { script ->
            assertEquals(1, listed.values.count { view -> view.chats.any { it.id == script.id } }, "${script.id} is on one chapter")
        }
        units.filter { it.number == null }.forEach { assertEquals(emptyList(), listed.getValue(it.id).games, "${it.id} is not a chapter of the book") }
        val first = listed.getValue(chapters.single { it.number == 1 }.id)
        assertEquals(listOf(Game.REGISTER_SORT), first.games.map { it.game })
        assertTrue(first.chats.none { it.ready }, "Nothing is learned yet")
        assertTrue(first.games.all { it.state == GameState.NEEDS_MORE })
    }

    @Test
    fun `with everything open a new Learner can play every Game and every Chat`() {
        val practice = practice(everything = true)
        practice.view(sound = true).games.forEach { assertEquals(GameState.READY, it.state, "${it.game}") }
        chapters.forEach { chapter ->
            practice.chapter(chapter.id).chats.forEach { chat ->
                assertTrue(chat.ready, chat.id)
                assertEquals(chat.id, practice.chatSetup(chat.id).selected.first())
            }
        }
        Game.entries.filter { it.rounds }.forEach { game ->
            val round = practice.start(practice.setup(game))
            assertTrue(round.total > 0, "$game")
        }
    }

    @Test
    fun `everything open leaves the Learner's own progress as it was`() {
        val progress = InMemoryLearnerProgress()
        practice(everything = true, progress).view()
        assertEquals(emptyMap(), progress.completedDailySets())
    }
}
