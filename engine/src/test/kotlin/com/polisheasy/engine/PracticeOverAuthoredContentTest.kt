package com.polisheasy.engine

import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Practice over the course shipped with the app, for a Learner at each point along the Path. */
class PracticeOverAuthoredContentTest {
    private val contentDir = File(System.getProperty("contentDir"))
    private val course = loadCourse { path -> File(contentDir, path).readText() }

    /** A Learner who completed the first [days] Daily Sets, the last of them today. */
    private fun practiceAfter(days: Int, seed: Int = 1): Practice {
        val clock = FakeClock()
        val progress = InMemoryLearnerProgress()
        course.path.take(days).forEachIndexed { index, dailySet ->
            progress.recordCompleted(dailySet.id, march(2).minusDays((days - 1 - index).toLong()))
        }
        return CourseEngine(course, progress, clock).practice(InMemoryPracticeProgress(), Random(seed))
    }

    @Test
    fun `every verb table gives four forms to pick from, each a form of that verb`() {
        val tables = course.path.flatMap { it.tips }.mapNotNull { it.conjugation }
        assertTrue(tables.isNotEmpty())
        tables.forEach { table ->
            assertEquals(Person.entries.toSet(), table.forms.keys, "The table of ${table.verb}")
            assertTrue(table.forms.values.toSet().size >= 4, "The table of ${table.verb} has fewer than four different forms")
        }
        val forms = tables.groupBy { it.verb }.mapValues { (_, same) -> same.flatMap { it.forms.values }.toSet() }
        val practice = practiceAfter(course.path.size)
        practice.setup(Game.VERB_CHALLENGE).choices.first().options.forEach { verb ->
            repeat(5) {
                val round = practice.verbRound(verb.id)
                round.run.questions.forEach { question ->
                    val wanted = if (question.stage == VerbStage.TABLE) 6 else 4
                    assertEquals(wanted, question.options.toSet().size, "${question.verb} ${question.person} ${question.sentence}")
                    assertTrue(forms.getValue(question.verb).containsAll(question.options), "$question")
                    assertEquals(1, question.options.count { it in question.answers }, "$question")
                }
            }
        }
    }

    @Test
    fun `at every point on the Path a Round asks only what has been learned`() {
        (1..course.path.size).forEach { days ->
            val learned = course.path.take(days)
            val tables = learned.flatMap { it.tips }.mapNotNull { it.conjugation?.verb }.toSet()
            val sentences = learned.flatMap { it.exercises }.filterIsInstance<Conjugation>().mapNotNull { it.sentence }.toSet()
            val practice = practiceAfter(days)
            val game = practice.view().games.single { it.game == Game.VERB_CHALLENGE }
            assertEquals(if (tables.isEmpty()) GameState.NEEDS_MORE else GameState.READY, game.state, "After $days Daily Sets")
            if (game.state != GameState.READY) return@forEach
            practice.setup(Game.VERB_CHALLENGE).choices.first().options.forEach { verb ->
                val round = practice.verbRound(verb.id).playOut()
                assertTrue(round.finished)
                round.run.questions.forEach { question ->
                    assertTrue(question.verb in tables, "After $days Daily Sets: ${question.verb}")
                    question.sentence?.let { assertTrue(it.before + GAP + it.after in sentences, "After $days Daily Sets: $it") }
                }
            }
        }
    }

    @Test
    fun `Picture cards opens after the first Daily Set and shows only pictures of what has been learned`() {
        assertTrue(course.pictures.size >= 50, "The course has ${course.pictures.size} pictures")
        (1..course.path.size).forEach { days ->
            val learned = course.path.take(days).flatMap { it.itemIds() }.toSet()
            val practice = practiceAfter(days)
            assertEquals(GameState.READY, practice.view().games.single { it.game == Game.PICTURE_CARDS }.state, "After $days Daily Sets")
            val cards = practice.pictureCards()
            assertEquals(learned.filter { it in course.pictures }.toSet(), cards.cards.map { it.itemId }.toSet(), "After $days Daily Sets")
            cards.decks.options.forEach { deck ->
                val inDeck = practice.pictureCards(deck.id)
                assertEquals(deck.id, inDeck.decks.selected)
                assertTrue(inDeck.cards.size >= PictureCards.LEAST, "After $days Daily Sets the deck ${deck.id} has ${inDeck.cards.size} cards")
            }
        }
    }

    @Test
    fun `Memory pairs lays a board of what has been learned in every way it offers, and no two pairs look alike`() {
        (1..course.path.size).forEach { days ->
            val learned = course.path.take(days).flatMap { it.itemIds() }.toSet()
            val practice = practiceAfter(days)
            val game = practice.view().games.single { it.game == Game.MEMORY_PAIRS }
            assertEquals(if (learned.size < 6) GameState.NEEDS_MORE else GameState.READY, game.state, "After $days Daily Sets")
            if (game.state != GameState.READY) return@forEach
            practice.setup(Game.MEMORY_PAIRS).choices.first().options.forEach { kind ->
                practice.setup(Game.MEMORY_PAIRS, listOf(kind.id)).choices[1].options.forEach { deck ->
                    practice.setup(Game.MEMORY_PAIRS, listOf(kind.id, deck.id)).choices[2].options.forEach { size ->
                        val way = listOf(kind.id, deck.id, size.id)
                        val round = practice.start(practice.setup(Game.MEMORY_PAIRS, way)) as MemoryRound
                        val about = "After $days Daily Sets, $way"
                        assertEquals(way, round.played, about)
                        assertEquals(2 * size.id.removePrefix("pairs-").toInt(), round.tiles.size, about)
                        assertTrue(learned.containsAll(round.asked), about)
                        // A word and the sound of it are one pair; any other two tiles alike would be two.
                        val faces = round.tiles.map { it.face }.filter { it !is MemoryFace.Sound }
                        assertEquals(faces.size, faces.toSet().size, about)
                        assertEquals(faces.size, faces.map { if (it is MemoryFace.Image) it.picture else it }.toSet().size, about)
                    }
                }
            }
        }
    }

    @Test
    fun `every picture is of a word that can be pictured without knowing Polish, once`() {
        // A numeral stands for its number and nothing else.
        val numerals = course.pictures.filterValues { it is Picture.Glyph }
        numerals.forEach { (item, picture) ->
            val text = (picture as Picture.Glyph).text
            assertTrue(text == "+" || text.toIntOrNull() != null, "$item is drawn as \"$text\"")
        }
        assertEquals(numerals.size, numerals.values.toSet().size)
    }
}
