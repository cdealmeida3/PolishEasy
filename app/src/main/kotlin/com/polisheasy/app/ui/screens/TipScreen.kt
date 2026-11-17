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
import com.polisheasy.app.ui.components.Callout
import com.polisheasy.app.ui.components.CalloutTone
import com.polisheasy.app.ui.components.GenderMark
import com.polisheasy.app.ui.components.GrammarCell
import com.polisheasy.app.ui.components.GrammarRow
import com.polisheasy.app.ui.components.GrammarTable
import com.polisheasy.engine.GrammarTableView
import com.polisheasy.engine.TableGender
import com.polisheasy.engine.TipKind
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
 * rest one sentence to a line, with the Polish the Tip marks in bold. An *Uwaga!* or a
 * *Zapamiętaj!* is set on its callout. A table follows, with a speaker on each form of a verb that
 * [clipOf] has a Clip for.
 */
@Composable
internal fun TipScreen(tip: TipView, clipOf: (String) -> ClipView?, onPlay: (ClipView) -> Unit, onContinue: () -> Unit) {
    DailySetStep(
        action = { PrimaryButton(stringResource(R.string.action_continue), onContinue) },
    ) {
        Instruction(stringResource(R.string.tip_label), highlighted = true)
        Spacer(Modifier.height(Space.xs))
        TipContent(tip, clipOf, onPlay)
    }
}

/** A Tip's title, body and table, as the player and the Grammar shelf show it. */
@Composable
internal fun TipContent(tip: TipView, clipOf: (String) -> ClipView?, onPlay: (ClipView) -> Unit) {
    val sentences = sentencesOf(tip)
    Text(tip.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(Space.m))
    when (tip.kind) {
        TipKind.NOTE -> TipPoints(lead = sentences.first(), points = sentences.drop(1))
        TipKind.UWAGA, TipKind.ZAPAMIETAJ -> Callout(
            tone = if (tip.kind == TipKind.UWAGA) CalloutTone.Uwaga else CalloutTone.Zapamietaj,
            label = stringResource(if (tip.kind == TipKind.UWAGA) R.string.tip_uwaga else R.string.tip_zapamietaj),
        ) { TipPoints(lead = sentences.first(), points = sentences.drop(1)) }
    }
    tip.table?.let { table ->
        Spacer(Modifier.height(Space.l))
        TipTable(table, clipOf, onPlay)
    }
}

/** A Tip's table: a verb's forms with a speaker on each, or one of the book's grammar tables. */
@Composable
internal fun TipTable(table: GrammarTableView, clipOf: (String) -> ClipView?, onPlay: (ClipView) -> Unit) {
    val speakable = table.columns.size <= 1 && table.rows.all { it.cells.size == 1 }
    GrammarTable(
        title = table.title,
        columns = table.columns.map { it.header to it.gender?.let { gender -> genderMark(gender) } },
        rows = table.rows.map { row ->
            GrammarRow(
                header = row.header,
                question = row.question,
                cells = row.cells.map { cell ->
                    val clip = if (speakable) clipOf(cell.text) else null
                    GrammarCell(
                        cell.text,
                        cell.ending,
                        speaker = clip?.let { { SpeakerButton(stringResource(R.string.audio_play_word, cell.text), onClick = { onPlay(it) }) } },
                    )
                },
                description = listOfNotNull(
                    row.case?.let { "${it.polishName}, ${it.question}" } ?: row.header,
                    row.cells.joinToString(", ") { it.text },
                ).joinToString(": "),
            )
        },
    )
}

/** The icon, abbreviation and spoken name of a gender over a column. */
@Composable
private fun genderMark(gender: TableGender): GenderMark = when (gender) {
    TableGender.MASCULINE -> GenderMark(R.drawable.ic_man, gender.mark, stringResource(R.string.gender_masculine))
    TableGender.FEMININE -> GenderMark(R.drawable.ic_woman, gender.mark, stringResource(R.string.gender_feminine))
    TableGender.NEUTER -> GenderMark(R.drawable.ic_child_care, gender.mark, stringResource(R.string.gender_neuter))
    TableGender.MASCULINE_PERSONAL -> GenderMark(R.drawable.ic_groups, gender.mark, stringResource(R.string.gender_masculine_personal))
    TableGender.NON_MASCULINE_PERSONAL -> GenderMark(R.drawable.ic_interests, gender.mark, stringResource(R.string.gender_non_masculine_personal))
}

/** How much of the Daily Set is done, from 0 to 1: Tips read and Exercises answered correctly. */
internal val StepProgress.fraction: Float
    get() = if (total == 0) 0f else done.toFloat() / total

@Composable
internal fun StepProgress.description(): String = stringResource(R.string.progress_description, done, total)
