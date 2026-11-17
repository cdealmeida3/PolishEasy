package com.polisheasy.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size as GeometrySize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.polisheasy.app.R
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.AccentColors
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish

// The pieces of the book's look: a chapter's banner and card in its own colour, its K, L and G,
// its sections, its words and its Powtórzenie.

/** The lesson colour of the chapter with accent number [accent]. */
@Composable
fun accentColors(accent: Int): AccentColors {
    val accents = PolishEasyTheme.colors.accents
    return accents[accent.mod(accents.size)]
}

/** The number disc of a chapter: its [number] on its colour. A Starter Unit's place stands in for a number. */
@Composable
private fun NumberDisc(number: String, accent: AccentColors, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(LessonDisc)
            .background(accent.fill, RoundedCornerShape(Corner.medium)),
    ) {
        Text(number, style = MaterialTheme.typography.titleLarge, color = accent.onFill, textAlign = TextAlign.Center)
    }
}

/**
 * A chapter on the Path: its number in its colour, its [title] (Polish for the book) and the
 * English [subtitle] under it, and [progress] in words. A [sealed] one has passed its Test; a
 * [done] one has every section completed; a [coming] one has no content yet and is quieter.
 * The [current] one, where the Learner is, carries [currentLabel].
 */
@Composable
fun LessonCard(
    number: String,
    title: String,
    subtitle: String?,
    progress: String,
    accent: Int,
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
    done: Boolean = false,
    sealed: Boolean = false,
    sealedLabel: String? = null,
    coming: Boolean = false,
    currentLabel: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = accentColors(accent)
    Surface(
        color = if (currentLabel != null) colors.container else scheme.surfaceContainerLowest,
        contentColor = if (currentLabel != null) colors.onContainer else scheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Edge.hairline, scheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(Space.s)) {
            NumberDisc(number, colors)
            Spacer(Modifier.width(Space.s))
            Column(modifier = Modifier.weight(1f)) {
                if (currentLabel != null) {
                    Text(currentLabel, style = MaterialTheme.typography.labelSmall, color = colors.onContainer)
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.inPolish(),
                    color = if (coming) scheme.onSurfaceVariant else if (currentLabel != null) colors.onContainer else scheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = if (currentLabel != null) colors.onContainer else scheme.onSurfaceVariant)
                }
                Text(progress, style = MaterialTheme.typography.labelSmall, color = if (currentLabel != null) colors.onContainer else scheme.onSurfaceVariant)
            }
            if (sealed) {
                Spacer(Modifier.width(Space.xs))
                Icon(painterResource(R.drawable.ic_verified), contentDescription = sealedLabel, tint = colors.onContainer, modifier = Modifier.size(Size.icon))
            } else if (done) {
                Spacer(Modifier.width(Space.xs))
                Icon(painterResource(R.drawable.ic_check_circle), contentDescription = null, tint = PolishEasyTheme.colors.success, modifier = Modifier.size(Size.icon))
            }
            Spacer(Modifier.width(Space.xs))
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(Size.icon))
        }
    }
}

/**
 * The top of a Lesson screen, like the book's opening page: the chapter's colour, a [label] such
 * as "Lekcja 5", the big [number], the [title] and the English [subtitle].
 */
@Composable
fun LessonBanner(label: String, number: String, title: String, subtitle: String?, accent: Int, modifier: Modifier = Modifier) {
    val colors = accentColors(accent)
    Surface(color = colors.fill, contentColor = colors.onFill, shape = MaterialTheme.shapes.extraLarge, modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(Space.l)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = colors.onFill)
                Spacer(Modifier.height(Space.xxs))
                Text(title, style = MaterialTheme.typography.headlineMedium.inPolish(), color = colors.onFill, modifier = Modifier.semantics { heading() })
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = colors.onFill)
                }
            }
            Spacer(Modifier.width(Space.s))
            Text(number, style = MaterialTheme.typography.displayMedium, color = colors.onFill, modifier = Modifier.clearAndSetSemantics { })
        }
    }
}

/**
 * One strand of a chapter, K, L or G as the book marks them: its [mark] in a square, its [name],
 * what it covers in [topics], and a band that fills as [done] of its [total] sections finish.
 * A screen reader hears [description].
 */
@Composable
fun StrandChip(mark: String, name: String, topics: String, done: Int, total: Int, accent: Int, description: String, modifier: Modifier = Modifier) {
    val colors = accentColors(accent)
    val share = if (total == 0) 0f else done.toFloat() / total
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(Edge.hairline, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .drawBehind { drawRect(colors.container, size = GeometrySize(size.width * share, size.height)) }
                .padding(Space.s),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(Size.icon + Space.xs).background(colors.fill, RoundedCornerShape(Corner.extraSmall))) {
                Text(mark, style = MaterialTheme.typography.titleSmall, color = colors.onFill)
            }
            Spacer(Modifier.width(Space.s))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(topics, style = MaterialTheme.typography.bodyMedium.inPolish(), color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(Space.xs))
            // A chapter still coming has no sections to count yet.
            if (total > 0) Text("$done/$total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** A row of chips that wraps, for the words of "Sprawdź w słowniku". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs), modifier = modifier.fillMaxWidth()) {
        content()
    }
}

/**
 * A word to look up: its Polish [text] on a chip that says it when tapped and then shows its
 * [meaning] under it, while [open].
 */
@Composable
fun WordChip(text: String, meaning: String, open: Boolean, onClick: () -> Unit, onClickLabel: String, accent: Int, modifier: Modifier = Modifier) {
    val colors = accentColors(accent)
    Surface(
        color = colors.container,
        contentColor = colors.onContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .heightIn(min = Size.touch)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(horizontal = Space.s, vertical = Space.xs)) {
            Text(text, style = MaterialTheme.typography.labelLarge.inPolish(), color = colors.onContainer)
            AnimatedVisibility(open) {
                Text(meaning, style = MaterialTheme.typography.bodySmall, color = colors.onContainer, modifier = Modifier.widthIn(max = WordChipWidest))
            }
        }
    }
}

/**
 * One of the Powtórzenie tools on a Lesson screen, Słownik, Fiszki or Test: its [icon], its
 * Polish [title], and a [note] such as how many words or the Best. With [sealed] it wears the seal
 * in the chapter's colour in place of its icon.
 */
@Composable
fun ToolTile(
    @DrawableRes icon: Int,
    title: String,
    note: String,
    accent: Int,
    onClick: (() -> Unit)?,
    onClickLabel: String,
    modifier: Modifier = Modifier,
    sealed: Boolean = false,
    sealedLabel: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = accentColors(accent)
    val tap = if (onClick == null) Modifier else Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
    Surface(
        color = scheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Edge.hairline, scheme.outlineVariant),
        modifier = modifier.clip(MaterialTheme.shapes.large).then(tap),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = Space.s, horizontal = Space.xs)) {
            // Once the Test is passed its tile wears the seal in place of its icon.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(Size.touch).background(if (sealed) colors.fill else colors.container, CircleShape),
            ) {
                Icon(
                    painterResource(if (sealed) R.drawable.ic_verified else icon),
                    contentDescription = if (sealed) sealedLabel else null,
                    tint = if (sealed) colors.onFill else colors.onContainer,
                    modifier = Modifier.size(Size.icon),
                )
            }
            Spacer(Modifier.height(Space.xs))
            Text(title, style = MaterialTheme.typography.titleSmall.inPolish(), color = scheme.onSurface, textAlign = TextAlign.Center)
            Text(note, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

/**
 * The book's register mark on a phrase: O (formal), N (informal) or U (both), in a small square,
 * with its [label] beside it when there is room for one. A screen reader hears [description].
 */
@Composable
fun RegisterChip(mark: String, description: String, modifier: Modifier = Modifier, label: String? = null) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(scheme.secondaryContainer, RoundedCornerShape(Corner.extraSmall))
            .padding(end = if (label != null) Space.xs else 0.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(Size.icon + Space.xxs)) {
            Text(mark, style = MaterialTheme.typography.titleSmall, color = scheme.onSecondaryContainer)
        }
        if (label != null) Text(label, style = MaterialTheme.typography.labelMedium, color = scheme.onSecondaryContainer)
    }
}

/**
 * An Exercise's instruction as the book gives it, in Polish: [polish], with a button that shows
 * the [english] under it while [translated], and a [speaker] to hear it. [translateLabel] says
 * what the button does next.
 */
@Composable
fun PolishInstruction(
    polish: String,
    english: String,
    translated: Boolean,
    translateLabel: String,
    onTranslate: () -> Unit,
    modifier: Modifier = Modifier,
    speaker: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(polish, style = MaterialTheme.typography.titleSmall.inPolish(), color = scheme.onSurface)
            AnimatedVisibility(translated) {
                Text(english, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
        }
        speaker?.invoke()
        IconButton(onClick = onTranslate, modifier = Modifier.size(Size.touch)) {
            Icon(
                painterResource(R.drawable.ic_translate),
                contentDescription = translateLabel,
                tint = if (translated) scheme.primary else scheme.onSurfaceVariant,
                modifier = Modifier.size(Size.icon),
            )
        }
    }
}

/** A field to find something by, with its [label] in it until something is typed. The keyboard waits for a tap. */
@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null, modifier = Modifier.size(Size.icon)) },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        textStyle = MaterialTheme.typography.bodyLarge.inPolish(),
        modifier = modifier.fillMaxWidth().semantics { contentDescription = label },
    )
}

/**
 * One word of the Słownik: the Polish [text], its [meaning], its register when the book marks
 * one, and a [speaker].
 */
@Composable
fun DictionaryRow(text: String, meaning: String, modifier: Modifier = Modifier, register: (@Composable () -> Unit)? = null, speaker: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().heightIn(min = Size.touch).padding(vertical = Space.xxs)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.titleMedium.inPolish(), color = MaterialTheme.colorScheme.onSurface)
            Text(meaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (register != null) {
            Spacer(Modifier.width(Space.xs))
            register()
        }
        if (speaker != null) speaker()
    }
}

/**
 * A flashcard: the Polish [front] on the chapter's colour until it is [flipped], then the [back],
 * its meaning. Tapping it turns it. [register] and [speaker] sit at its foot.
 */
@Composable
fun Flashcard(
    front: String,
    back: String,
    flipped: Boolean,
    onFlip: () -> Unit,
    flipLabel: String,
    accent: Int,
    modifier: Modifier = Modifier,
    register: (@Composable () -> Unit)? = null,
    speaker: (@Composable () -> Unit)? = null,
) {
    val colors = accentColors(accent)
    val scheme = MaterialTheme.colorScheme
    val turn = animateFloatAsState(if (flipped) 180f else 0f, animationSpec = Motion.flip(), label = "Flip")
    Surface(
        color = if (turn.value <= 90f) colors.container else scheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(Edge.border, colors.fill),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = FlashcardHeight)
            .turned(turn.value)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClickLabel = flipLabel, role = Role.Button, onClick = onFlip),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(Space.l).turned(if (turn.value > 90f) 180f else 0f),
        ) {
            Spacer(Modifier.height(Space.l))
            if (turn.value <= 90f) {
                Text(front, style = MaterialTheme.typography.displaySmall.inPolish(), color = colors.onContainer, textAlign = TextAlign.Center)
            } else {
                Text(front, style = MaterialTheme.typography.titleMedium.inPolish(), color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Space.xs))
                Text(back, style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(Space.m))
            Row(verticalAlignment = Alignment.CenterVertically) {
                register?.invoke()
                speaker?.invoke()
            }
        }
    }
}

private fun Modifier.turned(degrees: Float): Modifier = graphicsLayer {
    rotationY = degrees
    cameraDistance = CardDistance * density
}

private const val CardDistance = 12f

/**
 * A Test's seal: [value] in percent in a big disc, in the chapter's colour when [lit], which is
 * when the Test passed, with the seal's mark above it; grey until then. A screen reader hears
 * [description].
 */
@Composable
fun Seal(value: String, accent: Int, lit: Boolean, description: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val colors = accentColors(accent)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(SealSize)
            .background(if (lit) colors.fill else scheme.surfaceContainerHighest, CircleShape)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (lit) Icon(painterResource(R.drawable.ic_verified), contentDescription = null, tint = colors.onFill, modifier = Modifier.size(Size.icon))
            Text("$value%", style = MaterialTheme.typography.headlineLarge, color = if (lit) colors.onFill else scheme.onSurfaceVariant)
        }
    }
}

private val SealSize = 128.dp
private val LessonDisc = 48.dp
private val WordChipWidest = 200.dp
private val FlashcardHeight = 260.dp
