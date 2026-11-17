package com.polisheasy.app.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.polisheasy.app.LocalSpeech
import com.polisheasy.app.R
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Scaled
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space

/**
 * The microphone of *Powtórz na głos*. Tapped, the Learner is heard saying Polish, and [onHeard]
 * gets what was understood, best guess first, or nothing. The first tap asks for the microphone;
 * refused, it is not offered again. Where speech cannot be offered nothing is shown, so it never
 * stands in the way. While it listens it is a rounded square, as the play button is while a Clip
 * plays, and a tap stops it. [large] is the size of the play button, for where speaking is the
 * answer; otherwise it is the size of a speaker. [onStart] is called as the Learner taps to speak,
 * before the microphone is asked for, so what is playing can wait.
 */
@Composable
fun MicButton(
    description: String,
    listeningLabel: String,
    onHeard: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    onStart: () -> Unit = {},
) {
    val speech = LocalSpeech.current
    if (!speech.available) return
    val heard by rememberUpdatedState(onHeard)
    val started by rememberUpdatedState(onStart)
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            started()
            speech.listen { heard(it) }
        } else {
            speech.refused()
        }
    }
    // Leaving the screen stops listening: what is heard after belongs to nothing.
    DisposableEffect(Unit) { onDispose { speech.stop() } }
    val listening = speech.listening
    val size = if (large) Size.playButton else Scaled.icon() + Space.m
    val corner by animateDpAsState(
        targetValue = if (listening) (if (large) Corner.large else Corner.small) else size / 2,
        animationSpec = Motion.shape(),
        label = "Listening",
    )
    val colors = PolishEasyTheme.colors
    val scheme = MaterialTheme.colorScheme
    val fill = when {
        large -> colors.brand
        listening -> scheme.primary
        else -> scheme.primaryContainer
    }
    val ink = when {
        large -> colors.onBrand
        listening -> scheme.onPrimary
        else -> scheme.onPrimaryContainer
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(maxOf(Size.touch, size))
            .clip(RoundedCornerShape(corner))
            .clickable(role = Role.Button) {
                when {
                    listening -> speech.stop()
                    speech.permitted -> {
                        started()
                        speech.listen { heard(it) }
                    }
                    else -> {
                        started()
                        ask.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
            .semantics { if (listening) stateDescription = listeningLabel },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(corner))
                .background(fill)
                .then(if (large && listening) Modifier.border(Edge.border * 2, colors.brandEdge, RoundedCornerShape(corner)) else Modifier),
        ) {
            Icon(
                painterResource(R.drawable.ic_mic),
                contentDescription = description,
                tint = ink,
                modifier = Modifier.size(if (large) Size.playIcon else Scaled.icon()),
            )
        }
    }
}
