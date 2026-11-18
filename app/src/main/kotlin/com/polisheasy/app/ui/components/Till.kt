package com.polisheasy.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space

/** A coin or a note of Kiosk till: what it says on it, [label] ("50 gr", "20 zł"), and whether it is a [note]. */
class MoneyView(val label: String, val note: Boolean)

/**
 * The coins and notes to pay with: the coins as round keys three across, then the notes as wide
 * ones. Not [enabled], every key fades and none responds.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoneyKeys(money: List<MoneyView>, onPick: (Int) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Space.xs),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else Motion.FADED_ALPHA),
    ) {
        val (coins, notes) = money.withIndex().partition { !it.value.note }
        listOf(coins, notes).filter { it.isNotEmpty() }.forEach { kind ->
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.xs, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(Space.xs),
                modifier = Modifier.fillMaxWidth(),
            ) {
                kind.forEach { (index, view) -> MoneyKey(view, enabled) { onPick(index) } }
            }
        }
    }
}

@Composable
private fun MoneyKey(view: MoneyView, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Edged(
        fill = if (view.note) scheme.secondaryContainer else scheme.tertiaryContainer,
        edge = if (view.note) scheme.secondary else scheme.tertiary,
        corner = if (view.note) Corner.small else CoinCorner,
        border = Edge.border,
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = Size.control)
            .widthIn(min = if (view.note) NoteWidth else Size.control),
    ) {
        Text(
            view.label,
            style = MaterialTheme.typography.titleMedium,
            color = if (view.note) scheme.onSecondaryContainer else scheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = Space.xs),
        )
    }
}

/**
 * What is on the counter: each coin or note put there, small, in the order it came, tapped to take
 * it back ([takeBackLabel] is what a screen reader says the tap does). Empty, it says [empty].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoneyCounter(paid: List<MoneyView>, empty: String, takeBackLabel: String, onTakeBack: (Int) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Space.xxs, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(Space.xxs),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Size.touch),
    ) {
        if (paid.isEmpty()) {
            Text(empty, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterVertically))
        }
        paid.forEachIndexed { index, view ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = Size.touch)
                    .let { if (view.note) it else it.widthIn(min = Size.touch) },
            ) {
                Edged(
                    fill = if (view.note) scheme.secondaryContainer else scheme.tertiaryContainer,
                    edge = if (view.note) scheme.secondary else scheme.tertiary,
                    corner = if (view.note) Corner.small else CoinCorner,
                    border = Edge.border,
                    enabled = enabled,
                    onClick = { onTakeBack(index) },
                    onClickLabel = takeBackLabel,
                    modifier = Modifier
                        .heightIn(min = CounterCoin)
                        .widthIn(min = CounterCoin),
                ) {
                    Text(
                        view.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (view.note) scheme.onSecondaryContainer else scheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = Space.xs),
                    )
                }
            }
        }
    }
}

/** Round enough at any height to read as a coin. */
private val CoinCorner = 28.dp
private val NoteWidth = 96.dp
private val CounterCoin = 40.dp
