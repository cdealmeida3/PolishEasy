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
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PictureOption
import com.polisheasy.app.ui.components.PlateFoodView
import com.polisheasy.app.ui.components.PlateView
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.PlateRound
import com.polisheasy.engine.Round

/**
 * One order of Plate builder, chapter 8's Game. Heard, the order is said when it appears and again
 * from the play button; read, it is written. Below is the plate, with what is on it, and the tray
 * of foods, three across: a tap puts one on the plate or takes it off, and "Serve" checks it. Once
 * served, the foods wanted are marked right, any put on wrongly a cross, and the rest fade.
 */
@Composable
internal fun PlateBuilderStep(round: PlateRound, sayAll: (List<String>) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(round.step) { if (round.listen) sayAll(question.words) }
    Answered(given, next)
    fun stateOf(id: String): OptionState = when {
        given == null -> if (id in round.placed) OptionState.Chosen else OptionState.Idle
        id in question.wanted -> OptionState.Right
        id in round.placed -> OptionState.Wrong
        else -> OptionState.Faded
    }
    RoundStep(
        action = if (given != null) {
            { ContinueButton(given, next) }
        } else {
            { PrimaryButton(stringResource(R.string.plate_serve), { onMove(round.serve()) }, enabled = round.canServe) }
        },
        answer = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                question.tray.chunked(ACROSS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        row.forEach { food ->
                            PictureOption(
                                art = artOf(food.picture),
                                description = food.means,
                                state = stateOf(food.id),
                                onClick = { onMove(round.place(food.id)) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                        repeat(ACROSS - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        },
    ) {
        Instruction(stringResource(if (round.listen) R.string.plate_listen_instruction else R.string.plate_read_instruction))
        Spacer(Modifier.height(Space.m))
        if (round.listen) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PlayButton(stringResource(R.string.audio_play), onClick = { sayAll(question.words) })
            }
        } else {
            Text(
                question.polish,
                style = MaterialTheme.typography.titleLarge.inPolish(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(Space.m))
        val byId = question.tray.associateBy { it.id }
        PlateView(
            foods = round.placed.mapNotNull { byId[it] }.map { PlateFoodView(artOf(it.picture), it.means) },
            empty = stringResource(R.string.plate_empty),
            takeOffLabel = stringResource(R.string.plate_take_off),
            onTakeOff = { onMove(round.place(round.placed[it])) },
            enabled = given == null,
        )
        if (given != null) {
            Spacer(Modifier.height(Space.m))
            RoundNote(if (given.correct) Mark.Right else Mark.Missed, question.polish, polish = true)
        }
    }
}

private const val ACROSS = 3
