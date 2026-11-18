package com.polisheasy.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Space

/**
 * Tomorrow's forecast card in Forecaster: its [title], the weather [signs] put on it, each tapped
 * to take it off ([takeOffLabel] is what a screen reader says the tap does), and its [temperature]
 * once one is picked, or [noTemperature]. With no signs it says [empty]. Not [enabled], nothing
 * on it responds.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ForecastCard(
    title: String,
    signs: List<PlateFoodView>,
    temperature: String?,
    noTemperature: String,
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
        corner = Corner.extraLarge,
        border = Edge.border,
        flat = true,
        modifier = modifier.fillMaxWidth().heightIn(min = CardHeight),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth().padding(Space.m)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.fillMaxWidth()) {
                if (signs.isEmpty()) {
                    Text(empty, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Space.xs),
                        verticalArrangement = Arrangement.spacedBy(Space.xs),
                        modifier = Modifier.weight(1f),
                    ) {
                        signs.forEachIndexed { index, sign ->
                            Edged(
                                fill = scheme.primaryContainer,
                                edge = scheme.primary,
                                corner = Corner.large,
                                border = Edge.border,
                                enabled = enabled,
                                onClick = { onTakeOff(index) },
                                onClickLabel = takeOffLabel,
                                modifier = Modifier.size(SignSize).semantics { contentDescription = sign.description },
                            ) {
                                if (sign.art == null) {
                                    Text(sign.description, style = MaterialTheme.typography.labelMedium, color = scheme.onPrimaryContainer)
                                } else {
                                    PictureFace(sign.art, Modifier.fillMaxWidth().padding(Space.xxs))
                                }
                            }
                        }
                    }
                }
                Text(
                    temperature ?: noTemperature,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (temperature == null) scheme.outline else scheme.primary,
                )
            }
        }
    }
}

private val CardHeight = 132.dp
private val SignSize = 64.dp
