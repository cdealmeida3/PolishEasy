package com.polisheasy.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.AnswerField
import com.polisheasy.app.ui.components.BasePrompt
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.PolishPrompt
import com.polisheasy.app.ui.components.QuietButton
import com.polisheasy.app.ui.components.SpeakerButton
import com.polisheasy.app.ui.components.WordTileBoard
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.ClipView
import com.polisheasy.engine.ExerciseView
import com.polisheasy.engine.Feedback
import com.polisheasy.engine.Person
import com.polisheasy.engine.TileSelection
import androidx.compose.foundation.layout.width
import com.polisheasy.app.ui.components.TonalButton
import com.polisheasy.app.ui.components.PlayControls

// The seven Exercises as the player shows them. Each draws only what is above the main action.

@Composable
internal fun ChooseTranslationExercise(
    exercise: ExerciseView.ChooseTranslation,
    tapped: Int?,
    feedback: Feedback?,
    onChoose: (Int) -> Unit,
    onPlay: (ClipView, Boolean) -> Unit,
) {
    Instruction(stringResource(R.string.choose_translation_instruction))
    Spacer(Modifier.height(Space.xs))
    Row(verticalAlignment = Alignment.CenterVertically) {
        PolishPrompt(exercise.prompt, Modifier.weight(1f, fill = false))
        exercise.promptAudio?.let { clip ->
            SpeakerButton(stringResource(R.string.audio_play), onClick = { onPlay(clip, false) })
        }
    }
    Spacer(Modifier.height(Space.xl))
    Options(exercise.options, tapped, feedback, onChoose)
}

/** One card per option. Once answered, the right one is marked, and the one tapped if it was wrong. */
@Composable
private fun Options(options: List<String>, tapped: Int?, feedback: Feedback?, onChoose: (Int) -> Unit) {
    // The right option is the one that reads as the correct answer.
    val right = feedback?.let { given ->
        options.indexOf(given.correctAnswer).takeIf { it >= 0 } ?: tapped.takeIf { given.correct }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        options.forEachIndexed { index, option ->
            val state = when {
                feedback == null -> OptionState.Idle
                index == right -> OptionState.Right
                index == tapped -> OptionState.Wrong
                else -> OptionState.Faded
            }
            OptionCard(option, state, onClick = { onChoose(index) })
        }
    }
}

/**
 * The controls of a listening Exercise: [clip] plays by itself when the Exercise opens, the big
 * speaker plays it again and "Slow", beside it, plays it slowly. They stay usable after the answer.
 */
@Composable
private fun ListenControls(
    clip: ClipView,
    playing: Boolean,
    answering: Boolean,
    onPlay: (ClipView, Boolean) -> Unit,
) {
    // Once per showing: not again when the feedback appears, nor when an answered Exercise is rebuilt.
    LaunchedEffect(Unit) { if (answering) onPlay(clip, false) }

    PlayControls(
        play = { PlayButton(stringResource(R.string.audio_play_again), onClick = { onPlay(clip, false) }, playing = playing) },
        helper = { TonalButton(stringResource(R.string.audio_slow), R.drawable.ic_speed, onClick = { onPlay(clip, true) }) },
    )
}

/** The way out of listening, under the answers and quiet: it is rarely needed. Gone once the Exercise is answered. */
@Composable
private fun CantListen(answering: Boolean, onCantListen: () -> Unit) {
    if (!answering) return
    Spacer(Modifier.height(Space.xs))
    QuietButton(stringResource(R.string.cant_listen_now), onCantListen, modifier = Modifier.fillMaxWidth(), subdued = true)
}

@Composable
internal fun ListenChooseExercise(
    exercise: ExerciseView.ListenChoose,
    playing: Boolean,
    tapped: Int?,
    feedback: Feedback?,
    onChoose: (Int) -> Unit,
    onPlay: (ClipView, Boolean) -> Unit,
    onCantListen: () -> Unit,
) {
    Instruction(stringResource(R.string.listen_choose_instruction))
    Spacer(Modifier.height(Space.m))
    ListenControls(exercise.audio, playing, answering = feedback == null, onPlay)
    Spacer(Modifier.height(Space.l))
    Options(exercise.options, tapped, feedback, onChoose)
    CantListen(answering = feedback == null, onCantListen)
}

/** The reading form of a listen-and-choose Exercise: its meaning, and the same Polish options. Nothing plays. */
@Composable
internal fun ChoosePolishExercise(
    exercise: ExerciseView.ChoosePolish,
    tapped: Int?,
    feedback: Feedback?,
    onChoose: (Int) -> Unit,
) {
    Instruction(stringResource(R.string.choose_polish_instruction))
    Spacer(Modifier.height(Space.xs))
    BasePrompt(exercise.prompt)
    Spacer(Modifier.height(Space.xl))
    Options(exercise.options, tapped, feedback, onChoose)
}

@Composable
internal fun ListenTypeExercise(
    exercise: ExerciseView.ListenType,
    playing: Boolean,
    typed: TextFieldValue,
    onTypedChange: (TextFieldValue) -> Unit,
    answering: Boolean,
    onSubmit: () -> Unit,
    onPlay: (ClipView, Boolean) -> Unit,
    onCantListen: () -> Unit,
) {
    Instruction(stringResource(R.string.listen_type_instruction))
    Spacer(Modifier.height(Space.m))
    ListenControls(exercise.audio, playing, answering, onPlay)
    Spacer(Modifier.height(Space.l))
    PolishAnswerField(typed, onTypedChange, answering, onSubmit)
    CantListen(answering, onCantListen)
}

@Composable
internal fun TypeAnswerExercise(
    exercise: ExerciseView.TypeAnswer,
    typed: TextFieldValue,
    onTypedChange: (TextFieldValue) -> Unit,
    answering: Boolean,
    onSubmit: () -> Unit,
) {
    Instruction(stringResource(R.string.type_answer_instruction))
    Spacer(Modifier.height(Space.xs))
    BasePrompt(exercise.prompt)
    Spacer(Modifier.height(Space.xl))
    PolishAnswerField(typed, onTypedChange, answering, onSubmit)
}

@Composable
internal fun ConjugationExercise(
    exercise: ExerciseView.Conjugation,
    typed: TextFieldValue,
    onTypedChange: (TextFieldValue) -> Unit,
    answering: Boolean,
    onSubmit: () -> Unit,
) {
    Instruction(stringResource(R.string.conjugation_instruction))
    Spacer(Modifier.height(Space.xs))
    PolishPrompt(exercise.verb)
    Text(
        exercise.meaning,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Space.m))
    Text(
        stringResource(R.string.conjugation_person, personLabel(exercise.person), exercise.person.pronouns),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    exercise.sentence?.let { sentence ->
        Spacer(Modifier.height(Space.l))
        Text(sentence.before + SHOWN_GAP + sentence.after, style = MaterialTheme.typography.titleMedium)
        Text(
            sentence.meaning,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(Space.xl))
    PolishAnswerField(typed, onTypedChange, answering, onSubmit)
}

/** The gap of a sentence as the Learner sees it. */
private const val SHOWN_GAP = "_____"

/** The person in the Base Language, as the Learner is asked for it. */
@Composable
private fun personLabel(person: Person): String = stringResource(
    when (person) {
        Person.I -> R.string.person_i
        Person.YOU -> R.string.person_you
        Person.HE_SHE_IT -> R.string.person_he_she_it
        Person.WE -> R.string.person_we
        Person.YOU_PLURAL -> R.string.person_you_plural
        Person.THEY -> R.string.person_they
    },
)

@Composable
private fun PolishAnswerField(
    typed: TextFieldValue,
    onTypedChange: (TextFieldValue) -> Unit,
    answering: Boolean,
    onSubmit: () -> Unit,
) {
    AnswerField(typed, onTypedChange, stringResource(R.string.type_answer_field_label), enabled = answering, onDone = onSubmit)
}

@Composable
internal fun WordTilesExercise(
    exercise: ExerciseView.WordTiles,
    placed: List<Int>,
    onPlacedChange: (List<Int>) -> Unit,
    answering: Boolean,
) {
    // The engine's TileSelection holds the rule for what may be placed and taken back.
    val selection = TileSelection(exercise.tiles, placed)

    Instruction(stringResource(R.string.word_tiles_instruction))
    Spacer(Modifier.height(Space.xs))
    BasePrompt(exercise.prompt)
    Spacer(Modifier.height(Space.xl))
    WordTileBoard(
        words = exercise.tiles,
        placed = selection.placed,
        enabled = answering,
        onPlace = { tile -> onPlacedChange(selection.place(tile).placed) },
        onTakeBack = { tile -> onPlacedChange(selection.takeBack(tile).placed) },
        answerDescription = stringResource(R.string.word_tiles_answer_description),
    )
}
