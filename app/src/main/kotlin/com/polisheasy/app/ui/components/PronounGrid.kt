package com.polisheasy.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish

/** What a square of the Pronoun grid holds now. */
enum class GridSquareState { None, Waiting, Asked, Right, Missed }

/** A square of the Pronoun grid: its [form] once it is filled, and its [state]. */
class GridSquareView(val form: String?, val state: GridSquareState)

/** A row of the Pronoun grid: its [person] (*ona*), a square for each column, and what a screen reader says for the row. */
class GridRowView(val person: String, val squares: List<GridSquareView>, val description: String)

/**
 * The table of the pronouns the Learner fills in Pronoun grid: the persons down the side and,
 * across the top, each column's question in Polish (*z kim?*). A square filled keeps its form,
 * ticked or crossed; the one being asked is outlined in the brand's colour and says "?"; one
 * still to come is a dashed slot; one with no form to learn holds a dash.
 */
@Composable
fun PronounGrid(title: String, questions: List<String>, rows: List<GridRowView>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Edge.hairline, scheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Space.m), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }) {
                Box(Modifier.weight(PERSON_WEIGHT))
                questions.forEach { question ->
                    Text(
                        question,
                        style = MaterialTheme.typography.labelLarge.inPolish(),
                        color = scheme.primary,
                        modifier = Modifier.weight(1f).padding(horizontal = Space.xs),
                    )
                }
            }
            rows.forEach { row ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = RowHeight)
                        .clearAndSetSemantics { contentDescription = row.description },
                ) {
                    Text(
                        row.person,
                        style = MaterialTheme.typography.bodyLarge.inPolish(),
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.weight(PERSON_WEIGHT),
                    )
                    row.squares.forEach { square ->
                        Box(Modifier.weight(1f).padding(horizontal = Space.xs)) { Square(square) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Square(square: GridSquareView) {
    when (square.state) {
        GridSquareState.Right, GridSquareState.Missed -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(square.form.orEmpty(), style = MaterialTheme.typography.titleMedium.inPolish(), modifier = Modifier.weight(1f, fill = false))
            MarkIcon(if (square.state == GridSquareState.Right) Mark.Right else Mark.Missed, modifier = Modifier.padding(start = Space.xs))
        }
        GridSquareState.Asked -> TableSlot(asked = true)
        GridSquareState.Waiting -> TableSlot(asked = false)
        GridSquareState.None -> Text("–", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
    }
}

/** The persons' column is narrower than a column of forms. */
private const val PERSON_WEIGHT = 0.6f

private val RowHeight = 40.dp
