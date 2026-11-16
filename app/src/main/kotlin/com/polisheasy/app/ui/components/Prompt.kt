package com.polisheasy.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.polisheasy.app.ui.theme.inPolish
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.Edge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import com.polisheasy.app.ui.theme.Size

/** A Polish prompt up to this long is a word or two, and is set largest. One longer than [SHORT_PROMPT_LETTERS] uses the smallest prompt style. */
private const val WORD_PROMPT_LETTERS = 12
private const val SHORT_PROMPT_LETTERS = 20

/**
 * A Tip's prose in bites: the [lead], the one thing to take away, set apart, and under it each of
 * the [points] on a line of its own.
 */
@Composable
fun TipPoints(lead: String, points: List<String>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(lead, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
        points.forEach { point ->
            Spacer(Modifier.height(Space.s))
            Row {
                // The dot sits on the first line of its sentence.
                Box(contentAlignment = Alignment.Center, modifier = Modifier.height(Size.icon)) {
                    Box(Modifier.size(Space.xs).background(scheme.primary, CircleShape))
                }
                Spacer(Modifier.width(Space.s))
                Text(point, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
            }
        }
    }
}

/** What to do, above a prompt: "Type this in Polish". Also the small label of a Tip or a Review. */
@Composable
fun Instruction(text: String, modifier: Modifier = Modifier, highlighted: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * The Polish being learned: the biggest, boldest thing on the screen. It is never shrunk to fit:
 * a short one is set very large, a longer one wraps in a smaller style.
 */
@Composable
fun PolishPrompt(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = when {
            text.length <= WORD_PROMPT_LETTERS -> MaterialTheme.typography.displaySmall
            text.length <= SHORT_PROMPT_LETTERS -> MaterialTheme.typography.headlineLarge
            else -> MaterialTheme.typography.headlineMedium
        }.inPolish(),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

/** A prompt in the Base Language, to be answered in Polish. */
@Composable
fun BasePrompt(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

/**
 * What an Exercise asks about, set apart at the top of the screen on a soft field: the prompt as
 * [content], with the [speaker] for it at the end when it can be heard.
 */
@Composable
fun PromptCard(modifier: Modifier = Modifier, speaker: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = PromptCardLeast)
                .padding(start = Space.l, end = if (speaker == null) Space.l else Space.s, top = Space.m, bottom = Space.m),
        ) {
            Box(modifier = Modifier.weight(1f)) { content() }
            if (speaker != null) {
                Spacer(Modifier.width(Space.xs))
                speaker()
            }
        }
    }
}

/**
 * What a conjugation Exercise is about, in one small label above its sentence: the verb and the
 * person asked for.
 */
@Composable
fun PromptChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            .border(Edge.hairline, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(horizontal = Space.s, vertical = Space.xxs),
    )
}

private val PromptCardLeast = 96.dp
