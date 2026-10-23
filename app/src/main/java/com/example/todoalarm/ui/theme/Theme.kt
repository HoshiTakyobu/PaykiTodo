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
    background = Color(0xFFEEF3F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFDDE7E5),
    primaryContainer = Color(0xFFCFE5DB),
    secondaryContainer = Color(0xFFF0E1DA),
    tertiaryContainer = Color(0xFFD7E7FA),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onPrimaryContainer = Ink,
    onSecondaryContainer = Color(0xFF4A2C1D),
    onTertiaryContainer = Color(0xFF17365F),
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF3D4F49),
    outline = Color(0xFF60736C),
    outlineVariant = Color(0xFFBCCBC5)
)

private val DarkColors = darkColorScheme(
    primary = WarmSun,
    secondary = Sky,
    tertiary = Moss,
    background = Night,
    surface = Color(0xFF1C2C38),
    surfaceVariant = Color(0xFF2B3B48),
    primaryContainer = Color(0xFF584719),
    secondaryContainer = Color(0xFF184A5A),
    tertiaryContainer = Color(0xFF1F4B35),
    onPrimary = Night,
    onSecondary = Night,
    onTertiary = Night,
    onPrimaryContainer = Color(0xFFFFE3A0),
    onSecondaryContainer = Color(0xFFD8F3FF),
    onTertiaryContainer = Color(0xFFCDEACD),
    onBackground = Color(0xFFF2F4F5),
    onSurface = Color(0xFFF2F4F5),
    onSurfaceVariant = Color(0xFFC4CED8),
    outline = Color(0xFF899AA9),
    outlineVariant = Color(0xFF435363)
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
    mainFont: String = "SYSTEM",
    customFontPath: String = "",
    content: @Composable () -> Unit
) {
    val observedSystemDark = systemDarkTheme ?: isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> observedSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val font = androidx.compose.runtime.remember(mainFont, customFontPath) { resolveMainFont(mainFont, customFontPath) }
    androidx.compose.runtime.CompositionLocalProvider(LocalInterfaceFont provides font) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = androidx.compose.runtime.remember(font) { typographyFor(font) },
        shapes = PaykiShapes,
        content = content
    )
    }
}

fun resolveMainFont(name: String, customPath: String = ""): androidx.compose.ui.text.font.FontFamily = when (name) {
    "SARASA" -> PaykiBundledFontFamily
    "SERIF" -> androidx.compose.ui.text.font.FontFamily.Serif
    "MONOSPACE" -> androidx.compose.ui.text.font.FontFamily.Monospace
    "CUSTOM" -> runCatching {
        val file = java.io.File(customPath)
        require(file.isFile)
        androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(file))
    }.getOrDefault(androidx.compose.ui.text.font.FontFamily.SansSerif)
    else -> androidx.compose.ui.text.font.FontFamily.SansSerif
}
