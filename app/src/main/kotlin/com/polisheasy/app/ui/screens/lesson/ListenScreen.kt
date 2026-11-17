package com.polisheasy.app.ui.screens.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.polisheasy.app.ListenedAnswer
import com.polisheasy.app.PlayerState
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.AnswerField
import com.polisheasy.app.ui.components.AudioPlayerBar
import com.polisheasy.app.ui.components.ChoiceChips
import com.polisheasy.app.ui.components.DetailFrame
import com.polisheasy.app.ui.components.LessonBanner
import com.polisheasy.app.ui.components.ListeningTaskCard
import com.polisheasy.app.ui.components.Mark
import com.polisheasy.app.ui.components.OptionCard
import com.polisheasy.app.ui.components.OptionState
import com.polisheasy.app.ui.components.QuietButton
import com.polisheasy.app.ui.components.RoundNote
import com.polisheasy.app.ui.components.SecondaryButton
import com.polisheasy.app.ui.components.SettingDivider
import com.polisheasy.app.ui.components.SettingsCard
import com.polisheasy.app.ui.components.SwitchSetting
import com.polisheasy.app.ui.components.TopBar
import com.polisheasy.app.ui.components.YourTurn
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.LessonView
import com.polisheasy.engine.ListeningAnswer
import com.polisheasy.engine.ListeningTaskView
import com.polisheasy.engine.RecordingPlayView

/** The speeds the player offers, as the chips list them. */
internal val listenSpeeds = listOf(0.75f, 1f, 1.25f)

/**
 * A class recording in the player, opened from a chapter's Słuchaj: what it is, the player (play
 * or pause, 5 s back and on, the line before and after, speed, repeat, and step mode, the book's
 * *posłuchać i powtórzyć*, which plays a line and waits for the Learner to say it), its listening
 * tasks, and the recordings before and after it in the chapter.
 *
 * What the player is doing comes in [state]; the screen only asks it for things.
 */
@Composable
internal fun ListenScreen(
    lesson: LessonView,
    play: RecordingPlayView,
    state: PlayerState,
    answered: Map<String, ListenedAnswer>,
    onPlayPause: () -> Unit,
    onSkip: (Long) -> Unit,
    onLine: (Int) -> Unit,
    onSpeed: (Float) -> Unit,
    onRepeat: (Boolean) -> Unit,
    onStep: (Boolean) -> Unit,
    onAnswer: (String, ListeningAnswer) -> Unit,
    onRecording: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val track = play.track
    val lines = play.lines
    DetailFrame(backLabel = stringResource(R.string.listen_back), onBack = onBack) {
        val chapter = stringResource(R.string.lesson_label, track.lesson)
        LessonBanner(
            label = stringResource(R.string.listen_label, track.number),
            number = "%03d".format(track.number),
            title = track.title,
            subtitle = track.section?.let { stringResource(R.string.listen_where, chapter, it) } ?: chapter,
            accent = lesson.accent,
        )
        Spacer(Modifier.height(Space.m))
        val duration = state.durationMs.takeIf { it > 0 } ?: track.lengthMs
        val position = if (state.number == track.number) state.positionMs.coerceAtMost(duration) else 0
        val line = state.line?.takeIf { state.number == track.number }
        AudioPlayerBar(
            playing = state.playing && state.number == track.number,
            progress = position.toFloat() / duration.coerceAtLeast(1),
            time = listOfNotNull(
                stringResource(R.string.listen_time, clock(position), clock(duration)),
                if (state.step && line != null) stringResource(R.string.listen_line, line + 1, lines.size) else null,
            ).joinToString(" · "),
            progressDescription = stringResource(R.string.listen_progress, clock(position), clock(duration)),
            accent = lesson.accent,
            playLabel = stringResource(R.string.listen_play),
            pauseLabel = stringResource(R.string.listen_pause),
            backLabel = stringResource(R.string.listen_back_5),
            onLabel = stringResource(R.string.listen_on_5),
            previousLabel = stringResource(R.string.listen_previous_line),
            nextLabel = stringResource(R.string.listen_next_line),
            onPlayPause = onPlayPause,
            onBack = { onSkip(-SKIP_MS) },
            onOn = { onSkip(SKIP_MS) },
            onPrevious = { onLine(((line ?: 0) - 1).coerceAtLeast(0)) },
            onNext = { onLine(((line ?: -1) + 1).coerceAtMost(lines.lastIndex)) },
        )
        YourTurn(
            visible = state.yourTurn && state.number == track.number,
            title = stringResource(R.string.listen_your_turn),
            note = stringResource(R.string.listen_your_turn_note),
            accent = lesson.accent,
            modifier = Modifier.fillMaxWidth().padding(top = Space.s),
        )
        Spacer(Modifier.height(Space.m))
        ChoiceChips(
            label = stringResource(R.string.listen_speed),
            options = listOf(
                stringResource(R.string.listen_speed_slower),
                stringResource(R.string.listen_speed_normal),
                stringResource(R.string.listen_speed_faster),
            ),
            selected = listenSpeeds.indexOf(state.speed).coerceAtLeast(0),
            onSelect = { onSpeed(listenSpeeds[it]) },
        )
        Spacer(Modifier.height(Space.m))
        SettingsCard {
            SwitchSetting(
                icon = R.drawable.ic_record_voice_over,
                label = stringResource(R.string.listen_step),
                note = stringResource(R.string.listen_step_note),
                checked = state.step,
                onCheckedChange = onStep,
            )
            SettingDivider()
            SwitchSetting(
                icon = R.drawable.ic_repeat,
                label = stringResource(R.string.listen_repeat),
                note = stringResource(R.string.listen_repeat_note),
                checked = state.repeat,
                onCheckedChange = onRepeat,
            )
        }
        if (play.tasks.isNotEmpty()) {
            Spacer(Modifier.height(Space.l))
            TopBar(stringResource(R.string.listen_tasks_title), note = stringResource(R.string.listen_tasks_note))
            play.tasks.forEach { task ->
                ListeningTask(task, answered[task.id], onAnswer = { onAnswer(task.id, it) })
                Spacer(Modifier.height(Space.s))
            }
        }
        Spacer(Modifier.height(Space.m))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            play.previous?.let { QuietButton(stringResource(R.string.listen_previous_recording, it), { onRecording(it) }) }
            Spacer(Modifier.weight(1f))
            play.next?.let { QuietButton(stringResource(R.string.listen_next_recording, it), { onRecording(it) }) }
        }
    }
}

/** One listening task, answered in place: P or N, a choice, or what was heard typed. */
@Composable
private fun ListeningTask(task: ListeningTaskView, answered: ListenedAnswer?, onAnswer: (ListeningAnswer) -> Unit) {
    val kind = stringResource(
        when (task) {
            is ListeningTaskView.TrueFalse -> R.string.listen_task_true_false
            is ListeningTaskView.Choose -> R.string.listen_task_choose
            is ListeningTaskView.Dictation -> R.string.listen_task_dictation
        },
    )
    val lines = task.lines
    ListeningTaskCard(
        question = task.question,
        meaning = task.meaning,
        label = if (lines != null) stringResource(R.string.listen_task_lines, kind, lines.first, lines.last) else kind,
    ) {
        val feedback = answered?.feedback
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            when (task) {
                is ListeningTaskView.TrueFalse -> {
                    val given = (answered?.answer as? ListeningAnswer.TrueFalse)?.value
                    listOf(true, false).forEach { value ->
                        OptionCard(
                            text = stringResource(if (value) R.string.listen_true else R.string.listen_false),
                            state = optionState(feedback == null, given == value, feedback?.correct == true, rightValue(feedback, value)),
                            onClick = { onAnswer(ListeningAnswer.TrueFalse(value)) },
                        )
                    }
                }
                is ListeningTaskView.Choose -> {
                    val given = (answered?.answer as? ListeningAnswer.Choose)?.option
                    task.options.forEachIndexed { index, option ->
                        OptionCard(
                            text = option,
                            state = optionState(feedback == null, given == index, feedback?.correct == true, feedback?.correctAnswer == option),
                            onClick = { onAnswer(ListeningAnswer.Choose(index)) },
                            polish = true,
                        )
                    }
                }
                is ListeningTaskView.Dictation -> {
                    var typed by remember { mutableStateOf(TextFieldValue((answered?.answer as? ListeningAnswer.Typed)?.text.orEmpty())) }
                    val check = { if (typed.text.isNotBlank()) onAnswer(ListeningAnswer.Typed(typed.text)) }
                    // The keyboard waits for a tap: the recording comes first.
                    AnswerField(typed, { typed = it }, stringResource(R.string.listen_type), enabled = feedback == null, onDone = check, focus = false)
                    if (feedback == null) SecondaryButton(stringResource(R.string.listen_check), check)
                }
            }
            if (feedback != null) {
                RoundNote(
                    if (feedback.correct) Mark.Right else Mark.Missed,
                    if (feedback.correct) stringResource(R.string.listen_right) else stringResource(R.string.listen_wrong, feedback.correctAnswer),
                    polish = true,
                )
            }
        }
    }
}

/** Whether P or N, [value], is the right one, from the answer's feedback. */
private fun rightValue(feedback: com.polisheasy.engine.Feedback?, value: Boolean): Boolean =
    feedback?.correctAnswer == if (value) com.polisheasy.engine.ClassAudio.TRUE_MARK else com.polisheasy.engine.ClassAudio.FALSE_MARK

private fun optionState(open: Boolean, given: Boolean, correct: Boolean, right: Boolean): OptionState = when {
    open -> OptionState.Idle
    right -> OptionState.Right
    given && !correct -> OptionState.Wrong
    else -> OptionState.Faded
}

/** A time as the player shows it: minutes and seconds. */
internal fun clock(ms: Long): String {
    val seconds = ms / 1_000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

private const val SKIP_MS = 5_000L
