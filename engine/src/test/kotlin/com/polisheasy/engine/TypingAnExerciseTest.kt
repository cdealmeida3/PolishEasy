package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class TypingAnExerciseTest {
    @Test
    fun `a typing Exercise is shown with its prompt in the Base Language`() {
        val engine = newLearnerEngineWithTypingDay("dziękuję")

        val step = engine.startDailySet()

        assertEquals(
            Step.ShowExercise(
                ExerciseView.TypeAnswer(prompt = "thank you"),
                SetProgress(correct = 0, total = 1),
                exerciseId = "a1-u01-d01-e01",
            ),
            step,
        )
    }

    @Test
    fun `typing an Accepted Answer exactly is correct`() {
        assertEquals(Feedback(correct = true, correctAnswer = "dziękuję"), check("dziękuję", "dziękuję"))
    }

    @Test
    fun `any one of several Accepted Answers is correct, and the one matched is shown`() {
        val accepted = arrayOf("dziękuję", "dzięki")

        assertEquals(Feedback(correct = true, correctAnswer = "dziękuję"), check("dziękuję", *accepted))
        assertEquals(Feedback(correct = true, correctAnswer = "dzięki"), check("dzięki", *accepted))
    }

    @Test
    fun `capital letters are ignored`() {
        assertEach(
            Typed("Dziękuję", "dziękuję") to correct("dziękuję"),
            Typed("DZIĘKUJĘ", "dziękuję") to correct("dziękuję"),
            Typed("ŁÓDŹ", "Łódź") to correct("Łódź"),
            Typed("łódź", "Łódź") to correct("Łódź"),
        )
    }

    @Test
    fun `punctuation is ignored`() {
        assertEach(
            Typed("dziękuję!", "dziękuję") to correct("dziękuję"),
            Typed("dziękuję.", "dziękuję") to correct("dziękuję"),
            Typed("tak, proszę", "tak proszę") to correct("tak proszę"),
            Typed("tak,proszę", "tak proszę") to correct("tak proszę"),
            Typed("jak się masz", "jak się masz?") to correct("jak się masz?"),
            Typed("jak się masz?", "jak się masz") to correct("jak się masz"),
        )
    }

    @Test
    fun `surrounding and repeated whitespace is ignored`() {
        assertEach(
            Typed("  dziękuję  ", "dziękuję") to correct("dziękuję"),
            Typed("dzień   dobry", "dzień dobry") to correct("dzień dobry"),
            Typed("\tdzień\tdobry", "dzień dobry") to correct("dzień dobry"),
            Typed("dzień dobry\n", "dzień dobry") to correct("dzień dobry"),
        )
    }

    @Test
    fun `an answer right apart from its accents is accepted with a warning showing the proper spelling`() {
        assertEach(
            Typed("dziekuje", "dziękuję") to accentWarned("dziękuję"),
            Typed("czesc", "cześć") to accentWarned("cześć"),
            Typed("prosze", "proszę") to accentWarned("proszę"),
            Typed("dzien dobry", "dzień dobry") to accentWarned("dzień dobry"),
            Typed("lodz", "Łódź") to accentWarned("Łódź"),
            // One per Polish letter.
            Typed("sa", "są") to accentWarned("są"),
            Typed("byc", "być") to accentWarned("być"),
            Typed("sie", "się") to accentWarned("się"),
            Typed("maly", "mały") to accentWarned("mały"),
            Typed("kon", "koń") to accentWarned("koń"),
            Typed("gora", "góra") to accentWarned("góra"),
            Typed("sroda", "środa") to accentWarned("środa"),
            Typed("zle", "źle") to accentWarned("źle"),
            Typed("ze", "że") to accentWarned("że"),
            // A wrong accent, not only a missing one.
            Typed("źe", "że") to accentWarned("że"),
            Typed("dziękuje", "dziękuję") to accentWarned("dziękuję"),
            Typed("DZIEKUJE!", "dziękuję") to accentWarned("dziękuję"),
        )
    }

    @Test
    fun `the accent warning shows the Accepted Answer that was matched, not the first`() {
        assertEquals(accentWarned("dzięki"), check("dzieki", "dziękuję", "dzięki"))
    }

    @Test
    fun `an exact match wins over an accent match`() {
        assertEquals(correct("zadanie"), check("zadanie", "żądanie", "zadanie"))
        assertEquals(correct("zadanie"), check("zadanie", "zadanie", "żądanie"))
    }

    @Test
    fun `a letter typed as a base letter plus a combining accent equals the precomposed letter`() {
        val combining = "dziękuję"

        assertEquals(correct("dziękuję"), check(combining, "dziękuję"))
    }

    @Test
    fun `any other difference is wrong and shows the first Accepted Answer`() {
        val wrong = Feedback(correct = false, correctAnswer = "dziękuję")
        assertEach(
            Typed("dziękuj", "dziękuję", "dzięki") to wrong,
            Typed("dziękuja", "dziękuję", "dzięki") to wrong,
            Typed("dzięukję", "dziękuję", "dzięki") to wrong,
            Typed("dziękujęę", "dziękuję", "dzięki") to wrong,
            Typed("dzię kuję", "dziękuję", "dzięki") to wrong,
            Typed("thank you", "dziękuję", "dzięki") to wrong,
            Typed("", "dziękuję", "dzięki") to wrong,
            Typed("   ", "dziękuję", "dzięki") to wrong,
            Typed("!", "dziękuję", "dzięki") to wrong,
        )
        assertEquals(
            Feedback(correct = false, correctAnswer = "dzień dobry"),
            check("dziendobry", "dzień dobry"),
        )
    }

    @Test
    fun `an answer accepted with the accent warning counts as correct on the first attempt`() {
        val engine = newLearnerEngineWithTypingDay("dziękuję")
        engine.startDailySet()

        engine.answer(Answer.Typed("dziekuje"))

        assertEquals(Step.ShowSummary(correctAnswers = 1, exercises = 1), engine.next())
    }

    @Test
    fun `a wrongly typed Exercise returns until it is typed correctly`() {
        val engine = newLearnerEngineWithTypingDay("dziękuję")
        val exercise = engine.startDailySet()

        engine.answer(Answer.Typed("dzięki"))
        assertEquals(exercise, engine.next())
        engine.answer(Answer.Typed("dziękuję"))

        assertEquals(Step.ShowSummary(correctAnswers = 0, exercises = 1), engine.next())
    }

    private class Typed(val text: String, vararg val accepted: String)

    private fun correct(answer: String) = Feedback(correct = true, correctAnswer = answer)

    private fun accentWarned(answer: String) = Feedback(correct = true, correctAnswer = answer, accentWarning = true)

    private fun assertEach(vararg cases: Pair<Typed, Feedback>) = cases.forEach { (typed, expected) ->
        assertEquals(
            expected,
            check(typed.text, *typed.accepted),
            "Typed \"${typed.text}\" for ${typed.accepted.toList()}",
        )
    }

    /** The feedback on [typed] for a typing Exercise with these Accepted Answers. */
    private fun check(typed: String, vararg accepted: String): Feedback {
        val engine = newLearnerEngineWithTypingDay(*accepted)
        engine.startDailySet()
        return engine.answer(Answer.Typed(typed))
    }

    /** A new Learner whose first Daily Set is one typing Exercise, "thank you", with these Accepted Answers. */
    private fun newLearnerEngineWithTypingDay(vararg accepted: String): CourseEngine =
        CourseEngine(FixtureCourse.loadWithDayOne(typingDay(*accepted)), InMemoryLearnerProgress(), FakeClock(), oneADay = { true })
}

/** Day 1 as a single typing Exercise, "thank you", with these Accepted Answers. */
fun typingDay(vararg accepted: String): String {
    val answers = accepted.joinToString { "\"$it\"" }
    return """
        {
          "id": "a1-u01-d01",
          "exercises": [
            { "type": "type_answer", "id": "a1-u01-d01-e01", "prompt": { "en": "thank you" }, "acceptedAnswers": [$answers] }
          ]
        }
    """
}
