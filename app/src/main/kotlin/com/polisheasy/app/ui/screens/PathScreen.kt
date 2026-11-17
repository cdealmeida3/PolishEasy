package com.polisheasy.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.CardNote
import com.polisheasy.app.ui.components.Journey
import com.polisheasy.app.ui.components.JourneyMarks
import com.polisheasy.app.ui.components.ScreenFrame
import com.polisheasy.app.ui.components.StopState
import com.polisheasy.app.ui.components.TopBar
import com.polisheasy.app.ui.components.TrailStop
import com.polisheasy.app.ui.components.UnitCard
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.DailySetState
import com.polisheasy.engine.DailySetView
import com.polisheasy.engine.PathView
import com.polisheasy.engine.UnitView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.pluralStringResource
import com.polisheasy.app.ui.components.Teaches
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.TrailMilestone
import com.polisheasy.engine.NextDailySetView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import com.polisheasy.app.ui.components.QuietButton

/**
 * The Path as a journey: every Level opened so far, a card for each of its Units with how far
 * through it the Learner is, and in each card its Daily Sets as stops on a trail. A completed stop
 * starts a Replay and today's opens it; a locked one does nothing. It opens at the stop the
 * Learner is at: the first Daily Set not yet completed.
 *
 * The road ahead is told a little at a time: a Unit not yet reached is only its heading until it
 * is tapped open, and each Unit ends in a milestone. Today's stop has the room: with [today] it
 * says what else the Daily Set teaches, and it carries the button that starts it, or continues it
 * once [started].
 *
 * Without [oneADay], at the Learner's own pace, every stop not completed is open: tapping one
 * starts it with [onStartChosen], after asking first when another Daily Set is in progress, since
 * its progress would be lost. Nothing is locked and nothing unlocks tomorrow.
 */
@Composable
internal fun PathScreen(
    path: PathView,
    onReplay: (String) -> Unit,
    onOpenTodays: () -> Unit,
    today: NextDailySetView? = null,
    started: Boolean = false,
    onStartChosen: (String) -> Unit = {},
    oneADay: Boolean = true,
) {
    val stops = path.levels.flatMap { level -> level.units.flatMap { it.dailySets } }
    // The stop the Learner is at: today's, or with none today the first not completed.
    val at = stops.firstOrNull { it.state == DailySetState.TODAY } ?: stops.firstOrNull { it.state != DailySetState.COMPLETED }
    // The Daily Set the Learner chose while another is in progress, waiting for them to say it may replace it.
    var replacing by rememberSaveable { mutableStateOf<String?>(null) }
    val choose: (String) -> Unit = { id -> if (started) replacing = id else onStartChosen(id) }
    ScreenFrame {
        Journey(modifier = Modifier.fillMaxSize(), opensAtTop = at == null) { marks ->
            path.levels.forEachIndexed { index, level ->
                if (index > 0) Spacer(Modifier.height(Space.m))
                val dailySets = level.units.flatMap { it.dailySets }
                TopBar(
                    stringResource(R.string.path_level, level.title),
                    note = stringResource(R.string.path_progress, dailySets.count { it.state == DailySetState.COMPLETED }, dailySets.size),
                )
                level.units.forEach { unit ->
                    PathUnit(unit, at, marks, onReplay, onOpenTodays, today, started, choose, oneADay)
                    Spacer(Modifier.height(Space.m))
                }
            }
            if (at == null) CardNote(stringResource(R.string.path_more_units))
        }
    }
    replacing?.let { id ->
        val inProgress = at?.let { stop -> stop.title ?: stringResource(R.string.path_daily_set, numberInUnit(path, stop.id)) }.orEmpty()
        AlertDialog(
            onDismissRequest = { replacing = null },
            confirmButton = {
                QuietButton(stringResource(R.string.path_replace_start), onClick = {
                    replacing = null
                    onStartChosen(id)
                })
            },
            dismissButton = { QuietButton(stringResource(R.string.action_cancel), { replacing = null }) },
            title = { Text(stringResource(R.string.path_replace_title)) },
            text = { Text(stringResource(R.string.path_replace_note, inProgress)) },
        )
    }
}

/** The number of the Daily Set [id] in its Unit, from 1. */
private fun numberInUnit(path: PathView, id: String): Int =
    path.levels.flatMap { it.units }.firstNotNullOfOrNull { unit -> unit.dailySets.indexOfFirst { it.id == id }.takeIf { it >= 0 } }?.plus(1) ?: 1

/** One Unit's card. [at] is the stop the Learner is at: it and its card carry the [marks], so that the Path opens on them. */
@Composable
private fun PathUnit(
    unit: UnitView,
    at: DailySetView?,
    marks: JourneyMarks,
    onReplay: (String) -> Unit,
    onOpenTodays: () -> Unit,
    today: NextDailySetView?,
    started: Boolean,
    onStartChosen: (String) -> Unit,
    oneADay: Boolean,
) {
    val stops = unit.dailySets
    val completed = stops.count { it.state == DailySetState.COMPLETED }
    // A Unit the Learner has not reached is closed until they open it to look ahead.
    val locked = stops.all { it.state == DailySetState.LOCKED }
    val ahead = (locked || stops.all { it.state == DailySetState.OPEN }) && stops.none { it.id == at?.id }
    var open by rememberSaveable(unit.id) { mutableStateOf(false) }
    UnitCard(
        label = stringResource(R.string.path_unit_number, unit.number),
        title = unit.title,
        progress = if (ahead) {
            pluralStringResource(if (locked) R.plurals.path_unit_ahead else R.plurals.path_unit_ahead_open, stops.size, stops.size)
        } else {
            stringResource(R.string.path_unit_progress, completed, stops.size)
        },
        completed = completed,
        dailySets = stops.size,
        modifier = if (stops.any { it.id == at?.id }) marks.card else Modifier,
        current = stops.indexOfFirst { it.state == DailySetState.TODAY }.takeIf { it >= 0 },
        open = !ahead || open,
        toggleLabel = stringResource(if (open) R.string.path_unit_close else R.string.path_unit_open),
        onToggle = if (ahead) ({ open = !open }) else null,
    ) {
        stops.forEachIndexed { index, dailySet ->
            val state = dailySet.state
            // Its number in its Unit, the same one Home shows.
            val number = stringResource(R.string.path_stop_number, index + 1, stops.size)
            // A Daily Set with no Tip has no title of its own, so its number stands in for one.
            val numbered = dailySet.title != null
            val next = at?.id == dailySet.id && state == DailySetState.LOCKED
            val says = when {
                state == DailySetState.COMPLETED -> stringResource(R.string.path_completed)
                state == DailySetState.TODAY -> null
                next -> stringResource(R.string.path_unlocks_tomorrow)
                // The padlock says it to the eye; a screen reader is told below.
                else -> null
            }
            TrailStop(
                state = when (state) {
                    DailySetState.COMPLETED -> StopState.Completed
                    DailySetState.TODAY -> StopState.Today
                    DailySetState.OPEN -> StopState.Open
                    DailySetState.LOCKED -> StopState.Locked
                },
                title = dailySet.title ?: stringResource(R.string.path_daily_set, index + 1),
                note = when {
                    !numbered -> says
                    says == null -> number
                    else -> stringResource(R.string.path_stop_note, number, says)
                },
                first = index == 0,
                // The trail runs on from the last stop to the Unit's milestone.
                last = false,
                // The trail is walked as far as today's stop, and no further.
                walkedAbove = state == DailySetState.COMPLETED || state == DailySetState.TODAY,
                walkedBelow = state == DailySetState.COMPLETED && stops.getOrNull(index + 1)?.state == DailySetState.TODAY ||
                    stops.getOrNull(index + 1)?.state == DailySetState.COMPLETED ||
                    index == stops.lastIndex && state == DailySetState.COMPLETED,
                modifier = if (at?.id == dailySet.id) marks.stop else Modifier,
                label = when {
                    // At the Learner's own pace the stop they are at is only the suggested next one, until it is started.
                    state == DailySetState.TODAY && !oneADay && !started -> stringResource(R.string.home_up_next)
                    state == DailySetState.TODAY -> stringResource(R.string.path_today)
                    next -> stringResource(R.string.home_up_next)
                    else -> null
                },
                action = if (state == DailySetState.COMPLETED) R.drawable.ic_replay else null,
                onClickLabel = when (state) {
                    DailySetState.COMPLETED -> stringResource(R.string.path_replay)
                    DailySetState.TODAY, DailySetState.OPEN -> stringResource(R.string.home_start)
                    DailySetState.LOCKED -> null
                },
                onClick = when (state) {
                    DailySetState.COMPLETED -> ({ onReplay(dailySet.id) })
                    DailySetState.TODAY -> onOpenTodays
                    DailySetState.OPEN -> ({ onStartChosen(dailySet.id) })
                    DailySetState.LOCKED -> null
                },
                compact = state == DailySetState.COMPLETED,
                said = when {
                    state == DailySetState.LOCKED && !next -> stringResource(R.string.path_locked)
                    // The plain node says it to the eye.
                    state == DailySetState.OPEN -> stringResource(R.string.path_open)
                    else -> null
                },
                detail = if (state == DailySetState.TODAY) {
                    {
                        val alsoTeaches = today?.teaches.orEmpty().drop(1)
                        if (alsoTeaches.isNotEmpty()) {
                            Teaches(stringResource(R.string.home_teaches), alsoTeaches)
                            Spacer(Modifier.height(Space.m))
                        }
                        PrimaryButton(stringResource(if (started) R.string.home_continue else R.string.home_start), onOpenTodays)
                        Spacer(Modifier.height(Space.xs))
                    }
                } else {
                    null
                },
            )
        }
        TrailMilestone(
            title = stringResource(if (completed == stops.size) R.string.path_unit_complete else R.string.path_unit_end, unit.number),
            reached = completed == stops.size,
        )
    }
}
