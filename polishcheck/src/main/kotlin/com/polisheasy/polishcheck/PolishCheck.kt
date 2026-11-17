package com.polisheasy.polishcheck

import com.polisheasy.engine.Person
import com.polisheasy.engine.PolishContent
import com.polisheasy.engine.PolishUse
import java.util.Locale

/** The parts of the check, in the order the report lists them. */
enum class Pass(val title: String) {
    DICTIONARY("Dictionary"),
    VERB_FORMS("Verb forms"),
    GRAMMAR("Grammar rules"),
    REVIEW("Review"),

    /** The wrong spellings Sound gaps offers: none may be a real word. */
    SOUND_GAPS("Sound gaps"),

    /** Not a pass over the content: what is wrong with `polish-accepted.json` itself. */
    ACCEPTANCES("Accepted problems"),
}

/** A file and the Item, Exercise or Tip in it; [id] is null when the place is the file itself. */
data class Place(val file: String, val id: String?)

/**
 * One thing the check could not confirm. [key] names it for `polish-accepted.json`, and one
 * problem lists every place its text is written.
 */
data class PolishProblem(val pass: Pass, val key: String, val message: String, val places: List<Place>)

data class AcceptedProblem(val problem: PolishProblem, val reason: String)

class PolishReport(
    /** Every problem that is not accepted. The check passes when there is none. */
    val problems: List<PolishProblem>,
    val accepted: List<AcceptedProblem>,
    /** How many things each pass looked at. */
    val looked: Map<Pass, Int>,
    /** Everything with no verdict yet, with the context a reviewer needs. */
    val toReview: Reviews,
    val sentences: Int,
    val verbForms: Int,
    val tips: Int,
)

private val word = Regex("\\p{L}+")
private val polishLetter = Regex("[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ]")

/** How content writes each person, which is how a problem names it. */
private val Person.written: String
    get() = when (this) {
        Person.I -> "ja"
        Person.YOU -> "ty"
        Person.HE_SHE_IT -> "on"
        Person.WE -> "my"
        Person.YOU_PLURAL -> "wy"
        Person.THEY -> "oni"
    }

private val pronouns: Map<String, Person> = mapOf(
    "ja" to Person.I,
    "ty" to Person.YOU,
    "on" to Person.HE_SHE_IT,
    "ona" to Person.HE_SHE_IT,
    "ono" to Person.HE_SHE_IT,
    "my" to Person.WE,
    "wy" to Person.YOU_PLURAL,
    "oni" to Person.THEY,
    "one" to Person.THEY,
)

/** The id of the one grammar rule written here and not taken from the reference. */
const val PRONOUN_VERB = "PRONOUN_VERB"

/**
 * A pronoun directly followed by a word that can only be a finite verb, and not of the pronoun's
 * person. A word with any other reading is left alone ("mamy" is also a noun, so "ona mamy" is
 * missed): a rule that cries wolf gets its findings accepted unread. "ty i ja mamy" is fine.
 */
internal fun pronounVerbFindings(text: String, reference: PolishReference): List<RuleFinding> {
    val words = word.findAll(text).toList()
    return words.indices.mapNotNull { index ->
        val pronoun = words[index].value.lowercase(Locale.ROOT)
        val person = pronouns[pronoun] ?: return@mapNotNull null
        val next = words.getOrNull(index + 1) ?: return@mapNotNull null
        if (text.substring(words[index].range.last + 1, next.range.first).isNotBlank()) return@mapNotNull null
        if (words.getOrNull(index - 1)?.value?.lowercase(Locale.ROOT) == "i") return@mapNotNull null
        val readings = reference.readings(next.value)
        val onlyVerb = readings.isNotEmpty() && readings.all { it.startsWith("verb:fin:") }
        if (onlyVerb && readings.none { ":${person.tag}:" in "$it:" }) {
            RuleFinding(PRONOUN_VERB, "${next.value} is not a form of a verb for $pronoun")
        } else {
            null
        }
    }
}

private fun places(uses: List<PolishUse>): List<Place> = uses.map { Place(it.file, it.id) }.distinct()

/**
 * Runs the four passes over [content]: the dictionary, the verb forms, the grammar rules and the
 * recorded review. A problem named in [accepted] with a reason is listed apart and does not fail
 * the check.
 */
fun checkPolish(content: PolishContent, reference: PolishReference, reviews: Reviews, accepted: Accepted): PolishReport {
    val found = mutableListOf<PolishProblem>()
    val unreviewed = mutableListOf<PolishProblem>()

    // Dictionary
    val unknown = LinkedHashMap<String, MutableList<Place>>()
    val looked = mutableSetOf<String>()
    fun look(text: String, places: List<Place>, inTip: Boolean) {
        word.findAll(text).map { it.value }
            .filter { !inTip || (it.length > 2 && polishLetter.containsMatchIn(it)) }
            .forEach { candidate ->
                looked += candidate
                if (!reference.knows(candidate)) unknown.getOrPut(candidate) { mutableListOf() } += places
            }
    }
    content.sentences.forEach { look(it.text, places(it.uses), inTip = false) }
    content.tips.forEach { look(it.text, listOf(Place(it.file, it.id)), inTip = true) }
    // What a Tip marks as Polish has to be Polish: every word of it, not only those spelt with a Polish letter.
    // A letter, a pair of letters, an ending or a word cut into syllables ("sz", "-my", "do-BRA-noc") is not a
    // word and is not looked up.
    content.tips.forEach { tip ->
        tip.polish.filter { '-' !in it }.forEach { marked ->
            word.findAll(marked).map { it.value }.filter { it.length > 2 }.forEach { candidate ->
                looked += candidate
                if (!reference.knows(candidate)) unknown.getOrPut(candidate) { mutableListOf() } += Place(tip.file, tip.id)
            }
        }
    }
    unknown.forEach { (unknownWord, places) ->
        found += PolishProblem(Pass.DICTIONARY, "dictionary: $unknownWord", "$unknownWord is not in the dictionary", places.distinct())
    }

    // Verb forms
    content.verbForms.forEach { claim ->
        val forms = reference.conjugation(claim.verb, claim.person)
        when {
            forms.isEmpty() -> found += PolishProblem(
                Pass.VERB_FORMS,
                "verb-form: ${claim.verb}",
                "the reference has no conjugation of ${claim.verb}",
                places(claim.uses),
            )
            forms.none { it.lowercase(Locale.ROOT) == claim.form.lowercase(Locale.ROOT) } -> found += PolishProblem(
                Pass.VERB_FORMS,
                "verb-form: ${claim.verb} ${claim.person.written} ${claim.form}",
                "${claim.form} is not the form of ${claim.verb} for ${claim.person.written}; " +
                    "the reference has ${forms.sorted().joinToString(", ")}",
                places(claim.uses),
            )
        }
    }
    // A verb the reference does not know is one problem, however many of its forms the content claims.
    val verbForms = found.filter { it.pass == Pass.VERB_FORMS }.groupBy { it.key }
        .map { (_, same) -> same.first().copy(places = same.flatMap { it.places }.distinct()) }
    found.removeAll { it.pass == Pass.VERB_FORMS }
    found += verbForms

    // Grammar rules
    content.sentences.forEach { sentence ->
        (reference.grammar(sentence.text) + pronounVerbFindings(sentence.text, reference)).distinct().forEach { finding ->
            found += PolishProblem(
                Pass.GRAMMAR,
                "grammar: ${finding.ruleId}: ${sentence.text}",
                "\"${sentence.text}\": ${finding.ruleId}: ${finding.message}",
                places(sentence.uses),
            )
        }
    }

    // Sound gaps: a wrong option must not spell another word, or the Learner would be told a real word is wrong.
    val offered = LinkedHashMap<String, MutableList<Place>>()
    content.soundGaps.forEach { (file, gap) ->
        gap.wrongWords.forEach { wrong -> offered.getOrPut(wrong.lowercase(Locale.ROOT)) { mutableListOf() } += Place(file, gap.itemId) }
    }
    val practiceFile = content.practiceFile ?: "course.json"
    offered.filterKeys { reference.knows(it) }.forEach { (wrong, places) ->
        found += PolishProblem(
            Pass.SOUND_GAPS,
            "sound-gap: $wrong",
            "$wrong is a real word, and Sound gaps would offer it as a wrong spelling; list it under realWords in $practiceFile",
            places.distinct(),
        )
    }
    content.realWords.filter { !reference.knows(it) }.forEach { listed ->
        found += PolishProblem(
            Pass.SOUND_GAPS,
            "real-word: $listed",
            "$listed is listed as a real word but is not in the dictionary; delete it",
            listOf(Place(practiceFile, null)),
        )
    }

    // Review
    val sentencesToReview = mutableListOf<SentenceReview>()
    val tipsToReview = mutableListOf<TipReview>()
    content.sentences.forEach { sentence ->
        val review = reviews.of(sentence)
        val finding = review?.finding?.takeIf { it.isNotBlank() }
        when {
            review == null -> {
                unreviewed += PolishProblem(
                    Pass.REVIEW,
                    "unreviewed: ${sentence.text}",
                    "\"${sentence.text}\" has not been reviewed",
                    places(sentence.uses),
                )
                sentencesToReview += SentenceReview(
                    sentence.text,
                    sentence.meaning,
                    uses = sentence.uses.map { UseEntry(it.file, it.id, it.role.name.lowercase(Locale.ROOT)) },
                )
            }
            finding != null -> found += PolishProblem(Pass.REVIEW, "review: ${sentence.text}", finding, places(sentence.uses))
        }
    }
    content.tips.forEach { tip ->
        val review = reviews.of(tip)
        val finding = review?.finding?.takeIf { it.isNotBlank() }
        when {
            review == null -> {
                unreviewed += PolishProblem(
                    Pass.REVIEW,
                    "unreviewed: ${tip.id}",
                    "Tip ${tip.id} has not been reviewed",
                    listOf(Place(tip.file, tip.id)),
                )
                tipsToReview += TipReview(tip.id, hashOf(tip), text = tip.text)
            }
            finding != null -> found += PolishProblem(Pass.REVIEW, "review: ${tip.id}", finding, listOf(Place(tip.file, tip.id)))
        }
    }

    // Accepted problems. Something unreviewed cannot be accepted; the cure is to review it.
    val keys = found.map { it.key }.toSet()
    val reasons = accepted.accepted.filter { it.reason.isNotBlank() }.associate { it.problem to it.reason }
    val wrongAcceptances = accepted.accepted.mapNotNull { acceptance ->
        val message = when {
            acceptance.reason.isBlank() -> "is accepted with no reason; say why it is left as it is"
            acceptance.problem !in keys -> "accepts a problem that is no longer reported; delete it"
            else -> return@mapNotNull null
        }
        PolishProblem(Pass.ACCEPTANCES, "accepted: ${acceptance.problem}", "\"${acceptance.problem}\" $message", listOf(Place(ACCEPTED_FILE, null)))
    }
    val (acceptedProblems, open) = found.partition { it.key in reasons }

    return PolishReport(
        problems = (open + unreviewed + wrongAcceptances).sortedBy { it.pass },
        accepted = acceptedProblems.map { AcceptedProblem(it, reasons.getValue(it.key)) },
        looked = mapOf(
            Pass.DICTIONARY to looked.size,
            Pass.VERB_FORMS to content.verbForms.size,
            Pass.GRAMMAR to content.sentences.size,
            Pass.REVIEW to content.sentences.size + content.tips.size,
            Pass.SOUND_GAPS to offered.size,
        ),
        toReview = Reviews(sentencesToReview, tipsToReview),
        sentences = content.sentences.size,
        verbForms = content.verbForms.size,
        tips = content.tips.size,
    )
}
