package com.polisheasy.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.polisheasy.engine.InstructionView
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
    instruction: InstructionView? = null,
    checkpoint: Boolean = false,
    onPlayAll: (List<ClipView>) -> Unit = {},
) {
    var typed by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    // The bank positions of the tiles in the answer, in order. The engine's TileSelection holds the rule.
    var placed by rememberSaveable { mutableStateOf(listOf<Int>()) }
    // The option the Learner tapped, so it can be marked once the answer is known.
    var tapped by rememberSaveable { mutableStateOf<Int?>(null) }
    // Fill the gaps: what is in each gap ("" for none), the note it came from (-1 when typed or empty), and the gap a note goes to next.
    val gapCount = (exercise as? ExerciseView.FillGaps)?.gaps ?: 0
    var gapWords by rememberSaveable { mutableStateOf(List(gapCount) { "" }) }
    var gapNotes by rememberSaveable { mutableStateOf(List(gapCount) { -1 }) }
    var gapSelected by rememberSaveable { mutableStateOf(0) }
    var gapTyping by rememberSaveable { mutableStateOf(false) }
    // Match the pairs: the meaning each word is joined to (-1 for none), the word tapped, and the wrong lines so far.
    val pairCount = (exercise as? ExerciseView.MatchPairs)?.left?.size ?: 0
    var joined by rememberSaveable { mutableStateOf(List(pairCount) { -1 }) }
    var pairSelected by rememberSaveable { mutableStateOf(-1) }
    var wrongPairs by rememberSaveable { mutableStateOf(0) }
    var shaken by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    // True or false: 1 for P, 0 for N, -1 not yet marked.
    val statementCount = (exercise as? ExerciseView.TrueFalse)?.statements?.size ?: 0
    var marks by rememberSaveable { mutableStateOf(List(statementCount) { -1 }) }
    val keyboard = LocalSoftwareKeyboardController.current
    // Felt once, when the answer is checked, and not again if the screen is rebuilt.
    var felt by rememberSaveable { mutableStateOf(feedback != null) }
    val haptics = rememberAnswerHaptics()
    LaunchedEffect(feedback) {
        if (feedback != null && !felt) {
            felt = true
            if (feedback.correct) haptics.right() else haptics.wrong()
            // The answer is heard as the feedback appears, unless it is what the Learner has just listened to.
            val listened = exercise is ExerciseView.ListenChoose || exercise is ExerciseView.ListenType ||
                (exercise is ExerciseView.TrueFalse && exercise.heard)
            if (canListen && !listened) {
                // The finished dialogue plays as a whole; anything else is heard once.
                if (feedback.clips.isNotEmpty()) onPlayAll(feedback.clips) else feedback.audio?.let { onPlay(it, false) }
            }
        }
    }
    val answering = feedback == null
    val typing = exercise is ExerciseView.TypeAnswer || exercise is ExerciseView.Conjugation ||
        exercise is ExerciseView.ListenType || (exercise is ExerciseView.FillGaps && gapTyping && answering)
    val firstEmpty = { words: List<String> -> words.indexOfFirst { it.isEmpty() }.takeIf { it >= 0 } }
    val putInGap = { word: String, note: Int ->
        val gap = gapSelected.takeIf { gapWords.getOrNull(it)?.isEmpty() == true } ?: firstEmpty(gapWords)
        if (gap != null) {
            gapWords = gapWords.toMutableList().also { it[gap] = word }
            gapNotes = gapNotes.toMutableList().also { it[gap] = note }
            gapSelected = firstEmpty(gapWords) ?: gap
        }
    }
    val submitTyped = {
        if (exercise is ExerciseView.FillGaps) {
            if (typed.text.isNotBlank()) {
                putInGap(typed.text.trim(), -1)
                typed = TextFieldValue("")
                gapTyping = false
                keyboard?.hide()
            }
        } else if (answering && typed.text.isNotBlank()) {
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
        exercise is ExerciseView.FillGaps -> {
            {
                PrimaryButton(
                    stringResource(R.string.action_check),
                    onClick = { if (gapWords.none { it.isEmpty() }) onAnswer(Answer.Gaps(gapWords)) },
                    enabled = gapWords.none { it.isEmpty() },
                )
            }
        }
        exercise is ExerciseView.TrueFalse -> {
            {
                PrimaryButton(
                    stringResource(R.string.action_check),
                    onClick = { if (marks.none { it < 0 }) onAnswer(Answer.Marks(marks.map { it == 1 })) },
                    enabled = marks.none { it < 0 },
                )
            }
        }
        exercise is ExerciseView.OrderLines -> {
            {
                PrimaryButton(
                    stringResource(R.string.action_check),
                    onClick = { if (placed.size == exercise.lines.size) onAnswer(Answer.Order(placed.map(exercise.lines::get))) },
                    enabled = placed.size == exercise.lines.size,
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
            is ExerciseView.FillGaps -> {
                {
                    FillGapsAnswer(
                        exercise,
                        used = gapNotes.filter { it >= 0 }.toSet(),
                        answering = answering,
                        onNote = { note -> putInGap(exercise.notes[note], note) },
                        typing = gapTyping,
                        onTyping = { gapTyping = it },
                        typed = typed,
                        onTypedChange = { typed = it },
                        selected = gapSelected.takeIf { gapWords.getOrNull(it)?.isEmpty() == true } ?: firstEmpty(gapWords),
                        onTyped = submitTyped,
                    )
                }
            }
            is ExerciseView.MatchPairs -> {
                {
                    MatchPairsAnswer(
                        exercise,
                        joined = joined.withIndex().filter { it.value >= 0 }.associate { it.index to it.value },
                        selected = pairSelected.takeIf { it >= 0 },
                        shaken = shaken,
                        answering = answering,
                        onLeft = { word ->
                            pairSelected = word
                            clipOf(exercise.left[word])?.let { onPlay(it, false) }
                        },
                        onRight = { meaning ->
                            val word = pairSelected
                            if (word >= 0) {
                                if (exercise.match[word] == meaning) {
                                    haptics.right()
                                    joined = joined.toMutableList().also { it[word] = meaning }
                                    pairSelected = -1
                                    if (joined.none { it < 0 }) onAnswer(Answer.Pairs(wrongPairs))
                                } else {
                                    haptics.wrong()
                                    wrongPairs++
                                    shaken = meaning to wrongPairs
                                }
                            }
                        },
                    )
                }
            }
            is ExerciseView.TrueFalse -> {
                {
                    TrueFalseAnswer(
                        exercise,
                        marks = marks.map { if (it < 0) null else it == 1 },
                        feedback = feedback,
                        onMark = { index, value -> marks = marks.toMutableList().also { it[index] = if (value) 1 else 0 } },
                    )
                    if (exercise.heard) CantListen(answering, onCantListen)
                }
            }
            is ExerciseView.OrderLines -> {
                {
                    OrderLinesAnswer(
                        exercise,
                        placed = placed,
                        feedback = feedback,
                        onPlace = { line -> if (line !in placed) placed = placed + line },
                        onTakeBack = { line -> placed = placed - line },
                    )
                }
            }
        },
    ) {
        // A Replay and a Test have no Review, so the labels never meet.
        if (review || replay || checkpoint) {
            val label = when {
                checkpoint -> R.string.checkpoint_label
                replay -> R.string.replay_label
                else -> R.string.review_label
            }
            Instruction(stringResource(label), highlighted = true)
            Spacer(Modifier.height(Space.xs))
        }
        when (exercise) {
            is ExerciseView.ChooseTranslation -> ChooseTranslationPrompt(exercise, speak = answering && canListen, onPlay, instruction)
            is ExerciseView.TypeAnswer -> BasePromptCard(stringResource(R.string.type_answer_instruction), exercise.prompt, instruction, onPlay)
            is ExerciseView.WordTiles -> BasePromptCard(stringResource(R.string.word_tiles_instruction), exercise.prompt, instruction, onPlay)
            is ExerciseView.Conjugation ->
                ConjugationPrompt(exercise, typed, onTypedChange = { typed = it }, answering, submitTyped, instruction, onPlay)
            is ExerciseView.ListenChoose ->
                ListenPrompt(stringResource(R.string.listen_choose_instruction), exercise.audio, playing, answering, onPlay, instruction)
            is ExerciseView.ListenType ->
                ListenPrompt(stringResource(R.string.listen_type_instruction), exercise.audio, playing, answering, onPlay, instruction)
            is ExerciseView.ChoosePolish -> BasePromptCard(stringResource(R.string.choose_polish_instruction), exercise.prompt, instruction, onPlay)
            is ExerciseView.FillGaps -> FillGapsPrompt(
                exercise,
                filled = gapWords.map { it.ifEmpty { null } },
                selected = gapSelected.takeIf { gapWords.getOrNull(it)?.isEmpty() == true } ?: firstEmpty(gapWords),
                feedback = feedback,
                onGap = { gap ->
                    if (gapWords[gap].isEmpty()) {
                        gapSelected = gap
                    } else {
                        // A filled gap gives its word back: the note to the bank, or the typed word away.
                        gapWords = gapWords.toMutableList().also { it[gap] = "" }
                        gapNotes = gapNotes.toMutableList().also { it[gap] = -1 }
                        gapSelected = gap
                    }
                },
                instruction = instruction,
                onPlay = onPlay,
            )
            is ExerciseView.MatchPairs -> MatchPairsPrompt(instruction, onPlay)
            is ExerciseView.TrueFalse -> TrueFalsePrompt(exercise, answering, playing, instruction, onPlay, onPlayAll)
            is ExerciseView.OrderLines -> OrderLinesPrompt(exercise, instruction, onPlay)
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
