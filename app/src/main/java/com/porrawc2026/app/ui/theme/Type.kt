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
    displayLarge = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Bold, fontSize = 38.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Bold, fontSize = 30.sp),
    displaySmall = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineLarge = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    headlineMedium = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    headlineSmall = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleLarge = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Medium, fontSize = 15.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = UbuntuFont, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.5.sp),
)
