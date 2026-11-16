package com.polisheasy.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Motion

/**
 * The shape buttons, option cards and word tiles share: a flat face in [fill] with a darker bottom
 * edge in [edge] and no shadow. Pressing it moves the face down until the edge is gone, which is
 * what makes a tap feel physical. With a [border] the edge colour also outlines the other sides.
 *
 * Without [onClick], or when not [enabled], it does not respond. With [flat] it has no edge and
 * keeps the room for one, so a control does not change size when it is disabled.
 */
@Composable
internal fun Edged(
    fill: Color,
    edge: Color,
    corner: Dp,
    modifier: Modifier = Modifier,
    border: Dp = 0.dp,
    flat: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // What shows below the face: the whole edge, less the border the face already has.
    val travel = Edge.depth - border
    val sunk by animateDpAsState(
        targetValue = if (pressed && !flat) travel else 0.dp,
        animationSpec = Motion.press(),
        label = "Pressed",
    )
    val click = if (onClick == null) {
        Modifier
    } else {
        // No ripple: the face moving is the response.
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = Role.Button,
            onClick = onClick,
        )
    }
    Box(
        modifier = modifier
            .then(click)
            .drawBehind {
                val down = sunk.toPx()
                val line = border.toPx()
                val radius = corner.toPx()
                val faceHeight = size.height - travel.toPx()
                if (!flat || line > 0f) {
                    // The edge and the border are one shape, the face's outline stretched down.
                    val bottom = if (flat) faceHeight else size.height
                    drawRoundRect(
                        color = edge,
                        topLeft = Offset(0f, down),
                        size = Size(size.width, bottom - down),
                        cornerRadius = CornerRadius(radius),
                    )
                }
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(line, down + line),
                    size = Size(size.width - 2 * line, faceHeight - 2 * line),
                    cornerRadius = CornerRadius((radius - line).coerceAtLeast(0f)),
                )
            }
            .padding(bottom = travel)
            .offset(y = sunk),
        contentAlignment = contentAlignment,
        content = content,
    )
}
