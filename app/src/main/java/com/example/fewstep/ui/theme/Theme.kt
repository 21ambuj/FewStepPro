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
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    secondary = IndigoSecondaryDark,
    background = DeepDarkBackground,
    surface = DeepDarkSurface,
    onPrimary = TextPrimaryDark,
    onSecondary = TextPrimaryDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    secondary = IndigoSecondary,
    background = SlateBackground,
    surface = SlateSurface,
    onPrimary = SlateSurface,
    onSecondary = SlateSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary
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