package io.github.mrans2006.yaad.core

/**
 * One line of text that OCR found.
 * @param height how tall the text was on screen, in pixels. Taller text is usually a heading.
 * @param top    how far from the top of the image the line starts, in pixels.
 * @param block  which paragraph ("block") the line belongs to, or -1 if unknown.
 *               OCR tells us this, so a heading that wraps onto two lines can be glued back together.
 */
data class OcrLine(val text: String, val height: Float = 0f, val top: Float = 0f, val block: Int = -1)
