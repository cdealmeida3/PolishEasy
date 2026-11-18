package com.polisheasy.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Space

/** One place of a [StoryStrip]: the picture put in it, with what it shows, or nothing yet. */
data class StripPlace(val art: PictureArt?, val description: String?)

/**
 * The strip of a story, its places side by side and numbered from 1: each empty place shows its
 * number, the one to fill next outlined in the brand's colour, and a filled one its picture.
 * [placeLabel] is what a screen reader says for an empty place, given its number.
 */
@Composable
fun StoryStrip(places: List<StripPlace>, next: Int?, placeLabel: (Int) -> String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = modifier.fillMaxWidth()) {
        places.forEachIndexed { index, place ->
            val filled = place.description != null
            Edged(
                fill = if (filled) scheme.surfaceContainerLowest else scheme.surfaceContainer,
                edge = if (index == next) scheme.primary else scheme.outlineVariant,
                corner = Corner.medium,
                border = Edge.border,
                flat = !filled,
                modifier = Modifier.weight(1f).aspectRatio(1f)
                    .semantics { contentDescription = place.description ?: placeLabel(index + 1) },
            ) {
                when {
                    place.art != null -> PictureFace(place.art, Modifier.fillMaxWidth().padding(Space.xxs))
                    filled -> Text(checkNotNull(place.description), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(Space.xxs))
                    else -> Text(
                        (index + 1).toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (index == next) scheme.primary else scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
