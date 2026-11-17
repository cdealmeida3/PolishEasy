package com.polisheasy.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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

// The book's own Exercises, as the player draws them: sticky notes for gaps, lines drawn between
// pairs, P and N against statements, and the lines of a dialogue to put in order.

/**
 * A sticky note of a fill-the-gaps Exercise: a word on a pale yellow note, a little askew as notes
 * are, [tilt] one way or the other. Once [used] in a gap it fades and cannot be tapped.
 */
@Composable
fun StickyNote(word: String, used: Boolean, enabled: Boolean, onClick: () -> Unit, usedLabel: String, modifier: Modifier = Modifier, tilt: Int = 0) {
    val colors = PolishEasyTheme.colors
    Edged(
        fill = colors.note,
        edge = colors.noteEdge,
        corner = Corner.small,
        enabled = enabled && !used,
        onClick = onClick,
        modifier = modifier
            .graphicsLayer { rotationZ = NoteTilt * tilt }
            .alpha(if (used) Motion.FADED_ALPHA else 1f)
            .semantics { if (used) stateDescription = usedLabel },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.heightIn(min = Size.tile).padding(horizontal = Space.m)) {
            Text(word, style = MaterialTheme.typography.titleMedium.inPolish(), color = colors.onNote)
        }
    }
}

private const val NoteTilt = 1.5f

/**
 * A Polish text with gaps, as the book prints it: the words of [parts] at prompt size and between
 * them a gap for each word in [filled], empty or holding a note. The [selected] gap is outlined in
 * the brand's colour: a note goes there next. Tapping a filled gap gives its note back. Once the
 * answer is known each gap is coloured by [marks]. [gapDescription] names a gap for a screen reader.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GapText(
    parts: List<String>,
    filled: List<String?>,
    selected: Int?,
    marks: List<Boolean>?,
    enabled: Boolean,
    onGap: (Int) -> Unit,
    gapDescription: (Int, String?) -> String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = PolishEasyTheme.colors
    val style = MaterialTheme.typography.headlineSmall.inPolish()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
        itemVerticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        parts.forEachIndexed { index, part ->
            part.split(' ').filter { it.isNotBlank() }.forEach { Text(it, style = style, color = scheme.onSurface) }
            if (index < parts.lastIndex) {
                val word = filled.getOrNull(index)
                val mark = marks?.getOrNull(index)
                val (fill, line, ink) = when {
                    mark == true -> Triple(colors.successContainer, colors.success, colors.onSuccessContainer)
                    mark == false -> Triple(colors.cautionContainer, colors.caution, colors.onCautionContainer)
                    word != null -> Triple(colors.note, colors.noteEdge, colors.onNote)
                    index == selected -> Triple(scheme.primaryContainer, scheme.primary, scheme.onPrimaryContainer)
                    else -> Triple(scheme.surfaceContainerHigh, scheme.outline, scheme.onSurface)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .widthIn(min = GapLeast)
                        .heightIn(min = Size.touch)
                        .background(fill, MaterialTheme.shapes.small)
                        .border(if (index == selected && word == null) Edge.border * 2 else Edge.border, line, MaterialTheme.shapes.small)
                        .clickable(enabled = enabled, role = Role.Button) { onGap(index) }
                        .clearAndSetSemantics {
                            contentDescription = gapDescription(index, word)
                            role = Role.Button
                            this.selected = index == selected
                        }
                        .padding(horizontal = Space.s, vertical = Space.xxs),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(word ?: "${index + 1}", style = if (word == null) MaterialTheme.typography.labelLarge else style, color = ink)
                        if (mark != null) {
                            Spacer(Modifier.width(Space.xxs))
                            MarkIcon(if (mark) Mark.Right else Mark.Missed)
                        }
                    }
                }
            }
        }
    }
}

private val GapLeast = 72.dp

/**
 * The two columns of a match-the-pairs Exercise: the Polish on the [left], their meanings mixed on
 * the [right]. Tap a word, then its meaning: a right pair is joined by a line drawn between them
 * and turns green; a wrong meaning shakes. [joined] maps each joined word to its meaning's place.
 */
@Composable
fun MatchBoard(
    left: List<String>,
    right: List<String>,
    joined: Map<Int, Int>,
    selected: Int?,
    shaken: Pair<Int, Int>?,
    enabled: Boolean,
    onLeft: (Int) -> Unit,
    onRight: (Int) -> Unit,
    joinedDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = PolishEasyTheme.colors
    var board by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val leftAt = remember { mutableStateMapOf<Int, Offset>() }
    val rightAt = remember { mutableStateMapOf<Int, Offset>() }
    val lineColor = colors.success
    Row(
        horizontalArrangement = Arrangement.spacedBy(MatchGutter),
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { board = it }
            .drawBehind {
                joined.forEach { (from, to) ->
                    val start = leftAt[from] ?: return@forEach
                    val end = rightAt[to] ?: return@forEach
                    val width = Edge.border.toPx() * 2
                    drawLine(lineColor, start, end, width, StrokeCap.Round)
                    drawCircle(lineColor, width * 1.6f, start)
                    drawCircle(lineColor, width * 1.6f, end)
                }
            },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.weight(1f)) {
            left.forEachIndexed { index, word ->
                PairCard(
                    text = word,
                    polish = true,
                    state = when {
                        index in joined -> OptionState.Right
                        index == selected -> OptionState.Chosen
                        else -> OptionState.Idle
                    },
                    enabled = enabled && index !in joined,
                    onClick = { onLeft(index) },
                    description = if (index in joined) "$word, $joinedDescription ${right[joined.getValue(index)]}" else word,
                    shakes = 0,
                    modifier = Modifier.onGloballyPositioned { at ->
                        board?.let { leftAt[index] = it.localPositionOf(at, Offset(at.size.width.toFloat(), at.size.height / 2f)) }
                    },
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.weight(1f)) {
            right.forEachIndexed { index, meaning ->
                val done = index in joined.values
                PairCard(
                    text = meaning,
                    polish = false,
                    state = if (done) OptionState.Right else OptionState.Idle,
                    enabled = enabled && !done && selected != null,
                    onClick = { onRight(index) },
                    description = meaning,
                    shakes = if (shaken?.first == index) shaken.second else 0,
                    modifier = Modifier.onGloballyPositioned { at ->
                        board?.let { rightAt[index] = it.localPositionOf(at, Offset(0f, at.size.height / 2f)) }
                    },
                )
            }
        }
    }
}

private val MatchGutter = 32.dp

@Composable
private fun PairCard(
    text: String,
    polish: Boolean,
    state: OptionState,
    enabled: Boolean,
    onClick: () -> Unit,
    description: String,
    shakes: Int,
    modifier: Modifier = Modifier,
) {
    val colors = PolishEasyTheme.colors
    val scheme = MaterialTheme.colorScheme
    val (fill, edge, ink) = when (state) {
        OptionState.Chosen -> Triple(scheme.primaryContainer, scheme.primary, scheme.onPrimaryContainer)
        OptionState.Right -> Triple(colors.successContainer, colors.success, colors.onSuccessContainer)
        else -> Triple(scheme.surfaceContainerLowest, scheme.outlineVariant, scheme.onSurface)
    }
    Edged(
        fill = fill,
        edge = edge,
        corner = Corner.medium,
        border = Edge.border,
        enabled = enabled,
        onClick = onClick,
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .shakingEach(shakes)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                selected = state == OptionState.Chosen
            },
    ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.heightIn(min = Size.control).padding(horizontal = Space.s, vertical = Space.xs)) {
            val style = MaterialTheme.typography.titleSmall
            Text(text, style = if (polish) style.inPolish() else style, color = ink)
        }
    }
}

/** One shake each time [count] goes up: a wrong meaning tapped again shakes again. */
@Composable
internal fun Modifier.shakingEach(count: Int): Modifier {
    val spec = Motion.shake()
    val share = remember { Animatable(0f) }
    LaunchedEffect(count) { if (count > 0 && spec != null) share.animateTo(0f, spec) }
    return graphicsLayer { translationX = share.value * ShakeReach.toPx() }
}

private val ShakeReach = 8.dp

/**
 * A statement of a true-or-false Exercise: its [number], the Polish, its [meaning] under it, and P
 * and N to mark it. Once the answer is known the mark given gets its tick or its cross.
 */
@Composable
fun StatementRow(
    number: Int,
    text: String,
    meaning: String,
    marked: Boolean?,
    right: Boolean?,
    enabled: Boolean,
    onMark: (Boolean) -> Unit,
    trueLabel: String,
    falseLabel: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, MaterialTheme.shapes.medium)
            .border(Edge.hairline, scheme.outlineVariant, MaterialTheme.shapes.medium)
            .padding(start = Space.s, end = Space.xs, top = Space.xs, bottom = Space.xs),
    ) {
        Text("$number", style = MaterialTheme.typography.titleMedium, color = scheme.primary)
        Spacer(Modifier.width(Space.s))
        Column(modifier = Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.titleMedium.inPolish(), color = scheme.onSurface)
            Text(meaning, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(Space.xs))
        listOf(true to trueLabel, false to falseLabel).forEach { (value, label) ->
            MarkChip(
                mark = if (value) "P" else "N",
                description = label,
                chosen = marked == value,
                result = if (marked == value) right else null,
                enabled = enabled,
                onClick = { onMark(value) },
            )
        }
    }
}

/** P or N, a round chip to tap; the one chosen is filled, and once known it is green or amber. */
@Composable
private fun MarkChip(mark: String, description: String, chosen: Boolean, result: Boolean?, enabled: Boolean, onClick: () -> Unit) {
    val colors = PolishEasyTheme.colors
    val scheme = MaterialTheme.colorScheme
    val (fill, ink) = when {
        result == true -> colors.success to colors.onSuccess
        result == false -> colors.caution to colors.onCaution
        chosen -> scheme.primary to scheme.onPrimary
        else -> scheme.surfaceContainerHigh to scheme.onSurface
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(Size.touch)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                selected = chosen
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Size.tonalButton)
                .background(fill, CircleShape)
                .border(Edge.border, if (chosen) fill else scheme.outlineVariant, CircleShape),
        ) {
            Text(mark, style = MaterialTheme.typography.titleMedium, color = ink)
        }
    }
}

/**
 * A line of a dialogue, to place in order or placed: its [number] once placed, the Polish, and,
 * once the answer is known, its tick or cross.
 */
@Composable
fun DialogueLine(text: String, number: Int?, mark: Mark?, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val colors = PolishEasyTheme.colors
    val (fill, edge) = when (mark) {
        Mark.Right -> colors.successContainer to colors.success
        Mark.Missed -> colors.cautionContainer to colors.caution
        null -> if (number != null) scheme.secondaryContainer to scheme.outlineVariant else scheme.surfaceContainerLowest to scheme.outlineVariant
    }
    Edged(
        fill = fill,
        edge = edge,
        corner = Corner.medium,
        border = Edge.border,
        enabled = enabled,
        onClick = onClick,
        contentAlignment = Alignment.CenterStart,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = Size.control).padding(horizontal = Space.m, vertical = Space.xs),
        ) {
            if (number != null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(Scaled.icon()).background(scheme.primary, CircleShape)) {
                    Text("$number", style = MaterialTheme.typography.labelMedium, color = scheme.onPrimary)
                }
                Spacer(Modifier.width(Space.s))
            }
            Text(text, style = MaterialTheme.typography.titleMedium.inPolish(), color = scheme.onSurface, modifier = Modifier.weight(1f))
            mark?.let {
                Spacer(Modifier.width(Space.xs))
                MarkIcon(it)
            }
        }
    }
}

/** A text to read, as the book prints a short one: a line to each turn, each with its speaker when it can be heard. */
@Composable
fun ReadingText(lines: List<String>, speakers: List<(@Composable () -> Unit)?>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(Space.xxs),
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerHigh, MaterialTheme.shapes.large)
            .padding(start = Space.m, end = Space.xs, top = Space.s, bottom = Space.s),
    ) {
        lines.forEachIndexed { index, line ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = Size.touch)) {
                Text(line, style = MaterialTheme.typography.titleMedium.inPolish(), color = scheme.onSurface, modifier = Modifier.weight(1f))
                speakers.getOrNull(index)?.invoke()
            }
        }
    }
}

/** A gender as the book marks it over a column, with its icon and what a screen reader says. */
class GenderMark(val icon: Int, val mark: String, val description: String)

/** One cell of a [GrammarTable]: its text and where the ending is, which goes in a red box. */
class GrammarCell(val text: String, val ending: IntRange?, val speaker: (@Composable () -> Unit)? = null)

/** One row: its head (a case's mark, a pronoun) and its question, then a cell for each column. */
class GrammarRow(val header: String?, val question: String?, val cells: List<GrammarCell>, val description: String)

/**
 * A grammar table as the book draws one: the [title], a row of column heads with a gender icon
 * over each where it has one, then the rows, each headed by its case and question, with the
 * endings in red boxes. A table of one column, a verb's forms, has a speaker on each form.
 */
@Composable
fun GrammarTable(title: String?, columns: List<Pair<String?, GenderMark?>>, rows: List<GrammarRow>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val headed = rows.any { it.header != null }
    Column(
        verticalArrangement = Arrangement.spacedBy(Space.xxs),
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.secondaryContainer, MaterialTheme.shapes.large)
            .border(Edge.hairline, scheme.outlineVariant, MaterialTheme.shapes.large)
            .padding(Space.m),
    ) {
        title?.let { Text(it, style = MaterialTheme.typography.titleLarge, color = scheme.onSecondaryContainer) }
        if (columns.any { it.first != null || it.second != null }) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().padding(top = Space.xs)) {
                if (headed) Spacer(Modifier.weight(HeadWeight))
                columns.forEach { (header, gender) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        gender?.let { GenderIcon(it) }
                        header?.let {
                            Spacer(Modifier.width(Space.xxs))
                            Text(it, style = MaterialTheme.typography.labelLarge.inPolish(), color = scheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        rows.forEach { row ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Size.touch)
                    .semantics(mergeDescendants = true) { contentDescription = row.description },
            ) {
                if (headed) {
                    Column(modifier = Modifier.weight(HeadWeight)) {
                        row.header?.let { Text(it, style = MaterialTheme.typography.titleSmall.inPolish(), color = scheme.primary) }
                        row.question?.let { Text(it, style = MaterialTheme.typography.labelSmall.inPolish(), color = scheme.onSurfaceVariant) }
                    }
                }
                row.cells.forEach { cell ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        EndingText(cell.text, cell.ending, Modifier.weight(1f))
                        cell.speaker?.invoke()
                    }
                }
            }
        }
    }
}

private const val HeadWeight = 0.8f

/**
 * A word with its ending in a red box, as the book prints endings. It is one line of text, the box
 * drawn around the ending where it falls, with a narrow space either side of it so the box
 * clears the letters; a word too wide for its column at a large font size is set smaller to fit.
 */
@Composable
private fun EndingText(text: String, ending: IntRange?, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.titleMedium.inPolish().copy(color = scheme.onSecondaryContainer)
    val boxed = ending != null && ending.first >= 0 && ending.last < text.length
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    // The ending with its spaces runs from [from] to [until] in the text shown.
    val from = if (boxed) ending!!.first else 0
    val until = if (boxed) ending!!.last + 3 else 0
    val shown = remember(text, ending, scheme.primary) {
        buildAnnotatedString {
            if (!boxed) {
                append(text)
                return@buildAnnotatedString
            }
            append(text.substring(0, from))
            append(NARROW_SPACE)
            withStyle(SpanStyle(color = scheme.primary)) { append(text.substring(ending!!)) }
            append(NARROW_SPACE)
            append(text.substring(ending!!.last + 1))
        }
    }
    val edge = Edge.border
    BasicText(
        shown,
        style = style,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = style.fontSize * 0.5f, maxFontSize = style.fontSize),
        onTextLayout = { layout = it },
        modifier = modifier
            .clearAndSetSemantics { this.text = AnnotatedString(text) }
            .drawBehind {
                val laid = layout ?: return@drawBehind
                if (!boxed) return@drawBehind
                val left = laid.getHorizontalPosition(from, usePrimaryDirection = true)
                val right = laid.getHorizontalPosition(until, usePrimaryDirection = true)
                drawRoundRect(
                    color = scheme.primary,
                    topLeft = Offset(left, laid.getLineTop(0)),
                    size = androidx.compose.ui.geometry.Size(right - left, laid.getLineBottom(0) - laid.getLineTop(0)),
                    cornerRadius = CornerRadius((Corner.extraSmall / 2).toPx()),
                    style = Stroke(edge.toPx()),
                )
            },
    )
}

/** Narrow enough to keep a word whole, wide enough to clear the box round its ending. */
private const val NARROW_SPACE = "\u202F"

/** The icon of a gender over a column, with its abbreviation beside it. */
@Composable
private fun GenderIcon(gender: GenderMark) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(scheme.primaryContainer, CircleShape)
            .padding(horizontal = Space.xs, vertical = Space.xxs)
            .clearAndSetSemantics { contentDescription = gender.description },
    ) {
        Icon(painterResource(gender.icon), contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(Scaled.icon(Size.icon * 0.75f)))
        Text(gender.mark, style = MaterialTheme.typography.labelMedium, color = scheme.onPrimaryContainer)
    }
}

/** What a callout is: *Uwaga!* in amber, a thing to watch for, or *Zapamiętaj!* in orange, a thing to remember. */
enum class CalloutTone { Uwaga, Zapamietaj }

/** One of the book's callouts, set apart on its own colour with its icon and its Polish label. */
@Composable
fun Callout(tone: CalloutTone, label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = PolishEasyTheme.colors
    val scheme = MaterialTheme.colorScheme
    val (fill, ink, icon) = when (tone) {
        CalloutTone.Uwaga -> Triple(colors.cautionContainer, colors.onCautionContainer, R.drawable.ic_priority_high)
        CalloutTone.Zapamietaj -> Triple(scheme.tertiaryContainer, scheme.onTertiaryContainer, R.drawable.ic_lightbulb)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(fill, MaterialTheme.shapes.large)
            .padding(Space.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(Scaled.icon()))
            Spacer(Modifier.width(Space.xs))
            Text(label, style = MaterialTheme.typography.titleLarge.inPolish(), color = ink)
        }
        Spacer(Modifier.heightIn(min = Space.xs))
        content()
    }
}
