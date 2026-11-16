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

/**
 * The Path as a journey: every Level opened so far, a card for each of its Units with how far
 * through it the Learner is, and in each card its Daily Sets as stops on a trail. A completed stop
 * starts a Replay and today's opens it; a locked one does nothing. It opens at the stop the
 * Learner is at: the first Daily Set not yet completed.
 */
@Composable
internal fun PathScreen(path: PathView, onReplay: (String) -> Unit, onOpenTodays: () -> Unit) {
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
                    PathUnit(unit, at, marks, onReplay, onOpenTodays)
                    Spacer(Modifier.height(Space.m))
                }
            }
            if (at == null) CardNote(stringResource(R.string.path_more_units))
        }
    }
}

/** One Unit's card. [at] is the stop the Learner is at: it and its card carry the [marks], so that the Path opens on them. */
@Composable
private fun PathUnit(unit: UnitView, at: DailySetView?, marks: JourneyMarks, onReplay: (String) -> Unit, onOpenTodays: () -> Unit) {
    val stops = unit.dailySets
    val completed = stops.count { it.state == DailySetState.COMPLETED }
    UnitCard(
        label = stringResource(R.string.path_unit_number, unit.number),
        title = unit.title,
        progress = stringResource(R.string.path_unit_progress, completed, stops.size),
        completed = completed,
        dailySets = stops.size,
        modifier = if (stops.any { it.id == at?.id }) marks.card else Modifier,
        current = stops.indexOfFirst { it.state == DailySetState.TODAY }.takeIf { it >= 0 },
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
                else -> stringResource(R.string.path_locked)
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
                last = index == stops.lastIndex,
                // The trail is walked as far as today's stop, and no further.
                walkedAbove = state != DailySetState.LOCKED,
                walkedBelow = state == DailySetState.COMPLETED && stops.getOrNull(index + 1)?.state == DailySetState.TODAY ||
                    stops.getOrNull(index + 1)?.state == DailySetState.COMPLETED,
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
            )
        }
    }
}
