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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.ChatBubble
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.Round
import com.polisheasy.engine.TableRound

/**
 * One table of Take the order, chapter 10's Game. Heard, the guests' orders are said when the
 * table appears and again from the play button; read, each is a bubble. Below is the waiter's pad,
 * a line for each guest, and the menu, two across: a tap on a dish writes it for the next guest, a
 * tap on the last dish written rubs it out, and "Serve" serves. Once served, each guest's line is
 * marked right or wrong, the dishes ordered are marked on the menu, the orders are written as
 * bubbles, and a wrong table shows what each guest wanted.
 */
@Composable
internal fun TakeTheOrderStep(round: TableRound, sayAll: (List<String>) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    val said = question.lines.flatten()
    LaunchedEffect(round.step) { if (round.listen) sayAll(said) }
    Answered(given, next)
    val byId = question.menu.associateBy { it.id }
    RoundStep(
        action = if (given != null) {
            { ContinueButton(given, next) }
        } else {
            { PrimaryButton(stringResource(R.string.order_serve), { onMove(round.serve()) }, enabled = round.canServe) }
        },
        answer = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                question.menu.chunked(ACROSS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        row.forEach { dish ->
                            OptionCard(
                                text = dish.polish,
                                state = when {
                                    given == null -> OptionState.Idle
                                    dish.id in question.dishes -> OptionState.Right
                                    else -> OptionState.Faded
                                },
                                onClick = { onMove(round.write(dish.id)) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                polish = true,
                            )
                        }
                        repeat(ACROSS - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        },
    ) {
        Instruction(stringResource(if (round.listen) R.string.order_listen_instruction else R.string.order_read_instruction))
        Spacer(Modifier.height(Space.m))
        if (round.listen && given == null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PlayButton(stringResource(R.string.audio_play), onClick = { sayAll(said) })
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                question.polish.forEach { line -> ChatBubble(line, meaning = "", mine = false) }
            }
        }
        Spacer(Modifier.height(Space.m))
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            question.dishes.indices.forEach { guest ->
                val written = round.pad.getOrNull(guest)
                val last = written != null && guest == round.pad.lastIndex
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.order_guest, guest + 1),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = Space.s),
                    )
                    OptionCard(
                        text = written?.let { byId[it]?.polish } ?: stringResource(R.string.order_blank),
                        state = when {
                            given != null -> if (written == question.dishes[guest]) OptionState.Right else OptionState.Wrong
                            last -> OptionState.Chosen
                            else -> OptionState.Idle
                        },
                        onClick = { if (last) onMove(round.rubOut()) },
                        modifier = Modifier.weight(1f),
                        polish = written != null,
                    )
                }
            }
        }
        if (given != null && !given.correct) {
            Spacer(Modifier.height(Space.m))
            RoundNote(Mark.Missed, question.dishes.mapIndexed { guest, id -> "${guest + 1}. ${byId[id]?.polish.orEmpty()}" }.joinToString(" · "), polish = true)
        }
    }
}

private const val ACROSS = 2
