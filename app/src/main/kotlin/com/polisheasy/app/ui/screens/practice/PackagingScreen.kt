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
import com.polisheasy.app.ui.theme.withMarked
import com.polisheasy.engine.PackagingRound
import com.polisheasy.engine.Round

/**
 * One phrase of Packaging match, chapter 15's Game. A gap and what is in it is written, *___ coli*,
 * the gap marked in the brand's colour, and the Learner taps the container it comes in: *puszka*,
 * *słoik*, *kostka*, *tabliczka*. Once answered the whole phrase is written with its container
 * marked, and said, and a note gives what it means.
 */
@Composable
internal fun PackagingStep(round: PackagingRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    Answered(given, next) { say(question.text) }
    val container = if (given == null) GAP else question.offered[question.right].text
    val shown = withMarked("", container, " " + question.thing, MaterialTheme.colorScheme.primary)
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                question.offered.withIndex().chunked(ACROSS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { (index, offered) ->
                            OptionCard(
                                text = offered.text,
                                state = when {
                                    given == null -> OptionState.Idle
                                    index == question.right -> OptionState.Right
                                    offered.text == given.text -> OptionState.Wrong
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
        Instruction(stringResource(R.string.packaging_instruction))
        Spacer(Modifier.height(Space.s))
        PromptCard {
            Text(
                shown,
                style = MaterialTheme.typography.headlineSmall.inPolish(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (given != null) {
            Spacer(Modifier.height(Space.s))
            RoundNote(
                if (given.correct) Mark.Right else Mark.Missed,
                stringResource(R.string.packaging_note, question.text, question.meaning),
                polish = true,
            )
        }
    }
}

/** What stands in the container's place until the Learner answers. */
private const val GAP = "_____"

/** How many containers stand side by side: two, so each keeps its word on one line. */
private const val ACROSS = 2
