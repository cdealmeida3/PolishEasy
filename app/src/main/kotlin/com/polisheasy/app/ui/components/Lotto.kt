package com.polisheasy.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space

/** What a number on a Lotto card is showing. Each state has a mark as well as a colour. */
enum class LottoState {
    /** Not ticked, and can be. */
    Open,

    /** Ticked for a number called before. */
    Ticked,

    /** The number called now, once the call is answered. */
    Right,

    /** The number tapped for this call, when it was not the one called. */
    Wrong,
}

/** A number on a Lotto card: its [text], its [state], and what a screen reader says of a ticked one, [ticked]. */
class LottoNumberView(val text: String, val state: LottoState, val ticked: String? = null)

/**
 * A Lotto card: its numbers three across, as large tiles. A ticked one keeps its tick, the number
 * called is marked right once answered, and a wrong tap a cross and one shake. Not [enabled], the
 * open numbers fade and none responds.
 */
@Composable
fun LottoCard(numbers: List<LottoNumberView>, onTick: (Int) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs), modifier = modifier.fillMaxWidth()) {
        numbers.withIndex().chunked(ACROSS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (index, number) -> LottoTile(number, enabled, { onTick(index) }, Modifier.weight(1f)) }
                repeat(ACROSS - row.size) { Row(Modifier.weight(1f)) {} }
            }
        }
    }
}

@Composable
private fun LottoTile(number: LottoNumberView, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = PolishEasyTheme.colors
    val scheme = MaterialTheme.colorScheme
    val (fill, edge, label) = when (number.state) {
        LottoState.Open -> Triple(scheme.surfaceContainerLowest, scheme.outlineVariant, scheme.onSurface)
        LottoState.Ticked -> Triple(scheme.primaryContainer, scheme.primary, scheme.onPrimaryContainer)
        LottoState.Right -> Triple(colors.successContainer, colors.success, colors.onSuccessContainer)
        LottoState.Wrong -> Triple(colors.cautionContainer, colors.caution, colors.onCautionContainer)
    }
    val tappable = enabled && number.state == LottoState.Open
    Edged(
        fill = fill,
        edge = edge,
        corner = Corner.medium,
        border = Edge.border,
        enabled = tappable,
        onClick = onClick,
        modifier = modifier
            .heightIn(min = Size.letterOption)
            .shaking(number.state == LottoState.Wrong)
            .alpha(if (!enabled && number.state == LottoState.Open) Motion.FADED_ALPHA else 1f)
            .semantics { number.ticked?.let { stateDescription = it } },
    ) {
        Text(number.text, style = MaterialTheme.typography.headlineSmall, color = label, modifier = Modifier.padding(Space.xs))
        when (number.state) {
            LottoState.Ticked, LottoState.Right -> MarkIcon(Mark.Right, Modifier.align(Alignment.TopEnd).padding(Space.xxs), size = Size.tileMark)
            LottoState.Wrong -> MarkIcon(Mark.Missed, Modifier.align(Alignment.TopEnd).padding(Space.xxs), size = Size.tileMark)
            LottoState.Open -> Unit
        }
    }
}

private const val ACROSS = 3
