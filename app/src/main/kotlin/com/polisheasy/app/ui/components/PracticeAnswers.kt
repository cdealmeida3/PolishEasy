package com.polisheasy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.polisheasy.app.R
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Scaled
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish

/**
 * What has been typed on a [Keypad], large and centred in a slot of its own. Empty, it says "?".
 * Once the question is answered it takes the colours of its [mark], with the tick or the cross
 * beside the digits. [description] is what a screen reader says for it.
 */
@Composable
fun NumberDisplay(typed: String, description: String, modifier: Modifier = Modifier, mark: Mark? = null) {
    val scheme = MaterialTheme.colorScheme
    val colors = PolishEasyTheme.colors
    val (fill, line, ink) = when (mark) {
        Mark.Right -> Triple(colors.successContainer, colors.success, colors.onSuccessContainer)
        Mark.Missed -> Triple(colors.cautionContainer, colors.caution, colors.onCautionContainer)
        null -> Triple(scheme.secondaryContainer, scheme.outline, scheme.onSecondaryContainer)
    }
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs, Alignment.CenterHorizontally),
            modifier = Modifier
                .widthIn(min = DisplayLeast)
                .heightIn(min = Size.playButton)
                .background(fill, MaterialTheme.shapes.large)
                .border(Edge.border, line, MaterialTheme.shapes.large)
                .padding(horizontal = Space.l, vertical = Space.xs)
                .clearAndSetSemantics {
                    contentDescription = description
                    liveRegion = LiveRegionMode.Polite
                },
        ) {
            Text(
                typed.ifEmpty { "?" },
                style = MaterialTheme.typography.displaySmall,
                color = if (typed.isEmpty()) ink.copy(alpha = Motion.FADED_ALPHA) else ink,
            )
            if (mark != null) MarkIcon(mark)
        }
    }
}

private val DisplayLeast = 168.dp

/**
 * The keys a number is typed on: the digits as on a telephone, with a key that takes the last
 * digit back to the left of the zero and the key that checks the answer to its right. Not
 * [enabled], every key fades and none responds; the check key alone waits until [canCheck].
 */
@Composable
fun Keypad(
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    onCheck: () -> Unit,
    eraseLabel: String,
    checkLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    canCheck: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = PolishEasyTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(Space.xs),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else Motion.FADED_ALPHA),
    ) {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                row.forEach { digit -> DigitKey(digit, enabled, onDigit, Modifier.weight(1f)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            Edged(
                fill = scheme.secondaryContainer,
                edge = scheme.outlineVariant,
                corner = Corner.medium,
                border = Edge.border,
                enabled = enabled,
                onClick = onErase,
                modifier = Modifier.weight(1f).heightIn(min = Size.control),
            ) {
                Icon(
                    painterResource(R.drawable.ic_backspace),
                    contentDescription = eraseLabel,
                    tint = scheme.onSecondaryContainer,
                    modifier = Modifier.size(Scaled.icon()),
                )
            }
            DigitKey(0, enabled, onDigit, Modifier.weight(1f))
            val checks = enabled && canCheck
            Edged(
                fill = if (checks) colors.brand else scheme.surfaceContainerHighest,
                edge = if (checks) colors.brandEdge else scheme.surfaceContainerHighest,
                corner = Corner.medium,
                flat = !checks,
                enabled = checks,
                onClick = onCheck,
                modifier = Modifier.weight(1f).heightIn(min = Size.control),
            ) {
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = checkLabel,
                    tint = if (checks) colors.onBrand else scheme.onSurfaceVariant,
                    modifier = Modifier.size(Scaled.icon()),
                )
            }
        }
    }
}

@Composable
private fun DigitKey(digit: Int, enabled: Boolean, onDigit: (Int) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Edged(
        fill = scheme.surfaceContainerLowest,
        edge = scheme.outlineVariant,
        corner = Corner.medium,
        border = Edge.border,
        enabled = enabled,
        onClick = { onDigit(digit) },
        modifier = modifier.heightIn(min = Size.control),
    ) {
        Text("$digit", style = MaterialTheme.typography.headlineMedium, color = scheme.onSurface)
    }
}

/**
 * One of the words a noun is sorted under. [holds] is how many are under it so far, as a number,
 * and [holdsDescription] the same in words for a screen reader.
 */
class BucketView(val word: String, val holds: String, val holdsDescription: String, val state: OptionState)

/**
 * The words to sort a noun under, side by side, or one under the other when the text is large.
 * Each is a card in the states of an [OptionCard], with the Polish word large and under it a
 * small count of what it holds, so the Learner sees the nouns gather where they belong.
 */
@Composable
fun BucketRow(buckets: List<BucketView>, onClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (Scaled.roomy()) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs), modifier = modifier.fillMaxWidth()) {
            buckets.forEachIndexed { index, bucket -> Bucket(bucket, { onClick(index) }, Modifier.fillMaxWidth(), tall = false) }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = modifier.fillMaxWidth()) {
            buckets.forEachIndexed { index, bucket -> Bucket(bucket, { onClick(index) }, Modifier.weight(1f), tall = true) }
        }
    }
}

@Composable
private fun Bucket(bucket: BucketView, onClick: () -> Unit, modifier: Modifier, tall: Boolean) {
    val colors = PolishEasyTheme.colors
    val scheme = MaterialTheme.colorScheme
    val state = bucket.state
    val (fill, edge, label) = when (state) {
        OptionState.Idle, OptionState.Faded -> Triple(scheme.surfaceContainerLowest, scheme.outlineVariant, scheme.onSurface)
        OptionState.Chosen -> Triple(scheme.primaryContainer, scheme.primary, scheme.onPrimaryContainer)
        OptionState.Right -> Triple(colors.successContainer, colors.success, colors.onSuccessContainer)
        OptionState.Wrong -> Triple(colors.cautionContainer, colors.caution, colors.onCautionContainer)
    }
    Edged(
        fill = fill,
        edge = edge,
        corner = Corner.large,
        border = Edge.border,
        enabled = state == OptionState.Idle || state == OptionState.Chosen,
        onClick = onClick,
        modifier = modifier
            .heightIn(min = if (tall) Size.bucket else Size.control)
            .shaking(state == OptionState.Wrong)
            .alpha(if (state == OptionState.Faded) Motion.FADED_ALPHA else 1f),
    ) {
        val word = @Composable { Text(bucket.word, style = MaterialTheme.typography.headlineMedium.inPolish(), color = label) }
        val holds = @Composable {
            Text(
                bucket.holds,
                style = MaterialTheme.typography.labelLarge,
                color = label,
                modifier = Modifier
                    .border(Edge.hairline, edge, CircleShape)
                    .padding(horizontal = Space.s, vertical = Space.xxs)
                    .semantics { contentDescription = bucket.holdsDescription },
            )
        }
        if (tall) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.xs),
                modifier = Modifier.padding(Space.xs),
            ) {
                word()
                holds()
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s),
                modifier = Modifier.padding(horizontal = Space.m, vertical = Space.xs),
            ) {
                word()
                holds()
            }
        }
        when (state) {
            OptionState.Right -> MarkIcon(Mark.Right, Modifier.align(Alignment.TopEnd).padding(Space.xs), size = Size.tileMark)
            OptionState.Wrong -> MarkIcon(Mark.Missed, Modifier.align(Alignment.TopEnd).padding(Space.xs), size = Size.tileMark)
            else -> Unit
        }
    }
}

/**
 * A word being spelled: a slot for each of its [length] letters, the first of them holding the
 * [letters] placed so far. Once the word is whole it takes the colours of its [mark], with the
 * tick or the cross after it. [description] is what a screen reader says for it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpelledWord(letters: List<String>, length: Int, description: String, modifier: Modifier = Modifier, mark: Mark? = null) {
    val scheme = MaterialTheme.colorScheme
    val colors = PolishEasyTheme.colors
    val (fill, line, ink) = when (mark) {
        Mark.Right -> Triple(colors.successContainer, colors.success, colors.onSuccessContainer)
        Mark.Missed -> Triple(colors.cautionContainer, colors.caution, colors.onCautionContainer)
        null -> Triple(scheme.secondaryContainer, scheme.outline, scheme.onSecondaryContainer)
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Space.xxs, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(Space.xxs),
        itemVerticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        repeat(length) { place ->
            val letter = letters.getOrNull(place)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .widthIn(min = LetterSlotLeast)
                    .heightIn(min = Size.tile)
                    .background(if (letter == null) scheme.surfaceContainerHigh else fill, MaterialTheme.shapes.small)
                    .border(Edge.border, if (letter == null) scheme.outlineVariant else line, MaterialTheme.shapes.small),
            ) {
                if (letter != null) {
                    Text(letter, style = MaterialTheme.typography.headlineSmall.inPolish(), color = ink, modifier = Modifier.appearing())
                }
            }
        }
        if (mark != null) MarkIcon(mark)
    }
}

private val LetterSlotLeast = 30.dp

/**
 * The two answers of the Quick round, side by side: "no match" with a cross on the left and
 * "match" with a tick on the right. Not [enabled], they fade and do not respond.
 */
@Composable
fun MatchButtons(
    noMatchLabel: String,
    matchLabel: String,
    onNoMatch: () -> Unit,
    onMatch: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = PolishEasyTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else Motion.FADED_ALPHA),
    ) {
        MatchButton(noMatchLabel, R.drawable.ic_close, colors.caution, enabled, onNoMatch, Modifier.weight(1f))
        MatchButton(matchLabel, R.drawable.ic_check, colors.success, enabled, onMatch, Modifier.weight(1f))
    }
}

@Composable
private fun MatchButton(label: String, icon: Int, tint: Color, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Edged(
        fill = scheme.surfaceContainerLowest,
        edge = scheme.outlineVariant,
        corner = Corner.large,
        border = Edge.border,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier.heightIn(min = Size.letterOption),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.xxs),
            modifier = Modifier.padding(Space.xs),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(Scaled.icon(Size.feedbackIcon)))
            Text(label, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, textAlign = TextAlign.Center)
        }
    }
}
