package com.polisheasy.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.polisheasy.app.R
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Scaled
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish

// The class recordings: a chapter's list of them on its Lesson screen, and the player.

/** How faint a recording that is not on the phone yet is drawn. */
private const val NOT_ADDED_ALPHA = 0.55f

/**
 * One class recording in a chapter's Słuchaj: its [number] as on the publisher's discs in a badge
 * of the chapter's colour, its Polish [title], and a [note] such as its length. While it is not
 * on the phone it is faint, cannot be tapped, and a screen reader hears [said].
 */
@Composable
fun RecordingRow(
    number: String,
    title: String,
    note: String,
    accent: Int,
    onClick: (() -> Unit)?,
    onClickLabel: String,
    modifier: Modifier = Modifier,
    said: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = accentColors(accent)
    val tap = if (onClick == null) Modifier else Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Size.touch)
            .clip(MaterialTheme.shapes.medium)
            .then(tap)
            .semantics(mergeDescendants = true) { if (said != null) contentDescription = "$number, $title, $note. $said" }
            .padding(vertical = Space.xxs),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Size.touch)
                .alpha(if (onClick == null) NOT_ADDED_ALPHA else 1f)
                .background(colors.container, RoundedCornerShape(Corner.small)),
        ) {
            Text(number, style = MaterialTheme.typography.labelLarge, color = colors.onContainer)
        }
        Spacer(Modifier.width(Space.s))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.inPolish(), color = scheme.onSurface)
            Text(note, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        }
        if (onClick != null) {
            Icon(
                painterResource(R.drawable.ic_play_arrow),
                contentDescription = null,
                tint = colors.onContainer,
                modifier = Modifier.size(Scaled.icon()),
            )
        }
    }
}

/**
 * The player of a class recording: how far through it is ([progress], from 0 to 1, and [time] in
 * words), and under it 5 s back, the previous line, play or pause in the chapter's colour, the
 * next line, and 5 s on. A screen reader hears [progressDescription] for the bar.
 */
@Composable
fun AudioPlayerBar(
    playing: Boolean,
    progress: Float,
    time: String,
    progressDescription: String,
    accent: Int,
    playLabel: String,
    pauseLabel: String,
    backLabel: String,
    onLabel: String,
    previousLabel: String,
    nextLabel: String,
    onPlayPause: () -> Unit,
    onBack: () -> Unit,
    onOn: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = accentColors(accent)
    Surface(
        color = scheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Edge.hairline, scheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Space.m)) {
            ProgressBar(progress, progressDescription, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Space.xs))
            Text(time, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(Space.xs))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                PlayerIcon(R.drawable.ic_replay_5, backLabel, onBack)
                PlayerIcon(R.drawable.ic_skip_previous, previousLabel, onPrevious)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(Size.playButton)
                        .clip(CircleShape)
                        .background(colors.fill)
                        .clickable(role = Role.Button, onClick = onPlayPause)
                        .semantics { contentDescription = if (playing) pauseLabel else playLabel },
                ) {
                    Icon(
                        painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                        contentDescription = null,
                        tint = colors.onFill,
                        modifier = Modifier.size(Size.playIcon),
                    )
                }
                PlayerIcon(R.drawable.ic_skip_next, nextLabel, onNext)
                PlayerIcon(R.drawable.ic_forward_5, onLabel, onOn)
            }
        }
    }
}

@Composable
private fun PlayerIcon(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(Size.touch)) {
        Icon(
            painterResource(icon),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(Scaled.icon()),
        )
    }
}

/**
 * Step mode's turn, the book's *Proszę powtórzyć*: shown while the player waits for the Learner
 * to say the line aloud, with [action] beside it, the microphone where the phone can hear. A
 * screen reader hears it as it appears.
 */
@Composable
fun YourTurn(visible: Boolean, title: String, note: String, accent: Int, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val colors = accentColors(accent)
    AnimatedVisibility(visible, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.container, MaterialTheme.shapes.medium)
                .padding(horizontal = Space.m, vertical = Space.s)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            Icon(
                painterResource(R.drawable.ic_record_voice_over),
                contentDescription = null,
                tint = colors.onContainer,
                modifier = Modifier.size(Scaled.icon()),
            )
            Spacer(Modifier.width(Space.s))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.inPolish(), color = colors.onContainer)
                Text(note, style = MaterialTheme.typography.labelMedium, color = colors.onContainer)
            }
            action?.let {
                Spacer(Modifier.width(Space.s))
                it()
            }
        }
    }
}

/** A listening task on a recording: its Polish [question], its English [meaning] under it, and what answers it, [content]. */
@Composable
fun ListeningTaskCard(question: String, meaning: String?, label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = scheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Edge.hairline, scheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Space.m)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(Space.xxs))
            Text(question, style = MaterialTheme.typography.titleMedium.inPolish(), color = scheme.onSurface)
            if (meaning != null) Text(meaning, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(Space.s))
            content()
        }
    }
}
