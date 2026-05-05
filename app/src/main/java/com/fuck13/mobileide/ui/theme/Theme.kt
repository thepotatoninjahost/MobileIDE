package com.fuck13.mobileide.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color.Black,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = ElectricCyan,
    
    secondary = NeonGreen,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = NeonGreen,
    
    tertiary = VividPink,
    onTertiary = Color.White,
    tertiaryContainer = DarkSurfaceVariant,
    onTertiaryContainer = VividPink,
    
    background = DarkBackground,
    onBackground = DarkOnBackground,
    
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFF410002),
    onErrorContainer = Color(0xFFFFDAD6),
    
    outline = DarkSurfaceVariant,
    outlineVariant = DarkOnSurfaceVariant.copy(alpha = 0.3f),
    
    inverseSurface = LightOnBackground,
    inverseOnSurface = LightBackground,
    inversePrimary = Color(0xFF00B8A9),
    
    scrim = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00B8A9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8FFF8),
    onPrimaryContainer = Color(0xFF00201C),
    
    secondary = Color(0xFF4B6032),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCF5A9),
    onSecondaryContainer = Color(0xFF0E2000),
    
    tertiary = Color(0xFFB91D6E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E3),
    onTertiaryContainer = Color(0xFF3E001D),
    
    background = LightBackground,
    onBackground = LightOnBackground,
    
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    
    inverseSurface = DarkBackground,
    inverseOnSurface = DarkOnBackground,
    inversePrimary = ElectricCyan,
    
    scrim = Color.Black
)

@Composable
fun MobileIDETheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
