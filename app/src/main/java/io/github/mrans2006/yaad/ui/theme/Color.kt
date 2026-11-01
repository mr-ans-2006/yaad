package io.github.mrans2006.yaad.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.mrans2006.yaad.core.Tag

/**
 * The "soft brutalist" palette (v1.1): a warm cream page, ink-thin outlines,
 * and colour ONLY on tags (muted pastels), so the screenshots stay the colourful part.
 */
object Palette {
    // Light
    val Cream = Color(0xFFFBF7EE)
    val White = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1C1B19)
    val InkSoft = Color(0xFF6B665D)
    val Muted = Color(0xFFF3EEE3)

    // Dark (warm, not pure black)
    val Night = Color(0xFF171512)
    val NightCard = Color(0xFF221F1B)
    val NightMuted = Color(0xFF2B2823)
    val Bone = Color(0xFFF1ECE2)
    val BoneSoft = Color(0xFFA9A296)
    val BoneLine = Color(0xFFD8D0C2)

    // Muted tag colours (same in light and dark; tag text is always ink)
    val TagCode = Color(0xFFDCD2F7)
    val TagTools = Color(0xFFF5E3A6)
    val TagPlaces = Color(0xFFCDE9C8)
    val TagStudy = Color(0xFFCFE1F3)
    val TagHacks = Color(0xFFF5CFE2)
    val TagShopping = Color(0xFFF6D6B9)
    val TagQuotes = Color(0xFFC9E8E1)
    val TagChats = Color(0xFFF3CBC5)
    val TagNone = Color(0xFFE6E0D4)

    /** The app icon background (kept from v1.0). */
    val Yellow = Color(0xFFFFD23F)
}

/**
 * Every colour a screen needs, for light OR dark mode.
 * Screens never hard-code "black": they ask for [YaadColors.outline], so dark mode just works.
 */
@Immutable
data class YaadColors(
    val background: Color,
    val card: Color,
    /** Image frames and quiet fills. */
    val muted: Color,
    val outline: Color,
    val shadow: Color,
    val text: Color,
    val subtext: Color,
    /** Primary buttons and the selected tab: black in light mode, bone in dark mode. */
    val primary: Color,
    val onPrimary: Color,
    /** Text on a tag colour. */
    val onAccent: Color,
    val isDark: Boolean,
)

val LightYaadColors = YaadColors(
    background = Palette.Cream, card = Palette.White, muted = Palette.Muted, outline = Palette.Ink, shadow = Palette.Ink,
    text = Palette.Ink, subtext = Palette.InkSoft, primary = Palette.Ink, onPrimary = Palette.Cream,
    onAccent = Palette.Ink, isDark = false,
)

val DarkYaadColors = YaadColors(
    background = Palette.Night, card = Palette.NightCard, muted = Palette.NightMuted, outline = Palette.BoneLine,
    shadow = Palette.BoneLine, text = Palette.Bone, subtext = Palette.BoneSoft, primary = Palette.Bone,
    onPrimary = Palette.Night, onAccent = Palette.Ink, isDark = true,
)

val LocalYaadColors = staticCompositionLocalOf { LightYaadColors }

/** Each tag has its own muted colour, so you can spot "Code" or "Places" at a glance. */
fun tagColor(tag: Tag?): Color = when (tag) {
    Tag.CODE -> Palette.TagCode
    Tag.TOOLS -> Palette.TagTools
    Tag.PLACES -> Palette.TagPlaces
    Tag.STUDY -> Palette.TagStudy
    Tag.HACKS -> Palette.TagHacks
    Tag.SHOPPING -> Palette.TagShopping
    Tag.QUOTES -> Palette.TagQuotes
    Tag.CHATS -> Palette.TagChats
    null -> Palette.TagNone
}

fun tagColor(label: String): Color = tagColor(Tag.fromLabel(label))
