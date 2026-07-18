package com.example.todoalarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Forest,
    secondary = Clay,
    tertiary = Ocean,
    background = Sand,
    surface = Color(0xFFFFFBF4),
    surfaceVariant = Color(0xFFE8E1D3),
    primaryContainer = Color(0xFFDCE9E2),
    secondaryContainer = Color(0xFFF2DFD2),
    tertiaryContainer = Color(0xFFDDE7F8),
    onPrimary = Sand,
    onSecondary = Sand,
    onTertiary = Color.White,
    onPrimaryContainer = Ink,
    onSecondaryContainer = Color(0xFF4A2C1D),
    onTertiaryContainer = Color(0xFF17365F),
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF52605A),
    outline = Color(0xFF7C8983),
    outlineVariant = Color(0xFFD0D8D2)
)

private val DarkColors = darkColorScheme(
    primary = WarmSun,
    secondary = Sky,
    tertiary = Moss,
    background = Night,
    surface = NightSurface,
    surfaceVariant = Color(0xFF243A44),
    primaryContainer = Color(0xFF5B461C),
    secondaryContainer = Color(0xFF294752),
    tertiaryContainer = Color(0xFF2D4934),
    onPrimary = Night,
    onSecondary = Night,
    onTertiary = Night,
    onPrimaryContainer = Color(0xFFFFE3A0),
    onSecondaryContainer = Color(0xFFD8F3FF),
    onTertiaryContainer = Color(0xFFCDEACD),
    onBackground = Color(0xFFF2F4F5),
    onSurface = Color(0xFFF2F4F5),
    onSurfaceVariant = Color(0xFFCDD6DA),
    outline = Color(0xFF93A3AA),
    outlineVariant = Color(0xFF40545D)
)

private val PaykiShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun TodoAlarmTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    systemDarkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val observedSystemDark = systemDarkTheme ?: isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> observedSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = PaykiShapes,
        content = content
    )
}
