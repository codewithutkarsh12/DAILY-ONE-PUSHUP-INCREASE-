package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.preferences.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = AthleticLime,
    onPrimary = Color(0xFF0B0D10),
    primaryContainer = Color(0xFF283514),
    onPrimaryContainer = AthleticLime,
    secondary = CyanSecondary,
    onSecondary = Color(0xFF071E29),
    secondaryContainer = Color(0xFF142E3D),
    onSecondaryContainer = CyanSecondary,
    tertiary = EmeraldSuccess,
    onTertiary = Color(0xFF062615),
    background = CharcoalBackground,
    onBackground = TextPrimaryDark,
    surface = CharcoalSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CharcoalSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = CardBorderDark,
    outlineVariant = CharcoalSurfaceVariant,
    error = CrimsonMissed,
    onError = Color(0xFF290609),
    errorContainer = Color(0xFF3B1418),
    onErrorContainer = CrimsonMissed
)

private val LightColorScheme = lightColorScheme(
    primary = AthleticLimeLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF7A6),
    onPrimaryContainer = Color(0xFF1D3600),
    secondary = CyanSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4F1FF),
    onSecondaryContainer = Color(0xFF002B3D),
    tertiary = EmeraldSuccessLight,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = CardBorderLight,
    outlineVariant = LightSurfaceVariant,
    error = CrimsonMissedLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E7),
    onErrorContainer = CrimsonMissedLight
)

@Composable
fun RepRiseTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RepRiseTypography,
        content = content
    )
}
