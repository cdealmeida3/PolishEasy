package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
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
import com.polisheasy.engine.Round
import com.polisheasy.engine.Speaker
import com.polisheasy.engine.SpeakerRound

/**
 * One line of Who said it?, chapter 13's second Game. Heard, the line is said when it comes and
 * again from the play button; read, it is written. The Learner taps who said it: a man, a woman, a
 * group with a man in it or women only. Once answered the line is written, the right speaker is
 * marked and a note gives what it means.
 */
@Composable
internal fun WhoSaidItStep(round: SpeakerRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(round.step) { if (round.listen) say(question.text) }
    Answered(given, next) { if (!round.listen) say(question.text) }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                round.speakers.forEachIndexed { index, speaker ->
                    OptionCard(
                        text = stringResource(speaker.label()),
                        state = when {
                            given == null -> OptionState.Idle
                            speaker == question.who -> OptionState.Right
                            speaker.id == given.text -> OptionState.Wrong
                            else -> OptionState.Faded
                        },
                        onClick = { onMove(round.choose(index)) },
                    )
                }
            }
        },
    ) {
        Instruction(stringResource(R.string.who_said_instruction))
        Spacer(Modifier.height(Space.s))
        if (round.listen && given == null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PlayButton(stringResource(R.string.audio_play), onClick = { say(question.text) })
            }
        } else {
            PromptCard {
                Text(
                    question.text,
                    style = MaterialTheme.typography.titleLarge.inPolish(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (given != null) {
            Spacer(Modifier.height(Space.s))
            RoundNote(if (given.correct) Mark.Right else Mark.Missed, stringResource(R.string.who_said_note, stringResource(question.who.label()), question.meaning))
        }
    }
}

/** What a speaker is called on its card. */
internal fun Speaker.label(): Int = when (this) {
    Speaker.MAN -> R.string.speaker_man
    Speaker.WOMAN -> R.string.speaker_woman
    Speaker.MEN -> R.string.speaker_men
    Speaker.WOMEN -> R.string.speaker_women
}
