package io.github.mrans2006.yaad.brain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.github.mrans2006.yaad.core.OcrLine

/**
 * Reads the text in an image with Google ML Kit (OCR = Optical Character Recognition).
 * We use the BUNDLED model, which lives inside the APK, so it works with zero internet.
 */
class TextReader(private val context: Context) {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    /**
     * Returns the lines of text, top to bottom, with each line's height in pixels.
     * Must NOT be called on the main (UI) thread, because it waits for ML Kit to finish.
     */
    fun read(uri: Uri): List<OcrLine> {
        val bitmap = loadBitmap(uri) ?: throw IllegalStateException("Could not open image")
        try {
            val result = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)))
            // ML Kit groups lines into "blocks" (paragraphs). We keep the block number for each line.
            return result.textBlocks.flatMapIndexed { blockIndex, block ->
                block.lines.map { line ->
                    val box = line.boundingBox
                    OcrLine(line.text, box?.height()?.toFloat() ?: 0f, box?.top?.toFloat() ?: 0f, blockIndex)
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    /** Opens the image, shrinking very large ones so we don't run out of memory. */
    private fun loadBitmap(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        // Screenshots are ~1080 px wide. Only shrink images much wider than that,
        // so the text in long "scrolling screenshots" stays big enough to read.
        while (bounds.outWidth / sample > 2200 || bounds.outWidth.toLong() * bounds.outHeight / (sample * sample) > 24_000_000L) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }
}
