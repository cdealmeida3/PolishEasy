package com.polisheasy.app.ui.screens.practice

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.GridLetterState
import com.polisheasy.app.ui.components.GridLetterView
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.RoundStep
import com.polisheasy.app.ui.components.SecondaryButton
import com.polisheasy.app.ui.components.WordClueView
import com.polisheasy.app.ui.components.WordClues
import com.polisheasy.app.ui.components.WordGrid
import com.polisheasy.app.ui.components.rememberAnswerHaptics
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.Cell
import com.polisheasy.engine.Round
import com.polisheasy.engine.WordSearchRound
import kotlinx.coroutines.delay

/**
 * Word search, chapter 16's Game. The grid of letters, and under it what each hidden word means.
 * The Learner taps a word's first letter and then its last; a word found turns green, is said,
 * and shows its Polish beside its clue, and a mark that spells no word shakes. "Show me a word"
 * finds one for the Learner, counted as missed. Once every word is found the grid stays a moment
 * before [onFinish] ends the Round.
 */
@Composable
internal fun WordSearchStep(round: WordSearchRound, say: (String) -> Unit, onMove: (Round) -> Unit, onFinish: () -> Unit) {
    val haptics = rememberAnswerHaptics()
    LaunchedEffect(round.finished) {
        if (round.finished) {
            delay(Motion.BOARD_DONE_MILLIS)
            onFinish()
        }
    }
    val found = round.words.filter { it.id in round.found }.flatMap { it.letters }.toSet()
    val wrong = round.wrong.orEmpty().toSet()
    val foundSaid = stringResource(R.string.word_search_found_letter)
    val pickedSaid = stringResource(R.string.word_search_picked_letter)
    val rows = round.grid.mapIndexed { row, letters ->
        letters.mapIndexed { column, letter ->
            val cell = Cell(row, column)
            when {
                cell == round.first -> GridLetterView(letter.toString(), GridLetterState.Picked, pickedSaid)
                cell in wrong -> GridLetterView(letter.toString(), GridLetterState.Wrong)
                cell in found -> GridLetterView(letter.toString(), GridLetterState.Found, foundSaid)
                else -> GridLetterView(letter.toString(), GridLetterState.Open)
            }
        }
    }
    val show = {
        val next = round.show()
        next.last?.let { say(it.word) }
        onMove(next)
    }
    RoundStep(action = if (round.finished) null else { { SecondaryButton(stringResource(R.string.word_search_show), show) } }) {
        Instruction(stringResource(R.string.word_search_instruction))
        Spacer(Modifier.height(Space.s))
        WordGrid(
            rows = rows,
            onTap = { row, column ->
                val next = round.tap(Cell(row, column))
                when {
                    next.found.size > round.found.size -> {
                        haptics.right()
                        next.last?.let { say(it.word) }
                    }
                    next.wrong != null -> haptics.wrong()
                    else -> haptics.tick()
                }
                onMove(next)
            },
            enabled = !round.finished,
        )
        Spacer(Modifier.height(Space.m))
        WordClues(round.words.map { word -> WordClueView(word.clue, word.word.takeIf { word.id in round.found }, shown = word.id in round.shown) })
    }
}
