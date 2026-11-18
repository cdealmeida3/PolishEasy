package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.polisheasy.app.ui.components.ClockOptionView
import com.polisheasy.app.ui.components.ClockOptions
import com.polisheasy.app.ui.components.ClockPrompt
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.components.SpeakerButton
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.Round
import com.polisheasy.engine.SaidTime
import com.polisheasy.engine.TimeRound

/**
 * One question of Tell the time, chapter 12's Game. Told: a clock face shows a time and the Learner says
 * it, from four, *wpół do dziewiątej* among them. Found: the time is written, and said with sound
 * on, and the Learner taps its face among four. Once answered the right one is marked and said,
 * and a wrong answer writes the time in digits beside its Polish.
 */
@Composable
internal fun TellTheTimeStep(round: TimeRound, sound: Boolean, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val time = question.time
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(round.step) { if (!round.read) say(time.polish) }
    Answered(given, next) { say(time.polish) }
    fun stateOf(option: SaidTime) = when {
        given == null -> OptionState.Idle
        option.key == time.key -> OptionState.Right
        option.key == given.text -> OptionState.Wrong
        else -> OptionState.Faded
    }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            if (round.read) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    question.options.forEachIndexed { index, option ->
                        OptionCard(
                            text = option.polish,
                            state = stateOf(option),
                            onClick = { onMove(round.choose(index)) },
                            modifier = Modifier.fillMaxWidth(),
                            polish = true,
                        )
                    }
                }
            } else {
                ClockOptions(
                    question.options.map { option -> ClockOptionView(option.hour, option.minute, digits(option), stateOf(option)) },
                    onChoose = { onMove(round.choose(it)) },
                )
            }
        },
    ) {
        Instruction(stringResource(if (round.read) R.string.clock_tell_instruction else R.string.clock_find_instruction))
        Spacer(Modifier.height(Space.s))
        if (round.read) {
            ClockPrompt(time.hour, time.minute, digits(time))
        } else {
            PromptCard(speaker = if (sound) ({ SpeakerButton(stringResource(R.string.audio_play), onClick = { say(time.polish) }) }) else null) {
                Text(
                    time.polish,
                    style = MaterialTheme.typography.headlineMedium.inPolish(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (given != null) {
            Spacer(Modifier.height(Space.m))
            RoundNote(if (given.correct) Mark.Right else Mark.Missed, stringResource(R.string.clock_note, time.polish, digits(time)), polish = true)
        }
    }
}

/** The time in digits, 8:30, for the note and for a screen reader. */
@Composable
private fun digits(time: SaidTime): String = stringResource(R.string.times_clock, time.hour, time.minute)
