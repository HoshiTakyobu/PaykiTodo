package com.example.todoalarm.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.todoalarm.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PaykiBundledFontFamily = FontFamily(
    Font(R.font.sarasa_ui_regular, FontWeight.Normal),
    Font(R.font.sarasa_ui_semibold, FontWeight.Medium),
    Font(R.font.sarasa_ui_semibold, FontWeight.SemiBold),
    Font(R.font.sarasa_ui_semibold, FontWeight.Bold)
)

val LocalInterfaceFont = androidx.compose.runtime.staticCompositionLocalOf<FontFamily> { FontFamily.SansSerif }
val PaykiInterfaceFontFamily: FontFamily
    @androidx.compose.runtime.Composable get() = LocalInterfaceFont.current
val PaykiUtilityFontFamily: FontFamily
    @androidx.compose.runtime.Composable get() = LocalInterfaceFont.current

val PaykiGreetingFontFamily = FontFamily(Font(R.font.payki_greeting))

private val BaseTypography = Typography()
val Typography = typographyFor(FontFamily.SansSerif)

fun typographyFor(font: FontFamily) = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = font),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = font),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = font),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = font, fontWeight = FontWeight.SemiBold),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = font, fontSize = 17.sp, lineHeight = 25.sp, letterSpacing = 0.sp),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = font, fontSize = 15.sp, lineHeight = 23.sp, letterSpacing = 0.sp),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = font, lineHeight = 19.sp, letterSpacing = 0.sp),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = font),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = font),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = font)
)
