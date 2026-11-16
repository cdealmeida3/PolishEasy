package com.polisheasy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space

/** The height a row of word tiles takes, bottom edge included. */
val WordTileHeight: Dp = Size.tile + Edge.depth - Edge.border

/** A word to place in the answer, or to take back from it. */
@Composable
fun WordTile(word: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Edged(
        fill = scheme.secondaryContainer,
        edge = scheme.outlineVariant,
        corner = Corner.small,
        border = Edge.border,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    ) {
        TileWord(word, Modifier)
    }
}

/** Where a tile now in the answer came from: the same size as the tile, so the rest of the bank does not shift. */
@Composable
fun EmptyTileSlot(word: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(bottom = Edge.depth - Edge.border)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
            .clearAndSetSemantics { },
    ) {
        TileWord(word, Modifier.alpha(0f))
    }
}

@Composable
private fun TileWord(word: String, modifier: Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = Size.tile)
            .padding(horizontal = Space.m),
    ) {
        Text(
            word,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = modifier,
        )
    }
}
