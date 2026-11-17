package com.polisheasy.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Scaled
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish

/**
 * Who the Learner is talking to in a chat: the first letter of [person] in a round avatar, the
 * name, the [register] the chat is held in, and the [scene] under them.
 */
@Composable
fun ChatHeader(person: String, scene: String, modifier: Modifier = Modifier, register: (@Composable () -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(person)
            Spacer(Modifier.width(Space.s))
            Text(person, style = MaterialTheme.typography.titleLarge.inPolish(), color = scheme.onSurface, modifier = Modifier.weight(1f, fill = false))
            register?.let {
                Spacer(Modifier.width(Space.s))
                it()
            }
        }
        Spacer(Modifier.height(Space.xs))
        Text(scene, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    }
}

/** The first letter of [person], in a circle in the brand's tint: the app has no faces of people. */
@Composable
private fun Avatar(person: String) {
    val scheme = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(Size.touch)
            .background(scheme.primaryContainer, CircleShape)
            .clearAndSetSemantics { },
    ) {
        Text(person.trim().take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = scheme.onPrimaryContainer)
    }
}

/**
 * One bubble of a chat. The person's are on the left, on `surfaceContainerHigh`, with a [speaker];
 * the Learner's ([mine]) on the right in `primaryContainer`, or amber when [missed], the right
 * reply put in for a wrong one. The Polish is large and its meaning small under it. While
 * [typing] the person's bubble shows three dots, as a messenger does before the words arrive.
 */
@Composable
fun ChatBubble(
    text: String,
    meaning: String,
    mine: Boolean,
    modifier: Modifier = Modifier,
    missed: Boolean = false,
    typing: Boolean = false,
    typingLabel: String = "",
    speaker: (@Composable () -> Unit)? = null,
) {
    val colors = PolishEasyTheme.colors
    val scheme = MaterialTheme.colorScheme
    val (fill, ink) = when {
        !mine -> scheme.surfaceContainerHigh to scheme.onSurface
        missed -> colors.cautionContainer to colors.onCautionContainer
        else -> scheme.primaryContainer to scheme.onPrimaryContainer
    }
    // The corner nearest the speaker is the small one, as a messenger draws its tail.
    val shape = if (mine) {
        RoundedCornerShape(topStart = Corner.large, topEnd = Corner.large, bottomStart = Corner.large, bottomEnd = Corner.extraSmall / 2)
    } else {
        RoundedCornerShape(topStart = Corner.large, topEnd = Corner.large, bottomStart = Corner.extraSmall / 2, bottomEnd = Corner.large)
    }
    Row(
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = Size.bubbleMax)
                .background(fill, shape)
                .padding(start = Space.m, end = if (speaker != null && !typing) Space.xxs else Space.m, top = Space.s, bottom = Space.s),
        ) {
            AnimatedContent(
                targetState = typing,
                transitionSpec = { fadeIn(tween(Motion.BUBBLE_MILLIS)) togetherWith fadeOut(tween(Motion.BUBBLE_MILLIS)) },
                label = "Typing",
            ) { dots ->
                if (dots) {
                    Text(
                        "• • •",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier
                            .heightIn(min = Scaled.icon())
                            .semantics { contentDescription = typingLabel },
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(text, style = MaterialTheme.typography.titleMedium.inPolish(), color = ink)
                            Text(meaning, style = MaterialTheme.typography.bodySmall, color = if (mine) ink else scheme.onSurfaceVariant)
                        }
                        speaker?.let {
                            Spacer(Modifier.width(Space.xxs))
                            it()
                        }
                    }
                }
            }
        }
    }
}
