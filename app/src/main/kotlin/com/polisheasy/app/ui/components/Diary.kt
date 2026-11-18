package com.polisheasy.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish

/** The width of a diary's day names, so the plans line up. */
private val DayColumn = 112.dp

/** The fewest a diary's day is tall, so seven fit on a phone with the plan above and the times below. */
private val DayHeight = 44.dp

/** What a day of a diary is showing. Each state has a mark or words as well as a colour. */
enum class DiaryDayState {
    /** Can be tapped, or nothing to show. */
    Open,

    /** Tapped for the plan being asked, before its time is chosen. */
    Picked,

    /** Where the plan asked belongs, once answered. */
    Right,

    /** The day tapped for the plan asked, when it was not this one. */
    Wrong,
}

/** One plan written in a diary: its [time] (20:00) and [what] it is (*impreza*). */
class DiaryEntry(val time: String, val what: String)

/** One day of a diary: its Polish [name], the [entries] written in it, its [state], and what a screen reader says of that state, [stateWords]. */
class DiaryDayView(val name: String, val entries: List<DiaryEntry>, val state: DiaryDayState, val stateWords: String? = null)

/**
 * A week's diary, Monday first: each day a row with its name and the plans written in it. With
 * [onTap] a day can be tapped; the one picked is outlined in the brand's colour. Once answered,
 * the right day is marked with a tick and a wrong one with a cross and one shake.
 */
@Composable
fun Diary(days: List<DiaryDayView>, modifier: Modifier = Modifier, onTap: ((Int) -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.xxs), modifier = modifier.fillMaxWidth()) {
        days.forEachIndexed { index, day -> DiaryRow(day, onTap?.let { { it(index) } }) }
    }
}

@Composable
private fun DiaryRow(day: DiaryDayView, onClick: (() -> Unit)?) {
    val colors = PolishEasyTheme.colors
    val scheme = MaterialTheme.colorScheme
    val (fill, edge) = when (day.state) {
        DiaryDayState.Open -> scheme.surfaceContainerLowest to scheme.outlineVariant
        DiaryDayState.Picked -> scheme.primaryContainer to scheme.primary
        DiaryDayState.Right -> colors.successContainer to colors.success
        DiaryDayState.Wrong -> colors.cautionContainer to colors.caution
    }
    Edged(
        fill = fill,
        edge = edge,
        corner = Corner.small,
        border = Edge.border,
        flat = onClick == null,
        enabled = onClick != null,
        onClick = onClick,
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DayHeight)
            .shaking(day.state == DiaryDayState.Wrong)
            .semantics(mergeDescendants = true) { day.stateWords?.let { stateDescription = it } },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = Space.s, vertical = Space.xxs)) {
            Text(
                day.name,
                style = MaterialTheme.typography.titleSmall.inPolish(),
                color = scheme.onSurface,
                maxLines = 1,
                softWrap = false,
                // At least the column, so the plans line up; wider where a large text needs it, never wrapped.
                modifier = Modifier.widthIn(min = DayColumn),
            )
            Spacer(Modifier.width(Space.xs))
            Column(Modifier.weight(1f)) {
                day.entries.forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.time, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                        Spacer(Modifier.width(Space.xs))
                        Text(
                            entry.what,
                            style = MaterialTheme.typography.bodyLarge.inPolish(),
                            color = scheme.onSurface,
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }
            when (day.state) {
                DiaryDayState.Right -> MarkIcon(Mark.Right)
                DiaryDayState.Wrong -> MarkIcon(Mark.Missed)
                else -> Unit
            }
        }
    }
}
