package com.polisheasy.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import com.polisheasy.app.ui.theme.Motion
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
import androidx.compose.ui.draw.drawBehind

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
            .dashedOutline(Corner.small)
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

/**
 * The two halves of a word tiles Exercise: the answer being built, on ruled lines that show where
 * the tiles go, and below it the bank of [words]. [placed] is the bank positions of the tiles in the answer, in order. A tile travels
 * between its slot in the bank and its place in the answer, and its slot stays behind, so the bank
 * never shifts. [answerDescription] is what a screen reader calls the answer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordTileBoard(
    words: List<String>,
    placed: List<Int>,
    enabled: Boolean,
    onPlace: (Int) -> Unit,
    onTakeBack: (Int) -> Unit,
    answerDescription: String,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberAnswerHaptics()
    // Where each tile last sat in the bank and in the answer, so it can set off from there.
    val inBank = remember { mutableMapOf<Int, Offset>() }
    val inAnswer = remember { mutableMapOf<Int, Offset>() }
    val rule = MaterialTheme.colorScheme.outlineVariant
    Column(modifier = modifier) {
        // Tall enough for two rows of tiles from the start, so the bank below does not jump as tiles move.
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (WordTileHeight + Space.s) * 2)
                .zIndex(1f)
                // One ruled line under each row of the answer, there before the first tile is.
                .drawBehind {
                    val row = (WordTileHeight + Space.s).toPx()
                    var y = row - Edge.border.toPx() / 2
                    while (y <= size.height) {
                        drawLine(rule, Offset(0f, y), Offset(size.width, y), Edge.border.toPx())
                        y += row
                    }
                }
                .semantics { contentDescription = answerDescription },
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            placed.forEach { tile ->
                key(tile) {
                    Travelling(from = { inBank[tile] }, onPlaced = { inAnswer[tile] = it }) {
                        WordTile(words[tile], enabled, onClick = { onTakeBack(tile) })
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.l))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
            verticalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            words.forEachIndexed { tile, word ->
                if (tile in placed) {
                    EmptyTileSlot(word, Modifier.onGloballyPositioned { inBank[tile] = it.positionInRoot() })
                } else {
                    // A tile that was never in the answer has nowhere to come from, and stays put.
                    Travelling(from = { inAnswer.remove(tile) }, onPlaced = { inBank[tile] = it }) {
                        WordTile(
                            word,
                            enabled,
                            onClick = {
                                haptics.tick()
                                onPlace(tile)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws [content] setting off from where [from] says it was and arriving where it now belongs.
 * [onPlaced] is told that place. The layout is not touched, only where it is drawn.
 */
@Composable
private fun Travelling(from: () -> Offset?, onPlaced: (Offset) -> Unit, content: @Composable () -> Unit) {
    val spec = Motion.tile()
    // How much of the way is still to go, from 1 to 0.
    val left = remember { Animatable(1f) }
    var way by remember { mutableStateOf<Offset?>(null) }
    var set by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { left.animateTo(0f, spec) }
    Box(
        modifier = Modifier
            .zIndex(if (left.value != 0f) 1f else 0f)
            .onGloballyPositioned { coordinates ->
                val here = coordinates.positionInRoot()
                if (!set) {
                    set = true
                    way = from()?.minus(here)
                }
                onPlaced(here)
            },
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                val start = way ?: Offset.Zero
                translationX = start.x * left.value
                translationY = start.y * left.value
            },
        ) { content() }
    }
}
