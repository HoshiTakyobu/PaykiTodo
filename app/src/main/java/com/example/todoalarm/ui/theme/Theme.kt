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
    primary = Color(0xFF0F766E),
    secondary = Color(0xFF2563EB),
    tertiary = Color(0xFFD97706),
    background = Color(0xFFF6F7F9),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF1F4),
    primaryContainer = Color(0xFFD4F3EE),
    secondaryContainer = Color(0xFFDCE8FF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827),
    onSurfaceVariant = Color(0xFF56616F),
    outline = Color(0xFF8793A1),
    outlineVariant = Color(0xFFD9DEE5)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    secondary = Color(0xFF93C5FD),
    tertiary = Color(0xFFFBBF24),
    background = Color(0xFF0B0F14),
    surface = Color(0xFF121820),
    surfaceVariant = Color(0xFF1B2530),
    primaryContainer = Color(0xFF174D49),
    secondaryContainer = Color(0xFF1E3A5F),
    onPrimary = Color(0xFF062B28),
    onSecondary = Color(0xFF0B2544),
    onBackground = Color(0xFFE7EBF0),
    onSurface = Color(0xFFE7EBF0),
    onSurfaceVariant = Color(0xFFB4BEC9),
    outline = Color(0xFF7D8996),
    outlineVariant = Color(0xFF303B47)
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
