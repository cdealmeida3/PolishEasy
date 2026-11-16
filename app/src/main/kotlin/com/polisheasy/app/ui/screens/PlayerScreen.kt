package com.polisheasy.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.polisheasy.app.R
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.components.DailySetStep
import com.polisheasy.app.ui.components.FeedbackPanel
import com.polisheasy.app.ui.components.FeedbackTone
import com.polisheasy.app.ui.components.Instruction
import com.polisheasy.app.ui.components.PolishKeyRow
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.SpeakerButton
import com.polisheasy.app.ui.components.inserting
import com.polisheasy.app.ui.components.rememberAnswerHaptics
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.Answer
import com.polisheasy.engine.ClipView
import com.polisheasy.engine.ExerciseView
import com.polisheasy.engine.Feedback
import com.polisheasy.engine.TileSelection
import androidx.compose.ui.res.stringArrayResource

/**
 * One Exercise of a Daily Set, then what the Learner is told about the answer. What the Learner has
 * typed, placed or tapped is held here, so the Exercise above and "Check" below share it.
 * [praise] picks the word a right answer is praised with, so it is not the same every time.
 * With [canListen] the Polish is said as it appears, without a tap on a speaker.
 * [clipOf] gives the Clip that says a Polish word or sentence, if there is one and sound is on.
 */
@Composable
internal fun PlayerScreen(
    exercise: ExerciseView,
    review: Boolean,
    replay: Boolean,
    feedback: Feedback?,
    gloss: String?,
    playing: Boolean,
    praise: Int,
    canListen: Boolean,
    clipOf: (String) -> ClipView?,
    onAnswer: (Answer) -> Unit,
    onContinue: () -> Unit,
    onPlay: (ClipView, Boolean) -> Unit,
    onCantListen: () -> Unit,
    onReport: () -> Unit,
) {
    var typed by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    // The bank positions of the tiles in the answer, in order. The engine's TileSelection holds the rule.
    var placed by rememberSaveable { mutableStateOf(listOf<Int>()) }
    // The option the Learner tapped, so it can be marked once the answer is known.
    var tapped by rememberSaveable { mutableStateOf<Int?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    // Felt once, when the answer is checked, and not again if the screen is rebuilt.
    var felt by rememberSaveable { mutableStateOf(feedback != null) }
    val haptics = rememberAnswerHaptics()
    LaunchedEffect(feedback) {
        if (feedback != null && !felt) {
            felt = true
            if (feedback.correct) haptics.right() else haptics.wrong()
            // The answer is heard as the feedback appears, unless it is what the Learner has just listened to.
            val listened = exercise is ExerciseView.ListenChoose || exercise is ExerciseView.ListenType
            if (canListen && !listened) feedback.audio?.let { onPlay(it, false) }
        }
    }
    val answering = feedback == null
    val typing = exercise is ExerciseView.TypeAnswer || exercise is ExerciseView.Conjugation ||
        exercise is ExerciseView.ListenType
    val submitTyped = {
        if (answering && typed.text.isNotBlank()) {
            // Hidden so the feedback and "Continue" are not behind the keyboard.
            keyboard?.hide()
            onAnswer(Answer.Typed(typed.text))
        }
    }
    val choose: (Int) -> Unit = { option ->
        tapped = option
        onAnswer(Answer.Option(option))
    }
    val check: (@Composable () -> Unit)? = when {
        typing -> {
            { PrimaryButton(stringResource(R.string.action_check), submitTyped, enabled = typed.text.isNotBlank()) }
        }
        exercise is ExerciseView.WordTiles -> {
            {
                PrimaryButton(
                    stringResource(R.string.action_check),
                    onClick = { onAnswer(TileSelection(exercise.tiles, placed).answer) },
                    enabled = placed.isNotEmpty(),
                )
            }
        }
        else -> null
    }
    DailySetStep(
        feedback = feedback?.let { { Feedback(it, gloss, praise, onContinue, onPlay, onReport) } },
        action = check,
        keyRow = if (typing) {
            {
                PolishKeyRow(
                    onKey = { letter -> typed = typed.inserting(letter) },
                    keyDescription = { letter -> stringResource(R.string.polish_key_description, letter) },
                )
            }
        } else {
            null
        },
        answer = when (exercise) {
            is ExerciseView.ChooseTranslation -> {
                { Options(exercise.options, tapped, feedback, polish = false, choose) }
            }
            is ExerciseView.ListenChoose -> {
                {
                    Options(exercise.options, tapped, feedback, polish = true, choose)
                    CantListen(answering, onCantListen)
                }
            }
            is ExerciseView.ChoosePolish -> {
                { Options(exercise.options, tapped, feedback, polish = true, choose) }
            }
            is ExerciseView.WordTiles -> {
                { WordTilesAnswer(exercise, placed, onPlacedChange = { placed = it }, answering, clipOf, onPlay) }
            }
            is ExerciseView.TypeAnswer -> {
                { PolishAnswerField(typed, onTypedChange = { typed = it }, answering, submitTyped) }
            }
            is ExerciseView.ListenType -> {
                {
                    PolishAnswerField(typed, onTypedChange = { typed = it }, answering, submitTyped)
                    CantListen(answering, onCantListen)
                }
            }
            // With a sentence the Learner types into the sentence itself, up in the prompt.
            is ExerciseView.Conjugation -> if (exercise.sentence == null) {
                { PolishAnswerField(typed, onTypedChange = { typed = it }, answering, submitTyped) }
            } else {
                null
            }
        },
    ) {
        // A Replay has no Review, so the two labels never meet.
        if (review || replay) {
            Instruction(stringResource(if (replay) R.string.replay_label else R.string.review_label), highlighted = true)
            Spacer(Modifier.height(Space.xs))
        }
        when (exercise) {
            is ExerciseView.ChooseTranslation -> ChooseTranslationPrompt(exercise, speak = answering && canListen, onPlay)
            is ExerciseView.TypeAnswer -> BasePromptCard(stringResource(R.string.type_answer_instruction), exercise.prompt)
            is ExerciseView.WordTiles -> BasePromptCard(stringResource(R.string.word_tiles_instruction), exercise.prompt)
            is ExerciseView.Conjugation ->
                ConjugationPrompt(exercise, typed, onTypedChange = { typed = it }, answering, submitTyped)
            is ExerciseView.ListenChoose ->
                ListenPrompt(stringResource(R.string.listen_choose_instruction), exercise.audio, playing, answering, onPlay)
            is ExerciseView.ListenType ->
                ListenPrompt(stringResource(R.string.listen_type_instruction), exercise.audio, playing, answering, onPlay)
            is ExerciseView.ChoosePolish -> BasePromptCard(stringResource(R.string.choose_polish_instruction), exercise.prompt)
        }
    }
}

@Composable
private fun Feedback(feedback: Feedback, gloss: String?, praise: Int, onContinue: () -> Unit, onPlay: (ClipView, Boolean) -> Unit, onReport: () -> Unit) {
    val colors = PolishEasyTheme.colors
    val right = feedback.correct && !feedback.accentWarning
    // A right answer is praised in Polish, a different word each time; a screen reader says "Correct".
    val praises = stringArrayResource(R.array.feedback_praise)
    FeedbackPanel(
        tone = when {
            feedback.correct && feedback.accentWarning -> FeedbackTone.Noted
            feedback.correct -> FeedbackTone.Correct
            else -> FeedbackTone.Wrong
        },
        heading = when {
            right -> praises[praise.mod(praises.size)]
            feedback.correct -> stringResource(R.string.feedback_accepted)
            else -> stringResource(R.string.feedback_wrong)
        },
        headingDescription = if (right) stringResource(R.string.feedback_correct) else null,
        detail = when {
            feedback.correct && feedback.accentWarning ->
                stringResource(R.string.feedback_accent_warning, feedback.correctAnswer)
            // One more look at the other side of the answer: its meaning, or the Polish for it.
            feedback.correct -> gloss
            else -> stringResource(R.string.feedback_correct_answer, feedback.correctAnswer)
        },
        continueLabel = stringResource(R.string.action_continue),
        onContinue = onContinue,
        reportLabel = stringResource(R.string.report_mistake),
        onReport = onReport,
        speaker = feedback.audio?.let { clip ->
            {
                SpeakerButton(
                    stringResource(R.string.audio_play),
                    onClick = { onPlay(clip, false) },
                    tint = if (right) colors.success else colors.caution,
                )
            }
        },
    )
}
