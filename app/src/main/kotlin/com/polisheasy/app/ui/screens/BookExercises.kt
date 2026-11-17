package com.polisheasy.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.AnswerField
import com.polisheasy.app.ui.components.BasePrompt
import com.polisheasy.app.ui.components.DialogueLine
import com.polisheasy.app.ui.components.GapText
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.MatchBoard
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.QuietButton
import com.polisheasy.app.ui.components.ReadingText
import com.polisheasy.app.ui.components.SecondaryButton
import com.polisheasy.app.ui.components.SpeakerButton
import com.polisheasy.app.ui.components.StatementRow
import com.polisheasy.app.ui.components.StickyNote
import com.polisheasy.app.ui.components.TileBank
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.ClipView
import com.polisheasy.engine.ExerciseView
import com.polisheasy.engine.Feedback
import com.polisheasy.engine.InstructionView

// The book's own Exercises in the player, each as a prompt at the top and its answers at the
// bottom. What the Learner has done so far is held by the player screen and passed in.

/** A fill-the-gaps text, its gaps holding what the Learner has put there so far, and what it means. */
@Composable
internal fun FillGapsPrompt(
    exercise: ExerciseView.FillGaps,
    filled: List<String?>,
    selected: Int?,
    feedback: Feedback?,
    onGap: (Int) -> Unit,
    instruction: InstructionView?,
    onPlay: (ClipView, Boolean) -> Unit,
) {
    val resources = LocalContext.current.resources
    ExerciseInstruction(stringResource(R.string.fill_gaps_instruction), instruction, onPlay)
    Spacer(Modifier.height(Space.l))
    GapText(
        parts = exercise.parts,
        filled = filled,
        selected = selected.takeIf { feedback == null },
        marks = feedback?.parts?.takeIf { it.size == exercise.gaps },
        enabled = feedback == null,
        onGap = onGap,
        gapDescription = { gap, word ->
            if (word == null) resources.getString(R.string.fill_gaps_gap, gap + 1) else resources.getString(R.string.fill_gaps_gap_filled, gap + 1, word)
        },
    )
    Spacer(Modifier.height(Space.s))
    Text(exercise.meaning, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * The sticky notes to fill the gaps from, the used ones faded, and the way to type a word
 * instead: [typing] opens a field for the gap selected.
 */
@Composable
internal fun FillGapsAnswer(
    exercise: ExerciseView.FillGaps,
    used: Set<Int>,
    answering: Boolean,
    onNote: (Int) -> Unit,
    typing: Boolean,
    onTyping: (Boolean) -> Unit,
    typed: TextFieldValue,
    onTypedChange: (TextFieldValue) -> Unit,
    selected: Int?,
    onTyped: () -> Unit,
) {
    val usedLabel = stringResource(R.string.fill_gaps_note_used)
    TileBank {
        exercise.notes.forEachIndexed { index, note ->
            StickyNote(note, used = index in used, enabled = answering, onClick = { onNote(index) }, tilt = if (index % 2 == 0) -1 else 1, usedLabel = usedLabel)
        }
    }
    if (!answering) return
    Spacer(Modifier.height(Space.s))
    if (typing && selected != null) {
        AnswerField(typed, onTypedChange, stringResource(R.string.fill_gaps_type_label, selected + 1), enabled = true, onDone = onTyped)
        Spacer(Modifier.height(Space.xs))
        SecondaryButton(stringResource(R.string.fill_gaps_put), onTyped, modifier = Modifier.fillMaxWidth())
    } else {
        QuietButton(stringResource(R.string.fill_gaps_type), { onTyping(true) }, modifier = Modifier.fillMaxWidth(), subdued = true)
    }
}

/** What to do with the pairs, above them. */
@Composable
internal fun MatchPairsPrompt(instruction: InstructionView?, onPlay: (ClipView, Boolean) -> Unit) {
    ExerciseInstruction(stringResource(R.string.match_pairs_instruction), instruction, onPlay)
    Spacer(Modifier.height(Space.xs))
    Text(stringResource(R.string.match_pairs_note), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun MatchPairsAnswer(
    exercise: ExerciseView.MatchPairs,
    joined: Map<Int, Int>,
    selected: Int?,
    shaken: Pair<Int, Int>?,
    answering: Boolean,
    onLeft: (Int) -> Unit,
    onRight: (Int) -> Unit,
) {
    MatchBoard(
        left = exercise.left,
        right = exercise.right,
        joined = joined,
        selected = selected,
        shaken = shaken,
        enabled = answering,
        onLeft = onLeft,
        onRight = onRight,
        joinedDescription = stringResource(R.string.match_pairs_joined),
    )
}

/**
 * A true-or-false text: what it is about, then its lines to read with a speaker on each, or, heard,
 * the big button that plays it all, which it does by itself when the Exercise opens.
 */
@Composable
internal fun TrueFalsePrompt(
    exercise: ExerciseView.TrueFalse,
    answering: Boolean,
    playing: Boolean,
    instruction: InstructionView?,
    onPlay: (ClipView, Boolean) -> Unit,
    onPlayAll: (List<ClipView>) -> Unit,
) {
    ExerciseInstruction(
        stringResource(if (exercise.heard) R.string.listen_true_false_instruction else R.string.true_false_instruction),
        instruction,
        onPlay,
    )
    Spacer(Modifier.height(Space.xs))
    Text(exercise.meaning, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(Space.m))
    if (exercise.heard) {
        LaunchedEffect(Unit) { if (answering) onPlayAll(exercise.clips) }
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            PlayButton(stringResource(R.string.true_false_play), onClick = { onPlayAll(exercise.clips) }, playing = playing)
        }
    } else {
        val speakers = if (exercise.clips.size == exercise.lines.size) {
            exercise.clips.map { clip -> @Composable { SpeakerButton(stringResource(R.string.audio_play), onClick = { onPlay(clip, false) }) } }
        } else {
            emptyList()
        }
        ReadingText(exercise.lines, speakers)
    }
}

/** Each statement with P and N to mark; once known, each mark gets its tick or cross. */
@Composable
internal fun TrueFalseAnswer(exercise: ExerciseView.TrueFalse, marks: List<Boolean?>, feedback: Feedback?, onMark: (Int, Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        exercise.statements.forEachIndexed { index, statement ->
            StatementRow(
                number = index + 1,
                text = statement.text,
                meaning = statement.meaning,
                marked = marks.getOrNull(index),
                right = feedback?.parts?.getOrNull(index),
                enabled = feedback == null,
                onMark = { onMark(index, it) },
                trueLabel = stringResource(R.string.true_false_true, index + 1),
                falseLabel = stringResource(R.string.true_false_false, index + 1),
            )
        }
    }
}

/** The situation of a dialogue to put in order. */
@Composable
internal fun OrderLinesPrompt(exercise: ExerciseView.OrderLines, instruction: InstructionView?, onPlay: (ClipView, Boolean) -> Unit) {
    ExerciseInstruction(stringResource(R.string.order_lines_instruction), instruction, onPlay)
    Spacer(Modifier.height(Space.s))
    PromptCard { BasePrompt(exercise.prompt) }
}

/**
 * The dialogue being put together, its lines numbered, and under it the lines still to place.
 * Tap a line to place it next; tap a placed one to take it back.
 */
@Composable
internal fun OrderLinesAnswer(
    exercise: ExerciseView.OrderLines,
    placed: List<Int>,
    feedback: Feedback?,
    onPlace: (Int) -> Unit,
    onTakeBack: (Int) -> Unit,
) {
    val answering = feedback == null
    val sofar = stringResource(R.string.order_lines_answer)
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.semantics { contentDescription = sofar }) {
        placed.forEachIndexed { position, line ->
            val right = feedback?.parts?.getOrNull(position)
            DialogueLine(
                exercise.lines[line],
                number = position + 1,
                mark = right?.let { if (it) Mark.Right else Mark.Missed },
                enabled = answering,
                onClick = { onTakeBack(line) },
            )
        }
    }
    val rest = exercise.lines.indices.filter { it !in placed }
    if (rest.isNotEmpty()) {
        Spacer(Modifier.height(Space.l))
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            rest.forEach { line -> DialogueLine(exercise.lines[line], number = null, mark = null, enabled = answering, onClick = { onPlace(line) }) }
        }
    }
}
