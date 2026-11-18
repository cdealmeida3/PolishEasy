package com.polisheasy.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish

/**
 * One person on a [FamilyTreeBoard]: its [key], where it stands ([column] from the left, half
 * steps allowed, and [row] from the top), its [art], the [label] written under it (none while
 * unknown), how it stands, whether it is the Learner ([me]), and the whole of it in words for a
 * screen reader.
 */
class TreeSpot(
    val key: String,
    val column: Float,
    val row: Int,
    val art: PictureArt?,
    val label: String?,
    val state: FieldState,
    val me: Boolean,
    val description: String,
)

/** A family on a [FamilyTreeBoard]: one or two [parents], by key, joined by a line, and a line down from them to each of their [children]. */
class TreeLink(val parents: List<String>, val children: List<String>)

/**
 * A family tree, [columns] across: each person a round picture with a label under it, and lines
 * joining couples and parents to children. A label is blank while the person is unknown, "?" in
 * the brand's tint while it is the one asked, green once named right and amber when put right;
 * the Learner has a dark label and edge. With [onTap] every person but the Learner can be tapped.
 */
@Composable
fun FamilyTreeBoard(spots: List<TreeSpot>, links: List<TreeLink>, columns: Int, onTap: ((String) -> Unit)?, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.outline
    // Where each picture's centre and its top and bottom fall, found when the spots are placed.
    var places by remember { mutableStateOf(emptyMap<String, Place>()) }
    val stroke = Edge.border
    Layout(
        content = { spots.forEach { spot -> Spot(spot, onTap?.takeIf { !spot.me }) } },
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val width = stroke.toPx()
                links.forEach { link ->
                    val parents = link.parents.mapNotNull(places::get)
                    val children = link.children.mapNotNull(places::get)
                    if (parents.isEmpty() || children.isEmpty()) return@forEach
                    val drop = if (parents.size == 2) {
                        drawLine(line, Offset(parents[0].x, parents[0].middle), Offset(parents[1].x, parents[1].middle), width)
                        Offset((parents[0].x + parents[1].x) / 2, parents[0].middle)
                    } else {
                        Offset(parents[0].x, parents[0].bottom)
                    }
                    val bar = (parents.maxOf { it.bottom } + children.minOf { it.top }) / 2
                    drawLine(line, drop, Offset(drop.x, bar), width)
                    val left = minOf(drop.x, children.minOf { it.x })
                    val right = maxOf(drop.x, children.maxOf { it.x })
                    if (right > left) drawLine(line, Offset(left, bar), Offset(right, bar), width)
                    children.forEach { child -> drawLine(line, Offset(child.x, bar), Offset(child.x, child.top), width) }
                }
            },
    ) { measurables, constraints ->
        val cell = constraints.maxWidth / columns
        val placeables = measurables.map { it.measure(Constraints.fixedWidth(cell)) }
        val gap = Space.l.roundToPx()
        val rows = spots.maxOf { it.row } + 1
        val heights = (0 until rows).map { row -> spots.indices.filter { spots[it].row == row }.maxOfOrNull { placeables[it].height } ?: 0 }
        val tops = heights.runningFold(0) { top, height -> top + height + gap }
        val inset = Space.xxs.toPx()
        val picture = PICTURE.toPx()
        places = spots.associate { spot ->
            val top = tops[spot.row] + inset
            spot.key to Place(x = (spot.column + 0.5f) * cell, top = top, middle = top + picture / 2, bottom = top + picture)
        }
        layout(constraints.maxWidth, tops[rows] - gap) {
            placeables.forEachIndexed { index, placeable ->
                val spot = spots[index]
                placeable.place((spot.column * cell).toInt(), tops[spot.row])
            }
        }
    }
}

private class Place(val x: Float, val top: Float, val middle: Float, val bottom: Float)

@Composable
private fun Spot(spot: TreeSpot, onTap: ((String) -> Unit)?) {
    val scheme = MaterialTheme.colorScheme
    val colors = PolishEasyTheme.colors
    val asking = spot.state == FieldState.Asking
    val (fill, ink) = when (spot.state) {
        FieldState.Asking -> scheme.primaryContainer to scheme.onPrimaryContainer
        FieldState.Blank -> if (spot.me) scheme.inverseSurface to scheme.inverseOnSurface else scheme.surfaceContainerHigh to scheme.onSurfaceVariant
        FieldState.Right -> colors.successContainer to colors.onSuccessContainer
        FieldState.Missed -> colors.cautionContainer to colors.onCautionContainer
    }
    val edge = when {
        asking -> scheme.primary
        spot.me -> scheme.onSurfaceVariant
        spot.state == FieldState.Right -> colors.success
        spot.state == FieldState.Missed -> colors.caution
        else -> scheme.outlineVariant
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.xxs),
        modifier = Modifier
            .testTag(TREE_SPOT_TAG + spot.key)
            .then(if (onTap != null) Modifier.clickable { onTap(spot.key) } else Modifier)
            .clearAndSetSemantics {
                contentDescription = spot.description
                if (onTap != null) {
                    role = Role.Button
                    onClick { onTap(spot.key); true }
                }
            }
            .padding(Space.xxs),
    ) {
        Surface(
            shape = CircleShape,
            color = scheme.surfaceContainerLowest,
            border = BorderStroke(if (spot.me || asking || spot.state != FieldState.Blank) Edge.border else Edge.hairline, edge),
            modifier = Modifier.size(PICTURE),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(Space.xxs)) {
                spot.art?.let { PictureFace(it) }
            }
        }
        // A word never wraps or is cut: it shrinks to fit its column, as five stand side by side.
        val style = MaterialTheme.typography.labelMedium.inPolish().copy(color = ink, textAlign = TextAlign.Center)
        BasicText(
            spot.label ?: if (asking) "?" else " ",
            style = style,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = style.fontSize * SMALLEST, maxFontSize = style.fontSize),
            modifier = Modifier
                .fillMaxWidth()
                .background(fill, MaterialTheme.shapes.small)
                .padding(vertical = Space.xxs),
        )
    }
}

/** What a person on the tree is tagged for tests: this and their key. */
const val TREE_SPOT_TAG = "tree-spot-"

/** The least a label shrinks to, of its size. */
private const val SMALLEST = 0.5f

/** The picture of each person: small, as five stand side by side. */
private val PICTURE = 52.dp
