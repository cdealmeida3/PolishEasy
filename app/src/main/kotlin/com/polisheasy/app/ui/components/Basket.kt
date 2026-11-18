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
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Space

/**
 * The basket of Market list: a deep crate holding one picture for each good put in it, the same
 * goods side by side, so two pears are two pears. A tap on one takes it out ([takeOutLabel] is
 * what a screen reader says the tap does). Empty, it says [empty]. Not [enabled], nothing in it
 * responds. It takes [PlateFoodView]s, as the plate does.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BasketView(
    goods: List<PlateFoodView>,
    empty: String,
    takeOutLabel: String,
    onTakeOut: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Edged(
        fill = scheme.surfaceContainer,
        edge = scheme.outline,
        corner = Corner.large,
        border = Edge.border,
        flat = true,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BasketHeight),
    ) {
        if (goods.isEmpty()) {
            Text(
                empty,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(Space.m),
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.xxs, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(Space.xxs, Alignment.CenterVertically),
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(Space.s),
            ) {
                goods.forEachIndexed { index, good ->
                    Edged(
                        fill = scheme.surfaceContainerLowest,
                        edge = scheme.outlineVariant,
                        corner = Corner.medium,
                        border = Edge.hairline,
                        enabled = enabled,
                        onClick = { onTakeOut(index) },
                        onClickLabel = takeOutLabel,
                        modifier = Modifier.size(GoodSize).semantics { contentDescription = good.description },
                    ) {
                        Box(Modifier.padding(Space.xxs)) {
                            if (good.art == null) {
                                Text(good.description, style = MaterialTheme.typography.labelSmall, color = scheme.onSurface, textAlign = TextAlign.Center)
                            } else {
                                PictureFace(good.art, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A basket is deep, and holds up to eight goods in a row or two at any width. */
private val BasketHeight = 128.dp
private val GoodSize = 64.dp
