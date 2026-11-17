package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.MemoryTile
import com.polisheasy.app.ui.components.MemoryTileFace
import com.polisheasy.app.ui.components.MemoryTileLook
import com.polisheasy.app.ui.components.rememberAnswerHaptics
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.MemoryFace
import com.polisheasy.engine.MemoryRound
import com.polisheasy.engine.MemoryTileState
import com.polisheasy.engine.Round
import kotlinx.coroutines.delay

/**
 * The board of Memory pairs: every tile face down, three across, filling the screen. A tile turns
 * up when tapped, and Polish is said as it turns. A pair stays up with a tick; two that are not a
 * pair turn back after a moment, or at once when another tile is tapped. The finished board stands
 * for a moment before [onFinish] ends the Round. With [sound], a turned tile says its Polish again
 * when tapped.
 */
@Composable
internal fun MemoryPairsBoard(round: MemoryRound, sound: Boolean, say: (String) -> Unit, onMove: (Round) -> Unit, onFinish: () -> Unit) {
    val haptics = rememberAnswerHaptics()
    LaunchedEffect(round.up, round.turns) {
        if (round.up.size == 2) {
            delay(Motion.MISS_MILLIS)
            onMove(round.settle())
        }
    }
    LaunchedEffect(round.finished) {
        if (round.finished) {
            delay(Motion.BOARD_DONE_MILLIS)
            onFinish()
        }
    }
    val turn: (Int) -> Unit = { tile ->
        val next = round.turn(tile)
        val found = next.found.size > round.found.size
        when {
            found -> haptics.right()
            next.up.size == 2 -> haptics.wrong()
            else -> haptics.tick()
        }
        // The tile's own Polish as it turns; a pair found by its picture or its meaning is said too.
        (round.tiles[tile].face.polish() ?: round.polishOf(round.tiles[tile].pair).takeIf { found })?.let(say)
        onMove(next)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .widthIn(max = Size.contentMax)
            .padding(start = Space.screenSide, end = Space.screenSide, top = Space.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Instruction(stringResource(R.string.memory_instruction), Modifier.weight(1f))
            Text(
                pluralStringResource(R.plurals.practice_turns, round.turns, round.turns),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Space.s))
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val rows = (round.tiles.size + ACROSS - 1) / ACROSS
            // The rows share the height there is, within what a tile may be. At a large font size the board scrolls.
            val each = ((maxHeight - Space.screenBottom - Space.xs * (rows - 1)) / rows).coerceIn(Size.memoryTileMin, Size.memoryTileMax)
            Column(
                verticalArrangement = Arrangement.spacedBy(Space.xs),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = Space.screenBottom),
            ) {
                round.tiles.indices.chunked(ACROSS).forEach { row ->
                    // The tiles of a row are as tall as the tallest of them.
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        row.forEach { tile ->
                            Tile(round, tile, sound, say, turn, Modifier.weight(1f).fillMaxHeight().heightIn(min = each))
                        }
                        repeat(ACROSS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

private const val ACROSS = 3

@Composable
private fun Tile(round: MemoryRound, tile: Int, sound: Boolean, say: (String) -> Unit, turn: (Int) -> Unit, modifier: Modifier) {
    val face = round.tiles[tile].face
    val state = round.stateOf(tile)
    val polish = face.polish()
    val turns = state == MemoryTileState.DOWN || state == MemoryTileState.MISSED
    MemoryTile(
        look = when (state) {
            MemoryTileState.DOWN -> MemoryTileLook.Down
            MemoryTileState.UP -> MemoryTileLook.Up
            MemoryTileState.MISSED -> MemoryTileLook.Missed
            MemoryTileState.FOUND -> MemoryTileLook.Found
        },
        face = when (face) {
            is MemoryFace.Image -> MemoryTileFace.Picture(artOf(face.picture), face.meaning)
            is MemoryFace.Word -> MemoryTileFace.Words(face.polish, polish = true)
            is MemoryFace.Meaning -> MemoryTileFace.Words(face.text, polish = false)
            is MemoryFace.Sound -> MemoryTileFace.Sound
        },
        onTap = when {
            turns -> { { turn(tile) } }
            // A tile that is up says its Polish again.
            polish != null && sound -> { { say(polish) } }
            else -> null
        },
        tapLabel = stringResource(if (turns) R.string.memory_turn else R.string.memory_hear_again),
        description = when {
            state == MemoryTileState.DOWN -> stringResource(R.string.memory_tile, tile + 1)
            face is MemoryFace.Image -> face.meaning
            face is MemoryFace.Sound -> stringResource(R.string.memory_sound)
            else -> null
        },
        state = stringResource(
            when (state) {
                MemoryTileState.DOWN -> R.string.memory_down
                MemoryTileState.UP -> R.string.memory_up
                MemoryTileState.MISSED -> R.string.memory_missed
                MemoryTileState.FOUND -> R.string.memory_found
            },
        ),
        modifier = modifier,
    )
}

/** The Polish a face says, if it says any. */
private fun MemoryFace.polish(): String? = when (this) {
    is MemoryFace.Word -> polish
    is MemoryFace.Sound -> polish
    else -> null
}

/** The Polish of a pair: every pair has a tile with it. */
private fun MemoryRound.polishOf(pair: String): String? = tiles.filter { it.pair == pair }.firstNotNullOfOrNull { it.face.polish() }
