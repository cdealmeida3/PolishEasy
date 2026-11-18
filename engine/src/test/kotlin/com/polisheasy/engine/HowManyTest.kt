package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class HowManyTest {
    private val words = listOf(
        item("dwie"), item("butelka").copy(forms = listOf("butelki", "butelek", "butelkę")), item("piec", "pięć"), item("dwa"),
        item("karton").copy(forms = listOf("kartony", "kartonów")), item("trzy"), item("sloik", "słoik").copy(forms = listOf("słoiki", "słoików")),
    )

    private fun phrase(text: String, item: String, count: Int, vararg items: String) =
        CountPhrase(text, mapOf("en" to "$text (en)"), item, count, items.toList())

    private val phrases = listOf(
        phrase("dwie butelki", "butelka", 2, "dwie", "butelka"),
        phrase("pięć butelek", "butelka", 5, "piec", "butelka"),
        phrase("dwa kartony", "karton", 2, "dwa", "karton"),
        phrase("pięć kartonów", "karton", 5, "piec", "karton"),
        phrase("trzy słoiki", "sloik", 3, "trzy", "sloik"),
    )

    private val pictures = listOf("butelka", "karton", "sloik").associateWith { Picture.Glyph(it) as Picture }

    /** l1 teaches four phrases' words, l2 the fifth's. */
    private val course = Course(
        listOf(levelOf("a1", day("l1", *words.take(5).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()), day("l2", *words.drop(5).map { choose("e-${it.id}", it.text, it.id) }.toTypedArray()))),
        words,
        pictures = pictures,
        counting = phrases,
    )

    private fun player(seed: Int = 0) = Player(course = course, seed = seed)

    private fun Practice.round(): HowManyRound = start(setup(Game.HOW_MANY, emptyList())) as HowManyRound

    @Test
    fun `a phrase counts with the plural to four and the genitive plural from five`() {
        assertEquals(0, Counting.formFor(2))
        assertEquals(0, Counting.formFor(4))
        assertEquals(1, Counting.formFor(5))
        assertEquals(1, Counting.formFor(10))
        assertEquals("pięć", phrases[1].number)
        assertEquals("butelek", phrases[1].counted)
    }

    @Test
    fun `How many opens once four phrases are learned`() {
        val state = { p: Player -> p.practice().view().games.single { it.game == Game.HOW_MANY }.state }
        assertEquals(GameState.NEEDS_MORE, state(player()))
        assertEquals(GameState.READY, state(player().completed("l1")))
    }

    @Test
    fun `each question offers the number with the thing's three words, the phrase among them`() {
        val round = player().completed("l1", "l2").practice().round()
        assertEquals(phrases.size, round.total)
        round.run.questions.forEach { question ->
            val phrase = phrases.single { it.text == question.text }
            val item = words.single { it.id == phrase.item }
            assertEquals((listOf(item.text) + item.forms.take(2)).map { "${phrase.number} $it" }.toSet(), question.offered.toSet())
            assertEquals(phrase.text, question.offered[question.right])
            assertEquals(phrase.count, question.count)
        }
    }

    @Test
    fun `the right phrase is right first try, and the score counts first tries`() {
        var round = player().completed("l1", "l2").practice().round()
        val first = checkNotNull(round.question)
        round = round.choose(first.right)
        assertEquals(true, round.given?.correct)
        round = round.next()
        val second = checkNotNull(round.question)
        round = round.choose(second.offered.indices.first { it != second.right })
        assertEquals(false, round.given?.correct)
        round = round.next()
        while (!round.finished) round = round.choose(checkNotNull(round.question).right).next()
        assertEquals(Score(phrases.size - 1, of = phrases.size), round.score)
    }
}
