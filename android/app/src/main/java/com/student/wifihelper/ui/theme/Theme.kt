package com.student.wifihelper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ProLightColorScheme = lightColorScheme(
    primary = ProPurplePrimary,
    onPrimary = ProPurpleOnPrimary,
    primaryContainer = ProPurpleContainer,
    onPrimaryContainer = ProPurpleOnContainer,
    secondary = ProPurplePrimary,
    onSecondary = Color.White,
    secondaryContainer = ProSecondaryContainer,
    onSecondaryContainer = ProOnSecondaryContainer,
    background = ProBackground,
    onBackground = ProOnBackground,
    surface = ProSurface,
    onSurface = ProOnBackground,
    surfaceVariant = ProSurfaceVariant,
    onSurfaceVariant = ProOnSurfaceVariant,
    outline = ProOutline
)

private val ProDarkColorScheme = darkColorScheme(
    primary = ProPurpleContainer,
    onPrimary = ProPurpleOnContainer,
    primaryContainer = ProPurplePrimary,
    onPrimaryContainer = ProPurpleContainer,
    secondary = ProPurpleContainer,
    onSecondary = ProPurpleOnContainer,
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF2B2930),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99)
)

@Composable
fun UtarWifiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ProDarkColorScheme else ProLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
