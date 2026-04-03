package com.example.todoalarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.todoalarm.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Forest,
    secondary = Clay,
    tertiary = Ocean,
    background = Sand,
    surface = Color(0xFFFFFBF4),
    surfaceVariant = Color(0xFFE8E1D3),
    onPrimary = Sand,
    onSecondary = Sand,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF52605A)
)

private val DarkColors = darkColorScheme(
    primary = WarmSun,
    secondary = Sky,
    tertiary = Moss,
    background = Night,
    surface = NightSurface,
    surfaceVariant = Color(0xFF20343C),
    onPrimary = Night,
    onSecondary = Night,
    onBackground = Color(0xFFF2F4F5),
    onSurface = Color(0xFFF2F4F5),
    onSurfaceVariant = Color(0xFFB7C2C6)
)

@Composable
fun TodoAlarmTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
