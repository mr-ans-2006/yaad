package io.github.mrans2006.yaad.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.mrans2006.yaad.R

/** Inter: one clean font for everything (static weights cut from Inter 4.0, OFL). */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

/** Inter has no Arabic letters, so the small یاد wordmark uses Noto Sans Arabic SemiBold (only those 3 letters are bundled). */
val UrduFont = FontFamily(Font(R.font.noto_sans_arabic_semibold, FontWeight.SemiBold))

private val tight = (-0.011).em

object YaadType {
    val logo = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, letterSpacing = (-0.04).em)
    val urdu = TextStyle(fontFamily = UrduFont, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
    val hero = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = (-0.035).em)
    val heading = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.03).em)
    val title = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.02).em)
    val cardTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, lineHeight = 21.sp, letterSpacing = tight)
    val body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.5.sp, lineHeight = 21.sp, letterSpacing = tight)
    val bodyBold = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = tight)
    val key = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp, letterSpacing = tight)
    val search = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.5.sp, letterSpacing = tight)
    val small = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = tight)
    val tag = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, letterSpacing = tight)
    val tagSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, lineHeight = 15.sp, letterSpacing = tight)
    /** Small caps-style section labels: "KEY LINE", "9 SCREENSHOTS REMEMBERED". */
    val label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, letterSpacing = 0.07.em)
    val button = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, letterSpacing = tight)
    val nav = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, letterSpacing = tight)
}
