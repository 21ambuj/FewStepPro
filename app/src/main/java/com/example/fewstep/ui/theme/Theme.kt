package com.example.fewstep.ui.theme

import android.app.Activity
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
    primary = IndigoPrimaryDark,
    secondary = IndigoSecondaryDark,
    primaryContainer = Color(0xFF312E81), // Deepest Indigo for dark mode containers
    secondaryContainer = Color(0xFF1E1B4B), // Even deeper indigo
    background = DeepDarkBackground,
    surface = DeepDarkSurface,
    surfaceVariant = DeepDarkContainer,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onPrimaryContainer = TextPrimaryDark,
    onSecondaryContainer = TextPrimaryDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outlineVariant = Color(0xFF334155) // Slate 700 for distinct borders
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    secondary = IndigoSecondary,
    primaryContainer = Color(0xFFE0E7FF), // Soft Indigo
    secondaryContainer = Color(0xFFEEF2FF), // Very soft Indigo
    background = SoftLightBackground,
    surface = SoftLightSurface,
    surfaceVariant = SoftLightContainer,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onPrimaryContainer = IndigoPrimary,
    onSecondaryContainer = IndigoPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outlineVariant = Color(0xFFE2E8F0) // Zinc 200 for subtle borders
)

@Composable
fun FewStepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled for strict professional branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}