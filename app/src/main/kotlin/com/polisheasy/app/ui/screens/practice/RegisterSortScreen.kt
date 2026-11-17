package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RegisterButton
import com.polisheasy.app.ui.components.RegisterChip
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.components.rememberAnswerHaptics
import com.polisheasy.app.ui.screens.lesson.registerWords
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.Given
import com.polisheasy.engine.Register
import com.polisheasy.engine.RegisterRound
import com.polisheasy.engine.Round
import kotlinx.coroutines.delay

/**
 * Register sort, on the Quick round's clock. A phrase alone, with nothing that gives its register
 * away, and at the bottom a button for each register the Learner has met: O, N and U. A right
 * answer brings the next phrase at once. A wrong one shows the phrase's register and what it
 * means, says it, and waits for "Continue", while the clock runs. The time is written beside the
 * instruction and the bar at the top of the Round empties.
 */
@Composable
internal fun RegisterSortStep(round: RegisterRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val latest by rememberUpdatedState(round)
    val move by rememberUpdatedState(onMove)
    LaunchedEffect(Unit) {
        while (true) {
            delay(Motion.SECOND_MILLIS)
            move(latest.tick())
        }
    }
    val card = round.card ?: return
    val haptics = rememberAnswerHaptics()
    val answer: (Register) -> Unit = { register ->
        val answered = round.answer(register)
        if (answered.missed != null) {
            haptics.wrong()
            say(card.polish)
        } else {
            haptics.tick()
        }
        onMove(answered)
    }
    val next = { onMove(round.next()) }
    RoundStep(
        action = if (round.missed != null) ({ ContinueButton(Given(card.polish, correct = false), next) }) else null,
        answer = {
            // The buttons are as tall as the tallest, at any font size.
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                round.registers.forEach { register ->
                    val words = sortWords(register)
                    val count = round.sorted[register] ?: 0
                    RegisterButton(
                        mark = register.mark,
                        label = words,
                        count = count,
                        description = stringResource(R.string.register_sort_button, register.mark, words, count),
                        onClick = { answer(register) },
                        enabled = round.missed == null,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Instruction(stringResource(R.string.register_sort_instruction), Modifier.weight(1f))
            Text(
                stringResource(R.string.quick_time, round.left / SECONDS_A_MINUTE, round.left % SECONDS_A_MINUTE),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Space.s))
        PromptCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.s),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    card.polish,
                    style = MaterialTheme.typography.headlineLarge.inPolish(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                // The meaning often says who a phrase is for, so it is shown only once the phrase is answered wrong.
                if (round.missed != null) {
                    val words = registerWords(card.register)
                    RegisterChip(card.register.mark, description = stringResource(R.string.register_description, card.register.mark, words), label = words)
                    Text(
                        card.meaning,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.m))
        if (round.missed != null) {
            RoundNote(
                Mark.Missed,
                stringResource(
                    when (card.register) {
                        Register.FORMAL -> R.string.register_sort_missed_formal
                        Register.INFORMAL -> R.string.register_sort_missed_informal
                        Register.UNIVERSAL -> R.string.register_sort_missed_universal
                    },
                    card.polish,
                ),
            )
        } else {
            Text(
                pluralStringResource(R.plurals.practice_right, round.right, round.right),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** What a register's button says: who the phrase is said to. */
@Composable
private fun sortWords(register: Register): String = stringResource(
    when (register) {
        Register.FORMAL -> R.string.register_sort_formal
        Register.INFORMAL -> R.string.register_sort_informal
        Register.UNIVERSAL -> R.string.register_sort_universal
    },
)

private const val SECONDS_A_MINUTE = 60
