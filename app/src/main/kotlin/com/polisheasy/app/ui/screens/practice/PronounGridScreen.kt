package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.GridRowView
import com.polisheasy.app.ui.components.GridSquareState
import com.polisheasy.app.ui.components.GridSquareView
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PronounGrid
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.GridSquare
import com.polisheasy.engine.PronounGridRound
import com.polisheasy.engine.Round

/**
 * One square of Pronoun grid, chapter 17's Game. The table shows the persons down the side and
 * one or two columns, each headed by its question (*z kim?*); the square being asked is outlined.
 * The Learner taps its form from four, two across. Once answered the form goes into the square,
 * ticked or crossed, and is said, and a note gives the person, the question and the form.
 */
@Composable
internal fun PronounGridStep(round: PronounGridRound, say: (String) -> Unit, onMove: (Round) -> Unit) {
    val question = round.question ?: return
    val given = round.given
    val next = { onMove(round.next()) }
    Answered(given, next) { say(question.form) }
    val asked = stringResource(R.string.pronoun_grid_asked)
    val empty = stringResource(R.string.pronoun_grid_empty)
    val rows = round.persons.mapIndexed { person, name ->
        val squares = round.columns.indices.map { column ->
            val state = when (round.square(person, column)) {
                GridSquare.NONE -> GridSquareState.None
                GridSquare.WAITING -> GridSquareState.Waiting
                GridSquare.ASKED -> if (given == null) GridSquareState.Asked else if (given.correct) GridSquareState.Right else GridSquareState.Missed
                GridSquare.RIGHT -> GridSquareState.Right
                GridSquare.MISSED -> GridSquareState.Missed
            }
            GridSquareView(round.columns[column].cells[person], state)
        }
        val said = round.columns.zip(squares).joinToString(", ") { (column, square) ->
            "${column.question} " + when (square.state) {
                GridSquareState.Right, GridSquareState.Missed -> square.form.orEmpty()
                GridSquareState.Asked -> asked
                GridSquareState.Waiting, GridSquareState.None -> empty
            }
        }
        GridRowView(name, squares, "$name: $said")
    }
    RoundStep(
        action = given?.let { { ContinueButton(it, next) } },
        answer = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                question.offered.withIndex().chunked(ACROSS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { (index, form) ->
                            OptionCard(
                                text = form,
                                state = when {
                                    given == null -> OptionState.Idle
                                    form == question.form -> OptionState.Right
                                    form == given.text -> OptionState.Wrong
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
        Instruction(stringResource(R.string.pronoun_grid_instruction))
        Spacer(Modifier.height(Space.s))
        PronounGrid(
            title = stringResource(R.string.pronoun_grid_title),
            questions = round.columns.map { it.question },
            rows = rows,
        )
        if (given != null) {
            Spacer(Modifier.height(Space.s))
            val column = round.columns[question.column]
            RoundNote(
                if (given.correct) Mark.Right else Mark.Missed,
                stringResource(R.string.pronoun_grid_note, round.persons[question.person], column.question, question.form, column.meaning),
                polish = true,
            )
        }
    }
}

/** How many forms stand side by side: two, so *dla ciebie* keeps to one line. */
private const val ACROSS = 2
