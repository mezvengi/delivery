package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = DeliveryOrangePrimary,
    primaryContainer = Color(0xFF431407),
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = EmeraldSuccess,
    tertiary = AmberWarning,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = TextMainLight,
    onSurface = TextMainLight,
    onSurfaceVariant = TextMutedLight
)

private val LightColorScheme = lightColorScheme(
    primary = DeliveryOrangePrimary,
    primaryContainer = DeliveryOrangeLight,
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF7C2D12),
    secondary = EmeraldSuccess,
    tertiary = AmberWarning,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent branding
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Enforce Right-to-Left (RTL) Arabic layout for Sour El Ghozlane users
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
