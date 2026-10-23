package com.example.todoalarm.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.todoalarm.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PaykiInterfaceFontFamily = FontFamily(
    Font(R.font.sarasa_ui_regular, FontWeight.Normal),
    Font(R.font.sarasa_ui_semibold, FontWeight.Medium),
    Font(R.font.sarasa_ui_semibold, FontWeight.SemiBold),
    Font(R.font.sarasa_ui_semibold, FontWeight.Bold)
)

val PaykiUtilityFontFamily = PaykiInterfaceFontFamily

val PaykiGreetingFontFamily = FontFamily(Font(R.font.payki_greeting))

private val BaseTypography = Typography()
val Typography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = PaykiInterfaceFontFamily),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = PaykiInterfaceFontFamily),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = PaykiInterfaceFontFamily),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = PaykiInterfaceFontFamily, fontWeight = FontWeight.SemiBold),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = PaykiInterfaceFontFamily, fontWeight = FontWeight.SemiBold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = PaykiInterfaceFontFamily, fontWeight = FontWeight.SemiBold),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = PaykiInterfaceFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = PaykiInterfaceFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = PaykiInterfaceFontFamily, fontWeight = FontWeight.SemiBold),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = PaykiInterfaceFontFamily, fontSize = 17.sp, lineHeight = 25.sp, letterSpacing = 0.sp),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = PaykiInterfaceFontFamily, fontSize = 15.sp, lineHeight = 23.sp, letterSpacing = 0.sp),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = PaykiUtilityFontFamily, lineHeight = 19.sp, letterSpacing = 0.sp),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = PaykiInterfaceFontFamily),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = PaykiInterfaceFontFamily),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = PaykiUtilityFontFamily)
)
