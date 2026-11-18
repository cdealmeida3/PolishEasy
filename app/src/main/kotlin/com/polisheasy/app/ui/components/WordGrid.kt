package com.polisheasy.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish

/** What a letter of a Word search grid is showing. Each state but [Open] has words for a screen reader too. */
enum class GridLetterState {
    /** Not part of a word found, and not picked. */
    Open,

    /** Tapped as the first letter of a word, waiting for its last. */
    Picked,

    /** In a word found. */
    Found,

    /** In the mark just made, which spells no word to find. */
    Wrong,
}

/** A letter of a Word search grid: the [letter], its [state], and what a screen reader says of that state, [said]. */
class GridLetterView(val letter: String, val state: GridLetterState, val said: String? = null)

/**
 * A Word search grid: its [rows] of letters as small square tiles, all the grid's width. A tap on
 * one reports its row and column. Letters of words found are in the success colour, the letter
 * picked first in the brand's, and a wrong mark in the caution colour, with one shake. Not
 * [enabled], none responds.
 */
@Composable
fun WordGrid(rows: List<List<GridLetterView>>, onTap: (Int, Int) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = PolishEasyTheme.colors
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(Space.xxs), modifier = modifier.fillMaxWidth()) {
        rows.forEachIndexed { row, letters ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xxs), modifier = Modifier.fillMaxWidth()) {
                letters.forEachIndexed { column, letter ->
                    val (fill, edge, label) = when (letter.state) {
                        GridLetterState.Open -> Triple(scheme.surfaceContainerLowest, scheme.outlineVariant, scheme.onSurface)
                        GridLetterState.Picked -> Triple(scheme.primary, scheme.primary, scheme.onPrimary)
                        GridLetterState.Found -> Triple(colors.successContainer, colors.success, colors.onSuccessContainer)
                        GridLetterState.Wrong -> Triple(colors.cautionContainer, colors.caution, colors.onCautionContainer)
                    }
                    Edged(
                        fill = fill,
                        edge = edge,
                        corner = Corner.extraSmall,
                        border = Edge.border,
                        flat = true,
                        enabled = enabled,
                        onClick = { onTap(row, column) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .shaking(letter.state == GridLetterState.Wrong)
                            .semantics {
                                contentDescription = letter.letter
                                letter.said?.let { stateDescription = it }
                            },
                    ) {
                        Text(letter.letter, style = MaterialTheme.typography.titleMedium.inPolish(), color = label)
                    }
                }
            }
        }
    }
}

/** A word to find in Word search: its [clue], and once found its [word]; [shown] when it was shown rather than found. */
class WordClueView(val clue: String, val word: String? = null, val shown: Boolean = false)

/**
 * What Word search's words mean, as chips under the grid. A word found shows its Polish beside its
 * clue with a tick, and one shown rather than found a cross.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordClues(clues: List<WordClueView>, modifier: Modifier = Modifier) {
    val colors = PolishEasyTheme.colors
    val scheme = MaterialTheme.colorScheme
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
        modifier = modifier.fillMaxWidth(),
    ) {
        clues.forEach { clue ->
            val found = clue.word != null
            val (fill, edge, label) = when {
                !found -> Triple(scheme.surfaceContainerLowest, scheme.outlineVariant, scheme.onSurface)
                clue.shown -> Triple(colors.cautionContainer, colors.caution, colors.onCautionContainer)
                else -> Triple(colors.successContainer, colors.success, colors.onSuccessContainer)
            }
            Edged(fill = fill, edge = edge, corner = Corner.medium, border = Edge.border, flat = true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.xxs),
                    modifier = Modifier.padding(horizontal = Space.s, vertical = Space.xs),
                ) {
                    if (found) MarkIcon(if (clue.shown) Mark.Missed else Mark.Right, size = Size.tileMark)
                    Text(
                        if (clue.word == null) clue.clue else "${clue.word} · ${clue.clue}",
                        style = MaterialTheme.typography.labelLarge.let { if (found) it.inPolish() else it },
                        color = label,
                    )
                }
            }
        }
    }
}
