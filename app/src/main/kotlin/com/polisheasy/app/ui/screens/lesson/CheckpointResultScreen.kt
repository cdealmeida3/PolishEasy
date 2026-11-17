package com.polisheasy.app.ui.screens.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.BestLine
import com.polisheasy.app.ui.components.Confetti
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.QuietButton
import com.polisheasy.app.ui.components.ScreenFrame
import com.polisheasy.app.ui.components.Seal
import com.polisheasy.app.ui.components.StatRow
import com.polisheasy.app.ui.components.StatTile
import com.polisheasy.app.ui.components.rememberAnswerHaptics
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.engine.CHECKPOINT_PASS_PERCENT
import com.polisheasy.engine.CheckpointResultView

/**
 * The end of a chapter's Test: the seal in the chapter's colour when it passed, a word in Polish,
 * the score and the Best as two facts, and "New Best" when it was beaten. "Try again" asks other
 * questions; "Done" goes back to the Lesson screen. Nothing is locked behind a Test, so a score
 * under the pass mark only says how far there is to go.
 */
@Composable
internal fun CheckpointResultScreen(result: CheckpointResultView, accent: Int, onAgain: () -> Unit, onDone: () -> Unit) {
    BackHandler(onBack = onDone)
    val haptics = rememberAnswerHaptics()
    LaunchedEffect(Unit) { haptics.finished() }
    Box {
        ScreenFrame(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(Space.l))
                Seal(result.percent.toString(), accent = accent, lit = result.passed, description = stringResource(R.string.checkpoint_score_sentence, result.correct, result.questions, result.percent))
                Spacer(Modifier.height(Space.m))
                Text(
                    stringResource(if (result.passed) R.string.checkpoint_passed else R.string.checkpoint_not_passed),
                    style = MaterialTheme.typography.displaySmall.inPolish(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(if (result.passed) R.string.checkpoint_passed_meaning else R.string.checkpoint_not_passed_meaning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    stringResource(R.string.lesson_test) + " · " + result.title,
                    style = MaterialTheme.typography.titleLarge.inPolish(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                if (result.newBest) {
                    Spacer(Modifier.height(Space.l))
                    BestLine(stringResource(R.string.checkpoint_new_best), fresh = true)
                }
                Spacer(Modifier.height(Space.l))
                StatRow(
                    tiles = listOf(
                        { modifier ->
                            StatTile(
                                value = stringResource(R.string.checkpoint_right, result.correct, result.questions),
                                label = stringResource(R.string.checkpoint_score_label),
                                description = stringResource(R.string.checkpoint_score_sentence, result.correct, result.questions, result.percent),
                                modifier = modifier,
                            )
                        },
                        { modifier ->
                            StatTile(
                                value = stringResource(R.string.checkpoint_score, result.best),
                                label = stringResource(R.string.checkpoint_best_label),
                                description = stringResource(R.string.checkpoint_best_sentence, result.best),
                                modifier = modifier,
                            )
                        },
                    ),
                )
                Spacer(Modifier.height(Space.m))
                Text(
                    stringResource(if (result.passed) R.string.checkpoint_sealed_note else R.string.checkpoint_try_note, CHECKPOINT_PASS_PERCENT),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Space.m))
            }
            Spacer(Modifier.height(Space.m))
            PrimaryButton(stringResource(R.string.checkpoint_again), onAgain)
            Spacer(Modifier.height(Space.xs))
            QuietButton(stringResource(R.string.result_done), onDone)
        }
        // A seal newly won is a moment, as a Unit completed is.
        if (result.passed && result.newBest) Confetti(Modifier.matchParentSize())
    }
}
