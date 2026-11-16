package com.polisheasy.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as Area
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.polisheasy.app.R
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val RingSize = 184.dp
private val RingStroke = 16.dp
private val ConfettiSide = 10.dp

/**
 * The score of a Daily Set: a ring that fills to [fraction] while the number in it counts up.
 * The screen gives the score in words beside it.
 */
@Composable
fun ScoreRing(fraction: Float, modifier: Modifier = Modifier) {
    val target = fraction.coerceIn(0f, 1f)
    val spec = Motion.ring()
    val filled = remember { Animatable(if (spec == null) target else 0f) }
    if (spec != null) LaunchedEffect(target) { filled.animateTo(target, spec) }
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val fill = PolishEasyTheme.colors.success
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(RingSize)) {
        Canvas(Modifier.size(RingSize)) {
            val stroke = RingStroke.toPx()
            val inset = Offset(stroke / 2, stroke / 2)
            val area = Area(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = inset, size = area, style = Stroke(stroke))
            if (filled.value > 0f) {
                drawArc(
                    fill,
                    startAngle = -90f,
                    sweepAngle = 360f * filled.value,
                    useCenter = false,
                    topLeft = inset,
                    size = area,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            "${(filled.value * 100).roundToInt()}%",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * The Streak going up, on the Summary: the flame, and the number rolling from [from] to [to] once
 * the score ring has filled. [title] and [note] are the words beside the number, and [description]
 * is the whole of it in words for a screen reader.
 */
@Composable
fun StreakCard(from: Int, to: Int, title: String, note: String, description: String, modifier: Modifier = Modifier) {
    val reduced = Motion.reduced()
    var shown by remember { mutableIntStateOf(if (reduced) to else from) }
    val flame = remember { Animatable(1f) }
    val grow = Motion.flame()
    LaunchedEffect(to) {
        if (!reduced) {
            delay(Motion.RING_MILLIS.toLong())
            shown = to
            if (grow != null) {
                flame.snapTo(Motion.FLAME_FROM_SCALE)
                flame.animateTo(1f, grow)
            }
        }
    }
    val streak = PolishEasyTheme.colors.streak
    ContentCard(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_flame),
                contentDescription = null,
                tint = streak,
                modifier = Modifier
                    .size(Size.flame)
                    .graphicsLayer {
                        scaleX = flame.value
                        scaleY = flame.value
                    },
            )
            Spacer(Modifier.width(Space.xs))
            val roll = Motion.roll()
            AnimatedContent(shown, transitionSpec = { roll }, label = "streak") { number ->
                Text(number.toString(), style = MaterialTheme.typography.displayMedium, color = streak)
            }
            Spacer(Modifier.width(Space.m))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** One Item the Daily Set taught: the Polish, and under it what it means. */
@Composable
fun ItemCard(polish: String, meaning: String, modifier: Modifier = Modifier) {
    ContentCard(modifier = modifier) {
        Text(polish, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(meaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Confetti falling once over whatever it is laid on, after the score ring has filled. It is drawn
 * here, with no library and no image, cannot be tapped and is gone when it has fallen. With
 * animations removed there is none.
 */
@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val spec = Motion.confetti() ?: return
    val fall = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fall.animateTo(1f, spec) }
    if (fall.value >= 1f) return
    val colors = PolishEasyTheme.colors
    val palette = listOf(colors.brand, colors.success, colors.caution, colors.streak)
    val pieces = remember { List(CONFETTI_PIECES) { ConfettiPiece(Random(it)) } }
    Canvas(modifier.testTag(CONFETTI_TAG)) {
        val side = ConfettiSide.toPx()
        val time = fall.value
        pieces.forEachIndexed { index, piece ->
            // Each piece starts a little after the one before and falls at its own speed.
            val along = ((time - piece.start) * piece.speed).coerceAtLeast(0f)
            if (along <= 0f || along >= 1f) return@forEachIndexed
            val x = piece.x * size.width + sin(along * piece.swings * 2 * PI.toFloat()) * side * 2
            val y = -side + along * (size.height + 2 * side)
            rotate(degrees = piece.spin * along, pivot = Offset(x, y)) {
                drawRect(
                    palette[index % palette.size],
                    topLeft = Offset(x - side / 2, y - side * piece.length / 2),
                    size = Area(side, side * piece.length),
                    // Fades as it reaches the bottom, so nothing stops dead above the main button.
                    alpha = ((1f - along) * 4f).coerceAtMost(1f),
                )
            }
        }
    }
}

/** The tag a test finds the confetti by. */
const val CONFETTI_TAG = "confetti"

private const val CONFETTI_PIECES = 70

private class ConfettiPiece(random: Random) {
    val x = random.nextFloat()
    val start = random.nextFloat() * 0.35f
    val speed = 1.6f + random.nextFloat() * 0.9f
    val swings = 1f + random.nextFloat() * 2f
    val spin = (random.nextFloat() - 0.5f) * 1080f
    val length = 0.5f + random.nextFloat()
}
