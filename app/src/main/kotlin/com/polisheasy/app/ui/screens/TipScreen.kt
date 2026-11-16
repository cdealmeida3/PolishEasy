package com.polisheasy.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.ConjugationRowView
import com.polisheasy.app.ui.components.ConjugationTable
import com.polisheasy.app.ui.components.DailySetStep
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.SpeakerButton
import com.polisheasy.app.ui.components.TipPoints
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.ClipView
import com.polisheasy.engine.StepProgress
import com.polisheasy.engine.TipView

/** Where one sentence of a Tip ends and the next begins: a full stop, then a capital, a digit or an opening quote. */
private val SENTENCE_END = Regex("(?<=\\.)\\s+(?=[\\p{Lu}\\d\"])")

/**
 * A Tip in bites: its title, its first sentence set apart as the one thing to take away, and the
 * rest one sentence to a line. A conjugation table has a speaker on each form [clipOf] has a Clip for.
 */
@Composable
internal fun TipScreen(tip: TipView, clipOf: (String) -> ClipView?, onPlay: (ClipView) -> Unit, onContinue: () -> Unit) {
    val sentences = tip.body.split(SENTENCE_END)
    DailySetStep(
        action = { PrimaryButton(stringResource(R.string.action_continue), onContinue) },
    ) {
        Instruction(stringResource(R.string.tip_label), highlighted = true)
        Spacer(Modifier.height(Space.xs))
        Text(tip.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Space.m))
        TipPoints(lead = sentences.first(), points = sentences.drop(1))
        tip.conjugation?.let { table ->
            Spacer(Modifier.height(Space.l))
            ConjugationTable(
                title = stringResource(R.string.conjugation_table_title, table.verb, table.meaning),
                rows = table.rows.map { row ->
                    val clip = clipOf(row.form)
                    ConjugationRowView(
                        row.person.pronouns,
                        row.form,
                        speaker = clip?.let {
                            { SpeakerButton(stringResource(R.string.audio_play_word, row.form), onClick = { onPlay(it) }) }
                        },
                    )
                },
            )
        }
    }
}

/** How much of the Daily Set is done, from 0 to 1: Tips read and Exercises answered correctly. */
internal val StepProgress.fraction: Float
    get() = if (total == 0) 0f else done.toFloat() / total

@Composable
internal fun StepProgress.description(): String = stringResource(R.string.progress_description, done, total)
