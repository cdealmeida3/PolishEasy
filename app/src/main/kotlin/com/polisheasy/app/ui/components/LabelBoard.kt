package com.polisheasy.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Scaled
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish

/**
 * One picture of a [LabelBoard]: its [art], the [label] stuck under it once it is answered, how
 * it stands, and the whole of it in words for a screen reader.
 */
class LabelView(val art: PictureArt?, val label: String?, val state: FieldState, val description: String)

/**
 * A board of pictures to label, three across, or two when the text is large, each row as tall as
 * its tallest. Under each picture is its label: blank, "?" in the brand's tint while it is the one
 * asked, green once labelled right, and amber when put right. The one asked has the brand's edge.
 */
@Composable
fun LabelBoard(pictures: List<LabelView>, modifier: Modifier = Modifier) {
    val across = if (Scaled.roomy()) ROOMY_ACROSS else ACROSS
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs), modifier = modifier.fillMaxWidth()) {
        pictures.chunked(across).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                row.forEach { picture -> LabelledPicture(picture, Modifier.weight(1f).fillMaxHeight()) }
                if (row.size < across) Spacer(Modifier.weight((across - row.size).toFloat()))
            }
        }
    }
}

@Composable
private fun LabelledPicture(picture: LabelView, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val colors = PolishEasyTheme.colors
    val asking = picture.state == FieldState.Asking
    Surface(
        color = scheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(if (asking) Edge.border else Edge.hairline, if (asking) scheme.primary else scheme.outlineVariant),
        modifier = modifier.clearAndSetSemantics { contentDescription = picture.description },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(Space.xs)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                picture.art?.let { PictureFace(it) }
            }
            Spacer(Modifier.height(Space.xxs))
            val (fill, ink) = when (picture.state) {
                FieldState.Asking -> scheme.primaryContainer to scheme.onPrimaryContainer
                FieldState.Blank -> scheme.surfaceContainerHigh to scheme.onSurfaceVariant
                FieldState.Right -> colors.successContainer to colors.onSuccessContainer
                FieldState.Missed -> colors.cautionContainer to colors.onCautionContainer
            }
            Text(
                picture.label ?: if (asking) "?" else " ",
                style = MaterialTheme.typography.titleSmall.inPolish(),
                color = ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(fill, MaterialTheme.shapes.small)
                    .padding(horizontal = Space.xxs, vertical = Space.xxs),
            )
        }
    }
}

private const val ACROSS = 3
private const val ROOMY_ACROSS = 2
