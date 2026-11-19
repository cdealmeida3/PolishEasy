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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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
import com.polisheasy.engine.Compass
import com.polisheasy.engine.PolandMapRound
import com.polisheasy.engine.Round

/** The compass as Poland map lays it out: north at the top, west on the left, and the middle left empty. */
private val GRID: List<List<Compass?>> = listOf(
    listOf(Compass.NORTH_WEST, Compass.NORTH, Compass.NORTH_EAST),
    listOf(Compass.WEST, null, Compass.EAST),
    listOf(Compass.SOUTH_WEST, Compass.SOUTH, Compass.SOUTH_EAST),
)

/** From this font scale on, the compass is a list: its cells are too narrow for the words. */
private const val LIST_FROM_FONT_SCALE = 1.5f

/**
 * One place of Poland map, chapter 20's Game. The place is in a prompt card, *Gdańsk*, and under
 * it the eight parts of the country as a compass, north at the top, each cell saying where in
 * Polish, *na północy*; the Learner taps the one where the place lies. Once answered the sentence
 * that says so is said, and a note gives it with what it means.
 */
@Composable
internal fun PolandMapStep(round: PolandMapRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val chosen = round.chosen
    val next = { onMove(round.next()) }
    Answered(given, next) { say(question.says) }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            fun state(part: Compass) = when {
                given == null -> OptionState.Idle
                part == question.region -> OptionState.Right
                part == chosen -> OptionState.Wrong
                else -> OptionState.Faded
            }
            Column(verticalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.fillMaxWidth()) {
                // Large type does not fit three cells across: the parts then go one under another, north first.
                if (LocalDensity.current.fontScale >= LIST_FROM_FONT_SCALE) {
                    GRID.flatten().filterNotNull().forEach { part ->
                        OptionCard(text = part.polish, state = state(part), onClick = { onMove(round.choose(part)) }, polish = true)
                    }
                } else {
                    GRID.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            row.forEach { part ->
                                if (part == null) {
                                    Spacer(Modifier.weight(1f))
                                } else {
                                    OptionCard(
                                        text = part.polish,
                                        state = state(part),
                                        onClick = { onMove(round.choose(part)) },
                                        polish = true,
                                        compact = true,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    ) {
        Instruction(stringResource(R.string.poland_map_instruction))
        Spacer(Modifier.height(Space.s))
        PromptCard {
            Text(
                question.place,
                style = MaterialTheme.typography.headlineMedium.inPolish(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (given != null) {
            Spacer(Modifier.height(Space.s))
            RoundNote(
                if (given.correct) Mark.Right else Mark.Missed,
                stringResource(R.string.poland_map_note, question.says, question.meaning),
            )
        }
    }
}
