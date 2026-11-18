package com.polisheasy.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The widest a clock face is drawn on its own. */
private val ClockLargest = 220.dp

/**
 * A clock face showing [hour] (1 to 12) and [minute]: the twelve hours marked, 12, 3, 6 and 9
 * written, the hour hand short and thick, the minute hand long. A screen reader hears
 * [description], the time in digits.
 */
@Composable
fun ClockFace(hour: Int, minute: Int, description: String, modifier: Modifier = Modifier, numbers: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    val face = scheme.surfaceContainerLowest
    val rim = scheme.outline
    val marks = scheme.onSurfaceVariant
    val hand = scheme.onSurface
    val long = scheme.primary
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.titleMedium.copy(color = marks)
    Canvas(
        modifier
            .aspectRatio(1f)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        val radius = size.minDimension / 2
        val centre = Offset(size.width / 2, size.height / 2)
        val rimWidth = radius * RIM
        drawCircle(face, radius - rimWidth / 2, centre)
        drawCircle(rim, radius - rimWidth / 2, centre, style = Stroke(rimWidth))
        fun at(turn: Double, length: Float) = Offset(
            centre.x + (sin(turn * 2 * PI) * length).toFloat(),
            centre.y - (cos(turn * 2 * PI) * length).toFloat(),
        )
        for (mark in 0 until HOURS) {
            val turn = mark / HOURS.toDouble()
            val quarter = mark % 3 == 0
            if (numbers && quarter) {
                val label = measurer.measure(if (mark == 0) "12" else "$mark", style)
                val spot = at(turn, radius * NUMBER_AT)
                drawText(label, topLeft = Offset(spot.x - label.size.width / 2, spot.y - label.size.height / 2))
            } else {
                drawLine(marks, at(turn, radius * (if (quarter) TICK_LONG else TICK_SHORT)), at(turn, radius * TICK_OUT), strokeWidth = radius * TICK_WIDTH, cap = StrokeCap.Round)
            }
        }
        val hourTurn = (hour % HOURS + minute / 60.0) / HOURS
        drawLine(hand, centre, at(hourTurn, radius * HOUR_HAND), strokeWidth = radius * HOUR_WIDTH, cap = StrokeCap.Round)
        drawLine(long, centre, at(minute / 60.0, radius * MINUTE_HAND), strokeWidth = radius * MINUTE_WIDTH, cap = StrokeCap.Round)
        drawCircle(hand, radius * HUB, centre)
    }
}

/** A clock face on its own, as large as it fits up to its widest, for the time asked. */
@Composable
fun ClockPrompt(hour: Int, minute: Int, description: String, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.Center, modifier = modifier.fillMaxWidth()) {
        ClockFace(hour, minute, description, Modifier.widthIn(max = ClockLargest).fillMaxWidth())
    }
}

/** A clock face to choose, [state] as an [OptionCard]'s: its [hour], [minute] and [description], the time in digits. */
class ClockOptionView(val hour: Int, val minute: Int, val description: String, val state: OptionState)

/**
 * Four clock faces to choose from, two across. Once answered the right one is marked with a tick,
 * the one tapped wrongly with a cross and one shake, and the others fade; none responds.
 */
@Composable
fun ClockOptions(options: List<ClockOptionView>, onChoose: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs), modifier = modifier.fillMaxWidth()) {
        options.withIndex().chunked(ACROSS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (index, option) -> ClockTile(option, { onChoose(index) }, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ClockTile(option: ClockOptionView, onClick: () -> Unit, modifier: Modifier) {
    val colors = PolishEasyTheme.colors
    val scheme = MaterialTheme.colorScheme
    val (fill, edge) = when (option.state) {
        OptionState.Right -> colors.successContainer to colors.success
        OptionState.Wrong -> colors.cautionContainer to colors.caution
        OptionState.Chosen -> scheme.primaryContainer to scheme.primary
        else -> scheme.surfaceContainerLowest to scheme.outlineVariant
    }
    Edged(
        fill = fill,
        edge = edge,
        corner = Corner.medium,
        border = Edge.border,
        enabled = option.state == OptionState.Idle,
        onClick = onClick,
        modifier = modifier
            .shaking(option.state == OptionState.Wrong)
            .alpha(if (option.state == OptionState.Faded) Motion.FADED_ALPHA else 1f)
            .semantics(mergeDescendants = true) {},
    ) {
        ClockFace(option.hour, option.minute, option.description, Modifier.fillMaxWidth().padding(Space.s), numbers = false)
        when (option.state) {
            OptionState.Right -> MarkIcon(Mark.Right, Modifier.align(Alignment.TopEnd).padding(Space.xxs), size = Size.tileMark)
            OptionState.Wrong -> MarkIcon(Mark.Missed, Modifier.align(Alignment.TopEnd).padding(Space.xxs), size = Size.tileMark)
            else -> Unit
        }
    }
}

private const val ACROSS = 2
private const val HOURS = 12
private const val RIM = 0.06f
private const val NUMBER_AT = 0.72f
private const val TICK_OUT = 0.86f
private const val TICK_LONG = 0.7f
private const val TICK_SHORT = 0.76f
private const val TICK_WIDTH = 0.035f
private const val HOUR_HAND = 0.5f
private const val HOUR_WIDTH = 0.08f
private const val MINUTE_HAND = 0.78f
private const val MINUTE_WIDTH = 0.05f
private const val HUB = 0.07f
