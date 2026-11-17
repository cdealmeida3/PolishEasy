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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.AnswerField
import com.polisheasy.app.ui.components.BasePrompt
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.PlayButton
import com.polisheasy.app.ui.components.PlayControls
import com.polisheasy.app.ui.components.PolishPrompt
import com.polisheasy.app.ui.components.PromptCard
import com.polisheasy.app.ui.components.PromptChip
import com.polisheasy.app.ui.components.QuietButton
import com.polisheasy.app.ui.components.SentenceField
import com.polisheasy.app.ui.components.SpeakerButton
import com.polisheasy.app.ui.components.TonalButton
import com.polisheasy.app.ui.components.WordTileBoard
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.ClipView
import com.polisheasy.engine.InstructionView
import com.polisheasy.app.ui.components.PolishInstruction
import com.polisheasy.app.ui.components.RegisterChip
import com.polisheasy.app.ui.screens.lesson.registerWords
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.polisheasy.engine.ExerciseView
import com.polisheasy.engine.Feedback
import com.polisheasy.engine.Person
import com.polisheasy.engine.TileSelection

// The Exercises as the player shows them, each in two parts: its prompt, which goes at the top of
// the screen, and its answers, which sit at the bottom above the main action.

/** A Polish prompt on its card. With [speak] its Clip plays by itself when the Exercise opens; the speaker plays it again. */
@Composable
internal fun ChooseTranslationPrompt(
    exercise: ExerciseView.ChooseTranslation,
    speak: Boolean,
    onPlay: (ClipView, Boolean) -> Unit,
    polish: InstructionView? = null,
) {
    val clip = exercise.promptAudio
    // Once per showing: not again when the feedback appears, nor when an answered Exercise is rebuilt.
    LaunchedEffect(Unit) { if (speak && clip != null) onPlay(clip, false) }
    ExerciseInstruction(stringResource(R.string.choose_translation_instruction), polish, onPlay)
    Spacer(Modifier.height(Space.s))
    PromptCard(
        speaker = clip?.let { { SpeakerButton(stringResource(R.string.audio_play), onClick = { onPlay(it, false) }) } },
    ) { PolishPrompt(exercise.prompt) }
    // The book marks whether a greeting is formal or informal: the mark goes under the phrase.
    exercise.register?.let { register ->
        Spacer(Modifier.height(Space.xs))
        val words = registerWords(register)
        RegisterChip(register.mark, description = stringResource(R.string.register_description, register.mark, words), label = words)
    }
}

/**
 * What to do, above a prompt. When the course gives the book's instruction, it is in Polish, with
 * its English one tap away and a speaker to hear it; otherwise it is [english].
 */
@Composable
internal fun ExerciseInstruction(english: String, polish: InstructionView?, onPlay: (ClipView, Boolean) -> Unit) {
    if (polish == null) {
        Instruction(english)
        return
    }
    var translated by rememberSaveable { mutableStateOf(false) }
    PolishInstruction(
        polish = polish.polish,
        english = polish.meaning,
        translated = translated,
        translateLabel = stringResource(if (translated) R.string.instruction_hide else R.string.instruction_translate),
        onTranslate = { translated = !translated },
        speaker = polish.audio?.let { clip -> { SpeakerButton(stringResource(R.string.instruction_play), onClick = { onPlay(clip, false) }) } },
    )
}

/**
 * One card per option. Once answered, the right one is marked, and the one tapped if it was wrong.
 * [polish] when the options are Polish.
 */
@Composable
internal fun Options(options: List<String>, tapped: Int?, feedback: Feedback?, polish: Boolean, onChoose: (Int) -> Unit) {
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
            OptionCard(option, state, onClick = { onChoose(index) }, polish = polish)
        }
    }
}

/**
 * The prompt of a listening Exercise: [clip] plays by itself when the Exercise opens, the big
 * speaker plays it again and "Slow", beside it, plays it slowly. They stay usable after the answer.
 */
@Composable
internal fun ListenPrompt(
    instruction: String,
    clip: ClipView,
    playing: Boolean,
    answering: Boolean,
    onPlay: (ClipView, Boolean) -> Unit,
    polish: InstructionView? = null,
) {
    // Once per showing: not again when the feedback appears, nor when an answered Exercise is rebuilt.
    LaunchedEffect(Unit) { if (answering) onPlay(clip, false) }

    ExerciseInstruction(instruction, polish, onPlay)
    Spacer(Modifier.height(Space.l))
    PlayControls(
        play = { PlayButton(stringResource(R.string.audio_play_again), onClick = { onPlay(clip, false) }, playing = playing) },
        helper = { TonalButton(stringResource(R.string.audio_slow), R.drawable.ic_speed, onClick = { onPlay(clip, true) }) },
    )
}

/** The way out of listening, under the answers and quiet: it is rarely needed. Gone once the Exercise is answered. */
@Composable
internal fun CantListen(answering: Boolean, onCantListen: () -> Unit) {
    if (!answering) return
    Spacer(Modifier.height(Space.xs))
    QuietButton(stringResource(R.string.cant_listen_now), onCantListen, modifier = Modifier.fillMaxWidth(), subdued = true)
}

/** A prompt in the Base Language on its card, under what to do with it. */
@Composable
internal fun BasePromptCard(instruction: String, prompt: String, polish: InstructionView? = null, onPlay: (ClipView, Boolean) -> Unit = { _, _ -> }) {
    ExerciseInstruction(instruction, polish, onPlay)
    Spacer(Modifier.height(Space.s))
    PromptCard { BasePrompt(prompt) }
}

/**
 * A conjugation Exercise: the verb and the person in one chip, then the sentence with a slot the
 * Learner types into, and what the sentence means. Without a sentence the chip stands alone and
 * the form is typed into a field below.
 */
@Composable
internal fun ConjugationPrompt(
    exercise: ExerciseView.Conjugation,
    typed: TextFieldValue,
    onTypedChange: (TextFieldValue) -> Unit,
    answering: Boolean,
    onSubmit: () -> Unit,
    polish: InstructionView? = null,
    onPlay: (ClipView, Boolean) -> Unit = { _, _ -> },
) {
    ExerciseInstruction(stringResource(R.string.conjugation_instruction), polish, onPlay)
    Spacer(Modifier.height(Space.s))
    PromptChip(stringResource(R.string.conjugation_chip, exercise.verb, personLabel(exercise.person), exercise.person.pronouns))
    val sentence = exercise.sentence
    if (sentence == null) {
        Spacer(Modifier.height(Space.xs))
        Text(exercise.meaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Spacer(Modifier.height(Space.l))
        SentenceField(
            before = sentence.before,
            after = sentence.after,
            typed = typed,
            onTypedChange = onTypedChange,
            label = stringResource(R.string.type_answer_field_label),
            enabled = answering,
            onDone = onSubmit,
        )
        Spacer(Modifier.height(Space.xs))
        Text(sentence.meaning, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The person in the Base Language, as the Learner is asked for it. */
@Composable
internal fun personLabel(person: Person): String = stringResource(
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
internal fun PolishAnswerField(
    typed: TextFieldValue,
    onTypedChange: (TextFieldValue) -> Unit,
    answering: Boolean,
    onSubmit: () -> Unit,
) {
    AnswerField(typed, onTypedChange, stringResource(R.string.type_answer_field_label), enabled = answering, onDone = onSubmit)
}

/** The answer being built and the bank of tiles. A tile says its word as it is placed, when [clipOf] has a Clip for it. */
@Composable
internal fun WordTilesAnswer(
    exercise: ExerciseView.WordTiles,
    placed: List<Int>,
    onPlacedChange: (List<Int>) -> Unit,
    answering: Boolean,
    clipOf: (String) -> ClipView?,
    onPlay: (ClipView, Boolean) -> Unit,
) {
    // The engine's TileSelection holds the rule for what may be placed and taken back.
    val selection = TileSelection(exercise.tiles, placed)
    WordTileBoard(
        words = exercise.tiles,
        placed = selection.placed,
        enabled = answering,
        onPlace = { tile ->
            clipOf(exercise.tiles[tile])?.let { onPlay(it, false) }
            onPlacedChange(selection.place(tile).placed)
        },
        onTakeBack = { tile -> onPlacedChange(selection.takeBack(tile).placed) },
        answerDescription = stringResource(R.string.word_tiles_answer_description),
    )
}
