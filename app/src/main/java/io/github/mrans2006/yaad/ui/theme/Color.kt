package io.github.mrans2006.yaad.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.mrans2006.yaad.core.Tag

/** The flat, bright neo-brutalist palette. */
object Palette {
    val Yellow = Color(0xFFFFD23F)
    val Pink = Color(0xFFFF8FD8)
    val Purple = Color(0xFFB79CFF)
    val Green = Color(0xFF7DEB8E)
    val Blue = Color(0xFF7CC6FF)
    val Orange = Color(0xFFFFA35C)
    val Red = Color(0xFFFF6B6B)
    val Teal = Color(0xFF5FE3D0)
    val Ink = Color(0xFF111111)
    val Paper = Color(0xFFFFF6E5)
    val White = Color(0xFFFFFFFF)
    val Night = Color(0xFF151515)
    val NightCard = Color(0xFF232323)
    val Bone = Color(0xFFF3EEE3)
}

/**
 * Every colour a screen needs, for light OR dark mode.
 * Screens never hard-code "black": they ask for [YaadColors.outline], so dark mode just works.
 */
@Immutable
data class YaadColors(
    val background: Color,
    val card: Color,
    val outline: Color,
    val shadow: Color,
    val text: Color,
    val subtext: Color,
    val onAccent: Color,
    val isDark: Boolean,
)

val LightYaadColors = YaadColors(
    background = Palette.Paper, card = Palette.White, outline = Palette.Ink, shadow = Palette.Ink,
    text = Palette.Ink, subtext = Color(0xFF55524C), onAccent = Palette.Ink, isDark = false,
)

val DarkYaadColors = YaadColors(
    background = Palette.Night, card = Palette.NightCard, outline = Palette.Bone, shadow = Palette.Bone,
    text = Palette.Bone, subtext = Color(0xFFB5AFA4), onAccent = Palette.Ink, isDark = true,
)

val LocalYaadColors = staticCompositionLocalOf { LightYaadColors }

/** Each tag has its own colour, so you can spot "Code" or "Places" at a glance. */
fun tagColor(tag: Tag?): Color = when (tag) {
    Tag.CODE -> Palette.Purple
    Tag.TOOLS -> Palette.Yellow
    Tag.PLACES -> Palette.Green
    Tag.STUDY -> Palette.Blue
    Tag.HACKS -> Palette.Pink
    Tag.SHOPPING -> Palette.Orange
    Tag.QUOTES -> Palette.Teal
    Tag.CHATS -> Palette.Red
    null -> Color(0xFFD9D4CA)
}

fun tagColor(label: String): Color = tagColor(Tag.fromLabel(label))
