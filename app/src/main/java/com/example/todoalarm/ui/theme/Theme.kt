package com.example.todoalarm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Forest,
    secondary = Clay,
    tertiary = Moss,
    background = Sand,
    surface = Sand,
    onPrimary = Sand,
    onSecondary = Sand,
    onBackground = Ink,
    onSurface = Ink
)

@Composable
fun TodoAlarmTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}

