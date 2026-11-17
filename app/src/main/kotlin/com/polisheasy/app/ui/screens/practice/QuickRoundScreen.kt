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
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.MatchButtons
import com.polisheasy.app.ui.components.PictureFace
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.components.rememberAnswerHaptics
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.Given
import com.polisheasy.engine.QuickRound
import com.polisheasy.engine.Round
import kotlinx.coroutines.delay

/**
 * The Quick round. A Polish word and, under it, a picture or a meaning; at the bottom "no match"
 * and "match". A right answer brings the next card at once. A wrong one shows what the word
 * means and waits for "Continue", while the clock runs. The clock is the bar at the top of the
 * Round, which empties, and the time written beside the instruction: once a second this step
 * takes a second off, and at none left the Round is over.
 */
@Composable
internal fun QuickRoundStep(round: QuickRound, onMove: (Round) -> Unit) {
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
    val answer: (Boolean) -> Unit = { match ->
        val answered = round.answer(match)
        if (answered.missed) haptics.wrong() else haptics.tick()
        onMove(answered)
    }
    val next = { onMove(round.next()) }
    RoundStep(
        action = if (round.missed) ({ ContinueButton(Given(card.polish, correct = false), next) }) else null,
        answer = {
            MatchButtons(
                noMatchLabel = stringResource(R.string.quick_no_match),
                matchLabel = stringResource(R.string.quick_match),
                onNoMatch = { answer(false) },
                onMatch = { answer(true) },
                enabled = !round.missed,
            )
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Instruction(stringResource(R.string.quick_instruction), Modifier.weight(1f))
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
                    card.polish,
                    style = MaterialTheme.typography.headlineLarge.inPolish(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                val art = card.shownPicture?.let { artOf(it) }
                if (art != null) {
                    PictureFace(art, Modifier.size(Size.pictureOption), description = card.shownMeaning)
                } else {
                    Text(
                        card.shownMeaning,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.m))
        if (round.missed) {
            RoundNote(Mark.Missed, stringResource(R.string.quick_missed, card.polish, card.meaning))
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
