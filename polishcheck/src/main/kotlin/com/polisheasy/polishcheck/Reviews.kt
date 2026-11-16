package com.polisheasy.polishcheck

import com.polisheasy.engine.PolishContent
import com.polisheasy.engine.PolishSentence
import com.polisheasy.engine.TipText
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Where the review verdicts are recorded, at the repository root. */
const val REVIEWS_FILE = "polish-reviews.json"

/** Where accepted problems are recorded, at the repository root. */
const val ACCEPTED_FILE = "polish-accepted.json"

/** A place a sentence is written, as `to-review.json` shows it to the reviewer. */
@Serializable
data class UseEntry(val file: String, val id: String, val role: String)

/**
 * The reviewer's verdict on one Polish text and the English it stands for. No [finding] means it
 * is right. [uses] is only for the reviewer's context and is never recorded.
 */
@Serializable
data class SentenceReview(
    val text: String,
    val meaning: String? = null,
    val finding: String? = null,
    val uses: List<UseEntry>? = null,
)

/**
 * The reviewer's verdict on one Tip. [hash] ties it to the Tip's text as it was reviewed. [text]
 * is only for the reviewer's context and is never recorded.
 */
@Serializable
data class TipReview(val id: String, val hash: String, val finding: String? = null, val text: String? = null)

@Serializable
data class Reviews(val sentences: List<SentenceReview> = emptyList(), val tips: List<TipReview> = emptyList()) {
    private val bySentence = sentences.associateBy { it.text to it.meaning }
    private val byTip = tips.associateBy { it.id to it.hash }

    /** The verdict on [sentence], or null if it has not been reviewed with this meaning. */
    fun of(sentence: PolishSentence): SentenceReview? = bySentence[sentence.text to sentence.meaning]

    /** The verdict on [tip], or null if it has not been reviewed as it now reads. */
    fun of(tip: TipText): TipReview? = byTip[tip.id to hashOf(tip)]

    /**
     * These verdicts with those of [reviewed] added, each replacing an earlier verdict on the same
     * sentence or Tip, and without any that [content] no longer has. Sorted, and without the
     * reviewer's context.
     */
    fun with(reviewed: Reviews, content: PolishContent): Reviews {
        val sentenceKeys = content.sentences.map { it.text to it.meaning }.toSet()
        val tipKeys = content.tips.map { it.id to hashOf(it) }.toSet()
        return Reviews(
            (bySentence + reviewed.bySentence).filterKeys { it in sentenceKeys }.values
                .map { it.copy(uses = null, finding = it.finding?.takeIf(String::isNotBlank)) }
                .sortedWith(compareBy({ it.text }, { it.meaning.orEmpty() })),
            (byTip + reviewed.byTip).filterKeys { it in tipKeys }.values
                .map { it.copy(text = null, finding = it.finding?.takeIf(String::isNotBlank)) }
                .sortedBy { it.id },
        )
    }

    /** What each entry that matches nothing in [content] is, for telling the reviewer. */
    fun strangersTo(content: PolishContent): List<String> {
        val sentenceKeys = content.sentences.map { it.text to it.meaning }.toSet()
        val tipKeys = content.tips.map { it.id to hashOf(it) }.toSet()
        return bySentence.keys.filter { it !in sentenceKeys }.map { (text, meaning) ->
            if (meaning == null) "\"$text\"" else "\"$text\" meaning \"$meaning\""
        } + byTip.keys.filter { it !in tipKeys }.map { (id, hash) -> "Tip $id with hash $hash" }
    }
}

/** A problem the check reports that is known and left as it is, and why. */
@Serializable
data class Acceptance(val problem: String, val reason: String = "")

@Serializable
data class Accepted(val accepted: List<Acceptance> = emptyList())

/** How both files, and `to-review.json`, are written and read. */
internal val reviewJson = Json {
    prettyPrint = true
    explicitNulls = false
}

/** The first 12 hex digits of the SHA-256 of the Tip's text: editing a Tip sends it back to review. */
fun hashOf(tip: TipText): String =
    MessageDigest.getInstance("SHA-256").digest(tip.text.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }.take(12)
