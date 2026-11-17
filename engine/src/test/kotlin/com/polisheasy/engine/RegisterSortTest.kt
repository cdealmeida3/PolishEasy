package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegisterSortTest {
    private fun phrase(id: String, register: Register?) = item(id).copy(register = register)

    /**
     * Day r1 teaches four phrases: two formal, two informal. Day r2 two more, one of them for
     * anyone, and a word with no register. Day r3 teaches only formal phrases.
     */
    private val registerCourse = Course(
        listOf(
            levelOf(
                "a1",
                DailySet("r1", exercises = listOf("pan", "pani", "czesc", "pa").mapIndexed { i, id -> choose("r1-e$i", id, id) }),
                DailySet("r2", exercises = listOf("dziekuje", "nara", "kot").mapIndexed { i, id -> choose("r2-e$i", id, id) }),
                DailySet("r3", exercises = listOf("sz1", "sz2", "sz3", "sz4", "sz5", "sz6").mapIndexed { i, id -> choose("r3-e$i", id, id) }),
            ),
        ),
        listOf(
            phrase("pan", Register.FORMAL),
            phrase("pani", Register.FORMAL),
            phrase("czesc", Register.INFORMAL),
            phrase("pa", Register.INFORMAL),
            phrase("dziekuje", Register.UNIVERSAL),
            phrase("nara", Register.INFORMAL),
            phrase("kot", null),
        ) + (1..6).map { phrase("sz$it", Register.FORMAL) },
    )

    private fun player() = Player(course = registerCourse)

    private fun stateOf(player: Player) = player.practice().view().games.single { it.game == Game.REGISTER_SORT }.state

    private fun Practice.sort(vararg wanted: String): RegisterRound = start(setup(Game.REGISTER_SORT, wanted.toList())) as RegisterRound

    private fun RegisterRound.answerRightly(): RegisterRound = answer(checkNotNull(card).register)

    private fun RegisterRound.answerWrongly(): RegisterRound = answer(registers.first { it != checkNotNull(card).register })

    @Test
    fun `Register sort opens with six learned phrases the book marks, of two registers at least`() {
        assertEquals(GameState.NEEDS_MORE, stateOf(player().completed("r1")))
        assertEquals(GameState.READY, stateOf(player().completed("r1", "r2")))
        // Six phrases, all formal: nothing to tell apart.
        assertEquals(GameState.NEEDS_MORE, stateOf(player().completed("r3")))
    }

    @Test
    fun `it is played for 60 seconds or 120, over only the phrases that have a register`() {
        val practice = player().completed("r1", "r2").practice()
        assertEquals(listOf("seconds-60", "seconds-120"), practice.setup(Game.REGISTER_SORT).choices.single().options.map { it.id })

        val round = practice.sort("seconds-120")
        assertEquals(120, round.seconds)
        assertEquals(setOf("pan", "pani", "czesc", "pa", "dziekuje", "nara"), round.cards.map { it.itemId }.toSet())
        round.cards.forEach { card -> assertEquals(registerCourse.itemsById.getValue(card.itemId).register, card.register) }
        assertEquals(240, round.cards.size)
    }

    @Test
    fun `the registers offered are those of the learned phrases, in the book's order`() {
        assertEquals(listOf(Register.FORMAL, Register.INFORMAL, Register.UNIVERSAL), player().completed("r1", "r2").practice().sort().registers)
        assertEquals(listOf(Register.FORMAL, Register.INFORMAL), player().completed("r1", "r3").practice().sort().registers)
    }

    @Test
    fun `every phrase comes round before any comes again, and none twice running`() {
        val phrases = player().completed("r1", "r2").practice().sort().cards.map { it.itemId }
        assertEquals(6, phrases.take(6).toSet().size)
        assertEquals(6, phrases.subList(6, 12).toSet().size)
        assertTrue(phrases.zipWithNext().none { (one, next) -> one == next })
    }

    @Test
    fun `a right answer brings the next phrase at once and counts under its register`() {
        val start = player().completed("r1", "r2").practice().sort()
        val first = checkNotNull(start.card)
        val after = start.answerRightly()

        assertEquals(1, after.index)
        assertEquals(1, after.right)
        assertEquals(1, after.sorted[first.register])
        assertEquals(null, after.missed)
    }

    @Test
    fun `a wrong answer stays, with the register given, until the Learner moves on, while the clock runs`() {
        val start = player().completed("r1", "r2").practice().sort()
        val missed = start.answerWrongly()

        assertEquals(0, missed.index)
        assertTrue(missed.missed != null && missed.missed != checkNotNull(start.card).register)
        // Nothing else can be answered meanwhile, and the clock still runs.
        assertEquals(missed, missed.answerRightly())
        assertEquals(59, missed.tick().left)
        val moved = missed.next()
        assertEquals(1, moved.index)
        assertEquals(0, moved.right)
        assertEquals(null, moved.missed)
        // A register that is not offered is not an answer.
        val formalOnly = player().completed("r1", "r3").practice().sort()
        assertEquals(formalOnly, formalOnly.answer(Register.UNIVERSAL))
    }

    @Test
    fun `the Round ends when the time is up, and its result is the right answers`() {
        var round = player().completed("r1", "r2").practice().sort()
        repeat(5) { round = round.answerRightly() }
        round = round.answerWrongly().next()
        repeat(60) { round = round.tick() }

        assertTrue(round.finished)
        assertEquals(null, round.card)
        assertEquals(Score(5), round.score)
        assertEquals(round, round.tick())
    }

    @Test
    fun `the Best is the most right answers in each time`() {
        val player = player().completed("r1", "r2")
        val practice = player.practice()
        var round = practice.sort("seconds-60")
        repeat(4) { round = round.answerRightly() }
        repeat(60) { round = round.tick() }
        assertEquals(4, practice.finish(round).best)

        var shorter = practice.sort("seconds-60")
        repeat(2) { shorter = shorter.answerRightly() }
        repeat(60) { shorter = shorter.tick() }
        assertEquals(4, practice.finish(shorter).best)
        assertEquals(4, player.practice().setup(Game.REGISTER_SORT, listOf("seconds-60")).best)
        assertEquals(null, player.practice().setup(Game.REGISTER_SORT, listOf("seconds-120")).best)
    }
}
