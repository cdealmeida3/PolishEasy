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
data class ConjugationTable(val verb: String, val meaning: Localized, val forms: Map<Person, String>)

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
