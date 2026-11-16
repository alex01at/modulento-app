package at.modulento.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * Material 3 theme whose primary color follows the server's `branding.accent`.
 * Without a valid accent (guest mode, unusual value) the Material defaults apply.
 */
@Composable
fun ModulentoTheme(accentHex: String? = null, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = remember(accentHex, dark) {
        brandColorScheme(BrandColor.parseHex(accentHex)?.let { Color(it) }, dark)
    }
    MaterialTheme(colorScheme = colors, content = content)
}

private fun brandColorScheme(accent: Color?, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    if (accent == null) return base

    fun readableOn(background: Color) = if (background.luminance() > 0.45f) Color.Black else Color.White

    return if (dark) {
        // Lift the accent so it stays legible on dark surfaces.
        val primary = lerp(accent, Color.White, 0.35f)
        val container = lerp(accent, Color.Black, 0.5f)
        base.copy(
            primary = primary,
            onPrimary = readableOn(primary),
            primaryContainer = container,
            onPrimaryContainer = lerp(accent, Color.White, 0.8f),
            secondary = primary,
            onSecondary = readableOn(primary),
            secondaryContainer = lerp(accent, Color.Black, 0.62f),
            onSecondaryContainer = lerp(accent, Color.White, 0.85f),
            tertiary = primary,
            onTertiary = readableOn(primary),
            surfaceTint = primary,
        )
    } else {
        base.copy(
            primary = accent,
            onPrimary = readableOn(accent),
            primaryContainer = lerp(accent, Color.White, 0.82f),
            onPrimaryContainer = lerp(accent, Color.Black, 0.6f),
            secondary = accent,
            onSecondary = readableOn(accent),
            secondaryContainer = lerp(accent, Color.White, 0.86f),
            onSecondaryContainer = lerp(accent, Color.Black, 0.65f),
            tertiary = accent,
            onTertiary = readableOn(accent),
            surfaceTint = accent,
        )
    }
}
