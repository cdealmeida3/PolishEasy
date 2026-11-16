package com.polisheasy.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
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

/**
 * The layout of every Tip and Exercise: the progress bar at the top, [content] from the top and
 * scrolling if it must, and the main action pinned to the bottom, which is the top of the keyboard
 * while it is open.
 *
 * The bottom holds [feedback] when there is some, and otherwise [action] with [keyRow] below it.
 * Content starts at the same place whichever it is, so nothing moves when feedback appears.
 */
@Composable
fun DailySetFrame(
    progress: Float,
    progressDescription: String,
    modifier: Modifier = Modifier,
    feedback: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    keyRow: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sides = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .windowInsetsPadding(sides)
                .widthIn(max = Size.contentMax)
                .padding(start = Space.screenSide, end = Space.screenSide, top = Space.screenTop),
        ) {
            ProgressBar(progress, progressDescription)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = Space.l, bottom = Space.m),
                content = content,
            )
        }
        if (feedback != null) {
            feedback()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                ),
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
            .fillMaxWidth()
            .height(Size.progressBar)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
    }
}
