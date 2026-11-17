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

    /** The room kept at the bottom of a Round's step for "Continue", so the answers above it never move. */
    val roundActionRoom: Dp = 84.dp

    /** The picture or icon of a Game on its card, and larger at the top of the Game's own screen. */
    val gameIcon: Dp = 48.dp
    val gameIconHero: Dp = 72.dp

    /** The least height of a Picture card, so the picture has room and a turned card holds its words. */
    val pictureTile: Dp = 168.dp

    /** The least height of letters to choose for a gap. */
    val letterOption: Dp = 88.dp

    /** The least height of a word to sort a noun under, when the words are side by side. */
    val bucket: Dp = 104.dp

    /** The least height of a picture to choose as an answer. */
    val pictureOption: Dp = 120.dp

    /** The least and the most height of a tile of Memory pairs: a board fills the screen between the two. */
    val memoryTileMin: Dp = 64.dp
    val memoryTileMax: Dp = 160.dp

    /** The tick in the corner of a tile whose pair was found: small, so it stays clear of the tile's words. */
    val tileMark: Dp = 18.dp

    /** The Streak's flame on Home, and the larger one of the Summary, where the Streak going up is the hero. */
    val flame: Dp = 48.dp
    val flameHero: Dp = 72.dp

    /** Content is never wider than this, and is centred beyond it. */
    val contentMax: Dp = 480.dp

    /** The widest a chat bubble grows, so the two sides of a conversation stay apart. */
    val bubbleMax: Dp = 300.dp
}
