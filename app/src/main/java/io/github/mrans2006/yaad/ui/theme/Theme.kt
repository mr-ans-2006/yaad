package io.github.mrans2006.yaad.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

enum class ThemeMode { SYSTEM, LIGHT, DARK }

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
        darkColorScheme(primary = Palette.Yellow, background = colors.background, surface = colors.card,
            onBackground = colors.text, onSurface = colors.text)
    } else {
        lightColorScheme(primary = Palette.Ink, background = colors.background, surface = colors.card,
            onBackground = colors.text, onSurface = colors.text)
    }
    CompositionLocalProvider(LocalYaadColors provides colors) {
        MaterialTheme(colorScheme = material, content = content)
    }
}

/** Short way to read the current colours: `Yaad.colors.outline`. */
object Yaad {
    val colors: YaadColors @Composable get() = LocalYaadColors.current
}
