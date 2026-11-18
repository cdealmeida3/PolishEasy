package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RoomSpot
import com.polisheasy.app.ui.components.RoomThingView
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.RoomSceneRound
import com.polisheasy.engine.Round

/**
 * One sentence of Room scene, chapter 18's Game. Heard, the sentence is said when it comes and
 * again from the play button; read, it is written. Below is the room, its places three across, each
 * with the things already put there. The Learner taps the place the sentence puts its thing; once
 * answered the thing is on its place, the right place is marked and a note gives the sentence and
 * what it means.
 */
@Composable
internal fun RoomSceneStep(round: RoomSceneRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(round.step) { if (round.listen) say(question.text) }
    Answered(given, next) { if (!round.listen) say(question.text) }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                round.board.indices.chunked(ACROSS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { index ->
                            val place = round.board[index]
                            val put = round.things(index)
                            val things = put.map { RoomThingView(it.picture?.let { picture -> artOf(picture) }, it.thing) }
                            val state = when {
                                given == null -> OptionState.Idle
                                index == question.place -> OptionState.Right
                                index == round.chosen -> OptionState.Wrong
                                else -> OptionState.Faded
                            }
                            val description = if (put.isEmpty()) {
                                stringResource(R.string.room_spot_empty, place.text)
                            } else {
                                stringResource(R.string.room_spot_things, place.text, put.joinToString(", ") { it.thing })
                            }
                            RoomSpot(place.text, things, description, state, onClick = { onMove(round.choose(index)) }, modifier = Modifier.weight(1f))
                        }
                        repeat(ACROSS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        },
    ) {
        Instruction(stringResource(R.string.room_scene_instruction))
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
                stringResource(R.string.room_scene_note, question.text, question.meaning),
                polish = true,
            )
        }
    }
}

/** How many places of the room stand side by side: three, so the six of a board are two rows. */
private const val ACROSS = 3
