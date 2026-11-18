package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.components.rememberAnswerHaptics
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.FlipRound
import com.polisheasy.engine.Given
import com.polisheasy.engine.Round
import kotlinx.coroutines.delay

/**
 * Negation flip, chapter 10's Game, on the Quick round's clock. A sentence, *Lubię kawę.*, and
 * below the two ways to negate it, one above the other. A right answer brings the next sentence at
 * once. A wrong one marks the right negation, says it, writes below what it means and why, and
 * waits for "Continue", while the clock runs.
 */
@Composable
internal fun NegationFlipStep(round: FlipRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val latest by rememberUpdatedState(round)
    val move by rememberUpdatedState(onMove)
    LaunchedEffect(Unit) {
        while (true) {
            delay(Motion.SECOND_MILLIS)
            move(latest.tick())
        }
    }
    val card = round.card ?: return
    val haptics = rememberAnswerHaptics()
    val next = { onMove(round.next()) }
    RoundStep(
        action = if (round.missed != null) ({ ContinueButton(Given(card.no, correct = false), next) }) else null,
        answer = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                card.options.forEachIndexed { index, option ->
                    OptionCard(
                        text = option,
                        state = when {
                            round.missed == null -> OptionState.Idle
                            index == card.right -> OptionState.Right
                            index == round.missed -> OptionState.Wrong
                            else -> OptionState.Faded
                        },
                        onClick = {
                            val answered = round.answer(index)
                            if (answered.missed != null) {
                                haptics.wrong()
                                say(card.no)
                            } else {
                                haptics.tick()
                            }
                            onMove(answered)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        polish = true,
                    )
                }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Instruction(stringResource(R.string.flip_instruction), Modifier.weight(1f))
            Text(
                stringResource(R.string.quick_time, round.left / SECONDS_A_MINUTE, round.left % SECONDS_A_MINUTE),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Space.s))
        PromptCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.s),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    card.yes,
                    style = MaterialTheme.typography.headlineMedium.inPolish(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(Space.m))
        if (round.missed != null) {
            RoundNote(Mark.Missed, stringResource(R.string.flip_missed, card.no, card.meaning))
        } else {
            Text(
                pluralStringResource(R.plurals.practice_right, round.right, round.right),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val SECONDS_A_MINUTE = 60
