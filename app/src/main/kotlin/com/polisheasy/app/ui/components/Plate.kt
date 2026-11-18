package com.polisheasy.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Space

/** A food on the plate of Plate builder: its picture, and what a screen reader says of it, [description]. */
class PlateFoodView(val art: PictureArt?, val description: String)

/**
 * The plate of Plate builder: a wide round dish holding each food put on it, in the order it came,
 * tapped to take it off ([takeOffLabel] is what a screen reader says the tap does). Empty, it says
 * [empty]. Not [enabled], nothing on it responds.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlateView(
    foods: List<PlateFoodView>,
    empty: String,
    takeOffLabel: String,
    onTakeOff: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Edged(
        fill = scheme.surfaceContainerLowest,
        edge = scheme.outlineVariant,
        corner = PlateCorner,
        border = Edge.border,
        flat = true,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PlateHeight),
    ) {
        if (foods.isEmpty()) {
            Text(
                empty,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(Space.m),
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.xs, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(Space.xs, Alignment.CenterVertically),
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(Space.m),
            ) {
                foods.forEachIndexed { index, food ->
                    Edged(
                        fill = scheme.primaryContainer,
                        edge = scheme.primary,
                        corner = FoodCorner,
                        border = Edge.border,
                        enabled = enabled,
                        onClick = { onTakeOff(index) },
                        onClickLabel = takeOffLabel,
                        modifier = Modifier.size(FoodSize).semantics { contentDescription = food.description },
                    ) {
                        Box(Modifier.padding(Space.xxs)) {
                            if (food.art == null) {
                                Text(food.description, style = MaterialTheme.typography.labelMedium, color = scheme.onPrimaryContainer, textAlign = TextAlign.Center)
                            } else {
                                PictureFace(food.art, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A plate is wide and round-ended, like a dish seen from the side of the table. */
private val PlateCorner = 72.dp
private val PlateHeight = 144.dp
private val FoodSize = 72.dp
private val FoodCorner = 36.dp
