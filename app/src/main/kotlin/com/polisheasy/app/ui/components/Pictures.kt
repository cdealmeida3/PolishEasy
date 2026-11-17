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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Motion
import com.polisheasy.app.ui.theme.Scaled
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
 * pictures are the one place the app has shading. A glyph is at display size, in the brand's
 * colour unless the face it is on has a [glyphColor] of its own. [description] is what a screen reader says for it, or null when it should say nothing.
 */
@Composable
fun PictureFace(art: PictureArt, modifier: Modifier = Modifier, description: String? = null, glyphColor: Color = MaterialTheme.colorScheme.primary) {
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
            is PictureArt.Glyph -> Text(art.text, style = MaterialTheme.typography.displayMedium, color = glyphColor)
        }
    }
}

private const val PICTURE_SHARE = 0.62f

/**
 * A tile with two faces that turns over about its upright axis when tapped: [front] until it is
 * [turned], then [back]. The face changes at the half turn. With animations removed it changes at
 * once. [turnLabel] is what a screen reader calls the tap. Each face has its own look.
 */
@Composable
fun FlipTile(
    turned: Boolean,
    onTurn: () -> Unit,
    turnLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    frontLook: TileLook = TileLook.Plain,
    backLook: TileLook = TileLook.Brand,
    front: @Composable BoxScope.() -> Unit,
    back: @Composable BoxScope.() -> Unit,
) {
    val angle by animateFloatAsState(if (turned) HALF_TURN else 0f, Motion.flip(), label = "Flip")
    val showsBack = angle > HALF_TURN / 2
    val (fill, edge) = (if (showsBack) backLook else frontLook).colors()
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

/**
 * How a face of a [FlipTile] looks: plain as a card, in the brand's tint, as one of a pair that was
 * found, or as one of two that are not a pair.
 */
enum class TileLook { Plain, Brand, Found, Missed }

/** The fill and the edge of a look. */
@Composable
private fun TileLook.colors(): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val colors = PolishEasyTheme.colors
    return when (this) {
        TileLook.Plain -> scheme.surfaceContainerLowest to scheme.outlineVariant
        TileLook.Brand -> scheme.primaryContainer to scheme.primary
        TileLook.Found -> colors.successContainer to colors.success
        TileLook.Missed -> colors.cautionContainer to colors.caution
    }
}

/** The colour of words on a look. */
@Composable
private fun TileLook.content(): Color = when (this) {
    TileLook.Plain -> MaterialTheme.colorScheme.onSurface
    TileLook.Brand -> MaterialTheme.colorScheme.onPrimaryContainer
    TileLook.Found -> PolishEasyTheme.colors.onSuccessContainer
    TileLook.Missed -> PolishEasyTheme.colors.onCautionContainer
}

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

/** How a tile of Memory pairs lies: face down, turned up, one of two that are not a pair, or one of a pair found. */
enum class MemoryTileLook { Down, Up, Missed, Found }

/** What a turned tile of Memory pairs shows. */
sealed interface MemoryTileFace {
    /** A picture, or with no [art] what it shows in words. */
    class Picture(val art: PictureArt?, val meaning: String) : MemoryTileFace

    /** Polish, or with [polish] false words in the Base Language. */
    class Words(val text: String, val polish: Boolean) : MemoryTileFace

    /** Nothing to read: a speaker, for a tile that is only heard. */
    data object Sound : MemoryTileFace
}

/**
 * A tile of Memory pairs. Face down it shows a question mark on the brand's tint; turned up it
 * shows its [face] on a plain card, with a tick and the colour of a right answer once its pair is
 * found, or the colour of a wrong one for the moment two tiles are not a pair. [description] is
 * what a screen reader says for the tile where its face has no words of its own, and [state] how
 * it lies. With no [onTap] it does not respond.
 */
@Composable
fun MemoryTile(
    look: MemoryTileLook,
    face: MemoryTileFace,
    onTap: (() -> Unit)?,
    tapLabel: String,
    description: String?,
    state: String,
    modifier: Modifier = Modifier,
) {
    val backLook = when (look) {
        MemoryTileLook.Found -> TileLook.Found
        MemoryTileLook.Missed -> TileLook.Missed
        else -> TileLook.Plain
    }
    FlipTile(
        turned = look != MemoryTileLook.Down,
        onTurn = onTap ?: {},
        turnLabel = tapLabel,
        enabled = onTap != null,
        frontLook = TileLook.Brand,
        backLook = backLook,
        modifier = modifier.semantics {
            if (description != null) contentDescription = description
            stateDescription = state
        },
        front = {
            Text("?", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        },
        back = {
            val ink = backLook.content()
            when (face) {
                is MemoryTileFace.Picture ->
                    if (face.art == null) TileWords(face.meaning, polish = false, color = ink) else PictureFace(face.art, Modifier.fillMaxSize().padding(Space.xxs), glyphColor = ink)
                is MemoryTileFace.Words -> TileWords(face.text, face.polish, ink)
                MemoryTileFace.Sound -> Icon(
                    painterResource(R.drawable.ic_volume_up),
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(Scaled.icon(Size.playIcon)),
                )
            }
            if (look == MemoryTileLook.Found) {
                MarkIcon(Mark.Right, Modifier.align(Alignment.TopEnd).padding(Space.xxs), size = Size.tileMark)
            }
        },
    )
}

/** The words of a tile: Polish in its own weight, the Base Language plainer. A long word breaks rather than shrinks. */
@Composable
private fun TileWords(text: String, polish: Boolean, color: Color) {
    val type = MaterialTheme.typography
    Text(
        text,
        style = (if (polish) type.titleMedium.inPolish() else type.bodyLarge).copy(hyphens = Hyphens.Auto),
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Space.xs, vertical = Space.xs),
    )
}

/**
 * A picture to choose as an answer, in the states an [OptionCard] has: the right one gets a tick,
 * the one tapped wrongly a cross and one shake, and the others fade. [description] is what a
 * screen reader says for the picture. With no [art] it shows the description in words.
 */
@Composable
fun PictureOption(art: PictureArt?, description: String, state: OptionState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val look = when (state) {
        OptionState.Right -> TileLook.Found
        OptionState.Wrong -> TileLook.Missed
        OptionState.Chosen -> TileLook.Brand
        else -> TileLook.Plain
    }
    val (fill, edge) = look.colors()
    Edged(
        fill = fill,
        edge = edge,
        corner = Corner.large,
        border = Edge.border,
        enabled = state == OptionState.Idle || state == OptionState.Chosen,
        onClick = onClick,
        modifier = modifier
            .heightIn(min = Size.pictureOption)
            .shaking(state == OptionState.Wrong)
            .alpha(if (state == OptionState.Faded) Motion.FADED_ALPHA else 1f)
            .semantics { contentDescription = description },
    ) {
        if (art == null) {
            TileWords(description, polish = false, color = look.content())
        } else {
            PictureFace(art, Modifier.fillMaxWidth().padding(Space.xs), glyphColor = look.content())
        }
        when (state) {
            OptionState.Right -> MarkIcon(Mark.Right, Modifier.align(Alignment.TopEnd).padding(Space.xs))
            OptionState.Wrong -> MarkIcon(Mark.Missed, Modifier.align(Alignment.TopEnd).padding(Space.xs))
            else -> Unit
        }
    }
}
