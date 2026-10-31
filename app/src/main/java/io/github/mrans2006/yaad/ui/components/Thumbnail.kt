package io.github.mrans2006.yaad.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/**
 * For the screenshot tests only: lets a test hand in ready-made images instead of loading from the gallery.
 * In the real app this map is empty and Coil loads the image.
 */
val LocalPreviewImages = staticCompositionLocalOf<Map<String, ImageBitmap>> { emptyMap() }

/** Shows a screenshot. Coil loads it in the background and shrinks it to the size on screen. */
@Composable
fun Thumbnail(uri: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop, alignment: Alignment = Alignment.TopCenter) {
    val preview = LocalPreviewImages.current[uri]
    if (preview != null) {
        Image(preview, null, modifier, contentScale = contentScale, alignment = alignment)
    } else {
        AsyncImage(model = uri, contentDescription = null, modifier = modifier, contentScale = contentScale, alignment = alignment)
    }
}
