package com.mswdo.kalayaan.faced.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
    primary = Blue600,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = Emerald600,
    background = SlateBg,
    surface = androidx.compose.ui.graphics.Color.White,
    error = Red600
)

@Composable
fun FacedKalayaanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        content = content
    )
}
