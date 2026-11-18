package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.polisheasy.engine.HowManyRound
import com.polisheasy.engine.Round

/**
 * One question of How many?, chapter 15's second Game. A picture is shown as many times as it
 * counts, two to ten, five across, and the Learner taps the phrase that counts them: *pięć
 * butelek*, *pięć butelki* or *pięć butelka*. Once answered the phrase is said and a note gives it
 * and what it means.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HowManyStep(round: HowManyRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    Answered(given, next) { say(question.text) }
    val art = artOf(question.picture)
    val described = pluralStringResource(R.plurals.how_many_pictures, question.count, question.count)
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                question.offered.forEachIndexed { index, phrase ->
                    OptionCard(
                        text = phrase,
                        state = when {
                            given == null -> OptionState.Idle
                            index == question.right -> OptionState.Right
                            phrase == given.text -> OptionState.Wrong
                            else -> OptionState.Faded
                        },
                        onClick = { onMove(round.choose(index)) },
                        polish = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) {
        Instruction(stringResource(R.string.how_many_instruction))
        Spacer(Modifier.height(Space.s))
        PromptCard {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.xs, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(Space.xs),
                maxItemsInEachRow = ACROSS,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = described },
            ) {
                repeat(question.count) { if (art != null) PictureFace(art, Modifier.size(Size.countPicture)) }
            }
        }
        if (given != null) {
            Spacer(Modifier.height(Space.s))
            RoundNote(
                if (given.correct) Mark.Right else Mark.Missed,
                stringResource(R.string.how_many_note, question.text, question.meaning),
                polish = true,
            )
        }
    }
}

/** How many pictures stand side by side: five, so ten make two rows. */
private const val ACROSS = 5
