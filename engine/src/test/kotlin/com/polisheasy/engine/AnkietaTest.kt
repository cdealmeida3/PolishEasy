package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnkietaTest {
    private fun en(text: String) = mapOf("en" to text)

    private val fields = listOf(
        AnkietaField("imie", "Imię", en("First name")),
        AnkietaField("skad", "Skąd jest?", en("Where from?")),
    )

    private fun person(id: String, name: String, from: String, item: String = "z", wrongFrom: List<String> = emptyList(), answersFrom: Boolean = true) = AnkietaPerson(
        id = id,
        items = listOf(item),
        says = listOf(AnkietaLine("Mam na imię $name.", en("My name is $name.")), AnkietaLine("Jestem $from.", en("I am $from."))),
        answers = mapOf("imie" to name) + if (answersFrom) mapOf("skad" to from) else emptyMap(),
        wrong = if (wrongFrom.isEmpty()) emptyMap() else mapOf("skad" to wrongFrom),
    )

    private val people = listOf(
        person("julia", "Julia", "z Francji"),
        person("maria", "Maria", "z Hiszpanii"),
        person("anna", "Anna", "z Niemiec", wrongFrom = listOf("z Polski")),
        // Met only once the second Daily Set is completed, and says nothing of where she is from.
        person("ewa", "Ewa", "z Polski", item = "w", answersFrom = false),
    )

    private fun course(clipped: Boolean = true) = Course(
        listOf(levelOf("a1", day("d1", choose("d1-e1", "z", "z")), day("d2", choose("d2-e1", "w", "w")))),
        listOf(item("z", "z"), item("w", "w")),
        clips = if (clipped) people.flatMap { it.says }.map { clip(it.text) } else emptyList(),
        ankieta = AnkietaFile(fields, people),
    )

    private fun player(clipped: Boolean = true) = Player(course = course(clipped))

    private fun Practice.ankieta(way: String = "read"): AnkietaRound = start(setup(Game.ANKIETA, listOf(way))) as AnkietaRound

    /** Answers every field, right unless it is the field of a person whose id is in [wrongFor]. */
    private fun AnkietaRound.playOut(wrongFor: Set<String> = emptySet()): AnkietaRound {
        var round = this
        while (!round.finished) {
            val question = checkNotNull(round.question)
            val wrong = round.people[question.person].id in wrongFor
            val option = if (wrong) question.options.indices.first { it != question.answer } else question.answer
            round = round.choose(option).next()
        }
        return round
    }

    @Test
    fun `Ankieta opens once three people's every Item is learned`() {
        fun state(vararg done: String) = player().completed(*done).practice().view().games.single { it.game == Game.ANKIETA }.state
        assertEquals(GameState.NEEDS_MORE, state())
        assertEquals(GameState.READY, state("d1"))
    }

    @Test
    fun `the people can be heard only with sound on and a Clip for every line, and always read`() {
        val practice = player().completed("d1").practice()
        assertEquals(listOf("listen", "read"), practice.setup(Game.ANKIETA).choices.single().options.map { it.id })
        assertEquals(listOf("read"), practice.setup(Game.ANKIETA, sound = false).choices.single().options.map { it.id })
        assertEquals(listOf("read"), player(clipped = false).completed("d1").practice().setup(Game.ANKIETA).choices.single().options.map { it.id })
    }

    @Test
    fun `a Round meets three people and asks each the fields they answer, in the form's order`() {
        val round = player().completed("d1", "d2").practice().ankieta()
        assertEquals(3, round.people.size)
        assertEquals(round.people.size, round.people.map { it.id }.distinct().size)
        round.people.forEachIndexed { index, card ->
            val asked = round.run.questions.filter { it.person == index }
            val expected = people.single { it.id == card.id }.answers.keys.let { keys -> fields.map { it.id }.filter { it in keys } }
            assertEquals(expected, asked.map { it.field })
        }
        // A person is one step: the form of each fills in place.
        assertEquals(0, round.step)
        assertEquals(round.people.sumOf { card -> people.single { it.id == card.id }.answers.size }, round.total)
    }

    @Test
    fun `a field offers the right answer among others' answers and its own wrong ones, each once, at most four`() {
        val round = player(clipped = true).completed("d1").practice().ankieta()
        round.run.questions.forEach { question ->
            val person = people.single { it.id == round.people[question.person].id }
            assertEquals(person.answers[question.field], question.right)
            assertEquals(question.options.size, question.options.distinct().size)
            assertTrue(question.options.size in 3..4, "${question.options}")
            // Only the people the Learner can meet give wrong answers: Ewa is not met yet.
            assertFalse("Ewa" in question.options)
        }
        val annasFrom = round.run.questions.single { it.field == "skad" && round.people[it.person].id == "anna" }
        assertTrue("z Polski" in annasFrom.options)
    }

    @Test
    fun `a right answer fills its field, and a wrong one fills it with the right one and counts against the result`() {
        var round = player().completed("d1").practice().ankieta()
        val first = checkNotNull(round.question)
        assertEquals(listOf(true, false), round.form.map { it.current })
        assertEquals(listOf(null, null), round.form.map { it.filled })

        round = round.choose(first.answer)
        assertEquals(Given(first.right, correct = true), round.given)
        assertEquals(first.right, round.form[0].filled)
        assertEquals(true, round.form[0].right)
        // Answered once: another tap changes nothing.
        assertEquals(round, round.choose(first.answer))

        round = round.next()
        val second = checkNotNull(round.question)
        val wrong = second.options.indices.first { it != second.answer }
        round = round.choose(wrong)
        assertEquals(Given(second.options[wrong], correct = false), round.given)
        assertEquals(second.right, round.form[1].filled)
        assertEquals(false, round.form[1].right)

        round = round.next()
        assertEquals(1, round.step)
        val finished = round.playOut()
        assertTrue(finished.finished)
        assertNull(finished.question)
        assertEquals(Score(5, of = 6), finished.score)
    }

    @Test
    fun `heard, a Round meets only people whose every line has a Clip`() {
        val round = player().completed("d1").practice().ankieta("listen")
        assertTrue(round.listen)
        assertEquals(setOf("julia", "maria", "anna"), round.people.map { it.id }.toSet())
    }

    @Test
    fun `the result is the fields right first try, kept as the Best`() {
        val player = player().completed("d1")
        val practice = player.practice()
        val worse = practice.finish(practice.ankieta().playOut(wrongFor = setOf("julia")))
        assertEquals(4, worse.best)
        val better = practice.finish(practice.ankieta().playOut())
        assertEquals(6, better.best)
        assertTrue(better.newBest)
    }

    @Test
    fun `the loader rejects a form someone answers with a field that is not there or too little to choose among`() {
        fun problems(ankieta: AnkietaFile): List<String> {
            val files = mapOf(
                "course.json" to """{"levels": [], "ankieta": "ankieta.json"}""",
                "ankieta.json" to contentJson.encodeToString(AnkietaFile.serializer(), ankieta),
            )
            return loadingProblems(readContent { files.getValue(it) }).map { it.message }
        }
        val known = people.take(3).map { it.copy(items = emptyList()) }
        assertEquals(
            known.map { "Ankieta person ${it.id} lists no Items, so they would be met before anything is learned" },
            problems(AnkietaFile(fields, known)),
        )
        val julia = known[0].copy(answers = known[0].answers + ("wiek" to "20"))
        assertEquals(
            listOf(
                "Ankieta person julia lists no Items, so they would be met before anything is learned",
                "Ankieta person julia answers field wiek, which is not there",
                "Ankieta person julia answers imie with fewer than two other answers to choose among",
                "Ankieta person julia answers skad with fewer than two other answers to choose among",
                "Ankieta person julia answers wiek with fewer than two other answers to choose among",
                "Ankieta person maria lists no Items, so they would be met before anything is learned",
                "Ankieta person maria answers imie with fewer than two other answers to choose among",
                "Ankieta person maria answers skad with fewer than two other answers to choose among",
            ),
            problems(AnkietaFile(fields, listOf(julia, known[1]))),
        )
    }
}
