package io.github.mrans2006.yaad.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * For the screenshot tests only: lets a test hand in ready-made images instead of loading from the gallery.
 * In the real app this map is empty and Coil loads the image.
 */
val LocalPreviewImages = staticCompositionLocalOf<Map<String, ImageBitmap>> { emptyMap() }

/** The memory-cache name of a screenshot's grid thumbnail. The detail screen shows it while the big image loads. */
fun gridKey(uri: String) = "grid:$uri"

/**
 * Shows a screenshot. Coil loads it in the background and decodes it at the size it takes on screen
 * (it waits for the layout's size), never at full resolution, so long grids scroll smoothly.
 *
 * @param cacheKey a fixed memory-cache name (used for grid thumbnails, see [gridKey]).
 * @param placeholderKey show this cached image until the real one is ready (detail screen: the grid thumbnail).
 */
@Composable
fun Thumbnail(
    uri: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.TopCenter,
    cacheKey: String? = null,
    placeholderKey: String? = null,
) {
    val preview = LocalPreviewImages.current[uri]
    if (preview != null) {
        Image(preview, null, modifier, contentScale = contentScale, alignment = alignment)
    } else {
        val context = LocalContext.current
        val request = remember(uri, cacheKey, placeholderKey) {
            ImageRequest.Builder(context)
                .data(uri)
                .apply { if (cacheKey != null) memoryCacheKey(cacheKey) }
                .apply { if (placeholderKey != null) placeholderMemoryCacheKey(placeholderKey) }
                .crossfade(false)
                .build()
        }
        AsyncImage(model = request, contentDescription = null, modifier = modifier, contentScale = contentScale, alignment = alignment)
    }
}
