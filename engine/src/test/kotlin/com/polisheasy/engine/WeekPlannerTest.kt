package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeekPlannerTest {
    private val dayNames = listOf("poniedziałek", "wtorek", "środa", "czwartek", "piątek", "sobota", "niedziela")
    private val days = dayNames.mapIndexed { index, name -> item("d${index + 1}", name) }
    private val words = listOf(item("o"), item("osma", "ósma"), item("impreza"), item("lekcja"), item("kino"))

    private fun plan(day: Int, hour: Int, minute: Int, what: String) = WeekPlan(
        "Day $day o $hour: $what.", mapOf("en" to "Day $day at $hour: $what"), "d$day", hour, minute, listOf("d$day", "o", "osma", what),
    )

    /** One plan a day, and a second on Monday. */
    private val plans = (1..7).map { plan(it, 7 + it, if (it % 2 == 0) 30 else 0, if (it % 2 == 0) "impreza" else "lekcja") } + plan(1, 20, 0, "kino")

    /** l1 teaches the days, l2 the words of every plan but kino, l3 kino. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("l1", *days.map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l2", *words.take(4).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()),
                day("l3", choose("e-kino", "kino", "kino")),
            ),
        ),
        days + words,
        week = Week(days.map { it.id }, plans),
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun Practice.round(): PlannerRound = start(setup(Game.WEEK_PLANNER, listOf(WeekPlanner.READ))) as PlannerRound

    @Test
    fun `Week planner opens with the days and four plans on four days, read without a Clip`() {
        val state = { p: Player -> p.practice().view().games.single { it.game == Game.WEEK_PLANNER }.state }
        assertEquals(GameState.NEEDS_MORE, state(player()))
        assertEquals(GameState.NEEDS_MORE, state(player().completed("l2")))
        assertEquals(GameState.READY, state(player().completed("l1", "l2")))
        assertEquals(listOf(WeekPlanner.READ), player().completed("l1", "l2").practice().setup(Game.WEEK_PLANNER).choices[0].options.map { it.id })
    }

    @Test
    fun `a Round is four plans on four days, each with four times, its own among them`() {
        repeat(10) { seed ->
            val round = player(seed).completed("l1", "l2", "l3").practice().round()
            assertEquals(dayNames, round.days)
            assertEquals(WeekPlanner.PLANS, round.total)
            assertEquals(WeekPlanner.PLANS, round.run.questions.map { it.day }.toSet().size)
            round.run.questions.forEach { question ->
                val plan = plans.single { it.text == question.text }
                assertEquals(plan.day, "d${question.day + 1}")
                assertEquals(plan.time, question.time)
                assertEquals(plan.what, question.what)
                assertEquals(WeekPlanner.TIMES, question.times.toSet().size)
                assertTrue(question.time in question.times)
                // Never twelve hours away: o ósmej is 8:00 and the diary does not ask whether it is 20:00.
                question.times.forEach { time -> assertTrue(time.substringBefore(":").toInt() in (plan.hour - 1)..(plan.hour + 1), time) }
            }
        }
    }

    @Test
    fun `a day is tapped, then a time, and the plan is written in its day`() {
        var round = player().completed("l1", "l2").practice().round()
        val first = checkNotNull(round.question)
        assertEquals(round, round.pickTime(0))
        round = round.pickDay((first.day + 1) % 7).pickDay(first.day)
        assertEquals(first.day, round.day)
        round = round.pickTime(first.times.indexOf(first.time))
        assertEquals(true, round.given?.correct)
        assertEquals(listOf(first), round.written)
        assertEquals(round, round.pickDay(0))
        round = round.next()
        assertNull(round.day)

        val second = checkNotNull(round.question)
        round = round.pickDay((second.day + 1) % 7).pickTime(second.times.indexOf(second.time))
        assertEquals(false, round.given?.correct)
        round = round.next()
        val third = checkNotNull(round.question)
        round = round.pickDay(third.day).pickTime(third.times.indexOfFirst { it != third.time })
        assertEquals(false, round.given?.correct)
        round = round.next()
        while (!round.finished) {
            val question = checkNotNull(round.question)
            round = round.pickDay(question.day).pickTime(question.times.indexOf(question.time)).next()
        }
        assertEquals(Score(WeekPlanner.PLANS - 2, of = WeekPlanner.PLANS), round.score)
        assertEquals(WeekPlanner.PLANS, round.written.size)
    }

    @Test
    fun `a plan's time and what it is come from its text`() {
        val plan = WeekPlan("W piątek o dwudziestej: impreza.", mapOf("en" to "Friday at eight: a party."), "piatek", 20, 0, listOf("piatek"))
        assertEquals("impreza", plan.what)
        assertEquals("20:00", plan.time)
        assertEquals("8:30", clockTime(8, 30))
    }
}
