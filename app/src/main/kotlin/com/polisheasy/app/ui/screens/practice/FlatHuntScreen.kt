package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.FlatHuntRound
import com.polisheasy.engine.Round

/**
 * One hunt of Flat hunt, chapter 19's Game. What someone is looking for is in a prompt card,
 * *Mam psa. Szukam mieszkania na parterze.*, and under it the three adverts, one under another; the
 * Learner taps the one that fits. Once answered the wish is said, and a note gives what it and the
 * right advert mean.
 */
@Composable
internal fun FlatHuntStep(round: FlatHuntRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val chosen = round.chosen
    val next = { onMove(round.next()) }
    Answered(given, next) { say(question.wish) }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.fillMaxWidth()) {
                question.adverts.forEachIndexed { index, advert ->
                    OptionCard(
                        text = advert.text,
                        state = when {
                            given == null -> OptionState.Idle
                            index == question.right -> OptionState.Right
                            index == chosen -> OptionState.Wrong
                            else -> OptionState.Faded
                        },
                        onClick = { onMove(round.choose(index)) },
                        polish = true,
                    )
                }
            }
        },
    ) {
        Instruction(stringResource(R.string.flat_hunt_instruction))
        Spacer(Modifier.height(Space.s))
        PromptCard {
            Text(
                question.wish,
                style = MaterialTheme.typography.titleLarge.inPolish(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (given != null) {
            Spacer(Modifier.height(Space.s))
            RoundNote(
                if (given.correct) Mark.Right else Mark.Missed,
                stringResource(R.string.flat_hunt_note, question.wishMeaning, question.adverts[question.right].meaning),
            )
        }
    }
}
