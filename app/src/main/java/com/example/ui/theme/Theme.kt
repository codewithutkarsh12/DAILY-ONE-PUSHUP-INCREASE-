package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.preferences.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = EmberAccent,
    onPrimary = Color.White,
    secondary = EmeraldSuccess,
    onSecondary = Color.Black,
    tertiary = AmberWarning,
    background = CharcoalBackground,
    onBackground = TextPrimaryDark,
    surface = CharcoalSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    error = CrimsonMissed
)

private val LightColorScheme = lightColorScheme(
    primary = EmberAccentLight,
    onPrimary = Color.White,
    secondary = EmeraldSuccess,
    onSecondary = Color.White,
    tertiary = AmberWarning,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    error = CrimsonMissed
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
