package io.github.mrans2006.yaad.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * True when the phone's "Remove animations" setting is on (animator duration scale = 0).
 * Yaad then skips its shared-element and slide animations and just swaps screens.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Wrap any screen in YaadTheme and it gets the right colours and fonts. */
@Composable
fun YaadTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkYaadColors else LightYaadColors
    val material = if (dark) {
        darkColorScheme(primary = colors.primary, onPrimary = colors.onPrimary, background = colors.background,
            surface = colors.card, onBackground = colors.text, onSurface = colors.text, surfaceContainerHigh = colors.card)
    } else {
        lightColorScheme(primary = colors.primary, onPrimary = colors.onPrimary, background = colors.background,
            surface = colors.card, onBackground = colors.text, onSurface = colors.text, surfaceContainerHigh = colors.card)
    }
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
            .getOrDefault(false)
    }
    val typography = Typography().let { t ->
        t.copy(
            bodyLarge = t.bodyLarge.copy(fontFamily = Inter), bodyMedium = t.bodyMedium.copy(fontFamily = Inter),
            titleLarge = t.titleLarge.copy(fontFamily = Inter), labelLarge = t.labelLarge.copy(fontFamily = Inter),
            headlineSmall = t.headlineSmall.copy(fontFamily = Inter),
        )
    }
    CompositionLocalProvider(LocalYaadColors provides colors, LocalReduceMotion provides reduceMotion) {
        MaterialTheme(colorScheme = material, typography = typography, content = content)
    }
}

/** Short way to read the current colours: `Yaad.colors.outline`. */
object Yaad {
    val colors: YaadColors @Composable get() = LocalYaadColors.current
}
