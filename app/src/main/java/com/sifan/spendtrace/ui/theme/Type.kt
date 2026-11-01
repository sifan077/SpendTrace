package com.sifan.spendtrace.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val default = Typography()

/** 使用系统字体，保证中文与系统字体缩放下的可读性。 */
val SpendTraceTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 37.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.2).sp,
    ),
    headlineSmall = default.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = default.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = default.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    titleSmall = default.titleSmall.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodyLarge = default.bodyLarge.copy(
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = default.bodyMedium.copy(
        fontSize = 13.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = default.bodySmall.copy(
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = default.labelLarge.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = default.labelMedium.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = default.labelSmall.copy(
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)
