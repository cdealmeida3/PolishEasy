package com.polisheasy.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

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

/**
 * What changes with the phone's font size besides the text itself (docs/design.md, Accessibility).
 * Components ask here; screens do not.
 */
object Scaled {
    private const val ROOMY_FROM = 1.3f
    private const val ICON_MOST = 1.5f

    /** Whether text is large enough that two things side by side go one under the other. */
    @Composable
    fun roomy(): Boolean = LocalDensity.current.fontScale > ROOMY_FROM

    /** An icon that sits beside text: it grows with the text, up to half as big again. */
    @Composable
    fun icon(size: Dp = Size.icon): Dp = size * LocalDensity.current.fontScale.coerceIn(1f, ICON_MOST)
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

    /** A small round tonal button, such as the speaker beside Polish text, and a setting's badge. */
    val tonalButton: Dp = 40.dp

    val progressBar: Dp = 12.dp

    /** The room kept at the bottom of an Exercise for the feedback panel, so the answers above it never move. */
    val feedbackRoom: Dp = 176.dp

    /** The Streak's flame, on Home and on the Summary. */
    val flame: Dp = 48.dp

    /** Content is never wider than this, and is centred beyond it. */
    val contentMax: Dp = 480.dp
}
