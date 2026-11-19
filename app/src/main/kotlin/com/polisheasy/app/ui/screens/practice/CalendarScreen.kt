package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.CalendarRound
import com.polisheasy.engine.Round

/**
 * One page of Calendar, chapter 21's Game. A page of a calendar is in a prompt card: the date as a
 * calendar prints it, *14.02*, and the holiday on it, *Walentynki*; under it three ways of saying
 * when it is, one under another, and the Learner taps the right one, *czternastego lutego*. Once
 * answered the sentence that says so is said, and a note gives it with what it means.
 */
@Composable
internal fun CalendarStep(round: CalendarRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val chosen = round.chosen
    val next = { onMove(round.next()) }
    Answered(given, next) { say(question.says) }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.fillMaxWidth()) {
                question.options.forEachIndexed { index, option ->
                    OptionCard(
                        text = option,
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
        Instruction(stringResource(R.string.calendar_instruction))
        Spacer(Modifier.height(Space.s))
        PromptCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    question.date,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    question.holiday,
                    style = MaterialTheme.typography.titleLarge.inPolish(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (given != null) {
            Spacer(Modifier.height(Space.s))
            RoundNote(
                if (given.correct) Mark.Right else Mark.Missed,
                stringResource(R.string.calendar_note, question.says, question.meaning),
            )
        }
    }
}
