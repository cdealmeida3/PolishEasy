package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ListeningTest {
    private val oneOfOne = SetProgress(correct = 0, total = 1)
    private val firstClip = ClipView("audio/c1.ogg")
    private val helloOptions = listOf("cześć", "sześć", "część")
    private val helloDay = listenChooseDay("cześć", listOf("sześć", "część"))
    private val helloClips = clipsFile("cześć")

    @Test
    fun `a listen-and-choose Exercise is shown with its Clip and, as options, the spoken text and the distractors`() {
        val view = listenChooseShown(engineWith(helloDay, helloClips).startDailySet())

        assertEquals(firstClip, view.audio)
        assertEquals(helloOptions.sorted(), view.options.sorted())
    }

    @Test
    fun `the options are not in the authored order, and are the same every time the Exercise is shown`() {
        val first = listenChooseShown(engineWith(helloDay, helloClips).startDailySet()).options
        val again = listenChooseShown(engineWith(helloDay, helloClips).startDailySet()).options

        assertNotEquals(helloOptions, first)
        assertEquals(first, again)
    }

    @Test
    fun `choosing what was said is correct, and the feedback carries the spoken text and its Clip`() {
        val engine = engineWith(helloDay, helloClips)
        val options = listenChooseShown(engine.startDailySet()).options

        assertEquals(
            Feedback(correct = true, correctAnswer = "cześć", audio = firstClip),
            engine.answer(Answer.Option(options.indexOf("cześć"))),
        )
    }

    @Test
    fun `choosing a distractor, or an option that does not exist, is wrong and shows what was said`() {
        val options = listenChooseShown(engineWith(helloDay, helloClips).startDailySet()).options

        listOf(options.indexOf("sześć"), options.indexOf("część"), -1, 3, 99).forEach { index ->
            val engine = engineWith(helloDay, helloClips)
            engine.startDailySet()

            assertEquals(
                Feedback(correct = false, correctAnswer = "cześć", audio = firstClip),
                engine.answer(Answer.Option(index)),
                "Option $index",
            )
        }
    }

    @Test
    fun `a wrongly answered listen-and-choose Exercise returns until it is answered correctly, and counts against the Summary`() {
        val engine = engineWith(helloDay, helloClips)
        val first = engine.startDailySet()
        val options = listenChooseShown(first).options

        engine.answer(Answer.Option(options.indexOf("sześć")))
        assertEquals(first, engine.next())
        engine.answer(Answer.Option(options.indexOf("część")))
        assertEquals(first, engine.next())
        engine.answer(Answer.Option(options.indexOf("cześć")))

        assertEquals(Step.ShowSummary(correctAnswers = 0, exercises = 1), engine.next())
    }

    @Test
    fun `a listen-and-type Exercise is shown with the Clip of its first Accepted Answer and nothing else`() {
        val engine = engineWith(listenTypeDay("dziękuję", "dzięki"), clipsFile("dzięki", "dziękuję"))

        assertEquals(
            Step.ShowExercise(ExerciseView.ListenType(ClipView("audio/c2.ogg")), oneOfOne),
            engine.startDailySet(),
        )
    }

    @Test
    fun `typing what was said is checked like a typing Exercise, and the feedback carries the Clip that was played`() {
        fun correct(answer: String) = Feedback(correct = true, correctAnswer = answer)
        fun accentWarned(answer: String) = Feedback(correct = true, correctAnswer = answer, accentWarning = true)
        fun wrong(answer: String) = Feedback(correct = false, correctAnswer = answer)
        val oneAnswer = listOf("dziękuję")
        val twoAnswers = listOf("dziękuję", "dzięki")

        listOf(
            Triple("dziękuję", oneAnswer, correct("dziękuję")),
            Triple("Dziękuję", oneAnswer, correct("dziękuję")),
            Triple("  dziękuję!  ", oneAnswer, correct("dziękuję")),
            Triple("DZIĘKUJĘ.", oneAnswer, correct("dziękuję")),
            Triple("dziekuje", oneAnswer, accentWarned("dziękuję")),
            Triple("Dziekuję", oneAnswer, accentWarned("dziękuję")),
            Triple("dziękuje się", oneAnswer, wrong("dziękuję")),
            Triple("dzięki", oneAnswer, wrong("dziękuję")),
            Triple("", oneAnswer, wrong("dziękuję")),
            Triple("dzięki", twoAnswers, correct("dzięki")),
            Triple("dzieki", twoAnswers, accentWarned("dzięki")),
            Triple("proszę", twoAnswers, wrong("dziękuję")),
        ).forEach { (typed, accepted, expected) ->
            val engine = engineWith(listenTypeDay(*accepted.toTypedArray()), clipsFile("dziękuję"))
            engine.startDailySet()

            val feedback = engine.answer(Answer.Typed(typed))

            assertEquals(expected, feedback.copy(audio = null), "Typed \"$typed\" for $accepted")
            assertEquals(firstClip, feedback.audio, "Typed \"$typed\" for $accepted")
        }
    }

    @Test
    fun `a wrongly typed listening Exercise returns until it is typed correctly`() {
        val engine = engineWith(listenTypeDay("dziękuję"), clipsFile("dziękuję"))
        val first = engine.startDailySet()

        engine.answer(Answer.Typed("proszę"))
        assertEquals(first, engine.next())
        engine.answer(Answer.Typed("dziękuję"))

        assertEquals(Step.ShowSummary(correctAnswers = 0, exercises = 1), engine.next())
    }

    @Test
    fun `a Clip is found whatever the capitals and spacing of the text, but not when the punctuation differs`() {
        fun clipOfTyped(accepted: String, clips: String): ClipView? {
            val engine = engineWith(typingDay(accepted), clips)
            engine.startDailySet()
            return engine.answer(Answer.Typed(accepted)).audio
        }

        assertEquals(firstClip, clipOfTyped("Dzień  dobry", clipsFile("dzień dobry")))
        assertEquals(firstClip, clipOfTyped(" dzień dobry ", clipsFile("Dzień Dobry")))
        // A decomposed letter: n followed by a combining accent.
        assertEquals(firstClip, clipOfTyped("dzień dobry", clipsFile("dzień dobry")))
        assertEquals(null, clipOfTyped("jak się masz", clipsFile("jak się masz?")))
        assertEquals(null, clipOfTyped("tak.", clipsFile("tak?")))
        assertEquals(null, clipOfTyped("dzien dobry", clipsFile("dzień dobry")))
    }

    @Test
    fun `a choice Exercise whose Polish prompt has a Clip is shown with it, and its feedback has no Clip`() {
        // Day 1 of the fixture course asks for cześć, then do widzenia.
        val engine = CourseEngine(FixtureCourse.load(clipsFile("cześć")), InMemoryLearnerProgress(), FakeClock())
        engine.startDailySet()

        val hello = (engine.next() as Step.ShowExercise).exercise as ExerciseView.ChooseTranslation
        val feedback = engine.answer(right.getValue("cześć"))
        val goodbye = (engine.next() as Step.ShowExercise).exercise as ExerciseView.ChooseTranslation

        assertEquals("cześć", hello.prompt)
        assertEquals(firstClip, hello.promptAudio)
        assertEquals(Feedback(correct = true, correctAnswer = "hi"), feedback)
        assertEquals("do widzenia", goodbye.prompt)
        assertEquals(null, goodbye.promptAudio)
    }

    @Test
    fun `the feedback of a typing Exercise carries the Clip of the Accepted Answer it shows`() {
        fun clipFor(typed: String): ClipView? {
            val engine = engineWith(typingDay("dziękuję", "dzięki"), clipsFile("dziękuję", "dzięki"))
            engine.startDailySet()
            return engine.answer(Answer.Typed(typed)).audio
        }

        assertEquals(firstClip, clipFor("dziękuję"))
        assertEquals(firstClip, clipFor("dziekuje"))
        assertEquals(firstClip, clipFor("proszę"))
        assertEquals(ClipView("audio/c2.ogg"), clipFor("dzięki"))
    }

    @Test
    fun `the feedback of a word tiles Exercise carries the Clip of the Accepted Answer it shows`() {
        fun clipFor(vararg words: String): ClipView? {
            val day = tilesDay(listOf("dzień dobry", "dobry dzień"))
            val engine = engineWith(day, clipsFile("dzień dobry", "dobry dzień"))
            engine.startDailySet()
            return engine.answer(Answer.Tiles(words.toList())).audio
        }

        assertEquals(firstClip, clipFor("dzień", "dobry"))
        assertEquals(firstClip, clipFor("dobry"))
        assertEquals(ClipView("audio/c2.ogg"), clipFor("dobry", "dzień"))
    }

    @Test
    fun `the feedback of a conjugation Exercise carries the Clip of the form, or of the whole sentence with the form in it`() {
        val clips = clipsFile("jesteśmy", "My jesteśmy w Polsce.", "są")
        fun clipFor(typed: String, day: String): ClipView? {
            val engine = engineWith(day, clips)
            engine.startDailySet()
            return engine.answer(Answer.Typed(typed)).audio
        }
        val withSentence = conjugationDay(
            listOf("jesteśmy"),
            sentence = "My ___ w Polsce.",
            sentenceMeaning = "We are in Poland.",
        )

        assertEquals(firstClip, clipFor("jesteśmy", conjugationDay(listOf("jesteśmy"))))
        assertEquals(firstClip, clipFor("jestem", conjugationDay(listOf("jesteśmy"))))
        assertEquals(ClipView("audio/c2.ogg"), clipFor("jesteśmy", withSentence))
        assertEquals(ClipView("audio/c2.ogg"), clipFor("jestem", withSentence))
    }

    @Test
    fun `feedback has no Clip when the Polish it shows has none`() {
        val engine = engineWith(typingDay("dziękuję"), clipsFile("proszę"))
        engine.startDailySet()

        assertEquals(Feedback(correct = true, correctAnswer = "dziękuję"), engine.answer(Answer.Typed("dziękuję")))
    }

    @Test
    fun `the texts a course can speak are listed once each, in Path order`() {
        val course = courseOf(
            emptyList(),
            day(
                "d1",
                choose("d1-e1", "cześć"),
                typed("d1-e2", "thank you", "dziękuję").copy(acceptedAnswers = listOf("dziękuję", "dzięki")),
                tiles("d1-e3", "good morning", "dzień dobry"),
                conjugate("d1-e4", "być", Person.WE, "jesteśmy"),
            ),
            day(
                "d2",
                conjugate("d2-e1", "być", Person.WE, "jesteśmy").copy(
                    sentence = "My ___ w Polsce.",
                    sentenceMeaning = mapOf("en" to "We are in Poland."),
                ),
                listenChoose("d2-e2", "tak"),
                ListenType("d2-e3", listOf("nie", "nié")),
                // Already listed, whatever the capitals and spacing.
                choose("d2-e4", "Dzień  dobry"),
                typed("d2-e5", "hi", "cześć"),
            ),
        )

        assertEquals(
            listOf("cześć", "dziękuję", "dzięki", "dzień dobry", "jesteśmy", "My jesteśmy w Polsce.", "tak", "nie"),
            course.spokenTexts(),
        )
    }

    private fun listenChooseShown(step: Step): ExerciseView.ListenChoose =
        (step as Step.ShowExercise).exercise as ExerciseView.ListenChoose

    /** A new Learner whose first Daily Set is [day], in a course with these [clips]. */
    private fun engineWith(day: String, clips: String): CourseEngine =
        CourseEngine(FixtureCourse.loadWithDayOne(day, clips), InMemoryLearnerProgress(), FakeClock())
}

/** A Clip manifest with one Clip for each of [texts], with the ids c1, c2 and so on. */
fun clipsFile(vararg texts: String): String {
    val clips = texts.mapIndexed { index, text -> """{ "id": "c${index + 1}", "text": "$text" }""" }
    return """{ "clips": [${clips.joinToString()}] }"""
}

/** Day 1 as a single listen-and-choose Exercise that says [text]. */
fun listenChooseDay(text: String, distractors: List<String>): String {
    val wrong = distractors.joinToString { "\"$it\"" }
    return """
        {
          "id": "a1-u01-d01",
          "exercises": [
            { "type": "listen_choose", "id": "a1-u01-d01-e01", "text": "$text", "distractors": [$wrong] }
          ]
        }
    """
}

/** Day 1 as a single listen-and-type Exercise with these Accepted Answers, the first of which is spoken. */
fun listenTypeDay(vararg accepted: String): String {
    val answers = accepted.joinToString { "\"$it\"" }
    return """
        {
          "id": "a1-u01-d01",
          "exercises": [
            { "type": "listen_type", "id": "a1-u01-d01-e01", "acceptedAnswers": [$answers] }
          ]
        }
    """
}
