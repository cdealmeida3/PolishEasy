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

/** A plain card: a white field with a thin border and no shadow. */
@Composable
fun ContentCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Edge.hairline, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Space.m), content = content)
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
