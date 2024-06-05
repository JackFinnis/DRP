package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

private val defaultTypography = Typography()

val AppTypography = Typography(
    headlineMedium = defaultTypography.headlineMedium.copy(
        fontWeight = FontWeight.Medium,
    ),
)
