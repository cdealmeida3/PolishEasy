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
 */
@Composable
internal fun PathScreen(
    path: PathView,
    onReplay: (String) -> Unit,
    onOpenTodays: () -> Unit,
    today: NextDailySetView? = null,
    started: Boolean = false,
) {
    val at = path.levels.flatMap { level -> level.units.flatMap { it.dailySets } }
        .firstOrNull { it.state != DailySetState.COMPLETED }
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
                    PathUnit(unit, at, marks, onReplay, onOpenTodays, today, started)
                    Spacer(Modifier.height(Space.m))
                }
            }
            if (at == null) CardNote(stringResource(R.string.path_more_units))
        }
    }
}

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
) {
    val stops = unit.dailySets
    val completed = stops.count { it.state == DailySetState.COMPLETED }
    // A Unit the Learner has not reached is closed until they open it to look ahead.
    val ahead = stops.all { it.state == DailySetState.LOCKED } && stops.none { it.id == at?.id }
    var open by rememberSaveable(unit.id) { mutableStateOf(false) }
    UnitCard(
        label = stringResource(R.string.path_unit_number, unit.number),
        title = unit.title,
        progress = if (ahead) {
            pluralStringResource(R.plurals.path_unit_ahead, stops.size, stops.size)
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
                walkedAbove = state != DailySetState.LOCKED,
                walkedBelow = state == DailySetState.COMPLETED && stops.getOrNull(index + 1)?.state == DailySetState.TODAY ||
                    stops.getOrNull(index + 1)?.state == DailySetState.COMPLETED ||
                    index == stops.lastIndex && state == DailySetState.COMPLETED,
                modifier = if (at?.id == dailySet.id) marks.stop else Modifier,
                label = when {
                    state == DailySetState.TODAY -> stringResource(R.string.path_today)
                    next -> stringResource(R.string.home_up_next)
                    else -> null
                },
                action = if (state == DailySetState.COMPLETED) R.drawable.ic_replay else null,
                onClickLabel = when (state) {
                    DailySetState.COMPLETED -> stringResource(R.string.path_replay)
                    DailySetState.TODAY -> stringResource(R.string.home_start)
                    DailySetState.LOCKED -> null
                },
                onClick = when (state) {
                    DailySetState.COMPLETED -> ({ onReplay(dailySet.id) })
                    DailySetState.TODAY -> onOpenTodays
                    DailySetState.LOCKED -> null
                },
                compact = state == DailySetState.COMPLETED,
                said = if (state == DailySetState.LOCKED && !next) stringResource(R.string.path_locked) else null,
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
