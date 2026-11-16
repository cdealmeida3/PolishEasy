package com.polisheasy.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.polisheasy.app.ClipPlayer
import com.polisheasy.app.Clips
import com.polisheasy.app.CourseViewModel
import com.polisheasy.app.R
import com.polisheasy.app.Screen
import com.polisheasy.app.ui.components.DailySetFrame
import com.polisheasy.app.ui.screens.HomeScreen
import com.polisheasy.app.ui.screens.PathScreen
import com.polisheasy.app.ui.screens.PlayerScreen
import com.polisheasy.app.ui.screens.SettingsScreen
import com.polisheasy.app.ui.screens.SummaryScreen
import com.polisheasy.app.ui.screens.TipScreen
import com.polisheasy.app.ui.screens.description
import com.polisheasy.app.ui.screens.fraction
import com.polisheasy.engine.SetProgress

/** [player] plays the Clips, and is released when the app's screens go. */
@Composable
fun PolishEasyApp(viewModel: CourseViewModel = viewModel(), player: Clips = rememberClipPlayer()) {
    val context = LocalContext.current
    DisposableEffect(Unit) { onDispose { player.release() } }
    // The app is silent once it leaves the screen.
    LifecycleStartEffect(Unit) { onStopOrDispose { player.stop() } }
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
            is Screen.Tip -> DailySet(screen, screen.shown, screen.progress, viewModel, player)
            is Screen.Player -> DailySet(screen, screen.shown, screen.progress, viewModel, player)
            is Screen.Summary -> {
                BackHandler(onBack = viewModel::leaveDailySet)
                SummaryScreen(
                    screen.correctAnswers,
                    screen.exercises,
                    screen.itemsLearned,
                    screen.reviewOnly,
                    screen.replay,
                    screen.streakRaised,
                    onDone = viewModel::leaveDailySet,
                )
            }
        }
    }
}

/**
 * The Tip or Exercise the Learner is on. [shown] names it: when it changes, the old step slides
 * out while the new one slides in, and only the new one answers taps.
 */
@Composable
private fun DailySet(screen: Screen, shown: Int, progress: SetProgress, viewModel: CourseViewModel, player: Clips) {
    val context = LocalContext.current
    val current by rememberUpdatedState(shown)
    BackHandler(onBack = viewModel::leaveDailySet)
    // A Clip stops when the Learner leaves the Daily Set, and when they move on within it.
    DisposableEffect(Unit) { onDispose { player.stop() } }
    DailySetFrame(
        progress = progress.fraction,
        progressDescription = progress.description(),
        closeLabel = stringResource(R.string.daily_set_close),
        onClose = viewModel::leaveDailySet,
        step = shown,
    ) { step ->
        // A step on its way out keeps what it last showed.
        val held = remember { arrayOf(screen) }
        if (step == shown) held[0] = screen
        val live: (() -> Unit) -> Unit = { act -> if (step == current) act() }
        val moveOn = {
            live {
                player.stop()
                viewModel.continueToNext()
            }
        }
        when (val showing = held[0]) {
            is Screen.Tip -> TipScreen(showing.tip, onContinue = moveOn)
            is Screen.Player -> PlayerScreen(
                showing.exercise,
                showing.review,
                showing.replay,
                showing.feedback,
                playing = player.playing,
                onAnswer = { answer -> live { viewModel.answer(answer) } },
                onContinue = moveOn,
                onPlay = { clip, slow -> live { player.play(clip.path, slow) } },
                onCantListen = {
                    live {
                        player.stop()
                        viewModel.cantListenNow()
                    }
                },
                onReport = { live { reportMistake(context, showing.exerciseId) } },
            )
            else -> Unit
        }
    }
}

@Composable
private fun rememberClipPlayer(): Clips {
    val context = LocalContext.current
    return remember { ClipPlayer(context) }
}
