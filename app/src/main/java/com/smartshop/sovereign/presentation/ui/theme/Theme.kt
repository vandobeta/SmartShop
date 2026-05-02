package com.smartshop.sovereign.presentation.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SmartShopColors.ElectricBlue,
    onPrimary = SmartShopColors.Black,
    primaryContainer = SmartShopColors.ElectricBlue.copy(alpha = 0.3f),
    onPrimaryContainer = SmartShopColors.TextPrimary,
    secondary = SmartShopColors.ElectricBlue,
    onSecondary = SmartShopColors.Black,
    background = SmartShopColors.DarkBackground,
    onBackground = SmartShopColors.TextPrimary,
    surface = SmartShopColors.CardBackground,
    onSurface = SmartShopColors.TextPrimary,
    surfaceVariant = SmartShopColors.SurfaceVariant,
    onSurfaceVariant = SmartShopColors.TextSecondary,
    error = SmartShopColors.ErrorRed,
    onError = SmartShopColors.TextPrimary
)

/**
 * SmartShop Sovereign Theme - Noir-Cyber
 */
@Composable
fun SmartShopTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SmartShopColors.Black.toArgb()
            window.navigationBarColor = SmartShopColors.Black.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}