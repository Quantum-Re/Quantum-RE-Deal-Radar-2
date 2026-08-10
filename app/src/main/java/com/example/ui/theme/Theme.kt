package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color.White,
    primaryContainer = BentoPurpleContainer,
    onPrimaryContainer = BentoPurpleOnContainer,
    secondary = BentoPurpleOnContainer,
    onSecondary = Color.White,
    secondaryContainer = BentoPurpleHeader,
    onSecondaryContainer = BentoPurpleOnContainer,
    tertiary = AmberGold,
    background = DarkSlateBg,
    onBackground = TextPrimaryDark,
    surface = SurfaceCardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceCardDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = SurfaceCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = CyanAccent,
    onPrimary = Color.White,
    primaryContainer = BentoPurpleContainer,
    onPrimaryContainer = BentoPurpleOnContainer,
    secondary = BentoPurpleOnContainer,
    onSecondary = Color.White,
    secondaryContainer = BentoPurpleHeader,
    onSecondaryContainer = BentoPurpleOnContainer,
    tertiary = AmberGold,
    background = DarkSlateBg,
    onBackground = TextPrimaryDark,
    surface = SurfaceCardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceCardDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = SurfaceCardBorder
)

@Composable
fun QuantumDealRadarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false for consistent premium Deal Radar styling
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // Default to sleek dark investment theme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
