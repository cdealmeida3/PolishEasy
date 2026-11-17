package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.EmptyTileSlot
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.PhraseChain
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.components.TileBank
import com.polisheasy.app.ui.components.WordTile
import com.polisheasy.app.ui.components.rememberAnswerHaptics
import com.polisheasy.app.ui.components.shaking
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.ChainRound
import com.polisheasy.engine.Given
import com.polisheasy.engine.Round

/**
 * One sentence of Phrase chains. Its words lie mixed as cards at the bottom; tapped in order they
 * join the chain at the top. A wrong card shakes and stays, and is a slip. Heard, the sentence is
 * said when it appears and again from the play button, and what it means is shown once the chain
 * is whole; read, what it means is shown from the start and the sentence is said when it is whole.
 */
@Composable
internal fun PhraseChainsStep(round: ChainRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val sentence = round.sentence ?: return
    val said = round.said
    val haptics = rememberAnswerHaptics()
    val next = { onMove(round.next()) }
    if (round.listen) LaunchedEffect(Unit) { say(sentence.polish) }
    // A whole chain is a right answer: it is felt, and moves on by itself after a moment.
    val whole = said?.let { Given(it, correct = true) }
    Answered(whole, next) { if (!round.listen) say(it.text) }
    val tap: (Int) -> Unit = { card ->
        val tapped = round.tap(card)
        when {
            tapped.slips > round.slips -> haptics.wrong()
            !tapped.complete -> haptics.tick()
        }
        onMove(tapped)
    }
    RoundStep(
        action = whole?.let { { ContinueButton(it, next) } },
        answer = {
            TileBank {
                sentence.cards.forEachIndexed { index, word ->
                    if (index in round.chain) {
                        EmptyTileSlot(word)
                    } else {
                        // Each slip on a card shakes it again.
                        key(index, if (round.missed == index) round.slips else 0) {
                            WordTile(word, enabled = whole == null, onClick = { tap(index) }, modifier = Modifier.shaking(round.missed == index))
                        }
                    }
                }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Instruction(stringResource(if (round.listen) R.string.chain_listen_instruction else R.string.chain_read_instruction), Modifier.weight(1f))
            Text(
                pluralStringResource(R.plurals.practice_slips, round.slips, round.slips),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Space.s))
        if (round.listen) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PlayButton(stringResource(R.string.audio_play), onClick = { say(sentence.polish) })
            }
        } else {
            PromptCard {
                Text(sentence.meaning, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(Space.m))
        val words = round.chain.map { sentence.cards[it] }
        PhraseChain(
            words = words,
            complete = whole != null,
            hint = stringResource(R.string.chain_hint),
            description = when {
                words.isEmpty() -> stringResource(R.string.chain_empty)
                whole != null -> stringResource(R.string.chain_whole, words.joinToString(" "))
                else -> stringResource(R.string.chain_so_far, words.joinToString(" "))
            },
        )
        if (round.listen && whole != null) {
            Spacer(Modifier.height(Space.xs))
            Text(sentence.meaning, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
