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
import com.polisheasy.app.ui.components.NavigationFrame
import com.polisheasy.app.ui.components.NavigationTab
import com.polisheasy.app.ui.screens.HomeScreen
import com.polisheasy.app.ui.screens.PathScreen
import com.polisheasy.app.ui.screens.PlayerScreen
import com.polisheasy.app.ui.screens.SettingsScreen
import com.polisheasy.app.ui.screens.SummaryScreen
import com.polisheasy.app.ui.screens.TipScreen
import com.polisheasy.app.ui.screens.description
import com.polisheasy.app.ui.screens.fraction
import com.polisheasy.engine.StepProgress
import com.polisheasy.app.ui.screens.WelcomeScreen

/** [player] plays the Clips, and is released when the app's screens go. */
@Composable
fun PolishEasyApp(viewModel: CourseViewModel = viewModel(), player: Clips = rememberClipPlayer()) {
    val context = LocalContext.current
    DisposableEffect(Unit) { onDispose { player.release() } }
    // The app is silent once it leaves the screen.
    LifecycleStartEffect(Unit) { onStopOrDispose { player.stop() } }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val screen = viewModel.screen) {
            Screen.Welcome -> WelcomeScreen(onStart = viewModel::startFromWelcome)
            is Screen.Home, is Screen.Path, is Screen.Settings -> Tabs(screen, viewModel)
            is Screen.Tip -> DailySet(screen, screen.shown, screen.steps, viewModel, player)
            is Screen.Player -> DailySet(screen, screen.shown, screen.steps, viewModel, player)
            is Screen.Summary -> {
                BackHandler(onBack = viewModel::leaveDailySet)
                SummaryScreen(
                    screen.correctAnswers,
                    screen.exercises,
                    screen.itemsLearned,
                    screen.reviewOnly,
                    screen.replay,
                    screen.streakRaised,
                    screen.week,
                    clipOf = viewModel::clipOf,
                    onPlay = { clip -> player.play(clip.path) },
                    onDone = {
                        player.stop()
                        viewModel.leaveDailySet()
                    },
                    reminder = screen.reminder,
                    unitCompleted = screen.unitCompleted,
                    onRemind = viewModel::acceptReminder,
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
private fun DailySet(screen: Screen, shown: Int, progress: StepProgress, viewModel: CourseViewModel, player: Clips) {
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
            is Screen.Tip -> TipScreen(
                showing.tip,
                clipOf = viewModel::clipOf,
                onPlay = { clip -> live { player.play(clip.path) } },
                onContinue = moveOn,
            )
            is Screen.Player -> PlayerScreen(
                showing.exercise,
                showing.review,
                showing.replay,
                showing.feedback,
                showing.gloss,
                playing = player.playing,
                praise = showing.progress.correct,
                canListen = showing.canListen,
                clipOf = viewModel::clipOf,
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

/** Home, the Path or Settings, with the navigation bar that moves between them. Back goes to Home. */
@Composable
private fun Tabs(screen: Screen, viewModel: CourseViewModel) {
    val context = LocalContext.current
    // Whichever of the three is showing, coming back to the app asks again what it shows: midnight
    // may have unlocked the next Daily Set, and Android's settings may have changed notifications.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val selected = when (screen) {
        is Screen.Path -> 1
        is Screen.Settings -> 2
        else -> 0
    }
    val held = remember { arrayOfNulls<Screen>(3) }
    NavigationFrame(
        tabs = listOf(
            NavigationTab(stringResource(R.string.nav_home), R.drawable.ic_home),
            NavigationTab(stringResource(R.string.nav_path), R.drawable.ic_route),
            NavigationTab(stringResource(R.string.nav_settings), R.drawable.ic_settings),
        ),
        selected = selected,
        onSelect = { tab ->
            when (tab) {
                1 -> viewModel.openPath()
                2 -> viewModel.openSettings()
                else -> viewModel.backToHome()
            }
        },
    ) { tab ->
        // A tab on its way out keeps what it last showed.
        if (tab == selected) held[tab] = screen
        when (val showing = held[tab]) {
            is Screen.Home -> HomeScreen(
                showing.today,
                showing.streak,
                showing.week,
                showing.next,
                onStart = viewModel::startDailySet,
                onContinue = viewModel::resumeDailySet,
                soFar = showing.soFar,
                streakRaisedFrom = showing.streakRaisedFrom,
                onReplayToday = if (showing.replayToday) viewModel::replayToday else null,
            )
            is Screen.Path -> {
                BackHandler(enabled = tab == selected, onBack = viewModel::backToHome)
                PathScreen(showing.path, onReplay = viewModel::replay, onOpenTodays = viewModel::openTodays, today = showing.today, started = showing.started)
            }
            is Screen.Settings -> {
                BackHandler(enabled = tab == selected, onBack = viewModel::backToHome)
                SettingsScreen(
                    showing.reminderEnabled,
                    showing.reminderTime,
                    showing.notificationsAllowed,
                    showing.soundOn,
                    onReminderEnabled = viewModel::setReminderEnabled,
                    onReminderTime = viewModel::setReminderTime,
                    onSoundOn = viewModel::setSoundOn,
                    onReport = { reportMistake(context, exerciseId = null) },
                )
            }
            else -> Unit
        }
    }
}

@Composable
private fun rememberClipPlayer(): Clips {
    val context = LocalContext.current
    return remember { ClipPlayer(context) }
}
