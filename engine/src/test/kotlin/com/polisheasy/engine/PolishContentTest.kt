package com.polisheasy.engine

import com.polisheasy.engine.CheckerFixture.Companion.DAY_1
import com.polisheasy.engine.CheckerFixture.Companion.DAY_2
import com.polisheasy.engine.CheckerFixture.Companion.ITEMS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the Polish check is given to look at, listed from [CheckerFixture.valid] and variants of it. */
internal class PolishContentTest {
    private val valid = CheckerFixture.valid

    private fun en(text: String): Localized = mapOf("en" to text)

    private fun PolishContent.sentence(text: String, meaning: String?): PolishSentence =
        assertNotNull(
            sentences.singleOrNull { it.text == text && it.meaning == meaning },
            "No single sentence \"$text\" meaning \"$meaning\" in:\n${sentences.joinToString("\n")}",
        )

    private fun use(file: String, id: String, role: PolishRole) = PolishUse(file, id, role)

    private val tableOfByc = ConjugationTable(
        "być",
        en("to be"),
        mapOf(
            Person.I to "jestem",
            Person.YOU to "jesteś",
            Person.HE_SHE_IT to "jest",
            Person.WE to "jesteśmy",
            Person.YOU_PLURAL to "jesteście",
            Person.THEY to "są",
        ),
    )

    private val withTable = valid.withDailySet(DAY_1) { day ->
        day.copy(tips = listOf(Tip("a1-u01-d01-t01", "a1-u01-d01-e01", en("The verb być"), en("Być means to be."), tableOfByc)))
    }

    @Test
    fun `an Item's text is listed with its meaning, its file and its id`() {
        val sentence = polishContent(valid.files()).sentence("cześć", "hi")

        assertTrue(use(ITEMS, "czesc", PolishRole.ITEM) in sentence.uses, sentence.toString())
    }

    @Test
    fun `a choice Exercise lists its prompt with the right option`() {
        val sentence = polishContent(valid.files()).sentence("cześć", "hi")

        assertTrue(use(DAY_1, "a1-u01-d01-e01", PolishRole.PROMPT) in sentence.uses, sentence.toString())
    }

    @Test
    fun `a typing Exercise lists each Accepted Answer with its prompt`() {
        val content = polishContent(
            valid.withExercise("a1-u01-d01-e02") { (it as TypeAnswer).copy(acceptedAnswers = listOf("dziękuję", "dzięki")) }.files(),
        )

        assertTrue(use(DAY_1, "a1-u01-d01-e02", PolishRole.ANSWER) in content.sentence("dziękuję", "thank you").uses)
        assertEquals(listOf(use(DAY_1, "a1-u01-d01-e02", PolishRole.ANSWER)), content.sentence("dzięki", "thank you").uses)
    }

    @Test
    fun `a word tiles Exercise lists each Accepted Answer with its prompt, and each distractor with no meaning`() {
        val content = polishContent(
            valid.withExercise("a1-u01-d01-e03") { (it as WordTiles).copy(distractors = listOf("ale")) }.files(),
        )

        assertTrue(use(DAY_1, "a1-u01-d01-e03", PolishRole.ANSWER) in content.sentence("dzień dobry", "good morning").uses)
        assertEquals(listOf(use(DAY_1, "a1-u01-d01-e03", PolishRole.DISTRACTOR)), content.sentence("ale", null).uses)
    }

    @Test
    fun `a conjugation Exercise with a sentence lists the sentence with each answer in its gap`() {
        val content = polishContent(valid.files())

        assertEquals(
            listOf(use(DAY_1, "a1-u01-d01-e04", PolishRole.ANSWER)),
            content.sentence("Ja jestem w domu.", "I am at home.").uses,
        )
    }

    @Test
    fun `a conjugation Exercise without a sentence lists each answer with no meaning`() {
        val content = polishContent(
            valid.withExercise("a1-u01-d02-e06") { (it as Conjugation).copy(acceptedAnswers = listOf("jestem", "to ja")) }.files(),
        )

        assertEquals(listOf(use(DAY_2, "a1-u01-d02-e06", PolishRole.ANSWER)), content.sentence("to ja", null).uses)
    }

    @Test
    fun `a listen and choose Exercise lists its text with its meaning, and each distractor with no meaning`() {
        val content = polishContent(
            valid.withExercise("a1-u01-d01-e05") { (it as ListenChoose).copy(distractors = listOf("ale")) }.files(),
        )

        assertTrue(use(DAY_1, "a1-u01-d01-e05", PolishRole.ANSWER) in content.sentence("tak", "yes").uses)
        assertEquals(listOf(use(DAY_1, "a1-u01-d01-e05", PolishRole.DISTRACTOR)), content.sentence("ale", null).uses)
    }

    @Test
    fun `a listen and type Exercise lists each Accepted Answer and reading answer with its meaning`() {
        val content = polishContent(
            valid.withExercise("a1-u01-d02-e04") { (it as ListenType).copy(readingAnswers = listOf("dzięki")) }.files(),
        )
        val use = use(DAY_2, "a1-u01-d02-e04", PolishRole.ANSWER)

        assertEquals(listOf(use), content.sentence("dziękuję", "thank you (to anyone)").uses)
        assertEquals(listOf(use), content.sentence("dzięki", "thank you (to anyone)").uses)
    }

    @Test
    fun `the same text with the same meaning in two Exercises is one sentence with both uses`() {
        val uses = polishContent(valid.files()).sentence("cześć", "hi").uses

        assertEquals(
            listOf(
                use(ITEMS, "czesc", PolishRole.ITEM),
                use(DAY_1, "a1-u01-d01-e01", PolishRole.PROMPT),
                use(DAY_2, "a1-u01-d02-e03", PolishRole.ANSWER),
                // A distractor, which has no meaning of its own, is listed with the text's meaning.
                use(DAY_2, "a1-u01-d02-e05", PolishRole.DISTRACTOR),
            ),
            uses,
        )
    }

    @Test
    fun `the same text with two meanings is two sentences`() {
        val content = polishContent(valid.files())

        assertEquals(
            setOf("thank you", "thank you (to anyone)"),
            content.sentences.filter { it.text == "dziękuję" }.map { it.meaning }.toSet(),
        )
    }

    @Test
    fun `texts that differ by a capital are two sentences`() {
        val content = polishContent(
            valid.withExercise("a1-u01-d02-e03") { (it as TypeAnswer).copy(acceptedAnswers = listOf("Cześć")) }.files(),
        )

        assertEquals(listOf(use(DAY_2, "a1-u01-d02-e03", PolishRole.ANSWER)), content.sentence("Cześć", "hi").uses)
    }

    @Test
    fun `a distractor that is also an Item is listed once, with the Item's meaning and both uses`() {
        val content = polishContent(valid.files())

        // "tak" is the distractor of the word tiles Exercise.
        assertEquals(emptyList(), content.sentences.filter { it.text == "tak" && it.meaning == null })
        val uses = content.sentence("tak", "yes").uses
        assertTrue(use(ITEMS, "tak", PolishRole.ITEM) in uses, uses.toString())
        assertTrue(use(DAY_1, "a1-u01-d01-e03", PolishRole.DISTRACTOR) in uses, uses.toString())
    }

    @Test
    fun `an Exercise's answer, a Tip table's form and the Item practised are one verb form claim with every use`() {
        val content = polishContent(withTable.files())

        val claim = content.verbForms.single { it.verb == "być" && it.person == Person.I }
        assertEquals("jestem", claim.form)
        assertEquals(
            setOf(
                use(DAY_1, "a1-u01-d01-e04", PolishRole.ANSWER),
                use(ITEMS, "byc-jestem", PolishRole.ITEM),
                use(DAY_1, "a1-u01-d01-t01", PolishRole.TABLE),
                use(DAY_2, "a1-u01-d02-e06", PolishRole.ANSWER),
            ),
            claim.uses.toSet(),
        )
        assertEquals(6, content.verbForms.size, content.verbForms.toString())
    }

    @Test
    fun `an Item that no conjugation Exercise names makes no verb form claim`() {
        val content = polishContent(valid.files())

        assertEquals(listOf("jestem"), content.verbForms.map { it.form })
    }

    @Test
    fun `a Tip table's forms are listed as texts with no meaning`() {
        val content = polishContent(withTable.files())

        assertEquals(listOf(use(DAY_1, "a1-u01-d01-t01", PolishRole.TABLE)), content.sentence("jesteście", null).uses)
        assertNull(content.sentences.firstOrNull { it.text == "jestem" && it.meaning == null })
    }

    @Test
    fun `a Tip's text is its title, its body and its table's forms`() {
        val content = polishContent(withTable.files())

        assertEquals(
            listOf(
                TipText(DAY_1, "a1-u01-d01-t01", "The verb być\nByć means to be.\njestem\njesteś\njest\njesteśmy\njesteście\nsą"),
                TipText(DAY_2, "a1-u01-d02-t01", "Yes and no\nTak is yes."),
            ),
            content.tips,
        )
        assertEquals("Saying hello\nCześć is informal.", polishContent(valid.files()).tips.first().text)
    }

    @Test
    fun `a listed file that cannot be read is reported as unreadable`() {
        val content = polishContent(valid.without(DAY_2).files())

        assertEquals(listOf(DAY_2), content.unreadable.map { it.file })
        assertEquals(emptyList(), polishContent(valid.files()).unreadable)
    }
}
