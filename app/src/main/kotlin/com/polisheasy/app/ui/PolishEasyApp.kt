package com.polisheasy.app.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.polisheasy.app.ClipPlayer
import com.polisheasy.app.CourseViewModel
import com.polisheasy.app.R
import com.polisheasy.app.Screen
import com.polisheasy.engine.Answer
import com.polisheasy.engine.ClipView
import com.polisheasy.engine.ConjugationTableView
import com.polisheasy.engine.DailySetState
import com.polisheasy.engine.DailySetView
import com.polisheasy.engine.ExerciseView
import com.polisheasy.engine.Feedback
import com.polisheasy.engine.ItemView
import com.polisheasy.engine.PathView
import com.polisheasy.engine.Person
import com.polisheasy.engine.SetProgress
import com.polisheasy.engine.TileSelection
import com.polisheasy.engine.TipView
import com.polisheasy.engine.Today
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun PolishEasyApp(viewModel: CourseViewModel = viewModel()) {
    val context = LocalContext.current
    val player = remember { ClipPlayer(context) }
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
                    onReminderEnabled = viewModel::setReminderEnabled,
                    onReminderTime = viewModel::setReminderTime,
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
private fun HomeScreen(
    today: Today,
    streak: Int,
    onStart: () -> Unit,
    onContinue: () -> Unit,
    onOpenPath: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ScreenColumn {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(8.dp))
        Text(
            pluralStringResource(R.plurals.home_streak, streak, streak),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        when (today) {
            Today.DailySetAvailable -> {
                Text(stringResource(R.string.home_ready), textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_start))
                }
            }
            Today.ReviewOnlyAvailable -> {
                Text(stringResource(R.string.home_review_only_ready), textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_start))
                }
            }
            is Today.InProgress -> {
                val message = if (today.reviewOnly) R.string.home_review_only_in_progress else R.string.home_in_progress
                Text(
                    stringResource(message, today.progress.correct, today.progress.total),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_continue))
                }
            }
            is Today.DoneForToday -> {
                val unlockTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(today.nextUnlock)
                val message = if (today.reviewOnlyNext) R.string.home_done_review_only_next else R.string.home_done
                Text(stringResource(message, unlockTime), textAlign = TextAlign.Center)
            }
            Today.AllCompleted -> Text(stringResource(R.string.home_all_completed), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onOpenPath, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_path))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_settings))
        }
    }
}

/**
 * Settings: the Reminder's switch and time, and "Report a mistake". Turning the Reminder on is the
 * only moment the app asks for permission to show notifications.
 */
@Composable
private fun SettingsScreen(
    reminderEnabled: Boolean,
    reminderTime: LocalTime,
    notificationsAllowed: Boolean,
    onReminderEnabled: (Boolean) -> Unit,
    onReminderTime: (LocalTime) -> Unit,
    onReport: () -> Unit,
) {
    val context = LocalContext.current
    // Whether the Learner refused the permission here, so the screen can say why the switch stayed off.
    var refused by rememberSaveable { mutableStateOf(false) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refused = !granted
        if (granted) onReminderEnabled(true)
    }
    val reminderLabel = stringResource(R.string.settings_reminder)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(reminderLabel, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(
                checked = reminderEnabled,
                onCheckedChange = { on ->
                    val mustAsk = on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    if (mustAsk) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else onReminderEnabled(on)
                },
                modifier = Modifier.semantics { contentDescription = reminderLabel },
            )
        }
        Text(
            stringResource(R.string.settings_reminder_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!notificationsAllowed && (refused || reminderEnabled)) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.settings_notifications_off), color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { openNotificationSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_open_android_settings))
            }
        }
        if (reminderEnabled) {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { pickingTime = true }
                    .heightIn(min = 56.dp),
            ) {
                Text(
                    stringResource(R.string.settings_reminder_time),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(reminderTime),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onReport, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.report_mistake))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.settings_version, appVersion(context)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (pickingTime) {
        ReminderTimeDialog(
            reminderTime,
            onPicked = {
                pickingTime = false
                onReminderTime(it)
            },
            onDismiss = { pickingTime = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(time: LocalTime, onPicked: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(
        initialHour = time.hour,
        initialMinute = time.minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPicked(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        title = { Text(stringResource(R.string.settings_reminder_time)) },
        text = { TimePicker(state) },
    )
}

/** Android's own notification settings for this app, where a refused permission can be given. */
private fun openNotificationSettings(context: Context) {
    val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // A phone without that page: nothing to open.
    }
}

private fun appVersion(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

/**
 * Opens a draft in the Learner's email app, naming the Exercise when the report is about one.
 * The app itself sends nothing: it has no network permission.
 */
private fun reportMistake(context: Context, exerciseId: String?) {
    val address = context.getString(R.string.report_email)
    val subject = if (exerciseId == null) {
        context.getString(R.string.report_subject_general)
    } else {
        context.getString(R.string.report_subject_exercise, exerciseId)
    }
    val body = listOfNotNull(
        exerciseId?.let { context.getString(R.string.report_body_exercise, it) },
        context.getString(R.string.report_body_version, appVersion(context)),
        "",
        context.getString(R.string.report_body_prompt),
        "",
    ).joinToString("\n")
    val draft = Uri.parse(
        "mailto:" + Uri.encode(address, "@") +
            "?subject=" + Uri.encode(subject) +
            "&body=" + Uri.encode(body.replace("\n", "\r\n")),
    )
    // Email apps differ in which of the two they read, so the subject and body go in both.
    val intent = Intent(Intent.ACTION_SENDTO, draft)
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_TEXT, body)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.report_no_email_app, Toast.LENGTH_LONG).show()
    }
}

/** One row of the Path screen's list. */
private sealed interface PathRow {
    val key: String

    data class LevelTitle(override val key: String, val title: String) : PathRow

    data class UnitHeader(override val key: String, val number: Int, val title: String) : PathRow

    data class DailySetRow(val dailySet: DailySetView) : PathRow {
        override val key: String get() = dailySet.id
    }
}

/**
 * The Path: every Level opened so far, its Units and their Daily Sets, each marked Completed,
 * Today or Locked. A completed one starts a Replay and today's opens it; a locked one does nothing.
 */
@Composable
private fun PathScreen(path: PathView, onReplay: (String) -> Unit, onOpenTodays: () -> Unit) {
    val rows = remember(path) {
        path.levels.flatMap { level ->
            listOf(PathRow.LevelTitle("level-${level.id}", level.title)) + level.units.flatMap { unit ->
                listOf(PathRow.UnitHeader("unit-${unit.id}", unit.number, unit.title)) +
                    unit.dailySets.map { PathRow.DailySetRow(it) }
            }
        }
    }
    val allCompleted = rows.none { it is PathRow.DailySetRow && it.dailySet.state != DailySetState.COMPLETED }
    // Opens at the Unit the Learner is in: the one holding the first Daily Set not yet completed.
    val firstOpen = rows.indexOfFirst { it is PathRow.DailySetRow && it.dailySet.state != DailySetState.COMPLETED }
    val startAt = if (firstOpen < 0) 0 else rows.take(firstOpen).indexOfLast { it is PathRow.UnitHeader }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startAt)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(count = rows.size, key = { rows[it].key }) { index ->
                when (val row = rows[index]) {
                    is PathRow.LevelTitle -> Text(row.title, style = MaterialTheme.typography.headlineMedium)
                    is PathRow.UnitHeader -> Text(
                        stringResource(R.string.path_unit, row.number, row.title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    is PathRow.DailySetRow -> DailySetPathRow(row.dailySet, onReplay, onOpenTodays)
                }
            }
            if (allCompleted) {
                item(key = "more-units") {
                    Text(
                        stringResource(R.string.path_more_units),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

/** A Daily Set on the Path. Its state is always written out, never shown by colour alone. */
@Composable
private fun DailySetPathRow(dailySet: DailySetView, onReplay: (String) -> Unit, onOpenTodays: () -> Unit) {
    val replayLabel = stringResource(R.string.path_replay)
    val startLabel = stringResource(R.string.home_start)
    val modifier = when (dailySet.state) {
        DailySetState.COMPLETED -> Modifier.clickable(onClickLabel = replayLabel) { onReplay(dailySet.id) }
        DailySetState.TODAY -> Modifier.clickable(onClickLabel = startLabel, onClick = onOpenTodays)
        DailySetState.LOCKED -> Modifier.alpha(0.5f)
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        Row(
            modifier = modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.path_daily_set, dailySet.number),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            when (dailySet.state) {
                DailySetState.COMPLETED -> Text(
                    stringResource(R.string.path_completed),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DailySetState.TODAY -> Text(
                    stringResource(R.string.path_today),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                DailySetState.LOCKED -> Text(
                    stringResource(R.string.path_locked),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
        tip.conjugation?.let { table ->
            Spacer(Modifier.height(24.dp))
            ConjugationTableCard(table)
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_continue))
        }
    }
}

/** A verb's six forms, one row per person: the Polish pronouns and the form. */
@Composable
private fun ConjugationTableCard(table: ConjugationTableView) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.conjugation_table_title, table.verb, table.meaning),
                style = MaterialTheme.typography.titleMedium,
            )
            table.rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        row.person.pronouns,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        row.form,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerScreen(
    exercise: ExerciseView,
    progress: SetProgress,
    review: Boolean,
    replay: Boolean,
    feedback: Feedback?,
    onAnswer: (Answer) -> Unit,
    onContinue: () -> Unit,
    onPlay: (ClipView, Boolean) -> Unit,
    onReport: () -> Unit,
) {
    // Held here so the Polish key row and the text field share it.
    var typed by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    DailySetColumn(
        progress,
        bottomBar = {
            val typing = exercise is ExerciseView.TypeAnswer || exercise is ExerciseView.Conjugation ||
                exercise is ExerciseView.ListenType
            if (typing && feedback == null) {
                PolishKeyRow(onKey = { letter -> typed = typed.inserting(letter) })
            }
        },
    ) {
        // A Replay has no Review, so the two labels never meet.
        if (review || replay) {
            Text(
                stringResource(if (replay) R.string.replay_label else R.string.review_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
        }
        when (exercise) {
            is ExerciseView.ChooseTranslation ->
                ChooseTranslation(exercise, enabled = feedback == null, onAnswer, onPlay)
            is ExerciseView.TypeAnswer -> TypeAnswerExercise(
                exercise,
                typed,
                onTypedChange = { typed = it },
                enabled = feedback == null,
                onAnswer,
            )
            is ExerciseView.WordTiles -> WordTilesExercise(exercise, enabled = feedback == null, onAnswer)
            is ExerciseView.Conjugation -> ConjugationExercise(
                exercise,
                typed,
                onTypedChange = { typed = it },
                enabled = feedback == null,
                onAnswer,
            )
            is ExerciseView.ListenChoose -> ListenChooseExercise(exercise, enabled = feedback == null, onAnswer, onPlay)
            is ExerciseView.ListenType -> ListenTypeExercise(
                exercise,
                typed,
                onTypedChange = { typed = it },
                enabled = feedback == null,
                onAnswer,
                onPlay,
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
            feedback.audio?.let { clip -> SpeakerButton(clip, onPlay) }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_continue))
            }
            TextButton(onClick = onReport) {
                Text(stringResource(R.string.report_mistake))
            }
        }
    }
}

@Composable
private fun ChooseTranslation(
    exercise: ExerciseView.ChooseTranslation,
    enabled: Boolean,
    onAnswer: (Answer) -> Unit,
    onPlay: (ClipView, Boolean) -> Unit,
) {
    Text(stringResource(R.string.choose_translation_instruction), style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(16.dp))
    Text(exercise.prompt, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    exercise.promptAudio?.let { clip -> SpeakerButton(clip, onPlay) }
    Spacer(Modifier.height(32.dp))
    OptionButtons(exercise.options, enabled, onAnswer)
}

/** One button per option, answering with the option's position. */
@Composable
private fun OptionButtons(options: List<String>, enabled: Boolean, onAnswer: (Answer) -> Unit) {
    options.forEachIndexed { index, option ->
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

/** Plays [clip], the Polish shown beside it, at normal speed. */
@Composable
private fun SpeakerButton(clip: ClipView, onPlay: (ClipView, Boolean) -> Unit) {
    IconButton(onClick = { onPlay(clip, false) }) {
        Icon(
            painterResource(R.drawable.ic_volume_up),
            contentDescription = stringResource(R.string.audio_play),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * The controls of a listening Exercise: [clip] plays by itself when the Exercise opens, the big
 * speaker plays it again and "Slow" plays it slowly. They stay usable after the answer.
 */
@Composable
private fun ListenControls(clip: ClipView, enabled: Boolean, onPlay: (ClipView, Boolean) -> Unit) {
    // Once per showing: not again when the feedback appears, nor when an answered Exercise is rebuilt.
    LaunchedEffect(Unit) { if (enabled) onPlay(clip, false) }

    FilledIconButton(onClick = { onPlay(clip, false) }, modifier = Modifier.size(72.dp)) {
        Icon(
            painterResource(R.drawable.ic_volume_up),
            contentDescription = stringResource(R.string.audio_play_again),
            modifier = Modifier.size(36.dp),
        )
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = { onPlay(clip, true) }) {
        Text(stringResource(R.string.audio_slow))
    }
}

@Composable
private fun ListenChooseExercise(
    exercise: ExerciseView.ListenChoose,
    enabled: Boolean,
    onAnswer: (Answer) -> Unit,
    onPlay: (ClipView, Boolean) -> Unit,
) {
    Text(stringResource(R.string.listen_choose_instruction), style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(16.dp))
    ListenControls(exercise.audio, enabled, onPlay)
    Spacer(Modifier.height(32.dp))
    OptionButtons(exercise.options, enabled, onAnswer)
}

@Composable
private fun ListenTypeExercise(
    exercise: ExerciseView.ListenType,
    typed: TextFieldValue,
    onTypedChange: (TextFieldValue) -> Unit,
    enabled: Boolean,
    onAnswer: (Answer) -> Unit,
    onPlay: (ClipView, Boolean) -> Unit,
) {
    Text(stringResource(R.string.listen_type_instruction), style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(16.dp))
    ListenControls(exercise.audio, enabled, onPlay)
    Spacer(Modifier.height(32.dp))
    PolishAnswerField(typed, onTypedChange, enabled, onAnswer)
}

@Composable
private fun TypeAnswerExercise(
    exercise: ExerciseView.TypeAnswer,
    typed: TextFieldValue,
    onTypedChange: (TextFieldValue) -> Unit,
    enabled: Boolean,
    onAnswer: (Answer) -> Unit,
) {
    Text(stringResource(R.string.type_answer_instruction), style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(16.dp))
    Text(exercise.prompt, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(32.dp))
    PolishAnswerField(typed, onTypedChange, enabled, onAnswer)
}

@Composable
private fun ConjugationExercise(
    exercise: ExerciseView.Conjugation,
    typed: TextFieldValue,
    onTypedChange: (TextFieldValue) -> Unit,
    enabled: Boolean,
    onAnswer: (Answer) -> Unit,
) {
    Text(stringResource(R.string.conjugation_instruction), style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(16.dp))
    Text(exercise.verb, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Text(
        exercise.meaning,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        stringResource(R.string.conjugation_person, personLabel(exercise.person), exercise.person.pronouns),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
    )
    exercise.sentence?.let { sentence ->
        Spacer(Modifier.height(24.dp))
        Text(
            sentence.before + SHOWN_GAP + sentence.after,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            sentence.meaning,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.height(32.dp))
    PolishAnswerField(typed, onTypedChange, enabled, onAnswer)
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

/** The field a Polish answer is typed into, with its "Check" button. The keyboard opens with it. */
@Composable
private fun PolishAnswerField(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordTilesExercise(
    exercise: ExerciseView.WordTiles,
    enabled: Boolean,
    onAnswer: (Answer) -> Unit,
) {
    // The bank positions of the tiles in the answer, in order. The engine's TileSelection holds the rule.
    var placed by rememberSaveable { mutableStateOf(listOf<Int>()) }
    val selection = TileSelection(exercise.tiles, placed)
    val answerDescription = stringResource(R.string.word_tiles_answer_description)

    Text(stringResource(R.string.word_tiles_instruction), style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(16.dp))
    Text(exercise.prompt, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(32.dp))
    // Tall enough for two rows of tiles from the start, so the bank below does not jump as tiles move.
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TileHeight * 2 + TileSpacing)
            .semantics { contentDescription = answerDescription },
        horizontalArrangement = Arrangement.spacedBy(TileSpacing),
        verticalArrangement = Arrangement.spacedBy(TileSpacing),
    ) {
        selection.placed.forEach { tile ->
            WordTile(exercise.tiles[tile], enabled, onClick = { placed = selection.takeBack(tile).placed })
        }
    }
    Spacer(Modifier.height(TileSpacing))
    HorizontalDivider()
    Spacer(Modifier.height(24.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TileSpacing, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(TileSpacing),
    ) {
        exercise.tiles.forEachIndexed { tile, word ->
            if (selection.isPlaced(tile)) {
                EmptyTileSlot(word)
            } else {
                WordTile(word, enabled, onClick = { placed = selection.place(tile).placed })
            }
        }
    }
    if (enabled) {
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onAnswer(selection.answer) },
            enabled = selection.placed.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_check))
        }
    }
}

private val TileHeight = 48.dp
private val TileSpacing = 8.dp

@Composable
private fun WordTile(word: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.heightIn(min = TileHeight),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(word, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Where a tile now in the answer came from: the same size as the tile, so the rest of the bank does not shift. */
@Composable
private fun EmptyTileSlot(word: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .heightIn(min = TileHeight)
            .clearAndSetSemantics { },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(word, style = MaterialTheme.typography.titleMedium, modifier = Modifier.alpha(0f))
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
private fun SummaryScreen(
    correctAnswers: Int,
    exercises: Int,
    itemsLearned: List<ItemView>,
    reviewOnly: Boolean,
    replay: Boolean,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Scrolls when the list of Items is long, so the button below stays on screen.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(
                    when {
                        replay -> R.string.summary_replay_title
                        reviewOnly -> R.string.summary_review_only_title
                        else -> R.string.summary_title
                    },
                ),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.summary_accuracy, correctAnswers, exercises))
            if (reviewOnly) {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.summary_more_units), textAlign = TextAlign.Center)
            }
            if (itemsLearned.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                Text(stringResource(R.string.summary_items_learned), style = MaterialTheme.typography.labelLarge)
                itemsLearned.forEach { item ->
                    Spacer(Modifier.height(12.dp))
                    Text(item.text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text(item.meaning, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (replay) R.string.summary_back_path else R.string.summary_back_home))
        }
    }
}
