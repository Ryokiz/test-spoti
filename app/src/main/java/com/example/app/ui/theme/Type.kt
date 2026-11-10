package com.example.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// generated. Replace FontFamily.SansSerif with Inter (bundled or downloadable font).
val AppFont = FontFamily.SansSerif

@Immutable
data class AppTypography(
    val caption: TextStyle,
    val bodySm: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val titleSm: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val display: TextStyle,
)

val DefaultTypography = AppTypography(
    caption = TextStyle(fontFamily = AppFont, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight(400)),
    bodySm = TextStyle(fontFamily = AppFont, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight(400)),
    body = TextStyle(fontFamily = AppFont, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight(400)),
    label = TextStyle(fontFamily = AppFont, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight(600)),
    titleSm = TextStyle(fontFamily = AppFont, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight(600)),
    title = TextStyle(fontFamily = AppFont, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight(600)),
    headline = TextStyle(fontFamily = AppFont, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight(700)),
    display = TextStyle(fontFamily = AppFont, fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight(700)),
)
