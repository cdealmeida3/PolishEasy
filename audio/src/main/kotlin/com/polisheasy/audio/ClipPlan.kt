package com.polisheasy.audio

import com.polisheasy.engine.Clip
import com.polisheasy.engine.audioKey

/** Why a Clip is to be made. */
enum class Reason {
    /** A text the course speaks that has no Clip. */
    NEW,

    /** A Clip whose file is not there. */
    NO_FILE,

    /** A Clip whose file was made from other text or other voice settings, or by nobody knows what. */
    STALE,
}

data class ClipToMake(val clip: Clip, val reason: Reason)

/** What one run of the command would do. */
data class ClipPlan(
    /** Clips whose files are right as they are. */
    val keep: List<Clip>,
    val make: List<ClipToMake>,
    /** Clips that say something the course no longer speaks. */
    val remove: List<Clip>,
    /** Audio files beside the manifest, by Clip id, that no Clip names. Those of [remove] are not repeated here. */
    val strays: List<String>,
) {
    /** How many characters would be sent to the voice service. */
    val characters: Int get() = make.sumOf { it.clip.text.codePointCount(0, it.clip.text.length) }

    val nothingToDo: Boolean get() = make.isEmpty() && remove.isEmpty() && strays.isEmpty()
}

private val plainLetters = "ąćęłńóśźż".zip("acelnoszz").toMap()
private val notSlug = Regex("[^a-z0-9]+")
private const val SLUG_LENGTH = 48

/** An id for a new Clip of [text]: its letters without marks, hyphenated, and numbered if [taken]. */
internal fun newClipId(text: String, taken: Set<String>): String {
    val plain = audioKey(text).map { plainLetters[it] ?: it }.joinToString("")
    val slug = plain.replace(notSlug, "-").trim('-').take(SLUG_LENGTH).trim('-').ifEmpty { "clip" }
    return generateSequence(1) { it + 1 }.map { if (it == 1) slug else "$slug-$it" }.first { it !in taken }
}

/**
 * Decides what to do with every Clip. [spokenTexts] is what the course speaks, [manifest] the
 * Clips listed today, [recordings] the fingerprint recorded for each Clip id when its file was
 * made, [files] the ids that have an audio file, and [fingerprint] what the current settings give
 * a text. A Clip that stays keeps its id, whatever happens to its file.
 */
fun planClips(
    spokenTexts: List<String>,
    manifest: List<Clip>,
    recordings: Map<String, String>,
    files: Set<String>,
    fingerprint: (text: String) -> String,
): ClipPlan {
    val spoken = spokenTexts.associateBy(::audioKey)
    val (staying, remove) = manifest.partition { audioKey(it.text) in spoken }

    val keep = mutableListOf<Clip>()
    val make = mutableListOf<ClipToMake>()
    for (clip in staying) {
        when {
            clip.id !in files -> make += ClipToMake(clip, Reason.NO_FILE)
            recordings[clip.id] != fingerprint(clip.text) -> make += ClipToMake(clip, Reason.STALE)
            else -> keep += clip
        }
    }

    val have = staying.map { audioKey(it.text) }.toSet()
    val taken = staying.map { it.id }.toMutableSet()
    for ((key, text) in spoken) {
        if (key in have) continue
        val clip = Clip(newClipId(text, taken), text)
        taken += clip.id
        make += ClipToMake(clip, Reason.NEW)
    }

    val listed = manifest.map { it.id }.toSet()
    return ClipPlan(keep, make, remove, strays = (files - listed).sorted())
}
