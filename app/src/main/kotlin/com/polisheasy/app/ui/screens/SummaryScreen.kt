package com.polisheasy.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.ScreenFrame
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.ItemView

@Composable
internal fun SummaryScreen(
    correctAnswers: Int,
    exercises: Int,
    itemsLearned: List<ItemView>,
    reviewOnly: Boolean,
    replay: Boolean,
    onDone: () -> Unit,
) {
    ScreenFrame(horizontalAlignment = Alignment.CenterHorizontally) {
        // Scrolls when the list of Items is long, so the button below stays on screen.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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
            )
            Spacer(Modifier.height(Space.m))
            Text(
                stringResource(R.string.summary_accuracy, correctAnswers, exercises),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            if (reviewOnly) {
                Spacer(Modifier.height(Space.m))
                Text(
                    stringResource(R.string.summary_more_units),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
            if (itemsLearned.isNotEmpty()) {
                Spacer(Modifier.height(Space.xl))
                Instruction(stringResource(R.string.summary_items_learned))
                itemsLearned.forEach { item ->
                    Spacer(Modifier.height(Space.s))
                    Text(item.text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text(
                        item.meaning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.m))
        PrimaryButton(stringResource(if (replay) R.string.summary_back_path else R.string.summary_back_home), onDone)
    }
}
