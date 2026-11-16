package com.polisheasy.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import com.polisheasy.app.ui.theme.Scaled

/** What a [PrimaryButton] sits in: on a screen it is the brand's, inside feedback it takes the feedback's colour. */
enum class ButtonTone { Brand, Success, Caution }

/** The one main action of a screen: full width, with the bottom edge. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: ButtonTone = ButtonTone.Brand,
) {
    val colors = PolishEasyTheme.colors
    val scheme = MaterialTheme.colorScheme
    val (fill, edge, label) = when {
        !enabled -> Triple(scheme.surfaceContainerHighest, scheme.surfaceContainerHighest, scheme.onSurfaceVariant)
        tone == ButtonTone.Success -> Triple(colors.success, colors.successEdge, colors.onSuccess)
        tone == ButtonTone.Caution -> Triple(colors.caution, colors.cautionEdge, colors.onCaution)
        else -> Triple(colors.brand, colors.brandEdge, colors.onBrand)
    }
    Edged(
        fill = fill,
        edge = edge,
        corner = Corner.medium,
        flat = !enabled,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        ButtonLabel(text, label)
    }
}

/** A second action, used rarely: outlined, with the bottom edge. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Edged(
        fill = scheme.surfaceContainerLowest,
        edge = scheme.outlineVariant,
        corner = Corner.medium,
        border = Edge.border,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        ButtonLabel(text, scheme.primary)
    }
}

@Composable
private fun ButtonLabel(text: String, color: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = Size.control)
            .padding(horizontal = Space.m, vertical = Space.xs),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color, textAlign = TextAlign.Center)
    }
}

/**
 * A quiet action in words alone: "OK", "Cancel". With [subdued] it is the colour of secondary
 * text, for a way out that is rarely taken, such as "Can't listen now".
 */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, subdued: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = if (subdued) scheme.onSurfaceVariant else scheme.primary),
        modifier = modifier.heightIn(min = Size.touch),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/**
 * A small helper beside a bigger control, such as "Slow" beside the play button: a tonal pill with
 * an [icon] and the word, so it reads as a button without competing with the main one.
 */
@Composable
fun TonalButton(text: String, @DrawableRes icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer,
        shape = CircleShape,
        modifier = modifier.heightIn(min = Size.touch),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = Space.s, end = Space.m, top = Space.xs, bottom = Space.xs),
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(Scaled.icon()))
            Spacer(Modifier.width(Space.xs))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
