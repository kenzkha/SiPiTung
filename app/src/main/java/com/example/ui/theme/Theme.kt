package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class PintarHitungExtendedColors(
    val sunnyPill: Color,
    val sunnyPillContent: Color,
    val starGold: Color,
    val starContainer: Color,
    val success: Color,
    val successContainer: Color,
    val coral: Color,
    val coralContainer: Color,
    val berry: Color,
    val berryContainer: Color,
    val cardBorder: Color
)

private val LightExtendedColors = PintarHitungExtendedColors(
    sunnyPill = SunnyYellow,
    sunnyPillContent = OnSunnyYellow,
    starGold = SunnyYellowDeep,
    starContainer = SunnyYellowContainer,
    success = MintGreen,
    successContainer = MintContainer,
    coral = CoralRed,
    coralContainer = CoralContainer,
    berry = BerryPurple,
    berryContainer = BerryContainer,
    cardBorder = WarmBorder
)

private val DarkExtendedColors = PintarHitungExtendedColors(
    sunnyPill = SunnyYellow,
    sunnyPillContent = OnSunnyYellow,
    starGold = SunnyYellow,
    starContainer = Color(0xFF3E3110),
    success = Color(0xFF4ADE80),
    successContainer = Color(0xFF143522),
    coral = Color(0xFFFF7A76),
    coralContainer = Color(0xFF3D1D1E),
    berry = Color(0xFFA78BFA),
    berryContainer = Color(0xFF2B2046),
    cardBorder = NightBorder
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = SkyBlue,
    onPrimary = OnSkyBlue,
    primaryContainer = SkyBlueContainer,
    onPrimaryContainer = Color(0xFF0E2C6D),
    secondary = SunnyYellowDeep,
    onSecondary = Color.White,
    secondaryContainer = SunnyYellowContainer,
    onSecondaryContainer = OnSunnyYellow,
    tertiary = CoralRed,
    onTertiary = Color.White,
    tertiaryContainer = CoralContainer,
    onTertiaryContainer = Color(0xFF5A1110),
    background = WarmCream,
    onBackground = EspressoInk,
    surface = PaperWhite,
    onSurface = EspressoInk,
    surfaceVariant = SoftParchment,
    onSurfaceVariant = SlateSecondary,
    outline = WarmBorder,
    outlineVariant = Color(0xFFEDE4D4),
    error = CoralRed,
    onError = Color.White,
    errorContainer = CoralContainer,
    onErrorContainer = Color(0xFF5A1110)
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlueLight,
    onPrimary = Color(0xFF0A1D47),
    primaryContainer = Color(0xFF1A366E),
    onPrimaryContainer = SkyBlueContainer,
    secondary = SunnyYellow,
    onSecondary = OnSunnyYellow,
    secondaryContainer = Color(0xFF4A390A),
    onSecondaryContainer = SunnyYellowContainer,
    tertiary = Color(0xFFFF8A87),
    onTertiary = Color(0xFF4A0E0D),
    tertiaryContainer = Color(0xFF4F1D1C),
    onTertiaryContainer = CoralContainer,
    background = NightCanvas,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightSecondary,
    outline = NightBorder,
    outlineVariant = Color(0xFF2C344A),
    error = Color(0xFFFF8A87),
    onError = Color(0xFF4A0E0D),
    errorContainer = Color(0xFF4F1D1C),
    onErrorContainer = CoralContainer
)

private val PintarHitungShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun PintarHitungTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PintarHitungTypography,
            shapes = PintarHitungShapes,
            content = content
        )
    }
}

object PintarTheme {
    val extended: PintarHitungExtendedColors
        @Composable get() = LocalExtendedColors.current
}
