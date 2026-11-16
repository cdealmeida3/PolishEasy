package com.polisheasy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import com.polisheasy.app.R
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Size
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Scaled
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import com.polisheasy.app.ui.theme.Space
import androidx.compose.foundation.shape.CircleShape

/**
 * The big button of a listening Exercise. [description] is what a screen reader says. It is a
 * circle at rest and a rounded square while [playing], so a Clip being heard can be seen.
 */
@Composable
fun PlayButton(description: String, onClick: () -> Unit, modifier: Modifier = Modifier, playing: Boolean = false) {
    val corner by animateDpAsState(
        targetValue = if (playing) Corner.large else Size.playButton / 2,
        animationSpec = Motion.shape(),
        label = "Clip playing",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(Size.playButton)
            .clip(RoundedCornerShape(corner))
            .background(PolishEasyTheme.colors.brand)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Icon(
            painterResource(R.drawable.ic_volume_up),
            contentDescription = description,
            tint = PolishEasyTheme.colors.onBrand,
            modifier = Modifier.size(Size.playIcon),
        )
    }
}

/**
 * The [play] button of a listening Exercise in the middle, with its [helper], such as "Slow", to
 * one side. At a large font size the helper goes under it, where its word fits on one line.
 */
@Composable
fun PlayControls(play: @Composable () -> Unit, helper: @Composable () -> Unit, modifier: Modifier = Modifier) {
    if (Scaled.roomy()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth()) {
            play()
            Spacer(Modifier.height(Space.xs))
            helper()
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            play()
            Row(modifier = Modifier.weight(1f)) {
                Spacer(Modifier.width(Space.m))
                helper()
            }
        }
    }
}

/**
 * The small speaker beside Polish text: a round tonal button, so it reads as something to tap, and
 * it grows with the phone's font size. With a [tint] it has no field of its own, for a speaker
 * that sits on a coloured panel.
 */
@Composable
fun SpeakerButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val icon = Scaled.icon()
    IconButton(onClick = onClick, modifier = modifier.size(maxOf(Size.touch, icon + Space.m))) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(icon + Space.m)
                .background(if (tint == null) scheme.primaryContainer else Color.Transparent, CircleShape),
        ) {
            Icon(
                painterResource(R.drawable.ic_volume_up),
                contentDescription = description,
                tint = tint ?: scheme.onPrimaryContainer,
                modifier = Modifier.size(icon),
            )
        }
    }
}
