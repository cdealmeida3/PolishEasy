package com.polisheasy.app.ui.screens.practice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.ButtonTone
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.rememberAnswerHaptics
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.engine.ClipView
import com.polisheasy.engine.Given
import com.polisheasy.engine.MemoryRound
import com.polisheasy.engine.Round
import com.polisheasy.engine.VerbRound
import kotlinx.coroutines.delay

/**
 * The step of a Round being shown, whichever Game it is. A move the Learner makes is handed to
 * [onMove] as the Round it leaves; the rules of every move are the engine's. With [sound] the
 * Polish of an answer is said as it is given, where there is a Clip for it. A Game whose last
 * move should be seen before the result calls [onFinish] when it has been.
 */
@Composable
internal fun RoundScreen(
    round: Round,
    sound: Boolean,
    clipOf: (String) -> ClipView?,
    onPlay: (ClipView) -> Unit,
    onMove: (Round) -> Unit,
    onFinish: () -> Unit,
) {
    val say: (String) -> Unit = { polish -> if (sound) clipOf(polish)?.let(onPlay) }
    when (round) {
        is MemoryRound -> MemoryPairsBoard(round, sound, say, onMove, onFinish)
        is VerbRound -> VerbChallengeStep(round, say, onMove)
        else -> error("No screen for a Round of ${round.game}")
    }
}

/**
 * What follows an answer in every Game: it is felt, right or wrong, once; a right one moves on by
 * itself after a moment, and a wrong one waits for "Continue", so the right answer can be read.
 */
@Composable
internal fun Answered(given: Given?, onNext: () -> Unit, onFirst: (Given) -> Unit = {}) {
    // Felt once, when the answer is given, and not again if the screen is rebuilt.
    var felt by rememberSaveable { mutableStateOf(given != null) }
    val haptics = rememberAnswerHaptics()
    LaunchedEffect(given) {
        if (given == null) return@LaunchedEffect
        if (!felt) {
            felt = true
            if (given.correct) haptics.right() else haptics.wrong()
            onFirst(given)
        }
        if (given.correct) {
            delay(Motion.MOVE_ON_MILLIS)
            onNext()
        }
    }
}

/** "Continue" once a question is answered, in the colour of how it went. Tapping it does not wait for the moment to pass. */
@Composable
internal fun ContinueButton(given: Given, onNext: () -> Unit) {
    PrimaryButton(
        stringResource(R.string.action_continue),
        onNext,
        tone = if (given.correct) ButtonTone.Success else ButtonTone.Caution,
    )
}
