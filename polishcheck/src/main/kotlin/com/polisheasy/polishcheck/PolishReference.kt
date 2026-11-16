package com.polisheasy.polishcheck

import com.polisheasy.engine.Person
import org.languagetool.AnalyzedToken
import org.languagetool.JLanguageTool
import org.languagetool.Languages

/** What a grammar rule of the reference says about a text. */
data class RuleFinding(val ruleId: String, val message: String)

/** How the reference's tags write each person, for example `verb:fin:sg:pri:imperf`. */
internal val Person.tag: String
    get() = when (this) {
        Person.I -> "sg:pri"
        Person.YOU -> "sg:sec"
        Person.HE_SHE_IT -> "sg:ter"
        Person.WE -> "pl:pri"
        Person.YOU_PLURAL -> "pl:sec"
        Person.THEY -> "pl:ter"
    }

/**
 * The reference sources of the Polish check: LanguageTool's Polish dictionary, read both ways,
 * and its grammar rules. Starting it takes a few seconds, so make one and share it. Not for use
 * from more than one thread.
 */
class PolishReference {
    private val polish = Languages.getLanguageForShortCode("pl-PL")
    private val synthesizer = checkNotNull(polish.synthesizer) { "LanguageTool has no Polish synthesizer" }
    private val tool = JLanguageTool(polish).apply {
        // Answers are written without a sentence capital on purpose.
        disableRule("UPPERCASE_SENTENCE_START")
        // The speller. The dictionary pass reports a misspelt word, once.
        disableRule("MORFOLOGIK_RULE_PL_PL")
    }

    /** The versions of the sources, for the report. */
    val version: String = "LanguageTool ${JLanguageTool.VERSION} (language-pl) with its Morfologik Polish dictionary"

    /** The reference's tags for [word], one for each reading; empty when it is not a Polish word form. */
    fun readings(word: String): List<String> =
        polish.tagger.tag(listOf(word)).single().readings.mapNotNull { it.posTag }

    /** Whether [word] is a form in the dictionary. Capitals are ignored: "polski" and "Polski" are both known. */
    fun knows(word: String): Boolean = readings(word).isNotEmpty()

    /** The reference forms of [verb] for [person]; empty when the reference does not know the verb. */
    fun conjugation(verb: String, person: Person): Set<String> =
        synthesizer.synthesize(AnalyzedToken(verb, null, verb), "verb:fin:${person.tag}:.*", true).toSet()

    /**
     * What the reference's grammar rules find in [text]. Many rules stay silent about the last
     * word of a text that does not end its sentence ("jestem z Polska" passes, "jestem z Polska."
     * does not), and answers are written without a full stop, so one is added for the rules. A
     * single word is not a sentence and is checked as it is: "czy." would be a question with no
     * question mark.
     */
    fun grammar(text: String): List<RuleFinding> {
        val sentence = text.trim().let { if (' ' !in it || it.last() in SENTENCE_ENDS) it else "$it." }
        return tool.check(sentence).map { match -> RuleFinding(match.rule.id, match.message.replace(tags, "")) }.distinct()
    }

    private companion object {
        val tags = Regex("</?suggestion>")
        const val SENTENCE_ENDS = ".?!…"
    }
}
