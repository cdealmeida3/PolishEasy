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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.app.ui.theme.withMarked
import com.polisheasy.engine.Round
import com.polisheasy.engine.SwapRound

/**
 * One sentence of Pronoun swap, chapter 14's second Game. The sentence is written with its noun
 * marked in the brand's colour, and heard, it is also said. The Learner taps the word that takes
 * the noun's place, *go*, *ją*, *je*, *ich*, or after *na*, *niego*, *nią*, *nich*. Once answered
 * the shorter sentence is written with the pronoun marked, and said, and a note gives what it means.
 */
@Composable
internal fun PronounSwapStep(round: SwapRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(round.step) { if (round.listen) say(question.text) }
    Answered(given, next) { say(question.short) }
    val shown = withMarked(question.before, if (given == null) question.noun else question.pronoun, question.after, MaterialTheme.colorScheme.primary)
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                question.offered.withIndex().chunked(ACROSS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { (index, pronoun) ->
                            OptionCard(
                                text = pronoun,
                                state = when {
                                    given == null -> OptionState.Idle
                                    pronoun == question.pronoun -> OptionState.Right
                                    pronoun == given.text -> OptionState.Wrong
                                    else -> OptionState.Faded
                                },
                                onClick = { onMove(round.choose(index)) },
                                polish = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(ACROSS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        },
    ) {
        Instruction(stringResource(R.string.swap_instruction))
        Spacer(Modifier.height(Space.s))
        PromptCard {
            Text(
                shown,
                style = MaterialTheme.typography.titleLarge.inPolish(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (round.listen && given == null) {
            Spacer(Modifier.height(Space.s))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                PlayButton(stringResource(R.string.audio_play), onClick = { say(question.text) })
            }
        }
        if (given != null) {
            Spacer(Modifier.height(Space.s))
            RoundNote(
                if (given.correct) Mark.Right else Mark.Missed,
                stringResource(R.string.swap_note, question.text, question.meaning, question.shortMeaning),
                polish = true,
            )
        }
    }
}

/** How many pronouns stand side by side: two, so a marked one keeps its word on one line. */
private const val ACROSS = 2
