package com.polisheasy.app.ui.components

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as Area
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
import com.polisheasy.app.ui.theme.inPolish
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.style.TextAlign

private val ConfettiSide = 10.dp

/**
 * The Streak going up, the hero of the Summary of the Daily Set that raised it: a large flame, the
 * number rolling from [from] to [to], and the week's [days] with today's dot filling in. [title]
 * and [note] are the words under the number, and [description] is the whole of it in words for a
 * screen reader.
 */
@Composable
fun StreakHero(from: Int, to: Int, title: String, note: String, days: List<WeekDay>, description: String, modifier: Modifier = Modifier) {
    val flame = remember { Animatable(1f) }
    val grow = Motion.flame()
    val streak = PolishEasyTheme.colors.streak
    ContentCard(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_flame),
                    contentDescription = null,
                    tint = streak,
                    modifier = Modifier
                        .size(Size.flameHero)
                        .graphicsLayer {
                            scaleX = flame.value
                            scaleY = flame.value
                        },
                )
                Spacer(Modifier.width(Space.xs))
                RollingNumber(from, to, MaterialTheme.typography.displayLarge, streak) {
                    if (grow != null) {
                        flame.snapTo(Motion.FLAME_FROM_SCALE)
                        flame.animateTo(1f, grow)
                    }
                }
            }
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                note,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Space.m))
            WeekRow(days, raised = true)
        }
    }
}

/**
 * One Item the Daily Set taught: the Polish, and under it what it means, with the [speaker] that
 * says it at the end when it can be heard.
 */
@Composable
fun ItemCard(polish: String, meaning: String, modifier: Modifier = Modifier, speaker: (@Composable () -> Unit)? = null) {
    ContentCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(polish, style = MaterialTheme.typography.titleMedium.inPolish(), color = MaterialTheme.colorScheme.onSurface)
                Text(meaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            speaker?.invoke()
        }
    }
}

/**
 * Confetti falling once over whatever it is laid on, a moment after the cheer. It is drawn here,
 * with no library and no image, cannot be tapped and is gone when it has fallen. With animations
 * removed there is none. [plenty] doubles it, for a Streak that has reached a milestone.
 */
@Composable
fun Confetti(modifier: Modifier = Modifier, plenty: Boolean = false) {
    val spec = Motion.confetti() ?: return
    val fall = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fall.animateTo(1f, spec) }
    if (fall.value >= 1f) return
    val colors = PolishEasyTheme.colors
    val palette = listOf(colors.brand, colors.success, colors.caution, colors.streak)
    val pieces = remember { List(if (plenty) CONFETTI_PIECES * 2 else CONFETTI_PIECES) { ConfettiPiece(Random(it)) } }
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
