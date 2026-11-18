package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.CityMap
import com.polisheasy.engine.DirectionsRound
import com.polisheasy.engine.Round

/**
 * One route of Directions, chapter 14's Game. Heard, the route is said when it comes and again
 * from the play button; read, it is written. Below is the map, its blocks in rows with a place on
 * each, and under it where the Learner starts, facing up the map. The Learner taps the place the
 * route leads to; once answered the route is written, the right place is marked and a note gives
 * the place and what the route means.
 */
@Composable
internal fun DirectionsStep(round: DirectionsRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(round.step) { if (round.listen) say(question.text) }
    Answered(given, next) { if (!round.listen) say(question.text) }
    val arts = round.map.map { artOf(it.picture) }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                (0 until CityMap.ROWS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.fillMaxWidth()) {
                        (0 until CityMap.COLUMNS).forEach { column ->
                            val index = round.map.indexOfFirst { it.block.row == row && it.block.column == column }
                            val place = round.map[index]
                            val state = when {
                                given == null -> OptionState.Idle
                                index == question.place -> OptionState.Right
                                index == round.chosen -> OptionState.Wrong
                                else -> OptionState.Faded
                            }
                            PictureOption(arts[index], place.text, state, onClick = { onMove(round.choose(index)) }, modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
                Text(
                    stringResource(R.string.directions_start),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        Instruction(stringResource(R.string.directions_instruction))
        Spacer(Modifier.height(Space.s))
        if (round.listen && given == null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PlayButton(stringResource(R.string.audio_play), onClick = { say(question.text) })
            }
        } else {
            PromptCard {
                Text(
                    question.text,
                    style = MaterialTheme.typography.titleMedium.inPolish(),
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
                stringResource(R.string.directions_note, round.map[question.place].text, question.meaning),
                polish = true,
            )
        }
    }
}
