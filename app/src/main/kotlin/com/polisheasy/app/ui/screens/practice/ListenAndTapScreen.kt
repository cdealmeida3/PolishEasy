package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PictureOption
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.ListenRound
import com.polisheasy.engine.Round

/**
 * One question of Listen and tap. The word is said when the question appears and again from the
 * play button, and nothing of it is written until it is answered. Below are four pictures, two
 * across, or four meanings; the one tapped is marked, and the right one with it.
 */
@Composable
internal fun ListenAndTapStep(round: ListenRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(Unit) { say(question.polish) }
    Answered(given, next)
    fun stateOf(option: Int): OptionState = when {
        given == null -> OptionState.Idle
        option == question.answer -> OptionState.Right
        question.options[option].itemId == given.text -> OptionState.Wrong
        else -> OptionState.Faded
    }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            if (round.pictures) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    question.options.indices.chunked(ACROSS).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            row.forEach { option ->
                                val shown = question.options[option]
                                PictureOption(
                                    art = shown.picture?.let { artOf(it) },
                                    description = shown.meaning,
                                    state = stateOf(option),
                                    onClick = { onMove(round.choose(option)) },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    question.options.forEachIndexed { option, shown ->
                        OptionCard(shown.meaning, stateOf(option), onClick = { onMove(round.choose(option)) })
                    }
                }
            }
        },
    ) {
        Instruction(stringResource(if (round.pictures) R.string.listen_picture_instruction else R.string.listen_meaning_instruction))
        Spacer(Modifier.height(Space.l))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            PlayButton(stringResource(R.string.audio_play), onClick = { say(question.polish) })
        }
        // What was said is written once it is answered.
        if (given != null) {
            Spacer(Modifier.height(Space.m))
            RoundNote(if (given.correct) Mark.Right else Mark.Missed, question.polish, polish = true)
        }
    }
}

private const val ACROSS = 2
