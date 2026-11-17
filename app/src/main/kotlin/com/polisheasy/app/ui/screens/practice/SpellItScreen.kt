package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.EmptyTileSlot
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.PictureFace
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.components.SpelledWord
import com.polisheasy.app.ui.components.TileBank
import com.polisheasy.app.ui.components.WordTile
import com.polisheasy.app.ui.components.rememberAnswerHaptics
import com.polisheasy.app.ui.components.shaking
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.Round
import com.polisheasy.engine.SpellRound

/**
 * One word of Spell it. Its picture or its meaning is at the top, or, spelled aloud, a button that
 * says its letters' names again; under it a slot for each letter. The letters lie mixed as tiles
 * at the bottom; tapped in order they fill the slots. A wrong tile shakes and stays, and is a
 * slip. Whole, the word is said, and its meaning written where it was not shown.
 */
@Composable
internal fun SpellItStep(round: SpellRound, say: (String) -> Unit, sayAll: (List<String>) -> Unit, onMove: (Round) -> Unit) {
    val word = round.word ?: return
    val given = round.given
    // Spelled aloud, each word's letters are said as it comes, and again on the button.
    LaunchedEffect(word.itemId) { if (round.aloud) sayAll(word.spelled) }
    val haptics = rememberAnswerHaptics()
    val next = { onMove(round.next()) }
    Answered(given, next) { say(word.polish) }
    val tap: (Int) -> Unit = { tile ->
        val tapped = round.tap(tile)
        when {
            tapped.slips > round.slips -> haptics.wrong()
            !tapped.complete -> haptics.tick()
        }
        onMove(tapped)
    }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            TileBank {
                word.tiles.forEachIndexed { index, letter ->
                    if (index in round.placed) {
                        EmptyTileSlot(letter, Modifier.widthIn(min = Size.touch))
                    } else {
                        // Each slip on a tile shakes it again.
                        key(index, if (round.missed == index) round.slips else 0) {
                            WordTile(
                                letter,
                                enabled = given == null,
                                onClick = { tap(index) },
                                modifier = Modifier.widthIn(min = Size.touch).shaking(round.missed == index),
                            )
                        }
                    }
                }
            }
        },
    ) {
        Instruction(stringResource(if (round.aloud) R.string.spell_aloud_instruction else R.string.spell_instruction))
        Spacer(Modifier.height(Space.l))
        val art = word.picture?.let { artOf(it) }
        if (round.aloud) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PlayButton(stringResource(R.string.spell_aloud_again), onClick = { sayAll(word.spelled) })
            }
        } else if (art != null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PictureFace(art, Modifier.size(Size.pictureOption), description = word.meaning)
            }
        } else {
            PromptCard {
                Text(word.meaning, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(Space.l))
        val spelled = round.placed.map { word.tiles[it] }
        SpelledWord(
            letters = spelled,
            length = word.tiles.size,
            description = when {
                given != null -> stringResource(R.string.spell_whole, word.polish)
                spelled.isEmpty() -> stringResource(R.string.spell_empty, word.tiles.size)
                else -> stringResource(R.string.spell_so_far, spelled.joinToString(""), spelled.size, word.tiles.size)
            },
            mark = given?.let { if (it.correct) Mark.Right else Mark.Missed },
        )
        // A picture says what the word means only to someone who knows it, and spelled aloud nothing does: the meaning is written once the word is whole.
        if ((art != null || round.aloud) && given != null) {
            Spacer(Modifier.height(Space.xs))
            Text(
                word.meaning,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
