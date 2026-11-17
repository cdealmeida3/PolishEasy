package com.polisheasy.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Who a verb form is for. Written in content as the Polish pronoun. */
@Serializable
enum class Person(/** The Polish pronouns, as shown to the Learner. */ val pronouns: String) {
    @SerialName("ja") I("ja"),
    @SerialName("ty") YOU("ty"),
    @SerialName("on") HE_SHE_IT("on / ona / ono"),
    @SerialName("my") WE("my"),
    @SerialName("wy") YOU_PLURAL("wy"),
    @SerialName("oni") THEY("oni / one"),
}

/** A verb's form for every person, shown with a Tip. [verb] is the dictionary form. */
@Serializable
data class ConjugationTable(val verb: String, val meaning: Localized, val forms: Map<Person, String>) {
    /**
     * The book's conjugation the verb follows, named by its endings for *ja* and *ty*, as the book
     * heads its tables: "-ę, -isz/-ysz" (*mówić*, *uczyć się*), "-am, -asz" (*mieszkać*), "-ę, -esz"
     * (*dziękować*), "-em, -esz" (*jeść*). Null for a verb that follows none, such as *być*.
     */
    val group: String?
        get() {
            val ja = forms[Person.I]?.removeSuffix(REFLEXIVE) ?: return null
            val ty = forms[Person.YOU]?.removeSuffix(REFLEXIVE) ?: return null
            return when {
                ja.endsWith("ę") && (ty.endsWith("isz") || ty.endsWith("ysz")) -> "-ę, -isz/-ysz"
                ja.endsWith("ę") && ty.endsWith("esz") -> "-ę, -esz"
                ja.endsWith("am") && ty.endsWith("asz") -> "-am, -asz"
                ja.endsWith("em") && ty.endsWith("esz") -> "-em, -esz"
                else -> null
            }
        }

    private companion object {
        const val REFLEXIVE = " się"
    }
}

/** What marks the gap in the sentence of a conjugation Exercise. */
internal const val GAP = "___"

private val underscores = Regex("_+")

/**
 * The text before and after the gap of [sentence], spaces kept, or null unless it has exactly one
 * gap: a single run of underscores, three long.
 */
internal fun gapOf(sentence: String): Pair<String, String>? {
    val run = underscores.findAll(sentence).singleOrNull()?.takeIf { it.value == GAP } ?: return null
    return sentence.substring(0, run.range.first) to sentence.substring(run.range.last + 1)
}

/** [sentence] with [form] in its gap, or unchanged if it does not have exactly one gap. */
internal fun filled(sentence: String, form: String): String =
    gapOf(sentence)?.let { (before, after) -> before + form + after } ?: sentence
