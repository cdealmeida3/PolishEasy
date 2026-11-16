package com.polisheasy.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.ScreenFrame
import com.polisheasy.app.ui.components.TopBar
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.DailySetState
import com.polisheasy.engine.DailySetView
import com.polisheasy.engine.PathView

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
internal fun PathScreen(path: PathView, onReplay: (String) -> Unit, onOpenTodays: () -> Unit) {
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
    ScreenFrame {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            items(count = rows.size, key = { rows[it].key }) { index ->
                when (val row = rows[index]) {
                    is PathRow.LevelTitle -> TopBar(row.title)
                    is PathRow.UnitHeader -> Text(
                        stringResource(R.string.path_unit, row.number, row.title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = Space.m),
                    )
                    is PathRow.DailySetRow -> DailySetPathRow(row.dailySet, onReplay, onOpenTodays)
                }
            }
            if (allCompleted) {
                item(key = "more-units") {
                    Text(
                        stringResource(R.string.path_more_units),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Space.m),
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
    val scheme = MaterialTheme.colorScheme
    val today = dailySet.state == DailySetState.TODAY
    val modifier = when (dailySet.state) {
        DailySetState.COMPLETED -> Modifier.clickable(onClickLabel = replayLabel) { onReplay(dailySet.id) }
        DailySetState.TODAY -> Modifier.clickable(onClickLabel = startLabel, onClick = onOpenTodays)
        DailySetState.LOCKED -> Modifier.alpha(Motion.FADED_ALPHA)
    }
    Surface(
        color = if (today) scheme.primaryContainer else scheme.surfaceContainerLowest,
        contentColor = if (today) scheme.onPrimaryContainer else scheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(Edge.hairline, if (today) scheme.primary else scheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Size.control),
    ) {
        Row(
            modifier = modifier
                .heightIn(min = Size.control)
                .padding(horizontal = Space.m, vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.path_daily_set, dailySet.number),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(
                    when (dailySet.state) {
                        DailySetState.COMPLETED -> R.string.path_completed
                        DailySetState.TODAY -> R.string.path_today
                        DailySetState.LOCKED -> R.string.path_locked
                    },
                ),
                style = if (today) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
                color = if (today) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            )
        }
    }
}
