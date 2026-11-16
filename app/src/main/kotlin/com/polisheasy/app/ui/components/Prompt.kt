package com.polisheasy.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** A Polish prompt longer than this wraps, and uses the smaller of the two prompt styles. */
private const val SHORT_PROMPT_LETTERS = 20

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

/** The Polish being learned: the biggest, boldest thing on the screen. It is never shrunk to fit. */
@Composable
fun PolishPrompt(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = if (text.length <= SHORT_PROMPT_LETTERS) {
            MaterialTheme.typography.headlineLarge
        } else {
            MaterialTheme.typography.headlineMedium
        },
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
