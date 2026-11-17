package com.polisheasy.engine

import kotlinx.serialization.Serializable

/**
 * What stands for an Item in the Games of Practice (ADR-0011): an image packaged with the course,
 * or a numeral or sign the app draws in its own typeface. Daily Sets have no pictures.
 */
sealed interface Picture {
    /** An image file; [path] is relative to the content root. */
    data class Image(val path: String) : Picture

    /** A numeral or sign, such as "7" or "+", drawn as text. */
    data class Glyph(val text: String) : Picture
}

/** One line of the file that lists the pictures: the Item and exactly one of [image], a file beside the list, or [glyph]. */
@Serializable
internal data class PictureEntry(val item: String, val image: String? = null, val glyph: String? = null)

@Serializable
internal data class PicturesFile(val pictures: List<PictureEntry>)

/** The picture [entry] names, with an image's path from the content root, where the list is in [dir]. Null if it names none. */
internal fun pictureOf(entry: PictureEntry, dir: String): Picture? = when {
    entry.image != null -> Picture.Image(if (dir.isEmpty()) entry.image else "$dir/${entry.image}")
    entry.glyph != null -> Picture.Glyph(entry.glyph)
    else -> null
}
