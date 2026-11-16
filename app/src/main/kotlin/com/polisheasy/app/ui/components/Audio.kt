package com.polisheasy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.polisheasy.app.ui.theme.Size

/** The big round button of a listening Exercise. [description] is what a screen reader says. */
@Composable
fun PlayButton(description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(Size.playButton)
            .clip(CircleShape)
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

/** The small speaker beside Polish text. [tint] is for a speaker that sits on a coloured panel. */
@Composable
fun SpeakerButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    IconButton(onClick = onClick, modifier = modifier.size(Size.touch)) {
        Icon(painterResource(R.drawable.ic_volume_up), contentDescription = description, tint = tint)
    }
}
