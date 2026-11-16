package com.polisheasy.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.ConjugationTable
import com.polisheasy.app.ui.components.DailySetFrame
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.SetProgress
import com.polisheasy.engine.TipView

@Composable
internal fun TipScreen(tip: TipView, progress: SetProgress, onContinue: () -> Unit) {
    DailySetFrame(
        progress = progress.fraction,
        progressDescription = progress.description(),
        action = { PrimaryButton(stringResource(R.string.action_continue), onContinue) },
    ) {
        Instruction(stringResource(R.string.tip_label), highlighted = true)
        Spacer(Modifier.height(Space.xs))
        Text(tip.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(Space.m))
        Text(tip.body, style = MaterialTheme.typography.bodyLarge)
        tip.conjugation?.let { table ->
            Spacer(Modifier.height(Space.l))
            ConjugationTable(
                title = stringResource(R.string.conjugation_table_title, table.verb, table.meaning),
                rows = table.rows.map { it.person.pronouns to it.form },
            )
        }
    }
}

/** How much of the Daily Set is answered correctly, from 0 to 1. */
internal val SetProgress.fraction: Float
    get() = if (total == 0) 0f else correct.toFloat() / total

@Composable
internal fun SetProgress.description(): String = stringResource(R.string.progress_description, correct, total)
