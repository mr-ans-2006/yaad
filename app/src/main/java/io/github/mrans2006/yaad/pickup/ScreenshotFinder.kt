package io.github.mrans2006.yaad.pickup

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore

/**
 * Asks Android's media library (MediaStore) for every image in a "Screenshots" folder.
 *
 * Phones keep screenshots in different places:
 *   - Oppo / ColorOS:  DCIM/Screenshots  or  Pictures/Screenshots
 *   - Pixel, Samsung:  Pictures/Screenshots  or  DCIM/Screenshots
 * So we simply look for any folder whose name contains "screenshot".
 */
class ScreenshotFinder(private val context: Context) {

    data class Found(val mediaId: Long, val uri: String, val name: String, val takenAt: Long, val addedAtSec: Long, val width: Int, val height: Int)

    fun findAll(): List<Found> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
        )
        val selection: String
        val args: Array<String>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: folders are described by RELATIVE_PATH ("DCIM/Screenshots/").
            selection = "(${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} LIKE ?)"
            args = arrayOf("%screenshot%", "%screenshot%")
        } else {
            // Android 8 and 9: use the full file path.
            @Suppress("DEPRECATION")
            selection = "${MediaStore.Images.Media.DATA} LIKE ?"
            args = arrayOf("%/screenshot%")
        }

        val out = mutableListOf<Found>()
        context.contentResolver.query(
            collection, projection.toTypedArray(), selection, args, "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val takenCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val wCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val hCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val added = c.getLong(addedCol)
                val taken = c.getLong(takenCol).takeIf { it > 0 } ?: (added * 1000)
                out += Found(
                    mediaId = id,
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    name = c.getString(nameCol) ?: "",
                    takenAt = taken,
                    addedAtSec = added,
                    width = c.getInt(wCol),
                    height = c.getInt(hCol),
                )
            }
        } ?: throw IllegalStateException("Gallery not available")
        return out
    }
}
