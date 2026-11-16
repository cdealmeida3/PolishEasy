package com.polisheasy.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import com.polisheasy.app.ui.theme.Size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Space
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Scaled
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource

/** The title at the top of the Path and of Settings, with a quiet [note] under it if there is one. Home has none. */
@Composable
fun TopBar(title: String, modifier: Modifier = Modifier, note: String? = null) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Space.m),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        if (note != null) {
            Text(note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * A plain card: a white field with a thin border and no shadow. [padded] is off for a card whose
 * rows reach its edges and keep their own space, such as a card of settings.
 */
@Composable
fun ContentCard(modifier: Modifier = Modifier, padded: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Edge.hairline, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = if (padded) Modifier.padding(Space.m) else Modifier, content = content)
    }
}

/**
 * A screen outside a Daily Set: clear of the system bars, with the standard space at its edges,
 * and never wider than a phone's worth of content.
 */
@Composable
fun ScreenFrame(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        Column(
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = Size.contentMax)
                .fillMaxWidth()
                .padding(start = Space.screenSide, end = Space.screenSide, top = Space.screenTop, bottom = Space.screenBottom),
            content = content,
        )
    }
}

/**
 * One small fact with a number: the [value] large, and under it what it counts. Several sit side
 * by side in a [StatRow]. A screen reader hears [description], the fact as a sentence.
 */
@Composable
fun StatTile(value: String, label: String, description: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Edge.hairline, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = Space.xs, vertical = Space.s)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

/** [StatTile]s side by side, each as wide and as tall as the others. At a large font size they go one under the other. */
@Composable
fun StatRow(modifier: Modifier = Modifier, tiles: List<@Composable (Modifier) -> Unit>) {
    if (Scaled.roomy()) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs), modifier = modifier.fillMaxWidth()) {
            tiles.forEach { tile -> tile(Modifier.fillMaxWidth()) }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            tiles.forEach { tile -> tile(Modifier.weight(1f).fillMaxHeight()) }
        }
    }
}

/**
 * One thing worth knowing, as the welcome lists them: an [icon] in a soft circle, a short [title]
 * and a [note] under it.
 */
@Composable
fun Point(@DrawableRes icon: Int, title: String, note: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(Size.touch).background(scheme.primaryContainer, CircleShape),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(Size.icon))
        }
        Spacer(Modifier.width(Space.m))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            Text(note, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
    }
}

/**
 * Something offered once, where the Learner has a reason to want it, such as the Reminder at the
 * end of a Daily Set: an [icon], a [title] and a [note], and under them the [action] that takes
 * it. Without an [action] it only says what happened.
 */
@Composable
fun OfferCard(
    @DrawableRes icon: Int,
    title: String,
    note: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    ContentCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Icon(painterResource(icon), contentDescription = null, tint = scheme.primary, modifier = Modifier.size(Size.icon))
            Spacer(Modifier.width(Space.s))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                Text(note, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
        }
        if (action != null) {
            Spacer(Modifier.height(Space.s))
            SecondaryButton(action, onAction)
        }
    }
}

/**
 * [content] arriving to mark what has just happened: with [play] it fades in and grows to its
 * size once; without, it is simply there.
 */
@Composable
fun Arriving(play: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(!play).apply { targetState = true } }
    AnimatedVisibility(visibleState = state, enter = Motion.arrive(), exit = ExitTransition.None, modifier = modifier) { content() }
}

/**
 * Fades the bottom of a scrolling list into the screen behind it while there is more below, so a
 * list that runs under a pinned button is not cut off hard. It is the one fade in the app.
 */
@Composable
fun Modifier.fadingBottom(more: Boolean): Modifier {
    val behind = MaterialTheme.colorScheme.background
    return drawWithContent {
        drawContent()
        if (more) {
            val depth = FadeDepth.toPx()
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, behind), startY = size.height - depth, endY = size.height),
                topLeft = Offset(0f, size.height - depth),
                size = androidx.compose.ui.geometry.Size(size.width, depth),
            )
        }
    }
}

private val FadeDepth = 48.dp
