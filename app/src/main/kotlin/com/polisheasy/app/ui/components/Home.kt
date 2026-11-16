package com.polisheasy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.polisheasy.app.R
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space

private val DayDot = 32.dp
private val DayTick = 20.dp
private val Bullet = 8.dp

/** One day of the Streak's week: its [label], a letter for the weekday, and whether it is [completed] and is [today]. */
class WeekDay(val label: String, val completed: Boolean, val today: Boolean)

/**
 * The Streak on Home: the flame and the number, and under them the last seven [days] as dots.
 * A completed day is filled and ticked, today has a ring until it is completed, and with a Streak
 * of zero the flame is grey, not hidden. [title] and [note] are the words beside the number, and
 * [description] is the whole card in words for a screen reader.
 */
@Composable
fun StreakWeekCard(
    streak: Int,
    title: String,
    note: String?,
    days: List<WeekDay>,
    description: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val lit = if (streak > 0) PolishEasyTheme.colors.streak else scheme.outline
    ContentCard(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_flame), contentDescription = null, tint = lit, modifier = Modifier.size(Size.flame))
            Spacer(Modifier.width(Space.xs))
            Text(streak.toString(), style = MaterialTheme.typography.displayMedium, color = lit)
            Spacer(Modifier.width(Space.m))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
                if (note != null) Text(note, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(Space.m))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            days.forEach { day -> Day(day) }
        }
    }
}

@Composable
private fun Day(day: WeekDay) {
    val scheme = MaterialTheme.colorScheme
    val streak = PolishEasyTheme.colors.streak
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val dot = Modifier.size(DayDot)
        Box(
            contentAlignment = Alignment.Center,
            modifier = when {
                day.completed -> dot.background(streak, CircleShape)
                day.today -> dot.border(Edge.border, streak, CircleShape)
                else -> dot.background(scheme.surfaceContainerHigh, CircleShape)
            },
        ) {
            if (day.completed) {
                // onTertiary is what sits on the flame's colour: streak and tertiary are one value.
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = scheme.onTertiary,
                    modifier = Modifier.size(DayTick),
                )
            }
        }
        Spacer(Modifier.height(Space.xxs))
        Text(
            day.label,
            style = if (day.today) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
            color = if (day.today) scheme.onSurface else scheme.onSurfaceVariant,
        )
    }
}

/**
 * A card of Home that says one thing about today: a small [label] over its [title], then whatever
 * the state needs. [highlighted] gives the label the brand's colour, for the card that holds the
 * main action; [done] puts the tick beside the title, so "done" is not said by words alone.
 */
@Composable
fun TodayCard(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    highlighted: Boolean = false,
    done: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    ContentCard(modifier = modifier) {
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = if (highlighted) scheme.primary else scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.xxs))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (done) {
                Icon(
                    painterResource(R.drawable.ic_check_circle),
                    contentDescription = null,
                    tint = PolishEasyTheme.colors.success,
                    modifier = Modifier.size(Size.feedbackIcon),
                )
                Spacer(Modifier.width(Space.xs))
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
        }
        content()
    }
}

/** A sentence inside a [TodayCard], under its title. */
@Composable
fun CardNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = Space.xs),
    )
}

/**
 * How far through its Unit a Daily Set is: the [unit]'s name over one segment for each of its
 * [dailySets]. The first [completed] are filled, the one after them, the Daily Set this is about,
 * is outlined, and the rest are empty. [description] is what a screen reader says for the segments.
 */
@Composable
fun UnitProgress(unit: String, completed: Int, dailySets: Int, description: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(unit, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        Spacer(Modifier.height(Space.s))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.xxs),
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = description },
        ) {
            repeat(dailySets) { index ->
                val segment = Modifier
                    .weight(1f)
                    .height(Size.progressBar)
                Box(
                    modifier = when {
                        index < completed -> segment.background(scheme.primary, CircleShape)
                        index == completed ->
                            segment.background(scheme.primaryContainer, CircleShape).border(Edge.border, scheme.primary, CircleShape)
                        else -> segment.background(scheme.surfaceContainerHigh, CircleShape)
                    },
                )
            }
        }
    }
}

/** What a Daily Set teaches: a quiet [label] over one line for each of its [topics]. */
@Composable
fun Teaches(label: String, topics: List<String>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Instruction(label)
        topics.forEach { topic ->
            Spacer(Modifier.height(Space.xxs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(Bullet).background(scheme.primary, CircleShape))
                Spacer(Modifier.width(Space.s))
                Text(topic, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
            }
        }
    }
}
