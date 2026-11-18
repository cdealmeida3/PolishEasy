package com.polisheasy.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import com.polisheasy.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.Scaled
import com.polisheasy.app.ui.PolishEasyTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

/**
 * What every Tip and Exercise of a Daily Set sits in: a close button and the progress bar at the
 * top, which stay where they are, and below them the [step] being shown. When [step] changes the
 * old one slides out and the new one slides in; [content] draws either, usually as a [DailySetStep].
 * With [onSkip], a quiet "Skip" ([skipLabel]) sits after the progress bar.
 */
@Composable
fun <K> DailySetFrame(
    progress: Float,
    progressDescription: String,
    closeLabel: String,
    onClose: () -> Unit,
    step: K,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
    skipLabel: String = "",
    content: @Composable (step: K) -> Unit,
) {
    val transition = Motion.step()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .widthIn(max = Size.contentMax)
                // The close icon, not its touch area, lines up with the content below.
                .padding(start = Space.s, end = Space.screenSide, top = Space.xs),
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(Size.touch)) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    contentDescription = closeLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Scaled.icon()),
                )
            }
            Spacer(Modifier.width(Space.xxs))
            ProgressBar(progress, progressDescription, Modifier.weight(1f))
            if (onSkip != null) {
                Spacer(Modifier.width(Space.xs))
                QuietButton(skipLabel, onSkip, subdued = true)
            }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = { transition },
            label = "Daily Set step",
            modifier = Modifier.weight(1f),
        ) { shown -> content(shown) }
    }
}

/**
 * One Tip or Exercise inside a [DailySetFrame]: the prompt from the top and the answers from the
 * bottom, under the thumb. [content] is the prompt and [answer] what the Learner answers with, which
 * sits just above the main action. If the two do not fit they scroll as one.
 *
 * The bottom holds [action] with [keyRow] below it, on top of the keyboard while it is open.
 * [feedback] rises over that place when there is some. The room it needs is kept from the start,
 * so nothing moves when it appears. [reserve] is that room; a Round's step, which has no feedback
 * panel, keeps only the room of its main action.
 */
@Composable
fun DailySetStep(
    modifier: Modifier = Modifier,
    feedback: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    keyRow: (@Composable () -> Unit)? = null,
    answer: (@Composable ColumnScope.() -> Unit)? = null,
    reserve: Dp = Size.feedbackRoom,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    // A panel taller than the room kept for it, at a large font size, pushes the answers up to stay clear.
    var panel by remember { mutableStateOf(0.dp) }
    val room = if (answer == null) 0.dp else maxOf(reserve, if (feedback != null) panel else 0.dp)
    Box(modifier = modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .widthIn(max = Size.contentMax)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(start = Space.screenSide, end = Space.screenSide, top = Space.m, bottom = Space.m),
                ) {
                    Column(content = content)
                    if (answer != null) {
                        Spacer(Modifier.height(Space.l))
                        Column(content = answer)
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .heightIn(min = room),
            ) {
                if (action != null) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = Size.contentMax)
                            .padding(
                                start = Space.screenSide,
                                end = Space.screenSide,
                                bottom = if (keyRow == null) Space.screenBottom else Space.xs,
                            ),
                    ) { action() }
                }
                keyRow?.invoke()
            }
        }
        if (feedback != null) {
            // Starts hidden, so the panel rises the moment it is first shown.
            val rising = remember { MutableTransitionState(false).apply { targetState = true } }
            AnimatedVisibility(
                visibleState = rising,
                enter = Motion.panelEnter(),
                exit = ExitTransition.None,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { panel = with(density) { it.height.toDp() } },
            ) {
                feedback()
            }
        }
    }
}

/** How far through the Daily Set the Learner is, from 0 to 1. [description] is what a screen reader says. */
@Composable
fun ProgressBar(progress: Float, description: String, modifier: Modifier = Modifier) {
    val shown by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = Motion.progress(),
        label = "Daily Set progress",
    )
    Box(
        modifier = modifier
            .height(Size.progressBar)
            .clip(CircleShape)
            // One step quieter than other tracks, so the brand's fill stands out from it in the dark theme too.
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
            },
    ) {
        Box(
            modifier = Modifier
                // The spring may pass its target for a moment; the bar never draws outside its track.
                .fillMaxWidth(shown.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(PolishEasyTheme.colors.brand, CircleShape),
        )
    }
}
