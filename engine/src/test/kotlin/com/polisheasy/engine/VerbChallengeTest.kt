package com.polisheasy.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class VerbChallengeTest {
    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.VERB_CHALLENGE }.state

    @Test
    fun `the Verb challenge opens with the first verb table of a completed Daily Set`() {
        // The fixture course of the other tests has no conjugation table at all.
        val noTable = Learner().apply { progress.recordCompleted("a1-u01-d01", march(2)) }
        val practice = noTable.openApp().practice(InMemoryPracticeProgress())
        assertEquals(GameState.NEEDS_MORE, practice.view().games.single().state)
        assertFailsWith<IllegalStateException> { practice.setup(Game.VERB_CHALLENGE) }

        assertEquals(GameState.READY, stateOf(Player().completed("d1")))
    }

    @Test
    fun `only verbs of completed Daily Sets can be chosen, and Mixed once there are two`() {
        fun verbs(player: Player) = player.practice().setup(Game.VERB_CHALLENGE).choices.first().options.map { it.id }

        assertEquals(listOf("być"), verbs(Player().completed("d1")))
        assertEquals(listOf("być", "mieć", "mixed"), verbs(Player().completed("d1", "d2")))
    }

    @Test
    fun `a Round has six forms to pick, the table to fill and four sentences`() {
        val round = Player().completed("d1").practice().verbRound("być")

        assertEquals(
            List(6) { VerbStage.PICK } + List(6) { VerbStage.TABLE } + List(4) { VerbStage.SENTENCE },
            round.run.questions.map { it.stage },
        )
        assertEquals(Person.entries.toSet(), round.run.questions.take(6).map { it.person }.toSet())
        assertEquals(Person.entries.toSet(), round.run.questions.subList(6, 12).map { it.person }.toSet())
        assertEquals(16, round.total)
    }

    @Test
    fun `a verb with fewer than four sentences asks only those it has`() {
        val round = Player().completed("d1", "d2").practice().verbRound("mieć")

        assertEquals(listOf("___ czas.", "My ___ czas.").toSet(), round.run.questions.mapNotNull { it.sentence }.map { it.before + "___" + it.after }.toSet())
        assertEquals(14, round.total)
    }

    @Test
    fun `sentences come only from completed Daily Sets`() {
        val player = Player().completed("d1", "d2")
        repeat(20) { seed ->
            val round = Player(progress = player.progress, seed = seed).practice().verbRound("mixed")
            assertTrue(round.run.questions.none { it.verb == "mówić" })
            assertTrue(round.run.questions.mapNotNull { it.sentence }.none { it.after == " po polsku." })
        }
    }

    @Test
    fun `every option is a form of the verb asked, and one of four is right`() {
        val forms = mapOf("być" to byc, "mieć" to miec).mapValues { it.value.forms.values.toSet() }
        repeat(20) { seed ->
            val round = Player(seed = seed).completed("d1", "d2").practice().verbRound("mixed")
            round.run.questions.filter { it.stage != VerbStage.TABLE }.forEach { question ->
                assertEquals(4, question.options.toSet().size, "$question")
                assertTrue(forms.getValue(question.verb).containsAll(question.options), "$question")
                assertEquals(1, question.options.count { it in question.answers }, "$question")
            }
        }
    }

    @Test
    fun `Mixed asks about more than one verb`() {
        val round = Player().completed("d1", "d2").practice().verbRound("mixed")

        assertEquals(setOf("być", "mieć"), round.run.questions.map { it.verb }.toSet())
        // One verb's table is filled, whole.
        assertEquals(1, round.run.questions.filter { it.stage == VerbStage.TABLE }.map { it.verb }.toSet().size)
    }

    @Test
    fun `a right answer counts and a wrong one does not, and each question is asked once`() {
        var round = Player().completed("d1").practice().verbRound("być")

        round = round.choose(round.rightOption())
        assertEquals(true, round.given?.correct)
        // A second tap changes nothing once the question is answered.
        assertEquals(round, round.choose(round.wrongOption()))
        round = round.next()
        round = round.choose(round.wrongOption())
        assertEquals(false, round.given?.correct)
        round = round.next().playOut()

        assertTrue(round.finished)
        assertEquals(Score(15, of = 16), round.score)
    }

    @Test
    fun `moving on waits for an answer`() {
        val round = Player().completed("d1").practice().verbRound("być")

        assertEquals(round, round.next())
    }

    @Test
    fun `the table fills row by row and a used tile cannot be used again`() {
        var round = Player().completed("d1").practice().verbRound("być")
        repeat(6) { round = round.answerAndMoveOn() }

        assertEquals(1, round.table.count { it.state == VerbRowState.ASKED })
        assertEquals(5, round.table.count { it.state == VerbRowState.EMPTY })
        val first = checkNotNull(round.question)
        val tile = round.rightOption()
        round = round.choose(tile)
        assertEquals(VerbTableRow(first.person, first.answers.first(), VerbRowState.RIGHT), round.table.single { it.person == first.person })
        round = round.next()
        assertTrue(round.tilesUsed[tile])
        assertEquals(round, round.choose(tile))

        // A wrong tile fills the row with the right form, marked as missed, and uses that form's tile.
        val second = checkNotNull(round.question)
        round = round.choose(round.wrongOption())
        assertEquals(VerbTableRow(second.person, second.answers.first(), VerbRowState.MISSED), round.table.single { it.person == second.person })
        round = round.next()
        assertEquals(2, round.tilesUsed.count { it })
        assertEquals(byc.forms.values.toSet(), round.playOut().run.questions.filter { it.stage == VerbStage.TABLE }.flatMap { it.options }.toSet())
    }

    @Test
    fun `each question is a step of its own, but the table is one step while its rows are filled`() {
        var round = Player().completed("d1").practice().verbRound("być")
        val steps = mutableListOf<Pair<VerbStage, Int>>()
        while (!round.finished) {
            steps += checkNotNull(round.question).stage to round.step
            round = round.answerAndMoveOn()
        }
        fun stepsOf(stage: VerbStage) = steps.filter { it.first == stage }.map { it.second }
        assertEquals(6, stepsOf(VerbStage.PICK).distinct().size)
        assertEquals(1, stepsOf(VerbStage.TABLE).distinct().size)
        assertEquals(steps.map { it.second }.distinct().size, steps.count { it.first != VerbStage.TABLE } + 1)
    }

    @Test
    fun `at the typing level the form is typed, and the table still takes tiles`() {
        var round = Player().completed("d1").practice().verbRound("być", level = "type")

        assertTrue(round.typing)
        round = round.type("  " + checkNotNull(round.question).answers.first().uppercase() + " ")
        assertEquals(true, round.given?.correct)
        round = round.next().type("nie wiem")
        assertEquals(false, round.given?.correct)
        round = round.next()
        repeat(4) { round = round.answerAndMoveOn() }

        assertEquals(VerbStage.TABLE, round.question?.stage)
        assertFalse(round.typing)
    }

    @Test
    fun `a typed form that is right apart from its accents is taken as right`() {
        var round = Player().completed("d1").practice().verbRound("być", level = "type")
        while (round.question?.answers != listOf("są")) round = round.answerAndMoveOn()

        assertEquals(true, round.type("sa").given?.correct)
    }

    @Test
    fun `the next Round does not open with the same question, and asks what was not asked today first`() {
        val practice = Player().completed("d1").practice()

        val first = practice.verbRound("być")
        val second = practice.verbRound("być")

        assertNotEquals(first.run.questions.first().person, second.run.questions.first().person)
        // Five sentences, four a Round: the one left out of the first Round is in the second.
        val firstSentences = first.run.questions.mapNotNull { it.sentence }.toSet()
        val secondSentences = second.run.questions.mapNotNull { it.sentence }.toSet()
        assertEquals(5, (firstSentences + secondSentences).size)
    }

    @Test
    fun `the same seed gives the same Round`() {
        fun round() = Player(seed = 7).completed("d1", "d2").practice().verbRound("mixed")

        assertEquals(round(), round())
        assertNotEquals(round().run.questions, Player(seed = 8).completed("d1", "d2").practice().verbRound("mixed").run.questions)
    }

    @Test
    fun `draw takes what has not been seen first`() {
        val drawn = Random(1).draw(listOf("a", "b", "c", "d"), 2, seen = setOf("a", "b"), avoidFirst = null) { it }

        assertEquals(setOf("c", "d"), drawn.toSet())
    }
}
