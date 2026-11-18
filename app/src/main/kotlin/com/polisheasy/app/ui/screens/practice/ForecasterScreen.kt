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
import com.polisheasy.app.ui.components.ForecastCard
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PictureOption
import com.polisheasy.app.ui.components.PlateFoodView
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.ForecasterRound
import com.polisheasy.engine.Round

/**
 * One forecast of Forecaster, chapter 16's Game. Heard, the forecast is said when it appears and
 * again from the play button; read, it is written. Below is tomorrow's card, with the weather put
 * on it and its temperature, then the board of weather signs, three across, and the temperatures, two across
 * to pick from. A tap on a sign puts it on the card or takes it off, and "Send" checks the card.
 * Once sent, the signs wanted are marked right, any put on wrongly a cross, and the rest fade;
 * the temperatures likewise.
 */
@Composable
internal fun ForecasterStep(round: ForecasterRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(round.step) { if (round.listen) say(question.text) }
    Answered(given, next) { if (!round.listen) say(question.text) }
    fun signState(id: String): OptionState = when {
        given == null -> if (id in round.placed) OptionState.Chosen else OptionState.Idle
        id in question.wanted -> OptionState.Right
        id in round.placed -> OptionState.Wrong
        else -> OptionState.Faded
    }
    fun temperatureState(degrees: Int): OptionState = when {
        given == null -> if (degrees == round.temperature) OptionState.Chosen else OptionState.Idle
        degrees == question.degrees -> OptionState.Right
        degrees == round.temperature -> OptionState.Wrong
        else -> OptionState.Faded
    }
    RoundStep(
        action = if (given != null) {
            { ContinueButton(given, next) }
        } else {
            { PrimaryButton(stringResource(R.string.forecaster_send), { onMove(round.send()) }, enabled = round.canSend) }
        },
        answer = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                question.board.chunked(ACROSS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        row.forEach { sign ->
                            PictureOption(
                                art = artOf(sign.picture),
                                description = sign.means,
                                state = signState(sign.id),
                                onClick = { onMove(round.place(sign.id)) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                        repeat(ACROSS - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
                // Two across, so "−12 °C" and its mark fit on one line at any font size.
                question.temperatures.chunked(TEMPERATURES_ACROSS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        row.forEach { degrees ->
                            OptionCard(
                                text = celsius(degrees),
                                state = temperatureState(degrees),
                                onClick = { onMove(round.pick(degrees)) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                        repeat(TEMPERATURES_ACROSS - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        },
    ) {
        Instruction(stringResource(if (round.listen) R.string.forecaster_listen_instruction else R.string.forecaster_read_instruction))
        Spacer(Modifier.height(Space.s))
        if (round.listen) {
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
        Spacer(Modifier.height(Space.s))
        val byId = question.board.associateBy { it.id }
        ForecastCard(
            title = stringResource(R.string.forecaster_card),
            signs = round.placed.mapNotNull { byId[it] }.map { PlateFoodView(artOf(it.picture), it.means) },
            temperature = round.temperature?.let(::celsius),
            noTemperature = stringResource(R.string.forecaster_no_temperature),
            empty = stringResource(R.string.forecaster_empty),
            takeOffLabel = stringResource(R.string.forecaster_take_off),
            onTakeOff = { onMove(round.place(round.placed[it])) },
            enabled = given == null,
        )
        if (given != null) {
            Spacer(Modifier.height(Space.s))
            RoundNote(
                if (given.correct) Mark.Right else Mark.Missed,
                stringResource(R.string.forecaster_note, question.text, question.meaning),
                polish = true,
            )
        }
    }
}

/** A temperature as a forecast card shows it: −5 °C, with a true minus sign. */
private fun celsius(degrees: Int): String = (if (degrees < 0) "−${-degrees}" else "$degrees") + " °C"

private const val ACROSS = 3
private const val TEMPERATURES_ACROSS = 2
