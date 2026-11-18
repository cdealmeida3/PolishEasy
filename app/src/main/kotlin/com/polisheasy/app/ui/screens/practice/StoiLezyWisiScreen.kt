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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PictureFace
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.PositionsRound
import com.polisheasy.engine.Round

/**
 * One sentence of Stoi, leży czy wisi?, chapter 18's second Game. The thing's picture, when it has
 * one, is over the sentence with its verb gapped, *Obraz ___ na ścianie.*, and the Learner taps the
 * verb from the three of its row, side by side. Once answered the sentence is whole and said, and a
 * note gives it with what it means.
 */
@Composable
internal fun StoiLezyWisiStep(round: PositionsRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    Answered(given, next) { say(question.text) }
    val art = question.picture?.let { artOf(it) }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.fillMaxWidth()) {
                question.offered.forEachIndexed { index, verb ->
                    OptionCard(
                        text = verb,
                        state = when {
                            given == null -> OptionState.Idle
                            index == question.right -> OptionState.Right
                            verb == given.text -> OptionState.Wrong
                            else -> OptionState.Faded
                        },
                        onClick = { onMove(round.choose(index)) },
                        polish = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    ) {
        Instruction(stringResource(R.string.stoi_lezy_wisi_instruction))
        Spacer(Modifier.height(Space.s))
        PromptCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.s),
                modifier = Modifier.fillMaxWidth(),
            ) {
                art?.let { PictureFace(it, Modifier.size(Size.pictureOption)) }
                Text(
                    if (given == null) question.gapped else question.text,
                    style = MaterialTheme.typography.headlineSmall.inPolish(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (given != null) {
            Spacer(Modifier.height(Space.s))
            RoundNote(
                if (given.correct) Mark.Right else Mark.Missed,
                stringResource(R.string.stoi_lezy_wisi_note, question.text, question.meaning),
                polish = true,
            )
        }
    }
}
