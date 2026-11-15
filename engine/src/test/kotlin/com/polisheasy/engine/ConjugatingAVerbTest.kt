package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class ConjugatingAVerbTest {
    private val oneOfOne = SetProgress(correct = 0, total = 1)

    @Test
    fun `a conjugation Exercise is shown with the verb's dictionary form, its meaning and the person, and no sentence`() {
        val engine = engineWith(conjugationDay(listOf("jesteśmy")))

        assertEquals(
            Step.ShowExercise(ExerciseView.Conjugation(verb = "być", meaning = "to be", person = Person.WE), oneOfOne),
            engine.startDailySet(),
        )
    }

    @Test
    fun `with a sentence, it is shown with the text before and after the gap and the sentence's meaning`() {
        fun shown(sentence: String, meaning: String): GapSentence? {
            val day = conjugationDay(listOf("jesteśmy"), sentence = sentence, sentenceMeaning = meaning)
            val step = engineWith(day).startDailySet() as Step.ShowExercise
            return (step.exercise as ExerciseView.Conjugation).sentence
        }

        assertEquals(
            GapSentence(before = "My ", after = " w Polsce.", meaning = "We are in Poland."),
            shown("My ___ w Polsce.", "We are in Poland."),
        )
        assertEquals(
            GapSentence(before = "", after = " w Polsce.", meaning = "We are in Poland."),
            shown("___ w Polsce.", "We are in Poland."),
        )
        assertEquals(
            GapSentence(before = "W Polsce ", after = "", meaning = "In Poland we are"),
            shown("W Polsce ___", "In Poland we are"),
        )
    }

    @Test
    fun `each of the six persons written in content is shown as its person, with its Polish pronouns`() {
        val written = mapOf(
            "ja" to Person.I,
            "ty" to Person.YOU,
            "on" to Person.HE_SHE_IT,
            "my" to Person.WE,
            "wy" to Person.YOU_PLURAL,
            "oni" to Person.THEY,
        )

        written.forEach { (pronoun, person) ->
            val step = engineWith(conjugationDay(listOf("jest"), person = pronoun)).startDailySet() as Step.ShowExercise
            assertEquals(person, (step.exercise as ExerciseView.Conjugation).person, "Person \"$pronoun\"")
        }
        assertEquals(written.values.toList(), Person.entries)
        assertEquals(
            listOf("ja", "ty", "on / ona / ono", "my", "wy", "oni / one"),
            Person.entries.map { it.pronouns },
        )
    }

    @Test
    fun `typing the form exactly is correct, with or without a sentence`() {
        val withSentence = conjugationDay(
            listOf("jesteśmy"),
            sentence = "My ___ w Polsce.",
            sentenceMeaning = "We are in Poland.",
        )

        assertEquals(correct("jesteśmy"), check("jesteśmy", conjugationDay(listOf("jesteśmy"))))
        assertEquals(correct("jesteśmy"), check("jesteśmy", withSentence))
    }

    @Test
    fun `capitals, punctuation and extra whitespace are ignored`() {
        listOf("Jesteśmy", "JESTEŚMY", "jesteśmy.", "  jesteśmy ", "jesteśmy!\n").forEach { typed ->
            assertEquals(correct("jesteśmy"), check(typed, conjugationDay(listOf("jesteśmy"))), "Typed \"$typed\"")
        }
    }

    @Test
    fun `a form right apart from its accents is correct with the accent warning and shows the proper spelling`() {
        assertEquals(
            Feedback(correct = true, correctAnswer = "jesteśmy", accentWarning = true),
            check("jestesmy", conjugationDay(listOf("jesteśmy"))),
        )
        assertEquals(
            Feedback(correct = true, correctAnswer = "są", accentWarning = true),
            check("sa", conjugationDay(listOf("są"), person = "oni")),
        )
    }

    @Test
    fun `another form of the verb, a misspelling and nothing typed are wrong and show the first Accepted Answer`() {
        val day = conjugationDay(listOf("jesteśmy", "my jesteśmy"))

        listOf("jesteście", "jestesmi", "", "być").forEach { typed ->
            assertEquals(Feedback(correct = false, correctAnswer = "jesteśmy"), check(typed, day), "Typed \"$typed\"")
        }
        // The pronoun with the form is wrong unless the author lists it.
        assertEquals(
            Feedback(correct = false, correctAnswer = "jesteśmy"),
            check("my jesteśmy", conjugationDay(listOf("jesteśmy"))),
        )
    }

    @Test
    fun `any one of several Accepted Answers is correct, and the one matched is shown`() {
        val day = conjugationDay(listOf("będę mówić", "będę mówił"), person = "ja")

        assertEquals(correct("będę mówić"), check("będę mówić", day))
        assertEquals(correct("będę mówił"), check("będę mówił", day))
    }

    @Test
    fun `a wrongly answered conjugation Exercise returns until it is answered correctly`() {
        val engine = engineWith(conjugationDay(listOf("jesteśmy")))
        val exercise = engine.startDailySet()

        engine.answer(Answer.Typed("jesteście"))
        assertEquals(exercise, engine.next())
        engine.answer(Answer.Typed("jesteśmy"))

        assertEquals(Step.ShowSummary(correctAnswers = 0, exercises = 1), engine.next())
    }

    @Test
    fun `a Tip with a conjugation table is shown with the verb, its meaning and the six forms in person order`() {
        val shuffled = linkedMapOf(
            "oni" to "są",
            "my" to "jesteśmy",
            "ja" to "jestem",
            "wy" to "jesteście",
            "on" to "jest",
            "ty" to "jesteś",
        )
        val engine = engineWith(conjugationDay(listOf("jesteśmy"), tipTable = conjugationTable(forms = shuffled)))

        assertEquals(
            Step.ShowTip(
                TipView(
                    title = "The verb być",
                    body = "Być is irregular.",
                    conjugation = ConjugationTableView(
                        verb = "być",
                        meaning = "to be",
                        rows = listOf(
                            ConjugationRow(Person.I, "jestem"),
                            ConjugationRow(Person.YOU, "jesteś"),
                            ConjugationRow(Person.HE_SHE_IT, "jest"),
                            ConjugationRow(Person.WE, "jesteśmy"),
                            ConjugationRow(Person.YOU_PLURAL, "jesteście"),
                            ConjugationRow(Person.THEY, "są"),
                        ),
                    ),
                ),
                oneOfOne,
            ),
            engine.startDailySet(),
        )
    }

    @Test
    fun `a Tip without a conjugation table is shown without one`() {
        val step = CourseEngine(FixtureCourse.load(), InMemoryLearnerProgress(), FakeClock()).startDailySet()

        assertEquals(null, (step as Step.ShowTip).tip.conjugation)
    }

    private fun correct(answer: String) = Feedback(correct = true, correctAnswer = answer)

    /** The feedback on [typed] for the conjugation Exercise of [day]. */
    private fun check(typed: String, day: String): Feedback {
        val engine = engineWith(day)
        engine.startDailySet()
        return engine.answer(Answer.Typed(typed))
    }

    /** A new Learner whose first Daily Set is [day]. */
    private fun engineWith(day: String): CourseEngine =
        CourseEngine(FixtureCourse.loadWithDayOne(day), InMemoryLearnerProgress(), FakeClock())
}

/** The six forms of być, by person as written in content. */
val formsOfByc: Map<String, String> = linkedMapOf(
    "ja" to "jestem",
    "ty" to "jesteś",
    "on" to "jest",
    "my" to "jesteśmy",
    "wy" to "jesteście",
    "oni" to "są",
)

/** A Tip's conjugation table for być, "to be", as content JSON. */
fun conjugationTable(verb: String = "być", forms: Map<String, String> = formsOfByc): String {
    val formsJson = forms.entries.joinToString { (person, form) -> "\"$person\": \"$form\"" }
    return """{ "verb": "$verb", "meaning": { "en": "to be" }, "forms": { $formsJson } }"""
}

/**
 * Day 1 as a single conjugation Exercise for być, "to be", with these Accepted Answers. With
 * [tipTable], a Tip carrying that conjugation table comes before it.
 */
fun conjugationDay(
    accepted: List<String>,
    person: String = "my",
    sentence: String? = null,
    sentenceMeaning: String? = null,
    tipTable: String? = null,
    verb: String = "być",
): String {
    val answers = accepted.joinToString { "\"$it\"" }
    val sentenceJson = sentence?.let { "\"sentence\": \"$it\"," }.orEmpty()
    val sentenceMeaningJson = sentenceMeaning?.let { "\"sentenceMeaning\": { \"en\": \"$it\" }," }.orEmpty()
    val tips = tipTable?.let {
        """
          "tips": [
            { "id": "a1-u01-d01-t01", "before": "a1-u01-d01-e01", "title": { "en": "The verb być" },
              "body": { "en": "Być is irregular." }, "conjugation": $it }
          ],
        """
    }.orEmpty()
    return """
        {
          "id": "a1-u01-d01",
          $tips
          "exercises": [
            { "type": "conjugation", "id": "a1-u01-d01-e01", "verb": "$verb", "meaning": { "en": "to be" },
              "person": "$person", $sentenceJson $sentenceMeaningJson "acceptedAnswers": [$answers] }
          ]
        }
    """
}
