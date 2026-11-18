package com.polisheasy.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.polisheasy.app.RecordingPlayer
import com.polisheasy.app.RecordingTitle
import com.polisheasy.app.Recordings
import com.polisheasy.app.LocalSpeech
import com.polisheasy.app.Speech
import com.polisheasy.app.rememberSpeech
import androidx.compose.runtime.LaunchedEffect
import com.polisheasy.app.ui.screens.ClassAudioScreen
import com.polisheasy.app.ui.screens.GrammarScreen
import com.polisheasy.app.ui.screens.lesson.ListenScreen
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
import com.polisheasy.engine.TimedRound
import com.polisheasy.app.ui.screens.WelcomeScreen
import com.polisheasy.app.ui.screens.lesson.CheckpointResultScreen
import com.polisheasy.app.ui.screens.lesson.DictionaryScreen
import com.polisheasy.app.ui.screens.lesson.FlashcardsScreen
import com.polisheasy.app.ui.screens.lesson.LessonScreen
import com.polisheasy.app.ui.components.RoundFrame
import com.polisheasy.app.ui.screens.practice.GameSetupScreen
import com.polisheasy.app.ui.screens.practice.PracticeScreen
import com.polisheasy.app.ui.screens.practice.PictureCardsScreen
import com.polisheasy.app.ui.screens.practice.RoundResultScreen
import com.polisheasy.app.ui.screens.practice.RoundScreen
import com.polisheasy.app.ui.screens.practice.progressWords

/**
 * [player] plays the Clips and [recordings] the class recordings; both are released when the
 * app's screens go.
 */
@Composable
fun PolishEasyApp(
    viewModel: CourseViewModel = viewModel(),
    player: Clips = rememberClipPlayer(),
    recordings: Recordings = rememberRecordings(),
    speech: Speech = rememberSpeech(),
) {
    DisposableEffect(Unit) {
        onDispose {
            player.release()
            recordings.release()
            speech.release()
        }
    }
    // A Game offers saying an answer only where the phone can hear it.
    LaunchedEffect(speech.available) { viewModel.speaking = speech.available }
    CompositionLocalProvider(LocalSpeech provides speech) { Screens(viewModel, player, recordings) }
}

@Composable
private fun Screens(viewModel: CourseViewModel, player: Clips, recordings: Recordings) {
    val context = LocalContext.current
    // The app is silent once it leaves the screen.
    LifecycleStartEffect(Unit) { onStopOrDispose { player.stop() } }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val screen = viewModel.screen) {
            Screen.Welcome -> WelcomeScreen(onStart = viewModel::startFromWelcome)
            is Screen.Home, is Screen.Path, is Screen.Practice, is Screen.Settings -> Tabs(screen, viewModel)
            is Screen.PictureCards -> {
                BackHandler(onBack = viewModel::leaveGame)
                DisposableEffect(Unit) { onDispose { player.stop() } }
                PictureCardsScreen(
                    screen.view,
                    sound = screen.sound,
                    clipOf = viewModel::practiceClipOf,
                    onPlay = { clip -> player.play(clip.path) },
                    onKind = viewModel::chooseCards,
                    onDeck = viewModel::chooseDeck,
                    onBack = viewModel::leaveGame,
                )
            }
            is Screen.GameSetup -> {
                BackHandler(onBack = viewModel::leaveGame)
                GameSetupScreen(screen.setup, onChoose = viewModel::choose, onPlay = viewModel::play, onBack = viewModel::leaveGame)
            }
            is Screen.Round -> Round(screen, viewModel, player)
            is Screen.RoundResult -> RoundResultScreen(
                screen.result,
                onPlayAgain = viewModel::play,
                onDone = {
                    player.stop()
                    viewModel.leaveGame()
                },
            )
            is Screen.Tip -> DailySet(screen, screen.shown, screen.steps, viewModel, player)
            is Screen.Player -> DailySet(screen, screen.shown, screen.steps, viewModel, player)
            is Screen.Lesson -> {
                BackHandler(onBack = viewModel::openPath)
                DisposableEffect(Unit) { onDispose { player.stop() } }
                LifecycleResumeEffect(Unit) {
                    viewModel.refresh()
                    onPauseOrDispose { }
                }
                LessonScreen(
                    screen.lesson,
                    screen.path,
                    onSection = viewModel::openSection,
                    onDictionary = viewModel::openDictionary,
                    onFlashcards = viewModel::openFlashcards,
                    onTest = viewModel::startCheckpoint,
                    onBack = viewModel::openPath,
                    onPlay = { clip -> player.play(clip.path) },
                    today = screen.today,
                    started = screen.started,
                    oneADay = screen.oneADay,
                    inProgress = screen.inProgress,
                    sound = viewModel.soundOn,
                    listen = screen.listen,
                    onRecording = viewModel::openRecording,
                    onClassAudio = viewModel::openClassAudio,
                    onGrammar = viewModel::openGrammar,
                    games = screen.games,
                    onGame = viewModel::openChapterGame,
                    onChat = viewModel::openChapterChat,
                )
            }
            is Screen.ClassAudio -> {
                BackHandler(onBack = viewModel::leaveClassAudio)
                ClassAudioScreen(
                    screen.view,
                    importing = screen.importing,
                    result = screen.result,
                    failed = screen.failed,
                    onFiles = { viewModel.importClassAudio(files = it) },
                    onFolder = { viewModel.importClassAudio(folder = it) },
                    onRemove = viewModel::removeClassAudio,
                    onBack = viewModel::leaveClassAudio,
                )
            }
            is Screen.Listen -> Listen(screen, viewModel, player, recordings)
            is Screen.Grammar -> {
                BackHandler(onBack = viewModel::leaveGrammar)
                DisposableEffect(Unit) { onDispose { player.stop() } }
                GrammarScreen(
                    screen.chapters,
                    one = screen.from != null,
                    clipOf = { polish -> if (screen.sound) viewModel.practiceClipOf(polish) else null },
                    onPlay = { clip -> player.play(clip.path) },
                    onBack = viewModel::leaveGrammar,
                )
            }
            is Screen.Dictionary -> {
                BackHandler(onBack = viewModel::backToLesson)
                DisposableEffect(Unit) { onDispose { player.stop() } }
                DictionaryScreen(
                    screen.lesson,
                    screen.words,
                    sound = screen.sound,
                    onPlay = { clip -> player.play(clip.path) },
                    onBack = viewModel::backToLesson,
                )
            }
            is Screen.Flashcards -> {
                BackHandler(onBack = viewModel::backToLesson)
                DisposableEffect(Unit) { onDispose { player.stop() } }
                FlashcardsScreen(
                    screen.lesson,
                    screen.cards,
                    screen.index,
                    screen.knew,
                    screen.shown,
                    sound = screen.sound,
                    onAnswer = { knew ->
                        player.stop()
                        viewModel.answerFlashcard(knew)
                    },
                    onAgain = viewModel::flashcardsAgain,
                    onPlay = { clip -> player.play(clip.path) },
                    onBack = viewModel::backToLesson,
                )
            }
            is Screen.Summary -> if (screen.checkpoint != null) {
                CheckpointResultScreen(
                    screen.checkpoint,
                    accent = screen.accent,
                    onAgain = viewModel::checkpointAgain,
                    onDone = viewModel::leaveDailySet,
                )
            } else {
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
                    onStartNext = if (screen.startNext) {
                        {
                            player.stop()
                            viewModel.startNext()
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/**
 * A class recording in the player. It opens when the screen does, and again for each recording
 * skipped to; leaving the screen stops it. Leaving the app does not: it plays on in the background.
 */
@Composable
private fun Listen(screen: Screen.Listen, viewModel: CourseViewModel, player: Clips, recordings: Recordings) {
    val track = screen.play.track
    val title = RecordingTitle(
        stringResource(R.string.listen_notification_title, track.number, track.title),
        stringResource(R.string.listen_notification_chapter, stringResource(R.string.lesson_label, track.lesson)),
    )
    LaunchedEffect(track.number) {
        player.stop()
        recordings.open(track.number, viewModel.recordingFile(track.number), title, screen.play.lines)
    }
    DisposableEffect(Unit) { onDispose { recordings.close() } }
    BackHandler(onBack = viewModel::backToLesson)
    val state = recordings.state
    ListenScreen(
        screen.lesson,
        screen.play,
        state,
        screen.answered,
        onPlayPause = recordings::playPause,
        onSkip = recordings::skip,
        onLine = recordings::line,
        onSpeed = recordings::speed,
        onRepeat = recordings::repeat,
        onStep = recordings::step,
        onHold = recordings::hold,
        onAnswer = viewModel::answerListening,
        onRecording = viewModel::openRecording,
        onBack = viewModel::backToLesson,
    )
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
        // Any question may be skipped before it is answered.
        onSkip = (screen as? Screen.Player)?.takeIf { it.feedback == null }?.let {
            {
                player.stop()
                viewModel.skip()
            }
        },
        skipLabel = stringResource(R.string.exercise_skip),
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
                instruction = showing.instruction,
                checkpoint = showing.checkpoint,
                playing = player.playing,
                praise = showing.progress.correct,
                canListen = showing.canListen,
                clipOf = viewModel::clipOf,
                onAnswer = { answer -> live { viewModel.answer(answer) } },
                onContinue = moveOn,
                onPlay = { clip, slow -> live { player.play(clip.path, slow) } },
                onPlayAll = { clips -> live { player.playAll(clips.map { it.path }) } },
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

/**
 * The Round being played. Its steps slide as a Daily Set's do, and only the one showing answers
 * taps. Closing leaves at once: a Round left unfinished is not kept.
 */
@Composable
private fun Round(screen: Screen.Round, viewModel: CourseViewModel, player: Clips) {
    val shown = screen.number to screen.round.step
    val current by rememberUpdatedState(shown)
    BackHandler(onBack = viewModel::leaveGame)
    DisposableEffect(Unit) { onDispose { player.stop() } }
    RoundFrame(
        // The bar of a Round against the clock (the Quick round, Register sort, Opposites snap) is its clock, and empties; every other Round's fills as it is answered.
        progress = (screen.round as? TimedRound)?.let { it.left.toFloat() / it.seconds }
            ?: (screen.round.done.toFloat() / screen.round.total.coerceAtLeast(1)),
        progressDescription = progressWords(screen.round),
        closeLabel = stringResource(R.string.round_close),
        onClose = viewModel::leaveGame,
        step = shown,
    ) { step ->
        // A step on its way out keeps what it last showed.
        val held = remember { arrayOf(screen) }
        if (step == shown) held[0] = screen
        val showing = held[0]
        RoundScreen(
            showing.round,
            sound = showing.sound,
            clipOf = viewModel::practiceClipOf,
            onPlay = { clip -> if (step == current) player.play(clip.path) },
            onPlayAll = { clips -> if (step == current) player.playAll(clips.map { it.path }) },
            onMove = { round -> if (step == current) viewModel.move(round) },
            onFinish = { if (step == current) viewModel.endRound() },
        )
    }
}

/** Home, the Path, Practice or Settings, with the navigation bar that moves between them. Back goes to Home. */
@Composable
private fun Tabs(screen: Screen, viewModel: CourseViewModel) {
    val context = LocalContext.current
    // Whichever of them is showing, coming back to the app asks again what it shows: midnight
    // may have unlocked the next Daily Set, and Android's settings may have changed notifications.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val selected = when (screen) {
        is Screen.Path -> 1
        is Screen.Practice -> 2
        is Screen.Settings -> 3
        else -> 0
    }
    val held = remember { arrayOfNulls<Screen>(4) }
    NavigationFrame(
        tabs = listOf(
            NavigationTab(stringResource(R.string.nav_home), R.drawable.ic_home),
            NavigationTab(stringResource(R.string.nav_path), R.drawable.ic_route),
            NavigationTab(stringResource(R.string.nav_practice), R.drawable.ic_stadia_controller),
            NavigationTab(stringResource(R.string.nav_settings), R.drawable.ic_settings),
        ),
        selected = selected,
        onSelect = { tab ->
            when (tab) {
                1 -> viewModel.openPath()
                2 -> viewModel.openPractice()
                3 -> viewModel.openSettings()
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
                onPractice = viewModel::openPractice,
                doneToday = showing.doneToday,
                practiceOpen = showing.practiceOpen,
            )
            is Screen.Path -> {
                BackHandler(enabled = tab == selected, onBack = viewModel::backToHome)
                PathScreen(showing.shelves, onOpen = viewModel::openLesson)
            }
            is Screen.Practice -> {
                BackHandler(enabled = tab == selected, onBack = viewModel::backToHome)
                PracticeScreen(
                    showing.view,
                    showing.today,
                    onOpen = viewModel::openGame,
                    onStart = viewModel::startDailySet,
                    onContinue = viewModel::resumeDailySet,
                )
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
                    oneADay = showing.oneADay,
                    onOneADay = viewModel::setOneADay,
                    classAudio = showing.classAudio,
                    onClassAudio = viewModel::openClassAudio,
                    onGrammar = viewModel::openGrammar,
                )
            }
            else -> Unit
        }
    }
}

@Composable
private fun rememberRecordings(): Recordings {
    val context = LocalContext.current
    return remember { RecordingPlayer(context) }
}

@Composable
private fun rememberClipPlayer(): Clips {
    val context = LocalContext.current
    return remember { ClipPlayer(context) }
}
