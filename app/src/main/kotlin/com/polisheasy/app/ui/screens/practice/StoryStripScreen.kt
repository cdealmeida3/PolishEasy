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
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PictureOption
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.components.StoryStrip
import com.polisheasy.app.ui.components.StripPlace
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.Round
import com.polisheasy.engine.StoryRound

/**
 * One story of Story strip, chapter 13's Game. Heard, the story is said when it comes and again
 * from the play button; read, it is written. The strip is below, its places numbered, and under it
 * the story's pictures mixed up. The Learner taps the picture that comes next; it is put in the
 * strip, and a wrong one is crossed and the right one put in its place. A note gives the sentence
 * the picture shows and what it means.
 */
@Composable
internal fun StoryStripStep(round: StoryRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val story = round.story ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    LaunchedEffect(round.step) { if (round.listen) say(story.text) }
    Answered(given, next)
    val placed = round.placed
    val arts = story.tiles.map { artOf(it.picture) }
    val emptyPlaces = story.order.indices.map { stringResource(R.string.story_place, it + 1) }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                story.tiles.withIndex().chunked(ACROSS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { (index, tile) ->
                            val state = when {
                                given != null && index == question.tile -> OptionState.Right
                                given != null && index == round.chosen -> OptionState.Wrong
                                given != null || index in placed -> OptionState.Faded
                                else -> OptionState.Idle
                            }
                            PictureOption(arts[index], tile.meaning, state, onClick = { onMove(round.choose(index)) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
    ) {
        Instruction(stringResource(R.string.story_instruction))
        Spacer(Modifier.height(Space.s))
        Text(story.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Space.xs))
        if (round.listen) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                PlayButton(stringResource(R.string.audio_play), onClick = { say(story.text) })
            }
        } else {
            PromptCard {
                Text(
                    story.text,
                    style = MaterialTheme.typography.titleMedium.inPolish(),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(Space.s))
        StoryStrip(
            places = story.order.indices.map { place ->
                val tile = placed.getOrNull(place)
                if (tile == null) StripPlace(null, null) else StripPlace(arts[tile], story.tiles[tile].meaning)
            },
            next = question.place.takeIf { given == null },
            placeLabel = { emptyPlaces[it - 1] },
        )
        if (given != null) {
            val tile = story.tiles[question.tile]
            Spacer(Modifier.height(Space.s))
            RoundNote(
                if (given.correct) Mark.Right else Mark.Missed,
                stringResource(R.string.story_note, tile.text, tile.meaning),
                polish = true,
            )
        }
    }
}

private const val ACROSS = 2
