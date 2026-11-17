package com.polisheasy.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish

/** What a picture is drawn from: an image, or a numeral or sign set in the app's own typeface. */
sealed interface PictureArt {
    class Image(val bitmap: ImageBitmap) : PictureArt

    class Glyph(val text: String) : PictureArt
}

/**
 * A picture, filling [PICTURE_SHARE] of the width it is given. An image is drawn as it is: the
 * pictures are the one place the app has shading. A glyph is in the brand's colour at display
 * size. [description] is what a screen reader says for it, or null when it should say nothing.
 */
@Composable
fun PictureFace(art: PictureArt, modifier: Modifier = Modifier, description: String? = null) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.then(if (description == null) Modifier else Modifier.semantics { contentDescription = description }),
    ) {
        when (art) {
            is PictureArt.Image -> Image(
                art.bitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(PICTURE_SHARE)
                    .aspectRatio(1f),
            )
            is PictureArt.Glyph -> Text(art.text, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private const val PICTURE_SHARE = 0.62f

/**
 * A tile with two faces that turns over about its upright axis when tapped: [front] until it is
 * [turned], then [back]. The face changes at the half turn. With animations removed it changes at
 * once. [turnLabel] is what a screen reader calls the tap.
 */
@Composable
fun FlipTile(
    turned: Boolean,
    onTurn: () -> Unit,
    turnLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    look: TileLook = TileLook.Plain,
    front: @Composable BoxScope.() -> Unit,
    back: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = com.polisheasy.app.ui.PolishEasyTheme.colors
    val angle by animateFloatAsState(if (turned) HALF_TURN else 0f, Motion.flip(), label = "Flip")
    val showsBack = angle > HALF_TURN / 2
    val (fill, edge) = when {
        !showsBack -> scheme.surfaceContainerLowest to scheme.outlineVariant
        look == TileLook.Found -> colors.successContainer to colors.success
        else -> scheme.primaryContainer to scheme.primary
    }
    Edged(
        fill = fill,
        edge = edge,
        corner = Corner.large,
        border = Edge.border,
        enabled = enabled,
        onClick = onTurn,
        onClickLabel = turnLabel,
        modifier = modifier.graphicsLayer {
            rotationY = angle
            cameraDistance = CAMERA_DISTANCE * density
        },
    ) {
        if (showsBack) {
            // Turned back the right way round, or it would read mirrored.
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = HALF_TURN }, content = back)
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize(), content = front)
        }
    }
}

/** How the turned face of a [FlipTile] looks: as any turned tile, or as one of a pair that was found. */
enum class TileLook { Plain, Found }

private const val HALF_TURN = 180f
private const val CAMERA_DISTANCE = 12f

/**
 * A Picture card: the picture, or with no [art] the [meaning] in words, and turned over, the
 * [polish] in large type, what it means, and the [speaker] that says it when there is one. It is
 * at least [Size.pictureTile] high and grows with its words; a long phrase wraps and is never shrunk.
 */
@Composable
fun PictureTile(
    art: PictureArt?,
    polish: String,
    meaning: String,
    turned: Boolean,
    onTurn: () -> Unit,
    turnLabel: String,
    modifier: Modifier = Modifier,
    speaker: (@Composable () -> Unit)? = null,
) {
    FlipTile(
        turned = turned,
        onTurn = onTurn,
        turnLabel = turnLabel,
        modifier = modifier.heightIn(min = Size.pictureTile),
        front = {
            if (art == null) {
                Text(
                    meaning,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(Space.s),
                )
            } else {
                PictureFace(art, description = meaning, modifier = Modifier.fillMaxWidth().padding(Space.s))
            }
        },
        back = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(Space.s),
            ) {
                Text(
                    polish,
                    style = MaterialTheme.typography.titleLarge.inPolish(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                )
                Text(
                    meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                )
                speaker?.invoke()
            }
        },
    )
}
