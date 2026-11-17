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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.MatchButtons
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.components.rememberAnswerHaptics
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.Given
import com.polisheasy.engine.OppositesRound
import com.polisheasy.engine.Round
import kotlinx.coroutines.delay

/**
 * Opposites snap, on the Quick round's clock. Two adjectives, one above the other in the same
 * gender, and at the bottom "Not opposites" and "Opposites!". A right answer brings the next pair
 * at once. A wrong one shows what the first word's opposite really is, with both meanings, and
 * waits for "Continue", while the clock runs. The time is written beside the instruction and the
 * bar at the top of the Round empties.
 */
@Composable
internal fun OppositesSnapStep(round: OppositesRound, onMove: (Round) -> Unit) {
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
    val answer: (Boolean) -> Unit = { opposites ->
        val answered = round.answer(opposites)
        if (answered.missed) haptics.wrong() else haptics.tick()
        onMove(answered)
    }
    val next = { onMove(round.next()) }
    RoundStep(
        action = if (round.missed) ({ ContinueButton(Given(card.shown, correct = false), next) }) else null,
        answer = {
            MatchButtons(
                noMatchLabel = stringResource(R.string.opposites_no),
                matchLabel = stringResource(R.string.opposites_yes),
                onNoMatch = { answer(false) },
                onMatch = { answer(true) },
                enabled = !round.missed,
            )
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Instruction(stringResource(R.string.opposites_instruction), Modifier.weight(1f))
            Text(
                stringResource(R.string.quick_time, round.left / SECONDS_A_MINUTE, round.left % SECONDS_A_MINUTE),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Space.s))
        PromptCard {
            // One above the other, so a long word never has to share a line, at any font size.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.xs),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    card.word,
                    style = MaterialTheme.typography.headlineLarge.inPolish(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.opposites_versus),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clearAndSetSemantics { },
                )
                Text(
                    card.shown,
                    style = MaterialTheme.typography.headlineLarge.inPolish(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(Space.m))
        if (round.missed) {
            RoundNote(Mark.Missed, stringResource(R.string.opposites_missed, card.word, card.meaning, card.opposite, card.oppositeMeaning))
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
