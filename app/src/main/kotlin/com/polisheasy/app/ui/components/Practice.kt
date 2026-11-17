package com.polisheasy.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polisheasy.app.R
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Scaled
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish

/** The name of a group of Games on the Practice screen, or of one thing to choose before a Round. */
@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * What stands for a Game: its picture, [icon]. [locked] shows a padlock on a grey circle in its
 * place, so that "not open yet" is not said by colour alone.
 */
@Composable
fun GameBadge(@DrawableRes icon: Int, modifier: Modifier = Modifier, size: Dp = Size.gameIcon, locked: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    if (locked) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.size(size).background(scheme.surfaceContainerHighest, CircleShape)) {
            Icon(painterResource(R.drawable.ic_lock), contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(size / 2))
        }
    } else {
        Image(painterResource(icon), contentDescription = null, modifier = modifier.size(size))
    }
}

/**
 * A Game on the Practice screen: its [icon], its [name], one line of what it is, and under them
 * either its [best] or, when it cannot be played yet, the [reason] in words. The whole card is
 * tapped as one, and gives way like a button. [locked] when it cannot be played: it is flat, has
 * the padlock for its icon and does not respond, and a screen reader hears [lockedLabel].
 */
@Composable
fun GameCard(
    name: String,
    description: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    best: String? = null,
    locked: Boolean = false,
    lockedLabel: String = "",
    reason: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Edged(
        fill = scheme.surfaceContainerLowest,
        edge = scheme.outlineVariant,
        corner = Corner.large,
        border = Edge.border,
        flat = locked,
        enabled = !locked,
        onClick = onClick,
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            // The padlock says "locked" to the eye; a screen reader is told in words.
            .semantics { if (locked) stateDescription = lockedLabel },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Space.m),
        ) {
            GameBadge(icon, locked = locked)
            Spacer(Modifier.width(Space.m))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge, color = if (locked) scheme.onSurfaceVariant else scheme.onSurface)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                if (reason != null) {
                    Spacer(Modifier.height(Space.xxs))
                    Text(reason, style = MaterialTheme.typography.titleSmall, color = scheme.onSurfaceVariant)
                } else if (best != null) {
                    Spacer(Modifier.height(Space.xxs))
                    BestLine(best)
                }
            }
            if (!locked && !Scaled.roomy()) {
                Spacer(Modifier.width(Space.xs))
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(Size.icon),
                )
            }
        }
    }
}

/**
 * The Learner's Best in a Game: the star and [text], such as "Best · 14 right". With [fresh] it
 * has just been beaten: the words are larger and the star grows to its size once, as a tick does.
 */
@Composable
fun BestLine(text: String, modifier: Modifier = Modifier, fresh: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.semantics(mergeDescendants = true) {}) {
        Icon(
            painterResource(R.drawable.ic_star),
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier
                .size(Scaled.icon(if (fresh) Size.feedbackIcon else StarSmall))
                .then(if (fresh) Modifier.appearing() else Modifier),
        )
        Spacer(Modifier.width(if (fresh) Space.xs else Space.xxs))
        Text(
            text,
            style = if (fresh) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
            color = scheme.onSurface,
        )
    }
}

private val StarSmall = 20.dp

/**
 * One thing to choose before a Round, such as the verb: its [label] and its [options] as chips
 * that wrap. The one at [selected] is marked by its tick as well as its colour. An option at a
 * position in [polish] is Polish, and is said so to a screen reader.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceChips(
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    polish: Set<Int> = emptySet(),
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeading(label)
        Spacer(Modifier.height(Space.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            options.forEachIndexed { index, option ->
                ChoiceChip(option, chosen = index == selected, onClick = { onSelect(index) }, polish = index in polish)
            }
        }
    }
}

@Composable
private fun ChoiceChip(text: String, chosen: Boolean, onClick: () -> Unit, polish: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Edged(
        fill = if (chosen) scheme.primaryContainer else scheme.surfaceContainerLowest,
        edge = if (chosen) scheme.primary else scheme.outlineVariant,
        corner = Corner.small,
        border = Edge.border,
        onClick = onClick,
        modifier = Modifier.semantics {
            role = Role.RadioButton
            selected = chosen
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = Size.tile)
                .padding(horizontal = Space.m),
        ) {
            if (chosen) {
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(Scaled.icon(StarSmall)),
                )
                Spacer(Modifier.width(Space.xxs))
            }
            val style = MaterialTheme.typography.titleMedium
            Text(text, style = if (polish) style.inPolish() else style, color = if (chosen) scheme.onPrimaryContainer else scheme.onSurface)
        }
    }
}

/**
 * A screen opened from another, which Back goes back to: the back arrow at the top, the [content]
 * under it, which scrolls when it has to, and the [action] pinned to the bottom. It is never wider
 * than a phone's worth of content.
 */
@Composable
fun DetailFrame(
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // The arrow, not its touch area, lines up with the content below.
        Row(modifier = Modifier.widthIn(max = Size.contentMax).fillMaxWidth().padding(start = Space.s, top = Space.xs)) {
            IconButton(onClick = onBack, modifier = Modifier.size(Size.touch)) {
                Icon(
                    painterResource(R.drawable.ic_arrow_back),
                    contentDescription = backLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Scaled.icon()),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = Size.contentMax)
                .fillMaxWidth()
                .fadingBottom(more = scroll.canScrollForward)
                .verticalScroll(scroll)
                .padding(start = Space.screenSide, end = Space.screenSide, top = Space.xs, bottom = Space.m),
            content = content,
        )
        if (action != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(max = Size.contentMax)
                    .fillMaxWidth()
                    .padding(start = Space.screenSide, end = Space.screenSide, bottom = Space.screenBottom),
                content = action,
            )
        }
    }
}

/**
 * What every step of a Round sits in: the close button and the Round's progress at the top, which
 * stay where they are, and below them the [step] being shown, sliding as in a Daily Set. Closing
 * leaves the Round at once, with no question: a Round left unfinished is not kept.
 */
@Composable
fun <K> RoundFrame(
    progress: Float,
    progressDescription: String,
    closeLabel: String,
    onClose: () -> Unit,
    step: K,
    modifier: Modifier = Modifier,
    content: @Composable (step: K) -> Unit,
) {
    DailySetFrame(progress, progressDescription, closeLabel, onClose, step, modifier, content)
}

/**
 * One step of a Round inside a [RoundFrame], laid out as an Exercise is: what is asked from the
 * top as [content], what the Learner answers with from the bottom as [answer], and the [action]
 * pinned below, with [keyRow] under it when the answer is typed. A Game has no feedback panel, so
 * only the room of the action is kept, and the answers do not move when "Continue" appears.
 */
@Composable
fun RoundStep(
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    keyRow: (@Composable () -> Unit)? = null,
    answer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    DailySetStep(modifier = modifier, action = action, keyRow = keyRow, answer = answer, reserve = Size.roundActionRoom, content = content)
}

/** How an answer in a Game turned out, for the things that show it. */
enum class Mark { Right, Missed }

/**
 * What became of a typed answer, in words under the question: the tick or the cross and [text].
 * A screen reader hears it as soon as it appears.
 */
@Composable
fun RoundNote(mark: Mark, text: String, modifier: Modifier = Modifier, polish: Boolean = false) {
    val colors = PolishEasyTheme.colors
    val right = mark == Mark.Right
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(if (right) colors.successContainer else colors.cautionContainer, MaterialTheme.shapes.medium)
            .padding(horizontal = Space.m, vertical = Space.s)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        MarkIcon(mark)
        Spacer(Modifier.width(Space.s))
        val style = MaterialTheme.typography.titleMedium
        Text(
            text,
            style = if (polish) style.inPolish() else style,
            color = if (right) colors.onSuccessContainer else colors.onCautionContainer,
        )
    }
}

/** The tick of a right answer or the cross of a missed one, arriving with a little bounce. */
@Composable
internal fun MarkIcon(mark: Mark, modifier: Modifier = Modifier) {
    val colors = PolishEasyTheme.colors
    Icon(
        painterResource(if (mark == Mark.Right) R.drawable.ic_check_circle else R.drawable.ic_cancel),
        contentDescription = null,
        tint = if (mark == Mark.Right) colors.success else colors.caution,
        modifier = modifier
            .size(Scaled.icon())
            .appearing(),
    )
}

/** A row of the table the Learner fills in the Verb challenge. */
class VerbTableRowView(
    val pronouns: String,
    /** The form, once it is in the row. */
    val form: String?,
    /** Whether this is the row being asked for now. */
    val asked: Boolean,
    /** How the row was filled, once it is. */
    val mark: Mark?,
    /** What a screen reader says for the row. */
    val description: String,
)

/**
 * A verb's table being filled in, row by row: the pronouns, and beside them the form, an empty
 * slot, or the slot being asked for now, which is outlined in the brand's colour and says "?".
 * A filled row ends in its tick or cross.
 */
@Composable
fun VerbTable(title: String, rows: List<VerbTableRowView>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Edge.hairline, scheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Space.m), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            rows.forEach { row ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics { contentDescription = row.description },
                ) {
                    Text(
                        row.pronouns,
                        style = if (row.asked) MaterialTheme.typography.titleMedium.inPolish() else MaterialTheme.typography.bodyLarge.inPolish(),
                        color = if (row.asked) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        if (row.form != null) {
                            Text(row.form, style = MaterialTheme.typography.titleMedium.inPolish(), modifier = Modifier.weight(1f))
                            row.mark?.let { MarkIcon(it) }
                        } else {
                            TableSlot(asked = row.asked)
                        }
                    }
                }
            }
        }
    }
}

/** Where a form will go. The one being asked for is outlined in the brand's colour and says "?". */
@Composable
private fun TableSlot(asked: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SlotHeight)
            .then(
                if (asked) {
                    Modifier.background(scheme.primaryContainer, shape).border(Edge.border, scheme.primary, shape)
                } else {
                    Modifier.background(scheme.surfaceContainerHigh, shape).dashedOutline(Corner.small)
                },
            ),
    ) {
        if (asked) Text("?", style = MaterialTheme.typography.titleMedium, color = scheme.onPrimaryContainer)
    }
}

private val SlotHeight = 32.dp

/**
 * A Polish sentence with a gap, as a Game shows it: the words [before] and [after] the gap at
 * prompt size, and between them a slot. The slot is empty until the question is answered, then
 * holds [filled], coloured by its [mark].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GapSentenceText(before: String, after: String, modifier: Modifier = Modifier, filled: String? = null, mark: Mark? = null) {
    val scheme = MaterialTheme.colorScheme
    val colors = PolishEasyTheme.colors
    val style = MaterialTheme.typography.headlineMedium.inPolish()
    val (fill, line, ink) = when (mark) {
        Mark.Right -> Triple(colors.successContainer, colors.success, colors.onSuccessContainer)
        Mark.Missed -> Triple(colors.cautionContainer, colors.caution, colors.onCautionContainer)
        null -> Triple(scheme.secondaryContainer, scheme.outline, scheme.onSecondaryContainer)
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
        itemVerticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        before.split(' ').filter { it.isNotBlank() }.forEach { Text(it, style = style, color = scheme.onSurface) }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .widthIn(min = GapLeast)
                .background(fill, MaterialTheme.shapes.small)
                .border(Edge.border, line, MaterialTheme.shapes.small)
                .padding(horizontal = Space.s, vertical = Space.xxs),
        ) {
            // An empty gap keeps the height of a word, so the sentence does not move when it is filled.
            Text(filled ?: " ", style = style, color = ink)
        }
        after.split(' ').filter { it.isNotBlank() }.forEach { Text(it, style = style, color = scheme.onSurface) }
    }
}

private val GapLeast = 96.dp

/** Tiles laid out to wrap, centred, 8 apart: the tiles of a table, the cards of a chain. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TileBank(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Space.xs, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
        modifier = modifier.fillMaxWidth(),
    ) { content() }
}
