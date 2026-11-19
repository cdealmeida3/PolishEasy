package com.polisheasy.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.Scaled
import com.polisheasy.app.ui.theme.inPolish

/** What an [OptionCard] is showing. Each state has an icon or a border as well as a colour. */
enum class OptionState {
    /** Waiting to be chosen. */
    Idle,

    /** Chosen, before "Check", where an Exercise has one. */
    Chosen,

    /** The right answer, marked once the Exercise is answered, whichever option was tapped. */
    Right,

    /** The option the Learner tapped, when it was not the right one. */
    Wrong,

    /** Any other option once the Exercise is answered. */
    Faded,
}

/**
 * An answer to choose. Only an idle or a chosen card can be tapped. [polish] when the answer is
 * Polish, so a screen reader says it with a Polish voice. A [compact] card is for a grid of
 * narrow cells: smaller words, centred, with the tick or cross under them instead of beside.
 */
@Composable
fun OptionCard(text: String, state: OptionState, onClick: () -> Unit, modifier: Modifier = Modifier, polish: Boolean = false, compact: Boolean = false) {
    val colors = PolishEasyTheme.colors
    val scheme = MaterialTheme.colorScheme
    val (fill, edge, label) = when (state) {
        OptionState.Idle, OptionState.Faded -> Triple(scheme.surfaceContainerLowest, scheme.outlineVariant, scheme.onSurface)
        OptionState.Chosen -> Triple(scheme.primaryContainer, scheme.primary, scheme.onPrimaryContainer)
        OptionState.Right -> Triple(colors.successContainer, colors.success, colors.onSuccessContainer)
        OptionState.Wrong -> Triple(colors.cautionContainer, colors.caution, colors.onCautionContainer)
    }
    val tappable = state == OptionState.Idle || state == OptionState.Chosen
    Edged(
        fill = fill,
        edge = edge,
        corner = Corner.medium,
        border = Edge.border,
        enabled = tappable,
        onClick = onClick,
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .shaking(state == OptionState.Wrong)
            .alpha(if (state == OptionState.Faded) Motion.FADED_ALPHA else 1f),
    ) {
        if (compact) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Size.control)
                    .padding(horizontal = Space.xxs, vertical = Space.s),
            ) {
                val style = MaterialTheme.typography.labelMedium
                Text(text, style = if (polish) style.inPolish() else style, color = label, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                when (state) {
                    OptionState.Right -> StateIcon(R.drawable.ic_check_circle, colors.success, below = true)
                    OptionState.Wrong -> StateIcon(R.drawable.ic_cancel, colors.caution, below = true)
                    else -> Unit
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Size.control)
                    .padding(horizontal = Space.m, vertical = Space.s),
            ) {
                val style = MaterialTheme.typography.titleMedium
                Text(text, style = if (polish) style.inPolish() else style, color = label, modifier = Modifier.weight(1f))
                // The feedback panel says the result in words; these mark where it happened.
                when (state) {
                    OptionState.Right -> StateIcon(R.drawable.ic_check_circle, colors.success)
                    OptionState.Wrong -> StateIcon(R.drawable.ic_cancel, colors.caution)
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun StateIcon(drawable: Int, tint: Color, below: Boolean = false) {
    Spacer(if (below) Modifier.height(Space.xs) else Modifier.width(Space.s))
    Icon(
        painterResource(drawable),
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .size(Scaled.icon())
            .appearing(),
    )
}
