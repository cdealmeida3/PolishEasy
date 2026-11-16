package com.polisheasy.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The corner of each shape token, for the components that draw their own outline. */
object Corner {
    val extraSmall: Dp = 8.dp
    val small: Dp = 12.dp
    val medium: Dp = 16.dp
    val large: Dp = 20.dp
    val extraLarge: Dp = 28.dp
}

internal val PolishEasyShapes = Shapes(
    extraSmall = RoundedCornerShape(Corner.extraSmall),
    small = RoundedCornerShape(Corner.small),
    medium = RoundedCornerShape(Corner.medium),
    large = RoundedCornerShape(Corner.large),
    extraLarge = RoundedCornerShape(Corner.extraLarge),
)

/** The bottom edge of buttons, option cards and word tiles (docs/design.md, Shape). */
object Edge {
    /** How deep the bottom edge is, and how far a press moves the control down. */
    val depth: Dp = 4.dp

    /** The border of an outlined card or tile on its other three sides. */
    val border: Dp = 2.dp

    /** The border of a plain card. */
    val hairline: Dp = 1.dp
}
