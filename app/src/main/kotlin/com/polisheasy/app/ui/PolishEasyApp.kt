package com.polisheasy.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.polisheasy.app.CourseViewModel
import com.polisheasy.app.R
import com.polisheasy.app.Screen
import com.polisheasy.engine.Answer
import com.polisheasy.engine.ExerciseView
import com.polisheasy.engine.Feedback
import com.polisheasy.engine.Today

@Composable
fun PolishEasyApp(viewModel: CourseViewModel = viewModel()) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val screen = viewModel.screen) {
            is Screen.Home -> HomeScreen(screen.today, onStart = viewModel::startDailySet)
            is Screen.Player -> {
                BackHandler(onBack = viewModel::backToHome)
                PlayerScreen(
                    screen.exercise,
                    screen.feedback,
                    onAnswer = viewModel::answer,
                    onContinue = viewModel::continueAfterFeedback,
                )
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

@Composable
private fun HomeScreen(today: Today, onStart: () -> Unit) {
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
            Today.DoneForToday -> Text(stringResource(R.string.home_done), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PlayerScreen(
    exercise: ExerciseView,
    feedback: Feedback?,
    onAnswer: (Answer) -> Unit,
    onContinue: () -> Unit,
) {
    ScreenColumn {
        when (exercise) {
            is ExerciseView.ChooseTranslation -> ChooseTranslation(exercise, enabled = feedback == null, onAnswer)
        }
        Spacer(Modifier.height(24.dp))
        if (feedback != null) {
            Text(
                text = if (feedback.correct) {
                    stringResource(R.string.feedback_correct)
                } else {
                    stringResource(R.string.feedback_wrong, feedback.correctAnswer)
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
