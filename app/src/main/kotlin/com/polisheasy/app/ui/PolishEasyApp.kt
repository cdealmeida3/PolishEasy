package com.polisheasy.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.polisheasy.app.ClipPlayer
import com.polisheasy.app.Clips
import com.polisheasy.app.CourseViewModel
import com.polisheasy.app.Screen
import com.polisheasy.app.ui.screens.HomeScreen
import com.polisheasy.app.ui.screens.PathScreen
import com.polisheasy.app.ui.screens.PlayerScreen
import com.polisheasy.app.ui.screens.SettingsScreen
import com.polisheasy.app.ui.screens.SummaryScreen
import com.polisheasy.app.ui.screens.TipScreen
import com.polisheasy.engine.ClipView

/** [player] plays the Clips, and is released when the app's screens go. */
@Composable
fun PolishEasyApp(viewModel: CourseViewModel = viewModel(), player: Clips = rememberClipPlayer()) {
    val context = LocalContext.current
    DisposableEffect(Unit) { onDispose { player.release() } }
    // The app is silent once it leaves the screen.
    LifecycleStartEffect(Unit) { onStopOrDispose { player.stop() } }
    val onPlay: (ClipView, Boolean) -> Unit = { clip, slow -> player.play(clip.path, slow) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val screen = viewModel.screen) {
            is Screen.Home -> {
                // Coming back to the app after midnight unlocks the next Daily Set.
                LifecycleResumeEffect(Unit) {
                    viewModel.refresh()
                    onPauseOrDispose { }
                }
                HomeScreen(
                    screen.today,
                    screen.streak,
                    onStart = viewModel::startDailySet,
                    onContinue = viewModel::resumeDailySet,
                    onOpenPath = viewModel::openPath,
                    onOpenSettings = viewModel::openSettings,
                )
            }
            is Screen.Settings -> {
                BackHandler(onBack = viewModel::backToHome)
                // Coming back from Android's settings, notifications may have been allowed or turned off.
                LifecycleResumeEffect(Unit) {
                    viewModel.refresh()
                    onPauseOrDispose { }
                }
                SettingsScreen(
                    screen.reminderEnabled,
                    screen.reminderTime,
                    screen.notificationsAllowed,
                    screen.soundOn,
                    onReminderEnabled = viewModel::setReminderEnabled,
                    onReminderTime = viewModel::setReminderTime,
                    onSoundOn = viewModel::setSoundOn,
                    onReport = { reportMistake(context, exerciseId = null) },
                )
            }
            is Screen.Path -> {
                BackHandler(onBack = viewModel::backToHome)
                // Coming back after midnight makes the next Daily Set today's.
                LifecycleResumeEffect(Unit) {
                    viewModel.refresh()
                    onPauseOrDispose { }
                }
                PathScreen(screen.path, onReplay = viewModel::replay, onOpenTodays = viewModel::openTodays)
            }
            is Screen.Tip -> {
                BackHandler(onBack = viewModel::leaveDailySet)
                TipScreen(screen.tip, screen.progress, onContinue = viewModel::continueToNext)
            }
            is Screen.Player -> {
                BackHandler(onBack = viewModel::leaveDailySet)
                // Keyed on each showing, so an Exercise that returns starts fresh, typed text included.
                key(screen.shown) {
                    // A Clip stops when the Learner moves on.
                    DisposableEffect(Unit) { onDispose { player.stop() } }
                    PlayerScreen(
                        screen.exercise,
                        screen.progress,
                        screen.review,
                        screen.replay,
                        screen.feedback,
                        onAnswer = viewModel::answer,
                        onContinue = viewModel::continueToNext,
                        onPlay = onPlay,
                        onCantListen = viewModel::cantListenNow,
                        onReport = { reportMistake(context, screen.exerciseId) },
                    )
                }
            }
            is Screen.Summary -> {
                BackHandler(onBack = viewModel::leaveDailySet)
                SummaryScreen(
                    screen.correctAnswers,
                    screen.exercises,
                    screen.itemsLearned,
                    screen.reviewOnly,
                    screen.replay,
                    onDone = viewModel::leaveDailySet,
                )
            }
        }
    }
}

@Composable
private fun rememberClipPlayer(): Clips {
    val context = LocalContext.current
    return remember { ClipPlayer(context) }
}
