package com.polisheasy.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.polisheasy.app.CourseViewModel
import com.polisheasy.app.R
import com.polisheasy.app.Screen
import com.polisheasy.engine.Answer
import com.polisheasy.engine.ExerciseView
import com.polisheasy.engine.Feedback
import com.polisheasy.engine.SetProgress
import com.polisheasy.engine.TipView
import com.polisheasy.engine.Today
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun PolishEasyApp(viewModel: CourseViewModel = viewModel()) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val screen = viewModel.screen) {
            is Screen.Home -> {
                // Coming back to the app after midnight unlocks the next Daily Set.
                LifecycleResumeEffect(Unit) {
                    viewModel.refreshHome()
                    onPauseOrDispose { }
                }
                HomeScreen(
                    screen.today,
                    onStart = viewModel::startDailySet,
                    onContinue = viewModel::resumeDailySet,
                )
            }
            is Screen.Tip -> {
                BackHandler(onBack = viewModel::backToHome)
                TipScreen(screen.tip, screen.progress, onContinue = viewModel::continueToNext)
            }
            is Screen.Player -> {
                BackHandler(onBack = viewModel::backToHome)
                // Keyed on each showing, so an Exercise that returns starts fresh, typed text included.
                key(screen.shown) {
                    PlayerScreen(
                        screen.exercise,
                        screen.progress,
                        screen.feedback,
                        onAnswer = viewModel::answer,
                        onContinue = viewModel::continueToNext,
                    )
                }
            }
            is Screen.Summary -> {
                BackHandler(onBack = viewModel::backToHome)
                SummaryScreen(screen.correctAnswers, screen.exercises, onDone = viewModel::backToHome)
            }
        }
    }
}

@Composable
private fun ScreenColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

/**
 * A Daily Set screen: the progress bar pinned at the top, the content centred below it, and
 * [bottomBar] along the bottom edge, which is the top of the keyboard while it is open.
 */
@Composable
private fun DailySetColumn(
    progress: SetProgress,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val fraction by animateFloatAsState(
        targetValue = if (progress.total == 0) 0f else progress.correct.toFloat() / progress.total,
        label = "Daily Set progress",
    )
    val description = stringResource(R.string.progress_description, progress.correct, progress.total)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(24.dp),
        ) {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .semantics { contentDescription = description },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                content()
            }
        }
        bottomBar()
    }
}

@Composable
private fun HomeScreen(today: Today, onStart: () -> Unit, onContinue: () -> Unit) {
    ScreenColumn {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(16.dp))
        when (today) {
            Today.DailySetAvailable -> {
                Text(stringResource(R.string.home_ready), textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_start))
                }
            }
            is Today.InProgress -> {
                Text(
                    stringResource(R.string.home_in_progress, today.progress.correct, today.progress.total),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_continue))
                }
            }
            is Today.DoneForToday -> {
                val unlockTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(today.nextUnlock)
                Text(stringResource(R.string.home_done, unlockTime), textAlign = TextAlign.Center)
            }
            Today.AllCompleted -> Text(stringResource(R.string.home_all_completed), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TipScreen(tip: TipView, progress: SetProgress, onContinue: () -> Unit) {
    DailySetColumn(progress) {
        Text(stringResource(R.string.tip_label), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(16.dp))
        Text(tip.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(tip.body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_continue))
        }
    }
}

@Composable
private fun PlayerScreen(
    exercise: ExerciseView,
    progress: SetProgress,
    feedback: Feedback?,
    onAnswer: (Answer) -> Unit,
    onContinue: () -> Unit,
) {
    // Held here so the Polish key row and the text field share it.
    var typed by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    DailySetColumn(
        progress,
        bottomBar = {
            if (exercise is ExerciseView.TypeAnswer && feedback == null) {
                PolishKeyRow(onKey = { letter -> typed = typed.inserting(letter) })
            }
        },
    ) {
        when (exercise) {
            is ExerciseView.ChooseTranslation -> ChooseTranslation(exercise, enabled = feedback == null, onAnswer)
            is ExerciseView.TypeAnswer -> TypeAnswerExercise(
                exercise,
                typed,
                onTypedChange = { typed = it },
                enabled = feedback == null,
                onAnswer,
            )
        }
        Spacer(Modifier.height(24.dp))
        if (feedback != null) {
            Text(
                text = when {
                    feedback.correct && feedback.accentWarning ->
                        stringResource(R.string.feedback_accent_warning, feedback.correctAnswer)
                    feedback.correct -> stringResource(R.string.feedback_correct)
                    else -> stringResource(R.string.feedback_wrong, feedback.correctAnswer)
                },
                color = if (feedback.correct) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_continue))
            }
        }
    }
}

@Composable
private fun ChooseTranslation(
    exercise: ExerciseView.ChooseTranslation,
    enabled: Boolean,
    onAnswer: (Answer) -> Unit,
) {
    Text(stringResource(R.string.choose_translation_instruction), style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(16.dp))
    Text(exercise.prompt, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(32.dp))
    exercise.options.forEachIndexed { index, option ->
        OutlinedButton(
            onClick = { onAnswer(Answer.Option(index)) },
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(
                disabledContentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) { Text(option) }
    }
}

@Composable
private fun TypeAnswerExercise(
    exercise: ExerciseView.TypeAnswer,
    typed: TextFieldValue,
    onTypedChange: (TextFieldValue) -> Unit,
    enabled: Boolean,
    onAnswer: (Answer) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        if (enabled && typed.text.isNotBlank()) {
            // Hidden so the feedback and "Continue" are not behind the keyboard.
            keyboard?.hide()
            onAnswer(Answer.Typed(typed.text))
        }
    }
    // Opens the keyboard as soon as the Exercise appears.
    LaunchedEffect(Unit) { if (enabled) focusRequester.requestFocus() }

    Text(stringResource(R.string.type_answer_instruction), style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(16.dp))
    Text(exercise.prompt, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(32.dp))
    OutlinedTextField(
        value = typed,
        onValueChange = onTypedChange,
        readOnly = !enabled,
        singleLine = true,
        label = { Text(stringResource(R.string.type_answer_field_label)) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
            hintLocales = LocaleList("pl"),
        ),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )
    if (enabled) {
        Spacer(Modifier.height(16.dp))
        Button(onClick = submit, enabled = typed.text.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_check))
        }
    }
}

private val polishLetters = listOf("ą", "ć", "ę", "ł", "ń", "ó", "ś", "ź", "ż")

/** The Polish letters a standard keyboard hides behind a long press, one tap each. */
@Composable
private fun PolishKeyRow(onKey: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        polishLetters.forEach { letter ->
            val description = stringResource(R.string.polish_key_description, letter)
            Surface(
                onClick = { onKey(letter) },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    // A key must not take focus from the text field, or the keyboard would close.
                    .focusProperties { canFocus = false }
                    .semantics { contentDescription = description },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(letter, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

/** This text with [letter] in place of the selection, or at the cursor, and the cursor after it. */
private fun TextFieldValue.inserting(letter: String): TextFieldValue = copy(
    text = text.replaceRange(selection.min, selection.max, letter),
    selection = TextRange(selection.min + letter.length),
)

@Composable
private fun SummaryScreen(correctAnswers: Int, exercises: Int, onDone: () -> Unit) {
    ScreenColumn {
        Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.summary_accuracy, correctAnswers, exercises))
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.summary_back_home))
        }
    }
}
