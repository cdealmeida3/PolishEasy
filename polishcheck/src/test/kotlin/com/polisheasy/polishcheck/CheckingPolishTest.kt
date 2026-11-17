package com.polisheasy.polishcheck

import com.polisheasy.engine.Person
import com.polisheasy.engine.PolishContent
import com.polisheasy.engine.PolishRole
import com.polisheasy.engine.PolishSentence
import com.polisheasy.engine.Gender
import com.polisheasy.engine.GenderedWord
import com.polisheasy.engine.PolishUse
import com.polisheasy.engine.SoundGap
import com.polisheasy.engine.SoundGroup
import com.polisheasy.engine.TipText
import com.polisheasy.engine.VerbForm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** One reference for every test: starting it takes seconds. */
internal val reference by lazy { PolishReference() }

/** The four passes against small samples of known-right and known-wrong Polish. */
internal class CheckingPolishTest {
    private val file = "a1/unit-01/day-01.json"

    private fun sentence(text: String, meaning: String? = "something", vararg ids: String = arrayOf("e1")) =
        PolishSentence(text, meaning, ids.map { PolishUse(file, it, PolishRole.ANSWER) })

    private fun claim(verb: String, person: Person, form: String) =
        VerbForm(verb, person, form, listOf(PolishUse(file, "e1", PolishRole.ANSWER)))

    private fun tip(text: String, id: String = "t1") = TipText(file, id, text)

    private fun content(
        sentences: List<PolishSentence> = emptyList(),
        verbForms: List<VerbForm> = emptyList(),
        tips: List<TipText> = emptyList(),
    ) = PolishContent(sentences, verbForms, tips, emptyList())

    /** A verdict of "right" on everything in [content]. */
    private fun reviewed(content: PolishContent) = Reviews(
        content.sentences.map { SentenceReview(it.text, it.meaning) },
        content.tips.map { TipReview(it.id, hashOf(it)) },
    )

    /** Checks [content] with everything reviewed, unless [reviews] says otherwise. */
    private fun check(content: PolishContent, reviews: Reviews = reviewed(content), accepted: Accepted = Accepted()) =
        checkPolish(content, reference, reviews, accepted)

    private fun check(vararg texts: String) = check(content(texts.map { sentence(it) }))

    private fun accepting(key: String, reason: String = "It is right as it is.") = Accepted(listOf(Acceptance(key, reason)))

    // The whole check

    @Test
    fun `right Polish that has all been reviewed has no problem`() {
        val report = check(
            "dzień dobry",
            "jestem z Polski",
            "czy mówisz po polsku?",
            "moja córka ma trzy lata",
            "ty i ja mamy czas",
            "Adam i Ewa są tutaj",
        )

        assertEquals(emptyList(), report.problems)
        assertEquals(6, report.sentences)
    }

    // Sound gaps

    private fun gap(itemId: String, before: String, answer: String, after: String, group: SoundGroup) =
        "a1/unit-01/items.json" to SoundGap(itemId, before + answer + after, group, before, answer, after)

    @Test
    fun `a wrong spelling Sound gaps would offer that is a real word is a problem, until it is listed`() {
        // "kasa" is a word: asking for the sz of "kasza" would call a real word wrong. "śukam" and "sukam" are not.
        val gaps = listOf(gap("kasza", "ka", "sz", "a", SoundGroup.SZ), gap("szukam", "", "sz", "ukam", SoundGroup.SZ))
        val content = PolishContent(emptyList(), emptyList(), emptyList(), emptyList(), soundGaps = gaps, practiceFile = "practice/practice.json")

        val problem = check(content).problems.single()

        assertEquals(Pass.SOUND_GAPS, problem.pass)
        assertEquals("sound-gap: kasa", problem.key)
        assertTrue("practice/practice.json" in problem.message)
        assertEquals(listOf(Place("a1/unit-01/items.json", "kasza")), problem.places)
        assertEquals(4, check(content).looked[Pass.SOUND_GAPS])
    }

    @Test
    fun `a word listed as real that the dictionary does not know is a problem`() {
        val content = PolishContent(emptyList(), emptyList(), emptyList(), emptyList(), realWords = listOf("kasa", "śukam"), practiceFile = "practice/practice.json")

        val problem = check(content).problems.single()

        assertEquals("real-word: śukam", problem.key)
        assertEquals(listOf(Place("practice/practice.json", null)), problem.places)
    }

    // Genders

    private fun gendered(nouns: List<GenderedWord>, my: List<GenderedWord> = emptyList()) =
        PolishContent(emptyList(), emptyList(), emptyList(), emptyList(), practiceFile = "practice/practice.json", nouns = nouns, possessives = my)

    @Test
    fun `nouns and words for my with the gender the dictionary gives them pass`() {
        val content = gendered(
            // "tata" ends like a feminine noun and is masculine.
            listOf(
                GenderedWord("tata", "tata", Gender.MASCULINE),
                GenderedWord("siostra", "siostra", Gender.FEMININE),
                GenderedWord("dziecko", "dziecko", Gender.NEUTER),
                GenderedWord("maz", "mąż", Gender.MASCULINE),
            ),
            listOf(GenderedWord("moj", "mój", Gender.MASCULINE), GenderedWord("moja", "moja", Gender.FEMININE), GenderedWord("moje", "moje", Gender.NEUTER)),
        )

        val report = check(content)

        assertEquals(emptyList(), report.problems)
        assertEquals(7, report.looked[Pass.GENDERS])
    }

    @Test
    fun `a noun given the wrong gender is a problem that names the gender the dictionary gives`() {
        val problem = check(gendered(listOf(GenderedWord("tata", "tata", Gender.FEMININE)))).problems.single()

        assertEquals(Pass.GENDERS, problem.pass)
        assertEquals("gender: tata", problem.key)
        assertTrue("\"m\"" in problem.message, problem.message)
        assertEquals(listOf(Place("practice/practice.json", "tata")), problem.places)
    }

    @Test
    fun `a noun in another form than the dictionary lists, and a word that is no noun, are problems`() {
        // "siostrę" is the form after "mam"; "bardzo" is not a noun at all.
        val problems = check(gendered(listOf(GenderedWord("siostra-acc", "siostrę", Gender.FEMININE), GenderedWord("bardzo", "bardzo", Gender.NEUTER)))).problems

        assertEquals(listOf("gender: siostrę", "gender: bardzo"), problems.map { it.key })
    }

    @Test
    fun `a word for my given the wrong gender is a problem`() {
        val problem = check(gendered(emptyList(), listOf(GenderedWord("moja", "moja", Gender.MASCULINE)))).problems.single()

        assertEquals("gender: moja", problem.key)
        assertTrue("\"f\"" in problem.message, problem.message)
    }

    // Dictionary

    @Test
    fun `a misspelt word is a dictionary problem that names the word and every place it is used`() {
        mapOf("mówie po polsku" to "mówie", "Dzien dobry" to "Dzien", "my jestesmy tutaj" to "jestesmy").forEach { (text, word) ->
            val problem = check(content(listOf(sentence(text, "something", "e1", "e2")))).problems.single { it.pass == Pass.DICTIONARY }

            assertEquals("dictionary: $word", problem.key)
            assertEquals("$word is not in the dictionary", problem.message)
            assertEquals(listOf(Place(file, "e1"), Place(file, "e2")), problem.places)
        }
    }

    @Test
    fun `a word unknown twice is one problem with both uses`() {
        val report = check(content(listOf(sentence("mówie po polsku", "I speak Polish", "e1"), sentence("nie mówie", "I do not speak", "e2"))))

        assertEquals(listOf(Place(file, "e1"), Place(file, "e2")), report.problems.single().places)
    }

    @Test
    fun `a Tip with a misspelt Polish word is a problem, and a Tip that names letters is not`() {
        val wrong = check(content(tips = listOf(tip("With wy the form is jesteśćie."))))
        val letters = check(content(tips = listOf(tip("The letters ą and dź are new. Dziękuję is thank you."))))

        assertEquals(listOf("dictionary: jesteśćie"), wrong.problems.map { it.key })
        assertEquals(listOf(Place(file, "t1")), wrong.problems.single().places)
        assertEquals(emptyList(), letters.problems)
    }

    @Test
    fun `what a Tip marks as Polish has to be Polish, even a word with no Polish letter`() {
        val english = check(content(tips = listOf(TipText(file, "t1", "Say thanks and bardzo.", polish = listOf("thanks", "bardzo")))))
        val pieces = check(
            content(tips = listOf(TipText(file, "t1", "sz, -my and do-BRA-noc, mam czas.", polish = listOf("sz", "-my", "do-BRA-noc", "mam czas")))),
        )

        assertEquals(listOf("dictionary: thanks"), english.problems.map { it.key })
        assertEquals(emptyList(), pieces.problems)
    }

    // Verb forms

    @Test
    fun `a right verb form passes for each of the six persons`() {
        val forms = mapOf(
            "być" to listOf("jestem", "jesteś", "jest", "jesteśmy", "jesteście", "są"),
            "mówić" to listOf("mówię", "mówisz", "mówi", "mówimy", "mówicie", "mówią"),
        )
        val claims = forms.flatMap { (verb, written) -> Person.entries.zip(written).map { (person, form) -> claim(verb, person, form) } }

        val report = check(content(verbForms = claims))

        assertEquals(emptyList(), report.problems)
        assertEquals(12, report.verbForms)
    }

    @Test
    fun `a form of the wrong person is reported with the reference form`() {
        val problem = check(content(verbForms = listOf(claim("być", Person.WE, "jestem")))).problems.single()

        assertEquals(Pass.VERB_FORMS, problem.pass)
        assertEquals("verb-form: być my jestem", problem.key)
        assertEquals("jestem is not the form of być for my; the reference has jesteśmy", problem.message)
    }

    @Test
    fun `a form that is not a word is reported once as a verb form and once by the dictionary`() {
        val report = check(content(listOf(sentence("mówie")), listOf(claim("mówić", Person.I, "mówie"))))

        assertEquals(listOf("dictionary: mówie", "verb-form: mówić ja mówie"), report.problems.map { it.key })
    }

    @Test
    fun `a verb the reference does not know is reported once as having no conjugation`() {
        val report = check(content(verbForms = listOf(claim("blablać", Person.I, "blablam"), claim("blablać", Person.YOU, "blablasz"))))

        assertEquals("verb-form: blablać", report.problems.single().key)
        assertEquals("the reference has no conjugation of blablać", report.problems.single().message)
    }

    // Grammar rules

    @Test
    fun `a sentence a grammar rule rejects is reported with the rule's id`() {
        mapOf("Ewa ma pięć lata" to "NIEZGODNOSC_PRZYPADKU_PO_LICZEBNIKU", "jestem z Polska" to "PREP_CASUS").forEach { (text, rule) ->
            val problems = check(text).problems

            assertTrue(problems.any { it.pass == Pass.GRAMMAR && it.key == "grammar: $rule: $text" }, "$text: $problems")
        }
    }

    @Test
    fun `a sentence without a capital and a misspelt word are not grammar problems`() {
        assertEquals(emptyList(), check("mam na imię Ewa. jestem z Polski.").problems)
        assertEquals(listOf(Pass.DICTIONARY), check("mówie po polsku").problems.map { it.pass })
    }

    @Test
    fun `a single word is not held to the rules for a whole sentence`() {
        assertEquals(emptyList(), check("czy", "ale", "nie").problems)
    }

    @Test
    fun `a pronoun with a verb of another person is reported`() {
        listOf("my jestem tutaj", "oni jest z Anglii", "wy mówimy po polsku").forEach { text ->
            assertEquals(listOf("grammar: PRONOUN_VERB: $text"), check(text).problems.map { it.key }, text)
        }
    }

    @Test
    fun `a pronoun with its own verb, a word that is also a noun, and a pronoun after i are not reported`() {
        assertEquals(emptyList(), check("my mamy czas", "ty i ja mamy czas", "ona jest tutaj", "to ja. Jesteś tutaj?").problems)
    }

    @Test
    fun `the machine passes miss most wrong agreement and case, which is why every sentence is also reviewed`() {
        // If a newer reference starts to catch one of these, move it to a test above and update docs/polish-check.md.
        val missed = listOf(
            "mam siostra",
            "to jest moja brat",
            "mój mama ma na imię Ewa",
            "ona mają syna",
            "on ma dwa lat",
            "Adam mam syna",
            "czy ty mówić po polsku?",
        )

        assertEquals(emptyList(), check(*missed.toTypedArray()).problems)
    }

    // Review

    @Test
    fun `a sentence or a Tip with no review entry has not been reviewed`() {
        val content = content(listOf(sentence("dzień dobry", "good morning")), tips = listOf(tip("Say dzień dobry by day.")))

        val report = check(content, reviews = Reviews())

        assertEquals(
            listOf("\"dzień dobry\" has not been reviewed", "Tip t1 has not been reviewed"),
            report.problems.map { it.message },
        )
        assertEquals(listOf(Pass.REVIEW, Pass.REVIEW), report.problems.map { it.pass })
        assertEquals(listOf("dzień dobry" to "good morning"), report.toReview.sentences.map { it.text to it.meaning })
        assertEquals(listOf(UseEntry(file, "e1", "answer")), report.toReview.sentences.single().uses)
        assertEquals(listOf("Say dzień dobry by day."), report.toReview.tips.map { it.text })
    }

    @Test
    fun `a sentence reviewed under another meaning, and a Tip reviewed before its text changed, have not been reviewed`() {
        val before = content(listOf(sentence("cześć", "hi")), tips = listOf(tip("Cześć is informal.")))
        val after = content(listOf(sentence("cześć", "bye")), tips = listOf(tip("Cześć is very informal.")))

        val report = check(after, reviews = reviewed(before))

        assertEquals(listOf("unreviewed: cześć", "unreviewed: t1"), report.problems.map { it.key })
    }

    @Test
    fun `a review entry with a finding is a problem whose message is the finding`() {
        val content = content(listOf(sentence("to moja brat", "this is my brother")), tips = listOf(tip("Brat is feminine.")))
        val reviews = Reviews(
            listOf(SentenceReview("to moja brat", "this is my brother", "moja is feminine; brat needs mój")),
            listOf(TipReview("t1", hashOf(content.tips.single()), "brat is masculine")),
        )

        val report = check(content, reviews)

        assertEquals(
            listOf("review: to moja brat" to "moja is feminine; brat needs mój", "review: t1" to "brat is masculine"),
            report.problems.map { it.key to it.message },
        )
        assertEquals(Reviews(), report.toReview)
    }

    @Test
    fun `a review entry for a sentence the content does not have is not a problem`() {
        val content = content(listOf(sentence("dzień dobry")))

        val report = check(content, Reviews(reviewed(content).sentences + SentenceReview("dobry wieczór", "good evening", "gone")))

        assertEquals(emptyList(), report.problems)
    }

    // Accepted problems

    @Test
    fun `an accepted problem of each pass is not among the problems and is listed with its reason`() {
        val wrong = content(
            listOf(sentence("mówie"), sentence("jestem z Polska"), sentence("to moja brat")),
            listOf(claim("być", Person.WE, "jestem")),
        )
        val reviews = Reviews(reviewed(wrong).sentences.map { if (it.text == "to moja brat") it.copy(finding = "brat needs mój") else it })
        val keys = listOf("dictionary: mówie", "verb-form: być my jestem", "grammar: PREP_CASUS: jestem z Polska", "review: to moja brat")
        assertEquals(keys, check(wrong, reviews).problems.map { it.key })

        val report = check(wrong, reviews, Accepted(keys.map { Acceptance(it, "Reason for $it") }))

        assertEquals(emptyList(), report.problems)
        assertEquals(keys.map { it to "Reason for $it" }, report.accepted.map { it.problem.key to it.reason })
    }

    @Test
    fun `an acceptance with a blank reason is a problem and accepts nothing`() {
        val report = check(content(listOf(sentence("mówie"))), accepted = accepting("dictionary: mówie", reason = " "))

        assertEquals(listOf("dictionary: mówie", "accepted: dictionary: mówie"), report.problems.map { it.key })
        assertTrue("no reason" in report.problems.last().message, report.problems.last().message)
        assertEquals(emptyList(), report.accepted)
    }

    @Test
    fun `an acceptance that matches nothing is a problem`() {
        val report = check(content(listOf(sentence("mówię"))), accepted = accepting("dictionary: mówie"))

        assertEquals(listOf("accepted: dictionary: mówie"), report.problems.map { it.key })
        assertTrue("no longer reported" in report.problems.single().message, report.problems.single().message)
        assertEquals(listOf(Place(ACCEPTED_FILE, null)), report.problems.single().places)
    }

    @Test
    fun `an unreviewed sentence cannot be accepted`() {
        val report = check(content(listOf(sentence("dzień dobry"))), Reviews(), accepting("unreviewed: dzień dobry"))

        assertEquals(listOf("unreviewed: dzień dobry", "accepted: unreviewed: dzień dobry"), report.problems.map { it.key })
        assertEquals(emptyList(), report.accepted)
    }

    @Test
    fun `a problem that cannot be accepted cannot be accepted by naming its own acceptance`() {
        val accepted = Accepted(listOf(Acceptance("dictionary: nic", "x"), Acceptance("accepted: dictionary: nic", "x")))

        val report = check(content(listOf(sentence("dzień dobry"))), accepted = accepted)

        assertEquals(2, report.problems.size, report.problems.toString())
    }
}
