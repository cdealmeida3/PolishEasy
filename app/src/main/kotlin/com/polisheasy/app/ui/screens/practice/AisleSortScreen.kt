package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import com.polisheasy.app.ui.components.BucketGrid
import com.polisheasy.app.ui.components.BucketView
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PictureFace
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.components.rememberAnswerHaptics
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.AisleRound
import com.polisheasy.engine.Given
import com.polisheasy.engine.Round
import kotlinx.coroutines.delay

/**
 * Sort the aisles, chapter 9's Game, on the Quick round's clock. A product, with its Picture when
 * it has one and its Polish, and below the signs of the Round's aisles, two across, each counting
 * what it holds. A right answer brings the next product at once. A wrong one marks the aisle it
 * belongs in, writes what it means, says it, and waits for "Continue", while the clock runs.
 */
@Composable
internal fun AisleSortStep(round: AisleRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
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
        action = if (round.missed != null) ({ ContinueButton(Given(card.polish, correct = false), next) }) else null,
        answer = {
            BucketGrid(
                round.aisles.map { aisle ->
                    val holds = round.sorted[aisle.id] ?: 0
                    BucketView(
                        word = aisle.polish,
                        holds = holds.toString(),
                        holdsDescription = pluralStringResource(R.plurals.aisle_holds, holds, holds),
                        state = when {
                            round.missed == null -> OptionState.Idle
                            aisle.id == card.aisle -> OptionState.Right
                            aisle.id == round.missed -> OptionState.Wrong
                            else -> OptionState.Faded
                        },
                    )
                },
                onClick = { index ->
                    val answered = round.answer(round.aisles[index].id)
                    if (answered.missed != null) {
                        haptics.wrong()
                        say(card.polish)
                    } else {
                        haptics.tick()
                    }
                    onMove(answered)
                },
            )
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Instruction(stringResource(R.string.aisle_instruction), Modifier.weight(1f))
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
                card.picture?.let { picture -> artOf(picture)?.let { PictureFace(it, Modifier.size(Size.pictureOption)) } }
                Text(
                    card.polish,
                    style = MaterialTheme.typography.headlineLarge.inPolish(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                if (round.missed != null) {
                    Text(
                        card.meaning,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.m))
        val right = round.rightAisle
        if (round.missed != null && right != null) {
            RoundNote(Mark.Missed, stringResource(R.string.aisle_missed, card.polish, right.polish, right.meaning))
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
