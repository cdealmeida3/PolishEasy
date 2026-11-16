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
import androidx.compose.ui.text.AnnotatedString
import com.polisheasy.app.ui.theme.withPolish

/** Where one sentence of a Tip ends and the next begins: a full stop, then a capital, a digit or an opening quote. */
private val SENTENCE_END = Regex("(?<=\\.)\\s+(?=[\\p{Lu}\\d\"])")

/** The sentences of [tip]'s body, each with the Polish in it set apart. */
private fun sentencesOf(tip: TipView): List<AnnotatedString> {
    val breaks = SENTENCE_END.findAll(tip.body).map { it.range }.toList()
    val starts = listOf(0) + breaks.map { it.last + 1 }
    val ends = breaks.map { it.first } + tip.body.length
    return starts.zip(ends).map { (start, end) ->
        val within = tip.polish
            .filter { it.first < end && it.last >= start }
            .map { (it.first - start).coerceAtLeast(0)..(it.last - start).coerceAtMost(end - start - 1) }
        withPolish(tip.body.substring(start, end), within)
    }
}

/**
 * A Tip in bites: its title, its first sentence set apart as the one thing to take away, and the
 * rest one sentence to a line, with the Polish the Tip marks in bold. A conjugation table has a
 * speaker on each form [clipOf] has a Clip for.
 */
@Composable
internal fun TipScreen(tip: TipView, clipOf: (String) -> ClipView?, onPlay: (ClipView) -> Unit, onContinue: () -> Unit) {
    val sentences = sentencesOf(tip)
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
