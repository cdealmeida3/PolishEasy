package com.polisheasy.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.polisheasy.app.R
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/** A Path node, and the room its column keeps so that today's ring fits around one. */
private val Node = 56.dp
private val NodeColumn = 72.dp
private val Trail = 4.dp

/** Where the Path opens, as shares of the screen's height. */
private const val CARD_FITS = 0.6f
private const val STOP_SITS_AT = 1 / 3f

/** What [Journey] hands its content to mark where the Path opens: [stop] for the stop, [card] for the card holding it. */
class JourneyMarks internal constructor(val stop: Modifier, val card: Modifier)

/**
 * The Path's scrolling column. It opens already scrolled to the stop the Learner is at: the thing
 * that carries the `stop` mark handed to [content]. If the stop is near enough the top of the card
 * that carries the `card` mark, the Path opens at the top of that card, so the Unit's name and
 * progress are in view; otherwise the stop sits a third of the way down. Nothing is seen to jump:
 * the column shows once it is in place. With [opensAtTop] it stays at the top, for when nothing
 * is marked.
 */
@Composable
fun Journey(modifier: Modifier = Modifier, opensAtTop: Boolean = false, content: @Composable ColumnScope.(JourneyMarks) -> Unit) {
    val scroll = rememberScrollState()
    var viewport by remember { mutableIntStateOf(0) }
    var column by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var stop by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var card by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var opened by remember { mutableStateOf(opensAtTop) }
    val marks = remember {
        JourneyMarks(
            stop = Modifier.onGloballyPositioned { if (stop == null) stop = it },
            card = Modifier.onGloballyPositioned { if (card == null) card = it },
        )
    }
    LaunchedEffect(Unit) {
        if (opened) return@LaunchedEffect
        val at = snapshotFlow { stop }.filterNotNull().first()
        val within = column
        if (within != null && within.isAttached && at.isAttached) {
            val stopTop = within.localPositionOf(at, Offset.Zero).y
            val cardTop = card?.takeIf { it.isAttached }?.let { within.localPositionOf(it, Offset.Zero).y }
            val top = if (cardTop != null && stopTop - cardTop <= viewport * CARD_FITS) cardTop else stopTop - viewport * STOP_SITS_AT
            scroll.scrollTo(top.toInt().coerceAtLeast(0))
        }
        opened = true
    }
    Column(
        modifier = modifier
            .onSizeChanged { viewport = it.height }
            .graphicsLayer { alpha = if (opened) 1f else 0f }
            .verticalScroll(scroll)
            .onGloballyPositioned { column = it },
    ) {
        content(marks)
    }
}

/**
 * A Unit on the Path: a card with a small [label], the Unit's [title], how far through it the
 * Learner is, in the words of [progress] and as one segment for each of its [dailySets], and under
 * those its stops. The first [completed] segments are filled and the one at [current], today's,
 * is outlined. A Unit that is all completed has the tick beside its title.
 */
@Composable
fun UnitCard(
    label: String,
    title: String,
    progress: String,
    completed: Int,
    dailySets: Int,
    modifier: Modifier = Modifier,
    current: Int? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val done = completed == dailySets
    ContentCard(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = scheme.primary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (done) {
                Spacer(Modifier.width(Space.xs))
                Icon(
                    painterResource(R.drawable.ic_check_circle),
                    contentDescription = null,
                    tint = PolishEasyTheme.colors.success,
                    modifier = Modifier.size(Size.icon),
                )
            }
        }
        Spacer(Modifier.height(Space.xxs))
        Text(progress, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        Spacer(Modifier.height(Space.xs))
        ProgressSegments(completed, current, dailySets, description = null)
        Spacer(Modifier.height(Space.m))
        content()
    }
}

/** What a stop on the Path is: done, the one to play today, or not reachable yet. */
enum class StopState { Completed, Today, Locked }

/**
 * One Daily Set on a Unit's trail: its node, its [title] and a [note] under it, with a small
 * [label] above the title for the stop the Learner is at. The node says the [state] by its icon as
 * well as its colour: a tick, a play arrow in a ring, or a lock.
 *
 * The trail runs up to the stop before unless it is the [first] and down to the next unless it is
 * the [last], and each half is coloured as walked when the Learner has come that way. With
 * [onClick] the whole stop is tapped as one and its node gives way; [action] is the icon at its
 * end that says what a tap does, and [onClickLabel] says it to a screen reader.
 */
@Composable
fun TrailStop(
    state: StopState,
    title: String,
    note: String?,
    first: Boolean,
    last: Boolean,
    walkedAbove: Boolean,
    walkedBelow: Boolean,
    modifier: Modifier = Modifier,
    label: String? = null,
    action: Int? = null,
    onClickLabel: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val walked = PolishEasyTheme.colors.success
    val ahead = scheme.outlineVariant
    val interaction = remember { MutableInteractionSource() }
    val tap = if (onClick == null) {
        Modifier.semantics(mergeDescendants = true) {}
    } else {
        // No ripple: the node going down is the response.
        Modifier.clickable(interaction, indication = null, onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(tap)
            .height(IntrinsicSize.Min),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(NodeColumn)
                .fillMaxHeight()
                .drawBehind {
                    val middle = Offset(size.width / 2, size.height / 2)
                    if (!first) drawLine(if (walkedAbove) walked else ahead, Offset(middle.x, 0f), middle, Trail.toPx())
                    if (!last) drawLine(if (walkedBelow) walked else ahead, middle, Offset(middle.x, size.height), Trail.toPx())
                }
                .padding(vertical = Space.xs),
        ) {
            StopNode(state, interaction)
        }
        Spacer(Modifier.width(Space.s))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = Space.xs),
        ) {
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (state == StopState.Today) scheme.primary else scheme.onSurfaceVariant,
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (state == StopState.Locked) scheme.onSurfaceVariant else scheme.onSurface,
            )
            if (note != null) Text(note, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
        if (action != null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(Size.touch)) {
                Icon(painterResource(action), contentDescription = null, tint = scheme.primary, modifier = Modifier.size(Size.icon))
            }
        }
    }
}

/**
 * The node of a stop. One that can be tapped is raised on the bottom edge and goes down with
 * [interaction]; a locked one is flat. Today's has a ring around it.
 */
@Composable
private fun StopNode(state: StopState, interaction: MutableInteractionSource) {
    val scheme = MaterialTheme.colorScheme
    val colors = PolishEasyTheme.colors
    when (state) {
        StopState.Completed -> Raised(colors.success, colors.successEdge, interaction) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = colors.onSuccess, modifier = Modifier.size(Size.feedbackIcon))
        }
        StopState.Today -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .border(Edge.border, colors.brand, CircleShape)
                .padding(Space.xs),
        ) {
            Raised(colors.brand, colors.brandEdge, interaction) {
                Icon(painterResource(R.drawable.ic_play_arrow), contentDescription = null, tint = colors.onBrand, modifier = Modifier.size(Size.feedbackIcon))
            }
        }
        StopState.Locked -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Node)
                .background(scheme.surfaceContainerHighest, CircleShape),
        ) {
            Icon(painterResource(R.drawable.ic_lock), contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(Size.icon))
        }
    }
}

@Composable
private fun Raised(fill: Color, edge: Color, interaction: MutableInteractionSource, icon: @Composable () -> Unit) {
    Edged(fill = fill, edge = edge, corner = Node / 2, interaction = interaction) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(Node)) { icon() }
    }
}
