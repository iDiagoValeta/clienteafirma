package es.gob.afirma.standalone.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp

/**
 * Grafito palette: monochrome, the identity comes from typography and spacing.
 * The only non-grey colours are the semantic ones (success, warning, error), kept muted.
 */
data class Grafito(
    val canvas: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val line: Color,
    val lineStrong: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val primary: Color,
    val onPrimary: Color,
    val success: Color,
    val successBg: Color,
    val warning: Color,
    val warningBg: Color,
    val error: Color,
    val errorBg: Color,
    val dark: Boolean,
)

val LightGrafito = Grafito(
    canvas = Color(0xFFF4F4F2),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEDEDEA),
    line = Color(0xFFE2E2DE),
    lineStrong = Color(0xFFC9C9C4),
    ink = Color(0xFF1C1C1B),
    inkMuted = Color(0xFF5C5C58),
    inkFaint = Color(0xFF8E8E89),
    primary = Color(0xFF1C1C1B),
    onPrimary = Color(0xFFFFFFFF),
    success = Color(0xFF2E6B45),
    successBg = Color(0xFFE6F0E9),
    warning = Color(0xFF8A5A12),
    warningBg = Color(0xFFF6EEDF),
    error = Color(0xFFA23B32),
    errorBg = Color(0xFFF6E5E3),
    dark = false,
)

val DarkGrafito = Grafito(
    canvas = Color(0xFF161616),
    surface = Color(0xFF202020),
    surfaceAlt = Color(0xFF2A2A2A),
    line = Color(0xFF2F2F2F),
    lineStrong = Color(0xFF444444),
    ink = Color(0xFFEDEDEB),
    inkMuted = Color(0xFFA6A6A2),
    inkFaint = Color(0xFF75756F),
    primary = Color(0xFFEDEDEB),
    onPrimary = Color(0xFF161616),
    success = Color(0xFF7FC096),
    successBg = Color(0xFF1C2B21),
    warning = Color(0xFFE0B36A),
    warningBg = Color(0xFF2E2618),
    error = Color(0xFFE5877E),
    errorBg = Color(0xFF331F1D),
    dark = true,
)

val LocalGrafito = staticCompositionLocalOf { LightGrafito }

private fun interFont(file: String, weight: FontWeight) =
    Font("com/formdev/flatlaf/fonts/inter/$file", weight)

val Inter = FontFamily(
    interFont("Inter-Regular.otf", FontWeight.Normal),
    interFont("Inter-SemiBold.otf", FontWeight.SemiBold),
    interFont("Inter-Bold.otf", FontWeight.Bold),
    interFont("Inter-Light.otf", FontWeight.Light),
)

private fun scheme(g: Grafito): ColorScheme {
    val base = if (g.dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = g.primary, onPrimary = g.onPrimary,
        background = g.canvas, onBackground = g.ink,
        surface = g.surface, onSurface = g.ink,
        surfaceVariant = g.surfaceAlt, onSurfaceVariant = g.inkMuted,
        outline = g.lineStrong, outlineVariant = g.line,
        error = g.error, onError = g.onPrimary,
        secondary = g.inkMuted, onSecondary = g.onPrimary,
        surfaceContainerHigh = g.surface, surfaceContainerHighest = g.surfaceAlt,
        surfaceContainer = g.surface, surfaceContainerLow = g.surface,
    )
}

private fun typography(): Typography {
    fun s(size: Int, weight: FontWeight, line: Int, track: Double = 0.0) =
        TextStyle(fontFamily = Inter, fontSize = size.sp, fontWeight = weight, lineHeight = line.sp, letterSpacing = track.sp)
    return Typography(
        displaySmall = s(34, FontWeight.Bold, 40, -0.6),
        headlineMedium = s(26, FontWeight.Bold, 32, -0.4),
        headlineSmall = s(21, FontWeight.SemiBold, 28, -0.2),
        titleLarge = s(18, FontWeight.SemiBold, 24),
        titleMedium = s(15, FontWeight.SemiBold, 22),
        titleSmall = s(13, FontWeight.SemiBold, 18),
        bodyLarge = s(15, FontWeight.Normal, 23),
        bodyMedium = s(14, FontWeight.Normal, 21),
        bodySmall = s(12, FontWeight.Normal, 17),
        labelLarge = s(14, FontWeight.SemiBold, 20),
        labelMedium = s(12, FontWeight.SemiBold, 16, 0.2),
        labelSmall = s(11, FontWeight.SemiBold, 14, 0.6),
    )
}

@Composable
fun GrafitoTheme(dark: Boolean, content: @Composable () -> Unit) {
    val g = if (dark) DarkGrafito else LightGrafito
    androidx.compose.runtime.CompositionLocalProvider(LocalGrafito provides g) {
        MaterialTheme(colorScheme = scheme(g), typography = typography(), content = content)
    }
}
