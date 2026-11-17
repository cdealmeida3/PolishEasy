package com.polisheasy.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Scaled
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish

/** How a field of a [FormCard] stands: not filled in, being asked, filled in right, or put right after a wrong answer. */
enum class FieldState { Blank, Asking, Right, Missed }

/**
 * One field of a [FormCard]: its Polish [label], what it [meaning]s, what it is filled in with,
 * and the whole of it in words for a screen reader.
 */
class FieldView(val label: String, val meaning: String, val value: String?, val state: FieldState, val description: String)

/**
 * A paper form being filled in: a [title] and its fields, one a row, the label on the left and
 * the box to fill on the right, or the box under the label when the text is large. The field
 * being asked shows "?" in the brand's tint; one filled in right is green, one put right amber.
 */
@Composable
fun FormCard(title: String, fields: List<FieldView>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = scheme.surfaceContainerLowest,
        contentColor = scheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Edge.hairline, scheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Space.m), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.primary)
            val stacked = Scaled.roomy()
            fields.forEach { field -> Field(field, stacked) }
        }
    }
}

@Composable
private fun Field(field: FieldView, stacked: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val label = @Composable { modifier: Modifier ->
        Column(modifier = modifier) {
            Text(field.label, style = MaterialTheme.typography.labelLarge.inPolish(), color = scheme.onSurface)
            Text(field.meaning, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
    val semantics = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = field.description }
    if (stacked) {
        Column(modifier = semantics) {
            label(Modifier)
            Spacer(Modifier.height(Space.xxs))
            FieldBox(field, Modifier.fillMaxWidth())
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = semantics) {
            label(Modifier.weight(1f))
            FieldBox(field, Modifier.weight(BOX_WEIGHT))
        }
    }
}

@Composable
private fun FieldBox(field: FieldView, modifier: Modifier) {
    val colors = PolishEasyTheme.colors
    val scheme = MaterialTheme.colorScheme
    val (fill, ink) = when (field.state) {
        FieldState.Asking -> scheme.primaryContainer to scheme.onPrimaryContainer
        FieldState.Blank -> scheme.surfaceContainerHigh to scheme.onSurfaceVariant
        FieldState.Right -> colors.successContainer to colors.onSuccessContainer
        FieldState.Missed -> colors.cautionContainer to colors.onCautionContainer
    }
    Box(modifier = modifier.background(fill, MaterialTheme.shapes.small).padding(horizontal = Space.s, vertical = Space.xs)) {
        Text(
            field.value ?: if (field.state == FieldState.Asking) "?" else " ",
            style = MaterialTheme.typography.titleSmall.inPolish(),
            color = ink,
        )
    }
}

/**
 * Options as [OptionCard]s two across, each row as tall as its tallest, or one under the other
 * when the text is large. The Polish of each is set as Polish.
 */
@Composable
fun OptionGrid(options: List<String>, state: (Int) -> OptionState, onClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val across = if (Scaled.roomy()) 1 else ACROSS
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs), modifier = modifier.fillMaxWidth()) {
        options.indices.chunked(across).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                row.forEach { option ->
                    OptionCard(options[option], state(option), onClick = { onClick(option) }, polish = true, modifier = Modifier.weight(1f).fillMaxHeight())
                }
                if (row.size < across) Spacer(Modifier.weight((across - row.size).toFloat()))
            }
        }
    }
}

private const val ACROSS = 2
private const val BOX_WEIGHT = 1.2f
