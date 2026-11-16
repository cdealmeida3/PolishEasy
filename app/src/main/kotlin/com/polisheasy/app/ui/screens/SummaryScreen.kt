package com.polisheasy.app.ui.screens

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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.StreakRaised
import com.polisheasy.app.ui.components.Confetti
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.ItemCard
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.ScoreRing
import com.polisheasy.app.ui.components.ScreenFrame
import com.polisheasy.app.ui.components.StreakCard
import com.polisheasy.app.ui.components.rememberAnswerHaptics
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.ItemView

/**
 * The end of a Daily Set: the score, the Streak it raised, and what was learned. A Replay raises
 * no Streak, lists no Items and gets no confetti: it shows the score alone.
 */
@Composable
internal fun SummaryScreen(
    correctAnswers: Int,
    exercises: Int,
    itemsLearned: List<ItemView>,
    reviewOnly: Boolean,
    replay: Boolean,
    streakRaised: StreakRaised?,
    onDone: () -> Unit,
) {
    val haptics = rememberAnswerHaptics()
    LaunchedEffect(Unit) { haptics.finished() }
    ScreenFrame(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Scrolls when the list of Items is long, so the button below stays on screen.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(Space.m))
                Text(
                    stringResource(
                        when {
                            replay -> R.string.summary_replay_title
                            reviewOnly -> R.string.summary_review_only_title
                            else -> R.string.summary_title
                        },
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(Space.l))
                ScoreRing(if (exercises > 0) correctAnswers.toFloat() / exercises else 0f)
                Spacer(Modifier.height(Space.m))
                Text(
                    stringResource(R.string.summary_accuracy, correctAnswers, exercises),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                if (streakRaised != null) {
                    Spacer(Modifier.height(Space.l))
                    StreakCard(
                        from = streakRaised.from,
                        to = streakRaised.to,
                        title = stringResource(R.string.streak_title),
                        note = stringResource(
                            if (streakRaised.to == 1) R.string.summary_streak_note_first else R.string.summary_streak_note,
                        ),
                        description = pluralStringResource(R.plurals.home_streak, streakRaised.to, streakRaised.to),
                    )
                }
                if (reviewOnly) {
                    Spacer(Modifier.height(Space.l))
                    Text(
                        stringResource(R.string.summary_more_units),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                if (itemsLearned.isNotEmpty()) {
                    Spacer(Modifier.height(Space.xl))
                    Instruction(stringResource(R.string.summary_items_learned), modifier = Modifier.fillMaxWidth())
                    itemsLearned.forEach { item ->
                        Spacer(Modifier.height(Space.xs))
                        ItemCard(item.text, item.meaning)
                    }
                }
                Spacer(Modifier.height(Space.m))
            }
            if (!replay) Confetti(Modifier.matchParentSize())
        }
        Spacer(Modifier.height(Space.m))
        PrimaryButton(stringResource(if (replay) R.string.summary_back_path else R.string.summary_back_home), onDone)
    }
}
