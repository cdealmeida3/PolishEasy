package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NumbersTest {
    private val names = listOf("zero", "jeden", "dwa", "trzy", "cztery", "pięć", "sześć", "siedem", "osiem", "dziewięć", "dziesięć", "jedenaście", "dwanaście")

    /** n1 teaches 0 to 3, n2 "plus" and 4 to 6, n3 7 to 12 and a word that is no number. */
    private val course = Course(
        listOf(
            levelOf(
                "a1",
                day("n1", *names.take(4).map { choose("e-$it", it, it) }.toTypedArray()),
                day("n2", *(names.subList(4, 7) + "plus").map { choose("e-$it", it, it) }.toTypedArray()),
                day("n3", *(names.drop(7) + "tak").map { choose("e-$it", it, it) }.toTypedArray()),
            ),
        ),
        (names + "plus" + "tak").map { item(it) },
        clips = (names + "plus" + "tak").map { clip(it) },
        pictures = names.withIndex().associate { (number, name) -> name to Picture.Glyph("$number") } +
            mapOf("plus" to Picture.Glyph("+"), "tak" to Picture.Image("pictures/tak.webp")),
    )

    private fun player(course: Course = this.course) = Player(course = course)

    private fun Practice.numbers(vararg wanted: String, sound: Boolean = true): NumberRound =
        start(setup(Game.NUMBERS, wanted.toList(), sound), sound) as NumberRound

    private fun levels(practice: Practice, clue: String = Numbers.READ) =
        practice.setup(Game.NUMBERS, listOf(clue)).choices[1].options.map { it.id }

    private fun NumberRound.type(digits: String): NumberRound = digits.fold(this) { round, digit -> round.press(digit.digitToInt()) }

    private fun NumberRound.playOut(wrong: Int = 0): NumberRound {
        var round = this
        var missed = 0
        while (!round.finished) {
            val answer = checkNotNull(round.question).answer
            round = round.type(if (missed++ < wrong) (if (answer == "1") "2" else "1") else answer).check().next()
        }
        return round
    }

    @Test
    fun `Numbers opens with four learned numbers, and plays with sound off`() {
        val few = course.copy(pictures = course.pictures - "zero")
        assertEquals(GameState.NEEDS_MORE, player(few).completed("n1").practice().view().games.single { it.game == Game.NUMBERS }.state)
        assertEquals(GameState.READY, player().completed("n1").practice().view().games.single { it.game == Game.NUMBERS }.state)
        assertEquals(GameState.READY, player().completed("n1").practice().view(sound = false).games.single { it.game == Game.NUMBERS }.state)
    }

    @Test
    fun `a number is heard with sound on, or read, and only read with sound off`() {
        val practice = player().completed("n1").practice()

        assertEquals(listOf(Numbers.LISTEN, Numbers.READ), practice.setup(Game.NUMBERS).choices[0].options.map { it.id })
        assertEquals(listOf(Numbers.READ), practice.setup(Game.NUMBERS, sound = false).choices[0].options.map { it.id })
        assertTrue(practice.numbers(Numbers.LISTEN).listen)
        assertFalse(practice.numbers(sound = false).listen)
    }

    @Test
    fun `a level is offered once enough has been learned for it`() {
        // Four numbers, all of them digits: single numbers and runs.
        assertEquals(listOf(Numbers.TO_10, Numbers.RUNS), levels(player().completed("n1").practice()))
        // With "plus" there are sums of what has been learned: 1 + 1 to 3 + 3.
        assertEquals(listOf(Numbers.TO_10, Numbers.RUNS, Numbers.SUMS), levels(player().completed("n1", "n2").practice()))
        // A number above ten opens the level to twenty.
        assertEquals(listOf(Numbers.TO_10, Numbers.TO_20, Numbers.RUNS, Numbers.SUMS), levels(player().completed("n1", "n2", "n3").practice()))
    }

    @Test
    fun `a number that cannot be heard is not asked by ear`() {
        val quiet = course.copy(clips = course.clips.filter { it.text != "plus" && it.text != "trzy" })
        val practice = player(quiet).completed("n1", "n2").practice()

        assertEquals(listOf(Numbers.TO_10, Numbers.RUNS), levels(practice, Numbers.LISTEN))
        assertFalse("3" in practice.numbers(Numbers.LISTEN, Numbers.TO_10).asked)
        assertTrue("3" in practice.numbers(Numbers.READ, Numbers.TO_10).asked)
    }

    @Test
    fun `single numbers are asked once each, and only those learned`() {
        val early = player().completed("n1").practice().numbers(Numbers.READ, Numbers.TO_10)
        assertEquals(setOf("0", "1", "2", "3"), early.asked.toSet())
        assertEquals(4, early.total)

        val later = player().completed("n1", "n2", "n3").practice()
        val small = later.numbers(Numbers.READ, Numbers.TO_10)
        assertEquals(Numbers.QUESTIONS, small.total)
        assertTrue(small.run.questions.all { it.answer.toInt() <= 10 && it.words == listOf(names[it.answer.toInt()]) })
        val all = later.numbers(Numbers.READ, Numbers.TO_20)
        assertEquals(all.asked.size, all.asked.toSet().size)
        assertTrue(all.run.questions.all { it.numerals == it.answer && it.kind == NumberKind.SINGLE })
    }

    /** The course with a Daily Set n4 after n3 that teaches [tens], each the numeral of its ten from twenty up. */
    private fun withTens(vararg tens: String): Course = course.copy(
        levels = course.levels.map { level ->
            level.copy(units = level.units.map { unit -> unit.copy(dailySets = unit.dailySets + day("n4", *tens.map { choose("e-$it", it, it) }.toTypedArray())) })
        },
        items = course.items + tens.map { item(it) },
        clips = course.clips + tens.map { clip(it) },
        pictures = course.pictures + tens.withIndex().associate { (index, ten) -> ten to Picture.Glyph("${20 + 10 * index}") },
    )

    @Test
    fun `numbers to a hundred are the learned tens, and a ten and a learned unit said one after the other`() {
        // Twenty alone is no number above twenty.
        assertFalse(Numbers.TO_100 in levels(player(withTens("dwadzieścia")).completed("n1", "n2", "n3", "n4").practice()))

        val tens = listOf("dwadzieścia", "trzydzieści", "czterdzieści")
        val practice = player(withTens(*tens.toTypedArray())).completed("n1", "n2", "n3", "n4").practice()
        assertEquals(listOf(Numbers.TO_10, Numbers.TO_20, Numbers.TO_100, Numbers.RUNS, Numbers.SUMS), levels(practice))
        assertTrue(practice.numbers(Numbers.READ, Numbers.TO_20).run.questions.all { it.answer.toInt() <= 20 })

        val round = practice.numbers(Numbers.READ, Numbers.TO_100)
        assertEquals(Numbers.QUESTIONS, round.total)
        assertEquals(round.asked.size, round.asked.toSet().size)
        round.run.questions.forEach { question ->
            val number = question.answer.toInt()
            assertTrue(number in 20..49, question.toString())
            val unit = number % 10
            assertEquals(listOf(tens[number / 10 - 2]) + if (unit == 0) emptyList() else listOf(names[unit]), question.words)
            assertEquals("$number", question.numerals)
        }
        assertEquals(Numbers.QUESTIONS, round.playOut().score.value)
    }

    @Test
    fun `a run is three learned digits said in a row, typed as they are`() {
        val round = player().completed("n1").practice().numbers(Numbers.READ, Numbers.RUNS)

        assertEquals(Numbers.RUN_QUESTIONS, round.total)
        assertEquals(round.asked.size, round.asked.toSet().size)
        round.run.questions.forEach { question ->
            assertEquals(3, question.words.size)
            assertEquals(question.words.map { names.indexOf(it) }.joinToString(""), question.answer)
            assertTrue(question.answer.all { it in "0123" }, question.answer)
            assertTrue(question.answer.toSet().size > 1, "${question.answer} is one digit three times")
            assertEquals(question.answer.toList().joinToString(" "), question.numerals)
            assertEquals(NumberKind.RUN, question.kind)
        }
    }

    @Test
    fun `a run that starts with zero must be typed with its zero`() {
        val round = (1..40).firstNotNullOf { seed ->
            Player(course = course, seed = seed).completed("n1").practice().numbers(Numbers.READ, Numbers.RUNS)
                .takeIf { it.question?.answer?.startsWith("0") == true }
        }
        val answer = checkNotNull(round.question).answer

        assertEquals(Given(answer.drop(1), false), round.type(answer.drop(1)).check().given)
        assertEquals(Given(answer, true), round.type(answer).check().given)
    }

    @Test
    fun `a sum says two learned numbers with plus between them, and its answer is a learned number`() {
        val round = player().completed("n1", "n2").practice().numbers(Numbers.READ, Numbers.SUMS)

        assertEquals(Numbers.SUM_QUESTIONS, round.total)
        round.run.questions.forEach { question ->
            val (a, plus, b) = question.words
            assertEquals("plus", plus)
            val sum = names.indexOf(a) + names.indexOf(b)
            assertTrue(names.indexOf(a) in 1..6 && names.indexOf(b) in 1..6 && sum <= 6, question.numerals)
            assertEquals("$sum", question.answer)
            assertEquals("${names.indexOf(a)} + ${names.indexOf(b)} = $sum", question.numerals)
            assertEquals("$a plus $b", question.polish)
            assertEquals(NumberKind.SUM, question.kind)
        }
    }

    @Test
    fun `digits are typed and taken back, up to three, and nothing is checked until something is typed`() {
        val start = player().completed("n1").practice().numbers(Numbers.READ, Numbers.TO_10)

        assertFalse(start.canCheck)
        assertEquals(start, start.check())
        assertEquals(start, start.erase())
        assertEquals(start, start.press(10))
        assertEquals("12", start.type("12").typed)
        assertEquals("1", start.type("12").erase().typed)
        assertEquals("123", start.type("12345").typed)
        assertTrue(start.type("1").canCheck)
    }

    @Test
    fun `what is typed answers the question, right or wrong, once`() {
        val start = player().completed("n1").practice().numbers(Numbers.READ, Numbers.TO_10)
        val answer = checkNotNull(start.question).answer
        val other = if (answer == "1") "2" else "1"

        val right = start.type(answer).check()
        assertEquals(Given(answer, true), right.given)
        // A zero in front does not make a number wrong.
        assertEquals(true, start.type("0$answer").check().given?.correct)
        val wrong = start.type(other).check()
        assertEquals(Given(other, false), wrong.given)
        // Answered, the keypad does nothing more.
        assertEquals(wrong, wrong.press(1))
        assertEquals(wrong, wrong.erase())
        assertEquals(wrong, wrong.check())

        val next = wrong.next()
        assertEquals("", next.typed)
        assertEquals(1, next.step)
        assertEquals(start, start.next())
    }

    @Test
    fun `the result is the answers right first try, and the most is the Best`() {
        val player = player().completed("n1")
        val practice = player.practice()

        val first = practice.finish(practice.numbers(Numbers.READ, Numbers.TO_10).playOut(wrong = 1))
        assertEquals(Score(3, of = 4), first.score)
        assertTrue(first.first)
        val second = practice.finish(practice.numbers(Numbers.READ, Numbers.TO_10).playOut())
        assertEquals(Score(4, of = 4), second.score)
        assertTrue(second.newBest)
        // Each way of playing has its own Best.
        assertEquals(null, practice.setup(Game.NUMBERS, listOf(Numbers.READ, Numbers.RUNS)).best)
        assertEquals(4, player.practice().setup(Game.NUMBERS, listOf(Numbers.READ, Numbers.TO_10)).best)
    }
}
