package com.polisheasy.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The only steps of space between things: a 4 dp grid (docs/design.md, Space). */
object Space {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val s: Dp = 12.dp
    val m: Dp = 16.dp
    val l: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp

    /** Screen edge to content, at the sides. */
    val screenSide: Dp = l

    /** Below the top bar. */
    val screenTop: Dp = m

    /** Above the bottom edge or the navigation bar. */
    val screenBottom: Dp = l
}

/** Sizes that more than one component shares. */
object Size {
    /** The least a finger can tap, icons included. */
    val touch: Dp = 48.dp

    /** A button, an option card. */
    val control: Dp = 56.dp

    /** A word tile, a key of the Polish key row. */
    val tile: Dp = 48.dp

    val icon: Dp = 24.dp

    /** The tick or cross of the feedback panel. */
    val feedbackIcon: Dp = 32.dp

    /** The play button of a listening Exercise, and the speaker inside it. */
    val playButton: Dp = 72.dp
    val playIcon: Dp = 36.dp

    val progressBar: Dp = 12.dp

    /** Content is never wider than this, and is centred beyond it. */
    val contentMax: Dp = 480.dp
}
