package com.porrawc2026.app.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WCColorScheme = darkColorScheme(
    primary = WCGold,
    onPrimary = WCDarkBlue,
    primaryContainer = WCBlue,
    onPrimaryContainer = WCGoldLight,
    secondary = WCGreen,
    onSecondary = TextPrimary,
    secondaryContainer = SurfaceMedium,
    onSecondaryContainer = TextSecondary,
    tertiary = WCRed,
    onTertiary = TextPrimary,
    background = WCNavy,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMedium,
    onSurfaceVariant = TextSecondary,
    outline = InputBorder,
    error = AccentRed,
    onError = TextPrimary,
)

@Composable
fun WC2026Theme(content: @Composable () -> Unit) {
    val colorScheme = WCColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = WCDarkBlue.toArgb()
            window.navigationBarColor = WCDarkBlue.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WC2026Typography,
        content = content
    )
}
