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
import com.polisheasy.engine.ClockRound
import com.polisheasy.engine.Given
import com.polisheasy.engine.Round
import kotlinx.coroutines.delay

/**
 * Times of day, chapter 11's second Game, on the Quick round's clock. A time is shown, 7:30, and
 * below four times of day to say when it is. A right answer brings the next time at once. A wrong
 * one marks the right answer, writes what it means, says it, and waits for "Continue", while the
 * clock runs.
 */
@Composable
internal fun TimesOfDayStep(round: ClockRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
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
        action = if (round.missed != null) ({ ContinueButton(Given(card.answer, correct = false), next) }) else null,
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
                                say(card.answer)
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
            Instruction(stringResource(R.string.times_instruction), Modifier.weight(1f))
            Text(
                stringResource(R.string.quick_time, round.left / SECONDS_A_MINUTE, round.left % SECONDS_A_MINUTE),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Space.s))
        PromptCard {
            Text(
                stringResource(R.string.times_clock, card.hour, card.minute),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(Space.m))
        if (round.missed != null) {
            RoundNote(Mark.Missed, stringResource(R.string.times_missed, card.answer, card.meaning), polish = true)
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
