package com.porrawc2026.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.porrawc2026.app.R

val UbuntuFont = FontFamily(
    Font(R.font.ubuntu_light, FontWeight.Light),
    Font(R.font.ubuntu_regular, FontWeight.Normal),
    Font(R.font.ubuntu_medium, FontWeight.Medium),
    Font(R.font.ubuntu_bold, FontWeight.Bold),
)

val WC2026Typography = Typography(
    displayLarge = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Bold, fontSize = 36.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    displaySmall = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    headlineLarge = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineMedium = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    headlineSmall = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleLarge = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)
