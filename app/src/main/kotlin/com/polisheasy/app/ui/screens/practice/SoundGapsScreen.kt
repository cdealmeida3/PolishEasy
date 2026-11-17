package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import com.polisheasy.app.ui.components.GapWordText
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.LetterOption
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.GapRound
import com.polisheasy.engine.Round

/**
 * One question of Sound gaps. The word is said when the question appears and again from the play
 * button, and is written with a gap where a hard sound is. Below are the spellings that sound
 * alike, side by side; the one tapped is marked, and the gap is filled with the right one. A wrong
 * spelling is never written into the word.
 */
@Composable
internal fun SoundGapsStep(round: GapRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val gap = question.gap
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(Unit) { say(gap.polish) }
    // The whole word is heard again once it is answered.
    Answered(given, next) { say(gap.polish) }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth()) {
                question.options.forEachIndexed { option, letters ->
                    LetterOption(
                        letters = letters,
                        state = when {
                            given == null -> OptionState.Idle
                            letters == gap.answer -> OptionState.Right
                            letters == given.text -> OptionState.Wrong
                            else -> OptionState.Faded
                        },
                        onClick = { onMove(round.choose(option)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    ) {
        Instruction(stringResource(R.string.gaps_instruction))
        Spacer(Modifier.height(Space.l))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            PlayButton(stringResource(R.string.audio_play), onClick = { say(gap.polish) })
        }
        Spacer(Modifier.height(Space.l))
        GapWordText(
            before = gap.before,
            after = gap.after,
            description = if (given == null) stringResource(R.string.gaps_word, gap.before, gap.after) else gap.polish,
            filled = given?.let { gap.answer },
            mark = given?.let { if (it.correct) Mark.Right else Mark.Missed },
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            question.meaning,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
