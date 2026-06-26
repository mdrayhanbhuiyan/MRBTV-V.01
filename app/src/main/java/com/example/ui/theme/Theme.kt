package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CinematicDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = LightGreenBackground,
    surface = LightGreenCard,
    onPrimary = Color.Black, // High contrast black text on mint primary
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = LightGrey,
    onSurface = LightGrey
)

private val CinematicLightColorScheme = lightColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = Color(0xFFF1F5F2), // Very soft light-mint tint white background
    surface = Color.White,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = LightGreenBackground,
    onSurface = LightGreenCard
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to gorgeous dark mode for MRB-TV
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CinematicDarkColorScheme else CinematicLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
