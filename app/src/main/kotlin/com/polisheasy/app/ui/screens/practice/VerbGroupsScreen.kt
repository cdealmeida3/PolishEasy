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
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.Round
import com.polisheasy.engine.VerbGroupRound

/**
 * One question of Three groups, chapter 6's Game. A verb is written with its meaning, and its *ja*
 * form when the Learner chose to see it, over one card for each group, named by its endings. Once
 * answered, the right group is marked, and the one tapped if it was wrong, and *ja* and *ty* are
 * written and said where there is a Clip of the *ja* form.
 */
@Composable
internal fun VerbGroupsStep(round: VerbGroupRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    Answered(given, next) { say(question.ja) }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                round.groups.forEach { group ->
                    val state = when {
                        given == null -> OptionState.Idle
                        group == question.group -> OptionState.Right
                        group == round.chosen -> OptionState.Wrong
                        else -> OptionState.Faded
                    }
                    OptionCard(group.endings, state, onClick = { onMove(round.choose(group)) }, polish = true)
                }
            }
        },
    ) {
        Instruction(stringResource(R.string.verb_groups_instruction))
        Spacer(Modifier.height(Space.l))
        Text(
            question.verb,
            style = MaterialTheme.typography.headlineLarge.inPolish(),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            question.meaning,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (round.withJa && given == null) {
            Spacer(Modifier.height(Space.m))
            Text(
                stringResource(R.string.verb_groups_ja, question.ja),
                style = MaterialTheme.typography.titleLarge.inPolish(),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (given != null) {
            Spacer(Modifier.height(Space.m))
            RoundNote(if (given.correct) Mark.Right else Mark.Missed, stringResource(R.string.verb_groups_forms, question.ja, question.ty), polish = true)
        }
    }
}
