package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VerbGroupsTest {
    private val pisac = table("pisać", "to write", "piszę", "piszesz", "pisze", "piszemy", "piszecie", "piszą")
    private val znac = table("znać", "to know", "znam", "znasz", "zna", "znamy", "znacie", "znają")
    private val umiec = table("umieć", "to know how to", "umiem", "umiesz", "umie", "umiemy", "umiecie", "umieją")

    /** Days 1 to 3 of the practice course show być, mieć and mówić; day 4 pisać, day 5 znać and day 6 umieć. */
    private val groupsCourse = practiceCourse.copy(
        levels = listOf(
            levelOf(
                "a1",
                *practiceCourse.path.toTypedArray(),
                dayWithTable("d4", pisac, gap("d4-e1", pisac, Person.I, "___ e-mail.")),
                dayWithTable("d5", znac, gap("d5-e1", znac, Person.I, "___ Kraków.")),
                dayWithTable("d6", umiec, gap("d6-e1", umiec, Person.I, "___ tańczyć.")),
            ),
        ),
    )

    private fun player() = Player(course = groupsCourse)

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.VERB_GROUPS }.state

    private fun Practice.verbGroups(show: String = VerbGroups.VERB): VerbGroupRound = start(setup(Game.VERB_GROUPS, listOf(show))) as VerbGroupRound

    @Test
    fun `Three groups opens with four verbs of two conjugations, być following none`() {
        assertEquals(GameState.NEEDS_MORE, stateOf(player().completed("d1", "d2", "d3")))
        assertEquals(GameState.NEEDS_MORE, stateOf(player().completed("d1", "d2", "d3", "d4")))
        assertEquals(GameState.READY, stateOf(player().completed("d1", "d2", "d3", "d4", "d5")))
    }

    @Test
    fun `each verb is asked once, with its conjugation by the book's three groups`() {
        val round = player().completed("d1", "d2", "d3", "d4", "d5", "d6").practice().verbGroups()
        val groups = round.run.questions.associate { it.verb to it.group }
        assertEquals(
            mapOf("mieć" to VerbGroup.M_SZ, "mówić" to VerbGroup.E_ISZ, "pisać" to VerbGroup.E_ESZ, "znać" to VerbGroup.M_SZ, "umieć" to VerbGroup.M_SZ),
            groups,
        )
        assertEquals(VerbGroup.entries, round.groups)
        val pisz = round.run.questions.single { it.verb == "pisać" }
        assertEquals("piszę" to "piszesz", pisz.ja to pisz.ty)
        assertEquals("to write", pisz.meaning)
    }

    @Test
    fun `only the groups of the verbs learned are offered`() {
        val round = player().completed("d1", "d2", "d3", "d5", "d6").practice().verbGroups(VerbGroups.WITH_JA)
        assertTrue(round.withJa)
        assertEquals(listOf(VerbGroup.M_SZ, VerbGroup.E_ISZ), round.groups)
        assertEquals(round, round.choose(VerbGroup.E_ESZ))
    }

    @Test
    fun `a right sort counts, a wrong one is shown, and the result is the answers right first try`() {
        var round = player().completed("d1", "d2", "d3", "d4", "d5", "d6").practice().verbGroups()
        assertEquals(5, round.total)
        val wrong = VerbGroup.entries.first { it != round.question!!.group }
        round = round.choose(wrong)
        assertEquals(false, round.given?.correct)
        assertEquals(wrong, round.chosen)
        assertEquals(round, round.choose(round.question!!.group))
        round = round.next()
        while (!round.finished) round = round.choose(round.question!!.group).next()
        assertNull(round.question)
        assertEquals(Score(4, of = 5), round.score)
    }
}
