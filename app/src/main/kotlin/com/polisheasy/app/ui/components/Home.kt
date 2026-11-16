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
import com.polisheasy.app.ui.theme.Scaled
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import com.polisheasy.app.ui.theme.Motion
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.scale

private val DayDot = 32.dp
private val DayTick = 20.dp
private val UpcomingDot = 12.dp
private val Bullet = 8.dp

/**
 * One day of the Streak's week: its [label], two letters for the weekday, and whether it is
 * [completed], is [today], or is [upcoming]: a day still to come, which is never drawn as missed.
 */
class WeekDay(val label: String, val completed: Boolean, val today: Boolean, val upcoming: Boolean = false)

/**
 * The Streak on Home: the flame and the number, and under them the [days] of this week as dots.
 * A completed day is filled and ticked, today has a ring until it is completed, and with a Streak
 * of zero the flame is grey, not hidden. [title] and [note] are the words beside the number, and
 * [description] is the whole card in words for a screen reader. With [raisedFrom], the Streak has
 * just gone up from that number: the number rolls to the new one and today's dot fills in.
 */
@Composable
fun StreakWeekCard(
    streak: Int,
    title: String,
    note: String?,
    days: List<WeekDay>,
    description: String,
    modifier: Modifier = Modifier,
    raisedFrom: Int? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val lit = if (streak > 0) PolishEasyTheme.colors.streak else scheme.outline
    val stacked = Scaled.roomy()
    ContentCard(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        val words: @Composable ColumnScope.() -> Unit = {
            Text(title, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
            if (note != null) Text(note, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_flame), contentDescription = null, tint = lit, modifier = Modifier.size(Size.flame))
            Spacer(Modifier.width(Space.xs))
            RollingNumber(raisedFrom ?: streak, streak, MaterialTheme.typography.displayMedium, lit)
            if (!stacked) {
                Spacer(Modifier.width(Space.m))
                Column(modifier = Modifier.weight(1f), content = words)
            }
        }
        // At a large font size the words go under the number, where they have the card's whole width.
        if (stacked) Column(content = words)
        Spacer(Modifier.height(Space.m))
        WeekRow(days, raised = raisedFrom != null)
    }
}

/**
 * The Streak before it has started: no number and no week, only today's ring with the flame in
 * it, waiting to be lit. [title] invites, [note] says how, and a screen reader hears both.
 */
@Composable
fun StreakStartCard(title: String, note: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val streak = PolishEasyTheme.colors.streak
    ContentCard(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(Size.flame).border(Edge.border, streak, CircleShape),
            ) {
                Icon(painterResource(R.drawable.ic_flame), contentDescription = null, tint = streak, modifier = Modifier.size(Size.icon))
            }
            Spacer(Modifier.width(Space.m))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
                Text(note, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * The days of a week as dots, Monday first. With [raised], today has just been completed: its dot
 * starts as today's ring and fills in with a bounce, so the day is seen to be won.
 */
@Composable
fun WeekRow(days: List<WeekDay>, modifier: Modifier = Modifier, raised: Boolean = false) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = modifier.fillMaxWidth()) {
        days.forEach { day -> Day(day, filling = raised && day.today && day.completed) }
    }
}

@Composable
private fun Day(day: WeekDay, filling: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val streak = PolishEasyTheme.colors.streak
    // How much of a dot that is filling in has arrived, from 0 to 1.
    val spec = Motion.fill()
    val filled = remember(filling) { Animatable(if (filling) 0f else 1f) }
    if (filling) {
        LaunchedEffect(Unit) {
            delay(Motion.CHEER_MILLIS.toLong())
            filled.animateTo(1f, spec)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val dot = Modifier.size(DayDot)
        Box(
            contentAlignment = Alignment.Center,
            modifier = when {
                filling -> dot
                    .border(Edge.border, streak, CircleShape)
                    .drawBehind { drawCircle(streak, radius = size.minDimension / 2 * filled.value.coerceAtLeast(0f)) }
                day.completed -> dot.background(streak, CircleShape)
                day.today -> dot.border(Edge.border, streak, CircleShape)
                // A day to come is a small quiet dot; a day that passed without one is an empty circle.
                day.upcoming -> dot
                else -> dot.background(scheme.surfaceContainerHigh, CircleShape).dashedOutline(DayDot)
            },
        ) {
            if (day.upcoming && !day.completed) Box(Modifier.size(UpcomingDot).background(scheme.surfaceContainerHighest, CircleShape))
            if (day.completed) {
                // onTertiary is what sits on the flame's colour: streak and tertiary are one value.
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = scheme.onTertiary,
                    modifier = Modifier
                        .size(DayTick)
                        .drawWithContent { scale(filled.value.coerceAtLeast(0f)) { this@drawWithContent.drawContent() } },
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
        ProgressSegments(completed, current = completed, total = dailySets, description = description)
    }
}

/**
 * One segment for each of [total] things done in order. The first [completed] are filled, the one
 * at [current], if any, is outlined, and the rest are empty. A screen reader says [description];
 * without one the segments are skipped, for when the words beside them already say it.
 */
@Composable
internal fun ProgressSegments(completed: Int, current: Int?, total: Int, description: String?, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    // The brand's own colour in both themes: the dark theme's pink `primary` is for text.
    val brand = PolishEasyTheme.colors.brand
    Row(
        horizontalArrangement = Arrangement.spacedBy(Space.xxs),
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { if (description != null) contentDescription = description },
    ) {
        repeat(total) { index ->
            val segment = Modifier
                .weight(1f)
                .height(Size.progressBar)
            Box(
                modifier = when {
                    index < completed -> segment.background(brand, CircleShape)
                    index == current ->
                        segment.background(scheme.primaryContainer, CircleShape).border(Edge.border, brand, CircleShape)
                    else -> segment.background(scheme.surfaceContainerHigh, CircleShape)
                },
            )
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

/**
 * A number that rolls from [from] to [to] after the moment a celebration takes to begin: the old
 * one goes up and out, the new one comes in from below. With no change, or with animations
 * removed, it shows [to] at once.
 */
@Composable
internal fun RollingNumber(from: Int, to: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier, onRolled: suspend () -> Unit = {}) {
    val reduced = Motion.reduced()
    var shown by remember { mutableIntStateOf(if (reduced) to else from) }
    LaunchedEffect(to) {
        if (!reduced && from != to) {
            delay(Motion.CHEER_MILLIS.toLong())
            shown = to
            onRolled()
        }
    }
    val roll = Motion.roll()
    AnimatedContent(shown, transitionSpec = { roll }, label = "number", modifier = modifier) { number ->
        Text(number.toString(), style = style, color = color)
    }
}
