package io.github.mrans2006.yaad.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.mrans2006.yaad.R

/** Pixelify Sans: ONLY for the logo and big headings. */
val PixelFont = FontFamily(Font(R.font.pixelify_sans_bold, FontWeight.Bold))

/** Space Grotesk: clean and bold, for everything you actually read. */
val BodyFont = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

object YaadType {
    val logo = TextStyle(fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = 1.sp)
    val hero = TextStyle(fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 42.sp)
    val heading = TextStyle(fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp)
    val title = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 23.sp)
    val body = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp)
    val bodyBold = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp)
    val small = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
    val label = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.3.sp)
    val button = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
}
