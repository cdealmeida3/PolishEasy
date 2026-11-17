package com.polisheasy.app.ui.screens.practice

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.polisheasy.app.ui.components.PictureArt
import com.polisheasy.engine.Picture

/**
 * What [picture] is drawn from. An image is read from the course packaged with the app, once, and
 * kept while there is room. Null if the image cannot be read, and then nothing is drawn.
 */
@Composable
internal fun artOf(picture: Picture): PictureArt? = when (picture) {
    is Picture.Glyph -> remember(picture) { PictureArt.Glyph(picture.text) }
    is Picture.Image -> {
        val assets = LocalContext.current.assets
        remember(picture) { PictureImages.read(assets, picture.path)?.let(PictureArt::Image) }
    }
}

/** The images already read, by their path. They are small, and a Game shows the same ones again and again. */
private object PictureImages {
    private val read = LruCache<String, ImageBitmap>(KEPT)

    fun read(assets: AssetManager, path: String): ImageBitmap? = read[path]
        ?: runCatching { assets.open(path).use(BitmapFactory::decodeStream)?.asImageBitmap() }.getOrNull()?.also { read.put(path, it) }

    private const val KEPT = 96
}
